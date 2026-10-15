<template>
  <div class="order-book-page event-teams-page">
    <el-card shadow="never" class="event-teams-card">
      <div class="page-head">
        <el-button link type="primary" class="back-btn" @click="goBack">{{ t('orderBook.backSchedule') }}</el-button>
        <span class="page-title">{{ t('orderBook.teamsTitle') }}</span>
        <span v-if="eventName" class="page-sub">· {{ eventName }}</span>
        <div class="head-actions">
          <el-button @click="openOrderBook">{{ t('orderBook.viewBook') }}</el-button>
          <el-button v-if="canManage" type="primary" @click="openInvite">{{ t('orderBook.invite') }}</el-button>
        </div>
      </div>

      <el-table v-loading="loading" v-bind="listTableAttrs" :data="rows" class="event-teams-table">
        <el-table-column :label="t('orderBook.colTeam')" min-width="180">
          <template #default="{ row }">
            <div class="team-cell">
              <el-avatar :size="28" shape="square" :src="resolveAssetUrl(row.teamLogo) || undefined">
                {{ (row.teamName || '?').slice(0, 1) }}
              </el-avatar>
              <span>{{ row.teamName || `ID:${row.teamId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('orderBook.colStatus')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ displayStatus(row.status, t) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playerCount" :label="t('orderBook.colPlayerCount')" width="100" />
        <el-table-column :label="t('orderBook.colSubmittedAt')" width="170">
          <template #default="{ row }">{{ formatTime(row.rosterSubmittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('orderBook.colLocked')" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.rosterLocked" type="warning" effect="light">{{ t('orderBook.locked') }}</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('orderBook.colActions')" min-width="230" :fixed="opFixed">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoster(row)">{{ t('orderBook.viewRoster') }}</el-button>
            <el-button v-if="canManage" link type="primary" @click="toggleLock(row)">
              {{ row.rosterLocked ? t('orderBook.unlock') : t('orderBook.lock') }}
            </el-button>
            <el-button v-if="canManage" link type="danger" :disabled="row.rosterLocked" @click="removeTeam(row)">
              {{ t('orderBook.remove') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>{{ t('orderBook.emptyTeams') }}</template>
      </el-table>
    </el-card>

    <el-dialog v-model="inviteVisible" :title="t('orderBook.inviteTitle')" width="520px" destroy-on-close>
      <el-select
        v-model="inviteTeamIds"
        multiple
        filterable
        :placeholder="t('orderBook.invitePlaceholder')"
        style="width: 100%"
      >
        <el-option
          v-for="option in inviteOptions"
          :key="option.id"
          :label="option.label"
          :value="option.id"
          :disabled="option.disabled"
        />
      </el-select>
      <div class="dialog-hint">{{ t('orderBook.inviteHint') }}</div>
      <template #footer>
        <el-button @click="inviteVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="inviting" :disabled="!inviteTeamIds.length" @click="submitInvite">
          {{ t('orderBook.inviteButton') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rosterVisible" :title="`${t('orderBook.rosterTitle')} · ${activeRow?.teamName ?? ''}`" width="600px">
      <EventRosterTable :rows="rosterRows" :loading="rosterLoading" show-current-in-team :empty-text="t('orderBook.emptyRoster')" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { eventApi, eventRegistrationApi } from '../../api/business';
import { resolveAssetUrl } from '../../api/request';
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

const canManage = computed(() => hasPerm('business:event:teams'));
const eventId = computed(() => String(route.params.eventId ?? ''));
const listTableAttrs = useListTableAttrs();
const opFixed = useFixedOperationColumn();

const eventName = ref('');
const loading = ref(false);
const rows = ref([]);
const allTeamOptions = ref([]);
const inviteVisible = ref(false);
const inviteTeamIds = ref([]);
const inviting = ref(false);
const rosterVisible = ref(false);
const rosterLoading = ref(false);
const rosterRows = ref([]);
const activeRow = ref(null);

// 只禁用"不可重复邀请"的状态（invited/confirmed 幂等跳过）；已谢绝可重邀（将重置为待确认）
const lockedOptionIds = computed(
  () => new Set(rows.value.filter((r) => r.status === 'invited' || r.status === 'confirmed').map((r) => r.teamId))
);
const inviteOptions = computed(() =>
  allTeamOptions.value.map((option) => ({
    id: option.id,
    label: lockedOptionIds.value.has(option.id)
      ? `${option.name}（${t('orderBook.inviteDisabledSuffix')}）`
      : option.name,
    disabled: lockedOptionIds.value.has(option.id)
  }))
);

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-';
}

async function load() {
  loading.value = true;
  rows.value = await eventRegistrationApi.listEventTeams(eventId.value);
  loading.value = false;
}

async function loadEventName() {
  const ev = await eventApi.get(eventId.value);
  eventName.value = ev?.name ?? '';
}

function goBack() {
  router.push({ name: 'AdminGames', params: { eventId: eventId.value } });
}

function openOrderBook() {
  router.push({ name: 'AdminEventOrderBook', params: { eventId: eventId.value } });
}

async function openInvite() {
  inviteTeamIds.value = [];
  inviteVisible.value = true;
  if (!allTeamOptions.value.length) {
    allTeamOptions.value = await eventRegistrationApi.invitableTeams(eventId.value);
  }
}

async function submitInvite() {
  if (!inviteTeamIds.value.length) return;
  inviting.value = true;
  try {
    const res = await eventRegistrationApi.invite(eventId.value, { teamIds: inviteTeamIds.value });
    ElMessage.success(t('orderBook.invitedCount', { n: res.invited ?? inviteTeamIds.value.length }));
    inviteVisible.value = false;
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.inviteFailed'));
  } finally {
    inviting.value = false;
  }
}

async function toggleLock(row) {
  const locking = !row.rosterLocked;
  const ok = await confirmAction(
    t(locking ? 'orderBook.lockConfirm' : 'orderBook.unlockConfirm', { name: row.teamName }),
    t(locking ? 'orderBook.lockConfirmTitle' : 'orderBook.unlockConfirmTitle')
  );
  if (!ok) return;
  try {
    if (locking) {
      await eventRegistrationApi.lock(eventId.value, row.teamId);
    } else {
      await eventRegistrationApi.unlock(eventId.value, row.teamId);
    }
    ElMessage.success(t(locking ? 'orderBook.lockedDone' : 'orderBook.unlockedDone'));
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.operationFailed'));
  }
}

async function removeTeam(row) {
  const ok = await confirmAction(
    t('orderBook.removeConfirm', { name: row.teamName }),
    t('orderBook.removeConfirmTitle')
  );
  if (!ok) return;
  try {
    await eventRegistrationApi.remove(eventId.value, row.teamId);
    ElMessage.success(t('orderBook.removedDone'));
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? t('orderBook.removeFailed'));
  }
}

async function openRoster(row) {
  activeRow.value = row;
  rosterVisible.value = true;
  rosterLoading.value = true;
  rosterRows.value = await eventRegistrationApi.roster(eventId.value, row.teamId);
  rosterLoading.value = false;
}

onMounted(() => {
  loadEventName();
  load();
});
</script>

<style scoped>
.event-teams-page {
  padding: 12px;
}

.head-actions {
  margin-left: auto;
}

.team-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
