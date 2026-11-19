import { test } from 'node:test';
import assert from 'node:assert/strict';
import { computeCaptureScale, formatImageSize } from '../src/utils/clubhouseLongImage.js';

test('computeCaptureScale：手机典型长页取目标 2 倍', () => {
  assert.equal(computeCaptureScale({
    width: 390,
    height: 5000,
    mobile: true,
    targetScale: 2
  }), 2);
});

test('computeCaptureScale：超长页面按面积/最大边降 scale', () => {
  const longPage = computeCaptureScale({
    width: 390,
    height: 30000,
    mobile: true,
    targetScale: 2
  });
  assert.ok(longPage > 0 && longPage < 2);
  // 手机最大边 16000：30000px 高页面被压到 0.53
  assert.equal(longPage, 0.53);

  const extreme = computeCaptureScale({
    width: 390,
    height: 100000,
    mobile: true,
    targetScale: 2
  });
  assert.equal(extreme, 0.16);
});

test('computeCaptureScale：桌面整页取目标 1.75 倍', () => {
  assert.equal(computeCaptureScale({
    width: 1280,
    height: 6000,
    mobile: false,
    targetScale: 1.75
  }), 1.75);
});

test('computeCaptureScale：非法尺寸回退下限；自定义像素预算生效', () => {
  assert.equal(computeCaptureScale({ width: 0, height: 0, mobile: true }), 0.05);
  assert.equal(computeCaptureScale({
    width: 1000,
    height: 1000,
    mobile: false,
    targetScale: 2,
    maxPixels: 1_000_000
  }), 1);
});

test('formatImageSize：KB/MB、临界值与非法值', () => {
  assert.equal(formatImageSize(500 * 1024), '500 KB');
  assert.equal(formatImageSize(1.5 * 1024 * 1024), '1.5 MB');
  assert.equal(formatImageSize(1024 * 1024 - 1), '1.0 MB');
  assert.equal(formatImageSize(0), '—');
  assert.equal(formatImageSize(Number.NaN), '—');
});
