<template>
  <div class="lineup-pool">
    <section ref="poolEl" class="lineup-pool__section">
      <div class="lineup-pool__header">
        <h4>可用球员</h4>
        <span class="lineup-pool__count">{{ pool.length }}</span>
      </div>
      <div class="lineup-pool__cards">
        <LineupPlayerCard
          v-for="p in pool"
          :key="`pool-${p.id}`"
          :player="p"
          variant="pool"
          @pick="emit('pick', $event)"
          @open="emit('open-panel', { player: p, from: 'pool' })"
        />
      </div>
      <p v-if="!pool.length" class="lineup-pool__empty">该队名册已全部安排</p>
    </section>

    <section ref="benchEl" class="lineup-pool__section lineup-pool__section--bench">
      <div class="lineup-pool__header">
        <h4>替补名单</h4>
        <el-button size="small" @click="batchVisible = true">批量添加</el-button>
      </div>
      <div class="lineup-pool__cards">
        <LineupPlayerCard
          v-for="p in bench"
          :key="`bench-${p.id}`"
          :player="p"
          variant="bench"
          @pick="emit('pick', $event)"
          @open="emit('open-panel', { player: p, from: 'bench' })"
        />
      </div>
      <p v-if="!bench.length" class="lineup-pool__empty">拖到此处加入替补，或使用「批量添加」</p>
    </section>

    <el-dialog v-model="batchVisible" title="批量添加替补" width="420px">
      <el-select
        v-model="batchIds"
        multiple
        filterable
        clearable
        collapse-tags
        collapse-tags-tooltip
        placeholder="从名册多选替补"
        style="width: 100%"
      >
        <el-option
          v-for="p in batchOptions"
          :key="p.id"
          :label="`${p.name} #${p.number ?? '-'}`"
          :value="p.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatch">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import LineupPlayerCard from './LineupPlayerCard.vue';

// 可用球员池 + 替补区 + 批量添加（复用旧件的名册多选）
const props = defineProps({
  pool: { type: Array, default: () => [] },
  bench: { type: Array, default: () => [] },
  roster: { type: Array, default: () => [] },
  lineupIds: { type: Array, default: () => [] },
});
const emit = defineEmits(['pick', 'open-panel', 'batch-change']);

const poolEl = ref(null);
const benchEl = ref(null);
const batchVisible = ref(false);
const batchIds = ref([]);

// 选项池与旧件一致：名册 − 已在打线者（可含已在替补者，故可反选移除）
const batchOptions = computed(() => {
  const inLineup = new Set(props.lineupIds.map(String));
  return props.roster.filter((p) => !inLineup.has(String(p.id)));
});

watch(() => props.bench, (list) => {
  batchIds.value = list.map((p) => p.id);
}, { immediate: true });

function confirmBatch() {
  emit('batch-change', [...batchIds.value]);
  batchVisible.value = false;
}

defineExpose({
  poolEl,
  benchEl,
  // 返回原生元素，供页面直接取 rect（规避外层访问时 ref 未 unwrap 的坑）
  getPoolEl: () => poolEl.value,
  getBenchEl: () => benchEl.value,
});
</script>
