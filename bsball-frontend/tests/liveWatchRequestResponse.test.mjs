// 静态断言：观赛页主动请求快照时，录入页直接回复 e.source 并写本地缓存
// 运行：在 bsball-frontend 目录执行  node --test tests/liveWatchRequestResponse.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

// 读文件并归一化行尾（Windows 检出的 CRLF 会让多行字符串匹配失效）
const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const LIVE = '../src/views/admin/LiveGame.js';

test('快照请求分支直接回复请求窗口，不再只依赖窗口集合', () => {
  const src = read(LIVE);
  const start = src.indexOf('function Ws(e){');
  const end = src.indexOf('let Fn=null;', start);
  assert.ok(start >= 0 && end > start, '未找到消息处理器 Ws');
  const body = src.slice(start, end);
  const reqStart = body.indexOf('if(t?.type===x_){');
  const reqCall = body.indexOf('watchRespondSnapshot(e.source)');
  assert.ok(reqStart >= 0 && reqCall > reqStart, 'watchRespondSnapshot 不在快照请求分支内');
  assert.equal(body.split('watchRespondSnapshot').length - 1, 1, '快照请求分支应只调用一次 watchRespondSnapshot');
  assert.ok(body.includes('if(!a||Number(t.gameId??a)!==Number(a))return;'), '快照请求缺少 gameId 校验');
});

test('watchRespondSnapshot 直接 postMessage 并落本地缓存', () => {
  const src = read(LIVE);
  const start = src.indexOf('function watchRespondSnapshot(e){');
  const end = src.indexOf('function Bi(e){', start);
  assert.ok(start >= 0 && end > start, '未找到 watchRespondSnapshot');
  const body = src.slice(start, end);
  assert.ok(body.includes('if(!t||!Re.value||!F.value)return;'), '缺少录入中/比赛 ID 守卫');
  assert.ok(body.includes('includeUndoStacks:!1'), '快照未排除撤销栈');
  assert.ok(body.includes('type:R_,gameId:Number(t),payload:a'), '缺少快照消息结构');
  assert.ok(body.includes('e?.postMessage(s,window.location.origin)'), '未直接回复请求窗口');
  assert.ok(body.indexOf('e?.postMessage(s,window.location.origin)') < body.indexOf('gl(a),Bi(a)'), '应先直接回复请求窗口，再同步其他观赛窗口/写缓存');
  assert.ok(body.includes('gl(a),Bi(a)'), '未同步其他观赛窗口/未写本地缓存');
  assert.ok(body.includes('[LiveDebug] watchRespondSnapshot'), '缺少请求响应诊断日志');
});
