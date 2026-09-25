// 版本更新检查 —— 行为移植自编译产物入口 chunk（Ir()）
// 机制：挂载后立即检查 + 5 分钟轮询 + 页面可见性触发；
//       promptUpdate=false 或同版本 30 分钟内已提示 → 跳过；确认刷新会记账（sessionStorage）并 reload。
import { h } from 'vue';
import { ElIcon, ElMessageBox } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import 'dayjs/locale/zh-cn';
import 'dayjs/locale/zh-tw';
import 'dayjs/locale/en';
import 'dayjs/locale/ko';
import 'dayjs/locale/ja';
import { t, vendorLocaleCode } from '../i18n';
import { BASE_URL } from '../api/request';

dayjs.extend(utc);
dayjs.extend(timezone);

const CHECK_INTERVAL_MS = 300 * 1000;
export const RELOAD_ACK_KEY = 'bs-ball-version-reload-ack';
const RELOAD_ACK_TTL_MS = 1800 * 1000;
const DISPLAY_TIMEZONE = 'Asia/Shanghai';

// 运行版本构建标识（编译产物中的内联常量；与 version.json 对比判定是否提示）
const APP_VERSION = '1.1.0';
const APP_BUILD_TIME = '2026-07-01T14:33:17.536+08:00';

function normalizeVersionPayload(payload) {
  return {
    version: (payload.version || '').trim(),
    buildTime: (payload.buildTime || '').trim()
  };
}

// 读取刷新记账（过期自动清理）
function readReloadAck() {
  if (typeof sessionStorage === 'undefined') return null;
  try {
    const raw = sessionStorage.getItem(RELOAD_ACK_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (parsed == null || typeof parsed !== 'object') return null;
    const at = parsed.at;
    if (typeof at !== 'number' || Number.isNaN(at) || Date.now() - at > RELOAD_ACK_TTL_MS) {
      clearReloadAck();
      return null;
    }
    return {
      version: (parsed.version || '').trim(),
      buildTime: (parsed.buildTime || '').trim(),
      at
    };
  } catch {
    return null;
  }
}

function writeReloadAck(payload) {
  if (typeof sessionStorage === 'undefined') return;
  const record = { ...normalizeVersionPayload(payload), at: Date.now() };
  sessionStorage.setItem(RELOAD_ACK_KEY, JSON.stringify(record));
}

function clearReloadAck() {
  if (typeof sessionStorage === 'undefined') return;
  sessionStorage.removeItem(RELOAD_ACK_KEY);
}

// 同版本同构建时间且已提示过 → 不再提示
function reloadAckMatches(payload) {
  const ack = readReloadAck();
  if (!ack) return false;
  const current = normalizeVersionPayload(payload);
  return ack.version === current.version && ack.buildTime === current.buildTime;
}

function ensureTrailingSlash(base) {
  return base.endsWith('/') ? base : `${base}/`;
}

// 部署版本与运行版本一致（含默认构建）→ 无需提示
function shouldPrompt(payload) {
  const version = (payload.version || '').trim();
  const buildTime = (payload.buildTime || '').trim();
  return version !== APP_VERSION || buildTime !== APP_BUILD_TIME;
}

// 更新内容：支持 \\r\\n / \\n 转义与多行
function parseContentLines(rawContent) {
  const content = (rawContent || '').trim();
  if (!content) return [];
  return content
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
}

// 构建时间展示：Asia/Shanghai + 当前语言（YYYY.MM.DD HH:mm:ss (ddd)）
function formatBuildTime(value) {
  const raw = (value || '').trim();
  if (!raw) return '-';
  const parsed = dayjs(raw).tz(DISPLAY_TIMEZONE);
  if (!parsed.isValid()) return raw;
  const localized = parsed.locale(vendorLocaleCode());
  return `${localized.format('YYYY.MM.DD HH:mm:ss')} (${localized.format('ddd')})`;
}

function buildDialogHeading() {
  return h(
    'div',
    {
      class: 'version-check-dialog-heading',
      style:
        'display:flex;align-items:center;gap:8px;font-weight:600;font-size:16px;color:var(--el-text-color-primary);margin:0 0 14px;padding:0;'
    },
    [h(ElIcon, { size: 22, color: 'var(--el-color-info)' }, { default: () => h(InfoFilled) }), h('span', null, t('versionCheck.title'))]
  );
}

// 版本检查主流程（单飞：进行中不重入；弹窗展示中不重复弹）
export function startVersionCheck() {
  if (typeof window === 'undefined') return () => {};
  let stopped = false;
  let checking = false;
  let dialogOpen = false;

  async function check() {
    if (stopped || checking || dialogOpen) return;
    if (document.visibilityState !== 'visible') return;
    checking = true;
    try {
      const url = `${ensureTrailingSlash(BASE_URL)}version.json?_ts=${Date.now()}`;
      const res = await fetch(url, { cache: 'no-store' });
      if (!res.ok) return;
      const payload = await res.json();
      if (!shouldPrompt(payload)) {
        clearReloadAck();
        return;
      }
      if (payload.promptUpdate === false || reloadAckMatches(payload)) return;
      dialogOpen = true;

      const currentVersion = APP_VERSION.trim();
      const latestVersion = (payload.version || '-').trim() || '-';
      const labelStyle = 'color:var(--el-text-color-secondary);margin-right:6px;';
      const valueStyle = 'font-weight:600;color:var(--el-text-color-primary);margin-right:10px;';
      const timeStyle = 'font-variant-numeric:tabular-nums;color:var(--el-text-color-regular);';
      const rowStyle = 'display:flex;flex-wrap:wrap;align-items:baseline;margin-bottom:12px;font-size:14px;line-height:1.6;';
      const contentLines = parseContentLines(payload.updateContent);

      const message = h(
        'div',
        { class: 'version-check-dialog-msg', style: 'text-align:left;padding:2px 0 4px;' },
        [
          buildDialogHeading(),
          h(
            'p',
            { style: 'margin:0 0 14px;color:var(--el-text-color-regular);font-size:14px;line-height:1.55;' },
            t('versionCheck.intro')
          ),
          h('div', { style: rowStyle }, [
            h('span', { style: labelStyle }, t('versionCheck.currentVersionLabel')),
            h('span', { style: valueStyle }, currentVersion),
            h('span', { style: timeStyle }, formatBuildTime(APP_BUILD_TIME))
          ]),
          h('div', { style: rowStyle }, [
            h('span', { style: labelStyle }, t('versionCheck.latestVersionLabel')),
            h('span', { style: valueStyle }, latestVersion),
            h('span', { style: timeStyle }, formatBuildTime(payload.buildTime))
          ]),
          ...(contentLines.length > 0
            ? [
                h('div', { style: 'margin:2px 0 12px;' }, [
                  h(
                    'div',
                    { style: 'margin:0 0 6px;color:var(--el-text-color-secondary);font-size:13px;font-weight:600;' },
                    t('versionCheck.updateContentHeading')
                  ),
                  h(
                    'ul',
                    {
                      style:
                        'margin:0;padding-left:18px;color:var(--el-text-color-regular);font-size:13px;line-height:1.65;'
                    },
                    contentLines.map((line) => h('li', { style: 'margin:0;' }, line))
                  )
                ])
              ]
            : []),
          h(
            'div',
            { style: 'margin:4px 0 0;color:var(--el-text-color-secondary);font-size:13px;line-height:1.55;' },
            [
              h('p', { style: 'margin:0;' }, t('versionCheck.refreshHintLine1')),
              h('p', { style: 'margin:8px 0 0;' }, t('versionCheck.refreshHintLine2'))
            ]
          )
        ]
      );

      await ElMessageBox.confirm(message, {
        title: undefined,
        customClass: 'version-check-message-box',
        confirmButtonText: t('versionCheck.confirmRefresh'),
        cancelButtonText: t('versionCheck.cancelLater'),
        distinguishCancelAndClose: true,
        showClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        closeOnHashChange: false
      });
      writeReloadAck(payload);
      window.location.reload();
    } catch {
      // 静默：网络失败 / 用户取消 / 关闭弹窗
    } finally {
      dialogOpen = false;
      checking = false;
    }
  }

  const intervalId = window.setInterval(() => {
    check();
  }, CHECK_INTERVAL_MS);
  const onVisible = () => {
    if (document.visibilityState === 'visible') check();
  };
  document.addEventListener('visibilitychange', onVisible);
  check();

  return () => {
    stopped = true;
    window.clearInterval(intervalId);
    document.removeEventListener('visibilitychange', onVisible);
  };
}
