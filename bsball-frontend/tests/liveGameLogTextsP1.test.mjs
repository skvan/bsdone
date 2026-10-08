// P1 生成器单测（日志规范 v1.0 §2.1/§3 基线样例）
// 运行：node --test tests/liveGameLogTextsP1.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { advanceText, outText, fcGroup, pickoffGroup, nameWithNumber, caughtStealingText, toNumCodes } from '../src/utils/liveGameLogTexts.js';

test('nameWithNumber：名 (#N) / 缺号降级 / 缺名降级', () => {
  assert.equal(nameWithNumber({ name: 'Gytis', number: 8 }), 'Gytis (#8)');
  assert.equal(nameWithNumber({ name: 'Gytis', number: '' }), 'Gytis');
  assert.equal(nameWithNumber({ name: '王一', number: 23 }), '王一 (#23)');
  assert.equal(nameWithNumber(null), '跑者');
});

test('advanceText：盗垒成功样例（原因前置 + (#N) + — 安全；得分/退垒/留垒分支）', () => {
  assert.equal(
    advanceText({ reasonCn: '盗垒成功 (SB)', runner: { name: 'Gytis', number: 8 }, from: 1, to: 2 }),
    '盗垒成功 (SB)：一垒跑者 Gytis (#8) 进二垒 — 安全');
  assert.equal(
    advanceText({ reasonCn: '暴投 (WP)', runner: { name: '王一', number: 23 }, from: 3, to: 4 }),
    '暴投 (WP)：三垒跑者 王一 (#23) 回本垒得分');
  assert.equal(
    advanceText({ reasonCn: '未进垒', runner: { name: '王一', number: 23 }, from: 3, to: 2 }),
    '未进垒：三垒跑者 王一 (#23) 回到二垒');
  assert.equal(
    advanceText({ reasonCn: '守备大意 (DI)', runner: { name: '王一', number: 23 }, from: 1, to: 1 }),
    '守备大意 (DI)：一垒跑者 王一 (#23) 留在一垒');
});

test('outText：离垒出局/触杀出局样例（原因前置 + 于垒出局 + 行尾累计）', () => {
  assert.equal(
    outText({ reasonCn: '离垒过远出局', runner: { name: '钱乾', number: 95 }, from: 1, to: 2, outsCum: 1, total: 1 }),
    '离垒过远出局：一垒跑者 钱乾 (#95) 于二垒出局，本棒次1人出局，已累计1人出局');
  assert.equal(
    outText({ reasonCn: '触杀出局', runner: { name: '王一', number: 23 }, from: 2, to: 3, fielderCode: '6', fielder: { name: '王五', number: 6 }, outsCum: 1, total: 2 }),
    '触杀出局：二垒跑者 王一 (#23) 于三垒出局（游击 王五 (#6)），本棒次1人出局，已累计2人出局');
});

test('fcGroup：B4 野手选择 5-4 五行组（规范 §3.B 示例 1）', () => {
  const { lines } = fcGroup({
    batter: { name: '李熙俊', number: 22 },
    ballDesc: '击出滚地球到三垒（5）',
    chain: ['5', '4'],
    chainPlayers: [{ name: '张三', number: 51 }, { name: '李四', number: 4 }],
    outRunner: { name: '王一', number: 23 }, outFrom: 1, outBase: 2,
    outsCum: 1, total: 2
  });
  assert.deepEqual(lines, [
    '李熙俊: 击出滚地球到三垒（5）',
    '5 三垒手 张三 (#51): 接球传二垒（助杀 A）',
    '4 二垒手 李四 (#4): 踏垒封杀一垒跑者 王一 (#23)（刺杀 PO）',
    '一垒跑者 王一 (#23): 于二垒被封杀出局（5-4，野手选择 FC），本棒次1人出局，已累计2人出局',
    '李熙俊: 上一垒（野手选择 FC）'
  ]);
});

test('pickoffGroup：C3 牵制出局三行组（规范 §3.C 示例）', () => {
  const { lines } = pickoffGroup({
    pitcher: { name: '金宗勇', number: 17 },
    runner: { name: 'Gytis', number: 8 }, from: 2,
    fielderCode: '6', fielder: { name: '王五', number: 6 },
    chain: ['P', '6'], outsCum: 1, total: 3
  });
  assert.deepEqual(lines, [
    '投手 金宗勇 (#17): 牵制二垒',
    '6 游击 王五 (#6): 接球触杀（刺杀 PO）',
    '二垒跑者 Gytis (#8): 被牵制出局（投手→游击），本棒次1人出局，已累计3人出局'
  ]);
});

test('caughtStealingText：C4 盗垒死（自X盗Y + 累计）', () => {
  assert.equal(
    caughtStealingText({ fielderPos: '游击', runner: { name: '王一', number: 23 }, from: 1, to: 2, outsCum: 1, total: 2 }),
    '盗垒死（CS 游击）：一垒跑者 王一 (#23) 自一垒盗二垒，本棒次1人出局，已累计2人出局');
});

test('fcGroup：三段链中间行传出目标按下一链位（5-4-3）', () => {
  const { lines } = fcGroup({
    batter: { name: '孙八', number: 37 },
    ballDesc: '击出滚地球到三垒（5）',
    chain: ['5', '4', '3'],
    chainPlayers: [{ name: '张三', number: 51 }, { name: '李四', number: 4 }, { name: '赵三', number: 3 }],
    outRunner: { name: '王一', number: 23 }, outFrom: 1, outBase: 2,
    outsCum: 2, total: 2
  });
  assert.deepEqual(lines.slice(1, 4), [
    '5 三垒手 张三 (#51): 接球传二垒（助杀 A）',
    '4 二垒手 李四 (#4): 接球传一垒（助杀 A）',
    '3 一垒手 赵三 (#3): 踏垒封杀一垒跑者 王一 (#23)（刺杀 PO）'
  ]);
});

test('toNumCodes：守备位别名→数字制（链文本统一）', () => {
  assert.deepEqual(toNumCodes(['3B', '2B', 'SS', 'C', 'P', '5']), ['5', '4', '6', '2', '1', '5']);
  assert.deepEqual(toNumCodes([]), []);
});

// —— 观赛端与分类器静态断言（P1-⑤ / G2） ——
import fs from 'node:fs';
import { normalizeLineList } from '../src/utils/liveGameFieldLayout.js';
const watchSrc = fs.readFileSync(new URL('../src/views/admin/LiveGameWatch.js', import.meta.url), 'utf8');
const layoutSrc = fs.readFileSync(new URL('../src/utils/liveGameFieldLayout.js', import.meta.url), 'utf8');

test('C1 回归：normalizeLineList 透传 lines（观赛/重载往返不丢组行成员）', () => {
  const obj = {
    head: '[10:00:00] [1上] 第3棒 张三 (#51): 击出滚地球到三垒（5）',
    lines: [
      '5 三垒手 钱乾 (#95): 接球传二垒（助杀 A）',
      '一垒跑者 王一 (#23): 于二垒被封杀出局（5-4，野手选择 FC），本棒次1人出局，已累计2人出局'
    ]
  };
  const out = normalizeLineList([obj, 'plain']);
  assert.equal(typeof out[0], 'object', '组行对象保留');
  assert.ok(Array.isArray(out[0].lines) && out[0].lines.length === 2, 'lines 透传不丢');
  assert.ok(String(out[0].head).includes('击出滚地球到三垒（5）'), 'head 保留');
  const single = normalizeLineList([{ head: '[10:00:00] [1上] 普通行' }]);
  assert.equal(typeof single[0], 'string', '无 tail/lines 旧对象仍按旧行为降为字符串（兼容）');
});

test('Watch：组行提取与成员行缩进渲染（log-row__member）', () => {
  assert.match(watchSrc, /lines:typeof t=="object"&&t&&Array\.isArray\(t\.lines\)\?t\.lines:null/, 'lines 提取');
  assert.match(watchSrc, /class:"log-row__member"/, '成员行渲染');
  assert.match(watchSrc, /paddingLeft:"22px"/, '成员行缩进');
});

test('分类器白名单保持（G2）：失误族含牵制失误/失误推进', () => {
  assert.match(layoutSrc, /守备失误\|传球失误\|同一play内失误\|失误后推进\|暴传失误\|牵制失误/, '失误族白名单完整（含牵制失误）');
});
