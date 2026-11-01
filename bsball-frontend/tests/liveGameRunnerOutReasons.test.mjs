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

test('同垒拖拽出局菜单：去掉离垒过远，加入触杀并接入传球链', () => {
  for (const base of [1, 2, 3]) {
    const reasons = runnerOutReasonList(base, base);
    assert.deepEqual(reasons.map((item) => item.id), ['out_pk', 'out_tag', 'out_oi', 'out_appeal', 'out_other']);
    const tagged = reasons.find((item) => item.id === 'out_tag');
    assert.equal(tagged.cn, '触杀出局');
    assert.equal(tagged.en, 'Tagged Out');
  }
});

test('向前拖拽出局菜单：新增盗垒失败、移除独立 CS 与离垒过远，触杀保留', () => {
  for (const [from, to] of [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]) {
    const ids = runnerOutReasonList(from, to).map((item) => item.id);
    assert.ok(ids.includes('out_steal_fail'), `${from}→${to} 提供盗垒失败`);
    assert.ok(ids.includes('out_tag'), `${from}→${to} 保留触杀出局`);
    assert.ok(!ids.includes('out_cs'), `${from}→${to} 不再提供独立 CS`);
    assert.ok(!ids.includes('out_last'), `${from}→${to} 不再提供离垒过远`);
    assert.ok(!ids.includes('out_force'), `${from}→${to} 不再提供封杀出局`);
  }
  const stealFail = runnerOutReasonList(3, 4).find((item) => item.id === 'out_steal_fail');
  assert.equal(stealFail.cn, '盗垒失败');
  assert.equal(stealFail.en, 'Steal Failed');
  assert.ok(!runnerOutReasonList(2, 1).some((item) => item.id === 'out_steal_fail'), '回退进垒不提供盗垒失败');
  assert.ok(!runnerOutReasonList(2, 2).some((item) => item.id === 'out_steal_fail'), '同垒不提供盗垒失败');
  assert.ok(!source.includes('id:"out_last"'), '源码不再提供离垒过远选项');
  assert.ok(!source.includes('id:"out_force"'), '拖拽菜单不再提供封杀出局选项');
  assert.ok(!source.includes('id:"out_cs"'), '拖拽菜单不再提供独立 CS 选项');
  assert.ok(!source.includes('abCatchRecorded'), '接杀标记与接杀回传菜单入口已移除');
});

test('出局菜单不再受投球门禁，CI 跑者菜单同样提供盗垒失败', () => {
  assert.ok(!source.includes('if(tp.value)return'));
  assert.match(source, /return Xd\(e\.fromBase,e\.toBase\)/);
  const kdCode = sliceBetween('function Kd(', 'var zd=');
  assert.ok(kdCode.includes('id:"out_steal_fail"'), 'Kd 提供盗垒失败');
  assert.ok(!kdCode.includes('out_cs'), 'Kd 不再提供独立 CS');
  assert.ok(!kdCode.includes('out_last'), 'Kd 不再提供离垒过远');
});

test('盗垒失败进入传球链：捕手首发校验 + 恰好一传记 CS', () => {
  const kpBlock = sliceBetween('function kp(e){', 'function Pt(){');
  assert.match(kpBlock, /if\(ge\.value=null,e\.id==="out_steal_fail"\)\{/);
  assert.ok(kpBlock.includes('de(`拖拽-触杀出局(盗垒失败)[${s}>${r}]`)'), '盗垒失败历史动作名含触杀出局，撤销动画可识别');
  assert.match(kpBlock, /type:"RUNNER_OUT_TAG",runner:a,runnerBase:s,outAtBase:r,outType:"tag",stealFail:!0,steps:\[\]/);
  const acBlock = sliceBetween('function ac(e){', 'function lc(e){');
  assert.match(acBlock, /t\.stealFail&&t\.steps\.length===0&&e!=="C"/);
  assert.match(acBlock, /t\.stealFail&&t\.steps\.length>0&&t\.steps\[t\.steps\.length-1\]===e/, '连续重复点击同一守位不重复入链');
  const settleBlock = sliceBetween('function settleRunnerOut(e){', 'function ac(e){');
  assert.match(settleBlock, /isCs=!!e\.stealFail&&C1\[0\]==="C"&&C1\.length===2/);
  assert.match(settleBlock, /isCs&&\(a\.stats\.batting\.cs=\(a\.stats\.batting\.cs\?\?0\)\+1,paceCatcherStat\("catcherCs"\)\)/);
  assert.match(settleBlock, /reasonCn=isCs\?"盗垒死 \(CS\)"/);
});

test('触杀选项进入传球链：out_tag 路由 RUNNER_OUT_TAG 而非单行出局', () => {
  const kpBlock = sliceBetween('function kp(e){', 'function Pt(){');
  assert.match(kpBlock, /if\(e\.id==="out_tag"\)\{/);
  assert.ok(kpBlock.includes('de(`拖拽-触杀出局[${s}>${r}]`)'), '触杀历史动作名含出局，撤销动画可识别');
  assert.match(kpBlock, /type:"RUNNER_OUT_TAG",runner:a,runnerBase:s,outAtBase:r,outType:"tag",steps:\[\]/);
});
