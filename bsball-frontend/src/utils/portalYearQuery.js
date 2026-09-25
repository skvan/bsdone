// 门户年度查询参数工具 —— 行为移植自编译产物 portalYearQuery chunk
// 导出：parseYearsParam（o/n）、defaultPortalYearQuery（i/r）、deriveYearsRange（u/t）、buildYearQuery（l/i）

// 解析 years 参数（支持数组或 "2023,2024" / 中文逗号分隔）→ 去重数字数组或 null
export function parseYearsParam(raw) {
  if (raw == null || raw === '') return null;
  const text = Array.isArray(raw) ? raw[0] : String(raw);
  const years = text
    .split(/[,，]/)
    .map((part) => parseInt(String(part).trim(), 10))
    .filter((n) => !Number.isNaN(n));
  return years.length ? years : null;
}

// 默认查询：当前年份
export function defaultPortalYearQuery() {
  return { years: String(new Date().getFullYear()) };
}

// 依据 startDate/endDate 推导年份区间（无有效年份返回 []）
export function deriveYearsRange(params) {
  const years = new Set();
  if (params.startDate) {
    const y = parseInt(params.startDate.slice(0, 4), 10);
    if (!Number.isNaN(y)) years.add(y);
  }
  if (params.endDate) {
    const y = parseInt(params.endDate.slice(0, 4), 10);
    if (!Number.isNaN(y)) years.add(y);
  }
  if (years.size === 0) return [];
  const min = Math.min(...years);
  const max = Math.max(...years);
  const out = [];
  for (let y = min; y <= max; y += 1) out.push(y);
  return out;
}

// 依据日期区间构建查询（空区间回退默认年份）
export function buildYearQuery(params) {
  const range = deriveYearsRange(params);
  return range.length === 0 ? defaultPortalYearQuery() : { years: range.join(',') };
}
