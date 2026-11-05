<template>
  <div v-loading="loading" class="adjustment-ledger">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      title="比赛结束后才能查看修订台账"
    />
    <template v-else>
      <div class="adjustment-ledger__head">
        <span>结果页手工修改 · 字段级留痕</span>
        <span class="adjustment-ledger__actions">
          共 {{ rows.length }} 条
          <el-button link type="primary" size="small" @click="load">刷新</el-button>
        </span>
      </div>
      <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" />
      <el-empty v-if="!loading && !loadError && rows.length === 0" description="暂无字段级修订记录" />
      <el-table v-else :data="rows" size="small" border stripe max-height="520">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column label="球员" min-width="110">
          <template #default="{ row }">{{ row.playerName || `#${row.playerId}` }}</template>
        </el-table-column>
        <el-table-column label="身份" width="66" align="center">
          <template #default="{ row }">{{ row.isPitcher === 1 ? '投手' : '打者' }}</template>
        </el-table-column>
        <el-table-column label="字段" width="110" align="center">
          <template #default="{ row }">{{ row.fieldLabel || row.fieldKey || '—' }}</template>
        </el-table-column>
        <el-table-column label="修改" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">
              {{ row.beforeValue ?? '0' }} → {{ row.afterValue ?? '0' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="86" align="center">
          <template #default="{ row }">{{ row.operatorId ? `#${row.operatorId}` : '—' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
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
const loading = ref(false);
const loadError = ref('');
const isFinal = computed(() => String(props.gameStatus || '').toLowerCase() === 'final');

async function load() {
  if (!props.gameId || !isFinal.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  loadError.value = '';
  try {
    const result = await gameApi.listStatAdjustments(props.gameId);
    if (result?.error) {
      rows.value = [];
      loadError.value = result.error;
      ElMessage.error('加载修订台账失败');
      return;
    }
    rows.value = Array.isArray(result?.data) ? result.data : [];
  } catch (e) {
    rows.value = [];
    loadError.value = e?.message ?? '加载失败';
    ElMessage.error('加载修订台账失败');
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
.adjustment-ledger {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.adjustment-ledger__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.adjustment-ledger__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
