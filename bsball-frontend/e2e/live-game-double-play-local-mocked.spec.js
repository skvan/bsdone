import { test, expect } from '@playwright/test';

// #530：双杀按球路建链，出局由「拖拽持球守备员到垒包」逐个记录。
// 6-5-4：SS 接球 → 点 3B（6→5）→ 拖 3B 到三垒记第 1 个出局 → 点 2B（5→4）→ 拖 2B 到二垒记第 2 个出局 →
// 打者安全上一垒；完成后 6 助杀、5 刺杀+助杀、4 刺杀，三人各 DP+1，打者 GDP 不增加。
const zeroBatting = () => ({
  ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, so_swing: 0, so_looking: 0,
  sf: 0, sh: 0, sb: 0, cs: 0, doubles: 0, triples: 0, hr: 0, insideParkHr: 0, gdp: 0, ibb: 0, io: 0,
});
const zeroPitching = () => ({
  ip: 0, np: 0, pitchH: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, pitchInsideParkHr: 0,
  wp: 0, bk: 0, pitchPa: 0, pitchBf: 0, pitchBb: 0, pitchIbb: 0, pitchHbp: 0, pitchIo: 0,
  go: 0, fo: 0, er: 0,
});
const zeroFielding = () => ({ po: 0, a: 0, e: 0, pb: 0, dp: 0, inn: 0 });
function player(id, name, position, teamId) {
  return {
    id, name, number: String(id % 100), teamId, position, positions: [position],
    stats: { batting: zeroBatting(), pitching: zeroPitching(), fielding: zeroFielding() },
  };
}
function buildSnapshot(options = {}) {
  const awayLineup = [
    player(32, '打者', 'C', 2),
    player(45, '一垒跑者', '1B', 2),
    player(26, '二垒跑者', '2B', 2),
    player(204, '客四', '3B', 2),
    player(205, '客五', 'SS', 2),
    player(206, '客六', 'LF', 2),
    player(207, '客七', 'CF', 2),
    player(208, '客八', 'RF', 2),
    player(209, '客九', 'DH', 2),
  ];
  const awayPitcher = player(220, '客投手', 'P', 2);
  const homeLineup = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'].map((pos, i) =>
    player(101 + i, `主${pos}`, pos, 1));
  const awayIds = awayLineup.map((p) => p.id);
  const homeIds = homeLineup.map((p) => p.id);
  return {
    v: 1, savedAt: 1, gameId: 10, gameMode: 'BASEBALL', gameStarted: true,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    homeLineup, awayLineup,
    homeBench: [], awayBench: [],
    homeFieldingPitcherId: 101, awayFieldingPitcherId: 220,
    homeFieldingPitcherRow: null, awayFieldingPitcherRow: awayPitcher,
    awayLastDhAddedFromPoolId: null, homeLastDhAddedFromPoolId: null,
    awayUnavailablePlayerIds: [], homeUnavailablePlayerIds: [],
    awayLineupStarterIds: [...awayIds, null, null, null],
    homeLineupStarterIds: [...homeIds, null, null, null],
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    teamsData: { away: { score: 0, innings: [0, 0, 0, 0] }, home: { score: 0, innings: [0, 0, 0, 0] } },
    eventLog: [], atBatSummaries: [], playEvents: [], playerStatsById: {},
    gameState: {
      inning: 4, isTop: true, outs: 0, halfInningOuts: [], balls: 0, strikes: 0,
      batterIndex: { away: 0, home: 0 },
      runners: options.runners === 'secondOnly'
        ? { 1: null, 2: awayLineup[2].id, 3: null }
        : { 1: awayLineup[1].id, 2: awayLineup[2].id, 3: null },
      flow: null, isGameEnd: false, hasPitchThisAB: false, needNextBatter: false,
    },
  };
}

async function setup(page, options = {}) {
  const snapshot = buildSnapshot(options);
  const snapshots = [];
  const savedLive = [];
  const players = [...snapshot.homeLineup, ...snapshot.awayLineup, snapshot.awayFieldingPitcherRow];
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
    if (path === '/event/1') data = { id: 1, name: 'Double Play E2E' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 4, topBottom: 'top', homeScore: 0, awayScore: 0,
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
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data }) });
  });
  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
  return { snapshots, savedLive };
}

async function basePoint(page, base) {
  const coords = { 1: [352.5, 445.5], 2: [240, 333], 3: [127.5, 445.5], 4: [240, 555] };
  return page.evaluate(([x, y]) => {
    const svg = document.querySelector('.ballpark-svg');
    const point = svg.createSVGPoint();
    point.x = x; point.y = y;
    const screen = point.matrixTransform(svg.getScreenCTM());
    return { x: screen.x, y: screen.y };
  }, coords[base]);
}

async function dragFielderToBase(page, pos, base) {
  const card = page.locator(`g.defender-hit[data-pos="${pos}"]`).first();
  const box = await card.boundingBox();
  expect(box).toBeTruthy();
  const target = await basePoint(page, base);
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(260);
  await page.mouse.move(target.x, target.y, { steps: 14 });
  await page.waitForTimeout(160);
  await page.mouse.up();
  await expect(page.getByRole('button', { name: '封杀出局' })).toBeVisible();
  await page.getByRole('button', { name: '封杀出局' }).click();
  await page.waitForTimeout(220);
}

async function saveSnapshot(page, snapshots) {
  const before = snapshots.length;
  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => snapshots.length).toBeGreaterThan(before);
  return JSON.parse(snapshots.at(-1));
}

async function openDoublePlayGroundBall(page) {
  await page.getByRole('button', { name: '界内球' }).click();
  const menu = page.locator('.lgpfm__panel');
  await expect(menu).toBeVisible();
  await page.waitForTimeout(350);
  await menu.locator('.lgpfm__cn').filter({ hasText: /^滚地球$/ }).click();
  await menu.locator('.lgpfm__cn').filter({ hasText: /^双杀$/ }).click();
}

async function record654(page, onFirstOut) {
  // 6 接球点（SS）
  await page.locator('g.defender-hit[data-pos="SS"]').click();
  // 点击 5 传球，拖 5 到三垒记第 1 个出局
  await page.locator('g.defender-hit[data-pos="3B"]').click();
  await dragFielderToBase(page, '3B', 3);
  if (onFirstOut) await onFirstOut();
  // 点击 4 传球，拖 4 到二垒记第 2 个出局
  await page.locator('g.defender-hit[data-pos="2B"]').click();
  await dragFielderToBase(page, '2B', 2);
}

function assert654Result(snap, { extended = false } = {}) {
  expect(snap.gameState.outs).toBe(2);
  expect(snap.gameState.runners[1]).toBe(32);
  expect(snap.gameState.runners[2]).toBeNull();
  expect(snap.gameState.runners[3]).toBeNull();
  const logText = snap.eventLog.map((entry) => (typeof entry === 'string' ? entry : JSON.stringify(entry))).join('\n');
  expect(logText).toContain('6-5-4');
  expect(logText).toContain('一垒跑者');
  expect(logText).toContain('二垒跑者');

  const ss = snap.playerStatsById['106'].fielding;
  const third = snap.playerStatsById['105'].fielding;
  const second = snap.playerStatsById['104'].fielding;
  const batter = snap.playerStatsById['32'].batting;
  expect(ss).toMatchObject({ a: 1, dp: 1 });
  expect(third).toMatchObject({ po: 1, a: 1, dp: 1 });
  expect(second).toMatchObject({ po: 1, dp: 1 });
  expect(batter).toMatchObject({ ab: 1, gdp: 0 });
  if (extended) {
    // 第二出局后的延长传球只显示在链上，不参与双杀守备统计
    expect(third.a).toBe(1);
    expect(snap.playerStatsById['103'].fielding.a).toBe(0);
  }
}

test('6-5-4：拖 5 到三垒、4 到二垒，打者上一垒且不记 GDP', async ({ page }) => {
  const { snapshots, savedLive } = await setup(page);
  const flowBar = page.locator('.les-flow-card');
  await openDoublePlayGroundBall(page);
  await record654(page, async () => {
    await expect(flowBar).toContainText('已记录');
  });
  await expect(flowBar).toContainText('上一垒');

  await page.getByRole('button', { name: '完成记录' }).click();
  await expect(flowBar).toHaveCount(0);

  const snap = await saveSnapshot(page, snapshots);
  assert654Result(snap);
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
});

test('6-4-3：拖 4 到二垒、3 到一垒，打者出局并记 GDP，二垒跑者进三垒', async ({ page }) => {
  const { snapshots } = await setup(page);
  const flowBar = page.locator('.les-flow-card');
  await openDoublePlayGroundBall(page);
  await page.locator('g.defender-hit[data-pos="SS"]').click();
  await page.locator('g.defender-hit[data-pos="2B"]').click();
  await dragFielderToBase(page, '2B', 2);
  await page.locator('g.defender-hit[data-pos="1B"]').click();
  await dragFielderToBase(page, '1B', 1);
  await expect(flowBar).toContainText('出局');

  await page.getByRole('button', { name: '完成记录' }).click();
  await expect(flowBar).toHaveCount(0);

  const snap = await saveSnapshot(page, snapshots);
  expect(snap.gameState.outs).toBe(2);
  expect(snap.gameState.runners[1]).toBeNull();
  expect(snap.gameState.runners[2]).toBeNull();
  expect(snap.gameState.runners[3]).toBe(26);
  expect(snap.playerStatsById['32'].batting).toMatchObject({ ab: 1, gdp: 1 });
  expect(snap.playerStatsById['106'].fielding).toMatchObject({ a: 1, dp: 1 });
  expect(snap.playerStatsById['104'].fielding).toMatchObject({ po: 1, a: 1, dp: 1 });
  expect(snap.playerStatsById['103'].fielding).toMatchObject({ po: 1, dp: 1 });
  const logText = snap.eventLog.map((entry) => (typeof entry === 'string' ? entry : JSON.stringify(entry))).join('\n');
  expect(logText).toContain('6-4-3');
});

test('第二个出局后可继续延长传球链，完成记录仍只结算两个出局', async ({ page }) => {
  const { snapshots } = await setup(page);
  const flowBar = page.locator('.les-flow-card');
  await openDoublePlayGroundBall(page);
  await record654(page);
  // 4→3 延长传球：链变长，但出局点仍是三垒、二垒
  await page.locator('g.defender-hit[data-pos="1B"]').click();
  await expect(flowBar).toContainText('4（二垒）→3');

  await page.getByRole('button', { name: '完成记录' }).click();
  await expect(flowBar).toHaveCount(0);

  const snap = await saveSnapshot(page, snapshots);
  assert654Result(snap, { extended: true });
});

test('双杀入口：仅二垒有人时不提供双杀', async ({ page }) => {
  await setup(page, { runners: 'secondOnly' });
  await page.getByRole('button', { name: '界内球' }).click();
  const menu = page.locator('.lgpfm__panel');
  await expect(menu).toBeVisible();
  await page.waitForTimeout(350);
  await menu.locator('.lgpfm__cn').filter({ hasText: /^滚地球$/ }).click();
  await expect(menu.locator('.lgpfm__cn').filter({ hasText: /^双杀$/ })).toHaveCount(0);
  await expect(menu.locator('.lgpfm__cn').filter({ hasText: /^三杀$/ })).toHaveCount(0);
});
