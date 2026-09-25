// 页面滚动锁组合式 —— 行为移植自编译产物 useHtmlScrollLock chunk
// 引用计数式锁定 <html> 滚动（多个弹窗叠加时安全）
import { onBeforeUnmount, watch } from 'vue';

const LOCK_CLASS = 'html-scroll-locked';
let lockCount = 0;

function acquire() {
  lockCount += 1;
  if (lockCount === 1) document.documentElement.classList.add(LOCK_CLASS);
}

function release() {
  if (lockCount <= 0) return;
  lockCount -= 1;
  if (lockCount === 0) document.documentElement.classList.remove(LOCK_CLASS);
}

export function useHtmlScrollLock(lockedFlag) {
  watch(
    lockedFlag,
    (locked) => {
      if (locked) acquire();
      else release();
    },
    { immediate: true }
  );
  onBeforeUnmount(() => {
    if (lockedFlag.value) release();
  });
}
