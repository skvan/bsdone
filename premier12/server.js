// 簡易靜態檔案伺服器（預覽用）
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = __dirname;
const types = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
};

const server = http.createServer((req, res) => {
  let p;
  try {
    p = decodeURIComponent(req.url.split('?')[0]);
  } catch (e) {
    res.writeHead(400); return res.end('Bad Request'); // 畸形 URL（如 /%）會拋 URIError
  }
  if (p.includes('\0')) { res.writeHead(400); return res.end('Bad Request'); } // NUL 會讓 fs 同步拋錯
  if (p === '/') p = '/premier12.html';
  const file = path.join(root, p); // path.join 已包含正规化，可防 ../ 逸出 root
  if (file !== root && !file.startsWith(root + path.sep)) { res.writeHead(403); return res.end('Forbidden'); }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); return res.end('Not Found'); }
    res.writeHead(200, { 'Content-Type': types[path.extname(file).toLowerCase()] || 'application/octet-stream', 'Cache-Control': 'no-store, no-cache, must-revalidate' });
    res.end(data);
  });
});
server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') console.error('Port 3001 已被佔用，請先關閉佔用的程式');
  else console.error(err.message);
  process.exit(1);
});
server.listen(3001, '127.0.0.1', () => console.log('Static server running at http://localhost:3001'));
