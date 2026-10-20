// 实时录入 · 击球落点拖拽范围
//
// 场地 SVG 的视图为 -300 -150 1080 780，草坪扇形实际横跨 x -179.5 ~ 659.5、
// 上沿约 y -17.3。旧实现把拖拽坐标硬编码在 0 ~ 480 / 0 ~ 640，扇形左右两翼
// 正好落在矩形外，导致两个边角拖不到。
//
// 接杀可能发生在界外（含捕手后方的高飞球），因此这里不按两条边线裁剪成
// “界内”，而是用覆盖整个扇形两翼 + 捕手后方的矩形；拖出范围时贴边停住。
export const HIT_SPOT_DRAG_BOUNDS = Object.freeze({
  // 扇形左右边线极点（-179.5 / 659.5）向外取整，保证最外侧角落可拖。
  minX: -180,
  maxX: 660,
  // 上沿覆盖外野墙顶（约 -17.3）；下沿取场地视图下沿 630，保留捕手后方落点空间。
  minY: -20,
  maxY: 630,
});

const HOME_FALLBACK = { x: 240, y: 555 };

/**
 * 将击球落点拖拽坐标限制在球场可用范围内。
 * 非有限坐标（NaN/Infinity）回落到本垒附近，避免把 NaN 写入落点记录。
 * @param {number} x SVG 用户坐标
 * @param {number} y SVG 用户坐标
 * @returns {{x:number,y:number}}
 */
export function clampHitSpotToField(x, y) {
  const nx = Number.isFinite(x) ? x : HOME_FALLBACK.x;
  const ny = Number.isFinite(y) ? y : HOME_FALLBACK.y;
  return {
    x: Math.max(HIT_SPOT_DRAG_BOUNDS.minX, Math.min(HIT_SPOT_DRAG_BOUNDS.maxX, nx)),
    y: Math.max(HIT_SPOT_DRAG_BOUNDS.minY, Math.min(HIT_SPOT_DRAG_BOUNDS.maxY, ny)),
  };
}
