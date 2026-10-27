import { test, expect } from '@playwright/test';

// 实时录入页「编辑阵容」返回入口：首个 Play 前可返回，Play 开始后禁用
// 运行：npx playwright test e2e/live-game-lineup-return.spec.js
const positions = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];
const players = [1, 2].flatMap((teamId) => positions.map((position, index) => ({
  id: teamId * 100 + index + 1,
  name: `T${teamId}P${index + 1}`,
  number: String(index + 1),
  teamId,
  position,
  positions: [position]
})));

const zeroStats = () => ({
  batting: { ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, sf: 0, sh: 0, sb: 0, cs: 0 },
  pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0 },
  fielding: { po: 0, a: 0, e: 0 }
});

function buildStats(played = false) {
  return players.map((player, index) => {
    const row = {
      teamId: player.teamId,
      playerId: player.id,
      position: player.position,
      number: player.number,
      battingOrder: index % positions.length + 1,
      listOrder: index,
      ...zeroStats()
    };
    if (played && player.id === 101) Object.assign(row, { np: 1, pitchBf: 1, pitchPa: 1 });
    return row;
  });
}

async function setupLiveGame(page, { played = false } = {}) {
  const gameStats = buildStats(played);
  let savedSnapshot = '';

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

    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 }
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0
    };
    else if (path === '/game/10/stats') data = { list: gameStats, total: gameStats.length };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: savedSnapshot };
    } else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      savedSnapshot = request.postDataJSON().snapshotJson;
      data = { snapshotJson: savedSnapshot };
    } else if (path === '/game/10/save-live') data = { success: true };
    else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list') {
      const teamId = Number(url.searchParams.get('teamId') ?? 1);
      const list = players.filter((player) => player.teamId === teamId);
      data = { list, total: list.length };
    } else if (path === '/stadium/list') data = { list: [], total: 0 };
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

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
}

test('首个 Play 前「编辑阵容」可返回先发阵容页', async ({ page }) => {
  await setupLiveGame(page);
  const button = page.getByRole('button', { name: '编辑阵容' });
  await expect(button).toBeVisible();
  await expect(button).toBeEnabled();
  expect(await button.getAttribute('title')).toContain('返回先发阵容');

  await button.click();
  await page.waitForURL(/\/events\/1\/games\/10\/lineup$/);
  await expect(page.locator('.lineup-board-header .header-left')).toContainText('确认先发阵容');
  await page.waitForTimeout(1500);
  expect(page.url()).toMatch(/\/events\/1\/games\/10\/lineup$/);
  await expect(page.getByText('离开确认')).toHaveCount(0);
});

test('首个 Play 开始后「编辑阵容」变灰不可点', async ({ page }) => {
  await setupLiveGame(page);
  await page.locator('button[title="坏球 Ball"]').click();

  const button = page.getByRole('button', { name: '编辑阵容' });
  await expect(button).toBeDisabled();
  expect(await button.getAttribute('title')).toContain('第一个 Play 已开始');
  expect(page.url()).toMatch(/\/events\/1\/games\/10\/live$/);
});

test('按已保存统计重建现场时「编辑阵容」仍为禁用', async ({ page }) => {
  await setupLiveGame(page, { played: true });
  const button = page.getByRole('button', { name: '编辑阵容' });
  await expect(button).toBeDisabled();
  expect(await button.getAttribute('title')).toContain('第一个 Play 已开始');
});
