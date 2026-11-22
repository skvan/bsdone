// Issue #717：赛后打击落点回放——打者棒次补全 + 同打席去重后的真实交互验收（管理端 / 门户）
//
// Mock 说明：
//  - 落点数据使用生产 game 282 经“同一打席键保留最新”去重后的 20 条（原始 31 条中的 11 条旧行由后端迁移
//    软删除；前端按契约不做内容去重，因此这里直接返回去重后的 20 条，验证真实展示）。
//  - 数据取自 2026-11-22 生产只读 API 快照（.qoder/tmp-spray-282.json、.qoder/282-game_282_stats.json）。
//  - 球员姓名取自公开 /player/list（teamId=1/6）。
//
// 运行：
//   npx playwright test e2e/game-detail-hit-replay-local-mocked.spec.js
import { test, expect } from '@playwright/test';

const GAME_ID = 282;
const EVENT_ID = 18;
const HOME_ID = 1;
const AWAY_ID = 6;

const game = {
  id: GAME_ID,
  eventId: EVENT_ID,
  homeTeamId: HOME_ID,
  awayTeamId: AWAY_ID,
  status: 'final',
  gameMode: 'BASEBALL',
  homeScore: 5,
  awayScore: 6,
  homeH: 1,
  awayH: 4,
  homeE: 3,
  awayE: 1,
  gameNumber: 12,
  gameday: '2026-11-14',
  gameTime: '2026-11-14 15:11:27',
  venue: '中山熊猫纪念球场',
  totalInnings: 9,
  inning: 5,
  topBottom: 'top',
};

const event = { id: EVENT_ID, name: '2026花城联盟冬季赛' };
const teams = [
  { id: HOME_ID, name: '暴力羊', shortName: '暴力羊' },
  { id: AWAY_ID, name: 'Lancers', shortName: 'Lancers' },
];

const roster1 = [
  { id: 1, teamId: 1, name: '吴景风', number: '10' },
  { id: 2, teamId: 1, name: '翁仲甫', number: '23' },
  { id: 3, teamId: 1, name: '李子杰', number: '9' },
  { id: 5, teamId: 1, name: '蔡宇翔', number: '51' },
  { id: 6, teamId: 1, name: '徐丞', number: '2' },
  { id: 7, teamId: 1, name: '岳童凯', number: '13' },
  { id: 8, teamId: 1, name: '黄一翀', number: '56' },
  { id: 14, teamId: 1, name: '梁大鹏', number: '41' },
  { id: 16, teamId: 1, name: '周哲仪', number: '25' },
  { id: 21, teamId: 1, name: '王译圣', number: '40' },
  { id: 22, teamId: 1, name: '陈志宇', number: '99' },
  { id: 23, teamId: 1, name: '邝逸舜', number: '47' },
  { id: 26, teamId: 1, name: '邱俊锋', number: '17' },
  { id: 84, teamId: 1, name: '배규식 BAE KYUSIK', shortName: 'BAE KYUSIK', number: '76' },
  { id: 1406, teamId: 1, name: '谢秉翰', number: '49' },
];

const roster6 = [
  { id: 135, teamId: 6, name: '李浩宏', number: '92' },
  { id: 136, teamId: 6, name: '莫一凡', number: '34' },
  { id: 143, teamId: 6, name: '李夏杰', number: '27' },
  { id: 152, teamId: 6, name: '陈俊贤', number: '24' },
  { id: 153, teamId: 6, name: '赵梓言', number: '90' },
  { id: 164, teamId: 6, name: '吴昌言', number: '83' },
  { id: 166, teamId: 6, name: '周廷彦', number: '58' },
  { id: 167, teamId: 6, name: '梁文彬', number: '78' },
  { id: 179, teamId: 6, name: '陈浩天', number: '8' },
  { id: 329, teamId: 6, name: '王石桐', number: '93' },
  { id: 333, teamId: 6, name: '郑硕彦', number: '20' },
  { id: 334, teamId: 6, name: '陈盈戎', number: '67' },
  { id: 1362, teamId: 6, name: '罗廷铠', number: '36' },
];

const allPlayers = [...roster1, ...roster6];

function stat(teamId, playerId, battingOrder, listOrder, extra = {}) {
  return {
    teamId, playerId, battingOrder, listOrder, isPitcher: 0, pa: 0, ...extra,
  };
}

// game 282 官方统计（28 行）；仅保留页面/回放需要的键，缺省字段页面按 0 处理
const stats282 = [
  stat(6, 164, 1, 0, { pa: 3, ab: 2, r: 1, h: 1, bbHp: 1, bb: 1, ibb: 1, position: 'DH' }),
  stat(1, 2, 5, 7, { pa: 3, ab: 3, so: 1, po: 7, a: 1, position: 'C' }),
  stat(1, 6, 6, 8, { pa: 2, ab: 1, r: 1, bbHp: 1, bb: 1, position: '3B' }),
  stat(1, 22, 7, 9, { pa: 2, ab: 2, rbi: 1, so: 1, position: 'DH' }),
  stat(1, 8, 8, 10, { pa: 1, r: 1, bbHp: 1, bb: 1, e: 1, po: 1, position: '2B' }),
  stat(1, 14, 9, 12, { pa: 2, ab: 1, r: 1, bbHp: 1, bb: 1, so: 1, e: 1, po: 1, position: 'SS' }),
  stat(6, 179, 2, 1, { pa: 3, ab: 2, rbi: 1, sf: 1, po: 2, a: 2, position: 'SS' }),
  stat(6, 334, null, 0, { isPitcher: 1, e: 1, ip: 1.1, pitchR: 4, er: 4, pitchSo: 1, pitchBb: 5, pitchPa: 10, pitchBf: 10, position: 'P' }),
  stat(1, 3, 4, 5, { pa: 2, ab: 2, so: 1, position: '1B' }),
  stat(6, 166, null, 3, { pa: 1, ab: 1, so: 1, position: 'CF' }),
  stat(6, 153, 3, 2, { pa: 2, ab: 2, so: 1, position: 'CF' }),
  stat(6, 136, 4, 4, { pa: 3, r: 2, bbHp: 3, sb: 2, bb: 2, hbp: 1, position: 'RF' }),
  stat(6, 1362, 5, 5, { pa: 3, ab: 3, h: 1, sb: 2, so: 1, position: '2B' }),
  stat(6, 152, 6, 6, { pa: 3, ab: 2, bbHp: 1, bb: 1, so: 2, e: 1, po: 1, position: '3B' }),
  stat(6, 329, 7, 7, { pa: 3, ab: 3, r: 1, h: 1, sb: 1, so: 1, a: 1, position: 'LF' }),
  stat(1, 84, 1, 0, { pa: 3, ab: 1, r: 1, bbHp: 2, bb: 1, hbp: 1, so: 1, po: 1, position: 'LF' }),
  stat(1, 26, null, 0, { isPitcher: 1, e: 2, ip: 3.2, pitchH: 4, pitchR: 5, er: 1, pitchSo: 6, pitchBb: 3, pitchPa: 21, pitchBf: 21, position: 'P' }),
  stat(6, 333, null, 1, { isPitcher: 1, ip: 2.1, pitchH: 1, pitchR: 1, pitchSo: 5, pitchBb: 2, pitchPa: 12, pitchBf: 12, position: 'P' }),
  stat(6, 167, null, 2, { isPitcher: 1, ip: 0.1, pitchPa: 1, pitchBf: 1, position: 'P' }),
  stat(1, 7, null, 11, { pa: 1, ab: 1, so: 1, position: '2B' }),
  stat(1, 1, 2, 2, { pa: 3, ab: 1, rbi: 1, bbHp: 2, sb: 1, bb: 2, po: 2, position: 'CF' }),
  stat(1, 23, 3, 3, { pa: 2, ab: 1, h: 1, rbi: 2, bbHp: 1, sb: 1, hbp: 1, position: 'RF' }),
  stat(1, 21, null, 1, { isPitcher: 1, ip: 0.1, pitchR: 1, pitchSo: 1, pitchBb: 1, pitchPa: 4, pitchBf: 4, position: 'P' }),
  stat(1, 5, null, 6, { pa: 1, bbHp: 1, hbp: 1, e: 1, position: '1B' }),
  stat(1, 1406, null, 1, { pa: 0, r: 1, sb: 1, position: 'LF' }),
  stat(1, 16, null, 4, { pa: 1, bbHp: 1, bb: 1, position: 'RF' }),
  stat(6, 143, 8, 8, { pa: 2, ab: 2, r: 1, so: 1, po: 4, position: '1B' }),
  stat(6, 135, 9, 9, { pa: 2, ab: 2, r: 1, h: 1, rbi: 1, po: 6, a: 2, position: 'C' }),
];

// 去重后 20 条（历史 teamId=0，按 half 推导：top=客队 Lancers(6)，bottom=主队 暴力羊(1)）
const spray282 = [
  { id: 2027, half: 'top', inning: 1, batterOrder: 1, playerId: 164, outsBefore: 0, runnersBefore: '1', resultCode: 'H1', bipCode: 'bip:fly:1b', sprayX: 566.76, sprayY: 238.68, sprayZone: 'RF', sprayDepth: 'shallow' },
  { id: 2028, half: 'top', inning: 1, batterOrder: 2, playerId: 179, outsBefore: 0, runnersBefore: '1,2', resultCode: 'E', bipCode: null, sprayX: 224.19, sprayY: 365.17, sprayZone: 'P', sprayDepth: 'shallow' },
  { id: 2040, half: 'bottom', inning: 1, batterOrder: 4, playerId: 3, outsBefore: 2, runnersBefore: '1,2', resultCode: 'OUT_F', bipCode: 'bip:fly:o1', sprayX: 178.07, sprayY: 345.41, sprayZone: 'SS', sprayDepth: 'deep' },
  { id: 2041, half: 'bottom', inning: 1, batterOrder: 5, playerId: 2, outsBefore: 3, runnersBefore: '1,2', resultCode: 'OUT_F', bipCode: 'bip:fly:o1', sprayX: 188.61, sprayY: 348.04, sprayZone: 'SS', sprayDepth: 'deep' },
  { id: 2056, half: 'top', inning: 2, batterOrder: 7, playerId: 329, outsBefore: 0, runnersBefore: '1', resultCode: 'H1', bipCode: 'bip:hg:1b', sprayX: 172.8, sprayY: 228.14, sprayZone: 'LF', sprayDepth: 'medium' },
  { id: 2057, half: 'top', inning: 2, batterOrder: 8, playerId: 143, outsBefore: 0, runnersBefore: '1,2', resultCode: 'E', bipCode: null, sprayX: 351.99, sprayY: 316.42, sprayZone: '2B', sprayDepth: 'deep' },
  { id: 2058, half: 'top', inning: 2, batterOrder: 9, playerId: 135, outsBefore: 0, runnersBefore: '1,2,3', resultCode: 'H1', bipCode: 'bip:ld:1b', sprayX: 170.17, sprayY: 153.04, sprayZone: 'CF', sprayDepth: 'shallow' },
  { id: 2059, half: 'top', inning: 2, batterOrder: 2, playerId: 179, outsBefore: 1, runnersBefore: '1,2,3', resultCode: 'SF', bipCode: 'bip:fly:sf', sprayX: 351.99, sprayY: 45, sprayZone: 'CF', sprayDepth: 'shallow' },
  { id: 2060, half: 'top', inning: 2, batterOrder: 3, playerId: 153, outsBefore: 2, runnersBefore: '2,3', resultCode: 'OUT_F', bipCode: 'bip:fly:o1', sprayX: 174.12, sprayY: 336.18, sprayZone: 'SS', sprayDepth: 'deep' },
  { id: 2061, half: 'top', inning: 2, batterOrder: 5, playerId: 1362, outsBefore: 2, runnersBefore: '1,2', resultCode: 'H1', bipCode: 'bip:g:1b', sprayX: 254.49, sprayY: 388.89, sprayZone: 'P', sprayDepth: 'shallow' },
  { id: 2067, half: 'bottom', inning: 2, batterOrder: 7, playerId: 22, outsBefore: 1, runnersBefore: '3', resultCode: 'OUT_G', bipCode: 'bip:g:bo:1', sprayX: 353.31, sprayY: 438.95, sprayZone: '1B', sprayDepth: 'shallow' },
  { id: 2259, half: 'bottom', inning: 2, batterOrder: 3, playerId: 23, outsBefore: 1, runnersBefore: '1,2,3', resultCode: 'H1', bipCode: 'bip:g:1b', sprayX: 19.6, sprayY: 262.45, sprayZone: 'LF', sprayDepth: 'medium' },
  { id: 2260, half: 'top', inning: 3, batterOrder: 7, playerId: 329, outsBefore: 0, runnersBefore: '1', resultCode: 'E', bipCode: null, sprayX: 195.6, sprayY: 336.85, sprayZone: 'SS', sprayDepth: 'deep' },
  { id: 2261, half: 'top', inning: 3, batterOrder: 9, playerId: 135, outsBefore: 2, runnersBefore: '1', resultCode: 'OUT_F', bipCode: 'bip:fly:o1', sprayX: 227.6, sprayY: 190.45, sprayZone: 'CF', sprayDepth: 'medium' },
  { id: 2262, half: 'top', inning: 3, batterOrder: 1, playerId: 164, outsBefore: 3, runnersBefore: '2', resultCode: 'OUT_F', bipCode: 'bip:fly:o1', sprayX: 54.8, sprayY: 287.25, sprayZone: 'LF', sprayDepth: 'medium' },
  { id: 2263, half: 'bottom', inning: 3, batterOrder: 6, playerId: 6, outsBefore: 2, runnersBefore: null, resultCode: 'OUT_G', bipCode: 'bip:g:bo:1', sprayX: 162.8, sprayY: 363.25, sprayZone: 'SS', sprayDepth: 'deep' },
  { id: 2265, half: 'top', inning: 4, batterOrder: 2, playerId: 179, outsBefore: 1, runnersBefore: null, resultCode: 'OUT_F', bipCode: 'bip:fly:o1', sprayX: 235.6, sprayY: 504.05, sprayZone: 'C', sprayDepth: 'medium' },
  { id: 2266, half: 'top', inning: 4, batterOrder: 5, playerId: 1362, outsBefore: 2, runnersBefore: '1,2', resultCode: 'E', bipCode: null, sprayX: 320, sprayY: 330, sprayZone: '2B', sprayDepth: 'deep' },
  { id: 2268, half: 'bottom', inning: 4, batterOrder: 2, playerId: 1, outsBefore: 2, runnersBefore: '1,3', resultCode: 'E', bipCode: null, sprayX: 134.8, sprayY: 396.05, sprayZone: '3B', sprayDepth: 'shallow' },
  { id: 2269, half: 'bottom', inning: 4, batterOrder: 5, playerId: 2, outsBefore: 3, runnersBefore: '1,2,3', resultCode: 'OUT_G', bipCode: 'bip:g:bo:1', sprayX: 145.2, sprayY: 356.05, sprayZone: 'SS', sprayDepth: 'deep' },
];

async function mockApis(page) {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('bs-ball-locale', 'zh-CN');
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e',
    }));
    localStorage.setItem('bsball.portal.fieldSettingsGuide.v1', JSON.stringify({
      v: 1, byTenant: { 'bs-ball': true }, legacyPortalMigrated: true,
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === `/event/${EVENT_ID}`) data = event;
    else if (path === `/game/${GAME_ID}`) data = game;
    else if (path === `/game/${GAME_ID}/stats`) data = { list: stats282, total: stats282.length };
    else if (path === `/game/${GAME_ID}/fielding-positions`) data = { list: [], total: 0 };
    else if (path === '/player/list') {
      const idsParam = url.searchParams.get('ids');
      const teamId = Number(url.searchParams.get('teamId') ?? 0);
      let list;
      if (idsParam) {
        const ids = new Set(idsParam.split(',').map((v) => Number(v)));
        list = allPlayers.filter((p) => ids.has(p.id));
      } else if (teamId) {
        list = allPlayers.filter((p) => p.teamId === teamId);
      } else {
        list = allPlayers;
      }
      data = { list, total: list.length };
    } else if (path === `/team/${HOME_ID}` || path === `/team/${AWAY_ID}`) {
      const id = Number(path.split('/').at(-1));
      data = teams.find((t) => t.id === id) ?? { id, name: `球队#${id}` };
    } else if (path === `/hit-spray/game/${GAME_ID}`) {
      data = spray282;
    } else if (/\/substitutions$/.test(path) || /\/earned-runs\/decisions$/.test(path)
      || /\/hit-spray\/records$/.test(path) || /\/fielding-adjustments$/.test(path)) {
      data = { list: [], total: 0 };
    } else if (path === '/portal/settings') {
      data = {};
    } else {
      console.log('[issue717-e2e] unmocked path:', path);
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data }),
    });
  });
}

async function openDropdown(page, selector) {
  await page.locator(`${selector} .el-select__wrapper`).click();
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(1);
}

async function visibleOptions(page) {
  return page.locator('.el-select-dropdown:visible .el-select-dropdown__item');
}

async function chooseOption(page, text) {
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: text }).first();
  await expect(option).toBeVisible();
  await option.click();
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0);
}

async function assertHitReplay(page, screenshotPath) {
  const panel = page.locator('.ghr-panel');
  await expect(panel).toBeVisible({ timeout: 20000 });
  await expect(panel.locator('.ghr-count')).toHaveText('共 20 次击球落点', { timeout: 20000 });

  // 等 stats 到位：全部打者选项 = 23 名球员 + “全部打者”
  await openDropdown(page, '.ghr-filter-batter');
  await expect(await visibleOptions(page)).toHaveCount(24);
  await chooseOption(page, '全部打者');

  // 全部：5 个安打圆点（主 1 + 客 4）
  await expect(page.locator('.ghr-dot--hit')).toHaveCount(5);

  // 主队：打者 13 项，含第 1/8/9 棒（无落点）与替补
  await openDropdown(page, '.ghr-filter-team');
  await expect(await visibleOptions(page)).toHaveCount(3);
  await chooseOption(page, '暴力羊');
  await openDropdown(page, '.ghr-filter-batter');
  const homeOptions = await visibleOptions(page);
  await expect(homeOptions).toHaveCount(14);
  await expect(homeOptions.filter({ hasText: '第1棒 배규식 BAE KYUSIK（无落点）' })).toHaveCount(1);
  await expect(homeOptions.filter({ hasText: '第8棒 黄一翀（无落点）' })).toHaveCount(1);
  await expect(homeOptions.filter({ hasText: '第9棒 梁大鹏（无落点）' })).toHaveCount(1);
  await expect(homeOptions.filter({ hasText: '（替补）' })).toHaveCount(4);
  await expect(homeOptions.filter({ hasText: '邱俊锋' })).toHaveCount(0);
  await expect(homeOptions.filter({ hasText: '王译圣' })).toHaveCount(0);

  // 无落点打者（第1棒 84）：空态文案，球队/打者下拉仍可用
  await chooseOption(page, '第1棒 배규식 BAE KYUSIK（无落点）');
  await expect(panel.locator('.el-empty__description')).toHaveText('该打者本场暂无击球落点记录');
  await expect(page.locator('.ghr-filter-team')).toBeVisible();
  await expect(page.locator('.ghr-filter-batter')).toBeVisible();
  await expect(page.locator('.ghr-dot--hit')).toHaveCount(0);

  // 第 3 棒 23：仅 2 局下 1 条，无重复
  await openDropdown(page, '.ghr-filter-batter');
  await chooseOption(page, '第3棒 邝逸舜');
  await expect(panel.locator('.ghr-item')).toHaveCount(1);
  await expect(panel.locator('.ghr-item')).toContainText('2局下 · 第3棒 邝逸舜');
  await expect(panel.locator('.ghr-item')).toContainText('一垒安打');

  // 主队安打圆点 = 1
  await openDropdown(page, '.ghr-filter-batter');
  await chooseOption(page, '全部打者');
  await expect(page.locator('.ghr-dot--hit')).toHaveCount(1);
  await panel.scrollIntoViewIfNeeded();
  await page.screenshot({ path: screenshotPath });

  // 客队：打者 10 项；第 5 棒 1362 显示 2 局上 H1 + 4 局上 E（旧 H1 不再出现）
  await openDropdown(page, '.ghr-filter-team');
  await chooseOption(page, 'Lancers');
  await openDropdown(page, '.ghr-filter-batter');
  const awayOptions = await visibleOptions(page);
  await expect(awayOptions).toHaveCount(11);
  await expect(awayOptions.filter({ hasText: '第5棒 罗廷铠' })).toHaveCount(1);
  await expect(awayOptions.filter({ hasText: '陈盈戎' })).toHaveCount(0);
  await expect(awayOptions.filter({ hasText: '郑硕彦' })).toHaveCount(0);
  await expect(awayOptions.filter({ hasText: '梁文彬' })).toHaveCount(0);
  await chooseOption(page, '第5棒 罗廷铠');
  const awayItems = panel.locator('.ghr-item');
  await expect(awayItems).toHaveCount(2);
  await expect(awayItems.nth(0)).toContainText('2局上 · 第5棒 罗廷铠');
  await expect(awayItems.nth(0)).toContainText('一垒安打');
  await expect(awayItems.nth(1)).toContainText('4局上 · 第5棒 罗廷铠');
  await expect(awayItems.nth(1)).toContainText('失误上垒');
  await expect(awayItems.nth(1)).not.toContainText('一垒安打');

  // 客队安打圆点 = 4；全部 = 5
  await openDropdown(page, '.ghr-filter-batter');
  await chooseOption(page, '全部打者');
  await expect(page.locator('.ghr-dot--hit')).toHaveCount(4);
  await openDropdown(page, '.ghr-filter-team');
  await chooseOption(page, '全部球队');
  await expect(page.locator('.ghr-dot--hit')).toHaveCount(5);
}

test('后台比赛详情 · game 282 落点回放：20 条、打者补全、无落点空态、E 取代旧 H1', async ({ page }) => {
  await mockApis(page);
  await page.goto(`/bs-ball/bs-ball/admin/events/${EVENT_ID}/games/${GAME_ID}`);
  await assertHitReplay(page, '../.qoder/issue717-admin-hit-replay.png');
});

test('门户比赛详情 · game 282 落点回放：20 条、打者补全、无落点空态、E 取代旧 H1', async ({ page }) => {
  await mockApis(page);
  await page.goto(`/bs-ball/bs-ball/events/${EVENT_ID}/games/${GAME_ID}`);
  await assertHitReplay(page, '../.qoder/issue717-portal-hit-replay.png');
});
