<template>
  <section class="portal-bracket">
    <div class="bracket-head">
      <div>
        <h3>{{ t('portalEventDetail.bracket') }}</h3>
        <p>{{ t('portalEventDetail.bracketSubtitle') }}</p>
      </div>
      <span class="scroll-hint">{{ t('portalEventDetail.scrollHint') }}</span>
    </div>

    <template v-if="rounds.length || bracket.thirdPlace">
      <div v-if="rounds.length" class="bracket-scroll">
        <div class="bracket-canvas" :style="{ width: `${canvasWidth}px`, height: `${canvasHeight}px` }">
          <svg class="bracket-svg" :viewBox="`0 0 ${canvasWidth} ${canvasHeight}`" fill="none">
            <path v-for="(path, index) in connectors" :key="index" :d="path" stroke="currentColor" stroke-width="1.5" />
            <path v-if="championTeam" :d="championConnector" stroke="var(--el-color-warning)" stroke-width="2" />
          </svg>

          <template v-for="(layout, roundIndex) in layouts" :key="roundIndex">
            <div
              v-for="(item, matchIndex) in layout"
              :key="`${roundIndex}-${matchIndex}`"
              class="bracket-match"
              :style="{ left: `${item.x}px`, top: `${item.y}px` }"
              :role="item.match.gameId ? 'link' : undefined"
              :tabindex="item.match.gameId ? 0 : undefined"
              @click="openGame(item.match)"
              @keydown.enter="openGame(item.match)"
            >
              <div class="match-label">
                <span>{{ item.roundName }}<template v-if="item.match.slot"> · {{ item.match.slot }}</template></span>
                <span v-if="item.match.gameNumber != null">G{{ item.match.gameNumber }}</span>
              </div>
              <div
                class="match-team"
                :class="teamClass(item.match, item.match.homeTeamId)"
              >
                <span class="match-name">{{ item.match.homeTeamName || t('portalEventDetail.tbd') }}</span>
                <span class="match-score">{{ item.match.homeScore ?? '—' }}</span>
              </div>
              <div
                class="match-team"
                :class="teamClass(item.match, item.match.awayTeamId)"
              >
                <span class="match-name">{{ item.match.awayTeamName || t('portalEventDetail.tbd') }}</span>
                <span class="match-score">{{ item.match.awayScore ?? '—' }}</span>
              </div>
            </div>
          </template>

          <div
            v-if="championTeam"
            class="champion-card"
            :style="{ left: `${championX}px`, top: `${championY}px` }"
          >
            <div class="champion-kicker">CHAMPION</div>
            <div class="champion-name">{{ championTeam }}</div>
            <div class="champion-sub">{{ t('portalEventDetail.champion') }}</div>
          </div>
        </div>
      </div>

      <div v-if="bracket.thirdPlace" class="third-place-section">
        <div class="third-place-label">{{ t('portalEventDetail.thirdPlace') }}</div>
        <div
          class="bracket-match third-place-card"
          :role="bracket.thirdPlace.gameId ? 'link' : undefined"
          :tabindex="bracket.thirdPlace.gameId ? 0 : undefined"
          @click="openGame(bracket.thirdPlace)"
          @keydown.enter="openGame(bracket.thirdPlace)"
        >
          <div class="match-label">
            <span>{{ t('portalEventDetail.thirdPlace') }}</span>
            <span v-if="bracket.thirdPlace.gameNumber != null">G{{ bracket.thirdPlace.gameNumber }}</span>
          </div>
          <div class="match-team" :class="teamClass(bracket.thirdPlace, bracket.thirdPlace.homeTeamId)">
            <span class="match-name">{{ bracket.thirdPlace.homeTeamName || t('portalEventDetail.tbd') }}</span>
            <span class="match-score">{{ bracket.thirdPlace.homeScore ?? '—' }}</span>
          </div>
          <div class="match-team" :class="teamClass(bracket.thirdPlace, bracket.thirdPlace.awayTeamId)">
            <span class="match-name">{{ bracket.thirdPlace.awayTeamName || t('portalEventDetail.tbd') }}</span>
            <span class="match-score">{{ bracket.thirdPlace.awayScore ?? '—' }}</span>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else :description="t('portalEventDetail.noBracket')" />
  </section>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElEmpty } from 'element-plus';

const props = defineProps({
  bracket: { type: Object, required: true },
  eventId: { type: [String, Number], required: true }
});

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const tenantCode = computed(() => String(route.params.tenantCode || ''));
const rounds = computed(() => props.bracket?.rounds || []);
const cardWidth = 230;
const cardHeight = 72;
const gapX = 70;
const baseGap = 110;

const layouts = computed(() =>
  rounds.value.map((round, roundIndex) => {
    const pow = Math.pow(2, roundIndex);
    return (round.matches || []).map((match, matchIndex) => {
      const slot = Number(match.slot) || matchIndex + 1;
      return {
        match,
        roundName: round.name,
        x: roundIndex * (cardWidth + gapX),
        y: baseGap * pow * (slot - 1) + (baseGap * (pow - 1)) / 2
      };
    });
  })
);

const canvasWidth = computed(() => {
  const extra = championTeam.value ? 270 : 20;
  return rounds.value.length * (cardWidth + gapX) + extra;
});
const canvasHeight = computed(() => {
  const maxY = layouts.value.reduce((height, round) => {
    for (const item of round) {
      height = Math.max(height, item.y + cardHeight);
    }
    return height;
  }, cardHeight);
  return maxY + 24;
});

const connectors = computed(() => {
  const paths = [];
  for (let roundIndex = 0; roundIndex < layouts.value.length - 1; roundIndex++) {
    const source = layouts.value[roundIndex];
    const target = layouts.value[roundIndex + 1];
    for (let i = 0; i < source.length; i++) {
      const next = target[Math.floor(i / 2)];
      if (!next) continue;
      const sx = source[i].x + cardWidth;
      const sy = source[i].y + cardHeight / 2;
      const tx = next.x;
      const ty = next.y + cardHeight / 2;
      const mx = (sx + tx) / 2;
      paths.push(`M${sx},${sy} H${mx} V${ty} H${tx}`);
    }
  }
  return paths;
});

const finalLayout = computed(() => {
  if (!layouts.value.length) return null;
  const finalRoundIndex = rounds.value.findIndex((round) => round.code === 'FINAL');
  const index = finalRoundIndex >= 0 ? finalRoundIndex : layouts.value.length - 1;
  return layouts.value[index]?.[0] || null;
});
const championTeam = computed(() => {
  if (props.bracket?.championTeamId == null) return '';
  const championId = props.bracket.championTeamId;
  const finalMatch = finalLayout.value?.match;
  if (finalMatch) {
    if (finalMatch.homeTeamId === championId) return finalMatch.homeTeamName;
    if (finalMatch.awayTeamId === championId) return finalMatch.awayTeamName;
  }
  for (const round of rounds.value) {
    for (const match of round.matches || []) {
      if (match.homeTeamId === championId) return match.homeTeamName;
      if (match.awayTeamId === championId) return match.awayTeamName;
    }
  }
  return '';
});
const championX = computed(() => rounds.value.length * (cardWidth + gapX) + 10);
const championY = computed(() => (finalLayout.value ? Math.max(0, finalLayout.value.y + cardHeight / 2 - 45) : 0));
const championConnector = computed(() => {
  if (!finalLayout.value || !championTeam.value) return '';
  const sx = finalLayout.value.x + cardWidth;
  const sy = finalLayout.value.y + cardHeight / 2;
  return `M${sx},${sy} H${championX.value}`;
});

function teamClass(match, teamId) {
  if (match.winnerTeamId == null) return '';
  return match.winnerTeamId === teamId ? 'winner' : 'loser';
}

function openGame(match) {
  if (!match?.gameId) return;
  router.push({
    name: 'PortalGameDetail',
    params: {
      tenantCode: tenantCode.value,
      eventId: String(props.eventId),
      gameId: String(match.gameId)
    }
  });
}
</script>

<style scoped>
.portal-bracket {
  background: var(--portal-card-bg, var(--el-bg-color, #fff));
  border-radius: var(--portal-card-radius, 8px);
  padding: 20px;
}
.bracket-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.bracket-head h3 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}
.bracket-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.scroll-hint {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.bracket-scroll {
  overflow-x: auto;
  padding-bottom: 8px;
}
.bracket-canvas {
  position: relative;
  min-width: 100%;
}
.bracket-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  color: var(--el-border-color);
  pointer-events: none;
}
.bracket-match {
  position: absolute;
  width: 230px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 7px;
  box-shadow: 0 4px 14px rgb(0 0 0 / 6%);
  overflow: hidden;
  cursor: pointer;
}
.match-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 24px;
  padding: 0 10px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-secondary);
  font-size: 10px;
}
.match-team {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 29px;
  padding: 0 10px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}
.match-team + .match-team {
  border-top: 1px solid var(--el-border-color-lighter);
}
.match-team.winner {
  background: var(--el-color-warning-light-9);
  box-shadow: inset 3px 0 0 var(--el-color-warning);
}
.match-team.winner .match-name,
.match-team.winner .match-score {
  color: var(--el-text-color-primary);
  font-weight: 800;
}
.match-team.winner .match-score {
  color: var(--el-color-success);
}
.match-team.loser .match-name,
.match-team.loser .match-score {
  color: var(--el-text-color-placeholder);
}
.match-slot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--el-fill-color);
  color: var(--el-text-color-placeholder);
  font-size: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.match-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.match-score {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.champion-card {
  position: absolute;
  width: 240px;
  padding: 14px 16px;
  border: 1px solid var(--el-color-warning);
  border-radius: 8px;
  background: linear-gradient(135deg, #1b2637, #0f1a2b);
  color: #fff;
  box-shadow: 0 12px 28px rgb(15 26 43 / 22%);
}
.champion-kicker {
  color: var(--el-color-warning);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.5px;
}
.champion-name {
  margin-top: 5px;
  font-size: 18px;
  font-weight: 800;
}
.champion-sub {
  margin-top: 3px;
  color: rgb(255 255 255 / 58%);
  font-size: 11px;
}
.third-place-section {
  margin-top: 18px;
}
.third-place-label {
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
}
.third-place-card {
  position: relative;
}
@media (max-width: 768px) {
  .portal-bracket {
    padding: 12px;
    border-radius: 0;
  }
}
</style>
