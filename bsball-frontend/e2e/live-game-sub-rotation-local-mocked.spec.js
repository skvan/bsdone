import { test, expect } from '@playwright/test';

// #478 扩展：换人模式守备轮转速记栏（闭环数字串）
// 覆盖：1341 轮转落位与提交、非法输入拦截、含投手+DH 的取消 DH 确认流程

const zeroBatting = () => ({
  ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, so_swing: 0, so_looking: 0,
  sf: 0, sh: 0, sb: 0, cs: 0, doubles: 0, triples: 0, hr: 0, insideParkHr: 0, gdp: 0, ibb: 0, io: 0,
});
const zeroPitching = () => ({
  ip: 0, np: 0, pitchH: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, pitchInsideParkHr: 0,
  wp: 0, bk: 0, pitchPa: 0, pitchBf: 0, pitchBb: 0, pitchIbb: 0, pitchHbp: 0, pitchIo: 0,
  go: 0, fo: 0, er: 0,
});
const zeroFielding = () => ({ po: 0, a: 0, e: 0 });

function player(id, name, position, teamId) {
  return {
    id,
    name,
    number: String(id % 100),
    teamId,
    position,
    positions: position ? [position] : [],
    stats: { batting: zeroBatting(), pitching: zeroPitching(), fielding: zeroFielding() },
  };
}

function buildSnapshot({ dh }) {
  const awayLineup = [
    player(201, '客投手', 'P', 2), player(202, '客捕手', 'C', 2), player(203, '客一垒', '1B', 2),
    player(204, '客二垒', '2B', 2), player(205, '客三垒', '3B', 2), player(206, '客游击', 'SS', 2),
    player(207, '客左外', 'LF', 2), player(208, '客中外', 'CF', 2), player(209, '客右外', 'RF', 2),
  ];
  const homeFielders = [
    player(102, '主捕手', 'C', 1), player(103, '主一垒', '1B', 1), player(104, '主二垒', '2B', 1),
    player(105, '主三垒', '3B', 1), player(106, '主游击', 'SS', 1), player(107, '主左外', 'LF', 1),
    player(108, '主中外', 'CF', 1), player(109, '主右外', 'RF', 1),
  ];
  const homePitcher = player(120, '主投手', 'P', 1);
  const homeLineup = dh
    ? [...homeFielders, player(101, '主DH', 'DH', 1)]
    : [...homeFielders, player(101, '主投手', 'P', 1)];
  const homeBench = [player(111, 'H替补', '', 1)];
  const allPlayers = [...homeLineup, ...awayLineup, ...homeBench, ...(dh ? [homePitcher] : [])];

  return {
    snapshot: {
      v: 1,
      savedAt: 1,
      gameId: 10,
      gameMode: 'BASEBALL',
      gameStarted: true,
      setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
      homeLineup,
      awayLineup,
      homeBench,
      awayBench: [player(211, 'A替补', '', 2)],
      homeFieldingPitcherId: dh ? 120 : 101,
      awayFieldingPitcherId: 201,
      homeFieldingPitcherRow: dh ? homePitcher : null,
      awayFieldingPitcherRow: null,
      homeLineupStarterIds: [...homeLineup.map((p) => p.id), null, null, null],
      awayLineupStarterIds: [...awayLineup.map((p) => p.id), null, null, null],
      homeSlotSubHistory: Array.from({ length: 12 }, () => []),
      awaySlotSubHistory: Array.from({ length: 12 }, () => []),
      homeUnavailablePlayerIds: [],
      awayUnavailablePlayerIds: [],
      awayLastDhAddedFromPoolId: null,
      homeLastDhAddedFromPoolId: null,
      teamsData: { away: { score: 0, innings: [0] }, home: { score: 0, innings: [0] } },
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
    },
    allPlayers,
  };
}

async function setup(page, { dh }) {
  const savedLive = [];
  const { snapshot, allPlayers } = buildSnapshot({ dh });

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
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '换人模式' })).toBeVisible();
  return { savedLive };
}

const cardNum = (page, pos) => page.locator(`g.defender-hit[data-pos="${pos}"] .defender-card__num`).textContent();

async function rotate(page, value) {
  const input = page.locator('.live-sub-rotation__input');
  await input.fill(value);
  await input.press('Enter');
}

async function submit(page, savedLive) {
  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
}

test('无 DH：1341 完成 1→3→4→1 轮转，提交落位置变更与投手更换', async ({ page }) => {
  const { savedLive } = await setup(page, { dh: false });
  await page.getByRole('button', { name: '换人模式' }).click();
  await expect(page.getByText('换人模式进行中')).toBeVisible();

  expect(await cardNum(page, 'P')).toBe('#1');
  await rotate(page, '1341');
  await expect(page.getByText('守备轮转已应用')).toBeVisible();
  await expect(page.locator('.live-sub-rotation__input')).toHaveValue('');
  expect(await cardNum(page, 'P')).toBe('#4');
  expect(await cardNum(page, '1B')).toBe('#1');
  expect(await cardNum(page, '2B')).toBe('#3');
  expect(await cardNum(page, '3B')).toBe('#5');

  await submit(page, savedLive);
  const body = savedLive.at(-1);
  const positionOf = (playerId) => body.stats.find((s) => Number(s.playerId) === playerId)?.position;
  expect(positionOf(101)).toBe('1B');
  expect(positionOf(103)).toBe('2B');
  expect(positionOf(104)).toBe('P');
  const events = body.substitutions || [];
  expect(events.filter((e) => e.changeType === 'POSITION_CHANGE').length).toBe(3);
  expect(events.some((e) => e.changeType === 'PITCHING_CHANGE' && Number(e.outPlayerId) === 101 && Number(e.inPlayerId) === 104)).toBe(true);
  expect(events.filter((e) => e.changeType === 'SUBSTITUTION').length).toBe(0);
});

test('非法速记串：提示格式错误且不改阵容', async ({ page }) => {
  await setup(page, { dh: false });
  await page.getByRole('button', { name: '换人模式' }).click();

  await rotate(page, '134');
  await expect(page.getByText('首尾数字必须相同')).toBeVisible();
  expect(await cardNum(page, 'P')).toBe('#1');

  await rotate(page, '1331');
  await expect(page.getByText('同一位置在轮转中只能出现一次')).toBeVisible();
  expect(await cardNum(page, 'P')).toBe('#1');

  await rotate(page, '13');
  await expect(page.getByText('轮转至少需要 3 位数字')).toBeVisible();
  expect(await cardNum(page, 'P')).toBe('#1');
});

test('有 DH：含 1 号位轮转先确认取消 DH，确认后完成轮转并落 DH_CANCEL', async ({ page }) => {
  const { savedLive } = await setup(page, { dh: true });
  await page.getByRole('button', { name: '换人模式' }).click();
  await expect(page.locator('.live-sub-dh-card')).toHaveCount(1);

  await rotate(page, '1341');
  await expect(page.getByText('守备轮转并取消DH')).toBeVisible();
  await page.getByRole('button', { name: '确认轮转' }).click();

  await expect(page.locator('.live-sub-dh-card')).toHaveCount(0);
  expect(await cardNum(page, 'P')).toBe('#4');
  expect(await cardNum(page, '1B')).toBe('#20');
  expect(await cardNum(page, '2B')).toBe('#3');

  await submit(page, savedLive);
  const body = savedLive.at(-1);
  const dhEvent = (body.substitutions || []).find((e) => e.changeType === 'DH_CANCEL');
  expect(dhEvent).toBeTruthy();
  expect(Number(dhEvent.outPlayerId)).toBe(101);
  expect(Number(dhEvent.inPlayerId)).toBe(120);
  const positionOf = (playerId) => body.stats.find((s) => Number(s.playerId) === playerId)?.position;
  expect(positionOf(120)).toBe('1B');
  expect(positionOf(103)).toBe('2B');
  expect(positionOf(104)).toBe('P');
});

test('有 DH：取消确认时不变更阵容', async ({ page }) => {
  await setup(page, { dh: true });
  await page.getByRole('button', { name: '换人模式' }).click();
  await rotate(page, '1341');
  await expect(page.getByText('守备轮转并取消DH')).toBeVisible();
  await page.getByRole('button', { name: '返回' }).click();
  await expect(page.locator('.live-sub-dh-card')).toHaveCount(1);
  expect(await cardNum(page, 'P')).toBe('#20');
  expect(await cardNum(page, '1B')).toBe('#3');
});
