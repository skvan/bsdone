// 门户存储桥防回环（2026-10-09 生产 429 实证修复）：
//   ① 断链——storage 事件触发的拉取不写回缓存（新旧版互写 ping-pong 根源）
//   ② 去抖——事件风暴合并为一次拉取
//   ③ 写前比较——同值跳过 setItem（减少跨标签事件）
// 运行：node --test tests/portalStorageBridgeSync.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createSingleFlightDebounce } from '../src/utils/singleFlightDebounce.js';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

test('去抖调度：窗口内连续多次 schedule 只执行一次', async () => {
  let calls = 0;
  const s = createSingleFlightDebounce(() => {
    calls += 1;
  }, 30);
  s.schedule();
  s.schedule();
  s.schedule();
  await sleep(80);
  assert.equal(calls, 1);
});

test('去抖调度：cancel 后不触发；窗口结束后可再次调度', async () => {
  let calls = 0;
  const s = createSingleFlightDebounce(() => {
    calls += 1;
  }, 30);
  s.schedule();
  s.cancel();
  await sleep(80);
  assert.equal(calls, 0);
  s.schedule();
  await sleep(80);
  assert.equal(calls, 1);
});

test('静态门禁：桥的 app-config 事件分支走去抖且拉取不写回缓存（断链）', () => {
  const src = fs.readFileSync(new URL('../src/composables/usePortalStorageBridge.js', import.meta.url), 'utf8');
  assert.ok(src.includes('createSingleFlightDebounce'), '桥必须使用去抖调度器');
  assert.ok(src.includes('skipCacheWrite: true'), '事件拉取必须 skipCacheWrite（断链根因）');
  // 不得在 storage 事件分支内直调无参 fetchPortalSettings()（逐事件直发＝旧实现，防回退）
  const branch = src.slice(src.indexOf('function onStorageChange'), src.indexOf('let refCount'));
  assert.ok(branch.length > 0, '未能定位 onStorageChange 分支段落');
  assert.ok(!/fetchPortalSettings\(\s*\)/.test(branch), '事件分支内不得无参直调 fetchPortalSettings()（须经调度器 + skipCacheWrite）');
});

test('静态门禁：appConfig.load 支持 skipCacheWrite 且写前比较（同值跳过 setItem）', () => {
  const src = fs.readFileSync(new URL('../src/stores/appConfig.js', import.meta.url), 'utf8');
  assert.ok(src.includes('skipCacheWrite'), 'load 必须支持 skipCacheWrite');
  assert.match(src, /getItem\(key\)\s*!==\s*next/, '写前必须比较现值（相同跳过 setItem）');
});
