// 阵容板画布自适应：实测球场顶部到视口底部的可用高度与容器宽度，
// 计算球场列宽上限（CSS 变量下发），并按需切换设置区压缩形态（纯计算见 utils/lineupCanvasFit.js）
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { computeFieldCap, shouldCompactForm } from '../utils/lineupCanvasFit';

/**
 * @param {{
 *   boardEl: import('vue').Ref,
 *   pendingHintEl: import('vue').Ref,
 *   isMobile: import('vue').Ref<boolean>,
 * }} deps
 */
export function useLineupCanvasFit({ boardEl, pendingHintEl, isMobile }) {
  const formCompact = ref(false);
  const fieldCap = ref(null); // 球场列宽上限（px）；null = 尚未测量，CSS calc 兜底
  let formDelta = 0;          // 「未压缩」与「压缩」表单下球场顶部的差值（首次压缩时实测）
  let rafId = 0;
  let measuring = false;

  // 提示条高度含外边距（其上下留白也占用球场上方空间）
  function hintBlockHeight(el) {
    const cs = getComputedStyle(el);
    return el.offsetHeight + (parseFloat(cs.marginTop) || 0) + (parseFloat(cs.marginBottom) || 0);
  }

  // 顶部一律取「滚动容器内容坐标」：admin-main 自身滚动时（如滚到替补区再选人）结果不漂移
  function readBoard() {
    const el = boardEl.value;
    if (!el) return null;
    const rect = el.getBoundingClientRect();
    const hintH = pendingHintEl.value ? hintBlockHeight(pendingHintEl.value) : 0;
    const scroller = el.closest('.admin-main');
    if (scroller) {
      const sr = scroller.getBoundingClientRect();
      const top = rect.top - sr.top + scroller.scrollTop;
      return {
        boardTop: top,
        boardTopStable: top - hintH, // 「无提示条」时的顶部：临时提示的显隐不参与压缩判定
        boardWidth: el.clientWidth,
        viewportHeight: scroller.clientHeight,
      };
    }
    const top = rect.top + window.scrollY;
    return { boardTop: top, boardTopStable: top - hintH, boardWidth: el.clientWidth, viewportHeight: window.innerHeight };
  }

  async function measure() {
    if (measuring) return;
    measuring = true;
    try {
      if (isMobile.value) {
        // 移动端单列为另一套布局：不参与桌面压缩与列宽上限
        formCompact.value = false;
        fieldCap.value = null;
        return;
      }
      let info = readBoard();
      if (!info || !info.boardWidth) return;
      // 以「未压缩表单」为基准判定：压缩态用实测差值回推未压缩时的球场顶部（压缩后顶部更靠上）
      const roomyTop = formCompact.value ? info.boardTopStable + formDelta : info.boardTopStable;
      const wantCompact = shouldCompactForm({
        viewportHeight: info.viewportHeight,
        boardTopRoomy: roomyTop,
        boardWidth: info.boardWidth,
        currentlyCompact: formCompact.value,
      });
      if (wantCompact !== formCompact.value) {
        const beforeTop = info.boardTopStable;
        formCompact.value = wantCompact;
        await nextTick();
        const after = readBoard();
        if (after) {
          if (wantCompact) formDelta = beforeTop - after.boardTopStable; // 仅记录「未压缩 → 压缩」的正差值
          info = after;
        }
      }
      fieldCap.value = computeFieldCap({
        viewportHeight: info.viewportHeight,
        boardTop: info.boardTop, // 实际顶部：提示条在场时球场略缩，保持全部卡槽一屏可见
        boardWidth: info.boardWidth,
      });
    } finally {
      measuring = false;
    }
  }

  function scheduleMeasure() {
    if (rafId) return;
    rafId = requestAnimationFrame(() => {
      rafId = 0;
      measure();
    });
  }

  let resizeObserver = null;
  onMounted(() => {
    measure();
    window.addEventListener('resize', scheduleMeasure);
    if (typeof ResizeObserver !== 'undefined' && boardEl.value?.parentElement) {
      resizeObserver = new ResizeObserver(scheduleMeasure);
      resizeObserver.observe(boardEl.value.parentElement);
    }
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', scheduleMeasure);
    resizeObserver?.disconnect();
    resizeObserver = null;
    if (rafId) cancelAnimationFrame(rafId);
  });

  // 提示条位于球场上方的出现/消失、移动端与桌面布局切换，都会改变可用空间
  watch(pendingHintEl, () => nextTick(scheduleMeasure));
  watch(isMobile, () => nextTick(scheduleMeasure));

  return { formCompact, fieldCap };
}
