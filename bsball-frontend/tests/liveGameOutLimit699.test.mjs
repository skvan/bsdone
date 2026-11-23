// #699 防守局数 4.1 异常：出局上限、拖拽出局闸门、保存前对账接线的静态校验
// 运行：node --test tests/liveGameOutLimit699.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const liveGame = fs.readFileSync(path.join(root, 'src/views/admin/LiveGame.js'), 'utf8');
const fieldingRows = fs.readFileSync(path.join(root, 'src/utils/gameFieldingRows.js'), 'utf8');

function sliceBetween(source, startToken, endToken) {
  const start = source.indexOf(startToken);
  assert.ok(start >= 0, `缺少起点 ${startToken}`);
  const end = source.indexOf(endToken, start + startToken.length);
  assert.ok(end > start, `缺少终点 ${endToken}`);
  return source.slice(start, end);
}

test('#699 出局账本入口引入剩余出局/截断工具', () => {
  assert.ok(liveGame.includes('outsRemainingInHalfInning as LgOutsRemaining'), '引入本半局剩余出局');
  assert.ok(liveGame.includes('creditOutsWithinLimit as LgCreditOutsWithinLimit'), '引入事件出局截断');
});

test('#699 Bt：先按剩余出局判断，账本照记但只在允许时累计守备局数', () => {
  const body = sliceBetween(liveGame, 'function Bt(e,t){', 'function os(e,t,a,s){');
  assert.ok(body.includes('const allowed=LgOutsRemaining(l.outs)'), '读取累计前剩余出局');
  assert.ok(body.includes('l.outs=Math.min(3,l.outs+1)'), '半局出局数不超过 3');
  assert.ok(body.includes('l.halfInningOuts.push'), '出局事件照常入账');
  assert.ok(body.includes('allowed>0?Ipy()'), '仅在允许的出局数内累计局数');
  assert.ok(body.includes('未计入守备局数'), '被截断时提示记录员');
  assert.ok(body.indexOf('D(jr(e??"出局")') > body.indexOf('Ipy()'), '日志/事件仍在记账之后执行，不跳过');
});

test('#699 os：多出局事件按剩余出局数截断，且只提示一次', () => {
  const body = sliceBetween(liveGame, 'function os(e,t,a,s){', 'function Dn(e){');
  assert.ok(body.includes('LgCreditOutsWithinLimit(e,l.outs)'), '按剩余出局截断本次事件的出局数');
  assert.ok(body.includes('suppressOutLimitWarning:!0'), '内层 Bt 不重复提示');
  assert.ok(body.includes('allowed<e&&J('), '截断时统一提示记录员');
  assert.ok(body.includes('l.outs=Math.min(3,l.outs)'), '出局数兜底不超过 3');
  // 双杀/三杀/野选调用点仍沿用 os，不丢事件
  assert.ok(liveGame.includes('os(2,Gt(r,"双杀")'), '双杀仍走 os(2)');
  assert.ok(liveGame.includes('os(3,Gt(r,"三杀")'), '三杀仍走 os(3)');
  assert.ok(liveGame.includes('os(y,Gt(a,b)'), '野选出局仍走 os');
  // 逐出局结算链路（applyDoublePlay）保留三出局断点，配合 Bt 上限不再多记
  assert.ok(/for\(const out of preview\.outs\)\{[\s\S]*?if\(l\.outs>=3\)break;/.test(liveGame), '双杀链在三出局处停止记账');
});

test('#699 拖拽出局闸门：三出局/等待打者/比赛结束时不改状态', () => {
  const body = sliceBetween(liveGame, 'function kp(e){', 'function Pt(){');
  const gate = body.indexOf('if(l.outs>=3||l.needNextBatter||l.isGameEnd)');
  assert.ok(gate >= 0, '存在拖拽出局闸门');
  assert.ok(gate < body.indexOf('e.id==="out_steal_fail"'), '闸门先于各出局分支');
  assert.ok(gate < body.indexOf('de(`拖拽-'), '闸门先于撤销快照与状态改动');
  const gateBody = body.slice(gate, body.indexOf('if(ge.value=null,e.id==="out_steal_fail")'));
  assert.ok(gateBody.includes('ge.value=null'), '关闭拖拽菜单');
  assert.ok(gateBody.includes('return'), '拦截后续记账');
  assert.ok(gateBody.includes('J(') && gateBody.includes('"warning"'), '提示记录员');
});

test('#699 保存前对账覆盖替补与 DH 制投手行', () => {
  const body = sliceBetween(liveGame, 'function reconcileFieldingInningsForLiveState(reason){', 'function paceGoFo(e,t){');
  assert.ok(body.includes('const ctx=Ee(side)'), '按攻守侧取阵容上下文');
  assert.ok(body.includes('ctx.bench.value'), '替补球员的 byPos 一并收敛');
  assert.ok(body.includes('ctx.fpRow.value'), 'DH 制投手行一并收敛');
  assert.ok(body.includes('extraPlayers'), '对账工具接收场外球员');
});

test('#699 赛后 Totals INN 复用出局求和', () => {
  assert.ok(fieldingRows.includes('totalInningsPitched'), '引入出局求和工具');
  const body = sliceBetween(fieldingRows, 'export function buildFieldingTotals(rows, fields = []) {', 'export function buildFieldingSummaryCells');
  assert.ok(body.includes('totalInningsPitched(list.map'), 'defInn 逐行按出局求和');
  assert.ok(!body.includes('Math.max(max, Number(row?.defInn)'), '不再取全队最大守备局数');
});
