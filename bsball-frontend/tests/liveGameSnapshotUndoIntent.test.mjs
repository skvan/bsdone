// #713 清零放行的显式意图：撤销/重做后的快照保存带 allowEvidenceShrink，
// 旧设备/未标记的本地归零存档仍按空壳拒收。
// 运行：node --test tests/liveGameSnapshotUndoIntent.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const liveGame = fs.readFileSync(
  new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const business = fs.readFileSync(
  new URL('../src/api/business.js', import.meta.url), 'utf8');

test('撤销/重做后的快照保存带 allowEvidenceShrink，成功后清位', () => {
  assert.match(liveGame, /let allowEvidenceShrink=false;/, '默认必须不带意图');
  assert.match(liveGame, /function qn\(G\)\{[\s\S]{0,200}allowEvidenceShrink=true;/,
    'applyHistoryEntry 是撤销/重做/历史恢复的唯一入口，必须在这里置位');
  assert.match(liveGame, /ma\.saveLiveSnapshot\(R\.value,body,allowEvidenceShrink\)/,
    '现场快照推送必须把意图带给后端');
  assert.match(liveGame, /allowEvidenceShrink=false;[\s\S]{0,40}return !0/,
    '成功写入后清位，下一发普通保存恢复默认拒收口径');
  assert.match(liveGame, /ma\.saveLiveSnapshot\(R\.value,JSON\.stringify\(e\),allowEvidenceShrink\)/,
    '离开时轻量快照同样带意图');
});

test('API 把 allowEvidenceShrink 放进 live-snapshot 请求体', () => {
  assert.match(business,
    /saveLiveSnapshot: \(id, snapshotJson, allowEvidenceShrink\) => post\(`\/api\/game\/\$\{id\}\/live-snapshot`, \{ snapshotJson, allowEvidenceShrink \}\)/,
    '请求体必须带意图字段，缺省 undefined 时后端按 false 处理');
});
