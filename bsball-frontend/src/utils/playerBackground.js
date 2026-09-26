// 球员背景图设置工具 —— 行为移植自编译产物 playerBackground chunk（逐字）
export const BACKGROUND_DEVICES = ['desktop', 'mobile', 'tablet'];
const OBJECT_FIT_OPTIONS = ['fill', 'contain', 'cover', 'none', 'scale-down'];

export function normalizeObjectFit(value) {
  const text = typeof value === 'string' ? value.trim() : '';
  if (text) return OBJECT_FIT_OPTIONS.includes(text) ? text : undefined;
  return undefined;
}

function buildBackgroundSize(value) {
  const fit = value ?? 'cover';
  return `cover, cover, ${
    fit === 'fill' ? '100% 100%' : fit === 'contain' ? 'contain' : fit === 'none' ? 'auto' : fit === 'scale-down' ? 'contain' : 'cover'
  }`;
}

export const BACKGROUND_BREAKPOINTS = { mobileMax: 768, tabletMax: 1100 };

export function detectDeviceByWidth(width) {
  return width <= BACKGROUND_BREAKPOINTS.mobileMax ? 'mobile' : width <= BACKGROUND_BREAKPOINTS.tabletMax ? 'tablet' : 'desktop';
}

function identityJson(value) {
  return JSON.stringify(value || '');
}

// 背景样式解析：customBgResolved 优先，缺失时回退默认渐变 + fallbackBgResolved
export function resolveBackgroundStyle(entry) {
  const custom = (entry.customBgResolved || '').trim();
  const imageUrl = identityJson(custom || (entry.fallbackBgResolved || '').trim());
  if (!custom) {
    return {
      backgroundImage: `radial-gradient(circle at 18% 88%, rgba(0,0,0,0.54) 0%, rgba(0,0,0,0.30) 30%, rgba(0,0,0,0.08) 56%, rgba(0,0,0,0.00) 76%), linear-gradient(to top, rgba(0,0,0,0.40) 0%, rgba(0,0,0,0.14) 44%, rgba(0,0,0,0.00) 74%), url(${imageUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    };
  }
  const style = {
    backgroundColor: '#0f172a',
    backgroundRepeat: 'no-repeat',
    backgroundSize: buildBackgroundSize(normalizeObjectFit(entry.fit) ?? 'cover'),
    backgroundImage: `radial-gradient(circle at 18% 88%, rgba(0,0,0,0.58) 0%, rgba(0,0,0,0.34) 28%, rgba(0,0,0,0.12) 52%, rgba(0,0,0,0.00) 72%), linear-gradient(to top, rgba(0,0,0,0.46) 0%, rgba(0,0,0,0.18) 42%, rgba(0,0,0,0.00) 72%), url(${imageUrl})`
  };
  if (entry.mode === 'focus') {
    const x = Math.min(100, Math.max(0, entry.focusX));
    const y = Math.min(100, Math.max(0, entry.focusY));
    return { ...style, backgroundPosition: `center, center, ${x}% ${y}%` };
  }
  return { ...style, backgroundPosition: 'center, center, center' };
}

const DEFAULT_FOCUS_SETTING = { mode: 'focus', x: 50, y: 50 };

function clampPercent(value, fallback = 50) {
  const num = Number(value);
  return Number.isFinite(num) ? Math.min(100, Math.max(0, num)) : fallback;
}

// 单设备设置归一化（内部）
function normalizeDeviceSettings(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};
  const src = raw;
  const out = {};
  for (const device of BACKGROUND_DEVICES) {
    const item = src[device];
    if (!item) continue;
    const mode = item.mode === 'crop' ? 'crop' : 'focus';
    out[device] = { mode, x: clampPercent(item.x, 50), y: clampPercent(item.y, 50) };
    if (mode === 'crop') {
      if (item.scale != null && Number.isFinite(Number(item.scale))) out[device].scale = Number(item.scale);
      if (item.offsetX != null && Number.isFinite(Number(item.offsetX))) out[device].offsetX = Number(item.offsetX);
      if (item.offsetY != null && Number.isFinite(Number(item.offsetY))) out[device].offsetY = Number(item.offsetY);
    } else if (item.scale != null && Number.isFinite(Number(item.scale))) {
      out[device].scale = Number(item.scale);
    }
    const fit = normalizeObjectFit(item.fit);
    if (fit) out[device].fit = fit;
  }
  return out;
}

// 按图 URL 的设置归一化（内部）
function normalizePerImageSettings(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};
  const out = {};
  for (const [key, value] of Object.entries(raw)) {
    const url = key.trim();
    if (!url) continue;
    const settings = normalizeDeviceSettings(value);
    if (Object.keys(settings).length) out[url] = settings;
  }
  return out;
}

function isFlatDeviceSettings(raw) {
  return BACKGROUND_DEVICES.some((device) => device in raw) && !('devices' in raw) && !('perImage' in raw);
}

// 解析原始设置（字符串/对象）→ { devices, perImage }
export function parseBackgroundSettings(value) {
  if (value == null) return { devices: {}, perImage: {} };
  let parsed = value;
  if (typeof value === 'string') {
    try {
      parsed = JSON.parse(value);
    } catch {
      return { devices: {}, perImage: {} };
    }
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return { devices: {}, perImage: {} };
  const src = parsed;
  if ('devices' in src || 'perImage' in src) {
    return { devices: normalizeDeviceSettings(src.devices), perImage: normalizePerImageSettings(src.perImage) };
  }
  if (isFlatDeviceSettings(src)) return { devices: normalizeDeviceSettings(src), perImage: {} };
  return { devices: {}, perImage: {} };
}

// 压缩空值（perImage 为空则仅返回 devices）
export function compactBackgroundSettings(settings) {
  const devices = settings.devices && Object.keys(settings.devices).length ? settings.devices : {};
  const perImage = settings.perImage && Object.keys(settings.perImage).length ? settings.perImage : {};
  return Object.keys(perImage).length ? { devices, perImage } : devices;
}

export function cloneDeviceSettings(source) {
  const out = {};
  for (const device of BACKGROUND_DEVICES) {
    const item = source[device];
    if (item) out[device] = { ...item };
  }
  return out;
}

// 取指定设备设置（降级链：同设备 → desktop → tablet/mobile 相邻），并归一化
export function sanitizeDeviceSettings(settings, device) {
  const resolved = ((src, key) => {
    const item = src[key];
    if (item) return { ...item };
    if (key !== 'desktop' && src.desktop) return { ...src.desktop };
    if (key === 'tablet' && src.mobile) return { ...src.mobile };
    if (key === 'mobile' && src.tablet) return { ...src.tablet };
    return undefined;
  })(settings, device);
  if (resolved) {
    const mode = resolved.mode === 'crop' ? 'crop' : 'focus';
    const out = { mode, x: clampPercent(resolved.x, 50), y: clampPercent(resolved.y, 50) };
    if (mode === 'crop') {
      if (resolved.scale != null && Number.isFinite(Number(resolved.scale))) out.scale = Number(resolved.scale);
      if (resolved.offsetX != null && Number.isFinite(Number(resolved.offsetX))) out.offsetX = Number(resolved.offsetX);
      if (resolved.offsetY != null && Number.isFinite(Number(resolved.offsetY))) out.offsetY = Number(resolved.offsetY);
    } else if (resolved.scale != null && Number.isFinite(Number(resolved.scale))) {
      out.scale = Number(resolved.scale);
    }
    const fit = normalizeObjectFit(resolved.fit);
    if (fit) out.fit = fit;
    return out;
  }
  return { ...DEFAULT_FOCUS_SETTING };
}

// 背景图列表解析（bgImages 数组/字符串 + bgImage 置顶去重）
export function resolveBgImageList(entry) {
  if (!entry) return [];
  const rawList = entry.bgImages;
  const list = Array.isArray(rawList)
    ? rawList.map((item) => String(item ?? '').trim()).filter(Boolean)
    : typeof rawList === 'string'
      ? (() => {
          try {
            const parsed = JSON.parse(rawList);
            return Array.isArray(parsed) ? parsed.map((item) => String(item ?? '').trim()).filter(Boolean) : [];
          } catch {
            return [];
          }
        })()
      : [];
  const primary = (entry.bgImage ?? '').trim();
  if (primary) {
    const idx = list.indexOf(primary);
    if (idx > 0) {
      const next = [...list];
      next.splice(idx, 1);
      next.unshift(primary);
      return Array.from(new Set(next));
    }
    if (idx === -1) return [primary, ...list];
  }
  return Array.from(new Set(list));
}

// 按图选择设备设置：perImage[url] 非空优先，否则 devices
export function pickDeviceSettings(settings, imageUrl) {
  const url = imageUrl.trim();
  const perImage = url ? settings.perImage[url] : undefined;
  return perImage && Object.keys(perImage).length ? perImage : settings.devices;
}
