import { test, expect } from '@playwright/test';

// 滚地球 1-3 封杀：DH 仅守备投手必须拿到助杀 A（对应测试端 ID 243 缺陷）
// 运行：E2E_PORT=3019 npx playwright test e2e/live-game-pitcher-assist-local-mocked.spec.js
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

function player(id, name, position, teamId, stats = {}) {
  return {
    id,
    name,
    number: String(id % 100),
    teamId,
    position,
    positions: [position],
    stats: {
      batting: { ...zeroBatting(), ...(stats.batting || {}) },
      pitching: { ...zeroPitching(), ...(stats.pitching || {}) },
      fielding: { ...zeroFielding(), ...(stats.fielding || {}) },
    },
  };
}

// 客队为 DH 队：打线 9 人无守位 P，先发投手 220 仅守备（awayFieldingPitcherId/Row）
// isTop=true 时主队守备（主队打线含守位 P，用于对照非 DH 路径）
function buildSnapshot({ isTop = false } = {}) {
  const awayLineup = [
    player(201, '客捕手', 'C', 2),
    player(202, '客一垒', '1B', 2),
    player(203, '客二垒', '2B', 2),
    player(204, '客三垒', '3B', 2),
    player(205, '客游击', 'SS', 2),
    player(206, '客左外', 'LF', 2),
    player(207, '客中外', 'CF', 2),
    player(208, '客右外', 'RF', 2),
    player(209, '客DH', 'DH', 2),
  ];
  const awayPitcher = player(220, '客投手', 'P', 2);
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
  const awayIds = awayLineup.map((p) => p.id);
  const homeIds = homeLineup.map((p) => p.id);
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
    awayBench: [player(230, '客替补', '', 2)],
    homeFieldingPitcherId: 101,
    awayFieldingPitcherId: 220,
    homeFieldingPitcherRow: null,
    awayFieldingPitcherRow: awayPitcher,
    awayLastDhAddedFromPoolId: null,
    homeLastDhAddedFromPoolId: null,
    awayUnavailablePlayerIds: [],
    homeUnavailablePlayerIds: [],
    awayLineupStarterIds: [...awayIds, null, null, null],
    homeLineupStarterIds: [...homeIds, null, null, null],
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    teamsData: {
      away: { score: 0, innings: [0] },
      home: { score: 0, innings: [0] },
    },
    eventLog: [],
    atBatSummaries: [],
    playEvents: [],
    playerStatsById: {},
    gameState: {
      inning: 1,
      isTop,
      outs: 0,
      halfInningOuts: [],
      balls: 0,
      strikes: 0,
      batterIndex: { away: 0, home: 0 },
      runners: { 1: null, 2: null, 3: null },
      flow: null,
      isGameEnd: false,
      hasPitchThisAB: false,
      needNextBatter: false,
    },
  };
}

async function setup(page, options = {}) {
  const savedLive = [];
  const snapshots = [];
  const snapshot = buildSnapshot(options);
  const players = [
    ...snapshot.homeLineup, ...snapshot.awayLineup,
    snapshot.homeBench[0], snapshot.awayBench[0], snapshot.awayFieldingPitcherRow,
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
    if (path === '/event/1') data = { id: 1, name: 'Pitcher Assist E2E' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1,
      topBottom: options.isTop ? 'top' : 'bottom',
      homeScore: 0, awayScore: 0,
    };
    else if (path === '/game/10/stats') data = { list: [], total: 0 };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: JSON.stringify(snapshot) };
    } else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      const { snapshotJson } = request.postDataJSON();
      snapshots.push(snapshotJson);
      data = { snapshotJson };
    } else if (path === '/game/10/save-live') {
      savedLive.push(request.postDataJSON());
      data = { success: true };
    } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) {
      const teamId = Number(path.split('/')[4]);
      data = { restricted: false, players: players.filter((p) => p.teamId === teamId) };
    } else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = players.filter((p) => p.teamId === teamId);
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
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
  return { savedLive, snapshots };
}

async function recordGroundOut13(page) {
  await page.getByRole('button', { name: '界内球' }).click();
  const menu = page.locator('.lgpfm__panel');
  await expect(menu).toBeVisible();
  // 浮层打开后短暂拦截点击
  await page.waitForTimeout(350);
  await menu.locator('.lgpfm__cn').filter({ hasText: /^滚地球$/ }).click();
  await menu.locator('.lgpfm__cn').filter({ hasText: /^出局$/ }).click();
  await menu.locator('.lgpfm__cn').filter({ hasText: /^一垒前出局$/ }).click();

  // BIP 落点：投手接球 → 传一垒封杀
  const flowBar = page.locator('.les-flow-card');
  await expect(flowBar).toContainText('点击守备位置确认落点');
  await page.locator('g.defender-hit[data-pos="P"]').click();
  await page.locator('g.defender-hit[data-pos="1B"]').click();
  await page.getByRole('button', { name: '完成记录' }).click();
  await expect(flowBar).toHaveCount(0);
}

test('滚地球 1-3：DH 仅守备投手记 A，一垒手记 PO', async ({ page }) => {
  const { savedLive, snapshots } = await setup(page);
  await recordGroundOut13(page);

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);

  const body = savedLive.at(-1);
  const awayPitcher = (body.stats || []).find(
    (row) => row.teamId === 2 && row.playerId === 220 && row.isPitcher === 1,
  );
  const awayFirstBase = (body.stats || []).find(
    (row) => row.teamId === 2 && row.playerId === 202 && !row.isPitcher,
  );
  expect(awayFirstBase).toMatchObject({ po: 1, a: 0 });
  expect(awayPitcher).toMatchObject({ a: 1, defInn: 0.1 });

  await expect.poll(() => snapshots.length).toBeGreaterThan(0);
  const snapshot = JSON.parse(snapshots.at(-1));
  expect(snapshot.playerStatsById['220'].fielding).toMatchObject({ a: 1 });
  expect(snapshot.playerStatsById['202'].fielding).toMatchObject({ po: 1 });
});

test('滚地球 1-3：打线内投手（非 DH）记 A，一垒手记 PO', async ({ page }) => {
  const { savedLive } = await setup(page, { isTop: true });
  await recordGroundOut13(page);

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);

  const body = savedLive.at(-1);
  const homePitcher = (body.stats || []).find(
    (row) => row.teamId === 1 && row.playerId === 101 && row.isPitcher === 1,
  );
  const homeFirstBase = (body.stats || []).find(
    (row) => row.teamId === 1 && row.playerId === 103 && !row.isPitcher,
  );
  expect(homeFirstBase).toMatchObject({ po: 1, a: 0 });
  expect(homePitcher).toMatchObject({ a: 1, defInn: 0.1 });
});
