// 横幅项工具 —— 行为移植自编译产物入口 chunk（Dn / Nn / Hn）
// Dn: 过滤空横幅（全空时回退单空项）；Nn: 空列表补位；Hn: 横幅项唯一键（djb2）

export function filterBannerItems(list) {
  const filtered = list.filter((item) => (item.imageUrl ?? '').trim() || (item.linkUrl ?? '').trim());
  return filtered.length === 0 ? [{ imageUrl: '', linkUrl: '' }] : filtered;
}

export function ensureBannerList(list) {
  return list.length === 0 ? [{ imageUrl: '', linkUrl: '' }] : list;
}

export function bannerItemKey(item) {
  const text = `${(item.imageUrl ?? '').trim()}\0${(item.linkUrl ?? '').trim()}`;
  let hash = 5381;
  for (let i = 0; i < text.length; i++) hash = Math.imul(hash, 33) ^ text.charCodeAt(i);
  return hash >>> 0;
}
