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

test('三垒跑回本垒的盗垒出局菜单：触杀替换盗垒被刺杀，其他垒位保持 CS', () => {
  const menuBlock = sliceBetween('),ep=[{', '],tp=$(()=>{');
  const homeOptions = menuBlock.slice(menuBlock.indexOf('homeStealOutReasonOptions'));
  assert.match(homeOptions, /homeStealOutReasonOptions=\[\{\s*id:"out_tag",en:"Tagged Out",cn:"触杀出局"/);
  assert.ok(!homeOptions.includes('out_cs'), '本垒盗垒出局菜单不再提供盗垒被刺杀 (CS)');
  const stealOptions = menuBlock.slice(0, menuBlock.indexOf('homeStealOutReasonOptions'));
  assert.ok(stealOptions.includes('id:"out_cs"'), '一垒→二垒、二垒→三垒的盗垒出局仍提供 CS');
  assert.match(source, /if\(tp\.value\)return e\.fromBase===3&&e\.toBase===4\?homeStealOutReasonOptions:ep;/);
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
