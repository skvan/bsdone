// 比赛详情拼图导出 —— 行为移植自编译产物 gameDetailCollage chunk（逐字）
// 流程：隐藏 iframe 加载门户比赛详情页 → 等待稳定 → html2canvas 逐场截图 → canvas 纵向拼接 → 导出 Blob
import html2canvas from 'html2canvas';

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForImages(container, timeout = 12000) {
  const images = Array.from(container.querySelectorAll('img'));
  const waitOne = (img) =>
    new Promise((resolve) => {
      if (img.complete && img.naturalWidth > 0) {
        resolve();
        return;
      }
      const timer = window.setTimeout(() => resolve(), timeout);
      const done = () => {
        window.clearTimeout(timer);
        img.removeEventListener('load', onLoad);
        img.removeEventListener('error', onError);
        resolve();
      };
      function onLoad() {
        done();
      }
      function onError() {
        done();
      }
      img.addEventListener('load', onLoad);
      img.addEventListener('error', onError);
    });
  await Promise.all(images.map(waitOne));
  await Promise.allSettled(
    images.map((img) => (typeof img.decode === 'function' ? img.decode().catch(() => {}) : Promise.resolve()))
  );
}

function hasVisibleLoading(root) {
  const masks = root.querySelectorAll('.el-loading-mask');
  for (const mask of masks) {
    const el = mask;
    if (el.offsetParent === null) continue;
    const opacity = parseFloat(getComputedStyle(el).opacity || '1');
    if (getComputedStyle(el).display !== 'none' && opacity > 0.05) return true;
  }
  return false;
}

async function waitForStableFrame(win) {
  await delay(200);
  if (win) {
    await new Promise((resolve) => {
      win.requestAnimationFrame(() => {
        win.requestAnimationFrame(() => resolve());
      });
    });
    await delay(150);
  }
}

async function waitForDetailReady(iframe, timeout = 45000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const doc = iframe.contentDocument;
    const win = iframe.contentWindow;
    if (!doc) {
      await delay(80);
      continue;
    }
    const root = doc.querySelector('.portal-game-detail');
    if (!root) {
      await delay(80);
      continue;
    }
    if (!root.querySelector('.detail-card[data-capture-ready="1"]')) {
      await delay(100);
      continue;
    }
    if (hasVisibleLoading(root)) {
      await delay(100);
      continue;
    }
    if (!root.querySelector('.box-score-section .line-score-table')) {
      await delay(100);
      continue;
    }
    await waitForStableFrame(win);
    if (!hasVisibleLoading(root) && root.querySelector('.detail-card[data-capture-ready="1"]')) return;
    await delay(80);
  }
  throw new Error('等待比赛详情加载超时，请稍后重试');
}

function loadIframe(iframe, url) {
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => reject(new Error('页面加载超时')), 35000);
    const done = () => {
      window.clearTimeout(timer);
      iframe.onload = null;
      iframe.onerror = null;
    };
    iframe.onload = () => {
      done();
      resolve();
    };
    iframe.onerror = () => {
      done();
      reject(new Error('iframe 加载失败'));
    };
    iframe.src = url;
  });
}

async function captureIframeDetail(iframe, url, scale) {
  await loadIframe(iframe, url);
  await waitForDetailReady(iframe);
  const doc = iframe.contentDocument;
  const win = iframe.contentWindow;
  if (!doc || !win) throw new Error('无法读取页面内容');
  const root = doc.querySelector('.portal-game-detail');
  if (!root) throw new Error('未找到 .portal-game-detail');
  try {
    win.scrollTo(0, 0);
    root.scrollTop = 0;
  } catch {
    /* 忽略滚动异常 */
  }
  await waitForImages(root);
  try {
    await doc.fonts?.ready;
  } catch {
    /* 忽略字体异常 */
  }
  await delay(120);
  return html2canvas(root, {
    scale,
    useCORS: true,
    allowTaint: false,
    logging: false,
    imageTimeout: 20000,
    backgroundColor: '#ffffff',
    removeContainer: true,
    ignoreElements: (element) => {
      const el = element;
      return !!(el.closest?.('.floating-settings-wrap') || el.closest?.('.field-settings-guide'));
    }
  });
}

function stackCanvases(canvases, gap, scale) {
  const gapPx = Math.round(gap * scale);
  const width = Math.max(...canvases.map((item) => item.width));
  let height = 0;
  for (let i = 0; i < canvases.length; i++) {
    if (i > 0) height += gapPx;
    height += canvases[i].height;
  }
  const target = document.createElement('canvas');
  target.width = width;
  target.height = height;
  const ctx = target.getContext('2d');
  if (!ctx) throw new Error('无法创建画布');
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, width, height);
  let offset = 0;
  for (let i = 0; i < canvases.length; i++) {
    if (i > 0) offset += gapPx;
    const item = canvases[i];
    ctx.drawImage(item, 0, offset);
    offset += item.height;
  }
  return target;
}

export async function captureGamesCollage(urls, options) {
  if (!urls.length) throw new Error('未选择比赛');
  const gap = options?.gap ?? 16;
  const scale = options?.scale ?? 2;
  const iframeWidth = Math.max(480, Math.min(4096, Math.round(options?.iframeWidthPx ?? 1680)));
  const iframe = document.createElement('iframe');
  iframe.setAttribute('aria-hidden', 'true');
  iframe.title = 'game-detail-capture';
  iframe.style.cssText = `position:fixed;left:-32768px;top:0;width:${iframeWidth}px;min-height:2400px;border:0;opacity:1;pointer-events:none;`;
  document.body.appendChild(iframe);
  try {
    const canvases = [];
    const count = urls.length;
    for (let i = 0; i < count; i++) {
      options?.onProgress?.(i + 1, count);
      const shot = await captureIframeDetail(iframe, urls[i], scale);
      canvases.push(shot);
    }
    return stackCanvases(canvases, gap, scale);
  } finally {
    iframe.remove();
  }
}

export function canvasToBlob(canvas, options) {
  return new Promise((resolve, reject) => {
    if (options.mimeType === 'image/png') {
      canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('导出 PNG 失败'))), 'image/png');
      return;
    }
    const quality = Math.min(1, Math.max(0.1, options.quality));
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('导出 JPEG 失败'))), 'image/jpeg', quality);
  });
}

export function downloadBlob(blob, filename) {
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
