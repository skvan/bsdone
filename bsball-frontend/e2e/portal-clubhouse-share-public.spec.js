// 个人主场分享：公开只读页锁定范围、无编辑入口；/me 分享入口与公开开关。
// 运行：npx playwright test e2e/portal-clubhouse-share-public.spec.js
import { test, expect } from '@playwright/test';

const TENANT = 'bs-ball';
const VIEWPORT = { width: 1440, height: 1000 };

const teams = [
  {
    id: 1,
    name: '广州猎豹',
    shortName: '猎豹',
    logo: '',
    events: [{ id: 11, name: '春季联赛', gameMode: 'BASEBALL', status: 'ongoing' }],
    primaryEventId: 11,
    primaryEventName: '春季联赛',
    standing: {}
  }
];

const moduleCatalog = [
  { key: 'performance', label: '数据表现', defaultWidth: 800, defaultHeight: 500, minWidth: 360, minHeight: 320, maxWidth: 1216, maxHeight: 1600 },
  { key: 'teams', label: '代表球队', defaultWidth: 400, defaultHeight: 300, minWidth: 280, minHeight: 220, maxWidth: 1216, maxHeight: 1600 },
  { key: 'quickLinks', label: '快捷入口', defaultWidth: 400, defaultHeight: 220, minWidth: 240, minHeight: 180, maxWidth: 1216, maxHeight: 1600 },
  { key: 'schedule', label: '接下来的比赛', defaultWidth: 800, defaultHeight: 560, minWidth: 420, minHeight: 300, maxWidth: 1216, maxHeight: 1600 }
];

function baseDashboard(overrides = {}) {
  return {
    player: {
      id: 10,
      name: '公开球员',
      nameEn: 'PUBLIC PLAYER',
      number: '27',
      positions: ['CF'],
      throwHand: '右投',
      batHand: '右打',
      intro: '个人主场分享冒烟',
      height: 178,
      weight: 75
    },
    teams,
    scope: {
      teamId: 1,
      teamName: '广州猎豹',
      leagueId: 11,
      leagueName: '春季联赛',
      eventId: 11,
      eventName: '春季联赛',
      gameMode: 'BASEBALL',
      teamAll: false,
      leagueAll: false,
      career: false
    },
    events: [{ id: 11, name: '春季联赛', gameMode: 'BASEBALL', status: 'ongoing', leagueId: 11 }],
    gameModes: ['BASEBALL'],
    config: {
      version: 4,
      layout: 'v4-board',
      modules: [
        { key: 'performance', enabled: true, x: 0, y: 0, width: 800, height: 500 },
        { key: 'teams', enabled: true, x: 816, y: 0, width: 400, height: 300 },
        { key: 'quickLinks', enabled: false, x: 816, y: 316, width: 400, height: 220 }
      ],
      milestones: { metricKeys: [] },
      performance: { metricKey: 'batting.avg', viz: 'kpi' },
      ranks: []
    },
    stats: {
      batting: { gp: 20, pa: 88, ab: 70, h: 22, avg: '.314', obp: '.402', slg: '.457', ops: '.859', rbi: 18, sb: 6 },
      pitching: { ip: 0 },
      fielding: { gp: 20, inn: 42.3, tc: 55, po: 30, a: 22, e: 3 }
    },
    catalog: [],
    moduleCatalog,
    milestoneCatalog: [],
    performance: { available: true, viz: 'kpi', summaryValue: '0.314', label: 'AVG', items: [] },
    ranks: [],
    evaluation: { available: false, cards: [], overall: {} },
    nextGame: null,
    schedule: [],
    recentGames: [],
    ...overrides
  };
}

function ownerDashboard() {
  return baseDashboard({
    config: {
      version: 4,
      layout: 'v4-board',
      modules: [
        { key: 'performance', enabled: true, x: 0, y: 0, width: 800, height: 500 },
        { key: 'teams', enabled: true, x: 816, y: 0, width: 400, height: 300 },
        { key: 'quickLinks', enabled: true, x: 816, y: 316, width: 400, height: 220 }
      ],
      milestones: { metricKeys: [] },
      performance: { metricKey: 'batting.avg', viz: 'kpi' },
      ranks: [],
      share: { enabled: false }
    }
  });
}

async function mockShareApi(page, options = {}) {
  const state = { sharePayload: null, publicRequests: [] };
  await page.addInitScript(() => {
    localStorage.setItem('bs-ball-locale', 'zh-CN');
  });
  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '').replace(/^\/api/, '');
    const method = route.request().method();
    const json = (body) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
    state.publicRequests.push(`${method} ${path}${url.search}`);

    if (path === '/player/10/clubhouse') {
      if (options.publicUnavailable) {
        await json({ code: 404, msg: '该主页未公开' });
        return;
      }
      await json({ code: 200, data: baseDashboard() });
      return;
    }
    if (path === '/player/10/clubhouse/milestones') {
      await json({ code: 200, data: {} });
      return;
    }
    if (path === '/player/10/clubhouse/sprays') {
      await json({ code: 200, data: {} });
      return;
    }
    if (path === '/account/clubhouse' && method === 'GET') {
      const data = ownerDashboard();
      if (options.lineChart) {
        data.performance = {
          available: true,
          viz: 'line',
          metricKey: 'batting.avg',
          label: 'AVG',
          summaryValue: '0.314',
          points: [
            { label: '5/01', value: '0.300', gameday: '2026-05-01' },
            { label: '5/08', value: '0.312', gameday: '2026-05-08' },
            { label: '5/15', value: '0.308', gameday: '2026-05-15' },
            { label: '5/22', value: '0.320', gameday: '2026-05-22' }
          ]
        };
      }
      await json({ code: 200, data });
      return;
    }
    if (path === '/account/clubhouse/share' && method === 'PUT') {
      state.sharePayload = route.request().postDataJSON();
      const config = ownerDashboard().config;
      config.share = { enabled: Boolean(state.sharePayload?.enabled) };
      await json({ code: 200, data: config });
      return;
    }
    if (path === '/portal/visit/record') {
      await json({ code: 200, data: {} });
      return;
    }
    await json({ code: 200, data: {} });
  });
  return state;
}

test('公开页：免登录只读、范围锁定、无编辑/设置/快捷入口', async ({ page }) => {
  await page.setViewportSize(VIEWPORT);
  const state = await mockShareApi(page);

  await page.goto(`/bs-ball/${TENANT}/players/10/clubhouse?from=share&teamId=1&leagueId=11&eventId=11&gameMode=BASEBALL`);

  await expect(page.locator('.clubhouse-page')).toBeVisible();
  await expect(page.locator('.page-actions')).toContainText('分享');
  await expect(page.locator('.page-actions')).toContainText('球员主页');
  await expect(page.locator('.page-actions')).toContainText('返回门户');
  await expect(page.locator('.page-actions')).not.toContainText('设置');
  await expect(page.locator('.page-actions')).not.toContainText('账户');
  await expect(page.locator('.page-actions')).not.toContainText('编辑布局');
  await expect(page.locator('.scope-select')).toHaveCount(0);
  await expect(page.locator('.scope-static').first()).toBeVisible();
  await expect(page.locator('.public-badge')).toContainText('公开只读');
  await expect(page.locator('[data-module-key="quickLinks"]')).toHaveCount(0);
  await expect(page).toHaveURL(/\/players\/10\/clubhouse/);
  expect(state.publicRequests.some((request) => request.startsWith('GET /player/10/clubhouse'))).toBe(true);
});

test('公开页：未公开时展示空态，不跳登录', async ({ page }) => {
  await page.setViewportSize(VIEWPORT);
  await mockShareApi(page, { publicUnavailable: true });

  await page.goto(`/bs-ball/${TENANT}/players/10/clubhouse?from=share`);

  await expect(page.getByText('该主页未公开或已失效')).toBeVisible();
  await expect(page).not.toHaveURL(/account\/login/);
});

test('/me：保存长图生成预览弹窗', async ({ page }) => {
  await page.setViewportSize(VIEWPORT);
  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: false, username: 'local-e2e'
    }));
  });
  await mockShareApi(page, { lineChart: true });

  await page.goto(`/bs-ball/${TENANT}/me`);
  await page.locator('.clubhouse-share-entry').click();
  await expect(page.locator('.csp-image-action')).toBeVisible();

  await page.locator('.csp-image-action').click();
  await expect(page.locator('.cip-dialog')).toBeVisible({ timeout: 30_000 });
  const image = page.locator('.cip-frame img');
  await expect(image).toHaveAttribute('src', /^(blob:|data:image\/)/);
  await expect.poll(() => image.evaluate((img) => img.naturalWidth)).toBeGreaterThan(0);
  await expect.poll(() => image.evaluate((img) => img.naturalHeight)).toBeGreaterThan(0);
  // 生成结束后 capture 状态已恢复，页面控件不再被隐藏
  await expect(page.locator('.clubhouse-page.is-capturing')).toHaveCount(0);
  await expect(page.locator('.page-actions')).toBeVisible();
});

test('/me：顶部分享入口打开面板，公开开关调用保存接口', async ({ page }) => {
  await page.setViewportSize(VIEWPORT);
  await page.addInitScript(() => {
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: false, username: 'local-e2e'
    }));
  });
  const state = await mockShareApi(page);

  await page.goto(`/bs-ball/${TENANT}/me`);
  await expect(page.locator('.page-actions')).toContainText('分享');
  await expect(page.locator('.page-actions')).toContainText('设置');

  await page.locator('.clubhouse-share-entry').click();
  await expect(page.locator('.clubhouse-share-drawer')).toBeVisible();
  await expect(page.locator('.csp-switch-row')).toContainText('未公开');

  await page.locator('.csp-switch-row .el-switch').click();
  await expect(page.locator('.el-message-box')).toBeVisible();
  await page.locator('.el-message-box__btns .el-button--primary').click();

  await expect.poll(() => state.sharePayload).not.toBeNull();
  expect(state.sharePayload).toEqual({ enabled: true });
  await expect(page.locator('.csp-switch-row')).toContainText('公开中');
});
