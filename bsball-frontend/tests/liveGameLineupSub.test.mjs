// 换人模式：进攻替补拖到打线栏替换指定棒次击球员（静态接线护栏）。
// 运行：node --test tests/liveGameLineupSub.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const LIVE = read('../src/views/admin/LiveGame.js');
const DRAG = read('../src/utils/liveSubDrag.js');
const CSS = read('../src/styles/legacy/live-game.css');

test('打线栏作为进攻替补落点：布局收集/目标选择/悬浮高亮', () => {
  assert.ok(LIVE.includes('lineupRects'), '布局未收集打线栏');
  assert.ok(LIVE.includes('.live-lineup-rail__item'), '未查询打线栏');
  assert.ok(LIVE.includes('hit.kind==="lineup"?Ll.value.find'), '未按落点找目标击球员');
  assert.ok(LIVE.includes('const liveLineupHoverId='), '缺少打线落点悬浮高亮');
  assert.ok(LIVE.includes('"live-lineup-rail__item--drop":c(liveLineupHoverId)===Number(d.id)'), '模板未绑定落点高亮');
  assert.ok(DRAG.includes("targetKind === 'batter' || targetKind === 'lineup'"), '合法性矩阵未支持打线落点');
  assert.ok(DRAG.includes("['base', 'batter', 'lineup']"), '可落目标未含打线');
  assert.ok(CSS.includes('.live-lineup-rail__item--drop'), '缺少落点样式');
});
