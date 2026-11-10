// 投手犯规 / 不合法投球：按垒上状态自动分流——静态回归断言（Issue #651）
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

test('BK 使用纯函数按垒上状态分流，不再无条件记坏球或记 RBI', () => {
  const body = pick('if(e==="BK"){', 'else if(e==="WP"){');
  assert.match(body, /LgBalkAction\(\{runnersOnBase:Ut\(\)>0\}\)/,
    '有跑者才推进，垒上无人改判不合法投球');
  assert.match(body, /action\.advanceRunners/, '有跑者推进一垒');
  assert.match(body, /stats\.pitching\.bk/, '只有 Balk 记投手 BK');
  assert.match(body, /Wt\("B",\{logKind:action\.logKind\}\)/, '垒上无人走坏球记账（满四坏由 Wt 收尾为保送）');
  assert.doesNotMatch(body, /l\.balls\+\+/, '不得无条件累加坏球');
  assert.doesNotMatch(body, /creditRbiToBatter|lastAb|rbiReason:"BALK"/, '投手犯规不得再给打者记 RBI');
  assert.match(body, /ai\(\{runCause:"BALK"\}\)/, '有跑者推进且得分原因记为 BALK');
  assert.match(body, /kind:action\.logKind/, '有跑者日志为投手犯规');
});

test('不合法投球仍按正常投球计球数：满四坏保送且日志可区分', () => {
  const body = pick('function Wt(e,{logKind}={}){', 'function Hf(){');
  assert.match(body, /kind:logKind\?\?"ball"/, '默认仍为普通坏球');
  assert.match(body, /l\.balls\+\+,l\.balls>=4/, '坏球累加与满四坏判定保留');
  assert.match(body, /bt\("BB",\[\]\)/, '第四坏球按四坏保送收尾');
  assert.match(body, /kind:logKind,count:/, '第四坏球也先记不合法投球日志');
});

test('移除重复的「不合法投球」入口与推进/不推进子菜单', () => {
  assert.ok(!src.includes('extra:ILLEGAL_ADVANCE'), '旧 ILLEGAL_ADVANCE 命令与映射必须移除');
  assert.ok(!src.includes('extra:ILLEGAL_NO_ADVANCE'), '旧 ILLEGAL_NO_ADVANCE 命令与映射必须移除');
  assert.ok(!src.includes('M.value==="illegal"'), '旧“不合法投球”子菜单必须移除');
  assert.ok(!src.includes('onClick:le'), '旧“推进跑者”处理器必须移除');
  assert.ok(!src.includes('onClick:fe'), '旧“不推进跑者”处理器必须移除');
});

test('保留单一入口，标签同时说明 Balk / Illegal Pitch', () => {
  assert.ok(src.includes('投手犯规 / 不合法投球'), '中文入口标签');
  assert.ok(src.includes('Balk / Illegal Pitch'), '英文入口标签');
  assert.ok(!src.includes('"不合法投球"),n("span",{'), '旧独立“不合法投球”按钮不得残留');
});

test('实战使用的 PitchMenu 组件同样只保留单一入口', () => {
  assert.ok(!pitchMenuSrc.includes('extra:ILLEGAL'), '组件不得再发送旧 ILLEGAL 命令');
  assert.ok(!pitchMenuSrc.includes('不推进跑者'), '组件不得残留“不推进跑者”');
  assert.ok(pitchMenuSrc.includes('投手犯规 / 不合法投球'), '组件入口标签');
  assert.ok(pitchMenuSrc.includes('Balk / Illegal Pitch'), '组件英文标签');
});
