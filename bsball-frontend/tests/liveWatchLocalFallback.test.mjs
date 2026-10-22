// 静态断言：实时观赛「本地实时」推送兜底（storage 跨标签页 + visibilitychange 前台即时刷新）
// 运行：在 bsball-frontend 目录执行  node --test tests/liveWatchLocalFallback.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

// 读文件并归一化行尾（Windows 检出的 CRLF 会让多行字符串匹配失效）
const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const WATCH = '../src/views/admin/LiveGameWatch.js';

test('storage 事件会识别观赛快照键并立即应用', () => {
  const src = read(WATCH);
  const start = src.indexOf('function tt(e){');
  const end = src.indexOf('// #211', start);
  assert.ok(start >= 0 && end > start, '未找到 storage 处理器 tt');
  const body = src.slice(start, end);
  assert.ok(body.includes('if(!$.value)return;'), 'storage 快照未限定本地实时模式');
  assert.ok(body.includes('if(e.key!==t&&e.key!==mt(ue.value,void 0))return;'), '未兜底处理录入页写入的观赛快照键');
  assert.ok(body.includes('a=JSON.parse(e.newValue)'), '未解析 storage 快照');
  assert.ok(body.includes('const l=Number(a?.v??1);'), '未校验快照版本');
  assert.ok(body.includes('xe(a).then('), 'storage 快照未走 applySnapshot 同一路径');
  assert.ok(body.includes('G.value="cache"'), 'storage 快照未标记为本地缓存同步');
  assert.ok(body.includes('Fe.value=Date.now()'), 'storage 快照应用后未重置轮询节流');
});

test('切回前台立即补读一次，并支持绕过 4s 节流', () => {
  const src = read(WATCH);
  assert.ok(src.includes('function watchOnVisibility(){'), '缺少 visibilitychange 处理函数');
  assert.ok(src.includes('if(document.visibilityState!=="visible"||Q.value?.status==="final")return;'), '未判断页面可见状态/终局');
  assert.ok(src.includes('$.value?We(!0).then('), '本地模式切回前台未强制补读');
  assert.ok(src.includes('):ke(!0)'), '服务器模式切回前台未强制拉取');
  assert.ok(src.includes('async function We(force){'), 'We 未提供强制刷新参数');
  assert.ok(src.includes('async function ke(force){'), 'ke 未提供强制刷新参数');
  assert.ok(src.includes('if(!force&&Date.now()-Fe.value<4e3)return!1;'), 'We 的强制参数未绕过节流');
  assert.ok(src.includes('if($.value||(!force&&Date.now()-Fe.value<4e3))return;'), 'ke 的强制参数未绕过节流');
});

test('visibilitychange 监听在挂载/卸载成对出现', () => {
  const src = read(WATCH);
  assert.ok(src.includes('document.addEventListener("visibilitychange",watchOnVisibility)'), '未注册 visibilitychange');
  assert.ok(src.includes('document.removeEventListener("visibilitychange",watchOnVisibility)'), '卸载时未移除 visibilitychange');
});
