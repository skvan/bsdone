// 垒球 DH/自由人换人缺口回归（2026-11-10）：
// 1) 垒球 11 人（P+DH+八守位+XF）投手已在打序：P↔野手、DH↔野手都是普通守位互换，不取消 DH；
// 2) 棒球/垒球 10 人 DH 取消仍按净变化落 DH_CANCEL，DH 移防导致投手进打序的路径不能漏标；
// 3) 垒球不适用 MLB OBR 先发 DH 已打击门禁，canCancelDh 不再弹棒球规则提醒；
// 4) 页面接线：DH 卡片拖到守备位/拖离卡片在“投手已在打序”时走互换或明确提示。
// 运行：node --test tests/liveGameDhSoftball.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  diffTeamState,
  buildSubstitutionEvents,
  canCancelDh,
} from '../src/utils/liveSubstitutionMode.js';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = fs.readFileSync(SRC_PATH, 'utf8').replace(/\r\n/g, '\n');

const player = (id, name, position) => ({ id, name, position, number: String(id) });
const team = (lineup, bench = [], extra = {}) => ({
  teamId: 7,
  lineup,
  bench,
  fpId: 0,
  fpRow: null,
  unavailable: [],
  starterIds: Array.from({ length: 12 }, (_, i) => Number(lineup[i]?.id) || null),
  slotHistory: Array.from({ length: 12 }, () => []),
  ...extra,
});
const eventsOf = (diff) => buildSubstitutionEvents([diff], {
  transactionId: 'tx-softball-dh', inning: 1, topBottom: 'top',
});

/** 从 LiveGame.js 切出函数体（含 async/默认参数）。 */
function sliceFunction(header) {
  const at = src.indexOf(header);
  assert(at >= 0, `找不到 ${header}，元件结构可能已变动`);
  const open = src.indexOf('{', at);
  assert(open > at, `${header} 缺少函数体`);
  let depth = 0;
  for (let i = open; i < src.length; i++) {
    const c = src[i];
    if (c === '"' || c === "'" || c === '`') {
      for (i++; i < src.length; i++) {
        if (src[i] === '\\') i++;
        else if (src[i] === c) break;
      }
      continue;
    }
    if (c === '/' && src[i + 1] === '/') {
      while (i < src.length && src[i] !== '\n') i++;
      continue;
    }
    if (c === '/' && src[i + 1] === '*') {
      i = src.indexOf('*/', i) + 1;
      continue;
    }
    if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return src.slice(at, i + 1);
  }
  throw new Error(`${header} 括号不闭合`);
}

test('垒球不适用 MLB OBR：先发 DH 未打击也放行，且无规则提醒', () => {
  const dh = {
    ...player(5, 'DH丙', 'DH'),
    stats: { batting: { ab: 0, bb: 0, hbp: 0, sf: 0, sh: 0, io: 0 } },
  };
  const roster = team([dh], [], { starterIds: [5], roster: [dh] });
  assert.deepEqual(canCancelDh({ team: roster, opponentPlayers: [], sport: 'SOFTBALL' }), {
    ok: true,
    warning: '',
  });
});

test('垒球没有 DH 时仍提示，不因赛制短路吞掉基础校验', () => {
  const noDh = canCancelDh({
    team: { lineup: [], starterIds: [] },
    opponentPlayers: [],
    sport: 'SOFTBALL',
  });
  assert.equal(noDh.ok, false);
  assert.match(noDh.warning, /没有 DH/);
});

test('棒球 DH 移防（DH 进守备、投手进打序）：补标 DH_CANCEL', () => {
  const before = team(
    [player(1, 'SS甲', 'SS'), player(3, '三垒乙', '3B'), player(5, 'DH丙', 'DH')],
    [],
    { fpId: 20, fpRow: player(20, '投手丁', 'P'), starterIds: [1, 3, 5] },
  );
  const after = team(
    [player(20, '投手丁', 'P'), player(3, '三垒乙', '3B'), player(5, 'DH丙', 'SS')],
    [],
    { fpId: 20, starterIds: [1, 3, 5] },
  );
  const diff = diffTeamState(before, after, { gameMode: 'BASEBALL' });
  assert.ok(diff.dhCancel, 'DH 移防后打线已无 DH 且投手进打序，应识别取消 DH');
  assert.equal(diff.dhCancel.slot, 1);
  assert.equal(diff.dhCancel.outPlayerId, 1);
  assert.equal(diff.dhCancel.inPlayerId, 20);
  const events = eventsOf(diff);
  assert.ok(events.some((e) => e.changeType === 'DH_CANCEL' && e.slot === 1 && e.inPlayerId === 20));
});

test('垒球 11 人 P↔野手互换：普通守位调整，不标 DH_CANCEL', () => {
  const before = team(
    [player(1, '投手甲', 'P'), player(2, '游击乙', 'SS'), player(5, 'DH丙', 'DH')],
    [],
    { fpId: 1 },
  );
  const after = team(
    [player(1, '投手甲', 'SS'), player(2, '游击乙', 'P'), player(5, 'DH丙', 'DH')],
    [],
    { fpId: 2 },
  );
  const diff = diffTeamState(before, after, { gameMode: 'SOFTBALL' });
  assert.equal(diff.dhCancel, null);
  assert.deepEqual(diff.substitutions, []);
  assert.deepEqual(diff.positionChanges.map((p) => [p.slot, p.outPosition, p.inPosition]), [
    [1, 'P', 'SS'],
    [2, 'SS', 'P'],
  ]);
  assert.ok(diff.pitchingChange);
  const events = eventsOf(diff);
  assert.ok(!events.some((e) => e.changeType === 'DH_CANCEL'));
  assert.ok(events.some((e) => e.changeType === 'PITCHING_CHANGE'));
});

test('垒球 11 人 DH↔野手互换：DH 守位换人，整条打线仍保留 DH', () => {
  const before = team([player(1, '游击甲', 'SS'), player(5, 'DH丙', 'DH')], [], { fpId: 9 });
  const after = team([player(1, '游击甲', 'DH'), player(5, 'DH丙', 'SS')], [], { fpId: 9 });
  const diff = diffTeamState(before, after, { gameMode: 'SOFTBALL' });
  assert.equal(diff.dhCancel, null);
  assert.deepEqual(diff.substitutions, []);
  assert.deepEqual(diff.positionChanges.map((p) => [p.playerId, p.outPosition, p.inPosition]), [
    [1, 'SS', 'DH'],
    [5, 'DH', 'SS'],
  ]);
});

test('页面接线：投手已在打序时不走棒球取消 DH 流程，DH 拖离改为明确提示', () => {
  assert.ok(
    src.includes('!oa(y)&&y.some(player=>Nn(player.position)==="DH")'),
    'P↔野手互换需在投手已在打序（垒球 11 人）时跳过取消 DH',
  );
  const defense = sliceFunction('async function confirmDhEnterDefense(');
  assert.ok(defense.includes('if(oa(ctx.lineup.value))'), 'DH 拖到守备位需区分投手已在打序');
  assert.ok(defense.includes('target.position="DH"'), 'DH 与守备员应互换守位（对方转 DH）');
  const cancel = sliceFunction('async function requestCancelDh(');
  assert.ok(cancel.includes('if(oa(ctx.lineup.value))'), '投手已在打序时应阻止取消 DH');
  assert.ok(cancel.includes('dhCancelPitcherInOrder'), '拖离 DH 卡片需给明确提示');
  const rotation = sliceFunction('async function applyFieldRotation(');
  assert.ok(rotation.includes('N.value!=="SOFTBALL"'), '垒球 11 人轮转含 1 号位不应取消 DH');
});
