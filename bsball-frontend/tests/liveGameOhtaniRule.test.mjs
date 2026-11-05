// 大谷规则（Issue #525）：LiveGame 规则读取、换人守卫、并行行合并——静态回归断言
// 运行：node --test tests/liveGameOhtaniRule.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('大谷规则：从赛事特殊规则读取 ohtaniRule', () => {
  assert.match(src, /ohtaniRuleOn\.value=ev\?\.specialRules\?\.ohtaniRule===!0/);
});

test('大谷规则：DH 兼投仍在投手位时禁止 P ↔ 野手互换', () => {
  assert.match(src, /投手兼DH不能与野手互换守位/);
  assert.match(src, /const dh=y\.find\(player=>Nn\(player\.position\)==="DH"\)/);
});

test('大谷规则：DH 卡取消/移防被拦截', () => {
  assert.match(src, /已换投下场的 DH 不能移防或取消/);
  assert.match(src, /投手兼DH不能用 DH 卡取消或移防/);
});

test('普通 DH：存在 DH 移防并取消 DH 的独立流程', () => {
  assert.match(src, /async function confirmDhEnterDefense\(side,dhPlayer,targetCode\)/);
  assert.match(src, /确认取消DH并移防/);
});

test('并行行：快照 kf() 按 playerId 合并，不再覆盖', () => {
  assert.match(src, /e\[String\(p\)\]=e\[String\(p\)\]\?mergeStatsObjects\(e\[String\(p\)\],stats\):stats/);
});

test('并行行：换投回收时，旧投手仍在打线则并回统计而不是复制进替补', () => {
  assert.match(src, /const lineupRow=Array\.isArray\(lineup\)\?lineup\.find\(E=>Number\(E\.id\)===V\):null/);
  assert.match(src, /mergeRowStats\(lineupRow,row\.stats\)/);
});

test('守备投手解析：fpId 在打线中时可从打线行取投手', () => {
  assert.match(src, /const lf=e\.value\.find\(g=>Number\(g\.id\)===Number\(p\)\)/);
});

test('普通 DH 移防只允许当前守备方，避免把非守备方 DH 拖到球场', () => {
  assert.match(src, /if\(side!==cr\(\)\)\{\s*J\("只能调整当前守备方的守位"/);
  assert.match(src, /hit\?\.kind==="fielder"&&s\.side===cr\(\)/);
});

test('并行行合并：byPos 对象走专用合并，不能被 Number 运算变成 NaN', () => {
  assert.match(src, /mergeFieldingByPos\(current\[field\],incoming\[field\]\)/);
  assert.match(src, /if\(field==="byPos"\)/);
});

test('普通 DH 拖到投手位：取消 DH 并换投，DH 球员留在原棒次打击', () => {
  assert.match(src, /取消DH并换投/);
  assert.match(src, /转为投手 P，继续在第\$\{dhIndex\+1\}棒打击/);
  assert.match(src, /ctx\.lineup\.value\[dhIndex\]=\{\.\.\.dh,position:"P"/);
});
