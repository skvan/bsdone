// inject-legacy-button.mjs — 为旧版前端（webapps/bs-ball）注入「返回新版」兜底按钮（Issue #105）
// 架构：旧版原位 /bs-ball/；重建版部署于 /bs-ball-next/。本脚本幂等地：
//   1) 在旧版 index.html 的 </body> 前注入悬浮按钮（→ /bs-ball-next/）
//   2) 重新生成 index.html.gz（与 nginx gzip_static 保持一致，避免返回未处理的陈旧压缩页）
// 运行：node bsball_project/scripts/inject-legacy-button.mjs（repo 根执行）
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const REPO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const INDEX = path.join(REPO, 'bsball_project/webapps/bs-ball/index.html');
const GZ = INDEX + '.gz';

if (!fs.existsSync(INDEX)) {
  console.error('缺失：' + INDEX);
  process.exit(1);
}

const btnScript = `<script>
(function () {
  function mount() {
    if (document.getElementById('frontend-switch-fallback')) return;
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.id = 'frontend-switch-fallback';
    btn.textContent = '返回新版';
    btn.title = '返回新版前端（重建版）';
    btn.style.cssText = 'position:fixed;right:16px;top:16px;z-index:1900;padding:6px 14px;border-radius:18px;border:1px solid rgba(255,255,255,.35);background:rgba(15,32,58,.82);color:#fff;font-size:12px;line-height:20px;cursor:pointer;box-shadow:0 2px 8px rgba(0,0,0,.25);opacity:.72';
    btn.addEventListener('mouseenter', function () { btn.style.opacity = '1'; });
    btn.addEventListener('mouseleave', function () { btn.style.opacity = '.72'; });
    btn.addEventListener('click', function () { location.href = '/bs-ball-next/'; });
    document.body.appendChild(btn);
  }
  if (document.body) mount();
  else document.addEventListener('DOMContentLoaded', mount, { once: true });
})();
</script>
`;

let html = fs.readFileSync(INDEX, 'utf8');
if (html.includes('frontend-switch-fallback')) {
  console.log('按钮已存在，跳过注入');
} else {
  html = html.replace('</body>', btnScript + '</body>');
  fs.writeFileSync(INDEX, html, 'utf8');
  console.log('已注入「返回新版」按钮 → /bs-ball-next/');
}

// 重新生成 .gz（内容与明文一致）
const gz = zlib.gzipSync(Buffer.from(html, 'utf8'), { level: 9 });
fs.writeFileSync(GZ, gz);
console.log('已重生成 index.html.gz（' + gz.length + ' bytes）');
console.log('done');
