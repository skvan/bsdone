// LiveGame.js 投手归因同步（P 位易主全路径）——静态回归断言（#205）
// 验收口径：不管哪个路径，只要有投过球的人都要记录一条投手数据。
// 运行：node --test tests/liveGamePitchSync.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const pick = (startMark, endMark) => {
  const i = src.indexOf(startMark);
  assert.ok(i >= 0, startMark + ' 存在');
  const j = src.indexOf(endMark, i);
  assert.ok(j > i, endMark + ' 在 ' + startMark + ' 之后');
  return src.slice(i, j);
};

test('存在统一帮手 syncFieldingPitcher：fpId 落位 → ft → Ka → 换投日志', () => {
  const body = pick('function syncFieldingPitcher(', 'function cc(');
  assert.match(body, /g\.value=Number\(P\.id\)/, '必须设置 fpId 为新投手');
  assert.match(body, /ft\(e\)/, '必须调用 ft 同步守备投手行');
  assert.match(body, /Ka\(\)/, '必须调用 Ka 刷新投手解析');
  assert.match(body, /pc\(Y,logPlayerName\(P\)\)/, '必须输出「投手 X 更换为 Y」日志（含背号）');
});

test('分支②（板凳替换+DH 制 P 位=换投）改调统一帮手且行为链保留', () => {
  const body = pick('if(b==="P"&&!oa(y)){', 'de("换守备");');
  assert.match(body, /de\("换投"\),syncFieldingPitcher\(a,t,P,fieldingIpNote\(t\)\),Kt\(\)/, '换投分支须走统一帮手（P3：附守备局数结清注记）后收尾');
  assert.doesNotMatch(body, /ft\(a\),Ka\(\)/, '旧的内联链不得残留（切片仅覆盖换投分支）');
});

test('分支①（守备组互换）：涉及 P 时调用统一帮手（b 或对方原位为 P）', () => {
  const body = pick('if(e.groupKey==="fielders"){', 'const w=m.findIndex');
  assert.match(body, /b==="P"\?"P"|b==="P"\?T:Y==="P"/, '须判定哪一侧为 P');
  assert.match(body, /P\?syncFieldingPitcher\(a,b==="P"\?t:T,P\)/, 'P 易主后须调用统一帮手（三元式保证非 P 互换不触发）');
  assert.match(body, /fc\(b,logPlayerName\(t\),Y,Y,logPlayerName\(T\),b\)/, '互换标签与既有日志保持（含背号）');
});

test('分支③（板凳替换非 DH 制 P 位）：以统一帮手替代单独 Ka()', () => {
  const i = src.indexOf('b==="P"&&Ka()');
  assert.equal(i, -1, '不得残留 b==="P"&&Ka() 半吊子形态');
  assert.match(src, /b==="P"&&syncFieldingPitcher\(a,t,P,fieldingIpNote\(t\)\)/, 'P 位替换须走统一帮手（P3：附结清注记）');
});

test('非 P 的守位互换保持纯标签（不出换投日志）；只出不进时清空投手引用', () => {
  const body = pick('if(e.groupKey==="fielders"){', 'const w=m.findIndex');
  assert.match(body, /T\.position=b,t&&\(t\.position=Y\)/, '标签交换保留');
  // 同步调用必须带 P 判定（前置条件），确保 C↔SS 等互换不误触发
  assert.match(body, /P=b==="P"\?T:Y==="P"&&t\?t:null/, '新投手仅在 P 涉及两侧时生成');
  // 「只出不进」：原 P 位持有人移走且无人接替时，清空 fpId 并同步（防陈旧归因）
  assert.match(body, /Y==="P"&&!t&&\(g\.value=null,ft\(a\),Ka\(\)\)/, '移出 P 且无接替者时清空投手引用');
});

test('既有回归点不受影响：lc 失误得分挂接 / Qr 悬挂消费 / 派发日志仍在', () => {
  assert.match(src, /ls\(1,\{runner:s,pitcher:w\?\?void 0,erDelta:1,origin:"ERROR"\}\)/, 'lc 挂接保留');
  assert.match(src, /f\.playEvents\.push\(E\),Jl\(\),syncEarnedRunHalfInning\(E\)/, 'Qr 悬挂消费保留');
  assert.match(src, /function pc\(e,t\)\{[\s\S]*?投手 \$\{e\} 更换为 \$\{t\}/, '换投日志模板保留');
});

test('行模型去重不变量（防 #94 双行复发）：投手表一人一行、行标记互斥互补', () => {
  // 注：本用例断言的是 LiveGameWatch/GameDetailContent 的跨文件不变量锚点（非本次改动验证，仅为防误改守护）
  const watch = fs.readFileSync(new URL('../src/views/admin/LiveGameWatch.js', import.meta.url), 'utf8');
  assert.match(watch, /!Number\.isFinite\(m\)\|\|m<=0\|\|l\.has\(m\)\|\|\(l\.add\(m\),a\.push\(m\)\)/, 'summary 投手列表须 Set 去重');
  assert.match(watch, /return o\.includes\(v\)\?o:\[\.\.\.o,v\]/, 'fpId 追加须 includes 去重');
  assert.match(watch, /m\(V,o,void 0,g\+B\*100,\{\s*pitcherOrder:B,isPitcher:1\s*\}\s*\)/, '投手行构造保留（battingOrder 空 + isPitcher 1）');
  assert.match(watch, /C=B\?\.isPitcher\?\?0/, '打者行 isPitcher 恒 0（A 方案）');
  assert.match(watch, /m\(y,o,k,re\);/, '打者行调用不带 isPitcher 标记');
  const gd = fs.readFileSync(new URL('../src/components/admin/GameDetailContent.js', import.meta.url), 'utf8');
  assert.match(gd, /!ke\(e\)\|\|e\.battingOrder!=null/, '打者表筛选保留互补条件');
  assert.match(gd, /ta\(c\.stats\.filter\(e=>e\.teamId===c\.game\.awayTeamId&&ke\(e\)\)\)/, '投手表筛选保留 isPitcher 口径');
});
