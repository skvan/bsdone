// 前端单测：实时录入全屏 Play-by-Play 日志流工具（#359）
// 运行：在 bsball-frontend 目录执行  node --test tests/*.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  buildPbpEntries,
  clampPbpHeight,
  PBP_DEFAULT_RATIO,
  PBP_LOG_LIMIT,
  PBP_MAX_RATIO,
  PBP_MIN_HEIGHT
} from '../src/utils/liveGamePbp.js';

test('buildPbpEntries：解析时间/局次/正文，并保留 tail 与 lines', () => {
  const entries = buildPbpEntries([
    '[10:00:01] [1下] 曾祉乐: 一垒安打 (1B)',
    { head: '[10:00:20] [1下] 投手: 三振出局', tail: 'K', lines: ['好球未挥', '', 123] }
  ]);

  assert.equal(entries.length, 2);
  assert.deepEqual(entries[0], {
    key: '10:00:01|1下|曾祉乐: 一垒安打 (1B)||',
    time: '10:00:01',
    inning: '1下',
    text: '曾祉乐: 一垒安打 (1B)',
    tail: '',
    lines: []
  });
  assert.deepEqual(entries[1], {
    key: '10:00:20|1下|投手: 三振出局|K|好球未挥',
    time: '10:00:20',
    inning: '1下',
    text: '投手: 三振出局',
    tail: 'K',
    lines: ['好球未挥']
  });
});

test('buildPbpEntries：过滤 showInPbp:false 的系统条目', () => {
  const entries = buildPbpEntries([
    { head: '[10:00:30] [1下] 比赛开始', showInPbp: false },
    '[10:00:31] [1下] 曾祉乐: 一垒安打 (1B)'
  ]);

  assert.equal(entries.length, 1);
  assert.equal(entries[0].text, '曾祉乐: 一垒安打 (1B)');
});

test('buildPbpEntries：跳过空值、空正文与非数组输入', () => {
  assert.deepEqual(buildPbpEntries(null), []);
  assert.deepEqual(buildPbpEntries('not-array'), []);
  assert.deepEqual(buildPbpEntries([null, '', '   ', { head: '' }]), []);
});

test('buildPbpEntries：无标准前缀的日志原样保留正文', () => {
  const entries = buildPbpEntries(['无时间前缀日志']);
  assert.deepEqual(entries, [
    { key: '||无时间前缀日志||', time: '', inning: '', text: '无时间前缀日志', tail: '', lines: [] }
  ]);
});

test('buildPbpEntries：重复内容生成稳定且互不冲突的 key', () => {
  const entries = buildPbpEntries([
    '[10:00:01] [1下] 同一事件',
    '[10:00:01] [1下] 同一事件'
  ]);
  assert.equal(entries.length, 2);
  assert.notEqual(entries[0].key, entries[1].key);
  assert.match(entries[1].key, /#1$/);
});

test('buildPbpEntries：复用现有日志清洗，去掉坐标后缀', () => {
  const entries = buildPbpEntries([
    '[10:00:01] [1下] 击出滚地球到游击手 · 坐标(123, 456)'
  ]);
  assert.equal(entries[0].text, '击出滚地球到游击手');
});

test('buildPbpEntries：按 limit 截断，默认上限为最近 200 条', () => {
  const logs = Array.from({ length: 5 }, (_, i) => `[10:00:0${i}] [1下] 事件${i}`);
  assert.equal(buildPbpEntries(logs, 2).length, 2);
  assert.deepEqual(
    buildPbpEntries(logs, 2).map((entry) => entry.text),
    ['事件0', '事件1']
  );
  assert.equal(PBP_LOG_LIMIT, 200);
});

test('clampPbpHeight：限制在最小高度与最大屏占比之间', () => {
  assert.equal(PBP_MIN_HEIGHT, 120);
  assert.equal(PBP_MAX_RATIO, 0.55);
  assert.equal(PBP_DEFAULT_RATIO, 0.3);
  assert.equal(clampPbpHeight(200, 1000), 200);
  assert.equal(clampPbpHeight(50, 1000), 120);
  assert.equal(clampPbpHeight(999, 1000), 550);
  assert.equal(clampPbpHeight(333.6, 1000), 334);
  assert.equal(clampPbpHeight(200, 0), 120);
  assert.equal(clampPbpHeight(-10, 800), 120);
});
