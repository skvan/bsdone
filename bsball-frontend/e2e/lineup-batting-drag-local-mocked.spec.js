// #340 冒烟（本地 mock，无后端）：阵容页打线列表「桌面整卡拖动排序 + 插入线」；
// 其余既有语义（池卡拖入列表追加、打线行拖卡槽只改守备位）作为回归点同场验证。
// 移动端「把手 + sortablejs」路径由单测/静态断言守护（本 spec 为 Desktop Chrome = pointer: fine）。
// 运行：npm run test:e2e（E2E_PORT 可指定端口）
import { test, expect } from '@playwright/test';

test.use({ viewport: { width: 1400, height: 950 } });

const awayPlayers = Array.from({ length: 12 }, (_, i) => ({
  id: 201 + i,
  name: `客队球员${i + 1}`,
  number: String(i + 1),
  teamId: 2
}));
const homePlayers = [
  { id: 101, name: '主队球员1', number: '1', teamId: 1 },
  { id: 102, name: '主队球员2', number: '2', teamId: 1 }
];

test('#340 桌面整卡拖动：行拖动重排 + 插入线；拖卡槽 / 池卡拖入列表不回归', async ({ page }) => {
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
    const path = new URL(request.url()).pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event', gameMode: 'BASEBALL', specialRules: {} };
    else if (path === '/event/1/game-team-options') data = [{ id: 1, name: 'Home' }, { id: 2, name: 'Away' }];
    else if (path === '/stadium/list') data = { list: [], total: 0 };
    else if (path === '/event/1/teams/1/roster-players') data = { restricted: false, players: homePlayers };
    else if (path === '/event/1/teams/2/roster-players') data = { restricted: false, players: awayPlayers };

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/games/live?homeTeamId=1&awayTeamId=2');

  // 客队（默认激活）池卡片渲染齐全
  await expect(page.locator('.lineup-card--pool')).toHaveCount(12);

  // 点池卡 → 点卡槽：落 3 人进打线（棒次 = 落位顺序）
  const place = async (playerId, code) => {
    await page.locator(`.lineup-card--pool[data-player-id="${playerId}"]`).click();
    await page.locator(`.lineup-slot[data-slot-code="${code}"]`).click();
  };
  await place(201, 'C');
  await place(202, '1B');
  await place(203, '2B');
  const battingOrder = () => page.$$eval('.lineup-batting__row', (els) => els.map((el) => el.dataset.playerId));
  await expect.poll(battingOrder).toEqual(['201', '202', '203']);

  // 桌面整卡拖动：把 3 棒拖到 1 棒上方（gap 0）→ 插入线出现 → 松手重排
  const row1 = page.locator('.lineup-batting__row[data-player-id="201"]');
  const row3 = page.locator('.lineup-batting__row[data-player-id="203"]');
  const b1 = await row1.boundingBox();
  const b3 = await row3.boundingBox();
  const cx = b3.x + b3.width / 2;
  const cy = b3.y + b3.height / 2;
  await page.mouse.move(cx, cy);
  await page.mouse.down();
  await page.mouse.move(cx, cy - 20, { steps: 4 }); // 越过 12px 起步阈值
  await page.mouse.move(b1.x + b1.width / 2, b1.y + 4, { steps: 8 }); // 1 棒中点上方 → gap 0
  await expect(row1).toHaveClass(/is-drop-before/); // 插入线画在 1 棒上方
  await page.mouse.up();
  await expect(row1).not.toHaveClass(/is-drop-before/); // 松手后插入线消失
  await expect.poll(battingOrder).toEqual(['203', '201', '202']);

  // 回归：池卡拖入打线列表末尾 = 追加（既有语义）
  const card204 = page.locator('.lineup-card--pool[data-player-id="204"]');
  const b204 = await card204.boundingBox();
  const lastRow = page.locator('.lineup-batting__row').last();
  const bl = await lastRow.boundingBox();
  await page.mouse.move(b204.x + b204.width / 2, b204.y + b204.height / 2);
  await page.mouse.down();
  await page.mouse.move(b204.x + b204.width / 2 - 30, b204.y + b204.height / 2, { steps: 4 });
  await page.mouse.move(bl.x + bl.width / 2, bl.y + bl.height - 2, { steps: 8 });
  await page.mouse.up();
  await expect.poll(battingOrder).toEqual(['203', '201', '202', '204']);

  // 回归：打线行拖到球场卡槽 = 只改守备位、棒次不动（既有语义）
  const row201 = page.locator('.lineup-batting__row[data-player-id="201"]');
  const b201 = await row201.boundingBox();
  const slot3b = page.locator('.lineup-slot[data-slot-code="3B"]');
  const s3b = await slot3b.boundingBox();
  await page.mouse.move(b201.x + b201.width / 2, b201.y + b201.height / 2);
  await page.mouse.down();
  await page.mouse.move(b201.x + b201.width / 2, b201.y + b201.height / 2 - 20, { steps: 4 });
  await page.mouse.move(s3b.x + s3b.width / 2, s3b.y + s3b.height / 2, { steps: 10 });
  await page.mouse.up();
  await expect(slot3b.locator('.lineup-card--field')).toHaveAttribute('data-player-id', '201');
  await expect.poll(battingOrder).toEqual(['203', '201', '202', '204']);
});
