// 静态门禁：实时录入转播风记分牌与实时动态垒包/球数（#407）
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const scoreboard = read('../src/components/admin/LiveGameScoreboard.vue');
const liveGame = read('../src/views/admin/LiveGame.js');
const watch = read('../src/views/admin/LiveGameWatch.js');
const watchCss = read('../src/styles/legacy/live-game-watch.css');

test('顶部记分牌组件包含垒包、球数灯与打者/投手信息', () => {
  assert.match(scoreboard, /class="lg-scoreboard"/);
  assert.match(scoreboard, /lg-base/);
  assert.match(scoreboard, /\.lg-base \{[\s\S]*?border: 1px solid rgba\(148, 163, 184, 0\.75\);[\s\S]*?background: transparent;[\s\S]*?\}/);
  assert.match(scoreboard, /\.lg-base\.is-on \{[\s\S]*?background: var\(--lg-gold\);[\s\S]*?\}/);
  assert.match(scoreboard, /lg-light--ball/);
  assert.match(scoreboard, /lg-light--strike/);
  assert.match(scoreboard, /lg-light--out/);
  assert.match(scoreboard, /lg-actor-main/);
  assert.match(scoreboard, /lg-inning-arrows/);
  assert.match(scoreboard, /position: absolute/);
  assert.match(scoreboard, /right: 12px/);
  assert.match(scoreboard, /awayLogo/);
  assert.match(scoreboard, /homeLogo/);
  assert.match(scoreboard, /--lg-team-logo/);
  assert.match(scoreboard, /opacity: 0\.16/);
  assert.match(scoreboard, /aria-live="polite"/);
});

test('实时录入用右上角浮层替换旧顶部文本条，并透传球队 Logo', () => {
  assert.match(liveGame, /import LiveGameScoreboard from '\.\.\/\.\.\/components\/admin\/LiveGameScoreboard\.vue'/);
  assert.match(liveGame, /class:re\(\["field-svg-wrap"[\s\S]*v\(LiveGameScoreboard,/);
  assert.doesNotMatch(liveGame, /n\("div",Ag,\[v\(LiveGameScoreboard,/);
  assert.match(liveGame, /resolveUploadUrl\(At\.value\?\.logo/);
  assert.match(liveGame, /resolveUploadUrl\(xt\.value\?\.logo/);
  assert.doesNotMatch(liveGame, /les-teams|les-actors/);
});

test('记分牌 vnode 带 patchFlag 16，父组件重渲染时 props 才会更新', () => {
  assert.match(
    liveGame,
    /v\(LiveGameScoreboard,\{[^\n]*?np:Se\.value\?\.stats\?\.pitching\?\.np\?\?0\}\},null,16\)/
  );
});

test('实时动态两处打席均只保留 2 个好球灯与 3 个坏球灯', () => {
  const count = (pattern) => (watch.match(pattern) || []).length;
  assert.equal(count(/O\(2,o=>i\("span",\{\s*key:`s-/g), 2);
  assert.equal(count(/O\(3,o=>i\("span",\{\s*key:`b-/g), 2);
  assert.doesNotMatch(watch, /O\(4,o=>i\("span",\{/);
});

test('实时动态垒包有人实心、没人空心，球数灯保留发光', () => {
  assert.match(watchCss, /watch-column \.watch-bases__base\[data-v-80ab5033\]\{fill:#0000;stroke:#64748b\}/);
  assert.match(
    watchCss,
    /watch-column \.watch-bases__base--on\[data-v-80ab5033\]\{fill:#0b1220;stroke:#fbbf24;/
  );
  assert.match(watchCss, /fill:#fbbf24;stroke:#fbbf24/);
  assert.match(
    watchCss,
    /\[data-admin-theme=dark\] \.watch-column \.watch-bases__base\[data-v-80ab5033\][^}]*\{stroke:#94a3b8\}/
  );
  assert.match(watchCss, /\.pbp-sbo__strikes \.pbp-sbo__dot--active\[data-v-80ab5033\]\{box-shadow:/);
  assert.match(watchCss, /\.pbp-sbo__balls \.pbp-sbo__dot--active\[data-v-80ab5033\]\{box-shadow:/);
});

test('CSS 覆盖使用的 scopeId 与实时动态组件一致', () => {
  const scopeId = watchCss.match(/data-v-[0-9a-f]{8}/)[0];
  assert.ok(watch.includes(`["__scopeId","${scopeId}"]`));
});
