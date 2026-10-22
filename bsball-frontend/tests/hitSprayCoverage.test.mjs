// 落点入队覆盖静态断言（issue #279）：确保各结果分支与对话框路径都调用入队
// 说明：LiveGame.js 为 266KB 单文件记分台，纯静态断言防回归（与 liveGameSacrificeChoices 同法）
// 运行：node --test tests/hitSprayCoverage.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('轨迹参数表与配色已改为共享模块导入，内联表已移除', () => {
  assert.match(source, /import \{ battedShapeColor, battedShapeOf as LgBattedShapeOf, isHomeRunBip as LgIsHomeRun \} from '\.\.\/\.\.\/utils\/battedBallFlight';/);
  assert.doesNotMatch(source, /const BAT_SHAPES=\{/, '不应再保留内联 BAT_SHAPES 表');
  assert.doesNotMatch(source, /function battedShapeOf\(e\)\{/, '不应再保留本地 battedShapeOf 副本');
  assert.match(source, /LgBattedShapeOf\(a\)/, '录入动画应使用共享 battedShapeOf（含 HR 最慢时长）');
  assert.match(source, /battedShapeColor\(a\)/, '录入动画轨迹应按球种取色');
  assert.match(source, /style:c\(battedTrail\)\.color\?\{/, '轨迹 path 应绑定球种颜色');
});

test('推送逻辑已提取为 pushHitSpray，bt 主链经 s 包装调用', () => {
  assert.match(source, /function pushHitSpray\(resultCode,rbi,spray\)\{/);
  assert.match(source, /const s=\(b,w=0\)=>pushHitSpray\(b,w,a\);/);
  assert.match(source, /import \{ buildHitSprayRecord as LgHitSprayRecord \} from '\.\.\/\.\.\/utils\/hitSprayRecord';/);
});

test('bt 主链各结果分支均入队（含此前缺失的 7 类）', () => {
  // 既有覆盖（回归保护）
  for (const code of ['H1', 'H2', 'H3', 'HR']) {
    assert.match(source, new RegExp(`s\\("${code}"`), `${code} 分支应入队`);
  }
  assert.match(source, /s\(g,0\)/, 'G/F/L/TAG 出局经变量 g 入队');
  // 本次补齐
  assert.match(source, /s\("IPHR",b\)/, 'IPHR（场内全垒打）应入队');
  assert.match(source, /s\("SF",sfRbi\?1:0\)/, 'SF（高飞牺牲）应入队且打点取实际值');
  assert.match(source, /s\("SH",sacrificeRunDecision\.creditRbi\?1:0\)/, 'SH（牺牲触击）按正常防守判定入队');
  assert.match(source, /s\("E",0\)/, 'E（失误上垒）主链应入队');
  assert.match(source, /s\("DP",0\)/, 'DP（双杀）主链应入队');
  assert.match(source, /s\("TP",0\)/, 'TP（三杀）主链应入队，结果码为 TP');
});

test('对话框路径均入队：dp / fp / fcApply', () => {
  assert.match(source, /pushHitSpray\(e\.playType,0,e\.opts\);/, 'fp（野选系对话框）应入队');
  assert.match(source, /pushHitSpray\(dpException\?"FC":e\.playType,0,e\.opts\);/, 'dp（跑者推进）应入队，双杀特例按 FC 入队');
  assert.match(source, /pushHitSpray\("E",0,a\);/, 'fcApply“没有跑者出局（失误）”分支应入队 E');
  assert.match(source, /pushHitSpray\("FC",0,a\);/, 'fcApply 正常野选路径应入队 FC');
});

test('落点拖拽范围经共享工具收敛，不再硬编码 0–480 / 0–640', () => {
  assert.match(source, /import \{ clampHitSpotToField as LgClampHitSpot \} from '\.\.\/\.\.\/utils\/hitSprayField';/);
  assert.match(source, /LgClampHitSpot\(r\.x,r\.y\)/, '拖拽坐标应经 clampHitSpotToField 处理');
  assert.doesNotMatch(source, /Math\.max\(0,Math\.min\(480,r\.x\)\)/, '旧的 0–480 硬编码应移除');
});

test('击球落点热力图使用与录入页一致的完整球场坐标', () => {
  assert.doesNotMatch(source, /viewBox:`0 0 \$\{u\.viewBoxWidth\} \$\{u\.viewBoxHeight\}`/, '热力图不应再用 0 0 480 640 裁剪扇形两翼');
  assert.ok((source.match(/viewBox:"-300 -150 1080 780"/g) ?? []).length >= 2, '录入页与热力图都应使用完整球场 viewBox');
});

test('越墙全垒打：落点外推、彩虹粗线、全程火花与落点烟花已接入', () => {
  assert.match(source, /import \{ projectHomeRunLanding as LgProjectHrLanding, playHomeRunFireworks as LgPlayHrFireworks, spawnHomeRunSideSparks as LgSpawnHrSparks/);
  assert.match(source, /HR_TRAIL_STOPS as LgHrTrailStops/, '录入页应复用共享彩虹渐变');
  assert.match(source, /homeRunFieldPos as LgHrFieldPos/, '垒球 XF 落点应映射到标准野位');
  assert.match(source, /LgHrFieldPos\(e\)/, '点击外野手的 HR 记录野位应经过映射');
  assert.match(source, /LgHrFieldPos\(t\.pos\)/, '拖拽的 HR 记录野位应经过映射');
  assert.match(source, /LgProjectHrLanding\(s\.x,s\.y,e\)/, '点击外野手时应把 HR 落点外推');
  assert.match(source, /LgProjectHrLanding\(y,b,t\.pos\)/, '拖拽松手时应把扇形内落点外推');
  assert.match(source, /LgProjectHrLanding\(p,g,t\.pos\)/, '拖拽过程应预览墙外落点');
  assert.match(source, /LgIsHomeRun\(a\)/, '飞行动画应识别越墙 HR');
  assert.match(source, /batted-trail--hr/, 'HR 轨迹应使用粗线样式');
  const css = readFileSync(new URL('../src/styles/legacy/live-game.css', import.meta.url), 'utf8');
  assert.match(css, /\.batted-trail\.batted-trail--hr\{[^}]*stroke-width:7/, 'HR 粗线需压过带 scope 属性的基础轨迹样式');
  assert.match(source, /hr-fireworks-layer/, '应存在 HR 烟花图层');
  assert.match(source, /HR落点/, '拖拽时应显示 HR 落点预览');
});
