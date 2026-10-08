import { test, expect } from '@playwright/test';

const required = ['E2E_TENANT', 'E2E_USERNAME', 'E2E_PASSWORD', 'E2E_EVENT_ID', 'E2E_GAME_ID'];
for (const name of required) {
  if (!process.env[name]) throw new Error(`Missing required E2E environment variable: ${name}`);
}

test('LiveGame saves a browser-entered game state to the backend', async ({ page }) => {
  await page.goto(`${process.env.E2E_TENANT}/admin/login`);
  await page.getByRole('textbox').nth(0).fill(process.env.E2E_USERNAME);
  await page.getByRole('textbox').nth(1).fill(process.env.E2E_PASSWORD);
  await page.getByRole('button', { name: /登入|登录|Sign in|Login/i }).click();
  await expect(page).not.toHaveURL(/\/admin\/login/);

  const saveRequest = page.waitForRequest((request) =>
    request.method() === 'POST' &&
    request.url().includes(`/api/game/${process.env.E2E_GAME_ID}/save-live`)
  );
  await page.goto(
    `${process.env.E2E_TENANT}/admin/events/${process.env.E2E_EVENT_ID}/games/${process.env.E2E_GAME_ID}/live`
  );
  await expect(page.getByText(/实时录入|即時錄入|Live Game/i).first()).toBeVisible();
  await page.getByRole('button', { name: /保存比赛|保存比賽|Save Game/i }).click();

  const request = await saveRequest;
  const body = request.postDataJSON();
  expect(body.game).toBeTruthy();
  expect(Array.isArray(body.stats)).toBeTruthy();
  expect(body.game.status).toMatch(/live|final/i);

  const snapshotResponse = await page.request.get(
    `/api/game/${process.env.E2E_GAME_ID}/live-snapshot?_t=${Date.now()}`
  );
  expect(snapshotResponse.ok()).toBeTruthy();
  const snapshot = await snapshotResponse.json();
  expect(snapshot.data?.snapshotJson ?? snapshot.snapshotJson).toBeTruthy();
});
