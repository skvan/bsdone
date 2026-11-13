import { test, expect } from '@playwright/test';

const event = {
  id: 1,
  name: '2026 花城联盟春季赛',
  leagueId: 1,
  season: '2026春',
  startDate: '2026-03-07',
  endDate: '2026-06-14',
  status: 'ongoing',
  gameMode: 'BASEBALL',
  formatType: 'GROUP_KNOCKOUT',
  pointsWin: 2,
  pointsDraw: 1,
  pointsLoss: 0,
  groupCount: 2,
  advancePerGroup: 2,
  thirdPlace: true,
  bracketSize: 8
};

async function mockApis(page) {
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
    if (path === '/event/list') data = { list: [event], total: 1 };
    else if (path === '/event/1') data = event;
    else if (path === '/league/list') data = { list: [{ id: 1, name: '花城联盟' }], total: 1 };
    else if (path === '/portal/settings') data = {};
    else if (path === '/league/1/special-rules') data = { rules: { specialRulesEnabled: false } };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('后台发布赛事：赛制、积分制、小组与淘汰赛选项', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await mockApis(page);
  await page.goto('/bs-ball/bs-ball/admin/events');
  await expect(page.getByRole('button', { name: /发布赛事/ })).toBeVisible();
  await page.getByRole('button', { name: /发布赛事/ }).click();
  const dialog = page.locator('.el-dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('赛制设置');
  await expect(dialog).toContainText('积分制');
  await expect(dialog).not.toContainText('小组与晋级');

  await dialog.locator('.format-grid .el-radio').filter({ hasText: '小组单循环' }).click();
  await expect(dialog).toContainText('小组与晋级');
  await expect(dialog.getByText('签表规模')).toHaveCount(0);

  await dialog.locator('.format-grid .el-radio').filter({ hasText: '输一场即淘汰' }).click();
  await expect(dialog).toContainText('签表规模');
  await expect(dialog).not.toContainText('小组与晋级');

  await dialog.locator('.format-grid .el-radio').filter({ hasText: '每两队交手 1 次' }).click();
  await expect(dialog).toContainText('积分制');
  await expect(dialog).not.toContainText('小组与晋级');

  await dialog.locator('.points-grid .el-radio').filter({ hasText: '3-1-0' }).click();
  await dialog.locator('.format-grid .el-radio').filter({ hasText: '小组单循环' }).click();
  await expect(dialog).toContainText('小组与晋级');
});
