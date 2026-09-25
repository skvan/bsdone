<template>
  <div class="admin-tags-view" @contextmenu.prevent>
    <button
      v-show="canScroll"
      type="button"
      class="tags-scroll-btn tags-scroll-btn--prev"
      :disabled="!canScrollPrev"
      title="向左查看标签"
      aria-label="向左查看标签"
      @click="scrollByDir(-1)"
      @contextmenu.prevent
    >
      <el-icon><ArrowLeft /></el-icon>
    </button>
    <div ref="tagsWrapRef" class="tags-wrap" @wheel="onWheel" @contextmenu.prevent>
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="tag-item"
        :class="{ active: samePath(route.path, tab.path), 'tag-item--no-drag': isDashboardPath(tab.path) }"
        @contextmenu.prevent="openContextMenu($event, tab.path)"
      >
        <span class="tag-title">{{ tab.title }}</span>
        <template v-if="!isDashboardPath(tab.path)">
          <el-icon v-if="tab.pinned" class="tag-pin-lock" title="已固定" aria-label="已固定" @click.stop.prevent>
            <Lock />
          </el-icon>
          <el-icon v-else class="close" @click.prevent="closeTab(tab.path)">
            <Close />
          </el-icon>
        </template>
      </router-link>
    </div>
    <button
      v-show="canScroll"
      type="button"
      class="tags-scroll-btn tags-scroll-btn--next"
      :disabled="!canScrollNext"
      title="向右查看标签"
      aria-label="向右查看标签"
      @click="scrollByDir(1)"
      @contextmenu.prevent
    >
      <el-icon><ArrowRight /></el-icon>
    </button>
    <ul
      v-show="contextMenuVisible && contextMenuHasAction"
      ref="contextMenuRef"
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
      @contextmenu.prevent
    >
      <li v-if="canPin" @click="onContextAction('pin')">
        <el-icon class="menu-icon"><Lock /></el-icon><span>固定</span>
      </li>
      <li v-if="canUnpin" @click="onContextAction('unpin')">
        <el-icon class="menu-icon"><Unlock /></el-icon><span>取消固定</span>
      </li>
      <li v-if="contextMenuSeparator" key="sep" class="context-menu-sep" aria-hidden="true"></li>
      <li v-if="canCloseCurrent" @click="onContextAction('closeCurrent')">
        <el-icon class="menu-icon"><Close /></el-icon><span>关闭当前</span>
      </li>
      <li v-if="canCloseOther" @click="onContextAction('closeOther')">
        <el-icon class="menu-icon"><CircleClose /></el-icon><span>关闭其它</span>
      </li>
      <li v-if="canCloseLeft" @click="onContextAction('closeLeft')">
        <el-icon class="menu-icon"><DArrowLeft /></el-icon><span>关闭左侧</span>
      </li>
      <li v-if="canCloseRight" @click="onContextAction('closeRight')">
        <el-icon class="menu-icon"><DArrowRight /></el-icon><span>关闭右侧</span>
      </li>
      <li v-if="canCloseAll" @click="onContextAction('closeAll')">
        <el-icon class="menu-icon"><RemoveFilled /></el-icon><span>关闭所有</span>
      </li>
    </ul>
  </div>
</template>

<script>
// 多标签栏 —— 行为移植自编译产物 AdminLayout chunk（AdminTagsView）
export default {
  __scopeId: 'data-v-57ab9565'
};
</script>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Sortable from 'sortablejs';
import { ArrowLeft, ArrowRight, CircleClose, Close, DArrowLeft, DArrowRight, Lock, RemoveFilled, Unlock } from '@element-plus/icons-vue';
import { isDashboardPath, isLiveTabRoute, samePath, useTabsStore } from '../../stores/tabs';
import { useMediaQuery } from '../../composables/useMediaQuery';
import { DEFAULT_TENANT_CODE } from '../../utils/tenantRoute';

const MOBILE_DRAG_DELAY = 400;
const SCROLL_THRESHOLD = 2;

const route = useRoute();
const router = useRouter();
const tabsStore = useTabsStore();

const tabs = computed(() => tabsStore.tabs);
const tagsWrapRef = ref(null);
const contextMenuRef = ref(null);
const canScroll = ref(false);
const canScrollPrev = ref(false);
const canScrollNext = ref(false);
let resizeObserver = null;
const isMobile = useMediaQuery('(max-width: 768px)');
let sortable = null;

const contextMenuVisible = ref(false);
const contextMenuX = ref(0);
const contextMenuY = ref(0);
const contextPath = ref('');

const contextIndex = computed(() => {
  const path = contextPath.value;
  return tabs.value.findIndex((tab) => samePath(tab.path, path));
});
const contextTab = computed(() => tabs.value.find((tab) => samePath(tab.path, contextPath.value)));

const canPin = computed(() => !isDashboardPath(contextPath.value) && contextTab.value != null && !contextTab.value.pinned);
const canUnpin = computed(() => !isDashboardPath(contextPath.value) && contextTab.value != null && !!contextTab.value.pinned);
const canCloseCurrent = computed(() => !isDashboardPath(contextPath.value) && contextTab.value != null && !contextTab.value.pinned);
const canCloseLeft = computed(() => {
  if (contextTab.value?.pinned) return false;
  const index = contextIndex.value;
  return index >= 0 && index > 0;
});
const canCloseRight = computed(() => {
  if (contextTab.value?.pinned) return false;
  const index = contextIndex.value;
  return index >= 0 && index < tabs.value.length - 1;
});
const canCloseOther = computed(() => (contextTab.value?.pinned ? false : tabs.value.length > 1));
const canCloseAll = computed(() => (contextTab.value?.pinned ? false : tabs.value.length > 1));
const contextMenuHasAction = computed(
  () => canCloseCurrent.value || canCloseLeft.value || canCloseRight.value || canCloseOther.value || canCloseAll.value
);
const contextMenuSeparator = computed(() => (canPin.value || canUnpin.value) && contextMenuHasAction.value);

// ---------- 横向滚动 ----------
function updateScrollState() {
  const el = tagsWrapRef.value;
  if (!el) {
    canScroll.value = false;
    canScrollPrev.value = false;
    canScrollNext.value = false;
    return;
  }
  const { scrollLeft, scrollWidth, clientWidth } = el;
  canScroll.value = scrollWidth > clientWidth + SCROLL_THRESHOLD;
  canScrollPrev.value = scrollLeft > SCROLL_THRESHOLD;
  canScrollNext.value = scrollLeft + clientWidth < scrollWidth - SCROLL_THRESHOLD;
}

function scrollByDir(direction) {
  const el = tagsWrapRef.value;
  if (!el) return;
  const step = Math.max(120, Math.floor(el.clientWidth * 0.55));
  el.scrollBy({ left: direction * step, behavior: 'smooth' });
}

function onWheel(event) {
  const el = tagsWrapRef.value;
  if (!el || !canScroll.value) return;
  const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY;
  if (delta === 0) return;
  event.preventDefault();
  el.scrollBy({ left: delta, behavior: 'auto' });
  updateScrollState();
}

function scrollActiveIntoView() {
  const el = tagsWrapRef.value;
  if (!el) return;
  const active = el.querySelector('.tag-item.active');
  if (!active) return;
  active.scrollIntoView({ inline: 'nearest', block: 'nearest' });
  updateScrollState();
}

// ---------- 关闭页签 ----------
async function closeTab(path) {
  const tab = tabsStore.tabs.find((t) => samePath(t.path, path));
  if (tab?.pinned) return;
  const isActive = samePath(route.path, path);
  if (isActive && tabsStore.leaveConfirmHandler && !(await tabsStore.leaveConfirmHandler())) return;
  const others = tabsStore.tabs.filter((t) => !samePath(t.path, path));
  if (isActive) {
    const dashboard = `/${String(route.params.tenantCode || DEFAULT_TENANT_CODE)}/admin/dashboard`;
    let target;
    if (tab && isLiveTabRoute(tab)) {
      target = [...others].reverse().find((t) => !isLiveTabRoute(t))?.path ?? dashboard;
    } else {
      target = others.length ? others[others.length - 1].path : dashboard;
    }
    await router.push(target);
  }
  tabsStore.removeTab(path);
}

// ---------- 右键菜单 ----------
function openContextMenu(event, path) {
  contextPath.value = path;
  contextMenuX.value = event.clientX;
  contextMenuY.value = event.clientY;
  contextMenuVisible.value = true;
  nextTick(() => {
    const el = contextMenuRef.value;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const margin = 8;
    let x = contextMenuX.value;
    let y = contextMenuY.value;
    if (x + rect.width + margin > window.innerWidth) x = window.innerWidth - rect.width - margin;
    if (y + rect.height + margin > window.innerHeight) y = window.innerHeight - rect.height - margin;
    if (x < margin) x = margin;
    if (y < margin) y = margin;
    contextMenuX.value = x;
    contextMenuY.value = y;
  });
}

function closeContextMenu() {
  contextMenuVisible.value = false;
}

async function confirmLeave() {
  const handler = tabsStore.leaveConfirmHandler;
  return handler ? await handler() : true;
}

async function onContextAction(action) {
  const path = contextPath.value;
  closeContextMenu();
  if (action === 'pin') {
    tabsStore.pinTab(path);
    return;
  }
  if (action === 'unpin') {
    tabsStore.unpinTab(path);
    return;
  }
  if (action === 'closeCurrent') {
    await closeTab(path);
    return;
  }
  if (action === 'closeOther') {
    if (!samePath(route.path, path) && !(await confirmLeave())) return;
    tabsStore.closeOther(path);
    if (!samePath(route.path, path)) await router.push(path);
    return;
  }
  if (action === 'closeLeft') {
    const list = tabsStore.tabs;
    const targetIndex = list.findIndex((t) => samePath(t.path, path));
    const currentIndex = list.findIndex((t) => samePath(t.path, route.path));
    if (targetIndex > 0 && currentIndex >= 0 && currentIndex < targetIndex && !(await confirmLeave())) return;
    tabsStore.closeLeft(path);
    if (currentIndex >= 0 && currentIndex < targetIndex) await router.push(path);
    return;
  }
  if (action === 'closeRight') {
    const list = tabsStore.tabs;
    const targetIndex = list.findIndex((t) => samePath(t.path, path));
    const currentIndex = list.findIndex((t) => samePath(t.path, route.path));
    if (targetIndex >= 0 && currentIndex > targetIndex && !(await confirmLeave())) return;
    tabsStore.closeRight(path);
    if (currentIndex > targetIndex) await router.push(path);
    return;
  }
  if (action === 'closeAll') {
    if (!(await confirmLeave())) return;
    tabsStore.closeAll();
    const first = tabsStore.tabs[0]?.path || `/${String(route.params.tenantCode || DEFAULT_TENANT_CODE)}/admin/dashboard`;
    await router.push(first);
  }
}

// ---------- 拖拽排序 ----------
function canDrop(event) {
  const list = [...tabsStore.tabs];
  const el = tagsWrapRef.value;
  if (!el || !list.length) return true;
  const items = [...el.querySelectorAll('.tag-item')];
  const dragged = event.dragged;
  const fromIndex = dragged ? items.indexOf(dragged) : -1;
  if (fromIndex < 0 || fromIndex >= list.length) return true;
  const draggedTab = list[fromIndex];
  if (!draggedTab?.path || isDashboardPath(draggedTab.path)) return false;
  const related = event.related;
  if (!related?.classList.contains('tag-item')) return true;
  const toIndex = items.indexOf(related);
  if (toIndex < 0) return true;
  const targetTab = list[toIndex];
  if (!targetTab) return true;
  const draggedPinned = !!draggedTab.pinned;
  const targetPinned = !isDashboardPath(targetTab.path) && !!targetTab.pinned;
  const targetIsDashboard = isDashboardPath(targetTab.path);
  const after = !!event.willInsertAfter;
  return draggedPinned
    ? !((targetIsDashboard && !after) || (!targetIsDashboard && !targetPinned && after))
    : !((targetIsDashboard && !after) || (targetPinned && !after));
}

function initSortable() {
  sortable?.destroy();
  sortable = null;
  const el = tagsWrapRef.value;
  if (!el) return;
  sortable = Sortable.create(el, {
    animation: 150,
    handle: '.tag-title',
    filter: '.tag-item--no-drag',
    ghostClass: 'tag-dragging',
    chosenClass: 'tag-chosen',
    delay: isMobile.value ? MOBILE_DRAG_DELAY : 0,
    delayOnTouchOnly: false,
    fallbackOnBody: true,
    swapThreshold: 0.65,
    onStart() {
      closeContextMenu();
    },
    onMove(event) {
      if (event.related?.classList?.contains('tag-item--no-drag') && !event.willInsertAfter) return false;
      return canDrop(event);
    },
    onEnd(event) {
      const { oldIndex, newIndex } = event;
      if (oldIndex != null && newIndex != null && oldIndex !== newIndex) {
        tabsStore.reorderTabs(oldIndex, newIndex);
      }
    }
  });
}

function bindScroll() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  const el = tagsWrapRef.value;
  if (!el) return;
  el.removeEventListener('scroll', updateScrollState);
  el.addEventListener('scroll', updateScrollState, { passive: true });
  resizeObserver = new ResizeObserver(() => updateScrollState());
  resizeObserver.observe(el);
  updateScrollState();
}

onMounted(() => {
  document.addEventListener('click', closeContextMenu);
  nextTick(() => {
    initSortable();
    bindScroll();
    scrollActiveIntoView();
  });
});

watch(isMobile, () => {
  nextTick(() => {
    initSortable();
    bindScroll();
  });
});

watch(
  () => route.path,
  () => {
    nextTick(() => scrollActiveIntoView());
  }
);

watch(
  () => tabs.value.length,
  () => {
    nextTick(() => {
      updateScrollState();
      scrollActiveIntoView();
    });
  }
);

onBeforeUnmount(() => {
  document.removeEventListener('click', closeContextMenu);
  sortable?.destroy();
  sortable = null;
  tagsWrapRef.value?.removeEventListener('scroll', updateScrollState);
  resizeObserver?.disconnect();
  resizeObserver = null;
});
</script>
