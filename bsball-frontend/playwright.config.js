import { defineConfig, devices } from '@playwright/test';

const port = process.env.E2E_PORT || '3000';
const baseURL = process.env.E2E_BASE_URL || `http://127.0.0.1:${port}/bs-ball/`;

export default defineConfig({
  // #511 防呆：端口归属检查放在 npm pretest:e2e / pretest:e2e:local（见 package.json），
  // 必须早于 Playwright 的 webServer 启动；不要改回 globalSetup（Playwright 会先起 server）。
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL,
    locale: 'zh-CN',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ? 'off' : 'retain-on-failure',
    launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH }
      : {},
    ...devices['Desktop Chrome']
  },
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: `npm run dev -- --host 127.0.0.1 --port ${port} --strictPort`,
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 30_000
  },
});
