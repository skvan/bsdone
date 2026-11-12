// 云端重建可信判定：除 def_inn 外，零出局的 E/DP/PB/CS/SB 也属于历史守备指标
import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as rules from '../src/utils/liveGameFieldingByPos.js';

test('历史守备指标：0 局但存在 E/DP/PB/CS/SB 时仍视为已有逐位置数据', () => {
  const { hasHistoricalFieldingStats } = rules;
  assert.equal(typeof hasHistoricalFieldingStats, 'function', '缺少 hasHistoricalFieldingStats');
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0 }, {}]), false);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 1.2 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, e: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, dp: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, pb: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, catcherCs: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, catcherSb: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, po: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0, a: 1 }]), true);
  assert.equal(hasHistoricalFieldingStats(null), false);
});
