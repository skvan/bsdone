// 赛前阵容海报：球队端完整阵容保存态 → 生成 3:4 海报（本地 mock，无后端）
// 运行：E2E_PORT=3103 npx playwright test e2e/lineup-poster-local-mocked.spec.js
import { test, expect } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const asset = (rel) => path.join(REPO_ROOT, 'bsball_project/webapps/bs-ball', rel);

const players = [
  { id: 101, name: '陈晨威', number: '98', teamId: 1, positions: ['CF'] },
  { id: 102, name: '林立', number: '39', teamId: 1, positions: ['2B'] },
  { id: 103, name: '梁家荣', number: '79', teamId: 1, positions: ['1B'] },
  { id: 104, name: '林泓育', number: '11', teamId: 1, positions: ['DH'] },
  { id: 105, name: '朱育贤', number: '85', teamId: 1, positions: ['LF'] },
  { id: 106, name: '廖健富', number: '58', teamId: 1, positions: ['C'] },
  { id: 107, name: '郭严文', number: '21', teamId: 1, positions: ['3B'] },
  { id: 108, name: '成晋', number: '35', teamId: 1, positions: ['RF'] },
  { id: 109, name: '马杰森', number: '93', teamId: 1, positions: ['SS'] },
  { id: 110, name: '黄子鹏', number: '18', teamId: 1, positions: ['P'] }
];

const lineup = [
  { id: 101, name: '陈晨威', number: '98', teamId: 1, position: 'CF' },
  { id: 102, name: '林立', number: '39', teamId: 1, position: '2B' },
  { id: 103, name: '梁家荣', number: '79', teamId: 1, position: '1B' },
  { id: 104, name: '林泓育', number: '11', teamId: 1, position: 'DH' },
  { id: 105, name: '朱育贤', number: '85', teamId: 1, position: 'LF' },
  { id: 106, name: '廖健富', number: '58', teamId: 1, position: 'C' },
  { id: 107, name: '郭严文', number: '21', teamId: 1, position: '3B' },
  { id: 108, name: '成晋', number: '35', teamId: 1, position: 'RF' },
  { id: 109, name: '马杰森', number: '93', teamId: 1, position: 'SS' }
];

test.use({ viewport: { width: 1400, height: 950 } });

test('完整阵容保存后生成海报：3:4、打线 + 先发投手、二维码与品牌', async ({ page }) => {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });

  await page.route('**/bs-ball/bs-ball-logo.png', (route) =>
    route.fulfill({ path: asset('bs-ball-logo.png'), contentType: 'image/png' }));

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const pathName = new URL(request.url()).pathname.replace('/bsball-server', '');
    if (pathName.startsWith('/team-logos/')) {
      await route.fulfill({ path: asset(pathName.slice(1)), contentType: 'image/png' });
      return;
    }
    let data = [];
    if (pathName === '/team/1/lineup-games/10' && request.method() === 'GET') {
      data = {
        gameId: 10,
        eventId: 1,
        eventName: '2026 春季联赛',
        season: '2026',
        gameMode: 'BASEBALL',
        specialRules: {},
        teamId: 1,
        teamName: '乐天桃猿',
        teamLogo: '/team-logos/monkeys-large.png',
        side: 'home',
        opponentTeamId: 2,
        opponentName: '中信兄弟',
        gameNumber: 3,
        gameday: '2026-11-15',
        gameTime: '2026-11-15 14:00:00',
        venue: '天母棒球场',
        status: 'scheduled',
        rosterRestricted: false,
        rosterCount: players.length,
        lineupJson: JSON.stringify({
          v: 1,
          gameId: 10,
          teamId: 1,
          gameMode: 'BASEBALL',
          complete: true,
          lineup,
          bench: [{ id: 110, name: '黄子鹏', number: '18', teamId: 1 }],
          fieldingPitcherId: 110,
          lastDhAddedFromPoolId: 104
        }),
        lineupUpdatedAt: '2026-11-11T18:20:00',
        complete: true
      };
    } else if (pathName === '/event/1/teams/1/roster-players') {
      data = { restricted: false, players };
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/teams/1/lineup-games/1/10');
  const logo = page.locator('.lineup-poster__logo-card img');
  await expect(logo).toHaveCount(1);
  await expect.poll(() => logo.evaluate((img) => img.naturalWidth)).toBeGreaterThan(0);
  await expect(page.getByRole('button', { name: '生成海报' })).toBeEnabled();
  await page.getByRole('button', { name: '生成海报' }).click();

  const preview = page.locator('.poster-preview');
  await expect(preview).toBeVisible({ timeout: 30_000 });
  const size = await preview.evaluate((img) => ({ w: img.naturalWidth, h: img.naturalHeight }));
  expect(size.w).toBeGreaterThan(0);
  expect(Math.abs(size.w / size.h - 3 / 4)).toBeLessThan(0.01);

  await expect(page.locator('.lineup-poster__pitcher')).toContainText('黄子鹏');
  await expect.poll(() => page.locator('.lineup-poster__qr img').evaluate((img) => img.naturalWidth))
    .toBeGreaterThan(0);

  await preview.screenshot({
    path: test.info().outputPath('lineup-poster.jpg'),
    type: 'jpeg',
    quality: 82
  });
});
