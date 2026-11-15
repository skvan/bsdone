<template>
  <div class="identity-transfer">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      :title="lt('liveGame.revision.identityFinalGate')"
    />
    <template v-else>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="lt('liveGame.revision.identityTitle')"
        :description="lt('liveGame.revision.identityDesc')"
      />
      <el-form :label-width="isEnglish ? '112px' : '82px'" size="default" class="identity-transfer__form">
        <el-form-item :label="lt('liveGame.revision.sourcePlayer')">
          <el-select v-model="sourcePlayerId" filterable :placeholder="lt('liveGame.revision.sourcePlaceholder')" style="width: 100%">
            <el-option
              v-for="player in playerOptions"
              :key="player.playerId"
              :label="player.label"
              :value="player.playerId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('liveGame.revision.targetPlayer')">
          <el-select
            v-model="targetPlayerId"
            filterable
            :placeholder="lt('liveGame.revision.targetPlaceholder')"
            :disabled="!sourceTeamId"
            style="width: 100%"
          >
            <el-option
              v-for="player in targetOptions"
              :key="player.playerId"
              :label="player.label"
              :value="player.playerId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('liveGame.revision.scopes')">
          <el-checkbox-group v-model="scopes">
            <el-checkbox label="STATS">{{ lt('liveGame.revision.scopeStats') }}</el-checkbox>
            <el-checkbox label="FIELDING">{{ lt('liveGame.revision.scopeFieldingPositions') }}</el-checkbox>
            <el-checkbox label="ER_DECISIONS">{{ lt('liveGame.revision.scopeEr') }}</el-checkbox>
            <el-checkbox
              label="LIVE_RECORDS"
              :disabled="Boolean(preview) && (!preview.liveSnapshotAvailable || preview.liveRosterConflict)"
            >{{ lt('liveGame.revision.scopeLive') }}</el-checkbox>
          </el-checkbox-group>
          <div v-if="!scopes.length" class="identity-transfer__hint">{{ lt('liveGame.revision.scopeRequired') }}</div>
          <div v-else-if="preview?.liveRosterConflict" class="identity-transfer__hint">
            {{ lt('liveGame.revision.liveConflict') }}
          </div>
          <div v-else-if="scopes.includes('LIVE_RECORDS')" class="identity-transfer__hint is-info">
            {{ lt('liveGame.revision.liveHint') }}
          </div>
        </el-form-item>
        <el-form-item :label="lt('liveGame.revision.note')">
          <el-input v-model="note" maxlength="200" show-word-limit :placeholder="lt('liveGame.revision.notePlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="previewLoading" :disabled="!canPreview" @click="loadPreview">
            {{ lt('liveGame.revision.previewBtn') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="previewError" type="error" :closable="false" show-icon :title="previewError" />
      <template v-if="preview">
        <el-alert
          v-if="!preview.transferable"
          type="error"
          :closable="false"
          show-icon
          :title="lt('liveGame.revision.cannotTransfer')"
          :description="(preview.blockers || []).join(lt('liveGame.revision.blockerSeparator'))"
        />
        <template v-else>
          <el-descriptions :column="2" border size="small" class="identity-transfer__summary">
            <el-descriptions-item :label="lt('liveGame.revision.source')">{{ preview.sourcePlayerName }}</el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.target')">{{ preview.targetPlayerName }}</el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.scopeEr')">{{ lt('liveGame.revision.erCount', { count: preview.erDecisionCount }) }}</el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.fieldingPositions')">
              {{ (preview.fieldingPositions || []).join(lt('liveGame.common.listSeparator')) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.sourceOverride')">{{ preview.sourceOverride ? lt('liveGame.revision.yes') : lt('liveGame.revision.no') }}</el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.targetOverride')">{{ preview.targetOverride ? lt('liveGame.revision.yes') : lt('liveGame.revision.no') }}</el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.liveReference')">
              {{ preview.liveSnapshotAvailable ? lt('liveGame.revision.liveRefCount', { count: preview.liveReferenceCount || 0 }) : lt('liveGame.revision.noLive') }}
            </el-descriptions-item>
            <el-descriptions-item :label="lt('liveGame.revision.fieldingCredits')">
              {{ lt('liveGame.revision.countUnit', { count: preview.fieldingAdjustmentCount || 0 }) }}
            </el-descriptions-item>
          </el-descriptions>
          <el-table :data="preview.rows || []" size="small" border stripe class="identity-transfer__rows">
            <el-table-column :label="lt('liveGame.revision.rowType')" width="90" align="center">
              <template #default="{ row }">{{ row.isPitcher === 1 ? lt('liveGame.revision.pitcherRow') : lt('liveGame.revision.batterRow') }}</template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.revision.sourceRow')" width="100" align="center">
              <template #default="{ row }">#{{ row.sourceStatId }}</template>
            </el-table-column>
            <el-table-column :label="lt('liveGame.revision.targetRow')" min-width="120" align="center">
              <template #default="{ row }">{{ row.targetExists ? `#${row.targetStatId}` : lt('liveGame.revision.newOrMove') }}</template>
            </el-table-column>
          </el-table>
          <div class="identity-transfer__actions">
            <el-button
              type="primary"
              :loading="transferring"
              :disabled="!canTransfer"
              @click="confirmTransfer"
            >{{ lt('liveGame.revision.confirmTransfer') }}</el-button>
          </div>
        </template>
      </template>

      <el-alert
        v-if="pendingTransfer"
        type="warning"
        :closable="false"
        show-icon
        :title="lt('liveGame.revision.pendingTitle')"
        :description="`${pendingTransfer.sourceLabel} → ${pendingTransfer.targetLabel}`"
      >
        <template #default>
          <el-button size="small" type="warning" @click="deletePendingSource">{{ lt('liveGame.revision.deleteSource') }}</el-button>
        </template>
      </el-alert>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { gameApi, playerApi } from '../../api/business';
import { useLiveGameText } from '../../composables/useLiveGameText';

const { text: lt, isEnglish } = useLiveGameText();
const props = defineProps({
  gameId: { type: [Number, String], default: null },
  gameStatus: { type: String, default: '' },
  refreshKey: { type: Number, default: 0 }
});
const emit = defineEmits(['changed']);

const isFinal = computed(() => String(props.gameStatus || '').toLowerCase() === 'final');
const playerOptions = ref([]);
const sourcePlayerId = ref(null);
const targetPlayerId = ref(null);
const scopes = ref(['STATS', 'FIELDING', 'ER_DECISIONS']);
const note = ref('');
const preview = ref(null);
const previewError = ref('');
const previewLoading = ref(false);
const transferring = ref(false);
const pendingTransfer = ref(null);

const sourceTeamId = computed(() => {
  const player = playerOptions.value.find(
    (option) => String(option.playerId) === String(sourcePlayerId.value)
  );
  return player?.teamId ?? null;
});
const targetOptions = computed(() => playerOptions.value.filter(
  (player) => String(player.teamId) === String(sourceTeamId.value)
    && String(player.playerId) !== String(sourcePlayerId.value)
));
const canPreview = computed(() => Boolean(sourcePlayerId.value && targetPlayerId.value));
const canTransfer = computed(() => Boolean(
  preview.value?.transferable && scopes.value.length
));

async function loadPlayers() {
  if (!props.gameId) return;
  try {
    const game = await gameApi.get(props.gameId);
    const teams = [
      { id: game?.homeTeamId, label: lt('liveGame.team.homeShort') },
      { id: game?.awayTeamId, label: lt('liveGame.team.awayShort') }
    ].filter((team) => team.id != null);
    const lists = await Promise.all(teams.map(async (team) => {
      const players = await playerApi.teamOptions(team.id).catch(() => []);
      return { team, players: Array.isArray(players) ? players : [] };
    }));
    const options = [];
    for (const { team, players } of lists) {
      for (const player of players) {
        const playerId = player?.id ?? player?.playerId;
        if (playerId == null) continue;
        const number = player.number ? lt('liveGame.review.numberPrefix', { n: player.number }) : '';
        const name = player.name || `#${playerId}`;
        options.push({
          playerId,
          teamId: team.id,
          label: `${team.label} ${number}${name}`
        });
      }
    }
    playerOptions.value = options;
  } catch (e) {
    previewError.value = e?.message ?? lt('liveGame.revision.loadPlayersFailed');
  }
}

function resetPreview() {
  preview.value = null;
  previewError.value = '';
}

async function loadPreview() {
  if (!canPreview.value) return;
  previewLoading.value = true;
  resetPreview();
  try {
    const result = await gameApi.previewIdentityTransfer(
      props.gameId, sourcePlayerId.value, targetPlayerId.value
    );
    if (result?.error) {
      previewError.value = result.error;
      return;
    }
    preview.value = result?.data ?? null;
    if (preview.value && (!preview.value.liveSnapshotAvailable || preview.value.liveRosterConflict)
        && scopes.value.includes('LIVE_RECORDS')) {
      scopes.value = scopes.value.filter((scope) => scope !== 'LIVE_RECORDS');
    }
  } catch (e) {
    previewError.value = e?.message ?? lt('liveGame.revision.previewFailed');
  } finally {
    previewLoading.value = false;
  }
}

async function confirmTransfer() {
  if (!canTransfer.value) return;
  transferring.value = true;
  try {
    const result = await gameApi.transferIdentity(props.gameId, {
      sourcePlayerId: sourcePlayerId.value,
      targetPlayerId: targetPlayerId.value,
      scopes: scopes.value,
      note: note.value.trim() || null
    });
    const transferId = result?.data?.transferId;
    const sourceLabel = preview.value.sourcePlayerName;
    const targetLabel = preview.value.targetPlayerName;
    preview.value = null;
    if (transferId != null) {
      pendingTransfer.value = { id: transferId, sourceLabel, targetLabel };
    }
    try {
      await ElMessageBox.confirm(
        lt('liveGame.revision.transferredTo', { target: targetLabel, source: sourceLabel }),
        lt('liveGame.revision.transferDone'),
        { confirmButtonText: lt('liveGame.revision.deleteSource'), cancelButtonText: lt('liveGame.revision.keepSource'), type: 'warning' }
      );
    } catch {
      ElMessage.success(lt('liveGame.revision.transferredKeep'));
      finishTransfer();
      return;
    }
    if (transferId != null) {
      try {
        await gameApi.deleteIdentityTransferSource(props.gameId, transferId);
        pendingTransfer.value = null;
      } catch (e) {
        ElMessage.error(e?.message ?? lt('liveGame.revision.deleteAfterTransferFailed'));
        finishTransfer();
        return;
      }
    }
    ElMessage.success(lt('liveGame.revision.transferredDeleted'));
    finishTransfer();
  } catch (e) {
    ElMessage.error(e?.message ?? lt('liveGame.revision.transferFailed'));
  } finally {
    transferring.value = false;
  }
}

async function deletePendingSource() {
  if (!pendingTransfer.value) return;
  try {
    await gameApi.deleteIdentityTransferSource(props.gameId, pendingTransfer.value.id);
    pendingTransfer.value = null;
    ElMessage.success(lt('liveGame.revision.sourceDeleted'));
    emit('changed');
  } catch (e) {
    ElMessage.error(e?.message ?? lt('liveGame.revision.deleteSourceFailed'));
  }
}

function finishTransfer() {
  sourcePlayerId.value = null;
  targetPlayerId.value = null;
  note.value = '';
  resetPreview();
  emit('changed');
}

watch(() => props.gameId, () => {
  pendingTransfer.value = null;
  loadPlayers();
});
watch(() => props.refreshKey, () => {
  resetPreview();
  loadPlayers();
});
watch(sourcePlayerId, () => {
  targetPlayerId.value = null;
  resetPreview();
});
watch(targetPlayerId, resetPreview);
watch(scopes, resetPreview, { deep: true });
onMounted(loadPlayers);
</script>

<style scoped>
.identity-transfer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.identity-transfer__form {
  max-width: 560px;
}
.identity-transfer__hint {
  width: 100%;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.4;
}
.identity-transfer__hint.is-info {
  color: var(--el-text-color-secondary);
}
.identity-transfer__summary {
  margin-top: 4px;
}
.identity-transfer__rows {
  margin-top: 8px;
}
.identity-transfer__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
