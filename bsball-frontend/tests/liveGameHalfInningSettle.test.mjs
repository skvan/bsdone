// 半局定案推送静态护栏：翻局之前该半局必须已定案入库，定案作废的人工覆写要回传名单。
// 背景：局中每个打席的推送都是预览（后端不删旧判定、不作废覆写），只有半局收尾那一次
// 才定案。收尾漏掉时，该半局在库里零判定而记分栏已有分，之后每个半局的定案推送都会
// 被「重建数 < 官方总分」顶回 409（线上 game 261 即此型）。
// 运行：node --test tests/liveGameHalfInningSettle.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const liveGame = fs.readFileSync(
  new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const result = fs.readFileSync(
  new URL('../../bsball-backend/src/main/java/com/bsball/stats/earnedrun/EarnedRunReconstructionResult.java', import.meta.url),
  'utf8');

/** 去掉整行注释后的正文：注释里提到的符号不该被当成调用入口。 */
function codeOnly(source) {
  return source.split('\n').filter((line) => !line.trim().startsWith('//')).join('\n');
}

test('翻局入口在文字上都带定案兜底（重进场次是否真推见 #712）', () => {
  // 本用例只证明「两个翻局入口都接了兜底表达式」，证明不了兜底一定开火：
  // resyncEarnedRunHalfInningForSave() 在本地 playEvents 不可信（engineManaged 场次重进）时
  // 直接 return null，那个缺口由 #712 跟踪，不要把它当成已修。
  const body = codeOnly(liveGame);
  const sites = [...body.matchAll(/Qa\(!0\)/g)];
  assert.equal(sites.length, 2, '翻局入口应只有「第三出局」与「强制换局」两处');
  for (const site of sites) {
    const before = body.slice(Math.max(0, site.index - 300), site.index);
    assert.ok(
      /Qr\(\)/.test(before)
        || /flushPendingRunsToPlayEvent\(\)\|\|resyncEarnedRunHalfInningForSave\(\)/.test(before),
      '发现未定案就翻局的入口：' + before.slice(-80)
    );
  }
});

test('第三出局走逐棒推送：halfComplete 按实际出局数判定，不足三出仍是预览', () => {
  assert.match(liveGame, /f\.playEvents\.push\(E\),syncEarnedRunHalfInning\(E,\{halfComplete:l\.outs>=3\}\)/,
    '逐棒推送必须按实际出局数决定是否定案');
  assert.match(liveGame, /const halfComplete=options\.halfComplete!==false/,
    '未声明该选项的调用点（保存、换局、补台账）一律视为定案');
});

test('强制换局与保存链共用同一兜底写法', () => {
  const uses = liveGame.match(/flushPendingRunsToPlayEvent\(\)\|\|resyncEarnedRunHalfInningForSave\(\)/g) ?? [];
  assert.equal(uses.length, 2, '保存与强制换局各一处：台账补不到就按当前半局绝对重建');
});

test('定案作废的人工覆写名单回传前端，且只在定案时提示一次', () => {
  assert.match(result, /List<Long> overrideInvalidationPitcherIds/, '后端回包必须带名单');
  assert.match(liveGame, /f\.earnedRunReconstruction\?\.overrideInvalidationPitcherIds/, '前端读取同名字段');
  assert.match(liveGame, /if\(halfComplete&&Array\.isArray\(invalidated\)&&invalidated\.length\)/,
    '只在半局定案且名单非空时提示，免得局中每个打席都弹');
});
