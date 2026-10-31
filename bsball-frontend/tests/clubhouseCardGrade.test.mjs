import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CARD_GRADE_MATERIALS,
  cardGradeMaterial,
  gradeFrameGradient,
  gradeSheenGradient,
  gradeSparkSpots
} from '../src/components/portal/clubhouseCardGrade.js';

test('八级材质与 overall.grade 一一对应且顺序正确', () => {
  assert.deepEqual(CARD_GRADE_MATERIALS.map((m) => m.key), ['D', 'C', 'C+', 'B', 'B+', 'A', 'A+', 'S']);
  for (const material of CARD_GRADE_MATERIALS) {
    assert.ok(material.stops.length >= 5, `${material.key} 渐变 stop 不足`);
    assert.equal(material.stops[0][1], 0);
    assert.equal(material.stops[material.stops.length - 1][1], 100);
  }
});

test('未知或空等级回退到 D 档材质', () => {
  assert.equal(cardGradeMaterial('S').key, 'S');
  assert.equal(cardGradeMaterial(null).key, 'D');
  assert.equal(cardGradeMaterial('UR').key, 'D');
});

test('边框渐变与光泽串包含色板参数', () => {
  const gold = cardGradeMaterial('B');
  assert.equal(gradeFrameGradient(gold), 'linear-gradient(160deg, #fff4c2 0%, #c9992e 22%, #ffe9a0 46%, #8a6414 72%, #f0d072 100%)');
  assert.ok(gradeSheenGradient(gold).includes('rgba(255,255,255,0.5)'));
});

test('星芒数量随档位递增且不超过点位上限', () => {
  const counts = CARD_GRADE_MATERIALS.map((m) => m.sparkles);
  for (let i = 1; i < counts.length; i += 1) {
    assert.ok(counts[i] >= counts[i - 1], '星芒数应随档位不递减');
  }
  assert.equal(gradeSparkSpots(cardGradeMaterial('D')).length, 0);
  assert.equal(gradeSparkSpots(cardGradeMaterial('S')).length, 6);
  for (const spot of gradeSparkSpots(cardGradeMaterial('S'))) {
    assert.ok(spot.style.left.endsWith('%'));
    assert.ok(spot.style.width.endsWith('px'));
  }
});
