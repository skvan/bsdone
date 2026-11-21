// 失误记账判定：同一 play 内“同一失误推进”不重复计 E（比赛 236 捕手失误漏记）
// 运行：node --test tests/liveGameErrorCredit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { shouldCreditError, filterErrorReasons, selectErrorFielder, normalizeErrorFlow } from '../src/utils/liveGameErrorCredit.js';

test('普通失误原因始终记账', () => {
  for (const reason of ['safe_te', 'safe_fe', 'safe_pk_err', 'safe_diff_err', undefined]) {
    assert.equal(shouldCreditError(reason, false), true);
    assert.equal(shouldCreditError(reason, true), true);
  }
});

test('同一play内推进不再补记 E（承接失误/暴投/捕逸）', () => {
  assert.equal(shouldCreditError('safe_same_err'), false);
  assert.equal(shouldCreditError('safe_same_err', true), false);
});

test('未记过失误/暴投/捕逸时，“同一play内失误推进”不出现在进垒原因里', () => {
  const reasons = [
    { id: 'safe_sb' },
    { id: 'safe_same_err' },
    { id: 'safe_te' }
  ];
  assert.deepEqual(filterErrorReasons(reasons, false).map((r) => r.id), ['safe_sb', 'safe_te']);
  assert.deepEqual(filterErrorReasons(reasons, true), reasons);
  assert.deepEqual(filterErrorReasons(reasons, false, true), reasons);
  assert.deepEqual(filterErrorReasons(reasons, false, false, true), reasons);
  assert.deepEqual(filterErrorReasons(null, false), null);
});

test('E 失误责任者默认单选：点其他守备员替换当前选择', () => {
  assert.deepEqual(selectErrorFielder([], '3B', false), ['3B']);
  assert.deepEqual(selectErrorFielder(['RF'], '3B', false), ['3B']);
  assert.deepEqual(selectErrorFielder(['RF', '3B'], 'SS', false), ['SS']);
  assert.deepEqual(selectErrorFielder(null, 'RF', false), ['RF']);
  assert.deepEqual(selectErrorFielder('RF', '3B'), ['3B'], '非数组按空列表处理，且默认单选');
});

test('显式多失误模式：追加、去重、再点取消', () => {
  const steps = ['RF'];
  assert.deepEqual(selectErrorFielder(steps, '3B', true), ['RF', '3B']);
  assert.deepEqual(steps, ['RF'], '不修改原数组');
  assert.deepEqual(selectErrorFielder([], '3B', true), ['3B'], '空列表可开始多选');
  assert.deepEqual(selectErrorFielder(['RF', '3B'], 'RF', true), ['3B']);
  assert.deepEqual(selectErrorFielder(['RF'], 'RF', true), []);
  assert.deepEqual(selectErrorFielder(null, 'RF', true), ['RF']);
});

test('旧 E flow 已有多名责任者但没有 multiError 时按多选恢复', () => {
  const oldFlow = { type: 'E', steps: ['RF', '3B'] };
  assert.equal(normalizeErrorFlow(oldFlow).multiError, true);
  const explicitlySingle = { type: 'E', steps: ['RF', '3B'], multiError: false };
  assert.equal(normalizeErrorFlow(explicitlySingle).multiError, false);
  assert.equal(normalizeErrorFlow({ type: 'E', steps: ['RF'] }).multiError, undefined);
  assert.equal(normalizeErrorFlow(null), null);
});

test('LiveGame 接线：E 默认单选、显式多失误与日志姓名', () => {
  const src = fs
    .readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  assert.ok(src.includes('selectErrorFielder('), 'E 流程未接入选人函数');
  assert.match(src, /type:e,steps:p\?\[p\]:\[\],selectRunner:!1,required:r,multiError:!1/, 'E 流程进入时未重置为单选');
  assert.match(src, /type:"E",steps:\[\],selectRunner:!1,multiError:!1,errorMode:"foul_error"/, '界外球+失误未默认单选');
  assert.match(src, /function toggleErrorMulti\(\)\{/, 'E 流程缺少多失误模式切换');
  assert.ok(src.includes('再记一名失误'), 'E 流程缺少显式多失误入口');
  assert.ok(src.includes('切回单选'), 'E 流程缺少切回单选入口');
  assert.ok(src.includes('normalizeErrorFlow(pe.flow'), '撤销恢复未归一化 E flow');
  assert.ok(src.includes('normalizeErrorFlow(l.flow)'), '历史快照恢复未归一化 E flow');
  assert.ok(src.includes('e=normalizeErrorFlow(e)'), '云端/流程恢复未归一化 E flow');
  const toggleStart = src.indexOf('function toggleErrorMulti(){');
  const toggleBody = src.slice(toggleStart, src.indexOf('function ds(', toggleStart));
  assert.ok(toggleStart >= 0, '缺少 toggleErrorMulti');
  assert.doesNotMatch(toggleBody, /e\.steps=\[e\.steps\[0\]\]/, '切回单选不得静默丢弃已选责任者');
  assert.ok(src.includes('const list=Array.isArray(fielders)?fielders:[];'), '姓名 helper 未防御非数组');
  for (const call of ['errorCreditNote(e.fielders)', 'errorCreditNote(e.steps)', 'errorCreditNote(t)', 'errorCreditNote([t.errorPos])']) {
    assert.ok(src.includes(call), `记 E 日志未输出姓名：${call}`);
  }
});

test('LiveGame 接线：使用判定/过滤函数、记 E 置位、投球/换局重置', () => {
  const src = fs
    .readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  const body = (start, end) => {
    const s = src.indexOf(start);
    const e = src.indexOf(end, s);
    assert.ok(s >= 0 && e > s, `缺少代码段 ${start}`);
    return src.slice(s, e);
  };

  assert.match(src, /shouldCreditError\(t\.advanceFeReasonId\)/, 'ADVANCE_FE 未接判定函数');
  assert.ok(src.includes('filterErrorReasons(s,l.playErrorCredited,l.playWpCredited,l.playPbCredited)'), 'CI 进垒原因未按 WP/PB 过滤');
  assert.ok(src.includes('filterErrorReasons(Wd('), '常规进垒原因未过滤');
  assert.ok(src.includes('l.playWpCredited,l.playPbCredited)'), '常规进垒原因未接 WP/PB 标记');
  assert.ok(src.includes('e.id==="safe_wp"&&!l.playWpCredited'), '暴投缺少同球去重');
  assert.ok(src.includes('kind:"wild_pitch"'), '拖拽暴投未写投球行');
  assert.ok(src.includes('e.id==="safe_pb"&&!l.playPbCredited'), '捕逸缺少同球去重');
  assert.ok(src.includes('kind:"passed_ball"'), '拖拽捕逸未写投球行');
  assert.ok(src.includes('canWpPb=!!l.hasPitchThisAB&&!l.playWpCredited&&!l.playPbCredited'), 'WP/PB 未按投球/同球门禁');
  assert.ok(src.includes('lastPitchWasBall'), '暴投未按最近一球是否坏球门禁');
  assert.ok(src.includes('l.playWpCredited=!0,l.flow=null'), '暴投投球未置位本 play 标记');
  assert.ok(src.includes('l.playPbCredited=!0'), '捕逸未置位本 play 标记');
  assert.ok(src.includes('l.playPbCredited=!1,l.playWpCredited=!0'), '暴投未清除捕逸标记（同 play 承接类型会串）');
  assert.ok(src.includes('l.playPbCredited=!0,l.playWpCredited=!1') || src.includes('l.playWpCredited=!1,l.playPbCredited=!0'), '捕逸未清除暴投标记（同 play 承接类型会串）');
  assert.ok(src.includes('const samePlayWpPb=e.id==="safe_same_err"'), '同一 play 承接 WP/PB 未走普通进垒');
  assert.ok(src.includes('advanceReasonId=samePlayWpPb'), '承接 WP/PB 未沿用 WP/PB 来源');
  assert.ok(src.includes('同一play内暴投推进') && src.includes('同一play内捕逸推进'), '承接 WP/PB 日志缺少来源');
  assert.ok(src.includes('zd.has(e.id)&&!samePlayWpPb'), '承接 WP/PB 仍进入失误守备员流程');
  assert.ok(src.includes('if(e==="WP"&&(l.playWpCredited||l.playPbCredited))'), '暴投事件菜单缺少同球去重');

  // 所有记 E 的位置统一走 creditFieldingError，由该函数置位本 play 标记
  assert.match(body('function creditFieldingError(player){', 'function Bt('), /playErrorCredited=!0/, '记 E 助手未置位标记');
  for (const call of ['creditFieldingError(r)', 'creditFieldingError(Vt(w))', 'creditFieldingError(Vt(t))', 'creditFieldingError(Vt(b))', 'creditFieldingError(Vt(fc_b))']) {
    assert.ok(src.includes(call), `记 E 位置未使用助手：${call}`);
  }

  // 投球/换局重置；换打席不得重置（BIP 失误后跑者仍在同一 play 内进垒）
  assert.match(body('function Ea(e=1){', 'function $q(){'), /playErrorCredited=!1,l\.playWpCredited=!1,l\.playPbCredited=!1/, '新投球未重置');
  assert.match(body('function Af(){', 'function Of(){'), /playErrorCredited=!1,l\.playWpCredited=!1,l\.playPbCredited=!1/, '换局未重置');
  assert.match(src, /O\.playErrorCredited=!1,O\.playWpCredited=!1,O\.playPbCredited=!1/, '新比赛/清空未重置');
  assert.doesNotMatch(body('function fo(){', 'function Af(){'), /playErrorCredited|playWpCredited|playPbCredited/, '换打席不应重置本 play 标记');

  // 快照恢复
  assert.match(src, /O\.playErrorCredited=pe\.playErrorCredited\?\?!1/, '历史快照恢复未还原标记');
  assert.match(src, /O\.playWpCredited=pe\.playWpCredited\?\?!1/, '历史快照恢复未还原暴投标记');
  assert.match(src, /O\.playPbCredited=pe\.playPbCredited\?\?!1/, '历史快照恢复未还原捕逸标记');
  assert.match(src, /playErrorCredited:!1,playWpCredited:!1,playPbCredited:!1/, '初始状态缺少标记');
});

// #697：同一 play 已记失误时，「同一play内失误推进」只负责跑者进垒，不再二次点选失误守备员。
test('LiveGame 接线：#697 同一play内失误推进直接完成进垒', () => {
  const src = fs
    .readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  const body = (start, end) => {
    const s = src.indexOf(start);
    const e = src.indexOf(end, s);
    assert.ok(s >= 0 && e > s, `缺少代码段 ${start}`);
    return src.slice(s, e);
  };

  const spBody = body('function Sp(e){', 'function kp(e){');
  assert.match(spBody, /if\(e\.id==="safe_same_err"&&l\.playErrorCredited\)\{/, '缺少已记失误时直接进垒的分支');
  const shortCircuit = spBody.slice(
    spBody.indexOf('if(e.id==="safe_same_err"&&l.playErrorCredited)'),
    spBody.indexOf('if(zd.has(e.id)&&!samePlayWpPb)')
  );
  assert.ok(shortCircuit.length > 0, '直接进垒分支应位于原失误流程之前');
  assert.ok(shortCircuit.includes('advanceFeStep:"pick_target"'), '不应再进入点人步骤');
  assert.ok(!shortCircuit.includes('pick_fielder'), '已记失误时不得进入 pick_fielder');
  assert.ok(!shortCircuit.includes('errorPos'), '不再传递失误位置');
  assert.ok(!shortCircuit.includes('J(lu('), '不再提示点选失误责任守备员');
  assert.ok(shortCircuit.includes(',lc(r);'), '应直接用拖拽目标垒完成进垒');
  assert.ok(!shortCircuit.includes('creditFieldingError'), '直接进垒不得再记失误（E 不重复计）');
  assert.ok(!shortCircuit.includes('errorCreditNote'), '直接进垒不得追加失误说明');

  // 其他 zd 原因与 WP/PB 承接保持原状
  assert.ok(spBody.includes('if(zd.has(e.id)&&!samePlayWpPb){'), '其他失误原因仍走点人流程');
  assert.ok(spBody.includes('J(lu(e.id));'), '其他失误原因仍提示点选责任守备员');
  assert.ok(spBody.includes('const samePlayWpPb=e.id==="safe_same_err"&&!l.playErrorCredited'), 'WP/PB 承接分支被改动');
  assert.ok(spBody.includes('同一play内暴投推进') && spBody.includes('同一play内捕逸推进'), 'WP/PB 承接日志来源缺失');

  // lc：无失误位置时放行 safe_same_err，日志复用菜单文案、不拼守备位置
  const lcBody = body('function lc(e){', 'function ic(){');
  assert.ok(lcBody.includes('t.errorPos==null&&t.advanceFeReasonId!=="safe_same_err"'), '无 errorPos 时未放行 safe_same_err');
  assert.ok(lcBody.includes('t.errorPos==null?(t.advanceFeReasonCn??"失误推进")'), '无位置日志未复用菜单文案（不得拼守备位置）');
  assert.ok(lcBody.includes(':iu(t.advanceFeReasonId,y)'), '有位置日志未保留原失误明细');
  assert.ok(lcBody.includes('Je("ADVANCE_FE"'), '进垒事件类型应保持 ADVANCE_FE');
  assert.ok(lcBody.includes('t.errorPos!=null?It[t.errorPos]??t.errorPos:null'), '有位置路径未保留原失误明细');

  // 上版按位置复用的状态必须已彻底移除
  assert.ok(!src.includes('playErrorPos'), '不应保留 playErrorPos 状态');

  // 英文文案复用既有映射，不新增 i18n key
  const logTexts = fs
    .readFileSync(new URL('../src/utils/liveGameLogTexts.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  assert.match(logTexts, /"同一play内失误推进":\s*"Advanced on same-play error"/, '缺少英文映射：同一play内失误推进');
  assert.match(logTexts, /"同一失误推进":\s*"Advanced on same error"/, '缺少英文映射：同一失误推进');
});
