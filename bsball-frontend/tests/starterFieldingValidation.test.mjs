// starterFieldingValidation 单测：EP 组合矩阵、extra_batter 门禁与既有口径回归
// 运行：node --test tests/starterFieldingValidation.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  validateStarterFielding as V, getRosterSizeRange as R, POSITION_ORDER,
  validateRosterPositions as Q, validateStarterRows as Z,
} from '../src/utils/starterFieldingValidation.js';

const F8 = ['C','1B','2B','3B','SS','LF','CF','RF'];
const B = (mode, extra, extraPos = []) => V([...F8, ...extraPos], mode, { extraBatter: extra });
const rowsOf = (...positions) => positions.map((position) => ({ position }));

test('棒球关闭：恰 9 两态通过，10 人（P+DH）被拒并提示未开放', () => {
  assert.equal(B('BASEBALL', false, ['P']).ok, true);
  assert.equal(B('BASEBALL', false, ['DH']).ok, true);
  const r = B('BASEBALL', false, ['P','DH']);
  assert.equal(r.ok, false);
  assert.match(r.message, /未开放 EP 制/);
  assert.match(r.message, /上限 9 人，当前 10 人/);
});

test('棒球开启：合法 11 组合（节选）', () => {
  const cases = [
    ['P'], ['DH'], ['P','DH'], ['P','EP'], ['DH','EP'],
    ['P','DH','EP'], ['P','EP','EP'], ['P','DH','EP','EP'], ['P','EP','EP','EP'],
  ];
  for (const extra of cases) assert.equal(B('BASEBALL', true, extra).ok, true, extra.join('+'));
});

test('棒球开启：非法组合被拒（无 P/DH、双 DH、EP×4、13 人）', () => {
  assert.equal(B('BASEBALL', true, ['EP']).ok, false);          // 无 P 无 DH
  assert.equal(B('BASEBALL', true, ['DH','DH']).ok, false);     // 双 DH
  assert.equal(B('BASEBALL', true, ['P','EP','EP','EP','EP']).ok, false); // EP×4
  assert.equal(B('BASEBALL', true, ['P','DH','EP','EP','EP']).ok, false); // 13 人
});

test('棒球开启：EP 可多人（不被判“重复”）；EP 不计入缺失守备位', () => {
  const r = B('BASEBALL', true, ['P','EP','EP']);
  assert.equal(r.ok, true);
  // 说明：原计划用例写为 B('BASEBALL', true, ['P','EP']) // 缺 RF 并断言被拒，与本文件「合法组合」
  // 用例（同表达式断言 ok:true）及设计稿 §五「10 人：P+8+EP 合法」自相矛盾；按该例本意
  //（缺 RF 时提示缺守备位、EP 不掺入缺失列表）落在 validateRosterPositions 层验证。
  const miss = Q(rowsOf('C','1B','2B','3B','SS','LF','CF','P','EP'), 'BASEBALL', { extraBatter: true }); // 缺 RF
  assert.equal(miss.ok, false);
  assert.match(miss.msg, /缺少守备位置/);
  assert.doesNotMatch(miss.msg, /EP/);
});

test('垒球：关闭仅 10，开启 10-11', () => {
  const S8 = ['C','1B','2B','3B','SS','LF','CF','RF'];
  assert.equal(V([...S8,'P','XF'], 'SOFTBALL', { extraBatter:false }).ok, true);
  const r11 = V([...S8,'P','DH','XF'], 'SOFTBALL', { extraBatter:false });
  assert.equal(r11.ok, false);
  assert.match(r11.message, /未开放 EP 制/);
  assert.equal(V([...S8,'P','DH','XF'], 'SOFTBALL', { extraBatter:true }).ok, true);
});

test('getRosterSizeRange 四态', () => {
  assert.deepEqual(R('BASEBALL', false), { min:9, max:9 });
  assert.deepEqual(R('BASEBALL', true), { min:9, max:12 });
  assert.deepEqual(R('SOFTBALL', false), { min:10, max:10 });
  assert.deepEqual(R('SOFTBALL', true), { min:10, max:11 });
});

test('POSITION_ORDER 守卫：不含 EP（EP 只进合法集，不进守备位序）', () => {
  assert.ok(!POSITION_ORDER.includes('EP'));
  assert.deepEqual([...POSITION_ORDER], ['C','1B','2B','3B','SS','LF','CF','RF']);
});

test('回归：不传 opts 默认关闭（fail-closed），棒球 9 两态 / 垒球 10 两态不变', () => {
  assert.equal(V([...F8,'P'], 'BASEBALL').ok, true);
  assert.equal(V([...F8,'DH'], 'BASEBALL').ok, true);
  const r10 = V([...F8,'P','DH'], 'BASEBALL');
  assert.equal(r10.ok, false);
  assert.match(r10.message, /未开放 EP 制/);
  assert.equal(Q(rowsOf(...F8,'P'), 'BASEBALL').ok, true);
  const S8 = ['C','1B','2B','3B','SS','LF','CF','RF'];
  assert.equal(Q(rowsOf(...S8,'P','XF'), 'SOFTBALL').ok, true);
  assert.equal(Q(rowsOf(...S8,'DH','XF'), 'SOFTBALL').ok, true);
});

test('Q/Z 开关透传：开启后 11-12 人阵通过；关闭时超限被拒且提示未开放', () => {
  assert.equal(Q(rowsOf(...F8,'P','EP','EP'), 'BASEBALL', { extraBatter: true }).ok, true);
  assert.equal(Q(rowsOf(...F8,'P','DH','EP','EP'), 'BASEBALL', { extraBatter: true }).ok, true);
  const r = Q(rowsOf(...F8,'P','EP'), 'BASEBALL'); // 10 人、默认关闭
  assert.equal(r.ok, false);
  assert.match(r.msg, /未开放 EP 制/);
  assert.equal(Z(rowsOf(...F8,'P'), '主队', 'BASEBALL').valid, true);
  const z10 = Z(rowsOf(...F8,'P','DH'), '主队', 'BASEBALL');
  assert.equal(z10.valid, false);
  assert.match(z10.message, /主队：该联赛未开放 EP 制/);
  assert.equal(Z(rowsOf(...F8,'P','DH','EP','EP'), '主队', 'BASEBALL', { extraBatter: true }).valid, true);
});
