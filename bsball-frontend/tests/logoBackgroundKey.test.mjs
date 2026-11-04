import test from 'node:test';
import assert from 'node:assert/strict';
import {
  sampleBorder,
  solidBackground,
  keySolidBorder,
  alphaBounds,
  keyTeamLogo
} from '../src/components/portal/logoBackgroundKey.js';

function makePixels(width, height, filler) {
  const data = new Uint8ClampedArray(width * height * 4);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const [r, g, b, a] = filler(x, y);
      const i = (y * width + x) * 4;
      data[i] = r;
      data[i + 1] = g;
      data[i + 2] = b;
      data[i + 3] = a;
    }
  }
  return data;
}

const isBorder = (x, y, w, h) => x === 0 || y === 0 || x === w - 1 || y === h - 1;

test('不透明纯白边框判定为纯底色且覆盖率为 1', () => {
  const w = 64;
  const h = 48;
  const data = makePixels(w, h, (x, y) => (isBorder(x, y, w, h) ? [255, 255, 255, 255] : [195, 13, 35, 255]));
  const solid = solidBackground(data, w, h);
  assert.ok(solid);
  assert.deepEqual([solid.r, solid.g, solid.b], [255, 255, 255]);
  assert.equal(solid.coverage, 1);
  assert.equal(solid.transparentRatio, 0);
});

test('全透明边框（已抠图队徽）返回 null', () => {
  const w = 64;
  const h = 48;
  const data = makePixels(w, h, (x, y) => (isBorder(x, y, w, h) ? [0, 0, 0, 0] : [201, 162, 39, 255]));
  assert.equal(solidBackground(data, w, h), null);
  assert.equal(sampleBorder(data, w, h).transparentRatio, 1);
});

test('仅两条边透明（裁切留边）仍按不透明部分判定纯底色', () => {
  const w = 64;
  const h = 48;
  const data = makePixels(w, h, (x, y) => {
    if (x === w - 1 || y === h - 1) return [0, 0, 0, 0];
    return isBorder(x, y, w, h) ? [152, 0, 6, 255] : [240, 220, 200, 255];
  });
  const border = sampleBorder(data, w, h);
  assert.ok(border.transparentRatio > 0.4 && border.transparentRatio < 0.6);
  const solid = solidBackground(data, w, h);
  assert.ok(solid);
  assert.deepEqual([solid.r, solid.g, solid.b], [152, 0, 6]);
});

test('边框颜色分散（照片类）覆盖率不足，返回 null', () => {
  const w = 64;
  const h = 48;
  let n = 0;
  const data = makePixels(w, h, (x, y) => {
    if (isBorder(x, y, w, h)) {
      n += 1;
      return [(n * 37) % 256, (n * 91) % 256, (n * 149) % 256, 255];
    }
    return [10, 20, 30, 255];
  });
  assert.equal(solidBackground(data, w, h), null);
});

test('纯色底从边缘抠成透明，内部字形保持不变', () => {
  const w = 64;
  const h = 48;
  const data = makePixels(w, h, (x, y) => (
    isBorder(x, y, w, h) || x < 8 || y < 8
      ? [255, 255, 255, 255]
      : [10, 20, 30, 255]
  ));
  keySolidBorder(data, w, h, { r: 255, g: 255, b: 255 });
  assert.equal(data[3], 0);
  assert.equal(data[(24 * w + 32) * 4 + 3], 255);
});

test('裁切边界按不透明墨迹计算', () => {
  const w = 64;
  const h = 48;
  const data = makePixels(w, h, (x, y) => (
    x >= 10 && x <= 20 && y >= 8 && y <= 16
      ? [10, 20, 30, 255]
      : [10, 20, 30, 0]
  ));
  assert.deepEqual(alphaBounds(data, w, h), { minX: 10, minY: 8, maxX: 20, maxY: 16 });
});

test('空队徽地址不发请求并返回空结果', async () => {
  const result = await keyTeamLogo('');
  assert.deepEqual(result, { url: '', keyed: false });
});

test('卡面队徽入口已导出', () => {
  assert.equal(typeof keyTeamLogo, 'function');
  assert.equal(typeof keySolidBorder, 'function');
  assert.equal(typeof solidBackground, 'function');
});
