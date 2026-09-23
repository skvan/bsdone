// 媒体查询组合式函数 —— 与编译产物 It()（useMediaQuery）等价的轻量实现
import { onBeforeUnmount, ref } from 'vue';

export function useMediaQuery(query) {
  const matches = ref(false);
  if (typeof window === 'undefined') return matches;
  const mql = window.matchMedia(query);
  matches.value = mql.matches;
  const onChange = (event) => {
    matches.value = event.matches;
  };
  mql.addEventListener('change', onChange);
  onBeforeUnmount(() => mql.removeEventListener('change', onChange));
  return matches;
}
