import { test, expect } from '@playwright/test';

// #478 第 9 项：换人模式提交 / 失败重试 / 离场 E2E
// 运行：E2E_PORT=3021 npx playwright test e2e/live-game-substitution-flows-local-mocked.spec.js
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
    number: String(id % 100),
    teamId,
    position,
    positions: position ? [position] : [],
    stats: {
      batting: zeroBatting(),
      pitching: zeroPitching(),
      fielding: zeroFielding(),
    },
  };
}

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
  player(201, '客投手', 'P', 2),
  player(202, '客捕手', 'C', 2),
  player(203, '客一垒', '1B', 2),
  player(204, '客二垒', '2B', 2),
  player(205, '客三垒', '3B', 2),
  player(206, '客游击', 'SS', 2),
  player(207, '客左外', 'LF', 2),
  player(208, '客中外', 'CF', 2),
  player(209, '客右外', 'RF', 2),
];
const homeBench = [player(111, 'H替补', '', 1)];
const awayBench = [player(211, 'A替补', '', 2)];
const allPlayers = [...homeLineup, ...awayLineup, ...homeBench, ...awayBench];

function buildSnapshot() {
  return {
    v: 1,
    savedAt: 1,
    gameId: 10,
    gameMode: 'BASEBALL',
    gameStarted: true,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    homeLineup,
    awayLineup,
    homeBench,
    awayBench,
    homeFieldingPitcherId: 101,
    awayFieldingPitcherId: 201,
    homeFieldingPitcherRow: null,
    awayFieldingPitcherRow: null,
    homeLineupStarterIds: [...homeLineup.map((p) => p.id), null, null, null],
    awayLineupStarterIds: [...awayLineup.map((p) => p.id), null, null, null],
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeUnavailablePlayerIds: [],
    awayUnavailablePlayerIds: [],
    awayLastDhAddedFromPoolId: null,
    homeLastDhAddedFromPoolId: null,
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
      isTop: true,
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

async function setup(page, { saveLiveFailures = 0 } = {}) {
  const savedLive = [];
  const snapshots = [];
  const snapshot = buildSnapshot();
  let failures = saveLiveFailures;

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
    let status = 200;
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0,
    };
    else if (path === '/game/10/stats') data = { list: [], total: 0 };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: JSON.stringify(snapshot) };
    } else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      const json = request.postDataJSON().snapshotJson;
      snapshots.push(json);
      data = { snapshotJson: json };
    } else if (path === '/game/10/save-live') {
      if (failures > 0) {
        failures--;
        status = 500;
        data = { message: '模拟保存失败' };
      } else {
        savedLive.push(request.postDataJSON());
        data = { success: true };
      }
    } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) {
      const teamId = Number(path.split('/')[4]);
      data = { restricted: false, players: allPlayers.filter((p) => p.teamId === teamId) };
    } else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = allPlayers.filter((p) => p.teamId === teamId);
      data = { list, total: list.length };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === 1 ? 'Home' : 'Away' };
    }
    await route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(status === 200 ? { code: 200, data } : data),
    });
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '换人模式' })).toBeVisible();
  return { savedLive, snapshots };
}

async function dragBenchToField(page, playerId, pos) {
  const from = await page.locator(`.live-bench-card[data-player-id="${playerId}"][data-team-side="defense"]`).boundingBox();
  const to = await page.locator(`g.defender-hit[data-pos="${pos}"]`).boundingBox();
  expect(from && to).toBeTruthy();
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(300);
  await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, { steps: 12 });
  await page.mouse.up();
}

async function submitSubstitution(page) {
  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
}

test('换人提交成功：落 SUBSTITUTION 事件与统计换人', async ({ page }) => {
  const { savedLive } = await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  await dragBenchToField(page, 111, 'CF');
  await submitSubstitution(page);
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);

  const body = savedLive.at(-1);
  const event = (body.substitutions || [])[0];
  expect(event).toMatchObject({
    changeType: 'SUBSTITUTION', slot: 8, inPlayerId: 111, inPosition: 'CF',
  });
  expect(body.stats.some((row) => row.teamId === 1 && row.playerId === 111 && row.position === 'CF')).toBe(true);
});

test('换人提交失败后重试：保留模式、同一事务只落一次', async ({ page }) => {
  const { savedLive } = await setup(page, { saveLiveFailures: 1 });
  await page.getByRole('button', { name: '换人模式' }).click();
  await dragBenchToField(page, 111, 'CF');
  await submitSubstitution(page);
  await expect(page.getByText('换人模式进行中')).toBeVisible();
  expect(savedLive).toHaveLength(0);

  await submitSubstitution(page);
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
  const body = savedLive.at(-1);
  const substitutions = (body.substitutions || []).filter((event) => event.changeType === 'SUBSTITUTION');
  expect(substitutions).toHaveLength(1);
  expect(substitutions[0]).toMatchObject({ slot: 8, inPlayerId: 111 });
});

test('换人模式离场：离开确认后取消模式并保存轻量现场', async ({ page }) => {
  const { savedLive, snapshots } = await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  await expect(page.getByText('换人模式进行中')).toBeVisible();
  await dragBenchToField(page, 111, 'CF');
  await expect(page.locator('g.defender-hit[data-pos="CF"]')).toContainText('#11');

  await page.getByRole('link', { name: '工作台' }).first().click();
  await expect(page.getByText('离开确认')).toBeVisible();
  await page.getByRole('button', { name: '直接离开' }).click();
  await expect(page).toHaveURL(/\/admin\/dashboard/);
  expect(savedLive).toHaveLength(0);
  expect(snapshots.length).toBeGreaterThan(0);
  const snapshot = JSON.parse(snapshots.at(-1));
  expect(snapshot.homeLineup[7].id).toBe(108);
  expect(snapshot.homeBench.some((player) => player.id === 111)).toBe(true);
});

test('拖拽场员到替补席：拖拽卡片全程可见并保留红绿放大反馈', async ({ page }) => {
  const { savedLive } = await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  const field = await page.locator('g.defender-hit[data-pos="CF"]').boundingBox();
  const bench = await page.locator('.live-bench-card[data-player-id="111"][data-team-side="defense"]').boundingBox();
  expect(field && bench).toBeTruthy();

  await page.mouse.move(field.x + field.width / 2, field.y + field.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(320);
  await page.mouse.move(bench.x + bench.width / 2, bench.y + bench.height / 2, { steps: 16 });
  await page.waitForTimeout(120);

  // 场上卡片移出 SVG 会被裁切：改用 fixed 浮层保持可见，且原卡片暂时隐藏避免重影
  const ghost = page.locator('.live-bench-ghost.live-bench-ghost--fielder');
  await expect(ghost).toBeVisible();
  await expect(ghost).toContainText('CF · 主中外 #8');
  await expect(page.locator('g.defender-hit[data-pos="CF"]')).toHaveClass(/field-player-hit--ghost/);
  // 替补席目标绿色放大
  await expect(page.locator('.live-bench-card[data-player-id="111"][data-team-side="defense"]')).toHaveClass(/live-bench-card--swap-in/);

  await page.mouse.up();
  await expect(ghost).toHaveCount(0);
  await submitSubstitution(page);
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
  expect((savedLive.at(-1).substitutions || [])[0]).toMatchObject({
    changeType: 'SUBSTITUTION', slot: 8, inPlayerId: 111, inPosition: 'CF',
  });
});
