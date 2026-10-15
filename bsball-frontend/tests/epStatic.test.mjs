// 静态断言：LiveGame 录入页数据容量 11→12（打线槽位/守卫/保存循环/恢复上限，EP 制 #274）
// 运行：node --test tests/epStatic.test.mjs
// 依据：docs/联赛特殊规则与EP制-设计-2026-10-15.md §六（槽位数组 11→12、越界守卫 >=12、保存循环 d<12、恢复补齐上限 棒 12/垒 11）
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('打线槽位容量：不得残留 11 槽数组/守卫（含多行 Array.from 展开形式）', () => {
  // 槽位数组本体（lineupStarterIds / slotSubHistory 播种、快照、规整 fallback）不得再有 11 槽
  assert.ok(!/length:\s*11\b/.test(source), '不得残留 length:11 的打线槽位数组（打线 1..12 需 index 0..11）');
  assert.ok(!source.includes('Array(11)'), '不得残留 Array(11).fill(null) 槽位播种');
  // 越界守卫上限（store/页面两处 recordSlotSubstitution）
  assert.ok(!/>=\s*11\b/.test(source), '越界守卫上限应为 >=12（原 pe>=11 / t>=11 已扩）');
  assert.ok(source.includes('pe<0||pe>=12'), '缺少 store recordSlotSubstitution 槽位守卫扩至 12');
  assert.ok(source.includes('t<0||t>=12'), '缺少页面 recordSlotSubstitution 槽位守卫扩至 12');
});

test('新增容量：槽位数组与保存循环按 12 槽落位', () => {
  assert.ok(source.includes('Array(12).fill(null)'), '缺少 12 槽 lineupStarterIds 播种');
  assert.ok(source.includes('length:12'), '缺少 12 槽 Array.from({length:12})');
  assert.ok(source.includes('d<12;'), '缺少保存循环 No() 的 d<12（打序 1..12 逐一落位）');
  assert.ok(source.includes('for(let d=12;'), '缺少保存尾部循环起点 d=12（超槽位行继续按散列落位）');
});

test('恢复补齐上限：棒球 12 / 垒球 11（绝对上限，不设开关门禁）', () => {
  assert.ok(source.includes('N.value==="SOFTBALL"?11:12'), '缺少恢复补齐上限 N.value==="SOFTBALL"?11:12');
  // 快照恢复 Vl 的补齐上限 z 按设计 §六「保持现状」（完整阵容 E.length>=z 时原样返回不裁剪）；
  // 此处固定当前口径防误改，若回归显示 12 人快照被裁剪需按设计放宽时，同步更新本断言。
  assert.ok(source.includes('z=N.value==="SOFTBALL"?10:9'), 'Vl 快照恢复补齐上限应保持 ?10:9（设计明示保持现状）');
});

test('剩余合法 11 用法白名单：仅限编译产物下标/常量，不得有打线槽位遗漏', () => {
  // 逐处读原文确认语义后保留的合法「11」（与本任务打线槽位无关，删除后本断言仍须成立）：
  // 1. key:11 —— 编译产物静态 vnode key（与 9/10/12/13… 顺序编号同列，是节点序号而非容量）
  // 2. i[11]/S[11]/t[11] —— 渲染函数内联缓存数组下标（与 i[110]…t[55] 同列，是缓存位置）
  // 3. H(...),11,kg —— createElementVNode 的 patchFlag 位掩码常量（TEXT|CLASS|PROPS 组合位）
  // 4. d.cy±d.r/2-11 —— 场上跑垒图标 SVG 坐标偏移（像素值）
  // 5. ?11:12 —— 本任务的目标值本身（垒球 11 / 棒球 12 的恢复补齐上限）
  const scrubbed = source
    .replace(/key:11,class:"pmg__pill"/g, '')
    .replace(/key:11,class:"lgpfm__scroll"/g, '')
    .replace(/i\[11\]\|\|\(i\[11\]/g, '')
    .replace(/S\[11\]\|\|\(S\[11\]/g, '')
    .replace(/t\[11\]\|\|\(t\[11\]/g, '')
    .replace(/\),11,/g, '')
    .replace(/y:d\.cy[+-]d\.r\/2-11/g, '')
    .replace(/\?11:12/g, '');
  const leftover = scrubbed.match(/(?<![\w$])11(?![\w$])/);
  if (leftover) {
    const around = scrubbed.slice(Math.max(0, leftover.index - 40), leftover.index + 40).replace(/\s+/g, ' ');
    assert.fail(`存在未在测试注释中说明合法性的 11 用法：…${around}…`);
  }
});
