<template>
  <div class="team-registrations-page">
    <el-card shadow="never" class="team-registrations-card">
      <div class="page-head">
        <el-button link type="primary" class="back-btn" @click="goBack">← 返回球队</el-button>
        <span class="page-title">赛事报名</span>
        <span v-if="teamName" class="team-name">· {{ teamName }}</span>
      </div>

      <el-table v-loading="loading" :data="rows" class="team-registrations-table">
        <el-table-column label="赛事" min-width="200">
          <template #default="{ row }">
            <div class="event-cell">
              <span>{{ row.eventName || `赛事 ID:${row.eventId}` }}</span>
              <el-tag v-if="row.season" size="small" type="info" effect="plain">{{ row.season }}</el-tag>
              <el-tag v-if="row.gameMode === 'SOFTBALL'" size="small" type="warning" effect="plain">垒球</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playerCount" label="名单人数" width="100" />
        <el-table-column label="锁定" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.rosterLocked" type="warning" effect="light">已锁定</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManage && row.status === 'invited'" link type="primary" @click="confirmJoin(row)">
              确认参加
            </el-button>
            <el-button v-if="canManage && row.status === 'invited'" link type="info" @click="declineJoin(row)">
              谢绝
            </el-button>
            <el-button
              v-if="canManage && row.status === 'confirmed'"
              link
              type="primary"
              :disabled="row.rosterLocked"
              @click="openRoster(row, 'edit')"
            >
              编辑名单
            </el-button>
            <el-button link type="primary" @click="openRoster(row, 'view')">查看名单</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无赛事报名记录</template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="rosterVisible"
      :title="`${rosterMode === 'edit' ? '编辑报名名单' : '报名名单'} · ${activeRow?.eventName ?? ''}`"
      width="720px"
      destroy-on-close
    >
      <div v-if="activeRow?.rosterLocked" class="lock-hint">名单已锁定，仅可查看</div>

      <el-table
        v-if="rosterMode === 'edit'"
        ref="playerTableRef"
        v-loading="playersLoading"
        :data="playerOptions"
        max-height="420"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column prop="name" label="球员" min-width="110" />
        <el-table-column label="背号" width="80">
          <template #default="{ row }">{{ row.number || '-' }}</template>
        </el-table-column>
        <el-table-column label="位置" min-width="120">
          <template #default="{ row }">{{ (row.positions || []).join(' / ') || '-' }}</template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="playersLoading" :data="rosterRows" max-height="420">
        <el-table-column prop="name" label="球员" min-width="110" />
        <el-table-column label="背号" width="80">
          <template #default="{ row }">{{ row.number || '-' }}</template>
        </el-table-column>
        <el-table-column label="位置" min-width="120">
          <template #default="{ row }">{{ (row.positions || []).join(' / ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="在队" width="90">
          <template #default="{ row }">
            <el-tag v-if="!row.currentInTeam" type="danger" effect="light">已离队</el-tag>
            <span v-else>在队</span>
          </template>
        </el-table-column>
        <template #empty>尚未提交报名名单</template>
      </el-table>

      <template #footer>
        <el-button @click="rosterVisible = false">关闭</el-button>
        <el-button
          v-if="rosterMode === 'edit' && !activeRow?.rosterLocked"
          type="primary"
          :loading="saving"
          @click="saveRoster"
        >
          保存名单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { eventRegistrationApi, fetchPlayersByTeam, teamApi } from '../../api/business';
import { usePermission } from '../../composables/usePermission';

const route = useRoute();
const router = useRouter();
const { hasPerm } = usePermission();

const canManage = computed(() => hasPerm('business:team:eventRegistration'));
const teamId = computed(() => String(route.params.id ?? ''));

const teamName = ref('');
const loading = ref(false);
const rows = ref([]);

const rosterVisible = ref(false);
const rosterMode = ref('edit');
const activeRow = ref(null);
const playersLoading = ref(false);
const playerOptions = ref([]);
const rosterRows = ref([]);
const playerTableRef = ref(null);
const selectedIds = ref([]);
const saving = ref(false);

const STATUS_TEXT = { invited: '待确认', confirmed: '已确认', declined: '已谢绝' };
const STATUS_TYPE = { invited: 'info', confirmed: 'success', declined: 'danger' };

function statusText(status) {
  return STATUS_TEXT[status] ?? status ?? '-';
}

function statusTagType(status) {
  return STATUS_TYPE[status] ?? 'info';
}

async function load() {
  loading.value = true;
  rows.value = (await eventRegistrationApi.myRegistrations(teamId.value)) ?? [];
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
  try {
    await ElMessageBox.confirm(`确认参加「${row.eventName}」？确认后请提交报名名单。`, '确认参加', {
      type: 'info'
    });
  } catch {
    return;
  }
  try {
    await eventRegistrationApi.confirm(row.eventId, teamId.value);
    ElMessage.success('已确认参加');
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? '操作失败');
  }
}

async function declineJoin(row) {
  try {
    await ElMessageBox.confirm(`谢绝参加「${row.eventName}」？如需重新参加请联系办赛方重新邀请。`, '谢绝确认', {
      type: 'warning'
    });
  } catch {
    return;
  }
  try {
    await eventRegistrationApi.decline(row.eventId, teamId.value);
    ElMessage.success('已谢绝');
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? '操作失败');
  }
}

async function openRoster(row, mode) {
  activeRow.value = row;
  rosterMode.value = mode;
  rosterVisible.value = true;
  playersLoading.value = true;
  selectedIds.value = [];
  try {
    if (mode === 'edit') {
      const [players, current] = await Promise.all([
        fetchPlayersByTeam(teamId.value),
        eventRegistrationApi.roster(row.eventId, teamId.value)
      ]);
      playerOptions.value = players ?? [];
      const orderedIds = (current ?? []).map((r) => r.playerId);
      await restoreSelection(orderedIds);
    } else {
      rosterRows.value = (await eventRegistrationApi.roster(row.eventId, teamId.value)) ?? [];
    }
  } finally {
    playersLoading.value = false;
  }
}

async function restoreSelection(orderedIds) {
  selectedIds.value = [...orderedIds];
  await nextTick();
  const table = playerTableRef.value;
  if (!table) return;
  for (const id of orderedIds) {
    const rowData = playerOptions.value.find((p) => String(p.id) === String(id));
    if (rowData) table.toggleRowSelection(rowData, true);
  }
}

function onSelectionChange(selection) {
  selectedIds.value = selection.map((p) => p.id);
}

async function saveRoster() {
  if (!activeRow.value) return;
  if (!selectedIds.value.length) {
    ElMessage.warning('请至少选择 1 名球员');
    return;
  }
  saving.value = true;
  try {
    await eventRegistrationApi.submitRoster(activeRow.value.eventId, teamId.value, [...selectedIds.value]);
    ElMessage.success('名单已保存');
    rosterVisible.value = false;
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? '保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  loadTeamName();
  await load();
});
</script>

<style scoped>
.team-registrations-page {
  padding: 12px;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.team-name {
  color: var(--el-text-color-secondary);
}

.event-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.muted {
  color: var(--el-text-color-secondary);
}

.lock-hint {
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
  font-size: 12px;
}
</style>
