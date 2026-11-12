import { test, expect } from '@playwright/test';

test.use({ viewport: { width: 1366, height: 900 } });

test('pitch menu board: root, BIP result, out locations, conditions', async ({ page }) => {
  const positions = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];
  const players = [1, 2].flatMap((teamId) => positions.map((position, index) => ({
    id: teamId * 100 + index + 1,
    name: `T${teamId}P${index + 1}`,
    number: String(index + 1),
    teamId,
    position,
    positions: [position],
  })));
  const zeroStats = () => ({
    batting: { ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, sf: 0, sh: 0, sb: 0, cs: 0 },
    pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0 },
    fielding: { po: 0, a: 0, e: 0 },
  });
  const stats = players.map((player, index) => ({
    teamId: player.teamId,
    playerId: player.id,
    position: player.position,
    number: player.number,
    battingOrder: index % positions.length + 1,
    listOrder: index,
    ...zeroStats(),
  }));

  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e',
    }));
  });

  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event' };
    else if (path === '/team/select-options') data = [
      { id: 1, name: 'Home', tenantId: 1 }, { id: 2, name: 'Away', tenantId: 1 },
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0,
    };
    else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: '' };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') data = { snapshotJson: '' };
    else if (path === '/game/10/save-live') data = { success: true };
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
      body: JSON.stringify({ code: 200, data }),
    });
  });

  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();

  await page.getByRole('button', { name: '界内球' }).click();
  const menu = page.locator('.lgpm');
  await expect(menu).toBeVisible();
  await expect(menu.locator('.lgpm__col-title')).toHaveText(['投球判定', '界内球', '上垒与妨碍', '其他裁定']);
  await expect(menu.locator('.lgpm__state')).toContainText('第 1 局上');
  await expect(menu.locator('.lgpm__state')).toContainText('B 0');
  const pitchCol = menu.locator('.lgpm__col--pitch');
  await expect(pitchCol.getByText('漏接三振', { exact: true })).toHaveCount(0);
  await expect(pitchCol.getByText('指尖接杀', { exact: true })).toHaveCount(0);

  const bip = menu.locator('.lgpm__col--bip');
  await bip.getByRole('button', { name: /^滚地球/ }).click();
  await expect(bip.locator('.lgpm__crumb-current')).toHaveText('滚地球');
  await expect(bip.getByRole('button', { name: '出局' })).toBeVisible();
  await expect(bip.getByRole('button', { name: '一垒安打' })).toBeVisible();
  await expect(bip.getByRole('button', { name: '野手选择' })).toHaveCount(0);
  await expect(bip.getByRole('button', { name: '双杀' })).toHaveCount(0);
  await expect(bip.getByRole('button', { name: '三杀' })).toHaveCount(0);

  await bip.getByRole('button', { name: '出局' }).click();
  await expect(bip.locator('.lgpm__crumb-current')).toHaveText('出局');
  await expect(bip.getByRole('button', { name: '一垒前出局' })).toBeVisible();
  await expect(bip.getByRole('button', { name: '本垒前出局' })).toBeVisible();

  await bip.locator('.lgpm__crumb-back').click();
  await expect(bip.getByRole('button', { name: '一垒安打' })).toBeVisible();
  await bip.locator('.lgpm__crumb-back').click();
  await expect(bip.getByRole('button', { name: /^滚地球/ })).toBeVisible();

  await menu.getByRole('button', { name: '关闭' }).click();
  await expect(menu).toBeHidden();

  // 走一个保送，让一垒有人：野手选择/双杀应出现，三杀仍隐藏。
  const recordBall = async () => {
    await page.locator('button[title="坏球 Ball"]').click();
    await expect(page.locator('button[title="坏球 Ball"]')).toBeVisible();
  };
  for (let i = 0; i < 4; i++) await recordBall();
  await page.getByRole('button', { name: '界内球' }).click();
  await expect(menu).toBeVisible();
  await menu.locator('.lgpm__col--bip').getByRole('button', { name: /^滚地球/ }).click();
  await expect(bip.getByRole('button', { name: '野手选择' })).toBeVisible();
  await expect(bip.getByRole('button', { name: '双杀' })).toBeVisible();
  await expect(bip.getByRole('button', { name: '三杀' })).toHaveCount(0);

  // 一垒有人时，短打提供牺牲触击与安全牺牲短打。
  await bip.locator('.lgpm__crumb-back').click();
  await bip.getByRole('button', { name: /^短打/ }).click();
  await expect(bip.getByRole('button', { name: /牺牲短打（安全）/ })).toBeVisible();
  await expect(bip.getByRole('button', { name: /牺牲触击（SH）/ })).toBeVisible();

  // 再走一个保送，让一、二垒有人：三杀应出现。
  await menu.getByRole('button', { name: '关闭' }).click();
  for (let i = 0; i < 4; i++) await recordBall();
  await page.getByRole('button', { name: '界内球' }).click();
  await expect(menu).toBeVisible();
  await menu.locator('.lgpm__col--bip').getByRole('button', { name: /^滚地球/ }).click();
  await expect(bip.getByRole('button', { name: '三杀' })).toBeVisible();

  // 两好球：漏接三振与指尖接杀出现；漏接三振可在本列进入结果。
  await menu.getByRole('button', { name: '关闭' }).click();
  for (let i = 0; i < 2; i++) {
    await page.locator('button[title="好球 Called Strike"]').click();
    await expect(page.locator('button[title="好球 Called Strike"]')).toBeVisible();
  }
  await page.getByRole('button', { name: '界内球' }).click();
  await expect(menu).toBeVisible();
  await expect(pitchCol.getByText('漏接三振', { exact: true })).toBeVisible();
  await expect(pitchCol.getByText('指尖接杀', { exact: true })).toBeVisible();
  await expect(menu.locator('.lgpm__count--s')).toContainText('S 2');
  await pitchCol.getByRole('button', { name: '挥空', exact: true }).click();
  await expect(pitchCol.locator('.lgpm__crumb-current')).toHaveText('挥空');
  await expect(pitchCol.getByRole('button', { name: '暴投上一垒' })).toBeVisible();
  await expect(pitchCol.getByRole('button', { name: '捕逸上一垒' })).toBeVisible();
  await expect(pitchCol.getByRole('button', { name: '在一垒出局' })).toBeVisible();
  await pitchCol.locator('.lgpm__crumb-back').click();
  await expect(pitchCol.getByText('漏接三振', { exact: true })).toBeVisible();
  await expect(pitchCol.locator('.lgpm__col-title')).toHaveText('投球判定');

  // K_CHOOSE：第三好球后从流程卡打开漏接三振，验证二级/三级与命令收尾。
  await menu.getByRole('button', { name: '关闭' }).click();
  await page.locator('button[title="好球 Called Strike"]').click();
  await page.getByRole('button', { name: '漏接三振' }).click();
  await expect(menu).toBeVisible();
  await page.waitForTimeout(300); // 菜单打开后有 240ms 点击保护窗
  await expect(pitchCol.locator('.lgpm__crumb-current')).toHaveText('漏接三振');
  await expect(pitchCol.getByRole('button', { name: /挥空三振/ })).toBeVisible();
  await expect(pitchCol.getByRole('button', { name: /目送三振/ })).toBeVisible();
  await pitchCol.getByRole('button', { name: /挥空三振/ }).click();
  await expect(pitchCol.locator('.lgpm__crumb-current')).toHaveText('挥空');
  await expect(pitchCol.getByRole('button', { name: '在一垒出局' })).toBeVisible();
  await pitchCol.getByRole('button', { name: '暴投上一垒' }).click();
  await expect(menu).toBeHidden();
});
