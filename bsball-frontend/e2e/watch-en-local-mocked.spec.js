import { test, expect } from '@playwright/test';

test('English watch page shell: data source and empty snapshot copy', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
    localStorage.setItem('bs-ball-locale', 'en');
  });

  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Watch E2E Event' };
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0
    };
    else if (/^\/team\/[12]$/.test(path)) {
      const id = Number(path.split('/').at(-1));
      data = { id, name: id === 1 ? 'Home' : 'Away' };
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/watch?source=server');
  await expect(page.getByText('Data: server snapshot')).toBeVisible();
  await expect(page.getByText(/No server snapshot yet/)).toBeVisible();
  const text = await page.locator('body').innerText();
  for (const zh of ['数据源：服务器快照', '暂无服务端快照', '实时动态', '日志', '返回赛程']) {
    expect(text, `English watch page still shows ${zh}`).not.toContain(zh);
  }
});
