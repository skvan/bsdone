import { test, expect } from '@playwright/test';

test.use({ viewport: { width: 1366, height: 900 } });

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
  let savedSnapshot = '';
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
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: savedSnapshot };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') {
      savedSnapshot = request.postDataJSON().snapshotJson;
      bucket.snapshots.push(savedSnapshot);
      data = { snapshotJson: savedSnapshot };
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
  if (count >= 4) await page.waitForTimeout(800);
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
  const rect = page.locator(`rect.runner-drop-zone-rect[data-base="${base}"]:visible`).first();
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

async function recordSingle(page) {
  await page.getByRole('button', { name: '界内球' }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpm__col--bip .lgpm__cn, .lgpm__col--bip .lgpm__result', { hasText: /^滚地球$/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpm__col--bip .lgpm__cn, .lgpm__col--bip .lgpm__result', { hasText: /^一垒安打$/ }).click();
  await page.waitForTimeout(500);
  await page.getByRole('button', { name: '跳过' }).first().click();
  await page.waitForTimeout(500);
}

async function openBipType(page, typeText) {
  await page.getByRole('button', { name: '界内球' }).click();
  const bip = page.locator('.lgpm__col--bip');
  await expect(bip).toBeVisible({ timeout: 15000 });
  const typeItem = bip.locator('.lgpm__cn, .lgpm__result').filter({ hasText: new RegExp(`^${typeText}`) });
  await expect(typeItem).toBeVisible({ timeout: 15000 });
  await typeItem.click();
  await page.waitForTimeout(300);
}

async function clickBipItem(page, text) {
  const bip = page.locator('.lgpm__col--bip');
  const item = bip.locator('.lgpm__cn, .lgpm__result').filter({ hasText: new RegExp(`^${text}`) });
  await expect(item).toBeVisible({ timeout: 10000 });
  await item.click();
  await page.waitForTimeout(300);
}

async function recordGroundOut(page, fielder = 'SS') {
  await openBipType(page, '滚地球');
  await clickBipItem(page, '出局');
  await clickBipItem(page, '一垒前出局');
  await clickFielder(page, fielder);
  await clickFielder(page, '1B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  await page.waitForTimeout(500);
}

async function recordFlyCatch(page, fielder = 'CF') {
  await openBipType(page, '飞球');
  await clickBipItem(page, '出局');
  await clickBipItem(page, '一垒前出局');
  await clickFielder(page, fielder);
  await page.getByRole('button', { name: /完成记录/ }).click();
  await page.waitForTimeout(500);
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
  expect(tagSnapshot.playerStatsById['106'].fielding).toMatchObject({ a: 1, po: 0, dp: 0 });
  expect(tagSnapshot.playerStatsById['104'].fielding).toMatchObject({ a: 0, po: 1, dp: 0 });
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
  await expect(homeMenu.getByText('封杀出局')).toHaveCount(0);
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

test('Issue #527：飞球接杀后同垒出局改走封杀传球链，无离垒过远', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecond.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');

  await page.getByRole('button', { name: '界内球' }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpm__col--bip .lgpm__cn, .lgpm__col--bip .lgpm__result', { hasText: /^飞球$/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpm__col--bip .lgpm__cn, .lgpm__col--bip .lgpm__result', { hasText: /^出局/ }).click();
  await page.waitForTimeout(500);
  await page.locator('.lgpm__col--bip .lgpm__cn, .lgpm__col--bip .lgpm__result', { hasText: /^一垒前出局$/ }).click();
  await page.waitForTimeout(500);
  await clickFielder(page, 'CF');
  await page.getByRole('button', { name: /完成记录/ }).click();
  await page.waitForTimeout(400);

  const sameBaseMenu = await dragRunner(page, 2, 2, 'out');
  await expect(sameBaseMenu.getByText('触杀出局')).toBeVisible();
  await expect(sameBaseMenu.getByText('封杀出局')).toBeVisible();
  await expect(sameBaseMenu.getByText('盗垒失败')).toHaveCount(0);
  await expect(sameBaseMenu.getByText('离垒过远出局')).toHaveCount(0);
  await pickMenuItem(page, '封杀出局');
  await clickFielder(page, 'CF');
  await clickFielder(page, '2B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('封杀出局：二垒跑者 T2P1 (#1) 于二垒出局（8-4）');
  expect(log).not.toContain('接杀回传');
  expect(snapshot.gameState.outs).toBe(2);
  expect(snapshot.playerStatsById['108'].fielding).toMatchObject({ a: 1, po: 1, dp: 1 });
  expect(snapshot.playerStatsById['104'].fielding).toMatchObject({ a: 0, po: 1, dp: 1 });
});

test('Issue #527：没有接杀时同垒菜单不提供封杀', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const menu = await dragRunner(page, 1, 1, 'out');
  await expect(menu.getByText('触杀出局')).toBeVisible();
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await expect(menu.getByText('牵制出局')).toBeVisible();
  await page.locator('.rdm__cancel:visible').click();
});

test('拖拽时前方垒位有人保持阻塞：不能越过二垒跑者', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 1);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 2);
  await startRunnerDrag(page, 1);
  await expect(page.locator('rect.runner-drop-zone-rect[data-base="2"]:visible')).toHaveCount(0);
  await expect(page.locator('rect.runner-drop-zone-rect[data-base="3"]:visible')).toHaveCount(0);
  await expect(page.locator('rect.runner-drop-zone-rect[data-base="4"]:visible')).toHaveCount(0);
  await expect(page.locator('rect.runner-drop-zone-rect[data-base="1"]:visible')).toHaveCount(1);
  await page.mouse.up();
  await expect(page.locator('rect.runner-drop-zone-rect:visible')).toHaveCount(0);
  await waitForRunnerToken(page, 1);
  await waitForRunnerToken(page, 2);
});

test('拖拽选失误推进后直接使用拖拽目标垒，不再二次选目标垒', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const menu = await dragRunner(page, 1, 2, 'safe');
  await expect(menu.getByText('传球失误推进')).toBeVisible();
  await pickMenuItem(page, '传球失误推进');
  const flow = page.locator('.les-flow-card');
  await expect(flow).toContainText('请点击失误责任守备员');
  await clickFielder(page, 'SS');
  await expect(flow).toHaveCount(0);
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：二垒有人');
});

test('强迫进垒口径：仅二垒有人时一垒安打不自动上三垒，拖拽安打推进后才进垒', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await pickMenuItem(page, '盗垒成功 (SB)');
  await recordSingle(page);
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：一垒、二垒有人');
  await waitForRunnerToken(page, 2);
  const menu = await dragRunner(page, 2, 3, 'safe');
  await expect(menu.getByText('安打推进（不计打点）')).toBeVisible();
  await pickMenuItem(page, '安打推进（不计打点）');
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：一垒、三垒有人');
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.gameState.runners[2]).toBeNull();
  expect(snapshot.gameState.runners[3]).toBe(201);
  expect(snapshot.gameState.runners[1]).not.toBeNull();
});

test('Issue #527：地滚出局自动推进被迫进垒跑者', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await recordGroundOut(page, 'SS');
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：二垒有人');
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.gameState.outs).toBe(1);
  expect(snapshot.gameState.runners[1]).toBeNull();
  expect(snapshot.gameState.runners[2]).toBe(201);
});

test('满垒地滚出局：三垒跑者自动回本垒并确认，无“不进垒”', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 1);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 2);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 3);
  await recordGroundOut(page, 'SS');
  await expect(page.locator('.hsc-inline__btn--safe')).toBeVisible();
  await expect(page.locator('.hsc-inline__btn').filter({ hasText: '不进垒' })).toHaveCount(0);
  await page.locator('.hsc-inline__btn--safe').click();
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：二垒、三垒有人');
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.gameState.outs).toBe(1);
  expect(snapshot.gameState.runners[1]).toBeNull();
  expect(snapshot.gameState.runners[2]).not.toBeNull();
  expect(snapshot.gameState.runners[3]).not.toBeNull();
  const totalRuns = Object.values(snapshot.playerStatsById).reduce(
    (sum, player) => sum + (player.batting?.r ?? 0),
    0
  );
  expect(totalRuns).toBe(1);
});

test('Issue #527：地滚出局一、二垒跑者同时被迫前进', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await throwBalls(page, 4);
  await recordGroundOut(page, 'SS');
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：二垒、三垒有人');
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.gameState.runners[1]).toBeNull();
  expect(snapshot.gameState.runners[2]).toBe(202);
  expect(snapshot.gameState.runners[3]).toBe(201);
});

test('Issue #527：地滚出局后 2→3 触杀记 DP', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecond.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');
  await recordGroundOut(page, 'SS');
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：二垒有人');
  const menu = await dragRunner(page, 2, 3, 'out');
  await expect(menu.getByText('触杀出局')).toBeVisible();
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await pickMenuItem(page, '触杀出局');
  await clickFielder(page, '2B');
  await clickFielder(page, '3B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('触杀出局：二垒跑者 T2P1 (#1) 于三垒出局（4-5）');
  for (const id of ['106', '103', '104', '105']) {
    expect(snapshot.playerStatsById[id].fielding.dp).toBe(1);
  }
});

test('Issue #527：打者先出局后的牵制是独立 play，不记 DP', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await recordGroundOut(page, 'SS');
  const menu = await dragRunner(page, 2, 2, 'out');
  await expect(menu.getByText('牵制出局')).toBeVisible();
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await pickMenuItem(page, '牵制出局');
  await clickFielder(page, '2B');
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.playerStatsById['106'].fielding.dp).toBe(0);
  expect(snapshot.playerStatsById['103'].fielding.dp).toBe(0);
  expect(snapshot.playerStatsById['101'].fielding.dp).toBe(0);
  expect(snapshot.playerStatsById['104'].fielding.dp).toBe(0);
});

test('Issue #527：保存重载后 play 上下文恢复，第二出局仍记 DP', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecond.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');
  await recordGroundOut(page, 'SS');
  await saveGame(page, bucket);
  await page.reload();
  await expect(page.getByRole('button', { name: '界内球' })).toBeVisible();
  await page.waitForTimeout(1200);
  await waitForRunnerToken(page, 2);
  const menu = await dragRunner(page, 2, 3, 'out');
  await expect(menu.getByText('触杀出局')).toBeVisible();
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await pickMenuItem(page, '触杀出局');
  await clickFielder(page, '2B');
  await clickFielder(page, '3B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.playerStatsById['106'].fielding.dp).toBe(1);
  expect(snapshot.playerStatsById['103'].fielding.dp).toBe(1);
  expect(snapshot.playerStatsById['104'].fielding.dp).toBe(1);
  expect(snapshot.playerStatsById['105'].fielding.dp).toBe(1);
});

test('Issue #527：飞球接杀不自动推进一垒跑者', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await recordFlyCatch(page, 'CF');
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：一垒有人');
});

test('Issue #527：飞球接杀后 2→3 触杀记 DP', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  const toSecond = await dragRunner(page, 1, 2, 'safe');
  await expect(toSecond.getByText('盗垒成功 (SB)')).toBeVisible();
  await pickMenuItem(page, '盗垒成功 (SB)');
  await recordFlyCatch(page, 'CF');
  await expect(page.locator('[aria-label^="垒包"]')).toHaveAttribute('aria-label', '垒包：二垒有人');
  const menu = await dragRunner(page, 2, 3, 'out');
  await expect(menu.getByText('触杀出局')).toBeVisible();
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await pickMenuItem(page, '触杀出局');
  await clickFielder(page, 'CF');
  await clickFielder(page, '3B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('触杀出局：二垒跑者 T2P1 (#1) 于三垒出局（8-5）');
  expect(snapshot.playerStatsById['108'].fielding).toMatchObject({ a: 1, po: 1, dp: 1 });
  expect(snapshot.playerStatsById['105'].fielding).toMatchObject({ a: 0, po: 1, dp: 1 });
});

test('Issue #527：三振 + 同一球盗垒死记 DP', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  for (let i = 0; i < 3; i++) {
    await page.locator('button[title="好球 Called Strike"]').click();
    await page.waitForTimeout(200);
  }
  await page.getByRole('button', { name: '挥空三振' }).click();
  await page.waitForTimeout(400);
  const menu = await dragRunner(page, 1, 2, 'out');
  await expect(menu.getByText('盗垒失败')).toBeVisible();
  await pickMenuItem(page, '盗垒失败');
  await clickFielder(page, 'C');
  await clickFielder(page, 'SS');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  const log = JSON.stringify(snapshot.eventLog);
  expect(log).toContain('盗垒死 (CS)：一垒跑者 T2P1 (#1) 于二垒出局（2-6）');
  expect(snapshot.playerStatsById['102'].fielding).toMatchObject({ a: 1, catcherCs: 1, dp: 1 });
  expect(snapshot.playerStatsById['106'].fielding).toMatchObject({ po: 1, dp: 1 });
});

test('Issue #527：三振后同垒出局不提供封杀', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  for (let i = 0; i < 3; i++) {
    await page.locator('button[title="好球 Called Strike"]').click();
    await page.waitForTimeout(200);
  }
  await page.getByRole('button', { name: '挥空三振' }).click();
  await page.waitForTimeout(400);
  const menu = await dragRunner(page, 1, 1, 'out');
  await expect(menu.getByText('触杀出局')).toBeVisible();
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await expect(menu.getByText('牵制出局')).toBeVisible();
  await pickMenuItem(page, '触杀出局');
  await clickFielder(page, '1B');
  await page.getByRole('button', { name: /完成记录/ }).click();
  const snapshot = await saveGame(page, bucket);
  expect(snapshot.playerStatsById['103'].fielding.dp).toBe(0);
  expect(snapshot.playerStatsById['101'].fielding.dp).toBe(0);
  expect(snapshot.playerStatsById['106'].fielding.dp).toBe(0);
});

test('Issue #527：非满垒三垒跑者不自动回本垒；拖拽出局不提供封杀', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await dragRunner(page, 1, 2, 'safe');
  await pickMenuItem(page, '盗垒成功 (SB)');
  await dragRunner(page, 2, 3, 'safe');
  await pickMenuItem(page, '盗垒成功 (SB)');
  await recordSingle(page);
  // 非强迫进垒：三垒跑者留垒，不弹本垒确认
  await expect(page.locator('.hsc-inline__btn--out')).toHaveCount(0);
  await waitForRunnerToken(page, 3);
  const menu = await dragRunner(page, 3, 4, 'out');
  await expect(menu.getByText('封杀出局')).toHaveCount(0);
  await expect(menu.getByText('触杀出局')).toBeVisible();
  await page.locator('.rdm__head-close:visible').click();
});

test('Issue #527：本垒出局原因，满垒被迫进垒显示封杀', async ({ page }) => {
  const bucket = { snapshots: [] };
  await openLiveGame(page, bucket);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 1);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 2);
  await throwBalls(page, 4);
  await waitForRunnerToken(page, 3);
  await recordSingle(page);
  await expect(page.locator('.hsc-inline__btn--safe')).toBeVisible();
  await expect(page.locator('.hsc-inline__btn--out')).toBeVisible();
  await expect(page.locator('.hsc-inline__btn').filter({ hasText: '不进垒' })).toHaveCount(0);
  await page.locator('.hsc-inline__btn--out').click();
  const panel = page.locator('.rdm__panel:visible', { hasText: '在本垒出局的原因' }).first();
  await expect(panel).toBeVisible();
  await expect(panel.getByText('封杀出局')).toBeVisible();
  await expect(panel.getByText('触杀出局')).toBeVisible();
  await panel.getByText('封杀出局').click();
  await expect(panel).toBeHidden();
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
