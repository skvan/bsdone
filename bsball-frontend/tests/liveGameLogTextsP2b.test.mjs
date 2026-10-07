// P2 批次2 回归断言（模板收敛·纯迁移：投球/换人/特例行 + 静态门禁）
// 运行：node --test tests/liveGameLogTextsP2b.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { pitchText, coachTimeoutText, subText, setRunnerText, pickoffNoneText, fpMissText, foulErrorText } from '../src/utils/liveGameLogTexts.js';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('pitchText：投球族逐字映射（纯迁移基线）', () => {
  assert.equal(pitchText({ who: '梁志伟', kind: 'ball', count: '(B1-S0)' }), '梁志伟: 坏球 (B1-S0)');
  assert.equal(pitchText({ who: '梁志伟', kind: 'strike_called', count: '(B0-S1)' }), '梁志伟: 好球 (B0-S1)');
  assert.equal(pitchText({ who: '王一', kind: 'strike_swing', count: '(B0-S1)' }), '王一: 挥空 (B0-S1)');
  assert.equal(pitchText({ who: '梁志伟', kind: 'strikeout_called', count: '(B0-S3)' }), '梁志伟: 第三好球（判进） (B0-S3)');
  assert.equal(pitchText({ who: '王一', kind: 'strikeout_swing', count: '(B0-S3)' }), '王一: 第三好球（挥空） (B0-S3)');
  assert.equal(pitchText({ who: '王一', kind: 'foul', count: '(B1-S2)' }), '王一: 击出界外球 (B1-S2)');
  assert.equal(pitchText({ who: '王一', kind: 'foul_k', count: '(B1-S3)' }), '王一: 击出界外球（两好球后界外三振）(B1-S3)');
  assert.equal(pitchText({ who: '梁志伟', kind: 'hit_by_pitch' }), '梁志伟: 触身球');
  assert.equal(pitchText({ who: '梁志伟', kind: 'balk' }), '梁志伟: 投手犯规');
  assert.equal(pitchText({ who: '梁志伟', kind: 'balk_ball', count: '(B1-S0)' }), '梁志伟: 投手犯规（记坏球 (B1-S0)）');
  assert.equal(pitchText({ who: '梁志伟', kind: 'wild_pitch' }), '梁志伟: 暴投 (WP)');
  assert.equal(pitchText({ who: '陈捕', kind: 'passed_ball' }), '陈捕: 捕逸 (PB)');
  assert.equal(pitchText({ who: '梁志伟', kind: 'walk' }), '梁志伟: 四坏保送');
  assert.equal(pitchText({ who: '王一', kind: 'strikeout_dropped' }), '王一: 第三好球（漏接三振）');
});

test('教练暂停/换人/设置跑者/牵制 none/野选未得分/界外+失误：逐字映射', () => {
  assert.equal(coachTimeoutText('暴力羊'), '教练暂停：暴力羊');
  assert.equal(coachTimeoutText(undefined), '教练暂停：进攻方');
  assert.equal(subText({ kind: 'run', inName: '钱乾', outName: '李四' }), '换人: 代跑 钱乾 换下 李四');
  assert.equal(subText({ kind: 'hit', inName: '王五', outName: '张三' }), '换人: 代打 王五 换下 张三');
  assert.equal(setRunnerText({ name: '钱乾', baseCn: '二垒' }), '设置跑者: 钱乾 -> 二垒');
  assert.equal(pickoffNoneText({ pitcher: { name: 'Lee ju won', number: 0 }, base: 1 }), '投手 Lee ju won (#0): 牵制一垒（未造成进垒/出局）');
  assert.equal(pickoffNoneText({ pitcher: { name: null, number: 8 }, base: 2 }), '投手 跑者 (#8): 牵制二垒（未造成进垒/出局）');
  assert.equal(fpMissText({ runner: { name: '李四', number: 21 }, verdict: '封杀出局 (FO)' }), '野选推进 · 李四 (#21) 封杀出局 (FO) — 未得分');
  assert.equal(fpMissText({ runner: { name: '李四', number: null }, verdict: '本垒出局 (Out)' }), '野选推进 · 李四 (#-) 本垒出局 (Out) — 未得分');
  assert.equal(foulErrorText({ chainText: '5→4' }), '界外球+失误：5→4');
  assert.equal(foulErrorText({ chainText: '' }), '界外球+失误：守备失误');
});

test('P2b 门禁：D( 直写恰为白名单（9 行 / 12 次）+ 白名单片段存在', () => {
  // 白名单构成：纠偏×2（得分/垒况）/ 第三出局注记 / 换局行×2（同局行含两处）/ NP 注记 / 漏接 guard×2 / 失误推进×2（同类行含两处）/ SB 单选×2（同类行含两处）
  const n = (src.match(/D\(`/g) || []).length;
  assert.equal(n, 12, `D( 直写数 ${n} 应恰为白名单 12 次（新增文案请经 liveGameLogTexts 生成器）`);
  for (const f of ['D(`手动纠偏得分：', 'D(`手动纠偏垒况', 'D(`第三出局为封杀', 'D(`--- ${l.inning}局', 'D(`漏接三振：未识别代码']) {
    assert.ok(src.includes(f), '白名单片段仍在：' + f);
  }
});

test('P2b 静态：import 别名与迁移点抽检', () => {
  assert.ok(src.includes('pitchText as LgPitch'), 'pitchText 别名');
  assert.ok(src.includes('subText as LgSub'), 'subText 别名');
  assert.ok(src.includes('D(LgPitch({who:a,kind:"ball",count:'), '坏球迁移');
  assert.ok(src.includes('D(LgPitch({who:a,kind:"walk"})'), '四坏保送迁移');
  assert.ok(src.includes('D(LgCoach((l.isTop?At:xt).value?.name))'), '教练暂停迁移');
  assert.ok(src.includes('D(LgSub({kind:"run",inName:P.name,outName:t.name}))'), '代跑迁移');
  assert.ok(src.includes('D(LgFoulErr({chainText:'), '界外+失误迁移');
  assert.ok(src.includes('D(LgPickoffNone({pitcher:Se.value,base:s}))'), '牵制 none 迁移');
  assert.ok(src.includes('D(LgFpMiss({runner:p,verdict:'), '野选未得分迁移');
});
