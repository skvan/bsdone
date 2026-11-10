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

test('“未进垒”选项全部移除，拖拽后退改为安全回垒', () => {
  assert.ok(!source.includes('noscore'), '源码不再包含 noscore');
  assert.ok(!source.includes('不进垒'), '源码不再包含“不进垒”');
  assert.ok(!source.includes('未进垒'), '源码不再包含“未进垒”');
  assert.ok(!source.includes('safe_retreat'), 'safe_retreat 已移除');
  assert.match(source, /id:"safe_return",en:"Return to Base",cn:"回垒（安全返回）"/);
  assert.ok(!source.includes('allowDidntScore'), 'allowDidntScore 已移除');
});

test('安打/失误/捕逸确认队列只包含强迫回本垒跑者', () => {
  const body = sliceBetween('function $l(e,t,a,s){', 'function rr(e,t,a){');
  assert.match(body, /resolveBatterForceAdvance\(\{batterBase:t,runners:l\.runners,batter:be\.value\}\)/);
  assert.match(body, /if\(!plan\?\.homeRunners\?\.length\)return!1/);
  assert.match(body, /startForceQueue\(plan,outcomes=>dp\(context,outcomes\)\)/);
  assert.ok(!body.includes('$s(t,l.runners)'), '不再按安打垒数收集全部跑者');
  assert.ok(!body.includes('fu(t,l.runners'), '不再使用 fu 全体推进预览');
});

test('安打按击球员进占垒位计算强迫链，非强迫跑者留垒', () => {
  const h1 = sliceBetween('if(e==="H1"){', 'else if(e==="H2"){');
  assert.match(h1, /resolveBatterForceAdvance\(\{batterBase:1,runners:l\.runners,batter:r\}\)/);
  assert.match(h1, /applyForcePlanMoves\(plan,\{runCause:"HIT",reasonCn:"一垒安打推进"/);
  assert.ok(!h1.includes('Ql(1)'), 'H1 不再调用全体推进 Ql');
  const h2 = sliceBetween('else if(e==="H2"){', 'else if(e==="H3"){');
  assert.match(h2, /resolveBatterForceAdvance\(\{batterBase:2,runners:l\.runners,batter:r\}\)/);
  assert.ok(!h2.includes('Ql(2)'), 'H2 不再调用全体推进 Ql');
  const h3 = sliceBetween('else if(e==="H3"){', 'else if(e==="HR"){');
  assert.match(h3, /plan\.homeRunners\.map\(\(\)=>"safe"\)/, '三垒安打仍自动得分');
  assert.match(h3, /applyForcedHomeOutcomes\(plan,decisions/);
});

test('失误/野选/捕手妨碍/漏接三振接入强迫推进计划', () => {
  const e = sliceBetween('else if(e==="E"){', 'else if(e==="FC"){');
  assert.match(e, /resolveBatterForceAdvance\(\{batterBase:1,runners:l\.runners,batter:r\}\)/);
  assert.match(e, /runCause:"ERROR",reasonCn:"失误推进"/);
  assert.ok(!e.includes('Ql(1,!1'), 'E 不再调用全体推进 Ql');

  const safe = sliceBetween('if(fc_choice==="safe"){', 'const fc_base=Number(fc_choice)');
  assert.match(safe, /resolveBatterForceAdvance\(\{batterBase:1,runners:l\.runners,batter:r\}\)/);
  assert.match(safe, /startForceQueue\(fc_plan,/);
  assert.ok(!safe.includes('Jv(fc_old,r)'), 'FC 安全分支不再全体推进');

  const error = sliceBetween('if(fc_choice==="0"){', 'if(fc_choice==="safe"){');
  assert.match(error, /resolveBatterForceAdvance\(\{batterBase:1,runners:l\.runners,batter:r\}\)/);
  assert.match(error, /runCause:"ERROR",reasonCn:"失误推进"/);
  assert.ok(!error.includes('Ql(1,!1'), 'FC 失误分支不再全体推进');

  const ci = sliceBetween('if(e===1){', 'for(let g=0;');
  assert.match(ci, /resolveBatterForceAdvance\(\{batterBase:1,runners:l\.runners,batter:a\}\)/);
  assert.match(ci, /startForceQueue\(plan,/);

  const dk3 = sliceBetween('function ii(e,t,a){', 'function So(e,t){');
  assert.match(dk3, /resolveBatterForceAdvance\(\{/);
  assert.match(dk3, /applyForcePlanMoves\(plan,\{runCause:origin,origin,reasonCn/);
  assert.ok(!dk3.includes('ti({origin:'));

  const dk3e = sliceBetween('function dk3ReachOnError(e,t){', 'function jf(e){');
  assert.match(dk3e, /resolveBatterForceAdvance\(\{batterBase:1,runners:l\.runners,batter:a\}\)/);
  assert.match(dk3e, /startForceQueue\(plan,/);
});

test('地滚球出局满垒三垒自动回本垒并确认；双杀结算无“不进垒”', () => {
  const g = sliceBetween('else if(e==="G"||e==="F"||e==="L"||e==="TAG"){', 'else if(e==="SF"){');
  assert.match(g, /groundPlan=resolveGroundOutForceAdvance\(\{runners:l\.runners\}\)/);
  assert.match(g, /applyForcePlanMoves\(groundPlan,/);
  assert.match(g, /startForceQueue\(groundPlan,/);
  assert.match(g, /applyForcedHomeOutcomes\(groundPlan,outcomes/);
  assert.match(g, /if\(e==="G"&&l\.outs<3\)/, '第三出局不再弹本垒确认');
  assert.ok(!g.includes('bo(r)'), '地滚出局不再用 bo 全体推进');
  const outcomeHelper = sliceBetween('function applyForcedHomeOutcomes(', 'function startForceQueue(');
  assert.match(outcomeHelper, /l\.outs>3&&\(l\.outs=3\)/, '本垒出局兜底封顶 3 出局');
  const dpPrompt = sliceBetween('if(preview.needsRunner3Decision){', 'function applyDoublePlay');
  assert.match(dpPrompt, /key:"safe"/);
  assert.match(dpPrompt, /key:"out"/);
  assert.ok(!dpPrompt.includes('noscore'));
});

test('牺牲打/暴投改手动；投手犯规/不合法投球保留自动', () => {
  const sf = sliceBetween('else if(e==="SF"){', 'else if(e==="SH")');
  assert.ok(!sf.includes('ig(l.runners)'), 'SF 不再自动三垒得分');
  assert.ok(!sf.includes('ls(1,{'), 'SF 不再自动记分');
  assert.match(sf, /s\("SF",0\)/);
  assert.ok(!source.includes('bo(r,sacrificeRunDecision)'), 'SH 不再自动推进');
  assert.ok(!source.includes('牺牲打得分 RBI 判定'), 'SF/SH 不再预弹 RBI 判定');

  const wp = sliceBetween('else if(e==="WP"){', 'function ti(e){');
  assert.ok(!wp.includes('ai('), 'WP 不再自动推进跑者');
  assert.match(wp, /Je\("WP"/);

  assert.match(source, /ai\(\{runCause:"ILLEGAL_PITCH"\}\)/, '垒球不合法投球保留自动奖励进垒');
  assert.match(source, /ai\(\{runCause:"BALK"\}\)/, '棒球投手犯规保留自动奖励进垒');
});

test('拖拽安全原因新增安打/牺牲打，去掉未进垒；非强迫封杀不出现', () => {
  assert.match(source, /id:"safe_hit",en:"Advance on Hit \(No RBI\)"/);
  assert.match(source, /id:"safe_hit_rbi",en:"Advance on Hit \(RBI\)"/);
  assert.match(source, /id:"safe_sac_fly",en:"Advance on Sac Fly \(RBI\)"/);
  assert.match(source, /id:"safe_sac_bunt",en:"Advance on Sac Bunt"/);
  assert.match(source, /advanceReasons\.length&&o\.splice\(0,0,\.\.\.advanceReasons\)/, '前向拖拽菜单置顶安打/牺牲打原因');
  assert.match(source, /e\.id==="safe_hit_rbi"&&r===4&&\(rbiWho=creditLastBatterRbi\(\)\)/, '安打推进得分记 RBI');
  assert.match(source, /e\.id==="safe_sac_fly"&&r===4&&\(rbiWho=creditLastBatterRbi\(\)\)/, '牺牲高飞得分记 RBI');
});

test('双杀直接入口：非强迫三垒跑者不进入本垒确认', () => {
  const mi = sliceBetween('function Mi(e,t,a,s){', 'async function pp(e,t){');
  assert.match(mi, /const forced=r\.filter\(g=>g\.hasBehindOccupied\)/, '只保留被迫回本垒的跑者');
  assert.match(mi, /if\(forced\.length===0\)\{\s*fp\(e,\[\]\)/, '无被迫跑者时直接结算，不弹确认');
  const fp = sliceBetween('function fp(e,t){', 'function cp(e,t){');
  assert.match(fp, /hasHomeDecision=t\.length>0/, 'fp 只有收到确认结果才处理三垒跑者');
  assert.match(fp, /hasHomeDecision&&p&&/, '非强迫三垒跑者留垒');
});

test('ni 在二垒空时不再删除三垒跑者；fp 负责清空已处理的三垒跑者', () => {
  const ni = sliceBetween('function ni(e,t=0){', 'function ai(event){');
  assert.match(ni, /if\(l\.runners\[2\]&&\(l\.runners\[3\]=l\.runners\[2\],l\.runners\[2\]=null\)/);
  assert.ok(!ni.includes(':l.runners[3]=null'), '二垒空时不再清掉三垒跑者');
  const fp = sliceBetween('function fp(e,t){', 'function cp(e,t){');
  assert.match(fp, /p&&\(l\.runners\[3\]=null\)/, 'fp 处理完三垒跑者后清空垒位');
});
