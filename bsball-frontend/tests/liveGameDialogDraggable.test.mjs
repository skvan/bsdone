// Issue #529：实时录入 MessageBox 鼠标 + 触屏拖动
// 运行：node --test tests/liveGameDialogDraggable.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { bindLastMessageBoxTouchDrag, bindMessageBoxTouchDrag } from '../src/utils/messageBoxTouchDrag.js';

const liveGame = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('14 个实时录入 MessageBox 入口统一经 LgBox/LgConfirm 接入触屏拖动', () => {
  assert.match(liveGame, /import\s*\{[^}]*bindLastMessageBoxTouchDrag[^}]*\}\s*from\s*'\.\.\/\.\.\/utils\/messageBoxTouchDrag'/);
  assert.match(liveGame, /function LgBox\(/, '缺少对象式 MessageBox 包装');
  assert.match(liveGame, /function LgConfirm\(/, '缺少确认式 MessageBox 包装');

  const wrappers = liveGame.match(/\bLgBox\(|\bLgConfirm\(/g) || [];
  // 2 个包装定义 + 12 个既有入口 + #712 缺口提示/局部定案 2 个入口
  assert.equal(wrappers.length, 16, '应为 2 个包装定义 + 14 个弹窗入口');
  assert.equal((liveGame.match(/bindLastMessageBoxTouchDrag\(\)/g) || []).length, 2, '两个包装都必须调用触摸绑定');
  assert.equal((liveGame.match(/\bal\(\{/g) || []).length, 0, '对象式入口必须走 LgBox');
  assert.equal((liveGame.match(/\bal\.confirm\(/g) || []).length, 1, '确认式入口必须走 LgConfirm');
  assert.match(liveGame, /function LgBox\([\s\S]*?al\(options\)/);
  assert.match(liveGame, /function LgConfirm\([\s\S]*?al\.confirm\(message,title,options\)/);
  assert.equal((liveGame.match(/draggable:!0/g) || []).length, 14, '14 个入口仍需启用原生鼠标拖动');
});

test('触摸拖动实际派发 mousedown/mousemove/mouseup', () => {
  const dom = installFakeDom();
  bindMessageBoxTouchDrag(dom.box);
  assert.equal(dom.header.style.touchAction, 'none', '头部需禁用浏览器手势接管');

  const start = dom.header.listeners.get('touchstart');
  start({ touches: [{ identifier: 1, clientX: 100, clientY: 100 }], target: dom.header });
  assert.equal(dom.header.events.at(-1)?.type, 'mousedown');
  assert.equal(dom.header.events.at(-1)?.clientX, 100);
  assert.equal(dom.header.events.at(-1)?.buttons, 1, '按下态需与真实鼠标一致');

  let prevented = false;
  const move = dom.header.listeners.get('touchmove');
  move({
    touches: [{ identifier: 1, clientX: 130, clientY: 120 }],
    preventDefault() {
      prevented = true;
    }
  });
  assert.equal(prevented, true, '拖动时必须阻止页面滚动');
  assert.equal(dom.document.events.at(-1)?.type, 'mousemove');
  assert.equal(dom.document.events.at(-1)?.clientX, 130);

  const end = dom.header.listeners.get('touchend');
  end({ changedTouches: [{ identifier: 1, clientX: 140, clientY: 125 }] });
  assert.equal(dom.document.events.at(-1)?.type, 'mouseup');
  assert.equal(dom.document.events.at(-1)?.clientY, 125);
  assert.equal(dom.document.events.at(-1)?.buttons, 0);
});

test('按钮上的触摸不触发拖拽；多指抬起不误结束，touchcancel 正常收尾', () => {
  const dom = installFakeDom();
  bindMessageBoxTouchDrag(dom.box);

  const start = dom.header.listeners.get('touchstart');
  const button = dom.element();
  let closestSelector = '';
  button.closest = (selector) => {
    closestSelector = selector;
    return button;
  };
  start({ touches: [{ identifier: 1, clientX: 10, clientY: 10 }], target: button });
  assert.equal(closestSelector, 'button, a, input, select, textarea', '跳过区域的选择器必须完整');
  assert.equal(dom.header.events.length, 0, '按钮区域不应触发拖动');

  start({ touches: [{ identifier: 1, clientX: 20, clientY: 20 }], target: dom.header });
  const end = dom.header.listeners.get('touchend');
  end({ changedTouches: [{ identifier: 2, clientX: 30, clientY: 30 }] });
  assert.equal(dom.document.events.length, 0, '其他触点抬起时不应结束当前拖动');

  const cancel = dom.header.listeners.get('touchcancel');
  cancel({ changedTouches: [{ identifier: 1, clientX: 25, clientY: 22 }] });
  assert.equal(dom.document.events.at(-1)?.type, 'mouseup', 'touchcancel 也必须结束拖动');
  assert.equal(dom.document.events.at(-1)?.clientX, 25);
});

test('bindLastMessageBoxTouchDrag 等两帧挂载后只绑定最新弹窗', () => {
  const dom = installFakeDom();
  dom.boxes.length = 0;
  bindLastMessageBoxTouchDrag();
  assert.equal(dom.box.dataset.lgTouchBound, undefined, '调用后不应同步绑定');
  dom.flushFrame();
  assert.equal(dom.box.dataset.lgTouchBound, undefined, '第一帧时弹窗可能尚未挂载');
  dom.boxes.push(dom.olderBox, dom.box);
  dom.flushFrame();
  assert.equal(dom.box.dataset.lgTouchBound, '1');
  assert.equal(dom.olderBox.dataset.lgTouchBound, undefined, '旧弹窗不应被绑定');
  assert.equal(dom.olderHeader.style.touchAction, undefined, '旧弹窗不应被改写样式');
});

function installFakeDom() {
  class FakeMouseEvent {
    constructor(type, init) {
      this.type = type;
      this.clientX = init.clientX;
      this.clientY = init.clientY;
      this.buttons = init.buttons;
    }
  }
  class FakeElement {
    constructor() {
      this.listeners = new Map();
      this.style = {};
      this.dataset = {};
      this.events = [];
    }
    addEventListener(type, fn) {
      this.listeners.set(type, fn);
    }
    removeEventListener(type) {
      this.listeners.delete(type);
    }
    dispatchEvent(event) {
      this.events.push(event);
      return true;
    }
    closest() {
      return null;
    }
  }

  const createBox = () => {
    const header = new FakeElement();
    const box = new FakeElement();
    box.querySelector = (selector) => (selector === '.el-message-box__header' ? header : null);
    return { box, header };
  };
  const older = createBox();
  const latest = createBox();
  const boxes = [older.box, latest.box];
  const doc = new FakeElement();
  doc.querySelectorAll = (selector) => (selector === '.el-message-box.is-draggable' ? boxes : []);

  const rafQueue = [];
  global.MouseEvent = FakeMouseEvent;
  global.Element = FakeElement;
  global.document = doc;
  global.requestAnimationFrame = (cb) => rafQueue.push(cb);
  const flushFrame = () => {
    const callbacks = rafQueue.splice(0);
    for (const cb of callbacks) cb();
  };

  return {
    box: latest.box,
    header: latest.header,
    olderBox: older.box,
    olderHeader: older.header,
    boxes,
    document: doc,
    flushFrame,
    element: () => new FakeElement()
  };
}
