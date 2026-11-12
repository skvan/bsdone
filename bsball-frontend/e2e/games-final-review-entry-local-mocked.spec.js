import { test, expect } from '@playwright/test';

// 已结束比赛在列表中的入口收敛：
// - final（无论有无快照）-> 显示“数据补录/复核”，不再显示“赛后复核”
// - live -> 仍显示“继续录入”
// - 保留 final 的“查看比赛实况记录”
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
    totalInnings: 9,
    homeScore: 3,
    awayScore: 2,
    hasLiveSnapshot: false,
    createdAt: '2026-11-04T00:00:00',
    updatedAt: '2026-11-04T00:00:00',
    ...overrides
  };
}

async function setup(page) {
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
    if (path === '/event/1') {
      data = { id: 1, name: 'Final Review Entry E2E', gameMode: 'BASEBALL', specialRules: {} };
    } else if (path === '/team/select-options') {
      data = [
        { id: 1, name: 'Home', tenantId: 1 },
        { id: 2, name: 'Away', tenantId: 1 },
      ];
    } else if (path === '/game/list') {
      data = {
        list: [
          gameRow({ id: 101, hasLiveSnapshot: true, gameNumber: 1 }),
          gameRow({ id: 102, hasLiveSnapshot: false, gameNumber: 2 }),
          gameRow({ id: 103, status: 'live', hasLiveSnapshot: true, gameNumber: 3 }),
        ],
        total: 3,
      };
    } else if (path === '/event/1/game-team-options') {
      data = [];
    } else if (path === '/stadium/list') {
      data = { list: [], total: 0 };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data }),
    });
  });

  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games');
}

test('final 统一显示数据补录/复核，live 仍显示继续录入', async ({ page }) => {
  await setup(page);

  await expect(page.getByText('Final Review Entry E2E').first()).toBeVisible();
  const finalWithSnapshot = page.locator('.el-table__row').filter({ hasText: '101' });
  const finalWithoutSnapshot = page.locator('.el-table__row').filter({ hasText: '102' });
  const liveRow = page.locator('.el-table__row').filter({ hasText: '103' });

  await expect(finalWithSnapshot.getByRole('button', { name: '数据补录/复核' })).toBeVisible();
  await expect(finalWithSnapshot.getByRole('button', { name: '查看比赛实况记录' })).toBeVisible();
  await expect(finalWithoutSnapshot.getByRole('button', { name: '数据补录/复核' })).toBeVisible();
  await expect(page.getByRole('button', { name: '赛后复核' })).toHaveCount(0);
  await expect(liveRow.getByRole('button', { name: '继续录入' })).toBeVisible();
});
