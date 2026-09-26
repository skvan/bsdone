// 日期与区域工具补充 —— 行为移植自编译产物入口 chunk（qn / Zn / hr / ct）
// qn: YYYY-MM-DD 格式化；Zn: M.DD 周X 格式化；hr: dayjs zh-cn locale；ct: dayjs CJS 工厂
import dayjs from 'dayjs';
import updateLocale from 'dayjs/plugin/updateLocale';
import 'dayjs/locale/zh-cn';

let dayjsPluginReady = false;

function ensureDayjsPlugins() {
  if (dayjsPluginReady) return;
  dayjs.extend(updateLocale);
  dayjsPluginReady = true;
}

// 入口 chunk qn —— 日期 → 'YYYY-MM-DD'（非法返回 '-'）
export function formatDateYmd(value) {
  if (value == null || value === '') return '-';
  const date = typeof value === 'string' || typeof value === 'number' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

// 入口 chunk Zn —— 'YYYY-MM-DD' → 'M.DD 周X'（非法原样返回，空返回 '-'）
export function formatMonthDayWithWeek(value) {
  const text = (value || '').trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return text || '-';
  const date = new Date(text + 'T12:00:00');
  if (Number.isNaN(date.getTime())) return text;
  const week = '日一二三四五六'[date.getDay()];
  return `${date.getMonth() + 1}.${String(date.getDate()).padStart(2, '0')} 周${week}`;
}

// 入口 chunk hr —— dayjs 简体中文 locale 对象（调用即取）
export function getDayjsZhCnLocale() {
  return dayjs.locale('zh-cn');
}

// 入口 chunk ct(SP) —— dayjs 的 CJS 工厂（产物中 H() 返回 module.exports=dayjs）
// 产物 setup 会 extend(updateLocale 插件) + updateLocale('zh-cn',{weekStart:1}) + locale('zh-cn')；
// 此处预扩展官方插件保证等价（幂等）
export function loadDayjsCjs() {
  ensureDayjsPlugins();
  return dayjs;
}

// 入口 chunk c（gameStatusI18n）—— 比赛状态标签（i18n 优先，回退内置中文）
const GAME_STATUS_ORDER = ['scheduled', 'live', 'final', 'postponed', 'cancelled'];
const GAME_STATUS_FALLBACK = { scheduled: '未开始', live: '进行中', final: '已结束', postponed: '延期', cancelled: '取消' };

function normalizeGameStatus(value) {
  const lower = value.trim().toLowerCase();
  return GAME_STATUS_ORDER.includes(lower) ? lower : value.trim();
}

// t 为 vue-i18n 的 t；条目存在则用 i18n，否则用内置中文
export function formatGameStatusLabel(t, value) {
  if (value == null || String(value).trim() === '') return '—';
  const key = typeof value === 'string' ? normalizeGameStatus(value) : value;
  const i18nKey = `eventGames.status.${key}`;
  const translated = t(i18nKey);
  return translated !== i18nKey ? translated : key in GAME_STATUS_FALLBACK ? GAME_STATUS_FALLBACK[key] : String(value);
}
