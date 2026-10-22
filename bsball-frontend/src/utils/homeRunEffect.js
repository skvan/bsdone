// 越墙全垒打（HR）专用落点几何与视觉效果
// 与实时录入、比赛详情回放共用：射线外推落点、彩虹轨迹配色、两侧火花、落点绽放烟花。

// 彩虹轨迹（实时录入/回放 SVG 渐变共用）
export const HR_TRAIL_STOPS = [
  { offset: '0%', color: '#38BDF8' },
  { offset: '26%', color: '#B388FF' },
  { offset: '52%', color: '#FF4FD8' },
  { offset: '76%', color: '#FFD700' },
  { offset: '100%', color: '#4ADE80' },
];
export const HR_FIREWORK_HOLD_MS = 1500;
export const HR_SIDE_SPARK_INTERVAL_MS = 75;
export const HR_OUT_MARGIN = 40;

const HR_FIREWORK_COLORS = ['#38BDF8', '#B388FF', '#FF4FD8', '#FFD700', '#4ADE80', '#FFFFFF'];
const HR_INNER_COLORS = ['#FFF7D6', '#FFE082', '#FFD700', '#FFFFFF'];

// 场地几何与 LiveGame/GameDetailHitReplay 的球场 SVG 同源：
// 草坪扇形 M 240 590 L -179.5 165.8 A 572.2 572.2 0 0 1 659.5 165.8 Z
const HOME = { x: 240, y: 555 };
const APEX = { x: 240, y: 590 };
const LEFT_POLE = { x: -179.5, y: 165.8 };
const RIGHT_POLE = { x: 659.5, y: 165.8 };
const WALL = { x: 240, y: 165.8 + Math.sqrt(572.2 ** 2 - 419.5 ** 2), r: 572.2 };

const FALLBACK_DIRECTIONS = {
  LF: { x: 75, y: 230 },
  CF: { x: 240, y: 150 },
  RF: { x: 405, y: 230 },
  XF1: { x: 158, y: 190 },
  XF2: { x: 322, y: 190 },
};

/**
 * HR 落点记录用的标准野位：垒球 XF1/XF2 映射到 LF/RF，避免 hit-spray 的
 * FIELD_POS_CODES 拒绝 XF 后回退成“内野”。
 */
export function homeRunFieldPos(fielderPos) {
  const pos = String(fielderPos ?? '').trim().toUpperCase();
  if (pos === 'XF1') return 'LF';
  if (pos === 'XF2') return 'RF';
  return ['LF', 'CF', 'RF'].includes(pos) ? pos : 'CF';
}

function cross(a, b, p) {
  return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
}

/** 点是否在草坪扇形内（含边线；不含本垒后方/墙外） */
export function isInsideFairFan(x, y) {
  const px = Number(x);
  const py = Number(y);
  if (!Number.isFinite(px) || !Number.isFinite(py)) return false;
  const p = { x: px, y: py };
  const left = cross(APEX, LEFT_POLE, p);
  const right = cross(APEX, RIGHT_POLE, p);
  if (left < 0 || right > 0) return false;
  return Math.hypot(px - WALL.x, py - WALL.y) <= WALL.r;
}

/**
 * 越墙全垒打落点：
 * - 拖拽点已在扇形外且在本垒前方：原样使用；
 * - 在扇形内（或本垒后方）：沿「本垒 -> 落点」射线推到墙外，并额外留 HR_OUT_MARGIN。
 * 方向退化（落点在本垒后方）时退回所选外野手方向。
 * @param {number} x
 * @param {number} y
 * @param {string} fielderPos LF/CF/RF/XF1/XF2
 * @returns {{x:number,y:number}}
 */
export function projectHomeRunLanding(x, y, fielderPos) {
  let px = Number(x);
  let py = Number(y);
  const fallback = FALLBACK_DIRECTIONS[String(fielderPos ?? '').trim().toUpperCase()] ?? FALLBACK_DIRECTIONS.CF;
  if (!Number.isFinite(px) || !Number.isFinite(py)) {
    px = fallback.x;
    py = fallback.y;
  }

  // 已经在扇形外、且不是本垒后方：尊重用户拖到的位置
  if (!isInsideFairFan(px, py) && py <= HOME.y) {
    return { x: px, y: py };
  }

  let dx = px - HOME.x;
  let dy = py - HOME.y;
  if (dy >= 0 || Math.hypot(dx, dy) < 1) {
    dx = fallback.x - HOME.x;
    dy = fallback.y - HOME.y;
  }
  const len = Math.hypot(dx, dy);
  dx /= len;
  dy /= len;

  // 从本垒沿射线求与围墙圆的远交点，再向外留 margin，保证在扇形外
  const ox = HOME.x - WALL.x;
  const oy = HOME.y - WALL.y;
  const b = ox * dx + oy * dy;
  const c = ox * ox + oy * oy - WALL.r * WALL.r;
  const disc = b * b - c;
  const wallT = disc > 0 ? -b + Math.sqrt(disc) : WALL.r;
  const dist = wallT + HR_OUT_MARGIN;
  return { x: HOME.x + dx * dist, y: HOME.y + dy * dist };
}

/** 烟花绽放粒子参数（纯函数，便于单测；random 可注入） */
export function buildFireworkBurstSpecs({ count = 48, radius = 100, delay = 0, palette = HR_FIREWORK_COLORS, random = Math.random } = {}) {
  const specs = [];
  for (let i = 0; i < count; i++) {
    const angle = (Math.PI * 2 * i) / count + (random() - 0.5) * 0.2;
    const distance = radius * (0.62 + random() * 0.38);
    specs.push({
      angle,
      distance,
      length: 5 + random() * 9,
      width: 2 + random() * 1.8,
      color: palette[i % palette.length],
      duration: 780 + random() * 480,
      delay: delay + random() * 100,
    });
  }
  return specs;
}

function svgEl(doc, tag, attrs = {}) {
  const el = doc.createElementNS('http://www.w3.org/2000/svg', tag);
  for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, String(v));
  return el;
}

/** 在 SVG 图层上播放落点烟花（三层绽放：外圈/中圈/内圈 + 闪光 + 圆环） */
export function playHomeRunFireworks(layer, x, y) {
  const doc = layer?.ownerDocument;
  if (!doc) return;
  const group = svgEl(doc, 'g', { class: 'hr-firework-burst' });
  layer.appendChild(group);

  const flash = svgEl(doc, 'circle', { cx: x, cy: y, r: 18, fill: '#FFF7D6' });
  flash.style.filter = 'drop-shadow(0 0 18px rgba(255,220,120,.95))';
  flash.style.transformOrigin = `${x}px ${y}px`;
  group.appendChild(flash);
  flash.animate([
    { opacity: 0.95, transform: 'scale(.3)' },
    { opacity: 0, transform: 'scale(3.6)' },
  ], { duration: 650, easing: 'ease-out', fill: 'forwards' });

  const bursts = [
    { count: 48, radius: 100, delay: 0, palette: HR_FIREWORK_COLORS },
    { count: 24, radius: 68, delay: 130, palette: HR_FIREWORK_COLORS },
    { count: 12, radius: 40, delay: 260, palette: HR_INNER_COLORS },
  ];
  for (const burst of bursts) {
    for (const spec of buildFireworkBurstSpecs(burst)) {
      const line = svgEl(doc, 'line', {
        x1: x, y1: y,
        x2: x + Math.cos(spec.angle) * spec.length,
        y2: y + Math.sin(spec.angle) * spec.length,
        stroke: spec.color,
        'stroke-width': spec.width,
        'stroke-linecap': 'round',
      });
      line.style.filter = 'drop-shadow(0 0 4px rgba(255,255,255,.8))';
      line.style.transformOrigin = `${x}px ${y}px`;
      group.appendChild(line);
      line.animate([
        { opacity: 0, transform: 'translate(0px, 0px) scale(.55)' },
        { opacity: 1, offset: 0.16 },
        { opacity: 0, transform: `translate(${Math.cos(spec.angle) * spec.distance}px, ${Math.sin(spec.angle) * spec.distance}px) scale(1.08)` },
      ], {
        duration: spec.duration,
        delay: spec.delay,
        easing: 'cubic-bezier(.16,.84,.32,1)',
        fill: 'forwards',
      });
    }
    const ringRadius = Math.max(10, burst.radius * 0.12);
    const ring = svgEl(doc, 'circle', {
      cx: x, cy: y, r: ringRadius,
      fill: 'none', stroke: burst.palette[0], 'stroke-width': 3,
    });
    ring.style.transformOrigin = `${x}px ${y}px`;
    group.appendChild(ring);
    ring.animate([
      { opacity: 0.9, transform: 'scale(.5)' },
      { opacity: 0, transform: `scale(${burst.radius / ringRadius})` },
    ], { duration: 700, delay: burst.delay, easing: 'ease-out', fill: 'forwards' });
  }

  setTimeout(() => group.remove(), 2100);
}

/** 在飞行轨迹两侧喷一小簇火花（由动画循环节流调用；dirX/dirY 为飞行方向增量） */
export function spawnHomeRunSideSparks(layer, x, y, dirX, dirY) {
  const doc = layer?.ownerDocument;
  if (!doc) return;
  const len = Math.hypot(dirX, dirY) || 1;
  const ux = dirX / len;
  const uy = dirY / len;
  const px = -uy;
  const py = ux;
  const group = svgEl(doc, 'g', { class: 'hr-side-sparks' });
  layer.appendChild(group);
  for (const side of [-1, 1]) {
    for (let i = 0; i < 2; i++) {
      const off = 9 + Math.random() * 18;
      const sx = x + px * side * off + ux * (Math.random() * 12 - 6);
      const sy = y + py * side * off + uy * (Math.random() * 12 - 6);
      const sparkLen = 4 + Math.random() * 8;
      const line = svgEl(doc, 'line', {
        x1: sx, y1: sy,
        x2: sx + ux * sparkLen,
        y2: sy + uy * sparkLen,
        stroke: HR_FIREWORK_COLORS[Math.floor(Math.random() * HR_FIREWORK_COLORS.length)],
        'stroke-width': 1.6 + Math.random() * 1.6,
        'stroke-linecap': 'round',
      });
      line.style.filter = 'drop-shadow(0 0 3px rgba(255,255,255,.7))';
      group.appendChild(line);
      const out = 12 + Math.random() * 22;
      line.animate([
        { opacity: 0, transform: 'translate(0px, 0px)' },
        { opacity: 1, offset: 0.2 },
        { opacity: 0, transform: `translate(${px * side * out + ux * (Math.random() * 10 - 5)}px, ${py * side * out + uy * (Math.random() * 10 - 5)}px)` },
      ], {
        duration: 450 + Math.random() * 350,
        delay: Math.random() * 60,
        easing: 'ease-out',
        fill: 'forwards',
      });
    }
  }
  setTimeout(() => group.remove(), 1100);
}

/** 清空 HR 特效层（下一球/撤销/切换时调用） */
export function clearHomeRunFx(layer) {
  layer?.replaceChildren();
}
