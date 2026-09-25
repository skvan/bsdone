// 公告通知 —— 行为移植自编译产物 useAnnouncementNotifications chunk
// 机制：拉取目标端公告（fetchToShow）→ 去重/过期过滤 → ElNotification 逐条弹出（间隔 800ms）
// 已读记账：localStorage 多键（新键 + 兼容键）；关闭广播：跨标签页同步关闭（storage 事件）
import { onMounted, onUnmounted } from 'vue';
import { ElNotification } from 'element-plus';
import { noticeApi } from '../api/system';
import { currentTenantCodeFromUrl } from '../utils/tenantRoute';
import { useAuthStore } from '../stores/auth';

const OLD_NOTICE_MAX_AGE_MS = 10080 * 60 * 1000; // 7 天
const CLOSE_BROADCAST_KEY = 'bsball.notice.sync.close.v1';
const NOTIFICATION_OFFSET = 60;
const SHOWN_STORAGE_PREFIX = 'bsball.notice.shown.v2';
const LEGACY_SHOWN_PREFIX = 'bs-ball-announcement-shown';

// 过期公告只提示最新一条：取 createdAt 最大（同刻取 id 更大）的 id
function pickLatestId(list, now) {
  let latestId = null;
  let latestAt = -Infinity;
  for (const item of list) {
    if (!item.id) continue;
    const at = item.createdAt ? new Date(item.createdAt).getTime() : now;
    if (at > latestAt || (at === latestAt && item.id > (latestId ?? 0))) {
      latestAt = at;
      latestId = item.id;
    }
  }
  return latestId;
}

// 按（标题+内容+秒级创建时间）去重：保留最新一条，并记录该组全部 id
function dedupeNotices(list) {
  const keyOf = (item) => {
    const title = (item.title || '').trim();
    const content = (item.content || '').trim();
    const at = item.createdAt ? new Date(item.createdAt).getTime() : 0;
    return `${title}\u0000${content}\u0000${Math.floor(at / 1000)}`;
  };
  const groups = new Map();
  for (const item of list) {
    if (!item.id) continue;
    const key = keyOf(item);
    let group = groups.get(key);
    if (!group) {
      group = [];
      groups.set(key, group);
    }
    group.push(item);
  }
  const rows = [];
  const idsToMarkWith = new Map();
  for (const [, group] of groups) {
    group.sort((a, b) => {
      const atA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const atB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return atB - atA || (b.id || 0) - (a.id || 0);
    });
    const first = group[0];
    rows.push(first);
    idsToMarkWith.set(first.id, group.map((item) => item.id).filter((id) => id != null));
  }
  rows.sort((a, b) => {
    const atA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
    const atB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
    return atB - atA;
  });
  return { rows, idsToMarkWith };
}

export function useAnnouncementNotifications(target) {
  const auth = useAuthStore();
  let active = true;
  const timers = [];
  const scopeKey = `bsball.notice.shown.v2`;
  const source = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  const notificationsByNoticeId = new Map();
  const closingIds = new Set();

  // 当前作用域（租户码 + 用户租户 id）
  function scopeInfo() {
    const tenantCode = (typeof window !== 'undefined' ? currentTenantCodeFromUrl() : null) || '_global';
    const tenantId = auth.user?.tenantId;
    return {
      tenantCode: tenantCode.trim(),
      tenantId: tenantId == null ? 'anonymous' : String(tenantId)
    };
  }

  function scopeStorageKey() {
    const info = scopeInfo();
    return `${scopeKey}:${target}:tenantCode=${info.tenantCode}:tenantId=${info.tenantId}`;
  }

  // 兼容读取键（_global / c:租户 / id:用户 / 组合）
  function legacyStorageKeys() {
    const info = scopeInfo();
    const variants = new Set();
    variants.add('_global');
    if (info.tenantCode) variants.add(`c:${info.tenantCode}`);
    if (info.tenantId != null && info.tenantId !== 'anonymous') variants.add(`id:${info.tenantId}`);
    if (info.tenantCode && info.tenantId != null && info.tenantId !== 'anonymous') {
      variants.add(`c:${info.tenantCode}|id:${info.tenantId}`);
    }
    return [...variants].map((variant) => `${LEGACY_SHOWN_PREFIX}:${variant}:${target}`);
  }

  // 已读 id 并集（新键 + 兼容键）
  function readShownIds() {
    try {
      const keys = [scopeStorageKey(), ...legacyStorageKeys()];
      const shown = new Set();
      for (const key of keys) {
        const raw = localStorage.getItem(key);
        if (!raw) continue;
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          parsed.forEach((id) => {
            if (Number.isFinite(id)) shown.add(Number(id));
          });
        }
      }
      return shown;
    } catch {
      return new Set();
    }
  }

  // 标记已读（合并写回新键）
  function markShown(ids) {
    if (!ids.length) return;
    const shown = readShownIds();
    ids.forEach((id) => shown.add(id));
    localStorage.setItem(scopeStorageKey(), JSON.stringify([...shown]));
  }

  // 广播关闭（其他标签页同步关闭）
  function broadcastClose(ids) {
    const valid = ids.filter((id) => Number.isFinite(id));
    if (!valid.length) return;
    const payload = { type: 'close', target, ids: valid, source, at: Date.now() };
    localStorage.setItem(CLOSE_BROADCAST_KEY, JSON.stringify(payload));
  }

  // 关闭指定公告的通知实例
  function closeNotifications(ids) {
    const valid = [...new Set(ids.filter((id) => Number.isFinite(id)))];
    valid.forEach((id) => {
      closingIds.add(id);
      try {
        notificationsByNoticeId.get(id)?.close();
      } catch {
        // ignore
      }
    });
    if (valid.length > 0) setTimeout(() => valid.forEach((id) => closingIds.delete(id)), 0);
  }

  function onStorage(event) {
    if (!active || event.key !== CLOSE_BROADCAST_KEY || !event.newValue) return;
    try {
      const payload = JSON.parse(event.newValue);
      if (payload.type !== 'close' || payload.target !== target || payload.source === source) return;
      closeNotifications(Array.isArray(payload.ids) ? payload.ids : []);
    } catch {
      // ignore
    }
  }

  onMounted(async () => {
    window.addEventListener('storage', onStorage);
    try {
      const { list } = await noticeApi.fetchToShow(target);
      if (!active) return;
      const now = Date.now();
      const shown = readShownIds();
      const latestOldId = pickLatestId(list, now);
      const pending = list.filter((item) => {
        if (!item.id) return false;
        if (shown.has(item.id)) return false;
        const at = item.createdAt ? new Date(item.createdAt).getTime() : now;
        if (now - at > OLD_NOTICE_MAX_AGE_MS) return item.id === latestOldId;
        return true;
      });
      const { rows, idsToMarkWith } = dedupeNotices(pending);
      if (rows.length === 0) return;
      rows.forEach((row, index) => {
        const timer = setTimeout(() => {
          if (!active) return;
          try {
            const ids = row.id ? idsToMarkWith.get(row.id) ?? [row.id] : [];
            const notification = ElNotification({
              title: row.title || '系统通知',
              message: row.content || '',
              type: 'info',
              duration: 0,
              position: 'top-right',
              offset: NOTIFICATION_OFFSET,
              appendTo: document.body,
              onClose: () => {
                markShown(ids);
                ids.forEach((id) => notificationsByNoticeId.delete(id));
                if (!ids.some((id) => closingIds.has(id))) broadcastClose(ids);
              }
            });
            ids.forEach((id) => notificationsByNoticeId.set(id, notification));
          } catch {
            // ignore
          }
        }, index * 800);
        timers.push(timer);
      });
    } catch {
      // ignore
    }
  });

  onUnmounted(() => {
    active = false;
    timers.forEach((timer) => clearTimeout(timer));
    window.removeEventListener('storage', onStorage);
    notificationsByNoticeId.clear();
    closingIds.clear();
  });
}
