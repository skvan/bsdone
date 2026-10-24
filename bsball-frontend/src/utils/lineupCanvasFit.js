// 阵容板画布尺寸计算（纯函数，可直接 node 单测）
// 列宽上限 = min(按屏内可用高度反推的宽度, 容器宽度扣除右侧两列上限与间距)，下限 300px
import { FIELD_VIEWBOX, SLOT_SIZE } from './lineupFieldPositions.js';

/** 球场列最小宽度（px），与 lineup-board.css 的 minmax 下限一致 */
export const FIELD_MIN_WIDTH = 300;
/** 球场底部与视口之间预留（px） */
export const FIELD_BOTTOM_MARGIN = 12;
/** 打线顺序 + 可用球员两列的上限和（240 + 320，px），与 lineup-board.css 一致 */
export const FIELD_SIDE_COLUMNS = 560;
/** 三列之间的间距合计（14 × 2，px） */
export const FIELD_GAP_TOTAL = 28;
/** 设置区压缩阈值：球场达到卡槽上限 72px 对应的场宽（72 / 15% = 480），画布已无需再让位 */
export const COMPACT_FORM_THRESHOLD = SLOT_SIZE.max / SLOT_SIZE.ratio;
/** 压缩/展开切换回差（px），防止窗口在阈值附近抖动 */
export const COMPACT_FORM_HYSTERESIS = 10;

function fits({ viewportHeight, boardTop, boardWidth }) {
  const heightFit = ((viewportHeight - boardTop - FIELD_BOTTOM_MARGIN) * FIELD_VIEWBOX.width) / FIELD_VIEWBOX.height;
  const widthFit = boardWidth - FIELD_SIDE_COLUMNS - FIELD_GAP_TOTAL;
  return { heightFit, widthFit };
}

/**
 * 球场列宽上限（px）。
 * @param {{viewportHeight:number, boardTop:number, boardWidth:number}} m 视口高、球场顶部视口坐标、网格容器宽
 */
export function computeFieldCap({ viewportHeight, boardTop, boardWidth }) {
  const { heightFit, widthFit } = fits({ viewportHeight, boardTop, boardWidth });
  return Math.max(FIELD_MIN_WIDTH, Math.floor(Math.min(heightFit, widthFit)));
}

/**
 * 设置区是否需要压缩为两行。
 * 仅当「未压缩表单」下球场小于阈值且高度是瓶颈（压缩确实能让画布变大）才压缩；
 * 宽度受限时压缩不会增大画布，维持现状。
 * @param {{viewportHeight:number, boardTopRoomy:number, boardWidth:number, currentlyCompact?:boolean}} m
 */
export function shouldCompactForm({ viewportHeight, boardTopRoomy, boardWidth, currentlyCompact = false }) {
  const { heightFit, widthFit } = fits({ viewportHeight, boardTop: boardTopRoomy, boardWidth });
  if (!(heightFit < widthFit)) return false;
  const cap = computeFieldCap({ viewportHeight, boardTop: boardTopRoomy, boardWidth });
  const threshold = currentlyCompact ? COMPACT_FORM_THRESHOLD + COMPACT_FORM_HYSTERESIS : COMPACT_FORM_THRESHOLD;
  return cap < threshold;
}
