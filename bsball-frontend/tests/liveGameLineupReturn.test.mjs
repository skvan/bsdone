// 实时录入页「编辑阵容」返回入口与首个 Play 前可编辑门控
// 运行：在 bsball-frontend 目录执行 node --test tests/liveGameLineupReturn.test.mjs
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

test('编辑阵容按钮同时出现在常规头部与全屏工具栏', () => {
  const src = read('../src/views/admin/LiveGame.js');
  assert.equal((src.match(/"编辑阵容"/g) ?? []).length, 2, '编辑阵容按钮应有常规与全屏两个入口');
  assert.equal((src.match(/disabled:c\(firstPlayStarted\)/g) ?? []).length, 2, '两个入口都应按首个 Play 状态禁用');
});

test('首个 Play 判定排除比赛开始日志，并兼容打席与已保存统计', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const helper = slice(src, 'function hasRecordedPlayerActivity(player){', 'const firstPlayStarted=');
  assert.match(helper, /\(b\.ab\?\?0\)>0/, '未检查打者数据');
  assert.match(helper, /\(p\.np\?\?0\)>0/, '未检查投手用球数');
  assert.match(helper, /\(fd\.po\?\?0\)>0/, '未检查守备数据');
  assert.ok(!/\(p\.(?:er|pitchR)\?\?0\)>0/.test(helper), 'ER 派生字段不应作为首 Play 依据');
  const body = slice(src, 'const firstPlayStarted=$(()=>', 'function Ee(e){');
  assert.match(body, /ke\.value/, '未使用实时日志判断');
  assert.match(body, /!text\.includes\("比赛开始"\)/, '比赛开始日志不应视为首个 Play');
  assert.match(body, /Me\.value\.length>0/, '未兼容已存在的打席摘要');
  assert.ok(!/f\.playEvents/.test(body), '不应依赖可能跨场残留的 playEvents');
  assert.match(body, /hasRecordedPlayerActivity/, '未兼容从已保存统计重建的现场');
  assert.match(body, /\(l\.outs\?\?0\)>0/, '未兼容局面出局数');
  assert.match(body, /\(o\.away\.score\?\?0\)>0/, '未兼容比分');
});

test('阵容页门控按实际统计判断，已保存但未开赛仍可编辑', () => {
  const src = read('../src/views/admin/LiveGameLineupBoard.vue');
  const helper = slice(src, 'function hasRecordedGameStat(row)', 'async function loadGame');
  assert.match(helper, /'pa'/, '未检查打席');
  assert.match(helper, /'np'/, '未检查投手用球数');
  assert.match(helper, /'pitchBf'/, '未检查投手面对打者');
  assert.ok(!/'(?:er|earnedR|unearnedR|pendingR|pitchR)'/.test(helper), 'ER 派生字段不应作为已开始录入依据');
  assert.match(helper, /row\?\.\[key\]/, '未按统计字段读取');
  const guard = slice(src, "if (e.status === 'live') {", 'const n = parseLineupDraft');
  assert.match(guard, /some\(hasRecordedGameStat\)/, '未按实际统计判断是否已开始');
  assert.ok(!/battingOrder \?\? 0\) > 0/.test(guard), '不应仅凭 battingOrder 判定已开始录入');
});

test('编辑阵容返回路由、跳过离场确认并先保存撤销态', () => {
  const src = read('../src/views/admin/LiveGame.js');
  const body = slice(src, 'function openLineupEditor(){', 'async function Mo(e){');
  assert.match(body, /firstPlayStarted\.value\|\|he\.value\)return/, '已在录入或保存中仍允许返回');
  assert.match(body, /Ne\.value\.length>0/, '仅应在发生过撤销时先保存');
  assert.match(body, /await hs\(\{/, '返回前未保存当前统计');
  assert.match(body, /throwOnError:!0/, '保存失败时应中止返回');
  assert.match(body, /hc\(\),Pn\.push\(/, '未跳过离场确认或未使用路由 push');
  assert.match(body, /name:"AdminGameLiveLineupGame"/, '未指向先发阵容页路由');
  assert.match(body, /eventId:String\(Re\.value\),gameId:String\(R\.value\)/, '缺少赛事/比赛参数');
});
