// #339 冒烟（本地 mock，无后端）：秩序册卡片视图「守位 / 名字 / 背号」三行水平统一；
// 无守位时占一行空行（不显示 —）；多守位长文本单行省略、不换行（悬浮 title 可看全文）；
// 列表视图守位列空白（不显示破折号，背号列维持 — 兜底）。
// 运行：npm run test:e2e（E2E_PORT 可指定端口）
import { test, expect } from '@playwright/test';

test.use({ viewport: { width: 1400, height: 950 } });

test('#339 秩序册：无守位卡仍占一行（三行对齐）；列表守位列不显示破折号', async ({ page }) => {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem('admin_user::bs-ball', JSON.stringify({
      id: 1, tenantCode: 'bs-ball', superAdmin: true, username: 'local-e2e'
    }));
  });
  await page.addInitScript(() => localStorage.setItem('bs-ball-locale', 'zh-CN'));

  await page.route('**/bsball-server/**', async (route) => {
    const path = new URL(route.request().url()).pathname.replace('/bsball-server', '');
    let data = [];
    if (path === '/event/1') data = { id: 1, name: 'Local E2E Event', season: '2026' };
    else if (path === '/event/1/teams') data = [
      {
        teamId: 2, teamName: '雷霆队', teamLogo: null, status: 'confirmed',
        playerCount: 2, rosterSubmittedAt: '2026-10-15T10:00:00', rosterLocked: false
      },
      {
        teamId: 3, teamName: '疾风队', teamLogo: null, status: 'confirmed',
        playerCount: 0, rosterSubmittedAt: null, rosterLocked: false
      }
    ];
    else if (path === '/event/1/teams/2/roster') data = [
      // 张三多守位（长文本边界样本）：覆盖「单行省略不换行」；李四无守位：覆盖「空行占位」
      { playerId: 1, name: '张三', number: '7', positions: ['P', 'C', 'SS', 'LF', 'CF', 'RF', '1B', '2B'], avatar: null },
      { playerId: 2, name: '李四', number: '11', positions: [], avatar: null }
    ];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/events/1/order-book');

  // 卡片视图（默认）：已确认且已提交的雷霆队 2 张卡
  const cards = page.locator('.book-team .ob-player-card');
  await expect(cards).toHaveCount(2);

  // 无守位卡（李四）：守位行为空文本（不显示 '—'）；多守位卡（张三）：全文入 DOM（视觉单行省略）
  await expect(cards.nth(1).locator('.ob-player-position')).toHaveText('');
  await expect(cards.nth(0).locator('.ob-player-position')).toHaveText('P / C / SS / LF / CF / RF / 1B / 2B');

  // 几何：无守位也占一行、多守位长文本不换行；两卡「守位 / 名字 / 背号」行顶边各自对齐
  const geom = await cards.evaluateAll((els) =>
    els.map((el) => {
      const top = (sel) => Math.round(el.querySelector(sel).getBoundingClientRect().top);
      const height = (sel) => Math.round(el.querySelector(sel).getBoundingClientRect().height);
      return { posTop: top('.ob-player-position'), posH: height('.ob-player-position'), nameTop: top('.ob-player-name'), numTop: top('.ob-player-number') };
    })
  );
  expect(geom[1].posH).toBeGreaterThan(8); // 空守位行仍有高度（min-height 占位）
  expect(geom[1].posH).toBe(geom[0].posH); // 有/无守位行高一致
  expect(geom[1].posTop).toBe(geom[0].posTop);
  expect(geom[1].nameTop).toBe(geom[0].nameTop);
  expect(geom[1].numTop).toBe(geom[0].numTop);

  // 列表视图：无守位行守位列空白（非 '—'）；有守位行正常显示
  await page.locator('.head-actions .el-radio-button').nth(1).click();
  const zhangRow = page.getByRole('row', { name: /张三/ });
  const liRow = page.getByRole('row', { name: /李四/ });
  await expect(zhangRow.getByRole('cell').nth(3)).toHaveText('P / C / SS / LF / CF / RF / 1B / 2B');
  await expect(liRow.getByRole('cell').nth(3)).toHaveText('');
  await expect(liRow.getByRole('cell').nth(2)).toHaveText('11'); // 背号列不受影响
});
