// 静态门禁：实时录入顶部操作行整理 + 全屏场地工具行（#359 后续）
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const liveGame = read('../src/views/admin/LiveGame.js');
const css = read('../src/styles/legacy/live-game.css');

test('移除局数/球数条，但保留流程提示卡', () => {
  assert.doesNotMatch(liveGame, /C\("div",vg,/);
  assert.doesNotMatch(liveGame, /"第 "\+H\(c\(l\)\.inning\)\+" 局/);
  assert.doesNotMatch(liveGame, /"aria-label":"当前比分与球数"/);
  assert.match(liveGame, /c\(F\)&&Pp\.value\?\(h\(\),C\("div",mg,\[Pp\.value\?/);
  assert.match(liveGame, /class:"live-entry-flow-wrap"/);
  assert.match(liveGame, /les-flow-card/);
  assert.match(css, /\.live-entry-flow-wrap\[data-v-78b7b6a8\]\{margin-bottom:12px\}/);
});

test('三控件进 header；全屏场地行保留三控件与撤销/重做，顶栏只留退出', () => {
  const undoCount = (liveGame.match(/Q\("↩ 撤销",-1\)/g) || []).length;
  const redoCount = (liveGame.match(/Q\("↪ 重做",-1\)/g) || []).length;
  assert.equal(undoCount, 2);
  assert.equal(redoCount, 2);

  const headerStart = liveGame.indexOf('n("div",og,[');
  const headerEnd = liveGame.indexOf('c(F)&&c(R)?', headerStart);
  const header = liveGame.slice(headerStart, headerEnd);
  assert.match(header, /key:"undo",size:"small",disabled:(?:substitutionMode\.value\|\|)?!c\(f\)\.canUndo,onClick:as/);
  assert.match(header, /key:"redo",size:"small",disabled:(?:substitutionMode\.value\|\|)?!c\(f\)\.canRedo,onClick:Ff/);
  assert.match(header, /key:"entry-tools"/);
  assert.match(header, /场上设置/);
  assert.match(header, /击球落点分布/);
  assert.ok(header.indexOf('Q("↩ 撤销"') < header.indexOf('key:"entry-tools"'));
  assert.ok(liveGame.indexOf('key:"entry-tools"') < liveGame.indexOf('Q(" 实时观赛 "'));
  assert.match(liveGame, /c\(F\)&&!c\(oe\)\?\(h\(\),C\(me,\{key:"entry-tools"\}/);

  assert.match(liveGame, /c\(oe\)\?\(h\(\),C\("div",Og,/);
  assert.doesNotMatch(liveGame, /key:"fs-undo"|key:"fs-redo"/);
  assert.match(liveGame, /C\("div",cg,\[v\(s,\{/);

  const rowStart = liveGame.indexOf('n("div",Ug,[');
  const rowEnd = liveGame.indexOf(',n("div",{', rowStart);
  const fieldRow = liveGame.slice(rowStart, rowEnd);
  assert.match(fieldRow, /↩ 撤销/);
  assert.match(fieldRow, /↪ 重做/);
  assert.match(fieldRow, /击球落点分布/);

  const fieldStart = liveGame.indexOf('c(oe)?(h(),C("div",Og,[');
  const fieldTools = liveGame.slice(fieldStart, liveGame.indexOf('class:re(["field-svg-wrap"', fieldStart));
  const cacheKeys = (src) => new Set([...src.matchAll(/t\[(\d+)\]/g)].map((m) => m[1]));
  const fieldKeys = cacheKeys(fieldTools);
  const shared = [...cacheKeys(header)].filter((key) => fieldKeys.has(key));
  assert.deepEqual(shared, [], `header 与全屏场地工具行不能共用静态缓存索引：${shared.join(",")}`);
});

test('header 换行/全屏隐藏，场地按可用高度等比放大', () => {
  assert.match(
    css,
    /\.header-actions\[data-v-78b7b6a8\]\{[^}]*flex-wrap:wrap;justify-content:flex-end\}/
  );
  assert.match(
    css,
    /\.live-game-page:has\(\.card-body-fullscreen\) \.header-actions\[data-v-78b7b6a8\]\{display:none\}/
  );
  assert.match(
    css,
    /\.card-body-fullscreen \.field-svg-wrap\[data-v-78b7b6a8\]\{[^}]*align-items:center[^}]*\}/
  );
  assert.match(
    css,
    /\.card-body-fullscreen \.field-svg-wrap>\.ballpark-svg\[data-v-78b7b6a8\]\{flex:1 1 auto;min-height:200px;width:auto;height:100%;max-width:100%!important;margin:0 auto\}/
  );
});
