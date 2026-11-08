<template>
  <div class="game-earned-run-review">
    <div v-if="showStatus" class="review-status">
      <el-tag size="small" :type="isFinal ? 'success' : 'info'" effect="plain">
        {{ isFinal ? lt('liveGame.review.statusFinal') : lt('liveGame.review.statusLive') }}
      </el-tag>
      <span class="review-status__hint">
        {{ isFinal ? lt('liveGame.review.erHintFinal') : lt('liveGame.review.erHintLive') }}
      </span>
    </div>
    <el-tabs v-model="activeTab" class="review-tabs">
      <el-tab-pane :label="lt('liveGame.review.tabDecisions')" name="er">
        <div v-loading="loading" class="er-drawer">
          <p v-if="!loading && rows.length === 0" class="er-drawer__empty">
            {{ lt('liveGame.review.emptyDecisions') }}
          </p>
          <el-table v-else :data="rows" border stripe size="small" max-height="calc(100vh - 240px)">
            <el-table-column :label="lt('liveGame.review.inning')" width="62" align="center">
              <template #default="{ row }">
                {{ row.inning }}{{ halfLabel(row.half) }}#{{ row.sequence }}
              </template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.review.runner')" prop="runnerName" min-width="70" />
            <el-table-column :label="lt('liveGame.review.responsiblePitcher')" min-width="110">
              <template #default="{ row }">
                <span v-if="row.voided" class="pitcher-status">{{ lt('liveGame.review.voided') }}</span>
                <span v-else>{{ row.effectivePitcherName || row.pitcherName }}</span>
                <el-tag
                  v-if="row.adjustmentAction === 'REASSIGN'"
                  size="small"
                  type="warning"
                  effect="plain"
                  class="pitcher-team"
                >{{ lt('liveGame.review.manualReassign') }}</el-tag>
                <el-tag
                  v-else-if="row.voided"
                  size="small"
                  type="info"
                  effect="plain"
                  class="pitcher-team"
                >{{ lt('liveGame.review.voided') }}</el-tag>
                <span
                  v-if="row.adjustmentAction === 'REASSIGN'"
                  class="pitcher-engine"
                >{{ lt('liveGame.review.original', { name: row.pitcherName }) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.review.decision')" width="110" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="statusTagType(row.earnedStatus)"
                  size="small"
                  effect="plain"
                >{{ statusLabel(row.earnedStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.review.reason')" prop="unearnedReason" min-width="80" show-overflow-tooltip />
            <el-table-column :label="lt('liveGame.review.override')" width="50" align="center">
              <template #default="{ row }">
                <span v-if="row.overriddenBy" :title="lt('liveGame.review.overriddenTitle')">✓</span>
              </template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.review.actions')" width="190" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.voided">
                  <el-button
                    link
                    type="danger"
                    size="small"
                    :disabled="!isFinal"
                    @click="restoreAdjustment(row)"
                  >{{ lt('liveGame.review.restore') }}</el-button>
                </template>
                <template v-else>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="isPitcherOverridden(row.effectivePitcherId ?? row.responsiblePitcherId)"
                    :title="isPitcherOverridden(row.effectivePitcherId ?? row.responsiblePitcherId) ? lt('liveGame.review.pitcherOverriddenHint') : ''"
                    @click="openEdit(row)"
                  >{{ lt('liveGame.review.overrideBtn') }}</el-button>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="!isFinal"
                    :title="!isFinal ? lt('liveGame.review.notFinalReassign') : ''"
                    @click="openAdjust(row, 'REASSIGN')"
                  >{{ lt('liveGame.review.reassignBtn') }}</el-button>
                  <el-button
                    link
                    type="warning"
                    size="small"
                    :disabled="!isFinal"
                    :title="!isFinal ? lt('liveGame.review.notFinalVoid') : ''"
                    @click="openAdjust(row, 'VOID')"
                  >{{ lt('liveGame.review.voidBtn') }}</el-button>
                  <el-button
                    v-if="row.adjustmentId"
                    link
                    type="danger"
                    size="small"
                    :disabled="!isFinal"
                    @click="restoreAdjustment(row)"
                  >{{ lt('liveGame.review.restore') }}</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane :label="lt('liveGame.review.tabPitchers')" name="pitchers" lazy>
        <div v-if="!isFinal" class="er-drawer__empty">
          {{ lt('liveGame.review.pitcherHintLive') }}
        </div>
        <div v-else v-loading="pitcherLoading" class="pitcher-runs">
          <p class="pitcher-runs__hint">
            {{ lt('liveGame.review.pitcherHint') }}
          </p>
          <p v-if="!pitcherLoading && pitcherRows.length === 0" class="er-drawer__empty">
            {{ lt('liveGame.review.noPitchers') }}
          </p>
          <el-table
            v-else
            :data="pitcherRows"
            border
            stripe
            size="small"
            max-height="calc(100vh - 300px)"
          >
            <el-table-column :label="lt('liveGame.review.pitcher')" min-width="120" show-overflow-tooltip>
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
            <el-table-column :label="lt('liveGame.review.unearned')" width="64" align="center">
              <template #default="{ row }">{{ row.unearnedRuns ?? 0 }}</template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.review.status')" width="86" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.overrideId" size="small" type="warning" effect="plain">
                  {{ lt('liveGame.review.manualOverride') }}
                </el-tag>
                <span v-else class="pitcher-status">{{ lt('liveGame.review.engine') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.review.actions')" width="76" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.overrideId"
                  link
                  type="danger"
                  size="small"
                  @click="confirmRevokePitcher(row)"
                >{{ lt('liveGame.review.undo') }}</el-button>
                <el-button v-else link type="primary" size="small" @click="openPitcherEdit(row)">
                  {{ lt('liveGame.review.overrideBtn') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="pitcher-runs__foot">
            {{ lt('liveGame.review.pitcherFootnote') }}
          </p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 改派/作废弹窗 -->
    <el-dialog
      v-model="adjustVisible"
      :title="adjustMode === 'REASSIGN' ? lt('liveGame.review.reassignTitle') : lt('liveGame.review.voidTitle')"
      width="400px"
      append-to-body
      destroy-on-close
    >
      <el-form :label-width="isEnglish ? '96px' : '82px'" size="default">
        <el-form-item :label="lt('liveGame.review.runner')">
          {{ adjustRow?.runnerName }} ({{ lt('liveGame.review.inning') }}{{ adjustRow?.inning }}{{ halfLabel(adjustRow?.half) }}#{{ adjustRow?.sequence }})
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.currentPitcher')">
          {{ adjustRow?.effectivePitcherName || adjustRow?.pitcherName || '—' }}
        </el-form-item>
        <el-form-item v-if="adjustMode === 'REASSIGN'" :label="lt('liveGame.review.reassignTo')">
          <el-select
            v-model="adjustTargetId"
            filterable
            :placeholder="lt('liveGame.review.selectPitcher')"
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
        <el-form-item :label="lt('liveGame.review.reasonLabel')">
          <el-input
            v-model="adjustReason"
            maxlength="200"
            show-word-limit
            :placeholder="lt('liveGame.review.reasonPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <p v-if="adjustMode === 'VOID'" class="pitcher-warn">
        {{ lt('liveGame.review.voidHint') }}
      </p>
      <template #footer>
        <el-button @click="adjustVisible = false">{{ lt('liveGame.common.cancel') }}</el-button>
        <el-button type="primary" :loading="adjustSubmitting" @click="submitAdjust">
          {{ adjustMode === 'REASSIGN' ? lt('liveGame.review.confirmReassign') : lt('liveGame.review.confirmVoid') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 覆写弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="lt('liveGame.review.overrideTitle')"
      width="360px"
      append-to-body
      destroy-on-close
    >
      <el-form :label-width="isEnglish ? '96px' : '80px'" size="default">
        <el-form-item :label="lt('liveGame.review.runner')">
          {{ editRow?.runnerName }} ({{ lt('liveGame.review.inning') }}{{ editRow?.inning }}{{ halfLabel(editRow?.half) }}#{{ editRow?.sequence }})
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.currentDecision')">
          <el-tag :type="statusTagType(editRow?.earnedStatus)" size="small">{{ statusLabel(editRow?.earnedStatus) }}</el-tag>
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.newDecision')">
          <el-radio-group v-model="editForm.earnedStatus">
            <el-radio value="EARNED">{{ lt('liveGame.review.earned') }}</el-radio>
            <el-radio value="UNEARNED">{{ lt('liveGame.review.unearnedOption') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editForm.earnedStatus === 'UNEARNED'" :label="lt('liveGame.review.unearnedReason')">
          <el-select v-model="editForm.unearnedReason" :placeholder="lt('liveGame.review.selectPlaceholder')" style="width:100%">
            <el-option v-for="r in UNEARNED_REASONS" :key="r.value" :label="lt(r.key)" :value="r.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ lt('liveGame.common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOverride">{{ lt('liveGame.review.confirmOverride') }}</el-button>
      </template>
    </el-dialog>

    <!-- 投手 R/ER 覆写弹窗 -->
    <el-dialog
      v-model="pitcherEditVisible"
      :title="lt('liveGame.review.pitcherOverrideTitle')"
      width="380px"
      append-to-body
      destroy-on-close
    >
      <el-form :label-width="isEnglish ? '104px' : '86px'" size="default">
        <el-form-item :label="lt('liveGame.review.pitcher')">
          {{ pitcherEditRow?.playerName || `#${pitcherEditRow?.playerId}` }}
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.currentValue')">
          R {{ pitcherEditRow?.runsAllowed ?? 0 }} / ER {{ pitcherEditRow?.earnedRuns ?? 0 }}
          <span v-if="pitcherEditRow?.overrideId" class="pitcher-engine">
            {{ lt('liveGame.review.engineValue', { r: pitcherEditRow?.engineRunsAllowed ?? 0, er: pitcherEditRow?.engineEarnedRuns ?? 0 }) }}
          </span>
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.newR')">
          <el-input-number
            v-model="pitcherEditForm.runsAllowed"
            :min="0"
            :max="99"
            :precision="0"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.newER')">
          <el-input-number
            v-model="pitcherEditForm.earnedRuns"
            :min="0"
            :max="99"
            :precision="0"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="lt('liveGame.review.derivedUnearned')">{{ pitcherDerivedUnearned }}</el-form-item>
        <el-form-item :label="lt('liveGame.review.note')">
          <el-input v-model="pitcherEditForm.note" maxlength="200" :placeholder="lt('liveGame.review.optional')" />
        </el-form-item>
      </el-form>
      <p class="pitcher-warn">
        {{ lt('liveGame.review.pitcherSaveHint') }}
      </p>
      <template #footer>
        <el-button @click="pitcherEditVisible = false">{{ lt('liveGame.common.cancel') }}</el-button>
        <el-button type="primary" :loading="pitcherSubmitting" @click="submitPitcherOverride">
          {{ lt('liveGame.review.saveOverride') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { gameApi } from '../../api/business';
import { useLiveGameText } from '../../composables/useLiveGameText';

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
const { text: lt, isEnglish } = useLiveGameText();
const activeTab = ref('er');
const remoteGameStatus = ref('');
const resolvedGameStatus = computed(() => String(props.gameStatus || remoteGameStatus.value || '').trim());
const isFinal = computed(() => resolvedGameStatus.value.toLowerCase() === 'final');

const UNEARNED_REASONS = [
  { value: 'ERROR_REACHED_BASE', key: 'liveGame.review.reasonErrorReached' },
  { value: 'ERROR_EXTENDED_INNING', key: 'liveGame.review.reasonErrorExtended' },
  { value: 'PASSED_BALL', key: 'liveGame.review.reasonPassedBall' },
  { value: 'INTERFERENCE', key: 'liveGame.review.reasonInterference' },
  { value: 'TIE_BREAK_RUNNER', key: 'liveGame.review.reasonTieBreak' },
  { value: 'MANUAL_OVERRIDE', key: 'liveGame.review.reasonManual' },
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
  const homeShort = lt('liveGame.team.homeShort');
  const awayShort = lt('liveGame.team.awayShort');
  const mark = (list, label) => {
    for (const player of Array.isArray(list) ? list : []) {
      const id = player?.playerId ?? player?.id;
      if (id != null) labels.set(String(id), label);
    }
  };
  mark(props.homeLineup, homeShort);
  mark(props.homeBench, homeShort);
  mark(props.homeRoster, homeShort);
  mark(props.awayLineup, awayShort);
  mark(props.awayBench, awayShort);
  mark(props.awayRoster, awayShort);
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
      ElMessage.error(lt('liveGame.review.loadPitchersFailed'));
      console.error(result.error);
      return;
    }
    pitcherRows.value = Array.isArray(result?.data) ? result.data : [];
    pitcherLoaded.value = true;
  } catch (e) {
    pitcherRows.value = [];
    ElMessage.error(lt('liveGame.review.loadPitchersFailed'));
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
      ElMessage.error(lt('liveGame.review.loadDecisionsFailed'));
      console.error(result.error);
      return;
    }
    rows.value = Array.isArray(result?.data) ? result.data : [];
  } catch (e) {
    rows.value = [];
    ElMessage.error(lt('liveGame.review.loadDecisionsFailed'));
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
    ElMessage.warning(lt('liveGame.review.selectUnearnedReason'));
    return;
  }
  submitting.value = true;
  try {
    await gameApi.overrideEarnedRunDecision(props.gameId, editRow.value.decisionId, {
      earnedStatus,
      unearnedReason: earnedStatus === 'UNEARNED' ? unearnedReason : null,
    });
    ElMessage.success(lt('liveGame.review.overrideSuccess'));
    editVisible.value = false;
    load();
    loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(lt('liveGame.review.overrideFailed', { msg: e?.message || '' }));
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
    ElMessage.warning(lt('liveGame.review.rErInteger'));
    return;
  }
  if (earnedRuns > runsAllowed) {
    ElMessage.warning(lt('liveGame.review.erNotGreater'));
    return;
  }
  pitcherSubmitting.value = true;
  try {
    await gameApi.overridePitcherRuns(props.gameId, pitcherEditRow.value.playerId, {
      runsAllowed,
      earnedRuns,
      note: pitcherEditForm.value.note?.trim() || null,
    });
    ElMessage.success(lt('liveGame.review.overrideSaved'));
    pitcherEditVisible.value = false;
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(lt('liveGame.review.saveFailed', { msg: e?.message || '' }));
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
      lt('liveGame.review.undoConfirm', { name, r: engineR, er: engineEr }),
      lt('liveGame.review.undoTitle'),
      { type: 'warning', confirmButtonText: lt('liveGame.review.undoConfirmBtn'), cancelButtonText: lt('liveGame.common.cancel') }
    );
  } catch (e) {
    return;
  }
  try {
    await gameApi.revokePitcherRunOverride(props.gameId, row.overrideId);
    ElMessage.success(lt('liveGame.review.undoSuccess'));
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(lt('liveGame.review.undoFailed', { msg: e?.message || '' }));
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
  const number = pitcher.number ? lt('liveGame.review.numberPrefix', { n: pitcher.number }) : '';
  return `${number}${pitcher.playerName || `#${pitcher.playerId}`}`;
}

async function submitAdjust() {
  const row = adjustRow.value;
  if (!row?.decisionId) return;
  if (adjustMode.value === 'REASSIGN' && !adjustTargetId.value) {
    ElMessage.warning(lt('liveGame.review.selectReassignPitcher'));
    return;
  }
  adjustSubmitting.value = true;
  try {
    if (adjustMode.value === 'REASSIGN') {
      await gameApi.reassignEarnedRunDecision(props.gameId, row.decisionId, {
        targetPitcherId: adjustTargetId.value,
        reason: adjustReason.value.trim() || null
      });
      ElMessage.success(lt('liveGame.review.reassignSaved'));
    } else {
      await gameApi.voidEarnedRunDecision(props.gameId, row.decisionId, {
        reason: adjustReason.value.trim() || null
      });
      ElMessage.success(lt('liveGame.review.voidSaved'));
    }
    adjustVisible.value = false;
    await load();
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(e?.message || lt('liveGame.review.saveError'));
  } finally {
    adjustSubmitting.value = false;
  }
}

async function restoreAdjustment(row) {
  if (!isFinal.value || !row?.adjustmentId) return;
  try {
    await ElMessageBox.confirm(
      lt('liveGame.review.restoreConfirm', { name: row.runnerName || lt('liveGame.review.runner') }),
      lt('liveGame.review.restoreTitle'),
      { type: 'warning', confirmButtonText: lt('liveGame.review.restoreConfirmBtn'), cancelButtonText: lt('liveGame.common.cancel') }
    );
  } catch {
    return;
  }
  try {
    await gameApi.revokeEarnedRunDecisionAdjustment(props.gameId, row.adjustmentId);
    ElMessage.success(lt('liveGame.review.restoreSuccess'));
    await load();
    await loadPitcherTotals();
    emit('changed');
  } catch (e) {
    ElMessage.error(e?.message || lt('liveGame.review.restoreFailed'));
  }
}

// ─── helpers ──────────────────────────────────────────────
function halfLabel(half) {
  return half === 'top' ? lt('liveGame.review.halfTop') : lt('liveGame.review.halfBottom');
}
function statusTagType(s) {
  if (s === 'EARNED') return 'success';
  if (s === 'UNEARNED') return 'warning';
  return 'info';
}
function statusLabel(s) {
  if (s === 'EARNED') return lt('liveGame.review.decisionEarned');
  if (s === 'UNEARNED') return lt('liveGame.review.decisionUnearned');
  if (s === 'PENDING') return lt('liveGame.review.decisionPending');
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
