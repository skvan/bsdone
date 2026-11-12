// package-webapps.mjs — webapps 产物组装与打包（单轨 /bs-ball/）
//
// 组装规则（与线上部署约定一致：tar 顶层包含 webapps/ 目录）：
//   1) 以仓库 bsball_project/webapps/ 为底复制，但显式排除旧前端产物：
//      bs-ball/assets/、bs-ball/index.html(.gz)、bs-ball/version.json(.gz) 以及 bs-ball-next/；
//      保留 bs-ball 下新版仍使用的静态资源：geo、icons、team-logos、carousel、ticket、
//      advertising、logo、默认头像/队徽等；
//   2) 用 dist/（vite build 产物）写入 bs-ball/index.html + assets/，并重写 bs-ball/version.json；
//   3) 并入仓库 premier12/（十二强赛微站）为 webapps/premier12/（生成 index.html 目录入口）；
//   4) 输出 webapps-dev.tar.gz 到 dist/ 下。
//
// 用法：
//   npm run package
//   node scripts/package-webapps.mjs --out=custom.tar.gz
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FRONTEND_DIR = path.resolve(__dirname, '..');
const REPO_DIR = path.resolve(FRONTEND_DIR, '..');
const WEBAPPS_SRC = path.join(REPO_DIR, 'bsball_project', 'webapps');
const PREMIER12_SRC = path.join(REPO_DIR, 'premier12');
const DIST_DIR = path.join(FRONTEND_DIR, 'dist');
const STAGE_ROOT = path.join(DIST_DIR, 'package');
const STAGE_WEBAPPS = path.join(STAGE_ROOT, 'webapps');

const args = process.argv.slice(2);
const outArg = args.find((a) => a.startsWith('--out='));
const outName = outArg ? outArg.slice('--out='.length) : 'webapps-dev.tar.gz';
const OUT_TAR = path.join(DIST_DIR, outName);
const TARGET_DIR = 'bs-ball';
const TARGET_BASE = '/bs-ball/';
// 发布版本/构建时间覆盖（Issue #123/#113）：--version 优先于 package.json；--build-time 与 vite 构建注入保持一致
const verArg = args.find((a) => a.startsWith('--version='));
const versionOverride = verArg ? verArg.slice('--version='.length).trim() : '';
const btArg = args.find((a) => a.startsWith('--build-time='));
const buildTimeOverride = btArg ? btArg.slice('--build-time='.length).trim() : '';

const log = (...m) => console.log('[package-webapps]', ...m);
const fail = (msg) => {
  console.error('[package-webapps] 错误：' + msg);
  process.exit(1);
};

// ---------- 前置检查 ----------
if (!fs.existsSync(WEBAPPS_SRC)) fail('未找到 ' + WEBAPPS_SRC + '（仓库目录锚点异常）');
if (!fs.existsSync(path.join(PREMIER12_SRC, 'premier12.html'))) {
  fail('未找到 ' + PREMIER12_SRC + '/premier12.html（十二强赛页面缺失）');
}
if (!fs.existsSync(path.join(DIST_DIR, 'index.html'))) {
  fail('未找到 dist/index.html —— 请先执行 vite build（推荐直接使用 npm run package）');
}

// 旧前端产物与双轨目录一律不进包：即使仓库里仍有历史文件，也不能被底包带入
const LEGACY_PREFIXES = [
  'bs-ball-next',
  'bs-ball/assets',
  'bs-ball/index.html',
  'bs-ball/index.html.gz',
  'bs-ball/version.json',
  'bs-ball/version.json.gz'
];
const isLegacyPath = (src) => {
  const rel = path.relative(WEBAPPS_SRC, src).split(path.sep).join('/');
  return LEGACY_PREFIXES.some((p) => rel === p || rel.startsWith(p + '/'));
};

// ---------- 1) 复制底包（排除旧前端产物） ----------
fs.rmSync(STAGE_ROOT, { recursive: true, force: true });
fs.mkdirSync(STAGE_ROOT, { recursive: true });
fs.cpSync(WEBAPPS_SRC, STAGE_WEBAPPS, { recursive: true, filter: (src) => !isLegacyPath(src) });
log('底包复制完成（已排除旧前端产物）→', path.relative(FRONTEND_DIR, STAGE_WEBAPPS));

const bsBall = path.join(STAGE_WEBAPPS, TARGET_DIR);

// ---------- 2) 写入单轨构建产物（/bs-ball/） ----------
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
const buildTime = buildTimeOverride || `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` + `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}` + `${sign}${pad(Math.floor(Math.abs(off) / 60))}:${pad(Math.abs(off) % 60)}`;
const versionJson = {
  version: versionOverride || pkg.version || '1.0.0',
  buildTime,
  baseUrl: TARGET_BASE,
  promptUpdate: false,
  updateContent: '修复已知问题。'
};
fs.writeFileSync(path.join(bsBall, 'version.json'), JSON.stringify(versionJson, null, 2) + '\n', 'utf8');
// 旧预压缩副本不得残留：nginx gzip_static 会优先命中 .gz
fs.rmSync(path.join(bsBall, 'version.json.gz'), { force: true });
log('已写入 ' + TARGET_DIR + '/index.html + assets/（来自 dist/）；version.json buildTime =', buildTime);

// ---------- 3) Premier12 微站并入（Issue #153；/premier12/ 目录入口） ----------
const premier12Dir = path.join(STAGE_WEBAPPS, 'premier12');
fs.cpSync(PREMIER12_SRC, premier12Dir, {
  recursive: true,
  // server.js 为本地预览用静态服务器，非部署资产，不进公开 webroot
  filter: (src) => path.basename(src) !== 'server.js'
});
// 目录索引：/premier12/ 直达宣传页；保留 premier12.html 供站内「返回首页」相对链接
fs.copyFileSync(path.join(premier12Dir, 'premier12.html'), path.join(premier12Dir, 'index.html'));
log('Premier12 微站并入 →', path.relative(FRONTEND_DIR, premier12Dir));

// ---------- 4) 打包（系统 tar，顶层含 webapps/） ----------
// --force-local：仅 Windows Git Bash 的 GNU tar 需要（否则会把 "D:" 当成远端主机名）
const tarLocalArgs = process.platform === 'win32' ? ['--force-local'] : [];
const tar = spawnSync('tar', [...tarLocalArgs, '-czf', OUT_TAR, '-C', STAGE_ROOT, 'webapps'], { stdio: 'inherit' });
if (tar.error) fail('tar 执行失败（需系统 tar 命令）：' + tar.error.message);
if (tar.status !== 0) fail('tar 退出码 ' + tar.status);

// ---------- 5) 校验 ----------
const list = spawnSync('tar', [...tarLocalArgs, '-tzf', OUT_TAR], { encoding: 'utf8' });
if (list.status !== 0) fail('tar -t 校验失败');
// 注意：Windows tar 输出为 CRLF，需按 \r?\n 拆分并剔除尾部 \r
const files = list.stdout.split(/\r?\n/).map((s) => s.replace(/\r$/, '')).filter(Boolean);
const hasIndex = files.includes('webapps/' + TARGET_DIR + '/index.html');
const hasVersion = files.includes('webapps/' + TARGET_DIR + '/version.json');
const hasPortal = files.includes('webapps/portal.html');
const hasPremier12 = files.includes('webapps/premier12/index.html');
const hasPremier12Signup = files.includes('webapps/premier12/signup.html');
// server.js 为本地预览服务器，必须被 filter 排除；出现即视为打包缺陷
const hasPremier12Server = files.includes('webapps/premier12/server.js');
// 单轨约束：旧路径、旧入口、旧预压缩副本一律不得进包
const hasNextDir = files.some((f) => f === 'webapps/bs-ball-next' || f.startsWith('webapps/bs-ball-next/'));
const hasIndexGz = files.includes('webapps/' + TARGET_DIR + '/index.html.gz');
const hasVersionGz = files.includes('webapps/' + TARGET_DIR + '/version.json.gz');
const packedAssets = files
  .filter((f) => f.startsWith('webapps/' + TARGET_DIR + '/assets/'))
  .map((f) => f.slice(('webapps/' + TARGET_DIR + '/assets/').length))
  .filter((n) => n && !n.endsWith('/'))
  .sort();
const distAssets = fs.readdirSync(path.join(DIST_DIR, 'assets')).sort();
const assetsMatch = packedAssets.length === distAssets.length && distAssets.every((n, i) => n === packedAssets[i]);
const hasLegacyAssetGz = packedAssets.some((n) => n.endsWith('.gz'));
// 静态资源仍在（新版运行时依赖 /bs-ball/icons、geo 等）
const hasStaticIcons = files.includes('webapps/' + TARGET_DIR + '/icons/baseball-color.svg');
const builtIndex = fs.readFileSync(path.join(bsBall, 'index.html'), 'utf8');
const indexUsesBase = /(?:src|href)="\/bs-ball\/assets\//.test(builtIndex);
const indexHasLegacyPath = builtIndex.includes('/bs-ball-next/');
const stagedVersion = JSON.parse(fs.readFileSync(path.join(bsBall, 'version.json'), 'utf8'));
const versionBaseOk = stagedVersion.baseUrl === TARGET_BASE;
const assetCount = packedAssets.length;
if (!hasIndex || !hasVersion || !hasPortal || !hasPremier12 || !hasPremier12Signup || hasPremier12Server
  || hasNextDir || hasIndexGz || hasVersionGz || !assetsMatch || hasLegacyAssetGz
  || !hasStaticIcons || !indexUsesBase || indexHasLegacyPath || !versionBaseOk) {
  fail(
    `打包校验未通过（index.html=${hasIndex} version.json=${hasVersion} portal.html=${hasPortal} premier12=${hasPremier12} signup=${hasPremier12Signup} server.js=${hasPremier12Server}；`
    + `bs-ball-next=${hasNextDir} index.gz=${hasIndexGz} version.gz=${hasVersionGz} assetsMatch=${assetsMatch} legacyAssetGz=${hasLegacyAssetGz}；`
    + `staticIcons=${hasStaticIcons} indexBase=${indexUsesBase} indexLegacyPath=${indexHasLegacyPath} versionBase=${versionBaseOk}；条目=${files.length}）`
  );
}
log('打包校验通过：条目总数 =', files.length, '；assets 条目 =', assetCount);
log('输出：', OUT_TAR, '(' + (fs.statSync(OUT_TAR).size / 1024 / 1024).toFixed(1) + ' MB)');
