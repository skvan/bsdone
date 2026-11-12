import { test, expect } from '@playwright/test';

// 赛后守备补记：final 状态 + 失误 Play → 补记 A → 撤销 A。
// 运行：E2E_PORT=3021 npx playwright test e2e/live-game-fielding-adjustment-local-mocked.spec.js
const zeroBatting = () => ({
  ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, so_swing: 0, so_looking: 0,
  sf: 0, sh: 0, sb: 0, cs: 0, doubles: 0, triples: 0, hr: 0, insideParkHr: 0, gdp: 0, ibb: 0, io: 0,
});
const zeroPitching = () => ({
  ip: 0, np: 0, pitchH: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, pitchInsideParkHr: 0,
  wp: 0, bk: 0, pitchPa: 0, pitchBf: 0, pitchBb: 0, pitchIbb: 0, pitchHbp: 0, pitchIo: 0,
  go: 0, fo: 0, er: 0,
});
const zeroFielding = () => ({ po: 0, a: 0, e: 0, pb: 0 });

function player(id, name, position, teamId) {
  return {
    id,
    name,
    number: String(id),
    teamId,
    position,
    positions: [position],
    stats: {
      batting: zeroBatting(),
      pitching: zeroPitching(),
      fielding: zeroFielding(),
    },
  };
}

function buildSnapshot({ final = true } = {}) {
  const homeLineup = [
    player(101, '主投手', 'P', 1),
    player(102, '主捕手', 'C', 1),
    player(103, '主一垒', '1B', 1),
    player(104, '主二垒', '2B', 1),
    player(105, '主三垒', '3B', 1),
    player(106, '主游击', 'SS', 1),
    player(107, '主左外', 'LF', 1),
    player(108, '主中外', 'CF', 1),
    player(109, '主右外', 'RF', 1),
  ];
  const awayLineup = [
    player(23, '客中外', 'CF', 2),
    player(24, '客捕手', 'C', 2),
    player(25, '客一垒', '1B', 2),
    player(26, '客二垒', '2B', 2),
    player(27, '客三垒', '3B', 2),
    player(28, '客游击', 'SS', 2),
    player(29, '客左外', 'LF', 2),
    player(30, '客右外', 'RF', 2),
    player(31, '客DH', 'DH', 2),
  ];
  const homeIds = homeLineup.map((item) => item.id);
  const awayIds = awayLineup.map((item) => item.id);
  return {
    v: 1,
    savedAt: 1,
    gameId: 10,
    gameMode: 'BASEBALL',
    gameStarted: true,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    homeLineup,
    awayLineup,
    homeBench: [player(110, '主替补', '', 1)],
    awayBench: [player(32, '客替补', '', 2)],
    homeFieldingPitcherId: 101,
    awayFieldingPitcherId: 101,
    homeFieldingPitcherRow: null,
    awayFieldingPitcherRow: null,
    awayLastDhAddedFromPoolId: null,
    homeLastDhAddedFromPoolId: null,
    awayUnavailablePlayerIds: [],
    homeUnavailablePlayerIds: [],
    awayLineupStarterIds: [...awayIds, null, null, null],
    homeLineupStarterIds: [...homeIds, null, null, null],
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    teamsData: {
      away: { score: 1, innings: [0, 0, 0, 0, 1] },
      home: { score: 0, innings: [0, 0, 0, 0, 0] },
    },
    eventLog: ['5下 失误：CF 客中外 (#23) 记 E'],
    atBatSummaries: [],
    playEvents: [
      {
        id: 'play-error-1',
        inning: 5,
        half: 'bottom',
        sequence: 167,
        outsBefore: 1,
        outsAfter: 1,
        batterPlayerId: 31,
        batterName: '客DH',
        pitcherPlayerId: 101,
        resultCode: 'E',
        resultText: '5下 失误：CF 客中外 (#23) 记 E；二垒跑者 #31 回本垒得分',
        basesBefore: { 1: null, 2: 31, 3: null },
        basesAfter: { 1: 31, 2: null, 3: null },
        scoringRunners: [],
        scoreDelta: { away: 1, home: 0 },
      },
    ],
    playerStatsById: {},
    gameState: {
      inning: 5,
      isTop: false,
      outs: 1,
      halfInningOuts: ['失误'],
      balls: 0,
      strikes: 0,
      batterIndex: { away: 0, home: 0 },
      runners: { 1: null, 2: 31, 3: null },
      flow: null,
      isGameEnd: final,
      hasPitchThisAB: false,
      needNextBatter: false,
    },
  };
}

async function setup(page, options = {}) {
  const adjustments = [];
  const requests = { create: [], revoke: [], getStatus: 0 };
  const snapshot = buildSnapshot(options);
  const players = [
    ...snapshot.homeLineup, ...snapshot.awayLineup,
    snapshot.homeBench[0], snapshot.awayBench[0],
  ];

  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e',
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Fielding Adjustment E2E' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/game/10' && request.method() === 'GET') {
      requests.getStatus += 1;
      // 首次页面加载返回 live，抽屉打开时模拟“比赛已结束”。
      const status = options.finalStatusOnOpen && requests.getStatus > 1 ? 'final' : 'live';
      data = {
        id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status,
        gameMode: 'BASEBALL', inning: 5, topBottom: 'bottom',
        homeScore: 0, awayScore: 1,
      };
    } else if (path === '/game/10/stats') data = { list: [], total: 0 };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: JSON.stringify(snapshot) };
    } else if (path === '/game/10/fielding-adjustments' && request.method() === 'GET') {
      data = adjustments;
    } else if (path === '/game/10/fielding-adjustments' && request.method() === 'POST') {
      const body = request.postDataJSON();
      requests.create.push(body);
      const row = {
        adjustmentId: 77,
        sourceEventId: body.sourceEventId,
        inning: 5,
        half: 'bottom',
        sequence: 167,
        teamId: 2,
        playerId: body.playerId,
        playerName: '客中外',
        position: body.position,
        assistCount: 1,
        status: 'APPROVED',
        sourceText: '5下 失误：CF 客中外 (#23) 记 E',
        note: body.note ?? null,
        sourceValid: true,
      };
      adjustments.splice(0, adjustments.length, row);
      data = row;
    } else if (path === '/game/10/fielding-adjustments/77/revoke') {
      requests.revoke.push(request.postDataJSON());
      const row = adjustments[0];
      if (row) row.status = 'REVOKED';
      data = row;
    } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) {
      const teamId = Number(path.split('/')[4]);
      data = { restricted: false, players: players.filter((item) => item.teamId === teamId) };
    } else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = players.filter((item) => item.teamId === teamId);
      data = { list, total: list.length };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === 1 ? 'Home' : 'Away' };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data }),
    });
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.locator('button:visible').filter({ hasText: 'ER 复核' }).first()).toBeVisible();
  return { adjustments, requests };
}

async function openFieldingTab(page) {
  await page.getByRole('button', { name: 'ER 复核' }).click();
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '比赛复核' });
  await expect(drawer).toBeVisible();
  await drawer.getByRole('tab', { name: '守备补记' }).click();
  return drawer;
}

test('赛后补记失误 Play 的助杀 A，并可撤销扣回', async ({ page }) => {
  const { requests } = await setup(page, { finalStatusOnOpen: true });
  const drawer = await openFieldingTab(page);

  await expect(drawer.getByText('待复核的失误 Play')).toBeVisible();
  await expect(drawer.getByText('5下 #167')).toBeVisible();
  await expect(drawer.getByText('二垒有人')).toBeVisible();
  await expect(drawer.getByText('比赛已结束 · 赛后复核')).toBeVisible();

  await drawer.getByRole('button', { name: '补记助杀' }).click();
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '补记助杀' });
  await expect(dialog).toBeVisible();
  await dialog.locator('.el-select').first().click();
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '#23 客中外' }).click();
  await expect(dialog.locator('.fielding-adjustment__preview')).toContainText('位置 CF A +1、TC +1');
  await dialog.getByRole('button', { name: '确认补记' }).click();

  await expect.poll(() => requests.create.length).toBe(1);
  expect(requests.create[0]).toMatchObject({
    sourceEventId: 'play-error-1',
    playerId: 23,
    position: 'CF',
  });
  await expect(drawer.getByText('已生效').first()).toBeVisible();
  await expect(drawer.getByText('A +1').first()).toBeVisible();

  await drawer.getByRole('button', { name: '撤销' }).click();
  await page.getByRole('button', { name: '确认撤销' }).click();
  await expect.poll(() => requests.revoke.length).toBe(1);
  await expect(drawer.getByText('已撤销').first()).toBeVisible();
});

test('进行中的比赛守备补记页签只显示赛后门禁', async ({ page }) => {
  await setup(page, { final: false });
  const drawer = await openFieldingTab(page);

  await expect(drawer.getByText('比赛结束后才能补记守备助杀')).toBeVisible();
  await expect(drawer.getByRole('button', { name: '补记助杀' })).toHaveCount(0);
  await expect(drawer.getByText('补记记录')).toHaveCount(0);
});
