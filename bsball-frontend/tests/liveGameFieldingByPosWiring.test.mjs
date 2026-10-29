// LiveGame 逐位置 PO/A/E 接线：所有守备记账点必须同时累加 byPos
// 运行：node --test tests/liveGameFieldingByPosWiring.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const live = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('LiveGame 引入 accrueFieldingPlay 与投手记账目标解析', () => {
  assert.ok(
    live.includes('accrueFieldingPlay'),
    'LiveGame 未引入/使用 accrueFieldingPlay'
  );
  assert.ok(live.includes('resolveFieldingCreditTarget'), 'LiveGame 未引入投手记账目标解析');
});

test('DH 制仅守备投手：所有 PO/A/E 记账目标先解析到活体投手', () => {
  assert.equal(
    live.split('resolveFieldingCreditTarget(').length - 1,
    8,
    '应为 Dn/chain/失误/三振捕手/三振野手/牵制/接杀回传/盗垒死 共 8 处解析'
  );
  assert.equal(
    live.split('resolveFieldingCreditTarget(r.pos,Hn.value.find(a=>a.position===r.pos),Se.value)').length - 1,
    2,
    'Dn/applyChainFieldingStats 应各解析一次 P 位记账目标'
  );
  assert.ok(live.includes('resolveFieldingCreditTarget(player.position,player,Se.value)'), '失误未解析投手目标');
  assert.ok(live.includes('resolveFieldingCreditTarget("C",Vt("C"),Se.value)'), '三振捕手未解析目标');
  assert.ok(
    live.includes('resolveFieldingCreditTarget(fielderCode,Vt(String(fielderCode).trim().toUpperCase()),Se.value)'),
    '三振野手未解析目标'
  );
  assert.equal(live.split('resolveFieldingCreditTarget(e,Vt(e),Se.value)').length - 1, 2, '牵制/盗垒死未解析目标');
  assert.ok(
    live.includes('resolveFieldingCreditTarget(fielding.pos,Vt(fielding.pos),Se.value)'),
    '接杀回传未解析目标'
  );
});

test('传球链与单出局记账：PO/A 同时写入当前守位', () => {
  const pattern = 'accrueFieldingPlay(p.stats.fielding,r.pos,{po:r.po,a:r.a})';
  assert.equal(live.split(pattern).length - 1, 2, `Dn/applyChainFieldingStats 应各有一处：${pattern}`);
});

test('失误记账：E 写入失误责任人的当前守位', () => {
  assert.ok(
    live.includes('accrueFieldingPlay(target.stats.fielding,player.position,{e:1})'),
    'creditFieldingError 未同步逐位置 E'
  );
});

test('三振守备：捕手 PO/A 记 C，接球野手 PO 记其守位', () => {
  assert.ok(live.includes('accrueFieldingPlay(catcher.stats.fielding,"C",{po:credit.catcherPo})'), '捕手 PO 未同步');
  assert.ok(live.includes('accrueFieldingPlay(catcher.stats.fielding,"C",{a:credit.catcherA})'), '捕手 A 未同步');
  assert.ok(
    live.includes('accrueFieldingPlay(f.stats.fielding,fielderCode,{po:credit.fielderPo})'),
    '漏接三振野手 PO 未同步'
  );
});

test('牵制出局 / 盗垒死：PO 记守备员守位', () => {
  assert.ok(live.includes('accrueFieldingPlay(P0.stats.fielding,e,{po:1})'), '牵制出局 PO 未同步');
  assert.ok(live.includes('accrueFieldingPlay(p.stats.fielding,e,{po:1})'), '盗垒死 PO 未同步');
});

test('球队失误总数包含 DH 仅守备投手 fpRow', () => {
  assert.ok(
    live.includes('[...X.value,...ae.value,...(De.value?[De.value]:[])]'),
    '客队失误总数漏计仅守备投手'
  );
  assert.ok(
    live.includes('[...q.value,...ye.value,...(xe.value?[xe.value]:[])]'),
    '主队失误总数漏计仅守备投手'
  );
});

test('接杀回传双杀：PO/A 按链路位置逐位置累计', () => {
  assert.ok(
    live.includes('accrueFieldingPlay(player.stats.fielding,fielding.pos,{po:fielding.po,a:fielding.a})'),
    '接杀回传 PO/A 未同步'
  );
});
