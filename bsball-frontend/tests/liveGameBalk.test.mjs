// 投手犯规（BK）：自动坏球、自动推进与三垒得分 RBI 归属——静态回归断言（Issue #571）
// 运行：node --test tests/liveGameBalk.test.mjs
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

test('投手犯规不再被「请先记录至少一次投球」守卫拦截', () => {
  const body = pick('function Xt(e){', 'function ti(e){');
  assert.match(body, /if\(\["WP","INT","OBST"\]\.includes\(e\)&&!l\.hasPitchThisAB\)/,
    'BK 必须从投球前置守卫中移除，打席第一动作即可记录');
  assert.doesNotMatch(body, /\["BK","WP"/, '旧守卫不得残留');
});

test('投手犯规无论垒上是否有人都会自动记一个坏球，满四坏按保送处理', () => {
  const body = pick('if(e==="BK"){', 'else if(e==="WP"){');
  assert.match(body, /l\.balls\+\+/, 'BK 必须累加坏球');
  assert.match(body, /kind:"balk_ball"/, '日志使用「投手犯规（记坏球 Bx-Sy）」');
  assert.match(body, /l\.balls>=4[^;]*bt\("BB",\[\]\)/, '第四坏球按四坏保送收尾');
  const ballPos = body.indexOf('l.balls++');
  const aiPos = body.indexOf('ai({');
  assert.ok(aiPos < 0 || ballPos < aiPos, '坏球累加独立于「是否有跑者」分支');
});

test('投手犯规有跑者时自动推进，并按三垒跑者归属 RBI', () => {
  const body = pick('if(e==="BK"){', 'else if(e==="WP"){');
  assert.match(body, /dt\.value\?\.runnerIdsBefore\?\.\[3\]/, '以打席开始时的三垒跑者判定 RBI 归属');
  assert.match(body, /Me\.value\.reduce\(\(found,s\)=>s\.inning===l\.inning&&s\.half===halfNow\?s:found,null\)/,
    '上一棒取本半局最后一条已落定打席，避免打线回卷时取错');
  assert.doesNotMatch(body, /El\.value/, '不得再用按当前棒次过滤的 El，回卷时会漏掉上一棒');
  assert.match(body, /rbiBatter=thirdAtPaStart\?lastBatter:be\.value/, '上一棒找不到时不得回退到下一棒记打点');
  assert.match(body, /creditRbiToBatter:!!\(runnerOnThird&&rbiBatter\)/, '三垒有人且归属打者明确时才记 RBI');
  assert.match(body, /batter:runnerOnThird&&rbiBatter\?rbiBatter:void 0/, 'RBI 记给判定后的打者');
  assert.match(body, /lastAb\.rbi=\(lastAb\.rbi\?\?0\)\+1/, 'RBI 同步写入上一打席摘要');
  assert.match(body, /runCause:"BALK"/, '得分原因标记为投手犯规');
});

test('ai() 将完整事件传给 ti()，避免 RBI 记分字段被丢弃', () => {
  const body = pick('function ai(event){', 'function _o(){');
  assert.match(body, /ti\(event\?\?\{\}\)/, 'ai 必须透传事件对象');
});
