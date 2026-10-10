<template>
  <div v-loading="loading" class="admin-page live-game-lineup-board">
    <template v-if="mode === 'SOFTBALL'">
      <LegacyLineup />
    </template>
    <template v-else>
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

      <div class="lineup-board" :class="{ 'is-mobile': isMobile }">
        <LineupFieldBoard ref="fieldBoard" :slots="fieldSlotViews" @slot-click="onSlotClick" @card-open="openPanel" />
        <LineupBattingOrder
          ref="battingRef"
          :rows="board.rows.value"
          :error-indices="errorIndices"
          :max-count="board.maxCount.value"
          @reorder="({ from, to }) => board.reorderBatting(from, to)"
          @row-open="(id) => openPanel({ player: findPlayer(id), from: 'batting' })"
        />
        <LineupPoolPanel
          ref="poolRef"
          :pool="board.pool.value"
          :bench="board.bench.value"
          :roster="board.roster.value"
          :pending-player-id="pending ? pending.player.id : null"
          :lineup-ids="board.rows.value.filter((r) => !isPlaceholder(r)).map((r) => r.id)"
          @pick="onPick"
          @open-panel="openPanel"
          @batch-change="(ids) => board.setBench(ids)"
        />
      </div>

      <div v-if="!board.validation.value.ok && board.validation.value.msg" class="lineup-warn">{{ board.validation.value.msg }}</div>
      <div v-if="board.fieldingPitcherRequired.value && !board.fieldingPitcherOk.value" class="lineup-warn">须指定先发投手后才可开始录入</div>

      <!-- 操作面板：更换球员 / 移到其他位置 / 移出 -->
      <el-dialog :model-value="!!panel" :title="panelTitle" width="420px" @close="closePanel">
        <div class="lineup-panel">
          <section class="lineup-panel__block">
            <h4>更换球员</h4>
            <el-select
              v-model="panelReplaceId"
              filterable
              clearable
              placeholder="从名册选择替换球员"
              style="width: 100%"
              @change="panelReplace"
            >
              <el-option v-for="p in panelRosterOptions" :key="p.id" :label="`${p.name} #${p.number ?? '-'}`" :value="p.id" />
            </el-select>
          </section>
          <section class="lineup-panel__block">
            <h4>移到其他位置</h4>
            <div class="lineup-panel__slots">
              <el-button v-for="s in panelEmptySlots" :key="s.code" size="small" @click="panelMoveTo(s.code)">
                {{ s.code }} {{ s.label }}
              </el-button>
              <span v-if="!panelEmptySlots.length" class="lineup-panel__empty">当前没有空余守备位</span>
            </div>
          </section>
          <section class="lineup-panel__block">
            <el-button type="danger" plain @click="panelRemove">移出（回替补）</el-button>
          </section>
        </div>
      </el-dialog>

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
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, vLoading } from 'element-plus';
import { eventApi, gameApi, gameStatsApi, teamApi, fetchAllStadiums, fetchPlayersByTeam, lineupTemplateApi } from '../../api/business';
import { usePermission } from '../../composables/usePermission';
import { useTabsStore } from '../../stores/tabs';
import { useMediaQuery } from '../../composables/useMediaQuery';
import { useLineupBoard } from '../../composables/useLineupBoard';
import { useLineupDrag } from '../../composables/useLineupDrag';
import { FIELD_SLOTS, DH_SLOT } from '../../utils/lineupFieldPositions';
import { isPlaceholder, posOf } from '../../utils/lineupBoardRules';
import { buildLineupDraft, parseLineupDraft, resolveLineupResumeInit, splitGameTime, toLocalGameTime } from '../../utils/lineupDraft';
import { writeEntryBootstrap } from '../../utils/liveGameStorage';
import { getValidPositionOptions } from '../../utils/starterFieldingValidation';
import LineupFieldBoard from '../../components/admin/LineupFieldBoard.vue';
import LineupBattingOrder from '../../components/admin/LineupBattingOrder.vue';
import LineupPoolPanel from '../../components/admin/LineupPoolPanel.vue';
import LegacyLineup from './LiveGameLineup.js';
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
const panel = ref(null);            // 操作面板：{ player, from, code }
const panelReplaceId = ref(null);   // 面板「更换球员」选择
const templateDialog = ref({ visible: false, team: 'away', list: [], selected: null });
const unavailable = ref({ home: [], away: [] });   // 快照透传，无 UI

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
const positionOptions = computed(() => [...getValidPositionOptions(mode.value)]);
void positionOptions;

// R1：球场卡槽视图（9 守备位 + DH）
const fieldSlotViews = computed(() => {
  const b = board.value;
  const rows = b.rows.value;
  const filledRows = rows.filter((r) => !isPlaceholder(r));
  const errIdx = b.validation.value.errorRowIndices ?? [];
  return [...FIELD_SLOTS, DH_SLOT].map((slot) => {
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
    return { code, player: player ?? null, orderIndex, badge, error, pending: pendingAccepts(code) };
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

const { bind, unbind, wasRecentDrag } = useLineupDrag({
  onDrop(el, target) {
    const playerId = el.dataset.playerId;
    if (playerId == null) return;
    const player = findPlayer(playerId);
    if (!player) return;
    handleDrop(player, target);
  },
  onCancel() { clearPending(); },
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
    // 池球员 → 加入替补（sendBackToBench 只处理「在打线内 / 仅守备投手」，对纯池球员是空操作）
    const inRows = b.rows.value.some((r) => !isPlaceholder(r) && String(r.id) === String(player.id));
    const isFieldingPitcher = b.fieldingPitcherId.value != null && String(b.fieldingPitcherId.value) === String(player.id);
    if (inRows || isFieldingPitcher) b.sendBackToBench(player.id);
    else b.setBench([...b.bench.value.map((p) => p.id), player.id]);
  }
  else if (target.kind === 'pool') {
    // 替补卡片拖回池 = 从替补名单移除（sendBackToBench 是「加入替补」，不可混用）
    if (b.bench.value.some((p) => String(p.id) === String(player.id))) {
      b.setBench(b.bench.value.filter((p) => String(p.id) !== String(player.id)).map((p) => p.id));
    }
  }
  showNotice(b);
}

/* ---------------- 点选点放 ---------------- */
function onPick(player) {
  if (wasRecentDrag()) return; // 拖拽结束后浏览器会补发一次 click → 与点选点放互斥（Task 1 spike 实测）
  if (pending.value && String(pending.value.player.id) === String(player.id)) return clearPending();
  pending.value = { player };
}
function onSlotClick(code) {
  if (!pending.value) return;
  board.value.placeAtSlot(pending.value.player, code);
  showNotice(board.value);
  clearPending();
}
function clearPending() { pending.value = null; }
function showNotice(b) {
  const msg = b.consumeNotice();
  if (msg) ElMessage.warning(msg);
}

/* ---------------- 操作面板 ---------------- */
function openPanel(payloadOrCode) {
  // 拖拽结束后浏览器会补发一次 click → 与被拖卡片互斥（与 onPick 同源，Task 1 spike 实测）
  if (wasRecentDrag()) return;
  let payload = payloadOrCode;
  // 球场卡片 card-open 只带 code，需回查球员
  if (typeof payloadOrCode === 'string') {
    const code = payloadOrCode;
    const view = fieldSlotViews.value.find((s) => s.code === code);
    payload = { player: view?.player ?? null, from: 'field', code };
  }
  if (!payload?.player) return;
  panel.value = { player: payload.player, from: payload.from, code: payload.code || null };
  panelReplaceId.value = null;
}
function closePanel() {
  panel.value = null;
  panelReplaceId.value = null;
}

const panelTitle = computed(() => {
  const p = panel.value?.player;
  if (!p) return '球员操作';
  return `${p.name ?? ''} #${p.number ?? '-'}`;
});
// 当前面板球员所在守备位（球场卡片带 code；打线行按其守备位）
const panelCode = computed(() => {
  if (!panel.value) return '';
  if (panel.value.code) return panel.value.code;
  const row = board.value.rows.value.find((r) => !isPlaceholder(r) && String(r.id) === String(panel.value.player.id));
  return row ? posOf(row) : '';
});
const panelRosterOptions = computed(() => {
  const b = board.value;
  const inLineup = new Set(b.rows.value.filter((r) => !isPlaceholder(r)).map((r) => String(r.id)));
  const currentId = panel.value?.player?.id != null ? String(panel.value.player.id) : null;
  return b.roster.value.filter((p) => !inLineup.has(String(p.id)) && String(p.id) !== currentId);
});
const panelEmptySlots = computed(() => {
  const b = board.value;
  return [...FIELD_SLOTS, DH_SLOT].filter((s) => b.slotOfRow(s.code) == null);
});

function panelReplace(playerId) {
  if (playerId == null) return;
  const p = board.value.roster.value.find((x) => String(x.id) === String(playerId));
  const code = panelCode.value;
  if (!p || !code) return;
  board.value.placeAtSlot(p, code);
  showNotice(board.value);
  closePanel();
}
function panelMoveTo(code) {
  const p = panel.value?.player;
  if (!p) return;
  board.value.placeAtSlot(p, code);
  showNotice(board.value);
  closePanel();
}
function panelRemove() {
  const p = panel.value?.player;
  if (!p) return;
  board.value.sendBackToBench(p.id);
  showNotice(board.value);
  closePanel();
}

/* ---------------- 名册 / 比赛载入 ---------------- */
async function loadRosters() {
  if (!setupForm.value.homeTeamId || !setupForm.value.awayTeamId) return;
  const [homeRoster, awayRoster] = await Promise.all([
    fetchPlayersByTeam(setupForm.value.homeTeamId),
    fetchPlayersByTeam(setupForm.value.awayTeamId),
  ]);
  homeBoard.setRoster(homeRoster);
  awayBoard.setRoster(awayRoster);
  unavailable.value = { home: [], away: [] };
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
        homeBoard.loadDraft({
          rows: n.homeLineup,
          bench: n.homeBench,
          fieldingPitcherId: n.homeFieldingPitcherId,
          lastDhAddedFromPoolId: n.homeLastDhAddedFromPoolId,
        });
        awayBoard.loadDraft({
          rows: n.awayLineup,
          bench: n.awayBench,
          fieldingPitcherId: n.awayFieldingPitcherId,
          lastDhAddedFromPoolId: n.awayLastDhAddedFromPoolId,
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
    await gameApi.saveLiveSnapshot(id, JSON.stringify(payload));
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
    writeEntryBootstrap(eventId.value, id, payload);
    try {
      await gameApi.saveLiveSnapshot(id, JSON.stringify(payload));
    } catch (error) {
      console.warn('[LiveGameLineup] initial snapshot save failed', error);
      ElMessage.warning('初始阵容未同步到服务器，切换页面后可能需要重排阵容');
    }
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
    boardOf(team).applyTemplate({
      slots: t.slots,
      benchPlayerIds: t.benchPlayerIds,
      pitcherId: t.startingPitcherPlayerId,
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
  // F18：选择器委托绑定一次即可（interactjs 按选择器委托监听，覆盖后续动态渲染的卡片）
  bind(['.lineup-card'], getLayout);
});

onBeforeUnmount(() => unbind());
</script>

<style scoped>
.lineup-panel__block { margin-bottom: 16px; }
.lineup-panel__block h4 { margin: 0 0 8px; font-size: 13px; color: #606266; }
.lineup-panel__slots { display: flex; flex-wrap: wrap; gap: 8px; }
.lineup-panel__empty { color: #909399; font-size: 12px; }
</style>
