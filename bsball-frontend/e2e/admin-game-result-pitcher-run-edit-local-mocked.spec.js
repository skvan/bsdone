import { test, expect } from '@playwright/test';

// 结果录入页：引擎场次（final）允许直接改投手 R/ER，保存时带人工覆写标记。
const GAME_ID = 10;
const HOME_ID = 2;
const AWAY_ID = 1;
const BATTER_POSITIONS = ['C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'DH'];

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  engineManaged: true,
  homeScore: 3,
  awayScore: 2,
  gameday: '2026-11-04',
  totalInnings: 9,
  venue: '测试球场',
  hasLiveSnapshot: true
};

function makePlayers(teamId, offset) {
  const batters = BATTER_POSITIONS.map((position, index) => ({
    id: offset + index + 1,
    name: `T${teamId}P${index + 1}`,
    number: String(index + 1),
    teamId,
    positions: [position],
    batHand: 'R',
    throwHand: 'R',
    status: 'active'
  }));
  const pitcher = {
    id: offset + 100,
    name: `T${teamId}PITCHER`,
    number: '99',
    teamId,
    positions: ['P'],
    batHand: 'R',
    throwHand: 'R',
    status: 'active'
  };
  return [...batters, pitcher];
}

function makeStats(teamId, players, statIdOffset) {
  return players.map((player, index) => {
    const isPitcher = player.positions[0] === 'P';
    const batterIndex = isPitcher ? null : index;
    return {
      id: statIdOffset + index, gameId: GAME_ID, teamId, playerId: player.id,
      isPitcher: isPitcher ? 1 : 0,
      battingOrder: isPitcher ? null : batterIndex + 1,
      listOrder: index,
      pitcherOrder: isPitcher ? 1 : null,
      position: player.positions[0], number: player.number,
      batHand: player.batHand, throwHand: player.throwHand,
      pa: 0, ab: 0, r: 0, h: 0, e: 0, bbHp: 0, sb: 0, so: 0, soSwing: 0, soLooking: 0,
      rbi: 0, hr: 0, insideParkHr: 0, doubles: 0, triples: 0, gdp: 0, sh: 0, sf: 0,
      fieldingGs: 1, defInn: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0,
      ip: 0, pitchR: 0, er: 0, pitchH: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0,
      pitchAb: 0, po: 0, a: 0, e: 0, tc: 0,
      pitchInsideParkHr: 0, pitchPa: 0, pitchBf: 0, np: 0, pitchBb: 0, pitchIbb: 0,
      pitchHbp: 0, wp: 0, bk: 0, go: 0, fo: 0, gs: 0, svo: 0, cg: 0, pg: 0,
      w: 0, l: 0, sv: 0, hld: 0
    };
  });
}

const awayPlayers = [
  ...makePlayers(AWAY_ID, 1000),
  {
    id: 1114, name: 'T1PITCHER2', number: '16', teamId: AWAY_ID,
    positions: ['P'], batHand: 'R', throwHand: 'R', status: 'active'
  },
  {
    id: 1115, name: 'T1PITCHER3', number: '15', teamId: AWAY_ID,
    positions: ['P'], batHand: 'R', throwHand: 'R', status: 'active'
  }
];
const homePlayers = makePlayers(HOME_ID, 2000);
const awayStats = makeStats(AWAY_ID, awayPlayers, 3000);
const homeStats = makeStats(HOME_ID, homePlayers, 4000);
{
  const source = awayStats.find((stat) => stat.playerId === 1114);
  const target = awayStats.find((stat) => stat.playerId === 1100);
  source.pitcherOrder = 2;
  source.np = 27;
  source.pitchPa = 11;
  source.pitchAb = 9;
  source.fo = 3;
  target.ip = 1.2;
  target.pitchR = 3;
  target.er = 3;
  target.np = 0;
  target.pitchPa = 0;
  target.pitchAb = 0;
  target.fo = 0;
}

async function setup(page) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  let sourceDeleted = false;
  let decisionReassigned = false;
  let decisionVoided = false;
  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];

    if (path === '/event/1') data = { id: 1, name: 'Result Pitcher Run E2E' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) {
      const list = [...awayStats, ...homeStats].filter((stat) => !(
        sourceDeleted && Number(stat.playerId) === 1114 && Number(stat.isPitcher) === 1
      ));
      data = { list, total: list.length };
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions` && request.method() === 'GET') {
      data = [{
        decisionId: 901, gameId: GAME_ID, inning: 2, half: 'bottom', sequence: 5,
        runnerName: 'T2P1', responsiblePitcherId: 1100, pitcherName: 'T1PITCHER',
        earnedStatus: 'EARNED', unearnedReason: null, overriddenBy: null,
        effectivePitcherId: decisionVoided ? null : (decisionReassigned ? 1114 : 1100),
        effectivePitcherName: decisionVoided ? null : (decisionReassigned ? 'T1PITCHER2' : 'T1PITCHER'),
        adjustmentId: decisionVoided || decisionReassigned ? 88 : null,
        adjustmentAction: decisionVoided ? 'VOID' : (decisionReassigned ? 'REASSIGN' : null),
        adjustmentReason: decisionVoided ? '误录' : (decisionReassigned ? '应为 16 号' : null),
        voided: decisionVoided
      }];
    } else if (path === `/game/${GAME_ID}/earned-runs/pitchers` && request.method() === 'GET') {
      data = [{
        playerId: 1100, playerName: 'T1PITCHER', teamId: AWAY_ID,
        runsAllowed: decisionVoided ? 2 : 3, earnedRuns: decisionVoided ? 2 : 3, unearnedRuns: 0,
        engineRunsAllowed: 3, engineEarnedRuns: 3, overrideId: null, overrideNote: null
      }, {
        playerId: 1114, playerName: 'T1PITCHER2', teamId: AWAY_ID,
        runsAllowed: decisionVoided ? 0 : (decisionReassigned ? 1 : 0),
        earnedRuns: decisionVoided ? 0 : (decisionReassigned ? 1 : 0),
        unearnedRuns: 0, engineRunsAllowed: 0, engineEarnedRuns: 0,
        overrideId: null, overrideNote: null
      }];
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions/901`
        && request.method() === 'PUT') {
      data = {};
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions/901/reassign`
        && request.method() === 'POST') {
      decisionReassigned = true;
      decisionVoided = false;
      data = { decisionId: 901, effectivePitcherId: 1114, adjustmentId: 88 };
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions/901/void`
        && request.method() === 'POST') {
      decisionVoided = true;
      decisionReassigned = false;
      data = { decisionId: 901, voided: true, adjustmentId: 88 };
    } else if (path === `/game/${GAME_ID}/earned-runs/decision-adjustments/88/revoke`
        && request.method() === 'POST') {
      decisionVoided = false;
      decisionReassigned = false;
      data = { decisionId: 901, effectivePitcherId: 1100 };
    } else if (path === `/game/${GAME_ID}/fielding-adjustments` && request.method() === 'GET') {
      data = [];
    } else if (path === `/game/${GAME_ID}/fielding-adjustments/candidates`
        && request.method() === 'GET') {
      data = [{
        sourceEventId: 'play-err-1', inning: 2, topBottom: 'bottom', teamId: AWAY_ID,
        sequence: 5, inningLabel: '2下 #5', outsBefore: 1, baseLabel: '一垒有人',
        resultText: 'SS 接球失误',
        players: [
          { id: 1100, name: 'T1PITCHER', number: '99', position: 'P', positions: ['P'] },
          { id: 1114, name: 'T1PITCHER2', number: '16', position: 'P', positions: ['P'] }
        ]
      }];
    } else if (path === `/game/${GAME_ID}/fielding-adjustments` && request.method() === 'POST') {
      data = {
        adjustmentId: 71, sourceEventId: 'play-err-1', inning: 2, half: 'bottom',
        sequence: 5, teamId: AWAY_ID, playerId: 1100, playerName: 'T1PITCHER',
        position: 'P', assistCount: 1, status: 'APPROVED', sourceValid: true
      };
    } else if (/^\/game\/10\/stat-rows\/\d+\/delete-check$/.test(path)
        && request.method() === 'GET') {
      const statId = Number(path.split('/').at(-2));
      data = statId === 3011
        ? {
            statId, playerId: 1100, isPitcher: true, deletable: false,
            recommendedAction: 'FIELDING_REVIEW',
            blockers: [{
              type: 'FIELDING_ADJUSTMENT', key: null, label: '守备补记',
              value: 1, recommendedAction: 'FIELDING_REVIEW'
            }]
          }
        : { statId, playerId: null, isPitcher: true, deletable: true, recommendedAction: 'NONE', blockers: [] };
    } else if (path === '/player/team-options') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      data = teamId === AWAY_ID
        ? awayPlayers.map((p) => ({ ...p, id: p.id }))
        : teamId === HOME_ID
          ? homePlayers.map((p) => ({ ...p, id: p.id }))
          : [];
    } else if (path === `/game/${GAME_ID}/identity-transfers/preview`
        && request.method() === 'GET') {
      data = {
        sourcePlayerId: Number(url.searchParams.get('sourcePlayerId')),
        sourcePlayerName: 'T1PITCHER2',
        targetPlayerId: Number(url.searchParams.get('targetPlayerId')),
        targetPlayerName: 'T1PITCHER',
        teamId: AWAY_ID,
        transferable: true,
        blockers: [],
        rows: [{
          isPitcher: 1, sourceStatId: 3010, targetStatId: 3009, targetExists: true
        }],
        fieldingPositions: ['P'],
        erDecisionCount: 1,
        sourceOverride: false,
        targetOverride: false
      };
    } else if (path === `/game/${GAME_ID}/identity-transfers`
        && request.method() === 'POST') {
      data = {
        transferId: 66, sourcePlayerId: 1114, targetPlayerId: 1100, status: 'TRANSFERRED'
      };
    } else if (path === `/game/${GAME_ID}/identity-transfers/66/delete-source`
        && request.method() === 'POST') {
      data = {};
    } else if (path === `/game/${GAME_ID}/adjustments` && request.method() === 'GET') {
      data = [{
        id: 1, scope: 'PLAYER_STAT', action: 'UPDATE_FIELD', sourceType: 'RESULT_SAVE',
        sourceId: 3009, teamId: AWAY_ID, playerId: 1100, playerName: 'T1PITCHER',
        isPitcher: 1, fieldKey: 'np', fieldLabel: 'NP',
        beforeValue: '27', afterValue: '30', status: 'ACTIVE',
        reason: null, operatorId: 9, operatedAt: '2026-11-05T10:00:00'
      }, {
        id: 2, scope: 'ER_DECISION', action: 'REASSIGN', sourceType: 'DECISION_ADJUSTMENT',
        sourceId: 88, teamId: AWAY_ID, playerId: 1114, playerName: 'T1PITCHER2',
        isPitcher: 1, fieldKey: 'responsiblePitcherId', fieldLabel: '责任投手',
        beforeValue: '1100', afterValue: '1114', status: 'ACTIVE',
        reason: '现场更正', operatorId: 9, operatedAt: '2026-11-05T11:00:00'
      }];
    } else if (path === `/game/${GAME_ID}/stat-base/compare` && request.method() === 'GET') {
      data = {
        legacy: false,
        rows: [{
          playerId: 1100, playerName: 'T1PITCHER', teamId: AWAY_ID, isPitcher: 1,
          source: 'RESULT', capturedAt: '2026-11-05T10:00:00',
          fields: [
            { key: 'pitchPa', label: 'PA(投手)', baseValue: 11, effectiveValue: 13 },
            { key: 'np', label: 'NP', baseValue: 27, effectiveValue: 30 }
          ]
        }]
      };
    } else if (path === `/game/${GAME_ID}/revision-audit` && request.method() === 'GET') {
      data = [{
        auditType: 'PITCHER_TRANSFER', recordId: 4, action: 'DELETE_SOURCE',
        status: 'DELETED', summary: '投手行转记：T1PITCHER → T1PITCHER2',
        reason: '转记', operatorId: 12, operatedAt: '2026-11-05T13:00:00'
      }, {
        auditType: 'FIELDING_ADJUSTMENT', recordId: 3, action: 'REVOKE',
        status: 'REVOKED', summary: '守备补记：T2P1 · CF A +1（5下 #167）',
        reason: '补记', operatorId: 11, operatedAt: '2026-11-05T12:00:00'
      }, {
        auditType: 'PITCHER_RUN_OVERRIDE', recordId: 2, action: 'OVERRIDE',
        status: 'APPROVED', summary: '投手 R/ER 覆写：T1PITCHER R 5 / ER 3',
        reason: '投手合计更正', operatorId: 10, operatedAt: '2026-11-05T11:00:00'
      }, {
        auditType: 'ER_DECISION', recordId: 1, action: 'REASSIGN',
        status: 'ACTIVE', summary: 'ER 判定改派：跑者 T2P1 → T1PITCHER2',
        reason: '现场更正', operatorId: 9, operatedAt: '2026-11-05T10:00:00'
      }];
    } else if (path === `/game/${GAME_ID}/pitcher-transfers` && request.method() === 'POST') {
      const body = request.postDataJSON();
      const source = awayStats.find((stat) => Number(stat.id) === Number(body.sourceStatId));
      const target = awayStats.find((stat) => Number(stat.playerId) === Number(body.targetPlayerId)
        && Number(stat.isPitcher) === 1);
      const fields = ['ip', 'pitchPa', 'pitchBf', 'pitAb', 'np', 'fo'];
      if (source && target) {
        for (const key of fields) {
          const sourceValue = Number(source[key]) || 0;
          if ((Number(target[key]) || 0) === 0 && sourceValue !== 0) {
            target[key] = source[key];
          }
          source[key] = 0;
        }
      }
      data = {
        transferId: 55,
        sourceStatId: source?.id ?? body.sourceStatId,
        sourcePlayerId: 1114,
        targetStatId: target?.id ?? null,
        targetPlayerId: body.targetPlayerId,
        status: 'TRANSFERRED'
      };
    } else if (path === `/game/${GAME_ID}/pitcher-transfers/55/delete-source`
        && request.method() === 'POST') {
      sourceDeleted = true;
      data = {};
    } else if (path === `/game/${GAME_ID}/fielding-positions`) {
      data = { list: [], total: 0 };
    } else if (path === '/team/select-options' || path === '/event/1/game-team-options') {
      data = [
        { id: AWAY_ID, name: '客队A', teamId: AWAY_ID },
        { id: HOME_ID, name: '主队B', teamId: HOME_ID }
      ];
    } else if (path === '/game/list') {
      data = { list: [game], total: 1 };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = teamId === AWAY_ID ? awayPlayers : teamId === HOME_ID ? homePlayers : [...awayPlayers, ...homePlayers];
      data = { list, total: list.length };
    } else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === AWAY_ID ? '客队A' : '主队B', shortName: id === AWAY_ID ? '客队A' : '主队B' };
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('final 引擎场次：结果录入页可直接改投手 R/ER 并带人工覆写标记', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);

  const tab = page.getByRole('tab', { name: '投手数据' });
  await expect(tab).toBeVisible();
  await tab.click();

  const awayTable = page.locator('.el-table').filter({ hasText: 'T1PITCHER' }).first();
  const pitcherRow = awayTable.locator('tbody tr', { hasText: 'T1PITCHER' }).first();
  await expect(pitcherRow).toBeVisible();

  const headers = awayTable.locator('.el-table__header th');
  const count = await headers.count();
  let rIndex = -1;
  let erIndex = -1;
  for (let i = 0; i < count; i += 1) {
    const text = (await headers.nth(i).innerText()).trim();
    if (text === 'R') rIndex = i;
    if (text === 'ER') erIndex = i;
  }
  expect(rIndex).toBeGreaterThan(-1);
  expect(erIndex).toBeGreaterThan(-1);

  const rInput = pitcherRow.locator('td').nth(rIndex).locator('input');
  const erInput = pitcherRow.locator('td').nth(erIndex).locator('input');
  await expect(rInput).toBeEditable();
  await expect(erInput).toBeEditable();
  await rInput.fill('5');
  await rInput.blur();
  await erInput.fill('4');
  await erInput.blur();

  const saveRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes(`/game/${GAME_ID}/save-result`)
  ));
  await page.getByRole('button', { name: '保存比赛数据' }).click();
  const body = (await saveRequest).postDataJSON();
  const pitcherPart = body.stats.find((part) => (
    Number(part.playerId) === 1100 && Number(part.isPitcher) === 1
  ));

  expect(pitcherPart).toBeTruthy();
  expect(pitcherPart.manualPitcherRunOverride).toBe(true);
  expect(Number(pitcherPart.pitchR)).toBe(5);
  expect(Number(pitcherPart.er)).toBe(4);
});

test('投手转记：原投手 NP/PA 并入目标并清零，目标已有 IP/R/ER 保持', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);

  const tab = page.getByRole('tab', { name: '投手数据' });
  await expect(tab).toBeVisible();
  await tab.click();

  const awayTable = page.locator('.el-table').filter({ hasText: 'T1PITCHER2' }).first();
  const sourceRow = awayTable.locator('tbody tr', { hasText: 'T1PITCHER2' }).first();
  const targetRow = awayTable.locator('tbody tr', { hasText: 'T1PITCHER' }).first();
  await expect(sourceRow).toBeVisible();

  const headers = awayTable.locator('.el-table__header th');
  const count = await headers.count();
  let npIndex = -1;
  for (let i = 0; i < count; i += 1) {
    if ((await headers.nth(i).innerText()).trim() === 'NP') npIndex = i;
  }
  expect(npIndex).toBeGreaterThan(-1);

  await sourceRow.locator('td').nth(1).click();
  await page.getByRole('button', { name: '转记' }).first().click();
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '转记投手数据' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: '确认转记' }).click();

  const confirm = page.locator('.el-message-box:visible');
  await expect(confirm).toContainText('转记完成');
  await expect(confirm).toContainText('是否删除源行');
  await confirm.getByRole('button', { name: '删除源行' }).click();

  await expect(page.getByRole('tab', { name: '投手数据' })).toBeVisible();
  await page.getByRole('tab', { name: '投手数据' }).click();
  const tableAfter = page.locator('.el-table').filter({ hasText: 'T1PITCHER' }).first();
  await expect(tableAfter.locator('tbody tr', { hasText: 'T1PITCHER2' })).toHaveCount(0);
  const targetAfter = tableAfter.locator('tbody tr', { hasText: 'T1PITCHER' }).first();
  await expect(targetAfter).toBeVisible();

  const headersAfter = tableAfter.locator('.el-table__header th');
  const countAfter = await headersAfter.count();
  let npIndexAfter = -1;
  let paIndexAfter = -1;
  let foIndexAfter = -1;
  for (let i = 0; i < countAfter; i += 1) {
    const text = (await headersAfter.nth(i).innerText()).trim();
    if (text === 'NP') npIndexAfter = i;
    if (text === 'PA') paIndexAfter = i;
    if (text === 'FO') foIndexAfter = i;
  }
  expect(npIndexAfter).toBeGreaterThan(-1);
  expect(paIndexAfter).toBeGreaterThan(-1);
  expect(foIndexAfter).toBeGreaterThan(-1);
  await expect(targetAfter.locator('td').nth(npIndexAfter).locator('input')).toHaveValue('27');
  await expect(targetAfter.locator('td').nth(paIndexAfter).locator('input')).toHaveValue('11');
  await expect(targetAfter.locator('td').nth(foIndexAfter).locator('input')).toHaveValue('3');
});

test('PA 与 BF 双向同步', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  const tab = page.getByRole('tab', { name: '投手数据' });
  await tab.click();

  const table = page.locator('.el-table').filter({ hasText: 'T1PITCHER' }).first();
  const row = table.locator('tbody tr', { hasText: 'T1PITCHER' }).first();
  const headers = table.locator('.el-table__header th');
  const count = await headers.count();
  let paIndex = -1;
  let bfIndex = -1;
  for (let i = 0; i < count; i += 1) {
    const text = (await headers.nth(i).innerText()).trim();
    if (text === 'PA') paIndex = i;
    if (text === 'BF') bfIndex = i;
  }
  expect(paIndex).toBeGreaterThan(-1);
  expect(bfIndex).toBeGreaterThan(-1);

  const paInput = row.locator('td').nth(paIndex).locator('input');
  const bfInput = row.locator('td').nth(bfIndex).locator('input');
  await paInput.fill('13');
  await paInput.blur();
  await expect(bfInput).toHaveValue('13');
  await bfInput.fill('17');
  await bfInput.blur();
  await expect(paInput).toHaveValue('17');
});

test('删除投手行前诊断剩余字段并提供去转记', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '投手数据' }).click();

  const table = page.locator('.el-table').filter({ hasText: 'T1PITCHER2' }).first();
  const sourceRow = table.locator('tbody tr', { hasText: 'T1PITCHER2' }).first();
  const targetRow = table.locator('tbody tr', { hasText: 'T1PITCHER' })
    .filter({ hasNotText: 'T1PITCHER2' }).first();
  await page.getByRole('button', { name: '批量删除' }).first().click();
  await sourceRow.locator('.el-checkbox__inner').first().click();
  // 当前高亮行故意切到另一名投手：诊断必须按待删除行打开转记，而不是当前行。
  await targetRow.locator('td').nth(1).click();
  await page.getByRole('button', { name: /完成批量移除/ }).first().click();

  const box = page.locator('.el-message-box:visible');
  await expect(box).toContainText('该投手行还有数据');
  await expect(box).toContainText('AB=9');
  await box.getByRole('button', { name: '去转记' }).click();
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '转记投手数据' });
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('T1PITCHER2');
});

test('final 结果页整合 ER 复核，覆写后切换页签提示刷新', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);

  const erTab = page.getByRole('tab', { name: 'ER 复核' });
  await expect(erTab).toBeVisible();
  await expect(page.getByRole('tab', { name: '守备补记 A' })).toBeVisible();

  await erTab.click();
  const erTable = page.locator('.game-earned-run-review .el-table').first();
  await expect(erTable).toContainText('T1PITCHER');

  await erTable.getByRole('button', { name: '覆写' }).first().click();
  const overrideRequest = page.waitForRequest((request) => (
    request.method() === 'PUT'
    && request.url().includes('/earned-runs/decisions/901')
  ));
  await page.locator('.el-dialog:visible').filter({ hasText: '覆写判定' })
    .getByRole('button', { name: '确认覆写' }).click();
  await overrideRequest;

  await page.getByRole('tab', { name: '打者数据' }).click();
  const box = page.locator('.el-message-box:visible');
  await expect(box).toContainText('复核已修改服务器统计');
  await box.getByRole('button', { name: '保留当前页' }).click();
  await expect(page.getByRole('tab', { name: 'ER 复核' })).toHaveAttribute('aria-selected', 'true');

  await page.getByRole('button', { name: '保存比赛数据' }).click();
  const saveBox = page.locator('.el-message-box:visible');
  await expect(saveBox).toContainText('保存前需重新加载最新统计');
  await saveBox.getByRole('button', { name: '取消' }).click();
});

test('守备补记 A 使用服务端候选并提交补记', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '守备补记 A' }).click();

  await expect(page.getByText('SS 接球失误')).toBeVisible();
  await page.getByRole('button', { name: '补记助杀' }).click();
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '补记助杀' });
  await dialog.locator('.el-select').first().click();
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: 'T1PITCHER' }).first().click();

  const createRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes(`/game/${GAME_ID}/fielding-adjustments`)
    && !request.url().includes('/candidates')
  ));
  await dialog.getByRole('button', { name: '确认补记' }).click();
  const body = (await createRequest).postDataJSON();
  expect(body.sourceEventId).toBe('play-err-1');
  expect(body.playerId).toBe(1100);
  expect(body.position).toBe('P');
});

test('比赛列表 final 场次不再提供赛后复核入口', async ({ page }) => {
  await setup(page);
  await page.goto('/bs-ball/bs-ball/admin/events/1/games');

  await expect(page.getByRole('button', { name: '数据补录/复核' }).first()).toBeVisible();
  await expect(page.getByRole('button', { name: '赛后复核' })).toHaveCount(0);
});

test('ER 判定可改派、作废并恢复', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: 'ER 复核' }).click();

  const table = page.locator('.game-earned-run-review .el-table').first();
  const row = table.locator('.el-table__row').filter({ hasText: 'T2P1' }).first();
  await expect(row).toContainText('T1PITCHER');

  await row.getByRole('button', { name: '改派' }).click();
  const reassignDialog = page.locator('.el-dialog:visible').filter({ hasText: '改派判定' });
  await reassignDialog.locator('.el-select').first().click();
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: 'T1PITCHER2' }).first().click();
  const reassignRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes('/earned-runs/decisions/901/reassign')
  ));
  await reassignDialog.getByRole('button', { name: '确认改派' }).click();
  const reassignBody = (await reassignRequest).postDataJSON();
  expect(reassignBody.targetPitcherId).toBe(1114);
  await expect(row).toContainText('T1PITCHER2');
  await expect(row).toContainText('人工改派');

  await row.getByRole('button', { name: '作废' }).click();
  const voidDialog = page.locator('.el-dialog:visible').filter({ hasText: '作废判定' });
  const voidRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes('/earned-runs/decisions/901/void')
  ));
  await voidDialog.getByRole('button', { name: '确认作废' }).click();
  await voidRequest;
  await expect(row).toContainText('已作废');

  await row.getByRole('button', { name: '恢复' }).click();
  const box = page.locator('.el-message-box:visible');
  await expect(box).toContainText('恢复');
  const revokeRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes('/earned-runs/decision-adjustments/88/revoke')
  ));
  await box.getByRole('button', { name: '确认恢复' }).click();
  await revokeRequest;
  await expect(row).not.toContainText('人工改派');
  await expect(row).not.toContainText('已作废');
});

test('修订记录页签展示四类人工修订', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '修订记录' }).click();

  await expect(page.getByText('实时记录为基座 · 人工修订为最终值')).toBeVisible();
  const rows = page.locator('.revision-audit .el-table__row');
  await expect(rows).toHaveCount(4);
  await expect(rows.nth(0)).toContainText('投手行转记');
  await expect(rows.nth(1)).toContainText('守备补记');
  await expect(rows.nth(2)).toContainText('投手 R/ER 覆写');
  await expect(rows.nth(3)).toContainText('ER 判定改派');
});

test('删除前后端诊断：守备补记阻断时提示去守备补记 A', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '投手数据' }).click();

  const table = page.locator('.el-table').filter({ hasText: 'T1PITCHER2' }).first();
  const targetRow = table.locator('tbody tr', { hasText: 'T1PITCHER3' }).first();
  await page.getByRole('button', { name: '批量删除' }).first().click();
  await targetRow.locator('.el-checkbox__inner').first().click();
  const checkRequest = page.waitForRequest((request) => (
    request.url().includes('/stat-rows/3011/delete-check')
  ));
  await page.getByRole('button', { name: /完成批量移除/ }).first().click();
  await checkRequest;

  await expect(page.locator('.el-message').filter({ hasText: '守备补记或守备位置数据' })).toBeVisible();
  await expect(page.locator('.el-dialog:visible').filter({ hasText: '转记投手数据' })).toHaveCount(0);
});

test('基座对比页签展示基座与当前有效值差异', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '基座对比' }).click();

  await expect(page.getByText('原始基座 · 当前有效值')).toBeVisible();
  const row = page.locator('.stat-base-diff .el-table__row').filter({ hasText: 'T1PITCHER' }).first();
  await expect(row).toContainText('PA(投手)：11 → 13');
  await expect(row).toContainText('NP：27 → 30');
});

test('修订台账页签展示字段级修改', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '修订台账' }).click();

  await expect(page.getByText('人工修订台账 · 字段级留痕')).toBeVisible();
  const row = page.locator('.adjustment-ledger .el-table__row').filter({ hasText: 'T1PITCHER' }).first();
  await expect(row).toContainText('结果字段');
  await expect(row).toContainText('NP');
  await expect(row).toContainText('27 → 30');
  await expect(row).toContainText('#9');
  const erRow = page.locator('.adjustment-ledger .el-table__row').filter({ hasText: 'T1PITCHER2' }).first();
  await expect(erRow).toContainText('ER 判定');
  await expect(erRow).toContainText('责任投手');
});

test('身份转记：预览同队球员并单独删除源数据', async ({ page }) => {
  await setup(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  await page.getByRole('tab', { name: '身份转记' }).click();

  const form = page.locator('.identity-transfer__form');
  await form.locator('.el-select').nth(0).click();
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: 'T1PITCHER2' }).first().click();
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0);
  await form.locator('.el-select').nth(1).click();
  await page.locator('.el-select-dropdown:visible').last()
    .locator('.el-select-dropdown__item')
    .filter({ hasText: /T1PITCHER(?!\d)/ }).first().click();
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0);
  await page.getByRole('button', { name: '预览转记' }).click();

  await expect(page.locator('.identity-transfer__summary')).toContainText('T1PITCHER2');
  await expect(page.locator('.identity-transfer__summary')).toContainText('T1PITCHER');
  await expect(page.getByRole('button', { name: '确认转记' })).toBeVisible();

  const transferRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().endsWith(`/game/${GAME_ID}/identity-transfers`)
  ));
  await page.getByRole('button', { name: '确认转记' }).click();
  await transferRequest;

  const box = page.locator('.el-message-box:visible');
  await expect(box).toContainText('是否删除源数据');
  const deleteRequest = page.waitForRequest((request) => (
    request.method() === 'POST'
    && request.url().includes('/identity-transfers/66/delete-source')
  ));
  await box.getByRole('button', { name: '删除源数据' }).click();
  await deleteRequest;
});
