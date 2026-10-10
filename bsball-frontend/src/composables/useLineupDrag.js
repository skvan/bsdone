// 拖拽绑定（interactjs）：只负责「手势 → 落点」并回调，不改任何状态、不搬 DOM
// 语义：interactjs 的 drop 事件不会 re-parent 元素；DOM 与状态全由 Vue 掌控
import interact from 'interactjs';
// 注意：本文件要能被 node --test 直接加载（resolveDropTarget 单测）→ 相对导入必须带 .js 扩展名
import { hitTestSlot } from '../utils/lineupFieldPositions.js';

const within = (x, y, r) =>
  r != null && x >= r.left && x <= r.left + r.width && y >= r.top && y <= r.top + r.height;

/** 纯函数：指针落点 → 目标区域（卡槽优先，其次打线/替补/池） */
export function resolveDropTarget(x, y, layout, tolerance = 1.2) {
  const { slotRects = [], battingRect = null, benchRect = null, poolRect = null } = layout ?? {};
  const code = hitTestSlot(x, y, slotRects, tolerance);
  if (code) return { kind: 'slot', code };
  if (within(x, y, battingRect)) return { kind: 'batting' };
  if (within(x, y, benchRect)) return { kind: 'bench' };
  if (within(x, y, poolRect)) return { kind: 'pool' };
  return null;
}

export const DRAG_DELAY_MS = 180;

/**
 * interactjs 绑定。elements 为可拖元素数组；getLayout() 在松手瞬间读取各区域 rect。
 * 长按 delay 用 interactjs 原生 delay 选项（Task 1 spike 实测口径）。
 */
export function useLineupDrag({ onDragStart, onDrop, onCancel } = {}) {
  let interactables = [];
  let lastDragEndAt = 0; // 供「拖拽后浏览器补发 click」的互斥判断（Task 1 spike 实测）

  function bind(elements, getLayout) {
    interactables = (elements ?? []).filter(Boolean).map((el) =>
      interact(el).draggable({
        delay: DRAG_DELAY_MS,
        inertia: false,
        modifiers: [],
        listeners: {
          start(event) {
            event.target.dataset.dragX = '0';
            event.target.dataset.dragY = '0';
            event.target.classList.add('lineup-card--dragging');
            document.body.classList.add('lineup-dragging');
            onDragStart?.(event.target);
          },
          move(event) {
            // 注意：interactjs 的 event.dx/dy 是「相对上一次事件」的增量 → 必须累加（Task 1 spike 实测结论）
            const el = event.target;
            el.dataset.dragX = String(Number(el.dataset.dragX ?? 0) + event.dx);
            el.dataset.dragY = String(Number(el.dataset.dragY ?? 0) + event.dy);
            el.style.transform = `translate(${el.dataset.dragX}px, ${el.dataset.dragY}px)`;
          },
          end(event) {
            event.target.classList.remove('lineup-card--dragging');
            document.body.classList.remove('lineup-dragging');
            lastDragEndAt = Date.now();
            event.target.style.transform = '';
            delete event.target.dataset.dragX;
            delete event.target.dataset.dragY;
            const x = event.clientX ?? event.client?.x ?? 0;
            const y = event.clientY ?? event.client?.y ?? 0;
            const target = resolveDropTarget(x, y, getLayout?.());
            if (target) onDrop?.(event.target, target);
            else onCancel?.(event.target);
          },
          cancel() {
            document.body.classList.remove('lineup-dragging');
            onCancel?.();
          },
        },
      })
    );
  }

  function unbind() {
    interactables.forEach((it) => it.unset());
    interactables = [];
  }

  /** 拖拽结束后的一小段窗口内忽略 click（浏览器会在 mouseup 后补发一次卡片 click） */
  function wasRecentDrag(thresholdMs = 250) {
    return Date.now() - lastDragEndAt < thresholdMs;
  }

  return { bind, unbind, wasRecentDrag, DRAG_DELAY_MS };
}
