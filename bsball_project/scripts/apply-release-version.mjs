// apply-release-version.mjs — Issue #123：部署时把发布版本统一应用到旧版前端产物（webapps）
// 用法：
//   node bsball_project/scripts/apply-release-version.mjs --version 1.1.2 [--dry] [--dir <webapps 目录>]
// 设计：
//   - 精确语义锚点替换（防误伤：如 leaflet 的 WMS 协议版本 "1.1.1" 不在替换范围）
//   - 每条规则必须命中（未命中即失败退出 1）——防止产物结构变化导致"版本漏改无人发现"（#117 教训）
//   - 值安全：JS/JSON 上下文做字符串转义、HTML 上下文做实体转义（任意输入不破坏产物语法）
//   - .gz 同步：被修改文件若存在同名 .gz（nginx gzip_static 会优先命中），立即重生成
//   - 幂等：同值重复运行无副作用；未提供 --version（留空沿用仓库基线）时直接跳过
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const args = process.argv.slice(2);
const getArg = (name) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : null;
};
const DRY = args.includes('--dry');
const version = (getArg('--version') || '').trim();
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '../..');
const webappsDir = path.resolve(getArg('--dir') || path.join(repoRoot, 'bsball_project/webapps'));

console.log('[apply-release-version] 目标目录: ' + webappsDir);
if (!version) {
  console.log('[apply-release-version] 未提供 --version（留空沿用仓库基线），跳过');
  process.exit(0);
}
if (version.startsWith('v')) {
  console.warn('[apply-release-version] 警告：版本以 v 开头，建议传入裸版本号（如 1.1.2）；按输入原样应用');
}
// 拒绝控制字符/行终止符（防写坏 JS/JSON 字面量；异常输入应响亮失败而非静默转换）
if (/[\x00-\x1F\x7F\u2028\u2029]/.test(version)) {
  console.error('[apply-release-version] 版本含控制字符/行终止符，拒绝执行');
  process.exit(1);
}

const escJs = (v) => v.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\r/g, '\\r').replace(/\n/g, '\\n').replace(/\u2028/g, '\\u2028').replace(/\u2029/g, '\\u2029');
const escHtml = (v) => v.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// 规则：捕获组固定为 [前缀, 值, 可选后缀]；scope=目标文件类型；perFile=要求"每个目标文件各命中一次"（html）
const RULES = [
  { name: 'appVersion（设备/环境上报）', scope: 'js', re: /(appVersion:")([^"]*)(")/g },
  { name: 'console 前端构建版本', scope: 'js', re: /(const e=")([^"]*)(";console\.log\("%c)/g },
  { name: '版本检查常量', scope: 'js', re: /(t!==")([^"]*)("(?:\|\|a!=="))/g },
  { name: '更新提示当前版本', scope: 'js', re: /(const L=")([^"]*)("\.trim\(\))/g },
  { name: '管理台版本常量', scope: 'js', re: /(const w=")([^"]*)(",D=")/g },
  { name: '门户反馈 clientVersion', scope: 'js', re: /(const e=U\.value,a=")([^"]*)(",r=i\.contactValue)/g },
  { name: 'ServerMonitor 兜底版本', scope: 'js', re: /(version:")([^"]*)(",buildTime:")/g },
  { name: 'version.json version', scope: 'json', re: /("version"\s*:\s*")([^"]*)(")/g },
  { name: 'APP_VERSION 注释', scope: 'html', perFile: true, re: /(APP_VERSION: v)([0-9][^\s<]*)/g },
  { name: '页脚版本', scope: 'html', perFile: true, re: /(\u00b7 v)([0-9][^\s<]*)/g }
];

// 目标文件集：bs-ball/version.json + bs-ball/assets/*.js + index.html + portal.html（不触碰 bs-ball-next/）
const assetsDir = path.join(webappsDir, 'bs-ball/assets');
const indexHtml = path.join(webappsDir, 'index.html');
const portalHtml = path.join(webappsDir, 'portal.html');
const versionJson = path.join(webappsDir, 'bs-ball/version.json');
const missingStruct = [assetsDir, indexHtml, portalHtml, versionJson].filter((p) => !fs.existsSync(p));
if (missingStruct.length > 0) {
  console.error('[apply-release-version] 结构缺失（拒绝继续）: ' + missingStruct.join(', '));
  process.exit(1);
}
const targets = [
  { file: versionJson, rules: RULES.filter((r) => r.scope === 'json'), esc: escJs },
  ...fs.readdirSync(assetsDir).filter((n) => n.endsWith('.js')).map((n) => ({ file: path.join(assetsDir, n), rules: RULES.filter((r) => r.scope === 'js'), esc: escJs })),
  { file: indexHtml, rules: RULES.filter((r) => r.scope === 'html'), esc: escHtml },
  { file: portalHtml, rules: RULES.filter((r) => r.scope === 'html'), esc: escHtml }
];

const hits = new Map(RULES.map((r) => [r.name, 0]));
const perFileHits = new Map();
const changedFiles = [];
let gzSynced = 0;

for (const t of targets) {
  const original = fs.readFileSync(t.file, 'utf8');
  let text = original;
  t.rules.forEach((rule) => {
    text = text.replace(rule.re, (m, a, b, c) => {
      hits.set(rule.name, hits.get(rule.name) + 1);
      perFileHits.set(rule.name + '|' + t.file, (perFileHits.get(rule.name + '|' + t.file) || 0) + 1);
      return a + t.esc(version) + (c === undefined ? '' : c);
    });
  });
  if (text !== original) {
    changedFiles.push(path.relative(webappsDir, t.file));
    if (!DRY) {
      fs.writeFileSync(t.file, text, 'utf8');
      const gzPath = t.file + '.gz';
      if (fs.existsSync(gzPath)) {
        fs.writeFileSync(gzPath, zlib.gzipSync(fs.readFileSync(t.file), { level: 9 }));
        gzSynced += 1;
      }
    }
  }
}

console.log('[apply-release-version] 版本值: ' + version + (DRY ? '（DRY-RUN，不写盘）' : ''));
console.log('[apply-release-version] 规则命中统计:');
for (const r of RULES) {
  const n = hits.get(r.name);
  console.log('  ' + (n > 0 ? 'HIT ' : 'MISS') + '  ' + r.name + ': ' + n + ' 处');
}

// 断言：全局规则 ≥1；perFile 规则要求每个目标文件各 ≥1
const failures = [];
for (const r of RULES) {
  if (hits.get(r.name) < 1) {
    failures.push('规则未命中: ' + r.name);
  }
  if (r.perFile) {
    for (const t of targets.filter((x) => x.rules.includes(r))) {
      const key = r.name + '|' + t.file;
      if ((perFileHits.get(key) || 0) < 1) {
        failures.push('规则在文件内未命中: ' + r.name + ' @ ' + path.relative(webappsDir, t.file));
      }
    }
  }
}
if (failures.length > 0) {
  console.error('[apply-release-version] FAIL（版本替换不完整，拒绝继续）:');
  for (const f of failures) console.error('  - ' + f);
  console.error('  提示：产物结构可能已变化，请检查 RULES 锚点并更新脚本。');
  process.exit(1);
}

console.log('[apply-release-version] 变更文件 ' + changedFiles.length + ' 个' + (DRY ? '' : '（.gz 同步 ' + gzSynced + ' 个）') + ':');
for (const f of changedFiles) console.log('  - ' + f);
console.log('[apply-release-version] OK');
