import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { computeDoublePlayParticipants } from '../src/utils/liveGameChainStats.js';

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

test('同垒拖拽出局菜单：去掉离垒过远，加入触杀/封杀并接入传球链', () => {
  for (const base of [1, 2, 3]) {
    const reasons = runnerOutReasonList(base, base);
    assert.deepEqual(reasons.map((item) => item.id), ['out_pk', 'out_tag', 'out_force', 'out_oi', 'out_appeal', 'out_other']);
    assert.equal(reasons.find((item) => item.id === 'out_tag').cn, '触杀出局');
    const forced = reasons.find((item) => item.id === 'out_force');
    assert.equal(forced.cn, '封杀出局');
    assert.equal(forced.en, 'Force Out');
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
  assert.ok(!runnerOutReasonList(2, 1).some((item) => item.id === 'out_force'), '回退进垒不提供封杀');
  assert.ok(!runnerOutReasonList(2, 2).some((item) => item.id === 'out_steal_fail'), '同垒不提供盗垒失败');
  assert.ok(!source.includes('id:"out_last"'), '源码不再提供离垒过远选项');
  assert.ok(!source.includes('id:"out_cs"'), '拖拽菜单不再提供独立 CS 选项');
  assert.ok(!source.includes('abCatchRecorded'), '接杀标记与接杀回传菜单入口已移除');
});

test('双杀参与守备员：1-3-4 逆双杀、接杀回传去重', () => {
  assert.deepEqual(computeDoublePlayParticipants(['P', '1B', '2B']), ['P', '1B', '2B']);
  assert.deepEqual(computeDoublePlayParticipants(['CF', '2B']), ['CF', '2B']);
  assert.deepEqual(computeDoublePlayParticipants(['CF', 'CF', '2B']), ['CF', '2B']);
});

test('双杀守备统计：同一 play 第二个出局给参与守备员记 dp', () => {
  assert.match(source, /function recordPlayOut\(positions,kind="other"\)\{/);
  assert.match(source, /ctx\.outs>=2&&!ctx\.credited&&\(creditDoublePlayFielders\(ctx\.fielders\),ctx\.credited=!0\)/, '第二个出局合并两次出局的参与员记 dp');
  assert.match(source, /function canSecondPlayOut\(firstKind,kind\)\{[\s\S]*?firstKind==="batterContact"\)return kind==="tagForce"/, '打者先出局只允许触杀/封杀补第二出局');
  assert.ok(source.includes('type==="DP"&&creditDoublePlayFielders(s.map(r=>r.pos))'), 'DP 链参与守备员记 dp');
  assert.ok(source.includes('Dn(t),recordPlayOut(t,"batterContact")'), '打者出局记录守备链');
  assert.ok(source.includes('recordPlayOut(C1,e.stealFail?"steal":"tagForce")'), '跑者第二出局记录守备链');
  assert.ok(source.includes('creditStrikeoutFielding("caught"),recordPlayOut([],"strikeout")'), '三振出局记录 play 出局');
  assert.ok(source.includes('recordPlayOut(["P",e],"pickoff")'), '牵制出局记录参与员');
  assert.ok(source.includes('recordPlayOut(["C",e],"steal")'), '盗垒死记录参与员');
  assert.ok(source.includes('creditDoublePlayFielders(cbChain)'), '遗留接杀回传 flow 记 dp');
  assert.ok(source.includes('playOut:null'), '初始 state 带 playOut');
  assert.ok(source.includes('l.playOut=null'), '新投球/新打者/撤销/载入清空 play 上下文');
  assert.ok(!source.includes('e==="G"&&l.runners[1]&&(!l.runners[2]'), '地滚出局不再自动推进一垒跑者');
});

test('本垒出局原因封杀仅在被迫进垒时出现', () => {
  assert.ok(source.includes('hasBehindOccupied:b.hasBehindOccupied'), '安打/推进队列保存被迫进垒标记');
  assert.ok(source.includes('hasBehindOccupied:g.hasBehindOccupied'), '野选队列保存被迫进垒标记');
  assert.match(source, /l\.forceHomeRunnerId=forceHome\?\(forceHome\.runner\.id\?\?forceHome\.runner\.name\?\?null\):null/, '安打/野选队列记录被迫进垒跑者');
  assert.match(source, /homeForceOutVisible=\$\(\(\)=>\{[\s\S]*?String\(t\)===String\(l\.forceHomeRunnerId\)/, '封杀门禁按被迫进垒跑者计算');
  assert.ok(source.includes('l.forceHomeRunnerId=null'), '走路/取消队列清空被迫进垒标记');
  assert.match(source, /homeOutReasonItems=\$\(\(\)=>\[[\s\S]*?homeForceOutVisible\.value\?\[\{key:"force",cn:"封杀出局",en:"Force Out"\}\]:\[\][\s\S]*?\{key:"tag"/, '本垒出局原因项按被迫进垒动态生成');
  assert.match(source, /et\(homeOutReasonItems\.value,d=>n\("button"/, '原因面板按动态项渲染');
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

test('触杀/封杀选项进入传球链：分别路由 RUNNER_OUT_TAG / RUNNER_OUT_FORCE', () => {
  const kpBlock = sliceBetween('function kp(e){', 'function Pt(){');
  assert.match(kpBlock, /if\(e\.id==="out_tag"\|\|e\.id==="out_force"\)\{/);
  assert.ok(kpBlock.includes('const isForce=e.id==="out_force"'));
  assert.match(kpBlock, /type:isForce\?"RUNNER_OUT_FORCE":"RUNNER_OUT_TAG"/);
  assert.match(kpBlock, /outType:isForce\?"force":"tag"/);
});
