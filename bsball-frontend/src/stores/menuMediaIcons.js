// 菜单媒体图标 store —— 行为移植自编译产物 menuMediaIcons chunk（pinia "menuMediaIcons"）
// 数据来源：DB 下发的媒体图标（fontSymbol + svgContent），key 带 "media:" 前缀
import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { mediaIconApi } from '../api/system';
import { staticIconMap, MENU_MEDIA_ICON_PREFIX } from '../components/admin/menu-icons';
import { createMenuMediaIconWrap } from '../components/admin/MediaMenuIcon';

// 全量分页拉取（pageSize 100，与编译产物一致）
async function fetchAllMediaIcons() {
  const list = [];
  let page = 1;
  const pageSize = 100;
  for (;;) {
    const { list: rows, total } = await mediaIconApi.list({ page, pageSize });
    list.push(...rows);
    if (list.length >= total || rows.length === 0) break;
    page += 1;
  }
  return list;
}

export const useMenuMediaIconsStore = defineStore('menuMediaIcons', () => {
  const loaded = ref({});
  const isLoaded = ref(false);
  const loading = ref(false);
  let inflight = null;

  const mergedMap = computed(() => ({ ...staticIconMap, ...loaded.value }));
  const mediaIconKeys = computed(() => Object.keys(loaded.value).sort());

  async function ensureLoaded() {
    if (isLoaded.value) return;
    if (inflight) {
      await inflight;
      return;
    }
    inflight = (async () => {
      loading.value = true;
      try {
        const rows = await fetchAllMediaIcons();
        const map = {};
        for (const row of rows) {
          const symbol = row.fontSymbol?.trim();
          if (!symbol || !row.svgContent?.trim()) continue;
          map[`${MENU_MEDIA_ICON_PREFIX}${symbol}`] = createMenuMediaIconWrap(row.svgContent);
        }
        loaded.value = map;
        isLoaded.value = true;
      } catch {
        isLoaded.value = false;
      } finally {
        loading.value = false;
      }
    })();
    await inflight;
    inflight = null;
  }

  async function reload() {
    isLoaded.value = false;
    loaded.value = {};
    inflight = null;
    await ensureLoaded();
  }

  return { mergedMap, mediaIconKeys, ensureLoaded, reload, loading };
});
