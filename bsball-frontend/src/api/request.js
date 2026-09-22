// 请求封装与鉴权会话处理 —— 行为移植自编译产物 request chunk
// 核心语义：
//  1) /api/x → /bsball-server/x 重写；公开路径不带 Authorization
//  2) Result<T>：code 省略/200/1000-1999 视为成功；401 触发会话失效流程（门户静默重试/管理端弹窗）
//  3) 多租户：X-Tenant-Code 头 + admin_token::<tenant> 键位（见 tokenStorage）
//  4) 403 权限文案统一为中文提示；网络异常归一化
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  DEFAULT_TENANT_CODE,
  isPortalRoute,
  currentTenantCodeFromUrl,
  stripQueryHash,
  isSafeRedirect
} from '../utils/tenantRoute';
import { getExplicitTenantCode, resolveToken, clearAllTenantAuth } from './tokenStorage';
import { useAuthStore } from '../stores/auth';

export const API_BASE = (import.meta.env && import.meta.env.VITE_API_BASE) || '';
export const BASE_URL = (import.meta.env && import.meta.env.BASE_URL) || '/bs-ball/';

// 版本检查配置（供 versionCheck 使用；值来自构建期环境，与编译产物一致）
export const VERSION_CONFIG = {
  checkEnabled: (import.meta.env && import.meta.env.VITE_VERSION_CHECK_ENABLED) === 'true',
  promptUpdate: (import.meta.env && import.meta.env.VITE_VERSION_PROMPT_UPDATE) === 'true',
  updateContent: (import.meta.env && import.meta.env.VITE_VERSION_UPDATE_CONTENT) || ''
};

export class ApiAuthSessionError extends Error {
  isAuthSessionError = true;
  constructor(message) {
    super(message);
    this.name = 'ApiAuthSessionError';
  }
}

export const isAuthSessionError = (e) => e instanceof ApiAuthSessionError;

export class ApiBizError extends Error {
  apiCode;
  constructor(message, apiCode) {
    super(message);
    this.name = 'ApiBizError';
    this.apiCode = apiCode;
  }
}

export const isApiBizError = (e) => e instanceof ApiBizError;

// ---------- URL 工具 ----------
export function rewriteUrl(url) {
  if (url.startsWith('http')) return url;
  if (API_BASE && url.startsWith('/api')) return `${API_BASE}${url.slice(4)}`;
  return url;
}

export function getApiOrigin() {
  if (API_BASE && API_BASE.startsWith('http')) return new URL(API_BASE).origin;
  if (typeof window !== 'undefined' && window.location?.origin) return window.location.origin;
  return '';
}

function apiBasePath() {
  if (!API_BASE) return '';
  if (API_BASE.startsWith('http')) {
    try {
      return new URL(API_BASE).pathname.replace(/\/+$/, '');
    } catch {
      return '';
    }
  }
  return API_BASE.replace(/\/+$/, '');
}

// 静态资源 URL 解析（上传回传的相对路径 → 可访问地址）
export function resolveAssetUrl(url) {
  if (!url || url.startsWith('http') || url.startsWith('data:')) return url || '';
  const normalized = url.startsWith('/') ? url : '/' + url;
  const basePath = apiBasePath();
  if (normalized.includes('/carousel/')) return normalized;
  if (API_BASE && API_BASE.startsWith('http')) {
    try {
      const origin = new URL(API_BASE).origin;
      return basePath && normalized.startsWith(basePath + '/') ? origin + normalized : origin + (basePath || '') + normalized;
    } catch {
      // fallthrough
    }
  }
  if (basePath && normalized.startsWith(basePath + '/')) return normalized;
  const combined = (basePath || '') + normalized;
  return combined.startsWith('/') ? combined : '/' + combined;
}

// 去除 API base 前缀
export function stripApiBase(url) {
  if (!url) return '';
  const basePath = apiBasePath();
  if (!basePath) return url;
  return url.replace(new RegExp(basePath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '/?', 'g'), '/');
}

// Result code 约定：省略 / 200 / 1000-1999 为成功
export function isSuccessCode(code) {
  if (code === undefined) return true;
  return code === 200 || (code >= 1000 && code < 2000);
}

// ---------- 请求头 ----------
function tenantHeaders() {
  const code = currentTenantCodeFromUrl() || getExplicitTenantCode();
  return code ? { 'X-Tenant-Code': code } : {};
}

function authHeaders() {
  const token = resolveToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// 公开路径：登录/注册/验证码等不携带 Authorization
function isPublicPath(url) {
  const path = rewriteUrl(url).split('?')[0];
  return (
    /\/auth\/(login|login-by-phone|register|legal-docs|sms)(\/|$)/.test(path) ||
    /\/auth\/captcha(\/|$)/.test(path) ||
    /\/sys\/article\/\d+$/.test(path)
  );
}

function buildHeaders(fullUrl, custom) {
  const needAuth = !isPublicPath(fullUrl);
  return {
    'Content-Type': 'application/json',
    ...tenantHeaders(),
    ...(needAuth ? authHeaders() : {}),
    ...custom
  };
}

// ---------- 鉴权会话失效处理 ----------
let dialogShown = false;
let warnShown = false;

function hasAnySession() {
  return typeof localStorage !== 'undefined' && !!resolveToken();
}

function isTenantPermissionMessage(msg, detail) {
  return /缺少租户|没有权限|无权访问|无权|权限不足|无权限|不属于.*租户|请使用当前账号所属租户/.test(`${msg || ''}${detail || ''}`);
}

function warnOnce(message) {
  if (warnShown) return;
  warnShown = true;
  ElMessage.warning(message || '无权访问该功能');
  window.setTimeout(() => {
    warnShown = false;
  }, 2500);
}

function shouldTreatAsPlainError(msg, detail) {
  return !(hasAnySession() && isTenantPermissionMessage(msg, detail));
}

function forceLogout() {
  clearAllTenantAuth();
  try {
    useAuthStore().clear();
  } catch {
    // store 未初始化时忽略
  }
}

function classifyAuthReason(msg, detail) {
  const d = (detail ?? '').trim();
  const m = (msg ?? '').trim();
  const combined = d + m;
  if (m.includes('令牌已过期') || d.includes('已过期') || (d.includes('过期') && !d.includes('无效')) || d.includes('失效')) return 'expired';
  if (m.includes('无效') || d.includes('无效') || combined.includes('签名') || m.includes('格式') || m.includes('不支持') || m.includes('校验失败')) return 'invalid_token';
  if (d.includes('请先登录')) return 'need_login';
  return 'expired';
}

function authReasonLabel(reason) {
  if (reason === 'expired') return '登录已过期';
  if (reason === 'invalid_token') return '身份令牌无效';
  if (reason === 'need_login') return '未登录或凭证缺失';
  return '身份认证失败';
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function buildAuthFailDialogHtml(msg, detail, reason) {
  const label = authReasonLabel(reason);
  const specific = detail?.trim() || msg?.trim() || '—';
  return `
<div class="auth-fail-dialog-inner">
  <div class="auth-fail-section">
    <div class="auth-fail-k">错误类型</div>
    <div class="auth-fail-type">${escapeHtml(label)}</div>
  </div>
  <div class="auth-fail-section">
    <div class="auth-fail-k">具体错误</div>
    <div class="auth-fail-detail">${escapeHtml(specific)}</div>
  </div>
  <div class="auth-fail-section">
    <div class="auth-fail-k">提示</div>
    <div class="auth-fail-hint">${escapeHtml('您的身份认证已过期或无效，请重新登录。')}</div>
  </div>
</div>`;
}

// 展示认证失败弹窗；确认后清空登录态并跳转管理端登录页
function showAuthFailureDialog(msg, detail) {
  if (typeof localStorage === 'undefined') return;
  const first = !dialogShown;
  dialogShown = true;
  const message = msg?.trim() || '登录已失效，请重新登录';
  const reason = classifyAuthReason(message, detail);
  if (!first) return;

  const base = BASE_URL.replace(/\/+$/, '') || '/';
  const fullPath = window.location.pathname;
  const relative = base && base !== '/' && fullPath.startsWith(base) ? fullPath.slice(base.length) || '/' : fullPath;
  const isAdminLogin = /\/admin\/login\/?$/.test(relative);
  let redirectTarget;
  if (isAdminLogin && typeof window !== 'undefined') {
    const queryRedirect = new URLSearchParams(window.location.search || '').get('redirect');
    redirectTarget = queryRedirect && queryRedirect.startsWith('/') ? queryRedirect : `/${DEFAULT_TENANT_CODE}/`;
  } else {
    redirectTarget = relative + (typeof window !== 'undefined' ? window.location.search || '' : '');
  }
  const tenantForLogin = getExplicitTenantCode() || DEFAULT_TENANT_CODE;
  const loginUrl = (base !== '/' ? base + '/' : '/') + `${tenantForLogin}/admin/login`;
  const gotoLogin = () => {
    try {
      forceLogout();
    } catch {
      // ignore
    }
    window.location.href =
      loginUrl + '?redirect=' + encodeURIComponent(redirectTarget) + '&reason=' + encodeURIComponent(reason) + '&fromAuthModal=1';
  };
  const html = buildAuthFailDialogHtml(message, detail, reason);
  ElMessageBox.alert(html, '身份认证失败', {
    confirmButtonText: '确定',
    dangerouslyUseHTMLString: true,
    customClass: 'auth-failure-msgbox',
    showClose: false,
    closeOnClickModal: false,
    closeOnPressEscape: false,
    beforeClose: (action, instance, done) => {
      dialogShown = false;
      if (action !== 'confirm') {
        done();
        return;
      }
      done();
      gotoLogin();
    }
  });
}

function httpErrorMessage(status, body) {
  if (status === 403) return body.msg || body.message || '没有权限';
  if (status === 404) return body.msg || body.message || '资源不存在';
  if (status >= 500) return '服务器繁忙，请稍后重试';
  return body.msg || body.message || `请求失败（${status}）`;
}

// ---------- 核心请求 ----------
export async function request(url, options = {}, retried = false) {
  const fullUrl = rewriteUrl(url);
  let res;
  try {
    res = await fetch(fullUrl, { ...options, headers: buildHeaders(fullUrl, options.headers) });
  } catch (e) {
    const message = e?.message;
    if (message === 'Failed to fetch' || message?.includes('NetworkError') || message?.includes('Load failed')) {
      throw new Error('网络异常或没有权限，请检查连接与登录状态');
    }
    throw e;
  }
  const body = await res.json().catch(() => ({}));

  // 门户页面：静默登出并重试一次
  const retryAfterSessionLoss = () => {
    if (retried || !isPortalRoute() || !hasAnySession()) return null;
    forceLogout();
    return request(url, options, true);
  };

  const throwAuthError = (msg, detail) => {
    if (isPortalRoute()) {
      forceLogout();
      throw new ApiAuthSessionError(msg);
    }
    if (shouldTreatAsPlainError(msg, detail)) {
      showAuthFailureDialog(msg, detail);
      throw new ApiAuthSessionError(msg);
    }
    warnOnce(msg);
    throw new ApiBizError(msg, 403);
  };

  if (!res.ok) {
    if (res.status === 401) {
      const msg = body.msg || body.message || '登录已失效，请重新登录';
      const detail = body.detail;
      if (isPublicPath(fullUrl)) return Promise.reject(new Error(msg));
      const retriedRequest = retryAfterSessionLoss();
      if (retriedRequest) return retriedRequest;
      throwAuthError(msg, detail);
    }
    let message = httpErrorMessage(res.status, body);
    if (res.status === 403) {
      const hasSession = hasAnySession();
      const raw = body.msg || body.message || '';
      if (!hasSession && (raw.includes('请登录') || raw.includes('无权限'))) {
        message = `未登录或无权访问：${raw}`;
      } else if (hasSession) {
        message = `权限不足：${raw || '没有权限'}`;
        warnOnce(message);
      }
    }
    throw new Error(message);
  }

  if (body.code === 401) {
    const msg = body.msg || body.message || '登录已失效，请重新登录';
    const detail = body.detail;
    if (isPublicPath(fullUrl)) return Promise.reject(new Error(msg));
    const retriedRequest = retryAfterSessionLoss();
    if (retriedRequest) return retriedRequest;
    throwAuthError(msg, detail);
  }

  if (!isSuccessCode(body.code)) {
    const code = body.code ?? 1;
    const raw = body.msg || body.message || '';
    let message = code === 403 ? raw || '没有权限' : isPublicPath(fullUrl) && raw ? raw : code >= 500 ? '服务器繁忙，请稍后重试' : raw || '操作失败';
    if (code === 403) {
      const hasSession = hasAnySession();
      const rawMsg = body.msg || body.message || '';
      if (!hasSession && (rawMsg.includes('请登录') || rawMsg.includes('无权限'))) {
        message = `未登录或无权访问：${rawMsg}`;
      } else if (hasSession) {
        message = `权限不足：${rawMsg || '没有权限'}`;
        warnOnce(message);
      }
    }
    throw new ApiBizError(message, code);
  }

  return body;
}

// ---------- 方法封装 ----------
export const get = (url) => request(url, { method: 'GET' });
export const post = (url, data, headers) => request(url, { method: 'POST', body: JSON.stringify(data), headers });
export const patch = (url, data, headers) => request(url, { method: 'PATCH', body: JSON.stringify(data), headers });
export const put = (url, data) => request(url, { method: 'PUT', body: JSON.stringify(data) });
export const del = (url) => request(url, { method: 'DELETE' });

// ---------- 列表/详情辅助 ----------
function withQuery(url, params) {
  if (!params || !Object.keys(params).length) return url;
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') query.set(key, String(value));
  }
  const qs = query.toString();
  if (!qs) return url;
  return url + (url.includes('?') ? '&' : '?') + qs;
}

// 列表（静默鉴权错误；其他错误 toast「加载失败」）
export async function fetchList(url, params) {
  try {
    const data = (await get(withQuery(url, params))).data;
    return { list: data?.list ?? [], total: data?.total ?? 0 };
  } catch (e) {
    if (isAuthSessionError(e)) return { list: [], total: 0 };
    ElMessage.error(e?.message ?? '加载失败');
    return { list: [], total: 0 };
  }
}

// 详情（静默鉴权错误与 404；其他错误 toast）
export async function fetchData(url, params) {
  try {
    return (await get(withQuery(url, params))).data ?? null;
  } catch (e) {
    if (!isAuthSessionError(e) && !(e instanceof ApiBizError && e.apiCode === 404)) {
      ElMessage.error(e?.message ?? '加载失败');
    }
    return null;
  }
}

// 详情（返回 {data,error}，无 toast）
export async function fetchResult(url) {
  try {
    return { data: (await get(url)).data ?? null, error: null };
  } catch (e) {
    if (isAuthSessionError(e)) return { data: null, error: null };
    if (e instanceof ApiBizError && e.apiCode === 404) return { data: null, error: null };
    return { data: null, error: e?.message ?? '加载失败' };
  }
}

// 列表（返回 {list,total,error}，无 toast）
export async function fetchListResult(url, params) {
  try {
    const data = (await get(withQuery(url, params))).data;
    return { list: data?.list ?? [], total: data?.total ?? 0, error: null };
  } catch (e) {
    if (isAuthSessionError(e)) return { list: [], total: 0, error: null };
    return { list: [], total: 0, error: e?.message ?? '加载失败' };
  }
}

// 全量拉取（pageSize 100，最多 200 页）
const FETCH_ALL_PAGE_SIZE = 100;
const FETCH_ALL_MAX_PAGES = 200;
export async function fetchAllPages(url, params) {
  const list = [];
  let page = 1;
  while (page <= FETCH_ALL_MAX_PAGES) {
    const result = await fetchListResult(url, { ...(params || {}), page, pageSize: FETCH_ALL_PAGE_SIZE });
    if (result.error) return { list: [], error: result.error };
    list.push(...result.list);
    if (list.length >= (result.total || 0) || result.list.length < FETCH_ALL_PAGE_SIZE) break;
    page += 1;
  }
  return { list, error: null };
}

// ---------- 门户辅助（埋点/上报/浏览计数） ----------
const PV_THROTTLE_MS = 300 * 1000;
const PV_SESSION_KEY = 'bs-ball-portal-pv-sent-ts-v1';

function allowPortalVisitSample(path) {
  if (typeof sessionStorage === 'undefined') return true;
  const now = Date.now();
  try {
    const raw = sessionStorage.getItem(PV_SESSION_KEY);
    const sent = raw ? JSON.parse(raw) : {};
    const last = Number(sent[path] || 0);
    if (last > 0 && now - last < PV_THROTTLE_MS) return false;
    sent[path] = now;
    sessionStorage.setItem(PV_SESSION_KEY, JSON.stringify(sent));
    return true;
  } catch {
    return true;
  }
}

// 门户访问埋点（30 分钟内同一路径只报一次；仅租户路径）
export function recordPortalVisit(rawPath) {
  if (!isSafeRedirect(rawPath)) return;
  const path = stripQueryHash(rawPath);
  if (!path || !allowPortalVisitSample(path)) return;
  const url = rewriteUrl('/api/portal/visit/record');
  fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...tenantHeaders() },
    body: JSON.stringify({ path })
  }).catch(() => {});
}

// 开发者工具打开上报
export function reportDevtoolsIssue(payload) {
  const url = rewriteUrl('/api/portal/devtools/report');
  fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...tenantHeaders() },
    body: JSON.stringify(payload)
  })
    .then(async (res) => {
      await res.json().catch(() => ({}));
    })
    .catch(() => {});
}

// 文章浏览量 +1（返回最新值）
export async function incrementArticleView(id) {
  try {
    const url = rewriteUrl(`/api/sys/article/increment-view/${id}`);
    const body = await (
      await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', ...tenantHeaders() },
        body: '{}'
      })
    ).json();
    if (isSuccessCode(body.code) && body.data?.viewCount != null) return Number(body.data.viewCount);
  } catch {
    // ignore
  }
  return null;
}
