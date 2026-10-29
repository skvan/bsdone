import { test, expect } from '@playwright/test';

// 赛前补齐替补席：保存阵容时，把全队可上场且未先发的球员自动写入替补席（取消「追加新球员」入口）
// 覆盖无 DH（P 在打序）与 DH 制（投手仅守备）两种先发结构

function player(id, name, position, teamId) {
  return {
    id,
    name,
    number: String(id % 100),
    teamId,
    position,
    positions: position ? [position] : [],
  };
}

const positions = ['C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'P'];
const awayStarters = positions.map((pos, i) => player(201 + i, `客队${i + 1}`, pos, 2));
const awayRoster = [...awayStarters, player(210, '客替补1', '', 2)];

function buildFixture(dh) {
  const homeStarters = dh
    ? [...positions.slice(0, 8).map((pos, i) => player(101 + i, `主队${i + 1}`, pos, 1)), player(112, '主队DH', 'DH', 1)]
    : positions.map((pos, i) => player(101 + i, `主队${i + 1}`, pos, 1));
  const homeRoster = dh
    ? [...homeStarters, player(110, '主投手', 'P', 1), player(111, '主替补1', '', 1)]
    : [...homeStarters, player(110, '主替补1', '', 1), player(111, '主替补2', '', 1)];
  return { homeStarters, homeRoster, dh };
}

function buildSnapshot(fixture) {
  const { homeStarters, dh } = fixture;
  return {
    v: 1,
    savedAt: 1,
    gameId: 10,
    gameMode: 'BASEBALL',
    gameStarted: false,
    setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '', gameTime: '2026-10-29 18:00:00', gameday: '2026-10-29' },
    homeLineup: homeStarters,
    awayLineup: awayStarters,
    homeBench: [],
    awayBench: [],
    homeFieldingPitcherId: dh ? 110 : 109,
    awayFieldingPitcherId: 209,
    homeFieldingPitcherRow: null,
    awayFieldingPitcherRow: null,
    homeLineupStarterIds: [...homeStarters.map((p) => p.id), null, null, null],
    awayLineupStarterIds: [...awayStarters.map((p) => p.id), null, null, null],
    homeSlotSubHistory: Array.from({ length: 12 }, () => []),
    awaySlotSubHistory: Array.from({ length: 12 }, () => []),
    homeUnavailablePlayerIds: [],
    awayUnavailablePlayerIds: [],
    awayLastDhAddedFromPoolId: null,
    homeLastDhAddedFromPoolId: null,
    teamsData: { away: { score: 0, innings: [] }, home: { score: 0, innings: [] } },
    eventLog: [],
    atBatSummaries: [],
    playEvents: [],
    playerStatsById: {},
  };
}

async function saveLineupAndReadSnapshot(page, fixture) {
  const snapshots = [];
  const { homeRoster } = fixture;
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
    const path = new URL(request.url()).pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL', specialRules: {} };
    else if (path === '/event/1/game-team-options') data = [{ id: 1, name: 'Home' }, { id: 2, name: 'Away' }];
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/event/1/teams/1/roster-players') data = { restricted: false, players: homeRoster };
    else if (path === '/event/1/teams/2/roster-players') data = { restricted: false, players: awayRoster };
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'scheduled',
      gameMode: 'BASEBALL', gameTime: '2026-10-29 18:00:00', gameday: '2026-10-29',
    };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: JSON.stringify(buildSnapshot(fixture)) };
    } else if (path === '/game/10/lineup-snapshot' && request.method() === 'POST') {
      snapshots.push(request.postDataJSON().snapshotJson);
      data = { snapshotJson: request.postDataJSON().snapshotJson };
    } else if (path === '/game/10/save-result') data = { success: true };
    else if (path === '/player/list') {
      const teamId = Number(new URL(request.url()).searchParams.get('teamId') ?? 1);
      const list = teamId === 1 ? homeRoster : awayRoster;
      data = { list, total: list.length };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data }),
    });
  });

  await page.setViewportSize({ width: 1400, height: 950 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/lineup');
  await expect(page.getByRole('button', { name: '开始录入' })).toBeVisible();
  await page.getByRole('button', { name: '保存', exact: true }).click();
  await expect.poll(() => snapshots.length).toBeGreaterThan(0);
  return JSON.parse(snapshots.at(-1));
}

test('无 DH：保存阵容时自动把未先发球员放入替补席', async ({ page }) => {
  const payload = await saveLineupAndReadSnapshot(page, buildFixture(false));
  expect(payload.gameStarted).toBe(false);
  expect((payload.homeBench || []).map((p) => Number(p.id)).sort((a, b) => a - b)).toEqual([110, 111]);
  expect((payload.awayBench || []).map((p) => Number(p.id)).sort((a, b) => a - b)).toEqual([210]);
});

test('DH 制：仅守备投手不重复进替补席', async ({ page }) => {
  const payload = await saveLineupAndReadSnapshot(page, buildFixture(true));
  expect((payload.homeBench || []).map((p) => Number(p.id))).toEqual([111]);
  expect(payload.homeFieldingPitcherId).toBe(110);
});
