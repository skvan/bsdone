import { test, expect } from '@playwright/test';

// 换人后：换下球员保留在替补席灰显、不可再拖拽/点选上场（棒球）
// 覆盖：DH 队仅换投（旧投手回板凳）与无 DH 队投手换下（原先会被过滤消失）

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

function buildSnapshot({ dh }) {
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
  const homeFielders = [
    player(102, '主捕手', 'C', 1),
    player(103, '主一垒', '1B', 1),
    player(104, '主二垒', '2B', 1),
    player(105, '主三垒', '3B', 1),
    player(106, '主游击', 'SS', 1),
    player(107, '主左外', 'LF', 1),
    player(108, '主中外', 'CF', 1),
    player(109, '主右外', 'RF', 1),
  ];
  const homePitcher = player(120, '主投手', 'P', 1);
  if (!dh) {
    homePitcher.stats.pitching.np = 12;
    homePitcher.stats.pitching.pitchBf = 3;
    homePitcher.stats.pitching.pitchPa = 3;
  }
  const homeLineup = dh
    ? [...homeFielders, player(101, '主DH', 'DH', 1)]
    : [...homeFielders, player(101, '主投手', 'P', 1)];
  if (!dh) {
    homeLineup[8].stats.pitching = homePitcher.stats.pitching;
  }
  const homeBench = [player(111, 'H替补', '', 1)];
  const rosterExtra = player(121, '主替补2', '', 1);
  const allPlayers = [...homeLineup, ...awayLineup, ...homeBench, rosterExtra, ...(dh ? [homePitcher] : [])];

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
    },
    allPlayers,
  };
}

async function setup(page, { dh }) {
  const savedLive = [];
  const snapshots = [];
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
      snapshots.push(request.postDataJSON().snapshotJson);
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
  return { savedLive, snapshots };
}

async function benchToPitcher(page, playerId) {
  const from = await page.locator(`.live-bench-card[data-player-id="${playerId}"][data-team-side="defense"]`).boundingBox();
  const to = await page.locator('g.defender-hit[data-pos="P"]').boundingBox();
  expect(from && to).toBeTruthy();
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(320);
  await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, { steps: 12 });
  await page.mouse.up();
}

async function submit(page, savedLive) {
  await page.getByRole('button', { name: '结束换人' }).click();
  await expect(page.getByText('确认换人')).toBeVisible();
  await page.getByRole('button', { name: '提交换人' }).click();
  await expect.poll(() => savedLive.length).toBeGreaterThan(0);
}

test('DH 队换投：旧投手回到板凳灰显，不能再拖拽/点选上场', async ({ page }) => {
  const { savedLive, snapshots } = await setup(page, { dh: true });
  await page.getByRole('button', { name: '换人模式' }).click();
  await benchToPitcher(page, 111);
  await submit(page, savedLive);

  await expect.poll(() => snapshots.length).toBeGreaterThan(0);
  const snap = JSON.parse(snapshots.at(-1));
  expect(snap.homeFieldingPitcherId).toBe(111);
  expect((snap.homeBench || []).map((p) => p.id)).toContain(120);
  expect(snap.homeUnavailablePlayerIds).toContain(120);
  expect(snap.homeUnavailablePlayerIds).not.toContain(111);

  const outCard = page.locator('.live-bench-card[data-player-id="120"][data-team-side="defense"]');
  await expect(outCard).toBeVisible();
  await expect(outCard).toBeDisabled();
  await expect(outCard).toHaveClass(/live-bench-card--disabled/);
  expect(await outCard.evaluate((el) => Number(getComputedStyle(el).opacity))).toBeLessThan(1);

  // 点选无效
  const box = await outCard.boundingBox();
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await expect(outCard).not.toHaveClass(/live-bench-card--pending/);

  // 再进换人模式，拖向 CF 也无效
  await page.getByRole('button', { name: '换人模式' }).click();
  const from = await outCard.boundingBox();
  const to = await page.locator('g.defender-hit[data-pos="CF"]').boundingBox();
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(320);
  await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, { steps: 12 });
  await page.mouse.up();
  await expect(page.locator('g.defender-hit[data-pos="CF"]')).toContainText('#8');
  await expect(page.locator('.live-bench-card[data-player-id="120"]')).not.toHaveClass(/live-bench-card--pending/);
});

test('无 DH 队换投：被换下投手保留在板凳灰显（不再消失）', async ({ page }) => {
  const { savedLive, snapshots } = await setup(page, { dh: false });
  await page.getByRole('button', { name: '换人模式' }).click();
  await benchToPitcher(page, 111);
  await submit(page, savedLive);

  await expect.poll(() => snapshots.length).toBeGreaterThan(0);
  const snap = JSON.parse(snapshots.at(-1));
  expect(snap.homeFieldingPitcherId).toBe(111);
  expect((snap.homeBench || []).map((p) => p.id)).toContain(101);
  expect(snap.homeUnavailablePlayerIds).toContain(101);

  const outCard = page.locator('.live-bench-card[data-player-id="101"][data-team-side="defense"]');
  await expect(outCard).toBeVisible();
  await expect(outCard).toBeDisabled();
  await expect(outCard).toHaveClass(/live-bench-card--disabled/);

  await page.getByRole('button', { name: '换人模式' }).click();
  await expect(outCard).toBeDisabled();
  await page.mouse.click((await outCard.boundingBox()).x + 4, (await outCard.boundingBox()).y + 4);
  await expect(outCard).not.toHaveClass(/live-bench-card--pending/);
});


test('追加新球员：可恢复灰显球员，也可加入不在板凳的名单球员', async ({ page }) => {
  const { savedLive, snapshots } = await setup(page, { dh: true });
  await page.getByRole('button', { name: '换人模式' }).click();
  await benchToPitcher(page, 111);
  await submit(page, savedLive);

  const outCard = page.locator('.live-bench-card[data-player-id="120"][data-team-side="defense"]');
  await expect(outCard).toBeDisabled();

  const picker = page.locator('.lgsfm__panel');
  const openPicker = async () => {
    await page.locator('g.defender-hit[data-pos="CF"]').click();
    await expect(picker).toBeVisible();
  };
  const appendFromPicker = async (optionText) => {
    await page.waitForTimeout(450);
    await picker.getByRole('button', { name: '追加新球员' }).click();
    const dialog = page.locator('.el-dialog', { hasText: '追加板凳球员' });
    await expect(dialog).toBeVisible();
    await dialog.locator('.el-select').click();
    const option = page.locator('.el-select-dropdown__item').filter({ hasText: optionText });
    await expect(option).toBeVisible();
    await option.click();
    await dialog.getByRole('button', { name: '确认追加' }).click();
    await expect(picker).toBeVisible();
  };

  await openPicker();
  await appendFromPicker('主投手');
  await picker.getByRole('button', { name: '关闭' }).click();
  await expect(outCard).toBeEnabled();
  await expect(outCard).not.toHaveClass(/live-bench-card--disabled/);

  await openPicker();
  await appendFromPicker('主替补2');
  await picker.getByRole('button', { name: '关闭' }).click();
  const addedCard = page.locator('.live-bench-card[data-player-id="121"][data-team-side="defense"]');
  await expect(addedCard).toBeVisible();
  await expect(addedCard).toBeEnabled();

  // 保存后恢复状态落库：120 不再不可上，121 已在板凳
  const snapshotsBeforeSave = snapshots.length;
  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => snapshots.length).toBeGreaterThan(snapshotsBeforeSave);
  const snap = JSON.parse(snapshots.at(-1));
  expect(snap.homeUnavailablePlayerIds).not.toContain(120);
  expect((snap.homeBench || []).map((p) => p.id)).toContain(121);

  // 换人模式内不允许追加（避免只动板凳被事务回滚）
  await page.getByRole('button', { name: '换人模式' }).click();
  await page.locator('g.defender-hit[data-pos="CF"]').click();
  await expect(picker).toBeVisible();
  await page.waitForTimeout(450);
  await picker.getByRole('button', { name: '追加新球员' }).click();
  await expect(page.getByText('换人模式进行中，请先结束或取消换人模式后再追加球员')).toBeVisible();
  await expect(page.locator('.el-dialog', { hasText: '追加板凳球员' })).toBeHidden();
  await picker.getByRole('button', { name: '关闭' }).click();
});
