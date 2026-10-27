/**
 * #444 换人后打序与 BOX SCORE 行序（棒次留原列，换上来的人不带棒次、排在其下）
 *
 * 目标呈现由 skvan 以截图定案（Hustlers Batters）：
 *   2  Kim m… #66      ← 被换下的第二棒，仍保留自己的棒次
 *   -  Kim jo… #17     ← 换上来的人不带棒次，紧跟在原棒次下方
 * 判定用的就是那张表的真实渲染：GameDetailContent.js 的取值器 `E(e,t)=e?.[t]`、
 * 首格 `E(a,"battingOrder")!=null ? String(...) : "-"`，排序器 Kt()（主键 listOrder、
 * 次键 battingOrder、再 playerId）。本 PR 不改那张表，只改喂给它的数据。
 *
 * 沿用 liveGameHbpRoundTrip387.test.mjs 的做法：把 LiveGame.js 里的**原始码文字**
 * 绑上依赖后真的执行。案例是端到端的：先呼叫 _r()（换人记录，发生在名单数组被改动
 * 之前）产生状态，再原位顶替（cc() 的真实形态 y[A]={...P}），最后跑 No() 看 payload。
 *
 * 运行：node --test tests/liveGameSubstitutionBattingOrder444.test.mjs
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = fs.readFileSync(SRC_PATH, 'utf8').replace(/\r\n/g, '\n');

/** 平衡括号切片取函数原文；跳过字符串与注释，避免被内容里的括号带偏 */
function sliceFn(header) {
  const at = src.indexOf(header);
  assert(at >= 0, `找不到 ${header}，元件结构可能已变动`);
  const open = at + header.length - 1;
  let depth = 0;
  for (let i = open; i < src.length; i++) {
    const c = src[i];
    if (c === '"' || c === "'" || c === '`') {
      for (i++; i < src.length; i++) { if (src[i] === '\\') i++; else if (src[i] === c) break; }
      continue;
    }
    if (c === '/' && src[i + 1] === '/') { while (i < src.length && src[i] !== '\n') i++; continue; }
    if (c === '/' && src[i + 1] === '*') { i = src.indexOf('*/', i) + 1; continue; }
    if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return src.slice(at, i + 1);
  }
  throw new Error(header + ' 括号不闭合');
}

const DEPS = ['Et', 'Oo', 'Lo', '$z', 'ue', 'Pt', 'bs', 'mc', 'dc', 'mr', 'vr', 'gr', 'br', '_s'];
const code = DEPS.map((n) => sliceFn('function ' + n + '(')).join('\n')
  + '\n' + sliceFn('function _r(e,t,a){') + '\n' + sliceFn('function No(e,t){');
// No/br 用到的自由变量依原文注入：Ee 取队伍上下文、_ 是赛事对象、Eo 组 payload
const make = new Function('Ee', '_', 'ft', 'Eo', 'Nn', 'le', 'fe',
  'return (function(){' + code + '\nreturn {No,_r};})()');

const ref = (v) => ({ value: v });
const zero = { ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, sf: 0, sh: 0, sb: 0, doubles: 0, triples: 0, hr: 0, gdp: 0, ibb: 0, io: 0 };
const stats = (b = {}, f = {}) => ({ batting: { ...zero, ...b }, pitching: { ip: 0, np: 0 }, fielding: { po: 0, a: 0, e: 0, defInn: 0, ...f } });
const chance = (pos) => (pos === 'SS' ? { po: 1, a: 1, defInn: 1 } : {});
const mk = (id, ab = 3, pos = 'SS') => ({ id, name: '球员' + id, number: String(id), position: pos, teamId: 7, stats: stats({ ab }, chance(pos)) });
// 只有守备记录的人 Oo() 视为「有统计」会被旧码照送；#444 报的是**整场什么都没留下**的那种，
// 必须给他全零统计，否则 T() 的丢弃条件根本不会触发，测试就成了假绿。
const mkZero = (id, pos = 'SS') => ({ id, name: '球员' + id, number: String(id), position: pos, teamId: 7,
  stats: { batting: { ...zero }, pitching: { ip: 0, np: 0 }, fielding: { po: 0, a: 0, e: 0, defInn: 0 } } });
// 第 9 棒是投手时的真实形态：既有投球统计也有打数
const mkPitcher = (id, ab = 2) => ({ id, name: '投手' + id, number: String(id), position: 'P', teamId: 7,
  stats: { batting: { ...zero, ab, h: 1 }, pitching: { ip: 5, np: 80 }, fielding: { po: 0, a: 0, e: 0, defInn: 1 } } });
const mkPlayer = (id, pos = 'SS', ab = 3) => (pos === 'P' ? mkPitcher(id, ab) : mk(id, ab, pos));

/** BOX SCORE 排序器（GameDetailContent.js 的 Kt 原文口径） */
const Kt = (a, b) => {
  const l = a.listOrder, d = b.listOrder, v = l != null, y = d != null;
  if (v && y && l !== d) return l - d;
  if (v !== y) return v ? -1 : 1;
  const o = a.battingOrder, n = b.battingOrder, ho = o != null, hn = n != null;
  return ho && hn && o !== n ? o - n : ho !== hn ? (ho ? -1 : 1) : a.playerId - b.playerId;
};

const blankCtx = () => ({
  lineup: ref([]), bench: ref([]), roster: ref([]), fpRow: ref(null), fpId: ref(null), unavail: ref([]),
  lineupStarterIds: ref(Array(12).fill(null)), slotSubHistory: ref(Array.from({ length: 12 }, () => null)),
});

/**
 * 一局主队的换人流程。steps: [[slot, outId, incomingId], ...]
 * 依 cc() 的实序：先 _r() 记录（此时名单还是换人前的样子），再原位顶替、被换下者进板凳。
 * 换上来的人继承被换下者的守备位置（原位顶替，不另开一格）。
 * incomingOf 控制换上来的人有几个打数（默认 2；给 0 即全零统计，用来验「什么都没留下」的接手者也被送出）。
 */
function play(steps, starters = [1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009], zeroStat = () => false, posOf = () => 'SS', incomingOf = () => 2) {
  const lineup = starters.map((id) => (zeroStat(id) ? mkZero(id, posOf(id)) : mkPlayer(id, posOf(id))));
  const bench = [mk(1010, 0, 'LF'), mk(1011, 0, 'CF'), mkZero(1012, 'RF')];   // 板凳未上场时就该零统计，否则板凳回圈会把他当「打过球」送出
  const home = { ...blankCtx(), lineup: ref(lineup), bench: ref(bench), roster: ref([...lineup, ...bench]) };
  const away = blankCtx();
  const sent = [];
  const { No, _r } = make(
    (k) => (k === 'away' ? away : home),
    { homeTeamId: 7, awayTeamId: 8 },
    () => {},
    (pl, t, o) => { sent.push({ id: Number(pl.id), bo: o.battingOrder === undefined ? null : o.battingOrder, lo: o.listOrder, ab: pl.stats.batting.ab }); return {}; },
    (pos) => String(pos ?? '').toUpperCase(),
    away.lineupStarterIds, home.lineupStarterIds);
  for (const [slot, outId, incoming] of steps) {
    const idx = home.lineup.value.findIndex((p) => Number(p.id) === outId);
    assert.equal(idx, slot, `换人前提走样：${outId} 号不在名单第 ${slot + 1} 格`);
    _r('home', slot, outId);
    const out = home.lineup.value[idx];
    const ab = incomingOf(incoming);
    const fresh = ab === 0 ? mkZero(incoming, out.position) : mkPlayer(incoming, out.position, ab);
    home.lineup.value[idx] = fresh;
    home.roster.value.push(fresh);
    // cc() 实序：换上的人离开板凳（r.value=m.filter(...)），被换下的人才进板凳
    home.bench.value = home.bench.value.filter((p) => Number(p.id) !== Number(fresh.id));
    home.bench.value.push({ ...out, position: '' });
  }
  No('home', 7);
  return { sent: [...sent].sort(Kt), home };
}

/** 截图那一格的呈现：首格印出来的字（数字或「-」） */
const cell = (row) => (row.bo != null ? String(row.bo) : '-');

test('#444 未换人时不回归：先发九人各带 1..9 棒，行序即打序', () => {
  const { sent } = play([]);
  assert.deepEqual(sent.map((s) => s.bo), [1, 2, 3, 4, 5, 6, 7, 8, 9], '未换人的打序应完整保序');
  assert.deepEqual(sent.map((s) => s.id), [1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009]);
});

test('#444 照截图的呈现：被换下的人保留第 5 棒，换上来的人「-」排在他下方', () => {
  const { sent } = play([[4, 1005, 1010]]);
  const i5 = sent.findIndex((s) => s.id === 1005);
  const i10 = sent.findIndex((s) => s.id === 1010);
  assert.ok(i5 >= 0 && i10 >= 0, '两列都必须在');
  assert.equal(i10, i5 + 1, '换上来的人必须紧贴在被换下者下方（截图形态），不能隔到别处');
  assert.equal(sent[i5].bo, 5, '棒次属于「这一格原来的那个人」，被换下仍保留');
  assert.equal(sent[i10].bo, null, '换上来的人不带棒次（首格渲染成「-」）');
  assert.deepEqual(sent.map(cell), ['1', '2', '3', '4', '5', '-', '6', '7', '8', '9'],
    '整列棒次栏的印字：数字连续 1..9，替补那一列是「-」且紧跟第 5 棒');
});

test('#444 症状②整列消失：只守半局、零打数零守备的被换下者仍要送出', () => {
  const { sent } = play([[4, 1005, 1010]], undefined, (id) => id === 1005);
  const out = sent.find((s) => s.id === 1005);
  assert.ok(out, 'ab=0 的先发不能因为「没统计」被 T() 丢弃（#444 明确要求不能省略）');
  assert.equal(out.bo, 5, '他仍保有第 5 棒');
  assert.equal(sent[sent.findIndex((s) => s.id === 1010)].bo, null);
});

test('#444 换上来的人即使什么都没留下也要有一列（带「-」排在原棒次下方）', () => {
  const { sent } = play([[4, 1005, 1012]], undefined, () => false, () => 'RF', () => 0);
  const inRow = sent.find((s) => s.id === 1012);
  assert.ok(inRow, '刚顶上来、还没打也没守备记录的人不能被丢掉');
  assert.equal(inRow.bo, null, '他不带棒次，首格是「-」');
  assert.equal(sent.findIndex((s) => s.id === 1012), sent.findIndex((s) => s.id === 1005) + 1);
});

test('#444 手动双重换人（同槽两次换人）：棒次始终留在原列，两位接手者都带「-」按先后排', () => {
  const { sent } = play([[4, 1005, 1010], [4, 1010, 1011]]);
  assert.deepEqual(sent.filter((s) => [1005, 1010, 1011].includes(s.id)).map((s) => `${s.id}:${cell(s)}`),
    ['1005:5', '1010:-', '1011:-'], '先发→第一位替补→现任，三人各占一行，只有先发持第 5 棒');
  assert.equal(sent.filter((s) => s.bo != null).length, 9, '带棒次的列数须恒等于 9，换人不得膨胀或缩水打序长度');
});

test('#444 投手被换下（风险最高的一格）：第 9 棒仍留在原投手列上，凑足 9 个带棒次的人', () => {
  // 无 DH 联盟里第 9 棒就是投手。GameService.assertStarterFieldingPositions（#468）
  // 是按「有棒次的列数」数先发九人，这里掉一列就会在「保存比赛」时被 400 误拦。
  const { sent } = play([[8, 1009, 1099]], undefined, () => false, (id) => (id === 1009 ? 'P' : 'SS'));
  const old = sent.find((s) => s.id === 1009);
  const neo = sent.find((s) => s.id === 1099);
  assert.ok(old, '旧投手那一列必须在，且保有第 9 棒');
  assert.equal(old.bo, 9);
  assert.equal(sent.filter((s) => s.bo != null).length, 9, '换投不得让带棒次的列数少于 9（否则保存比赛被 400）');
  assert.ok(!neo || neo.bo == null, '新投手若不在这张表里是因为他还没上场打击，会被打者表过滤器挡掉，不会抢棒次');
});

test('#444 中间接手者又被换下且什么都没留下：②分支的 keep 才真承重', () => {
  // 先发（有统计）→ A（零统计，只守了半局就被换下）→ 现任（有统计）。
  // A 不在①（他不是先发）、也没任何统计，旧码的丢弃条件会把他直接吞掉；
  // 没这条案例，②分支的 keep 就是未经验证的改动。
  const { sent } = play([[4, 1005, 1010], [4, 1010, 1011]], undefined, () => false, () => 'SS', (id) => (id === 1010 ? 0 : 2));
  assert.deepEqual(sent.filter((s) => [1005, 1010, 1011].includes(s.id)).map((s) => `${s.id}:${cell(s)}`),
    ['1005:5', '1010:-', '1011:-'], '三列按换人先后排在第 5 棒下方，中间那位零统计的也不能被吞');
  assert.equal(sent.filter((s) => s.bo != null).length, 9);
});

test('#444 换人前冻结先发：_r() 之后 lineupStarterIds 记的是换人前的名单', () => {
  const { home } = play([[4, 1005, 1010]]);
  assert.deepEqual(home.lineupStarterIds.value, [1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, null, null, null],
    '第 5 格的先发必须仍是 1005，否则 br() 会拿替补当先发、棒次与行序一起错位');
  assert.equal(home.slotSubHistory.value[4], null,
    '第一次换人不写历史：被换下者就是先发本身，由 ① 分支涵盖，避免重复列');
  const twice = play([[4, 1005, 1010], [4, 1010, 1011]]);
  assert.deepEqual(twice.home.slotSubHistory.value[4], [1010], '第二次换人才进历史（他是「曾任占用者」）');
});
