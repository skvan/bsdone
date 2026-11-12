import { test, expect } from '@playwright/test';

const GAME_ID = 10;
const HOME_ID = 1;
const AWAY_ID = 2;

const game = {
  id: GAME_ID,
  eventId: 1,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  homeScore: 8,
  awayScore: 10,
  homeH: 8,
  awayH: 7,
  homeE: 7,
  awayE: 1,
  homeScoreByInning: ['3', '1', '4', '0'],
  awayScoreByInning: ['3', '2', '1', '4'],
  totalInnings: 4,
  gameNumber: 4,
  gameday: '2026-11-01',
  gameTime: '2026-11-01 15:29:00',
  venue: '中山熊猫纪念球场'
};

const homePlayers = [{ id: 101, teamId: HOME_ID, name: '林一', shortName: '林一', number: '1' }];
const awayPlayers = [{ id: 201, teamId: AWAY_ID, name: '客一', shortName: '客一', number: '1' }];

const homeStats = [{
  teamId: HOME_ID, playerId: 101, position: 'CF', firstPosition: 'CF', startingPosition: 'CF',
  battingOrder: 1, listOrder: 0, fieldingGs: 1, po: 2, a: 0, e: 0, dp: 0, defInn: 9
}];
const awayStats = [{
  teamId: AWAY_ID, playerId: 201, position: 'P', firstPosition: 'P', startingPosition: 'P',
  battingOrder: 1, listOrder: 0, fieldingGs: 1, isPitcher: 1, po: 0, a: 1, e: 0, dp: 0, defInn: 9
}];

async function mockApis(page) {
  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
    localStorage.setItem('bsball.portal.fieldSettingsGuide.v1', JSON.stringify({
      v: 1, byTenant: { 'bs-ball': true }, legacyPortalMigrated: true
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: '2026花城联盟冬季赛' };
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) data = { list: [...awayStats, ...homeStats], total: 2 };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      const list = teamId === HOME_ID ? homePlayers : teamId === AWAY_ID ? awayPlayers : [...homePlayers, ...awayPlayers];
      data = { list, total: list.length };
    } else if (path === `/team/${HOME_ID}`) data = { id: HOME_ID, name: '广州不死鸟', shortName: '不死鸟' };
    else if (path === `/team/${AWAY_ID}`) data = { id: AWAY_ID, name: '暴力羊', shortName: '暴力羊' };
    else if (path === `/hit-spray/game/${GAME_ID}`) data = [];
    else if (path === '/portal/settings') data = {};

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
}

test('门户比赛详情：悬浮分享入口位于齿轮上方，分享面板可复制链接/生成海报', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockApis(page);
  await page.goto(`/bs-ball/bs-ball/events/1/games/${GAME_ID}`);

  const fab = page.locator('.game-share-fab');
  await expect(fab).toBeVisible();
  const gear = page.locator('.floating-settings-wrap .floating-settings-btn');
  await expect(gear).toBeVisible();

  const [fabBox, gearBox] = await Promise.all([fab.boundingBox(), gear.boundingBox()]);
  expect(fabBox.y + fabBox.height).toBeLessThanOrEqual(gearBox.y + 1);
  expect(Math.abs(fabBox.x - gearBox.x)).toBeLessThanOrEqual(1);

  await fab.click();
  await expect(page.locator('.gs-sheet')).toBeVisible();
  await expect(page.getByRole('button', { name: '复制链接' })).toBeVisible();
  await expect(page.getByRole('button', { name: '生成海报' })).toBeVisible();
  await expect(page.getByRole('button', { name: '微信好友' })).toHaveCount(0);
  await expect(page.locator('.gs-qr')).toBeVisible();
  await expect(page.locator('.gs-match')).toContainText('暴力羊 10 : 8 广州不死鸟');

  await page.getByRole('button', { name: '生成海报' }).click();
  const poster = page.locator('.gs-poster-img');
  await expect(poster).toBeVisible({ timeout: 30000 });
  const naturalWidth = await poster.evaluate((img) => img.naturalWidth);
  expect(naturalWidth).toBeGreaterThan(300);
});

test('微信内分享显示右上角引导层', async ({ browser }) => {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 MicroMessenger/8.0.50'
  });
  const page = await context.newPage();
  await mockApis(page);
  await page.goto(`/bs-ball/bs-ball/events/1/games/${GAME_ID}`);
  await page.locator('.game-share-fab').click();
  await page.getByRole('button', { name: '微信好友' }).click();
  await expect(page.locator('.gs-guide')).toBeVisible();
  await expect(page.locator('.gs-guide')).toContainText('点右上角');
  await context.close();
});
