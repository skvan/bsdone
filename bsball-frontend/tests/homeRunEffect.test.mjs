// 越墙全垒打落点几何与烟花参数单测（纯函数，不触碰 DOM）
// 运行：node --test tests/homeRunEffect.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  HR_OUT_MARGIN,
  buildFireworkBurstSpecs,
  homeRunFieldPos,
  isInsideFairFan,
  projectHomeRunLanding,
} from '../src/utils/homeRunEffect.js';

function seeded(seed) {
  let s = seed >>> 0;
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0;
    return s / 4294967296;
  };
}

test('扇形内外判定：场内/边角/墙外/本垒后方', () => {
  assert.equal(isInsideFairFan(240, 150), true, '中外野深处在场内');
  assert.equal(isInsideFairFan(75, 230), true, '左外野在场内');
  assert.equal(isInsideFairFan(0, 120), true, '左侧极浅区域在场内');
  assert.equal(isInsideFairFan(-60, 330), false, '左边界外（界外）');
  assert.equal(isInsideFairFan(540, 330), false, '右边界外（界外）');
  assert.equal(isInsideFairFan(240, -35), false, '围墙上方（墙外）');
  assert.equal(isInsideFairFan(240, 620), false, '本垒后方');
});

test('HR 落点：场内点沿本垒射线外推到扇形外，方向不变', () => {
  const inside = { x: 240, y: 150 };
  const out = projectHomeRunLanding(inside.x, inside.y, 'CF');
  assert.equal(isInsideFairFan(out.x, out.y), false, '外推后应在扇形外');
  assert.ok(out.y < -17.3, '中外野 HR 应落在围墙上方');
  assert.ok(Math.abs(out.x - 240) < 0.001, '中外野方向应保持 x=240');

  const left = projectHomeRunLanding(75, 230, 'LF');
  const dirIn = { x: 75 - 240, y: 230 - 555 };
  const dirOut = { x: left.x - 240, y: left.y - 555 };
  const cos = (dirIn.x * dirOut.x + dirIn.y * dirOut.y) / (Math.hypot(dirIn.x, dirIn.y) * Math.hypot(dirOut.x, dirOut.y));
  assert.ok(cos > 0.9999, '外推方向应与本垒→拖拽点方向一致');
  assert.equal(isInsideFairFan(left.x, left.y), false);
});

test('HR 落点：已经在扇形外时原样使用', () => {
  const outside = { x: 110, y: -35 };
  assert.deepEqual(projectHomeRunLanding(outside.x, outside.y, 'LF'), outside, '墙外点不再外推');
  const corner = { x: -120, y: 260 };
  assert.deepEqual(projectHomeRunLanding(corner.x, corner.y, 'LF'), corner, '界外白色区域原样使用');
});

test('HR 落点：本垒后方/非有限坐标回退到所选外野方向', () => {
  const behind = projectHomeRunLanding(240, 620, 'RF');
  assert.equal(isInsideFairFan(behind.x, behind.y), false, '本垒后方应改投影到墙外');
  assert.ok(behind.x > 240, 'RF 回退方向应偏右');

  const fallback = projectHomeRunLanding(NaN, 120, 'LF');
  assert.equal(isInsideFairFan(fallback.x, fallback.y), false);
  assert.ok(fallback.x < 240, 'LF 回退方向应偏左');
});

test('HR 外推距离至少超过围墙且带安全 margin', () => {
  const center = projectHomeRunLanding(240, 150, 'CF');
  // 围墙顶约 y=-17.3；margin 40 => 约 -57
  assert.ok(center.y <= -17.3 - HR_OUT_MARGIN + 1, `外推点应至少越过墙顶 margin，实际 y=${center.y}`);
});

test('烟花绽放参数：数量/半径/延迟/配色/方向分布', () => {
  const palette = ['#111111', '#222222', '#333333'];
  const specs = buildFireworkBurstSpecs({ count: 24, radius: 80, delay: 130, palette, random: seeded(7) });
  assert.equal(specs.length, 24);
  for (const s of specs) {
    assert.ok(palette.includes(s.color));
    assert.ok(s.distance >= 80 * 0.62 && s.distance <= 80, `distance=${s.distance}`);
    assert.ok(s.duration >= 780 && s.duration <= 1260);
    assert.ok(s.delay >= 130 && s.delay <= 230);
    assert.ok(Number.isFinite(s.angle));
  }
  const angles = specs.map((s) => s.angle).sort((a, b) => a - b);
  assert.ok(angles[angles.length - 1] - angles[0] > Math.PI, '粒子应覆盖整圈');
});

test('烟花绽放参数：同一 seed 结果可复现', () => {
  const a = buildFireworkBurstSpecs({ count: 12, radius: 40, palette: ['#abc'], random: seeded(42) });
  const b = buildFireworkBurstSpecs({ count: 12, radius: 40, palette: ['#abc'], random: seeded(42) });
  assert.deepEqual(a, b);
});

test('比赛详情回放接入同一套 HR 特效（落点外推/彩虹粗线/两侧火花/烟花）', () => {
  const src = readFileSync(new URL('../src/components/common/GameDetailHitReplay.vue', import.meta.url), 'utf8');
  assert.match(src, /isHomeRunBip/, '回放应识别越墙 HR');
  assert.match(src, /projectHomeRunLanding\(r\.sprayX, r\.sprayY, r\.sprayFieldPos \?\? r\.sprayZone\)/, '回放应把 HR 落点投影到扇形外（库中通常只有 sprayZone）');
  assert.match(src, /HR_TRAIL_STOPS/, '回放应使用共享彩虹渐变');
  assert.match(src, /ghr-hr-trail/, '回放应有 HR 渐变定义');
  assert.match(src, /ghr-trail--hr/, '回放应使用 HR 粗线样式');
  assert.match(src, /ghr-fireworks-layer/, '回放应有烟花图层');
  assert.match(src, /playHomeRunFireworks\(fxLayer\.value/, '回放落点应绽放烟花');
  assert.match(src, /spawnHomeRunSideSparks\(fxLayer\.value/, '回放飞行全程应有两侧火花');
  assert.match(src, /clearHomeRunFx\(fxLayer\.value\)/, '回放切换时应清理特效');
});

test('实时录入与共享模块的彩虹渐变同源', () => {
  const live = readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
  const effect = readFileSync(new URL('../src/utils/homeRunEffect.js', import.meta.url), 'utf8');
  assert.match(live, /HR_TRAIL_STOPS as LgHrTrailStops/, '录入页应直接复用共享渐变停靠点');
  assert.match(live, /et\(LgHrTrailStops/, '录入页渐变应由共享停靠点渲染');
  for (const color of ['#38BDF8', '#B388FF', '#FF4FD8', '#FFD700', '#4ADE80']) {
    assert.ok(effect.includes(color), `共享模块缺少渐变颜色 ${color}`);
  }
});

test('HR 记录野位：垒球 XF1/XF2 映射到 LF/RF，避免落点区域回退成内野', () => {
  assert.equal(homeRunFieldPos('XF1'), 'LF');
  assert.equal(homeRunFieldPos('xf2'), 'RF');
  assert.equal(homeRunFieldPos('LF'), 'LF');
  assert.equal(homeRunFieldPos('CF'), 'CF');
  assert.equal(homeRunFieldPos('RF'), 'RF');
  assert.equal(homeRunFieldPos('XX'), 'CF');
  assert.equal(homeRunFieldPos(null), 'CF');
});

test('烟花闪光与粒子围绕落点缩放，避免 SVG 默认 0 0 原点把绽放扫出画面', () => {
  const src = readFileSync(new URL('../src/utils/homeRunEffect.js', import.meta.url), 'utf8');
  assert.ok(src.includes('flash.style.transformOrigin = `${x}px ${y}px`'), '闪光需以落点为缩放原点');
  assert.ok(src.includes('line.style.transformOrigin = `${x}px ${y}px`'), '烟花粒子需以落点为缩放原点');
  assert.ok(src.includes('ring.style.transformOrigin = `${x}px ${y}px`'), '圆环需以落点为缩放原点');
});
