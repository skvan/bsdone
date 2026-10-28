import { test } from 'node:test';
import assert from 'node:assert/strict';
import { strikeoutFieldingCredit } from '../src/utils/liveGameStrikeoutFielding.js';

test('普通三振/擦棒被捕：捕手 PO +1', () => {
  assert.deepEqual(strikeoutFieldingCredit('caught'), { catcherPo: 1, catcherA: 0, fielderPo: 0 });
});

test('漏接三振传一垒：捕手 A +1、一垒手 PO +1', () => {
  assert.deepEqual(strikeoutFieldingCredit('dropped', '1B'), { catcherPo: 0, catcherA: 1, fielderPo: 1 });
  assert.deepEqual(strikeoutFieldingCredit('dropped', 'P'), { catcherPo: 0, catcherA: 1, fielderPo: 1 });
});

test('漏接三振捕手自己触杀：捕手 PO +1，不记 A', () => {
  assert.deepEqual(strikeoutFieldingCredit('dropped', 'C'), { catcherPo: 1, catcherA: 0, fielderPo: 0 });
  assert.deepEqual(strikeoutFieldingCredit('dropped', 'c'), { catcherPo: 1, catcherA: 0, fielderPo: 0 });
});

test('漏接三振未完成出局/未知类型：不记守备', () => {
  assert.deepEqual(strikeoutFieldingCredit('dropped', null), { catcherPo: 0, catcherA: 0, fielderPo: 0 });
  assert.deepEqual(strikeoutFieldingCredit('dropped', ''), { catcherPo: 0, catcherA: 0, fielderPo: 0 });
  assert.deepEqual(strikeoutFieldingCredit('other', '1B'), { catcherPo: 0, catcherA: 0, fielderPo: 0 });
});

import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const SRC = join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');

test('LiveGame 接线：普通/擦棒三振与漏接三振均调用守备记账', () => {
  const src = fs.readFileSync(SRC, 'utf8');
  assert.equal((src.match(/creditStrikeoutFielding\("caught"\)/g) ?? []).length, 3, '普通/界外/擦棒三个三振分支都要记捕手 PO');
  assert.equal((src.match(/creditStrikeoutFielding\("dropped",t\)/g) ?? []).length, 1, '漏接三振一垒出局要按点选守备员分配 PO/A');
  assert.ok(src.includes('import { strikeoutFieldingCredit }'), '缺少纯函数导入');
});

test('LiveGame 接线定位：captured 分属 vo/Df/go，dropped 属 So', () => {
  const src = fs.readFileSync(SRC, 'utf8');
  const slice = (a, b) => {
    const i = src.indexOf(a); const j = src.indexOf(b, i);
    assert.ok(i >= 0 && j > i, `缺少区间 ${a}..${b}`);
    return src.slice(i, j);
  };
  assert.match(slice('function vo(', 'function Df('), /creditStrikeoutFielding\("caught"\)/);
  assert.match(slice('function Df(', 'function go('), /creditStrikeoutFielding\("caught"\)/);
  assert.match(slice('function go(', 'function Mf('), /creditStrikeoutFielding\("caught"\)/);
  assert.match(slice('function So(', 'function dk3ReachOnError('), /creditStrikeoutFielding\("dropped",t\)/);
});
