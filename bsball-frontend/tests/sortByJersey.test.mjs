// 背号排序单测：数字升序、空背号最后、同号按姓名
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { compareByJersey, sortByJersey } from '../src/utils/sortByJersey.js';

test('背号按数字升序，空背号/非数字排最后', () => {
  const list = [
    { id: 1, name: '甲', number: '10' },
    { id: 2, name: '乙', number: '2' },
    { id: 3, name: '丙', number: '00' },
    { id: 4, name: '丁', number: '' },
    { id: 5, name: '戊', number: '99' },
    { id: 6, name: '己', number: '-' }
  ];
  assert.deepEqual(sortByJersey(list).map((p) => p.id), [3, 2, 1, 5, 4, 6]);
});

test('同背号按姓名排序，不修改原数组', () => {
  const list = [
    { id: 1, name: 'B', number: '7' },
    { id: 2, name: 'A', number: '7' }
  ];
  assert.deepEqual(sortByJersey(list).map((p) => p.name), ['A', 'B']);
  assert.deepEqual(list.map((p) => p.name), ['B', 'A']);
  assert.ok(compareByJersey({ name: 'A', number: '2' }, { name: 'B', number: '10' }) < 0);
});
