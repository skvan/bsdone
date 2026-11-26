// #696 漏接三振“在一垒出局”传球链
// 运行：在 bsball-frontend 目录执行 node --test tests/liveGameDk3Chain.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = () => fs
  .readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8')
  .replace(/\r\n/g, '\n');

function slice(src, startMarker, endMarker) {
  const start = src.indexOf(startMarker);
  assert.ok(start >= 0, `缺少 ${startMarker}`);
  const end = src.indexOf(endMarker, start);
  assert.ok(end > start, `缺少 ${endMarker}`);
  return src.slice(start, end);
}

test('isDk3ChainFlow：仅 DK3_FIELDER_PICK 且带 steps 视为链式，并纳入 isChainFlow', () => {
  const src = read();
  assert.match(src, /function isDk3ChainFlow\(u\)\{\n  return!!u&&u\.type==="DK3_FIELDER_PICK"&&Array\.isArray\(u\.steps\)\n\}/);
  assert.match(src, /const isChainFlow=e=>!!e&&\(CHAIN_TYPES\.has\(e\.type\)\|\|isDk3ChainFlow\(e\)\|\|e\.type==="RUNNER_OUT_TAG"\|\|e\.type==="RUNNER_OUT_FORCE"\)/);
});

test('jf：o1 进入以捕手为起点的传球链（steps=["C"]、required=1）', () => {
  const body = slice(read(), 'function jf(e){', 'function Jf(e){');
  assert.match(body, /const chain=e\.endsWith\(":o1"\),flow=\{/, '未识别 o1 链式分支');
  assert.match(body, /chain&&\(flow\.steps=\["C"\],flow\.required=1\),l\.flow=flow/, '未以捕手预置链首');
  assert.match(body, /捕手已接球 — 直接点「完成记录」记捕手触杀/, '缺少直接完成=捕手触杀提示');
  assert.match(body, /chain&&syncChainVisualFromFlow\(\)/, '进入链式未同步球场视觉');
});

test('Jf：链式流程不再走单点守备员直接结算', () => {
  const body = slice(read(), 'function Jf(e){', 'function ko(e,t){');
  assert.match(body, /if\(isDk3ChainFlow\(t\)\)return;/, '链式流程缺少提前返回守卫');
  assert.ok(!body.includes('case"dk3:sw:o1"'), '挥空出局分支不应再由 Jf 直接结算');
  assert.ok(!body.includes('case"dk3:lk:o1"'), '目送出局分支不应再由 Jf 直接结算');
});

test('settleChainIfReady：DK3 链先于通用链结算，qf 后交给 So 记账', () => {
  const body = slice(read(), 'function settleChainIfReady(force=!1){', 'function completeChainFlow(){');
  assert.match(body, /if\(isDk3ChainFlow\(e\)\)\{/, '结算入口未识别 DK3 链');
  assert.match(body, /LgChainReady\(e\.type,e\.steps\?\?\[\],e\.required\?\?us\(e\.type\)\)/, 'DK3 链缺少完成门槛');
  assert.match(body, /de\(`漏接三振:\$\{e\.dk3Code\}`\),qf\(\),l\.flow=null,So\(e\.dk3Code\?\.includes\(":lk:"\),a\)/, 'DK3 结算未按挥空/目送交给 So');
});

test('Ro：链式漏接三振点选守备员时追加步骤并播放传球动画', () => {
  const body = slice(read(), 'if(a?.type==="DK3_FIELDER_PICK"){', 'if(a?.type==="CS"');
  assert.match(body, /if\(isDk3ChainFlow\(a\)\)\{/, '守备员点击未进入链式分支');
  assert.match(body, /a\.steps\.push\(e\)/, '链式步骤未追加点选守备员');
  assert.match(body, /pushChainTrail\(s,r\),ballFlyTo\(r\.x,r\.y,450\)/, '缺少传球链动画');
  assert.match(body, /Jf\(e\)/, 'WP/PB/E 单点分支未保留');
});

test('So：≤2 步沿用单点规则，长链走单出局链；日志区分触杀/传杀', () => {
  const body = slice(read(), 'function So(e,t){', 'function dk3ReachOnError(');
  assert.match(body, /const chain=_i\(Array\.isArray\(t\)&&t\.length\?t:\["C"\]\)/, '未归一化传球链');
  assert.match(body, /chain\.length<=2\?creditStrikeoutFielding\("dropped",chain\[chain\.length-1\]\):Dn\(chain\)/, '未按 1-2 步单点规则/长链单出局链分流');
  assert.match(body, /recordPlayOut\(\[\],"strikeout"\)/, '未保留三振出局上下文');
  assert.match(body, /Un2\("DK3",chain\)/, '日志未按链生成');
});

test('Un：DK3 链日志直接完成=触杀，捕手传一垒=传杀一垒出局', () => {
  const body = slice(read(), 'if(u==="DK3"){', 'if(u==="SF")');
  assert.match(body, /触杀出局/, '缺少捕手触杀文案');
  assert.match(body, /传杀 一垒出局/, '缺少捕手传一垒文案');
});

test('提示：初始可完成后仍提示点选接球守备员；形成链后显示链并提示可完成', () => {
  const body = slice(read(), 'if(e.type==="DK3_FIELDER_PICK"){', 'if(e.type==="RUNNER_OUT_TAG"');
  assert.match(body, /chainDisp\("DK3",dk3Steps\)/, '未显示当前传球链');
  assert.match(body, /捕手已接球 — 直接点「完成记录」记捕手触杀/, '缺少直接完成提示');
  assert.match(body, /已可完成/, '缺少链形成后的完成提示');
});
