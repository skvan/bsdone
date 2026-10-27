// 个人驾驶舱自由画布几何单测：间距边界、移动吸附、八方向缩放、装箱与画布高度
// 运行：node --test tests/clubhouseBoard.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  BOARD_WIDTH,
  BOARD_GAP,
  BOARD_SNAP,
  BOARD_MIN_HEIGHT,
  MAX_MODULE_HEIGHT,
  MAX_MODULE_Y,
  snapToGrid,
  clamp,
  rectsOverlap,
  collidesWithAny,
  clampToBoard,
  movedRect,
  moveModule,
  findSwapTarget,
  planSwap,
  resizeModule,
  packModules,
  boardHeight,
  placeBelow
} from '../src/views/portal/clubhouseBoard.js';

const rect = (key, x, y, width, height) => ({ key, x, y, width, height });

test('画布常量与设计稿一致', () => {
  assert.equal(BOARD_WIDTH, 1216);
  assert.equal(BOARD_GAP, 16);
  assert.equal(BOARD_SNAP, 8);
  assert.equal(BOARD_MIN_HEIGHT, 720);
  assert.equal(MAX_MODULE_HEIGHT, 1600);
  assert.equal(MAX_MODULE_Y, 12000);
});

test('吸附与限制工具按步长取整并收敛区间', () => {
  assert.equal(snapToGrid(0), 0);
  assert.equal(snapToGrid(4), 8);
  assert.equal(snapToGrid(12), 16);
  assert.equal(snapToGrid(-4), 0);
  assert.equal(snapToGrid(14, 10), 10);
  assert.equal(snapToGrid(15, 10), 20);
  assert.equal(snapToGrid(Number.NaN), 0);
  assert.equal(clamp(5, 0, 10), 5);
  assert.equal(clamp(-1, 0, 10), 0);
  assert.equal(clamp(11, 0, 10), 10);
});

test('16px 间距合法，15px 视为重叠', () => {
  const a = rect('a', 0, 0, 800, 460);
  assert.equal(rectsOverlap(a, rect('b', 816, 0, 400, 300)), false);
  assert.equal(rectsOverlap(a, rect('b', 815, 0, 400, 300)), true);
  assert.equal(rectsOverlap(a, rect('b', 0, 476, 400, 300)), false);
  assert.equal(rectsOverlap(a, rect('b', 0, 475, 400, 300)), true);
  assert.equal(rectsOverlap(a, rect('b', 900, 0, 400, 300)), false);
  assert.equal(rectsOverlap(rect('b', 816, 0, 400, 300), a), false);
  assert.equal(rectsOverlap(rect('b', 815, 0, 400, 300), a), true);
});

test('碰撞检测跳过同 key 模块并支持自定义间距', () => {
  const base = rect('a', 0, 0, 400, 320);
  assert.equal(collidesWithAny(base, []), false);
  assert.equal(collidesWithAny(base, [rect('b', 416, 0, 400, 320)]), false);
  assert.equal(collidesWithAny(base, [rect('b', 415, 0, 400, 320)]), true);
  assert.equal(collidesWithAny(base, [rect('a', 0, 0, 400, 320)]), false);
  assert.equal(collidesWithAny(base, [rect('b', 408, 0, 400, 320)], 8), false);
  assert.equal(collidesWithAny(base, [rect('b', 407, 0, 400, 320)], 8), true);
});

test('越界矩形收敛回画布且保留其他字段', () => {
  assert.deepEqual(
    clampToBoard({ ...rect('a', -20, -16, 4000, 2000), enabled: true }),
    { key: 'a', x: 0, y: 0, width: 1216, height: 1600, enabled: true }
  );
  assert.deepEqual(clampToBoard(rect('a', 1000, 13000, 400, 300)), rect('a', 816, 12000, 400, 300));
});

test('移动吸附 8px，撞邻居不返回候选', () => {
  const start = rect('a', 0, 0, 400, 320);
  const other = rect('b', 600, 0, 400, 320);
  assert.deepEqual(moveModule(start, 80, 80, [other]), rect('a', 80, 80, 400, 320));
  assert.equal(moveModule(start, 500, 0, [other]), null);
  assert.equal(start.x, 0);
  assert.equal(start.y, 0);
});

test('移动受画布左右与 y 上限约束，负坐标归零', () => {
  assert.deepEqual(moveModule(rect('a', 800, 104, 400, 300), 500, 0, []), rect('a', 816, 104, 400, 300));
  assert.deepEqual(moveModule(rect('a', 0, 11900, 400, 300), 0, 1000, []), rect('a', 0, 12000, 400, 300));
  assert.deepEqual(moveModule(rect('a', 0, 0, 400, 300), -80, -80, []), rect('a', 0, 0, 400, 300));
});

test('移动忽略自身副本，并支持关闭吸附', () => {
  const start = rect('a', 96, 96, 400, 320);
  assert.deepEqual(moveModule(start, 8, 8, [{ ...start }]), rect('a', 104, 104, 400, 320));
  assert.deepEqual(
    moveModule(start, 2, 2, [], { snap: false }),
    rect('a', 98, 98, 400, 320)
  );
});

test('八方向缩放锚定对边或对角', () => {
  const start = rect('a', 200, 200, 400, 320);
  const resize = (direction, dx, dy) => resizeModule(start, direction, dx, dy, 360, 320, []);
  assert.deepEqual(resize('e', 96, 0), rect('a', 200, 200, 496, 320));
  assert.deepEqual(resize('w', -96, 0), rect('a', 104, 200, 496, 320));
  assert.deepEqual(resize('n', 0, -96), rect('a', 200, 104, 400, 416));
  assert.deepEqual(resize('s', 0, 96), rect('a', 200, 200, 400, 416));
  assert.deepEqual(resize('ne', 96, -96), rect('a', 200, 104, 496, 416));
  assert.deepEqual(resize('nw', -96, -96), rect('a', 104, 104, 496, 416));
  assert.deepEqual(resize('se', 96, 96), rect('a', 200, 200, 496, 416));
  assert.deepEqual(resize('sw', -96, 96), rect('a', 104, 200, 496, 416));
});

test('东边缩放撞邻居返回 null，西边缩放固定右边', () => {
  const start = rect('a', 200, 0, 400, 320);
  const other = rect('b', 700, 0, 400, 320);
  const west = resizeModule(start, 'w', -96, 0, 360, 320, [other]);
  assert.equal(west.x, 104);
  assert.equal(west.x + west.width, 600);
  assert.equal(resizeModule(start, 'e', 400, 0, 360, 320, [other]), null);
});

test('缩放遵守最小宽高并保持锚点', () => {
  assert.deepEqual(
    resizeModule(rect('a', 200, 0, 400, 320), 'w', 500, 0, 360, 320, []),
    rect('a', 240, 0, 360, 320)
  );
  assert.deepEqual(
    resizeModule(rect('a', 200, 0, 400, 320), 'e', -500, 0, 360, 320, []),
    rect('a', 200, 0, 360, 320)
  );
  assert.deepEqual(
    resizeModule(rect('a', 0, 200, 400, 320), 'n', 0, 500, 360, 320, []),
    rect('a', 0, 200, 400, 320)
  );
  assert.deepEqual(
    resizeModule(rect('a', 0, 200, 400, 320), 's', 0, -500, 360, 320, []),
    rect('a', 0, 200, 400, 320)
  );
});

test('缩放遵守画布宽度、最大高度与 y 上限', () => {
  assert.deepEqual(
    resizeModule(rect('a', 0, 0, 400, 320), 's', 0, 2000, 360, 320, []),
    rect('a', 0, 0, 400, 1600)
  );
  assert.deepEqual(
    resizeModule(rect('a', 800, 0, 400, 320), 'e', 96, 0, 360, 320, []),
    rect('a', 800, 0, 416, 320)
  );
  assert.deepEqual(
    resizeModule(rect('a', 40, 0, 400, 320), 'w', -96, 0, 360, 320, []),
    rect('a', 0, 0, 440, 320)
  );
  assert.equal(resizeModule(rect('a', 900, 0, 400, 320), 'e', 96, 0, 360, 320, []), null);
  assert.equal(resizeModule(rect('a', 0, 11900, 800, 1600), 'n', 0, 200, 360, 320, []), null);
  assert.equal(resizeModule(rect('a', 0, 0, 400, 320), 'x', 8, 8, 360, 320, []), null);
});

test('默认装箱与画布高度', () => {
  const modules = packModules([
    { key: 'a', width: 1216, height: 220 },
    { key: 'b', width: 800, height: 460 },
    { key: 'c', width: 400, height: 300 }
  ]);
  assert.equal(modules[1].y - (modules[0].y + modules[0].height), BOARD_GAP);
  assert.equal(modules[1].y, 236);
  assert.equal(modules[2].x, 816);
  assert.equal(modules[2].y, 236);
  assert.equal(boardHeight(modules), 720);
});

test('九个默认模块装箱位置与设计稿一致', () => {
  const placed = packModules([
    { key: 'ranks', width: 1216, height: 220 },
    { key: 'evaluation', width: 1216, height: 620 },
    { key: 'milestones', width: 1216, height: 500 },
    { key: 'performance', width: 800, height: 460 },
    { key: 'teams', width: 400, height: 300 },
    { key: 'schedule', width: 800, height: 560 },
    { key: 'about', width: 400, height: 280 },
    { key: 'recentGames', width: 800, height: 320 },
    { key: 'quickLinks', width: 400, height: 220 }
  ]);
  const byKey = Object.fromEntries(placed.map((module) => [module.key, module]));
  assert.deepEqual(
    { x: byKey.performance.x, y: byKey.performance.y },
    { x: 0, y: 1388 }
  );
  assert.deepEqual(
    { x: byKey.teams.x, y: byKey.teams.y },
    { x: 816, y: 1388 }
  );
  assert.deepEqual(
    { x: byKey.schedule.x, y: byKey.schedule.y },
    { x: 0, y: 1864 }
  );
  assert.deepEqual(
    { x: byKey.quickLinks.x, y: byKey.quickLinks.y },
    { x: 816, y: 2440 }
  );
  assert.equal(boardHeight(placed), 2760);
});

test('装箱支持自定义画布宽度与间距，且不修改入参', () => {
  const input = [
    { key: 'a', width: 100, height: 50 },
    { key: 'b', width: 100, height: 50 },
    { key: 'c', width: 100, height: 50 }
  ];
  const packed = packModules(input, { width: 216, gap: 16 });
  assert.deepEqual(packed.map(({ x, y }) => [x, y]), [[0, 0], [116, 0], [0, 66]]);
  assert.equal(input[0].x, undefined);
  assert.equal(input[0].width, 100);
});

test('装箱收敛超宽超高模块', () => {
  const [placed] = packModules([{ key: 'a', width: 2000, height: 2000 }]);
  assert.equal(placed.width, 1216);
  assert.equal(placed.height, 1600);
});

test('画布高度取最小高度与模块底边的较大值', () => {
  assert.equal(boardHeight([]), BOARD_MIN_HEIGHT);
  assert.equal(boardHeight([], 900), 900);
  assert.equal(boardHeight([rect('a', 0, 1388, 800, 460)]), 1848);
});

test('重新启用模块下移到所有已占用模块底部', () => {
  assert.deepEqual(placeBelow(800, 460, []), { x: 0, y: 0, width: 800, height: 460 });
  assert.deepEqual(
    placeBelow(800, 460, [rect('a', 0, 0, 800, 460), rect('b', 0, 872, 400, 500)]),
    { x: 0, y: 1388, width: 800, height: 460 }
  );
  assert.deepEqual(placeBelow(2000, 700, []), { x: 0, y: 0, width: 1216, height: 700 });
});

test('拖动矩形吸附并收敛到画布内，不检查碰撞', () => {
  assert.deepEqual(movedRect(rect('a', 0, 0, 400, 320), 80, 80), rect('a', 80, 80, 400, 320));
  assert.deepEqual(movedRect(rect('a', 800, 104, 400, 320), 500, 0), rect('a', 816, 104, 400, 320));
  assert.deepEqual(movedRect(rect('a', 0, 0, 400, 320), -80, -80), rect('a', 0, 0, 400, 320));
});

test('交换目标按重叠比例选择，低于阈值不算', () => {
  const dragged = rect('a', 0, 0, 800, 500);
  assert.deepEqual(findSwapTarget(dragged, [rect('b', 0, 0, 800, 500)]), rect('b', 0, 0, 800, 500));
  assert.equal(findSwapTarget(dragged, [rect('b', 700, 0, 400, 300)]), null);
  // 与默认阈值 35% 的边界：140 / 400 宽 = 35%
  assert.equal(findSwapTarget(dragged, [rect('b', 660, 0, 400, 300)])?.key, 'b');
  assert.equal(findSwapTarget(dragged, [rect('b', 660, 0, 400, 300)], { minRatio: 0.36 }), null);
  // 两个都完全命中时取重叠面积更大的
  assert.equal(findSwapTarget(dragged, [
    rect('b', 0, 0, 800, 500),
    rect('c', 0, 0, 400, 300)
  ]).key, 'b');
  // 忽略自身
  assert.equal(findSwapTarget(dragged, [rect('a', 0, 0, 800, 500)]), null);
});

test('同尺寸模块交换位置且不改入参', () => {
  const start = rect('a', 0, 0, 400, 300);
  const target = rect('b', 0, 400, 400, 300);
  const others = [rect('c', 0, 800, 400, 300)];
  const plan = planSwap(start, target, others);
  assert.deepEqual(plan.dragged, rect('a', 0, 400, 400, 300));
  assert.deepEqual(plan.target, rect('b', 0, 0, 400, 300));
  assert.deepEqual(plan.moved, []);
  assert.deepEqual(start, rect('a', 0, 0, 400, 300));
  assert.deepEqual(target, rect('b', 0, 400, 400, 300));
  assert.deepEqual(others, [rect('c', 0, 800, 400, 300)]);
});

test('不同尺寸同排交换：保留尺寸并收敛到画布内', () => {
  const plan = planSwap(
    rect('a', 0, 0, 800, 500),
    rect('b', 816, 0, 400, 300),
    []
  );
  assert.deepEqual(plan.dragged, rect('a', 416, 0, 800, 500));
  assert.deepEqual(plan.target, rect('b', 0, 0, 400, 300));
  assert.equal(plan.dragged.x + plan.dragged.width, BOARD_WIDTH);
});

test('交换后第三方冲突整体下移并级联', () => {
  const plan = planSwap(
    rect('a', 0, 0, 400, 300),
    rect('b', 0, 400, 400, 300),
    [rect('c', 0, 700, 400, 300), rect('d', 0, 1016, 400, 300)]
  );
  const moved = Object.fromEntries(plan.moved.map((module) => [module.key, module.y]));
  assert.equal(moved.c, 716);
  assert.equal(moved.d, 1032);
});

test('上下相邻且高度不同：上方保留、下方下移，双向结果一致', () => {
  const ranks = rect('ranks', 0, 0, 1216, 220);
  const evaluation = rect('evaluation', 0, 236, 1216, 700);
  const milestones = rect('milestones', 0, 952, 1216, 500);
  for (const plan of [
    planSwap(ranks, evaluation, [milestones]),
    planSwap(evaluation, ranks, [milestones])
  ]) {
    const byKey = {
      [plan.dragged.key]: plan.dragged,
      [plan.target.key]: plan.target
    };
    assert.equal(byKey.evaluation.y, 0);
    assert.equal(byKey.ranks.y, 716);
    assert.deepEqual(plan.moved, []);
  }
});

test('交换位置互相重叠时按上/下回流，而不是放弃', () => {
  const plan = planSwap(
    rect('a', 0, 0, 600, 300),
    rect('b', 100, 100, 600, 300),
    []
  );
  assert.deepEqual(plan.target, rect('b', 0, 0, 600, 300));
  assert.deepEqual(plan.dragged, rect('a', 100, 316, 600, 300));
});

test('交换后下移超限时放弃', () => {
  assert.equal(planSwap(
    rect('a', 0, 0, 400, 1000),
    rect('b', 0, 11300, 400, 300),
    [rect('c', 0, 11616, 400, 300)]
  ), null);
});
