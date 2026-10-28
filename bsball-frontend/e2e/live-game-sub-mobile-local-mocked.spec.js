import { test, expect } from '@playwright/test';

// #478 第 8 项：换人模式小屏（390x844）替补席抽屉与点选换人

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
const homeBench = [player(111, 'H替补', '', 1), player(112, 'H替补2', '', 1), player(113, 'H替补3', '', 1)];
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

async function setup(page) {
  const savedLive = [];
  const snapshot = buildSnapshot();

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
      data = { snapshotJson: request.postDataJSON().snapshotJson };
    } else if (path === '/game/10/save-live') {
      savedLive.push(request.postDataJSON());
      data = { success: true };
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
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data }),
    });
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '换人模式' })).toBeVisible();
  return { savedLive };
}

test('小屏换人：底部替补席选人后收起，点守备员完成换人并提交', async ({ page }) => {
  const { savedLive } = await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  await expect(page.getByText('换人模式进行中')).toBeVisible();

  const sheet = page.locator('.live-bench-panel--defense');
  await expect(sheet).toBeVisible();
  const card = sheet.locator('.live-bench-card[data-player-id="111"]');
  await expect(card).toBeVisible();
  // 横向可滑动：多替补时列表可滚动，卡片允许横向手势
  expect(await sheet.locator('.live-bench-panel__list').evaluate((el) => el.scrollWidth > el.clientWidth)).toBe(true);
  expect(await card.evaluate((el) => getComputedStyle(el).touchAction)).toBe('pan-x');
  await card.click();
  await expect(card).toHaveAttribute('aria-pressed', 'true');
  // 选人后抽屉收起，露出全场便于点守备员
  await expect(sheet).toBeHidden();
  await expect(page.locator('.live-sub-bench-pill')).toBeVisible();

  await page.locator('g.defender-hit[data-pos="CF"]').click();
  await expect(page.locator('g.defender-hit[data-pos="CF"]')).toContainText('#11');

  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);

  const event = (savedLive.at(-1).substitutions || [])[0];
  expect(event).toMatchObject({ changeType: 'SUBSTITUTION', slot: 8, inPlayerId: 111, inPosition: 'CF' });
});

test('小屏换人：进攻/防守替补席切换与浮动按钮重开', async ({ page }) => {
  await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();

  const defenseSheet = page.locator('.live-bench-panel--defense');
  const offenseSheet = page.locator('.live-bench-left-col');
  await expect(defenseSheet).toBeVisible();
  await expect(offenseSheet).toBeHidden();
  await expect(page.getByRole('button', { name: '防守替补' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('button', { name: '进攻替补' })).toHaveAttribute('aria-pressed', 'false');

  await page.getByRole('button', { name: '进攻替补' }).click();
  await expect(offenseSheet).toBeVisible();
  await expect(defenseSheet).toBeHidden();
  await expect(offenseSheet.locator('.live-bench-card[data-player-id="211"]')).toBeVisible();

  // 再点当前侧按钮 = 收起抽屉
  await page.getByRole('button', { name: '进攻替补' }).click();
  await expect(offenseSheet).toBeHidden();
  await page.locator('.live-sub-bench-pill').click();
  await expect(offenseSheet).toBeVisible();
});


test('取消换人模式后不残留待放置卡片', async ({ page }) => {
  await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  const defenseSheet = page.locator('.live-bench-panel--defense');
  const card = defenseSheet.locator('.live-bench-card[data-player-id="111"]');
  await card.click();
  await expect(card).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: '取消换人模式' }).click();
  await expect(defenseSheet).toBeHidden();

  // 模式已退出：点守备员不应消费已取消的待放置卡片（不会静默换人）
  await page.locator('g.defender-hit[data-pos="CF"]').click();
  await expect(page.locator('g.defender-hit[data-pos="CF"]')).toContainText('#8');
});
