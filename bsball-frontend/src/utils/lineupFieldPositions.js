// 编辑阵容球场卡槽几何与命中判定（纯数据 + 纯函数，可直接 node 单测）
// 坐标系与 LiveGame.js 的 field-svg 同族：本垒在下、外野在上；x 向右、y 向下
export const FIELD_VIEWBOX = { width: 400, height: 470 };

// 内野与中轴卡槽按纵向分成四行（CF ↔ XF ↔ SS/2B ↔ 3B/P/1B ↔ C），行距 ≥76 坐标单位且横向接近处
// 足够分离 → 卡槽下限 56px 在球场 300px 时对应 74.7 单位，任意宽度下卡槽矩形不相交
// （外包 LF/CF/RF 行距小但横向相距 116 单位，单测 lineupFieldPositions.test.mjs 穷举校验）。
export const FIELD_SLOTS = [
  { code: 'C',  label: '捕手',     x: 200, y: 424 },
  { code: 'P',  label: '投手',     x: 200, y: 348 },
  { code: '1B', label: '一垒手',   x: 286, y: 348 },
  { code: '2B', label: '二垒手',   x: 246, y: 272 },
  { code: '3B', label: '三垒手',   x: 114, y: 348 },
  { code: 'SS', label: '游击',     x: 154, y: 272 },
  { code: 'LF', label: '左外野手', x: 84,  y: 150 },
  { code: 'CF', label: '中外野手', x: 200, y: 118 },
  { code: 'RF', label: '右外野手', x: 316, y: 150 },
];

/** DH 卡槽固定在本垒侧（打击位，不守备） */
export const DH_SLOT = { code: 'DH', label: '指定打击', x: 348, y: 424 };

/** XF 卡槽：垒球第 10 守备位（自由人），中列、二垒身后与外野之间（对齐旧件观赛页 field 布局的相对位置） */
export const XF_SLOT = { code: 'XF', label: '自由人', x: 200, y: 195 };

const ALL_SLOTS = [...FIELD_SLOTS, XF_SLOT, DH_SLOT];

/** 卡槽尺寸规则：球场宽度的 15%，限定 56~72px（与 lineup-board.css 的 clamp 一致）；
    guardRatio 18.5% 以「最紧行距 76 单位 − 余量」封顶，保证极小球场（窄屏移动端）也不重叠 */
export const SLOT_SIZE = { min: 56, ratio: 0.15, max: 72, guardRatio: 0.185 };

/** 该赛制下球场上的全部卡槽：垒球含 XF（自由人）；DH 恒在末位 */
export function slotsForMode(mode) {
  return mode === 'SOFTBALL' ? [...ALL_SLOTS] : [...FIELD_SLOTS, DH_SLOT];
}

export const ALL_SLOT_CODES = ALL_SLOTS.map((s) => s.code);

const SLOT_MAP = new Map(ALL_SLOTS.map((s) => [s.code, s]));

/** @returns {{code:string,label:string,x:number,y:number}|null} */
export function slotDef(code) {
  return SLOT_MAP.get(String(code ?? '').toUpperCase()) ?? null;
}

/** 守备位（9 常规位 + 垒球 XF；不含 DH） */
export function isFieldCode(code) {
  const c = String(code ?? '').toUpperCase();
  return c === XF_SLOT.code || FIELD_SLOTS.some((s) => s.code === c);
}

/**
 * 落点命中：先精确命中（点在矩形内）；否则取中心距离最近且落在 tolerance 倍外扩内者。
 * @param {number} x 视口坐标
 * @param {number} y 视口坐标
 * @param {Array<{code:string,left:number,top:number,width:number,height:number}>} rects
 * @param {number} tolerance 命中半径放宽倍数（触屏容差）
 */
export function hitTestSlot(x, y, rects, tolerance = 1.2) {
  if (!Array.isArray(rects) || rects.length === 0) return null;
  for (const r of rects) {
    if (x >= r.left && x <= r.left + r.width && y >= r.top && y <= r.top + r.height) return r.code;
  }
  let best = null;
  let bestDist = Infinity;
  for (const r of rects) {
    const cx = r.left + r.width / 2;
    const cy = r.top + r.height / 2;
    const dx = Math.abs(x - cx);
    const dy = Math.abs(y - cy);
    if (dx > (r.width / 2) * tolerance || dy > (r.height / 2) * tolerance) continue;
    const dist = Math.hypot(dx, dy);
    if (dist < bestDist) { bestDist = dist; best = r.code; }
  }
  return best;
}
