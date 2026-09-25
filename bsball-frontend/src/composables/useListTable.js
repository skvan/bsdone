// 列表表格共享组合式 —— 行为移植自编译产物 useListTableProps chunk
// 导出：useFixedOperationColumn（固定操作列方向）、useListTableAttrs（border/stripe/defaultSort）、
//       默认排序常量（id 升序 / createdAt 降序 / gameTime 降序）
import { computed } from 'vue';
import { useSettingsStore } from '../stores/settings';

export const DEFAULT_SORT = { prop: 'id', order: 'ascending' };
export const DEFAULT_SORT_CREATED_AT = { prop: 'createdAt', order: 'descending' };
export const DEFAULT_SORT_GAME_TIME = { prop: 'gameTime', order: 'descending' };

// 固定操作列（编译产物 i/a）
export function useFixedOperationColumn() {
  const settings = useSettingsStore();
  return computed(() => (settings.fixedOperationColumn ? 'right' : false));
}

// 列表表格属性（编译产物 u/i）：border 跟随"表格竖分割线"，stripe 跟随"隔行底色"
export function useListTableAttrs(options) {
  const settings = useSettingsStore();
  return computed(() => ({
    border: settings.listRenderMode === 'bordered',
    stripe: settings.listStripe,
    ...(options?.defaultSort && { defaultSort: options.defaultSort })
  }));
}
