// 跨语言/跨模块的措辞一致性：e2e mock 里那份「后端词典转写」必须与后端真文件逐字相同（#713 文案本地化）
// 运行：node --test tests/snapshotGuardMessageParity.test.mjs
//
// 为什么要这条：e2e 的假服务器按请求语言挑文案，画面断言用的又是英文句子里的片段。
// 后端改了 messages*.properties 而 mock 没跟上时，画面上测的是 mock 那句话，线上说的是另一句，
// 两边都绿、谁也不知道。这条把「mock 是后端的逐字转写」这件事从注释变成断言。
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const FRONTEND_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const BACKEND_RES = path.join(FRONTEND_ROOT, '..', 'bsball-backend', 'src', 'main', 'resources');
const SPEC = path.join(FRONTEND_ROOT, 'e2e', 'live-game-snapshot-conflict-local-mocked.spec.js');

const BUNDLE_FILES = { 'zh-CN': 'messages.properties', 'zh-TW': 'messages_zh_TW.properties', en: 'messages_en.properties' };

function readProperties(file) {
  const full = path.join(BACKEND_RES, file);
  assert.ok(fs.existsSync(full), `后端词典文件不在：${full}（monorepo 结构变了就要同步改这条测试的路径）`);
  const out = {};
  for (const rawLine of fs.readFileSync(full, 'utf8').replace(/\r\n/g, '\n').split('\n')) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const i = line.indexOf('=');
    assert.ok(i > 0, `词典行没有等号：${line}`);
    out[line.slice(0, i).trim()] = line.slice(i + 1);
  }
  return out;
}

function readSpecTable() {
  const src = fs.readFileSync(SPEC, 'utf8');
  const begin = src.indexOf('// SNAPSHOT_GUARD_MESSAGES:BEGIN');
  const end = src.indexOf('// SNAPSHOT_GUARD_MESSAGES:END');
  assert.ok(begin >= 0 && end > begin, 'e2e spec 里的词典转写块标记不见了（改了格式要同步改这条测试）');
  const chunk = src.slice(begin, end);
  const start = chunk.indexOf('{');
  const stop = chunk.lastIndexOf('}');
  assert.ok(start >= 0 && stop > start, '词典转写块里找不到对象字面量');
  return JSON.parse(chunk.slice(start, stop + 1));
}

const table = readSpecTable();

test('三语词典的键集合与后端一致', () => {
  for (const [locale, file] of Object.entries(BUNDLE_FILES)) {
    const backend = readProperties(file);
    assert.ok(table[locale], `mock 转写缺了 ${locale} 整块`);
    assert.deepEqual(Object.keys(table[locale]).sort(), Object.keys(backend).sort(),
      `${locale} 的键集合与后端不一致`);
  }
});

test('mock 转写与后端词典逐字相同（含 {0,number,#} 占位符写法）', () => {
  for (const [locale, file] of Object.entries(BUNDLE_FILES)) {
    const backend = readProperties(file);
    for (const [key, text] of Object.entries(backend)) {
      assert.equal(table[locale][key], text,
        `${locale} / ${key} 的措辞与后端不一致：mock 说「${String(table[locale][key]).slice(0, 40)}」` +
        `而后端说「${text.slice(0, 40)}」`);
    }
  }
});

test('每条文案都带它声称的那个按钮：前端词典里真有这个词', () => {
  // 后端文案承诺「用『从云端恢复录入现场』」这类可执行动作，动作名取自前端词典
  // （liveGame.restore.confirmTitle）。前端哪天改了按钮名，后端与 mock 的话就指向一个不存在的按钮。
  const restoreTitlePerLocale = {};
  for (const locale of Object.keys(BUNDLE_FILES)) {
    const dict = JSON.parse(fs.readFileSync(path.join(FRONTEND_ROOT, 'src', 'locales', `${locale}.json`), 'utf8'));
    restoreTitlePerLocale[locale] = dict.liveGame.restore.confirmTitle;
    assert.ok(restoreTitlePerLocale[locale], `${locale} 缺 liveGame.restore.confirmTitle`);
  }
  // 简体中文与繁体：文案里用「」引用按钮名，引用内容必须与词典一致
  for (const locale of ['zh-CN', 'zh-TW']) {
    for (const [key, text] of Object.entries(table[locale])) {
      const quoted = [...String(text).matchAll(/「([^」]+)」/g)].map((m) => m[1]);
      for (const q of quoted) {
        assert.equal(q, restoreTitlePerLocale[locale],
          `${locale} / ${key} 引用的按钮名「${q}」与前端词典「${restoreTitlePerLocale[locale]}」不一致`);
      }
    }
  }
  // 英文：文案用引号引用同一个按钮名
  for (const [key, text] of Object.entries(table.en)) {
    const quoted = [...String(text).matchAll(/"([^"]+)"/g)].map((m) => m[1]);
    for (const q of quoted) {
      assert.equal(q, restoreTitlePerLocale.en,
        `en / ${key} 引用的按钮名 "${q}" 与前端词典 "${restoreTitlePerLocale.en}" 不一致`);
    }
  }
});

test('mock 的语言判定与后端 UiLocaleResolver 同规则', () => {
  const src = fs.readFileSync(SPEC, 'utf8');
  const fn = src.match(/function guardLocale\(headers\)[\s\S]*?\n}\n/);
  assert.ok(fn, 'e2e spec 里找不到 guardLocale（改名了就要同步看这条）');
  const body = fn[0];
  // 后端把 ja/ko 归英文（前端 useLiveGameText 非 zh 取 en 词典），这条不能默默变回中文
  assert.match(body, /startsWith\('ko'\)[\s\S]*'en'|ko[\s\S]{0,80}ja[\s\S]*\breturn 'en'/,
    'guardLocale 不再把 ja/ko 归英文：与后端 UiLocaleResolver 脱钩');
  assert.match(body, /hant/, 'guardLocale 不再认 zh-Hant 为繁体');
});
