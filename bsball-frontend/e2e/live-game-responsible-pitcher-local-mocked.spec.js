import { test, expect } from '@playwright/test';

// 跑者责任投手已被换下后得分：不得崩溃，且安打/得分正常落库（#436 runnerPitcher 回归）
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

function player(id, name, number, position, teamId) {
  return {
    id, name, number, teamId, position, positions: [position],
    stats: { batting: zeroBatting(), pitching: zeroPitching(), fielding: zeroFielding() }
  };
}

function buildSnapshot() {
  const awayLineup = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'].map((pos, i) =>
    player(201 + i, `客${pos}`, String(i + 1), pos, 2));
  const homeLineup = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'].map((pos, i) =>
    player(101 + i, `主${pos}`, String(i + 1), pos, 1));
  homeLineup[5] = player(166, '主六棒', '66', 'SS', 1);
  // 二三垒跑者由已被换下的投手（635）送上垒；当前投手为 201
  homeLineup[6].responsiblePitcherId = 635;
  homeLineup[7].responsiblePitcherId = 635;
  const homeIds = homeLineup.map((p) => p.id);
  const awayIds = awayLineup.map((p) => p.id);
  return {
    v: 1, savedAt: 1, gameId: 10, gameMode: 'BASEBALL', gameStarted: true,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    homeLineup, awayLineup,
    homeBench: [],
    // 已被换下的责任投手仍在客队替补席（含本场累计统计）
    awayBench: [player(635, '客前投手', '99', 'P', 2)],
    homeFieldingPitcherId: 101, awayFieldingPitcherId: 201,
    homeFieldingPitcherRow: null, awayFieldingPitcherRow: null,
    awayLastDhAddedFromPoolId: null, homeLastDhAddedFromPoolId: null,
    awayUnavailablePlayerIds: [], homeUnavailablePlayerIds: [],
    awayLineupStarterIds: [...awayIds, null, null, null],
    homeLineupStarterIds: [...homeIds, null, null, null],
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    teamsData: { away: { score: 0, innings: [0, 0, 0, 0] }, home: { score: 0, innings: [0, 0, 0, 0] } },
    eventLog: [],
    atBatSummaries: [],
    playEvents: [],
    playerStatsById: {},
    gameState: {
      inning: 4, isTop: false, outs: 0, halfInningOuts: [], balls: 0, strikes: 0,
      batterIndex: { away: 0, home: 5 },
      runners: { 1: null, 2: 107, 3: 108 },
      flow: null, isGameEnd: false, hasPitchThisAB: false, needNextBatter: false,
    },
  };
}

async function setup(page) {
  const snapshot = buildSnapshot();
  const savedLive = [];
  const snapshots = [];
  const errors = [];
  page.on('pageerror', (err) => errors.push(err.message));

  await page.addInitScript(() => {
    Object.keys(localStorage).filter((k) => k.startsWith('bs-ball-live-')).forEach((k) => localStorage.removeItem(k));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({ id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e' }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Responsible Pitcher E2E' };
    else if (path === '/team/select-options') data = [{ id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 4, topBottom: 'bottom', homeScore: 0, awayScore: 0,
    };
    else if (path === '/game/10/stats') data = { list: [], total: 0 };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: JSON.stringify(snapshot) };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      snapshots.push(request.postDataJSON().snapshotJson);
      data = { snapshotJson: '' };
    } else if (path === '/game/10/save-live') {
      savedLive.push(request.postDataJSON());
      data = { success: true };
    } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = [...snapshot.homeLineup, ...snapshot.awayLineup].filter((p) => p.teamId === teamId);
      data = { list, total: list.length };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (/^\/team\/[12]$/.test(path)) { const id = Number(path.split('/').at(-1)); data = { id, name: id === 1 ? 'Home' : 'Away' }; }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data }) });
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
  return { savedLive, snapshots, errors };
}

async function recordDouble(page) {
  await page.getByRole('button', { name: '界内球' }).click();
  const menu = page.locator('.lgpfm__panel');
  await expect(menu).toBeVisible();
  // 浮层打开后 240ms 内会拦截点击
  await page.waitForTimeout(350);
  await menu.locator('.lgpfm__cn').filter({ hasText: /^平飞球$/ }).click();
  await menu.locator('.lgpfm__cn').filter({ hasText: /^二垒安打$/ }).click();
  await page.locator('g.defender-hit[data-pos="CF"]').click();

  const safe = page.locator('.hsc-inline__btn--safe');
  await expect(safe).toBeVisible({ timeout: 8000 });
  await page.waitForTimeout(300);
  await safe.click();
  await page.waitForTimeout(300);
  await safe.click();
}

test('二三垒跑者责任投手已被换下：二垒安打得分正常记账且不崩溃', async ({ page }) => {
  const { savedLive, snapshots, errors } = await setup(page);
  await recordDouble(page);

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
  await expect.poll(() => snapshots.length).toBeGreaterThan(0);

  expect(errors).toEqual([]);

  const body = savedLive.at(-1);
  const stat = (playerId) => (body.stats || []).find((row) => row.playerId === playerId);
  expect(stat(166)).toMatchObject({ h: 1, doubles: 1, rbi: 2 });
  expect(stat(107)).toMatchObject({ r: 1 });
  expect(stat(108)).toMatchObject({ r: 1 });

  const snapshot = JSON.parse(snapshots.at(-1));
  expect(snapshot.gameState.runners).toMatchObject({ 2: 166, 3: null });
  expect(snapshot.gameState.batterIndex.home).toBe(6);
  // 两分都记在责任投手（已被换下、在替补席）头上，当前投手不背
  expect(snapshot.playerStatsById['635'].pitching).toMatchObject({ pitchR: 2 });
  expect(snapshot.playerStatsById['201'].pitching?.pitchR ?? 0).toBe(0);
  const logText = snapshot.eventLog
    .map((entry) => (typeof entry === 'string' ? entry : JSON.stringify(entry)))
    .join('\n');
  expect(logText).toContain('二垒安打');
  expect(logText).toContain('回本垒得分');
});
