<template>
  <div class="game-earned-run-review">
    <div v-if="showStatus" class="review-status">
      <el-tag size="small" :type="isFinal ? 'success' : 'info'" effect="plain">
        {{ isFinal ? '比赛已结束 · 赛后复核' : '比赛进行中' }}
      </el-tag>
      <span class="review-status__hint">
        {{ isFinal ? '自责分判定与投手 R/ER 均可复核' : '比赛结束后才能手工覆写投手 R/ER' }}
      </span>
    </div>
    <el-tabs v-model="activeTab" class="review-tabs">
      <el-tab-pane label="自责分判定" name="er">
        <div v-loading="loading" class="er-drawer">
          <p v-if="!loading && rows.length === 0" class="er-drawer__empty">
            暂无判定记录（需先完成重建）。
          </p>
          <el-table v-else :data="rows" border stripe size="small" max-height="calc(100vh - 240px)">
            <el-table-column label="局" width="62" align="center">
              <template #default="{ row }">
                {{ row.inning }}{{ row.half === 'top' ? '上' : '下' }}#{{ row.sequence }}
              </template>
            </el-table-column>
            <el-table-column label="跑者" prop="runnerName" min-width="70" />
            <el-table-column label="责任投手" min-width="110">
              <template #default="{ row }">
                <span v-if="row.voided" class="pitcher-status">已作废</span>
                <span v-else>{{ row.effectivePitcherName || row.pitcherName }}</span>
                <el-tag
                  v-if="row.adjustmentAction === 'REASSIGN'"
                  size="small"
                  type="warning"
                  effect="plain"
                  class="pitcher-team"
                >人工改派</el-tag>
                <el-tag
                  v-else-if="row.voided"
                  size="small"
                  type="info"
                  effect="plain"
                  class="pitcher-team"
                >已作废</el-tag>
                <span
                  v-if="row.adjustmentAction === 'REASSIGN'"
                  class="pitcher-engine"
                >原：{{ row.pitcherName }}</span>
              </template>
            </el-table-column>
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
            <el-table-column label="操作" width="190" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.voided">
                  <el-button
                    link
                    type="danger"
                    size="small"
                    :disabled="!isFinal"
                    @click="restoreAdjustment(row)"
                  >恢复</el-button>
                </template>
                <template v-else>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="isPitcherOverridden(row.effectivePitcherId ?? row.responsiblePitcherId)"
                    :title="isPitcherOverridden(row.effectivePitcherId ?? row.responsiblePitcherId) ? '该投手已人工覆写 R/ER，请先撤销' : ''"
                    @click="openEdit(row)"
                  >覆写</el-button>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="!isFinal"
                    :title="!isFinal ? '比赛结束后才能改派判定' : ''"
                    @click="openAdjust(row, 'REASSIGN')"
                  >改派</el-button>
                  <el-button
                    link
                    type="warning"
                    size="small"
                    :disabled="!isFinal"
                    :title="!isFinal ? '比赛结束后才能作废判定' : ''"
                    @click="openAdjust(row, 'VOID')"
                  >作废</el-button>
                  <el-button
                    v-if="row.adjustmentId"
                    link
                    type="danger"
                    size="small"
                    :disabled="!isFinal"
                    @click="restoreAdjustment(row)"
                  >恢复</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="投手 R/ER" name="pitchers" lazy>
        <div v-if="!isFinal" class="er-drawer__empty">
          比赛结束后才能手工覆写投手 R/ER。
        </div>
        <div v-else v-loading="pitcherLoading" class="pitcher-runs">
          <p class="pitcher-runs__hint">
            R/ER 以引擎判定为基准；手工覆写后以覆写值为准，可随时撤销恢复引擎值。
          </p>
          <p v-if="!pitcherLoading && pitcherRows.length === 0" class="er-drawer__empty">
            本场暂无可覆写的投手。
          </p>
          <el-table
            v-else
            :data="pitcherRows"
            border
            stripe
            size="small"
            max-height="calc(100vh - 300px)"
          >
            <el-table-column label="投手" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ row.playerName || `#${row.playerId}` }}</span>
                <el-tag
                  v-if="teamLabel(row.playerId)"
                  size="small"
                  effect="plain"
                  class="pitcher-team"
                >{{ teamLabel(row.playerId) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="R" width="52" align="center">
              <template #default="{ row }">{{ row.runsAllowed ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="ER" width="52" align="center">
              <template #default="{ row }">{{ row.earnedRuns ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="非自责" width="64" align="center">
              <template #default="{ row }">{{ row.unearnedRuns ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="状态" width="86" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.overrideId" size="small" type="warning" effect="plain">
                  人工覆写
                </el-tag>
                <span v-else class="pitcher-status">引擎</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="76" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.overrideId"
                  link
                  type="danger"
                  size="small"
                  @click="confirmRevokePitcher(row)"
                >撤销</el-button>
                <el-button v-else link type="primary" size="small" @click="openPitcherEdit(row)">
                  覆写
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="pitcher-runs__foot">
            R = 责任失分（含非自责）；ER = 自责分；非自责自动按 R − ER 计算。覆写只影响该投手的 R/ER 合计，不改动逐分判定。
          </p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 改派/作废弹窗 -->
    <el-dialog
      v-model="adjustVisible"
      :title="adjustMode === 'REASSIGN' ? '改派判定' : '作废判定'"
      width="400px"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="82px" size="default">
        <el-form-item label="跑者">
          {{ adjustRow?.runnerName }}（局{{ adjustRow?.inning }}{{ adjustRow?.half === 'top' ? '上' : '下' }}#{{ adjustRow?.sequence }}）
        </el-form-item>
        <el-form-item label="当前投手">
          {{ adjustRow?.effectivePitcherName || adjustRow?.pitcherName || '—' }}
        </el-form-item>
        <el-form-item v-if="adjustMode === 'REASSIGN'" label="改派到">
          <el-select
            v-model="adjustTargetId"
            filterable
            placeholder="选择同队投手"
            style="width: 100%"
          >
            <el-option
              v-for="pitcher in adjustTargets"
              :key="pitcher.playerId"
              :label="adjustOptionLabel(pitcher)"
              :value="pitcher.playerId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input
            v-model="adjustReason"
            maxlength="200"
            show-word-limit
            placeholder="可空，例如：16 号误录，应为 21 号"
          />
        </el-form-item>
      </el-form>
      <p v-if="adjustMode === 'VOID'" class="pitcher-warn">
        作废后该得分不再计入任何投手 R/ER；原始判定保留，可随时恢复。
      </p>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSubmitting" @click="submitAdjust">
          {{ adjustMode === 'REASSIGN' ? '确认改派' : '确认作废' }}
        </el-button>
      </template>
    </el-dialog>

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

    <!-- 投手 R/ER 覆写弹窗 -->
    <el-dialog
      v-model="pitcherEditVisible"
      title="覆写投手 R/ER"
      width="380px"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="86px" size="default">
        <el-form-item label="投手">
          {{ pitcherEditRow?.playerName || `#${pitcherEditRow?.playerId}` }}
        </el-form-item>
        <el-form-item label="当前值">
          R {{ pitcherEditRow?.runsAllowed ?? 0 }} / ER {{ pitcherEditRow?.earnedRuns ?? 0 }}
          <span v-if="pitcherEditRow?.overrideId" class="pitcher-engine">
            （引擎 R {{ pitcherEditRow?.engineRunsAllowed ?? 0 }} / ER {{ pitcherEditRow?.engineEarnedRuns ?? 0 }}）
          </span>
        </el-form-item>
        <el-form-item label="新的 R">
          <el-input-number
            v-model="pitcherEditForm.runsAllowed"
            :min="0"
            :max="99"
            :precision="0"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="新的 ER">
          <el-input-number
            v-model="pitcherEditForm.earnedRuns"
            :min="0"
            :max="99"
            :precision="0"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="非自责">{{ pitcherDerivedUnearned }}</el-form-item>
        <el-form-item label="备注">
          <el-input v-model="pitcherEditForm.note" maxlength="200" placeholder="可选" />
        </el-form-item>
      </el-form>
      <p class="pitcher-warn">
        保存后该投手 R/ER 以人工值为准；若之后重建半局或修改判定，该投手覆写会失效并需重新复核。
      </p>
      <template #footer>
        <el-button @click="pitcherEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="pitcherSubmitting" @click="submitPitcherOverride">
          保存覆写
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { gameApi } from '../../api/business';

const props = defineProps({
  gameId: { type: [Number, String], default: null },
  gameStatus: { type: String, default: '' },
  showStatus: { type: Boolean, default: true },
  homeLineup: { type: Array, default: () => [] },
  awayLineup: { type: Array, default: () => [] },
  homeBench: { type: Array, default: () => [] },
  awayBench: { type: Array, default: () => [] },
  homeRoster: { type: Array, default: () => [] },
  awayRoster: { type: Array, default: () => [] }
});
const emit = defineEmits(['changed']);
const activeTab = ref('er');
const remoteGameStatus = ref('');
const resolvedGameStatus = computed(() => String(props.gameStatus || remoteGameStatus.value || '').trim());
const isFinal = computed(() => resolvedGameStatus.value.toLowerCase() === 'final');

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

const pitcherRows = ref([]);
const pitcherLoading = ref(false);
const pitcherLoaded = ref(false);
const pitcherEditVisible = ref(false);
const pitcherEditRow = ref(null);
const pitcherEditForm = ref({ runsAllowed: 0, earnedRuns: 0, note: '' });
const pitcherSubmitting = ref(false);

const adjustVisible = ref(false);
const adjustMode = ref('REASSIGN');
const adjustRow = ref(null);
const adjustTargetId = ref(null);
const adjustReason = ref('');
const adjustSubmitting = ref(false);

const adjustTeamId = computed(() => {
  const row = adjustRow.value;
  if (!row) return null;
  const pitcherId = row.effectivePitcherId ?? row.responsiblePitcherId;
  return pitcherRows.value.find(
    (pitcher) => String(pitcher.playerId) === String(pitcherId)
  )?.teamId ?? null;
});
const adjustTargets = computed(() => pitcherRows.value.filter((pitcher) => (
  String(pitcher.teamId) === String(adjustTeamId.value)
  && String(pitcher.playerId) !== String(adjustRow.value?.effectivePitcherId)
)));

const pitcherDerivedUnearned = computed(() => Math.max(
  0,
  (Number(pitcherEditForm.value.runsAllowed) || 0) - (Number(pitcherEditForm.value.earnedRuns) || 0)
));

const teamLabelByPlayer = computed(() => {
  const labels = new Map();
  const mark = (list, label) => {
    for (const player of Array.isArray(list) ? list : []) {
      const id = player?.playerId ?? player?.id;
      if (id != null) labels.set(String(id), label);
    }
  };
  mark(props.homeLineup, '主');
  mark(props.homeBench, '主');
  mark(props.homeRoster, '主');
  mark(props.awayLineup, '客');
  mark(props.awayBench, '客');
  mark(props.awayRoster, '客');
  return labels;
});

const overriddenPitcherIds = computed(() => new Set(
  pitcherRows.value
    .filter((row) => row?.overrideId)
    .map((row) => String(row.playerId))
));

function teamLabel(playerId) {
  return teamLabelByPlayer.value.get(String(playerId)) || '';
}

function isPitcherOverridden(playerId) {
  return playerId != null && overriddenPitcherIds.value.has(String(playerId));
}

async function loadPitcherTotals() {
  if (!props.gameId || !isFinal.value || pitcherLoading.value) return;
  pitcherLoading.value = true;
  try {
    const result = await gameApi.listPitcherRunTotals(props.gameId);
    if (result?.error) {
      pitcherRows.value = [];
      ElMessage.error('加载投手 R/ER 失败');
      console.error(result.error);
      return;
    }
    pitcherRows.value = Array.isArray(result?.data) ? result.data : [];
    pitcherLoaded.value = true;
  } catch (e) {
    pitcherRows.value = [];
    ElMessage.error('加载投手 R/ER 失败');
    console.error(e);
  } finally {
    pitcherLoading.value = false;
  }
}

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

onMounted(() => {
  load();
  loadGameStatus();
});

watch(() => props.gameId, (value, oldValue) => {
  if (value && value !== oldValue) {
    load();
    loadGameStatus();
  }
});

watch(activeTab, (tab) => {
  if (tab === 'pitchers' && isFinal.value && !pitcherLoaded.value) {
    loadPitcherTotals();
  }
});

async function loadGameStatus() {
  if (!props.gameId) return;
  if (!props.gameStatus) {
    const game = await gameApi.get(props.gameId);
    remoteGameStatus.value = game?.status ?? '';
  }
  if (isFinal.value && !pitcherLoaded.value) {
    loadPitcherTotals();
  }
}

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
    loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error('覆写失败: ' + (e?.message || ''));
  } finally {
    submitting.value = false;
  }
}

// ─── 投手 R/ER 覆写 ──────────────────────────────────────
function openPitcherEdit(row) {
  pitcherEditRow.value = row;
  pitcherEditForm.value = {
    runsAllowed: Number(row?.runsAllowed) || 0,
    earnedRuns: Number(row?.earnedRuns) || 0,
    note: row?.overrideNote || '',
  };
  pitcherEditVisible.value = true;
}

async function submitPitcherOverride() {
  const runsAllowed = Number(pitcherEditForm.value.runsAllowed);
  const earnedRuns = Number(pitcherEditForm.value.earnedRuns);
  if (![runsAllowed, earnedRuns].every((value) => Number.isInteger(value) && value >= 0 && value <= 99)) {
    ElMessage.warning('R/ER 需为 0-99 的整数');
    return;
  }
  if (earnedRuns > runsAllowed) {
    ElMessage.warning('ER 不能大于 R');
    return;
  }
  pitcherSubmitting.value = true;
  try {
    await gameApi.overridePitcherRuns(props.gameId, pitcherEditRow.value.playerId, {
      runsAllowed,
      earnedRuns,
      note: pitcherEditForm.value.note?.trim() || null,
    });
    ElMessage.success('覆写已保存');
    pitcherEditVisible.value = false;
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error('保存失败: ' + (e?.message || ''));
  } finally {
    pitcherSubmitting.value = false;
  }
}

async function confirmRevokePitcher(row) {
  const name = row?.playerName || `#${row?.playerId}`;
  const engineR = row?.engineRunsAllowed ?? 0;
  const engineEr = row?.engineEarnedRuns ?? 0;
  try {
    await ElMessageBox.confirm(
      `将撤销 ${name} 的人工覆写，恢复引擎值 R ${engineR} / ER ${engineEr}。`,
      '撤销人工覆写',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' }
    );
  } catch (e) {
    return;
  }
  try {
    await gameApi.revokePitcherRunOverride(props.gameId, row.overrideId);
    ElMessage.success('已撤销，恢复引擎值');
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error('撤销失败: ' + (e?.message || ''));
  }
}

// ─── 判定改派/作废/恢复 ──────────────────────────────────
function openAdjust(row, mode) {
  if (!isFinal.value || !row) return;
  adjustRow.value = row;
  adjustMode.value = mode;
  adjustTargetId.value = null;
  adjustReason.value = '';
  adjustVisible.value = true;
  if (!pitcherLoaded.value) {
    loadPitcherTotals();
  }
}

function adjustOptionLabel(pitcher) {
  if (!pitcher) return '';
  const number = pitcher.number ? `${pitcher.number}号 ` : '';
  return `${number}${pitcher.playerName || `#${pitcher.playerId}`}`;
}

async function submitAdjust() {
  const row = adjustRow.value;
  if (!row?.decisionId) return;
  if (adjustMode.value === 'REASSIGN' && !adjustTargetId.value) {
    ElMessage.warning('请选择改派目标投手');
    return;
  }
  adjustSubmitting.value = true;
  try {
    if (adjustMode.value === 'REASSIGN') {
      await gameApi.reassignEarnedRunDecision(props.gameId, row.decisionId, {
        targetPitcherId: adjustTargetId.value,
        reason: adjustReason.value.trim() || null
      });
      ElMessage.success('改派已保存');
    } else {
      await gameApi.voidEarnedRunDecision(props.gameId, row.decisionId, {
        reason: adjustReason.value.trim() || null
      });
      ElMessage.success('判定已作废');
    }
    adjustVisible.value = false;
    await load();
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(e?.message || '保存失败');
  } finally {
    adjustSubmitting.value = false;
  }
}

async function restoreAdjustment(row) {
  if (!isFinal.value || !row?.adjustmentId) return;
  try {
    await ElMessageBox.confirm(
      `恢复 ${row.runnerName || '该跑者'} 的判定为引擎责任投手？`,
      '恢复判定',
      { type: 'warning', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
    );
  } catch {
    return;
  }
  try {
    await gameApi.revokeEarnedRunDecisionAdjustment(props.gameId, row.adjustmentId);
    ElMessage.success('已恢复引擎责任投手');
    await load();
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(e?.message || '恢复失败');
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
.review-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.review-status__hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.review-tabs {
  margin-top: -4px;
}
.er-drawer__empty {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 40px 0;
}
.pitcher-runs__hint {
  margin: 0 0 10px;
  padding: 8px 10px;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  background: #f4f8ff;
  color: #5b6b80;
  font-size: 12px;
  line-height: 1.55;
}
.pitcher-runs__foot {
  margin: 10px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.6;
}
.pitcher-team {
  margin-left: 6px;
  vertical-align: middle;
}
.pitcher-status {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.pitcher-engine {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.pitcher-warn {
  margin: 4px 0 0;
  padding: 8px 10px;
  border: 1px solid #faecd8;
  border-radius: 5px;
  background: #fdf6ec;
  color: #966b28;
  font-size: 11px;
  line-height: 1.55;
}
.game-earned-run-review {
  min-height: 200px;
}
</style>
