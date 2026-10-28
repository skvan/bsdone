// 个人驾驶舱自由画布几何工具：纯函数、无外部依赖，可直接用 node --test 验证。
// 坐标原点在画布左上角，x 向右、y 向下，单位均为像素。

/** 画布逻辑宽度：1280px 页面最大宽度减去左右各 32px 内边距 */
export const BOARD_WIDTH = 1216;
/** 任意两个模块之间至少保留的间距 */
export const BOARD_GAP = 16;
/** 拖动与缩放的吸附步长 */
export const BOARD_SNAP = 8;
/** 编辑画布最小高度，留出可拖动空间 */
export const BOARD_MIN_HEIGHT = 720;
/** 单个模块高度上限 */
export const MAX_MODULE_HEIGHT = 1600;
/** 模块 y 坐标上限，避免异常配置无限拉长画布 */
export const MAX_MODULE_Y = 12000;

/** 八方向缩放手柄 */
const RESIZE_DIRECTIONS = new Set(['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw']);

/**
 * 四舍五入到最近的吸附网格线。
 * @param {number} value 原始坐标或尺寸
 * @param {number} [step=BOARD_SNAP] 吸附步长
 * @returns {number}
 */
export function snapToGrid(value, step = BOARD_SNAP) {
  const numeric = Number(value);
  const numericStep = Number(step);
  const unit = numericStep > 0 ? numericStep : BOARD_SNAP;
  if (!Number.isFinite(numeric)) return 0;
  const snapped = Math.round(numeric / unit) * unit;
  // 归一化 -0，方便严格相等断言与 JSON 序列化
  return snapped === 0 ? 0 : snapped;
}

/**
 * 将数值限制在 [min, max] 区间。
 * @param {number} value
 * @param {number} min
 * @param {number} max
 * @returns {number}
 */
export function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

/**
 * 判断两个矩形在保持 gap 间距时是否重叠。
 * 恰好相距 gap 视为不重叠，小于 gap 视为重叠。
 * @param {{x:number,y:number,width:number,height:number}} a
 * @param {{x:number,y:number,width:number,height:number}} b
 * @param {number} [gap=BOARD_GAP]
 * @returns {boolean}
 */
export function rectsOverlap(a, b, gap = BOARD_GAP) {
  return a.x < b.x + b.width + gap
    && a.x + a.width + gap > b.x
    && a.y < b.y + b.height + gap
    && a.y + a.height + gap > b.y;
}

/**
 * 判断矩形是否与集合中的任一其他模块重叠（同 key 跳过）。
 * @param {{key?:string,x:number,y:number,width:number,height:number}} rect
 * @param {Array<{key?:string,x:number,y:number,width:number,height:number}>} others
 * @param {number} [gap=BOARD_GAP]
 * @returns {boolean}
 */
export function collidesWithAny(rect, others, gap = BOARD_GAP) {
  return others.some((other) => other.key !== rect.key && rectsOverlap(rect, other, gap));
}

/**
 * 把矩形收敛回画布：整数坐标、宽度不超画布、高度不超上限、x/y 不为负。
 * @param {{x:number,y:number,width:number,height:number}} rect
 * @returns {{x:number,y:number,width:number,height:number}}
 */
export function clampToBoard(rect) {
  const width = clamp(Math.round(rect.width), 1, BOARD_WIDTH);
  const height = clamp(Math.round(rect.height), 1, MAX_MODULE_HEIGHT);
  const x = clamp(Math.round(rect.x), 0, BOARD_WIDTH - width);
  const y = clamp(Math.round(rect.y), 0, MAX_MODULE_Y);
  return { ...rect, x, y, width, height };
}

/**
 * 计算拖动后的矩形：位移按吸附步长取整并限制在画布内（不检查碰撞）。
 * @param {{key?:string,x:number,y:number,width:number,height:number}} start
 * @param {number} dx
 * @param {number} dy
 * @param {{snap?:boolean}} [options]
 * @returns {{key?:string,x:number,y:number,width:number,height:number}}
 */
export function movedRect(start, dx, dy, { snap = true } = {}) {
  let x = start.x + dx;
  let y = start.y + dy;
  if (snap) {
    x = snapToGrid(x);
    y = snapToGrid(y);
  }
  return clampToBoard({ ...start, x, y });
}

/**
 * 计算拖动后的候选矩形：位移按吸附步长取整并限制在画布内。
 * 与任一其他模块冲突时返回 null，调用方应保留最近一次合法位置。
 * @param {{key?:string,x:number,y:number,width:number,height:number}} start
 * @param {number} dx
 * @param {number} dy
 * @param {Array<{key?:string,x:number,y:number,width:number,height:number}>} others
 * @param {{snap?:boolean}} [options]
 * @returns {{key?:string,x:number,y:number,width:number,height:number}|null}
 */
export function moveModule(start, dx, dy, others, options = {}) {
  const candidate = movedRect(start, dx, dy, options);
  return collidesWithAny(candidate, others) ? null : candidate;
}

/** 两个矩形的重叠面积；无重叠返回 0。 */
function overlapArea(a, b) {
  const width = Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x);
  const height = Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y);
  return width > 0 && height > 0 ? width * height : 0;
}

/** 拖动交换的最小重叠比例（相对较小模块面积）。 */
export const SWAP_MIN_RATIO = 0.35;

/**
 * 拖动时寻找交换目标：取重叠面积占较小模块面积比例最高者。
 * 仅比例达到 minRatio 才视为“拖到了对方身上”。
 * @param {{key?:string,x:number,y:number,width:number,height:number}} dragged
 * @param {Array<{key?:string,x:number,y:number,width:number,height:number}>} others
 * @param {{minRatio?:number}} [options]
 * @returns {{key?:string,x:number,y:number,width:number,height:number}|null}
 */
export function findSwapTarget(dragged, others, { minRatio = SWAP_MIN_RATIO } = {}) {
  if (!dragged || !Array.isArray(others)) return null;
  let best = null;
  let bestRatio = 0;
  let bestArea = 0;
  for (const other of others) {
    if (!other || other.key === dragged.key) continue;
    const area = overlapArea(dragged, other);
    if (area <= 0) continue;
    const smaller = Math.min(dragged.width * dragged.height, other.width * other.height);
    if (smaller <= 0) continue;
    const ratio = area / smaller;
    if (ratio < minRatio) continue;
    if (ratio > bestRatio + 1e-9 || (Math.abs(ratio - bestRatio) <= 1e-9 && area > bestArea)) {
      best = other;
      bestRatio = ratio;
      bestArea = area;
    }
  }
  return best;
}

/**
 * 交换两个模块的位置（保留各自尺寸），并把被挤到的第三方模块整体下移。
 * 规则：
 *  - A 放到 B 的原位置、B 放到 A 的原位置，越界时收敛回画布；
 *  - 两个位置互相重叠时（常见于上下相邻、高度不同的模块），上方模块保留，
 *    下方模块整体下移到上方模块底边 + gap，再做第三方回流；
 *  - 与 A/B 或已下移模块冲突的第三方，按 y/x 升序下移到冲突底边 + gap；
 *  - 下移超过 MAX_MODULE_Y 或无法收敛时放弃。
 * @param {{key?:string,x:number,y:number,width:number,height:number}} start
 * @param {{key?:string,x:number,y:number,width:number,height:number}} target
 * @param {Array<{key?:string,x:number,y:number,width:number,height:number}>} others
 * @param {{gap?:number}} [options]
 * @returns {{dragged:object,target:object,moved:Array<object>}|null}
 */
export function planSwap(start, target, others, { gap = BOARD_GAP } = {}) {
  if (!start || !target || start.key === target.key) return null;
  let dragged = clampToBoard({ ...start, x: target.x, y: target.y });
  let targetAtStart = clampToBoard({ ...target, x: start.x, y: start.y });
  if (rectsOverlap(dragged, targetAtStart, gap)) {
    if (dragged.y <= targetAtStart.y) {
      targetAtStart = { ...targetAtStart, y: dragged.y + dragged.height + gap };
      if (targetAtStart.y > MAX_MODULE_Y) return null;
    } else {
      dragged = { ...dragged, y: targetAtStart.y + targetAtStart.height + gap };
      if (dragged.y > MAX_MODULE_Y) return null;
    }
  }

  const ordered = (Array.isArray(others) ? others : [])
    .filter((module) => module && module.key !== start.key && module.key !== target.key)
    .sort((a, b) => (a.y - b.y) || (a.x - b.x));
  const accepted = [dragged, targetAtStart];
  const moved = [];
  for (const module of ordered) {
    let current = { ...module };
    for (let guard = 0; ; guard += 1) {
      if (guard > accepted.length + 2) return null;
      const colliders = accepted.filter((other) => rectsOverlap(current, other, gap));
      if (!colliders.length) break;
      const bottom = colliders.reduce((max, other) => Math.max(max, other.y + other.height), 0);
      const nextY = bottom + gap;
      if (nextY <= current.y) return null;
      if (nextY > MAX_MODULE_Y) return null;
      current = { ...current, y: nextY };
    }
    if (current.y !== module.y) moved.push(current);
    accepted.push(current);
  }
  return { dragged, target: targetAtStart, moved };
}

/**
 * 计算缩放后的候选矩形。direction 为 n/s/e/w 的单边或对角组合，
 * 对边/对角保持固定；同时满足最小尺寸、画布宽度、模块高度与 y 上限。
 * 与任一其他模块冲突或越界时返回 null。
 * @param {{key?:string,x:number,y:number,width:number,height:number}} start
 * @param {'n'|'s'|'e'|'w'|'ne'|'nw'|'se'|'sw'} direction
 * @param {number} dx
 * @param {number} dy
 * @param {number} minWidth
 * @param {number} minHeight
 * @param {Array<{key?:string,x:number,y:number,width:number,height:number}>} others
 * @param {{snap?:boolean}} [options]
 * @returns {{key?:string,x:number,y:number,width:number,height:number}|null}
 */
export function resizeModule(start, direction, dx, dy, minWidth, minHeight, others, { snap = true } = {}) {
  if (!RESIZE_DIRECTIONS.has(direction)) return null;

  let left = start.x;
  let top = start.y;
  let right = start.x + start.width;
  let bottom = start.y + start.height;

  if (direction.includes('e')) right += dx;
  if (direction.includes('s')) bottom += dy;
  if (direction.includes('w')) left += dx;
  if (direction.includes('n')) top += dy;

  if (snap) {
    if (direction.includes('e')) right = snapToGrid(right);
    if (direction.includes('s')) bottom = snapToGrid(bottom);
    if (direction.includes('w')) left = snapToGrid(left);
    if (direction.includes('n')) top = snapToGrid(top);
  }

  // 反方向边固定，最小尺寸优先于用户拖动距离
  if (direction.includes('w')) left = Math.min(left, right - minWidth);
  else right = Math.max(right, left + minWidth);
  if (direction.includes('n')) top = Math.min(top, bottom - minHeight);
  else bottom = Math.max(bottom, top + minHeight);

  // 高度上限与画布左右边界
  if (direction.includes('n')) top = Math.max(top, bottom - MAX_MODULE_HEIGHT);
  if (direction.includes('s')) bottom = Math.min(bottom, top + MAX_MODULE_HEIGHT);
  left = Math.max(0, left);
  top = Math.max(0, top);
  right = Math.min(BOARD_WIDTH, right);
  if (right - left < minWidth || bottom - top < minHeight) return null;

  const candidate = {
    key: start.key,
    x: Math.round(left),
    y: Math.round(top),
    width: Math.round(right - left),
    height: Math.round(bottom - top)
  };
  if (candidate.x < 0 || candidate.y < 0 || candidate.y > MAX_MODULE_Y) return null;
  if (candidate.x + candidate.width > BOARD_WIDTH) return null;
  if (candidate.width < minWidth || candidate.height < minHeight) return null;
  if (candidate.height > MAX_MODULE_HEIGHT) return null;
  return collidesWithAny(candidate, others) ? null : candidate;
}

/**
 * 按数组顺序从左到右装箱，当前行放不下时换行；用于默认布局与迁移兜底。
 * 输入的宽高会取整并收敛到画布允许范围，返回新数组，不修改入参。
 * @param {Array<{width:number,height:number}>} modules
 * @param {{width?:number,gap?:number}} [options]
 * @returns {Array<{x:number,y:number,width:number,height:number}>}
 */
export function packModules(modules, { width = BOARD_WIDTH, gap = BOARD_GAP } = {}) {
  let x = 0;
  let y = 0;
  let rowHeight = 0;
  return modules.map((module) => {
    const moduleWidth = clamp(Math.round(module.width), 1, width);
    const moduleHeight = clamp(Math.round(module.height), 1, MAX_MODULE_HEIGHT);
    if (x + moduleWidth > width) {
      x = 0;
      y += rowHeight + gap;
      rowHeight = 0;
    }
    const placed = { ...module, x, y, width: moduleWidth, height: moduleHeight };
    x += moduleWidth + gap;
    rowHeight = Math.max(rowHeight, moduleHeight);
    return placed;
  });
}

/**
 * 画布高度 = max(最小高度, 所有模块底边最大值)。
 * @param {Array<{y:number,height:number}>} modules
 * @param {number} [minimum=BOARD_MIN_HEIGHT]
 * @returns {number}
 */
export function boardHeight(modules, minimum = BOARD_MIN_HEIGHT) {
  const bottom = modules.reduce((max, module) => Math.max(max, module.y + module.height), 0);
  return Math.max(minimum, bottom);
}

/**
 * 把模块放到所有已占用模块的下方（重新启用模块时的兜底）。
 * @param {number} width
 * @param {number} height
 * @param {Array<{y:number,height:number}>} others
 * @returns {{x:number,y:number,width:number,height:number}}
 */
export function placeBelow(width, height, others) {
  const bottom = others.reduce((max, other) => Math.max(max, other.y + other.height), 0);
  const y = others.length ? bottom + BOARD_GAP : 0;
  return { x: 0, y, width: Math.min(width, BOARD_WIDTH), height };
}
