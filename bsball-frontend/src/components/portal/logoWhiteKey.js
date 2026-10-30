// 球队队徽上传件多为白底 JPG/PNG：卡面展示前把接近纯白的像素抠成透明
// 仅用于卡面渲染，不改动上传文件本身；结果按完整 URL 缓存，避免重复处理
const cache = new Map();
const SOLID_WHITE = 232;
const EDGE_WHITE = 208;

function keyWhitePixels(pixels) {
  const data = pixels.data;
  for (let i = 0; i < data.length; i += 4) {
    const min = Math.min(data[i], data[i + 1], data[i + 2]);
    if (min >= SOLID_WHITE) {
      data[i + 3] = 0;
    } else if (min > EDGE_WHITE) {
      data[i + 3] = Math.round(data[i + 3] * (SOLID_WHITE - min) / (SOLID_WHITE - EDGE_WHITE));
    }
  }
  return pixels;
}

export function whiteKeyedLogo(url) {
  if (!url) {
    return Promise.resolve('');
  }
  const src = new URL(url, window.location.origin).href;
  const cached = cache.get(src);
  if (cached) {
    return cached;
  }
  const task = new Promise((resolve) => {
    const image = new Image();
    image.decoding = 'async';
    image.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const context = canvas.getContext('2d');
        context.drawImage(image, 0, 0);
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
        context.putImageData(keyWhitePixels(pixels), 0, 0);
        resolve(canvas.toDataURL('image/png'));
      } catch {
        resolve(src);
      }
    };
    image.onerror = () => resolve(src);
    image.src = src;
  });
  cache.set(src, task);
  return task;
}
