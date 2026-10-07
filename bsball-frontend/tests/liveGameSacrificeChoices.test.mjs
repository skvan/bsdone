import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

function menuSection(start, end) {
  const from = source.indexOf(start);
  const to = source.indexOf(end, from + start.length);
  assert.notEqual(from, -1, `menu start exists: ${start}`);
  assert.notEqual(to, -1, `menu end exists: ${end}`);
  return source.slice(from, to);
}

test('fly-ball result menu offers SF only with a runner on third and routes to the SF play flow', () => {
  const menu = menuSection('M.value==="bip_fly"?', 'M.value==="bip_fly_bo"?');
  assert.match(menu, /runnerOnThird\?\([\s\S]*?I\("bip:fly:sf"\)/);
  assert.match(menu, /牺牲高飞打（SF）/);
  assert.match(source, /case"bip:fly:sf":Yt\(e,"SF"\)/);
});

test('bunt result menu offers SH when runners are aboard and routes to the SH play flow', () => {
  const menu = menuSection('M.value==="bip_bunt"?', 'M.value==="bip_bunt_bo"?');
  assert.match(menu, /he\.value\?\([\s\S]*?I\("bip:b:sh"\)/);
  assert.match(menu, /牺牲触击（SH）/);
  assert.match(source, /case"bip:b:sh":Yt\(e,"SH"\)/);
});

test('the fly-ball menu receives actual third-base occupancy and sacrifice outcomes update batter stats', () => {
  assert.match(source, /"runner-on-third":!!l\.runners\[3\]/);
  assert.match(source, /r\.stats\.batting\.sf=\(r\.stats\.batting\.sf\?\?0\)\+1/);
  assert.match(source, /r\.stats\.batting\.sh=\(r\.stats\.batting\.sh\?\?0\)\+1/);
});
