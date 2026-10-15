// 静态断言：LiveGame 录入页数据容量 11→12（打线槽位/守卫/保存循环/恢复上限，EP 制 #274）
// 追加（T12，#275/#274）：force_half 显隐（event.specialRules 捕获 + 强制换局按钮 v-if）与录入页校验接入（extraBatter 开关、仅守备投手判定对齐 T8）
// 追加（终评审修复）：模板页 league-less 调用点显式传参（LineupTemplates.js）+ 仅守备投手清理 watcher 去 t.length!==9 长度门禁（LiveGame.js）
// 运行：node --test tests/epStatic.test.mjs
// 依据：docs/联赛特殊规则与EP制-设计-2026-10-15.md §六（槽位数组 11→12、越界守卫 >=12、保存循环 d<12、恢复补齐上限 棒 12/垒 11；行1 force_half 显隐 / 行6 仅守备投手判定）、§4.4
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

// ===== T12 追加：force_half 显隐 + 录入页校验接入规则 =====

test('事件加载点捕获 specialRules：forceHalf 缺省 true（!==false）/ extraBatter 缺省 false（===true）', () => {
  // 状态名用 forceHalfOn/extraBatterOn：`forceHalf` 裸名已被点击处理函数占用（async function forceHalf）
  assert.ok(source.includes('const forceHalfOn=x(!0),extraBatterOn=x(!1)'), '缺少规则状态初始化（缺省 forceHalf=true / extraBatter=false，页面直载兜底）');
  assert.ok(source.includes('forceHalfOn.value='), '缺少 event 加载点对 forceHalf 的响应式落值');
  assert.ok(source.includes('?.specialRules?.forceHalf!==!1'), 'forceHalf 语义应为 !==false（缺省 true：未挂联赛/无记录时保持现状显示）');
  assert.ok(source.includes('extraBatterOn.value='), '缺少 event 加载点对 extraBatter 的响应式落值');
  assert.ok(source.includes('?.specialRules?.extraBatter===!0'), 'extraBatter 语义应为 ===true（缺省 false：fail-closed）');
});

test('force_half 显隐：强制换局按钮挂 forceHalfOn 条件渲染（v-if 编译形态，其余渲染/disabled 逻辑不动）', () => {
  // 齿轮面板「垒况纠偏/得分纠偏」popover 内第三个按钮：v-if → cond?(h(),Oe(btn)):createCommentVNode
  assert.ok(
    /c\(forceHalfOn\)\?\(h\(\),Oe\(s,\{\s+key:0,size:"small",type:"danger"[\s\S]{0,400}?Q\(" 强制换局 ",-1\)/.test(source),
    '强制换局按钮未挂 forceHalfOn 条件渲染（条件、按钮 props 与文案须在同一 vnode 分支内）'
  );
  assert.ok(source.includes(',8,["disabled"])):W("",!0)])])]),_:1'), '强制换局按钮 v-if 假分支（注释节点）或弹层容器闭合结构漂移');
  assert.ok(!source.includes(',8,["disabled"])])])]),_:1'), '强制换局按钮不得退回无条件渲染（v-if 条件被移除）');
});

test('录入页校验接入规则：Er() 透传 extraBatter 开关，仅守备投手判定对齐 T8 口径', () => {
  // 第二参（mode）保持现状不传（undefined → 棒球口径），本任务仅接入开关
  assert.ok(source.includes('sd(e,void 0,{extraBatter:extraBatterOn.value})'), 'Er() 未把 extraBatter 开关传入 validateRosterPositions');
  // 判定对齐 useLineupBoard.fieldingPitcherRequired（T8）：删「行数=9」硬编码，补「未指定守备投手」
  assert.ok(source.includes('Yp=$(()=>Nr.value.ok&&!oa(X.value)&&O.value==null)'), 'Yp 应为「校验通过且打线无 P 且未指定守备投手」');
  assert.ok(source.includes('Kp=$(()=>Hr.value.ok&&!oa(q.value)&&te.value==null)'), 'Kp 应为「校验通过且打线无 P 且未指定守备投手」');
  assert.ok(!source.includes('X.value.length===9&&Nr.value.ok'), 'Yp 不得保留「行数=9 且校验通过且无 P」旧判定');
  assert.ok(!source.includes('q.value.length===9&&Hr.value.ok'), 'Kp 不得保留「行数=9 且校验通过且无 P」旧判定');
});

// ===== 终评审修复：模板页 league-less 开关对齐 + 仅守备投手清理守卫 =====

test('模板页 league-less：validateRosterPositions 调用点显式传 {extraBatter:true}（保持改动前 9-10 两态）', () => {
  const tpl = fs.readFileSync(new URL('../src/views/admin/LineupTemplates.js', import.meta.url), 'utf8');
  // ne computed（第 10 条「指定打击」行会凑出 10 位置）→ 缺省 fail-closed 上限 9 会稳定误报「未开放 EP 制」横幅
  assert.ok(tpl.includes('yl(t,void 0,{extraBatter:!0})'), '模板页 ne 校验须显式传 {extraBatter:true}（不可依赖缺省）');
  assert.ok(!tpl.includes('yl(t)'), '不得保留裸调用 yl(t)：缺省 fail-closed 会误报「未开放 EP 制」横幅');
  // 其余 validate* 调用点审计结论（保持现状，无需传参）：Ze=validateStarterFielding 仅 bl/gl 两处，
  // 各带 b.length!==9 前置守卫（恒 9 人输入）；hl 亦为 9 人守卫。缺省上限 9 即改动前语义。
  assert.equal((tpl.match(/Ze\(/g) ?? []).length, 2, 'Ze 调用点应仍为 bl/gl 两处（各带 9 人守卫）');
  assert.equal((tpl.match(/b\.length!==9/g) ?? []).length, 3, 'bl/gl/hl 的 9 人守卫不得移除');
});

test('仅守备投手清理 watcher：不得按打线长度跳过（>9 阵含 P 时旧仅守备投手身份同样失效）', () => {
  assert.ok(!source.includes('t.length!==9||!Nr.value.ok'), 'Yp watcher 不得保留 t.length!==9 长度门禁（10-12 阵含 P 不清理）');
  assert.ok(!source.includes('t.length!==9||!Hr.value.ok'), 'Kp watcher 不得保留 t.length!==9 长度门禁');
  assert.ok(source.includes('!Nr.value.ok||oa(t)&&O.value!=null&&(O.value=null)'), '缺少 Yp watcher 跨长度清理分支');
  assert.ok(source.includes('!Hr.value.ok||oa(t)&&te.value!=null&&(te.value=null)'), '缺少 Kp watcher 跨长度清理分支');
});
