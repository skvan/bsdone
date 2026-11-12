<template>
  <el-drawer
    :model-value="visible"
    title="比赛复核"
    direction="rtl"
    size="520px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="statusReady" class="review-status">
      <el-tag size="small" :type="isFinal ? 'success' : 'info'" effect="plain">
        {{ isFinal ? '比赛已结束 · 赛后复核' : '比赛进行中' }}
      </el-tag>
      <span class="review-status__hint">
        {{ isFinal ? 'ER 复核与守备补记均可操作' : '守备补记需比赛结束后操作' }}
      </span>
    </div>
    <el-tabs v-model="activeTab" class="review-tabs">
      <el-tab-pane label="ER 复核" name="er">
        <GameEarnedRunReviewPanel
          v-if="visible && statusReady"
          :game-id="gameId"
          :game-status="resolvedGameStatus"
          :show-status="false"
          :home-lineup="homeLineup"
          :away-lineup="awayLineup"
          :home-bench="homeBench"
          :away-bench="awayBench"
          :home-roster="homeRoster"
          :away-roster="awayRoster"
        />
      </el-tab-pane>
      <el-tab-pane label="守备补记" name="fielding" lazy>
        <LiveGameFieldingAdjustmentPanel
          v-if="visible && statusReady"
          :visible="visible"
          :game-id="gameId"
          :game-status="resolvedGameStatus"
          :play-events="playEvents"
          :home-lineup="homeLineup"
          :away-lineup="awayLineup"
          :home-bench="homeBench"
          :away-bench="awayBench"
          :home-fielding-pitcher-row="homeFieldingPitcherRow"
          :away-fielding-pitcher-row="awayFieldingPitcherRow"
          :home-roster="homeRoster"
          :away-roster="awayRoster"
        />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { gameApi } from '../../api/business';
import GameEarnedRunReviewPanel from './GameEarnedRunReviewPanel.vue';
import LiveGameFieldingAdjustmentPanel from './LiveGameFieldingAdjustmentPanel.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  gameId: { type: [Number, String], default: null },
  gameStatus: { type: String, default: '' },
  playEvents: { type: Array, default: () => [] },
  homeLineup: { type: Array, default: () => [] },
  awayLineup: { type: Array, default: () => [] },
  homeBench: { type: Array, default: () => [] },
  awayBench: { type: Array, default: () => [] },
  homeFieldingPitcherRow: { type: Object, default: null },
  awayFieldingPitcherRow: { type: Object, default: null },
  homeRoster: { type: Array, default: () => [] },
  awayRoster: { type: Array, default: () => [] }
});
defineEmits(['update:visible']);

const activeTab = ref('er');
const remoteGameStatus = ref('');
const statusReady = ref(false);
const resolvedGameStatus = computed(
  () => String(props.gameStatus || remoteGameStatus.value || '').trim()
);
const isFinal = computed(() => resolvedGameStatus.value.toLowerCase() === 'final');

watch(
  () => [props.visible, props.gameId, props.gameStatus],
  async ([visible, gameId, gameStatus]) => {
    if (!visible) {
      statusReady.value = false;
      return;
    }
    if (gameStatus) {
      remoteGameStatus.value = gameStatus;
      statusReady.value = true;
      return;
    }
    if (!gameId) {
      statusReady.value = true;
      return;
    }
    statusReady.value = false;
    try {
      const game = await gameApi.get(gameId);
      remoteGameStatus.value = game?.status ?? '';
    } catch {
      remoteGameStatus.value = '';
    } finally {
      statusReady.value = true;
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.review-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.review-status__hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.review-tabs {
  margin-top: -4px;
}
</style>
