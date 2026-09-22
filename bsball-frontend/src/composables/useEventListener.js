// DOM 事件监听组合式函数 —— 等价编译产物 Mn()（useEventListener，目标为 ref/computed 时响应式重绑）
import { onBeforeUnmount, unref, watch } from 'vue';

export function useEventListener(target, event, handler, options) {
  let dispose = null;
  const bind = () => {
    if (dispose) {
      dispose();
      dispose = null;
    }
    const el = unref(target);
    if (!el || typeof el.addEventListener !== 'function') return;
    el.addEventListener(event, handler, options);
    dispose = () => el.removeEventListener(event, handler, options);
  };
  watch(() => unref(target), bind, { immediate: true });
  onBeforeUnmount(() => {
    if (dispose) dispose();
  });
}
