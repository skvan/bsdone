// 静态接线：换人模式守备轮转速记栏（#478 扩展）
// 运行：node --test tests/liveSubRotationStatic.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const LIVE = read('../src/views/admin/LiveGame.js');
const CSS = read('../src/styles/legacy/live-game.css');
// 切片两端必须命中，避免 indexOf -1 导致断言假通过
const sliceBetween = (src, start, end) => {
  const i = src.indexOf(start);
  assert.ok(i >= 0, `缺少起始标记：${start}`);
  const j = src.indexOf(end, i + start.length);
  assert.ok(j > i, `缺少结束标记：${end}`);
  return src.slice(i, j);
};

test('横幅接线：速记输入框 + 轮转按钮，value 绑定带 patchFlag 以便成功后清空', () => {
  const banner = sliceBetween(LIVE, 'key:"sub-mode-banner"', 'key:"dh-card-"');
  assert.ok(banner.includes('class:"live-sub-rotation__input"'), '缺少速记输入框');
  assert.ok(banner.includes('placeholder:lgText("liveGame.sub.rotationPlaceholder")'), '缺少输入提示');
  assert.ok(banner.includes('event.key==="Enter"&&(event.preventDefault(),event.stopPropagation(),applyFieldRotation())'), 'Enter 未接轮转执行');
  assert.ok(banner.includes('onClick:applyFieldRotation'), '缺少轮转按钮接线');
  assert.ok(banner.includes(',null,8,["value"]'), '输入框 value 缺少 patchFlag（成功清空会失效）');
});

test('执行接线：纯函数解析/占位校验/置换表，含 DH 取消确认与 busy 门禁', () => {
  const movesBody = sliceBetween(LIVE, 'function applyFieldRotationMoves(side,moves){', 'async function applyFieldRotation(){');
  assert.ok(movesBody.includes('if(moves.some(move=>!byId.has(Number(move.id))))return!1;'), '缺少球员归属校验');
  assert.ok(movesBody.includes('syncFieldingPitcher(side,oldPitcher,byId.get(Number(intoPitcher.id)))'), '投手变更未同步守备投手行');

  const applyBody = sliceBetween(LIVE, 'async function applyFieldRotation(){', '/* ---------------- 替补席拖拽换人');
  assert.ok(applyBody.includes('parseFieldRotation(rotationDraft.value,lgText)'), '未使用纯函数解析');
  assert.ok(applyBody.includes('buildFieldRotationMoves(parsed.digits,rotationTeamEntries(side),lgText)'), '未使用占位校验/置换表');
  assert.ok(applyBody.includes('liveGame.sub.confirmRotation'), '含投手+DH 未走取消 DH 确认');
  assert.ok(applyBody.includes('if(!applyCancelDh(side))return;'), '未复用取消 DH 实现');
  assert.ok(applyBody.includes('hasDh&&includesPitcher&&hasPitcherInOrder&&N.value!=="SOFTBALL"'), 'DH 队已有 P 时仅棒球拦截（垒球 11 人为普通轮转）');
  assert.ok(applyBody.includes('hasDh&&includesPitcher&&!hasPitcherInOrder'), '仅投手不在打序且轮转含 1 号位时走取消 DH 确认');
  assert.ok(applyBody.includes('if(substitutionModeBusy.value){'), '缺少提交进行中门禁');

  const enterBody = sliceBetween(LIVE, 'function enterSubstitutionMode(){', 'function toggleSubstitutionMode(){');
  assert.ok(enterBody.includes('rotationDraft.value=""'), '进入换人模式未清空上次速记');
});

test('样式门禁：速记栏/输入框/移动端宽度', () => {
  for (const cls of ['.live-sub-rotation{', '.live-sub-rotation__input{', '.live-sub-rotation__input:focus{']) {
    assert.ok(CSS.includes(cls), `缺少样式：${cls}`);
  }
  assert.match(CSS, /@media \(width<=1279px\)\{\.live-sub-rotation__input\{/, '缺少小屏输入框适配');
});
