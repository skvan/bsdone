// 新旧版前端兜底切换入口（Issue #105）
// 架构（用户确认）：旧版原位 /bs-ball/（零改动、base 天然匹配）；重建版部署于 /bs-ball-next/。
// 按钮形态（用户确认）：优先像「语言切换」那样置于顶栏——
//   - 门户：在 .portal-header-btn.lang-trigger 之前插入同款类按钮（锚点存在时）
//   - 管理台：在 .user-avatar 之前插入同风格文字按钮
//   - 保底：启动即挂右上角悬浮按钮；随后后台低频检查顶栏锚点，出现则迁移进顶栏（上限 30s）
//   - 补位：MutationObserver 监听 #app，按钮被布局卸载时自动重新挂载
// 设计口径：默认新版；仅手动兜底入口（无偏好记忆、无自动跳转）。
// 说明：纯 DOM 注入（不改动任何生成件/组件），仅浏览器环境生效；幂等。

export const LEGACY_URL = '/bs-ball/';
const BTN_ID = 'frontend-switch-fallback';
const TITLE = '遇到问题时切回旧版前端（原有版本）';

// 页面级版本映射（H27）：新版→旧版按当前路径前缀互换（两版路由同构）；
// 特例：入口页 → 旧版首页；介绍页 → 旧版介绍页；根路径兜底旧版首页。
// dev（base=/bs-ball/）下映射等价自指（本地无旧版），属预期限制。
export function legacyTarget() {
  const base = ((import.meta.env && import.meta.env.BASE_URL) || '/bs-ball/').replace(/\/+$/, '');
  let p = (typeof location !== 'undefined' && location.pathname) || '/';
  if (base && base !== '/' && p.startsWith(base)) p = p.slice(base.length) || '/';
  p = p.replace(/\/+$/, '') || '/';
  if (p === '/welcome') return '/bs-ball/';
  if (p === '/intro') return '/index.html';
  return '/bs-ball' + (p === '/' ? '/' : p);
}

function makeButton(className, text) {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.id = BTN_ID;
  btn.textContent = text;
  btn.title = TITLE;
  if (className) btn.className = className;
  btn.style.cursor = 'pointer';
  btn.addEventListener('click', () => {
    location.href = legacyTarget();
  });
  return btn;
}

// 右上角悬浮（保底形态）
function makeFloatingButton() {
  const btn = makeButton('', '切回旧版');
  btn.style.cssText = [
    'position:fixed',
    'right:16px',
    'top:16px',
    'z-index:1900',
    'padding:6px 14px',
    'border-radius:18px',
    'border:1px solid rgba(255,255,255,.35)',
    'background:rgba(15,32,58,.82)',
    'color:#fff',
    'font-size:12px',
    'line-height:20px',
    'box-shadow:0 2px 8px rgba(0,0,0,.25)',
    'opacity:.72',
    'cursor:pointer'
  ].join(';');
  return btn;
}

// 顶栏形态：门户在语言按钮前；管理台在头像前。返回 true 表示按钮已处于顶栏
function placeIntoHeader() {
  const existing = document.getElementById(BTN_ID);
  if (existing && existing.dataset.placement === 'header') return true;
  // 门户：语言按钮之前（同款 portal-header-btn 类，样式自然继承）
  const langTrigger = document.querySelector('.portal-header-btn.lang-trigger');
  if (langTrigger && langTrigger.parentElement) {
    const btn = makeButton('portal-header-btn', '切回旧版');
    btn.dataset.placement = 'header';
    langTrigger.parentElement.insertBefore(btn, langTrigger);
    if (existing) existing.remove();
    return true;
  }
  // 管理台：用户头像之前（同风格文字按钮）
  const avatar = document.querySelector('.user-avatar');
  if (avatar && avatar.parentElement) {
    const btn = makeButton('', '切回旧版');
    btn.dataset.placement = 'header';
    btn.style.cssText = [
      'cursor:pointer',
      'padding:4px 10px',
      'border-radius:14px',
      'border:1px solid rgba(255,255,255,.25)',
      'background:rgba(255,255,255,.08)',
      'color:inherit',
      'font-size:12px',
      'line-height:18px',
      'margin-right:10px',
      'vertical-align:middle'
    ].join(';');
    avatar.parentElement.insertBefore(btn, avatar);
    if (existing) existing.remove();
    return true;
  }
  // 介绍页（H27/H28）：插入导航条右侧（.nav-inner 内、☰ 之前）——
  // 桌面在导航条右端、移动端在 ☰ 左侧，全视口常显且不遮挡 CTA/菜单键；
  // 不可放入 .nav-links（≤640px 随折叠菜单隐藏，H28 用户反馈修复）
  const introBar = document.querySelector('.intro-page .nav-inner');
  if (introBar) {
    const btn = makeButton('', '切回旧版');
    btn.dataset.placement = 'header';
    btn.style.cssText = [
      'cursor:pointer',
      'padding:6px 14px',
      'border-radius:18px',
      'border:1px solid rgba(255,255,255,.35)',
      'background:rgba(255,255,255,.08)',
      'color:rgba(255,255,255,.85)',
      'font-size:13px',
      'font-weight:500',
      'line-height:20px',
      'margin-left:16px',
      'margin-right:8px',
      'flex-shrink:0'
    ].join(';');
    const toggle = introBar.querySelector('.nav-toggle');
    if (toggle) introBar.insertBefore(btn, toggle);
    else introBar.appendChild(btn);
    if (existing) existing.remove();
    return true;
  }
  return false;
}

// 保底：确保 body 上存在悬浮按钮
function ensureFloating() {
  const existing = document.getElementById(BTN_ID);
  if (existing) return;
  if (!document.body) return;
  document.body.appendChild(makeFloatingButton());
}

// 挂载兜底切换入口（幂等；SSR/无 DOM 环境安全退出）
export function installFrontendSwitchButton() {
  if (typeof document === 'undefined' || typeof location === 'undefined') return;
  const start = () => {
    ensureFloating();
    // 后台低频尝试迁移进顶栏（锚点渲染时机不定；上限 30s）
    const deadline = Date.now() + 30000;
    let migrated = false;
    const timer = setInterval(() => {
      migrated = placeIntoHeader();
      if (migrated || Date.now() > deadline) clearInterval(timer);
    }, 1000);
    // 补位：布局切换导致按钮被卸载时恢复（挂 body 保底）
    const observer = new MutationObserver(() => {
      const btn = document.getElementById(BTN_ID);
      if (!btn || !document.contains(btn)) {
        ensureFloating();
        if (!migrated && Date.now() <= deadline) placeIntoHeader();
      }
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
  };
  if (document.body) start();
  else document.addEventListener('DOMContentLoaded', start, { once: true });
}
