// 球队队徽上传件常带纯色底（白、红、蓝等）：卡面渲染前把纯色底替换为
// 「队徽自身底色的手刷笔触块」，整图墨迹居中叠放；透明底/非纯底队徽回退到近白抠底。
// 仅用于卡面渲染，不改动上传文件；结果按绝对 URL 缓存。
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

function keyNearWhite(data) {
  for (let index = 0; index < data.length; index += 4) {
    const min = Math.min(data[index], data[index + 1], data[index + 2]);
    if (min >= 232) {
      data[index + 3] = 0;
    } else if (min > 208) {
      data[index + 3] = Math.round(data[index + 3] * (232 - min) / (232 - 208));
    }
  }
}

function keySolidBorder(data, width, height, bg) {
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

/** 非纯底队徽的回退处理：仅抠近白像素（与线上既有口径一致）。 */
function nearWhiteKeyed(image) {
  const canvas = document.createElement('canvas');
  canvas.width = image.naturalWidth;
  canvas.height = image.naturalHeight;
  const context = canvas.getContext('2d');
  context.drawImage(image, 0, 0);
  const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
  keyNearWhite(pixels.data);
  context.putImageData(pixels, 0, 0);
  return canvas.toDataURL('image/png');
}

const PATCH_SIZE = 420;
const PATCH_ALPHA = 0.8;

function seedFrom(url) {
  let hash = 7;
  for (let i = 0; i < url.length; i += 1) hash = (hash * 31 + url.charCodeAt(i)) % 233280;
  return hash || 17;
}

function randFactory(seed) {
  let state = seed;
  return () => {
    state = (state * 9301 + 49297) % 233280;
    return state / 233280;
  };
}

/** 单道手刷笔触路径：起笔重收笔轻（宽度 taper），边沿微起伏不毛糙。 */
function strokePath(ctx, rand, len, hs) {
  const h0 = hs * 0.56;
  const h1 = hs * 0.4;
  const nTop = 7;
  const pts = [];
  for (let i = 0; i <= nTop; i += 1) {
    const t = i / nTop;
    pts.push([-len / 2 + len * t, -(h0 + (h1 - h0) * t) + (rand() - 0.5) * hs * 0.05]);
  }
  pts.push([len / 2 + hs * 0.22, -h1 * 0.5]);
  pts.push([len / 2 + hs * 0.16, h1 * 0.5]);
  for (let i = nTop; i >= 0; i -= 1) {
    const t = i / nTop;
    pts.push([-len / 2 + len * t, h0 + (h1 - h0) * t + (rand() - 0.5) * hs * 0.05]);
  }
  pts.push([-len / 2 - hs * 0.14, h0 * 0.4]);
  ctx.beginPath();
  pts.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
  ctx.closePath();
}

/** 笔触内的压力条纹：顺笔触方向的深浅细线（不挖穿，保持实心）。 */
function strokeStreaks(ctx, rand, len, hs, rgb) {
  ctx.globalCompositeOperation = 'source-atop';
  for (let i = 0; i < 6; i += 1) {
    const oy = (rand() - 0.5) * hs * 0.8;
    ctx.strokeStyle = `rgba(${Math.max(0, rgb[0] - 46)},${Math.max(0, rgb[1] - 46)},${Math.max(0, rgb[2] - 42)},${0.04 + rand() * 0.07})`;
    ctx.lineWidth = 0.8 + rand() * 2.2;
    ctx.beginPath();
    ctx.moveTo(-len / 2 + rand() * len * 0.15, oy);
    ctx.lineTo(len / 2 - rand() * len * 0.15, oy + (rand() - 0.5) * hs * 0.1);
    ctx.stroke();
  }
  for (let i = 0; i < 3; i += 1) {
    const oy = (rand() - 0.5) * hs * 0.8;
    ctx.strokeStyle = `rgba(255,255,255,${0.05 + rand() * 0.08})`;
    ctx.lineWidth = 0.6 + rand() * 1.4;
    ctx.beginPath();
    ctx.moveTo(-len / 2 + rand() * len * 0.2, oy);
    ctx.lineTo(len / 2 - rand() * len * 0.2, oy + (rand() - 0.5) * hs * 0.1);
    ctx.stroke();
  }
}

/** 一道完整手刷笔触：起笔重收笔轻的透明度渐变 + 压力条纹。 */
function markerStroke(ctx, rand, cx, cy, len, hs, ang, rgb) {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(ang);
  strokePath(ctx, rand, len, hs);
  const g = ctx.createLinearGradient(-len / 2, 0, len / 2, 0);
  g.addColorStop(0, `rgba(${rgb[0]},${rgb[1]},${rgb[2]},${PATCH_ALPHA})`);
  g.addColorStop(0.62, `rgba(${rgb[0]},${rgb[1]},${rgb[2]},${PATCH_ALPHA * 0.86})`);
  g.addColorStop(1, `rgba(${rgb[0]},${rgb[1]},${rgb[2]},${PATCH_ALPHA * 0.55})`);
  ctx.filter = 'blur(0.8px)';
  ctx.fillStyle = g;
  ctx.fill();
  ctx.filter = 'none';
  strokeStreaks(ctx, rand, len, hs, rgb);
  ctx.restore();
}

/** 墨迹（抠底后图案）在不透明像素上的 bbox。 */
function inkBounds(data, width, height) {
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

/** 笔触块：n 道叠刷铺满墨迹外扩区域，内部实心、端头参差。 */
function paintStrokePatch(ctx, rand, rgb, patchW, patchH) {
  const ang = -0.1 + (rand() - 0.5) * 0.04;
  const scale = Math.min(
    (PATCH_SIZE - 40) / (patchW + patchH * Math.abs(Math.sin(ang)) + patchW * 0.06),
    (PATCH_SIZE - 40) / (patchH + patchW * Math.abs(Math.sin(ang)))
  );
  const pw = patchW * scale;
  const ph = patchH * scale;
  const n = Math.max(3, Math.min(6, Math.round(patchH / (patchW * 0.24)) + 2));
  const hs = (ph / n) * 1.8;
  for (let i = 0; i < n; i += 1) {
    const t = i / (n - 1);
    const oy = (t - 0.5) * (ph - hs * 0.5);
    markerStroke(
      ctx, rand,
      PATCH_SIZE / 2 + (rand() - 0.5) * pw * 0.06,
      PATCH_SIZE / 2 + oy,
      pw * (0.94 + rand() * 0.14),
      hs,
      ang + (rand() - 0.5) * 0.03,
      rgb
    );
  }
  return { ang, scale };
}

/**
 * 队徽卡面渲染入口：纯底色队徽把纯色底替换为「队徽自身底色的手刷笔触块」，整图墨迹居中叠放；
 * 透明底/非纯底队徽回退到近白抠底。结果按绝对 URL 缓存。
 */
export function brushPatchLogo(url) {
  if (!url) {
    return Promise.resolve({ url: '', patched: false });
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
        resolve(buildPatch(image, src) || { url: nearWhiteKeyed(image), patched: false });
      } catch {
        resolve({ url: src, patched: false });
      }
    };
    image.onerror = () => resolve({ url: src, patched: false });
    image.src = src;
  });
  cache.set(src, task);
  return task;
}

function buildPatch(image, src) {
  const width = image.naturalWidth;
  const height = image.naturalHeight;
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext('2d');
  context.drawImage(image, 0, 0);
  const pixels = context.getImageData(0, 0, width, height);
  const solid = solidBackground(pixels.data, width, height);
  if (!solid) {
    return null;
  }
  keySolidBorder(pixels.data, width, height, solid);
  cleanKeyedFringe(pixels);
  context.putImageData(pixels, 0, 0);
  const box = inkBounds(pixels.data, width, height);
  if (!box) {
    return null;
  }
  const bw = box.maxX - box.minX;
  const bh = box.maxY - box.minY;
  const patch = document.createElement('canvas');
  patch.width = PATCH_SIZE;
  patch.height = PATCH_SIZE;
  const pctx = patch.getContext('2d');
  const rand = randFactory(seedFrom(src));
  const rgb = [solid.r, solid.g, solid.b];
  const placed = paintStrokePatch(pctx, rand, rgb, bw * 1.26, bh * 1.26);
  const bcx = (box.minX + box.maxX) / 2;
  const bcy = (box.minY + box.maxY) / 2;
  pctx.save();
  pctx.translate(PATCH_SIZE / 2, PATCH_SIZE / 2);
  pctx.rotate(placed.ang);
  pctx.drawImage(canvas, -bcx * placed.scale, -bcy * placed.scale, width * placed.scale, height * placed.scale);
  pctx.restore();
  return { url: patch.toDataURL('image/png'), patched: true };
}
