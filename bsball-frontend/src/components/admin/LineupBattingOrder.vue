<template>
  <div class="lineup-batting">
    <div class="lineup-batting__header">
      <h4>打线顺序</h4>
      <span class="lineup-batting__hint">{{ filledCount }} / {{ maxCount }} 人</span>
    </div>
    <ul ref="listEl" class="lineup-batting__list">
      <li
        v-for="(row, index) in filledRows"
        :key="`p-${row.id}`"
        class="lineup-batting__row"
        :class="{ 'is-error': errorIndices.includes(index) }"
        @click="emit('row-open', row.id)"
      >
        <span class="lineup-batting__handle drag-handle" />
        <span class="lineup-batting__order">{{ index + 1 }}</span>
        <span class="lineup-batting__name">{{ row.name }}</span>
        <span class="lineup-batting__number">#{{ row.number ?? '-' }}</span>
        <span class="lineup-batting__pos">{{ posOf(row) }}</span>
      </li>
    </ul>
    <p v-if="!filledRows.length" class="lineup-batting__empty">把球员拖到球场上，或直接拖进这里（不占守备位）</p>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import Sortable from 'sortablejs';
import { isPlaceholder, posOf } from '../../utils/lineupBoardRules';

// 打线列表：只渲染非空行（下标即棒次），故 onEnd 的 oldIndex/newIndex 可直接喂 reorderBatting
const props = defineProps({
  rows: { type: Array, default: () => [] },
  errorIndices: { type: Array, default: () => [] },
  maxCount: { type: Number, default: 10 },
});
const emit = defineEmits(['reorder', 'row-open']);
const listEl = ref(null);
const filledRows = computed(() => props.rows.filter((r) => !isPlaceholder(r)));
const filledCount = computed(() => filledRows.value.length);
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
