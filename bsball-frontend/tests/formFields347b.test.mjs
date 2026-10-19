// 静态断言：Issue #347 PR B——LiveGame 自动写 + 门户守备视图
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const live = read('../src/views/admin/LiveGame.js');
const detail = read('../src/components/admin/GameDetailContent.js');
const css = read('../src/styles/legacy/game-detail-content.css');

test('LiveGame 默认值包含纸质表单新字段', () => {
  for (const needle of [
    'gdp:0,io:0',
    'pitchBb:0,pitchIbb:0,pitchHbp:0',
    'pitchDoubles:0,pitchTriples:0,pitchSh:0,pitchSf:0,pitchIo:0',
    'go:0,fo:0',
    'dp:0,pb:0,catcherSb:0,catcherCs:0'
  ]) {
    assert.ok(live.includes(needle), `Pt 缺少 ${needle}`);
  }
});

test('LiveGame 保存映射包含新字段且 PA 含 IO', () => {
  assert.ok(live.includes('(s.io??0)'), 'PA 未计入 IO');
  assert.ok(live.includes('gdp:s.gdp??0'), '未保存 GDP');
  assert.ok(live.includes('io:s.io??0'), '未保存 IO');
  assert.ok(live.includes('pitchDoubles:r.pitchDoubles??0'), '未保存 pitchDoubles');
  assert.ok(live.includes('pitchTriples:r.pitchTriples??0'), '未保存 pitchTriples');
  assert.ok(live.includes('pitchSh:r.pitchSh??0'), '未保存 pitchSh');
  assert.ok(live.includes('pitchSf:r.pitchSf??0'), '未保存 pitchSf');
  assert.ok(live.includes('pitchIo:r.pitchIo??0'), '未保存 pitchIo');
  assert.ok(live.includes('pitchBb:r.pitchBb??0'), '未保存 pitchBb');
  assert.ok(live.includes('pitchIbb:r.pitchIbb??0'), '未保存 pitchIbb');
  assert.ok(live.includes('pitchHbp:r.pitchHbp??0'), '未保存 pitchHbp');
  assert.ok(live.includes('pitchBbHp:(r.pitchBb??0)+(r.pitchHbp??0)'), 'BBHP 未按分项计算');
  assert.ok(live.includes('defInn:p.ip??0'), '未保存守备局数');
  assert.ok(live.includes('dp:p.dp??0'), '未保存守备 DP');
  assert.ok(live.includes('catcherSb:p.catcherSb??0'), '未保存捕手 SB允许');
  assert.ok(live.includes('catcherCs:p.catcherCs??0'), '未保存捕手 CS');
});

test('LiveGame 事件自动累加', () => {
  assert.ok(live.includes('function paceGoFo'), '缺少 GO/FO 累加');
  assert.ok(live.includes('function paceCatcherStat'), '缺少捕手数据累加');
  for (const needle of [
    'p.stats.pitching.pitchDoubles=(p.stats.pitching.pitchDoubles??0)+1',
    'p.stats.pitching.pitchTriples=(p.stats.pitching.pitchTriples??0)+1',
    'p.stats.pitching.pitchSf=(p.stats.pitching.pitchSf??0)+1',
    'p.stats.pitching.pitchSh=(p.stats.pitching.pitchSh??0)+1',
    'r.stats.batting.gdp=(r.stats.batting.gdp??0)+1',
    'a.stats.batting.io=(a.stats.batting.io??0)+1',
    'paceCatcherStat("catcherSb")',
    'paceCatcherStat("catcherCs")',
    'paceCatcherStat("pb")'
  ]) {
    assert.ok(live.includes(needle), `事件累加缺少 ${needle}`);
  }
  assert.ok(live.includes('type==="DP"&&!seen.has(r.pos)'), '守备 DP 累加缺失');
});

test('门户比赛详情具备打者/投手/守备切换', () => {
  assert.ok(detail.includes('import GameDetailFieldingTable'), '未导入守备表组件');
  assert.ok(detail.includes('const boxView=I("batting")'), '缺少视图状态');
  assert.ok(detail.includes('gd-box-view-tabs'), '缺少视图标签栏');
  assert.ok(detail.includes('boxView.value!=="batting"'), '打者区块未按视图条件渲染');
  assert.ok(detail.includes('boxView.value!=="pitching"'), '投手区块未按视图条件渲染');
  assert.ok(detail.includes('boxView.value!=="fielding"'), '守备区块未按视图条件渲染');
  assert.ok(detail.includes('gd-fielding-block'), '缺少守备区块');
  assert.ok(detail.includes('gameDetail.fielding'), '守备标签未使用词典');
  assert.ok(css.includes('.gd-box-view-tab'), '缺少视图标签样式');
  for (const key of ['batting', 'pitching', 'fielding']) {
    assert.ok(detail.includes(`p(tabLabel("gameDetail.${key}"),1),3)`), `${key} 标签缺少 text/class patch flag`);
  }
});
