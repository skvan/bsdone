// 回归：未认领球员被邀请时直接入队（不生成链接），已认领球员仍走通用链接
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const dialog = fs.readFileSync(
  new URL('../src/components/admin/InvitePlayerDialog.vue', import.meta.url),
  'utf8'
);

function generateInviteBody() {
  const start = dialog.indexOf('async function generateInvite');
  const end = dialog.indexOf('async function copyLink', start);
  assert.ok(start >= 0 && end > start, '未找到 generateInvite 函数');
  return dialog.slice(start, end);
}

test('未认领候选：直接入队并刷新候选，不弹邀请链接', () => {
  const body = generateInviteBody();
  const directStart = body.indexOf('if (!row.claimed)');
  assert.ok(directStart >= 0, '未找到未认领分支');
  const directEnd = body.indexOf('// 已认领', directStart);
  assert.ok(directEnd > directStart, '未找到未认领分支的结束位置');
  const direct = body.slice(directStart, directEnd);
  assert.match(direct, /playerId:\s*row\.id/, '未认领分支未绑定球员档案');
  assert.match(direct, /runSearch\(\)/, '直接入队后未刷新候选列表');
  assert.ok(!direct.includes('linkVisible.value = true'), '未认领分支仍弹出邀请链接');
});

test('已认领候选：仍生成通用链接', () => {
  const body = generateInviteBody();
  assert.match(body, /playerId:\s*null/, '已认领分支未使用通用链接');
  assert.match(body, /linkVisible\.value = true/, '已认领分支未展示邀请链接');
});

test('按钮文案区分未认领（邀请入队）与已认领（生成邀请）', () => {
  const buttonStart = dialog.indexOf('@click="generateInvite(row)"');
  const button = dialog.slice(
    dialog.lastIndexOf('<el-button', buttonStart),
    dialog.indexOf('</el-button>', buttonStart)
  );
  assert.match(button, /row\.claimed/, '按钮文案未按认领状态区分');
  assert.match(button, /invitePlayer\.directJoin/, '缺少未认领直接入队文案');
});

test('五种语言均包含直接入队文案', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja', 'ko']) {
    const json = JSON.parse(
      fs.readFileSync(new URL(`../src/locales/${locale}.json`, import.meta.url), 'utf8')
    );
    assert.ok(json.invitePlayer?.directJoin, `${locale} 缺少 directJoin`);
    assert.ok(json.invitePlayer?.directJoinDone, `${locale} 缺少 directJoinDone`);
    assert.ok(json.invitePlayer?.alreadyJoined, `${locale} 缺少 alreadyJoined`);
  }
});
