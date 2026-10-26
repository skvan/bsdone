import { test, expect } from '@playwright/test';

// 漏接三振 → 守备失误上一垒：菜单入口、失误守备员选择与 E 记账
// 运行：npx playwright test e2e/live-game-dk3-error.spec.js
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

const stats = players.map((player, index) => ({
  teamId: player.teamId,
  playerId: player.id,
  position: player.position,
  number: player.number,
  battingOrder: index % positions.length + 1,
  listOrder: index,
  ...zeroStats()
}));

test('漏接三振守备失误上一垒：选择失误守备员记 E 并安全上一垒', async ({ page }) => {
  const savedSnapshots = [];

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
    else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') {
      data = { snapshotJson: '' };
    } else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      savedSnapshots.push(request.postDataJSON().snapshotJson);
      data = { snapshotJson: request.postDataJSON().snapshotJson };
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

  const strike = page.locator('button[title="挥空（计好球）Swing & Miss"]');
  await strike.click();
  await strike.click();
  await strike.click();

  const flowBar = page.locator('.les-flow-card');
  await expect(flowBar).toBeVisible();
  await flowBar.getByRole('button', { name: '漏接三振' }).click();

  const menu = page.locator('.lgpfm__panel');
  await expect(menu).toBeVisible();
  // 浮层打开后 240ms 内会拦截点击（blockClickMsAfterOpen）
  await page.waitForTimeout(350);
  await menu.getByRole('button', { name: /挥空三振/ }).click();
  await menu.getByRole('button', { name: /守备失误上一垒/ }).click();
  await expect(flowBar).toContainText('请点击失误责任守备员');
  await page.locator('g.defender-hit[data-pos="3B"]').click();

  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => savedSnapshots.length).toBeGreaterThan(0);

  const snapshot = JSON.parse(savedSnapshots.at(-1));
  expect(snapshot.gameState).toMatchObject({
    outs: 0,
    balls: 0,
    strikes: 0,
    runners: { 1: 201, 2: null, 3: null }
  });
  expect(snapshot.atBatSummaries).toHaveLength(1);
  expect(snapshot.atBatSummaries[0].resultCode).toBe('DK3_REACH_E');
  expect(snapshot.playerStatsById['201'].batting).toMatchObject({ so: 1, ab: 1 });
  expect(snapshot.playerStatsById['101'].pitching).toMatchObject({ pitchSo: 1 });
  expect(snapshot.playerStatsById['105'].fielding).toMatchObject({ e: 1 });
  expect(snapshot.teamsData.away.score).toBe(0);
  expect(snapshot.teamsData.home.score).toBe(0);
  const logText = JSON.stringify(snapshot.eventLog);
  expect(logText).toContain('因守备失误安全上一垒 (K+E)');
  expect(logText).toContain('T1P5');
  expect(logText).toContain('记 E');
});
