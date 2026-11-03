import { resolveAssetUrl } from '../api/request';
import { loadPrintExport, loadJsPdf } from './printExport';
import {
  buildOrderBookPdfPagesHtml,
  paginateOrderBookCards,
  sanitizePdfFileName,
  scaleImageToFit,
  fitImageContainSquare,
  ORDER_BOOK_PDF_GEOMETRY
} from './orderBookPdfLayout';

const IMAGE_TIMEOUT_MS = 4000;
const IMAGE_CONCURRENCY = 6;

function imageSource(url) {
  const value = String(url || '').trim();
  if (!value) return '';
  if (value.startsWith('data:image/')) return value;
  const resolved = resolveAssetUrl(value);
  if (!/^(https?:|data:image\/|\/)/i.test(resolved)) return '';
  return resolved;
}

function loadImageData(url, {
  maxSize = 256,
  mime = 'image/jpeg',
  quality = 0.85,
  cropSquare = false,
  containSquare = false
} = {}) {
  return new Promise((resolve) => {
    const src = imageSource(url);
    if (!src) return resolve('');
    const image = new Image();
    let done = false;
    const finish = (value) => {
      if (done) return;
      done = true;
      clearTimeout(timer);
      image.onload = null;
      image.onerror = null;
      resolve(value);
    };
    const timer = setTimeout(() => finish(''), IMAGE_TIMEOUT_MS);
    const rootRelative = src.startsWith('/') && !src.startsWith('//');
    const sameOrigin = rootRelative || src.startsWith(window.location.origin);
    if (!sameOrigin) image.crossOrigin = 'anonymous';
    image.onload = () => {
      try {
        const width = image.naturalWidth;
        const height = image.naturalHeight;
        if (!width || !height) return finish('');
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        if (!ctx) return finish('');
        if (cropSquare) {
          const sourceSize = Math.min(width, height);
          const side = Math.min(maxSize, sourceSize);
          canvas.width = side;
          canvas.height = side;
          if (mime === 'image/jpeg') {
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, side, side);
          }
          ctx.drawImage(
            image,
            Math.round((width - sourceSize) / 2),
            Math.round((height - sourceSize) / 2),
            sourceSize,
            sourceSize,
            0,
            0,
            side,
            side
          );
        } else if (containSquare) {
          const fit = fitImageContainSquare(width, height, maxSize);
          canvas.width = fit.side;
          canvas.height = fit.side;
          ctx.drawImage(image, fit.offsetX, fit.offsetY, fit.drawWidth, fit.drawHeight);
        } else {
          const size = scaleImageToFit(width, height, maxSize);
          canvas.width = size.width;
          canvas.height = size.height;
          if (mime === 'image/jpeg') {
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, size.width, size.height);
          }
          ctx.drawImage(image, 0, 0, size.width, size.height);
        }
        finish(canvas.toDataURL(mime, quality));
      } catch {
        finish('');
      }
    };
    image.onerror = () => finish('');
    image.src = src;
  });
}

async function mapLimit(items, limit, worker) {
  const list = [...items];
  let cursor = 0;
  const runners = Array.from({ length: Math.min(limit, list.length) }, async () => {
    while (cursor < list.length) {
      const index = cursor;
      cursor += 1;
      await worker(list[index]);
    }
  });
  await Promise.all(runners);
}

async function preloadImages(sections) {
  const jobs = new Map();
  for (const section of sections) {
    if (section?.teamLogo) jobs.set(section.teamLogo, 'logo');
    for (const row of section?.rows || []) {
      if (row?.avatar && !jobs.has(row.avatar)) jobs.set(row.avatar, 'avatar');
    }
  }
  const images = new Map();
  await mapLimit(jobs.entries(), IMAGE_CONCURRENCY, async ([url, kind]) => {
    const options = kind === 'logo'
      ? { maxSize: 128, mime: 'image/png', containSquare: true }
      : { maxSize: 256, mime: 'image/jpeg', quality: 0.85, cropSquare: true };
    const dataUrl = await loadImageData(url, options);
    if (dataUrl) images.set(url, dataUrl);
  });
  return images;
}

function waitForImages(root) {
  const pending = [...root.querySelectorAll('img')]
    .filter((img) => !img.complete)
    .map((img) => new Promise((resolve) => {
      let done = false;
      let timer;
      const finish = () => {
        if (done) return;
        done = true;
        clearTimeout(timer);
        img.removeEventListener('load', finish);
        img.removeEventListener('error', finish);
        resolve();
      };
      timer = setTimeout(finish, IMAGE_TIMEOUT_MS);
      img.addEventListener('load', finish, { once: true });
      img.addEventListener('error', finish, { once: true });
    }));
  return Promise.all(pending);
}

const nextFrame = () => new Promise((resolve) => requestAnimationFrame(() => resolve()));

export async function downloadEventOrderBookPdf(options) {
  const {
    title,
    eventName,
    eventSeason,
    generatedAtLine,
    summaryText,
    fileName,
    labels,
    sections
  } = options || {};
  const pages = paginateOrderBookCards(sections);
  if (!pages.length) throw new Error('empty order book');

  const images = await preloadImages(sections);
  const root = document.createElement('div');
  root.style.cssText = `position:fixed;left:-12000px;top:0;width:${ORDER_BOOK_PDF_GEOMETRY.pageWidth}px;background:#fff;z-index:-1;pointer-events:none`;
  root.innerHTML = buildOrderBookPdfPagesHtml(pages, {
    title,
    eventName,
    eventSeason,
    generatedAtLine,
    summaryText,
    labels,
    images
  });
  document.body.appendChild(root);

  try {
    await document.fonts?.ready?.catch?.(() => {});
    await waitForImages(root);
    await nextFrame();
    await nextFrame();

    const pageEls = [...root.querySelectorAll('.obp-page')];
    const html2canvas = loadPrintExport();
    const JsPdf = loadJsPdf();
    const pdf = new JsPdf({ orientation: 'portrait', unit: 'mm', format: 'a4', compress: true });
    pdf.setProperties({ title: String(title || eventName || 'order-book'), creator: 'BS Ball' });

    for (let i = 0; i < pageEls.length; i += 1) {
      const canvas = await html2canvas(pageEls[i], {
        scale: 2,
        useCORS: true,
        allowTaint: false,
        backgroundColor: '#ffffff',
        logging: false
      });
      if (i > 0) pdf.addPage('a4', 'portrait');
      pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, 0, 210, 297, undefined, 'FAST');
      canvas.width = 0;
      canvas.height = 0;
    }

    const safeName = sanitizePdfFileName(fileName);
    pdf.save(`${safeName}.pdf`);
  } finally {
    root.remove();
  }
}
