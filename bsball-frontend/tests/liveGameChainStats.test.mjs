// 双杀/三杀链守备统计精细化——单测矩阵（设计稿 §二 + LiveGame 接入点静态断言）
// 运行：node --test tests/liveGameChainStats.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { computeChainFieldingStats, normalizeChainForDisplay, chainReadyForSettle } from '../src/utils/liveGameChainStats.js';

test('DP 标准链：6-3（[SS,1B]）与 6-4-3（[SS,2B,1B]）', () => {
  assert.deepEqual(computeChainFieldingStats('DP', ['SS', '1B']), [
    { pos: 'SS', po: 1, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
  assert.deepEqual(computeChainFieldingStats('DP', ['SS', '2B', '1B']), [
    { pos: 'SS', po: 0, a: 1 },
    { pos: '2B', po: 1, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
});

test('DP 自踩链：6-6-3（[SS,SS,1B]）与四步尾自踩（[SS,2B,1B,1B]）及自踩+两传（[SS,SS,2B,1B]）', () => {
  assert.deepEqual(computeChainFieldingStats('DP', ['SS', 'SS', '1B']), [
    { pos: 'SS', po: 1, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
  assert.deepEqual(computeChainFieldingStats('DP', ['SS', '2B', '1B', '1B']), [
    { pos: 'SS', po: 0, a: 1 },
    { pos: '2B', po: 1, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
  assert.deepEqual(computeChainFieldingStats('DP', ['SS', 'SS', '2B', '1B']), [
    { pos: 'SS', po: 1, a: 1 },
    { pos: '2B', po: 0, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
});

test('DP 独力：未压缩 [1B,1B] 与结算压缩后 [1B] 同结果（PO×2）', () => {
  assert.deepEqual(computeChainFieldingStats('DP', ['1B', '1B']), [{ pos: '1B', po: 2, a: 0 }]);
  assert.deepEqual(computeChainFieldingStats('DP', ['1B']), [{ pos: '1B', po: 2, a: 0 }]);
});

test('TP：标准三传链全点位（[SS,2B,1B] → 每人 PO+A，末位 PO）与独力（PO×3）', () => {
  assert.deepEqual(computeChainFieldingStats('TP', ['SS', '2B', '1B']), [
    { pos: 'SS', po: 1, a: 1 },
    { pos: '2B', po: 1, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
  assert.deepEqual(computeChainFieldingStats('TP', ['SS']), [{ pos: 'SS', po: 3, a: 0 }]);
  assert.deepEqual(computeChainFieldingStats('TP', ['SS', 'SS', '2B', '1B']), [
    { pos: 'SS', po: 1, a: 1 },
    { pos: '2B', po: 1, a: 1 },
    { pos: '1B', po: 1, a: 0 },
  ]);
});

test('边界：空链/空值返回空；非 DP/TP 返回 null（调用方回退原 Dn）', () => {
  assert.deepEqual(computeChainFieldingStats('DP', []), []);
  assert.deepEqual(computeChainFieldingStats('DP', undefined), []);
  assert.equal(computeChainFieldingStats('G', ['SS', '1B']), null);
  assert.equal(computeChainFieldingStats('FC', ['SS', '1B']), null);
  assert.equal(computeChainFieldingStats('CATCH_BACK_DP', ['P', 'SS']), null);
  assert.equal(computeChainFieldingStats('F', ['CF']), null);
});

test('DP/TP 显示链规整：相邻重复合并（独力全同保留）', () => {
  assert.deepEqual(normalizeChainForDisplay('DP', ['SS', 'SS', '1B']), ['SS', '1B']);
  assert.deepEqual(normalizeChainForDisplay('DP', ['SS', '1B']), ['SS', '1B']);
  assert.deepEqual(normalizeChainForDisplay('DP', ['1B', '1B']), ['1B', '1B']);
  assert.deepEqual(normalizeChainForDisplay('DP', ['SS', '2B', '1B', '1B']), ['SS', '2B', '1B']);
  assert.deepEqual(normalizeChainForDisplay('TP', ['SS', 'SS']), ['SS', 'SS']);
  assert.deepEqual(normalizeChainForDisplay('G', ['SS', '1B']), ['SS', '1B']);
  assert.deepEqual(normalizeChainForDisplay('DP', []), []);
});

test('链完成判定：TP 三人口径（2026-10-08 裁决）', () => {
  assert.equal(chainReadyForSettle('TP', ['SS', '2B', '1B'], 4), true);
  assert.equal(chainReadyForSettle('TP', ['3B', '3B', '2B', '1B'], 4), true);
  assert.equal(chainReadyForSettle('TP', ['SS', 'SS', '1B'], 4), false);
  assert.equal(chainReadyForSettle('TP', ['SS', 'SS'], 4), false);
  assert.equal(chainReadyForSettle('DP', ['SS', '1B'], 2), true);
  assert.equal(chainReadyForSettle('DP', ['SS'], 2), false);
  assert.equal(chainReadyForSettle('G', ['SS', '1B'], 2), true);
  assert.equal(chainReadyForSettle('G', ['SS'], 2), false);
});

test('批C 三杀门槛接入点（静态断言，防回退）', () => {
  const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
  assert.match(src, /chainReadyForSettle as LgChainReady/, '导入 LgChainReady');
  assert.match(src, /if\(LgChainReady\(e\.type,e\.steps\?\?\[\],T0\)\)return/, '提示“已可完成”判定接入');
  assert.match(src, /a&&isChainFlow\(a\)&&LgChainReady\(a\.type,a\.steps\?\?\[\],a\.required\?\?us\(a\.type\)\)/, 'Wf 自动结算判定接入');
  assert.match(src, /if\(!LgChainReady\(e\.type,e\.steps\?\?\[\],e\.required\?\?us\(e\.type\)\)\)return!1;/, 'settle 完成门槛接入');
  assert.match(src, /LgChainReady\(c\(l\)\.flow\.type,c\(l\)\.flow\.steps\?\?\[\],c\(l\)\.flow\.required\?\?us\(c\(l\)\.flow\.type\)\)/, '模板完成按钮判定接入');
});

test('LiveGame.js 接入点（静态断言，防回退）', () => {
  const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
  assert.ok(src.includes("import { computeChainFieldingStats as LgChainStats, normalizeChainForDisplay as LgChainDisplay, chainReadyForSettle as LgChainReady } from '../../utils/liveGameChainStats';"), '导入新模块（含显示规整+完成判定）');
  assert.match(src, /function applyChainFieldingStats\(type,fielders\)\{/, 'fp 统计分流函数存在');
  assert.match(src, /a\.stats\.batting\.ab=\(a\.stats\.batting\.ab\?\?0\)\+1,applyChainFieldingStats\(e\.playType,e\.fielders\)/, 'fp 通道已接入分流（不再直接 Dn(e.fielders)）');
  assert.match(src, /applyChainFieldingStats\("DP",t\)/, 'bt DP 回退通道已接入分流');
  assert.match(src, /applyChainFieldingStats\("TP",t\)/, 'bt TP 回退通道已接入分流');
  assert.match(src, /if\(!s\)\{\r?\n\s+Dn\(fielders\);/, 'apply 回退分支调用 Dn(fielders)');
  assert.ok(!src.match(/Dn\(t\),g="DP"/), 'bt 的 DP/TP 回退未回退为旧 Dn');
  assert.match(src, /function chainDisp\(u,f\)\{\r?\n\s+return u==="DP"\|\|u==="TP"\?LgChainDisplay\(u,f\):_i\(f\)/, 'chainDisp 类型化显示');
  assert.match(src, /D0=chainDisp\(e\.type,e\.steps\?\?\[\]\)/, '已完成提示去压缩接入');
  assert.match(src, /const l=u==="DP"\|\|u==="TP"\?LgChainDisplay\(u,f\):_i\(f\)/, 'Un 对 DP/TP 走显示规整');
  assert.match(src, /const a=e\.type==="DP"\|\|e\.type==="TP"\?e\.steps:_i\(e\.steps\),s=a\[a\.length-1\]/, 'settle 对 DP/TP 保持原始链');
  assert.match(src, /includes\(e\)&&e!=="DP"&&e!=="TP"&&\(t=_i\(t\)\)/, 'bt 对 DP/TP 跳过压缩');
});

test('批B 拖拽自踩接入点（静态断言，防回退）', () => {
  const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
  assert.match(src, /_st\[_st\.length-1\]===e/, 'Qf 链尾持球者判定');
  assert.match(src, /function chainSelfStep\(e\)\{/, 'chainSelfStep 存在');
  assert.match(src, /function hlBaseNear\(e,t\)\{/, 'hlBaseNear 垒包邻近判定');
  assert.match(src, /key:"force",label:"封杀出局"/, '封杀/触杀弹层选项');
  assert.match(src, /Array\.isArray\(g\.stepActions\)\|\|\(g\.stepActions=new Array\(g\.steps\.length-1\)\.fill\(null\)\)/, 'stepActions 对齐补 null');
  assert.match(src, /g\.stepActions\.push\(p\)/, 'stepActions 写入');
  assert.match(src, /key:`ssb-\$\{c\(hh\)\}`/, '垒包高亮渲染');
  assert.match(src, /class:"selfstep-base-hl"/, '垒包高亮 class');
  assert.match(src, /a\.steps\.push\(e\),Array\.isArray\(a\.stepActions\)&&a\.stepActions\.push\(null\),pt\(\);/, '点选 push 成对补位（E 链）');
  assert.match(src, /a\.steps\.push\(e\),Array\.isArray\(a\.stepActions\)&&a\.stepActions\.push\(null\),ru\.has/, '点选 push 成对补位（主链）');
  assert.match(src, /t\.steps\.push\(e\),Array\.isArray\(t\.stepActions\)&&t\.stepActions\.push\(null\),t\.steps\.length===1\?/, '点选 push 成对补位（跑者出局链）');
  assert.match(src, /if\(\(r\.steps\?\.length\?\?0\)>=hu\)\{/, '拖拽自踩 hu 上限守卫');
  assert.match(src, /if\(g!==r\|\|!ho\.has\(g\.type\)\)return;/, '弹层回调同实例校验');
  assert.match(src, /window\.addEventListener\("blur",Yh,\{/, '链模式拖拽 blur 兑底挂载');
  assert.match(src, /function Yh\(\)\{/, 'blur 清理函数');
  assert.match(src, /b>\(t\.mode==="chain"\?10:5\)/, '链模式拖动阈值 10px');
  assert.match(src, /tSuppressClick=0,t&&t\.stopPropagation/, 'tSuppressClick 一次性消费');
  const css = fs.readFileSync(new URL('../src/styles/legacy/live-game.css', import.meta.url), 'utf8');
  assert.ok(css.includes('.selfstep-base-hl[data-v-78b7b6a8]{'), '垒包高亮样式');
});
