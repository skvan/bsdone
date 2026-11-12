<template>
  <div v-loading="loading" class="fielding-adjustment">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      title="比赛结束后才能补记守备助杀"
      description="进行中的比赛请先结束并保存赛果；赛后复核只处理失误导致未完成的出局。"
    />
    <el-alert
      v-else
      type="success"
      :closable="false"
      show-icon
      title="赛后复核"
      description="补记立即生效；源 Play 已不在当前快照时会自动失效并扣回。"
    />
    <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" />

    <template v-if="isFinal">
      <div class="fielding-adjustment__section">
        <div class="fielding-adjustment__title">
          待复核的失误 Play
          <span class="fielding-adjustment__count">{{ candidates.length }}</span>
        </div>
        <el-empty v-if="!candidates.length" :image-size="64" description="本场没有带失误的 Play" />
        <div v-for="candidate in candidates" :key="candidate.sourceEventId" class="fielding-adjustment__play">
          <div class="fielding-adjustment__play-head">
            <div>
              <b>{{ candidate.inningLabel }}</b>
              <span class="fielding-adjustment__meta">{{ candidate.outsBefore }}出局 · {{ candidate.baseLabel }}</span>
            </div>
            <el-button
              type="primary"
              size="small"
              plain
              :disabled="!hasAvailablePlayers(candidate)"
              @click="openAdjust(candidate)"
            >补记助杀</el-button>
          </div>
          <div class="fielding-adjustment__play-text">{{ candidate.resultText || '—' }}</div>
          <div v-if="candidate.adjustments.length" class="fielding-adjustment__credited">
            <span class="fielding-adjustment__credited-label">已处理：</span>
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
        <div class="fielding-adjustment__title">补记记录</div>
        <el-table :data="rows" size="small" border stripe empty-text="暂无补记记录">
          <el-table-column label="局" width="82">
            <template #default="{ row }">{{ inningLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="守备员" min-width="90">
            <template #default="{ row }">{{ row.playerName || playerName(row.playerId) }}</template>
          </el-table-column>
          <el-table-column label="位置" prop="position" width="56" align="center" />
          <el-table-column label="补记" width="86" align="center">
            <template #default="{ row }">A +{{ row.assistCount || 1 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="78" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)" effect="plain">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="76" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'APPROVED'"
                link
                type="danger"
                size="small"
                @click="revoke(row)"
              >撤销</el-button>
              <span v-else class="fielding-adjustment__muted">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-dialog
      v-model="dialogVisible"
      title="补记助杀"
      width="420px"
      append-to-body
      destroy-on-close
    >
      <template v-if="selectedCandidate">
        <div class="fielding-adjustment__dialog-context">
          <div><b>{{ selectedCandidate.inningLabel }}</b> · {{ selectedCandidate.outsBefore }}出局 · {{ selectedCandidate.baseLabel }}</div>
          <div class="fielding-adjustment__dialog-text">{{ selectedCandidate.resultText || '—' }}</div>
        </div>
        <el-form label-width="82px" size="default">
          <el-form-item label="守备员">
            <el-select
              v-model="selectedPlayerId"
              filterable
              placeholder="选择补记助杀的守备员"
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
          <el-form-item label="守位">
            <el-select v-model="selectedPosition" placeholder="选择守位" style="width: 100%">
              <el-option v-for="position in positionOptions" :key="position" :label="position" :value="position" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="note" maxlength="200" show-word-limit placeholder="可空，例如：外野传本垒，捕手接球失误" />
          </el-form-item>
        </el-form>
        <div v-if="selectedPlayer" class="fielding-adjustment__preview">
          <b>{{ selectedPlayer.name || playerName(selectedPlayer.id) }}</b>：A +1、TC +1；
          位置 <b>{{ selectedPosition || '—' }}</b> A +1、TC +1；全队 A +1。
          <div class="fielding-adjustment__muted">确认后立即生效；不会改变 R/ER。</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确认补记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { gameApi } from '../../api/business';
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
      ElMessage.error('加载守备补记失败');
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
      ElMessage.error('加载补记候选失败');
      return;
    }
    remoteCandidateRows.value = Array.isArray(candidateResult?.data) ? candidateResult.data : [];
  } catch (error) {
    rows.value = [];
    remoteCandidateRows.value = [];
    loadError.value = error?.message ?? '加载失败';
    ElMessage.error('加载守备补记失败');
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
    ElMessage.warning('请选择补记助杀的守备员');
    return;
  }
  if (!selectedPosition.value) {
    ElMessage.warning('请选择守位');
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
    ElMessage.success(`补记已生效：${selectedPosition.value} A +1`);
    dialogVisible.value = false;
    await load();
    emit('changed');
  } catch (error) {
    ElMessage.error(error?.message ?? '补记失败');
  } finally {
    submitting.value = false;
  }
}

async function revoke(row) {
  try {
    await ElMessageBox.confirm(
      `撤销 ${row.playerName || playerName(row.playerId)} 的这次补记？撤销后将扣回 A/TC。`,
      '撤销补记',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' }
    );
  } catch {
    return;
  }
  try {
    await gameApi.revokeFieldingAdjustment(props.gameId, row.adjustmentId);
    ElMessage.success('已撤销补记，统计同步扣回');
    await load();
    emit('changed');
  } catch (error) {
    ElMessage.error(error?.message ?? '撤销失败');
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
  return `${row.inning ?? '-'}${row.half === 'top' ? '上' : '下'}#${row.sequence ?? '-'}`;
}

function statusLabel(status) {
  if (status === 'APPROVED') return '已生效';
  if (status === 'REVOKED') return '已撤销';
  if (status === 'INVALIDATED') return '已失效';
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
