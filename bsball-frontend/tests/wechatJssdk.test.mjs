import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { initWechatShare, updateShareData, isWechatBrowser } from '../src/utils/wechatJssdk.js';

const META = {
  title: '张三 | 打击率 .350',
  desc: '雄鹰队 · 春季联赛 · BSD棒垒球数据平台',
  link: 'https://www.bsdone.com/bs-ball-next/team1/players/9?from=share',
  imgUrl: 'https://www.bsdone.com/files/a.png'
};

function setPage(href) {
  globalThis.window = { location: { href } };
}

function fakeWx(opts = {}) {
  const calls = { config: null, configCount: 0, app: [], timeline: [] };
  const wx = {
    config(configOpts) {
      calls.config = configOpts;
      calls.configCount += 1;
      queueMicrotask(() => {
        if (opts.failConfig) {
          if (wx.__errorCb) wx.__errorCb(new Error('invalid signature'));
        } else if (wx.__readyCb) {
          wx.__readyCb();
        }
      });
    },
    ready(cb) { wx.__readyCb = cb; },
    error(cb) { wx.__errorCb = cb; },
    updateAppMessageShareData(data) { calls.app.push(data); },
    updateTimelineShareData(data) { calls.timeline.push(data); }
  };
  return { wx, calls };
}

function deps(overrides = {}) {
  return {
    isWechat: () => true,
    loadScript: () => Promise.resolve(),
    fetchConfig: async () => ({}),
    getWx: () => undefined,
    warn: () => {},
    ...overrides
  };
}

beforeEach(() => {
  delete globalThis.window;
  delete globalThis.navigator;
});

test('isWechatBrowser：UA 判定（显式参数与 navigator）', () => {
  assert.equal(isWechatBrowser('Mozilla/5.0 MicroMessenger/8.0.0'), true);
  assert.equal(isWechatBrowser('Mozilla/5.0 Chrome/120'), false);
  globalThis.navigator = { userAgent: 'MicroMessenger/8.0' };
  assert.equal(isWechatBrowser(), true);
});

test('非微信环境：不加载脚本、不请求签名', async () => {
  let loaded = 0;
  let fetched = 0;
  await initWechatShare(META, deps({
    isWechat: () => false,
    loadScript: () => { loaded += 1; return Promise.resolve(); },
    fetchConfig: async () => { fetched += 1; return {}; }
  }));
  assert.equal(loaded, 0);
  assert.equal(fetched, 0);
});

test('meta 缺 title/link：提前返回', async () => {
  let loaded = 0;
  await initWechatShare({ title: '' }, deps({ loadScript: () => { loaded += 1; return Promise.resolve(); } }));
  assert.equal(loaded, 0);
});

test('脚本加载失败：静默（warn 一次），不请求签名', async () => {
  const warned = [];
  let fetched = 0;
  await initWechatShare(META, deps({
    loadScript: () => Promise.reject(new Error('load failed')),
    fetchConfig: async () => { fetched += 1; return {}; },
    warn: (e) => warned.push(e)
  }));
  assert.equal(warned.length, 1);
  assert.equal(fetched, 0);
});

test('签名接口失败：静默，不调 wx.config', async () => {
  setPage('https://www.bsdone.com/bs-ball-next/team1/players/900?from=share');
  const warned = [];
  const { wx, calls } = fakeWx();
  await initWechatShare(META, deps({
    fetchConfig: () => Promise.reject(new Error('api down')),
    getWx: () => wx,
    warn: (e) => warned.push(e)
  }));
  assert.equal(warned.length, 1);
  assert.equal(calls.configCount, 0);
  assert.equal(calls.app.length, 0);
});

test('wx.config 失败：静默，不注册分享数据、不记为已配置', async () => {
  setPage('https://www.bsdone.com/bs-ball-next/team1/players/901?from=share');
  const warned = [];
  const { wx, calls } = fakeWx({ failConfig: true });
  await initWechatShare(META, deps({
    fetchConfig: async () => ({ appId: 'wx-app', timestamp: 1700000000, nonceStr: 'n', signature: 's' }),
    getWx: () => wx,
    warn: (e) => warned.push(e)
  }));
  assert.equal(warned.length, 1);
  assert.equal(calls.configCount, 1);
  assert.equal(calls.app.length, 0);
  // 失败未写入活动 URL：updateShareData 必须 no-op
  updateShareData(META, { getWx: () => wx, warn: () => {} });
  assert.equal(calls.app.length, 0);
});

test('happy path：config 参数正确、分享数据注册、url 去掉 # 片段', async () => {
  setPage('https://www.bsdone.com/bs-ball-next/team1/players/902?from=share#section');
  const { wx, calls } = fakeWx();
  let fetchedUrl = '';
  await initWechatShare(META, deps({
    fetchConfig: async (url) => { fetchedUrl = url; return { appId: 'wx-app', timestamp: 1700000000, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx
  }));
  assert.equal(fetchedUrl, 'https://www.bsdone.com/bs-ball-next/team1/players/902?from=share');
  assert.equal(calls.config.appId, 'wx-app');
  assert.deepEqual(calls.config.jsApiList, ['updateAppMessageShareData', 'updateTimelineShareData']);
  assert.deepEqual(calls.app[0], { title: META.title, desc: META.desc, link: META.link, imgUrl: META.imgUrl });
  assert.deepEqual(calls.timeline[0], { title: META.title, desc: META.desc, link: META.link, imgUrl: META.imgUrl });
});

test('updateShareData：已配置页面更新成功（不再请求签名）', async () => {
  setPage('https://www.bsdone.com/bs-ball-next/team1/players/903?from=share');
  const { wx, calls } = fakeWx();
  let fetchCount = 0;
  await initWechatShare(META, deps({
    fetchConfig: async () => { fetchCount += 1; return { appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx
  }));
  assert.equal(fetchCount, 1);
  assert.equal(calls.app.length, 1);
  updateShareData({ ...META, title: '张三 | 打击率 .350 · 12HR' }, { getWx: () => wx, warn: () => {} });
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);
  assert.equal(calls.app.length, 2);
  assert.equal(calls.app[1].title, '张三 | 打击率 .350 · 12HR');
});

test('updateShareData：未配置页面 no-op', () => {
  setPage('https://www.bsdone.com/bs-ball-next/team1/players/904?from=share');
  const { wx, calls } = fakeWx();
  updateShareData(META, { getWx: () => wx, warn: () => {} });
  assert.equal(calls.app.length, 0);
});

test('同 URL 二次 init：不重复签名、不重复 wx.config', async () => {
  setPage('https://www.bsdone.com/bs-ball-next/team1/players/905?from=share');
  const { wx, calls } = fakeWx();
  let fetchCount = 0;
  const initDeps = deps({
    fetchConfig: async () => { fetchCount += 1; return { appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx
  });
  await initWechatShare(META, initDeps);
  await initWechatShare({ ...META, title: '张三 | 打击率 .350 · 2H' }, initDeps);
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);
  assert.equal(calls.app.length, 2);
  assert.deepEqual(calls.timeline[1], {
    title: '张三 | 打击率 .350 · 2H', desc: META.desc, link: META.link, imgUrl: META.imgUrl
  });
});
