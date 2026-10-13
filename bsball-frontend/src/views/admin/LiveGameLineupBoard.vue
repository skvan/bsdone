<template>
  <div v-loading="loading" class="admin-page live-game-lineup-board">
    <div class="lineup-board-header">
      <span class="header-left">确认先发阵容 · {{ eventName }}</span>
      <div class="header-actions">
        <el-button v-if="hasPerm('business:lineup-template:manage')" @click="openTemplates(activeTeam)">应用阵容模板</el-button>
        <el-button v-if="gameStatus !== 'live'" :disabled="!canSave || saving" @click="save(false)">保存</el-button>
        <el-button v-if="gameStatus !== 'live'" :disabled="!canSave || saving" @click="save(true)">保存并退出</el-button>
        <el-button type="primary" :disabled="!canStartEntry" :loading="saving" @click="startEntry">开始录入</el-button>
      </div>
    </div>

    <!-- 设置区：与旧件同字段同校验 -->
    <el-form :model="setupForm" label-width="90px" class="setup-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="比赛模式">
            <el-select v-model="mode" placeholder="比赛模式" disabled style="width: 100%">
              <el-option label="棒球" value="BASEBALL" />
              <el-option label="垒球" value="SOFTBALL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="场地">
            <el-select v-model="setupForm.venue" placeholder="选择球场" clearable filterable style="width: 100%">
              <el-option v-for="s in stadiums" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="客队" required>
            <el-select
              v-model="setupForm.awayTeamId"
              placeholder="选择客队"
              filterable
              style="width: 100%"
              @change="loadRosters"
            >
              <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="主队" required>
            <el-select
              v-model="setupForm.homeTeamId"
              placeholder="选择主队"
              filterable
              style="width: 100%"
              @change="loadRosters"
            >
              <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="比赛时间">
            <el-date-picker
              v-model="gameTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择比赛时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">{{ rosterHint }}</el-divider>

    <el-radio-group v-model="activeTeam" class="team-switch">
      <el-radio-button value="away">{{ awayTeamName }}（客）</el-radio-button>
      <el-radio-button value="home">{{ homeTeamName }}（主）</el-radio-button>
    </el-radio-group>

    <div v-if="pending" class="lineup-pending-hint">
      待放置：{{ pending.player.name }}｜点卡槽落位；再点卡片或空白处取消
    </div>

    <div class="lineup-board" :class="{ 'is-mobile': isMobile }">
      <LineupFieldBoard
        ref="fieldBoard"
        :mode="mode"
        :slots="fieldSlotViews"
        :hover-code="hoverSlotCode"
        @slot-click="onSlotClick"
      />
      <LineupBattingOrder
        ref="battingRef"
        :rows="board.rows.value"
        :error-indices="errorIndices"
        :max-count="board.maxCount.value"
        :drop-hover="isBattingHover"
        @reorder="({ from, to }) => board.reorderBatting(from, to)"
        @pick="(id) => onPick(findPlayer(id))"
      />
      <LineupPoolPanel
        ref="poolRef"
        :pool="board.pool.value"
        :bench="board.bench.value"
        :roster="board.roster.value"
        :pending-player-id="pending ? pending.player.id : null"
        :hover-kind="hoverPanelKind"
        :lineup-ids="board.rows.value.filter((r) => !isPlaceholder(r)).map((r) => r.id)"
        @pick="onPick"
        @batch-change="(ids) => board.setBench(ids)"
      />
    </div>

    <div v-if="!board.validation.value.ok && board.validation.value.msg" class="lineup-warn">{{ board.validation.value.msg }}</div>
    <div v-if="board.fieldingPitcherRequired.value && !board.fieldingPitcherOk.value" class="lineup-warn">须指定先发投手后才可开始录入</div>

    <!-- 模板对话框：应用前二次确认 -->
    <el-dialog v-model="templateDialog.visible" title="应用阵容模板" width="420px" :close-on-click-modal="false">
      <el-select v-model="templateDialog.selected" placeholder="选择阵容模板" filterable style="width: 100%">
        <el-option v-for="t in templateDialog.list" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <template #footer>
        <el-button @click="templateDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="templateDialog.selected == null" @click="applyTemplate">应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, vLoading } from 'element-plus';
import { eventApi, gameApi, gameStatsApi, teamApi, fetchAllStadiums, fetchPlayersByTeam, fetchRosterPlayers, lineupTemplateApi } from '../../api/business';
import { filterOutOfRoster, filterTemplateByPool, pickPoolId } from '../../utils/rosterPool';
import { isBusinessError } from '../../utils/apiError';
import { usePermission } from '../../composables/usePermission';
import { useTabsStore } from '../../stores/tabs';
import { useMediaQuery } from '../../composables/useMediaQuery';
import { useLineupBoard } from '../../composables/useLineupBoard';
import { useLineupDrag } from '../../composables/useLineupDrag';
import { slotsForMode } from '../../utils/lineupFieldPositions';
import { isPlaceholder, findRowIndex, posOf } from '../../utils/lineupBoardRules';
import { buildLineupDraft, parseLineupDraft, resolveLineupResumeInit, splitGameTime, toLocalGameTime } from '../../utils/lineupDraft';
import { writeEntryBootstrap } from '../../utils/liveGameStorage';
import LineupFieldBoard from '../../components/admin/LineupFieldBoard.vue';
import LineupBattingOrder from '../../components/admin/LineupBattingOrder.vue';
import LineupPoolPanel from '../../components/admin/LineupPoolPanel.vue';
import '../../styles/lineup-board.css';

// 兜底：保证 v-loading 指令在 <script setup> 下可解析（与全局注册等价）
void vLoading;

const route = useRoute();
const router = useRouter();
const tabs = useTabsStore();
const { hasPerm } = usePermission();
const isMobile = useMediaQuery('(max-width: 768px)');

const eventId = computed(() => Number(route.params.eventId));
const gameId = computed(() => {
  const n = Number(route.params.gameId);
  return Number.isFinite(n) && n > 0 ? n : null;
});

const loading = ref(true);
const saving = ref(false);
const mode = ref('BASEBALL');
const eventName = ref('');
const game = ref(null);
const currentGameId = ref(null);
const gameStatus = ref('');
const teams = ref([]);
const stadiums = ref([]);
const setupForm = ref({ homeTeamId: undefined, awayTeamId: undefined, venue: '' });
const gameTime = ref('');
const activeTeam = ref('away');
const pending = ref(null);          // 点选点放：{ player }
const templateDialog = ref({ visible: false, team: 'away', list: [], selected: null });
const unavailable = ref({ home: [], away: [] });   // 快照透传，无 UI
const dragHover = ref(null);        // 拖拽中指针所在的可释放区域（null = 不在任何区域）

// 与旧件 el() 文案逐字一致
const rosterHint = computed(() => mode.value === 'SOFTBALL'
  ? '阵容确认（垒球：10-11 人；10 人为「P+八守位+XF」或「DH+八守位+XF」；11 人为再加 DH 都上场打击）'
  : '阵容确认（棒球：9-10 人；9 人可为「P+八守位」或「DH+八守位」；10 人为「P+八守位+DH」）');

const awayBoard = useLineupBoard(mode, computed(() => setupForm.value.awayTeamId ?? null));
const homeBoard = useLineupBoard(mode, computed(() => setupForm.value.homeTeamId ?? null));
const boardOf = (team) => (team === 'away' ? awayBoard : homeBoard);
const board = computed(() => boardOf(activeTeam.value));

const teamsReady = computed(() =>
  setupForm.value.homeTeamId != null &&
  setupForm.value.awayTeamId != null &&
  setupForm.value.homeTeamId !== setupForm.value.awayTeamId
);
const canSave = computed(() => teamsReady.value);
const canStartEntry = computed(() => teamsReady.value && awayBoard.lineupComplete.value && homeBoard.lineupComplete.value);

const homeTeamName = computed(() => teams.value.find((t) => t.id === setupForm.value.homeTeamId)?.name ?? '主队');
const awayTeamName = computed(() => teams.value.find((t) => t.id === setupForm.value.awayTeamId)?.name ?? '客队');

const errorIndices = computed(() => board.value.validation.value.errorRowIndices ?? []);

// R1：球场卡槽视图（9 守备位 + DH）
const fieldSlotViews = computed(() => {
  const b = board.value;
  const rows = b.rows.value;
  const filledRows = rows.filter((r) => !isPlaceholder(r));
  const errIdx = b.validation.value.errorRowIndices ?? [];
  // 当前「持牌」球员 id：球场卡命中它 = 拿起态描边
  const pickedId = pending.value?.player ? String(pending.value.player.id) : null;
  return slotsForMode(mode.value).map((slot) => {
    const code = slot.code;
    const rowIndex = rows.findIndex((r) => !isPlaceholder(r) && posOf(r) === code);
    const row = rowIndex >= 0 ? rows[rowIndex] : null;
    let player = row;
    let badge = '';
    // P 槽为空但已指定仅守备投手 → 悬停显示
    if (!player && code === 'P' && b.fieldingPitcherId.value != null) {
      player = b.roster.value.find((p) => String(p.id) === String(b.fieldingPitcherId.value)) ?? null;
      if (player) badge = '仅守备';
    }
    const orderIndex = row ? filledRows.findIndex((r) => String(r.id) === String(row.id)) + 1 : 0;
    const error = rowIndex >= 0 && errIdx.includes(rowIndex);
    const selfPending = !!player && String(player.id) === pickedId;
    return { code, player: player ?? null, orderIndex, badge, error, pending: pendingAccepts(code), selfPending };
  });
});

// 待放置球员能否落到该槽（用于卡槽呼吸高亮）
function pendingAccepts(code) {
  const p = pending.value?.player;
  if (!p) return false;
  const b = board.value;
  const inRows = b.rows.value.some((r) => !isPlaceholder(r) && String(r.id) === String(p.id));
  const occ = b.rows.value.find((r) => !isPlaceholder(r) && posOf(r) === code) ?? null;
  if (code === 'DH') return !occ || String(occ.id) === String(p.id);
  if (occ) return true;       // 占用槽：顶替 / 对调
  if (inRows) return true;    // 已在打线：换位置
  return !b.battingFull.value; // 空槽：未满员才可新增
}

/* ---------------- 拖拽装配（interactjs 委托绑定） ---------------- */
const fieldBoard = ref(null);
const battingRef = ref(null);
const poolRef = ref(null);

// 拖拽悬停态分发：指针所在区域高亮「松手会落在这里」（无高亮 = 松手即移出阵容）
const hoverSlotCode = computed(() => (dragHover.value?.kind === 'slot' ? dragHover.value.code : ''));
const isBattingHover = computed(() => dragHover.value?.kind === 'batting');
const hoverPanelKind = computed(() =>
  dragHover.value?.kind === 'bench' || dragHover.value?.kind === 'pool' ? dragHover.value.kind : ''
);

const { bind, unbind, wasRecentDrag } = useLineupDrag({
  onDrop(el, target) {
    const playerId = el.dataset.playerId;
    if (playerId == null) return;
    const player = findPlayer(playerId);
    if (!player) return;
    handleDrop(player, target);
  },
  // 指针一移动即进入拖拽语境 → 退出待放置（纯长按原地释放 = 点击，不在此清，保证点选的切换/取消语义）
  onHover(target) {
    if (pending.value) clearPending();
    dragHover.value = target;
  },
});

function findPlayer(playerId) {
  const b = board.value;
  return (
    b.rows.value.find((r) => String(r.id) === String(playerId)) ??
    b.bench.value.find((p) => String(p.id) === String(playerId)) ??
    b.roster.value.find((p) => String(p.id) === String(playerId)) ??
    null
  );
}

// R2：三个组件均 expose 返回原生元素的方法
function getLayout() {
  const battingEl = battingRef.value?.getListEl?.() ?? null;
  const benchEl = poolRef.value?.getBenchEl?.() ?? null;
  const poolEl = poolRef.value?.getPoolEl?.() ?? null;
  const toRect = (el) => {
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { left: r.left, top: r.top, width: r.width, height: r.height };
  };
  return {
    slotRects: fieldBoard.value?.getSlotRects?.() ?? [],
    battingRect: toRect(battingEl),
    benchRect: toRect(benchEl),
    poolRect: toRect(poolEl),
  };
}

function handleDrop(player, target) {
  const b = board.value;
  if (target.kind === 'slot') b.placeAtSlot(player, target.code);
  else if (target.kind === 'batting') b.addToBatting(player);
  else if (target.kind === 'bench') {
    // 决策（DH / 打线内 / 仅守备投手 / 纯池球员）由规则层统一处理
    b.sendToBench(player.id);
  }
  else if (target.kind === 'pool') {
    // 替补卡片拖回池 = 从替补名单移除（sendBackToBench 是「加入替补」，不可混用）
    if (b.bench.value.some((p) => String(p.id) === String(player.id))) {
      b.setBench(b.bench.value.filter((p) => String(p.id) !== String(player.id)).map((p) => p.id));
    }
  }
  else if (target.kind === 'outside') {
    // 松手处不在任何可释放区域（无高亮）= 移出：阵容内球员（含 DH / 仅守备投手）回替补；
    // 池/替补卡片落在空白处 = 取消（无副作用）
    const inLineup =
      findRowIndex(b.rows.value, player.id) >= 0 ||
      (b.fieldingPitcherId.value != null && String(b.fieldingPitcherId.value) === String(player.id));
    if (inLineup) b.sendToBench(player.id);
  }
  showNotice(b);
}

/* ---------------- 点选点放 ---------------- */
function onPick(player) {
  if (wasRecentDrag()) return; // 拖拽结束后浏览器会补发一次 click → 与点选点放互斥（Task 1 spike 实测）
  if (pending.value && String(pending.value.player.id) === String(player.id)) return clearPending();
  pending.value = { player };
}
// 点卡片/卡槽以外区域 = 取消待放置（设计稿 §四.3 修订：池/替补卡、球场卡槽、打线行的点击均已 stop）
function onDocClick() {
  if (!pending.value) return;
  clearPending();
}
// 卡槽单击（球场卡槽 @click.stop 到达）：持牌 → 落位/顶替/对调；无牌且该槽有人 → 「拿起」进入待放置
// （2026-10-12 负责人口径：单击已落位卡片=拿起；原「操作面板」已移除，功能由拖拽与拿起覆盖）
// 拿起取「卡槽视图的 player」而非 slotOfRow：覆盖仅守备投手（无打线行、由 fieldingPitcherId 合成）的情形
function onSlotClick(code) {
  if (pending.value) {
    board.value.placeAtSlot(pending.value.player, code);
    showNotice(board.value);
    clearPending();
    return;
  }
  const occ = fieldSlotViews.value.find((s) => s.code === code)?.player ?? null;
  if (occ) onPick(occ);
}
function clearPending() { pending.value = null; }
function showNotice(b) {
  const msg = b.consumeNotice();
  if (msg) ElMessage.warning(msg);
}

/* ---------------- 名册 / 比赛载入 ---------------- */
// 电子秩序册（Issue #310）：受限赛事选人池=报名名单；未受限回落本队全量在役
const rosterPools = ref({ home: null, away: null });

async function loadRosters() {
  if (!setupForm.value.homeTeamId || !setupForm.value.awayTeamId) return;
  let [homePool, awayPool] = await Promise.all([
    fetchRosterPlayers(eventId.value, setupForm.value.homeTeamId),
    fetchRosterPlayers(eventId.value, setupForm.value.awayTeamId),
  ]);
  if (homePool.failed || awayPool.failed) {
    // 秩序册服务不可用：回落本队全名册保持可用（保存时门①/门②仍兜底），并提示
    ElMessage.warning('未能加载赛事秩序册名单，已回退本队全名册（保存时仍将校验）');
    homePool = { restricted: false, players: await fetchPlayersByTeam(setupForm.value.homeTeamId) };
    awayPool = { restricted: false, players: await fetchPlayersByTeam(setupForm.value.awayTeamId) };
  }
  rosterPools.value = { home: homePool, away: awayPool };
  homeBoard.setRoster(homePool.players);
  awayBoard.setRoster(awayPool.players);
  unavailable.value = { home: [], away: [] };
  warnEmptyPool('主队', homePool);
  warnEmptyPool('客队', awayPool);
}

// 受限赛事下池空提示（未报名 / 未提交名单）
function warnEmptyPool(label, pool) {
  if (pool?.restricted === true && !(pool.players ?? []).length) {
    ElMessage.warning(`${label}尚未在本赛事报名或提交名单，暂无可选球员`);
  }
}

// 旧件 qi()：保存比赛记录时的字段集合（顺序与默认值逐条对齐）
function gamePayload(g) {
  const e = g ?? {};
  return {
    eventId: e.eventId,
    homeTeamId: e.homeTeamId,
    awayTeamId: e.awayTeamId,
    gameTime: e.gameTime,
    gameday: e.gameday,
    gameEndTime: e.gameEndTime ?? undefined,
    gameNumber: e.gameNumber ?? undefined,
    stadiumId: e.stadiumId ?? undefined,
    homeScore: e.homeScore ?? undefined,
    awayScore: e.awayScore ?? undefined,
    homeScoreByInning: e.homeScoreByInning ?? undefined,
    awayScoreByInning: e.awayScoreByInning ?? undefined,
    totalInnings: e.totalInnings ?? undefined,
    homeH: e.homeH ?? undefined,
    awayH: e.awayH ?? undefined,
    homeE: e.homeE ?? undefined,
    awayE: e.awayE ?? undefined,
    spectatorCount: e.spectatorCount ?? undefined,
    umpireHp: e.umpireHp ?? undefined,
    umpire1b: e.umpire1b ?? undefined,
    umpire2b: e.umpire2b ?? undefined,
    umpire3b: e.umpire3b ?? undefined,
    recorders: e.recorders ?? undefined,
    gameTag: e.gameTag ?? undefined,
    remark: e.remark ?? undefined,
    isSpecialResult: e.isSpecialResult ?? false,
    showRemarkInCard: e.showRemarkInCard ?? false,
    includeStatsInRanking: e.includeStatsInRanking ?? true,
  };
}

// 旧件 zs()：比赛时间/日期/场馆 ID 解析
function resolvedGameTime() {
  const first = splitGameTime(gameTime.value ? gameTime.value : toLocalGameTime());
  const resolved = first.gameTime && first.gameday ? first : splitGameTime(toLocalGameTime());
  const stadiumId = stadiums.value.find((s) => s.value === setupForm.value.venue)?.stadiumId
    ?? (game.value && game.value.venue === setupForm.value.venue ? game.value.stadiumId ?? null : null);
  return { gameTime: resolved.gameTime, gameday: resolved.gameday, stadiumId };
}

function draftPayload(gid) {
  const t = resolvedGameTime();
  return buildLineupDraft({
    gameId: gid,
    gameMode: mode.value,
    homeTeamId: setupForm.value.homeTeamId,
    awayTeamId: setupForm.value.awayTeamId,
    venue: setupForm.value.venue,
    gameTime: t.gameTime,
    gameday: t.gameday,
    homeLineup: homeBoard.rows.value,
    awayLineup: awayBoard.rows.value,
    homeBench: homeBoard.bench.value,
    awayBench: awayBoard.bench.value,
    homeFieldingPitcherId: homeBoard.fieldingPitcherId.value,
    awayFieldingPitcherId: awayBoard.fieldingPitcherId.value,
    homeLastDhAddedFromPoolId: homeBoard.lastDhAddedFromPoolId.value,
    awayLastDhAddedFromPoolId: awayBoard.lastDhAddedFromPoolId.value,
    homeUnavailablePlayerIds: unavailable.value.home,
    awayUnavailablePlayerIds: unavailable.value.away,
  });
}

// 旧件 Es()：载入比赛与门控
async function loadGame(id) {
  try {
    const e = await gameApi.get(id);
    if (e == null || e.id == null) {
      ElMessage.warning('比赛不存在或已删除');
      await router.replace({ name: 'AdminGames', params: { eventId: String(eventId.value) } });
      return;
    }
    gameStatus.value = e.status ?? '';
    if (e.status === 'final') {
      ElMessage.warning('该比赛已结束，无法编辑先发阵容');
      await router.replace({ name: 'AdminGames', params: { eventId: String(eventId.value) } });
      return;
    }
    if (e.status === 'live') {
      const r = await gameStatsApi.listByGame(id).catch(() => null);
      if (r && (r.list ?? []).some((u) => (u.battingOrder ?? 0) > 0)) {
        ElMessage.warning('该比赛已开始录入，请从列表点「继续录入」进入录入页');
        await router.replace({ name: 'AdminGameLiveResume', params: { eventId: String(eventId.value), gameId: String(id) } });
        return;
      }
    }
    const n = parseLineupDraft(await gameApi.getLiveSnapshot(id).catch(() => ''));
    const t = resolveLineupResumeInit(e, n);
    game.value = e;
    currentGameId.value = id;
    setupForm.value.homeTeamId = t.homeTeamId ?? undefined;
    setupForm.value.awayTeamId = t.awayTeamId ?? undefined;
    setupForm.value.venue = t.venue ?? '';
    if (t.gameMode) mode.value = t.gameMode;
    if (t.gameTime) gameTime.value = splitGameTime(t.gameTime).gameTime;
    if (teamsReady.value) {
      await loadRosters();
      if (n) {
        // 电子秩序册（Issue #310）：载入存量草稿时剔除名单外球员并提示
        const homeFiltered = filterOutOfRoster(n.homeLineup, n.homeBench, rosterPools.value.home);
        const awayFiltered = filterOutOfRoster(n.awayLineup, n.awayBench, rosterPools.value.away);
        const removedNames = [...homeFiltered.removed, ...awayFiltered.removed]
          .map((x) => x.name || x.id)
          .filter((x) => x != null);
        if (removedNames.length) {
          ElMessage.warning(`已从阵容中移除未报名球员：${removedNames.join('、')}`);
        }
        homeBoard.loadDraft({
          rows: homeFiltered.keptRows,
          bench: homeFiltered.keptBench,
          fieldingPitcherId: pickPoolId(n.homeFieldingPitcherId, rosterPools.value.home),
          lastDhAddedFromPoolId: pickPoolId(n.homeLastDhAddedFromPoolId, rosterPools.value.home),
        });
        awayBoard.loadDraft({
          rows: awayFiltered.keptRows,
          bench: awayFiltered.keptBench,
          fieldingPitcherId: pickPoolId(n.awayFieldingPitcherId, rosterPools.value.away),
          lastDhAddedFromPoolId: pickPoolId(n.awayLastDhAddedFromPoolId, rosterPools.value.away),
        });
        unavailable.value.home = Array.isArray(n.homeUnavailablePlayerIds) ? n.homeUnavailablePlayerIds : [];
        unavailable.value.away = Array.isArray(n.awayUnavailablePlayerIds) ? n.awayUnavailablePlayerIds : [];
      }
    }
  } catch (err) {
    ElMessage.error(err?.message ?? '加载已保存阵容失败');
  }
}

// 旧件 Ns()：保存 / 保存并退出
async function save(exit) {
  if (setupForm.value.homeTeamId == null || setupForm.value.awayTeamId == null) {
    ElMessage.warning('请先选择主客队');
    return;
  }
  if (setupForm.value.homeTeamId === setupForm.value.awayTeamId) {
    ElMessage.warning('主队与客队不能相同');
    return;
  }
  saving.value = true;
  try {
    const cu = resolvedGameTime();
    let id = currentGameId.value;
    if (id == null) {
      const res = await gameApi.create({
        eventId: eventId.value,
        homeTeamId: setupForm.value.homeTeamId,
        awayTeamId: setupForm.value.awayTeamId,
        venue: setupForm.value.venue || undefined,
        stadiumId: cu.stadiumId ?? undefined,
        gameMode: mode.value,
        gameday: cu.gameday,
        gameTime: cu.gameTime,
        status: 'scheduled',
      });
      id = res?.data?.id ?? null;
      if (id == null) {
        ElMessage.error(res?.msg || '保存失败');
        return;
      }
      currentGameId.value = id;
      game.value = { id, eventId: eventId.value, venue: setupForm.value.venue, stadiumId: cu.stadiumId ?? undefined };
    } else {
      await gameApi.saveResult(id, {
        game: {
          ...gamePayload(game.value),
          eventId: eventId.value,
          homeTeamId: setupForm.value.homeTeamId,
          awayTeamId: setupForm.value.awayTeamId,
          venue: setupForm.value.venue || undefined,
          stadiumId: cu.stadiumId ?? undefined,
          gameMode: mode.value,
          gameday: cu.gameday,
          gameTime: cu.gameTime,
          status: 'scheduled',
        },
      });
    }
    const payload = draftPayload(id);
    await gameApi.saveLineup(id, JSON.stringify(payload));
    ElMessage.success('已保存，可在赛程/结果页继续录入');
    if (exit) {
      tabs.removeTab(route.path);
      await router.replace({ name: 'AdminGames', params: { eventId: String(eventId.value) } });
    } else if (route.name !== 'AdminGameLiveLineupGame' || String(route.params.gameId) !== String(id)) {
      await router.replace({ name: 'AdminGameLiveLineupGame', params: { eventId: String(eventId.value), gameId: String(id) } });
    }
  } catch (err) {
    ElMessage.error(err?.message ?? '保存失败');
  } finally {
    saving.value = false;
  }
}

// 旧件 rl()：开始录入
async function startEntry() {
  if (!canStartEntry.value) return;
  saving.value = true;
  try {
    const cu = resolvedGameTime();
    let id = currentGameId.value;
    if (id == null) {
      const res = await gameApi.create({
        eventId: eventId.value,
        homeTeamId: setupForm.value.homeTeamId,
        awayTeamId: setupForm.value.awayTeamId,
        venue: setupForm.value.venue || undefined,
        stadiumId: cu.stadiumId ?? undefined,
        status: 'live',
        gameMode: mode.value,
        gameday: cu.gameday,
        gameTime: cu.gameTime,
      });
      id = res?.data?.id ?? null;
      if (!id) {
        ElMessage.error(res?.msg || '创建比赛失败');
        return;
      }
      currentGameId.value = id;
    } else {
      await gameApi.saveResult(id, {
        game: {
          ...gamePayload(game.value),
          eventId: eventId.value,
          homeTeamId: setupForm.value.homeTeamId,
          awayTeamId: setupForm.value.awayTeamId,
          venue: setupForm.value.venue || undefined,
          stadiumId: cu.stadiumId ?? undefined,
          gameMode: mode.value,
          gameday: cu.gameday,
          gameTime: cu.gameTime,
          status: 'live',
        },
      });
    }
    const payload = {
      v: 1,
      savedAt: Date.now(),
      gameId: id,
      gameMode: mode.value,
      gameStarted: true,
      setupForm: {
        homeTeamId: setupForm.value.homeTeamId,
        awayTeamId: setupForm.value.awayTeamId,
        venue: setupForm.value.venue,
        gameTime: cu.gameTime,
        gameday: cu.gameday,
      },
      homeLineup: homeBoard.rows.value,
      awayLineup: awayBoard.rows.value,
      homeBench: homeBoard.bench.value,
      awayBench: awayBoard.bench.value,
      awayFieldingPitcherId: awayBoard.fieldingPitcherId.value,
      homeFieldingPitcherId: homeBoard.fieldingPitcherId.value,
      awayLastDhAddedFromPoolId: awayBoard.lastDhAddedFromPoolId.value,
      homeLastDhAddedFromPoolId: homeBoard.lastDhAddedFromPoolId.value,
      awayUnavailablePlayerIds: unavailable.value.away,
      homeUnavailablePlayerIds: unavailable.value.home,
      gameState: {
        inning: 1,
        isTop: true,
        outs: 0,
        balls: mode.value === 'SOFTBALL' ? 1 : 0,
        strikes: mode.value === 'SOFTBALL' ? 1 : 0,
        runsAway: 0,
        runsHome: 0,
        runners: [null, null, null, null],
      },
      teamsData: {
        home: { score: 0, innings: Array(7).fill(0) },
        away: { score: 0, innings: Array(7).fill(0) },
      },
    };
    try {
      await gameApi.saveLineup(id, JSON.stringify(payload));
    } catch (error) {
      if (isBusinessError(error)) {
        // 业务校验失败（如秩序册名单外球员 400）：中止进入录入，先修正阵容（不写 bootstrap）
        ElMessage.error(error.message ?? '阵容校验未通过，请修正后重试');
        return;
      }
      console.warn('[LiveGameLineup] initial snapshot save failed', error);
      ElMessage.warning('初始阵容未同步到服务器，切换页面后可能需要重排阵容');
    }
    // 保存成功或网络降级均写入录入引导（业务校验失败路径已在上面 return，不写）
    writeEntryBootstrap(eventId.value, id, payload);
    ElMessage.success('阵容与对战信息已保存，正在进入录入…');
    await router.replace({ name: 'AdminGameLiveResume', params: { eventId: String(eventId.value), gameId: String(id) } });
  } catch (err) {
    ElMessage.error(err?.message ?? '保存阵容失败');
  } finally {
    saving.value = false;
  }
}

/* ---------------- 阵容模板 ---------------- */
async function openTemplates(team) {
  const teamId = team === 'away' ? setupForm.value.awayTeamId : setupForm.value.homeTeamId;
  if (!teamId) {
    ElMessage.warning('请先选择主客队');
    return;
  }
  templateDialog.value.team = team;
  templateDialog.value.selected = null;
  try {
    const list = (await lineupTemplateApi.list(teamId)) ?? [];
    templateDialog.value.list = list;
    if (!list.length) {
      ElMessage.info('该球队暂无阵容模板，请先在「阵容模板」菜单中创建');
      return;
    }
    templateDialog.value.visible = true;
  } catch (err) {
    ElMessage.error(err?.message ?? '加载模板列表失败');
  }
}

async function applyTemplate() {
  const team = templateDialog.value.team;
  const teamId = team === 'away' ? setupForm.value.awayTeamId : setupForm.value.homeTeamId;
  const id = templateDialog.value.selected;
  if (!teamId || id == null) return;
  try {
    const t = await lineupTemplateApi.get(teamId, id);
    if (!t?.slots?.length) {
      ElMessage.warning('模板数据为空');
      return;
    }
    await ElMessageBox.confirm('将用该模板覆盖当前球队的先发阵容与替补名单，是否继续？', '应用阵容模板');
    // 电子秩序册（Issue #310）：受限赛事下过滤模板中的名单外球员（槽位置空待选、替补/投手剔除）
    const filtered = filterTemplateByPool(t, rosterPools.value[team]);
    if (filtered.removedCount > 0) {
      ElMessage.warning(`模板中有 ${filtered.removedCount} 名球员不在本赛事报名名单中，已留空或移除，请重新选择`);
    }
    boardOf(team).applyTemplate({
      slots: filtered.slots,
      benchPlayerIds: filtered.benchPlayerIds,
      pitcherId: filtered.pitcherId,
    });
    templateDialog.value.visible = false;
  } catch (err) {
    if (err === 'cancel' || err === 'close') return; // 用户取消二次确认
    ElMessage.error(err?.message ?? '加载模板失败');
  }
}

/* ---------------- notice 联动（两个 board 都要监听） ---------------- */
watch(() => awayBoard.notice.value, (msg) => { if (msg) ElMessage.warning(awayBoard.consumeNotice()); });
watch(() => homeBoard.notice.value, (msg) => { if (msg) ElMessage.warning(homeBoard.consumeNotice()); });

/* ---------------- 生命周期 ---------------- */
onMounted(async () => {
  try {
    const e = await eventApi.get(eventId.value);
    eventName.value = e?.name ?? '';
    mode.value = e?.gameMode ?? 'BASEBALL';
    const [teamList, stadiumList] = await Promise.all([
      teamApi.selectOptions(),
      fetchAllStadiums().catch(() => []),
    ]);
    teams.value = teamList ?? [];
    stadiums.value = (stadiumList ?? []).map((c) => ({ value: c.name, label: c.name, stadiumId: c.id }));
    gameTime.value = toLocalGameTime();
    if (gameId.value != null) {
      await loadGame(gameId.value);
    } else {
      const q = route.query;
      if (q.homeTeamId) setupForm.value.homeTeamId = Number(q.homeTeamId);
      if (q.awayTeamId) setupForm.value.awayTeamId = Number(q.awayTeamId);
      if (q.venue) setupForm.value.venue = String(q.venue);
      if (teamsReady.value) await loadRosters();
    }
  } catch (err) {
    ElMessage.error(err?.message ?? '加载失败');
  } finally {
    loading.value = false;
  }
  await nextTick();
  // F18：选择器委托绑定一次即可（interactjs 按选择器委托监听，覆盖后续动态渲染的卡片）；
  // 打线行亦可拖起（拖到卡槽=落位、拖离所有区域=移出打线），行内把手由 sortablejs 接管（ignoreFrom）
  bind(['.lineup-card', '.lineup-batting__row'], getLayout);
  document.addEventListener('click', onDocClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick);
  unbind();
});
</script>
