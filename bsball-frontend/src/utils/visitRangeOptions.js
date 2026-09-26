// 访问统计时间范围预设 —— 行为移植自编译产物 visitRangeOptions chunk
// 提供：预设解析（getRangePreset）/ 范围求界（resolveVisitRange）/ 范围校正（normalizeRange）等
export function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function formatYmd(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function lastNDays(days, base = new Date()) {
  const end = startOfDay(base);
  const start = new Date(end);
  start.setDate(start.getDate() - (Math.max(1, days) - 1));
  return [start, end];
}

function startOfWeek(date) {
  const day = startOfDay(date);
  const offset = (day.getDay() + 6) % 7;
  return new Date(day.getFullYear(), day.getMonth(), day.getDate() - offset);
}

function lastNaturalWeek(base = new Date()) {
  const weekStart = startOfWeek(base);
  const start = new Date(weekStart.getFullYear(), weekStart.getMonth(), weekStart.getDate() - 7);
  const end = new Date(weekStart.getFullYear(), weekStart.getMonth(), weekStart.getDate() - 1);
  return [startOfDay(start), startOfDay(end)];
}

function lastNaturalMonth(base = new Date()) {
  const year = base.getFullYear();
  const month = base.getMonth();
  const start = month === 0 ? new Date(year - 1, 11, 1) : new Date(year, month - 1, 1);
  const end = month === 0 ? new Date(year - 1, 11, 31) : new Date(year, month, 0);
  return [startOfDay(start), startOfDay(end)];
}

export function isSecondHalfOfYear(date = new Date()) {
  return date.getMonth() >= 6;
}

export function getRangePreset(key, base = new Date()) {
  const year = base.getFullYear();
  const month = base.getMonth();
  const today = startOfDay(base);
  if (key === 'today') {
    const value = formatYmd(today);
    return { mode: 'fixed', from: value, to: value, label: '今天' };
  }
  if (key === 'yesterday') {
    const date = new Date(today);
    date.setDate(date.getDate() - 1);
    const value = formatYmd(date);
    return { mode: 'fixed', from: value, to: value, label: '昨天' };
  }
  if (key === 'last_week') {
    const [from, to] = lastNaturalWeek(base);
    return { mode: 'fixed', from: formatYmd(from), to: formatYmd(to), label: '上周（自然周）' };
  }
  if (key === 'last_month') {
    const [from, to] = lastNaturalMonth(base);
    return { mode: 'fixed', from: formatYmd(from), to: formatYmd(to), label: '上个月（自然月）' };
  }
  if (key === 'd7') return { mode: 'days', days: 7, label: '近 7 天' };
  if (key === 'd14') return { mode: 'days', days: 14, label: '近 14 天' };
  if (key === 'd30') return { mode: 'days', days: 30, label: '近 30 天' };
  if (key === 'q_this') {
    const quarterStart = Math.floor(month / 3);
    return { mode: 'fixed', from: formatYmd(new Date(year, quarterStart * 3, 1)), to: formatYmd(today), label: '本季度迄今' };
  }
  if (key === 'q_prev') {
    let quarterStart = Math.floor(month / 3) - 1;
    let targetYear = year;
    if (quarterStart < 0) {
      quarterStart = 3;
      targetYear -= 1;
    }
    const start = new Date(targetYear, quarterStart * 3, 1);
    const lastMonth = quarterStart * 3 + 2;
    const lastDay = new Date(targetYear, lastMonth + 1, 0).getDate();
    const end = new Date(targetYear, lastMonth, lastDay);
    return { mode: 'fixed', from: formatYmd(start), to: formatYmd(end), label: '上一完整季度' };
  }
  if (key === 'h1') {
    const start = new Date(year, 0, 1);
    const h1End = new Date(year, 5, 30);
    const end = today <= h1End ? today : h1End;
    return { mode: 'fixed', from: formatYmd(start), to: formatYmd(end), label: today <= h1End ? '今年上半年迄今' : '今年上半年' };
  }
  if (key === 'h2') {
    const start = new Date(year, 6, 1);
    const h2End = new Date(year, 11, 31);
    const end = today < start ? start : today <= h2End ? today : h2End;
    return { mode: 'fixed', from: formatYmd(start), to: formatYmd(end), label: '今年下半年迄今' };
  }
  if (key === 'ytd') return { mode: 'fixed', from: formatYmd(new Date(year, 0, 1)), to: formatYmd(today), label: '今年迄今' };
  if (key === 'last_year') {
    const start = new Date(year - 1, 0, 1);
    const end = new Date(year - 1, 11, 31);
    return { mode: 'fixed', from: formatYmd(start), to: formatYmd(end), label: '去年全年' };
  }
  return { mode: 'days', days: 14, label: '近 14 天' };
}

// 按预设键求日期界（days 模式随基点滚动；fixed 模式解析为日期）
export function resolveVisitRange(key, base = new Date()) {
  const preset = getRangePreset(key, base);
  return preset.mode === 'days' ? lastNDays(preset.days, base) : [parseYmd(preset.from), parseYmd(preset.to)];
}

function parseYmd(text) {
  const parts = text.split('-').map((part) => Number(part));
  const year = parts[0] ?? 1970;
  const month = parts[1] ?? 1;
  const day = parts[2] ?? 1;
  return startOfDay(new Date(year, month - 1, day));
}

// 校正日期范围（空值回落近 14 天；倒置自动交换）
export function normalizeRange(range) {
  if (!range || range.length !== 2 || !range[0] || !range[1]) {
    const [from, to] = lastNDays(14);
    return { from: formatYmd(from), to: formatYmd(to) };
  }
  let from = startOfDay(range[0]);
  let to = startOfDay(range[1]);
  if (from.getTime() > to.getTime()) {
    const swap = from;
    from = to;
    to = swap;
  }
  return { from: formatYmd(from), to: formatYmd(to) };
}
