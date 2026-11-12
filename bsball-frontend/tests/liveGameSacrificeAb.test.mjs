// 牺牲打补记：SH/SF 不计 AB，重复补记不重复扣减（Issue #651）
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { resolveSacrificeCredit } from '../src/utils/liveGameSacrifice.js';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const pick = (startMark, endMark) => {
  const i = src.indexOf(startMark);
  assert.ok(i >= 0, startMark + ' 存在');
  const j = src.indexOf(endMark, i);
  assert.ok(j > i, endMark + ' 在 ' + startMark + ' 之后');
  return src.slice(i, j);
};

test('飞球/平飞出局且跑者得分：补记 SF，AB -1，记 RBI', () => {
  for (const resultCode of ['F', 'OUT_F', 'L', 'OUT_L']) {
    const credit = resolveSacrificeCredit({ resultCode, toBase: 4 });
    assert.deepEqual(credit, { resultCode: 'SF', creditRbi: true }, resultCode);
  }
});

test('飞球/平飞出局但跑者未得分：不是牺牲飞球，不补记', () => {
  for (const resultCode of ['F', 'OUT_F', 'L', 'OUT_L']) {
    assert.equal(resolveSacrificeCredit({ resultCode, toBase: 3 }), null, resultCode);
  }
});

test('滚地/触杀出局：补记 SH，AB -1；跑者得分时同时记 RBI', () => {
  for (const resultCode of ['G', 'OUT_G', 'TAG', 'OUT_TAG']) {
    assert.deepEqual(resolveSacrificeCredit({ resultCode, toBase: 3 }), { resultCode: 'SH', creditRbi: false }, resultCode);
    assert.deepEqual(resolveSacrificeCredit({ resultCode, toBase: 4 }), { resultCode: 'SH', creditRbi: true }, resultCode);
  }
});

test('已记 SF/SH、安打、保送、触身、失误、野选等结果不补记', () => {
  for (const resultCode of ['SF', 'SH', 'H1', 'H2', 'H3', 'HR', 'BB', 'HBP', 'E', 'FC', 'K', 'DP', 'TP', null, undefined, '']) {
    assert.equal(resolveSacrificeCredit({ resultCode, toBase: 3 }), null, String(resultCode));
    assert.equal(resolveSacrificeCredit({ resultCode, toBase: 4 }), null, String(resultCode));
  }
});

test('wf() 使用纯函数判定，并回减 AB（PA 不变）', () => {
  const body = pick('function wf(e){', '// 「上一棒内推进（有打点）」');
  assert.match(body, /LgSacrificeCredit\(\{resultCode,toBase:e\}\)/, '走纯函数判定');
  assert.match(body, /a\.stats\.batting\.ab=Math\.max\(0,\(a\.stats\.batting\.ab\?\?0\)-1\)/,
    '补记牺牲打必须回减 AB，且不出现负数');
  assert.match(body, /credit\.resultCode==="SF"/, '按判定结果记 SF/SH');
  assert.match(body, /credit\.creditRbi/, 'RBI 只在跑者得分时记');
  assert.match(body, /t\.inning!==l\.inning\|\|t\.half!==halfNow/, '只允许补记本半局的上一棒');
  assert.match(body, /return null/, '不可补记时仅保留跑者推进');
  assert.match(body, /runnerPitcher\(\{responsiblePitcherId:t\.pitcherPlayerId\},null\)/, '按上一打席投手定位');
  assert.match(body, /credit\.resultCode==="SF"\?"pitchSf":"pitchSh"/, '同步投手被记牺牲飞球/牺牲触击');
  assert.match(body, /未找到上一打席投手/, '投手定位失败时提示');
  assert.match(body, /f\.playEvents\.find\(x=>x\.id===t\.id\)/, '定位上一打席对应 playEvent');
  assert.match(body, /event\.resultCode=credit\.resultCode/, '同步 playEvent 结果码');
  assert.match(body, /event&&\(event\.rbi=t\.rbi\)/, 'RBI 补记后同步 playEvent');
  assert.match(body, /LgSacrificeNote\(credit\.resultCode\)/, '生成 PBP 补记尾注');
  assert.match(body, /yf\(note,""\)/, '写入上一打席日志');
});

test('未实际推进时不补记牺牲打', () => {
  assert.ok(src.includes('e.id==="safe_last_play_sac"&&s!==r&&(rbiWho=wf(r))'),
    'wf 必须在确认 from/to 不同后才执行');
});

test('正常菜单的 SF/SH 分支本身不累加 AB', () => {
  const sf = pick('else if(e==="SF"){', 'else if(e==="SH")');
  const sh = pick('else if(e==="SH")', 'else if(e==="E"){');
  assert.doesNotMatch(sf, /stats\.batting\.ab/, '正常 SF 不得累加 AB');
  assert.doesNotMatch(sh, /stats\.batting\.ab/, '正常 SH 不得累加 AB');
});
