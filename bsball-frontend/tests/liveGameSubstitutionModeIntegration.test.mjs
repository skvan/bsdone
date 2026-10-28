/**
 * 换人事务模式集成护栏（#444 后续）：
 * 1) No() 不能把「已进入打序的投手」（取消 DH / 二刀流）从打击行分流掉；
 * 2) 被换下者的最后守位要能从 lastPosition 回填，不能落成空。
 *
 * 用 LiveGame.js 原文切片执行 No()/Eo()，不复制业务逻辑。
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = fs.readFileSync(SRC_PATH, 'utf8').replace(/\r\n/g, '\n');

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

const DEPS = ['Et', 'Oo', 'Lo', '$z', 'ue', 'Pt', 'bs', 'mc', 'dc', 'mr', 'vr', 'gr', 'br', '_s', 'Eo'];
const code = DEPS.map((n) => sliceFn('function ' + n + '(')).join('\n')
  + '\n' + sliceFn('function No(e,t){');
const make = new Function('Ee', '_', 'ft', 'Nn', 'le', 'fe',
  'return (function(){' + code + '\nreturn {No,Eo};})()');

const ref = (v) => ({ value: v });
const zero = { ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, sf: 0, sh: 0, sb: 0, doubles: 0, triples: 0, hr: 0, gdp: 0, ibb: 0, io: 0 };
const stats = (b = {}, p = {}, f = {}) => ({
  batting: { ...zero, ...b },
  pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchSo: 0, ...p },
  fielding: { po: 0, a: 0, e: 0, defInn: 0, ...f },
});
const player = (id, pos, opts = {}) => ({
  id, name: '球员' + id, number: String(id), position: pos, teamId: 7,
  stats: stats(opts.batting, opts.pitching, opts.fielding),
});

function buildHome() {
  const lineup = [
    player(1, 'C', { batting: { ab: 3 } }),
    player(2, '1B', { batting: { ab: 3 } }),
    player(3, '2B', { batting: { ab: 3 } }),
    player(200, 'P', { pitching: { np: 30 }, batting: {} }),
    player(5, 'SS', { batting: { ab: 3 } }),
    player(6, 'LF', { batting: { ab: 3 } }),
    player(7, 'CF', { batting: { ab: 3 } }),
    player(8, '3B', { batting: { ab: 3 } }),
    player(9, 'RF', { batting: { ab: 3 } }),
  ];
  const bench = [
    { ...player(100, '', { fielding: {} }), lastPosition: 'DH' },
    { ...player(300, '', { fielding: { po: 1 } }), lastPosition: 'C' },
  ];
  return {
    teamId: 7,
    lineup: ref(lineup),
    bench: ref(bench),
    roster: ref([...lineup, ...bench]),
    fpRow: ref(null),
    fpId: ref(200),
    unavail: ref([100]),
    lineupStarterIds: ref([1, 2, 3, 100, 5, 6, 7, 8, 9, null, null, null]),
    slotSubHistory: ref(Array.from({ length: 12 }, () => [])),
  };
}

test('取消 DH / 投手进打序：No() 仍给投手一条打击行（不是只留投手行）', () => {
  const home = buildHome();
  const away = {
    teamId: 8,
    lineup: ref([]), bench: ref([]), roster: ref([]), fpRow: ref(null), fpId: ref(null),
    unavail: ref([]), lineupStarterIds: ref(Array(12).fill(null)),
    slotSubHistory: ref(Array.from({ length: 12 }, () => [])),
  };
  const { No } = make(
    (side) => (side === 'away' ? away : home),
    { homeTeamId: 7, awayTeamId: 8 },
    () => {},
    (v) => String(v ?? '').trim().toUpperCase(),
    away.lineupStarterIds, home.lineupStarterIds,
  );
  const rows = No('home', 7);
  const pitcherBatting = rows.find((r) => Number(r.playerId) === 200 && r.isPitcher === 0);
  const pitcherRow = rows.find((r) => Number(r.playerId) === 200 && r.isPitcher === 1);
  assert.ok(pitcherBatting, '进入打序的投手必须有打击行');
  assert.equal(pitcherBatting.position, 'P');
  assert.ok(pitcherRow, '同时仍要有投手行');
});

test('被换下者最后守位：starter 的 bench 副本靠 lastPosition 回填 DH/C', () => {
  const home = buildHome();
  const away = {
    teamId: 8,
    lineup: ref([]), bench: ref([]), roster: ref([]), fpRow: ref(null), fpId: ref(null),
    unavail: ref([]), lineupStarterIds: ref(Array(12).fill(null)),
    slotSubHistory: ref(Array.from({ length: 12 }, () => [])),
  };
  const { No } = make(
    (side) => (side === 'away' ? away : home),
    { homeTeamId: 7, awayTeamId: 8 },
    () => {},
    (v) => String(v ?? '').trim().toUpperCase(),
    away.lineupStarterIds, home.lineupStarterIds,
  );
  const rows = No('home', 7);
  const dh = rows.find((r) => Number(r.playerId) === 100 && r.isPitcher === 0);
  assert.ok(dh, '被换下的先发仍要留列');
  assert.equal(dh.position, 'DH', '先发 DH 的 lastPosition 不能为空');
  assert.equal(dh.battingOrder, 4);
  const benchOut = rows.find((r) => Number(r.playerId) === 300 && r.isPitcher === 0);
  assert.ok(benchOut, '有守备统计的板凳仍要留列');
  assert.equal(benchOut.position, 'C', '板凳最后守位靠 lastPosition 回填');
});

test('提交接线：save-live payload 带 substitutions，换人提交走 fromSubstitutionMode', () => {
  assert.match(src, /payload\.substitutions=e\.substitutions/);
  assert.match(src, /substitutions:events/);
  assert.match(src, /fromSubstitutionMode:!0/);
});

test('净零退出：恢复两队 before 状态与 runners，并用进入前历史快照回滚 undo', () => {
  assert.match(src, /!diffs\.some\(diff=>diff\.hasChanges\)[\s\S]{0,260}restoreSubstitutionTeam\("away",before\.away\)/);
  assert.match(src, /before\.runners&&applySubstitutionRunners\(before\.runners\)/);
  assert.match(src, /substitutionModeHistoryBase\.value=\$e\.value\.slice\(\)/);
  assert.match(src, /\$e\.value=\[\.\.\.substitutionModeHistoryBase\.value\]/);
});

test('撤销检查点：进入模式时压入 pre-mode 快照，提交后保留一条；Yl/Cf 携带先发槽与换人史', () => {
  assert.match(src, /de\("换人模式"\),substitutionModeUndoEntry/);
  assert.match(src, /substitutionModeUndoEntry\.value\?\[\.\.\.substitutionModeHistoryBase\.value,substitutionModeUndoEntry\.value\]/);
  assert.match(src, /homeLineupStarterIds:fe\.value,awaySlotSubHistory:Ae\.value,homeSlotSubHistory:ie\.value/);
  assert.match(src, /le\.value=Array\.isArray\(e\.awayLineupStarterIds\)/);
  assert.match(src, /ie\.value=Array\.isArray\(e\.homeSlotSubHistory\)/);
});
