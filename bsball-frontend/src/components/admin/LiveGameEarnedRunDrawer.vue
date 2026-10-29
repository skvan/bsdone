<template>
  <el-drawer
    :model-value="visible"
    title="自责分复核"
    direction="rtl"
    size="520px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-loading="loading" class="er-drawer">
      <p v-if="!loading && rows.length === 0" class="er-drawer__empty">
        暂无判定记录（需先完成重建）。
      </p>
      <el-table v-else :data="rows" border stripe size="small" max-height="calc(100vh - 200px)">
        <el-table-column label="局" width="62" align="center">
          <template #default="{ row }">
            {{ row.inning }}{{ row.half === 'top' ? '上' : '下' }}#{{ row.sequence }}
          </template>
        </el-table-column>
        <el-table-column label="跑者" prop="runnerName" min-width="70" />
        <el-table-column label="责任投手" prop="pitcherName" min-width="70" />
        <el-table-column label="判定" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.earnedStatus)"
              size="small"
              effect="plain"
            >{{ statusLabel(row.earnedStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="unearnedReason" min-width="80" show-overflow-tooltip />
        <el-table-column label="覆写" width="50" align="center">
          <template #default="{ row }">
            <span v-if="row.overriddenBy" title="已人工覆写">✓</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">覆写</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 覆写弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="覆写判定"
      width="360px"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="80px" size="default">
        <el-form-item label="跑者">
          {{ editRow?.runnerName }}（局{{ editRow?.inning }}{{ editRow?.half === 'top' ? '上' : '下' }}#{{ editRow?.sequence }}）
        </el-form-item>
        <el-form-item label="当前判定">
          <el-tag :type="statusTagType(editRow?.earnedStatus)" size="small">{{ statusLabel(editRow?.earnedStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="新判定">
          <el-radio-group v-model="editForm.earnedStatus">
            <el-radio value="EARNED">自责</el-radio>
            <el-radio value="UNEARNED">非自责</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editForm.earnedStatus === 'UNEARNED'" label="非自责原因">
          <el-select v-model="editForm.unearnedReason" placeholder="请选择" style="width:100%">
            <el-option v-for="r in UNEARNED_REASONS" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOverride">确认覆写</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { gameApi } from '../../api/business';

const props = defineProps({
  visible: { type: Boolean, default: false },
  gameId: { type: [Number, String], default: null },
});
const emit = defineEmits(['update:visible']);

const UNEARNED_REASONS = [
  { value: 'ERROR_REACHED_BASE', label: '失误上垒' },
  { value: 'ERROR_EXTENDED_INNING', label: '失误延长半局' },
  { value: 'PASSED_BALL', label: '捕逸' },
  { value: 'INTERFERENCE', label: '妨碍行为' },
  { value: 'TIE_BREAK_RUNNER', label: '突破僵局跑者' },
  { value: 'MANUAL_OVERRIDE', label: '人工覆写' },
];

const rows = ref([]);
const loading = ref(false);

async function load() {
  if (!props.gameId) return;
  loading.value = true;
  try {
    const result = await gameApi.listEarnedRunDecisions(props.gameId);
    if (result?.error) {
      rows.value = [];
      ElMessage.error('加载判定失败');
      console.error(result.error);
      return;
    }
    rows.value = Array.isArray(result?.data) ? result.data : [];
  } catch (e) {
    rows.value = [];
    ElMessage.error('加载判定失败');
    console.error(e);
  } finally {
    loading.value = false;
  }
}

watch(() => props.visible, (v) => { if (v) load(); });

// ─── 覆写 ────────────────────────────────────────────────
const editVisible = ref(false);
const editRow = ref(null);
const editForm = ref({ earnedStatus: 'EARNED', unearnedReason: '' });
const submitting = ref(false);

function openEdit(row) {
  editRow.value = row;
  editForm.value = {
    earnedStatus: row.earnedStatus === 'UNEARNED' ? 'UNEARNED' : 'EARNED',
    unearnedReason: row.unearnedReason || '',
  };
  editVisible.value = true;
}

async function submitOverride() {
  const { earnedStatus, unearnedReason } = editForm.value;
  if (earnedStatus === 'UNEARNED' && !unearnedReason) {
    ElMessage.warning('请选择非自责原因');
    return;
  }
  submitting.value = true;
  try {
    await gameApi.overrideEarnedRunDecision(props.gameId, editRow.value.decisionId, {
      earnedStatus,
      unearnedReason: earnedStatus === 'UNEARNED' ? unearnedReason : null,
    });
    ElMessage.success('覆写成功');
    editVisible.value = false;
    load();
  } catch (e) {
    ElMessage.error('覆写失败: ' + (e?.message || ''));
  } finally {
    submitting.value = false;
  }
}

// ─── helpers ──────────────────────────────────────────────
function statusTagType(s) {
  if (s === 'EARNED') return 'success';
  if (s === 'UNEARNED') return 'warning';
  return 'info';
}
function statusLabel(s) {
  if (s === 'EARNED') return '自责';
  if (s === 'UNEARNED') return '非自责';
  if (s === 'PENDING') return '待定';
  return s || '—';
}
</script>

<style scoped>
.er-drawer__empty {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 40px 0;
}
</style>
