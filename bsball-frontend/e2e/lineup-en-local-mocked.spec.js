import { test, expect } from '@playwright/test';

test('Lineup page English mode: header, form, field slots and pool', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
    localStorage.setItem('bs-ball-locale', 'en');
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event', specialRules: { ohtaniRule: true } };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }
    ];
    else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      data = { list: [{ id: teamId * 100 + 1, name: `T${teamId}P1`, number: '1', teamId, position: 'P', positions: ['P'] }], total: 1 };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/live');
  await expect(page.getByText('Confirm Starting Lineup · Local E2E Event')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Apply Lineup Template' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save & Exit' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Start Scoring' })).toBeVisible();
  await expect(page.getByText('Game Mode')).toBeVisible();
  await expect(page.getByText('Away Team', { exact: true })).toBeVisible();
  await expect(page.getByText('Home Team', { exact: true })).toBeVisible();
  await expect(page.getByText('Batting Order')).toBeVisible();
  await expect(page.getByText('Available Players')).toBeVisible();
  await expect(page.getByText('Bench', { exact: true })).toBeVisible();
  await expect(page.getByText('Catcher')).toBeVisible();
  await expect(page.getByText('Shortstop')).toBeVisible();
  const text = await page.locator('.live-game-lineup-board').innerText();
  for (const zh of ['确认先发阵容', '比赛模式', '客队', '主队', '打线顺序', '可用球员', '替补名单', '保存并退出', '开始录入']) {
    expect(text, `English lineup UI still shows ${zh}`).not.toContain(zh);
  }
  await page.getByRole('button', { name: 'Add Multiple' }).click();
  await expect(page.getByRole('dialog')).toContainText('Add Bench Players');
});
