// inject-legacy-button.mjs — 为旧版各页面注入「新版入口」悬浮按钮（Issue #105 扩展；H27 改页面级映射）
// 架构：旧版原位 /bs-ball/；重建版 /bs-ball-next/。按钮点击时按当前路径映射到新版对应页（两版路由同构，见注入块 newTarget）：
//   - webapps/bs-ball/index.html（旧版 SPA 外壳，覆盖全部旧版页面）：按钮「返回新版」
//   - webapps/index.html（旧版系统介绍页）：按钮「进入新版」→ /bs-ball-next/intro
// 注：webapps/portal.html 已改为「直通新版」页（H25），不再注入按钮。
// 注入块含标记 frontend-switch-mapped-v2；旧版（固定 /bs-ball-next/）块会被自动替换。
// 并对同名 .gz 重新生成（nginx gzip_static 优先命中 .gz，必须同步）：
//   gz 基行尾对齐 git 索引（i/lf→LF、i/crlf→CRLF、-text→原样），
//   否则 Linux CI（LF 检出）的 zcat|cmp 门禁会判 stale（2026-09-28 实测教训）。
// 运行：node bsball_project/scripts/inject-legacy-button.mjs（repo 根执行）
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const REPO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

const TARGETS = [
  { rel: 'bsball_project/webapps/bs-ball/index.html', text: '返回新版', title: '返回新版前端（重建版）' },
  { rel: 'bsball_project/webapps/index.html', text: '进入新版', title: '进入新版前端（重建版 /bs-ball-next/）' }
];

// git 索引行尾表（gz 基行尾对齐用）
const indexEol = new Map();
try {
  const out = execFileSync('git', ['ls-files', '--eol', '--', 'bsball_project/webapps'], { cwd: REPO, encoding: 'utf8' });
  for (const line of out.split('\n')) {
    const parts = line.split(/\t+/);
    if (parts.length < 2) continue;
    const iField = parts[0].trim().split(/\s+/)[0];
    if (!iField.startsWith('i/')) continue;
    const v = iField.slice(2);
    indexEol.set(parts[1].trim(), v === 'lf' ? 'lf' : v === 'crlf' ? 'crlf' : 'other');
  }
} catch { /* git 不可用：全部按 other（原样）处理 */ }

for (const t of TARGETS) {
  const file = path.join(REPO, t.rel);
  if (!fs.existsSync(file)) {
    console.error('缺失：' + t.rel + '（跳过）');
    continue;
  }
  const btnScript = `<script>
/* frontend-switch-mapped-v2：页面级版本映射（H27）——旧版→新版按当前路径互换；
   特例：旧版介绍页(/index.html) → 新版介绍页；旧版首页(/bs-ball/、/bs-ball) → 新版门户首页 */
(function () {
  function newTarget() {
    var p = window.location.pathname || '/';
    if (p === '/index.html') return '/bs-ball-next/intro';
    if (p === '/bs-ball' || p === '/bs-ball/') return '/bs-ball-next/bs-ball/';
    if (p.indexOf('/bs-ball/') === 0) return '/bs-ball-next' + p.slice('/bs-ball'.length);
    return '/bs-ball-next/';
  }
  function mount() {
    if (document.getElementById('frontend-switch-fallback')) return;
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.id = 'frontend-switch-fallback';
    btn.textContent = '${t.text}';
    btn.title = '${t.title}';
    btn.style.cssText = 'position:fixed;right:16px;top:16px;z-index:1900;padding:6px 14px;border-radius:18px;border:1px solid rgba(255,255,255,.35);background:rgba(15,32,58,.82);color:#fff;font-size:12px;line-height:20px;cursor:pointer;box-shadow:0 2px 8px rgba(0,0,0,.25);opacity:.72';
    btn.addEventListener('mouseenter', function () { btn.style.opacity = '1'; });
    btn.addEventListener('mouseleave', function () { btn.style.opacity = '.72'; });
    btn.addEventListener('click', function () { location.href = newTarget(); });
    document.body.appendChild(btn);
  }
  if (document.body) mount();
  else document.addEventListener('DOMContentLoaded', mount, { once: true });
})();
</script>
`;
  let html = fs.readFileSync(file, 'utf8');
  if (html.includes('frontend-switch-mapped-v2')) {
    console.log(t.rel + '：已是最新映射版注入（v2），跳过');
  } else {
    // 替换旧版固定指向 /bs-ball-next/ 的注入块
    const oldBlock = /<script>\s*\(function \(\) \{\s*function mount\(\) \{[\s\S]*?frontend-switch-fallback[\s\S]*?\}\)\(\);\s*<\/script>/;
    if (oldBlock.test(html)) {
      html = html.replace(oldBlock, '');
      console.log(t.rel + '：已移除旧注入块（固定指向）');
    }
    if (!html.includes('</body>')) {
      console.error(t.rel + '：未找到 </body>，跳过注入');
      continue;
    }
    html = html.replace('</body>', btnScript + '</body>');
    fs.writeFileSync(file, html, 'utf8');
    console.log(t.rel + '：已注入映射版「' + t.text + '」按钮（按当前页映射 → 新版对应页）');
  }
  // .gz 同步（存在才处理；基行尾对齐索引）
  const gzPath = file + '.gz';
  if (fs.existsSync(gzPath)) {
    const eol = indexEol.get(t.rel) || 'other';
    let gzBase = html;
    if (eol === 'lf') gzBase = html.replace(/\r\n/g, '\n');
    else if (eol === 'crlf') gzBase = html.replace(/\r?\n/g, '\r\n');
    const gz = zlib.gzipSync(Buffer.from(gzBase, 'utf8'), { level: 9 });
    fs.writeFileSync(gzPath, gz);
    console.log(t.rel + '.gz：已重生成（' + gz.length + ' bytes；基行尾 ' + eol + '）');
  } else {
    console.log(t.rel + '.gz：无同名 .gz（跳过）');
  }
}
console.log('done');
