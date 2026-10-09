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
  assert.match(menu, /u\.currentOuts<2&&u\.runnerOnThird\?\([\s\S]*?I\("bip:fly:sf"\)/, 'SF 门控：出局数<2 且三垒有人');
  assert.ok(!menu.includes(',u.runnerOnThird?(h()'), 'SF 旧形态（无出局数门控）不得回退');
  assert.match(menu, /高飞牺牲（SF）/);
  assert.match(source, /case"bip:fly:sf":Yt\(e,"SF"\)/);
});

test('bunt result menu offers SH when runners are aboard and routes to the SH play flow', () => {
  const menu = menuSection('M.value==="bip_bunt"?', 'M.value==="bip_bunt_bo"?');
  assert.match(menu, /u\.currentOuts<2&&he\.value\?\([\s\S]*?I\("bip:b:sh"\)/, 'SH 门控：出局数<2 且有人上垒');
  assert.ok(!menu.includes('))])):W("",!0),he.value?(h(),C("button",{'), 'SH 旧形态（无出局数门控）不得回退');
  assert.match(menu, /牺牲触击（SH）/);
  assert.match(source, /case"bip:b:sh":Yt\(e,"SH"\)/);
});

test('the fly-ball menu receives actual third-base occupancy and sacrifice outcomes update batter stats', () => {
  assert.match(source, /"runner-on-third":!!l\.runners\[3\]/);
  assert.match(source, /r\.stats\.batting\.sf=\(r\.stats\.batting\.sf\?\?0\)\+1/);
  assert.match(source, /r\.stats\.batting\.sh=\(r\.stats\.batting\.sh\?\?0\)\+1/);
});
