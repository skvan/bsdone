// 编辑阵容球场卡槽几何与命中判定（纯数据 + 纯函数，可直接 node 单测）
// 坐标系与 LiveGame.js 的 field-svg 同族：本垒在下、外野在上；x 向右、y 向下
export const FIELD_VIEWBOX = { width: 400, height: 470 };

export const FIELD_SLOTS = [
  { code: 'C',  label: '捕手',     x: 200, y: 424 },
  { code: 'P',  label: '投手',     x: 200, y: 330 },
  { code: '1B', label: '一垒手',   x: 286, y: 330 },
  { code: '2B', label: '二垒手',   x: 246, y: 268 },
  { code: '3B', label: '三垒手',   x: 114, y: 330 },
  { code: 'SS', label: '游击',     x: 154, y: 268 },
  { code: 'LF', label: '左外野手', x: 84,  y: 150 },
  { code: 'CF', label: '中外野手', x: 200, y: 118 },
  { code: 'RF', label: '右外野手', x: 316, y: 150 },
];

/** DH 卡槽固定在本垒侧（打击位，不守备） */
export const DH_SLOT = { code: 'DH', label: '指定打击', x: 348, y: 424 };

export const ALL_SLOT_CODES = [...FIELD_SLOTS.map((s) => s.code), DH_SLOT.code];

const SLOT_MAP = new Map([...FIELD_SLOTS, DH_SLOT].map((s) => [s.code, s]));

/** @returns {{code:string,label:string,x:number,y:number}|null} */
export function slotDef(code) {
  return SLOT_MAP.get(String(code ?? '').toUpperCase()) ?? null;
}

/** 9 个守备位（不含 DH） */
export function isFieldCode(code) {
  const c = String(code ?? '').toUpperCase();
  return FIELD_SLOTS.some((s) => s.code === c);
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
