// 投手犯规 / 不合法投球：按赛事制与垒上状态自动分流——静态回归断言（Issue #651 / #653）
// 运行：node --test tests/liveGameBalk.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const pitchMenuSrc = fs.readFileSync(new URL('../src/components/admin/LiveGamePitchMenu.vue', import.meta.url), 'utf8');

const pick = (startMark, endMark) => {
  const i = src.indexOf(startMark);
  assert.ok(i >= 0, startMark + ' 存在');
  const j = src.indexOf(endMark, i);
  assert.ok(j > i, endMark + ' 在 ' + startMark + ' 之后');
  return src.slice(i, j);
};

test('投手犯规不再被「请先记录至少一次投球」守卫拦截', () => {
  const body = pick('function Xt(e){', 'function ti(e){');
  assert.match(body, /if\(\["WP","INT","OBST"\]\.includes\(e\)&&!l\.hasPitchThisAB\)/,
    'BK 必须从投球前置守卫中移除，打席第一动作即可记录');
  assert.doesNotMatch(body, /\["BK","WP"/, '旧守卫不得残留');
});

test('BK 按 sport 与垒上状态分流：棒球 Balk / 垒球 Illegal Pitch', () => {
  const body = pick('if(e==="BK"){', 'else if(e==="WP"){');
  assert.match(body, /LgBalkAction\(\{sport:N\.value,runnersOnBase:Ut\(\)>0\}\)/,
    '分流必须同时看赛事制与垒上状态');
  assert.match(body, /if\(action\.countPitcherBalk\)/, '棒球有跑者走 Balk 分支');
  assert.match(body, /else if\(action\.advanceRunners\)/, '垒球有跑者走 Illegal Pitch 推进分支');
  assert.match(body, /Wt\("B",\{logKind:action\.logKind,deferWalk:!0,tail:ca\(\)\}\)/,
    '垒球非法投球先记坏球，并附带进垒明细');
  assert.match(body, /ballsBefore=l\.balls/, '记录记坏球前的球数，防止被守卫拦截后误推进');
  assert.match(body, /l\.balls===ballsBefore/, 'Wt 被拦截时不得继续推进跑者');
  assert.match(body, /ai\(\{runCause:"ILLEGAL_PITCH"\}\)/, '垒球非法投球推进原因');
  assert.match(body, /if\(l\.balls>=4\)return bt\("BB",\[\]\)/, '第四坏球先推进再保送');
  const wtPos = body.indexOf('Wt("B",{logKind:action.logKind,deferWalk:!0,tail:ca()})');
  const aiPos = body.indexOf('ai({runCause:"ILLEGAL_PITCH"})');
  const btPos = body.indexOf('if(l.balls>=4)return bt("BB",[])');
  assert.ok(wtPos >= 0 && aiPos > wtPos && btPos > aiPos,
    '顺序必须是：记坏球 → 推进全部跑者 → 第四坏球保送');
  assert.match(body, /action\.countPitcherBalk/, '只有棒球 Balk 记投手 BK');
  assert.match(body, /stats\.pitching\.bk/, '投手 BK 字段');
  assert.match(body, /ai\(\{runCause:"BALK"\}\)/, '棒球 Balk 推进原因');
  assert.match(body, /Wt\("B",\{logKind:action\.logKind\}\)/, '无人上垒走坏球记账');
  assert.doesNotMatch(body, /l\.balls\+\+/, '不得无条件累加坏球');
  assert.doesNotMatch(body, /creditRbiToBatter|lastAb|rbiReason:"BALK"/, '投手犯规不得再给打者记 RBI');
});

test('不合法投球仍按正常投球计球数，并支持延迟保送与进垒明细', () => {
  const body = pick('function Wt(e,{logKind,deferWalk,tail}={}){', 'function Hf(){');
  assert.match(body, /kind:logKind\?\?"ball"/, '默认仍为普通坏球');
  assert.match(body, /l\.balls\+\+,l\.balls>=4/, '坏球累加与满四坏判定保留');
  assert.match(body, /if\(deferWalk\)return/, '垒球有跑者时第四坏球延迟给调用方处理');
  assert.match(body, /bt\("BB",\[\]\)/, '第四坏球按四坏保送收尾');
  assert.match(body, /kind:logKind,count:_n\(l\.balls,l\.strikes\)\}\),tail\)/, '第四坏球也先记不合法投球日志');
  assert.match(body, /kind:logKind\?\?"ball",count:_n\(l\.balls,l\.strikes\)\}\),tail\)/, '坏球日志附带进垒明细');
});

test('移除重复的「不合法投球」入口与推进/不推进子菜单', () => {
  assert.ok(!src.includes('extra:ILLEGAL_ADVANCE'), '旧 ILLEGAL_ADVANCE 命令与映射必须移除');
  assert.ok(!src.includes('extra:ILLEGAL_NO_ADVANCE'), '旧 ILLEGAL_NO_ADVANCE 命令与映射必须移除');
  assert.ok(!src.includes('M.value==="illegal"'), '旧“不合法投球”子菜单必须移除');
  assert.ok(!src.includes('onClick:le'), '旧“推进跑者”处理器必须移除');
  assert.ok(!src.includes('onClick:fe'), '旧“不推进跑者”处理器必须移除');
});

test('保留单一入口，棒球标签同时说明 Balk / Illegal Pitch', () => {
  assert.ok(src.includes('投手犯规 / 不合法投球'), '中文入口标签');
  assert.ok(src.includes('Balk / Illegal Pitch'), '英文入口标签');
  assert.ok(!src.includes('"不合法投球"),n("span",{'), '旧独立“不合法投球”按钮不得残留');
});

test('PitchMenu 组件按 gameMode 动态显示入口标签，并接收父组件赛事制', () => {
  assert.ok(!pitchMenuSrc.includes('extra:ILLEGAL'), '组件不得再发送旧 ILLEGAL 命令');
  assert.ok(!pitchMenuSrc.includes('不推进跑者'), '组件不得残留“不推进跑者”');
  assert.ok(pitchMenuSrc.includes("gameMode: { type: String, default: 'BASEBALL' }"), '组件声明 gameMode');
  assert.ok(pitchMenuSrc.includes('const isSoftball = computed'), '组件按 gameMode 判定垒球');
  assert.ok(pitchMenuSrc.includes('v-for="item in otherRoot"'), '模板使用动态 otherRoot');
  assert.ok(pitchMenuSrc.includes("item.cmd === 'evt:BK' && isSoftball.value"), '垒球只改写 BK 入口标签');
  assert.ok(pitchMenuSrc.includes("cn: '不合法投球', en: 'Illegal Pitch', glyph: 'IP'"), '垒球入口标签与图标');
  assert.ok(pitchMenuSrc.includes("cn: '投手犯规 / 不合法投球', en: 'Balk / Illegal Pitch'"), '棒球入口标签');
  assert.ok(src.includes('"game-mode":N.value'), '父组件传入当前赛事制');
});
