import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { ref } from 'vue';
import {
  buildPlayerShareUrl,
  buildPlayerShareMeta,
  useWechatShare
} from '../src/composables/useWechatShare.js';

function fakeRouter(base = '/bs-ball-next/') {
  return {
    resolve({ params, query }) {
      const q = query && query.from ? `?from=${query.from}` : '';
      return { href: `${base}${params.tenantCode}/players/${params.id}${q}` };
    }
  };
}

function wxHarness() {
  const calls = { configCount: 0, app: [], timeline: [] };
  const wx = {
    config() {
      calls.configCount += 1;
      queueMicrotask(() => wx.__ready && wx.__ready());
    },
    ready(cb) { wx.__ready = cb; },
    error(cb) { wx.__error = cb; },
    updateAppMessageShareData(data) { calls.app.push(data); },
    updateTimelineShareData(data) { calls.timeline.push(data); }
  };
  return { wx, calls };
}

function flush() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

beforeEach(() => {
  globalThis.location = { origin: 'https://www.bsdone.com' };
  delete globalThis.window;
});

test('buildPlayerShareUrl：含 base 与 from，playerId 字符串化', () => {
  const router = fakeRouter();
  assert.equal(
    buildPlayerShareUrl(router, 9, 'share', 'team1'),
    'https://www.bsdone.com/bs-ball-next/team1/players/9?from=share'
  );
  assert.equal(
    buildPlayerShareUrl(router, 9, 'poster', 'team1'),
    'https://www.bsdone.com/bs-ball-next/team1/players/9?from=poster'
  );
});

test('buildPlayerShareMeta：击球员摘要（打击率/HR/打点）', () => {
  const meta = buildPlayerShareMeta(
    { name: '张三', avatar: '/files/2026/01/a.png' },
    { name: '雄鹰队' },
    { name: '春季联赛' },
    { batting: { ab: 100, avg: '.350', hr: 12, rbi: 34 } }
  );
  assert.equal(meta.title, '张三 | 打击率 .350 · 12HR · 34打点');
  assert.equal(meta.desc, '雄鹰队 · 春季联赛 · BSD棒垒球数据平台');
  assert.equal(meta.imgUrl, 'https://www.bsdone.com/files/2026/01/a.png');
});

test('buildPlayerShareMeta：投手摘要（无打击数据时）+ 平台 logo 兜底', () => {
  const meta = buildPlayerShareMeta(
    { name: '李明', avatar: '' },
    { name: '闪电队' },
    null,
    { batting: { ab: 0 }, pitching: { gp: 8, ip: 40.0, era: '2.10', pitchSo: 30, w: 3 } }
  );
  assert.equal(meta.title, '李明 | 防御率 2.10 · 30K · 3胜');
  assert.equal(meta.desc, '闪电队 · BSD棒垒球数据平台');
  assert.equal(meta.imgUrl, 'https://www.bsdone.com/static/logo/logo2.png');
});

test('buildPlayerShareMeta：无统计仅姓名；缺 player 返回 null', () => {
  const meta = buildPlayerShareMeta({ name: '王五', avatar: '' }, null, null, null);
  assert.equal(meta.title, '王五');
  assert.equal(meta.desc, 'BSD棒垒球数据平台');
  assert.equal(buildPlayerShareMeta(null, null, null, null), null);
  assert.equal(buildPlayerShareMeta({ name: '' }, null, null, null), null);
});

test('buildPlayerShareMeta：resolveAsset/origin/platformName 选项与绝对头像', () => {
  const meta = buildPlayerShareMeta(
    { name: '赵六', avatar: '/files/a.png' },
    null,
    null,
    null,
    {
      origin: 'https://x.test',
      resolveAsset: (url) => `/bsball-server${url}`,
      platformName: '平台X'
    }
  );
  assert.deepEqual(meta, {
    title: '赵六',
    desc: '平台X',
    imgUrl: 'https://x.test/bsball-server/files/a.png'
  });

  const absolute = buildPlayerShareMeta(
    { name: '钱七', avatar: 'https://cdn.test/a.png' },
    null,
    null,
    null,
    { origin: 'https://x.test' }
  );
  assert.equal(absolute.imgUrl, 'https://cdn.test/a.png');
});

test('buildPlayerShareMeta：data/blob/协议相对头像原样返回', () => {
  const dataUri = 'data:image/png;base64,iVBORw0KGgo=';
  const withData = buildPlayerShareMeta({ name: '周九', avatar: dataUri }, null, null, null);
  assert.equal(withData.imgUrl, dataUri);
  assert.equal(
    buildPlayerShareMeta({ name: '吴十', avatar: '//cdn.test/a.png' }, null, null, null).imgUrl,
    '//cdn.test/a.png'
  );
});

test('buildPlayerShareMeta：空白头像回退平台 logo', () => {
  const meta = buildPlayerShareMeta({ name: '孙八', avatar: '   ' }, null, null, null);
  assert.equal(meta.imgUrl, 'https://www.bsdone.com/static/logo/logo2.png');
});

test('useWechatShare：链接变化触发初始化、同链接标题变化不再请求签名', async () => {
  const href = 'https://www.bsdone.com/bs-ball-next/t/players/9?from=share';
  globalThis.window = { location: { href } };
  const { wx, calls } = wxHarness();
  let fetchCount = 0;
  const metaRef = ref(null);
  const stop = useWechatShare(metaRef, {
    isWechat: () => true,
    loadScript: () => Promise.resolve(),
    fetchConfig: async () => { fetchCount += 1; return { appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx,
    warn: () => {}
  });
  const base = { link: href, desc: '', imgUrl: '' };
  metaRef.value = { ...base, title: '张三' };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);
  assert.deepEqual(calls.app.map((d) => d.title), ['张三']);
  metaRef.value = { ...base, title: '张三 | 打击率 .350' };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);
  assert.deepEqual(calls.app.map((d) => d.title), ['张三', '张三 | 打击率 .350']);
  assert.deepEqual(calls.timeline.map((d) => d.title), ['张三', '张三 | 打击率 .350']);
  stop();
});

test('useWechatShare：同链接仅 desc 变化也更新分享数据且不重复请求签名', async () => {
  const href = 'https://www.bsdone.com/bs-ball-next/t/players/915?from=share';
  globalThis.window = { location: { href } };
  const { wx, calls } = wxHarness();
  let fetchCount = 0;
  const metaRef = ref(null);
  const stop = useWechatShare(metaRef, {
    isWechat: () => true,
    loadScript: () => Promise.resolve(),
    fetchConfig: async () => { fetchCount += 1; return { appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx,
    warn: () => {}
  });
  metaRef.value = { title: '张三', desc: '雄鹰队', imgUrl: 'https://cdn.test/a.png', link: href };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);

  metaRef.value = { title: '张三', desc: '雄鹰队 · 春季联赛 · BSD棒垒球数据平台', imgUrl: 'https://cdn.test/a.png', link: href };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);
  assert.equal(calls.app.length, 2);
  assert.equal(calls.app[1].desc, '雄鹰队 · 春季联赛 · BSD棒垒球数据平台');
  assert.equal(calls.timeline[1].desc, '雄鹰队 · 春季联赛 · BSD棒垒球数据平台');
  stop();
});

test('useWechatShare：链接变化（页面跳转）重新初始化并应用新分享数据', async () => {
  const href1 = 'https://www.bsdone.com/bs-ball-next/t/players/910?from=share';
  const href2 = 'https://www.bsdone.com/bs-ball-next/t/players/911?from=poster';
  globalThis.window = { location: { href: href1 } };
  const { wx, calls } = wxHarness();
  let fetchCount = 0;
  const metaRef = ref(null);
  const stop = useWechatShare(metaRef, {
    isWechat: () => true,
    loadScript: () => Promise.resolve(),
    fetchConfig: async () => { fetchCount += 1; return { appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx,
    warn: () => {}
  });
  metaRef.value = { title: '甲', desc: 'D1', imgUrl: 'I1', link: href1 };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 1);
  assert.equal(calls.app.length, 1);
  assert.equal(calls.app[0].link, href1);

  globalThis.window.location.href = href2;
  metaRef.value = { title: '乙', desc: 'D2', imgUrl: 'I2', link: href2 };
  await flush();
  assert.equal(fetchCount, 2);
  assert.equal(calls.configCount, 2);
  assert.equal(calls.app.length, 2);
  assert.deepEqual(calls.app[1], { title: '乙', desc: 'D2', link: href2, imgUrl: 'I2' });
  assert.deepEqual(calls.timeline[1], { title: '乙', desc: 'D2', link: href2, imgUrl: 'I2' });
  stop();
});

test('useWechatShare：初始化期间标题更新，配置成功后补发最新标题', async () => {
  const href = 'https://www.bsdone.com/bs-ball-next/t/players/912?from=share';
  globalThis.window = { location: { href } };
  const { wx, calls } = wxHarness();
  let fetchCount = 0;
  let resolveConfig;
  const metaRef = ref(null);
  const stop = useWechatShare(metaRef, {
    isWechat: () => true,
    loadScript: () => Promise.resolve(),
    fetchConfig: () => {
      fetchCount += 1;
      return new Promise((resolve) => { resolveConfig = resolve; });
    },
    getWx: () => wx,
    warn: () => {}
  });
  metaRef.value = { title: '张三', desc: '', imgUrl: '', link: href };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.configCount, 0);
  assert.equal(calls.app.length, 0);

  // 签名请求未返回时数据到达（仅标题变化）：此时尚未配置，更新应暂存
  metaRef.value = { title: '张三 | 打击率 .350', desc: '', imgUrl: '', link: href };
  await flush();
  assert.equal(calls.app.length, 0);

  resolveConfig({ appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' });
  await flush();
  assert.equal(calls.configCount, 1);
  assert.deepEqual(calls.app.map((d) => d.title), ['张三', '张三 | 打击率 .350']);
  stop();
});

test('useWechatShare：stop 后不再响应元数据变化', async () => {
  const href = 'https://www.bsdone.com/bs-ball-next/t/players/913?from=share';
  globalThis.window = { location: { href } };
  const { wx, calls } = wxHarness();
  let fetchCount = 0;
  const metaRef = ref(null);
  const stop = useWechatShare(metaRef, {
    isWechat: () => true,
    loadScript: () => Promise.resolve(),
    fetchConfig: async () => { fetchCount += 1; return { appId: 'a', timestamp: 1, nonceStr: 'n', signature: 's' }; },
    getWx: () => wx,
    warn: () => {}
  });
  metaRef.value = { title: '甲', desc: '', imgUrl: '', link: href };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.app.length, 1);

  stop();
  metaRef.value = { title: '甲2', desc: '', imgUrl: '', link: href };
  await flush();
  assert.equal(calls.app.length, 1);
  globalThis.window.location.href = 'https://www.bsdone.com/bs-ball-next/t/players/914?from=share';
  metaRef.value = { title: '乙', desc: '', imgUrl: '', link: globalThis.window.location.href };
  await flush();
  assert.equal(fetchCount, 1);
  assert.equal(calls.app.length, 1);
});
