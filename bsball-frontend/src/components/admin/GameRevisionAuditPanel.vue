<template>
  <div v-loading="loading" class="revision-audit">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      title="比赛结束后才能查看修订记录"
    />
    <template v-else>
      <div class="revision-audit__head">
        <span>实时记录为基座 · 人工修订为最终值</span>
        <span class="revision-audit__actions">
          <el-select v-model="filterType" size="small" style="width: 120px">
            <el-option label="全部类型" value="" />
            <el-option
              v-for="(option, key) in TYPE_OPTIONS"
              :key="key"
              :label="option.label"
              :value="key"
            />
          </el-select>
          共 {{ filteredRows.length }} 条
          <el-button link type="primary" size="small" @click="load">刷新</el-button>
        </span>
      </div>
      <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" />
      <el-empty v-if="!loading && filteredRows.length === 0" description="暂无修订记录" />
      <el-table
        v-else
        :data="filteredRows"
        size="small"
        border
        stripe
        max-height="520"
      >
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="112" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="typeTagType(row.auditType)">
              {{ typeLabel(row.auditType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="摘要" prop="summary" min-width="230" show-overflow-tooltip />
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="86" align="center">
          <template #default="{ row }">{{ row.operatorId ? `#${row.operatorId}` : '—' }}</template>
        </el-table-column>
        <el-table-column label="原因/备注" min-width="150" show-overflow-tooltip>
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
const filterType = ref('');
const isFinal = computed(() => String(props.gameStatus || '').toLowerCase() === 'final');
const filteredRows = computed(() => (
  filterType.value ? rows.value.filter((row) => row.auditType === filterType.value) : rows.value
));

const TYPE_OPTIONS = {
  ER_DECISION: { label: 'ER 判定', type: 'warning' },
  PITCHER_RUN_OVERRIDE: { label: '投手 R/ER', type: 'primary' },
  FIELDING_ADJUSTMENT: { label: '守备补记', type: 'success' },
  PITCHER_TRANSFER: { label: '投手转记', type: 'info' },
  IDENTITY_TRANSFER: { label: '身份转记', type: 'info' }
};

const STATUS_OPTIONS = {
  ACTIVE: { label: '生效', type: 'success' },
  SUPERSEDED: { label: '已替代', type: 'warning' },
  REVOKED: { label: '已撤销', type: 'info' },
  INVALIDATED: { label: '已失效', type: 'danger' },
  APPROVED: { label: '已生效', type: 'success' },
  TRANSFERRED: { label: '已转记', type: 'success' },
  DELETED: { label: '已删源数据', type: 'info' }
};

async function load() {
  if (!props.gameId || !isFinal.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  loadError.value = '';
  try {
    const result = await gameApi.listRevisionAudit(props.gameId);
    if (result?.error) {
      rows.value = [];
      loadError.value = result.error;
      ElMessage.error('加载修订记录失败');
      return;
    }
    rows.value = Array.isArray(result?.data) ? result.data : [];
  } catch (e) {
    rows.value = [];
    loadError.value = e?.message ?? '加载失败';
    ElMessage.error('加载修订记录失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(() => [props.gameId, props.gameStatus, props.refreshKey], load);

function typeLabel(type) {
  return TYPE_OPTIONS[type]?.label ?? type ?? '—';
}
function typeTagType(type) {
  return TYPE_OPTIONS[type]?.type ?? 'info';
}
function statusLabel(status) {
  return STATUS_OPTIONS[status]?.label ?? status ?? '—';
}
function statusTagType(status) {
  return STATUS_OPTIONS[status]?.type ?? 'info';
}
function formatTime(value) {
  if (!value) return '—';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.revision-audit {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.revision-audit__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.revision-audit__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
