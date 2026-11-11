<template>
  <div class="lineup-pool">
    <section ref="poolEl" class="lineup-pool__section" :class="{ 'is-drop-over': hoverKind === 'pool' }">
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
          :state="String(pendingPlayerId ?? '') === String(p.id) ? 'pending' : 'idle'"
          @pick="emit('pick', $event)"
        />
      </div>
      <p v-if="!pool.length" class="lineup-pool__empty">该队名册已全部安排</p>
    </section>

    <section ref="benchEl" class="lineup-pool__section lineup-pool__section--bench" :class="{ 'is-drop-over': hoverKind === 'bench' }">
      <div class="lineup-pool__header">
        <h4>替补名单</h4>
        <el-button size="small" @click="openBatch">批量添加</el-button>
      </div>
      <div class="lineup-pool__cards">
        <LineupPlayerCard
          v-for="p in bench"
          :key="`bench-${p.id}`"
          :player="p"
          variant="bench"
          :state="String(pendingPlayerId ?? '') === String(p.id) ? 'pending' : 'idle'"
          @pick="emit('pick', $event)"
        />
      </div>
      <p v-if="!bench.length" class="lineup-pool__empty">拖到此处加入替补，或使用「批量添加」</p>
    </section>

    <el-dialog v-model="batchVisible" title="批量添加替补" width="420px">
      <div class="lineup-batch">
        <div class="lineup-batch__head">
          <el-checkbox
            :model-value="batchSelection.allSelected"
            :indeterminate="batchSelection.indeterminate"
            :disabled="!batchOptions.length"
            @change="toggleAll"
          >
            全选
          </el-checkbox>
          <span class="lineup-batch__count">已选 {{ batchSelection.selectedCount }} / {{ batchOptions.length }}</span>
        </div>
        <div v-if="batchOptions.length" class="lineup-batch__list">
          <el-checkbox-group v-model="batchIds">
            <el-checkbox
              v-for="p in batchOptions"
              :key="p.id"
              :value="p.id"
              class="lineup-batch__row"
            >
              <span class="lineup-batch__name">{{ p.name }}</span>
              <span class="lineup-batch__number">#{{ p.number ?? '-' }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <p v-else class="lineup-batch__empty">名册中没有可加入替补的球员</p>
      </div>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatch">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import LineupPlayerCard from './LineupPlayerCard.vue';
import { benchSelectionState, sanitizeBenchIds } from '../../utils/lineupBenchSelection.js';

// 可用球员池 + 替补区 + 批量添加（勾选名册中的替补）
const props = defineProps({
  pool: { type: Array, default: () => [] },
  bench: { type: Array, default: () => [] },
  roster: { type: Array, default: () => [] },
  lineupIds: { type: Array, default: () => [] },
  // 点选点放待放置的球员 id（用于卡片待放描边，设计稿 §四.3）
  pendingPlayerId: { type: [Number, String], default: null },
  // 拖拽悬停的区域（'pool' | 'bench' | ''），用于「松手会落在这里」高亮
  hoverKind: { type: String, default: '' },
});
const emit = defineEmits(['pick', 'batch-change']);

const poolEl = ref(null);
const benchEl = ref(null);
const batchVisible = ref(false);
const batchIds = ref([]);

// 选项池与旧件一致：名册 − 已在打线者（可含已在替补者，故可反选移除）
const batchOptions = computed(() => {
  const inLineup = new Set(props.lineupIds.map(String));
  return props.roster.filter((p) => !inLineup.has(String(p.id)));
});

// 表头全选/半选状态只按当前候选项计算
const batchSelection = computed(() => benchSelectionState(batchOptions.value, batchIds.value));

// 每次打开都以当前替补席为准，取消后再次打开不会残留上次勾选
function openBatch() {
  batchIds.value = props.bench.map((p) => p.id);
  batchVisible.value = true;
}

function toggleAll(checked) {
  batchIds.value = checked ? batchOptions.value.map((p) => p.id) : [];
}

function confirmBatch() {
  emit('batch-change', sanitizeBenchIds(batchOptions.value, batchIds.value));
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
