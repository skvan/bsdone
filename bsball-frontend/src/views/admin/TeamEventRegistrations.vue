<template>
  <div class="order-book-page team-registrations-page">
    <el-card shadow="never" class="team-registrations-card">
      <div class="page-head">
        <el-button link type="primary" class="back-btn" @click="goBack">{{ t('orderBook.backTeams') }}</el-button>
        <span class="page-title">{{ t('orderBook.registrationTitle') }}</span>
        <span v-if="teamName" class="page-sub">· {{ teamName }}</span>
      </div>

      <el-table v-loading="loading" v-bind="listTableAttrs" :data="rows" class="team-registrations-table">
        <el-table-column :label="t('orderBook.colEvent')" min-width="200">
          <template #default="{ row }">
            <div class="event-cell">
              <span>{{ row.eventName || `ID:${row.eventId}` }}</span>
              <el-tag v-if="row.season" size="small" type="info" effect="plain">{{ row.season }}</el-tag>
              <el-tag v-if="row.gameMode === 'SOFTBALL'" size="small" type="warning" effect="plain">
                {{ t('orderBook.softball') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('orderBook.colStatus')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ displayStatus(row.status, t) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playerCount" :label="t('orderBook.colPlayerCount')" width="100" />
        <el-table-column :label="t('orderBook.colLocked')" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.rosterLocked" type="warning" effect="light">{{ t('orderBook.locked') }}</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('orderBook.colActions')" min-width="280" :fixed="opFixed">
          <template #default="{ row }">
            <el-button v-if="canManage && row.status === 'invited'" link type="primary" @click="confirmJoin(row)">
              {{ t('orderBook.confirmJoin') }}
            </el-button>
            <el-button v-if="canManage && row.status === 'invited'" link type="info" @click="declineJoin(row)">
              {{ t('orderBook.declineJoin') }}
            </el-button>
            <el-button
              v-if="canManage && row.status === 'confirmed'"
              link
              type="primary"
              :disabled="row.rosterLocked"
              @click="openRoster(row, 'edit')"
            >
              {{ t('orderBook.editRoster') }}
            </el-button>
            <el-button link type="primary" @click="openRoster(row, 'view')">{{ t('orderBook.viewRoster') }}</el-button>
            <el-tooltip
              v-if="canManage && row.status === 'confirmed'"
              :disabled="row.canWithdraw === true"
              :content="t('orderBook.withdrawBlocked')"
              placement="top"
            >
              <span class="withdraw-wrap">
                <el-button link type="danger" :disabled="row.canWithdraw !== true" @click="withdrawRegistration(row)">
                  {{ t('orderBook.withdraw') }}
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <template #empty>{{ t('orderBook.emptyRegistrations') }}</template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="rosterVisible"
      :title="`${t(rosterMode === 'edit' ? 'orderBook.editRosterTitle' : 'orderBook.rosterTitle')} · ${activeRow?.eventName ?? ''}`"
      width="720px"
      destroy-on-close
    >
      <div v-if="activeRow?.rosterLocked" class="lock-hint">{{ t('orderBook.lockHint') }}</div>
      <div v-if="rosterMode === 'edit' && departedCount > 0" class="departed-hint">
        {{ t('orderBook.departedHint', { n: departedCount }) }}
      </div>

      <EventRosterTable
        v-if="rosterMode === 'edit'"
        ref="rosterTableRef"
        :rows="playerOptions"
        :loading="playersLoading"
        selectable
        :empty-text="t('orderBook.noPlayers')"
        @selection-change="onSelectionChange"
      />
      <EventRosterTable
        v-else
        :rows="rosterRows"
        :loading="playersLoading"
        show-current-in-team
        :empty-text="t('orderBook.emptyRoster')"
      />

      <div v-if="rosterMode === 'edit' && selectedNames.length" class="selected-panel">
        <span class="selected-title">{{ t('orderBook.selectedTitle', { n: selectedNames.length }) }}</span>
        <el-tag v-for="item in selectedNames" :key="item.id" size="small" class="selected-tag">{{ item.name }}</el-tag>
      </div>

      <template #footer>
        <el-button @click="rosterVisible = false">{{ t('orderBook.close') }}</el-button>
        <el-button
          v-if="rosterMode === 'edit' && !activeRow?.rosterLocked"
          type="primary"
          :loading="saving"
          @click="saveRoster"
        >
          {{ t('orderBook.saveRoster') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { eventRegistrationApi, fetchPlayersByTeam, teamApi } from '../../api/business';
import { usePermission } from '../../composables/usePermission';
import { useFixedOperationColumn, useListTableAttrs } from '../../composables/useListTable';
import { displayStatus, statusTagType } from '../../utils/eventRegistrationStatus';
import { confirmAction } from '../../utils/confirmAction';
import EventRosterTable from '../../components/admin/EventRosterTable.vue';
import '../../styles/admin-page-head.css';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const { hasPerm } = usePermission();

const canManage = computed(() => hasPerm('business:team:eventRegistration'));
const teamId = computed(() => String(route.params.id ?? ''));
const listTableAttrs = useListTableAttrs();
const opFixed = useFixedOperationColumn();

const teamName = ref('');
const loading = ref(false);
const rows = ref([]);

const rosterVisible = ref(false);
const rosterMode = ref('edit');
const activeRow = ref(null);
const playersLoading = ref(false);
const playerOptions = ref([]);
const rosterRows = ref([]);
const currentRosterIds = ref([]);
const rosterTableRef = ref(null);
const selectedIds = ref([]);
const saving = ref(false);

const selectedNames = computed(() => {
  const byId = new Map(playerOptions.value.map((p) => [String(p.id), p]));
  return selectedIds.value.map((id) => ({ id, name: byId.get(String(id))?.name ?? `ID:${id}` }));
});

// 上次名单中含已不在池中的球员（已离队）：保存后将自动从名单移除
const departedCount = computed(() => {
  if (rosterMode.value !== 'edit') return 0;
  const pool = new Set(playerOptions.value.map((p) => String(p.id)));
  return currentRosterIds.value.filter((id) => !pool.has(String(id))).length;
});

async function load() {
  loading.value = true;
  rows.value = await eventRegistrationApi.myRegistrations(teamId.value);
  loading.value = false;
}

async function loadTeamName() {
  const team = await teamApi.get(teamId.value);
  teamName.value = team?.name ?? '';
}

function goBack() {
  router.push({ name: 'AdminTeams' });
}

async function confirmJoin(row) {
  const ok = await confirmAction(
    t('orderBook.confirmJoinConfirm', { name: row.eventName }),
    t('orderBook.confirmJoinTitle'),
    'info'
  );
  if (!ok) return;
  try {
    await eventRegistrationApi.confirm(row.eventId, teamId.value);
    ElMessage.success(t('orderBook.joinedDone'));
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.operationFailed'));
  }
}

async function declineJoin(row) {
  const ok = await confirmAction(
    t('orderBook.declineJoinConfirm', { name: row.eventName }),
    t('orderBook.declineJoinTitle')
  );
  if (!ok) return;
  try {
    await eventRegistrationApi.decline(row.eventId, teamId.value);
    ElMessage.success(t('orderBook.declinedDone'));
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.operationFailed'));
  }
}

// 撤销报名（球队侧自助退赛）：仅已确认可操作；本队尚有已开赛比赛时后端拦截
async function withdrawRegistration(row) {
  const ok = await confirmAction(
    t('orderBook.withdrawConfirm', { name: row.eventName }),
    t('orderBook.withdrawConfirmTitle')
  );
  if (!ok) return;
  try {
    await eventRegistrationApi.withdraw(row.eventId, teamId.value);
    ElMessage.success(t('orderBook.withdrawnDone'));
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.withdrawFailed'));
  }
}

async function openRoster(row, mode) {
  activeRow.value = row;
  rosterMode.value = mode;
  rosterVisible.value = true;
  playersLoading.value = true;
  selectedIds.value = [];
  currentRosterIds.value = [];
  try {
    if (mode === 'edit') {
      const [players, current] = await Promise.all([
        fetchPlayersByTeam(teamId.value),
        eventRegistrationApi.roster(row.eventId, teamId.value)
      ]);
      playerOptions.value = players;
      currentRosterIds.value = current.map((r) => r.playerId);
      await restoreSelection(currentRosterIds.value);
    } else {
      rosterRows.value = await eventRegistrationApi.roster(row.eventId, teamId.value);
    }
  } finally {
    playersLoading.value = false;
  }
}

/**
 * el-table 勾选态非完全受控：逐个 toggleRowSelection 回填上次名单（按名单顺序），
 * 回填触发的 selection-change 会把 selectedIds 恢复为真实勾选集合（不再于表格之外预置 id）。
 */
async function restoreSelection(orderedIds) {
  await nextTick();
  for (const id of orderedIds) {
    const rowData = playerOptions.value.find((p) => String(p.id) === String(id));
    if (rowData) rosterTableRef.value?.toggleRowSelection(rowData, true);
  }
}

function onSelectionChange(selection) {
  selectedIds.value = selection.map((p) => p.id);
}

async function saveRoster() {
  if (!activeRow.value) return;
  if (!selectedIds.value.length) {
    ElMessage.warning(t('orderBook.atLeastOne'));
    return;
  }
  saving.value = true;
  try {
    await eventRegistrationApi.submitRoster(activeRow.value.eventId, teamId.value, selectedIds.value);
    ElMessage.success(t('orderBook.rosterSaved'));
    rosterVisible.value = false;
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.saveFailed'));
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadTeamName();
  load();
});
</script>

<style scoped>
.team-registrations-page {
  padding: 12px;
}

.event-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lock-hint {
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
  font-size: 12px;
}

.departed-hint {
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  font-size: 12px;
}

.selected-panel {
  margin-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.selected-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-right: 4px;
}

.selected-tag {
  max-width: 140px;
}

.withdraw-wrap {
  display: inline-flex;
  margin-left: 12px;
}
</style>
