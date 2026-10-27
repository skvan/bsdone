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
  orderLengthOf,
  plateAppearances,
  teamPlateAppearances,
  resolveBattingSlots,
  deriveBatterIndex,
  starterIdsFromLineup,
  starterIdsOfOrderedStarters,
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

test('4 当前打者回溯：该队已完成打席数 mod 真实打线人数；未完成的那半个打席不算', () => {
  assert.equal(teamPlateAppearances(STAT_ROWS, TEAM), 8, '投手列的 ab 不计入，只数打者列的 8 个 PA');
  assert.equal(deriveBatterIndex(12, teamPlateAppearances(STAT_ROWS, TEAM)), 8, '第 9 棒正在打击');
  assert.equal(deriveBatterIndex(11, 12), 1, '软式 11 人圈一轮回到第 2 格');
  assert.equal(orderLengthOf(STAT_ROWS, TEAM), 12, '样本 12 棒都带棒次 → 真实打线人数 12');
  assert.equal(deriveBatterIndex(0, 5), 0, '名单还没建好时不越界');
  assert.equal(deriveBatterIndex(12, null), 0, '没有打席记录就是第 1 棒，不该 NaN');
});

test('5 PA 口径与存档 payload 同构：服务端 pa 为空时按 ab+bb+hbp+sf+sh+io 回填', () => {
  assert.equal(plateAppearances({ pa: 3 }), 3);
  assert.equal(plateAppearances({ pa: null, ab: 2, bb: 1, hbp: 1, sf: 0, sh: 0, io: 1 }), 5,
    'payload 里 pa 本就是推导值，读取端必须用同一个公式');
  assert.equal(plateAppearances({ pa: 0, ab: 5 }), 0, '显式 0 不能被推导值覆盖');
  assert.equal(plateAppearances({ pa: '', ab: 3, bb: 1 }), 4, '空串按缺栏走推导：Number(\'\')===0 会把该队 PA 少数');
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

const restoreNames = ['boStarterIds', 'boTeamPA', 'boOrderLength', 'boBatterIndex', 'g', 't', 'q', 'X', 'l', 'fe', 'le'];
const runRestore = (scope) => new Function(...restoreNames, doRestoreBlock())(
  ...restoreNames.map((n) => scope[n]),
);
const lineupOf = (from, count) => Array.from({ length: count }, (_, i) => ({ id: from + i }));
const baseScope = (over = {}) => ({
  boStarterIds: starterIdsOfOrderedStarters,
  boTeamPA: teamPlateAppearances,
  boOrderLength: orderLengthOf,
  boBatterIndex: deriveBatterIndex,
  g: STAT_ROWS,
  t: { homeTeamId: TEAM, awayTeamId: 58 },
  q: { value: lineupOf(1001, 12) },
  X: { value: lineupOf(2001, 12) },
  l: { batterIndex: { away: 0, home: 0 } },
  // 真实初值：store 里 homeLineupStarterIds/awayLineupStarterIds 都是 x(Array(12).fill(null))
  // （component 的 fe/le 由 :5065 解构自这两个 ref），不是 null/[]。
  fe: { value: Array(12).fill(null) },
  le: { value: Array(12).fill(null) },
  ...over,
});

test('8 元件层：Do() 的还原段照原文执行，指针不再停在第 1 棒、先发打序被填回', () => {
  const scope = baseScope();
  runRestore(scope);
  assert.equal(scope.l.batterIndex.home, 8, 'H 队已完成 8 个打席 → 第 9 棒在场，指针不该停在第 1 棒');
  assert.equal(scope.l.batterIndex.away, 0, '客队样本只有 1 个带棒次的打者 → 真实打线人数 1，7 个打席轮回来仍是同一人（模 1 恒 0）；旧算法按补齐的 12 取模会指向第 8 格——那格是补出来的板凳');
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

test('10 元件层：服务端没有任何棒次数据时不改指针、不凭空填先发槽位', () => {
  const scope = baseScope({ q: { value: [] }, X: { value: [] }, l: { batterIndex: { away: 3, home: 4 } }, g: [] });
  runRestore(scope);
  assert.equal(scope.l.batterIndex.home, 4, '名单空时不该把指针乱改成 0');
  assert.equal(scope.l.batterIndex.away, 3);
  assert.ok(scope.fe.value.every((e) => e === null), '服务端没有任何棒次数据时不凭空填人（先发槽位仍全空，等用户确认阵容）');
});

/**
 * 11) 真实 b() 的占位渲染：第 7 案把 b() 换成 stub，挡不住「占位行显示成空白/undefined」。
 *     这里把 LiveGame.js 里 b() 的原文取出来真的执行，钉住：姓名兜底「队员」、背号可回落到统计行、
 *     统计行数值不因缺 roster 而被抹成 0。
 */
test('11 元件层：真实 b() 对占位球员的渲染与统计承接', () => {
  const i = src.indexOf(',b=(P,A,T,Y)=>{');
  assert(i > 0, '找不到 b() 构造段，Do() 内的建档函数可能已改名');
  const j = src.indexOf('\n      ,w=(P,A)=>{', i);
  assert(j > i, '找不到 b() 构造段终点');
  const text = src.slice(i + 1, j).trim();
  const b = new Function('Pt', 'return ' + text)(( ) => ({ batting: {}, pitching: {}, fielding: {} }));
  const row = {
    teamId: TEAM, playerId: 9999, isPitcher: 0, battingOrder: 4, position: '3B', number: '88',
    pa: 4, ab: 3, hbp: 1, bb: 0, sf: 0, sh: 0, r: 1, h: 2, rbi: 2,
  };
  const built = b({ id: 9999 }, TEAM, row, 3);
  assert.equal(built.name, '队员', 'roster 查不到时用 b() 的姓名兜底（不得是 undefined/空串）');
  assert.equal(built.number, '88', '背号可回落到统计行，占位行不该整栏空白');
  assert.equal(built.position, '3B', '守位取服务端统计行，保证守备数据仍挂在本人身上');
  assert.equal(built.stats.batting.ab, 3, 'AB 承接统计行（不得被 ??0 抹除）');
  assert.equal(built.stats.batting.hbp, 1, 'HBP 承接统计行');
  assert.equal(built.stats.batting.r, 1, 'R 承接统计行');
});

/**
 * 12) 投手列不得混入打席合计：线上响应实证（game202 dump）同一 playerId 同时存在
 *     isPitcher=1 列（ab 是「被打者数」）与 isPitcher=0 打者列（pa 才是本人打席）。
 *     若不排除投手列，指针回溯会一次跳好几格。
 */
test('12 纯函数层：同一球员的投手列不计入打席合计（真实响应形态）', () => {
  const rows = [
    { teamId: TEAM, playerId: 1180, isPitcher: 1, battingOrder: null, pa: 0, ab: 40, bb: 12, hbp: 3, sf: 0, sh: 0 },
    { teamId: TEAM, playerId: 1180, isPitcher: 0, battingOrder: 1, pa: 4, ab: 1, bb: 1, hbp: 2, sf: 0, sh: 0 },
    { teamId: TEAM, playerId: 1181, isPitcher: 0, battingOrder: 2, pa: 3, ab: 2, bb: 1, hbp: 0, sf: 0, sh: 0 },
    { teamId: TEAM + 1, playerId: 2180, isPitcher: 0, battingOrder: 1, pa: 7, ab: 5, bb: 2, hbp: 0, sf: 0, sh: 0 },
  ];
  assert.equal(teamPlateAppearances(rows, TEAM), 7, '只累加本队打者列（4+3），投手列 ab=40 与它队列都不算');
  assert.notEqual(teamPlateAppearances(rows, TEAM), 47, '常见错法：把投手「被打数」当打席');
});

/**
 * 13) 真实 gr() 与工具版逐格对齐（评审 S-2：同一条口径写两份，必须防止漂移）。
 *     直接抽出 LiveGame.js 里 gr()/Et() 的原文执行，与 starterIdsFromLineup() 逐格比对。
 *     gr() 将来改动（长度、Et() 负 id 过滤、id>0 判定）时本案会红，提醒同步工具档。
 */
test('13 元件层：工具版反推与真实 gr() 逐格一致', () => {
  const gi = src.indexOf('function gr(e){');
  assert(gi > 0, '找不到 gr() 本体');
  const gj = src.indexOf('\n    }', gi);
  assert(gj > gi, '找不到 gr() 终点');
  const ei = src.indexOf('function Et(e){');
  assert(ei > 0 && ei < gi, '找不到 Et() 本体（gr() 依赖它过滤占位）');
  const ej = src.indexOf('\n    }', ei);
  const lineup = [
    { id: 1001 }, { id: -7 }, null, { id: 1004 }, { id: '1005' },
    { id: 0 }, { id: NaN }, { id: 1008 },
  ];
  const scope = { Ee: () => ({ lineup: { value: lineup } }) };
  const real = new Function('Ee', src.slice(ei, ej + 6) + '\n' + src.slice(gi, gj + 6) + '\nreturn gr("any");')(scope.Ee);
  const mine = starterIdsFromLineup(lineup);
  assert.deepEqual(real, mine, '工具版与页面 gr() 的反推结果必须逐格相同（否则 No() 送出的 batting_order 会与现场不一致）');
  assert.deepEqual(mine, [1001, null, null, 1004, 1005, null, null, 1008, null, null, null, null],
    '负 id/0/NaN → null，数字字符串仍收下，长度固定 12');
});

/**
 * 14) 合并前评审的 Blocker 回归钉：模数必须是「真实打线人数」，不能用补齐后的数组长度。
 *     未开 EP 制的棒球 9 人打线，w() 会补到 12 格；按 12 取模时 PA=9 会把下一次打席
 *     交给 battingOrder=null 的板凳补位格（那人在秩序册里 → 后端名单闸门放行 → 凭空多一列统计）。
 */
const NINE_ROWS = Array.from({ length: 9 }, (_, i) => ({
  teamId: TEAM, playerId: 1001 + i, isPitcher: 0, battingOrder: i + 1,
  pa: 1, ab: 1, bb: 0, hbp: 0, sf: 0, sh: 0, io: 0,
}));
const ROSTER_15 = Array.from({ length: 15 }, (_, i) => ({ id: 1001 + i, name: `球员${1001 + i}` }));

test('14 模数口径：9 人棒球打线补齐到 12 格时，指针仍只能落在带棒次的格上', () => {
  const slots = resolveBattingSlots({ statRows: NINE_ROWS, roster: ROSTER_15, teamId: TEAM, sport: 'BASEBALL' });
  assert.equal(slots.length, 12, '补齐上限不变（设计 §六：恢复补齐上限 棒 12）');
  assert.equal(orderLengthOf(NINE_ROWS, TEAM), 9, '真实打线人数 = 9，取模必须用它');
  assert.equal(teamPlateAppearances(NINE_ROWS, TEAM), 9);
  for (const [pa, want] of [[0, 0], [8, 8], [9, 0], [10, 1], [18, 0], [25, 7]]) {
    const idx = deriveBatterIndex(orderLengthOf(NINE_ROWS, TEAM), pa);
    assert.equal(idx, want, `PA=${pa} 应指向第 ${want + 1} 棒（现场 fo() 按 9 推进的结果）`);
    assert.ok(slots[idx].battingOrder != null, `PA=${pa} 的落点 index=${idx} 必须带真实棒次，不能是补出来的板凳格`);
  }
  // 负对照：旧算法（拿补齐后的 12 当模数）在 PA=9 时正好踩到没有棒次的格
  const wrongWay = deriveBatterIndex(slots.length, 9);
  assert.equal(wrongWay, 9);
  assert.equal(slots[wrongWay].battingOrder, null, '旧口径的失败点：第 10 格是 roster 补位，本场从没打过');
});

/** 元件层同一场景照原文执行，钉住 Do() 真的改用真实打线人数当模数 */
test('15 元件层：Do() 还原段对 9 人棒球打线不再指向板凳格', () => {
  const slots12 = resolveBattingSlots({ statRows: NINE_ROWS, roster: ROSTER_15, teamId: TEAM, sport: 'BASEBALL' });
  const scope = baseScope({
    g: NINE_ROWS,
    t: { homeTeamId: TEAM, awayTeamId: 58 },
    q: { value: slots12.map((x) => ({ id: x.playerId })) },
    X: { value: [] },
    l: { batterIndex: { away: 7, home: 3 } },
    fe: { value: Array(12).fill(null) },
    le: { value: Array(12).fill(null) },
  });
  runRestore(scope);
  assert.equal(scope.l.batterIndex.home, 0, '9 人打线打完 9 个 PA → 该轮回到第 1 棒，而不是第 10 格板凳');
  assert.equal(scope.q.value[scope.l.batterIndex.home].id, 1001, '指针落在第 1 棒本人身上');
  assert.equal(scope.l.batterIndex.away, 7, '客队没有任何带棒次的行 → 指针原封不动（7 是我故意给的不对称初值，能抓住主客写反）');
  assert.equal(scope.fe.value[0], 1001, '主队先发打序必须回填自「主队名单 q」；若与客队 le 写反，这里仍是 null');
  assert.ok(scope.le.value.every((v) => v === null), '客队名单为空 → 客队先发打序保持全 null，不能被主队数据污染');
  assert.deepEqual(scope.fe.value.slice(0, 9), [1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009],
    '9 个带棒次的人必须按槽位填回先发打序');
  assert.ok(scope.fe.value.slice(9).every((v) => v === null),
    'w() 补齐进来的第 10~12 格板凳不得被声明为先发（否则保存时会被写成第 10/11/12 棒）');
});

/**
 * 16) 板凳补位冒充先发的污染回归钉（#444/#445 同源）。
 *     用真实 No() 原文跑过：lineupStarterIds 一旦含补齐的板凳，No() 的①分支就按「槽位+1」
 *     给他们送上棒次（ab=0 也照送），于是 orderLengthOf() 从 9 变 12、回溯指针开始轮空档。
 *     这里钉两件事：① gr() 原口径确实会把补位格填进去（污染源存在，不是臆想）；
 *     ② 重建路径改用的 starterIdsOfOrderedStarters() 必须把他挡成 null。
 */
test('16 污染钉：补齐进来的板凳不能被声明为先发（gr() 原口径对照）', () => {
  const slots12 = resolveBattingSlots({ statRows: NINE_ROWS, roster: ROSTER_15, teamId: TEAM, sport: 'BASEBALL' });
  const lineup = slots12.map((x) => ({ id: x.playerId }));
  assert.equal(orderLengthOf(NINE_ROWS, TEAM), 9, '本场真实打线 9 人');
  assert.deepEqual(grLike(lineup).slice(9), [1010, 1011, 1012],
    'gr() 原口径会把 roster 补位格当先发——这就是 dev 现况的污染源（Do() 后保存时 br() 会全量反推）');
  assert.deepEqual(starterIdsOfOrderedStarters(lineup, NINE_ROWS, TEAM),
    [1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, null, null, null],
    '重建路径必须只承认带棒次的 9 人；板凳留 null，No() 就不会替他们凭空造棒次');
  // 与快照路径对齐：带回来的先发本就只含有棒次的人，两者必须一致
  assert.deepEqual(starterIdsOfOrderedStarters(lineup, NINE_ROWS, TEAM),
    [...Array.from({ length: 9 }, (_, i) => 1001 + i), null, null, null],
    '重建路径的输出必须与快照路径带回的先发同形（对齐，而不是另立一套口径）');
});

/** gr() 的等价复刻（只用于第 16 案制造对照；真正的 gr() 口径由第 13 案钉住） */
const grLike = (lineup) => Array.from({ length: 12 }, (_, i) => {
  const r = lineup[i];
  if (!r || (r.id != null && r.id < 0)) return null;
  const p = Number(r.id);
  return Number.isFinite(p) && p > 0 ? p : null;
});
