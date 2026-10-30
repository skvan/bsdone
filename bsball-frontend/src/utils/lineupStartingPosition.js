// 开始录入快照：先发守备位映射（阵容板 → LiveGame 快照）
import { normalizeFieldingPosition } from './gamePlayerStat.js';

/** 阵容行：先发守位=当前守位；DH/PH/EP 为空 */
export function buildStartingLineupRows(rows = []) {
  return (Array.isArray(rows) ? rows : []).map((row) => ({
    ...row,
    startingPosition: normalizeFieldingPosition(row?.position)
  }));
}

/** 板凳行：DH 制纯先发投手（不在打线）补 P，其余替补为空 */
export function buildStartingBenchRows(rows = [], pitcherId) {
  const pid = Number(pitcherId);
  return (Array.isArray(rows) ? rows : []).map((row) => ({
    ...row,
    startingPosition: Number.isFinite(pid) && pid > 0 && Number(row?.id) === pid ? 'P' : ''
  }));
}

/** 守备投手行：从阵容/板凳/名单中取先发投手，固定 position=P、startingPosition=P */
export function buildStartingPitcherRow(rows = [], bench = [], roster = [], pitcherId, teamId) {
  const pid = Number(pitcherId);
  if (!Number.isFinite(pid) || pid <= 0) return null;
  const player = [...(rows ?? []), ...(bench ?? []), ...(roster ?? [])]
    .find((row) => Number(row?.id) === pid);
  if (!player) return null;
  return {
    ...player,
    teamId: teamId ?? player.teamId,
    position: 'P',
    startingPosition: 'P'
  };
}
