// 门户首页区块枚举与规范化——行为保真移植自编译产物 index chunk 的 Ht/ge/ve
export const HOME_SECTION_KEYS = ['carousel', 'nav', 'promo', 'events', 'teams', 'news'];

export function normalizeHomeSections(value) {
  const allowed = new Set(HOME_SECTION_KEYS);
  let list = [];
  if (Array.isArray(value)) list = value.map((item) => String(item).trim()).filter((item) => allowed.has(item));
  else if (typeof value === 'string' && value.trim()) list = value.split(',').map((item) => item.trim()).filter((item) => allowed.has(item));
  const seen = new Set();
  const result = [];
  for (const item of list) seen.has(item) || (seen.add(item), result.push(item));
  for (const item of HOME_SECTION_KEYS) seen.has(item) || result.push(item);
  return result;
}

export function dedupeHomeSections(value) {
  if (value == null) return [];
  const allowed = new Set(HOME_SECTION_KEYS);
  let list = [];
  if (Array.isArray(value)) list = value.map((item) => String(item).trim()).filter((item) => allowed.has(item));
  const seen = new Set();
  const result = [];
  for (const item of list) seen.has(item) || (seen.add(item), result.push(item));
  return result;
}
