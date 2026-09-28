// 响应式日期格式化（VueUse useDateFormat）——行为保真移植自编译产物 element chunk 的 rj
// rj 正文：computed(() => format(toValue(date), toValue(format), options))；format 即 dayjs 格式化语义
// 依赖映射：k→computed、Vt→toValue、$7→ensureDayjs、N7→dayjs format
import { computed, toValue } from 'vue';
import { loadDayjsCjs } from '../utils/dateExtras';

export function useDateFormat(date, format = 'HH:mm:ss', options = {}) {
  return computed(() => {
    const dayjs = loadDayjsCjs();
    const value = toValue(date);
    if (value == null || value === '') return '';
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.format(toValue(format)) : '';
  });
}
