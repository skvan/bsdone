<template>
  <div
    ref="splitterRef"
    class="live-pbp-splitter"
    role="separator"
    :aria-orientation="sideDock ? 'vertical' : 'horizontal'"
    :aria-valuemin="minSize"
    :aria-valuemax="maxSize"
    :aria-valuenow="size"
    :aria-label="sideDock ? '调整日志宽度' : '调整日志高度'"
    :title="sideDock ? '左右拖动调整日志宽度，双击复位' : '上下拖动调整日志高度，双击复位'"
    tabindex="0"
    @pointerdown="startDrag"
    @dblclick="resetSize"
    @keydown="onKeydown"
  >
    <span class="live-pbp-splitter__grip" aria-hidden="true"></span>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  PBP_HORIZONTAL_MIN,
  PBP_VERTICAL_MIN,
  clampPbpSize,
  defaultPbpSize,
  isHorizontalDock,
  persistPbpSize,
  readLegacyPbpHeight,
  readPbpSize,
} from '../../utils/liveGamePbpDock.js';

const props = defineProps({
  dock: { type: String, default: 'bottom' },
});

const splitterRef = ref(null);
const parentWidth = ref(0);
const parentHeight = ref(0);
const sideDock = computed(() => isHorizontalDock(props.dock));
const minSize = computed(() => (sideDock.value ? PBP_HORIZONTAL_MIN : PBP_VERTICAL_MIN));
const maxSize = computed(() => clampPbpSize(props.dock, Number.MAX_SAFE_INTEGER, dimensions()));
let parent = null;

function initialSize(dock) {
  if (typeof window === 'undefined') return 220;
  const width = window.innerWidth;
  const height = window.innerHeight;
  return readPbpSize(dock) || defaultPbpSize(dock, {
    width,
    height,
    legacyHeight: readLegacyPbpHeight(undefined, height),
  });
}

const size = ref(initialSize(props.dock));

function dimensions() {
  return {
    width: parentWidth.value,
    height: parentHeight.value,
  };
}

function measureParent() {
  if (!parent) return;
  parentWidth.value = parent.clientWidth;
  parentHeight.value = parent.clientHeight;
}

function applySize() {
  if (!parent) return;
  parent.dataset.dock = props.dock;
  parent.style.setProperty('--pbp-size', `${size.value}px`);
}

/** 尺寸只经此处落地：夹取到父级可视范围后写回 CSS 变量。 */
function resizeTo(next) {
  size.value = clampPbpSize(props.dock, next, dimensions());
  applySize();
}

function clampCurrent() {
  if (parent) resizeTo(size.value);
}

function persistCurrent() {
  persistPbpSize(props.dock, size.value);
}

function resetSize() {
  resizeTo(defaultPbpSize(props.dock, dimensions()));
  persistCurrent();
}

let drag = null;

function startDrag(event) {
  if (event.button !== 0) return;
  event.preventDefault();
  try {
    splitterRef.value?.setPointerCapture?.(event.pointerId);
  } catch {
    // 老浏览器或合成事件没有指针捕获，不影响拖动
  }
  drag = { dock: props.dock, x: event.clientX, y: event.clientY, size: size.value };
  document.body.classList.add('lg-pbp-dock-resizing');
  if (sideDock.value) document.body.classList.add('lg-pbp-dock-resizing--col');
  window.addEventListener('pointermove', onDragMove);
  window.addEventListener('pointerup', stopDrag);
  window.addEventListener('pointercancel', stopDrag);
}

function onDragMove(event) {
  if (!drag) return;
  event.preventDefault();
  // 分界线远离所贴的边即放大：下方/右侧取反，上方/左侧取正。
  let moved = 0;
  if (drag.dock === 'bottom') moved = drag.y - event.clientY;
  else if (drag.dock === 'top') moved = event.clientY - drag.y;
  else if (drag.dock === 'left') moved = event.clientX - drag.x;
  else if (drag.dock === 'right') moved = drag.x - event.clientX;
  resizeTo(drag.size + moved);
}

function stopDrag() {
  if (!drag) return;
  drag = null;
  document.body.classList.remove('lg-pbp-dock-resizing', 'lg-pbp-dock-resizing--col');
  window.removeEventListener('pointermove', onDragMove);
  window.removeEventListener('pointerup', stopDrag);
  window.removeEventListener('pointercancel', stopDrag);
  persistCurrent();
}

function onKeydown(event) {
  if (event.key === 'Home') {
    event.preventDefault();
    resetSize();
    return;
  }
  const arrowKeys = sideDock.value ? ['ArrowLeft', 'ArrowRight'] : ['ArrowUp', 'ArrowDown'];
  if (!arrowKeys.includes(event.key)) return;
  event.preventDefault();
  const step = event.shiftKey ? 48 : 16;
  const pressingForward = event.key === 'ArrowDown' || event.key === 'ArrowRight';
  const dockOnEndEdge = props.dock === 'bottom' || props.dock === 'right';
  // 贴在下方/右侧时，向端点方向按键缩小日志；贴上/左侧时相反。
  const delta = dockOnEndEdge === pressingForward ? -step : step;
  resizeTo(size.value + delta);
  persistCurrent();
}

let observer = null;
onMounted(() => {
  parent = splitterRef.value?.parentElement ?? null;
  measureParent();
  clampCurrent();
  if (typeof ResizeObserver !== 'undefined' && parent) {
    observer = new ResizeObserver(() => {
      measureParent();
      clampCurrent();
    });
    observer.observe(parent);
  }
});
onBeforeUnmount(() => {
  observer?.disconnect();
  stopDrag();
});
watch(() => props.dock, (dock) => {
  if (!parent) return;
  size.value = initialSize(dock);
  applySize();
  clampCurrent();
});
</script>
