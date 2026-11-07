<template>
  <div class="lineup-batting">
    <div class="lineup-batting__header">
      <h4>{{ lt('liveGame.lineup.battingOrder.title') }}</h4>
      <span class="lineup-batting__hint">{{ lt('liveGame.lineup.battingOrder.count', { filled: filledCount, max: maxCount }) }}</span>
    </div>
    <ul ref="listEl" class="lineup-batting__list" :class="{ 'is-drop-over': dropHover }">
      <li v-if="!filledRows.length" class="lineup-batting__empty">{{ lt('liveGame.lineup.battingOrder.empty') }}</li>
      <li
        v-for="(row, index) in filledRows"
        :key="`p-${row.id}`"
        class="lineup-batting__row"
        :class="{ 'is-error': errorIndices.includes(index), 'is-drop-before': isDropBefore(row), 'is-drop-after': isDropAfter(row) }"
        :data-player-id="row.id"
        @click.stop="emit('pick', row.id)"
      >
        <el-icon class="lineup-batting__handle drag-handle" :title="lt('liveGame.lineup.battingOrder.handle')"><Rank /></el-icon>
        <span class="lineup-batting__order">{{ orderText(index + 1) }}</span>
        <span class="lineup-batting__name">{{ row.name }}</span>
        <span class="lineup-batting__number">#{{ row.number ?? '-' }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import Sortable from 'sortablejs';
import { Rank } from '@element-plus/icons-vue';
import { isPlaceholder } from '../../utils/lineupBoardRules';
import { useLiveGameText } from '../../composables/useLiveGameText';

const { text: lt, isEnglish } = useLiveGameText();
const ordinal = (n) => {
  const value = Number(n);
  const mod100 = value % 100;
  if (mod100 >= 11 && mod100 <= 13) return `${value}th`;
  return `${value}${['th', 'st', 'nd', 'rd'][value % 10] ?? 'th'}`;
};
const orderText = (n) => (isEnglish.value ? ordinal(n) : lt('liveGame.lineup.battingOrder.order', { n }));

// 打线列表：只渲染非空行（下标即棒次），故 onEnd 的 oldIndex/newIndex 可直接喂 reorderBatting
// 行可被页面绑定的 interactjs 拖起（data-player-id 供落点取人）；列表内排序走把手的 sortablejs
const props = defineProps({
  rows: { type: Array, default: () => [] },
  errorIndices: { type: Array, default: () => [] },
  maxCount: { type: Number, default: 10 },
  // 拖拽悬停在本区域（页面下发，用于「松手会落在这里」高亮）
  dropHover: { type: Boolean, default: false },
  // 桌面整卡拖动：{ beforeId, afterId } 标记插入线画在哪一行（页面按 gap 计算下发）
  dropLine: { type: Object, default: null },
});
const emit = defineEmits(['reorder', 'pick']);
const listEl = ref(null);
const filledRows = computed(() => props.rows.filter((r) => !isPlaceholder(r)));
const filledCount = computed(() => filledRows.value.length);
const isDropBefore = (row) => props.dropLine?.beforeId != null && String(props.dropLine.beforeId) === String(row.id);
const isDropAfter = (row) => props.dropLine?.afterId != null && String(props.dropLine.afterId) === String(row.id);
let sortable = null;

onMounted(() => {
  sortable = Sortable.create(listEl.value, {
    handle: '.drag-handle',
    animation: 150,
    onEnd({ oldIndex, newIndex }) {
      if (oldIndex == null || newIndex == null || oldIndex === newIndex) return;
      emit('reorder', { from: oldIndex, to: newIndex });
    },
  });
});
onBeforeUnmount(() => {
  sortable?.destroy();
  sortable = null;
});

defineExpose({
  listEl,
  // 返回原生元素，供页面直接取 rect（规避外层访问时 ref 未 unwrap 的坑）
  getListEl: () => listEl.value,
});
</script>
