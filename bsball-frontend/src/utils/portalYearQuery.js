// 门户年份查询 —— 行为移植自编译产物 portalYearQuery chunk（逐字）
export function parseYearList(value) {
  if (value == null || value === '') return null;
  const parts = (Array.isArray(value) ? value[0] : String(value))
    .split(/[,，]/)
    .map((token) => parseInt(String(token).trim(), 10))
    .filter((num) => !Number.isNaN(num));
  return parts.length ? parts : null;
}

export function getDefaultYearQuery() {
  return { years: String(new Date().getFullYear()) };
}

export function deriveYearsFromDateRange(item) {
  const years = new Set();
  if (item.startDate) {
    const year = parseInt(item.startDate.slice(0, 4), 10);
    if (!Number.isNaN(year)) years.add(year);
  }
  if (item.endDate) {
    const year = parseInt(item.endDate.slice(0, 4), 10);
    if (!Number.isNaN(year)) years.add(year);
  }
  if (years.size === 0) return [];
  const min = Math.min(...years);
  const max = Math.max(...years);
  const list = [];
  for (let year = min; year <= max; year++) list.push(year);
  return list;
}

export function resolveYearQuery(item) {
  const years = deriveYearsFromDateRange(item);
  return years.length === 0 ? getDefaultYearQuery() : { years: years.join(',') };
}
