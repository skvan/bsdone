// 阵容板首份快照被服务器拒收时的中止顺序（#713 可见性）
// 运行：在 bsball-frontend 目录执行 node --test tests/liveGameLineupBoardGuard409.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

test('谁是活件：路由只挂 LiveGameLineupBoard.vue，不挂反编译参考件', () => {
  // 这一条是被自己绊出来的：本轮先改了 src/views/admin/LiveGameLineup.js（以为那是线上跑的），
  // 实际 router 只 import .vue；那份 .js 是反编译参考件，tests/lineupBoardStatic.test.mjs 还专门
  // 要求它保持「勿手改」开头。改它没有任何运行时效果，却会让人以为缺陷已修，所以钉成断言。
  const router = read('../src/router/index.js');
  assert.ok(!/from '[^']*admin\/LiveGameLineup(\.js)?'/.test(router),
    'router 里出现了 LiveGameLineup.js 的 import：参考件被接上线了，本文件与相邻测试的口径要重定');
  assert.match(router, /LiveGameLineupBoard\.vue/, 'router 没挂阵容板，本测试的前提已不成立');
});

test('整个 src 都没把参考件接上线：不只 router，任何组件 import 它都算', () => {
  // 上一条只看 router，但「改死件白改」这个坑不挑入口：哪天某个 .vue import 了它，同一件事
  // 会照样复发。GameService 的 javadoc 写的就是「src 下没有任何地方 import 它」，这句话
  // 得有个测试撑着，不然它又会变成一条没人验的宣称。
  // 正则不要求路径里带 admin/：同一个目录里写 './LiveGameLineup.js' 是最可能的接法，
  // 第一版只认 admin/ 前缀，拿它做变异（往 .vue 里插一份 ./ 的 import）实测杀不掉，已修正。
  const re = /\bfrom\s*['"][^'"]*\/LiveGameLineup(?:\.js)?['"]/;
  const hits = [];
  let scanned = 0;
  const root = fileURLToPath(new URL('../src/', import.meta.url));
  const walk = (dir) => fs.readdirSync(dir, { withFileTypes: true }).forEach((ent) => {
    const abs = path.join(dir, ent.name);
    if (ent.isDirectory()) return walk(abs);
    if (!/\.(js|mjs|vue)$/.test(ent.name)) return;
    scanned += 1;
    if (re.test(fs.readFileSync(abs, 'utf8'))) hits.push(path.relative(root, abs));
  });
  walk(root);
  // 扫不到东西就是假绿（目录错、扩展名错都会让它空跑而全绿）
  assert.ok(scanned > 50, `src 下只扫到 ${scanned} 个文件，本测试在空跑`);
  assert.deepEqual(hits, [], '参考件 LiveGameLineup.js 被这些文件 import 了：' + hits.join(', '));
});

test('快照业务失败（含守门 409）：抬真实原因，并且在写引导与跳转之前就中止', () => {
  // 409 会不会落到这个分支由 tests/apiError.test.mjs 钉（isBusinessError 认 4xx），
  // 这里只钉顺序：中止必须先于 writeEntryBootstrap / 「已保存」/ 进入录入页三件事。
  const src = read('../src/views/admin/LiveGameLineupBoard.vue');
  const start = src.indexOf('async function startEntry()');
  assert.ok(start >= 0, '找不到 startEntry');
  const body = src.slice(start, src.indexOf('\n}\n', start));
  const biz = body.indexOf('if (isBusinessError(error))');
  assert.ok(biz >= 0, '阵容板的快照保存没有业务失败分支');
  assert.match(body.slice(biz, biz + 400), /ElMessage\.error\(error\.message/,
    '业务失败要抬服务器给的原因；换成通用话术就又回到「不知道被拒了」');
  const ret = body.indexOf('return;', biz);
  assert.ok(ret > biz, '业务失败分支必须自行中止');
  for (const marker of ['writeEntryBootstrap(', 'liveGame.lineup.savedEntering', "name: 'AdminGameLiveResume'"]) {
    const at = body.indexOf(marker);
    assert.ok(at >= 0, `缺少 ${marker}：阵容板流程形状变了，这条守门要重新看`);
    assert.ok(ret < at, `${marker} 不能发生在中止之前——被拒收也照样进录入页就是假保存`);
  }
});
