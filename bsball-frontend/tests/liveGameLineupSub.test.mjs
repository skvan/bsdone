// 换人模式：进攻替补拖/点选打线栏替换指定棒次击球员；iPad 打线栏长驻（静态接线护栏）。
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
  assert.ok(LIVE.includes('document.querySelectorAll(".live-lineup-rail")'), '布局未按打线栏实例逐栏收集');
  assert.ok(LIVE.includes('hit.kind==="lineup"?Ll.value.find'), '未按落点找目标击球员');
  assert.ok(LIVE.includes('const liveLineupHoverId='), '缺少打线落点悬浮高亮');
  assert.ok(LIVE.includes('"live-lineup-rail__item--drop":c(liveLineupHoverId)===Number(d.id)'), '模板未绑定落点高亮');
  assert.ok(LIVE.includes('liveBenchConsumePendingClicks({kind:"lineup",playerId:Number(d.id)})'), '缺少打线栏点选落点');
  const clickWiring = LIVE.split('liveBenchConsumePendingClicks({kind:"lineup",playerId:Number(d.id)})').length - 1;
  assert.ok(clickWiring >= 2, '原打线栏与 iPad 固定栏都应接点选');
  assert.ok(DRAG.includes("targetKind === 'batter' || targetKind === 'lineup'"), '合法性矩阵未支持打线落点');
  assert.ok(DRAG.includes("['base', 'batter', 'lineup']"), '可落目标未含打线');
  assert.ok(CSS.includes('.live-lineup-rail__item--drop'), '缺少落点样式');
});

test('iPad 打线栏长驻：独立固定栏 + 媒体查询', () => {
  assert.ok(LIVE.includes('live-lineup-rail live-lineup-rail--fixed'), '缺少 iPad 固定打线栏');
  assert.ok(CSS.includes('.live-lineup-rail--fixed{display:none}'), '固定打线栏默认应隐藏');
  assert.ok(CSS.includes('@media (min-width:768px) and (max-width:1279px)'), '缺少 iPad 媒体查询');
  assert.ok(CSS.includes('.live-content--sub-mode .live-lineup-rail--fixed'), '缺少换人模式长驻样式');
  assert.ok(CSS.includes('--live-sub-rail-top:250px') && CSS.includes('var(--live-sub-rail-top,250px)'), '固定打线栏应稳定定位且可参数化');
});
