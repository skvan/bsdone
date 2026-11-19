// Issue #529：Element Plus MessageBox 原生 draggable 仅支持鼠标，
// 将单指触摸事件桥接为原生拖拽依赖的 mouse 事件，复用其位移与视口限制。

function dispatchMouse(target, type, x, y) {
  target.dispatchEvent(new MouseEvent(type, {
    bubbles: true,
    cancelable: true,
    clientX: x,
    clientY: y,
    button: 0,
    buttons: type === 'mouseup' ? 0 : 1
  }));
}

/** 给 MessageBox 根节点补绑触摸拖动；同一节点重复调用只绑一次。 */
export function bindMessageBoxTouchDrag(box) {
  const header = box?.querySelector?.('.el-message-box__header');
  if (!header || box.dataset.lgTouchBound === '1') return;
  box.dataset.lgTouchBound = '1';
  header.style.touchAction = 'none';

  let active = null;
  const onStart = (event) => {
    if (event.touches?.length !== 1) return;
    const target = event.target;
    if (target instanceof Element && target.closest('button, a, input, select, textarea')) return;
    const touch = event.touches[0];
    active = { id: touch.identifier, x: touch.clientX, y: touch.clientY };
    dispatchMouse(header, 'mousedown', touch.clientX, touch.clientY);
  };
  const onMove = (event) => {
    if (!active) return;
    const touch = Array.from(event.touches || []).find((item) => item.identifier === active.id);
    if (!touch) return;
    event.preventDefault();
    active.x = touch.clientX;
    active.y = touch.clientY;
    dispatchMouse(document, 'mousemove', touch.clientX, touch.clientY);
  };
  const onEnd = (event) => {
    if (!active) return;
    const changed = Array.from(event.changedTouches || []);
    const touch = changed.find((item) => item.identifier === active.id);
    if (changed.length && !touch) return;
    const x = touch?.clientX ?? active.x;
    const y = touch?.clientY ?? active.y;
    active = null;
    dispatchMouse(document, 'mouseup', x, y);
  };

  header.addEventListener('touchstart', onStart, { passive: true });
  header.addEventListener('touchmove', onMove, { passive: false });
  header.addEventListener('touchend', onEnd);
  header.addEventListener('touchcancel', onEnd);
}

/** 绑定当前最新打开的 draggable 弹窗（等两帧让弹窗挂载完成）。 */
export function bindLastMessageBoxTouchDrag() {
  requestAnimationFrame(() => requestAnimationFrame(() => {
    const boxes = document.querySelectorAll('.el-message-box.is-draggable');
    const box = boxes[boxes.length - 1];
    if (box) bindMessageBoxTouchDrag(box);
  }));
}
