// #524 逐位置明细接线：赛后详情拉取接口，实时观赛从快照 byPos 携带明细
// 运行：node --test tests/fieldingBreakdownWiring.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('#524 赛后详情：拉取逐位置接口并挂到统计行', () => {
  const source = read('../src/views/admin/GameDetail.js');
  assert.match(source, /attachFieldingPositions/);
  assert.match(source, /listFieldingPositions\(O\.value\)/);
  assert.match(source, /attachFieldingPositions\(b\.list\?\?\[\],fp\?\?\[\]\)/);
});

test('#524 门户详情：拉取逐位置接口并挂到统计行', () => {
  const source = read('../src/views/portal/PortalGameDetail.js');
  assert.match(source, /attachFieldingPositions/);
  assert.match(source, /listFieldingPositions\(e\)/);
  assert.match(source, /attachFieldingPositions\(C\.list,fp\?\?\[\]\)/);
});

test('#524 比赛 API：逐位置读取端点与列表解包', () => {
  const source = read('../src/api/business.js');
  assert.match(source, /listFieldingPositions: async \(id\) =>/);
  assert.match(source, /\/api\/game\/\$\{id\}\/fielding-positions/);
});

test('#524 实时观赛：统计行携带快照 byPos 与 startingPosition', () => {
  const source = read('../src/views/admin/LiveGameWatch.js');
  assert.match(source, /catcherCs:y\.catcherCs,fieldingByPos:y\.byPos\?\?null/);
  assert.match(source, /startingPosition:String\(s\.startingPosition\?\?""\)/);
});
