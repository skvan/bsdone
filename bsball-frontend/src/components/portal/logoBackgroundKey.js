// 球队队徽上传件常带纯色底（白、红等）：卡面渲染前只移除与边缘连通的纯色底，
// 再按 alpha 墨迹裁掉透明留边，让队徽直接落在深蓝卡面上；结果按绝对 URL 缓存，不改动上传文件。
const cache = new Map();
const HARD_DISTANCE = 38;
const SOFT_DISTANCE = 96;
const BORDER_COVERAGE = 0.55;

function colorDistance(data, index, r, g, b) {
  const dr = data[index] - r;
  const dg = data[index + 1] - g;
  const db = data[index + 2] - b;
  return Math.sqrt(dr * dr + dg * dg + db * db);
}

/** 沿四条边按密度上限采样，返回像素数据下标列表。 */
function borderSampleIndices(width, height) {
  const samples = [];
  const stepX = Math.max(1, Math.floor(width / 220));
  const stepY = Math.max(1, Math.floor(height / 220));
  for (let x = 0; x < width; x += stepX) {
    samples.push(x * 4);
    samples.push(((height - 1) * width + x) * 4);
  }
  for (let y = 0; y < height; y += stepY) {
    samples.push(y * width * 4);
    samples.push((y * width + width - 1) * 4);
  }
  return samples;
}

/** 边框采样（忽略透明像素）：返回主色、不透明边框覆盖率与透明边框占比。 */
export function sampleBorder(data, width, height) {
  const samples = borderSampleIndices(width, height);
  let transparent = 0;
  const buckets = new Map();
  for (const index of samples) {
    if (data[index + 3] < 128) {
      transparent += 1;
      continue;
    }
    const key = `${data[index] >> 4},${data[index + 1] >> 4},${data[index + 2] >> 4}`;
    const bucket = buckets.get(key) || { count: 0, r: 0, g: 0, b: 0 };
    bucket.count += 1;
    bucket.r += data[index];
    bucket.g += data[index + 1];
    bucket.b += data[index + 2];
    buckets.set(key, bucket);
  }
  const opaque = samples.length - transparent;
  let dominant = null;
  for (const bucket of buckets.values()) {
    if (!dominant || bucket.count > dominant.count) dominant = bucket;
  }
  // dominant 非空时必然有至少一个不透明采样，故 opaque ≥ 1，除法安全。
  return {
    dominant: dominant
      ? { r: dominant.r / dominant.count, g: dominant.g / dominant.count, b: dominant.b / dominant.count }
      : null,
    coverage: dominant ? dominant.count / opaque : 0,
    transparentRatio: transparent / samples.length
  };
}

/** 纯底色判定：边框不透明且主色占比过半；透明底（如已抠图队徽）返回 null。 */
export function solidBackground(data, width, height) {
  const border = sampleBorder(data, width, height);
  if (!border.dominant || border.transparentRatio >= 0.9 || border.coverage < BORDER_COVERAGE) {
    return null;
  }
  return {
    r: Math.round(border.dominant.r),
    g: Math.round(border.dominant.g),
    b: Math.round(border.dominant.b),
    coverage: border.coverage,
    transparentRatio: border.transparentRatio
  };
}

/** 抠底后的软边/水印残留清理：亮且半透的像素直接清掉，稍暗的减淡。 */
function cleanKeyedFringe(pixels) {
  const data = pixels.data;
  for (let index = 0; index < data.length; index += 4) {
    const alpha = data[index + 3];
    if (!alpha || alpha >= 210) continue;
    const luminance = 0.299 * data[index] + 0.587 * data[index + 1] + 0.114 * data[index + 2];
    if (luminance > 140) data[index + 3] = 0;
    else if (luminance > 110) data[index + 3] = Math.max(0, alpha - 60);
  }
}

/** 从四边起沿连通区域抠掉纯色底：硬距离内全透明，软边按距离衰减透明度。 */
export function keySolidBorder(data, width, height, bg) {
  const total = width * height;
  const visited = new Uint8Array(total);
  const queue = new Int32Array(total);
  let head = 0;
  let tail = 0;
  const seed = (x, y) => {
    const pixel = y * width + x;
    if (visited[pixel]) return;
    const index = pixel * 4;
    if (data[index + 3] === 0) return;
    if (colorDistance(data, index, bg.r, bg.g, bg.b) > SOFT_DISTANCE) return;
    visited[pixel] = 1;
    queue[tail++] = pixel;
  };
  for (let x = 0; x < width; x += 1) {
    seed(x, 0);
    seed(x, height - 1);
  }
  for (let y = 0; y < height; y += 1) {
    seed(0, y);
    seed(width - 1, y);
  }
  while (head < tail) {
    const pixel = queue[head++];
    const index = pixel * 4;
    const distance = colorDistance(data, index, bg.r, bg.g, bg.b);
    data[index + 3] = distance <= HARD_DISTANCE
      ? 0
      : Math.round(data[index + 3] * (1 - (distance - HARD_DISTANCE) / (SOFT_DISTANCE - HARD_DISTANCE)));
    const x = pixel % width;
    const y = (pixel - x) / width;
    for (let dy = -1; dy <= 1; dy += 1) {
      for (let dx = -1; dx <= 1; dx += 1) {
        if (!dx && !dy) continue;
        const nx = x + dx;
        const ny = y + dy;
        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
        seed(nx, ny);
      }
    }
  }
}

/** 不透明像素（alpha > 10）的包围盒；全透明图返回 null。 */
export function alphaBounds(data, width, height) {
  let minX = width;
  let minY = height;
  let maxX = -1;
  let maxY = -1;
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      if (data[(y * width + x) * 4 + 3] <= 10) continue;
      if (x < minX) minX = x;
      if (x > maxX) maxX = x;
      if (y < minY) minY = y;
      if (y > maxY) maxY = y;
    }
  }
  return maxX < 0 ? null : { minX, minY, maxX, maxY };
}

/** 按 alpha 墨迹裁掉透明留边，四周留 4% 内边距统一卡面占比。 */
function cropToInk(canvas, data) {
  const { width, height } = canvas;
  const box = alphaBounds(data, width, height);
  if (!box) return canvas;
  const pad = Math.max(2, Math.round(Math.max(box.maxX - box.minX, box.maxY - box.minY) * 0.04));
  const left = Math.max(0, box.minX - pad);
  const top = Math.max(0, box.minY - pad);
  const right = Math.min(width - 1, box.maxX + pad);
  const bottom = Math.min(height - 1, box.maxY + pad);
  const croppedWidth = right - left + 1;
  const croppedHeight = bottom - top + 1;
  if (croppedWidth === width && croppedHeight === height) return canvas;
  const cropped = document.createElement('canvas');
  cropped.width = croppedWidth;
  cropped.height = croppedHeight;
  cropped.getContext('2d').drawImage(canvas, left, top, croppedWidth, croppedHeight, 0, 0, croppedWidth, croppedHeight);
  return cropped;
}

function buildTransparent(image, src) {
  const width = image.naturalWidth;
  const height = image.naturalHeight;
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext('2d');
  context.drawImage(image, 0, 0);
  const pixels = context.getImageData(0, 0, width, height);
  const solid = solidBackground(pixels.data, width, height);
  if (solid) {
    keySolidBorder(pixels.data, width, height, solid);
    cleanKeyedFringe(pixels);
    context.putImageData(pixels, 0, 0);
  }
  const trimmed = cropToInk(canvas, pixels.data);
  if (trimmed === canvas && !solid) {
    return { url: src, keyed: false };
  }
  return { url: trimmed.toDataURL('image/png'), keyed: Boolean(solid) };
}

/** 卡面队徽入口：移除纯色底并按 alpha 裁掉透明留边；无需处理的队徽直接返回原 URL。 */
export function keyTeamLogo(url) {
  if (!url) {
    return Promise.resolve({ url: '', keyed: false });
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
        resolve(buildTransparent(image, src));
      } catch {
        resolve({ url: src, keyed: false });
      }
    };
    image.onerror = () => resolve({ url: src, keyed: false });
    image.src = src;
  });
  cache.set(src, task);
  return task;
}
