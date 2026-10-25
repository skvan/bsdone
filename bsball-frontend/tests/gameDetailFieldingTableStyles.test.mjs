// 静态门禁：守备表子组件补齐与击球/投手表一致的 box-table 表头样式（#417）
// 运行：node --test tests/gameDetailFieldingTableStyles.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

test('守备表子组件声明 side 并应用队色类', () => {
  const vue = read('../src/components/admin/GameDetailFieldingTable.vue');
  assert.ok(/side:\s*\{\s*type:\s*String/.test(vue), '缺少 side prop');
  assert.ok(vue.includes('box-table-${props.side}'), '未按 side 生成队色类');
  assert.ok(vue.includes("'box-table-fielding'"), '缺少守备表类');
});

test('守备表用 scoped deep 补齐 box-table 表头与单元格样式', () => {
  const vue = read('../src/components/admin/GameDetailFieldingTable.vue');
  for (const need of [
    ':deep(.el-table__header th)',
    ':deep(th.el-table__cell)',
    ':deep(td.el-table__cell)',
    '#ff9595',
    '#ffc000',
    '@media (max-width: 768px)',
  ]) {
    assert.ok(vue.includes(need), `缺少样式：${need}`);
  }
});

test('父组件给两处守备表传 away/home，且与 team-id 配对', () => {
  const parent = read('../src/components/admin/GameDetailContent.js');
  const start = parent.indexOf('class:"gd-fielding-block"');
  assert.ok(start >= 0, '未找到守备表块');
  const block = parent.slice(start, start + 1200);
  assert.match(
    block,
    /"team-id":c\.game\.awayTeamId[^}]*fields:c\.fieldingFields,side:"away"/,
    '客队守备表 side 未配对'
  );
  assert.match(
    block,
    /"team-id":c\.game\.homeTeamId[^}]*fields:c\.fieldingFields,side:"home"/,
    '主队守备表 side 未配对'
  );
});
