// 开发者工具防护链 —— 行为移植自编译产物入口 chunk（It/Uo/Yo/Ko/$o/zo/jo + it/ir/ha/or/Bt/nr）
// 机制：门户路径下检测开发者工具（外宽差 >110 或 console 探针，4s 节流缓存）；
//       命中后：遮挡层（阻断点击）+ debugger 陷阱（rAF 递归）+ 首次命中弹版权提示并上报（8 分钟节流）；
//       管理端路径不启用；登录的 superAdmin/admin 用户豁免；F12/Ctrl+Shift+I,J,C/Ctrl+U/Cmd+Alt+I,J,C 拦截。
import { watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAppConfigStore } from '../stores/appConfig';
import { useAuthStore } from '../stores/auth';
import { resolveToken, currentTenantCode } from '../api/tokenStorage';
import { reportDevtoolsIssue } from '../api/request';

// 常量（与编译产物一致）
const OVERLAY_POLL_MS = 120;
const OUTER_WIDTH_DELTA = 110;
const DETECT_THROTTLE_MS = 4000;
const REPORT_THROTTLE_KEY = 'bsball_devtools_report_at';
const REPORT_THROTTLE_MS = 480 * 1000;
const APP_VERSION = '1.0.0';

// 运行态
let overlayEl = null;
let detectTimer = null;
let viewportDisposer = null;
let trapRunning = false;
let trapFrame = 0;
let lastDetected = false;
let guardActive = false; // Te：当前路径是否处于防护（非管理端）
let installed = false;
let routerRef = null;

function isAdminPath(path) {
  return /\/admin(\/|$)/.test(String(path || ''));
}

function guardEnabled() {
  try {
    return useAppConfigStore().portalDevtoolsGuard !== false;
  } catch {
    return true;
  }
}

// 豁免：已登录且 superAdmin 或 username=admin
function isExemptUser() {
  try {
    if (typeof localStorage === 'undefined' || !resolveToken(currentTenantCode())) return false;
    const user = useAuthStore().user;
    if (!user) return false;
    if (user.superAdmin === true) return true;
    return String(user.username || '').trim().toLowerCase() === 'admin';
  } catch {
    return false;
  }
}

function protectionActive() {
  return guardEnabled() && !isExemptUser();
}

function overlayEnabled() {
  try {
    return protectionActive() && useAppConfigStore().portalDevtoolsGuardOverlay !== false;
  } catch {
    return protectionActive();
  }
}

function trapEnabled() {
  try {
    return protectionActive() && useAppConfigStore().portalDevtoolsGuardDebuggerTrap !== false;
  } catch {
    return protectionActive();
  }
}

function noticeEnabled() {
  try {
    return guardEnabled() && useAppConfigStore().portalDevtoolsGuardCopyrightNotice !== false;
  } catch {
    return guardEnabled();
  }
}

// ---------- 检测 ----------
function byOuterWidth() {
  return window.outerWidth - window.innerWidth > OUTER_WIDTH_DELTA;
}

function probeConsole() {
  let detected = false;
  const mark = () => {
    detected = true;
  };
  try {
    const trap = { r: /./, el: document.createElement('div') };
    trap.r.toString = () => {
      mark();
      return '';
    };
    Object.defineProperty(trap.el, 'id', {
      get() {
        mark();
        return 'bsball-devtools-probe';
      },
      configurable: true
    });
    if (console.log(trap), detected) return detected;
    if (console.warn(trap), detected) return detected;
    if (console.info(trap), detected) return detected;
    if (console.dir(trap), detected) return detected;
    try {
      console.table(trap);
    } catch {
      // ignore
    }
  } catch {
    return false;
  }
  return detected;
}

let detectCheckedAt = 0;
let detectCached = false;
export function isDevtoolsOpen() {
  if (byOuterWidth()) {
    detectCached = false;
    return true;
  }
  const now = Date.now();
  if (detectCheckedAt === 0 || now - detectCheckedAt >= DETECT_THROTTLE_MS) {
    detectCheckedAt = now;
    detectCached = probeConsole();
  }
  return detectCached;
}

// ---------- 上报上下文与节流 ----------
function buildContext() {
  const nav = typeof navigator !== 'undefined' ? navigator : undefined;
  const scr = typeof screen !== 'undefined' ? screen : undefined;
  const vv = typeof window !== 'undefined' ? window.visualViewport : undefined;
  let timezone;
  try {
    timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  } catch {
    timezone = undefined;
  }
  const route = routerRef?.currentRoute?.value;
  const fullPath = route?.fullPath ?? '';
  const name = route?.name;
  const meta = route?.meta;
  const title = meta && typeof meta.title === 'string' ? meta.title : typeof meta?.titleKey === 'string' ? meta.titleKey : undefined;
  const fallbackPath = typeof window !== 'undefined' ? `${window.location.pathname}${window.location.search || ''}` : '';
  return {
    routeFullPath: fullPath || fallbackPath,
    path: fullPath || fallbackPath,
    routeName: name != null ? String(name) : undefined,
    routeTitle: title,
    userAgent: nav?.userAgent ?? '',
    language: nav?.language,
    platform: nav?.platform,
    screenWidth: scr?.width,
    screenHeight: scr?.height,
    viewportWidth: vv?.width ?? (typeof window !== 'undefined' ? window.innerWidth : undefined),
    viewportHeight: vv?.height ?? (typeof window !== 'undefined' ? window.innerHeight : undefined),
    timezone,
    devicePixelRatio: typeof window !== 'undefined' ? window.devicePixelRatio : undefined,
    hardwareConcurrency: nav?.hardwareConcurrency,
    maxTouchPoints: nav?.maxTouchPoints,
    appVersion: APP_VERSION
  };
}

function reportThrottled() {
  try {
    const last = Number(sessionStorage.getItem(REPORT_THROTTLE_KEY) || '0');
    if (Date.now() - last < REPORT_THROTTLE_MS) return true;
    sessionStorage.setItem(REPORT_THROTTLE_KEY, String(Date.now()));
  } catch {
    // ignore
  }
  return false;
}

function reportOnce() {
  if (!reportThrottled()) reportDevtoolsIssue(buildContext());
}

// 控制台版权提示（首次命中时）
function consoleNotice() {
  try {
    const version = APP_VERSION;
    console.log('%c⚠ 提示', 'color:#c9a227;font-size:13px;font-weight:bold;padding:2px 0');
    console.log(
      '%c本网站（含页面脚本与展示内容）受著作权及相关法律保护。未经授权，禁止通过开发者工具或其他方式抓取、破解、篡改或传播本站代码与数据。',
      'color:#909399;font-size:12px;line-height:1.6'
    );
    console.log(
      '%c若您为授权运维或开发人员，请通过正规渠道登录后台或联系管理员获取文档与支持。',
      'color:#606266;font-size:12px;line-height:1.6'
    );
    console.log('%c前端构建版本：' + version, 'color:#a8abb2;font-size:11px');
  } catch {
    // ignore
  }
}

function onDetected(showNotice) {
  if (showNotice) consoleNotice();
  reportOnce();
}

// ---------- 遮挡层 / 陷阱 ----------
function ensureOverlay() {
  if (overlayEl) return;
  overlayEl = document.createElement('div');
  overlayEl.className = 'devtools-guard-overlay';
  overlayEl.setAttribute('aria-hidden', 'true');
  overlayEl.style.cssText = [
    'position:fixed',
    'inset:0',
    'z-index:2147483646',
    'background:transparent',
    'pointer-events:auto',
    'cursor:not-allowed',
    'touch-action:none'
  ].join(';');
  document.body.appendChild(overlayEl);
}

function removeOverlay() {
  overlayEl?.remove();
  overlayEl = null;
}

function stopTrap() {
  if (trapFrame !== 0) cancelAnimationFrame(trapFrame);
  trapFrame = 0;
  trapRunning = false;
}

function runTrap() {
  if (!guardActive || !trapEnabled() || !isDevtoolsOpen()) {
    stopTrap();
    return;
  }
  trapRunning = true;
  // eslint-disable-next-line no-debugger
  debugger;
  if (!guardActive || !trapEnabled() || !isDevtoolsOpen()) {
    stopTrap();
    return;
  }
  trapFrame = requestAnimationFrame(() => {
    trapFrame = 0;
    runTrap();
  });
}

function startTrap() {
  if (!trapRunning) runTrap();
}

// ---------- 主同步 ----------
function sync() {
  if (!guardActive || !guardEnabled()) {
    removeOverlay();
    stopTrap();
    lastDetected = false;
    return;
  }
  const detected = isDevtoolsOpen();
  if (detected && !lastDetected) onDetected(noticeEnabled());
  lastDetected = detected;
  if (detected) {
    if (overlayEnabled()) ensureOverlay();
    else removeOverlay();
    if (trapEnabled()) startTrap();
    else stopTrap();
  } else {
    removeOverlay();
    stopTrap();
  }
}

function onViewportChange() {
  sync();
}

function attachViewportListeners() {
  window.addEventListener('resize', onViewportChange);
  const vv = window.visualViewport;
  if (vv) {
    const handler = () => sync();
    vv.addEventListener('resize', handler);
    vv.addEventListener('scroll', handler);
    viewportDisposer = () => {
      vv.removeEventListener('resize', handler);
      vv.removeEventListener('scroll', handler);
      viewportDisposer = null;
    };
  }
}

function detachViewportListeners() {
  window.removeEventListener('resize', onViewportChange);
  viewportDisposer?.();
}

function start() {
  if (detectTimer == null) {
    attachViewportListeners();
    detectTimer = window.setInterval(sync, OVERLAY_POLL_MS);
    sync();
  }
}

function stop() {
  if (detectTimer != null) {
    clearInterval(detectTimer);
    detectTimer = null;
  }
  detachViewportListeners();
  removeOverlay();
  stopTrap();
  lastDetected = false;
}

// 键位拦截（F12 / Ctrl+Shift+I/J/C / Ctrl+U / Cmd+Alt+I/J/C）
function onKeydown(event) {
  if (!guardActive || !protectionActive()) return;
  if (event.key === 'F12') {
    event.preventDefault();
    event.stopPropagation();
    return;
  }
  if (event.ctrlKey && event.shiftKey && ['I', 'J', 'C', 'i', 'j', 'c'].includes(event.key)) {
    event.preventDefault();
    event.stopPropagation();
    return;
  }
  if (event.ctrlKey && (event.key === 'u' || event.key === 'U')) {
    event.preventDefault();
    event.stopPropagation();
    return;
  }
  if (event.metaKey && event.altKey && ['I', 'J', 'C', 'i', 'j', 'c'].includes(event.key)) {
    event.preventDefault();
    event.stopPropagation();
  }
}

// 路径切换时更新启用态（管理端不防护）
function applyForPath(path) {
  guardActive = !isAdminPath(path);
  if (!guardEnabled()) {
    stop();
    return;
  }
  if (guardActive) start();
  else stop();
}

// 安装（对应编译产物 nr(router)；main.js 在 router.isReady 后调用）
export function installPortalDevtoolsGuard(router) {
  if (installed) return;
  installed = true;
  routerRef = router;

  const appConfig = useAppConfigStore();
  const auth = useAuthStore();

  router.afterEach((to) => {
    applyForPath(to.path);
  });
  watch(
    () => [
      appConfig.portalDevtoolsGuard,
      appConfig.portalDevtoolsGuardOverlay,
      appConfig.portalDevtoolsGuardDebuggerTrap,
      appConfig.portalDevtoolsGuardCopyrightNotice
    ],
    () => applyForPath(router.currentRoute.value.path),
    { immediate: true }
  );
  watch(() => auth.user, () => applyForPath(router.currentRoute.value.path), { deep: true });
  window.addEventListener('keydown', onKeydown, true);
}
