import { test, expect } from '@playwright/test';

// 换人模式 DH 取消：P↔野手互换确认、DH 卡片拖离、规则提醒
// 运行：E2E_PORT=3019 npx playwright test e2e/live-game-dh-substitution.spec.js
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

function buildSnapshot({ dhBatted = false } = {}) {
  const awayFielders = [
    player(201, '客捕手', 'C', 2),
    player(202, '客一垒', '1B', 2),
    player(203, '客二垒', '2B', 2),
    player(204, '客三垒', '3B', 2),
    player(205, '客游击', 'SS', 2),
    player(206, '客左外', 'LF', 2),
    player(207, '客中外', 'CF', 2),
    player(208, '客右外', 'RF', 2),
    player(209, '客DH', 'DH', 2, { batting: dhBatted ? { ab: 1, so: 1 } : {} }),
  ];
  const awayPitcher = player(220, '客投手', 'P', 2, { pitching: { np: 10, pitchBf: 3, pitchPa: 3 } });
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
  const awayIds = awayFielders.map((p) => p.id);
  const homeIds = homeLineup.map((p) => p.id);
  return {
    v: 1,
    savedAt: 1,
    gameId: 10,
    gameMode: 'BASEBALL',
    gameStarted: true,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
    homeLineup,
    awayLineup: awayFielders,
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
      isTop: false,
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
    if (path === '/event/1') data = { id: 1, name: 'DH E2E Event' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'bottom', homeScore: 0, awayScore: 0,
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
      data = { restricted: false, players: players.filter((p) => p.teamId === teamId) };
    } else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      data = { list: players.filter((p) => p.teamId === teamId), total: players.filter((p) => p.teamId === teamId).length };
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
  return { savedLive };
}

async function dragFieldToField(page, fromPos, toPos) {
  const from = await page.locator(`g.defender-hit[data-pos="${fromPos}"]`).boundingBox();
  const to = await page.locator(`g.defender-hit[data-pos="${toPos}"]`).boundingBox();
  expect(from && to).toBeTruthy();
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(300);
  await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, { steps: 12 });
  await page.mouse.up();
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

async function dragDhCardOut(page) {
  const card = page.locator('.live-sub-dh-card');
  const box = await card.boundingBox();
  expect(box).toBeTruthy();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2, box.y + 120, { steps: 10 });
  await page.mouse.up();
  return card;
}

test('投手↔野手互换：确认后先取消 DH 再交换守位，提交含 DH_CANCEL/守位/换投', async ({ page }) => {
  const { savedLive } = await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  await expect(page.locator('.live-sub-dh-card')).toContainText('客DH');

  await dragFieldToField(page, 'P', '3B');
  const dialog = page.getByRole('dialog', { name: '取消DH并换位' });
  await expect(dialog).toContainText('此操作将取消 DH');
  await expect(dialog).toContainText('客DH');
  await page.getByRole('button', { name: '确认取消DH并换位' }).click();
  await expect(page.locator('.live-sub-dh-card')).toHaveCount(0);

  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await expect(page.locator('.el-message-box__message')).toContainText('取消DH');
  await expect(page.locator('.el-message-box__message')).toContainText('投手更换');
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);

  const body = savedLive.at(-1);
  const types = (body.substitutions || []).map((event) => event.changeType);
  expect(types).toContain('DH_CANCEL');
  expect(types).toContain('POSITION_CHANGE');
  expect(types).toContain('PITCHING_CHANGE');
  const dhEvent = (body.substitutions || []).find((event) => event.changeType === 'DH_CANCEL');
  expect(dhEvent).toMatchObject({ slot: 9, outPlayerId: 209, inPlayerId: 220 });
  const awayStats = (body.stats || []).filter((row) => row.teamId === 2);
  expect(awayStats.find((row) => row.playerId === 220)).toMatchObject({ position: '3B' });
  expect(awayStats.find((row) => row.playerId === 204)).toMatchObject({ position: 'P' });
  expect(awayStats.find((row) => row.playerId === 209)).toMatchObject({ position: 'DH', battingOrder: 9 });
});

test('野手拖到投手：反向互换同样确认取消 DH 并交换守位', async ({ page }) => {
  const { savedLive } = await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  await dragFieldToField(page, '3B', 'P');
  const dialog = page.getByRole('dialog', { name: '取消DH并换位' });
  await expect(dialog).toContainText('此操作将取消 DH');
  await page.getByRole('button', { name: '确认取消DH并换位' }).click();
  await expect(page.locator('.live-sub-dh-card')).toHaveCount(0);

  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
  const body = savedLive.at(-1);
  const dhEvent = (body.substitutions || []).find((event) => event.changeType === 'DH_CANCEL');
  expect(dhEvent).toMatchObject({ slot: 9, outPlayerId: 209, inPlayerId: 220 });
  const awayStats = (body.stats || []).filter((row) => row.teamId === 2);
  expect(awayStats.find((row) => row.playerId === 220)).toMatchObject({ position: '3B' });
  expect(awayStats.find((row) => row.playerId === 204)).toMatchObject({ position: 'P' });
});

test('替补投手上场后拖离 DH 卡片：新投手进入 DH 棒次', async ({ page }) => {
  const { savedLive } = await setup(page, { dhBatted: true });
  await page.getByRole('button', { name: '换人模式' }).click();
  await dragBenchToField(page, 230, 'P');
  await expect(page.locator('.live-sub-dh-card')).toHaveCount(1);

  const card = await dragDhCardOut(page);
  await expect(card).toHaveCount(0);

  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
  const body = savedLive.at(-1);
  const dhEvent = (body.substitutions || []).find((event) => event.changeType === 'DH_CANCEL');
  expect(dhEvent).toMatchObject({ slot: 9, outPlayerId: 209, inPlayerId: 230 });
  const awayPitchers = (body.stats || []).filter((row) => row.teamId === 2 && row.isPitcher === 1);
  expect(awayPitchers.find((row) => row.playerId === 220)).toMatchObject({ pitcherOrder: 1 });
  expect(awayPitchers.find((row) => row.playerId === 230)).toMatchObject({ position: 'P', pitcherOrder: 2 });
});

test('拖离 DH 卡片：先发 DH 已打击时直接取消 DH', async ({ page }) => {
  const { savedLive } = await setup(page, { dhBatted: true });
  await page.getByRole('button', { name: '换人模式' }).click();
  const card = page.locator('.live-sub-dh-card');
  const box = await card.boundingBox();
  expect(box).toBeTruthy();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2, box.y + 120, { steps: 10 });
  await page.mouse.up();
  await expect(card).toHaveCount(0);

  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
  const body = savedLive.at(-1);
  expect((body.substitutions || []).map((e) => e.changeType)).toContain('DH_CANCEL');
  expect((body.stats || []).find((row) => row.teamId === 2 && row.playerId === 220))
    .toMatchObject({ position: 'P' });
});

test('先发 DH 未打击且对方未换投：拖离 DH 卡片先弹规则提醒', async ({ page }) => {
  await setup(page);
  await page.getByRole('button', { name: '换人模式' }).click();
  const card = page.locator('.live-sub-dh-card');
  const box = await card.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2, box.y + 120, { steps: 10 });
  await page.mouse.up();
  await expect(page.getByText('取消DH规则提醒')).toBeVisible();
  await expect(page.getByText(/先发 DH/)).toBeVisible();
  await page.getByRole('button', { name: '返回' }).click();
  await expect(card).toHaveCount(1);
});
