<template>
  <div v-loading="loading" class="stat-base-diff">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      title="比赛结束后才能查看基座对比"
    />
    <el-alert
      v-else-if="legacy"
      type="info"
      :closable="false"
      show-icon
      title="历史场次没有基座快照，无法对比原始值"
      description="基座快照从本功能上线后的首次 final 保存开始记录。"
    />
    <template v-else>
      <div class="stat-base-diff__head">
        <span>原始基座 · 当前有效值</span>
        <el-button link type="primary" size="small" @click="load">刷新</el-button>
      </div>
      <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" />
      <el-empty v-if="!loading && !loadError && rows.length === 0" description="当前有效值与基座一致" />
      <el-table v-else :data="rows" size="small" border stripe max-height="520">
        <el-table-column label="球员" min-width="110">
          <template #default="{ row }">{{ row.playerName || `#${row.playerId}` }}</template>
        </el-table-column>
        <el-table-column label="身份" width="68" align="center">
          <template #default="{ row }">{{ row.isPitcher === 1 ? '投手' : '打者' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="76" align="center" prop="source" />
        <el-table-column label="捕获时间" width="150">
          <template #default="{ row }">{{ formatTime(row.capturedAt) }}</template>
        </el-table-column>
        <el-table-column label="差异（基座 → 当前）" min-width="260">
          <template #default="{ row }">
            <el-tag
              v-for="field in row.fields"
              :key="field.key"
              size="small"
              effect="plain"
              class="stat-base-diff__tag"
            >{{ field.label }}：{{ field.baseValue }} → {{ field.effectiveValue }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { gameApi } from '../../api/business';

const props = defineProps({
  gameId: { type: [Number, String], default: null },
  gameStatus: { type: String, default: '' },
  refreshKey: { type: Number, default: 0 }
});

const rows = ref([]);
const legacy = ref(false);
const loading = ref(false);
const loadError = ref('');
const isFinal = computed(() => String(props.gameStatus || '').toLowerCase() === 'final');

async function load() {
  rows.value = [];
  legacy.value = false;
  if (!props.gameId || !isFinal.value) {
    return;
  }
  loading.value = true;
  loadError.value = '';
  try {
    const result = await gameApi.compareStatBase(props.gameId);
    if (result?.error) {
      loadError.value = result.error;
      ElMessage.error('加载基座对比失败');
      return;
    }
    legacy.value = result?.data?.legacy === true;
    rows.value = Array.isArray(result?.data?.rows) ? result.data.rows : [];
  } catch (e) {
    loadError.value = e?.message ?? '加载失败';
    ElMessage.error('加载基座对比失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(() => [props.gameId, props.gameStatus, props.refreshKey], load);

function formatTime(value) {
  if (!value) return '—';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.stat-base-diff {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stat-base-diff__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.stat-base-diff__tag {
  margin: 2px 4px 2px 0;
}
</style>
