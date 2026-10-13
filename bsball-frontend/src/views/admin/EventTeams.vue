<template>
  <div class="event-teams-page">
    <el-card shadow="never" class="event-teams-card">
      <div class="page-head">
        <el-button link type="primary" class="back-btn" @click="goBack">← 返回赛程</el-button>
        <span class="page-title">参赛球队</span>
        <span v-if="eventName" class="event-name">· {{ eventName }}</span>
        <div class="head-actions">
          <el-button v-if="canManage" type="primary" @click="openInvite">邀请球队</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" class="event-teams-table">
        <el-table-column label="球队" min-width="180">
          <template #default="{ row }">
            <div class="team-cell">
              <el-avatar :size="28" shape="square" :src="row.teamLogo || undefined">
                {{ (row.teamName || '?').slice(0, 1) }}
              </el-avatar>
              <span>{{ row.teamName || `球队 ID:${row.teamId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playerCount" label="名单人数" width="100" />
        <el-table-column label="最近提交" width="170">
          <template #default="{ row }">{{ formatTime(row.rosterSubmittedAt) }}</template>
        </el-table-column>
        <el-table-column label="锁定" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.rosterLocked" type="warning" effect="light">已锁定</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoster(row)">查看名单</el-button>
            <el-button v-if="canManage" link type="primary" @click="toggleLock(row)">
              {{ row.rosterLocked ? '解锁' : '锁定' }}
            </el-button>
            <el-button v-if="canManage" link type="danger" :disabled="row.rosterLocked" @click="removeTeam(row)">
              移除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>暂无参赛球队，点击「邀请球队」开始报名</template>
      </el-table>
    </el-card>

    <el-dialog v-model="inviteVisible" title="邀请球队参赛" width="520px" destroy-on-close>
      <el-select
        v-model="inviteTeamIds"
        multiple
        filterable
        placeholder="选择球队（支持搜索）"
        style="width: 100%"
      >
        <el-option
          v-for="t in teamOptions"
          :key="t.id"
          :label="t.name"
          :value="t.id"
          :disabled="invitedTeamIds.has(t.id)"
        />
      </el-select>
      <div class="dialog-hint">已邀请的球队不可重复选择；状态为「已谢绝」的球队重邀将重置为待确认。</div>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviting" :disabled="!inviteTeamIds.length" @click="submitInvite">
          邀请
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rosterVisible" :title="`报名名单 · ${rosterTeam?.teamName ?? ''}`" width="600px">
      <el-table v-loading="rosterLoading" :data="rosterRows" max-height="420">
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
        <template #empty>该球队尚未提交报名名单</template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { eventApi, eventRegistrationApi, teamApi } from '../../api/business';
import { usePermission } from '../../composables/usePermission';

const route = useRoute();
const router = useRouter();
const { hasPerm } = usePermission();

const canManage = computed(() => hasPerm('business:event:teams'));
const eventId = computed(() => String(route.params.eventId ?? ''));

const eventName = ref('');
const loading = ref(false);
const rows = ref([]);
const teamOptions = ref([]);
const inviteVisible = ref(false);
const inviteTeamIds = ref([]);
const inviting = ref(false);
const rosterVisible = ref(false);
const rosterLoading = ref(false);
const rosterRows = ref([]);
const rosterTeam = ref(null);

const invitedTeamIds = computed(() => new Set(rows.value.map((r) => r.teamId)));

const STATUS_TEXT = { invited: '待确认', confirmed: '已确认', declined: '已谢绝' };
const STATUS_TYPE = { invited: 'info', confirmed: 'success', declined: 'danger' };

function statusText(status) {
  return STATUS_TEXT[status] ?? status ?? '-';
}

function statusTagType(status) {
  return STATUS_TYPE[status] ?? 'info';
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-';
}

async function load() {
  loading.value = true;
  rows.value = (await eventRegistrationApi.listEventTeams(eventId.value)) ?? [];
  loading.value = false;
}

async function loadEventName() {
  const ev = await eventApi.get(eventId.value).catch(() => null);
  eventName.value = ev?.name ?? '';
}

function goBack() {
  router.push({ name: 'AdminGames', params: { eventId: eventId.value } });
}

async function openInvite() {
  inviteTeamIds.value = [];
  inviteVisible.value = true;
  if (!teamOptions.value.length) {
    teamOptions.value = (await teamApi.selectOptions()) ?? [];
  }
}

async function submitInvite() {
  if (!inviteTeamIds.value.length) return;
  inviting.value = true;
  try {
    const res = await eventRegistrationApi.invite(eventId.value, { teamIds: [...inviteTeamIds.value] });
    const invited = res?.data?.invited ?? inviteTeamIds.value.length;
    ElMessage.success(`已邀请 ${invited} 支球队`);
    inviteVisible.value = false;
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? '邀请失败');
  } finally {
    inviting.value = false;
  }
}

async function toggleLock(row) {
  const locking = !row.rosterLocked;
  try {
    await ElMessageBox.confirm(
      locking
        ? `锁定后球队将无法再修改报名名单，确定锁定「${row.teamName}」？`
        : `解锁后球队可继续修改报名名单，确定解锁「${row.teamName}」？`,
      locking ? '锁定确认' : '解锁确认',
      { type: 'warning' }
    );
  } catch {
    return;
  }
  try {
    if (locking) {
      await eventRegistrationApi.lock(eventId.value, row.teamId);
    } else {
      await eventRegistrationApi.unlock(eventId.value, row.teamId);
    }
    ElMessage.success(locking ? '已锁定' : '已解锁');
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? '操作失败');
  }
}

async function removeTeam(row) {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.teamName}」移出本赛事？其报名名单将一并移除。`,
      '移除确认',
      { type: 'warning' }
    );
  } catch {
    return;
  }
  try {
    await eventRegistrationApi.remove(eventId.value, row.teamId);
    ElMessage.success('已移除');
    await load();
  } catch (e) {
    ElMessage.error(e?.message ?? '移除失败');
  }
}

async function openRoster(row) {
  rosterTeam.value = row;
  rosterVisible.value = true;
  rosterLoading.value = true;
  rosterRows.value = (await eventRegistrationApi.roster(eventId.value, row.teamId)) ?? [];
  rosterLoading.value = false;
}

onMounted(async () => {
  loadEventName();
  await load();
});
</script>

<style scoped>
.event-teams-page {
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

.event-name {
  color: var(--el-text-color-secondary);
}

.head-actions {
  margin-left: auto;
}

.team-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.muted {
  color: var(--el-text-color-secondary);
}

.dialog-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
