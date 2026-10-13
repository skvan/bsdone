<template>
  <el-dialog
    :model-value="modelValue"
    :title="`${t('invitePlayer.title')}${teamName ? ` · ${teamName}` : ''}`"
    width="760px"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane :label="t('invitePlayer.tabSearch')" name="search">
        <el-input
          v-model="keyword"
          :placeholder="t('invitePlayer.searchPlaceholder')"
          clearable
          class="search-input"
        />
        <el-table v-loading="searching" :data="candidates" max-height="380" class="invite-candidates-table">
          <el-table-column prop="name" :label="t('invitePlayer.colName')" min-width="110" />
          <el-table-column :label="t('invitePlayer.colNumber')" width="70">
            <template #default="{ row }">{{ row.number || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colPositions')" min-width="100">
            <template #default="{ row }">{{ (row.positions || []).join(' / ') || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colClaimed')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.claimed ? 'success' : 'info'" effect="light">
                {{ row.claimed ? t('invitePlayer.claimed') : t('invitePlayer.unclaimed') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colTeams')" min-width="130">
            <template #default="{ row }">
              <span v-if="row.currentTeams?.length">{{ row.currentTeams.map((x) => x.teamName).join(' / ') }}</span>
              <span v-else class="muted">{{ t('invitePlayer.freeAgent') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colInTeam')" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.inThisTeam" type="warning" effect="light">{{ t('invitePlayer.inThisTeam') }}</el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('orderBook.colActions')" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :disabled="row.inThisTeam || creating"
                @click="generateInvite(row)"
              >
                {{ t('invitePlayer.generate') }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>{{ keyword ? t('invitePlayer.emptySearch') : t('invitePlayer.searchPlaceholder') }}</template>
        </el-table>
        <div class="dialog-hint">{{ t('invitePlayer.genericHint') }}</div>
      </el-tab-pane>

      <el-tab-pane :label="t('invitePlayer.tabInvites')" name="invites">
        <el-table v-loading="listLoading" :data="invites" max-height="380">
          <el-table-column :label="t('invitePlayer.colPlayer')" min-width="110">
            <template #default="{ row }">
              <span v-if="row.playerName">{{ row.playerName }}</span>
              <span v-else class="muted">{{ t('invitePlayer.notBound') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colStatus')" width="100">
            <template #default="{ row }">
              <el-tag :type="inviteStatusType(row.status)" effect="light">{{ inviteStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colUses')" width="90">
            <template #default="{ row }">{{ row.usedCount ?? 0 }}/{{ row.maxUses ?? 1 }}</template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colExpires')" width="150">
            <template #default="{ row }">{{ formatTime(row.expiresAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colCreated')" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('orderBook.colActions')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'active'" link type="danger" @click="revokeInvite(row)">
                {{ t('invitePlayer.revoke') }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>{{ t('invitePlayer.emptyInvites') }}</template>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="linkVisible" :title="t('invitePlayer.linkTitle')" width="520px" append-to-body>
      <el-input :model-value="linkPreview" readonly class="link-input">
        <template #append>
          <el-button @click="copyLink">{{ t('invitePlayer.copy') }}</el-button>
        </template>
      </el-input>
      <div class="dialog-hint">{{ t('invitePlayer.linkHint') }}</div>
    </el-dialog>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t('orderBook.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { accountApi } from '../../api/account';
import { currentTenantCodeFromUrl } from '../../utils/tenantRoute';
import '../../styles/admin-page-head.css';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  teamId: { type: [Number, String], default: null },
  teamName: { type: String, default: '' }
});
const emit = defineEmits(['update:modelValue']);

const { t } = useI18n();

const activeTab = ref('search');
const keyword = ref('');
const candidates = ref([]);
const searching = ref(false);
const creating = ref(false);
const invites = ref([]);
const listLoading = ref(false);
const linkVisible = ref(false);
const linkToken = ref('');

const linkPreview = computed(() => {
  if (!linkToken.value) return '';
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  const tenant = currentTenantCodeFromUrl() || '';
  const prefix = tenant ? `/${tenant}` : '';
  return `${origin}${prefix}/claim/${linkToken.value}`;
});

let searchTimer = null;

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return;
    activeTab.value = 'search';
    keyword.value = '';
    candidates.value = [];
    runSearch();
    loadInvites();
  }
);

watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(runSearch, 300);
});

async function runSearch() {
  if (props.teamId == null) return;
  searching.value = true;
  try {
    const res = await accountApi.inviteCandidates(props.teamId, { keyword: keyword.value || undefined, page: 1, pageSize: 20 });
    candidates.value = res?.data?.list ?? [];
  } catch {
    candidates.value = [];
  } finally {
    searching.value = false;
  }
}

async function loadInvites() {
  if (props.teamId == null) return;
  listLoading.value = true;
  try {
    invites.value = await accountApi.listClaimInvites(props.teamId);
  } finally {
    listLoading.value = false;
  }
}

async function generateInvite(row) {
  creating.value = true;
  try {
    // 未认领：定向绑定该球员档案；已认领：通用链接（服务端语义）
    const res = await accountApi.createClaimInvite(props.teamId, { playerId: row.claimed ? null : row.id });
    linkToken.value = res?.data?.token ?? '';
    linkVisible.value = true;
    await copyLink();
    await loadInvites();
  } catch (e) {
    ElMessage.error(e?.message ?? t('invitePlayer.createFailed'));
  } finally {
    creating.value = false;
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(linkPreview.value);
    ElMessage.success(t('invitePlayer.copyDone'));
  } catch {
    ElMessage.warning(t('invitePlayer.copyUnsupported'));
  }
}

async function revokeInvite(row) {
  try {
    await ElMessageBox.confirm(t('invitePlayer.revokeConfirm'), t('invitePlayer.revokeConfirmTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await accountApi.revokeClaimInvite(props.teamId, row.id);
    ElMessage.success(t('invitePlayer.revoked'));
    await loadInvites();
  } catch (e) {
    ElMessage.error(e?.message ?? t('invitePlayer.revokeFailed'));
  }
}

const INVITE_STATUS_TEXT = {
  active: 'invitePlayer.statusActive',
  expired: 'invitePlayer.statusExpired',
  revoked: 'invitePlayer.statusRevoked'
};
const INVITE_STATUS_TYPE = { active: 'success', expired: 'info', revoked: 'danger' };

function inviteStatusText(status) {
  return INVITE_STATUS_TEXT[status] ? t(INVITE_STATUS_TEXT[status]) : status ?? '-';
}

function inviteStatusType(status) {
  return INVITE_STATUS_TYPE[status] ?? 'info';
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-';
}
</script>

<style scoped>
.search-input {
  margin-bottom: 10px;
}

.dialog-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.muted {
  color: var(--el-text-color-secondary);
}

.link-input {
  width: 100%;
}
</style>
