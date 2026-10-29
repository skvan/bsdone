<template>
  <div ref="pageEl" class="clubhouse-page" :class="{ 'is-resizing': layoutResizing, 'is-reordering': layoutDragging }" v-loading="loading">
    <template v-if="data">
      <div class="page-head">
        <div>
          <div class="breadcrumb">门户 / <b>我的主场</b></div>
          <h1>我的主场</h1>
          <p class="page-sub">球员个人主页 · 已关联球员档案</p>
        </div>
        <div class="page-actions">
          <el-button v-if="freeBoard && !layoutEditing && renderableModules.length" ref="layoutEditEntryEl" class="layout-edit-entry" :disabled="layoutSaving" @click="enterLayoutEditing">编辑布局</el-button>
          <el-button link type="primary" :disabled="layoutEditing || layoutSaving" @click="openSettings">数据设置</el-button>
          <el-button link type="primary" @click="goAccount">个人资料</el-button>
          <el-button @click="goPortal">返回门户</el-button>
        </div>
      </div>

      <div class="scope-bar">
        <span class="scope-title">数据范围</span>
        <el-select
          v-if="availableGameModes.length > 1"
          v-model="gameMode"
          size="small"
          class="scope-select scope-game-mode"
          aria-label="运动类型"
          :disabled="layoutEditing"
          @change="onGameModeChange"
        >
          <el-option v-for="mode in availableGameModes" :key="mode" :label="gameModeLabel(mode)" :value="mode" />
        </el-select>
        <el-select v-model="teamId" size="small" class="scope-select scope-team" aria-label="球队" :disabled="layoutEditing" @change="onTeamChange">
          <el-option label="全部球队" :value="SCOPE_ALL" />
          <el-option v-for="team in availableTeams" :key="team.id" :label="team.name" :value="team.id" />
        </el-select>
        <el-select v-model="eventId" size="small" class="scope-select scope-event" aria-label="赛事" :disabled="layoutEditing" @change="onEventChange">
          <el-option label="生涯" :value="SCOPE_ALL" />
          <el-option v-for="event in scopeEvents" :key="event.id" :label="scopeEventLabel(event)" :value="event.id" />
        </el-select>
        <span class="scope-hint">{{ scopeHint }}</span>
        <span class="scope-right">代表 <b>{{ availableTeams.length }}</b> 支球队</span>
      </div>

      <section class="hero">
        <div class="hero-27">{{ player.number || '27' }}</div>
        <div class="hero-left">
          <div class="hero-top">
            <span class="hero-kicker">MY CLUBHOUSE</span>
            <span class="claim"><i></i>已关联球员档案</span>
          </div>
          <h1>{{ player.name }}</h1>
          <p class="hero-en">{{ player.nameEn || 'PLAYER' }} <template v-if="player.number">· #{{ player.number }}</template></p>
          <p class="hero-intro">{{ player.intro || '这位球员还没有填写简介。' }}</p>
          <div class="hero-tags">
            <span v-for="position in player.positions || []" :key="position" class="tag">{{ position }}</span>
            <span class="tag">{{ throwBatLabel }}</span>
            <span v-if="player.height || player.weight" class="tag">{{ player.height || '-' }}cm / {{ player.weight || '-' }}kg</span>
          </div>
          <div class="hero-team">
            <span class="crest">{{ heroTeamCrest }}</span>
            <div class="team-copy">
              <b>{{ heroTeamName }}</b>
              <span>{{ heroTeamSub }}</span>
            </div>
          </div>
          <div class="hero-actions">
            <button class="hero-btn primary" type="button" @click="goPublicProfile">查看公开主页</button>
            <button class="hero-btn ghost" type="button" @click="goTeam">球队主页</button>
          </div>
        </div>

        <div class="card-stage">
          <button ref="heroCardBtnRef" type="button" class="card-slot" :style="{ '--hero-tilt': `${heroTiltDeg}deg` }" aria-label="放大查看球星卡" @click="openCardZoom">
            <span class="card-stack two" aria-hidden="true">
              <ClubhousePlayerCard v-bind="fanLegendCards[1]" :scale="heroCardScale" />
            </span>
            <span class="card-stack one" aria-hidden="true">
              <ClubhousePlayerCard v-bind="fanLegendCards[0]" :scale="heroCardScale" />
            </span>
            <ClubhousePlayerCard v-bind="cardFaceProps" :scale="heroCardScale" />
          </button>
        </div>
      </section>

      <div class="identity-band">
        <div class="id-item"><span>守备位置</span><b>{{ primaryPosition }}</b></div>
        <div class="id-item"><span>投打</span><b>{{ throwBatLabel }}</b></div>
        <div class="id-item"><span>身高 / 体重</span><b><em>{{ player.height || '-' }}</em> / {{ player.weight || '-' }}</b></div>
        <div class="id-item"><span>出生地</span><b>{{ player.birthPlace || '未填写' }}</b></div>
      </div>

      <div v-if="layoutEditing" class="layout-edit-banner">
        <div class="edit-copy">
          <b>布局编辑</b>
          <span v-if="swapTargetLabel" class="edit-swap" role="status" aria-live="polite">松开与「{{ swapTargetLabel }}」交换位置</span>
          <span v-else-if="selectedModule" class="edit-selected">
            <b>{{ selectedModule.label }}</b>
            <em>{{ selectionSizeLabel }}</em>
            <i v-if="contentLimitHeight" class="edit-content-limit">内容下限 {{ contentLimitHeight }}px</i>
          </span>
          <span v-else>拖动模块调整位置；拖到另一模块上可交换；拖动八个小方块缩放；点击空白处取消选中</span>
        </div>
        <div class="edit-actions">
          <button v-if="selectedModule" type="button" class="edit-reset" :disabled="layoutSaving" @click="resetSelectedModule">重置尺寸</button>
          <span class="edit-status" :class="{ 'is-saved': layoutStatus === 'saved' }" role="status" aria-live="polite">{{ layoutStatus === 'saving' ? '保存中…' : layoutStatus === 'saved' ? '已保存' : '' }}</span>
          <button type="button" class="edit-done" @click="finishLayoutEditing">完成</button>
        </div>
      </div>

      <div
        v-if="renderableModules.length"
        class="dashboard-board"
        :class="{ 'is-free': freeBoard, 'layout-editing': layoutEditing }"
        :style="freeBoard ? { height: `${boardHeight}px` } : undefined"
        @click.self="clearModuleSelection()"
      >
      <section
        v-if="freeBoard && layoutEditing && isModuleActive('ranks') && !isModuleVisible('ranks')"
        class="rank-band dashboard-module rank-placeholder"
        data-module-key="ranks"
        tabindex="0"
        :style="moduleLayoutStyle('ranks')"
        @click="selectModule('ranks')"
        @pointerdown="startModulePointer('ranks', $event)"
        @keydown="onModuleKeydown('ranks', $event)"
      >
        <div class="rank-placeholder-copy">
          <span class="kicker">TEAM RANKING</span>
          <b>队内坐标</b>
          <span>选择球队后显示；此位置会保留</span>
        </div>
      </section>

      <section
        v-if="isModuleVisible('ranks') && teamId"
        class="rank-band dashboard-module"
        data-module-key="ranks"
        :tabindex="layoutEditing ? 0 : undefined"
        :style="moduleLayoutStyle('ranks')"
        @click="selectModule('ranks')"
        @pointerdown="startModulePointer('ranks', $event)"
        @keydown="onModuleKeydown('ranks', $event)"
      >
        <div class="rank-band-head">
          <div>
            <span class="kicker">TEAM RANKING</span>
            <h2>队内坐标 <small>{{ rankScopeLabel }}</small>
              <el-tooltip content="排名按当前代表球队的同队同赛事或球队跨赛事生涯口径统计；无最低打席门槛；全垒打不进入排名。" placement="bottom-start" trigger="click">
                <button type="button" class="metric-info" aria-label="排名口径">i</button>
              </el-tooltip>
            </h2>
          </div>
          <span class="rank-scope">{{ data.scope.teamName || '-' }} · {{ data.scope.eventName || '生涯' }}<template v-if="rankPopulation"> · {{ rankPopulation }} 人</template> <a href="#" @click.prevent="openSettings">切换</a></span>
        </div>
        <p class="rank-note"><b>口径</b><span>{{ rankNote }}</span></p>
        <div class="rank-band-grid">
          <div v-for="(rank, index) in data.ranks" :key="rank.metricKey || index" class="rank-feature" :class="{ neutral: rank.rank == null || rank.metricKey === 'batting.obp' }">
            <div class="line">
              <div class="no">
                <template v-if="rank.rank != null">{{ rank.rank }}</template>
                <template v-else>—</template>
                <small v-if="rank.total">/{{ rank.total }}</small>
              </div>
              <span class="value">{{ rank.rank != null ? rank.value : '-' }}</span>
            </div>
            <div class="metric">{{ rank.label }}<span>{{ rank.rank != null ? `队内第 ${rank.rank}` : (rank.available ? '暂无排名' : '暂无该数据') }}</span></div>
            <div class="track"><i :style="{ width: `${rank.rank != null ? rank.barPct : 0}%` }"></i></div>
          </div>
        </div>
      </section>

      <section
        v-if="isModuleVisible('evaluation') && evaluationCards.length"
        class="panel evaluation-panel dashboard-module"
        data-module-key="evaluation"
        :tabindex="layoutEditing ? 0 : undefined"
        :style="moduleLayoutStyle('evaluation')"
        @click="selectModule('evaluation')"
        @pointerdown="startModulePointer('evaluation', $event)"
        @keydown="onModuleKeydown('evaluation', $event)"
      >
        <div class="eval-head">
          <div>
            <span class="kicker">OVERALL PROFILE</span>
            <h3>总体评价 <small>职业指标 · {{ evaluationComparison }}百分位</small></h3>
          </div>
          <div v-if="evaluationOverall.score != null" class="eval-overall">
            <div class="eo-score">
              <b>{{ evaluationOverall.score }}</b>
              <span class="eo-scale">/ 100</span>
              <em>{{ evaluationOverall.grade }}</em>
            </div>
            <div class="eo-side">
              <span class="eo-tags">
                <b class="eo-role">{{ overallRoleLabel }}</b>
                <i class="eo-confidence" :class="confidenceTone(evaluationOverall.confidenceLabel)">{{ evaluationOverall.confidenceLabel }}</i>
              </span>
              <span class="eo-sample">{{ overallSampleText }}</span>
              <span class="eo-reliability">总体可靠性 {{ formatReliability(evaluationOverall.reliability) }}</span>
            </div>
          </div>
          <div class="eval-meta">
            <span><b>{{ evaluationCards.length }}</b> 个评价维度</span>
            <span>{{ evaluationComparison }} · 不随筛选变化</span>
          </div>
        </div>
        <div class="radar-grid" :class="evaluationCards.length > 2 ? 'three-up' : 'two-up'">
          <article
            v-for="card in evaluationCards"
            :key="card.domain"
            class="radar-card"
            :class="card.domain"
          >
            <div class="radar-card-head">
              <div>
                <span class="radar-kicker">{{ radarKicker(card.domain) }}</span>
                <h4>{{ card.title }}</h4>
                <p class="radar-card-sub">{{ card.subtitle }}</p>
              </div>
              <div v-if="card.available" class="radar-score">
                <b>{{ card.score }}</b><em>{{ card.grade }}</em><span>综合评分</span>
              </div>
              <div v-else class="radar-score is-empty">
                <b>—</b><span>暂无评分</span>
              </div>
            </div>

            <template v-if="card.available && card.radar">
              <svg class="radar-chart" viewBox="0 0 340 270" role="img" :aria-label="radarAria(card)">
                <polygon
                  v-for="(ring, index) in card.radar.rings"
                  :key="`ring-${index}`"
                  class="radar-ring"
                  :points="ring"
                />
                <line
                  v-for="(spoke, index) in card.radar.spokes"
                  :key="`spoke-${index}`"
                  class="radar-spoke"
                  :x1="spoke.x1"
                  :y1="spoke.y1"
                  :x2="spoke.x2"
                  :y2="spoke.y2"
                />
                <polygon class="radar-area" :points="card.radar.shape" />
                <polygon class="radar-line" :points="card.radar.shape" />
                <circle
                  v-for="(dot, index) in card.radar.dots"
                  :key="`dot-${index}`"
                  class="radar-dot"
                  :cx="dot.x"
                  :cy="dot.y"
                  r="3.1"
                />
                <g v-for="(label, index) in card.radar.labels" :key="`label-${index}`">
                  <text class="radar-axis-name" :x="label.x" :y="label.nameY" :text-anchor="label.anchor">{{ label.abbr }}</text>
                  <text class="radar-axis-value" :x="label.x" :y="label.valueY" :text-anchor="label.anchor">{{ label.value }}</text>
                  <text class="radar-axis-pct" :x="label.x" :y="label.pctY" :text-anchor="label.anchor">P{{ label.percentile }}</text>
                </g>
              </svg>
              <div class="radar-metrics">
                <div v-for="axis in card.axes" :key="axis.key" class="radar-metric">
                  <i></i><b>{{ axis.label }}<small>{{ axis.abbr }}</small></b><em>{{ axis.value }}</em><span>{{ axis.percentile }}</span>
                </div>
              </div>
              <div class="radar-highlights">
                <span>强项<b>{{ card.strongest.label }}</b><em>{{ card.strongest.percentile }}</em></span>
                <span>提升<b>{{ card.weakest.label }}</b><em>{{ card.weakest.percentile }}</em></span>
              </div>
              <div class="radar-foot">
                <span><b>样本池</b> {{ card.sampleSize }} 人</span>
                <span><b>本人</b> {{ ownSampleText(card) }}</span>
                <span>{{ radarFoot(card) }}</span>
              </div>
              <div class="radar-tags">
                <i class="radar-confidence" :class="confidenceTone(card.confidenceLabel)">{{ card.confidenceLabel }}</i>
                <span class="radar-reliability">可靠性 {{ formatReliability(card.reliability) }}</span>
                <span v-if="radarNote(card)" class="radar-note">{{ radarNote(card) }}</span>
              </div>
            </template>
            <div v-else class="radar-empty">
              <b>—</b>
              <span>{{ card.reason || '暂无评价数据' }}</span>
            </div>
          </article>
        </div>
      </section>

      <section
        v-if="isModuleVisible('milestones')"
        class="panel milestone-panel dashboard-module"
        data-module-key="milestones"
        :tabindex="layoutEditing ? 0 : undefined"
        :style="moduleLayoutStyle('milestones')"
        @click="selectModule('milestones')"
        @pointerdown="startModulePointer('milestones', $event)"
        @keydown="onModuleKeydown('milestones', $event)"
      >
        <div class="panel-head milestone-head">
          <div>
            <span class="kicker">CAREER MILESTONES</span>
            <h3>生涯里程碑 <small>全租户生涯 · 跨赛事与球队</small></h3>
          </div>
          <div class="milestone-summary">
            <span>已达成 <b>{{ milestoneData.reachedCount || 0 }}</b> 项</span>
            <span>下一站 <b>{{ nextMilestoneText }}</b></span>
          </div>
        </div>

        <div v-if="milestonesLoading" class="data-empty">正在加载生涯里程碑…</div>
        <template v-else-if="milestoneData.available">
          <div class="milestone-body">
            <div class="milestone-metrics">
              <article v-for="item in milestoneItems" :key="item.metricKey" class="milestone-metric" :class="item.domain">
                <div class="mm-top">
                  <span class="mm-short">{{ item.shortLabel }}</span>
                  <span class="mm-domain">{{ milestoneDomainLabel(item.domain) }}</span>
                </div>
                <div class="mm-value">
                  {{ item.value }}<span v-if="item.nextTarget">/ {{ item.nextTarget }}</span>
                </div>
                <div class="mm-label">{{ item.label }}</div>
                <div class="mm-track"><i :style="{ width: `${item.progressPct}%` }"></i></div>
                <div class="mm-foot">
                  <span>{{ item.nextTarget ? `还差 ${item.remaining} ${item.unit}` : '已达最高里程碑' }}</span>
                  <b>{{ item.progressPct }}%</b>
                </div>
              </article>
            </div>
            <div class="milestone-timeline">
              <div class="mt-head"><b>最近里程碑</b><span>最近 {{ milestoneTimeline.length }} 项 / 共 {{ milestoneData.reachedCount || 0 }} 项</span></div>
              <div v-if="milestoneTimeline.length" class="mt-scroll">
                <div
                  ref="milestoneListEl"
                  class="mt-list"
                  tabindex="0"
                  role="region"
                  aria-label="最近里程碑时间线"
                  @scroll="updateMilestoneScrollbar"
                >
                  <div v-for="event in milestoneTimeline" :key="`${event.metricKey}-${event.gameId}-${event.title}`" class="mt-row">
                    <span class="mt-dot"></span>
                    <div class="mt-copy">
                      <b>{{ event.title }}</b>
                      <span>{{ event.eventName || '赛事' }}<template v-if="event.opponentName"> · vs {{ event.opponentName }}</template></span>
                    </div>
                    <time>{{ formatMilestoneDate(event.gameday) }}</time>
                  </div>
                </div>
                <div v-if="milestoneScroll.canScroll" class="mt-scrollbar" aria-hidden="true">
                  <span
                    class="mt-scroll-thumb"
                    :style="milestoneThumbStyle"
                    @pointerdown.prevent.stop="startMilestoneScrollDrag"
                  ></span>
                </div>
              </div>
              <p v-else class="mt-empty">还没有达成里程碑，继续保持。</p>
              <p class="mt-note">{{ milestoneData.note || '统计自系统记录 · 仅计入排行的比赛' }}</p>
            </div>
          </div>
        </template>
        <div v-else class="data-empty">{{ milestonesError || '暂无生涯累计数据。' }}</div>
      </section>

      <div class="content-grid">
        <div class="main-col">
          <section
            v-if="isModuleVisible('performance')"
            class="panel data-panel dashboard-module"
            data-module-key="performance"
            :tabindex="layoutEditing ? 0 : undefined"
            :style="moduleLayoutStyle('performance')"
            @click="selectModule('performance')"
            @pointerdown="startModulePointer('performance', $event)"
            @keydown="onModuleKeydown('performance', $event)"
          >
            <div class="panel-head">
              <div><span class="kicker">PERFORMANCE</span><h3>数据表现 <small>{{ performanceScopeLabel }}</small></h3></div>
              <div class="panel-head-actions">
                <div class="chart-summary">
                  <div><b>{{ performanceSummary }}</b><span>{{ performanceLabel }}</span></div>
                </div>
                <el-tooltip content="按比赛日排序的最近 12 场；指标与图表可在数据设置切换。" placement="bottom-end" trigger="click">
                  <button type="button" class="metric-info" aria-label="表现图口径">i</button>
                </el-tooltip>
                <el-button link :icon="Setting" aria-label="数据设置" @click="openSettings" />
              </div>
            </div>

            <div v-if="data.performance.available && data.performance.viz === 'donut'" class="donut-wrap">
              <div class="donut" :style="{ background: donutStyle }">
                <div class="donut-center"><b>{{ donutTotal }}</b><span>安打</span></div>
              </div>
              <div class="donut-legend">
                <div v-for="(item, index) in data.performance.items" :key="item.label" class="donut-legend-row">
                  <i :style="{ background: chartColor(index) }"></i><span>{{ item.label }}</span><b>{{ item.value }}</b>
                </div>
              </div>
            </div>

            <div v-else-if="data.performance.available && data.performance.viz === 'kpi'" class="kpi-view">
              <b>{{ performanceSummary }}</b>
              <span>{{ performanceLabel }} · {{ performanceScopeLabel }}</span>
            </div>

            <div v-else-if="chart.points.length" class="chart-wrap">
              <svg class="trend-chart" viewBox="0 0 640 190" role="img" :aria-label="`${performanceLabel}走势`">
                <defs>
                  <linearGradient :id="areaGradientId" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0" stop-color="var(--bs-chart-line)" stop-opacity=".24" />
                    <stop offset="1" stop-color="var(--bs-chart-line)" stop-opacity="0" />
                  </linearGradient>
                </defs>
                <line v-for="tick in chart.yTicks" :key="`g-${tick.y}`" class="grid" x1="46" :y1="tick.y" x2="624" :y2="tick.y" />
                <text v-for="tick in chart.yTicks" :key="`l-${tick.y}`" class="axis" x="4" :y="tick.y + 3">{{ tick.label }}</text>
                <template v-if="data.performance.viz === 'bar'">
                  <rect v-for="(bar, index) in chart.bars" :key="`b-${index}`" class="bar" :x="bar.x" :y="bar.y" :width="bar.w" :height="bar.h" rx="3" />
                </template>
                <template v-else>
                  <path class="area" :fill="`url(#${areaGradientId})`" :d="chart.areaPath" />
                  <path class="line" :d="chart.linePath" />
                  <circle v-for="(point, index) in chart.points" :key="`p-${index}`" class="dot" :cx="point.x" :cy="point.y" r="3.2" />
                </template>
                <text v-for="label in chart.xLabels" :key="`x-${label.x}`" class="axis" :x="label.x" y="182">{{ label.text }}</text>
              </svg>
              <div class="chart-note"><span>近 {{ chart.points.length }} 场 · 按比赛日排序</span><b>{{ performanceScopeLabel }}</b></div>
            </div>

            <div v-else class="data-empty">所选范围暂无比赛数据。</div>

            <div v-if="matrixItems.length" class="matrix-grid">
              <div v-for="item in matrixItems" :key="item.label" class="matrix-item">
                <b>{{ item.value }}</b><span>{{ item.label }}</span>
              </div>
            </div>
          </section>

          <section
            v-if="isModuleVisible('schedule')"
            class="panel dashboard-module"
            data-module-key="schedule"
            :tabindex="layoutEditing ? 0 : undefined"
            :style="moduleLayoutStyle('schedule')"
            @click="selectModule('schedule')"
            @pointerdown="startModulePointer('schedule', $event)"
            @keydown="onModuleKeydown('schedule', $event)"
          >
            <div class="panel-head"><div><span class="kicker">UP NEXT</span><h3>接下来的比赛 <small>{{ scheduleScopeLabel }}</small></h3></div><el-button link type="primary" @click="goEvents">全部赛程</el-button></div>
            <div v-if="currentEvent" class="event-context">
              <span class="ec-label">当前赛事</span>
              <b>{{ currentEvent.name }}</b>
              <span class="ec-status" :class="currentEvent.status ? `is-${currentEvent.status}` : ''">{{ currentEventStatus }}</span>
              <span v-if="currentEventDateRange" class="ec-dates">{{ currentEventDateRange }}</span>
              <span v-if="featuredGame && !featuredGameIsCurrent" class="ec-note">当前赛事近期暂无未开赛比赛</span>
            </div>
            <div v-if="featuredGame" class="next-game">
              <div class="ng-head"><span class="event-pill">{{ featuredGame.eventName || '赛事' }}</span><span class="countdown">{{ countdown(featuredGame.gameday) }}</span></div>
              <div class="ng-body">
                <div class="ng-side"><span class="crest">{{ featuredGame.teamName?.slice(0, 2) || '?' }}</span><div><b>{{ featuredGame.teamName }}</b><span>{{ featuredGame.isHome ? '主场' : '客场' }}</span></div></div>
                <span class="ng-vs">VS</span>
                <div class="ng-side right"><div><b>{{ featuredGame.opponentName }}</b><span>{{ featuredGame.isHome ? '客场' : '主场' }}</span></div><span class="crest dark">{{ opponentCrest(featuredGame) }}</span></div>
              </div>
              <div class="ng-foot">
                <div class="ng-meta"><span>{{ formatDay(featuredGame.gameday) }} · {{ weekday(featuredGame.gameday) }}</span><span>{{ featuredGame.venue || '场地待定' }}</span><span><b>{{ featuredGame.gameTime || '--:--' }}</b></span></div>
                <el-button link type="primary" @click="goGame(featuredGame)">赛事详情 ›</el-button>
              </div>
            </div>
            <div v-else class="data-empty">{{ scheduleEmptyText }}</div>
            <div v-if="upcomingGames.length" class="schedule-list">
              <div v-for="game in upcomingGames" :key="game.gameId" class="schedule-row">
                <div class="sch-date">{{ formatDay(game.gameday) }}<span>{{ weekday(game.gameday) }}</span></div>
                <div class="sch-main"><b>{{ game.teamName }} {{ game.isHome ? 'vs' : '@' }} {{ game.opponentName }}</b><span>{{ game.isHome ? '主场' : '客场' }} · {{ game.teamName }}</span></div>
                <span class="sch-event" :class="eventTone(game.eventName)">{{ game.eventName || '赛事' }}</span>
                <span class="sch-time">{{ game.gameTime || '--:--' }}</span>
              </div>
            </div>
          </section>

          <section
            v-if="isModuleVisible('recentGames')"
            class="panel dashboard-module"
            data-module-key="recentGames"
            :tabindex="layoutEditing ? 0 : undefined"
            :style="moduleLayoutStyle('recentGames')"
            @click="selectModule('recentGames')"
            @pointerdown="startModulePointer('recentGames', $event)"
            @keydown="onModuleKeydown('recentGames', $event)"
          >
            <div class="panel-head"><div><span class="kicker">RECENT FORM</span><h3>最近战报</h3></div><el-button link type="primary" @click="goEvents">比赛记录</el-button></div>
            <div v-if="data.recentGames.length" class="recent-cards">
              <article v-for="game in data.recentGames" :key="game.gameId" class="result-card">
                <div class="result-top"><span class="date">{{ formatDay(game.gameday) }} · {{ game.isHome ? '主场' : '客场' }}</span><span class="res-badge" :class="resultType(game.result)">{{ game.result }}</span></div>
                <div class="result-score"><b>{{ game.scoreFor ?? '-' }} : {{ game.scoreAgainst ?? '-' }}</b><span>最终</span></div>
                <div class="result-opponent">{{ game.opponentName }}</div>
                <div class="result-meta">{{ game.teamName }} · {{ game.eventName || '赛事' }}</div>
                <div class="result-line">{{ recentLine(game) }}</div>
              </article>
            </div>
            <div v-else class="data-empty">{{ recentEmptyText }}</div>
          </section>
        </div>

        <aside class="side-col">
          <section
            v-if="isModuleVisible('teams')"
            class="panel team-panel dashboard-module"
            data-module-key="teams"
            :tabindex="layoutEditing ? 0 : undefined"
            :style="moduleLayoutStyle('teams')"
            @click="selectModule('teams')"
            @pointerdown="startModulePointer('teams', $event)"
            @keydown="onModuleKeydown('teams', $event)"
          >
            <div class="panel-head"><div><span class="kicker">REPRESENTED TEAMS</span><h3>代表球队</h3></div></div>
            <div v-for="team in availableTeams" :key="team.id" class="rep-team" :class="{ active: team.id === teamId }" @click="switchTeam(team)">
              <span class="crest-lg">{{ teamCrest(team) }}</span>
              <div class="rep-name"><b>{{ team.name }}</b><span>{{ teamEventLabel(team) }}</span></div>
              <div class="rep-stat"><b>{{ standingRank(team) }}</b><span>{{ standingRecord(team) }}</span></div>
            </div>
            <div class="rep-scope"><span>球队积分按各队主赛事显示</span><el-button link type="primary" @click="goTeam">球队主页 ›</el-button></div>
          </section>

          <section
            v-if="isModuleVisible('about')"
            class="panel about-panel dashboard-module"
            data-module-key="about"
            :tabindex="layoutEditing ? 0 : undefined"
            :style="moduleLayoutStyle('about')"
            @click="selectModule('about')"
            @pointerdown="startModulePointer('about', $event)"
            @keydown="onModuleKeydown('about', $event)"
          >
            <div class="panel-head"><div><span class="kicker">ABOUT ME</span><h3>关于我</h3></div></div>
            <p class="about-intro">{{ player.intro || '这位球员还没有填写简介。' }}</p>
            <div class="about-meta">
              <div><span>守备位置</span><b>{{ (player.positions || []).join(' / ') || '未填写' }}</b></div>
              <div><span>投打</span><b>{{ throwBatLabel }}</b></div>
              <div><span>身高 / 体重</span><b>{{ player.height || '-' }} / {{ player.weight || '-' }}</b></div>
              <div><span>出生地</span><b>{{ player.birthPlace || '未填写' }}</b></div>
            </div>
          </section>

          <section
            v-if="isModuleVisible('quickLinks')"
            class="panel dashboard-module"
            data-module-key="quickLinks"
            :tabindex="layoutEditing ? 0 : undefined"
            :style="moduleLayoutStyle('quickLinks')"
            @click="selectModule('quickLinks')"
            @pointerdown="startModulePointer('quickLinks', $event)"
            @keydown="onModuleKeydown('quickLinks', $event)"
          >
            <div class="panel-head"><div><span class="kicker">QUICK LINKS</span><h3>快捷入口</h3></div></div>
            <div class="shortcut-list">
              <el-button link type="primary" class="shortcut" @click="goPublicProfile">公开球员主页</el-button>
              <el-button link type="primary" class="shortcut" @click="goAccount">我的认领与资料</el-button>
              <el-button link type="primary" class="shortcut" @click="openSettings">数据设置</el-button>
            </div>
          </section>
        </aside>
      </div>
      </div>

      <div
        v-if="layoutEditing && selectedModule && selectionRect"
        class="layout-selection-frame"
        :class="{ 'is-blocked': layoutBlocked }"
        :style="selectionFrameStyle"
      >
        <button
          v-for="handle in RESIZE_HANDLES"
          :key="handle"
          type="button"
          class="resize-handle"
          :class="`handle-${handle}`"
          :aria-label="HANDLE_LABELS[handle]"
          :disabled="layoutSaving"
          @pointerdown.prevent.stop="startResize(handle, $event)"
          @keydown="onResizeHandleKeydown(handle, $event)"
        ></button>
      </div>

      <div
        v-if="swapTargetRect"
        class="layout-swap-frame"
        :style="swapFrameStyle"
        aria-hidden="true"
      >
        <span class="swap-frame-badge">松手交换</span>
      </div>

      <div v-if="!activeModules.length" class="panel dashboard-empty">
        <span class="kicker">EMPTY DASHBOARD</span>
        <h3>所有内容模块已隐藏</h3>
        <p>可以随时在数据设置里重新打开需要的内容。</p>
        <el-button type="primary" @click="openSettings">去设置</el-button>
      </div>
      <div v-else-if="!renderableModules.length" class="panel dashboard-empty">
        <span class="kicker">SCOPE EMPTY</span>
        <h3>当前范围暂无展示模块</h3>
        <p>选择球队或在数据设置中启用其他模块。</p>
        <el-button type="primary" @click="openSettings">去设置</el-button>
      </div>
    </template>

    <div v-else-if="loading" class="page-state">
      <el-skeleton :rows="6" animated />
    </div>
    <div v-else-if="loadError" class="page-state">
      <el-empty description="驾驶舱加载失败">
        <el-button type="primary" @click="load">重新加载</el-button>
      </el-empty>
      <p class="state-detail">{{ loadError }}</p>
    </div>
    <div v-else class="page-state">
      <el-empty description="尚未关联球员档案">
        <el-button type="primary" @click="goPortal">返回门户</el-button>
      </el-empty>
    </div>

    <el-drawer
      v-model="settingsOpen"
      title="数据设置"
      size="min(430px, 92vw)"
      :append-to-body="false"
      class="clubhouse-settings"
      destroy-on-close
      @opened="initModuleSortable"
      @closed="destroyModuleSortable"
    >
      <div class="settings-section">
        <div class="settings-label">模块布局</div>
        <p class="settings-hint">拖动左侧把手排序，也可用上下按钮微调；顺序用于手机/紧凑模式。桌面位置与大小请在页面「编辑布局」中拖动调整。</p>
        <div ref="moduleListEl" class="module-sort-list">
          <div
            v-for="(module, index) in draft.modules"
            :key="module.key"
            class="module-sort-row"
            :class="{ 'is-off': !module.enabled }"
          >
            <span class="module-drag" aria-hidden="true"></span>
            <div class="module-sort-name">
              <b>{{ module.label }}</b>
              <span>{{ moduleSizeLabel(module) }}<template v-if="module.key === 'ranks'"> · 仅在选择球队后显示</template></span>
            </div>
            <div class="module-sort-actions">
              <el-button link :icon="ArrowUp" :disabled="index === 0" aria-label="上移" @click="moveDraftModule(index, -1)" />
              <el-button link :icon="ArrowDown" :disabled="index === draft.modules.length - 1" aria-label="下移" @click="moveDraftModule(index, 1)" />
            </div>
            <div class="module-switch" @click.self="module.enabled = !module.enabled">
              <el-switch v-model="module.enabled" size="small" :aria-label="`显示${module.label}`" />
            </div>
          </div>
        </div>
        <div class="module-sort-count">已启用 {{ enabledDraftCount }} / {{ draft.modules.length }}</div>
      </div>

      <div class="settings-section">
        <div class="settings-label">生涯里程碑 · 最多 6 项</div>
        <p class="settings-hint">选择要展示的累计指标；时间线会展示已选指标最近达成的里程碑。</p>
        <el-select
          v-model="draft.milestones.metricKeys"
          multiple
          filterable
          :multiple-limit="6"
          class="settings-control"
        >
          <el-option-group label="打击">
            <el-option
              v-for="metric in milestoneBattingOptions"
              :key="metric.key"
              :label="`${metric.label} ${metric.shortLabel}`"
              :value="metric.key"
            />
          </el-option-group>
          <el-option-group label="投球">
            <el-option
              v-for="metric in milestonePitchingOptions"
              :key="metric.key"
              :label="`${metric.label} ${metric.shortLabel}`"
              :value="metric.key"
            />
          </el-option-group>
        </el-select>
        <div class="settings-count">已选 {{ draft.milestones.metricKeys.length }} / 6</div>
      </div>

      <div class="settings-section">
        <div class="settings-label">表现图</div>
        <p class="settings-hint">选择要展示的指标与图表类型；不可用的指标会置灰。</p>
        <el-form label-position="top">
          <el-form-item label="指标">
            <el-select v-model="draft.performance.metricKey" filterable class="settings-control" @change="onPerformanceMetricChange">
              <el-option-group label="打击">
                <el-option v-for="metric in battingMetrics" :key="metric.key" :label="metric.label" :value="metric.key" :disabled="!metric.available" />
              </el-option-group>
              <el-option-group label="投球">
                <el-option v-for="metric in pitchingMetrics" :key="metric.key" :label="metric.label" :value="metric.key" :disabled="!metric.available" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item label="图表类型">
            <el-radio-group v-model="draft.performance.viz">
              <el-radio-button v-for="viz in performanceVizOptions" :key="viz" :value="viz">{{ vizLabel(viz) }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>

      <div class="settings-section">
        <div class="settings-label">队内排名 · 四个指标</div>
        <p class="settings-hint">排名按当前代表球队的同队同赛事或球队跨赛事生涯计算；未选择球队时模块不显示。默认不提供全垒打。</p>
        <el-form label-position="top">
          <el-form-item v-for="(slot, index) in draft.ranks" :key="index" :label="`排名位 ${index + 1}`">
            <el-select v-model="slot.metricKey" filterable class="settings-control">
              <el-option-group label="打击">
                <el-option v-for="metric in battingRankMetrics" :key="metric.key" :label="metric.label" :value="metric.key" :disabled="rankOptionDisabled(metric, index)" />
              </el-option-group>
              <el-option-group label="投球">
                <el-option v-for="metric in pitchingRankMetrics" :key="metric.key" :label="metric.label" :value="metric.key" :disabled="rankOptionDisabled(metric, index)" />
              </el-option-group>
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="resetSettings">恢复默认</el-button>
        <el-button @click="settingsOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSettings">保存</el-button>
      </template>
    </el-drawer>

    <Teleport to="body">
      <div v-if="cardZoomOpen" class="pcard-zoom" role="dialog" aria-modal="true" aria-label="球星卡放大查看">
        <div class="pcard-zoom-backdrop" :class="{ on: cardZoomShown }" @click="closeCardZoom"></div>
        <div class="pcard-zoom-card" :class="{ on: cardZoomShown }" :style="zoomCardStyle">
          <ClubhousePlayerCard v-bind="cardFaceProps" :scale="zoomScaleValue" />
        </div>
        <button ref="zoomCloseBtnRef" type="button" class="pcard-zoom-close" aria-label="关闭放大" @click="closeCardZoom">×</button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, ArrowUp, Setting } from '@element-plus/icons-vue';
import Sortable from 'sortablejs';
import dayjs from 'dayjs';
import { clubhouseApi } from '../../api/clubhouse';
import ClubhousePlayerCard from '../../components/portal/ClubhousePlayerCard.vue';
import { CARD_BASE_WIDTH, CARD_BASE_HEIGHT } from '../../components/portal/clubhouseCardGrade.js';
import { isAuthSessionError, resolveAssetUrl } from '../../api/request';
import { useTenantRouter } from '../../composables/useTenantRouter';
import {
  BOARD_WIDTH,
  BOARD_GAP,
  BOARD_SNAP,
  MAX_MODULE_HEIGHT,
  MAX_MODULE_Y,
  boardHeight as computeBoardHeight,
  clamp,
  clampToBoard,
  collidesWithAny,
  findSwapTarget,
  movedRect,
  moveModule,
  packModules,
  planSwap,
  resizeModule
} from './clubhouseBoard.js';

const router = useRouter();
const { tenantCode } = useTenantRouter();

const SCOPE_ALL = 0;
const DEFAULT_RANK_KEYS = ['batting.avg', 'batting.h', 'batting.obp', 'batting.rbi'];
const DEFAULT_MODULE_KEYS = ['ranks', 'evaluation', 'milestones', 'performance', 'teams', 'schedule', 'about', 'recentGames', 'quickLinks'];
const CONFIG_VERSION = 4;
const CONFIG_LAYOUT = 'v4-board';
const MILESTONE_TRACK_INSET = 4;
const RESIZE_HANDLES = ['nw', 'n', 'ne', 'w', 'e', 'sw', 's', 'se'];
const HANDLE_LABELS = {
  nw: '左上缩放',
  n: '向上缩放',
  ne: '右上缩放',
  w: '向左缩放',
  e: '向右缩放',
  sw: '左下缩放',
  s: '向下缩放',
  se: '右下缩放'
};
const freeBoardQuery = window.matchMedia('(min-width: 1340px)');
const compactBoardQuery = window.matchMedia('(min-width: 981px) and (max-width: 1339px)');
const data = ref(null);
const loading = ref(false);
const loadError = ref('');
const teamId = ref(SCOPE_ALL);
const eventId = ref(SCOPE_ALL);
const gameMode = ref('BASEBALL');
const settingsOpen = ref(false);
const saving = ref(false);
const pageEl = ref(null);
const layoutEditEntryEl = ref(null);
const layoutEditing = ref(false);
const layoutResizing = ref(false);
const layoutDragging = ref(false);
const layoutSaving = ref(false);
const layoutBlocked = ref(false);
const layoutStatus = ref('');
const contentLimitHeight = ref(0);
const freeBoard = ref(freeBoardQuery.matches);
const compactBoard = ref(compactBoardQuery.matches);
const selectedModuleKey = ref('');
const selectionRect = ref(null);
const swapTargetKey = ref('');
const swapTargetRect = ref(null);
const milestonePayload = ref(null);
const milestonesLoading = ref(false);
const milestonesError = ref('');
const milestoneListEl = ref(null);
const milestoneScroll = reactive({ canScroll: false, thumbHeight: 0, thumbTop: 0 });
const moduleListEl = ref(null);
let milestoneScrollDrag = null;
let milestoneResizeObserver = null;
let moduleSortable = null;
let layoutPointer = null;
const contentMeasureCache = new Map();
let selectionObserver = null;
let keyboardLayoutSnapshot = null;
let keyboardLayoutTimer = null;
let selectionFrameRaf = 0;
let layoutStatusTimer = null;
let requestSeq = 0;
let autoGameModeAdjusted = false;
let eventScopeResetApplied = false;
const areaGradientId = `clubhouse-area-${Math.random().toString(36).slice(2)}`;

const draft = reactive({
  performance: { metricKey: 'batting.avg', viz: 'line' },
  ranks: DEFAULT_RANK_KEYS.map((metricKey) => ({ metricKey })),
  milestones: { metricKeys: [] },
  modules: []
});

const player = computed(() => data.value?.player || {});
const teams = computed(() => data.value?.teams || []);
const catalog = computed(() => data.value?.catalog || []);
const gameModeLabel = (mode) => ({ BASEBALL: '棒球', SOFTBALL: '垒球' }[mode] || mode);
function eventGameMode(event) {
  return String(event?.gameMode || '').trim().toUpperCase() === 'SOFTBALL' ? 'SOFTBALL' : 'BASEBALL';
}
const availableGameModes = computed(() => {
  const modes = new Set();
  teams.value.forEach((team) => (team.events || []).forEach((event) => modes.add(eventGameMode(event))));
  return ['BASEBALL', 'SOFTBALL'].filter((mode) => modes.has(mode));
});
const availableTeams = computed(() => teams.value.filter((team) => (
  (team.events || []).some((event) => eventGameMode(event) === gameMode.value)
)));
const scopeEvents = computed(() => data.value?.events || []);
const scopeTeam = computed(() => teams.value.find((item) => item.id === teamId.value) || null);
/** 全部球队时展示当前运动类型下的全部代表球队；选中球队时只展示该队。 */
const heroTeamNames = computed(() => availableTeams.value
  .map((team) => String(team.name || '').trim())
  .filter(Boolean));
const heroTeamName = computed(() => {
  if (scopeTeam.value) return scopeTeam.value.name;
  return heroTeamNames.value.length ? heroTeamNames.value.join(' / ') : '未加入球队';
});
const heroTeamCrest = computed(() => (scopeTeam.value ? teamCrest(scopeTeam.value)
  : heroTeamNames.value.length ? '全' : '—'));
const heroTeamSub = computed(() => {
  if (scopeTeam.value) return teamEventLabel(scopeTeam.value);
  return heroTeamNames.value.length ? '跨全部代表球队' : '尚未加入球队';
});
const primaryPosition = computed(() => (player.value.positions || [])[0] || '未填写');
const throwBatLabel = computed(() => {
  const throwHand = String(player.value.throwHand || '-').replace(/投$/, '');
  const batHand = String(player.value.batHand || '-').replace(/打$/, '');
  return `${throwHand}投${batHand}打`;
});
const cardFaceProps = computed(() => {
  const logoTeam = scopeTeam.value || availableTeams.value[0];
  return {
    player: player.value,
    grade: evaluationOverall.value.grade || '',
    score: evaluationOverall.value.score ?? null,
    positionLabel: primaryPosition.value,
    throwBatLabel: throwBatLabel.value,
    teamName: heroTeamName.value,
    teamLogo: resolveAssetUrl(logoTeam?.logo),
    teamCrest: heroTeamCrest.value
  };
});

const heroCardBtnRef = ref(null);
const zoomCloseBtnRef = ref(null);
const cardZoomOpen = ref(false);
const cardZoomShown = ref(false);
const ZOOM_INITIAL_SCALE = 1.4;
const zoomScaleValue = ref(ZOOM_INITIAL_SCALE);
const zoomTarget = ref({
  tx: 0,
  ty: 0,
  tw: Math.round(CARD_BASE_WIDTH * ZOOM_INITIAL_SCALE),
  th: Math.round(CARD_BASE_HEIGHT * ZOOM_INITIAL_SCALE)
});
const zoomFrom = ref({ dx: 0, dy: 0, k: 1 });
let zoomCloseTimer = 0;

const narrowCardQuery = window.matchMedia('(max-width: 980px)');
const isNarrowCard = ref(narrowCardQuery.matches);
function onNarrowCardChange(event) {
  isNarrowCard.value = event.matches;
}
narrowCardQuery.addEventListener('change', onNarrowCardChange);
// 扇形背景：固定两张 The Show 风格传奇卡，纯装饰不参与数据口径
const fanLegendCards = [
  { player: { name: 'SUZUKI' }, grade: 'S', score: 97, positionLabel: 'RF', throwBatLabel: 'R投L打', teamName: 'LEGENDS', teamLogo: '', teamCrest: '★' },
  { player: { name: 'RUTH' }, grade: 'A+', score: 93, positionLabel: '1B', throwBatLabel: 'L投L打', teamName: 'LEGENDS', teamLogo: '', teamCrest: '★' }
];
const heroCardScale = computed(() => (isNarrowCard.value ? 1 : 1.16));
// 手持扇形：桌面端主卡歪斜，放大飞入时顺势转正
const heroTiltDeg = computed(() => (isNarrowCard.value ? 0 : -8));

function calcZoomScale() {
  const scale = Math.min((window.innerHeight - 140) / CARD_BASE_HEIGHT, (window.innerWidth - 56) / CARD_BASE_WIDTH, 1.8);
  return Math.max(1.05, Math.round(scale * 100) / 100);
}

/** 按视口重算放大卡的缩放与居中目标位 */
function applyZoomLayout() {
  const scale = calcZoomScale();
  const tw = Math.round(CARD_BASE_WIDTH * scale);
  const th = Math.round(CARD_BASE_HEIGHT * scale);
  zoomScaleValue.value = scale;
  zoomTarget.value = {
    tx: Math.round((window.innerWidth - tw) / 2),
    ty: Math.round((window.innerHeight - th) / 2),
    tw,
    th
  };
}

const zoomCardStyle = computed(() => {
  const { tx, ty, tw, th } = zoomTarget.value;
  const { dx, dy, k } = zoomFrom.value;
  return {
    left: `${tx}px`,
    top: `${ty}px`,
    width: `${tw}px`,
    height: `${th}px`,
    transform: cardZoomShown.value ? 'none' : `translate(${dx}px, ${dy}px) scale(${k}) rotate(${heroTiltDeg.value}deg)`
  };
});

function openCardZoom() {
  applyZoomLayout();
  const { tx, ty, tw, th } = zoomTarget.value;
  // 主卡带扇形歪斜：取视觉包围盒中心定位，缩放比按卡面名义宽度算（包围盒因旋转偏宽）
  const origin = heroCardBtnRef.value?.querySelector(':scope > .pcard')?.getBoundingClientRect();
  zoomFrom.value = origin
    ? {
      dx: origin.left + origin.width / 2 - (tx + tw / 2),
      dy: origin.top + origin.height / 2 - (ty + th / 2),
      k: (CARD_BASE_WIDTH * heroCardScale.value) / tw
    }
    : { dx: 0, dy: 0, k: 0.6 };
  cardZoomOpen.value = true;
  cardZoomShown.value = false;
  document.body.style.overflow = 'hidden';
  window.addEventListener('keydown', onZoomKeydown);
  window.addEventListener('resize', applyZoomLayout);
  requestAnimationFrame(() => requestAnimationFrame(() => {
    cardZoomShown.value = true;
  }));
  nextTick(() => zoomCloseBtnRef.value?.focus());
}

function closeCardZoom() {
  if (!cardZoomOpen.value) return;
  cardZoomShown.value = false;
  window.removeEventListener('keydown', onZoomKeydown);
  window.removeEventListener('resize', applyZoomLayout);
  clearTimeout(zoomCloseTimer);
  zoomCloseTimer = setTimeout(() => {
    cardZoomOpen.value = false;
    document.body.style.overflow = '';
    heroCardBtnRef.value?.focus();
  }, 420);
}

function onZoomKeydown(event) {
  if (event.key === 'Escape') closeCardZoom();
  // 弹层内仅关闭按钮可聚焦，锁住 Tab 防止焦点跑到遮罩后的页面
  if (event.key === 'Tab') {
    event.preventDefault();
    zoomCloseBtnRef.value?.focus();
  }
}

onBeforeUnmount(() => {
  narrowCardQuery.removeEventListener('change', onNarrowCardChange);
  window.removeEventListener('keydown', onZoomKeydown);
  window.removeEventListener('resize', applyZoomLayout);
  clearTimeout(zoomCloseTimer);
  if (cardZoomOpen.value) document.body.style.overflow = '';
});
const scopeHint = computed(() => (teamId.value
  ? '队内排名按所选球队与赛事/生涯口径计算；雷达为全租户生涯，不随筛选变化'
  : '生涯合计跨全部球队与赛事；队内排名需选择球队；雷达不随筛选变化'));
const scopeLabel = computed(() => {
  const scope = data.value?.scope || {};
  if (scope.teamAll && scope.career) return '生涯';
  return [scope.teamAll ? null : scope.teamName, scope.career ? '生涯' : scope.eventName]
    .filter(Boolean)
    .join(' · ');
});
const rankScopeLabel = computed(() => (eventId.value ? '同队同赛事' : '球队跨赛事生涯'));
const rankNote = computed(() => `${rankScopeLabel.value} · 无最低打席门槛 · 全垒打不进入排名`);
const scheduleScopeLabel = computed(() => {
  if (eventId.value) return teamId.value ? '同队同赛事' : '当前赛事';
  return teamId.value ? '球队跨赛事生涯' : '跨球队生涯';
});
const recentEmptyText = computed(() => (eventId.value
  ? '该赛事暂无比赛记录。'
  : teamId.value ? '该球队暂无比赛记录。' : '暂无比赛记录。'));
const scheduleEmptyText = computed(() => (eventId.value
  ? '该赛事暂无未开赛的比赛。'
  : teamId.value ? '该球队暂无未开赛的比赛。' : '暂无未开赛的比赛。'));
const rankPopulation = computed(() => {
  const totals = (data.value?.ranks || []).map((item) => item.total).filter((total) => total > 0);
  return totals.length && totals.every((total) => total === totals[0]) ? totals[0] : 0;
});
const performance = computed(() => data.value?.performance || {});
const performanceLabel = computed(() => performance.value.label || '数据');
const performanceScopeLabel = computed(() => scopeLabel.value);
const performanceSummary = computed(() => {
  const value = performance.value.summaryValue;
  return value == null || value === '-' || value === '' ? '—' : value;
});
const batting = computed(() => data.value?.stats?.batting || {});
const pitching = computed(() => data.value?.stats?.pitching || {});
const hasBatting = computed(() => Number(batting.value.ab) > 0 || Number(batting.value.pa) > 0);
const hasPitchingData = computed(() => Number(pitching.value.gp) > 0 || Number(pitching.value.ip) > 0);
const hasStatsData = computed(() => (
  Number(batting.value.gp) > 0
  || Number(batting.value.ab) > 0
  || Number(batting.value.pa) > 0
  || hasPitchingData.value
  || Number(data.value?.stats?.fielding?.gp) > 0
));
const evaluationComparison = computed(() => data.value?.evaluation?.comparison || '全租户生涯');
const evaluationOverall = computed(() => data.value?.evaluation?.overall || {});
const overallRoleLabel = computed(() => ({
  POSITION_PLAYER: '野手',
  PITCHER: '投手',
  TWO_WAY: '二刀流'
}[evaluationOverall.value.role] || '球员'));
const overallSampleText = computed(() => {
  const sizes = evaluationOverall.value.sampleSize || {};
  const parts = [
    formatOwnSample(sizes.batting, 'PA'),
    formatOwnSample(sizes.pitching, 'IP'),
    formatOwnSample(sizes.fielding, 'INN')
  ].filter(Boolean);
  return parts.length ? parts.join(' · ') : '暂无有效样本';
});
/** 旧数据兜底：野手（POSITION_PLAYER）不渲染投球卡，即使接口仍返回了投球卡。 */
const evaluationCards = computed(() => {
  const role = evaluationOverall.value.role;
  return (data.value?.evaluation?.cards || [])
    .filter((card) => !(role === 'POSITION_PLAYER' && card.domain === 'pitching'))
    .map(prepareEvaluationCard);
});
const milestoneData = computed(() => milestonePayload.value || {});
const milestoneItems = computed(() => milestoneData.value.items || []);
const milestoneTimeline = computed(() => milestoneData.value.timeline || []);
const milestoneThumbStyle = computed(() => ({
  height: `${milestoneScroll.thumbHeight}px`,
  transform: `translateY(${milestoneScroll.thumbTop}px)`
}));
const milestoneCatalog = computed(() => data.value?.milestoneCatalog || []);
const milestoneBattingOptions = computed(() => milestoneCatalog.value.filter((item) => item.domain === 'batting'));
const milestonePitchingOptions = computed(() => milestoneCatalog.value.filter((item) => item.domain === 'pitching'));
watch(milestoneTimeline, async () => {
  await nextTick();
  if (milestoneListEl.value) {
    milestoneListEl.value.scrollTop = 0;
  }
  updateMilestoneScrollbar();
  observeMilestoneList();
});
const nextMilestoneText = computed(() => {
  if (milestonesLoading.value) return '加载中';
  if (milestonesError.value) return '暂不可用';
  if (!milestoneItems.value.length) return '等待数据';
  const next = milestoneItems.value.find((item) => item.nextTarget != null);
  return next ? `${next.nextTarget} ${next.label}` : '已达最高里程碑';
});
const enabledDraftCount = computed(() => draft.modules.filter((module) => module.enabled).length);
const moduleCatalog = computed(() => data.value?.moduleCatalog || []);
const moduleMeta = computed(() => Object.fromEntries(
  moduleCatalog.value.map((item) => [item.key, item])
));
function finiteNumber(value, fallback) {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : fallback;
}
function resolveModule(module) {
  const meta = moduleMeta.value[module.key] || {};
  const minWidth = finiteNumber(meta.minWidth, 240);
  const minHeight = finiteNumber(meta.minHeight, 160);
  const defaultWidth = finiteNumber(meta.defaultWidth, 400);
  const defaultHeight = finiteNumber(meta.defaultHeight, 240);
  const width = clamp(Math.round(finiteNumber(module.width, defaultWidth)), minWidth, BOARD_WIDTH);
  const height = clamp(Math.round(finiteNumber(module.height, defaultHeight)), minHeight, MAX_MODULE_HEIGHT);
  const x = clamp(Math.round(finiteNumber(module.x, 0)), 0, BOARD_WIDTH - width);
  const y = clamp(Math.round(finiteNumber(module.y, 0)), 0, MAX_MODULE_Y);
  return {
    ...module,
    enabled: module.enabled !== false,
    label: meta.label || module.label || module.key,
    x,
    y,
    width,
    height,
    minWidth,
    minHeight,
    defaultWidth,
    defaultHeight
  };
}
function defaultModules() {
  return packModules(DEFAULT_MODULE_KEYS.map((key) => {
    const meta = moduleMeta.value[key] || {};
    return resolveModule({
      key,
      enabled: true,
      width: meta.defaultWidth,
      height: meta.defaultHeight
    });
  }));
}
const activeModules = computed(() => {
  const configured = data.value?.config?.modules;
  const source = configured?.length ? configured : defaultModules();
  return source.filter((module) => module.enabled !== false).map(resolveModule);
});
/**
 * 查看态全部球队时收起隐藏的队内坐标占位，避免页面顶部出现大块空白；
 * 编辑态保留占位以便调整位置。任何潜在重叠都退回原始布局。
 */
const viewLayoutModules = computed(() => {
  const modules = activeModules.value;
  const ranks = modules.find((module) => module.key === 'ranks');
  if (layoutEditing.value || teamId.value || !ranks) return modules;
  const cutoff = ranks.y + ranks.height + BOARD_GAP;
  const shift = ranks.height + BOARD_GAP;
  const compacted = modules
    .filter((module) => module.key !== 'ranks')
    .map((module) => (module.y >= cutoff ? { ...module, y: module.y - shift } : module));
  const collision = compacted.some((module) => collidesWithAny(module, compacted));
  return collision ? modules : compacted;
});
/** 当前范围下真正渲染的模块：队内坐标仅在选中具体球队时出现。 */
const renderableModules = computed(() => viewLayoutModules.value.filter((module) => (
  module.key !== 'ranks' || Boolean(teamId.value)
)));
const moduleIndex = computed(() => new Map(activeModules.value.map((module, index) => [module.key, index])));
const renderableKeys = computed(() => new Set(renderableModules.value.map((module) => module.key)));
const activeKeys = computed(() => new Set(activeModules.value.map((module) => module.key)));
const boardHeight = computed(() => computeBoardHeight(viewLayoutModules.value));
function isModuleVisible(key) {
  return renderableKeys.value.has(key);
}
function isModuleActive(key) {
  return activeKeys.value.has(key);
}
function moduleLayoutStyle(key) {
  const source = layoutEditing.value ? activeModules.value : viewLayoutModules.value;
  const module = source.find((item) => item.key === key);
  if (!module) return {};
  const order = moduleIndex.value.get(key) ?? 0;
  if (freeBoard.value) {
    return {
      order,
      left: `${module.x}px`,
      top: `${module.y}px`,
      width: `${module.width}px`,
      height: `${module.height}px`,
      ...(layoutDragging.value && selectedModuleKey.value === key
        ? { zIndex: 30, opacity: swapTargetKey.value ? 0.6 : undefined, transition: 'opacity 120ms ease-out' }
        : {})
    };
  }
  if (compactBoard.value && module.width >= 900) {
    return { order, gridColumn: '1 / -1' };
  }
  return { order };
}

const selectedModule = computed(() => activeModules.value.find((item) => item.key === selectedModuleKey.value) || null);
const selectionFrameStyle = computed(() => frameStyle(selectionRect.value));
const selectionSizeLabel = computed(() => {
  const module = selectedModule.value;
  if (!module) return '';
  return `${module.width} × ${module.height} px`;
});
const swapTargetLabel = computed(() => {
  if (!swapTargetKey.value) return '';
  return activeModules.value.find((module) => module.key === swapTargetKey.value)?.label || '';
});
const swapFrameStyle = computed(() => frameStyle(swapTargetRect.value));

function moduleElement(key) {
  return document.querySelector(`.dashboard-module[data-module-key="${key}"]`);
}

/** 模块相对页面容器的矩形；用于选中框与交换目标框。 */
function frameRect(key) {
  if (!key) return null;
  const el = moduleElement(key);
  const page = pageEl.value;
  if (!el || !page) return null;
  const rect = el.getBoundingClientRect();
  const pageRect = page.getBoundingClientRect();
  return {
    left: rect.left - pageRect.left,
    top: rect.top - pageRect.top,
    width: rect.width,
    height: rect.height
  };
}

function frameStyle(rect) {
  if (!rect) return {};
  return {
    left: `${rect.left - 2}px`,
    top: `${rect.top - 2}px`,
    width: `${rect.width + 4}px`,
    height: `${rect.height + 4}px`
  };
}

/** 量出模块在指定宽度下的内容自然高度（临时 auto 高度，量完立即还原）。 */
function measureModuleContentHeight(key, width) {
  const el = moduleElement(key);
  if (!el) return 0;
  const cacheKey = `${key}#${width == null ? 'auto' : Math.round(width)}`;
  if (contentMeasureCache.has(cacheKey)) return contentMeasureCache.get(cacheKey);
  const prevWidth = el.style.width;
  const prevHeight = el.style.height;
  if (width != null && Number.isFinite(width)) el.style.width = `${Math.max(1, Math.round(width))}px`;
  el.style.height = 'auto';
  const height = el.offsetHeight;
  el.style.width = prevWidth;
  el.style.height = prevHeight;
  contentMeasureCache.set(cacheKey, height);
  return height;
}

function resizeModuleWithinContent(startRect, direction, dx, dy, minWidth, minHeight, others) {
  const base = resizeModule(startRect, direction, dx, dy, minWidth, minHeight, others);
  if (!base) return { candidate: null, contentMin: 0, limited: false };
  const contentMin = measureModuleContentHeight(startRect.key, base.width);
  if (!contentMin || base.height + 0.5 >= contentMin) {
    return { candidate: base, contentMin, limited: false };
  }
  const adjusted = resizeModule(
    startRect,
    direction,
    dx,
    dy,
    minWidth,
    Math.max(minHeight, contentMin),
    others
  );
  return { candidate: adjusted, contentMin, limited: true };
}

function updateSelectionRect() {
  if (!layoutEditing.value || !selectedModuleKey.value) {
    selectionRect.value = null;
    return;
  }
  selectionRect.value = frameRect(selectedModuleKey.value);
}

function scheduleSelectionFrame() {
  if (selectionFrameRaf) return;
  selectionFrameRaf = requestAnimationFrame(() => {
    selectionFrameRaf = 0;
    updateSelectionRect();
    updateSwapFrame();
  });
}

function updateSwapFrame() {
  swapTargetRect.value = swapTargetKey.value ? frameRect(swapTargetKey.value) : null;
}

function setSwapTarget(target) {
  swapTargetKey.value = target?.key || '';
  updateSwapFrame();
}

function clearSwapTarget() {
  swapTargetKey.value = '';
  swapTargetRect.value = null;
}

function applyModuleSelection(key, focusHandle = false) {
  selectedModuleKey.value = key;
  contentLimitHeight.value = 0;
  updateSelectionRect();
  nextTick(() => {
    updateSelectionRect();
    observeSelectedModule();
    if (focusHandle) {
      document.querySelector('.layout-selection-frame .handle-se')?.focus();
    }
  });
}

function selectModule(key, focusHandle = false) {
  if (!layoutEditing.value) return;
  if (selectedModuleKey.value && selectedModuleKey.value !== key) {
    flushKeyboardLayout();
  }
  applyModuleSelection(key, focusHandle);
}

function clearModuleSelection({ restoreFocus = false } = {}) {
  const key = selectedModuleKey.value;
  cancelLayoutPointer();
  flushKeyboardLayout();
  selectedModuleKey.value = '';
  selectionRect.value = null;
  contentLimitHeight.value = 0;
  selectionObserver?.disconnect();
  selectionObserver = null;
  if (restoreFocus && key) {
    nextTick(() => moduleElement(key)?.focus());
  }
}

function observeSelectedModule() {
  selectionObserver?.disconnect();
  selectionObserver = null;
  const key = selectedModuleKey.value;
  const el = key ? moduleElement(key) : null;
  if (!el || typeof ResizeObserver === 'undefined') return;
  selectionObserver = new ResizeObserver(scheduleSelectionFrame);
  selectionObserver.observe(el);
}

/** 编辑时把模块内容设为 inert，既禁用指针也移出 Tab 顺序；模块外壳仍可选中/拖动。 */
function syncModuleContentInert() {
  document.querySelectorAll('.dashboard-board .dashboard-module').forEach((moduleEl) => {
    Array.from(moduleEl.children).forEach((child) => {
      if (layoutEditing.value) child.setAttribute('inert', '');
      else child.removeAttribute('inert');
    });
  });
}

watch(layoutEditing, async () => {
  await nextTick();
  syncModuleContentInert();
});

function onModuleKeydown(key, event) {
  if (!layoutEditing.value) return;
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    selectModule(key, true);
    return;
  }
  if (selectedModuleKey.value !== key) return;
  const horizontal = event.key === 'ArrowLeft' ? -1 : event.key === 'ArrowRight' ? 1 : 0;
  const vertical = event.key === 'ArrowUp' ? -1 : event.key === 'ArrowDown' ? 1 : 0;
  if (!horizontal && !vertical) return;
  event.preventDefault();
  if (event.shiftKey) {
    const verticalEdge = vertical < 0 ? 'n' : vertical > 0 ? 's' : '';
    const horizontalEdge = horizontal < 0 ? 'w' : horizontal > 0 ? 'e' : '';
    resizeSelectedBy(`${verticalEdge}${horizontalEdge}`, horizontal * BOARD_SNAP, vertical * BOARD_SNAP);
  } else {
    moveSelectedBy(horizontal * BOARD_SNAP, vertical * BOARD_SNAP);
  }
}

function onResizeHandleKeydown(direction, event) {
  const horizontal = event.key === 'ArrowLeft' ? -1 : event.key === 'ArrowRight' ? 1 : 0;
  const vertical = event.key === 'ArrowUp' ? -1 : event.key === 'ArrowDown' ? 1 : 0;
  const dx = direction.includes('e') || direction.includes('w') ? horizontal * BOARD_SNAP : 0;
  const dy = direction.includes('n') || direction.includes('s') ? vertical * BOARD_SNAP : 0;
  if (!dx && !dy) return;
  event.preventDefault();
  resizeSelectedBy(direction, dx, dy);
}

function moduleRect(module) {
  return {
    key: module.key,
    x: module.x,
    y: module.y,
    width: module.width,
    height: module.height
  };
}

function otherModuleRects(key) {
  return activeModules.value.filter((module) => module.key !== key);
}

function moveSelectedBy(dx, dy) {
  const module = selectedModule.value;
  if (!module || layoutSaving.value || !freeBoard.value) return false;
  contentLimitHeight.value = 0;
  const snapshot = keyboardLayoutSnapshot || currentModuleConfig();
  const candidate = moveModule(moduleRect(module), dx, dy, otherModuleRects(module.key));
  if (!candidate) return false;
  updateModuleRect(module.key, candidate);
  keyboardLayoutSnapshot = snapshot;
  nextTick(updateSelectionRect);
  scheduleKeyboardLayoutSave();
  return true;
}

function resizeSelectedBy(direction, dx, dy) {
  const module = selectedModule.value;
  if (!module || layoutSaving.value || !freeBoard.value || !direction) return false;
  const snapshot = keyboardLayoutSnapshot || currentModuleConfig();
  const result = resizeModuleWithinContent(
    moduleRect(module),
    direction,
    dx,
    dy,
    module.minWidth,
    module.minHeight,
    otherModuleRects(module.key)
  );
  contentLimitHeight.value = result.limited ? result.contentMin : 0;
  if (!result.candidate) return false;
  updateModuleRect(module.key, result.candidate);
  keyboardLayoutSnapshot = snapshot;
  nextTick(updateSelectionRect);
  scheduleKeyboardLayoutSave();
  return true;
}

function scheduleKeyboardLayoutSave() {
  if (keyboardLayoutTimer) window.clearTimeout(keyboardLayoutTimer);
  keyboardLayoutTimer = window.setTimeout(() => {
    keyboardLayoutTimer = null;
    const snapshot = keyboardLayoutSnapshot;
    keyboardLayoutSnapshot = null;
    if (snapshot && moduleLayoutChanged(snapshot, currentModuleConfig())) {
      persistLayout(snapshot);
    }
  }, 320);
}

function flushKeyboardLayout() {
  if (keyboardLayoutTimer) {
    window.clearTimeout(keyboardLayoutTimer);
    keyboardLayoutTimer = null;
  }
  const snapshot = keyboardLayoutSnapshot;
  keyboardLayoutSnapshot = null;
  if (snapshot && moduleLayoutChanged(snapshot, currentModuleConfig())) {
    persistLayout(snapshot);
  }
}

function cancelKeyboardLayout() {
  if (keyboardLayoutTimer) window.clearTimeout(keyboardLayoutTimer);
  keyboardLayoutTimer = null;
  keyboardLayoutSnapshot = null;
}

function startModulePointer(key, event) {
  if (!layoutEditing.value || layoutSaving.value || layoutPointer || !freeBoard.value) return;
  if (event.pointerType === 'mouse' && event.button !== 0) return;
  // 有未落地的键盘微调时，flush 会立即开始保存，本次手势应放弃
  flushKeyboardLayout();
  if (layoutSaving.value) return;
  if (selectedModuleKey.value !== key) {
    applyModuleSelection(key);
  }
  const module = selectedModule.value;
  if (!module) return;
  contentLimitHeight.value = 0;
  event.preventDefault();
  try {
    event.currentTarget.setPointerCapture?.(event.pointerId);
  } catch {
    // 旧浏览器不支持指针捕获，窗口监听仍可完成拖动
  }
  beginLayoutPointer(event, {
    mode: 'move',
    key,
    startRect: moduleRect(module),
    snapshot: currentModuleConfig()
  });
}

function startResize(direction, event) {
  if (!layoutEditing.value || layoutSaving.value || layoutPointer || !freeBoard.value) return;
  const module = selectedModule.value;
  if (!module) return;
  if (event.pointerType === 'mouse' && event.button !== 0) return;
  // 有未落地的键盘微调时，flush 会立即开始保存，本次手势应放弃
  flushKeyboardLayout();
  if (layoutSaving.value) return;
  contentLimitHeight.value = 0;
  beginLayoutPointer(event, {
    mode: 'resize',
    key: module.key,
    direction,
    startRect: moduleRect(module),
    minWidth: module.minWidth,
    minHeight: module.minHeight,
    snapshot: currentModuleConfig()
  });
}

function beginLayoutPointer(event, state) {
  contentMeasureCache.clear();
  layoutPointer = {
    ...state,
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    startScrollY: window.scrollY,
    lastClientX: event.clientX,
    lastClientY: event.clientY,
    moved: false,
    lastValid: state.startRect,
    swapPlan: null,
    raf: 0
  };
  window.addEventListener('pointermove', onLayoutPointerMove, true);
  window.addEventListener('pointerup', finishLayoutPointer, true);
  window.addEventListener('pointercancel', cancelLayoutPointer, true);
  window.addEventListener('lostpointercapture', finishLayoutPointer, true);
  window.addEventListener('blur', cancelLayoutPointer, true);
  layoutPointer.raf = requestAnimationFrame(autoScrollLayoutPointer);
}

function onLayoutPointerMove(event) {
  if (!layoutPointer || event.pointerId !== layoutPointer.pointerId) return;
  if (event.buttons === 0) {
    finishLayoutPointer(event);
    return;
  }
  event.preventDefault();
  layoutPointer.lastClientX = event.clientX;
  layoutPointer.lastClientY = event.clientY;
  applyLayoutPointer();
}

function applyLayoutPointer() {
  const state = layoutPointer;
  if (!state) return;
  if (!state.moved) {
    if (Math.hypot(state.lastClientX - state.startX, state.lastClientY - state.startY) < 4) return;
    state.moved = true;
    // 手柄获得焦点可能触发一次浏览器滚动；从手势真正开始时重新记录滚动原点。
    state.startScrollY = window.scrollY;
  }
  const dx = state.lastClientX - state.startX;
  const dy = state.lastClientY - state.startY + (window.scrollY - state.startScrollY);
  const others = otherModuleRects(state.key);
  let candidate;
  let swapPlan = null;
  if (state.mode === 'move') {
    const dragged = movedRect(state.startRect, dx, dy);
    candidate = collidesWithAny(dragged, others) ? null : dragged;
    updateModuleRect(state.key, dragged);
    if (candidate) {
      state.lastValid = candidate;
    } else {
      const target = findSwapTarget(dragged, others);
      swapPlan = target ? planSwap(state.startRect, target, others) : null;
    }
  } else {
    const result = resizeModuleWithinContent(
      state.startRect,
      state.direction,
      dx,
      dy,
      state.minWidth,
      state.minHeight,
      others
    );
    candidate = result.candidate;
    contentLimitHeight.value = result.limited ? result.contentMin : 0;
    if (candidate) updateModuleRect(state.key, candidate);
  }
  state.swapPlan = swapPlan;
  if (swapPlan) {
    setSwapTarget(swapPlan.target);
  } else {
    clearSwapTarget();
  }
  layoutBlocked.value = !candidate && !swapPlan;
  nextTick(scheduleSelectionFrame);
  layoutDragging.value = state.mode === 'move';
  layoutResizing.value = state.mode === 'resize';
}

function finishLayoutPointer(event) {
  if (!layoutPointer) return;
  if (event?.pointerId !== undefined && event.pointerId !== layoutPointer.pointerId) return;
  const state = layoutPointer;
  clearLayoutPointer();
  nextTick(updateSelectionRect);
  if (state.swapPlan) {
    applySwapPlan(state.swapPlan);
  } else if (state.mode === 'move') {
    updateModuleRect(state.key, state.lastValid);
  }
  if (state.moved && moduleLayoutChanged(state.snapshot, currentModuleConfig())) {
    persistLayout(state.snapshot);
  }
}

function applySwapPlan(plan) {
  updateModuleRect(plan.dragged.key, plan.dragged);
  updateModuleRect(plan.target.key, plan.target);
  for (const row of plan.moved) updateModuleRect(row.key, row);
  nextTick(scheduleSelectionFrame);
}

function cancelLayoutPointer() {
  clearSwapTarget();
  if (!layoutPointer) return;
  const snapshot = layoutPointer.snapshot;
  clearLayoutPointer();
  if (data.value?.config) {
    data.value.config.modules = snapshot;
  }
  nextTick(updateSelectionRect);
}

function clearLayoutPointer() {
  clearSwapTarget();
  if (!layoutPointer) return;
  if (layoutPointer.raf) cancelAnimationFrame(layoutPointer.raf);
  layoutPointer = null;
  layoutDragging.value = false;
  layoutResizing.value = false;
  layoutBlocked.value = false;
  window.removeEventListener('pointermove', onLayoutPointerMove, true);
  window.removeEventListener('pointerup', finishLayoutPointer, true);
  window.removeEventListener('pointercancel', cancelLayoutPointer, true);
  window.removeEventListener('lostpointercapture', finishLayoutPointer, true);
  window.removeEventListener('blur', cancelLayoutPointer, true);
}

function autoScrollLayoutPointer() {
  const state = layoutPointer;
  if (!state) return;
  if (state.moved) {
    const threshold = 48;
    const movingUp = state.lastClientY < state.startY;
    const movingDown = state.lastClientY > state.startY;
    let delta = 0;
    if (movingUp && state.lastClientY < threshold) {
      delta = -Math.min(12, Math.ceil(((threshold - state.lastClientY) / threshold) * 12));
    } else if (movingDown && state.lastClientY > window.innerHeight - threshold) {
      delta = Math.min(12, Math.ceil(((state.lastClientY - (window.innerHeight - threshold)) / threshold) * 12));
    }
    if (delta) {
      window.scrollBy(0, delta);
      applyLayoutPointer();
    }
  }
  if (layoutPointer) layoutPointer.raf = requestAnimationFrame(autoScrollLayoutPointer);
}

function enterLayoutEditing() {
  if (!data.value || layoutSaving.value || !renderableModules.value.length || !freeBoard.value) return;
  settingsOpen.value = false;
  layoutEditing.value = true;
  clearModuleSelection();
  setLayoutStatus('');
  nextTick(() => moduleElement(renderableModules.value[0]?.key)?.focus());
}

function finishLayoutEditing() {
  cancelLayoutPointer();
  flushKeyboardLayout();
  layoutEditing.value = false;
  clearModuleSelection();
  setLayoutStatus('');
  nextTick(() => layoutEditEntryEl.value?.$el?.focus?.());
}

function onLayoutKeydown(event) {
  if (!layoutEditing.value || event.key !== 'Escape') return;
  if (layoutPointer) {
    cancelLayoutPointer();
    return;
  }
  if (selectedModuleKey.value) {
    clearModuleSelection({ restoreFocus: true });
    return;
  }
  finishLayoutEditing();
}

function currentModuleConfig() {
  const source = data.value?.config?.modules?.length ? data.value.config.modules : defaultModules();
  return toConfigRows(source.map(resolveModule));
}

function ensureModuleRows() {
  const config = data.value?.config;
  if (!config) return null;
  if (!Array.isArray(config.modules) || !config.modules.length) {
    config.modules = currentModuleConfig();
  }
  return config.modules;
}

function updateModuleRect(key, rect) {
  const modules = ensureModuleRows();
  if (!modules) return;
  const row = modules.find((item) => item.key === key);
  if (!row) return;
  row.x = rect.x;
  row.y = rect.y;
  row.width = rect.width;
  row.height = rect.height;
}

function moduleLayoutChanged(before, after) {
  if (before.length !== after.length) return true;
  return before.some((item, index) => {
    const next = after[index];
    return !next
      || item.key !== next.key
      || item.x !== next.x
      || item.y !== next.y
      || item.width !== next.width
      || item.height !== next.height;
  });
}

function toConfigRows(modules) {
  return modules.map(({ key, enabled, x, y, width, height }) => ({
    key,
    enabled: enabled !== false,
    x,
    y,
    width,
    height
  }));
}

function buildConfigPayload(modules, extras = {}) {
  const config = data.value?.config || {};
  const performance = extras.performance
    || (config.performance?.metricKey ? { ...config.performance } : { metricKey: 'batting.avg', viz: 'line' });
  const ranks = extras.ranks
    || (Array.isArray(config.ranks) && config.ranks.length
      ? config.ranks.map((row) => ({ metricKey: row.metricKey }))
      : DEFAULT_RANK_KEYS.map((metricKey) => ({ metricKey })));
  const milestoneKeys = extras.milestones?.metricKeys
    || (Array.isArray(config.milestones?.metricKeys) ? [...config.milestones.metricKeys] : []);
  return {
    version: CONFIG_VERSION,
    layout: CONFIG_LAYOUT,
    modules: toConfigRows(modules),
    milestones: { metricKeys: milestoneKeys },
    performance,
    ranks
  };
}

async function persistLayout(snapshot) {
  if (layoutSaving.value) return;
  layoutSaving.value = true;
  setLayoutStatus('saving');
  try {
    const saved = await clubhouseApi.saveConfig(buildConfigPayload(currentModuleConfig()));
    if (saved && data.value) {
      data.value.config = saved;
    }
    setLayoutStatus('saved');
  } catch (error) {
    if (snapshot && data.value?.config) {
      data.value.config.modules = snapshot;
    }
    setLayoutStatus('');
    ElMessage.error(error?.message || '布局保存失败');
  } finally {
    layoutSaving.value = false;
    await nextTick();
    updateSelectionRect();
    observeSelectedModule();
  }
}

function resetSelectedModule() {
  const module = selectedModule.value;
  if (!module || layoutSaving.value) return;
  cancelKeyboardLayout();
  const snapshot = currentModuleConfig();
  const width = module.defaultWidth || module.width;
  const contentMin = measureModuleContentHeight(module.key, width);
  const candidate = clampToBoard({
    ...moduleRect(module),
    width,
    height: Math.max(module.defaultHeight || module.height, contentMin)
  });
  if (collidesWithAny(candidate, otherModuleRects(module.key))) {
    ElMessage.warning('默认尺寸会与其他模块重叠，请先调整位置');
    return;
  }
  contentLimitHeight.value = 0;
  updateModuleRect(module.key, candidate);
  nextTick(updateSelectionRect);
  persistLayout(snapshot);
}

function setLayoutStatus(status) {
  layoutStatus.value = status;
  if (layoutStatusTimer) {
    window.clearTimeout(layoutStatusTimer);
    layoutStatusTimer = null;
  }
  if (status === 'saved') {
    layoutStatusTimer = window.setTimeout(() => {
      if (layoutStatus.value === 'saved') layoutStatus.value = '';
    }, 1600);
  }
}

function syncBoardMode() {
  freeBoard.value = freeBoardQuery.matches;
  compactBoard.value = compactBoardQuery.matches;
  if (!freeBoard.value && layoutEditing.value) {
    finishLayoutEditing();
    return;
  }
  nextTick(updateSelectionRect);
}

function onViewportResize() {
  updateMilestoneScrollbar();
  if (!layoutEditing.value) return;
  nextTick(updateSelectionRect);
}

const matrixItems = computed(() => {
  if (!hasStatsData.value) return [];
  const b = batting.value;
  if (hasBatting.value) {
    return [
      { label: '上垒率 OBP', value: b.obp ?? '-' },
      { label: '长打率 SLG', value: b.slg ?? '-' },
      { label: 'OPS', value: b.ops ?? '-' },
      { label: '安打 H', value: b.h ?? '-' },
      { label: '盗垒 SB', value: b.sb ?? '-' },
      { label: '保送 BB', value: b.bb ?? '-' }
    ];
  }
  const p = pitching.value;
  if (hasPitchingData.value) {
    return [
      { label: '防御率 ERA', value: p.era ?? '-' },
      { label: 'WHIP', value: p.whip ?? '-' },
      { label: '夺三振 SO', value: p.pitchSo ?? '-' },
      { label: '胜投 W', value: p.w ?? '-' },
      { label: '救援 SV', value: p.sv ?? '-' },
      { label: '投球局数 IP', value: p.ip ?? '-' }
    ];
  }
  const f = data.value?.stats?.fielding || {};
  return [
    { label: '守备率 FPCT', value: f.tcPct ?? '-' },
    { label: '守备机会 TC', value: f.tc ?? '-' },
    { label: '刺杀 PO', value: f.po ?? '-' },
    { label: '助杀 A', value: f.a ?? '-' },
    { label: '失误 E', value: f.e ?? '-' },
    { label: '双杀 DP', value: f.dp ?? '-' }
  ];
});
const nextGame = computed(() => data.value?.nextGame || null);
const currentEvent = computed(() => scopeEvents.value.find((event) => event.id === eventId.value) || null);
const currentEventStatus = computed(() => {
  const status = currentEvent.value?.status;
  return { draft: '未开始', ongoing: '进行中', ended: '已结束' }[status] || '赛程中';
});
const currentEventDateRange = computed(() => {
  const event = currentEvent.value;
  if (!event) return '';
  const start = formatEventDate(event.startDate);
  const end = formatEventDate(event.endDate);
  if (start && end) return `${start} – ${end}`;
  return start || end;
});
const featuredGame = computed(() => {
  const event = currentEvent.value;
  const eventGame = event
    ? (data.value?.schedule || []).find((game) => game.eventId === event.id)
    : null;
  return eventGame || nextGame.value;
});
const featuredGameIsCurrent = computed(() => currentEvent.value != null
  && featuredGame.value?.eventId === currentEvent.value.id);
const upcomingGames = computed(() => {
  const list = data.value?.schedule || [];
  return list.filter((game) => game.gameId !== featuredGame.value?.gameId);
});
const battingMetrics = computed(() => catalog.value.filter((item) => item.domain === 'batting'));
const pitchingMetrics = computed(() => catalog.value.filter((item) => item.domain === 'pitching'));
const battingRankMetrics = computed(() => battingMetrics.value.filter((item) => item.rankable));
const pitchingRankMetrics = computed(() => pitchingMetrics.value.filter((item) => item.rankable));
const performanceVizOptions = computed(() => catalog.value.find((item) => item.key === draft.performance.metricKey)?.viz || []);
const donutTotal = computed(() => (performance.value.items || []).reduce((sum, item) => sum + Number(item.value || 0), 0));
const donutStyle = computed(() => {
  const items = performance.value.items || [];
  const total = donutTotal.value;
  if (!total) return 'var(--bs-surface-soft)';
  let offset = 0;
  const stops = items.map((item, index) => {
    const start = offset;
    offset += (Number(item.value || 0) / total) * 100;
    return `${chartColor(index)} ${start}% ${offset}%`;
  });
  return `conic-gradient(${stops.join(', ')})`;
});
const chart = computed(() => buildChart(performance.value.points || [], performance.value.viz, performance.value.metricKey));

function buildChart(points, viz, metricKey) {
  if (!points.length) return { points: [], bars: [], xLabels: [], yTicks: [], linePath: '', areaPath: '' };
  const width = 640;
  const height = 190;
  const padLeft = 46;
  const padRight = 16;
  const padTop = 18;
  const padBottom = 32;
  const values = points.map((point) => Number(point.value)).filter((value) => Number.isFinite(value));
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const plotW = width - padLeft - padRight;
  const plotH = height - padTop - padBottom;
  const xAt = (index) => padLeft + (points.length === 1 ? plotW / 2 : (index * plotW) / (points.length - 1));
  const yAt = (value) => padTop + (1 - (Number(value) - min) / span) * plotH;
  const mapped = points.map((point, index) => ({ ...point, x: xAt(index), y: yAt(point.value) }));
  const linePath = mapped.map((point, index) => `${index ? 'L' : 'M'}${point.x},${point.y}`).join(' ');
  const areaPath = `${linePath} L${mapped[mapped.length - 1].x},${height - padBottom} L${mapped[0].x},${height - padBottom} Z`;
  const barWidth = Math.max(6, Math.min(24, plotW / Math.max(points.length, 1) - 8));
  const bars = mapped.map((point) => ({ x: point.x - barWidth / 2, y: point.y, w: barWidth, h: height - padBottom - point.y }));
  const xLabels = mapped.filter((_, index) => index % Math.max(1, Math.ceil(points.length / 6)) === 0).map((point) => ({ x: point.x, text: point.label }));
  const yTicks = [0, 1, 2, 3].map((index) => {
    const value = min + (span * index) / 3;
    return { y: yAt(value), label: formatChartValue(value, metricKey) };
  });
  return { points: mapped, bars: viz === 'bar' ? bars : [], xLabels, yTicks, linePath, areaPath };
}

function formatChartValue(value, metricKey) {
  const key = String(metricKey || '');
  if (/avg|obp|slg|ops/.test(key)) return Number(value).toFixed(3);
  if (/era|whip/.test(key)) return Number(value).toFixed(2);
  return String(Math.round(Number(value)));
}

function chartColor(index) {
  return `var(--bs-chart-${(index % 6) + 1})`;
}

function prepareEvaluationCard(card) {
  const axes = card.axes || [];
  const ranked = [...axes].sort((left, right) => Number(right.percentile) - Number(left.percentile));
  return {
    ...card,
    radar: axes.length ? buildRadar(axes) : null,
    strongest: ranked[0] || { label: '—', percentile: 0 },
    weakest: ranked[ranked.length - 1] || { label: '—', percentile: 0 }
  };
}

function buildRadar(axes) {
  const geometry = { cx: 170, cy: 132, radius: 84, count: axes.length };
  const points = axes.map((axis, index) => radarPoint(geometry, index, clampPercentile(axis.percentile)));
  return {
    rings: [20, 40, 60, 80, 100].map((level) => radarRing(geometry, level)),
    spokes: radarSpokes(geometry),
    shape: points.map(radarCoords).join(' '),
    dots: points.map((point) => ({ x: point.x.toFixed(1), y: point.y.toFixed(1) })),
    labels: radarLabels(axes, geometry)
  };
}

function clampPercentile(value) {
  return Math.max(0, Math.min(100, Number(value) || 0));
}

function radarCoords(point) {
  return `${point.x.toFixed(1)},${point.y.toFixed(1)}`;
}

function radarAngle(index, count) {
  return -Math.PI / 2 + index * (Math.PI * 2 / count);
}

function radarPoint(geometry, index, value) {
  const angle = radarAngle(index, geometry.count);
  const distance = geometry.radius * (value / 100);
  return {
    x: geometry.cx + distance * Math.cos(angle),
    y: geometry.cy + distance * Math.sin(angle)
  };
}

function radarRing(geometry, level) {
  return Array.from({ length: geometry.count }, (_, index) => (
    radarCoords(radarPoint(geometry, index, level))
  )).join(' ');
}

function radarSpokes(geometry) {
  return Array.from({ length: geometry.count }, (_, index) => {
    const end = radarPoint(geometry, index, 100);
    return {
      x1: geometry.cx,
      y1: geometry.cy,
      x2: end.x.toFixed(1),
      y2: end.y.toFixed(1)
    };
  });
}

function radarLabels(axes, geometry) {
  return axes.map((axis, index) => {
    const angle = radarAngle(index, geometry.count);
    const horizontal = Math.cos(angle);
    const labelRadius = geometry.radius + 26;
    const x = geometry.cx + labelRadius * horizontal;
    const y = geometry.cy + labelRadius * Math.sin(angle);
    let anchor = 'middle';
    if (horizontal >= 0.25) {
      anchor = 'start';
    } else if (horizontal <= -0.25) {
      anchor = 'end';
    }
    return {
      x: x.toFixed(1),
      nameY: (y - 5).toFixed(1),
      valueY: (y + 8).toFixed(1),
      pctY: (y + 20).toFixed(1),
      anchor,
      abbr: axis.abbr,
      value: axis.value,
      percentile: axis.percentile
    };
  });
}

function radarKicker(domain) {
  return { batting: 'BATTING', fielding: 'DEFENSE', pitching: 'PITCHING' }[domain] || 'EVALUATION';
}

function radarFoot(card) {
  return `${(card.axes || []).length} 轴 · ${evaluationComparison.value}`;
}

function ownSampleText(card) {
  return formatOwnSample(card.ownSample, card.sampleUnit) || '暂无';
}

function formatOwnSample(value, unit) {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount <= 0) return '';
  if (unit === 'IP') return `${amount.toFixed(1)} IP`;
  if (unit === 'INN') return `${Number.isInteger(amount) ? amount : amount.toFixed(1)} 局`;
  return `${Math.round(amount)} PA`;
}

function formatReliability(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? amount.toFixed(2) : '—';
}

function confidenceTone(label) {
  if (label === '样本不足') return 'is-insufficient';
  if (label === '参考值') return 'is-reference';
  return 'is-sufficient';
}

function radarNote(card) {
  const note = String(card.note || '').trim();
  const meta = card.meta || {};
  if (meta.woba != null) return `${note || '参考 wOBA'} ${meta.woba}`;
  if (meta.fip != null) {
    const constant = meta.fipConstant != null ? `（C ${meta.fipConstant}）` : '';
    return `${note || '参考 FIP'} ${meta.fip}${constant}`;
  }
  return note;
}

function radarAria(card) {
  const axes = (card.axes || []).map((axis) => `${axis.label}${axis.value}，百分位${axis.percentile}`).join('；');
  return `${card.title}雷达图，${axes}`;
}

function vizLabel(viz) {
  return { kpi: '大数字', line: '折线图', bar: '柱状图', donut: '环形图' }[viz] || viz;
}

function teamCrest(team) {
  return (team?.shortName || team?.name || '?').slice(0, 2);
}

function teamEventLabel(team) {
  const events = team?.events || [];
  return events.length ? events.map((event) => event.name).join(' · ') : '暂无已确认赛事';
}

function standingRank(team) {
  const rank = team?.standing?.rank;
  return rank ? `#${rank}` : '—';
}

function standingRecord(team) {
  const standing = team?.standing;
  if (!standing || standing.win == null) return '暂无战绩';
  return `${standing.win}–${standing.loss ?? 0}`;
}

function opponentCrest(game) {
  return (game?.opponentName || '?').slice(0, 2);
}

function eventTone(name) {
  if (String(name || '').includes('杯')) return 'blue';
  if (String(name || '').includes('挑战')) return 'teal';
  return '';
}

function resultType(result) {
  return result === 'W' ? 'w' : result === 'L' ? 'l' : '';
}

function formatDay(value) {
  const date = dayjs(value);
  return date.isValid() ? date.format('MM.DD') : (value || '--.--');
}

function formatEventDate(value) {
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY.MM.DD') : '';
}

function formatMilestoneDate(value) {
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY.MM.DD') : (value || '日期未记录');
}

function milestoneScrollMetrics(el) {
  const clientHeight = el.clientHeight;
  const scrollHeight = el.scrollHeight;
  return {
    clientHeight,
    scrollHeight,
    trackHeight: Math.max(1, clientHeight - MILESTONE_TRACK_INSET * 2),
    maxScroll: Math.max(1, scrollHeight - clientHeight)
  };
}

function updateMilestoneScrollbar() {
  const el = milestoneListEl.value;
  const canScroll = el ? el.scrollHeight > el.clientHeight + 1 : false;
  milestoneScroll.canScroll = canScroll;
  if (!canScroll) {
    milestoneScroll.thumbHeight = 0;
    milestoneScroll.thumbTop = 0;
    return;
  }
  const { clientHeight, scrollHeight, trackHeight, maxScroll } = milestoneScrollMetrics(el);
  const thumbHeight = Math.min(
    trackHeight,
    Math.max(28, Math.round(trackHeight * clientHeight / scrollHeight))
  );
  const maxThumb = Math.max(0, trackHeight - thumbHeight);
  milestoneScroll.thumbHeight = thumbHeight;
  milestoneScroll.thumbTop = Math.round((el.scrollTop / maxScroll) * maxThumb);
}

function observeMilestoneList() {
  milestoneResizeObserver?.disconnect();
  milestoneResizeObserver = null;
  const el = milestoneListEl.value;
  if (!el || typeof ResizeObserver === 'undefined') return;
  milestoneResizeObserver = new ResizeObserver(updateMilestoneScrollbar);
  milestoneResizeObserver.observe(el);
}

function startMilestoneScrollDrag(event) {
  const el = milestoneListEl.value;
  if (!el || !milestoneScroll.canScroll || milestoneScrollDrag) return;
  const { trackHeight, maxScroll } = milestoneScrollMetrics(el);
  milestoneScrollDrag = {
    pointerId: event.pointerId,
    startY: event.clientY,
    startScrollTop: el.scrollTop,
    maxScroll,
    maxThumb: Math.max(1, trackHeight - milestoneScroll.thumbHeight)
  };
  try {
    event.currentTarget.setPointerCapture?.(event.pointerId);
  } catch {
    // 旧浏览器不支持指针捕获时仍可用窗口监听拖动
  }
  window.addEventListener('pointermove', onMilestoneScrollDrag, true);
  window.addEventListener('pointerup', stopMilestoneScrollDrag, true);
  window.addEventListener('pointercancel', stopMilestoneScrollDrag, true);
  window.addEventListener('lostpointercapture', onMilestoneScrollLostCapture, true);
}

function onMilestoneScrollDrag(event) {
  if (!milestoneScrollDrag || event.pointerId !== milestoneScrollDrag.pointerId) return;
  if (event.buttons === 0) {
    stopMilestoneScrollDrag();
    return;
  }
  event.preventDefault();
  const el = milestoneListEl.value;
  if (!el) return;
  const delta = event.clientY - milestoneScrollDrag.startY;
  const ratio = milestoneScrollDrag.maxScroll / milestoneScrollDrag.maxThumb;
  el.scrollTop = Math.min(
    milestoneScrollDrag.maxScroll,
    Math.max(0, milestoneScrollDrag.startScrollTop + delta * ratio)
  );
  updateMilestoneScrollbar();
}

function onMilestoneScrollLostCapture(event) {
  if (!milestoneScrollDrag || event.pointerId !== milestoneScrollDrag.pointerId) return;
  stopMilestoneScrollDrag();
}

function stopMilestoneScrollDrag(event) {
  if (event && event.pointerId !== undefined
      && milestoneScrollDrag && event.pointerId !== milestoneScrollDrag.pointerId) {
    return;
  }
  milestoneScrollDrag = null;
  window.removeEventListener('pointermove', onMilestoneScrollDrag, true);
  window.removeEventListener('pointerup', stopMilestoneScrollDrag, true);
  window.removeEventListener('pointercancel', stopMilestoneScrollDrag, true);
  window.removeEventListener('lostpointercapture', onMilestoneScrollLostCapture, true);
}

function milestoneDomainLabel(domain) {
  return domain === 'pitching' ? '投球' : '打击';
}

function weekday(value) {
  const date = dayjs(value);
  return date.isValid() ? ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.day()] : '';
}

function countdown(value) {
  const target = dayjs(value).startOf('day');
  if (!target.isValid()) return '';
  const days = target.diff(dayjs().startOf('day'), 'day');
  if (days <= 0) return '今天';
  if (days === 1) return '明天';
  return `还有 ${days} 天`;
}

function recentLine(game) {
  const b = game?.batting || {};
  const p = game?.pitching || {};
  const hasBattingLine = Number(b.ab) > 0 || Number(b.pa) > 0;
  if (hasBattingLine) {
    const parts = [`${Number(b.ab) || 0}打数 ${Number(b.h) || 0}安打`];
    if (b.rbi) parts.push(`${b.rbi}打点`);
    if (b.sb) parts.push(`${b.sb}盗垒`);
    return parts.join(' · ');
  }
  if (Number(p.ip) > 0) {
    return `${p.ip}局 · ${p.pitchSo ?? 0}三振 · ${p.er ?? 0}自责分`;
  }
  return '本场暂无个人数据';
}

function openSettings() {
  if (!data.value || layoutSaving.value || layoutEditing.value) return;
  resetDraft();
  settingsOpen.value = true;
}

function resetDraft() {
  const config = data.value?.config || {};
  draft.performance.metricKey = config.performance?.metricKey || 'batting.avg';
  draft.performance.viz = config.performance?.viz || 'line';
  const ranks = config.ranks || [];
  draft.ranks = DEFAULT_RANK_KEYS.map((metricKey, index) => ({
    metricKey: ranks[index]?.metricKey || metricKey
  }));
  const modules = config.modules?.length ? config.modules : defaultModules();
  draft.modules = modules.map(resolveModule);
  const selectedMilestones = config.milestones?.metricKeys;
  draft.milestones.metricKeys = selectedMilestones?.length
    ? [...selectedMilestones]
    : milestoneCatalog.value.filter((metric) => metric.defaultSelected).map((metric) => metric.key);
}

function moveDraftModule(index, direction) {
  const target = index + direction;
  if (target < 0 || target >= draft.modules.length) return;
  const [module] = draft.modules.splice(index, 1);
  draft.modules.splice(target, 0, module);
}

function moduleSizeLabel(module) {
  return `${module.width} × ${module.height} px`;
}

function initModuleSortable() {
  destroyModuleSortable();
  if (!moduleListEl.value) return;
  moduleSortable = Sortable.create(moduleListEl.value, {
    handle: '.module-drag',
    animation: 150,
    onEnd: ({ oldIndex, newIndex }) => {
      if (oldIndex == null || newIndex == null || oldIndex === newIndex) return;
      const [module] = draft.modules.splice(oldIndex, 1);
      draft.modules.splice(newIndex, 0, module);
    }
  });
}

function destroyModuleSortable() {
  if (moduleSortable) {
    moduleSortable.destroy();
    moduleSortable = null;
  }
}

function onPerformanceMetricChange() {
  if (!performanceVizOptions.value.includes(draft.performance.viz)) {
    draft.performance.viz = performanceVizOptions.value[0] || 'line';
  }
}

function rankOptionDisabled(metric, index) {
  if (!metric.available) return true;
  return draft.ranks.some((slot, slotIndex) => slotIndex !== index && slot.metricKey === metric.key);
}

async function saveSettings() {
  const keys = draft.ranks.map((slot) => slot.metricKey);
  if (new Set(keys).size !== 4 || keys.some((key) => !key)) {
    ElMessage.warning('四个排名位需要选择不同指标');
    return;
  }
  if (!draft.milestones.metricKeys.length) {
    ElMessage.warning('生涯里程碑至少选择 1 个指标');
    return;
  }
  saving.value = true;
  try {
    await clubhouseApi.saveConfig(buildConfigPayload(draft.modules, {
      milestones: { metricKeys: [...draft.milestones.metricKeys] },
      performance: { ...draft.performance },
      ranks: draft.ranks.map((slot) => ({ metricKey: slot.metricKey }))
    }));
    settingsOpen.value = false;
    ElMessage.success('数据设置已保存');
    await load();
  } catch (error) {
    ElMessage.error(error?.message || '保存失败');
  } finally {
    saving.value = false;
  }
}

async function resetSettings() {
  try {
    await ElMessageBox.confirm('恢复默认数据设置？当前选择将被覆盖。', '恢复默认', {
      confirmButtonText: '恢复',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  saving.value = true;
  try {
    await clubhouseApi.resetConfig();
    settingsOpen.value = false;
    ElMessage.success('已恢复默认');
    await load();
  } catch (error) {
    ElMessage.error(error?.message || '恢复失败');
  } finally {
    saving.value = false;
  }
}

/** 当前球队（或全部球队）在当前运动类型下的赛事 id，用于切换后校验赛事是否仍然可选。 */
function eventIdsForScope(teamIdValue) {
  const source = teamIdValue
    ? teams.value.filter((team) => team.id === teamIdValue)
    : availableTeams.value;
  const ids = new Set();
  source.forEach((team) => (team.events || []).forEach((event) => {
    if (eventGameMode(event) === gameMode.value) ids.add(event.id);
  }));
  return ids;
}

/** 当前运动类型没有任何赛事且只存在另一种类型时，返回应切换到的类型；否则返回 null。 */
function fallbackGameMode() {
  const modes = availableGameModes.value;
  if (!modes.length || modes.includes(gameMode.value) || modes.length !== 1) return null;
  return modes[0];
}

function onGameModeChange() {
  if (!data.value) return;
  if (layoutEditing.value) finishLayoutEditing();
  eventScopeResetApplied = false;
  if (teamId.value && !availableTeams.value.some((team) => team.id === teamId.value)) {
    teamId.value = SCOPE_ALL;
  }
  if (eventId.value && !eventIdsForScope(teamId.value).has(eventId.value)) {
    eventId.value = SCOPE_ALL;
  }
  load();
}

function onTeamChange() {
  if (!data.value) return;
  if (layoutEditing.value) finishLayoutEditing();
  eventScopeResetApplied = false;
  if (eventId.value && !eventIdsForScope(teamId.value).has(eventId.value)) {
    eventId.value = SCOPE_ALL;
  }
  load();
}

function onEventChange() {
  if (!data.value) return;
  if (layoutEditing.value) finishLayoutEditing();
  eventScopeResetApplied = false;
  load();
}

function switchTeam(team) {
  if (layoutEditing.value) return;
  teamId.value = team.id;
  onTeamChange();
}

function scopeEventLabel(event) {
  const last = dayjs(event?.lastGameday);
  return `${event?.name || '赛事'} · ${event?.lastGameday && last.isValid() ? last.format('MM.DD') : '暂无比赛'}`;
}

async function load() {
  const seq = ++requestSeq;
  loading.value = true;
  loadError.value = '';
  milestonePayload.value = null;
  milestonesError.value = '';
  milestonesLoading.value = false;
  try {
    const result = await clubhouseApi.dashboard({
      teamId: teamId.value || null,
      eventId: eventId.value || null,
      gameMode: gameMode.value
    });
    if (seq !== requestSeq) return;
    if (!result) {
      await goUnclaimed();
      return;
    }
    data.value = result;
    teamId.value = result.scope?.teamId ?? SCOPE_ALL;
    eventId.value = result.scope?.eventId ?? SCOPE_ALL;
    gameMode.value = result.scope?.gameMode || gameMode.value;
    // 仅垒球（或仅棒球）球员：当前类型没有任何赛事且只存在另一种类型时，自动切换一次并重载。
    if (!autoGameModeAdjusted) {
      const fallbackMode = fallbackGameMode();
      if (fallbackMode) {
        autoGameModeAdjusted = true;
        gameMode.value = fallbackMode;
        teamId.value = SCOPE_ALL;
        eventId.value = SCOPE_ALL;
        await load();
        return;
      }
    }
    // 赛事以后端返回的 events[] 为准：已从下拉消失的赛事回退「生涯」，只重载一次。
    const responseEventIds = new Set((result.events || []).map((event) => String(event.id)));
    if (eventId.value && !responseEventIds.has(String(eventId.value))) {
      eventId.value = SCOPE_ALL;
      if (!eventScopeResetApplied) {
        eventScopeResetApplied = true;
        await load();
        return;
      }
    }
    resetDraft();
    void loadMilestones(seq);
  } catch (error) {
    if (seq !== requestSeq) return;
    if (isAuthSessionError(error)) {
      await redirectToLogin();
      return;
    }
    if (error?.apiCode === 404 || String(error?.message || '').includes('未关联')) {
      await goUnclaimed();
      return;
    }
    loadError.value = error?.message || '加载失败';
  } finally {
    if (seq === requestSeq) loading.value = false;
  }
}

async function loadMilestones(seq) {
  if (!renderableModules.value.some((module) => module.key === 'milestones')) {
    milestonePayload.value = null;
    milestonesLoading.value = false;
    return;
  }
  milestonesLoading.value = true;
  milestonesError.value = '';
  try {
    const result = await clubhouseApi.milestones();
    if (seq !== requestSeq) return;
    milestonePayload.value = result || {};
  } catch (error) {
    if (seq !== requestSeq) return;
    if (isAuthSessionError(error)) {
      await redirectToLogin();
      return;
    }
    milestonesError.value = error?.message || '里程碑加载失败';
  } finally {
    if (seq === requestSeq) milestonesLoading.value = false;
  }
}

async function redirectToLogin() {
  await router.replace({
    name: 'PortalAccountLogin',
    params: { tenantCode: tenantCode.value },
    query: { redirect: router.currentRoute.value.fullPath }
  });
}

async function goUnclaimed() {
  ElMessage.warning('当前账号未关联球员档案，已返回门户');
  await router.replace({ name: 'PortalHome', params: { tenantCode: tenantCode.value } });
}

function goPortal() {
  router.push({ name: 'PortalHome', params: { tenantCode: tenantCode.value } });
}
function goAccount() {
  router.push({ name: 'PortalAccountProfile', params: { tenantCode: tenantCode.value } });
}
function goPublicProfile() {
  if (player.value.id) router.push({ name: 'PortalPlayerDetail', params: { tenantCode: tenantCode.value, id: player.value.id } });
}
function goTeam() {
  const team = scopeTeam.value || availableTeams.value[0];
  if (team?.id) router.push({ name: 'PortalTeamDetail', params: { tenantCode: tenantCode.value, id: team.id } });
}
function goEvents() {
  router.push({ name: 'PortalEvents', params: { tenantCode: tenantCode.value } });
}
function goGame(game) {
  if (game?.eventId && game?.gameId) {
    router.push({ name: 'PortalGameDetail', params: { tenantCode: tenantCode.value, eventId: game.eventId, gameId: game.gameId } });
  }
}

onMounted(() => {
  document.documentElement.classList.add('clubhouse-route');
  freeBoardQuery.addEventListener('change', syncBoardMode);
  compactBoardQuery.addEventListener('change', syncBoardMode);
  window.addEventListener('resize', onViewportResize);
  document.addEventListener('keydown', onLayoutKeydown);
  load();
});
onBeforeUnmount(() => {
  destroyModuleSortable();
  flushKeyboardLayout();
  clearLayoutPointer();
  selectionObserver?.disconnect();
  milestoneResizeObserver?.disconnect();
  if (milestoneScrollDrag) stopMilestoneScrollDrag();
  if (selectionFrameRaf) cancelAnimationFrame(selectionFrameRaf);
  if (layoutStatusTimer) window.clearTimeout(layoutStatusTimer);
  freeBoardQuery.removeEventListener('change', syncBoardMode);
  compactBoardQuery.removeEventListener('change', syncBoardMode);
  window.removeEventListener('resize', onViewportResize);
  document.removeEventListener('keydown', onLayoutKeydown);
  document.documentElement.classList.remove('clubhouse-route');
});
</script>

<style scoped>
/* 页面主体沿用 V3 设计令牌；宽屏自由画布，窄屏紧凑/单列。 */
.clubhouse-page { position: relative; max-width: 1280px; margin: 0 auto; padding: 24px 32px 56px; color: var(--bs-text); background: var(--bs-canvas); font-family: var(--bs-font-sans); }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.breadcrumb { font-size: 10px; color: var(--bs-text-faint); }
.breadcrumb b { color: var(--bs-text-muted); font-weight: 500; }
.page-head h1 { margin: 6px 0 0; font-size: 27px; line-height: 1.15; font-weight: 800; color: var(--bs-ink); }
.page-sub { margin: 6px 0 0; font-size: 11px; color: var(--bs-text-muted); }
.page-actions { display: flex; gap: 10px; }
.layout-edit-banner { position: sticky; top: 72px; z-index: 60; display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 20px; padding: 10px 14px; border: 1px solid var(--bs-accent); border-radius: 10px; background: color-mix(in srgb, var(--bs-surface) 92%, var(--bs-accent-soft)); box-shadow: var(--bs-shadow-sm), 0 10px 30px rgba(7,26,51,.08); color: var(--bs-text); backdrop-filter: blur(10px); }
.layout-edit-banner .edit-copy { display: flex; align-items: center; gap: 9px; min-width: 0; font-size: 11px; }
.layout-edit-banner .edit-copy > b { color: var(--bs-ink); font-weight: 800; white-space: nowrap; }
.layout-edit-banner .edit-copy > span { color: var(--bs-text-muted); }
.layout-edit-banner .edit-selected { display: inline-flex; align-items: center; gap: 7px; min-width: 0; }
.layout-edit-banner .edit-selected b { color: var(--bs-ink); font-weight: 800; }
.layout-edit-banner .edit-selected em { padding: 2px 6px; border: 1px solid var(--bs-hairline); border-radius: 5px; background: var(--bs-surface-soft); color: var(--bs-accent-ink); font-family: var(--bs-font-num); font-size: 10px; font-style: normal; font-weight: 700; white-space: nowrap; }
.layout-edit-banner .edit-content-limit { padding: 2px 6px; border: 1px dashed var(--bs-accent); border-radius: 5px; color: var(--bs-accent-ink); font-size: 10px; font-style: normal; font-weight: 700; white-space: nowrap; }
.layout-edit-banner .edit-actions { display: flex; align-items: center; gap: 10px; flex: 0 0 auto; }
.layout-edit-banner .edit-status { font-size: 10px; color: var(--bs-text-muted); }
.layout-edit-banner .edit-status.is-saved { color: var(--bs-success); }
.layout-edit-banner .edit-swap { color: var(--bs-accent-ink); font-weight: 800; }
.layout-edit-banner .edit-reset { height: 30px; padding: 0 10px; border: 1px solid var(--bs-hairline); border-radius: 7px; background: var(--bs-surface); color: var(--bs-accent-ink); font: inherit; font-size: 10px; font-weight: 700; cursor: pointer; }
.layout-edit-banner .edit-reset:hover { border-color: var(--bs-accent); background: var(--bs-accent-soft); }
.layout-edit-banner .edit-reset:disabled { opacity: .4; cursor: not-allowed; }
.layout-edit-banner .edit-done { height: 30px; padding: 0 13px; border: 1px solid var(--bs-accent); border-radius: 7px; color: var(--bs-on-accent); background: var(--bs-accent); font: inherit; font-size: 10px; font-weight: 700; cursor: pointer; }
.layout-edit-banner .edit-done:hover { background: var(--bs-accent-hover); }
.layout-edit-banner button:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 2px; }
.clubhouse-page.is-resizing, .clubhouse-page.is-reordering { user-select: none; }
.clubhouse-page.is-reordering { cursor: grabbing; }
.scope-bar { display: flex; align-items: center; gap: 9px; margin-bottom: 16px; padding: 9px 13px; background: var(--bs-surface); border: 1px solid var(--bs-hairline-soft); border-radius: 12px; box-shadow: var(--bs-shadow-sm); }
.scope-title { font-size: 10px; font-weight: 700; letter-spacing: .1em; color: var(--bs-accent-ink); white-space: nowrap; }
.scope-select { width: 148px; max-width: 100%; }
.scope-select :deep(.el-select__wrapper) { min-height: 30px; border-radius: 7px; box-shadow: 0 0 0 1px var(--bs-hairline) inset; background: var(--bs-surface-soft); }
.scope-hint { font-size: 10px; color: var(--bs-text-faint); margin-left: 4px; }
.scope-right { margin-left: auto; font-size: 10px; color: var(--bs-text-muted); white-space: nowrap; }
.scope-right b { color: var(--bs-ink); }
.event-context { display: flex; align-items: center; flex-wrap: wrap; gap: 9px; margin: -2px 0 12px; padding: 8px 10px; border: 1px solid var(--bs-hairline-soft); border-radius: 8px; background: var(--bs-surface-soft); color: var(--bs-text-muted); font-size: 10px; }
.event-context .ec-label { font-weight: 700; letter-spacing: .08em; color: var(--bs-accent-ink); }
.event-context b { font-size: 12px; color: var(--bs-ink); }
.event-context .ec-status { padding: 2px 6px; border-radius: 99px; background: var(--bs-accent-soft); color: var(--bs-accent-ink); font-weight: 700; }
.event-context .ec-status.is-ongoing { background: rgba(31,122,90,.1); color: var(--bs-success); }
.event-context .ec-status.is-ended { background: var(--bs-surface); color: var(--bs-text-muted); }
.event-context .ec-dates { font-family: var(--bs-font-num); }
.event-context .ec-note { padding: 2px 6px; border: 1px dashed var(--bs-hairline); border-radius: 99px; color: var(--bs-text-muted); }

.hero { position: relative; isolation: isolate; overflow: hidden; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(420px, .85fr); gap: 24px; min-height: 430px; padding: 34px 40px; border-radius: 24px; color: #fff; background: radial-gradient(90% 100% at 90% -10%, rgba(201,164,91,.25), transparent 55%), radial-gradient(80% 100% at 0% 110%, rgba(18,54,95,.78), transparent 64%), linear-gradient(145deg, var(--bs-hero-start), var(--bs-hero-mid) 58%, var(--bs-hero-end)); border: 1px solid var(--bs-hairline); box-shadow: var(--bs-shadow-hero); }
.hero::before { content: ""; position: absolute; inset: 0; z-index: -1; opacity: .45; background: repeating-linear-gradient(115deg, transparent 0 42px, rgba(255,255,255,.018) 42px 43px), radial-gradient(circle at 80% 14%, rgba(255,255,255,.14) 0 1px, transparent 2px); background-size: auto, 28px 28px; }
.hero::after { content: ""; position: absolute; z-index: -1; right: -110px; top: -90px; width: 420px; height: 420px; border-radius: 50%; border: 1px solid rgba(201,164,91,.17); box-shadow: 0 0 0 48px rgba(201,164,91,.028), 0 0 0 108px rgba(201,164,91,.016); }
.hero-27 { position: absolute; z-index: -1; right: 2%; top: -34px; font-family: var(--bs-font-num); font-size: 400px; line-height: .8; font-weight: 800; letter-spacing: -.09em; color: transparent; -webkit-text-stroke: 1px rgba(255,255,255,.09); pointer-events: none; user-select: none; }
.hero-left { display: flex; flex-direction: column; justify-content: space-between; min-width: 0; }
.hero-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.hero-kicker { display: flex; align-items: center; gap: 9px; font-size: 10px; font-weight: 700; letter-spacing: .26em; color: var(--bs-accent-ink); }
.hero-kicker::before { content: ""; width: 26px; height: 1px; background: var(--bs-accent); }
.claim { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; color: var(--bs-hero-muted); padding: 5px 9px; border-radius: 999px; border: 1px solid var(--bs-hero-card-border); background: var(--bs-hero-card); }
.claim i { width: 4px; height: 4px; border-radius: 50%; background: var(--bs-accent); box-shadow: 0 0 0 3px rgba(201,164,91,.15); }
.hero h1 { margin: 16px 0 0; font-family: var(--bs-font-serif); font-size: 66px; line-height: 1; font-weight: 700; letter-spacing: .005em; text-shadow: 0 6px 30px rgba(0,0,0,.3); }
.hero-en { margin: 10px 0 0; font-family: var(--bs-font-num); font-size: 10px; font-weight: 700; letter-spacing: .34em; color: var(--bs-hero-muted); }
.hero-intro { max-width: 470px; margin: 18px 0 0; font-size: 12px; line-height: 1.9; color: var(--bs-hero-muted); }
.hero-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 16px; }
.tag { font-size: 10px; padding: 6px 9px; color: rgba(255,255,255,.78); background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.08); border-radius: 999px; }
.hero-team { display: flex; align-items: flex-start; gap: 10px; margin-top: 19px; padding-top: 14px; border-top: 1px solid var(--bs-hero-line); max-width: 470px; min-width: 0; }
.hero-actions { display: flex; gap: 9px; margin-top: 16px; }
.hero-btn { height: 36px; padding: 0 16px; border-radius: 9px; display: inline-flex; align-items: center; justify-content: center; font-family: var(--bs-font-sans); font-size: 11px; font-weight: 700; cursor: pointer; transition: background 160ms ease-out, border-color 160ms ease-out; }
.hero-btn.primary { color: var(--bs-on-accent); border: 1px solid var(--bs-accent); background: linear-gradient(135deg, var(--bs-accent-hover), var(--bs-accent)); }
.hero-btn.primary:hover { background: var(--bs-accent-hover); }
.hero-btn.ghost { color: #fff; border: 1px solid rgba(255,255,255,.22); background: rgba(255,255,255,.06); }
.hero-btn.ghost:hover { border-color: rgba(255,255,255,.4); }
.hero-btn:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 2px; }
.crest { width: 34px; height: 34px; border-radius: 9px; flex: 0 0 auto; display: grid; place-items: center; font-family: var(--bs-font-num); font-size: 11px; font-weight: 800; color: #071a33; background: linear-gradient(145deg, var(--bs-accent-soft), var(--bs-accent)); }
.crest.dark { color: #fff; background: linear-gradient(145deg, #173e62, #0a1b2e); border: 1px solid rgba(201,164,91,.5); }
.team-copy { min-width: 0; }
.team-copy b { display: block; font-size: 12px; line-height: 1.5; overflow-wrap: anywhere; }
.team-copy span { display: block; margin-top: 2px; font-size: 10px; color: var(--bs-hero-muted); }

.card-stage { position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; min-height: 360px; }
.card-slot { position: relative; z-index: 3; display: block; padding: 0; border: 0; background: none; cursor: zoom-in; transition: transform 220ms ease-out; }
.card-slot:hover { transform: translateY(-5px); }
.card-slot:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 8px; border-radius: 18px; }
.card-stack { position: absolute; inset: 0; pointer-events: none; transform-origin: 50% 100%; }
.card-stack.one { transform: rotate(4deg); filter: brightness(.9); }
.card-stack.two { transform: rotate(16deg); filter: brightness(.78); }
.card-slot .pcard { transform: rotate(var(--hero-tilt, 0deg)); transform-origin: 50% 100%; }
.pcard-zoom { position: fixed; inset: 0; z-index: 3000; }
.pcard-zoom-backdrop { position: absolute; inset: 0; background: rgba(4, 8, 14, .62); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); opacity: 0; transition: opacity 320ms ease-out; }
.pcard-zoom-backdrop.on { opacity: 1; }
.pcard-zoom-card { position: absolute; transition: transform 400ms cubic-bezier(.22, .9, .26, 1); will-change: transform; }
.pcard-zoom-close { position: absolute; right: max(16px, env(safe-area-inset-right)); top: max(16px, env(safe-area-inset-top)); width: 44px; height: 44px; border-radius: 50%; border: 1px solid rgba(255, 255, 255, .28); background: rgba(10, 16, 26, .72); color: #fff; font-size: 22px; line-height: 1; cursor: pointer; z-index: 2; }
.pcard-zoom-close:hover { border-color: rgba(255, 255, 255, .5); }
.pcard-zoom-close:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 2px; }

.identity-band { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 18px; border-top: 1px solid var(--bs-hairline); border-bottom: 1px solid var(--bs-hairline); padding: 13px 0; }
.id-item { padding: 0 24px; border-right: 1px solid var(--bs-hairline); }
.id-item:first-child { padding-left: 0; }
.id-item:last-child { border-right: 0; padding-right: 0; }
.id-item span { display: block; font-size: 10px; color: var(--bs-text-faint); letter-spacing: .05em; }
.id-item b { display: block; margin-top: 4px; font-size: 11px; font-weight: 600; color: var(--bs-text); }
.id-item b em { font-style: normal; font-family: var(--bs-font-num); font-size: 14px; color: var(--bs-ink); }

.rank-band { margin-top: 22px; background: var(--bs-surface); border: 1px solid var(--bs-hairline-soft); border-radius: 18px; box-shadow: var(--bs-shadow-sm); padding: 18px 22px 17px; }
.rank-band-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; }
.rank-band-head h2 { margin: 5px 0 0; font-family: var(--bs-font-serif); font-size: 23px; line-height: 1.2; font-weight: 700; color: var(--bs-ink); }
.rank-band-head h2 small { margin-left: 5px; font-family: var(--bs-font-sans); font-size: 10px; font-weight: 400; color: var(--bs-text-faint); }
.rank-band-head span { font-size: 10px; color: var(--bs-text-faint); }
.rank-scope a { color: var(--bs-accent-ink); font-weight: 700; margin-left: 5px; }
.rank-note { display: flex; align-items: center; gap: 6px; margin: 6px 0 0; font-size: 10px; line-height: 1.5; color: var(--bs-text-muted); }
.rank-note b { color: var(--bs-accent-ink); font-weight: 700; }
.metric-info { display: inline-grid; place-items: center; width: 24px; height: 24px; margin-left: 6px; padding: 0; border: 1px solid var(--bs-hairline); border-radius: 50%; background: var(--bs-surface); color: var(--bs-text-muted); font: inherit; font-size: 10px; font-weight: 800; vertical-align: middle; cursor: help; }
.metric-info:hover { border-color: var(--bs-accent); color: var(--bs-accent-ink); }
.metric-info:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 2px; }
.rank-band-grid { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 15px; }
.rank-feature { padding: 0 24px; border-right: 1px solid var(--bs-hairline-soft); }
.rank-feature:first-child { padding-left: 0; }
.rank-feature:last-child { border-right: 0; padding-right: 0; }
.rank-feature .line { display: flex; align-items: baseline; gap: 8px; }
.rank-feature .no { font-family: var(--bs-font-num); font-size: 36px; line-height: .9; font-weight: 700; letter-spacing: -.04em; color: var(--bs-ink); }
.rank-feature .no small { font-size: 11px; color: var(--bs-text-faint); font-weight: 500; letter-spacing: 0; }
.rank-feature .value { margin-left: auto; font-family: var(--bs-font-num); font-size: 17px; font-weight: 700; color: var(--bs-ink); }
.rank-feature .metric { margin-top: 8px; font-size: 10px; font-weight: 700; color: var(--bs-text); }
.rank-feature .metric span { display: block; margin-top: 2px; font-size: 10px; font-weight: 400; color: var(--bs-text-faint); }
.rank-feature .track { height: 3px; border-radius: 99px; background: var(--bs-hairline-soft); margin-top: 10px; overflow: hidden; }
.rank-feature .track i { display: block; height: 100%; border-radius: 99px; background: linear-gradient(90deg, var(--bs-accent), var(--bs-accent-hover)); }
.rank-feature:nth-child(1) .track i { background: linear-gradient(90deg, #aeb8c4, #d8dee6); }
.rank-feature:nth-child(2) .track i { background: linear-gradient(90deg, #b87a4b, #e0b28c); }
.rank-feature.neutral .track i { background: linear-gradient(90deg, #7c8a9c, #aeb8c4); }

.evaluation-panel { margin-top: 20px; padding: 19px 21px 16px; }
.eval-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.eval-head h3 { margin: 5px 0 0; font-family: var(--bs-font-serif); font-size: 22px; line-height: 1.2; color: var(--bs-ink); }
.eval-head h3 small { margin-left: 5px; font-family: var(--bs-font-sans); font-size: 11px; font-weight: 400; color: var(--bs-text-muted); }
.eval-meta { display: flex; align-items: center; gap: 16px; padding-bottom: 2px; font-size: 11px; color: var(--bs-text-muted); }
.eval-meta b { color: var(--bs-ink); font-family: var(--bs-font-num); font-size: 14px; margin-right: 3px; }
.eval-overall { display: flex; align-items: center; gap: 14px; padding: 7px 13px; border: 1px solid var(--bs-hairline); border-radius: 12px; background: linear-gradient(180deg, var(--bs-surface), var(--bs-surface-soft)); box-shadow: var(--bs-shadow-sm); }
.eo-score { display: flex; align-items: baseline; gap: 5px; }
.eo-score b { font-family: var(--bs-font-num); font-size: 38px; line-height: .9; font-weight: 700; letter-spacing: -.04em; color: var(--bs-ink); }
.eo-score .eo-scale { font-family: var(--bs-font-num); font-size: 10px; color: var(--bs-text-faint); }
.eo-score em { align-self: center; padding: 3px 6px; font-style: normal; font-family: var(--bs-font-num); font-size: 11px; font-weight: 800; color: var(--bs-ink); border: 1px solid var(--bs-hairline); border-radius: 5px; background: var(--bs-surface-soft); }
.eo-side { display: flex; flex-direction: column; gap: 3px; font-size: 10px; line-height: 1.3; color: var(--bs-text-muted); }
.eo-tags { display: flex; align-items: center; gap: 6px; }
.eo-role { font-size: 10px; font-weight: 700; color: var(--bs-text); }
.eo-confidence, .radar-confidence { padding: 1px 6px; font-style: normal; font-size: 10px; font-weight: 600; line-height: 1.5; color: var(--bs-text-muted); border: 1px solid var(--bs-hairline); border-radius: 99px; background: var(--bs-surface-soft); }
.eo-confidence.is-reference, .radar-confidence.is-reference { color: var(--bs-accent-ink); border-color: var(--bs-accent-soft); background: var(--bs-accent-soft); }
.eo-confidence.is-insufficient, .radar-confidence.is-insufficient { color: var(--bs-text-faint); }
.eo-confidence.is-sufficient, .radar-confidence.is-sufficient { color: var(--bs-text); border-color: var(--bs-hairline); }
.eo-sample { font-family: var(--bs-font-num); letter-spacing: .02em; color: var(--bs-text); }
.eo-reliability { font-size: 9px; letter-spacing: .04em; color: var(--bs-text-faint); }
.radar-grid { display: grid; gap: 14px; margin-top: 14px; }
.radar-grid.two-up { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.radar-grid.three-up { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.radar-card { --radar-color: var(--bs-chart-line); position: relative; overflow: hidden; display: flex; flex-direction: column; min-width: 0; padding: 16px 16px 13px; border: 1px solid var(--bs-hairline-soft); border-radius: 14px; background: linear-gradient(180deg, var(--bs-surface), var(--bs-surface-soft)); box-shadow: var(--bs-shadow-sm); }
.radar-card.batting { --radar-color: var(--bs-chart-6); }
.radar-card.fielding { --radar-color: var(--bs-accent); }
.radar-card.pitching { --radar-color: var(--bs-chart-3); }
.radar-card::before { content: ""; position: absolute; inset: 0 auto 0 0; width: 3px; background: linear-gradient(180deg, var(--radar-color), transparent); opacity: .85; }
.radar-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.radar-kicker { display: block; font-size: 10px; font-weight: 700; letter-spacing: .16em; color: var(--bs-text-muted); }
.radar-card h4 { margin: 5px 0 0; font-family: var(--bs-font-serif); font-size: 19px; line-height: 1.15; color: var(--bs-ink); }
.radar-card-sub { margin: 5px 0 0; font-family: var(--bs-font-num); font-size: 10px; letter-spacing: .06em; color: var(--bs-text-muted); }
.radar-score { display: grid; grid-template-columns: auto auto; align-items: center; column-gap: 7px; text-align: right; }
.radar-score b { font-family: var(--bs-font-num); font-size: 35px; line-height: .9; font-weight: 700; letter-spacing: -.04em; color: var(--bs-ink); }
.radar-score em { align-self: start; padding: 3px 5px; font-style: normal; font-family: var(--bs-font-num); font-size: 10px; font-weight: 800; color: var(--bs-ink); border: 1px solid var(--bs-hairline); border-radius: 4px; background: var(--bs-surface-soft); }
.radar-score span { grid-column: 1 / -1; margin-top: 4px; font-size: 10px; letter-spacing: .08em; color: var(--bs-text-muted); }
.radar-score.is-empty b { color: var(--bs-text-faint); }
.radar-chart { width: 100%; max-width: 340px; margin: 7px auto 0; overflow: visible; }
.radar-ring { fill: none; stroke: var(--bs-hairline); stroke-width: 1; }
.radar-spoke { stroke: var(--bs-hairline); stroke-width: 1; }
.radar-area { fill: var(--radar-color); fill-opacity: .15; }
.radar-line { fill: none; stroke: var(--radar-color); stroke-width: 2; stroke-linejoin: round; stroke-linecap: round; }
.radar-dot { fill: var(--bs-surface); stroke: var(--radar-color); stroke-width: 2; }
.radar-axis-name { fill: var(--bs-text); font-family: var(--bs-font-sans); font-size: 12px; font-weight: 700; }
.radar-axis-value { fill: var(--bs-text-muted); font-family: var(--bs-font-num); font-size: 11px; }
.radar-axis-pct { fill: var(--bs-text-muted); font-family: var(--bs-font-num); font-size: 10px; }
.radar-metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; margin-top: 2px; padding-top: 10px; border-top: 1px solid var(--bs-hairline-soft); }
.radar-metric { display: grid; grid-template-columns: 6px 1fr auto 22px; align-items: center; gap: 5px; padding: 6px 0; border-bottom: 1px solid var(--bs-hairline-soft); }
.radar-metric:nth-last-child(-n+2) { border-bottom: 0; }
.radar-metric i { width: 5px; height: 5px; border-radius: 50%; background: var(--radar-color); }
.radar-metric b { font-size: 10px; font-weight: 600; color: var(--bs-text); white-space: nowrap; }
.radar-metric b small { margin-left: 4px; font-family: var(--bs-font-num); font-size: 10px; color: var(--bs-text-muted); letter-spacing: .03em; }
.radar-metric em { font-style: normal; font-family: var(--bs-font-num); font-size: 12px; font-weight: 700; color: var(--bs-ink); }
.radar-metric > span { font-family: var(--bs-font-num); font-size: 10px; font-weight: 600; text-align: right; color: var(--bs-text-muted); }
.radar-highlights { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 9px; padding: 8px 9px; border-radius: 7px; background: var(--bs-surface-soft); }
.radar-highlights span { font-size: 10px; color: var(--bs-text-muted); }
.radar-highlights b { margin-left: 3px; font-size: 10px; color: var(--bs-text); }
.radar-highlights em { margin-left: 3px; font-style: normal; font-family: var(--bs-font-num); font-size: 10px; color: var(--bs-text-muted); }
.radar-foot { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 10px; margin-top: 8px; font-size: 10px; color: var(--bs-text-muted); }
.radar-foot span:last-child { margin-left: auto; }
.radar-foot b { color: var(--bs-text-faint); font-weight: 500; }
.radar-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; margin-top: 6px; font-size: 9px; line-height: 1.5; color: var(--bs-text-faint); }
.radar-reliability { letter-spacing: .03em; }
.radar-note { flex-basis: 100%; color: var(--bs-text-muted); }
.radar-note::before { content: "· "; color: var(--bs-text-faint); }
.radar-empty { display: grid; place-items: center; align-content: center; min-height: 270px; text-align: center; }
.radar-empty b { font-family: var(--bs-font-num); font-size: 30px; font-weight: 700; color: var(--bs-text-faint); }
.radar-empty span { margin-top: 6px; font-size: 11px; color: var(--bs-text-muted); }

.milestone-panel { padding: 19px 21px 16px; }
.milestone-head { align-items: flex-end; }
.milestone-summary { display: flex; align-items: center; gap: 16px; padding-bottom: 2px; font-size: 10px; color: var(--bs-text-muted); }
.milestone-summary b { margin-left: 3px; font-family: var(--bs-font-num); font-size: 13px; color: var(--bs-ink); }
.milestone-body { display: grid; grid-template-columns: minmax(0, 1.45fr) minmax(300px, .55fr); gap: 24px; margin-top: 14px; }
.milestone-metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 9px; }
.milestone-metric { --mm-color: var(--bs-chart-6); min-width: 0; padding: 11px 11px 9px; border: 1px solid var(--bs-hairline-soft); border-radius: 9px; background: var(--bs-surface-soft); }
.milestone-metric.pitching { --mm-color: var(--bs-chart-3); }
.mm-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.mm-short { font-family: var(--bs-font-num); font-size: 10px; font-weight: 800; color: var(--mm-color); letter-spacing: .08em; }
.mm-domain { font-size: 10px; color: var(--bs-text-muted); }
.mm-value { margin-top: 5px; font-family: var(--bs-font-num); font-size: 26px; line-height: 1; font-weight: 700; letter-spacing: -.03em; color: var(--bs-ink); }
.mm-value span { margin-left: 3px; font-size: 11px; font-weight: 500; color: var(--bs-text-muted); letter-spacing: 0; }
.mm-label { margin-top: 3px; font-size: 11px; font-weight: 600; color: var(--bs-text); }
.mm-track { height: 3px; margin-top: 9px; overflow: hidden; border-radius: 99px; background: var(--bs-hairline); }
.mm-track i { display: block; height: 100%; border-radius: 99px; background: var(--mm-color); }
.mm-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 6px; font-size: 10px; color: var(--bs-text-muted); }
.mm-foot b { font-family: var(--bs-font-num); font-size: 10px; color: var(--bs-text-muted); }
.milestone-timeline { min-width: 0; padding-left: 20px; }
.mt-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--bs-hairline-soft); }
.mt-head b { font-family: var(--bs-font-serif); font-size: 17px; color: var(--bs-ink); }
.mt-head span { font-size: 10px; color: var(--bs-text-muted); }
.mt-scroll { position: relative; }
.mt-list { max-height: 238px; overflow-y: auto; overscroll-behavior: contain; padding-right: 14px; scrollbar-width: none; }
.mt-list::-webkit-scrollbar { display: none; }
.mt-list:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 3px; border-radius: 8px; }
.mt-scrollbar { position: absolute; top: 4px; right: 0; bottom: 4px; width: 8px; border-radius: 99px; background: var(--bs-accent-soft); pointer-events: none; }
.mt-scroll-thumb { position: absolute; top: 0; left: 0; width: 8px; border-radius: 99px; background: var(--bs-accent); cursor: grab; pointer-events: auto; touch-action: none; }
.mt-scroll-thumb:active { cursor: grabbing; }
.mt-row { position: relative; display: grid; grid-template-columns: 7px minmax(0, 1fr) auto; gap: 8px; align-items: start; padding: 9px 0; border-bottom: 1px solid var(--bs-hairline-soft); }
.mt-row::before { content: ""; position: absolute; left: 2px; top: 0; bottom: 0; border-left: 1px dashed var(--bs-accent); opacity: .58; pointer-events: none; }
.mt-row:first-child::before { top: 16px; }
.mt-row:last-child::before { bottom: calc(100% - 16px); }
.mt-row:only-child::before { display: none; }
.mt-row:last-child { border-bottom: 0; }
.mt-dot { position: relative; z-index: 1; width: 5px; height: 5px; margin-top: 5px; border-radius: 50%; background: var(--bs-accent); box-shadow: 0 0 0 3px var(--bs-accent-soft); }
.mt-copy b { display: block; font-size: 11px; font-weight: 700; color: var(--bs-text); }
.mt-copy span { display: block; margin-top: 2px; font-size: 10px; color: var(--bs-text-muted); }
.mt-row time { font-family: var(--bs-font-num); font-size: 10px; color: var(--bs-text-muted); white-space: nowrap; }
.mt-note, .mt-empty { margin: 7px 0 0; font-size: 10px; color: var(--bs-text-muted); }
.mt-empty { padding: 10px 0; }

@media (max-width: 1120px) {
  .radar-grid.three-up { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

.dashboard-board { position: relative; margin-top: 20px; width: 100%; min-width: 0; }
.dashboard-board .dashboard-module { min-width: 0; margin: 0; container-type: inline-size; }
.dashboard-board.is-free .dashboard-module {
  position: absolute;
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
}
.dashboard-board.is-free.layout-editing {
  background-image: radial-gradient(circle, var(--bs-hairline-soft) 1px, transparent 1px);
  background-size: 16px 16px;
  border-radius: 12px;
}
.dashboard-board.is-free.layout-editing .dashboard-module {
  overflow: hidden;
  cursor: grab;
  touch-action: none;
  user-select: none;
  transition: box-shadow 160ms ease-out;
}
.dashboard-board.is-free.layout-editing .dashboard-module:hover { box-shadow: var(--bs-shadow-md); }
.dashboard-board.layout-editing .dashboard-module:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 4px; }
.dashboard-board.layout-editing .dashboard-module > * { pointer-events: none; user-select: none; }
.clubhouse-page.is-reordering .dashboard-board.is-free.layout-editing .dashboard-module { cursor: grabbing; }
.rank-placeholder { display: grid; place-items: center; border-style: dashed; background: transparent; box-shadow: none; }
.rank-placeholder-copy { display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--bs-text-muted); font-size: 11px; text-align: center; }
.rank-placeholder-copy b { font-family: var(--bs-font-serif); font-size: 22px; color: var(--bs-ink); }
.layout-selection-frame { position: absolute; z-index: 40; pointer-events: none; border: 2px solid var(--bs-accent); border-radius: 20px; box-shadow: 0 0 0 4px var(--bs-accent-soft), 0 14px 38px rgba(7,26,51,.14); transition: border-color 120ms ease-out, box-shadow 120ms ease-out; }
.layout-swap-frame { position: absolute; z-index: 39; pointer-events: none; border: 2px dashed var(--bs-accent); border-radius: 20px; background: color-mix(in srgb, var(--bs-accent) 5%, transparent); box-shadow: 0 0 0 4px var(--bs-accent-soft); }
.layout-swap-frame .swap-frame-badge { position: absolute; top: -14px; left: 50%; transform: translateX(-50%); padding: 3px 9px; border-radius: 999px; background: var(--bs-accent); color: var(--bs-on-accent); font-size: 10px; font-weight: 800; letter-spacing: .04em; white-space: nowrap; box-shadow: 0 4px 12px rgba(7,26,51,.18); }
.layout-selection-frame.is-blocked { border-color: var(--bs-danger); box-shadow: 0 0 0 4px color-mix(in srgb, var(--bs-danger) 16%, transparent), 0 14px 38px rgba(7,26,51,.14); }
.resize-handle { position: absolute; display: grid; place-items: center; width: 24px; height: 24px; padding: 0; border: 0; background: transparent; pointer-events: auto; touch-action: none; }
.resize-handle::before { content: ""; width: 10px; height: 10px; border: 1px solid var(--bs-accent-ink); border-radius: 2px; background: var(--bs-accent); box-shadow: 0 1px 4px rgba(7,26,51,.22); }
.layout-selection-frame.is-blocked .resize-handle::before { border-color: var(--bs-danger); background: var(--bs-danger); }
.resize-handle:disabled { opacity: .4; cursor: not-allowed; }
.resize-handle:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 2px; }
.resize-handle.handle-nw { left: -12px; top: -12px; cursor: nwse-resize; }
.resize-handle.handle-n { left: 50%; top: -12px; transform: translateX(-50%); cursor: ns-resize; }
.resize-handle.handle-ne { right: -12px; top: -12px; cursor: nesw-resize; }
.resize-handle.handle-w { left: -12px; top: 50%; transform: translateY(-50%); cursor: ew-resize; }
.resize-handle.handle-e { right: -12px; top: 50%; transform: translateY(-50%); cursor: ew-resize; }
.resize-handle.handle-sw { left: -12px; bottom: -12px; cursor: nesw-resize; }
.resize-handle.handle-s { left: 50%; bottom: -12px; transform: translateX(-50%); cursor: ns-resize; }
.resize-handle.handle-se { right: -12px; bottom: -12px; cursor: nwse-resize; }
/* 模块宽度不再固定，内部布局按模块自身宽度收敛。 */
@container (max-width: 900px) {
  .eval-head { flex-wrap: wrap; align-items: flex-start; }
  .eval-overall { flex-wrap: wrap; }
  .rank-band-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .rank-feature { padding: 0; border-right: 0; }
  .milestone-body { grid-template-columns: 1fr; gap: 18px; }
  .milestone-timeline { padding-left: 0; padding-top: 14px; border-top: 1px solid var(--bs-hairline-soft); }
}
@container (max-width: 620px) {
  .panel-head { align-items: flex-start; flex-wrap: wrap; }
  .panel-head-actions { flex-wrap: wrap; }
  .matrix-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .recent-cards { grid-template-columns: 1fr; }
  .schedule-row { grid-template-columns: 46px 1fr auto; }
  .sch-time { display: none; }
  .radar-grid.two-up, .radar-grid.three-up { grid-template-columns: 1fr; }
  .donut-wrap { flex-wrap: wrap; gap: 16px; }
  .chart-wrap { overflow-x: auto; }
  .trend-chart { min-width: 480px; }
  .eval-meta { align-self: flex-start; }
}
.content-grid, .main-col, .side-col { display: contents; }
.dashboard-empty { margin-top: 20px; padding: 46px 20px; text-align: center; }
.dashboard-empty h3 { margin: 8px 0; font-family: var(--bs-font-serif); font-size: 22px; color: var(--bs-ink); }
.dashboard-empty p { margin: 0 0 16px; font-size: 11px; color: var(--bs-text-muted); }
.panel { background: var(--bs-surface); border: 1px solid var(--bs-hairline-soft); border-radius: 18px; box-shadow: var(--bs-shadow-sm); padding: 19px 20px; }
.panel-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin-bottom: 13px; }
.kicker { display: block; font-size: 10px; font-weight: 700; letter-spacing: .16em; color: var(--bs-accent-ink); text-transform: uppercase; }
.panel-head h3 { margin: 5px 0 0; font-family: var(--bs-font-serif); font-size: 20px; line-height: 1.2; font-weight: 700; color: var(--bs-ink); }
.panel-head h3 small { font-family: var(--bs-font-sans); font-size: 10px; color: var(--bs-text-faint); font-weight: 400; margin-left: 5px; }
.panel-head-actions { display: flex; align-items: center; gap: 14px; }
.eval-meta .metric-info { margin-left: 0; }
.chart-summary { text-align: right; }
.chart-summary b { display: block; font-family: var(--bs-font-num); font-size: 18px; color: var(--bs-ink); }
.chart-summary span { display: block; margin-top: 3px; font-size: 10px; color: var(--bs-text-faint); }
.chart-wrap { margin-top: 4px; }
.trend-chart { width: 100%; height: auto; overflow: visible; }
.trend-chart .grid { stroke: var(--bs-hairline-soft); stroke-width: 1; }
.trend-chart .axis { fill: var(--bs-text-faint); font-family: var(--bs-font-sans); font-size: 10px; }
.trend-chart .line { fill: none; stroke: var(--bs-chart-line); stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; }
.trend-chart .area { fill: var(--bs-chart-fill); }
.trend-chart .dot { fill: var(--bs-surface); stroke: var(--bs-chart-line); stroke-width: 2; }
.trend-chart .bar { fill: var(--bs-chart-1); opacity: .9; }
.chart-note { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 5px; font-size: 10px; color: var(--bs-text-muted); }
.chart-note b { color: var(--bs-text-faint); font-weight: 500; }
.data-empty { padding: 24px 0; text-align: center; font-size: 11px; color: var(--bs-text-muted); }
.kpi-view { padding: 24px 0 18px; }
.kpi-view b { display: block; font-family: var(--bs-font-num); font-size: 64px; line-height: 1; color: var(--bs-ink); letter-spacing: -.04em; }
.kpi-view span { display: block; margin-top: 8px; font-size: 11px; color: var(--bs-text-muted); }
.donut-wrap { display: flex; align-items: center; gap: 28px; padding: 18px 0 12px; }
.donut { width: 150px; height: 150px; border-radius: 50%; display: grid; place-items: center; flex: 0 0 auto; }
.donut-center { width: 104px; height: 104px; border-radius: 50%; display: grid; place-items: center; align-content: center; background: var(--bs-surface); }
.donut-center b { font-family: var(--bs-font-num); font-size: 26px; color: var(--bs-ink); }
.donut-center span { margin-top: 3px; font-size: 10px; color: var(--bs-text-faint); }
.donut-legend { flex: 1; min-width: 0; }
.donut-legend-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 10px; color: var(--bs-text); }
.donut-legend-row i { width: 8px; height: 8px; border-radius: 2px; flex: 0 0 auto; }
.donut-legend-row span { flex: 1; }
.donut-legend-row b { font-family: var(--bs-font-num); color: var(--bs-ink); }
.matrix-grid { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 14px; padding-top: 13px; border-top: 1px solid var(--bs-hairline-soft); }
.matrix-item { padding: 8px 16px; border-right: 1px solid var(--bs-hairline-soft); }
.matrix-item:nth-child(3n) { border-right: 0; }
.matrix-item:nth-child(3n + 1) { padding-left: 0; }
.matrix-item b { display: block; font-family: var(--bs-font-num); font-size: 19px; line-height: 1; color: var(--bs-ink); }
.matrix-item span { display: block; margin-top: 4px; font-size: 10px; color: var(--bs-text-muted); }

.next-game { position: relative; overflow: hidden; background: linear-gradient(145deg, var(--bs-hero-start), var(--bs-hero-mid) 68%); border-radius: 14px; padding: 17px 19px; color: #fff; border: 1px solid var(--bs-hairline); }
.next-game::after { content: ""; position: absolute; right: -50px; bottom: -76px; width: 190px; height: 190px; border-radius: 50%; border: 1px solid rgba(201,164,91,.17); box-shadow: 0 0 0 24px rgba(201,164,91,.035), 0 0 0 54px rgba(201,164,91,.018); }
.ng-head { display: flex; align-items: center; justify-content: space-between; position: relative; z-index: 1; }
.event-pill { display: inline-flex; align-items: center; padding: 4px 8px; border-radius: 5px; font-size: 10px; font-weight: 700; color: #24180a; background: linear-gradient(135deg, var(--bs-accent-hover), var(--bs-accent)); }
.countdown { font-size: 10px; color: var(--bs-hero-muted); }
.ng-body { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 18px; margin-top: 16px; position: relative; z-index: 1; }
.ng-side { display: flex; align-items: center; gap: 10px; min-width: 0; }
.ng-side.right { justify-content: flex-end; text-align: right; }
.ng-side .crest { width: 38px; height: 38px; border-radius: 11px; }
.ng-side b { display: block; font-size: 13px; font-weight: 700; line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ng-side span:last-child { display: block; margin-top: 2px; font-size: 10px; color: var(--bs-hero-muted); }
.ng-vs { font-family: var(--bs-font-num); font-size: 13px; font-weight: 800; color: var(--bs-accent-ink); letter-spacing: .08em; }
.ng-foot { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 14px; padding-top: 10px; border-top: 1px solid var(--bs-hero-line); position: relative; z-index: 1; }
.ng-meta { display: flex; align-items: center; gap: 18px; font-size: 10px; color: var(--bs-hero-muted); }
.ng-meta b { font-family: var(--bs-font-num); font-size: 11px; color: #fff; }
.ng-foot :deep(.el-button) { color: var(--bs-accent-ink); font-size: 10px; }
.schedule-list { margin-top: 4px; }
.schedule-row { display: grid; grid-template-columns: 58px 1fr auto auto; align-items: center; gap: 14px; padding: 11px 0; border-bottom: 1px solid var(--bs-hairline-soft); }
.schedule-row:last-child { border-bottom: 0; padding-bottom: 0; }
.sch-date { font-family: var(--bs-font-num); font-size: 12px; font-weight: 700; color: var(--bs-ink); }
.sch-date span { display: block; font-family: var(--bs-font-sans); font-size: 10px; color: var(--bs-text-faint); margin-top: 1px; }
.sch-main b { display: block; font-size: 10px; font-weight: 700; color: var(--bs-text); }
.sch-main span { display: block; margin-top: 2px; font-size: 10px; color: var(--bs-text-muted); }
.sch-event { font-size: 10px; font-weight: 700; color: var(--bs-accent-ink); background: var(--bs-accent-soft); padding: 4px 6px; border-radius: 4px; white-space: nowrap; }
.sch-event.blue { color: #3d7fb5; background: rgba(61,127,181,.1); }
.sch-event.teal { color: #2f7f7a; background: rgba(47,127,122,.1); }
html.dark .sch-event.blue,
html[data-portal-theme="dark"] .sch-event.blue { color: #89b9de; background: rgba(89,185,222,.1); }
html.dark .sch-event.teal,
html[data-portal-theme="dark"] .sch-event.teal { color: #7fd1c8; background: rgba(127,209,200,.1); }
.sch-time { font-family: var(--bs-font-num); font-size: 10px; font-weight: 700; color: var(--bs-ink); }

.recent-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.result-card { background: var(--bs-surface-soft); border: 1px solid var(--bs-hairline-soft); border-radius: 10px; padding: 12px; }
.result-top { display: flex; align-items: center; justify-content: space-between; }
.result-top .date { font-size: 10px; color: var(--bs-text-faint); }
.res-badge { width: 20px; height: 20px; border-radius: 5px; display: grid; place-items: center; font-family: var(--bs-font-num); font-size: 10px; font-weight: 800; }
.res-badge.w { color: var(--bs-success); background: rgba(31,122,90,.1); }
.res-badge.l { color: var(--bs-danger); background: rgba(181,18,46,.1); }
.result-score { display: flex; align-items: baseline; gap: 6px; margin-top: 10px; }
.result-score b { font-family: var(--bs-font-num); font-size: 24px; line-height: 1; color: var(--bs-ink); }
.result-score span { font-size: 10px; color: var(--bs-text-faint); }
.result-opponent { margin-top: 5px; font-size: 10px; font-weight: 700; color: var(--bs-text); }
.result-meta { margin-top: 3px; font-size: 10px; color: var(--bs-text-faint); }
.result-line { margin-top: 9px; padding-top: 8px; border-top: 1px solid var(--bs-hairline-soft); font-size: 10px; line-height: 1.65; color: var(--bs-text-muted); }

.team-panel { position: relative; overflow: hidden; }
.rep-team { display: flex; align-items: center; gap: 10px; padding: 9px 0; border-bottom: 1px solid var(--bs-hairline-soft); cursor: pointer; }
.rep-team:last-of-type { border-bottom: 0; }
.rep-team.active { background: var(--bs-surface-soft); margin: 0 -8px; padding-left: 8px; padding-right: 8px; border-radius: 8px; }
.crest-lg { width: 38px; height: 38px; border-radius: 11px; display: grid; place-items: center; font-family: var(--bs-font-num); font-size: 12px; font-weight: 800; color: #071a33; background: linear-gradient(145deg, var(--bs-accent-soft), var(--bs-accent)); }
.rep-name { flex: 1; min-width: 0; }
.rep-name b { display: block; font-size: 12px; font-weight: 800; color: var(--bs-ink); }
.rep-name span { display: block; margin-top: 2px; font-size: 10px; color: var(--bs-text-muted); }
.rep-stat { text-align: right; }
.rep-stat b { display: block; font-family: var(--bs-font-num); font-size: 14px; color: var(--bs-ink); }
.rep-stat span { display: block; margin-top: 2px; font-size: 10px; color: var(--bs-text-faint); }
.rep-scope { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 10px; padding-top: 9px; border-top: 1px solid var(--bs-hairline-soft); font-size: 10px; color: var(--bs-text-muted); }
.about-intro { margin: 0; font-size: 10px; line-height: 1.85; color: var(--bs-text-muted); }
.about-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; margin-top: 14px; padding-top: 13px; border-top: 1px solid var(--bs-hairline-soft); }
.about-meta span { display: block; font-size: 10px; color: var(--bs-text-faint); }
.about-meta b { display: block; margin-top: 3px; font-size: 10px; font-weight: 600; color: var(--bs-text); }
.shortcut-list { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.shortcut { padding: 5px 0 !important; font-size: 10px; }
@media (prefers-reduced-motion: reduce) {
  .dashboard-board.layout-editing .dashboard-module { transition: none; }
}
.page-state { padding: 80px 20px; }
.state-detail { text-align: center; font-size: 11px; color: var(--bs-text-muted); }
.settings-section { margin-bottom: 22px; }
.settings-label { font-size: 13px; font-weight: 700; color: var(--bs-ink); }
.settings-hint { margin: 5px 0 12px; font-size: 11px; line-height: 1.6; color: var(--bs-text-muted); }
.settings-control { width: 100%; }
.module-sort-list { border-top: 1px solid var(--bs-hairline-soft); }
.module-sort-row { display: grid; grid-template-columns: 24px minmax(0, 1fr) auto auto; align-items: center; gap: 8px; padding: 7px 0; border-bottom: 1px solid var(--bs-hairline-soft); }
.module-drag { position: relative; width: 24px; height: 24px; cursor: grab; opacity: .45; }
.module-drag::before { content: ""; position: absolute; top: 50%; left: 50%; width: 12px; height: 18px; transform: translate(-50%, -50%); background-image: radial-gradient(circle, currentColor 1px, transparent 1.2px); background-size: 5px 6px; background-position: center; }
.module-drag:active { cursor: grabbing; }
.module-sort-name { min-width: 0; }
.module-sort-name b { display: block; font-size: 12px; font-weight: 700; color: var(--bs-text); }
.module-sort-name span { display: block; margin-top: 2px; font-size: 11px; color: var(--bs-text-muted); }
.module-sort-actions { display: flex; gap: 1px; }
.module-sort-actions :deep(.el-button) { width: 24px; height: 24px; margin-left: 0; padding: 0; }
.module-switch { display: grid; place-items: center; width: 34px; height: 34px; cursor: pointer; }
.module-sort-row :deep(.el-switch) { --el-switch-on-color: var(--bs-accent); --el-switch-off-color: var(--bs-hairline); }
.module-sort-row.is-off .module-drag { opacity: .25; }
.module-sort-row.is-off .module-sort-name span { color: var(--bs-text-muted); }
.module-sort-count { margin-top: 8px; text-align: right; font-size: 11px; color: var(--bs-text-muted); }
.settings-count { margin-top: 7px; text-align: right; font-size: 11px; color: var(--bs-text-muted); }

/* 紧凑桌面/平板：双列流式，保存顺序生效，禁用自由布局编辑。 */
@media (max-width: 1339px) {
  .layout-edit-entry, .layout-edit-banner, .layout-selection-frame, .layout-swap-frame { display: none !important; }
  .dashboard-board {
    display: grid;
    grid-template-columns: 1fr;
    gap: 20px;
    height: auto !important;
  }
  .dashboard-board .dashboard-module {
    position: static !important;
    width: auto !important;
    height: auto !important;
    min-height: 0 !important;
    overflow: visible;
  }
}
@media (min-width: 981px) and (max-width: 1339px) {
  .dashboard-board { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

/* 手机端：单列，玩家卡全宽，图表/矩阵/赛程堆叠。 */
@media (max-width: 980px) {
  .clubhouse-page { padding: 18px 16px 40px; }
  .hero { grid-template-columns: 1fr; min-height: 0; padding: 22px 18px; }
  .hero h1 { font-size: 44px; }
  .hero-27 { font-size: 260px; right: -20px; }
  .card-stage { min-height: 0; margin-top: 20px; justify-content: flex-start; }
  .card-stack { display: none; }
  .identity-band { grid-template-columns: 1fr 1fr; }
  .id-item { padding: 11px 14px; border-bottom: 1px solid var(--bs-hairline); }
  .id-item:nth-child(odd) { padding-left: 0; border-right: 1px solid var(--bs-hairline); }
  .id-item:nth-child(even) { padding-right: 0; }
  .id-item:nth-last-child(-n+2) { border-bottom: 0; }
  .rank-band { padding: 16px; }
  .rank-band-grid { grid-template-columns: 1fr 1fr; gap: 12px; }
  .rank-feature { padding: 0; border-right: 0; }
  .evaluation-panel { padding: 17px 16px 14px; }
  .eval-head { align-items: flex-start; flex-direction: column; gap: 10px; }
  .eval-overall { align-self: stretch; justify-content: space-between; }
  .eval-meta { align-self: flex-end; }
  .milestone-panel { padding: 17px 16px 14px; }
  .milestone-head { align-items: flex-start; flex-direction: column; gap: 8px; }
  .milestone-summary { align-self: flex-end; flex-wrap: wrap; justify-content: flex-end; }
  .milestone-body { grid-template-columns: 1fr; gap: 18px; }
  .milestone-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .milestone-timeline { padding-left: 0; padding-top: 14px; border-top: 1px solid var(--bs-hairline-soft); }
  .radar-grid.two-up, .radar-grid.three-up { grid-template-columns: 1fr; }
  .radar-card { padding: 16px 15px 13px; }
  .dashboard-board { grid-template-columns: 1fr; }
  .dashboard-board .dashboard-module { grid-column: 1 / -1; min-height: 0; }
  .module-sort-row { grid-template-columns: 44px minmax(0, 1fr) auto auto; min-height: 52px; }
  .module-drag { width: 44px; height: 44px; }
  .module-sort-actions :deep(.el-button) { width: 44px; height: 44px; }
  .module-switch { width: 44px; height: 44px; }
  .recent-cards { grid-template-columns: 1fr; }
  .chart-summary { display: none; }
  .scope-bar { flex-wrap: wrap; }
  .scope-hint { width: 100%; order: 5; }
  .scope-right { margin-left: 0; }
  .panel-head { align-items: flex-start; }
  .hero-kicker { color: var(--bs-accent-hover); }
  .chart-wrap { overflow-x: auto; }
  .trend-chart { min-width: 540px; }
}
@media (max-width: 560px) {
  .page-head { align-items: flex-start; flex-direction: column; }
  .page-actions { align-self: flex-end; }
  .scope-select { width: calc(50% - 5px); }
  .hero h1 { font-size: 38px; }
  .hero-intro { font-size: 11px; }
  .hero-team { max-width: none; }
  .schedule-row { grid-template-columns: 46px 1fr auto; }
  .sch-time { display: none; }
  .donut-wrap { gap: 16px; }
  .donut { width: 120px; height: 120px; }
  .donut-center { width: 84px; height: 84px; }
  .matrix-item { padding: 8px 10px; }
  .radar-card h4 { font-size: 20px; }
  .radar-score b { font-size: 38px; }
  .radar-metrics { gap: 0 20px; }
  .radar-metric { padding: 7px 0; }
  .radar-metric em { font-size: 13px; }
  .eval-overall { flex-wrap: wrap; gap: 8px 12px; }
  .eo-side { flex-basis: 100%; }
  .eo-tags, .eo-sample { overflow-wrap: anywhere; }
}
</style>

<style>
/* 仅在本路由生效的门户外壳轻量对齐；不影响其他门户页面。 */
html.clubhouse-route .portal-layout { background: var(--bs-canvas); }
html.clubhouse-route .portal-header {
  --portal-header-bg: var(--bs-canvas);
  --portal-header-text: var(--bs-ink);
  background: var(--bs-canvas) !important;
  border-bottom: 1px solid var(--bs-hairline);
}
html.clubhouse-route .portal-header .logo-text { color: var(--bs-ink) !important; }
html.clubhouse-route .portal-header .nav a { color: var(--bs-text-muted) !important; }
html.clubhouse-route .portal-header .nav a.nav-active,
html.clubhouse-route .portal-header .nav a.router-link-active,
html.clubhouse-route .portal-header .nav a[aria-current="page"] { color: var(--bs-ink) !important; }
html.clubhouse-route .portal-footer {
  --portal-footer-bg: var(--bs-ink);
  --portal-footer-text: rgba(255, 255, 255, 0.68);
  background: var(--bs-ink) !important;
}
html.clubhouse-route .portal-layout { --el-color-primary: var(--bs-accent); }
html.clubhouse-route .clubhouse-settings {
  --el-bg-color: var(--bs-surface);
  --el-bg-color-overlay: var(--bs-surface);
  --el-text-color-primary: var(--bs-ink);
  --el-text-color-regular: var(--bs-text);
  --el-text-color-secondary: var(--bs-text-muted);
  --el-border-color: var(--bs-hairline);
  --el-border-color-light: var(--bs-hairline-soft);
  --el-color-primary: var(--bs-accent);
  --el-color-primary-light-9: var(--bs-accent-soft);
}
</style>
