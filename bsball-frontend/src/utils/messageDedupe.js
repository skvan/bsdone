// 全局消息去重 —— 行为移植自编译产物入口 chunk（ur()/ya/lr/sr：ElMessage.error/warning 2.5s 同文案抑制）
import { ElMessage } from 'element-plus';

const DEDUPE_WINDOW_MS = 2500;

function messageText(value) {
  if (typeof value === 'string') return value.trim();
  const text = value?.message;
  return (typeof text === 'string' ? text : '').trim();
}

let lastErrorText = '';
let lastErrorAt = 0;
let lastWarningText = '';
let lastWarningAt = 0;

export function installMessageDedupe() {
  const rawError = ElMessage.error.bind(ElMessage);
  const rawWarning = ElMessage.warning.bind(ElMessage);
  ElMessage.error = (value) => {
    const text = messageText(value ?? '') || '\0';
    const now = Date.now();
    if (text !== lastErrorText || now - lastErrorAt >= DEDUPE_WINDOW_MS) {
      lastErrorText = text;
      lastErrorAt = now;
      rawError(value);
    }
  };
  ElMessage.warning = (value) => {
    const text = messageText(value ?? '') || '\0';
    const now = Date.now();
    if (text !== lastWarningText || now - lastWarningAt >= DEDUPE_WINDOW_MS) {
      lastWarningText = text;
      lastWarningAt = now;
      rawWarning(value);
    }
  };
}
