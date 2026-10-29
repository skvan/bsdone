import { test, expect } from '@playwright/test';

// #478 第 5 项：半局结束自动保存统计（无需手动点「保存比赛」）

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
  pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, wp: 0, pitchPa: 0, pitchBf: 0 },
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

test('半局结束自动保存统计：确认攻守交换后自动提交 save-live', async ({ page }) => {
  const savedLive = [];
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
      data = { snapshotJson: '' };
    } else if (path === '/game/10/save-live') {
      savedLive.push(request.postDataJSON());
      data = { success: true };
    } else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) {
      data = { restricted: false, players: [] };
    } else if (path === '/player/list') {
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
  const flowBar = page.locator('.les-flow-card');
  for (let out = 0; out < 3; out++) {
    await strike.click();
    await strike.click();
    await strike.click();
    await expect(flowBar).toBeVisible();
    // 浮层打开后 240ms 内会拦截点击（blockClickMsAfterOpen）
    await page.waitForTimeout(350);
    await flowBar.getByRole('button', { name: '挥空三振' }).click();
    await expect(flowBar).toBeHidden();
  }

  const dialog = page.locator('.inning-change-dialog');
  await expect(dialog).toBeVisible();
  // 三出局后、确认攻守交换前不自动保存
  expect(savedLive).toHaveLength(0);

  await dialog.getByRole('button', { name: '接受并交换攻守' }).click();
  await expect.poll(() => savedLive.length).toBe(1);

  const body = savedLive.at(-1);
  expect(body.game).toMatchObject({ inning: 1, topBottom: 'bottom' });
  expect(Array.isArray(body.stats)).toBe(true);
  // 18 条打击行 + 两队投手专用行
  expect(body.stats.length).toBe(20);
  expect(body.stats.find((row) => row.playerId === 201 && row.isPitcher === 1)).toBeTruthy();
  // 客队前三棒各被三振一次；主队投手 101 记 3 次夺三振
  expect(body.stats.find((row) => row.playerId === 201 && row.isPitcher === 0)).toMatchObject({
    teamId: 2, battingOrder: 1, so: 1
  });
  expect(body.stats.find((row) => row.playerId === 203 && row.isPitcher === 0)?.so).toBe(1);
  expect(body.stats.find((row) => row.playerId === 101 && row.isPitcher === 0)?.pitchSo).toBe(3);
  // 守备方（主队）9 名在场球员各记 3 个守备出局；投手按 P 计
  const fieldingPositions = body.fieldingPositions || [];
  expect(fieldingPositions).toHaveLength(9);
  expect(fieldingPositions.every((row) => row.teamId === 1)).toBe(true);
  expect(fieldingPositions.find((row) => row.playerId === 101 && row.position === 'P')).toMatchObject({
    outs: 3, fieldingGs: true
  });
  expect(fieldingPositions.find((row) => row.playerId === 108 && row.position === 'CF')).toMatchObject({ outs: 3 });
  // P3：三次挥空三振的 PO 记捕手 C（3 次），逐位置 PO 总数与球员级一致
  expect(fieldingPositions.find((row) => row.playerId === 102 && row.position === 'C')).toMatchObject({
    outs: 3, po: 3, a: 0, e: 0
  });
  expect(fieldingPositions.reduce((sum, row) => sum + (row.po || 0), 0)).toBe(3);
  await expect.poll(() => savedSnapshots.length).toBeGreaterThan(0);

  // 自动保存静默：不弹「保存成功」
  await expect(page.getByText('保存成功')).toHaveCount(0);
  expect(savedLive).toHaveLength(1);
});
