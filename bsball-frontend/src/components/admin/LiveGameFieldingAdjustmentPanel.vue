<template>
  <div v-loading="loading" class="fielding-adjustment">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      :title="lt('liveGame.review.fieldingBlockedTitle')"
      :description="lt('liveGame.review.fieldingBlockedDesc')"
    />
    <el-alert
      v-else
      type="success"
      :closable="false"
      show-icon
      :title="lt('liveGame.review.fieldingFinalTitle')"
      :description="lt('liveGame.review.fieldingFinalDesc')"
    />
    <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" />

    <template v-if="isFinal">
      <div class="fielding-adjustment__section">
        <div class="fielding-adjustment__title">
          {{ lt('liveGame.review.pendingErrorPlays') }}
          <span class="fielding-adjustment__count">{{ candidates.length }}</span>
        </div>
        <el-empty v-if="!candidates.length" :image-size="64" :description="lt('liveGame.review.noErrorPlays')" />
        <div v-for="candidate in candidates" :key="candidate.sourceEventId" class="fielding-adjustment__play">
          <div class="fielding-adjustment__play-head">
            <div>
              <b>{{ candidateInningLabel(candidate) }}</b>
              <span class="fielding-adjustment__meta">{{ lt('liveGame.review.outsBefore', { n: candidate.outsBefore }) }} · {{ candidateBaseLabel(candidate) }}</span>
            </div>
            <el-button
              type="primary"
              size="small"
              plain
              :disabled="!hasAvailablePlayers(candidate)"
              @click="openAdjust(candidate)"
            >{{ lt('liveGame.review.creditAssist') }}</el-button>
          </div>
          <div class="fielding-adjustment__play-text">{{ candidate.resultText || '—' }}</div>
          <div v-if="candidate.adjustments.length" class="fielding-adjustment__credited">
            <span class="fielding-adjustment__credited-label">{{ lt('liveGame.review.credited') }}</span>
            <el-tag
              v-for="item in candidate.adjustments"
              :key="item.adjustmentId"
              size="small"
              :type="statusTagType(item.status)"
              effect="plain"
            >{{ playerName(item.playerId) }} · {{ item.position }} · {{ statusLabel(item.status) }}</el-tag>
          </div>
        </div>
      </div>

      <div class="fielding-adjustment__section">
        <div class="fielding-adjustment__title">{{ lt('liveGame.review.creditsTitle') }}</div>
        <el-table :data="rows" size="small" border stripe :empty-text="lt('liveGame.review.noCredits')">
          <el-table-column :label="lt('liveGame.review.inning')" width="82">
            <template #default="{ row }">{{ inningLabel(row) }}</template>
          </el-table-column>
          <el-table-column :label="lt('liveGame.review.fielder')" min-width="90">
            <template #default="{ row }">{{ row.playerName || playerName(row.playerId) }}</template>
          </el-table-column>
          <el-table-column :label="lt('liveGame.review.position')" prop="position" width="56" align="center" />
          <el-table-column :label="lt('liveGame.review.credit')" width="86" align="center">
            <template #default="{ row }">A +{{ row.assistCount || 1 }}</template>
          </el-table-column>
          <el-table-column :label="lt('liveGame.review.status')" width="78" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)" effect="plain">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="lt('liveGame.review.actions')" width="76" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'APPROVED'"
                link
                type="danger"
                size="small"
                @click="revoke(row)"
              >{{ lt('liveGame.review.undo') }}</el-button>
              <span v-else class="fielding-adjustment__muted">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-dialog
      v-model="dialogVisible"
      :title="lt('liveGame.review.creditAssist')"
      width="420px"
      append-to-body
      destroy-on-close
    >
      <template v-if="selectedCandidate">
        <div class="fielding-adjustment__dialog-context">
          <div><b>{{ candidateInningLabel(selectedCandidate) }}</b> · {{ lt('liveGame.review.outsBefore', { n: selectedCandidate.outsBefore }) }} · {{ candidateBaseLabel(selectedCandidate) }}</div>
          <div class="fielding-adjustment__dialog-text">{{ selectedCandidate.resultText || '—' }}</div>
        </div>
        <el-form :label-width="isEnglish ? '96px' : '82px'" size="default">
          <el-form-item :label="lt('liveGame.review.fielder')">
            <el-select
              v-model="selectedPlayerId"
              filterable
              :placeholder="lt('liveGame.review.selectFielder')"
              style="width: 100%"
              @change="handlePlayerChange"
            >
              <el-option
                v-for="player in selectedAvailablePlayers"
                :key="player.id"
                :label="playerOptionLabel(player)"
                :value="player.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="lt('liveGame.review.positionLabel')">
            <el-select v-model="selectedPosition" :placeholder="lt('liveGame.review.selectPosition')" style="width: 100%">
              <el-option v-for="position in positionOptions" :key="position" :label="position" :value="position" />
            </el-select>
          </el-form-item>
          <el-form-item :label="lt('liveGame.review.note')">
            <el-input v-model="note" maxlength="200" show-word-limit :placeholder="lt('liveGame.review.notePlaceholder')" />
          </el-form-item>
        </el-form>
        <div v-if="selectedPlayer" class="fielding-adjustment__preview">
          <b>{{ selectedPlayer.name || playerName(selectedPlayer.id) }}</b>{{ lt('liveGame.review.effectPlayerSuffix') }}
          {{ lt('liveGame.review.effectLine', { pos: selectedPosition || '—' }) }}
          <div class="fielding-adjustment__muted">{{ lt('liveGame.review.effectHint') }}</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ lt('liveGame.common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">{{ lt('liveGame.review.confirmCredit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { gameApi } from '../../api/business';
import { useLiveGameText } from '../../composables/useLiveGameText';
import {
  FIELDING_ADJUSTMENT_POSITIONS,
  buildFieldingAdjustmentCandidates,
  playerPositionOptions
} from '../../utils/liveGameFieldingAdjustment';

const props = defineProps({
  visible: { type: Boolean, default: false },
  gameId: { type: [Number, String], default: null },
  gameStatus: { type: String, default: '' },
  playEvents: { type: Array, default: () => [] },
  homeLineup: { type: Array, default: () => [] },
  awayLineup: { type: Array, default: () => [] },
  homeBench: { type: Array, default: () => [] },
  awayBench: { type: Array, default: () => [] },
  homeFieldingPitcherRow: { type: Object, default: null },
  awayFieldingPitcherRow: { type: Object, default: null },
  homeRoster: { type: Array, default: () => [] },
  awayRoster: { type: Array, default: () => [] },
  remoteCandidates: { type: Boolean, default: false }
});
const emit = defineEmits(['changed']);
const { text: lt, isEnglish } = useLiveGameText();

const rows = ref([]);
const remoteCandidateRows = ref([]);
const loading = ref(false);
const loadError = ref('');
const dialogVisible = ref(false);
const selectedCandidate = ref(null);
const selectedPlayerId = ref(null);
const selectedPosition = ref('');
const note = ref('');
const submitting = ref(false);

const isFinal = computed(() => String(props.gameStatus ?? '').toLowerCase() === 'final');
const candidates = computed(() => {
  if (!props.remoteCandidates) {
    return buildFieldingAdjustmentCandidates({
      playEvents: props.playEvents,
      homeLineup: props.homeLineup,
      awayLineup: props.awayLineup,
      homeBench: props.homeBench,
      awayBench: props.awayBench,
      homeFieldingPitcherRow: props.homeFieldingPitcherRow,
      awayFieldingPitcherRow: props.awayFieldingPitcherRow,
      homeRoster: props.homeRoster,
      awayRoster: props.awayRoster,
      adjustments: rows.value
    });
  }
  const bySource = new Map();
  for (const row of rows.value) {
    const key = String(row?.sourceEventId ?? '');
    if (!key) continue;
    if (!bySource.has(key)) bySource.set(key, []);
    bySource.get(key).push(row);
  }
  return remoteCandidateRows.value.map((candidate) => ({
    ...candidate,
    adjustments: bySource.get(String(candidate.sourceEventId)) ?? []
  }));
});
const selectedPlayer = computed(() => {
  if (!selectedPlayerId.value) return null;
  return selectedCandidate.value?.players.find(
    (player) => Number(player.id) === Number(selectedPlayerId.value)
  ) ?? null;
});
const selectedAvailablePlayers = computed(() => {
  const candidate = selectedCandidate.value;
  if (!candidate) return [];
  const credited = creditedPlayerIds(candidate);
  return candidate.players.filter((player) => !credited.has(Number(player.id)));
});
const positionOptions = computed(() => {
  const options = playerPositionOptions(selectedPlayer.value);
  return options.length ? options : FIELDING_ADJUSTMENT_POSITIONS;
});

watch(
  () => [props.visible, props.gameId],
  ([visible]) => { if (visible) load(); },
  { immediate: true }
);
watch(isFinal, (final) => {
  if (final) load();
  else {
    rows.value = [];
    remoteCandidateRows.value = [];
  }
});

async function load() {
  if (!props.gameId) return;
  if (!isFinal.value) {
    rows.value = [];
    remoteCandidateRows.value = [];
    loadError.value = '';
    return;
  }
  loading.value = true;
  loadError.value = '';
  try {
    const [result, candidateResult] = await Promise.all([
      gameApi.listFieldingAdjustments(props.gameId),
      props.remoteCandidates
        ? gameApi.listFieldingAdjustmentCandidates(props.gameId)
        : null
    ]);
    if (result?.error) {
      rows.value = [];
      remoteCandidateRows.value = [];
      loadError.value = result.error;
      ElMessage.error(lt('liveGame.review.loadFieldingFailed'));
      return;
    }
    rows.value = Array.isArray(result?.data) ? result.data : [];
    if (!props.remoteCandidates) {
      remoteCandidateRows.value = [];
      return;
    }
    if (candidateResult?.error) {
      remoteCandidateRows.value = [];
      loadError.value = candidateResult.error;
      ElMessage.error(lt('liveGame.review.loadCandidatesFailed'));
      return;
    }
    remoteCandidateRows.value = Array.isArray(candidateResult?.data) ? candidateResult.data : [];
  } catch (error) {
    rows.value = [];
    remoteCandidateRows.value = [];
    loadError.value = error?.message ?? lt('liveGame.review.loadFailed');
    ElMessage.error(lt('liveGame.review.loadFieldingFailed'));
  } finally {
    loading.value = false;
  }
}

function openAdjust(candidate) {
  if (!isFinal.value) return;
  selectedCandidate.value = candidate;
  selectedPlayerId.value = null;
  selectedPosition.value = '';
  note.value = '';
  dialogVisible.value = true;
}

function hasAvailablePlayers(candidate) {
  const credited = creditedPlayerIds(candidate);
  return (candidate?.players ?? []).some((player) => !credited.has(Number(player.id)));
}

function creditedPlayerIds(candidate) {
  return new Set(
    (candidate?.adjustments ?? [])
      .filter((item) => item.status === 'APPROVED')
      .map((item) => Number(item.playerId))
  );
}

function handlePlayerChange() {
  const options = playerPositionOptions(selectedPlayer.value);
  selectedPosition.value = options[0] ?? '';
}

async function submit() {
  if (!selectedCandidate.value) return;
  if (!selectedPlayerId.value) {
    ElMessage.warning(lt('liveGame.review.selectCreditFielder'));
    return;
  }
  if (!selectedPosition.value) {
    ElMessage.warning(lt('liveGame.review.selectPositionRequired'));
    return;
  }
  submitting.value = true;
  try {
    await gameApi.createFieldingAdjustment(props.gameId, {
      sourceEventId: selectedCandidate.value.sourceEventId,
      playerId: Number(selectedPlayerId.value),
      position: selectedPosition.value,
      note: note.value.trim() || undefined
    });
    ElMessage.success(lt('liveGame.review.creditSaved', { pos: selectedPosition.value }));
    dialogVisible.value = false;
    await load();
    emit('changed');
  } catch (error) {
    ElMessage.error(error?.message ?? lt('liveGame.review.creditFailed'));
  } finally {
    submitting.value = false;
  }
}

async function revoke(row) {
  try {
    await ElMessageBox.confirm(
      lt('liveGame.review.fieldingUndoConfirm', { name: row.playerName || playerName(row.playerId) }),
      lt('liveGame.review.fieldingUndoTitle'),
      { type: 'warning', confirmButtonText: lt('liveGame.review.undoConfirmBtn'), cancelButtonText: lt('liveGame.common.cancel') }
    );
  } catch {
    return;
  }
  try {
    await gameApi.revokeFieldingAdjustment(props.gameId, row.adjustmentId);
    ElMessage.success(lt('liveGame.review.fieldingUndoSuccess'));
    await load();
    emit('changed');
  } catch (error) {
    ElMessage.error(error?.message ?? lt('liveGame.review.fieldingUndoFailed'));
  }
}

function playerOptionLabel(player) {
  if (!player) return '';
  const number = player.number ? `#${player.number}` : `#${player.id}`;
  const positions = playerPositionOptions(player).join('/');
  return `${number} ${player.name ?? ''}${positions ? ` · ${positions}` : ''}`.trim();
}

function playerName(playerId) {
  if (!playerId) return '—';
  const all = [
    ...props.homeLineup, ...props.awayLineup,
    ...props.homeBench, ...props.awayBench,
    props.homeFieldingPitcherRow, props.awayFieldingPitcherRow,
    ...props.homeRoster, ...props.awayRoster
  ].filter(Boolean);
  const player = all.find((item) => Number(item.id) === Number(playerId));
  return player?.name ?? `#${playerId}`;
}

function inningLabel(row) {
  return `${row.inning ?? '-'}${row.half === 'top' ? lt('liveGame.review.halfTop') : lt('liveGame.review.halfBottom')}#${row.sequence ?? '-'}`;
}

function candidateInningLabel(candidate) {
  if (candidate?.inning == null) return candidate?.inningLabel ?? '';
  const half = candidate.half === 'top' ? lt('liveGame.review.halfTop') : lt('liveGame.review.halfBottom');
  return `${candidate.inning}${half} #${candidate.sequence ?? '-'}`;
}

function candidateBaseLabel(candidate) {
  if (!candidate?.basesBefore) return candidate?.baseLabel ?? '';
  const occupied = [1, 2, 3].filter((base) => candidate.basesBefore?.[base]);
  if (occupied.length === 0) return lt('liveGame.review.basesEmpty');
  if (occupied.length === 3) return lt('liveGame.review.basesFull');
  const names = occupied.map((base) => lt(`liveGame.scoreboard.base${base}`));
  if (names.length === 1) return lt('liveGame.review.baseOccupied', { base: names[0] });
  return lt('liveGame.review.basesOccupied', { bases: names.join(lt('liveGame.common.listSeparator')) });
}

function statusLabel(status) {
  if (status === 'APPROVED') return lt('liveGame.review.statusApproved');
  if (status === 'REVOKED') return lt('liveGame.review.statusRevoked');
  if (status === 'INVALIDATED') return lt('liveGame.review.statusInvalidated');
  return status || '—';
}

function statusTagType(status) {
  if (status === 'APPROVED') return 'success';
  if (status === 'INVALIDATED') return 'danger';
  return 'info';
}
</script>

<style scoped>
.fielding-adjustment {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.fielding-adjustment__section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.fielding-adjustment__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.fielding-adjustment__count {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}
.fielding-adjustment__play {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}
.fielding-adjustment__play-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.fielding-adjustment__meta {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.fielding-adjustment__play-text {
  margin-top: 6px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.45;
}
.fielding-adjustment__credited {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}
.fielding-adjustment__credited-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.fielding-adjustment__muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.fielding-adjustment__dialog-context {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 13px;
}
.fielding-adjustment__dialog-text {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
}
.fielding-adjustment__preview {
  padding: 10px 12px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.55;
}
</style>
