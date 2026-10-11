// 落点入队覆盖静态断言（issue #279）：确保各结果分支与对话框路径都调用入队
// 说明：LiveGame.js 为 266KB 单文件记分台，纯静态断言防回归（与 liveGameSacrificeChoices 同法）
// 运行：node --test tests/hitSprayCoverage.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('轨迹参数表与配色已改为共享模块导入，内联表已移除', () => {
  assert.match(source, /import \{ BAT_SHAPES, battedShapeColor \} from '\.\.\/\.\.\/utils\/battedBallFlight';/);
  assert.doesNotMatch(source, /const BAT_SHAPES=\{/, '不应再保留内联 BAT_SHAPES 表');
  assert.match(source, /battedShapeColor\(a\)/, '录入动画轨迹应按球种取色');
  assert.match(source, /style:c\(battedTrail\)\.color\?\{/, '轨迹 path 应绑定球种颜色');
});

test('推送逻辑已提取为 pushHitSpray，bt 主链经 s 包装调用', () => {
  assert.match(source, /function pushHitSpray\(resultCode,rbi,spray\)\{/);
  assert.match(source, /const s=\(b,w=0\)=>pushHitSpray\(b,w,a\);/);
  assert.match(source, /import \{ buildHitSprayRecord as LgHitSprayRecord \} from '\.\.\/\.\.\/utils\/hitSprayRecord';/);
});

test('bt 主链各结果分支均入队（含此前缺失的 7 类）', () => {
  // 既有覆盖（回归保护）
  for (const code of ['H1', 'H2', 'H3', 'HR']) {
    assert.match(source, new RegExp(`s\\("${code}"`), `${code} 分支应入队`);
  }
  assert.match(source, /s\(g,0\)/, 'G/F/L/TAG 出局经变量 g 入队');
  // 本次补齐
  assert.match(source, /s\("IPHR",b\)/, 'IPHR（场内全垒打）应入队');
  assert.match(source, /s\("SF",sfRbi\?1:0\)/, 'SF（高飞牺牲）应入队且打点取实际值');
  assert.match(source, /s\("SH",0\)/, 'SH（牺牲触击）应入队');
  assert.match(source, /s\("E",0\)/, 'E（失误上垒）主链应入队');
  assert.match(source, /s\("DP",0\)/, 'DP（双杀）主链应入队');
  assert.match(source, /s\("TP",0\)/, 'TP（三杀）主链应入队，结果码为 TP');
});

test('对话框路径均入队：dp / fp / fcApply', () => {
  const dialogCalls = source.split('pushHitSpray(e.playType,0,e.opts)').length - 1;
  assert.equal(dialogCalls, 2, 'dp（跑者推进）与 fp（野选系对话框）各一处');
  assert.match(source, /pushHitSpray\("E",0,a\);/, 'fcApply“没有跑者出局（失误）”分支应入队 E');
  assert.match(source, /pushHitSpray\("FC",0,a\);/, 'fcApply 正常野选路径应入队 FC');
});
