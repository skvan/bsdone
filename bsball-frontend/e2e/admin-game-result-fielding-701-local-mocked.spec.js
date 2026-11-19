import { test, expect } from '@playwright/test';

// Issue #701 复核 + 逐位置编辑（2026-11-19 设计）：有逐位置明细的球员，合计改为
// 逐位置派生、球员级只读；球员级全零的旧数据仍由逐位置合计补全（#560）。
// 运行：E2E_PORT=3030 npx playwright test e2e/admin-game-result-fielding-701-local-mocked.spec.js
const GAME_ID = 701;
const HOME_ID = 2;
const AWAY_ID = 1;

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  homeScore: 1,
  awayScore: 0,
  gameday: '2026-11-18',
  totalInnings: 4,
  venue: '测试球场'
};

const awayPlayers = [
  { id: 14, teamId: AWAY_ID, name: '梁大鹏', number: '7', positions: ['SS'] },
  { id: 26, teamId: AWAY_ID, name: '邱俊锋', number: '18', positions: ['P'] },
  { id: 44, teamId: AWAY_ID, name: '姚智鹏', number: '26', positions: ['P'] }
];
const homePlayers = [
  { id: 101, teamId: HOME_ID, name: '主队投手', number: '1', positions: ['P'] }
];

// 球员级统计（人工修订后的最终值）：梁大鹏 PO=1/E=1、邱俊锋 PO=1/E=2；
// 姚智鹏是最早的旧数据：球员级守备全零，只有逐位置表有 A=2。
const awayStats = [
  {
    id: 8101, teamId: AWAY_ID, playerId: 14, isPitcher: 0,
    position: 'SS', firstPosition: 'SS',
    battingOrder: 1, listOrder: 0, po: 1, a: 0, e: 1, dp: 0, defInn: 4
  },
  {
    id: 8102, teamId: AWAY_ID, playerId: 26, isPitcher: 1,
    position: 'P', firstPosition: 'P', pitcherOrder: 1,
    battingOrder: null, listOrder: 5, po: 1, a: 0, e: 2, dp: 0, defInn: 4, ip: 4
  },
  {
    id: 8103, teamId: AWAY_ID, playerId: 44, isPitcher: 1,
    position: 'P', firstPosition: 'P', pitcherOrder: 2,
    battingOrder: null, listOrder: 6, po: 0, a: 0, e: 0, dp: 0, defInn: 0, ip: 0
  }
];
const homeStats = [
  {
    id: 8201, teamId: HOME_ID, playerId: 101, isPitcher: 0,
    position: 'P', firstPosition: 'P',
    battingOrder: 1, listOrder: 0, po: 0, a: 0, e: 0, dp: 0, defInn: 0
  }
];

// 逐位置合计是旧值：与球员级人工值不一致
const fieldingPositions = [
  { teamId: AWAY_ID, playerId: 14, position: 'SS', outs: 12, po: 0, a: 0, e: 2, dp: 0, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 26, position: 'P', outs: 12, po: 0, a: 0, e: 1, dp: 0, fieldingGs: true },
  { teamId: AWAY_ID, playerId: 44, position: 'P', outs: 12, po: 0, a: 2, e: 0, dp: 0, fieldingGs: true },
  { teamId: HOME_ID, playerId: 101, position: 'P', outs: 12, po: 0, a: 0, e: 0, dp: 0, fieldingGs: true }
];

async function mockApis(page) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];

    if (path === '/event/1') data = { id: 1, name: 'Issue 701' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) {
      data = { list: [...awayStats, ...homeStats], total: awayStats.length + homeStats.length };
    } else if (path === `/game/${GAME_ID}/fielding-positions`) {
      data = { list: fieldingPositions, total: fieldingPositions.length };
    } else if (path === '/team/select-options' || path === '/event/1/game-team-options') {
      data = [
        { id: AWAY_ID, name: '客队A', teamId: AWAY_ID },
        { id: HOME_ID, name: '主队B', teamId: HOME_ID }
      ];
    } else if (path === '/game/list') {
      data = { list: [game], total: 1 };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = teamId === AWAY_ID ? awayPlayers : teamId === HOME_ID ? homePlayers : [...awayPlayers, ...homePlayers];
      data = { list, total: list.length };
    } else if (/^\/team\/1$/.test(path) || /^\/team\/2$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === AWAY_ID ? '客队A' : '主队B', shortName: id === AWAY_ID ? '客队A' : '主队B' };
    } else if (path === `/game/${GAME_ID}/earned-runs/decisions`) {
      data = [];
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

async function openFieldingTab(page) {
  await page.goto(`/bs-ball/bs-ball/admin/events/1/games/${GAME_ID}/edit`);
  const tab = page.getByRole('tab', { name: '守备数据' });
  await expect(tab).toBeVisible();
  await tab.click();
  return page.locator('.game-fielding-table', { hasText: '客队A 守备' });
}

test('结果录入守备表：有逐位置明细时合计按逐位置派生', async ({ page }) => {
  await mockApis(page);
  const awayTable = await openFieldingTab(page);
  await expect(awayTable).toBeVisible();

  const ssRow = awayTable.locator('tbody tr', { hasText: '梁大鹏' }).first();
  await expect(ssRow).toBeVisible();
  // GS PO A E DP INN：逐位置 SS 为 PO=0 / E=2，球员级只读显示派生值
  await expect(ssRow.locator('td').nth(4)).toHaveText('0');
  await expect(ssRow.locator('td').nth(6)).toHaveText('2');

  const pitcherRow = awayTable.locator('tbody tr', { hasText: '邱俊锋' }).first();
  await expect(pitcherRow).toBeVisible();
  await expect(pitcherRow.locator('td').nth(4)).toHaveText('0');
  await expect(pitcherRow.locator('td').nth(6)).toHaveText('1');
});

test('结果录入守备表：旧数据球员级全零时仍用逐位置合计补全（#560）', async ({ page }) => {
  await mockApis(page);
  const awayTable = await openFieldingTab(page);
  await expect(awayTable).toBeVisible();

  const legacyPitcherRow = awayTable.locator('tbody tr', { hasText: '姚智鹏' }).first();
  await expect(legacyPitcherRow).toBeVisible();
  await expect(legacyPitcherRow.locator('td').nth(5)).toHaveText('2');
});
