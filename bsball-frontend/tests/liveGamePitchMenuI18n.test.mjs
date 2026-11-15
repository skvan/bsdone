// 实时录入投球菜单英文模式守护
// 运行：node --test tests/liveGamePitchMenuI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(
  new URL('../src/components/admin/LiveGamePitchMenu.vue', import.meta.url),
  'utf8'
);

test('投球菜单：英文模式隐藏中文并显示英文', () => {
  assert.ok(source.includes('useI18n'), '缺少 useI18n');
  assert.ok(source.includes('const isEnglish'), '缺少 isEnglish');
  assert.ok(source.includes("'lgpm--en': isEnglish"), '根节点未切换英文模式');
  assert.ok(source.includes('.lgpm--en .lgpm__cn'), '缺少英文模式隐藏中文样式');
  assert.ok(source.includes('.lgpm--en .lgpm__en'), '缺少英文模式主文案样式');
});

test('投球菜单：标题、状态、结果与提示均有英文分支', () => {
  for (const text of [
    'Pitch & Ruling',
    'Pitch Call',
    'Dropped 3rd Strike',
    'In Play',
    'Pick pitch type → record result',
    'Tap to record · › opens the next step',
    'Safe on First (WP)',
  ]) {
    assert.ok(source.includes(text), `缺少英文文案 ${text}`);
  }
  assert.ok(source.includes("isEnglish ? item.en : item.cn"), '结果按钮未按语言切换');
  assert.ok(source.includes('chip.labelEn'), '筹码标签未按语言切换');
  assert.ok(source.includes('chip.fullEn'), '筹码提示未按语言切换');
  assert.ok(source.includes('badgeEn'), '徽标未按语言切换');
  assert.ok(source.includes("(isTop ? 'T' : 'B') + inning"), '局数状态未英文缩写');
});

test('投球菜单：中文模式保留原文案', () => {
  assert.ok(source.includes('投球与裁定'));
  assert.ok(source.includes('投球判定'));
  assert.ok(source.includes('点选项即记录'));
  assert.ok(source.includes('关闭'));
  assert.ok(source.includes('漏接三振'));
});

test('投球菜单：六个界内球球种都有英文名，窄屏英文标题仍显示', () => {
  const block = source.match(/const BIP_RESULTS = \{([\s\S]*?)\n\};/)?.[1] || '';
  assert.ok(block, '未找到 BIP_RESULTS');
  for (const key of ['bip_g', 'bip_hg', 'bip_fly', 'bip_ld', 'bip_bunt', 'bip_pop']) {
    const start = block.indexOf(`${key}: {`);
    assert.ok(start >= 0, `缺少 ${key}`);
    const end = block.indexOf('\n  },', start);
    const entry = block.slice(start, end < 0 ? undefined : end);
    assert.ok(entry.includes("en: '"), `${key} 缺少英文名，英文面包屑会出现 undefined`);
  }
  assert.ok(source.includes('<h2 v-if="!isEnglish" class="lgpm__title">'), '中文标题未按语言隐藏');
  assert.match(source, /\.lgpm--en \.lgpm__title-en \{[^}]*display: block;/, '英文标题在窄屏可能被隐藏');
});
