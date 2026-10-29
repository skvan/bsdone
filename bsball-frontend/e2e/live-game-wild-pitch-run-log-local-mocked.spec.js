import { test, expect } from '@playwright/test';

// 三垒跑者拖回本垒选「暴投」：必须同时留下暴投投球行与回本垒得分行（#536 回归）
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
  const away = [
    player(1, '客一', '1', 'LF', 2), player(2, '客二', '2', 'CF', 2),
    player(4, '刘大路', '22', 'C', 2), player(5, '客四', '4', '1B', 2),
    player(6, '客五', '5', '3B', 2), player(7, '客六', '6', 'SS', 2),
    player(8, '客七', '7', 'RF', 2), player(9, '客八', '8', '2B', 2),
    player(10, '客九', '9', 'DH', 2),
  ];
  const home = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'].map((pos, i) =>
    player(101 + i, `主${pos}`, String(i + 1), pos, 1));
  const awayIds = away.map((p) => p.id);
  const homeIds = home.map((p) => p.id);
  return {
    v: 1, savedAt: 1, gameId: 10, gameMode: 'BASEBALL', gameStarted: true,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    homeLineup: home, awayLineup: away,
    homeBench: [], awayBench: [],
    homeFieldingPitcherId: 101, awayFieldingPitcherId: null,
    homeFieldingPitcherRow: null, awayFieldingPitcherRow: null,
    awayLastDhAddedFromPoolId: null, homeLastDhAddedFromPoolId: null,
    awayUnavailablePlayerIds: [], homeUnavailablePlayerIds: [],
    awayLineupStarterIds: [...awayIds, null, null, null],
    homeLineupStarterIds: [...homeIds, null, null, null],
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    teamsData: { away: { score: 0, innings: [0, 0, 0, 0] }, home: { score: 0, innings: [0, 0, 0, 0] } },
    eventLog: [], atBatSummaries: [], playEvents: [], playerStatsById: {},
    gameState: {
      inning: 4, isTop: true, outs: 0, halfInningOuts: [], balls: 1, strikes: 0,
      batterIndex: { away: 4, home: 0 },
      runners: { 1: 6, 2: 5, 3: 4 },
      flow: null, isGameEnd: false, hasPitchThisAB: true, needNextBatter: false,
    },
  };
}

async function setup(page) {
  const snapshot = buildSnapshot();
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
    if (path === '/event/1') data = { id: 1, name: 'Wild Pitch Run Log E2E' };
    else if (path === '/team/select-options') data = [{ id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }];
    else if (path === '/game/10') data = { id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live', gameMode: 'BASEBALL', inning: 4, topBottom: 'top', homeScore: 0, awayScore: 0 };
    else if (path === '/game/10/stats') data = { list: [], total: 0 };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: JSON.stringify(snapshot) };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') { snapshots.push(request.postDataJSON().snapshotJson); data = { snapshotJson: '' }; }
    else if (path === '/game/10/save-live') data = { success: true };
    else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
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
  return { snapshots, errors };
}

async function dragRunnerHome(page) {
  // 等服务器现场恢复的 loading 遮罩退场，否则会吃掉第一次 pointerdown
  await page.waitForTimeout(1200);
  const runner = page.locator('.runners-group > .runner-draggable-group')
    .filter({ hasText: '#22' }).first().locator('circle.runner-token-circle');
  const box = await runner.boundingBox();
  expect(box).toBeTruthy();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(320);
  await page.mouse.move(box.x + box.width / 2 + 80, box.y + box.height / 2 + 40, { steps: 8 });
  await page.waitForTimeout(300);

  const zones = page.locator('rect.runner-drop-zone-rect');
  const boxes = [];
  for (let i = 0; i < await zones.count(); i += 1) boxes.push(await zones.nth(i).boundingBox());
  const home = boxes.filter(Boolean).sort((a, b) => b.y - a.y)[0];
  expect(home).toBeTruthy();
  await page.mouse.move(home.x + home.width / 2, home.y + home.height * 0.25, { steps: 12 });
  await page.waitForTimeout(120);
  await page.mouse.up();

  const item = page.locator('.rdm__item').filter({ hasText: /暴投/ }).first();
  await expect(item).toBeVisible();
  await item.click();
}

test('三垒跑者拖回本垒选暴投：暴投行与回本垒得分行都要有', async ({ page }) => {
  const { snapshots, errors } = await setup(page);
  await dragRunnerHome(page);

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => snapshots.length).toBeGreaterThan(0);
  const snap = JSON.parse(snapshots.at(-1));

  const logText = snap.eventLog.map((entry) => (typeof entry === 'string' ? entry : JSON.stringify(entry))).join('\n');
  expect(errors).toEqual([]);
  expect(snap.gameState.runners).toMatchObject({ 1: 6, 2: 5, 3: null });
  expect(snap.playerStatsById['4'].batting.r).toBe(1);
  expect(logText).toContain('暴投 (WP)：三垒跑者 刘大路 (#22) 回本垒得分');
  expect(logText).toContain('主P: 暴投 (WP)');
  expect((logText.match(/主P: 暴投 \(WP\)/g) ?? []).length).toBe(1);
});
