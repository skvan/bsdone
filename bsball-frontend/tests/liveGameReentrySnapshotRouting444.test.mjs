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
const make = new Function('R', 'F', 'Tn', 'to', 'pa', 'Fe', 'ma', 'lgText',
  'return (function(){' + code + '\nreturn {Zr, pushLiveSnapshot, getFlag: () => snapshotSyncFailed};})()');

/** 造一个可控的 saveLiveSnapshot：state.failRemaining>0 时抛错并自减（可中途切换模拟恢复）
 *  businessFail 给定时改抛「带 apiCode 的业务错误」（businessCode 缺省 409）——后端快照覆盖
 *  守门的 409 就是这个形状（GlobalExceptionHandler 把 BusinessException 包成 HTTP 200 +
 *  body.code，request.js 再抛 ApiBizError），E2E 之外在这里钉一层。 */
function ctx({ failRemaining = 0, canSave = true, businessFail = null, businessCode = 409 } = {}) {
  const calls = { save: 0, warn: 0, err: 0 };
  const state = { failRemaining, businessFail, businessCode };
  const ma = {
    saveLiveSnapshot: async () => {
      calls.save++;
      if (state.businessFail) {
        const error = new Error(state.businessFail);
        error.apiCode = state.businessCode;
        throw error;
      }
      if (state.failRemaining > 0) { state.failRemaining--; throw new Error('network'); }
    },
  };
  const Fe = {
    // 两条通道都可能是字符串或带 duration 的选项对象（长文案要读得完），测试里归一成字符串再断
    warning: (o) => { calls.warn++; calls.lastWarn = typeof o === 'string' ? o : o.message; },
    error: (o) => { calls.err++; calls.lastError = typeof o === 'string' ? o : o.message; }
  };
  const env = {
    R: { value: 137 },
    F: { value: canSave },
    Tn: () => {},
    to: () => {},
    pa: () => ({ ok: 1 }),
    Fe,
    ma,
  };
  const api = make(env.R, env.F, env.Tn, env.to, env.pa, env.Fe, env.ma, (key) => key);
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

// #713：快照覆盖守门的 409 不能被当成「网络抖了一下」
test('#713 pushLiveSnapshot：服务器点名拒收（409）时不重试，重试也只会再撞一次', async () => {
  const { api, calls } = ctx({ businessFail: '已拒绝覆盖', failRemaining: Infinity });
  const ok = await api.pushLiveSnapshot({ a: 1 });
  assert.equal(ok, false);
  assert.equal(calls.save, 1, '4xx 不是网络问题，白花 800ms 再撞一次没意义');
});

test('#713 Zr：拒收时把服务器给的原因原样抬出来，不被「换人在场状态会丢失」顶掉', async () => {
  const msg = '提交的快照逐棒记录（2 条）少于服务器现有记录（3 条），已拒绝覆盖；用「从云端恢复录入现场」可回到服务器那份继续记';
  const { api, calls } = ctx({ businessFail: msg });
  await api.Zr();
  assert.equal(api.getFlag(), true, '被拒收同样是没同步上去，脏标要置');
  assert.equal(calls.err, 1, '拒收走 error 通道（它是服务器确定的决定，不是可重试的暂态）');
  assert.equal(calls.lastError, msg, '原因要原样到人眼里，因为下一步动作就写在里面');
  assert.equal(calls.warn, 0, '不能再被那句无关的通用文案顶掉');
});

test('#713 Zr：拒收转成功后要清掉原因，之后的真故障回到通用文案而不是旧拒收词', async () => {
  const { api, calls, state } = ctx({ businessFail: '旧一轮的拒收原因', failRemaining: Infinity });
  await api.Zr();
  assert.equal(calls.err, 1);
  // 服务器不再点名拒收，且这一发成功：清脏标的同时必须清原因，
  // 否则下次真网络故障会说着一句已经不成立的拒收话。
  state.businessFail = null;
  state.failRemaining = 0;
  await api.Zr();
  assert.equal(api.getFlag(), false, '同步成功应清脏标');
  // 再撞真网络故障（无 apiCode）：走的必须还是原来的重试＋通用文案那一路
  state.failRemaining = Infinity;
  await api.Zr();
  assert.equal(calls.err, 1, '真故障不能拿旧拒收原因走 error 通道播报');
  assert.equal(calls.warn, 1, '回到通用告警文案');
  assert.equal(calls.save, 4, '次数＝1 拒收（不重试）＋1 成功＋2 网络失败重试');
});

// 评审指出：限流不是拒收。RateLimitFilter 对 /game 这个端点会回 HTTP 429 + body.code 429
// 「请求过于频繁，请稍后再试」，而 request.js 会把 body.code 挂成 apiCode：拿 apiCode<500
// 当拒收就会把「明说稍后再试」的暂态归为不重试，还把限流文案以 error 级抬给用户。
test('#713 限流 429 不算拒收：仍重试一次，并走通用文案', async () => {
  const { api, calls } = ctx({ businessFail: '请求过于频繁，请稍后再试', businessCode: 429 });
  const ok = await api.pushLiveSnapshot({ a: 1 });
  assert.equal(ok, false);
  assert.equal(calls.save, 2, '429 是告诉你可以再试一次，不能当成服务器拒收');
  await api.Zr();
  assert.equal(calls.err, 0, '限流不是存档拒收，不走“点名拒收”那一条 error 文案');
  assert.equal(calls.warn, 1, '维持原有的通用提醒');
});

test('#713 Zr：先网络抖动、后被拒收——脏标不能把后来的拒收原因压回无声', async () => {
  // 脏标只在成功时清（#444-A 的刷屏节流），所以「失败过一次就什么都不播」会把 #713
  // 要解决的「409 被吞」在已经有过一次抖动的会话里带回来。去重得按「这次要播的话」。
  const { api, calls, state } = ctx({ failRemaining: Infinity });
  await api.Zr();
  assert.equal(calls.warn, 1, '第一拍：网络抖动→通用文案');
  state.failRemaining = 0;
  state.businessFail = '提交的快照逐棒记录少于服务器现有记录，已拒绝覆盖';
  await api.Zr();
  assert.equal(calls.err, 1, '抖动过后的拒收同样要看得见');
  assert.match(calls.lastError, /已拒绝覆盖/);
});

test('#713 Zr：上一拍的拒收原因不能顶替这一拍的网络故障文案', async () => {
  const { api, calls, state } = ctx({ businessFail: '旧一拍的拒收原因' });
  await api.Zr();
  assert.equal(calls.err, 1);
  // 服务器不再点名拒收，这一拍只撞网络：说的必须是被丢下的那句「未能同步」，
  // 而不是拿着已经不成立的旧拒收话当现况
  state.businessFail = null;
  state.failRemaining = Infinity;
  await api.Zr();
  assert.equal(calls.err, 1, '旧拒收原因不能泄到这一拍');
  assert.equal(calls.warn, 1, '真故障回到通用文案');
  assert.equal(calls.save, 3, '1 拒收（不重试）＋2 网络失败重试');
});
