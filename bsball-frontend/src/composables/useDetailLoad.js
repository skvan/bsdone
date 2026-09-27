// 详情加载组合式 —— 行为移植自编译产物 useDetailLoad chunk（逐字）
import { ref, onMounted } from 'vue';

export function useDetailLoad(loader) {
  const loading = ref(true);
  const error = ref(null);
  const result = ref(null);
  async function load() {
    loading.value = true;
    error.value = null;
    try {
      result.value = (await loader()) ?? null;
    } catch (e) {
      error.value = e?.message ?? '加载失败';
      result.value = null;
    } finally {
      loading.value = false;
    }
  }
  onMounted(load);
  return { loading, error, result, retry: load };
}
