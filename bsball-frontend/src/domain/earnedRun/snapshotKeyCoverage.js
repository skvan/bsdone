import { toEarnedRunPlay } from './livePlayEvent.js';

// 与后端 EarnedRunReconstructionService.toEntity 的 sourceEventId 同源：
// toEarnedRunPlay 的 playId = inning*1e6 + half*5e5 + sequence，后端原样存成字符串。
export function localPlayKey(event) {
  try {
    return String(toEarnedRunPlay(event).playId);
  } catch {
    return null;
  }
}

// 云端已定案的打席键里，本地快照没有覆盖的部分；按半局汇总供缺口提示使用。
export function findMissingPlayKeys(serverKeys, localEvents) {
  const events = Array.isArray(localEvents) ? localEvents : [];
  const localKeys = new Set(events.map(localPlayKey).filter(Boolean));
  const missing = (Array.isArray(serverKeys) ? serverKeys : [])
    .filter((key) => key && key.sourceEventId != null && String(key.sourceEventId).trim() !== '')
    .filter((key) => !localKeys.has(String(key.sourceEventId)));

  const byHalf = [];
  const index = new Map();
  for (const key of missing) {
    const id = `${key.inning}:${key.half}`;
    let item = index.get(id);
    if (!item) {
      item = { inning: key.inning, half: key.half, count: 0 };
      index.set(id, item);
      byHalf.push(item);
    }
    item.count += 1;
  }
  return { missing, byHalf };
}
