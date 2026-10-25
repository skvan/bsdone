// 静态门禁：实时录入顶部操作行整理 + 移除局数/球数条（#359 后续）
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

test('撤销/重做同时出现在顶部操作行与全屏退出栏，场地行不再重复', () => {
  const undoCount = (liveGame.match(/Q\("↩ 撤销",-1\)/g) || []).length;
  const redoCount = (liveGame.match(/Q\("↪ 重做",-1\)/g) || []).length;
  assert.equal(undoCount, 2);
  assert.equal(redoCount, 2);

  const headerStart = liveGame.indexOf('n("div",og,[');
  const headerEnd = liveGame.indexOf('c(F)&&c(R)?', headerStart);
  const header = liveGame.slice(headerStart, headerEnd);
  assert.match(header, /key:"undo",size:"small",disabled:!c\(f\)\.canUndo,onClick:as/);
  assert.match(header, /key:"redo",size:"small",disabled:!c\(f\)\.canRedo,onClick:Ff/);
  assert.ok(liveGame.indexOf('Q("↩ 撤销"') < liveGame.indexOf('Q(" 实时观赛 "'));

  const fsStart = liveGame.indexOf('C("div",cg,[');
  const fsEnd = liveGame.indexOf('v(s,{', fsStart);
  const fullscreen = liveGame.slice(fsStart, fsEnd);
  assert.match(fullscreen, /key:"fs-undo"/);
  assert.match(fullscreen, /key:"fs-redo"/);

  const rowStart = liveGame.indexOf('n("div",Ug,[');
  const rowEnd = liveGame.indexOf(',n("div",{', rowStart);
  const fieldRow = liveGame.slice(rowStart, rowEnd);
  assert.match(fieldRow, /击球落点分布/);
  assert.doesNotMatch(fieldRow, /撤销|重做/);
});

test('顶部操作组允许换行，窄屏不溢出；全屏时隐藏 header 操作组', () => {
  assert.match(
    css,
    /\.header-actions\[data-v-78b7b6a8\]\{[^}]*flex-wrap:wrap;justify-content:flex-end\}/
  );
  assert.match(
    css,
    /\.live-game-page:has\(\.card-body-fullscreen\) \.header-actions\[data-v-78b7b6a8\]\{display:none\}/
  );
});
