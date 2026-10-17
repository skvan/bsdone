// 微信 JSSDK 分享：非微信环境或任一环节失败一律静默跳过（设计 §六），绝不抛出
export const JSSDK_SRC = 'https://res.wx.qq.com/open/js/jweixin-1.6.0.js';

/** UA 是否为微信内置浏览器；不传 ua 时读 navigator.userAgent */
export function isWechatBrowser(ua) {
  const s = typeof ua === 'string' ? ua : (typeof navigator !== 'undefined' && navigator.userAgent) || '';
  return /MicroMessenger/i.test(s);
}

/** 当前页面 URL（去掉 # 片段）：微信签名要求与页面实际地址一致 */
function currentUrl() {
  return (typeof window !== 'undefined' && window.location ? window.location.href : '').split('#')[0];
}

let scriptPromise = null;

/** 动态加载 jweixin（单例 Promise）；已有 window.wx 直接完成 */
export function loadJssdkScript() {
  if (typeof window !== 'undefined' && window.wx) return Promise.resolve();
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise((resolve, reject) => {
    const el = document.createElement('script');
    el.src = JSSDK_SRC;
    el.onload = () => resolve();
    el.onerror = () => {
      // 失败后清空单例，允许下次重试
      scriptPromise = null;
      reject(new Error('jweixin load failed'));
    };
    document.head.appendChild(el);
  });
  return scriptPromise;
}

/** 调后端签名端点（动态 import，避免单测加载 request.js 重依赖链）；返回解包后的签名字段 */
export async function fetchJssdkConfig(url) {
  const { get } = await import('../api/request.js');
  // request.js 的 get 只接收完整 URL 且返回 Result 包装：这里拼 query 并解包 data
  const result = await get(`/api/wechat/jssdk-config?url=${encodeURIComponent(url)}`);
  return result?.data ?? null;
}

const defaultWarn = (e) => {
  if (typeof console !== 'undefined' && console.warn) console.warn('[wechatJssdk]', e);
};

/** 注册自定义分享数据（朋友圈/会话；缺失的 desc/imgUrl 以空串兜底） */
function applyShareData(wx, meta) {
  const data = { title: meta.title, desc: meta.desc || '', link: meta.link, imgUrl: meta.imgUrl || '' };
  if (typeof wx.updateAppMessageShareData === 'function') wx.updateAppMessageShareData(data);
  if (typeof wx.updateTimelineShareData === 'function') wx.updateTimelineShareData(data);
}

// 已成功 wx.config 的页面 URL 集合（已配置页面的分享数据更新无需重复签名）
const configuredUrls = new Set();

/**
 * 主入口：微信内 wx.config + 注册自定义分享。meta={title,desc,link,imgUrl}；deps 可注入便于单测。
 */
export async function initWechatShare(meta, deps = {}) {
  const d = {
    isWechat: () => isWechatBrowser(),
    loadScript: loadJssdkScript,
    fetchConfig: fetchJssdkConfig,
    getWx: () => (typeof window !== 'undefined' ? window.wx : undefined),
    warn: defaultWarn,
    ...deps
  };
  try {
    if (!d.isWechat()) return;
    if (!meta || !meta.title || !meta.link) return;
    await d.loadScript();
    const wx = d.getWx();
    if (!wx || typeof wx.config !== 'function') return;
    const url = currentUrl();
    const conf = await d.fetchConfig(url);
    if (!conf || !conf.signature) return;
    await new Promise((resolve, reject) => {
      wx.config({
        debug: false,
        appId: conf.appId,
        timestamp: conf.timestamp,
        nonceStr: conf.nonceStr,
        signature: conf.signature,
        jsApiList: ['updateAppMessageShareData', 'updateTimelineShareData']
      });
      wx.ready(() => resolve());
      wx.error((err) => reject(err || new Error('wx.config failed')));
    });
    configuredUrls.add(url);
    applyShareData(wx, meta);
  } catch (e) {
    d.warn(e);
  }
}

/** 已配置页面上更新分享数据（标题随数据到达变化时用；未配置则 no-op） */
export function updateShareData(meta, deps = {}) {
  const d = {
    getWx: () => (typeof window !== 'undefined' ? window.wx : undefined),
    warn: defaultWarn,
    ...deps
  };
  try {
    if (!meta || !meta.title || !meta.link) return;
    if (!configuredUrls.has(currentUrl())) return;
    const wx = d.getWx();
    if (!wx) return;
    applyShareData(wx, meta);
  } catch (e) {
    d.warn(e);
  }
}
