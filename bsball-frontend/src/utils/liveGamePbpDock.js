// 实时录入·球场与日志分栏（2026-11-12）：四向吸附、尺寸夹取、持久化。
// 纯函数，供 LiveGamePbpSplitter / LiveGamePlayByPlay / LiveGame 与 node --test 共用。

export const PBP_DOCK_SIDES = ['top', 'bottom', 'left', 'right'];
export const PBP_DOCK_KEY = 'bs-ball-live-pbp-dock';
export const PBP_VERTICAL_KEY = 'bs-ball-live-pbp-vertical';
export const PBP_HORIZONTAL_KEY = 'bs-ball-live-pbp-horizontal';
export const PBP_LEGACY_RATIO_KEY = 'bs-ball-live-pbp-height-ratio';

export const PBP_VERTICAL_MIN = 120;
export const PBP_HORIZONTAL_MIN = 260;
export const PBP_VERTICAL_MAX_RATIO = 0.55;
export const PBP_HORIZONTAL_MAX_RATIO = 0.42;
export const PBP_FIELD_MIN_VERTICAL = 250;
export const PBP_FIELD_MIN_HORIZONTAL = 430;
export const PBP_NARROW_MAX = 767;

/** 上下 / 左右两种吸附方向各自的尺寸约束。 */
const SIZE_RULES = {
  vertical: {
    min: PBP_VERTICAL_MIN,
    fieldMin: PBP_FIELD_MIN_VERTICAL,
    maxRatio: PBP_VERTICAL_MAX_RATIO,
  },
  horizontal: {
    min: PBP_HORIZONTAL_MIN,
    fieldMin: PBP_FIELD_MIN_HORIZONTAL,
    maxRatio: PBP_HORIZONTAL_MAX_RATIO,
  },
};

const safeNumber = (value, fallback = 0) => {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
};

/** 归一化吸附边；非法值回落到下方（当前默认）。 */
export function normalizePbpDock(value) {
  const dock = String(value ?? '').trim().toLowerCase();
  return PBP_DOCK_SIDES.includes(dock) ? dock : 'bottom';
}

export function isHorizontalDock(dock) {
  const normalized = normalizePbpDock(dock);
  return normalized === 'left' || normalized === 'right';
}

/** 手机宽度（<768）只保留上下；左/右吸附回落到下方，上和下保持。 */
export function effectivePbpDock(preferredDock, viewportWidth) {
  const dock = normalizePbpDock(preferredDock);
  if (isHorizontalDock(dock) && safeNumber(viewportWidth) <= PBP_NARROW_MAX) return 'bottom';
  return dock;
}

/**
 * 夹取日志尺寸：上下吸附限制高度，左右吸附限制宽度。
 * 同时保证球场最小尺寸；width/height 尚未测量时至少返回最小值。
 */
export function clampPbpSize(dock, value, { width = 0, height = 0 } = {}) {
  const horizontal = isHorizontalDock(dock);
  const { min, fieldMin, maxRatio } = horizontal ? SIZE_RULES.horizontal : SIZE_RULES.vertical;
  const total = safeNumber(horizontal ? width : height);
  const max = total > 0
    ? Math.max(min, Math.min(Math.round(total * maxRatio), total - fieldMin))
    : min;
  const next = safeNumber(value, min);
  return Math.round(Math.min(max, Math.max(min, next)));
}

/** 默认尺寸：左右 320；上下优先迁移旧高度比例，否则日志占 30%。 */
export function defaultPbpSize(dock, { width = 0, height = 0, legacyHeight = 0 } = {}) {
  if (isHorizontalDock(dock)) return clampPbpSize(dock, 320, { width, height });
  const legacy = safeNumber(legacyHeight);
  const preferred = legacy > 0 ? legacy : Math.round(safeNumber(height) * 0.3);
  return clampPbpSize(dock, preferred, { width, height });
}

function storageOf(storage) {
  if (storage) return storage;
  try {
    return typeof window !== 'undefined' ? window.localStorage : null;
  } catch {
    return null;
  }
}

function sizeKey(dock) {
  return isHorizontalDock(dock) ? PBP_HORIZONTAL_KEY : PBP_VERTICAL_KEY;
}

/** 受限存储（隐私模式等）读取失败时按“没有存过”处理。 */
function readRaw(storage, key) {
  try {
    return storageOf(storage)?.getItem?.(key) ?? null;
  } catch {
    return null;
  }
}

function readNumber(storage, key) {
  const value = Number(readRaw(storage, key));
  return Number.isFinite(value) && value > 0 ? value : 0;
}

function writeValue(storage, key, value) {
  try {
    storageOf(storage)?.setItem?.(key, String(value));
  } catch {
    // 隐私模式等场景无法写入，保持本次会话内调整即可
  }
}

export function readPbpDock(storage) {
  return normalizePbpDock(readRaw(storage, PBP_DOCK_KEY));
}

export function readPbpSize(dock, storage) {
  return readNumber(storage, sizeKey(dock));
}

/** 旧日志组件只存高度比例；迁移为像素高度，避免用户升级后尺寸跳变。 */
export function readLegacyPbpHeight(storage, viewportHeight) {
  const ratio = readNumber(storage, PBP_LEGACY_RATIO_KEY);
  if (ratio <= 0) return 0;
  return Math.round(safeNumber(viewportHeight) * ratio);
}

export function persistPbpDock(dock, storage) {
  writeValue(storage, PBP_DOCK_KEY, normalizePbpDock(dock));
}

export function persistPbpSize(dock, size, storage) {
  writeValue(storage, sizeKey(dock), Math.round(safeNumber(size)));
}
