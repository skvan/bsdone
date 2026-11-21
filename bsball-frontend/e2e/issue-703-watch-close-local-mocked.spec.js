import { test, expect } from '@playwright/test';

// #703 回归：直播快照里含结束行字符串日志时，观赛页「实时动态」不得因数字子节点崩溃，
// 且「返回赛程」后观赛页必须真正卸载（修复前 Vue patch 抛 Invalid array length，DOM 卡住）。
function gameRow(overrides) {
  return {
    id: 101,
    eventId: 1,
    homeTeamId: 1,
    awayTeamId: 2,
    homeTeamName: 'Home',
    awayTeamName: 'Away',
    gameday: '2026-11-04',
    gameTime: '2026-11-04 12:00:00',
    gameNumber: 1,
    venue: 'E2E Park',
    status: 'final',
    gameMode: 'BASEBALL',
    totalInnings: 7,
    homeScore: 0,
    awayScore: 0,
    hasLiveSnapshot: true,
    createdAt: '2026-11-04T00:00:00',
    updatedAt: '2026-11-04T00:00:00',
    ...overrides
  };
}

const snapshot = {
  v: 1,
  gameMode: 'BASEBALL',
  setupForm: { homeTeamId: 1, awayTeamId: 2, venue: '' },
  gameStarted: true,
  homeLineup: [],
  awayLineup: [],
  homeBench: [],
  awayBench: [],
  awayFieldingPitcherId: null,
  homeFieldingPitcherId: null,
  awayFieldingPitcherRow: null,
  homeFieldingPitcherRow: null,
  awayLastDhAddedFromPoolId: null,
  homeLastDhAddedFromPoolId: null,
  awayUnavailablePlayerIds: [],
  homeUnavailablePlayerIds: [],
  awayLineupStarterIds: [],
  homeLineupStarterIds: [],
  awaySlotSubHistory: {},
  homeSlotSubHistory: {},
  teamsData: {
    away: { score: 0, innings: [0, 0, 0, 0] },
    home: { score: 0, innings: [0, 0, 0, 0] }
  },
  eventLog: ['[22:14:26] [4上] 比赛已结束'],
  atBatSummaries: [],
  playerStatsById: {},
  undoHistory: [],
  redoHistory: [],
  gameState: {
    inning: 4,
    isTop: true,
    outs: 0,
    halfInningOuts: [],
    balls: 0,
    strikes: 0,
    batterIndex: { away: 0, home: 0 },
    runners: { 1: null, 2: null, 3: null },
    flow: null,
    isGameEnd: true,
    hasPitchThisAB: false,
    needNextBatter: false,
    mvp: null,
    svp: null,
    runnerDragCiRunnerId: null
  }
};

async function setup(page) {
  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') {
      data = { id: 1, name: 'Issue703 E2E', gameMode: 'BASEBALL', specialRules: {} };
    } else if (path === '/game/list') {
      data = { list: [gameRow({})], total: 1 };
    } else if (path === '/game/101') {
      data = gameRow({});
    } else if (path === '/game/101/live-snapshot') {
      data = { snapshotJson: JSON.stringify(snapshot) };
    } else if (path === '/team/1' || path === '/team/2') {
      data = { id: Number(path.slice('/team/'.length)), name: path.endsWith('1') ? 'Home' : 'Away' };
    } else if (path === '/team/select-options') {
      data = [{ id: 1, name: 'Home' }, { id: 2, name: 'Away' }];
    } else if (path === '/stadium/list') {
      data = { list: [], total: 0 };
    } else if (path === '/player/list') {
      data = { list: [], total: 0 };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.setViewportSize({ width: 1600, height: 1000 });
}

test('结束行日志下观赛页可正常刷新并可返回赛程', async ({ page }) => {
  const pageErrors = [];
  page.on('pageerror', (e) => pageErrors.push(e.message));

  await setup(page);
  await page.goto('/bs-ball/bs-ball/admin/events/1/games');
  await page.locator('.el-table__row', { hasText: '101' }).first()
    .getByText('查看比赛实况记录', { exact: true }).click();
  await page.waitForURL(/\/watch(\?|$)/, { timeout: 15000 });
  await expect(page.locator('.live-game-watch-page')).toBeVisible();
  await expect(page.locator('.pbp-atbat-order').first()).toHaveText('第1棒');

  // 等一轮快照轮询 + 切换日志/实时动态，触发 Vue patch
  await page.waitForTimeout(6000);
  await page.getByText('日志', { exact: true }).click();
  await page.getByText('实时动态', { exact: true }).click();
  await page.waitForTimeout(1000);

  expect(pageErrors, pageErrors.join('\n')).toEqual([]);

  await page.getByText('返回赛程', { exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/events\/1\/games$/, { timeout: 10000 });
  await expect(page.locator('.live-game-watch-page')).toHaveCount(0);
});
