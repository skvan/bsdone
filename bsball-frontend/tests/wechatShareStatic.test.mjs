import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (p) => readFileSync(path.join(root, p), 'utf8');

test('request.js：埋点来源提取接线', () => {
  const src = read('src/api/request.js');
  assert.match(src, /from '\.\.\/utils\/shareSource'/);
  assert.match(src, /extractShareSource\(typeof window/);
  assert.match(src, /source \? \{ path, source \} : \{ path \}/);
});

test('PlayerDetailContent：微信分享接线锚点', () => {
  const src = read('src/components/admin/PlayerDetailContent.js');
  assert.match(src, /useWechatShare\(wxShareMeta\)/);
  assert.match(src, /buildPlayerShareUrl\(wxRouter/);
  assert.match(src, /buildPlayerShareMeta\(L\.player, L\.team, L\.league, be\.value/);
  assert.match(src, /手工补丁/);
  assert.match(src, /share-wechat-tip/);
  assert.match(src, /share-wechat-guide/);
  assert.match(src, /buildPlayerShareUrl\(wxRouter, L\.player\.id, "share", x2\.value\)/);
});

test('locales：微信分享文案键（zh-CN/zh-TW）', () => {
  const keys = ['wechatTip', 'wechatGuideTitle', 'wechatGuideBody', 'wechatGuideOk'];
  for (const f of ['src/locales/zh-CN.json', 'src/locales/zh-TW.json']) {
    const raw = read(f);
    for (const key of keys) assert.match(raw, new RegExp(`"${key}"`));
    const card = JSON.parse(raw).player?.shareCard || {};
    for (const key of keys) {
      assert.equal(typeof card[key], 'string', `${f} player.shareCard.${key} 应为字符串`);
      assert.ok(card[key].length > 0, `${f} player.shareCard.${key} 不得为空`);
    }
  }
});

test('player-detail-content.css：微信提示条与引导浮层样式（弹窗 scope）', () => {
  const css = read('src/styles/legacy/player-detail-content.css');
  assert.match(css, /\.share-wechat-tip\[data-v-4a548fe0\]/);
  assert.match(css, /\.share-wechat-tip__text\[data-v-4a548fe0\]/);
  assert.match(css, /\.share-wechat-guide\[data-v-4a548fe0\]/);
  assert.match(css, /\.share-wechat-guide__ok\[data-v-4a548fe0\]/);
});

test('PlayerShareCard/Dialog：二维码与品牌区补丁锚点', () => {
  const src = read('src/components/admin/PlayerDetailContent.js');
  assert.match(src, /import QrCode from 'qrcode';/);
  assert.match(src, /card-qr/);
  assert.match(src, /qrUrl:wxQr\.value/);
  assert.match(src, /QrCode\.toDataURL/);
  assert.match(src, /posterUrl/);
  assert.match(src, /poster-url/);
  assert.match(src, /"bat-throw-line","poster-url"\]/);
  assert.match(src, /fe\(\(\)=>\[m\.value,r\.posterUrl\],/);
  assert.match(src, /let wxQrSeq = 0;/);
  assert.match(src, /seq===wxQrSeq&&\(wxQr\.value=q\)/);
  assert.match(src, /ue\.value=!1\s+wxGuide\.value=!1;/);
  const pkg = JSON.parse(read('package.json'));
  assert.match(pkg.dependencies.qrcode, /^\^1\.5\./);
  const css = read('src/styles/legacy/player-detail-content.css');
  assert.match(css, /\.card-qr\[data-v-9d93789c\]/);
  assert.match(css, /\.card-qr\[data-v-9d93789c\] \{[^}]*z-index: 4;[^}]*pointer-events: none;[^}]*right: 48px;[^}]*bottom: 220px;/);
  assert.match(css, /\.card-qr-img\[data-v-9d93789c\] \{[^}]*width: 150px;[^}]*height: 150px;/);
  assert.match(css, /\.player-share-card--landscape \.card-qr\[data-v-9d93789c\] \{[^}]*right: 56px;[^}]*bottom: 360px;/);
  assert.match(css, /\.player-share-card--landscape \.card-qr-img\[data-v-9d93789c\] \{[^}]*width: 160px;[^}]*height: 160px;/);
  for (const f of ['src/locales/zh-CN.json', 'src/locales/zh-TW.json']) {
    assert.match(read(f), /"qrCaption"/);
    assert.match(read(f), /"qrBrand"/);
  }
});

test('PortalPlayerDetail：回流横幅补丁锚点', () => {
  const src = read('src/views/portal/PortalPlayerDetail.js');
  assert.match(src, /share-landing-banner/);
  assert.match(src, /extractShareSource/);
  assert.match(src, /手工补丁/);
  assert.match(src, /bs-share-landing-dismissed/);
  assert.match(src, /sessionStorage\.setItem\("bs-share-landing-dismissed", "1"\)/);
  const css = read('src/styles/legacy/portal-player-detail.css');
  assert.match(css, /\.share-landing-banner\[data-v-4b84f8d2\]/);
  assert.match(css, /\.share-landing-banner__text\[data-v-4b84f8d2\]/);
});

test('PortalVisitHitList：来源列补丁锚点', () => {
  const src = read('src/views/admin/PortalVisitHitList.js');
  assert.match(src, /label:"来源"/);
  assert.match(src, /分享链接/);
  assert.match(src, /二维码海报/);
  assert.match(src, /o\.source==="share"\?"分享链接":o\.source==="poster"\?"二维码海报":"-"/);
});

test('locales：shareLanding 键（zh-CN/zh-TW）', () => {
  const keys = ['text', 'home', 'learn', 'close'];
  for (const f of ['src/locales/zh-CN.json', 'src/locales/zh-TW.json']) {
    const raw = read(f);
    assert.match(raw, /"shareLanding"/);
    const landing = JSON.parse(raw).shareLanding || {};
    for (const key of keys) {
      assert.equal(typeof landing[key], 'string', `${f} shareLanding.${key} 应为字符串`);
      assert.ok(landing[key].length > 0, `${f} shareLanding.${key} 不得为空`);
    }
  }
});
