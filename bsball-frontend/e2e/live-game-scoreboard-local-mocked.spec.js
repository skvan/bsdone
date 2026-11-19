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

async function openLive(page) {
  await page.addInitScript(() => {
    if (!sessionStorage.getItem('e2e-live-692-init')) {
      Object.keys(localStorage)
        .filter((key) => key.startsWith('bs-ball-live-'))
        .forEach((key) => localStorage.removeItem(key));
      sessionStorage.setItem('e2e-live-692-init', '1');
    }
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
      { id: 1, name: 'Home', tenantId: 1, logo: '/files/home-logo.png' },
      { id: 2, name: 'Away', tenantId: 1, logo: '/files/away-logo.png' }
    ];
    else if (path === '/game/10') data = {
      id: 10, eventId: 1, homeTeamId: 1, awayTeamId: 2, status: 'live',
      gameMode: 'BASEBALL', inning: 1, topBottom: 'top', homeScore: 0, awayScore: 0
    };
    else if (path === '/game/10/stats') data = { list: stats, total: stats.length };
    else if (path === '/game/10/live-snapshot' && request.method() === 'GET') data = { snapshotJson: '' };
    else if (path === '/game/10/live-snapshot' && request.method() === 'POST') data = { snapshotJson: request.postDataJSON().snapshotJson };
    else if (path === '/game/10/save-live') data = { success: true };
    else if (/^\/event\/1\/teams\/[12]\/roster-players$/.test(path)) data = { restricted: false, players: [] };
    else if (path === '/player/list') data = { list: players.filter((p) => p.teamId === Number(url.searchParams.get('teamId') ?? 1)), total: 9 };
    else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/team/1') data = { id: 1, name: 'Home', logo: '/files/home-logo.png' };
    else if (path === '/team/2') data = { id: 2, name: 'Away', logo: '/files/away-logo.png' };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });
  await page.clock.setFixedTime(new Date('2026-01-01T12:00:00'));
  await page.goto('/bs-ball/bs-ball/admin/events/1/games/10/live');
}

test('记分牌：灯色、拖动、缩放、点击穿透与位置记忆', async ({ page }) => {
  await openLive(page);
  const dock = page.locator('.lg-scoreboard-dock');
  const card = page.locator('.lg-scoreboard');
  const container = page.locator('.field-svg-wrap');
  await expect(card).toBeVisible();
  await expect(page.getByRole('button', { name: '保存比赛' })).toBeVisible();

  // LOGO 水印仍在（两台球队都注入 logo）
  const logoVars = await page.locator('.lg-team-row').evaluateAll((rows) =>
    rows.map((row) => row.style.getPropertyValue('--lg-team-logo'))
  );
  expect(logoVars.some((value) => value.includes('home-logo.png'))).toBe(true);
  expect(logoVars.some((value) => value.includes('away-logo.png'))).toBe(true);

  // 牌面下半部点击穿透：不遮挡球场点击（守备员/落点）
  const cardBox = await card.boundingBox();
  const bodyHit = await page.evaluate(([x, y]) => {
    const el = document.elementFromPoint(x, y);
    return el ? Boolean(el.closest('.lg-scoreboard-dock')) : null;
  }, [cardBox.x + 40, cardBox.y + cardBox.height - 25]);
  expect(bodyHit).toBe(false);

  // 等现场恢复的 loading 遮罩退场，并确认拖动区是最上层命中目标
  const waitForBoardHit = async () => {
    await expect.poll(async () => {
      const box = await page.locator('.lg-scoreboard__drag').boundingBox();
      if (!box) return false;
      return page.evaluate(([x, y]) => Boolean(document.elementFromPoint(x, y)?.closest('.lg-scoreboard-dock')), [box.x + box.width / 2, box.y + box.height / 2]);
    }).toBe(true);
  };
  await expect(page.locator('.el-loading-mask')).toHaveCount(0);
  await waitForBoardHit();
  // 默认位置外扩的把手不能撑出横向滚动
  expect(await container.evaluate((el) => el.scrollWidth - el.clientWidth)).toBeLessThanOrEqual(0);

  // 拖拽移动
  const containerBox = await container.boundingBox();
  const before = await dock.boundingBox();
  const dragArea = await page.locator('.lg-scoreboard__drag').boundingBox();
  await page.mouse.move(dragArea.x + dragArea.width / 2, dragArea.y + dragArea.height / 2);
  await page.mouse.down();
  await page.mouse.move(dragArea.x + dragArea.width / 2 - 120, dragArea.y + dragArea.height / 2 + 80, { steps: 12 });
  await page.mouse.up();
  const afterDrag = await dock.boundingBox();
  expect(afterDrag.x).toBeLessThan(before.x - 80);
  expect(afterDrag.y).toBeGreaterThan(before.y + 40);
  expect(afterDrag.x).toBeGreaterThanOrEqual(containerBox.x - 0.5);
  expect(afterDrag.y).toBeGreaterThanOrEqual(containerBox.y - 0.5);
  expect(afterDrag.x + afterDrag.width).toBeLessThanOrEqual(containerBox.x + containerBox.width + 0.5);
  expect(afterDrag.y + afterDrag.height).toBeLessThanOrEqual(containerBox.y + containerBox.height + 0.5);
  const savedAfterDrag = await page.evaluate(() => JSON.parse(localStorage.getItem('bs-ball-live-scoreboard-layout')));
  expect(savedAfterDrag.left).toBeCloseTo(afterDrag.x - containerBox.x, 0);
  expect(savedAfterDrag.top).toBeCloseTo(afterDrag.y - containerBox.y, 0);

  // 边框放大
  let handle = await page.locator('.lg-scoreboard-resize--se').boundingBox();
  const widthBefore = (await dock.boundingBox()).width;
  await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
  await page.mouse.down();
  await page.mouse.move(handle.x + handle.width / 2 + 110, handle.y + handle.height / 2 + 30, { steps: 12 });
  await page.mouse.up();
  const widthAfter = (await dock.boundingBox()).width;
  expect(widthAfter).toBeGreaterThan(widthBefore + 80);
  const savedAfterGrow = await page.evaluate(() => JSON.parse(localStorage.getItem('bs-ball-live-scoreboard-layout')));
  expect(savedAfterGrow.scale).toBeGreaterThan(1);

  // 边框缩小到最小尺寸 240px
  handle = await page.locator('.lg-scoreboard-resize--se').boundingBox();
  await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
  await page.mouse.down();
  await page.mouse.move(handle.x + handle.width / 2 - 220, handle.y + handle.height / 2 - 20, { steps: 12 });
  await page.mouse.up();
  const widthMin = (await dock.boundingBox()).width;
  expect(widthMin).toBeGreaterThan(236);
  expect(widthMin).toBeLessThan(244);

  // 右下角可见握把本身也能拖动缩放
  const grip = await page.locator('.lg-scoreboard-grip').boundingBox();
  await page.mouse.move(grip.x + grip.width / 2, grip.y + grip.height / 2);
  await page.mouse.down();
  await page.mouse.move(grip.x + grip.width / 2 + 30, grip.y + grip.height / 2 + 15, { steps: 8 });
  await page.mouse.up();
  const widthAfterGrip = (await dock.boundingBox()).width;
  expect(widthAfterGrip).toBeGreaterThan(258);

  // 拖到左上角：四边留 16px 安全边距，8 个缩放手柄仍能命中
  const dragAreaTopLeft = await page.locator('.lg-scoreboard__drag').boundingBox();
  await page.mouse.move(dragAreaTopLeft.x + dragAreaTopLeft.width / 2, dragAreaTopLeft.y + dragAreaTopLeft.height / 2);
  await page.mouse.down();
  await page.mouse.move(containerBox.x + 20, containerBox.y + 20, { steps: 12 });
  await page.mouse.up();
  const cornerBox = await dock.boundingBox();
  expect(cornerBox.x - containerBox.x).toBeCloseTo(16, 0);
  expect(cornerBox.y - containerBox.y).toBeCloseTo(16, 0);
  const handleSelectors = ['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw'].map((dir) => `.lg-scoreboard-resize--${dir}`);
  const handleHits = await page.evaluate((selectors) => selectors.map((selector) => {
    const el = document.querySelector(selector);
    if (!el) return false;
    const box = el.getBoundingClientRect();
    return document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2) === el;
  }), handleSelectors);
  expect(handleHits).toEqual(handleSelectors.map(() => true));

  // 灯色：B 绿、S 黄、O 红
  await page.locator('button[title="坏球 Ball"]').click();
  await page.locator('button[title="好球 Called Strike"]').click();
  const ballLight = page.locator('.lg-light--ball.is-on').first();
  const strikeLight = page.locator('.lg-light--strike.is-on').first();
  await expect(ballLight).toBeVisible();
  await expect(strikeLight).toBeVisible();
  expect(await ballLight.evaluate((el) => getComputedStyle(el).backgroundColor)).toBe('rgb(34, 197, 94)');
  expect(await strikeLight.evaluate((el) => getComputedStyle(el).backgroundColor)).toBe('rgb(250, 204, 21)');
  const outColor = await page.locator('.lg-light--out').first().evaluate((el) => {
    el.classList.add('is-on');
    const color = getComputedStyle(el).backgroundColor;
    el.classList.remove('is-on');
    return color;
  });
  expect(outColor).toBe('rgb(239, 68, 68)');

  // 刷新后记住位置与尺寸
  const stored = await page.evaluate(() => JSON.parse(localStorage.getItem('bs-ball-live-scoreboard-layout')));
  await page.reload();
  await expect(card).toBeVisible();
  await expect(page.locator('.el-loading-mask')).toHaveCount(0);
  const restored = await dock.boundingBox();
  expect(restored.width).toBeCloseTo(widthAfterGrip, 0);
  expect(restored.x - containerBox.x).toBeCloseTo(stored.left, 0);
  expect(restored.y - containerBox.y).toBeCloseTo(stored.top, 0);
});
