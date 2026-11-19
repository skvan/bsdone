// 个人主场整页长图：等待数据/图片就绪，按设备自适应 scale，html2canvas 导出。
// 纯函数（computeCaptureScale/formatImageSize）可在 node --test 中直接使用；
// html2canvas / waitForImages 在 capture 内动态加载，避免单测引入浏览器依赖。
import { inlineSvgPresentationStyles } from './captureStyleInline.js';

const MOBILE_QUERY = '(max-width: 980px)';
const MAX_SIDE = 32000;
// iOS Safari 画布单边上限约 16384，手机侧留余量
const MOBILE_MAX_SIDE = 16000;
const MAX_PIXELS_MOBILE = 16_000_000;
const MAX_PIXELS_DESKTOP = 100_000_000;
const MAX_SCALE = 2;
// 极限超长页面允许低于 1（调用方提示清晰度下降），优先保证完整且不超过浏览器画布上限
const MIN_SCALE = 0.05;

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function withTimeout(promise, ms, message) {
  let timer = 0;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Error(message)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

/** 按目标清晰度、画布面积与最大边长取最小 scale；手机/桌面给不同像素预算。 */
export function computeCaptureScale({
  width,
  height,
  mobile = false,
  targetScale = MAX_SCALE,
  maxPixels,
  maxSide = mobile ? MOBILE_MAX_SIDE : MAX_SIDE
}) {
  const w = Number(width);
  const h = Number(height);
  if (!Number.isFinite(w) || !Number.isFinite(h) || w <= 0 || h <= 0) return MIN_SCALE;
  const budget = maxPixels || (mobile ? MAX_PIXELS_MOBILE : MAX_PIXELS_DESKTOP);
  const areaScale = Math.sqrt(budget / (w * h));
  const sideScale = Math.min(maxSide / w, maxSide / h);
  const scale = Math.min(targetScale, areaScale, sideScale);
  return Math.max(MIN_SCALE, Math.round(scale * 100) / 100);
}

export function downloadBlob(blob, filename) {
  if (!blob) return;
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function formatImageSize(bytes) {
  const value = Number(bytes);
  if (!Number.isFinite(value) || value <= 0) return '—';
  if (value < 1024 * 1024) {
    const kb = Math.max(1, Math.round(value / 1024));
    return kb >= 1024 ? '1.0 MB' : `${kb} KB`;
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function hasVisibleLoading(root) {
  return [...root.querySelectorAll('.el-loading-mask')].some((mask) => {
    if (mask.offsetParent === null) return false;
    const style = window.getComputedStyle(mask);
    return style.display !== 'none' && Number(style.opacity || '1') > 0.05;
  });
}

function nextFrame() {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || typeof window.requestAnimationFrame !== 'function') {
      resolve();
      return;
    }
    window.requestAnimationFrame(() => window.requestAnimationFrame(() => resolve()));
  });
}

/** 等待业务数据、图片、字体、loading 与双 rAF 稳定。 */
export async function waitForClubhouseCaptureReady(root, waitUntil) {
  if (typeof waitUntil === 'function') await waitUntil();
  const { waitForImages } = await import('./gameDetailCollage.js');
  await waitForImages(root);
  if (typeof document !== 'undefined' && document.fonts?.ready) {
    // 字体请求悬挂时不阻塞截图，超时后按当前字体渲染
    await withTimeout(document.fonts.ready, 5000, 'font ready timeout').catch(() => {});
  }
  const deadline = Date.now() + 8000;
  while (hasVisibleLoading(root) && Date.now() < deadline) {
    await delay(80);
  }
  await nextFrame();
  await delay(120);
}

/**
 * 捕获 .clubhouse-page 整页。
 * @returns {{src:string,blob:Blob|null,width:number,height:number,scale:number,size:number,limited:boolean}}
 * src 在微信下为 JPEG data URL（blob=null），其他环境为 PNG Blob URL（blob 用于下载/释放）。
 */
export async function captureClubhouseLongImage(root, { wechat = false, waitUntil } = {}) {
  if (!root) throw new Error('missing capture root');
  await waitForClubhouseCaptureReady(root, waitUntil);
  const [{ default: html2canvas }, { canvasToBlob }] = await Promise.all([
    import('html2canvas'),
    import('./gameDetailCollage.js')
  ]);

  const rect = root.getBoundingClientRect();
  const width = Math.max(1, Math.ceil(rect.width || root.scrollWidth));
  const height = Math.max(1, Math.ceil(root.scrollHeight || rect.height));
  const mobile = typeof window !== 'undefined' && window.matchMedia(MOBILE_QUERY).matches;
  const scale = computeCaptureScale({ width, height, mobile, targetScale: mobile ? 2 : 1.75 });
  const backgroundColor = getComputedStyle(root).backgroundColor;
  const restoreStyles = inlineSvgPresentationStyles(root);

  try {
    const canvas = await withTimeout(html2canvas(root, {
      scale,
      useCORS: true,
      allowTaint: false,
      logging: false,
      backgroundColor: backgroundColor && backgroundColor !== 'rgba(0, 0, 0, 0)'
        ? backgroundColor
        : '#f2efe9',
      windowWidth: document.documentElement.clientWidth,
      windowHeight: document.documentElement.clientHeight,
      onclone: (doc) => {
        const clone = doc.querySelector('.clubhouse-page');
        if (!clone) return;
        clone.classList.add('is-capture-clone');
        // 卡片倾角是内联 CSS 变量，需在克隆中直接覆盖
        clone.querySelectorAll('.card-slot').forEach((slot) => {
          slot.style.setProperty('--hero-tilt', '0deg');
        });
      }
    }), 45000, 'long image capture timeout');
    const limited = scale < 1;
    if (wechat) {
      const src = canvas.toDataURL('image/jpeg', 0.92);
      return {
        src,
        blob: null,
        width: canvas.width,
        height: canvas.height,
        scale,
        size: Math.round(src.length * 0.75),
        limited
      };
    }
    const blob = await withTimeout(
      canvasToBlob(canvas, { mimeType: 'image/png' }),
      15000,
      'long image encode timeout'
    );
    return {
      src: URL.createObjectURL(blob),
      blob,
      width: canvas.width,
      height: canvas.height,
      scale,
      size: blob.size,
      limited
    };
  } finally {
    restoreStyles();
  }
}
