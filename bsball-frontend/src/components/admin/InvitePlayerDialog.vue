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
          <template #empty>
            {{ keyword ? t('invitePlayer.emptySearch') : t('invitePlayer.searchPlaceholder') }}
          </template>
        </el-table>
        <div v-if="total > candidates.length" class="dialog-hint">
          {{ t('invitePlayer.moreHint', { total }) }}
        </div>
        <div class="dialog-hint">{{ t('invitePlayer.genericHint') }}</div>
      </el-tab-pane>

      <el-tab-pane v-if="canRevoke" :label="t('invitePlayer.tabInvites')" name="invites">
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
            <template #default="{ row }">{{ formatDateTimeMinute(row.expiresAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('invitePlayer.colCreated')" width="150">
            <template #default="{ row }">{{ formatDateTimeMinute(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('orderBook.colActions')" width="170" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'active'" link type="primary" @click="copyLink(row.token)">
                {{ t('invitePlayer.copy') }}
              </el-button>
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
          <el-button @click="copyLink()">{{ t('invitePlayer.copy') }}</el-button>
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
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { accountApi } from '../../api/account';
import { usePermission } from '../../composables/usePermission';
import { currentTenantCodeFromUrl, DEFAULT_TENANT_CODE } from '../../utils/tenantRoute';
import { formatDateTimeMinute } from '../../utils/dateExtras';
import { confirmAction } from '../../utils/confirmAction';
import '../../styles/admin-page-head.css';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  teamId: { type: [Number, String], default: null },
  teamName: { type: String, default: '' }
});
const emit = defineEmits(['update:modelValue']);

const { t } = useI18n();
const router = useRouter();
const { hasPerm } = usePermission();

const canRevoke = computed(() => hasPerm('business:team:inviteRevoke'));

const activeTab = ref('search');
const keyword = ref('');
const candidates = ref([]);
const total = ref(0);
const searching = ref(false);
const creating = ref(false);
const invites = ref([]);
const listLoading = ref(false);
const linkVisible = ref(false);
const linkToken = ref('');

/** 邀请链接：经路由解析携带部署基址（BASE_URL）+ 租户段，与门户领取页路由一致。 */
function buildLink(token) {
  if (!token) return '';
  const tenant = currentTenantCodeFromUrl() || DEFAULT_TENANT_CODE;
  const href = router.resolve({
    name: 'PortalPlayerClaimInvite',
    params: { tenantCode: tenant, token }
  }).href;
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  return `${origin}${href}`;
}

const linkPreview = computed(() => buildLink(linkToken.value));

let searchTimer = null;

function clearSearchTimer() {
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      clearSearchTimer();
      return;
    }
    activeTab.value = 'search';
    keyword.value = '';
    candidates.value = [];
    runSearch();
    if (canRevoke.value) loadInvites();
  }
);

watch(keyword, () => {
  clearSearchTimer();
  searchTimer = setTimeout(runSearch, 300);
});

onBeforeUnmount(clearSearchTimer);

async function runSearch() {
  if (props.teamId == null) return;
  searching.value = true;
  try {
    const page = await accountApi.inviteCandidates(props.teamId, {
      keyword: keyword.value || undefined,
      page: 1,
      pageSize: 20
    });
    candidates.value = page.list;
    total.value = page.total;
  } catch {
    candidates.value = [];
    total.value = 0;
  } finally {
    searching.value = false;
  }
}

async function loadInvites() {
  if (props.teamId == null) return;
  listLoading.value = true;
  try {
    invites.value = await accountApi.listClaimInvites(props.teamId);
  } catch (e) {
    invites.value = [];
    ElMessage.error(e?.message ?? t('invitePlayer.loadFailed'));
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
    if (canRevoke.value) await loadInvites();
  } catch (e) {
    ElMessage.error(e?.message ?? t('invitePlayer.createFailed'));
  } finally {
    creating.value = false;
  }
}

async function copyLink(token) {
  const link = token ? buildLink(token) : linkPreview.value;
  try {
    await navigator.clipboard.writeText(link);
    ElMessage.success(t('invitePlayer.copyDone'));
  } catch {
    ElMessage.warning(t('invitePlayer.copyUnsupported'));
  }
}

async function revokeInvite(row) {
  const ok = await confirmAction(t('invitePlayer.revokeConfirm'), t('invitePlayer.revokeConfirmTitle'));
  if (!ok) return;
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
