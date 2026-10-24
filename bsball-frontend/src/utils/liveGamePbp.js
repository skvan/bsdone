// 实时录入全屏 Play-by-Play 日志流（#359）
// 复用实时观赛的日志清洗/表头解析，保证录入页与观赛页文案口径一致。
import {
  getLineTail,
  isMetaLine,
  normalizeLineList,
  parseHeaderLine
} from './liveGameFieldLayout.js';

export const PBP_LOG_LIMIT = 200;
export const PBP_MIN_HEIGHT = 120;
export const PBP_MAX_RATIO = 0.55;
export const PBP_DEFAULT_RATIO = 0.3;
export const PBP_STORAGE_KEY = 'bs-ball-live-pbp-height-ratio';

function normalizeLines(lines) {
  if (!Array.isArray(lines)) return [];
  return lines
    .filter((line) => typeof line === 'string' && line.trim())
    .map((line) => line.trim());
}

function isLogEntry(raw) {
  if (typeof raw === 'string') return raw.trim() !== '';
  return !!raw && typeof raw === 'object' && typeof raw.head === 'string';
}

/**
 * 把 eventLog 转换为 PBP 面板可直接渲染的条目，最新在上。
 * @param {Array<string|object>} logs 实时录入 eventLog
 * @param {number} limit 最多返回条数
 * @returns {Array<{time: string, inning: string, text: string, tail: string, lines: string[]}>}
 */
export function buildPbpEntries(logs, limit = PBP_LOG_LIMIT) {
  if (!Array.isArray(logs)) return [];
  const max = Number.isFinite(limit) && limit > 0 ? Math.floor(limit) : PBP_LOG_LIMIT;
  const entries = [];
  const seenKeys = new Map();
  for (const raw of normalizeLineList(logs.filter(isLogEntry))) {
    if (isMetaLine(raw)) continue;
    const head = typeof raw === 'string' ? raw : raw.head;
    const parsed = parseHeaderLine(head);
    const text = (parsed?.text ?? head).trim();
    if (!text) continue;
    const time = parsed?.time ?? '';
    const inning = parsed ? `${parsed.inning}${parsed.half === 'top' ? '上' : '下'}` : '';
    const tail = (getLineTail(raw) ?? '').trim();
    const lines = normalizeLines(raw?.lines);
    const baseKey = `${time}|${inning}|${text}|${tail}|${lines.join('|')}`;
    const occurrence = seenKeys.get(baseKey) ?? 0;
    seenKeys.set(baseKey, occurrence + 1);
    entries.push({
      key: occurrence ? `${baseKey}#${occurrence}` : baseKey,
      time,
      inning,
      text,
      tail,
      lines
    });
    if (entries.length >= max) break;
  }
  return entries;
}

/**
 * 把 PBP 面板高度限制在最小高度与最大屏占比之间。
 * @param {number} value 目标高度（px）
 * @param {number} viewportHeight 视口高度（px）
 * @param {number} minHeight 最小高度（px）
 * @param {number} maxRatio 最大屏占比
 * @returns {number} 取整后的高度（px）
 */
export function clampPbpHeight(
  value,
  viewportHeight,
  minHeight = PBP_MIN_HEIGHT,
  maxRatio = PBP_MAX_RATIO
) {
  const min = Math.max(0, Math.round(minHeight));
  const viewport = Number.isFinite(viewportHeight) ? viewportHeight : 0;
  const max = Math.max(min, Math.round(viewport * maxRatio));
  const next = Number.isFinite(value) ? Math.round(value) : min;
  return Math.min(max, Math.max(min, next));
}
