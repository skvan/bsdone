<template>
  <div class="admin-page team-lineup-games-page">
    <el-card shadow="never" class="team-lineup-games-card">
      <div class="page-head">
        <el-button link type="primary" class="back-btn" @click="goBack">返回赛事报名</el-button>
        <span class="page-title">赛前阵容</span>
        <span v-if="eventName" class="page-sub">· {{ eventName }}</span>
        <span v-if="season" class="page-sub">· {{ season }}</span>
      </div>

      <el-alert
        v-if="gameMode === 'SOFTBALL'"
        type="info"
        :closable="false"
        show-icon
        title="垒球赛事：先发 10-11 人，按赛事规则填写"
        class="mode-hint"
      />

      <el-table v-loading="loading" :data="games" class="team-lineup-games-table">
        <el-table-column label="比赛" min-width="220">
          <template #default="{ row }">
            <div class="game-main">
              <span class="game-opponent">{{ opponentText(row) }}</span>
              <el-tag size="small" effect="plain" :type="row.side === 'home' ? 'primary' : 'info'">
                {{ row.side === 'home' ? '主场' : '客场' }}
              </el-tag>
            </div>
            <div class="game-sub">
              <span v-if="row.gameNumber != null">第 {{ row.gameNumber }} 场</span>
              <span>{{ formatLineupDateTime(row.gameTime || row.gameday) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="场地" min-width="150">
          <template #default="{ row }">{{ row.venue?.trim() || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阵容" min-width="180">
          <template #default="{ row }">
            <div class="lineup-state">
              <el-tag v-if="row.hasSubmission" size="small" :type="row.complete ? 'success' : 'warning'" effect="light">
                {{ row.complete ? '已保存' : '未完成' }}
              </el-tag>
              <span v-else class="muted">未保存</span>
              <span v-if="row.lineupUpdatedAt" class="muted">{{ formatLineupDateTime(row.lineupUpdatedAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGame(row)">编辑阵容</el-button>
          </template>
        </el-table-column>
        <template #empty>该赛事暂无未开始的比赛（比赛由办赛方创建）</template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { teamLineupApi } from '../../api/business';
import { formatLineupDateTime } from '../../utils/teamLineupDraft';

const route = useRoute();
const router = useRouter();
const teamId = computed(() => String(route.params.id ?? ''));
const eventId = computed(() => String(route.params.eventId ?? ''));
const loading = ref(false);
const eventName = ref('');
const season = ref('');
const gameMode = ref('BASEBALL');
const games = ref([]);

async function load() {
  loading.value = true;
  try {
    const data = await teamLineupApi.listGames(teamId.value, eventId.value);
    if (data == null) {
      games.value = [];
      return;
    }
    eventName.value = data.eventName ?? '';
    season.value = data.season ?? '';
    gameMode.value = data.gameMode ?? 'BASEBALL';
    games.value = Array.isArray(data.games) ? data.games : [];
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push({ name: 'AdminTeamEventRegistrations', params: { id: teamId.value } });
}

function openGame(row) {
  router.push({
    name: 'AdminTeamGameLineup',
    params: { id: teamId.value, eventId: eventId.value, gameId: String(row.gameId) }
  });
}

function opponentText(row) {
  return row.opponentName ? `对阵 ${row.opponentName}` : '对手待定';
}

function statusText(status) {
  return status === 'postponed' ? '延期' : '未开始';
}

onMounted(load);
</script>

<style scoped>
.team-lineup-games-page {
  padding: 12px;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.page-sub {
  color: #909399;
  font-size: 13px;
}

.mode-hint {
  margin-bottom: 10px;
}

.game-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.game-opponent {
  font-weight: 600;
  color: #303133;
}

.game-sub,
.lineup-state {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.muted {
  color: #909399;
  font-size: 12px;
}
</style>
