// 窗口尺寸组合式（VueUse useWindowSize）——行为保真移植自编译产物 element chunk 的 j7
// 依赖映射：Lt→shallowRef、Dn→defaultWindow、C4→tryOnMounted（内联）、Bt→useEventListener（本地）、
//   B7→useMediaQuery（本地）、de→watch
import { shallowRef, watch, getCurrentInstance, onMounted } from 'vue';
import { useEventListener } from './useEventListener';
import { useMediaQuery } from './useMediaQuery';

const defaultWindow = typeof window !== 'undefined' ? window : undefined;

function tryOnMounted(fn) {
  if (getCurrentInstance()) onMounted(fn);
  else fn();
}

export function useWindowSize(options = {}) {
  const {
    window: windowTarget = defaultWindow,
    initialWidth = Number.POSITIVE_INFINITY,
    initialHeight = Number.POSITIVE_INFINITY,
    listenOrientation = true,
    includeScrollbar = true,
    type = 'inner'
  } = options;
  const width = shallowRef(initialWidth);
  const height = shallowRef(initialHeight);
  const update = () => {
    if (!windowTarget) return;
    if (type === 'outer') {
      width.value = windowTarget.outerWidth;
      height.value = windowTarget.outerHeight;
    } else if (type === 'visual' && windowTarget.visualViewport) {
      const { width: w, height: h, scale } = windowTarget.visualViewport;
      width.value = Math.round(w * scale);
      height.value = Math.round(h * scale);
    } else if (includeScrollbar) {
      width.value = windowTarget.innerWidth;
      height.value = windowTarget.innerHeight;
    } else {
      width.value = windowTarget.document.documentElement.clientWidth;
      height.value = windowTarget.document.documentElement.clientHeight;
    }
  };
  update();
  tryOnMounted(update);
  const listenerOptions = { passive: true };
  useEventListener(defaultWindow, 'resize', update, listenerOptions);
  if (windowTarget && type === 'visual' && windowTarget.visualViewport) useEventListener(windowTarget.visualViewport, 'resize', update, listenerOptions);
  if (listenOrientation) watch(useMediaQuery('(orientation: portrait)'), () => update());
  return { width, height };
}
