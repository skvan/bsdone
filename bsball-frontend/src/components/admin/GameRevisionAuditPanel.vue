<template>
  <div v-loading="loading" class="revision-audit">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      :title="lt('liveGame.revision.auditFinalGate')"
    />
    <template v-else>
      <div class="revision-audit__head">
        <span>{{ lt('liveGame.revision.auditTitle') }}</span>
        <span class="revision-audit__actions">
          <el-select v-model="filterType" size="small" style="width: 120px">
            <el-option :label="lt('liveGame.revision.allTypes')" value="" />
            <el-option
              v-for="(option, key) in TYPE_OPTIONS"
              :key="key"
              :label="lt(option.labelKey)"
              :value="key"
            />
          </el-select>
          {{ lt('liveGame.revision.count', { count: filteredRows.length }) }}
          <el-button link type="primary" size="small" @click="load">{{ lt('liveGame.revision.refresh') }}</el-button>
        </span>
      </div>
      <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" />
      <el-empty v-if="!loading && filteredRows.length === 0" :description="lt('liveGame.revision.auditEmpty')" />
      <el-table
        v-else
        :data="filteredRows"
        size="small"
        border
        stripe
        max-height="520"
      >
        <el-table-column :label="lt('liveGame.revision.time')" width="150">
          <template #default="{ row }">{{ formatTime(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('liveGame.revision.type')" width="112" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="typeTagType(row.auditType)">
              {{ typeLabel(row.auditType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('liveGame.revision.summary')" prop="summary" min-width="230" show-overflow-tooltip />
        <el-table-column :label="lt('liveGame.revision.status')" width="96" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('liveGame.revision.operator')" width="86" align="center">
          <template #default="{ row }">{{ row.operatorId ? `#${row.operatorId}` : '—' }}</template>
        </el-table-column>
        <el-table-column :label="lt('liveGame.revision.reasonNote')" min-width="150" show-overflow-tooltip>
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
import { useLiveGameText } from '../../composables/useLiveGameText';

const { text: lt } = useLiveGameText();
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
  ER_DECISION: { labelKey: 'liveGame.revision.auditTypeEr', type: 'warning' },
  PITCHER_RUN_OVERRIDE: { labelKey: 'liveGame.revision.auditTypePitcher', type: 'primary' },
  FIELDING_ADJUSTMENT: { labelKey: 'liveGame.revision.auditTypeFielding', type: 'success' },
  PITCHER_TRANSFER: { labelKey: 'liveGame.revision.auditTypePitcherTransfer', type: 'info' },
  IDENTITY_TRANSFER: { labelKey: 'liveGame.revision.auditTypeIdentity', type: 'info' }
};

const STATUS_OPTIONS = {
  ACTIVE: { labelKey: 'liveGame.revision.statusActive', type: 'success' },
  SUPERSEDED: { labelKey: 'liveGame.revision.statusSuperseded', type: 'warning' },
  REVOKED: { labelKey: 'liveGame.revision.statusRevoked', type: 'info' },
  INVALIDATED: { labelKey: 'liveGame.revision.statusInvalidated', type: 'danger' },
  APPROVED: { labelKey: 'liveGame.revision.statusApproved', type: 'success' },
  TRANSFERRED: { labelKey: 'liveGame.revision.statusTransferred', type: 'success' },
  DELETED: { labelKey: 'liveGame.revision.statusDeleted', type: 'info' }
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
      ElMessage.error(lt('liveGame.revision.loadAuditFailed'));
      return;
    }
    rows.value = Array.isArray(result?.data) ? result.data : [];
  } catch (e) {
    rows.value = [];
    loadError.value = e?.message ?? lt('liveGame.revision.loadFailed');
    ElMessage.error(lt('liveGame.revision.loadAuditFailed'));
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(() => [props.gameId, props.gameStatus, props.refreshKey], load);

function typeLabel(type) {
  const labelKey = TYPE_OPTIONS[type]?.labelKey;
  return labelKey ? lt(labelKey) : (type ?? '—');
}
function typeTagType(type) {
  return TYPE_OPTIONS[type]?.type ?? 'info';
}
function statusLabel(status) {
  const labelKey = STATUS_OPTIONS[status]?.labelKey;
  return labelKey ? lt(labelKey) : (status ?? '—');
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
