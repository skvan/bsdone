// package-webapps.mjs — webapps 产物组装与打包（B0 工程化）
//
// 组装规则（与线上部署约定一致：tar 顶层包含 webapps/ 目录）：
//   1) 以仓库现 bsball_project/webapps/ 为底完整复制（保留 portal.html / index.html / static/，
//      以及 bs-ball 下静态资源：geo、icons、team-logos、carousel、ticket、advertising、*.svg 等）；
//   2) 非 --legacy 模式：删除副本中的 bs-ball/assets/ 与 bs-ball/index.html（含 .gz 孪生），
//      用 dist/（vite build 产物）替换，并重写 bs-ball/version.json（buildTime = 当前时间）；
//   3) --legacy 模式：不做任何替换（内容与仓库现有编译产物等价，用于验证【打包→部署→回滚】
//      链路，页面零变化）；
//   4) 输出 webapps-dev.tar.gz 到 dist/ 下（--legacy 默认输出 webapps-dev-legacy.tar.gz）。
//
// 用法：
//   npm run package          # vite build + 组装打包（正式产物）
//   npm run package:legacy   # 等价重打包（部署链路验证用）
//   node scripts/package-webapps.mjs --out=custom.tar.gz
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FRONTEND_DIR = path.resolve(__dirname, '..');
const REPO_DIR = path.resolve(FRONTEND_DIR, '..');
const WEBAPPS_SRC = path.join(REPO_DIR, 'bsball_project', 'webapps');
const DIST_DIR = path.join(FRONTEND_DIR, 'dist');
const STAGE_ROOT = path.join(DIST_DIR, 'package');
const STAGE_WEBAPPS = path.join(STAGE_ROOT, 'webapps');

const args = process.argv.slice(2);
const legacy = args.includes('--legacy');
const outArg = args.find((a) => a.startsWith('--out='));
const outName = outArg ? outArg.slice('--out='.length) : legacy ? 'webapps-dev-legacy.tar.gz' : 'webapps-dev.tar.gz';
const OUT_TAR = path.join(DIST_DIR, outName);

const log = (...m) => console.log('[package-webapps]', ...m);
const fail = (msg) => {
  console.error('[package-webapps] 错误：' + msg);
  process.exit(1);
};

// ---------- 前置检查 ----------
if (!fs.existsSync(WEBAPPS_SRC)) fail('未找到 ' + WEBAPPS_SRC + '（仓库目录锚点异常）');
if (!legacy && !fs.existsSync(path.join(DIST_DIR, 'index.html'))) {
  fail('未找到 dist/index.html —— 请先执行 vite build（推荐直接使用 npm run package）');
}

// ---------- 1) 复制底包 ----------
fs.rmSync(STAGE_ROOT, { recursive: true, force: true });
fs.mkdirSync(STAGE_ROOT, { recursive: true });
fs.cpSync(WEBAPPS_SRC, STAGE_WEBAPPS, { recursive: true });
log('底包复制完成 →', path.relative(FRONTEND_DIR, STAGE_WEBAPPS));

const bsBall = path.join(STAGE_WEBAPPS, 'bs-ball');

// ---------- 2) 替换 bs-ball 构建产物 / 或等价保留 ----------
if (!legacy) {
  fs.rmSync(path.join(bsBall, 'assets'), { recursive: true, force: true });
  for (const f of ['index.html', 'index.html.gz']) {
    const p = path.join(bsBall, f);
    if (fs.existsSync(p)) fs.rmSync(p);
  }
  fs.copyFileSync(path.join(DIST_DIR, 'index.html'), path.join(bsBall, 'index.html'));
  fs.cpSync(path.join(DIST_DIR, 'assets'), path.join(bsBall, 'assets'), { recursive: true });

  // version.json 重写（字段与现格式对齐）
  const pkg = JSON.parse(fs.readFileSync(path.join(FRONTEND_DIR, 'package.json'), 'utf8'));
  const d = new Date();
  const pad = (n, l = 2) => String(n).padStart(l, '0');
  const off = -d.getTimezoneOffset();
  const sign = off >= 0 ? '+' : '-';
  const buildTime =
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}` +
    `${sign}${pad(Math.floor(Math.abs(off) / 60))}:${pad(Math.abs(off) % 60)}`;
  const versionJson = {
    version: pkg.version || '1.0.0',
    buildTime,
    baseUrl: '/bs-ball/',
    promptUpdate: false,
    updateContent: '修复已知问题。'
  };
  fs.writeFileSync(path.join(bsBall, 'version.json'), JSON.stringify(versionJson, null, 2) + '\n', 'utf8');
  log('已替换 bs-ball/index.html + assets/（来自 dist/）；version.json buildTime =', buildTime);
} else {
  log('legacy 模式：不替换（等值重打包，页面零变化）');
}

// ---------- 3) 打包（系统 tar，顶层含 webapps/） ----------
const tar = spawnSync('tar', ['-czf', OUT_TAR, '-C', STAGE_ROOT, 'webapps'], { stdio: 'inherit' });
if (tar.error) fail('tar 执行失败（需系统 tar 命令）：' + tar.error.message);
if (tar.status !== 0) fail('tar 退出码 ' + tar.status);

// ---------- 4) 校验 ----------
const list = spawnSync('tar', ['-tzf', OUT_TAR], { encoding: 'utf8' });
if (list.status !== 0) fail('tar -t 校验失败');
const files = list.stdout.split('\n').filter(Boolean);
const hasIndex = files.includes('webapps/bs-ball/index.html');
const hasVersion = files.includes('webapps/bs-ball/version.json');
const hasPortal = files.includes('webapps/portal.html');
const assetCount = files.filter((f) => f.startsWith('webapps/bs-ball/assets/')).length;
if (!hasIndex || !hasVersion || !hasPortal) fail('打包校验未通过（缺 index.html / version.json / portal.html）');
log('打包校验通过：条目总数 =', files.length, '；assets 条目 =', assetCount);
log('输出：', OUT_TAR, '(' + (fs.statSync(OUT_TAR).size / 1024 / 1024).toFixed(1) + ' MB)');
