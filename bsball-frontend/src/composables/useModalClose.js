// 弹窗遮罩关闭偏好 —— 行为移植自编译产物 useModalClose chunk（读取 settings store）
import { computed } from 'vue';
import { useSettingsStore } from '../stores/settings';

export function useModalClose() {
  const settings = useSettingsStore();
  return computed(() => settings.modalCloseOnClickMask);
}
