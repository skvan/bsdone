// 全仓静态守卫：禁止本地 function 与同一文件的 import 本地别名同名。
// 背景：GameDetailContent 的 function Zt 与 getCurrentInstance as Zt 同名，
// 函数声明提升遮蔽导入，生产构建后 setup 崩溃（PR #364）。
// 文本级扫描（不引入解析器）：注释/字符串中的 "function X(" 也会命中，如遇误报请调整注释。
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SRC = fileURLToPath(new URL('../src', import.meta.url));

function walk(dir, out = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) walk(p, out);
    else if (/\.(js|vue)$/.test(ent.name)) out.push(p);
  }
  return out;
}

function lineOf(text, idx) {
  let line = 1;
  for (let i = 0; i < idx; i++) if (text.charCodeAt(i) === 10) line++;
  return line;
}

function scriptBlocks(file, raw) {
  if (!file.endsWith('.vue')) return [{ code: raw, offset: 0 }];
  const blocks = [];
  const re = /<script[^>]*>([\s\S]*?)<\/script>/g;
  let m;
  while ((m = re.exec(raw))) {
    blocks.push({ code: m[1], offset: m.index + m[0].indexOf(m[1]) });
  }
  return blocks;
}

const IDENT = /^[A-Za-z_$][\w$]*$/;

function importAliases(code) {
  const aliases = new Map();
  const re = /import\b\s*([^;]+?)\s+from\s+['"]([^'"]+)['"]/g;
  let m;
  while ((m = re.exec(code))) {
    const clause = m[1].trim();
    const source = m[2];
    const add = (alias, imported) => {
      if (IDENT.test(alias)) aliases.set(alias, { source, imported });
    };
    const ns = clause.match(/\*\s+as\s+([A-Za-z_$][\w$]*)/);
    if (ns) add(ns[1], '*');
    const brace = clause.indexOf('{');
    const head = (brace >= 0 ? clause.slice(0, brace) : clause)
      .replace(/\*.*$/, '')
      .replace(/,/g, '')
      .trim();
    if (head) add(head, 'default');
    if (brace >= 0) {
      const inner = clause.slice(brace + 1, clause.lastIndexOf('}'));
      for (const part of inner.split(',')) {
        const s = part.trim();
        if (!s) continue;
        const asMatch = s.match(/^([A-Za-z_$][\w$]*)\s+as\s+([A-Za-z_$][\w$]*)$/);
        if (asMatch) add(asMatch[2], asMatch[1]);
        else if (IDENT.test(s)) add(s, s);
      }
    }
  }
  return aliases;
}

function functionDeclarations(code) {
  const out = [];
  const re = /function(?:\s*\*\s*|\s+)([A-Za-z_$][\w$]*)\s*\(/g;
  let m;
  while ((m = re.exec(code))) out.push({ name: m[1], idx: m.index });
  return out;
}

test('import 别名解析覆盖默认/命名/命名空间/组合/无空格写法', () => {
  const aliases = importAliases(
    "import A, { b as c, d } from 'x'; import * as E from 'y'; import { f } from 'z'; import{ g as h } from 'w';"
  );
  assert.deepEqual([...aliases.keys()].sort(), ['A', 'E', 'c', 'd', 'f', 'h'].sort());
  assert.deepEqual(aliases.get('A'), { source: 'x', imported: 'default' });
  assert.deepEqual(aliases.get('c'), { source: 'x', imported: 'b' });
  assert.deepEqual(aliases.get('E'), { source: 'y', imported: '*' });
  assert.deepEqual(
    functionDeclarations('function a(){} function* b(){} async function c(){} function*d(){}').map((x) => x.name),
    ['a', 'b', 'c', 'd']
  );
});

test('全仓不存在本地 function 与 import 别名同名（防止声明提升遮蔽）', () => {
  const files = walk(SRC);
  assert.ok(files.length > 100, `扫描文件数异常：${files.length}`);

  const findings = [];
  for (const file of files) {
    const raw = fs.readFileSync(file, 'utf8');
    const blocks = scriptBlocks(file, raw);
    const aliases = new Map();
    for (const { code } of blocks) {
      for (const [name, meta] of importAliases(code)) aliases.set(name, meta);
    }
    if (!aliases.size) continue;
    for (const { code, offset } of blocks) {
      for (const fn of functionDeclarations(code)) {
        const imported = aliases.get(fn.name);
        if (!imported) continue;
        const at = `${path.relative(SRC, file)}:${lineOf(raw, offset + fn.idx)}`;
        findings.push(`${at} function ${fn.name} 与 import ${imported.source}: ${imported.imported} 同名`);
      }
    }
  }

  assert.deepEqual(
    findings,
    [],
    `发现 ${findings.length} 处同名遮蔽（函数声明提升会遮蔽 import，仅显示前 20 条）：\n${findings.slice(0, 20).join('\n')}`
  );
});
