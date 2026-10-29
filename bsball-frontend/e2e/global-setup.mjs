import net from 'node:net';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// #511 防呆：playwright.config.js 里 reuseExistingServer 在本地为 true，
// 若目标端口上残留着「另一个 checkout」起的 dev server，测试会静默复用它、
// 打到旧代码 → 出现与真实代码无关的假绿/假红。跑测试前先校验端口归属。
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoFrontend = path.resolve(__dirname, '..');

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

// 取监听该端口的进程工作目录（cwd）。取不到返回 null（无法判定归属）。
function servingCwd(port) {
  try {
    const pids = execFileSync('lsof', ['-nP', `-iTCP:${port}`, '-sTCP:LISTEN', '-t'], { encoding: 'utf8' })
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);
    const cwds = new Set();
    for (const pid of pids) {
      const out = execFileSync('lsof', ['-a', '-p', pid, '-d', 'cwd', '-Fn'], { encoding: 'utf8' });
      for (const line of out.split('\n')) {
        // lsof -Fn：cwd 条目的名称行以 'n' 开头，其后即目录绝对路径
        if (line.startsWith('n') && line.length > 1) cwds.add(line.slice(1));
      }
    }
    if (cwds.size === 0) return null;
    // 只要有一个监听者不在当前树，就视为「跨 checkout 复用」风险
    for (const cwd of cwds) if (path.resolve(cwd) !== repoFrontend) return cwd;
    return repoFrontend;
  } catch {
    return null;
  }
}

export default async function globalSetup() {
  // 显式指定外部目标（部署环境）时，由使用者负责，跳过本地端口指纹检查。
  if (process.env.E2E_BASE_URL) return;
  const port = Number(process.env.E2E_PORT || 3000);
  if (!(await isPortListening(port))) return; // 端口空闲：Playwright 会从当前树自起新 server
  const cwd = servingCwd(port);
  if (cwd && path.resolve(cwd) === repoFrontend) return; // 端口上正是当前树的 server：合法复用
  const detail = cwd
    ? `  端口 ${port} 上的 dev server 来自另一个目录：\n    服务中: ${cwd}\n    当前树: ${repoFrontend}`
    : `  端口 ${port} 上已有服务，但无法确认它来自当前工作树。`;
  throw new Error(
    `[e2e guard] 检测到端口复用风险，已中止以免误测旧代码（见 issue #511）。\n${detail}\n\n` +
    `请二选一：\n` +
    `  1) 停掉旧服务后重跑：kill $(lsof -nP -iTCP:${port} -sTCP:LISTEN -t)\n` +
    `  2) 换干净端口（会强制从当前树起新 server）：E2E_PORT=<空闲端口> npm run test:e2e:local\n`
  );
}
