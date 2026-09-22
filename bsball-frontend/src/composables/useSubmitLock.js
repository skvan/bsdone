// 提交锁组合式 —— 行为移植自编译产物 useSubmitLock chunk（重复提交防抖）
import { ref } from 'vue';

export function useSubmitLock() {
  const submitting = ref(false);

  async function withSubmitLock(task) {
    if (submitting.value) return undefined;
    submitting.value = true;
    try {
      return await task();
    } finally {
      submitting.value = false;
    }
  }

  return { submitting, withSubmitLock };
}
