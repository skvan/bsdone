// 响应式当前时间（VueUse useNow）——行为保真移植自编译产物 element chunk 的 sj/H7/O7/Fv
// 依赖映射：Lt→shallowRef、Vt→toValue、jt→isRef、Jr→tryOnScopeDispose、Gs→readonly、de→watch、vt→isClient
import { shallowRef, toValue, isRef, readonly, watch, onScopeDispose } from 'vue';

const isClient = typeof window !== 'undefined';

function tryOnScopeDispose(fn) {
  onScopeDispose(fn);
}

// O7 → useIntervalFn
function useIntervalFn(callback, interval = 1000, options = {}) {
  const { immediate = true, immediateCallback = false } = options;
  let timer = null;
  const isActive = shallowRef(false);
  function clean() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }
  function pause() {
    isActive.value = false;
    clean();
  }
  function resume() {
    const ms = toValue(interval);
    if (ms <= 0) return;
    isActive.value = true;
    if (immediateCallback) callback();
    clean();
    if (isActive.value) timer = setInterval(callback, ms);
  }
  if (immediate && isClient) resume();
  if (isRef(interval) || typeof interval === 'function') {
    tryOnScopeDispose(
      watch(interval, () => {
        if (isActive.value && isClient) resume();
      })
    );
  }
  tryOnScopeDispose(pause);
  return { isActive: readonly(isActive), pause, resume };
}

// Fv → useRafFn
function useRafFn(fn, options = {}) {
  const { immediate = true } = options;
  let rafId = null;
  const isActive = shallowRef(false);
  function loop() {
    fn();
    if (isActive.value) rafId = requestAnimationFrame(loop);
  }
  function resume() {
    if (!isActive.value) {
      isActive.value = true;
      rafId = requestAnimationFrame(loop);
    }
  }
  function pause() {
    isActive.value = false;
    if (rafId != null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
  }
  if (immediate && isClient) resume();
  tryOnScopeDispose(pause);
  return { isActive: readonly(isActive), pause, resume };
}

// H7 → 调度器选择
function resolveScheduler(options) {
  if ('interval' in options || 'immediate' in options) {
    const { interval = 'requestAnimationFrame', immediate = true } = options;
    return interval === 'requestAnimationFrame'
      ? (fn) => useRafFn(fn, { immediate })
      : (fn) => useIntervalFn(fn, interval, options);
  }
  return useRafFn;
}

// sj → useNow
export function useNow(options = {}) {
  const { controls = false } = options;
  const scheduler = resolveScheduler(options);
  const now = shallowRef(new Date());
  const { pause, resume } = scheduler(() => {
    now.value = new Date();
  });
  return controls ? { now, pause, resume } : now;
}
