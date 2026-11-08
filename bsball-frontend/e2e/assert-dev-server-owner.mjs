import net from 'node:net';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// #511 防呆：npm pretest:e2e / pretest:e2e:local 在 Playwright 启动 webServer 之前运行。
// 若目标端口上是「另一个 checkout」的 dev server，会静默复用旧代码 → 假绿/假红。
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoFrontend = realpath(path.resolve(__dirname, '..'));

function realpath(value) {
  try {
    return fs.realpathSync.native(value);
  } catch {
    return path.resolve(value);
  }
}

function norm(value) {
  return String(value ?? '').replace(/\\/g, '/').replace(/\/+$/, '').toLowerCase();
}

function isPortListening(port, host = '127.0.0.1', timeout = 800) {
  return new Promise((resolve) => {
    const socket = net.connect({ host, port });
    const done = (result) => {
      socket.destroy();
      resolve(result);
    };
    socket.setTimeout(timeout);
    socket.once('connect', () => done(true));
    socket.once('timeout', () => done(false));
    socket.once('error', () => done(false));
  });
}

function listenerPids(port) {
  try {
    return execFileSync('lsof', ['-nP', `-iTCP:${port}`, '-sTCP:LISTEN', '-t'], {
      encoding: 'utf8', timeout: 5000
    })
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
      .map(Number)
      .filter((n) => n > 0);
  } catch {
    return null;
  }
}

/**
 * 返回当前树路径（同一棵树）、其他归属描述（跨树/未知混用）或 null（无法判定）。
 * Unix 用监听进程 cwd；Windows 无法可靠取 cwd，端口被占时一律 fail-closed。
 */
function servingOwner(port) {
  if (process.platform === 'win32') return { windows: true };
  const pids = listenerPids(port);
  if (pids == null || pids.length === 0) return null;
  try {
    const cwds = new Set();
    for (const pid of pids) {
      const out = execFileSync('lsof', ['-a', '-p', String(pid), '-d', 'cwd', '-Fn'], {
        encoding: 'utf8', timeout: 5000
      });
      for (const line of out.split('\n')) {
        if (line.startsWith('n') && line.length > 1) cwds.add(realpath(line.slice(1)));
      }
    }
    if (cwds.size === 0) return null;
    for (const cwd of cwds) {
      if (norm(cwd) !== norm(repoFrontend)) return [...cwds].join(' | ');
    }
    return repoFrontend;
  } catch {
    return null;
  }
}

async function main() {
  if (process.env.E2E_BASE_URL) return; // 打部署环境时由使用者负责
  const port = Number(process.env.E2E_PORT || 3000);
  if (!(await isPortListening(port))) return; // 端口空闲：Playwright 会从当前树起新 server
  const owner = servingOwner(port);
  if (typeof owner === 'string' && norm(owner) === norm(repoFrontend)) return;

  const detail = owner?.windows
    ? `  端口 ${port} 上已有服务。Windows 下无法可靠判断进程 cwd，为避免误测旧代码一律中止。`
    : owner
      ? `  端口 ${port} 上的 dev server 来自另一个目录/进程：\n    服务中: ${owner}\n    当前树: ${repoFrontend}`
      : `  端口 ${port} 上已有服务，但无法确认它来自当前工作树。`;
  const kill = process.platform === 'win32'
    ? `taskkill /PID <pid> /F（PID 用 netstat -ano | findstr :${port} 查）`
    : `kill $(lsof -nP -iTCP:${port} -sTCP:LISTEN -t)`;
  console.error(
    `[e2e guard] 检测到端口复用风险，已中止以免误测旧代码（见 issue #511）。\n${detail}\n\n` +
    `请二选一：\n` +
    `  1) 停掉旧服务后重跑：${kill}\n` +
    `  2) 换干净端口（会强制从当前树起新 server）：E2E_PORT=<空闲端口> npm run test:e2e:local\n`
  );
  process.exit(1);
}

main();
