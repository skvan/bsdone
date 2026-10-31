import { test, expect } from '@playwright/test';

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

async function openLiveGame(page, bucket) {
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
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: '' };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      bucket.snapshots.push(request.postDataJSON().snapshotJson);
      data = { snapshotJson: '' };
    }
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
      body: JSON.stringify({ code: 200, data })
    });
  });
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
}

async function throwBalls(page, count) {
  for (let i = 0; i < count; i++) {
    await page.locator('button[title="坏球 Ball"]').click();
    await expect(page.locator('button[title="坏球 Ball"]')).toBeVisible();
  }
}

async function setRunnerOnBase(page, base, playerName) {
  await page.locator('.runners-group > .runner-draggable-group:visible').nth(base - 1).click();
  await expect(page.getByText(/谁在.垒上/).first()).toBeVisible();
  await page.waitForTimeout(600);
  await page.locator('.lgsfm__item').filter({ hasText: playerName }).first().click();
  await page.waitForTimeout(400);
  await waitForRunnerToken(page, base);
}

async function waitForRunnerToken(page, base) {
  const group = page.locator('.runners-group > .runner-draggable-group').nth(base - 1);
  await expect(group.locator('.runner-token-g')).toBeVisible({ timeout: 20000 });
  await expect(page.locator('.runner-path-anim-token')).toHaveCount(0, { timeout: 20000 });
}

async function startRunnerDrag(page, base) {
  await waitForRunnerToken(page, base);
  const group = page.locator('.runners-group > .runner-draggable-group:visible').nth(base - 1);
  await group.scrollIntoViewIfNeeded();
  const box = await group.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(300);
  await page.mouse.move(box.x + box.width / 2 + 28, box.y + box.height / 2 + 28, { steps: 5 });
  await page.waitForTimeout(150);
}

async function dropRunnerOnBase(page, base, half) {
  const rect = page.locator('rect.runner-drop-zone-rect:visible').nth(base - 1);
  await expect(rect).toBeVisible({ timeout: 10000 });
  const box = await rect.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height * (half === 'out' ? 0.78 : 0.22), { steps: 6 });
  await page.waitForTimeout(150);
  await page.mouse.up();
  await page.waitForTimeout(500);
  return page.locator('.rdm__panel', { hasText: '跑者进垒' });
}

async function dragRunner(page, fromBase, toBase, half) {
  await startRunnerDrag(page, fromBase);
  return dropRunnerOnBase(page, toBase, half);
}

async function pickMenuItem(page, text) {
  await page.locator('.rdm__item').filter({ hasText: text }).first().click();
  await page.waitForTimeout(200);
}

async function clickFielder(page, pos) {
  await page.locator(`g.defender-hit[data-pos="${pos}"]:visible`).first().click();
  await page.waitForTimeout(250);
}

async function saveGame(page, bucket) {
  const before = bucket.snapshots.length;
  await page.getByRole('button', { name: '保存比赛' }).click();
  await expect.poll(() => bucket.snapshots.length).toBeGreaterThan(before);
  return JSON.parse(bucket.snapshots.at(-1));
}

test('Issue #527：二垒同垒出局改为触杀链，三垒回本垒盗垒失败记 CS', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);

  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecond.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');

  const sameBaseMenu = await dragRunner(page, 2, 2, 'out');
  await expect(sameBaseMenu.getByText('牵制出局')).toBeVisible();
  await expect(sameBaseMenu.getByText('触杀出局')).toBeVisible();
  await expect(sameBaseMenu.getByText('离垒过远出局')).toHaveCount(0);
  await pickMenuItem(page, '触杀出局');
  await expect(page.getByText('请点击完成该出局的守备员（夹杀可继续点选记录多传）').first()).toBeVisible();
  await clickFielder(page, 'SS');
  await clickFielder(page, '2B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const tagSnapshot = await saveGame(page, bucket);
  const tagLog = JSON.stringify(tagSnapshot.eventLog);
  expect(tagLog).toContain('触杀出局：二垒跑者 T2P1 (#1) 于二垒出局（6-4）');
  expect(tagSnapshot.gameState.outs).toBe(1);
  expect(tagSnapshot.playerStatsById['106'].fielding).toMatchObject({ a: 1, po: 0 });
  expect(tagSnapshot.playerStatsById['104'].fielding).toMatchObject({ a: 0, po: 1 });
  expect(tagSnapshot.playerStatsById['201'].batting.cs).toBe(0);

  await throwBalls(page, 4);
  const toSecondAgain = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecondAgain.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');
  const toThird = await dragRunner(page, 2, 3, 'safe');
  await expect(toThird.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');
  await throwBalls(page, 1);

  const homeMenu = await dragRunner(page, 3, 4, 'out');
  await expect(homeMenu.getByText('盗垒失败')).toBeVisible();
  await expect(homeMenu.getByText('触杀出局')).toBeVisible();
  await expect(homeMenu.getByText('盗垒被刺杀 (CS)')).toHaveCount(0);
  await pickMenuItem(page, '盗垒失败');
  await expect(page.getByText('盗垒失败：请从捕手（C）开始点选传球链（夹杀可继续点选记录多传）').first()).toBeVisible();
  await clickFielder(page, 'P');
  await expect(page.getByText('盗垒失败请先点捕手（C）发起传球链').first()).toBeVisible();
  await clickFielder(page, 'C');
  await clickFielder(page, 'P');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const homeSnapshot = await saveGame(page, bucket);
  const homeLog = JSON.stringify(homeSnapshot.eventLog);
  expect(homeLog).toContain('盗垒死 (CS)：三垒跑者 T2P2 (#2) 于本垒出局（2-1）');
  expect(homeSnapshot.gameState.outs).toBe(2);
  expect(homeSnapshot.playerStatsById['102'].fielding).toMatchObject({ a: 1, po: 0, catcherCs: 1 });
  expect(homeSnapshot.playerStatsById['101'].fielding).toMatchObject({ a: 0, po: 1 });
  expect(homeSnapshot.playerStatsById['202'].batting.cs).toBe(1);
});

test('Issue #527：飞球接杀后同垒出局仍保留离垒过远并走接杀回传', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecond.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');

  await page.getByRole('button', { name: '界内球' }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpfm__cn', { hasText: /^飞球$/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpfm__cn', { hasText: /^出局$/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpfm__cn', { hasText: /^一垒前出局$/ }).click();
  await page.waitForTimeout(500);
  await clickFielder(page, 'CF');
  await page.getByRole('button', { name: /完成记录/ }).click();
  await page.waitForTimeout(400);

  const sameBaseMenu = await dragRunner(page, 2, 2, 'out');
  await expect(sameBaseMenu.getByText('触杀出局')).toBeVisible();
  await expect(sameBaseMenu.getByText('盗垒失败')).toHaveCount(0);
  await expect(sameBaseMenu.getByText('离垒过远出局')).toBeVisible();
  await pickMenuItem(page, '离垒过远出局');
  await expect(page.getByText('请点击回传踩垒的守备员（接杀回传）').first()).toBeVisible();
  await clickFielder(page, 'SS');
  const catchBackSnapshot = await saveGame(page, bucket);
  const catchBackLog = JSON.stringify(catchBackSnapshot.eventLog);
  expect(catchBackLog).toContain('离垒过远出局：二垒跑者 T2P1 (#1) 于二垒出局（8-6，接杀回传）');
  expect(catchBackSnapshot.gameState.outs).toBe(2);
  expect(catchBackSnapshot.playerStatsById['108'].fielding).toMatchObject({ a: 1, po: 1 });
  expect(catchBackSnapshot.playerStatsById['106'].fielding).toMatchObject({ a: 0, po: 1 });

  await page.getByRole('button', { name: /撤销/ }).click();
  const cancelRestoredFlow = page.getByRole('button', { name: '取消' }).first();
  if (await cancelRestoredFlow.isVisible().catch(() => false)) await cancelRestoredFlow.click();
  await waitForRunnerToken(page, 2);
  const afterUndoMenu = await dragRunner(page, 2, 2, 'out');
  await expect(afterUndoMenu.getByText('离垒过远出局')).toBeVisible();
  await page.locator('.rdm__cancel').first().click();
});

test('Issue #527：撤销接杀后同垒菜单不再残留离垒过远', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await setRunnerOnBase(page, 1, 'T2P2');

  await page.getByRole('button', { name: '界内球' }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpfm__cn', { hasText: /^飞球$/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpfm__cn', { hasText: /^出局$/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpfm__cn', { hasText: /^一垒前出局$/ }).click();
  await page.waitForTimeout(500);
  await clickFielder(page, 'CF');
  await page.getByRole('button', { name: /完成记录/ }).click();
  await page.waitForTimeout(400);

  await page.getByRole('button', { name: /撤销/ }).click();
  await expect(page.locator('.lg-scoreboard')).toContainText('打者#1T2P1');
  await page.waitForTimeout(500);
  await waitForRunnerToken(page, 1);
  const afterUndoMenu = await dragRunner(page, 1, 1, 'out');
  await expect(afterUndoMenu.getByText('触杀出局')).toBeVisible();
  await expect(afterUndoMenu.getByText('离垒过远出局')).toHaveCount(0);
  await page.locator('.rdm__cancel').first().click();
});

test('Issue #527：无投球一垒盗垒失败，C 首发单传记 CS', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const stealOutMenu = await dragRunner(page, 1, 2, 'out');
  await expect(stealOutMenu.getByText('盗垒失败')).toBeVisible();
  await expect(stealOutMenu.getByText('触杀出局')).toBeVisible();
  await expect(stealOutMenu.getByText('盗垒被刺杀 (CS)')).toHaveCount(0);
  await pickMenuItem(page, '盗垒失败');
  await expect(page.getByText('盗垒失败：请从捕手（C）开始点选传球链（夹杀可继续点选记录多传）').first()).toBeVisible();
  await clickFielder(page, 'SS');
  await expect(page.getByText('盗垒失败请先点捕手（C）发起传球链').first()).toBeVisible();
  await clickFielder(page, 'C');
  await clickFielder(page, 'C');
  await clickFielder(page, 'SS');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('盗垒死 (CS)：一垒跑者 T2P1 (#1) 于二垒出局（2-6）');
  expect(snapshot.gameState.outs).toBe(1);
  expect(snapshot.playerStatsById['102'].fielding).toMatchObject({ a: 1, po: 0, catcherCs: 1 });
  expect(snapshot.playerStatsById['106'].fielding).toMatchObject({ a: 0, po: 1 });
  expect(snapshot.playerStatsById['201'].batting.cs).toBe(1);
});

test('Issue #527：盗垒失败仅捕手触杀不记 CS', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const stealOutMenu = await dragRunner(page, 1, 2, 'out');
  await expect(stealOutMenu.getByText('盗垒失败')).toBeVisible();
  await pickMenuItem(page, '盗垒失败');
  await clickFielder(page, 'C');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('触杀出局：一垒跑者 T2P1 (#1) 于二垒出局（捕手 T1P2 (#2)）');
  expect(log).not.toContain('盗垒死 (CS)');
  expect(snapshot.gameState.outs).toBe(1);
  expect(snapshot.playerStatsById['102'].fielding).toMatchObject({ a: 0, po: 1, catcherCs: 0 });
  expect(snapshot.playerStatsById['201'].batting.cs).toBe(0);
});

test('Issue #527：盗垒失败多传夹杀不记 CS', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await throwBalls(page, 1);
  const stealOutMenu = await dragRunner(page, 1, 2, 'out');
  await expect(stealOutMenu.getByText('盗垒失败')).toBeVisible();
  await expect(stealOutMenu.getByText('触杀出局')).toBeVisible();
  await expect(stealOutMenu.getByText('盗垒被刺杀 (CS)')).toHaveCount(0);
  await pickMenuItem(page, '盗垒失败');
  await clickFielder(page, 'C');
  await clickFielder(page, '3B');
  await clickFielder(page, '2B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('触杀出局：一垒跑者 T2P1 (#1) 于二垒出局（2-5-4）');
  expect(log).not.toContain('盗垒死 (CS)');
  expect(snapshot.gameState.outs).toBe(1);
  expect(snapshot.playerStatsById['102'].fielding).toMatchObject({ a: 1, po: 0, catcherCs: 0 });
  expect(snapshot.playerStatsById['105'].fielding).toMatchObject({ a: 1, po: 0 });
  expect(snapshot.playerStatsById['104'].fielding).toMatchObject({ a: 0, po: 1 });
  expect(snapshot.playerStatsById['201'].batting.cs).toBe(0);
});
