import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

function sliceBetween(start, end) {
  const from = source.indexOf(start);
  const to = source.indexOf(end, from + start.length);
  assert.notEqual(from, -1, `起始标记存在：${start}`);
  assert.notEqual(to, -1, `结束标记存在：${end}`);
  return source.slice(from, to);
}

const runnerOutReasonList = (() => {
  const code = sliceBetween('function Xd(', 'function Yd(u,f){');
  return new Function(`${code}; return Xd;`)();
})();

test('同垒拖拽出局菜单：未记录接杀时去掉离垒过远，加入触杀并接入传球链', () => {
  for (const base of [1, 2, 3]) {
    const reasons = runnerOutReasonList(base, base, false);
    assert.deepEqual(reasons.map((item) => item.id), ['out_pk', 'out_tag', 'out_oi', 'out_appeal', 'out_other']);
    const tagged = reasons.find((item) => item.id === 'out_tag');
    assert.equal(tagged.cn, '触杀出局');
    assert.equal(tagged.en, 'Tagged Out');
  }
});

test('同垒拖拽出局菜单：接杀回传可用时保留离垒过远，且仍可选触杀', () => {
  for (const base of [1, 2, 3]) {
    const reasons = runnerOutReasonList(base, base, true);
    assert.deepEqual(reasons.map((item) => item.id), ['out_pk', 'out_last', 'out_tag', 'out_oi', 'out_appeal', 'out_other']);
  }
});

test('接杀回传门禁：菜单与出局路由共用同半局 abCatchRecorded 判定', () => {
  assert.match(
    source,
    /function isCatchBackAvailable\(\)\{\s*return !!\(l\.abCatchRecorded&&l\.abCatchRecorded\.inning===l\.inning&&l\.abCatchRecorded\.half===l\.isTop\)/
  );
  assert.match(source, /return Xd\(e\.fromBase,e\.toBase,isCatchBackAvailable\(\)\)/);
  assert.match(source, /if\(e\.id==="out_last"&&isCatchBackAvailable\(\)\)\{/);
});

test('向前拖拽出局菜单：新增盗垒失败、移除独立 CS，触杀保留', () => {
  for (const [from, to] of [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]) {
    const ids = runnerOutReasonList(from, to, false).map((item) => item.id);
    assert.ok(ids.includes('out_steal_fail'), `${from}→${to} 提供盗垒失败`);
    assert.ok(ids.includes('out_tag'), `${from}→${to} 保留触杀出局`);
    assert.ok(!ids.includes('out_cs'), `${from}→${to} 不再提供独立 CS`);
  }
  const stealFail = runnerOutReasonList(3, 4, false).find((item) => item.id === 'out_steal_fail');
  assert.equal(stealFail.cn, '盗垒失败');
  assert.equal(stealFail.en, 'Steal Failed');
  assert.ok(!runnerOutReasonList(2, 1, false).some((item) => item.id === 'out_steal_fail'), '回退进垒不提供盗垒失败');
  assert.ok(!runnerOutReasonList(2, 2, false).some((item) => item.id === 'out_steal_fail'), '同垒不提供盗垒失败');
  for (const to of [2, 3, 4]) {
    assert.ok(!runnerOutReasonList(1, to, false).some((item) => item.id === 'out_last'), `1→${to} 无接杀不提供离垒过远`);
    assert.ok(runnerOutReasonList(1, to, true).some((item) => item.id === 'out_last'), `1→${to} 接杀回传可用时保留离垒过远`);
  }
  assert.ok(!source.includes('id:"out_cs"'), '拖拽菜单不再提供独立 CS 选项');
});

test('出局菜单不再受投球门禁，CI 跑者菜单同样提供盗垒失败', () => {
  assert.ok(!source.includes('if(tp.value)return'));
  assert.match(source, /return Xd\(e\.fromBase,e\.toBase,isCatchBackAvailable\(\)\)/);
  const kdCode = sliceBetween('function Kd(', 'var zd=');
  assert.ok(kdCode.includes('id:"out_steal_fail"'), 'Kd 提供盗垒失败');
  assert.ok(!kdCode.includes('out_cs'), 'Kd 不再提供独立 CS');
});

test('盗垒失败进入传球链：捕手首发校验 + 恰好一传记 CS', () => {
  const kpBlock = sliceBetween('function kp(e){', 'function Pt(){');
  assert.match(kpBlock, /if\(ge\.value=null,e\.id==="out_steal_fail"\)\{/);
  assert.match(kpBlock, /type:"RUNNER_OUT_TAG",runner:a,runnerBase:s,outAtBase:r,outType:"tag",stealFail:!0,steps:\[\]/);
  const acBlock = sliceBetween('function ac(e){', 'function lc(e){');
  assert.match(acBlock, /t\.stealFail&&t\.steps\.length===0&&e!=="C"/);
  assert.match(acBlock, /t\.stealFail&&t\.steps\.length>0&&t\.steps\[t\.steps\.length-1\]===e/, '连续重复点击同一守位不重复入链');
  const settleBlock = sliceBetween('function settleRunnerOut(e){', 'function ac(e){');
  assert.match(settleBlock, /isCs=!!e\.stealFail&&C1\[0\]==="C"&&C1\.length===2/);
  assert.match(settleBlock, /isCs&&\(a\.stats\.batting\.cs=\(a\.stats\.batting\.cs\?\?0\)\+1,paceCatcherStat\("catcherCs"\)\)/);
  assert.match(settleBlock, /reasonCn=isCs\?"盗垒死 \(CS\)"/);
});

test('撤销/重做/载入携带接杀标记，菜单门禁不因撤销失效', () => {
  assert.match(source, /lastPitchWasBall:!1,abCatchRecorded:null,hitSprayQueue:\[\]/, '初始 state 带 abCatchRecorded 键，历史帧不因缺键残留');
  assert.match(
    source,
    /Object\.prototype\.hasOwnProperty\.call\(e\.game,"abCatchRecorded"\)\|\|\(l\.abCatchRecorded=null\)/,
    '撤销/重做恢复时缺键归一化为 null'
  );
  assert.match(source, /l\.abCatchRecorded=null;\r?\n\s*const A=e\.teamsData;/, '载入现场时清空接杀标记');
});

test('触杀选项进入传球链：out_tag 路由 RUNNER_OUT_TAG 而非单行出局', () => {
  const kpBlock = sliceBetween('function kp(e){', 'function Pt(){');
  assert.match(kpBlock, /if\(e\.id==="out_tag"\|\|e\.id==="out_force"\)\{/);
  assert.match(kpBlock, /type:e\.id==="out_tag"\?"RUNNER_OUT_TAG":"RUNNER_OUT_FORCE"/);
  assert.match(kpBlock, /steps:\[\]/);
});
