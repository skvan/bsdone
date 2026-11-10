import assert from 'node:assert/strict';
import test from 'node:test';
import {
  resolveBatterForceAdvance,
  resolveGroundOutForceAdvance
} from '../src/utils/liveGameForceAdvance.js';

const runner = (id) => ({ id });
const bases = (spec) => ({
  1: spec[1] ? runner(spec[1]) : null,
  2: spec[2] ? runner(spec[2]) : null,
  3: spec[3] ? runner(spec[3]) : null
});
const moveKeys = (plan) => plan.moves.map((move) => `${move.runner.id}:${move.from}>${move.to}`).sort();
const homeKeys = (plan) => plan.homeRunners.map((home) => `${home.runner.id}:${home.from}`).sort();
const finalKeys = (plan) => Object.fromEntries(
  [1, 2, 3].map((base) => [base, plan.finalBases[base]?.id ?? null])
);

test('一垒安打：仅二垒有人时二垒跑者留二垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 1, runners: bases({ 2: 'R2' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), []);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: 'B', 2: 'R2', 3: null });
});

test('一垒安打：一垒有人时一垒跑者自动到二垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 1, runners: bases({ 1: 'R1' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>2']);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: 'B', 2: 'R1', 3: null });
});

test('一垒安打：一垒二垒有人时两人各进一个垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 1, runners: bases({ 1: 'R1', 2: 'R2' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>2', 'R2:2>3']);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: 'B', 2: 'R1', 3: 'R2' });
});

test('一垒安打：一垒三垒有人时三垒跑者不被迫进垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 1, runners: bases({ 1: 'R1', 3: 'R3' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>2']);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: 'B', 2: 'R1', 3: 'R3' });
});

test('一垒安打：满垒时三垒跑者被挤回本垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 1, runners: bases({ 1: 'R1', 2: 'R2', 3: 'R3' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>2', 'R2:2>3']);
  assert.deepEqual(homeKeys(plan), ['R3:3']);
  assert.deepEqual(finalKeys(plan), { 1: 'B', 2: 'R1', 3: 'R2' });
});

test('二垒安打：仅二垒有人时二垒跑者被击球员挤出到三垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 2, runners: bases({ 2: 'R2' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R2:2>3']);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: 'B', 3: 'R2' });
});

test('二垒安打：仅一垒有人时一垒跑者到三垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 2, runners: bases({ 1: 'R1' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>3']);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: 'B', 3: 'R1' });
});

test('二垒安打：一垒二垒有人时二垒跑者回本垒、一垒跑者到三垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 2, runners: bases({ 1: 'R1', 2: 'R2' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>3']);
  assert.deepEqual(homeKeys(plan), ['R2:2']);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: 'B', 3: 'R1' });
});

test('二垒安打：满垒时三垒和二垒跑者回本垒、一垒跑者到三垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 2, runners: bases({ 1: 'R1', 2: 'R2', 3: 'R3' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), ['R1:1>3']);
  assert.deepEqual(homeKeys(plan), ['R2:2', 'R3:3']);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: 'B', 3: 'R1' });
});

test('三垒安打：所有跑者回本垒，打者到三垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 3, runners: bases({ 1: 'R1', 2: 'R2', 3: 'R3' }), batter: runner('B') });
  assert.deepEqual(moveKeys(plan), []);
  assert.deepEqual(homeKeys(plan), ['R1:1', 'R2:2', 'R3:3']);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: null, 3: 'B' });
});

test('全垒打：所有跑者和打者回本垒', () => {
  const plan = resolveBatterForceAdvance({ batterBase: 4, runners: bases({ 1: 'R1', 2: 'R2', 3: 'R3' }), batter: runner('B') });
  assert.deepEqual(homeKeys(plan), ['R1:1', 'R2:2', 'R3:3']);
  assert.equal(plan.batterHome, true);
  assert.equal(plan.batterTo, 4);
});

test('地滚出局：仅一垒有人时一垒跑者到二垒', () => {
  const plan = resolveGroundOutForceAdvance({ runners: bases({ 1: 'R1' }) });
  assert.deepEqual(moveKeys(plan), ['R1:1>2']);
  assert.deepEqual(homeKeys(plan), []);
  assert.equal(plan.batterTo, 0);
});

test('地滚出局：一垒二垒有人时二垒到三垒、一垒到二垒', () => {
  const plan = resolveGroundOutForceAdvance({ runners: bases({ 1: 'R1', 2: 'R2' }) });
  assert.deepEqual(moveKeys(plan), ['R1:1>2', 'R2:2>3']);
  assert.deepEqual(homeKeys(plan), []);
});

test('地滚出局：满垒时三垒跑者回本垒', () => {
  const plan = resolveGroundOutForceAdvance({ runners: bases({ 1: 'R1', 2: 'R2', 3: 'R3' }) });
  assert.deepEqual(moveKeys(plan), ['R1:1>2', 'R2:2>3']);
  assert.deepEqual(homeKeys(plan), ['R3:3']);
});

test('地滚出局：一垒三垒有人但二垒空时三垒跑者不被迫进垒', () => {
  const plan = resolveGroundOutForceAdvance({ runners: bases({ 1: 'R1', 3: 'R3' }) });
  assert.deepEqual(moveKeys(plan), ['R1:1>2']);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: 'R1', 3: 'R3' });
});

test('地滚出局：一垒无人时二垒三垒跑者都不动', () => {
  const plan = resolveGroundOutForceAdvance({ runners: bases({ 2: 'R2', 3: 'R3' }) });
  assert.deepEqual(moveKeys(plan), []);
  assert.deepEqual(homeKeys(plan), []);
  assert.deepEqual(finalKeys(plan), { 1: null, 2: 'R2', 3: 'R3' });
});

