// 替补席拖拽换人·纯函数层：替补列表构建、空席文案、落点解析、合法性矩阵（不触碰 DOM/Vue）
// 供 LiveGame 补丁与单测共用；本文件须能被 node --test 直接加载（相对导入需带 .js 扩展名）
import { sortByJersey } from './sortByJersey.js';
const BASEBALL_BENCH_EMPTY_TEXT = '板凳席无可用球员（已换下球员不可再上场）';
const SOFTBALL_BENCH_EMPTY_TEXT = '板凳席无可用球员';

/** 替补席球员：不可再上名单内且未开放再上场时标 disabled（灰显不可操作）。
 *  垒球按赛事 softballReentry 开关（默认关）决定是否允许再上；棒球恒不允许。 */
export function buildLiveBenchItems(bench, unavailableIds, gameMode, reentryAllowed = gameMode === 'SOFTBALL') {
  const list = Array.isArray(bench) ? bench : [];
  const blocked = new Set((Array.isArray(unavailableIds) ? unavailableIds : []).map((id) => String(id)));
  const toItem = (p) => ({
    id: p.id,
    name: p.name,
    number: p.number,
    disabled: !reentryAllowed && blocked.has(String(p.id)),
  });
  return sortByJersey(list.map(toItem));
}

export function liveBenchEmptyText(gameMode, reentryAllowed = gameMode === 'SOFTBALL') {
  return reentryAllowed ? SOFTBALL_BENCH_EMPTY_TEXT : BASEBALL_BENCH_EMPTY_TEXT;
}

const withinTolerance = (x, y, r, tolerance) => {
  if (!r) return false;
  const cx = r.left + r.width / 2;
  const cy = r.top + r.height / 2;
  return Math.abs(x - cx) <= (r.width / 2) * tolerance && Math.abs(y - cy) <= (r.height / 2) * tolerance;
};

/** 落点命中（有序多命中）：守备员 → 垒位 → 打者 → 替补席 → 打线栏。
 *  同一点可能同时命中多个目标（如本垒处捕手标签与打者重叠）；
 *  调用方按来源的可落种类过滤后取第一个命中项。 */
export function resolveLiveSubDropHits(x, y, layout, tolerance = 1.2) {
  const hits = [];
  const { fielderRects = [], runnerRects = [], batterRect = null, benchRects = [], lineupRects = [] } = layout ?? {};
  for (const item of fielderRects) {
    if (withinTolerance(x, y, item?.rect, tolerance)) hits.push({ kind: 'fielder', code: item.code });
  }
  for (const item of runnerRects) {
    if (withinTolerance(x, y, item?.rect, tolerance)) hits.push({ kind: 'base', base: item.base });
  }
  if (withinTolerance(x, y, batterRect, tolerance)) hits.push({ kind: 'batter' });
  for (const item of benchRects) {
    if (withinTolerance(x, y, item?.rect, tolerance)) hits.push({ kind: 'bench', side: item.side, playerId: item.playerId });
  }
  for (const item of lineupRects) {
    if (withinTolerance(x, y, item?.rect, tolerance)) hits.push({ kind: 'lineup', playerId: item.playerId });
  }
  return hits;
}

/** 合法性矩阵：来源 → 目标 → 换人动作；非法组合返回 null。
 *  baseOccupied：垒上有人=代跑、空垒=设置跑者；samePlayer：守备员拖回自身无效。
 *  进攻替补落到打线栏与打者同义（代打指定棒次）。 */
export function liveSubDropAction(sourceKind, hit, opts = {}) {
  const targetKind = hit?.kind;
  if (sourceKind === 'offenseBench') {
    if (targetKind === 'batter' || targetKind === 'lineup') return 'pinchHit';
    if (targetKind === 'base') return opts.baseOccupied ? 'pinchRun' : 'setRunner';
    return null;
  }
  if (sourceKind === 'defenseBench') {
    return targetKind === 'fielder' ? 'fieldReplace' : null;
  }
  if (sourceKind === 'fielder') {
    if (targetKind === 'bench') return 'benchSwap';
    if (targetKind !== 'fielder' || opts.samePlayer) return null;
    return 'fieldSwap';
  }
  return null;
}

/** 某来源种类的可落目标种类（拖拽/待放置时用于高亮可落目标）。 */
export function liveSubTargetKinds(sourceKind) {
  if (sourceKind === 'offenseBench') return ['base', 'batter', 'lineup'];
  if (sourceKind === 'defenseBench') return ['fielder'];
  if (sourceKind === 'fielder') return ['fielder', 'bench'];
  // 普通 DH 卡可拖到守备位上场守备；大谷规则下由页面拦截
  if (sourceKind === 'dhCard') return ['fielder'];
  return [];
}
