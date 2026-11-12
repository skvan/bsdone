<template>
  <div class="identity-transfer">
    <el-alert
      v-if="!isFinal"
      type="info"
      :closable="false"
      show-icon
      title="比赛结束后才能进行身份转记"
    />
    <template v-else>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="整名球员身份转记"
        description="把源球员的统计、逐位置守备与 ER 判定转到同队目标球员；转记成功后再单独确认是否删除源数据。"
      />
      <el-form label-width="82px" size="default" class="identity-transfer__form">
        <el-form-item label="源球员">
          <el-select v-model="sourcePlayerId" filterable placeholder="选择录错的球员" style="width: 100%">
            <el-option
              v-for="player in playerOptions"
              :key="player.playerId"
              :label="player.label"
              :value="player.playerId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标球员">
          <el-select
            v-model="targetPlayerId"
            filterable
            placeholder="选择同队正确球员"
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
        <el-form-item label="转记范围">
          <el-checkbox-group v-model="scopes">
            <el-checkbox label="STATS">统计</el-checkbox>
            <el-checkbox label="FIELDING">守备位置</el-checkbox>
            <el-checkbox label="ER_DECISIONS">ER 判定</el-checkbox>
            <el-checkbox
              label="LIVE_RECORDS"
              :disabled="Boolean(preview) && (!preview.liveSnapshotAvailable || preview.liveRosterConflict)"
            >现场记录</el-checkbox>
          </el-checkbox-group>
          <div v-if="!scopes.length" class="identity-transfer__hint">至少选择一个转记范围</div>
          <div v-else-if="preview?.liveRosterConflict" class="identity-transfer__hint">
            源/目标球员同时在同一阵容或替补名单，现场记录范围不可用；请先调整阵容
          </div>
          <div v-else-if="scopes.includes('LIVE_RECORDS')" class="identity-transfer__hint is-info">
            现场记录将改写 Play 事件与阵容中的球员归属，转记前快照写入审计
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="note" maxlength="200" show-word-limit placeholder="可空，例如：16 号误录，应为 21 号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="previewLoading" :disabled="!canPreview" @click="loadPreview">
            预览转记
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
          title="无法转记"
          :description="(preview.blockers || []).join('；')"
        />
        <template v-else>
          <el-descriptions :column="2" border size="small" class="identity-transfer__summary">
            <el-descriptions-item label="源">{{ preview.sourcePlayerName }}</el-descriptions-item>
            <el-descriptions-item label="目标">{{ preview.targetPlayerName }}</el-descriptions-item>
            <el-descriptions-item label="ER 判定">{{ preview.erDecisionCount }} 条</el-descriptions-item>
            <el-descriptions-item label="守备位置">
              {{ (preview.fieldingPositions || []).join('、') || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="源 R/ER 覆写">{{ preview.sourceOverride ? '有' : '无' }}</el-descriptions-item>
            <el-descriptions-item label="目标 R/ER 覆写">{{ preview.targetOverride ? '有' : '无' }}</el-descriptions-item>
            <el-descriptions-item label="现场记录引用">
              {{ preview.liveSnapshotAvailable ? `${preview.liveReferenceCount || 0} 处` : '无现场记录' }}
            </el-descriptions-item>
            <el-descriptions-item label="守备补记">
              {{ preview.fieldingAdjustmentCount || 0 }} 条
            </el-descriptions-item>
          </el-descriptions>
          <el-table :data="preview.rows || []" size="small" border stripe class="identity-transfer__rows">
            <el-table-column label="行类型" width="90" align="center">
              <template #default="{ row }">{{ row.isPitcher === 1 ? '投手行' : '打者行' }}</template>
            </el-table-column>
            <el-table-column label="源行" width="100" align="center">
              <template #default="{ row }">#{{ row.sourceStatId }}</template>
            </el-table-column>
            <el-table-column label="目标行" min-width="120" align="center">
              <template #default="{ row }">{{ row.targetExists ? `#${row.targetStatId}` : '新建/移动源行' }}</template>
            </el-table-column>
          </el-table>
          <div class="identity-transfer__actions">
            <el-button
              type="primary"
              :loading="transferring"
              :disabled="!canTransfer"
              @click="confirmTransfer"
            >确认转记</el-button>
          </div>
        </template>
      </template>

      <el-alert
        v-if="pendingTransfer"
        type="warning"
        :closable="false"
        show-icon
        title="转记已完成，源数据尚未删除"
        :description="`${pendingTransfer.sourceLabel} → ${pendingTransfer.targetLabel}`"
      >
        <template #default>
          <el-button size="small" type="warning" @click="deletePendingSource">删除源数据</el-button>
        </template>
      </el-alert>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { gameApi, playerApi } from '../../api/business';

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
      { id: game?.homeTeamId, label: '主' },
      { id: game?.awayTeamId, label: '客' }
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
        const number = player.number ? `${player.number}号 ` : '';
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
    previewError.value = e?.message ?? '加载球员失败';
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
    previewError.value = e?.message ?? '预览失败';
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
        `已转记到 ${targetLabel}。是否删除源数据 ${sourceLabel}？`,
        '转记完成',
        { confirmButtonText: '删除源数据', cancelButtonText: '暂不删除', type: 'warning' }
      );
    } catch {
      ElMessage.success('已转记，源数据保留');
      finishTransfer();
      return;
    }
    if (transferId != null) {
      try {
        await gameApi.deleteIdentityTransferSource(props.gameId, transferId);
        pendingTransfer.value = null;
      } catch (e) {
        ElMessage.error(e?.message ?? '转记已完成，但删除源数据失败');
        finishTransfer();
        return;
      }
    }
    ElMessage.success('已转记并删除源数据');
    finishTransfer();
  } catch (e) {
    ElMessage.error(e?.message ?? '身份转记失败');
  } finally {
    transferring.value = false;
  }
}

async function deletePendingSource() {
  if (!pendingTransfer.value) return;
  try {
    await gameApi.deleteIdentityTransferSource(props.gameId, pendingTransfer.value.id);
    pendingTransfer.value = null;
    ElMessage.success('已删除源数据');
    emit('changed');
  } catch (e) {
    ElMessage.error(e?.message ?? '删除源数据失败');
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
