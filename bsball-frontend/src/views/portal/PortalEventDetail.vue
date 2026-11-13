<template>
  <div class="portal-event-detail">
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty v-else-if="error" :description="error">
      <el-button type="primary" @click="retry">{{ t('common.retry') }}</el-button>
    </el-empty>
    <el-empty v-else-if="!eventInfo" :description="t('portalEventDetail.notFound')" />
    <template v-else>
      <section class="event-detail-card">
        <div class="event-detail-head">
          <div>
            <el-button link type="primary" class="back-link" @click="goBack">
              ← {{ t('portalEventDetail.back') }}
            </el-button>
            <div class="event-title-row">
              <h1>{{ eventInfo.name }}</h1>
              <el-tag :type="statusTagType" effect="light">{{ statusLabel(eventInfo.status) }}</el-tag>
              <el-tag type="warning" effect="plain">{{ formatLabel }}</el-tag>
            </div>
            <div class="event-meta">
              <span v-if="eventInfo.season">{{ eventInfo.season }}</span>
              <span v-if="dateRange">{{ dateRange }}</span>
              <span>{{ t('portalEventDetail.played') }} {{ games.length }}</span>
            </div>
          </div>
        </div>
      </section>

      <el-alert
        v-if="tournament?.warnings?.length"
        class="event-warning"
        type="warning"
        :closable="false"
        show-icon
        :title="t('portalEventDetail.warnings')"
      >
        <div v-for="(warning, index) in tournament.warnings" :key="index">{{ warning }}</div>
      </el-alert>

      <el-tabs v-model="activeTab" class="event-tabs">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name">
          <template v-if="tab.name === 'schedule'">
            <EventScheduleListTable
              v-if="games.length"
              :games="games"
              :team-name="teamName"
              :game-link="gameLink"
              :table-title="t('portalEventDetail.schedule')"
            />
            <el-empty v-else :description="t('portalEventDetail.noGames')" />
          </template>
          <template v-else-if="tab.name === 'standings'">
            <PortalTournamentStandings
              v-if="tournament"
              :groups="tournament.standings || []"
              :points-rule="tournament.pointsRule"
            />
            <el-empty v-else :description="t('portalEventDetail.noStandings')" />
          </template>
          <template v-else>
            <PortalTournamentBracket
              v-if="tournament?.bracket"
              :bracket="tournament.bracket"
              :event-id="eventId"
            />
            <el-empty v-else :description="t('portalEventDetail.noBracket')" />
          </template>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElAlert, ElButton, ElEmpty, ElSkeleton, ElTabPane, ElTabs, ElTag } from 'element-plus';
import { eventApi, teamApi } from '../../api/business';
import { fetchAllPages } from '../../api/request';
import { useDetailLoad } from '../../composables/useDetailLoad';
import { formatSmartDateTime } from '../../utils/dateExtras';
import EventScheduleListTable from '../../components/admin/EventScheduleListTable.js';
import PortalTournamentStandings from '../../components/portal/PortalTournamentStandings.vue';
import PortalTournamentBracket from '../../components/portal/PortalTournamentBracket.vue';

const route = useRoute();
const router = useRouter();
const { t, te } = useI18n();
const activeTab = ref('schedule');
const eventId = computed(() => String(route.params.eventId));
const tenantCode = computed(() => String(route.params.tenantCode || ''));

const { loading, error, result, retry } = useDetailLoad(async () => {
  const id = Number(route.params.eventId);
  if (!Number.isFinite(id) || id <= 0) {
    throw new Error(t('portalEventDetail.notFound'));
  }
  const [event, tournament, gamesResult, teams] = await Promise.all([
    eventApi.get(id),
    eventApi.getTournament(id),
    fetchAllPages('/api/game/list', { eventId: id }),
    teamApi.selectOptions()
  ]);
  if (gamesResult.error) {
    throw new Error(gamesResult.error);
  }
  return {
    event,
    tournament,
    games: gamesResult.list || [],
    teams: teams || []
  };
});
watch(() => route.params.eventId, retry);

const eventInfo = computed(() => result.value?.event ?? null);
const tournament = computed(() => result.value?.tournament ?? null);
const games = computed(() => result.value?.games ?? []);
const teamNames = computed(() =>
  Object.fromEntries((result.value?.teams || []).map((team) => [team.id, team.name]))
);
const formatType = computed(() => tournament.value?.formatType || eventInfo.value?.formatType || 'SINGLE_ROUND_ROBIN');
const isKnockoutOnly = computed(() => formatType.value === 'KNOCKOUT');
const hasBracket = computed(() => ['KNOCKOUT', 'GROUP_KNOCKOUT'].includes(formatType.value));
const tabs = computed(() => {
  const list = [{ name: 'schedule', label: t('portalEventDetail.schedule') }];
  if (!isKnockoutOnly.value) {
    list.push({ name: 'standings', label: t('portalEventDetail.standings') });
  }
  if (hasBracket.value) {
    list.push({ name: 'bracket', label: t('portalEventDetail.bracket') });
  }
  return list;
});
watch(
  tabs,
  (list) => {
    if (!list.some((tab) => tab.name === activeTab.value)) {
      activeTab.value = list[0]?.name || 'schedule';
    }
  },
  { immediate: true }
);

const formatLabel = computed(() => {
  const labels = {
    SINGLE_ROUND_ROBIN: t('portalEventDetail.single'),
    DOUBLE_ROUND_ROBIN: t('portalEventDetail.double'),
    KNOCKOUT: t('portalEventDetail.knockout'),
    GROUP_KNOCKOUT: t('portalEventDetail.groupKnockout')
  };
  return labels[formatType.value] || formatType.value;
});
const statusTagType = computed(() => {
  const status = eventInfo.value?.status;
  if (status === 'ongoing') return 'success';
  if (status === 'ended') return '';
  return 'info';
});
const dateRange = computed(() => {
  if (!eventInfo.value) return '';
  const start = eventInfo.value.startDate ? formatSmartDateTime(eventInfo.value.startDate) : '';
  const end = eventInfo.value.endDate ? formatSmartDateTime(eventInfo.value.endDate) : '';
  if (start && end) return `${start} ~ ${end}`;
  return start || end;
});

function statusLabel(status) {
  const key = `events.eventStatus.${status}`;
  return status && te(key) ? t(key) : status || '';
}

function teamName(teamId) {
  return teamNames.value[teamId] ?? `#${teamId}`;
}

function gameLink(game) {
  return {
    name: 'PortalGameDetail',
    params: {
      tenantCode: tenantCode.value,
      eventId: String(game.eventId || eventId.value),
      gameId: String(game.id)
    }
  };
}

function goBack() {
  router.push({ name: 'PortalEvents', params: { tenantCode: tenantCode.value } });
}
</script>

<style scoped>
.portal-event-detail {
  width: 100%;
}
.event-detail-card {
  padding: 18px 20px;
  margin-bottom: 16px;
  background: var(--portal-card-bg, var(--el-bg-color, #fff));
  border-radius: var(--portal-card-radius, 8px);
  box-shadow: var(--portal-card-shadow, 0 2px 8px rgb(0 0 0 / 6%));
}
.event-detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.back-link {
  margin-bottom: 8px;
  padding-left: 0;
}
.event-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.event-title-row h1 {
  margin: 0;
  font-size: 24px;
  color: var(--el-text-color-primary);
}
.event-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.event-warning {
  margin-bottom: 16px;
}
.event-tabs {
  background: transparent;
}
@media (max-width: 768px) {
  .event-detail-card {
    padding: 14px;
    border-radius: 0;
  }
  .event-title-row h1 {
    font-size: 20px;
  }
}
</style>
