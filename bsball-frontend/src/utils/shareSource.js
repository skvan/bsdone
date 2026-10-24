// 分享来源参数：埋点与分享链接共用（独立小文件，避免 request.js ↔ wechatJssdk.js 循环引用）
export const SHARE_SOURCES = ['share', 'poster'];
export const SHARE_SOURCE_PARAM = 'from';

/** 从 query string（如 window.location.search）提取白名单来源；非白名单/缺失 → null */
export function extractShareSource(search) {
  if (!search || typeof search !== 'string') return null;
  const value = new URLSearchParams(search).get(SHARE_SOURCE_PARAM);
  return value && SHARE_SOURCES.includes(value) ? value : null;
}
