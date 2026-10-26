// 个人驾驶舱自由画布：绝对定位、八手柄、拖动防重叠、保存 v4、紧凑/手机回退。
// 运行：npx playwright test e2e/portal-clubhouse-free-layout.spec.js
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
  },
  {
    id: 2,
    name: '海珠蓝鸟',
    shortName: '蓝鸟',
    logo: '',
    events: [{ id: 12, name: '城市杯', gameMode: 'BASEBALL', status: 'ongoing' }],
    primaryEventId: 12,
    primaryEventName: '城市杯',
    standing: {}
  }
];

const moduleCatalog = [
  {
    key: 'performance',
    label: '数据表现',
    defaultWidth: 800,
    defaultHeight: 500,
    minWidth: 360,
    minHeight: 320,
    maxWidth: 1216,
    maxHeight: 1600
  },
  {
    key: 'teams',
    label: '代表球队',
    defaultWidth: 400,
    defaultHeight: 300,
    minWidth: 280,
    minHeight: 220,
    maxWidth: 1216,
    maxHeight: 1600
  },
  {
    key: 'schedule',
    label: '接下来的比赛',
    defaultWidth: 800,
    defaultHeight: 560,
    minWidth: 420,
    minHeight: 300,
    maxWidth: 1216,
    maxHeight: 1600
  }
];

function dashboard() {
  return {
    player: {
      id: 10,
      name: '陈一',
      nameEn: 'CHEN YI',
      number: '27',
      positions: ['CF'],
      throwHand: '右投',
      batHand: '右打',
      intro: '本地冒烟球员',
      height: 178,
      weight: 75
    },
    teams,
    scope: {
      teamId: null,
      teamName: '全部球队',
      eventId: null,
      eventName: '生涯',
      gameMode: 'BASEBALL',
      teamAll: true,
      career: true
    },
    events: [],
    config: {
      version: 4,
      layout: 'v4-board',
      modules: [
        { key: 'performance', enabled: true, x: 100, y: 100, width: 400, height: 320 },
        { key: 'teams', enabled: true, x: 600, y: 100, width: 400, height: 300 },
        { key: 'schedule', enabled: true, x: 100, y: 500, width: 440, height: 300 }
      ]
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
    recentGames: []
  };
}

async function mockClubhouse(page) {
  const state = { payload: null };
  await page.addInitScript(() => {
    localStorage.setItem('bs-ball-locale', 'zh-CN');
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: false, username: 'local-e2e'
    }));
  });
  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    if (route.request().method() === 'PUT' && path === '/account/clubhouse/config') {
      state.payload = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: state.payload })
      });
      return;
    }
    if (path === '/account/clubhouse') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: dashboard() })
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: {} })
    });
  });
  return state;
}

async function openBoard(page) {
  await page.setViewportSize(VIEWPORT);
  const state = await mockClubhouse(page);
  await page.goto(`/bs-ball/${TENANT}/me`);
  await expect(page.locator('.dashboard-board.is-free')).toBeVisible();
  return state;
}

async function enterEditAndSelect(page, key) {
  await page.locator('.layout-edit-entry').click();
  await expect(page.locator('.layout-edit-banner')).toBeVisible();
  const module = page.locator(`[data-module-key="${key}"]`);
  await module.click();
  await expect(page.locator('.layout-selection-frame')).toBeVisible();
  await expect(page.locator('.resize-handle')).toHaveCount(8);
  return module;
}

async function moduleGeometry(module) {
  return module.evaluate((el) => ({
    left: parseFloat(el.style.left),
    top: parseFloat(el.style.top),
    width: parseFloat(el.style.width),
    height: parseFloat(el.style.height),
    position: getComputedStyle(el).position
  }));
}

async function dragBy(page, locator, dx, dy) {
  const box = await locator.boundingBox();
  const x = box.x + box.width / 2;
  const y = box.y + box.height / 2;
  await page.mouse.move(x, y);
  await page.mouse.down();
  await page.mouse.move(x + dx, y + dy, { steps: 24 });
  await page.mouse.up();
}

test('自由画布：按 v4 坐标渲染，八手柄齐全，拖动主体保存', async ({ page }) => {
  const state = await openBoard(page);
  const performance = page.locator('[data-module-key="performance"]');
  const geometry = await moduleGeometry(performance);

  expect(geometry).toEqual({ left: 100, top: 100, width: 400, height: 320, position: 'absolute' });
  expect(await page.locator('.dashboard-board').evaluate((el) => el.style.height)).toBe('800px');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);

  await enterEditAndSelect(page, 'performance');
  await expect(page.locator('.layout-edit-banner')).toContainText('数据表现');
  await expect(page.locator('.layout-edit-banner')).toContainText('400 × 320 px');
  await expect(page.locator('[data-module-key="performance"] > *').first()).toHaveAttribute('inert', '');

  await dragBy(page, performance, 80, 80);
  await expect(page.locator('.edit-status')).toHaveText('已保存');
  const moved = await moduleGeometry(performance);
  expect(moved.left).toBeGreaterThan(100);
  expect(moved.top).toBeGreaterThan(100);
  expect(state.payload.version).toBe(4);
  expect(state.payload.layout).toBe('v4-board');
  const saved = state.payload.modules.find((row) => row.key === 'performance');
  expect(saved.x).toBe(moved.left);
  expect(saved.y).toBe(moved.top);
});

test('拖动主体撞邻居停在最近合法位置，不重叠', async ({ page }) => {
  await openBoard(page);
  const performance = await enterEditAndSelect(page, 'performance');
  const teams = page.locator('[data-module-key="teams"]');
  const teamsBox = await teams.boundingBox();
  const perfBox = await performance.boundingBox();
  const startX = perfBox.x + perfBox.width / 2;
  const startY = perfBox.y + 40;

  await page.mouse.move(startX, startY);
  await page.mouse.down();
  await page.mouse.move(teamsBox.x + 10, startY, { steps: 30 });
  await page.mouse.up();

  const moved = await moduleGeometry(performance);
  expect(moved.left).toBeLessThanOrEqual(184);
  expect(moved.left + moved.width + 16).toBeLessThanOrEqual(600);
});

test('八手柄：东边推入邻居停止，西北角同时改位置与尺寸', async ({ page }) => {
  await openBoard(page);
  const performance = await enterEditAndSelect(page, 'performance');

  const east = page.locator('.layout-selection-frame .handle-e');
  await dragBy(page, east, 200, 0);
  let geometry = await moduleGeometry(performance);
  expect(geometry.width).toBeGreaterThanOrEqual(400);
  expect(geometry.width).toBeLessThanOrEqual(484);
  expect(geometry.left + geometry.width + 16).toBeLessThanOrEqual(600);

  const northWest = page.locator('.layout-selection-frame .handle-nw');
  await dragBy(page, northWest, -50, -50);
  geometry = await moduleGeometry(performance);
  expect(geometry.left).toBeLessThan(100);
  expect(geometry.top).toBeLessThan(100);
  expect(geometry.width).toBeGreaterThan(400);
  expect(geometry.height).toBeGreaterThan(320);
});

test('南手柄可缩小到目录最小高度，内容改为内部滚动', async ({ page }) => {
  await openBoard(page);
  const teams = await enterEditAndSelect(page, 'teams');
  const south = page.locator('.layout-selection-frame .handle-s');
  await dragBy(page, south, 0, -48);
  const geometry = await moduleGeometry(teams);
  expect(geometry.height).toBeLessThan(300);
  expect(geometry.height).toBeGreaterThanOrEqual(220);
});

test('断点 1339/1340：只有 1340px 才启用自由画布', async ({ page }) => {
  await openBoard(page);
  const board = page.locator('.dashboard-board');

  await page.setViewportSize({ width: 1339, height: 900 });
  await expect(board).not.toHaveClass(/is-free/);
  await expect(page.locator('.layout-edit-entry')).toHaveCount(0);

  await page.setViewportSize({ width: 1340, height: 900 });
  await expect(board).toHaveClass(/is-free/);
  await expect(page.locator('.layout-edit-entry')).toBeVisible();
  expect(await board.evaluate((el) => el.clientWidth)).toBe(1216);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('紧凑与手机：隐藏编辑入口，忽略绝对坐标', async ({ page }) => {
  await openBoard(page);
  await page.setViewportSize({ width: 1100, height: 900 });
  await expect(page.locator('.dashboard-board')).not.toHaveClass(/is-free/);
  await expect(page.locator('.layout-edit-entry')).toHaveCount(0);
  const compactColumns = await page.locator('.dashboard-board').evaluate((el) => getComputedStyle(el).gridTemplateColumns.split(' ').length);
  expect(compactColumns).toBe(2);
  expect(await page.locator('[data-module-key="performance"]').evaluate((el) => getComputedStyle(el).position)).toBe('static');

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator('.layout-edit-entry')).toHaveCount(0);
  await expect(page.locator('.resize-handle')).toHaveCount(0);
  const performanceBox = await page.locator('[data-module-key="performance"]').boundingBox();
  const teamsBox = await page.locator('[data-module-key="teams"]').boundingBox();
  const scheduleBox = await page.locator('[data-module-key="schedule"]').boundingBox();
  expect(performanceBox.y).toBeLessThan(teamsBox.y);
  expect(teamsBox.y).toBeLessThan(scheduleBox.y);
  expect(await page.locator('[data-module-key="performance"]').evaluate((el) => el.style.left)).toBe('');
});
