/**
 * #444-A 重进现场路由与快照同步韧性
 *
 * 背景（2026-10-27 复查）：换人史 lineupStarterIds/slotSubHistory 只在服务端 snapshotJson 里，
 * 重进入口 _c()/bc() 已是「有快照→Vl（完整还原）、无快照→Do（仅按 battingOrder 重建）」。
 * 真正的缺口是 Zr 里 saveLiveSnapshot 的失败被 catch{} 静默吞掉：统计存了、快照没同步，
 * 下次跨设备/清缓存重进就会悄悄退到 Do()，丢「当前在场占用者」。
 *
 * 本测试只验 #444-A 改动的承重行为（Zr/pushLiveSnapshot），手法沿用同族测试：
 * 平衡处理无关，这里直接按函数名切片取原文、绑上注入的依赖后真的执行。
 *
 * 运行：node --test tests/liveGameReentrySnapshotRouting444.test.mjs
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SRC_PATH = process.env.LIVEGAME_PATH
  || join(dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js');
const src = fs.readFileSync(SRC_PATH, 'utf8').replace(/\r\n/g, '\n');

// 从脏标声明切到 ZrLite 之前：正好覆盖 snapshotSyncFailed + pushLiveSnapshot + Zr 三件套
const start = src.indexOf('let snapshotSyncFailed');
const end = src.indexOf('async function ZrLite');
assert(start >= 0 && end > start, '找不到 snapshotSyncFailed..ZrLite 区间，元件结构可能已变动');
const code = src.slice(start, end).trimEnd();

// Zr 依赖：R(比赛id ref)、F(可录入 flag)、Tn/to(存前刷新)、pa(构造快照对象)、Fe(消息)、ma(api)
const make = new Function('R', 'F', 'Tn', 'to', 'pa', 'Fe', 'ma',
  'return (function(){' + code + '\nreturn {Zr, pushLiveSnapshot, getFlag: () => snapshotSyncFailed};})()');

/** 造一个可控的 saveLiveSnapshot：state.failRemaining>0 时抛错并自减（可中途切换模拟恢复） */
function ctx({ failRemaining = 0, canSave = true } = {}) {
  const calls = { save: 0, warn: 0 };
  const state = { failRemaining };
  const ma = {
    saveLiveSnapshot: async () => {
      calls.save++;
      if (state.failRemaining > 0) { state.failRemaining--; throw new Error('network'); }
    },
  };
  const Fe = { warning: (m) => { calls.warn++; calls.lastWarn = m; } };
  const env = {
    R: { value: 137 },
    F: { value: canSave },
    Tn: () => {},
    to: () => {},
    pa: () => ({ ok: 1 }),
    Fe,
    ma,
  };
  const api = make(env.R, env.F, env.Tn, env.to, env.pa, env.Fe, env.ma);
  return { api, calls, state };
}

test('#444-A pushLiveSnapshot：首次即成功只调用一次、返回 true', async () => {
  const { api, calls } = ctx({ failRemaining: 0 });
  const ok = await api.pushLiveSnapshot({ a: 1 });
  assert.equal(ok, true);
  assert.equal(calls.save, 1, '成功路径不该触发多余重试');
});

test('#444-A pushLiveSnapshot：首次失败、重试成功 → 返回 true 且共调用两次', async () => {
  const { api, calls } = ctx({ failRemaining: 1 });
  const ok = await api.pushLiveSnapshot({ a: 1 });
  assert.equal(ok, true, '一次失败后重试应救回');
  assert.equal(calls.save, 2);
});

test('#444-A Zr：连续两次都失败 → 置脏标并提示一次（不静默吞掉）', async () => {
  const { api, calls } = ctx({ failRemaining: 2 });
  await api.Zr();
  assert.equal(api.getFlag(), true, '同步失败必须留下可见状态');
  assert.equal(calls.warn, 1, '要有一次告警，而不是 catch{} 空吞');
});

test('#444-A Zr：脏标已置时再次自动保存失败不重复刷屏（节流）', async () => {
  const { api, calls, state } = ctx({ failRemaining: Infinity });
  await api.Zr();
  await api.Zr();
  await api.Zr();
  assert.equal(api.getFlag(), true);
  assert.equal(calls.warn, 1, '同一故障期间只提示一次');
  assert.ok(state.failRemaining === Infinity, '持续失败模式');
});

test('#444-A Zr：同一闭包内 失败→恢复成功清脏标→再次失败可重新告警', async () => {
  const { api, calls, state } = ctx({ failRemaining: Infinity });
  // 1) 持续失败：置脏标 + 告警一次
  await api.Zr();
  assert.equal(api.getFlag(), true);
  assert.equal(calls.warn, 1);
  // 2) 故障恢复（不再抛错）：成功后必须清除脏标、且不再告警
  state.failRemaining = 0;
  await api.Zr();
  assert.equal(api.getFlag(), false, '同步成功应清脏标');
  assert.equal(calls.warn, 1, '成功不新增告警');
  // 3) 再次故障：脏标既已清，应能重新提示一次（证明 reset 真的生效）
  state.failRemaining = Infinity;
  await api.Zr();
  assert.equal(api.getFlag(), true);
  assert.equal(calls.warn, 2, '恢复后再失败要能重新提示，而非被旧脏标永久静音');
});

test('#444-A Zr：不可录入（F=false）时直接返回，不写快照也不告警', async () => {
  const { api, calls } = ctx({ canSave: false });
  await api.Zr();
  assert.equal(calls.save, 0);
  assert.equal(calls.warn, 0);
});
