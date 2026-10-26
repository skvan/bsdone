/**
 * #445（与 #444 同源）：无快照重建录入现场时，打序槽位不丢位、当前打者指针可回溯
 *
 * 两层验证：
 *   1) 纯函数层：src/utils/liveGameBattingOrder.js 的排序/回溯语义
 *   2) 元件层：把 LiveGame.js 里 w() 的重建段与 Do() 的还原段**原文**绑上变量真的执行，
 *      证明调用点确实接上了纯函数（只测工具函式挡不住「写了但没挂上」的回归）
 *
 * 判别力自检：把 LiveGame.js 换回未修版（w() 带 .filter(Boolean)、Do() 无还原段），第 5 项起应转红。
 * 运行：cd bsball-frontend && node --test tests/liveGameBattingOrderRestore445.test.mjs
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  lineupSlotCount,
  plateAppearances,
  teamPlateAppearances,
  resolveBattingSlots,
  deriveBatterIndex,
  starterIdsFromLineup,
} from '../src/utils/liveGameBattingOrder.js';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = readFileSync(SRC_PATH, 'utf8');

const TEAM = 57;
/** ID237 H 队等价场景：12 棒，第 4 棒(1004) 已被移出 roster，场上已完成 8 个打席 */
const STAT_ROWS = Array.from({ length: 12 }, (_, i) => ({
  teamId: TEAM,
  playerId: 1001 + i,
  isPitcher: 0,
  battingOrder: i + 1,
  pa: i < 8 ? 1 : 0,
  ab: 0, bb: 0, hbp: 0, sf: 0, sh: 0, io: 0,
  number: String(1001 + i),
})).concat([
  // 投手列的 ab 是「被打者数」，不能混进打序回溯
  { teamId: TEAM, playerId: 9001, isPitcher: 1, battingOrder: null, pa: null, ab: 40, bb: 5, hbp: 2, sf: 1, sh: 1, io: 0 },
  // 客队同 id 段，验证不会串队
  { teamId: 58, playerId: 2001, isPitcher: 0, battingOrder: 1, pa: 7, ab: 0, bb: 0, hbp: 0, sf: 0, sh: 0, io: 0 },
]);
const ROSTER = Array.from({ length: 12 }, (_, i) => ({ id: 1001 + i, name: `球员${1001 + i}` }))
  .filter((p) => p.id !== 1004);

test('1 打序槽位数：软式 11、其余 12（与 Do() 原口径一致）', () => {
  assert.equal(lineupSlotCount('SOFTBALL'), 11);
  assert.equal(lineupSlotCount('BASEBALL'), 12);
  assert.equal(lineupSlotCount(undefined), 12);
});

test('2 不丢位：roster 解析不到的球员仍保留自己的棒次槽位，其后的人不移位', () => {
  const slots = resolveBattingSlots({ statRows: STAT_ROWS, roster: ROSTER, teamId: TEAM, sport: 'BASEBALL' });
  assert.equal(slots.length, 12, '12 棒就是 12 格，少一个人也不能塌成 11 格');
  assert.equal(slots[3].playerId, 1004, '第 4 格仍是第 4 棒');
  assert.equal(slots[4].playerId, 1005, '其后的人不被前移一格——这正是「当前打者指向错人」的机制');
  assert.deepEqual(slots.map((s) => s.battingOrder), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
});

test('3 负对照：旧语义（filter(Boolean) 丢位）会让同一格指向另一个人', () => {
  const oldWay = resolveBattingSlots({ statRows: STAT_ROWS, roster: ROSTER, teamId: TEAM, sport: 'BASEBALL' })
    .filter((s) => ROSTER.some((p) => Number(p.id) === s.playerId));
  assert.equal(oldWay.length, 11);
  assert.equal(oldWay[3].playerId, 1005, '丢位后第 4 格变成第 5 棒 → 指针不动就会记错人');
});

test('4 当前打者回溯：该队已完成打席数 mod 名单长度；未完成的那半个打席不算', () => {
  assert.equal(teamPlateAppearances(STAT_ROWS, TEAM), 8, '投手列的 ab 不计入，只数打者列的 8 个 PA');
  assert.equal(deriveBatterIndex(12, teamPlateAppearances(STAT_ROWS, TEAM)), 8, '第 9 棒正在打击');
  assert.equal(deriveBatterIndex(11, 12), 1, '软式 11 人圈一轮回到第 2 格');
  assert.equal(deriveBatterIndex(0, 5), 0, '名单还没建好时不越界');
  assert.equal(deriveBatterIndex(12, null), 0, '没有打席记录就是第 1 棒，不该 NaN');
});

test('5 PA 口径与存档 payload 同构：服务端 pa 为空时按 ab+bb+hbp+sf+sh+io 回填', () => {
  assert.equal(plateAppearances({ pa: 3 }), 3);
  assert.equal(plateAppearances({ pa: null, ab: 2, bb: 1, hbp: 1, sf: 0, sh: 0, io: 1 }), 5,
    'payload 里 pa 本就是推导值，读取端必须用同一个公式');
  assert.equal(plateAppearances({ pa: 0, ab: 5 }), 0, '显式 0 不能被推导值覆盖');
});

test('6 服务端没有任何 battingOrder>0 的行时返回空名单（保留「请先确认先发阵容」的拦截）', () => {
  const rows = [{ teamId: TEAM, playerId: 1001, isPitcher: 0, battingOrder: null, pa: 1 }];
  assert.deepEqual(resolveBattingSlots({ statRows: rows, roster: ROSTER, teamId: TEAM, sport: 'BASEBALL' }), [],
    '不能拿 roster 顺序凭空当真实棒次');
});

/** 从 LiveGame.js 抓「锚点起 → 结束串」之间的原文，绑变量执行（与 #387 往返测试同一手法） */
function slice(startNeedle, endNeedle, label) {
  const i = src.indexOf(startNeedle);
  assert(i >= 0, `找不到 ${label} 起点，元件结构可能已变动`);
  const j = src.indexOf(endNeedle, i);
  assert(j > i, `找不到 ${label} 终点`);
  return src.slice(i, j + endNeedle.length);
}

test('7 元件层：w() 真的改用不丢位的重建，且为解析不到的球员用统计行造占位', () => {
  const block = slice(',w=(P,A)=>{', '\n      };', 'w() 重建段');
  const body = block.slice(block.indexOf('=') + 1).replace(/;\s*$/, '');
  const built = [];
  const scope = {
    g: STAT_ROWS,
    m: new Map(STAT_ROWS.filter((r) => r.teamId === TEAM).map((r) => [`${TEAM}_${r.playerId}`, r])),
    N: { value: 'BASEBALL' },
    b: (P, A, T, Y) => {
      built.push({ id: P?.id, order: Y, hasRow: !!T });
      return { id: Number(P.id), teamId: A, name: P.name ?? '队员', number: P.number ?? '-' };
    },
    boResolveSlots: resolveBattingSlots,
  };
  const names = Object.keys(scope);
  const lineup = new Function(...names, 'return ' + body)(...names.map((n) => scope[n]))(ROSTER, TEAM);
  assert.equal(lineup.length, 12, '名单长度 = 打序格数，不因 roster 缺人而缩短');
  assert.equal(lineup[3].id, 1004, '第 4 格是第 4 棒本人');
  assert.equal(lineup[3].name, '队员', '解析不到 roster 时用统计行造占位，姓名走 b() 的兜底');
  assert.equal(built[3].hasRow, true, '占位球员仍带着自己的服务端统计行（PA/AB 不会凭空消失）');
  assert.ok(!/filter\(Boolean\)/.test(body), 'w() 不该再出现丢位的 filter(Boolean)');
});

/** 取 Do() 重建行之后的还原段原文（从锚点到下一个语句 "const P=Y=>new Set" 之前） */
function doRestoreBlock() {
  const anchor = 'q.value=w(a,t.homeTeamId),X.value=w(s,t.awayTeamId);';
  assert.ok(src.includes(anchor), '找不到 Do() 的重建行，元件结构可能已变动');
  const start = src.indexOf(anchor) + anchor.length;
  const end = src.indexOf('const P=Y=>new Set', start);
  assert(end > start, '找不到 Do() 还原段的终点');
  return src.slice(start, end).split('\n')
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('//'))
    .join('\n');
}

const restoreNames = ['boStarterIds', 'boTeamPA', 'boBatterIndex', 'g', 't', 'q', 'X', 'l', 'fe', 'le'];
const runRestore = (scope) => new Function(...restoreNames, doRestoreBlock())(
  ...restoreNames.map((n) => scope[n]),
);
const lineupOf = (from, count) => Array.from({ length: count }, (_, i) => ({ id: from + i }));
const baseScope = (over = {}) => ({
  boStarterIds: starterIdsFromLineup,
  boTeamPA: teamPlateAppearances,
  boBatterIndex: deriveBatterIndex,
  g: STAT_ROWS,
  t: { homeTeamId: TEAM, awayTeamId: 58 },
  q: { value: lineupOf(1001, 12) },
  X: { value: lineupOf(2001, 12) },
  l: { batterIndex: { away: 0, home: 0 } },
  fe: { value: null },
  le: { value: [] },
  ...over,
});

test('8 元件层：Do() 的还原段照原文执行，指针不再停在第 1 棒、先发打序被填回', () => {
  const scope = baseScope();
  runRestore(scope);
  assert.equal(scope.l.batterIndex.home, 8, 'H 队已完成 8 个打席 → 第 9 棒在场，指针不该停在第 1 棒');
  assert.equal(scope.l.batterIndex.away, 7, '客队按其 PA 回溯（7 个 PA → 第 8 格）');
  assert.deepEqual(scope.fe.value, Array.from({ length: 12 }, (_, i) => 1001 + i),
    '先发打序回填成 12 格真实棒次，No() 才不会把 battingOrder 送成 null（#444 的「-」）');
  assert.equal(scope.le.value.length, 12);
});

test('9 元件层：已有先发打序时不被重建结果覆盖（快照路径更准）', () => {
  const kept = Array.from({ length: 12 }, (_, i) => 5001 + i);
  const scope = baseScope({ fe: { value: [...kept] } });
  runRestore(scope);
  assert.deepEqual(scope.fe.value, kept, '快照回来的先发不能被就地反推顶掉');
  assert.equal(scope.l.batterIndex.home, 8, '指针回溯仍照常进行');
});

test('10 元件层：名单没重建成功时不改指针，也不会踩到 fe=null 的 TypeError', () => {
  const scope = baseScope({ q: { value: [] }, X: { value: [] }, l: { batterIndex: { away: 3, home: 4 } }, g: [] });
  runRestore(scope);
  assert.equal(scope.l.batterIndex.home, 4, '名单空时不该把指针乱改成 0');
  assert.equal(scope.l.batterIndex.away, 3);
  const brLine = src.split('\n').find((l) => l.includes('&&(le.value=gr("away"))'));
  assert.ok(brLine, '找不到 br() 本体');
  assert.ok(brLine.includes('(le.value??[])') && brLine.includes('(fe.value??[])'),
    'br() 仍直接对初值 null 的 fe 调 .every()，无快照重建会先抛 TypeError（#445 现场走不进还原）');
});
