// 跑者拖拽进垒原因 → 投捕统计入账（比赛 237 第一局捕逸日志有、PB 未记）
// 运行：node --test tests/liveGameAdvanceReasonStats.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

function slice(src, startMarker, endMarker) {
  const start = src.indexOf(startMarker);
  assert.ok(start >= 0, `缺少 ${startMarker}`);
  const end = src.indexOf(endMarker, start);
  assert.ok(end > start, `缺少 ${endMarker}`);
  return src.slice(start, end);
}

test('拖拽进垒选择「捕逸 (PB)」时给捕手记 PB', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const body = slice(src, 'function Sp(e){', 'function kp(e){');
  assert.match(body, /e\.id==="safe_wp"/, '缺少暴投入账（对照路径）');
  assert.match(body, /e\.id==="safe_sb"/, '缺少盗垒/捕手 SB 入账（对照路径）');
  assert.match(body, /e\.id==="safe_pb"&&paceCatcherStat\("pb"\)/, '捕逸进垒未给捕手记 PB');
});

test('漏接三振捕逸仍给捕手记 PB（保留入口）', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const body = slice(src, 'function ii(e,t,a){', 'function So(e,t){');
  assert.match(body, /paceCatcherStat\("pb"\)/, '漏接三振捕逸未记 PB');
});

test('无 UI 入口的 evt:PB 死分支已移除', () => {
  const src = read('../src/views/admin/LiveGame.js');
  assert.ok(!src.includes('case"evt:PB"'), '仍保留 evt:PB 分发');
  assert.ok(!src.includes('Xt("PB")'), '仍保留 Xt("PB") 调用');
  assert.ok(!/e==="PB"\)\{/.test(src), '仍保留 PB 投球事件分支');
});
