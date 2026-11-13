// 批量添加替补：勾选状态与提交值整理
// 运行：node --test tests/lineupBenchSelection.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { benchSelectionState, sanitizeBenchIds } from '../src/utils/lineupBenchSelection.js';

const options = [{ id: 1 }, { id: 2 }, { id: 3 }];

test('无候选或未选择：不全选、不半选、计数为 0', () => {
  assert.deepEqual(benchSelectionState([], []), {
    selectedCount: 0,
    allSelected: false,
    indeterminate: false,
  });
  assert.deepEqual(benchSelectionState(options, []), {
    selectedCount: 0,
    allSelected: false,
    indeterminate: false,
  });
});

test('部分选择：半选态，计数只含候选项', () => {
  const state = benchSelectionState(options, [1, 3, 99]);
  assert.equal(state.selectedCount, 2);
  assert.equal(state.allSelected, false);
  assert.equal(state.indeterminate, true);
});

test('全部候选已选：全选态，不再是半选', () => {
  assert.deepEqual(benchSelectionState(options, [1, 2, 3]), {
    selectedCount: 3,
    allSelected: true,
    indeterminate: false,
  });
});

test('提交值：剔除候选外旧 id、去重，并保持原有选择顺序', () => {
  assert.deepEqual(sanitizeBenchIds(options, [3, 99, 1, 3, 2]), [3, 1, 2]);
  assert.deepEqual(sanitizeBenchIds(options, []), []);
  assert.deepEqual(sanitizeBenchIds([], [1, 2]), []);
});

test('字符串/数字 id 混用时按候选自身类型回传', () => {
  const stringOptions = [{ id: 'a' }, { id: 'b' }];
  assert.deepEqual(sanitizeBenchIds(stringOptions, ['b', 'x']), ['b']);
  assert.deepEqual(benchSelectionState(stringOptions, ['b']), {
    selectedCount: 1,
    allSelected: false,
    indeterminate: true,
  });
});
