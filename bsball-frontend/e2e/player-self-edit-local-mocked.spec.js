import { test, expect } from '@playwright/test';

// 已认领球员在管理台编辑本人档案：进入「个人资料模式」，保存走 SELF 通道。
// 运行：E2E_PORT=3037 npx playwright test e2e/player-self-edit-local-mocked.spec.js
const PLAYER_ID = 74;
const SELF_USER_ID = 14;

const claimedPlayer = {
  id: PLAYER_ID,
  userId: SELF_USER_ID,
  tenantId: 1,
  name: '蔡子杰',
  number: '99',
  teamId: 3,
  positions: ['1B'],
  profilePositions: ['1B'],
  status: 'active',
  throwHand: 'R',
  batHand: 'R',
  avatar: '',
  bgImages: [],
  contactPhone: '',
  contactEmail: '',
  intro: '',
  teamEntries: [
    { id: 901, teamId: 3, teamName: '野钻', number: '99', positions: ['1B'], current: true }
  ]
};

test('本人已认领档案：管理台编辑走 SELF 通道，名册字段只读', async ({ page }) => {
  await page.addInitScript(() => {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('bs-ball-live-'))
      .forEach((key) => localStorage.removeItem(key));
    localStorage.setItem('admin_token::bs-ball', 'local-e2e-token');
    localStorage.setItem(
      'admin_user::bs-ball',
      JSON.stringify({
        id: 14,
        tenantCode: 'bs-ball',
        username: 'chad',
        superAdmin: false,
        tenantAdmin: false,
        menuIds: [22, 2]
      })
    );
  });

  let savedPayload = null;
  await page.route('**/bsball-server/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/bsball-server', '');
    let data = [];

    if (path === '/dict/type/list' || path === '/dict/data/list') {
      data = { list: [], total: 0 };
    } else if (path === '/team/select-options') {
      data = [{ id: 3, name: '野钻', shortName: '野钻' }];
    } else if (path === '/player/list') {
      data = { list: [claimedPlayer], total: 1 };
    } else if (request.method() === 'PUT' && path === `/account/player-profile/${PLAYER_ID}`) {
      savedPayload = request.postDataJSON();
      data = { ...claimedPlayer, ...savedPayload };
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    });
  });

  await page.goto('/bs-ball/bs-ball/admin/players');

  const row = page.locator('.el-table__row', { hasText: '蔡子杰' }).first();
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: '编辑' }).click();

  const dialog = page.locator('.player-edit-dialog');
  await expect(dialog.getByText('编辑本人档案')).toBeVisible();
  await expect(dialog.getByText('球队经历、背号、简称、在役状态不可在此修改')).toBeVisible();

  // 名册字段只读：在役状态、简称、球队经历（球队/背号/守备位置/当前球队）
  await expect(
    dialog.locator('.el-form-item', { hasText: '在役状态' }).locator('.el-select__wrapper')
  ).toHaveClass(/is-disabled/);
  await expect(dialog.locator('.el-form-item', { hasText: '简称' }).locator('input')).toBeDisabled();
  await expect(dialog.locator('.pte-editor .el-select__wrapper.is-disabled')).toHaveCount(2);
  await expect(dialog.locator('.pte-editor .el-checkbox.is-disabled')).toHaveCount(1);
  await expect(dialog.locator('.pte-editor button:has-text("添加球队经历")')).toHaveCount(0);
  await expect(dialog.locator('.pte-editor button:has-text("删除")')).toHaveCount(0);

  // 守备位置（个人展示）可编辑
  const selfPositions = dialog.locator('.player-self-positions');
  await expect(selfPositions.locator('.el-select')).not.toHaveClass(/is-disabled/);
  await selfPositions.locator('.el-select').click();
  await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '捕手' })
    .first()
    .click();
  await page.keyboard.press('Escape');

  await dialog.locator('input[placeholder="联系电话"]').fill('13800000000');

  const requestPromise = page.waitForRequest(
    (req) => req.method() === 'PUT' && req.url().includes(`/bsball-server/account/player-profile/${PLAYER_ID}`)
  );
  await dialog.getByRole('button', { name: '确定' }).click();
  await requestPromise;
  await expect(page.getByText('修改成功')).toBeVisible();

  expect(savedPayload).not.toBeNull();
  expect(savedPayload.contactPhone).toBe('13800000000');
  expect(savedPayload.profilePositions).toEqual(expect.arrayContaining(['C']));
  expect(savedPayload).not.toHaveProperty('positions');
  expect(savedPayload).not.toHaveProperty('number');
  expect(savedPayload).not.toHaveProperty('status');
  expect(savedPayload).not.toHaveProperty('shortName');
  expect(savedPayload).not.toHaveProperty('teamEntries');
  expect(savedPayload).not.toHaveProperty('teamId');
});
