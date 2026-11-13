<template>
  <section class="tournament-standings">
    <div v-if="groupTabs.length > 1" class="group-switch">
      <el-radio-group v-model="activeGroupKey" size="small">
        <el-radio-button v-for="group in groupTabs" :key="group.key" :value="group.key">
          {{ group.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <template v-if="activeGroup && activeGroup.rows.length">
      <div class="standings-head">
        <div>
          <h3>{{ activeGroup.groupName || t('portalEventDetail.standingsTitle') }}</h3>
          <p>{{ groupSubtitle }}</p>
        </div>
        <div class="standings-legend">
          <span class="rule-chip">{{ pointsRuleText }}</span>
          <span class="legend-item"><i class="dot dot-win"></i>{{ t('portalEventDetail.winScore') }}</span>
          <span class="legend-item"><i class="dot dot-loss"></i>{{ t('portalEventDetail.lossScore') }}</span>
          <span class="legend-item"><i class="dot dot-tie"></i>{{ t('portalEventDetail.tieScore') }}</span>
        </div>
      </div>

      <div class="table-scroll">
        <table class="combined-table">
          <thead>
            <tr>
              <th rowspan="2" class="rank-col">{{ t('portalEventDetail.rank') }}</th>
              <th rowspan="2" class="team-col">{{ t('portalEventDetail.team') }}</th>
              <th colspan="7" class="group-head">{{ t('portalEventDetail.pointsData') }}</th>
              <th :colspan="columns.length" class="group-head">{{ t('portalEventDetail.resultsData') }}</th>
            </tr>
            <tr>
              <th>{{ t('portalEventDetail.played') }}</th>
              <th>{{ t('portalEventDetail.points') }}</th>
              <th>{{ t('portalEventDetail.winPct') }}</th>
              <th>{{ t('portalEventDetail.gamesBehind') }}</th>
              <th>{{ t('portalEventDetail.runsFor') }}</th>
              <th>{{ t('portalEventDetail.runsAgainst') }}</th>
              <th>{{ t('portalEventDetail.runDiff') }}</th>
              <th v-for="team in columns" :key="team.teamId" class="opp-head">
                <span class="opp-team">
                  <el-avatar :size="18" shape="square" :src="logoSrc(team.teamLogo)">
                    {{ initial(team.teamName) }}
                  </el-avatar>
                  <span>{{ team.teamName }}</span>
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in activeGroup.rows" :key="row.teamId" :class="{ qualified: row.qualified }">
              <td><span class="rank" :class="rankClass(row.rank)">{{ row.rank }}</span></td>
              <td class="team-cell">
                <div class="team-cell-inner">
                  <el-avatar :size="28" shape="square" :src="logoSrc(row.teamLogo)">
                    {{ initial(row.teamName) }}
                  </el-avatar>
                  <span class="team-name-wrap">
                    <router-link v-if="tenantCode" class="team-link" :to="teamRoute(row.teamId)">
                      {{ row.teamName }}
                    </router-link>
                    <span v-else>{{ row.teamName }}</span>
                    <span v-if="row.qualified" class="qual-tag">{{ t('portalEventDetail.qualified') }}</span>
                  </span>
                </div>
              </td>
              <td>{{ row.played }}</td>
              <td class="points">{{ row.points }}</td>
              <td>{{ formatPct(row.winPct) }}</td>
              <td>{{ formatGb(row) }}</td>
              <td>{{ row.runsFor }}</td>
              <td>{{ row.runsAgainst }}</td>
              <td :class="diffClass(row.runDiff)">{{ signed(row.runDiff) }}</td>
              <td
                v-for="col in columns"
                :key="col.teamId"
                class="opp-cell"
                :class="{ 'diag-cell': row.teamId === col.teamId }"
                :title="row.teamId === col.teamId ? t('portalEventDetail.sameTeam') : undefined"
              >
                <template v-if="row.teamId !== col.teamId">
                  <div v-for="(leg, index) in cellLegs(row.teamId, col.teamId)" :key="index" class="leg-line">
                    <span :class="scoreClass(leg.left, leg.right)">{{ leg.left }}</span>
                    <span class="sep">:</span>
                    <span :class="scoreClass(leg.right, leg.left)">{{ leg.right }}</span>
                  </div>
                  <span v-if="cellLegs(row.teamId, col.teamId).length === 0" class="no-result">—</span>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="table-note">{{ t('portalEventDetail.matrixNote') }}</p>
    </template>

    <el-empty v-else :description="t('portalEventDetail.noStandings')" />
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElAvatar, ElEmpty, ElRadioButton, ElRadioGroup } from 'element-plus';
import { resolveAssetUrl } from '../../api/request';

const props = defineProps({
  groups: { type: Array, default: () => [] },
  pointsRule: { type: Object, default: () => ({ win: 2, draw: 1, loss: 0 }) }
});

const { t } = useI18n();
const route = useRoute();
const tenantCode = computed(() => String(route.params.tenantCode || ''));
const activeGroupKey = ref('');

const groupTabs = computed(() =>
  (props.groups || []).map((group, index) => ({
    key: group.groupCode || `group-${index}`,
    label: group.groupName || t('portalEventDetail.unassignedGroup')
  }))
);
watch(
  groupTabs,
  (tabs) => {
    if (!tabs.some((tab) => tab.key === activeGroupKey.value)) {
      activeGroupKey.value = tabs[0]?.key || '';
    }
  },
  { immediate: true }
);
const activeGroup = computed(() =>
  (props.groups || []).find(
    (group, index) => (group.groupCode || `group-${index}`) === activeGroupKey.value
  ) || null
);
const columns = computed(() => activeGroup.value?.rows || []);
const groupSubtitle = computed(() => {
  if (!activeGroup.value) return '';
  return activeGroup.value.advanceCount > 0
    ? t('portalEventDetail.groupSubtitle', { n: activeGroup.value.advanceCount })
    : t('portalEventDetail.roundRobinSubtitle');
});
const pointsRuleText = computed(() => {
  const rule = props.pointsRule || {};
  return t('portalEventDetail.pointsRule', {
    win: rule.win ?? 2,
    draw: rule.draw ?? 1,
    loss: rule.loss ?? 0
  });
});

const legsByPair = computed(() => {
  const map = new Map();
  for (const match of activeGroup.value?.matches || []) {
    if (match.homeTeamId == null || match.awayTeamId == null) continue;
    const key = pairKey(match.homeTeamId, match.awayTeamId);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(match);
  }
  return map;
});

function pairKey(left, right) {
  return Number(left) < Number(right) ? `${left}:${right}` : `${right}:${left}`;
}

function cellLegs(rowTeamId, colTeamId) {
  const matches = legsByPair.value.get(pairKey(rowTeamId, colTeamId)) || [];
  return matches.map((match) =>
    match.homeTeamId === rowTeamId
      ? { left: match.homeScore, right: match.awayScore }
      : { left: match.awayScore, right: match.homeScore }
  );
}

function scoreClass(own, other) {
  if (Number(own) === Number(other)) return 'score-tie';
  return Number(own) > Number(other) ? 'score-win' : 'score-loss';
}

function diffClass(diff) {
  if (Number(diff) > 0) return 'positive';
  if (Number(diff) < 0) return 'negative';
  return 'zero';
}

function signed(value) {
  const number = Number(value) || 0;
  return number > 0 ? `+${number}` : String(number);
}

function rankClass(rank) {
  return rank === 1 ? 'r1' : rank === 2 ? 'r2' : rank === 3 ? 'r3' : '';
}

function formatPct(value) {
  const number = Number(value) || 0;
  return number >= 1 ? '1.000' : number.toFixed(3).replace(/^0/, '');
}

function formatGb(row) {
  if (row.rank === 1 || row.gamesBehind == null) return '—';
  return Number(row.gamesBehind).toFixed(1);
}

function logoSrc(logo) {
  return logo ? resolveAssetUrl(logo) : undefined;
}

function initial(name) {
  return (name || '?').slice(0, 1);
}

function teamRoute(teamId) {
  return {
    name: 'PortalTeamDetail',
    params: { tenantCode: tenantCode.value, id: String(teamId) }
  };
}
</script>

<style scoped>
.tournament-standings {
  background: var(--portal-card-bg, var(--el-bg-color, #fff));
  border-radius: var(--portal-card-radius, 8px);
  padding: 20px;
}
.group-switch {
  margin-bottom: 14px;
}
.standings-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.standings-head h3 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}
.standings-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.standings-legend {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.rule-chip {
  padding: 4px 7px;
  border: 1px solid var(--el-border-color);
  border-radius: 3px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}
.dot-win {
  background: var(--el-color-success);
}
.dot-loss {
  background: var(--el-color-danger);
}
.dot-tie {
  background: var(--el-text-color-placeholder);
}
.table-scroll {
  overflow-x: auto;
}
.combined-table {
  width: 100%;
  min-width: 960px;
  border-collapse: collapse;
  font-variant-numeric: tabular-nums;
}
.combined-table th,
.combined-table td {
  text-align: center;
  padding: 9px 7px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
  white-space: nowrap;
}
.combined-table th {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 600;
}
.combined-table .group-head {
  color: var(--el-text-color-regular);
  border-left: 1px solid var(--el-border-color-lighter);
}
.combined-table .rank-col {
  width: 52px;
}
.combined-table .team-col {
  width: 200px;
  text-align: left;
  padding-left: 16px;
}
.combined-table .opp-head {
  min-width: 100px;
}
.opp-team {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 96px;
}
.opp-team span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
}
.combined-table tbody tr:last-child td {
  border-bottom: none;
}
.combined-table tbody tr.qualified td {
  background: var(--el-color-warning-light-9);
}
.combined-table tbody tr.qualified td:first-child {
  box-shadow: inset 3px 0 0 var(--el-color-warning);
}
.combined-table .team-cell {
  text-align: left;
  padding-left: 16px;
}
.team-cell-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}
.team-name-wrap {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}
.team-link {
  color: var(--el-text-color-primary);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
}
.team-link:hover {
  color: var(--el-color-primary);
}
.qual-tag {
  padding: 1px 4px;
  border-radius: 2px;
  background: var(--el-color-warning-light-8);
  color: var(--el-color-warning-dark-2);
  font-size: 10px;
}
.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 700;
  background: var(--el-bg-color);
}
.rank.r1 {
  background: linear-gradient(135deg, #e4c77e, #c9a45b);
  border-color: #d6b66e;
  color: #4c3a12;
}
.rank.r2 {
  background: linear-gradient(135deg, #e7ebef, #cbd2da);
  border-color: #d5dbe2;
  color: #4b5563;
}
.rank.r3 {
  background: linear-gradient(135deg, #e5c3a6, #c98d5f);
  border-color: #d6a477;
  color: #4a2e1b;
}
.points {
  font-size: 15px !important;
  font-weight: 800;
  color: var(--el-text-color-primary);
}
.positive {
  color: var(--el-color-success);
  font-weight: 600;
}
.negative {
  color: var(--el-color-danger);
  font-weight: 600;
}
.zero {
  color: var(--el-text-color-secondary);
}
.opp-cell {
  border-left: 1px solid var(--el-border-color-lighter);
  min-width: 100px;
}
.leg-line {
  line-height: 1.35;
}
.score-win {
  color: var(--el-color-success);
  font-weight: 700;
}
.score-loss {
  color: var(--el-color-danger);
  font-weight: 700;
}
.score-tie {
  color: var(--el-text-color-primary);
  font-weight: 700;
}
.sep {
  color: var(--el-text-color-placeholder);
  margin: 0 1px;
}
.no-result {
  color: var(--el-text-color-placeholder);
}
.diag-cell {
  position: relative;
  background: var(--el-fill-color-lighter);
}
.diag-cell::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top right,
    transparent calc(50% - 0.75px),
    var(--el-border-color-dark) calc(50% - 0.75px),
    var(--el-border-color-dark) calc(50% + 0.75px),
    transparent calc(50% + 0.75px)
  );
  pointer-events: none;
}
.table-note {
  margin: 10px 2px 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
@media (max-width: 768px) {
  .tournament-standings {
    padding: 12px;
    border-radius: 0;
  }
  .standings-head {
    align-items: flex-start;
  }
}
</style>
