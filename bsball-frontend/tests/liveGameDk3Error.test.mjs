// 漏接三振「守备失误上一垒」入口与记账
// 运行：在 bsball-frontend 目录执行 node --test tests/liveGameDk3Error.test.mjs
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

test('漏接三振挥空/目送子菜单均提供守备失误上一垒', () => {
  const src = read('../src/views/admin/LiveGame.js');
  assert.match(src, /I\("dk3:sw:e"\)/, '挥空子菜单缺少失误上一垒');
  assert.match(src, /I\("dk3:lk:e"\)/, '目送子菜单缺少失误上一垒');
  assert.equal((src.match(/"守备失误上一垒"/g) ?? []).length, 2, '失误上一垒文案应各出现一次');
  assert.equal((src.match(/"Safe on First \(E\)"/g) ?? []).length, 2, '英文文案应各出现一次');
});

test('失误上一垒进入守备员选择流程并校验代码', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const body = slice(src, 'function jf(e){', 'function Jf(e){');
  assert.match(body, /"dk3:sw:e"/, '挥空失误代码未纳入白名单');
  assert.match(body, /"dk3:lk:e"/, '目送失误代码未纳入白名单');
  assert.match(body, /e\.endsWith\(":e"\)\?"漏接三振：请点击失误责任守备员"/, '失误分支提示文案缺失');
});

test('守备员点选后按漏接三振失误记账：E + 打者上一垒 + 来源 ERROR', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const body = slice(src, 'function dk3ReachOnError(e,t){', 'function jf(e){');
  assert.match(body, /creditFieldingError\(Vt\(t\)\)/, '未给所选守备员记 E');
  assert.match(body, /l\.runners\[1\]=a/, '打者未上一垒');
  assert.match(body, /a\.origin="ERROR"/, '打者上垒来源未标为 ERROR');
  assert.match(body, /ti\(\{origin:"ERROR"\}\)/, '被迫进垒跑者未按失误来源推进');
  assert.match(body, /Je\("DK3_REACH_E",y\)/, '打席结果代码缺失');
  assert.match(body, /\(K\+E\)/, '日志未标注 K+E');
  assert.match(body, /stats\.batting\.so=/, '未给打者记三振');
  assert.match(body, /stats\.batting\.ab=/, '未给打者记打数');
  assert.match(body, /pitchSo=/, '未给投手记夺三振');
  assert.ok(!/l\.outs\+\+/.test(body), '失误上一垒不应增加出局数');
});

test('状态机把挥空/目送失误分支接到记账函数', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const body = slice(src, 'function Jf(e){', 'function ko(e,t){');
  assert.match(body, /case"dk3:sw:e":dk3ReachOnError\(!1,e\)/, '挥空失误分支未接线');
  assert.match(body, /case"dk3:lk:e":dk3ReachOnError\(!0,e\)/, '目送失误分支未接线');
});

test('守备员选择提示按失误分支区分文案', () => {
  const src = read('../src/views/admin/LiveGame.js');
  assert.match(
    src,
    /e\.type==="DK3_FIELDER_PICK"\)return e\.dk3Code\?\.endsWith\(":e"\)\?"漏接三振：请点击失误责任守备员":"漏接三振：请选择完成处理的守备员"/
  );
});
