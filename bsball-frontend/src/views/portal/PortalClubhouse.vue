<template>
  <div class="clubhouse-page" v-loading="loading">
    <template v-if="data">
      <div class="page-head">
        <div>
          <div class="breadcrumb">门户 / <b>我的主场</b></div>
          <h1>我的主场</h1>
          <p class="page-sub">球员个人主页 · 已关联球员档案</p>
        </div>
        <div class="page-actions">
          <el-button link type="primary" @click="openSettings">数据设置</el-button>
          <el-button link type="primary" @click="goAccount">个人资料</el-button>
          <el-button @click="goPortal">返回门户</el-button>
        </div>
      </div>

      <div class="scope-bar">
        <span class="scope-title">数据范围</span>
        <el-select v-model="teamId" size="small" class="scope-select" @change="onTeamChange">
          <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
        </el-select>
        <el-select v-model="eventId" size="small" class="scope-select" :disabled="!scopeEvents.length" @change="load">
          <el-option v-for="event in scopeEvents" :key="event.id" :label="event.name" :value="event.id" />
        </el-select>
        <span class="scope-hint">上方数据按所选赛事聚合；队内排名按所选球队与赛事计算</span>
        <span class="scope-right">代表 <b>{{ teams.length }}</b> 支球队</span>
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
            <span class="crest">{{ teamCrest(scopeTeam) }}</span>
            <div class="team-copy">
              <b>{{ scopeTeam?.name || '未加入球队' }}</b>
              <span>{{ teamEventLabel(scopeTeam) }}</span>
            </div>
          </div>
          <div class="hero-actions">
            <button class="hero-btn primary" type="button" @click="goPublicProfile">查看公开主页</button>
            <button class="hero-btn ghost" type="button" @click="goTeam">球队主页</button>
          </div>
        </div>

        <div class="card-stage">
          <div class="card-back one"></div>
          <div class="card-back two"></div>
          <div class="player-card">
            <div>
              <div class="card-top"><span>BSD PLAYER CARD</span><b>{{ new Date().getFullYear() }} / 001</b></div>
              <div class="card-portrait">
                <div class="card-avatar">
                  <img v-if="player.avatar" :src="resolveAssetUrl(player.avatar)" :alt="player.name" />
                  <span v-else>{{ (player.name || '?').slice(0, 1) }}</span>
                </div>
                <div class="card-name">
                  <b>{{ player.name }}</b>
                  <span>{{ player.nameEn || 'PLAYER' }} <template v-if="player.number">· #{{ player.number }}</template></span>
                </div>
                <div class="card-team"><span class="crest">{{ teamCrest(scopeTeam) }}</span><span>{{ scopeTeam?.name || '未加入球队' }} · {{ primaryPosition }}</span></div>
              </div>
            </div>
            <div>
              <div class="card-stats">
                <div class="card-stat"><b>{{ heroStats[0].value }}</b><span>{{ heroStats[0].label }}</span></div>
                <div class="card-stat"><b>{{ heroStats[1].value }}</b><span>{{ heroStats[1].label }}</span></div>
                <div class="card-stat"><b>{{ heroStats[2].value }}</b><span>{{ heroStats[2].label }}</span></div>
              </div>
              <div class="card-foot"><span>代表 {{ teams.length }} 队 · {{ totalEvents }} 项赛事</span><b>{{ performanceScopeLabel }}</b></div>
            </div>
          </div>
        </div>
      </section>

      <div class="identity-band">
        <div class="id-item"><span>守备位置</span><b>{{ primaryPosition }}</b></div>
        <div class="id-item"><span>投打</span><b>{{ throwBatLabel }}</b></div>
        <div class="id-item"><span>身高 / 体重</span><b><em>{{ player.height || '-' }}</em> / {{ player.weight || '-' }}</b></div>
        <div class="id-item"><span>出生地</span><b>{{ player.birthPlace || '未填写' }}</b></div>
      </div>

      <div v-if="activeModules.length" class="dashboard-grid">
      <section
        v-if="isModuleVisible('ranks')"
        class="rank-band"
        :class="moduleSpanClass('ranks')"
        :style="{ order: moduleOrder('ranks') }"
      >
        <div class="rank-band-head">
          <div><span class="kicker">TEAM RANKING</span><h2>队内坐标</h2></div>
          <span class="rank-scope">{{ data.scope.teamName || '-' }} · {{ data.scope.eventName || '全部赛事' }}<template v-if="rankPopulation"> · {{ rankPopulation }} 人</template> <a href="#" @click.prevent="openSettings">切换</a></span>
        </div>
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
        class="panel evaluation-panel"
        :class="moduleSpanClass('evaluation')"
        :style="{ order: moduleOrder('evaluation') }"
      >
        <div class="eval-head">
          <div>
            <span class="kicker">OVERALL PROFILE</span>
            <h3>总体评价 <small>职业指标 · 同队同赛事百分位</small></h3>
          </div>
          <div class="eval-meta">
            <span><b>{{ evaluationCards.length }}</b> 个评价维度</span>
            <span>{{ data.evaluation?.comparison || '同队同赛事' }}</span>
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
                <span><b>样本</b> {{ card.sampleSize }} 人</span>
                <span>{{ radarFoot(card.domain) }}</span>
              </div>
            </template>
            <div v-else class="radar-empty">
              <b>—</b>
              <span>{{ card.reason || '暂无评价数据' }}</span>
            </div>
          </article>
        </div>
        <p class="eval-note">
          <i></i>
          <span>百分位越高代表同队同赛事排名越靠前；投球指标已按“越低越好”反向换算。守备指标受守位影响，请结合球员位置阅读。</span>
        </p>
      </section>

      <div class="content-grid">
        <div class="main-col">
          <section
            v-if="isModuleVisible('performance')"
            class="panel data-panel"
            :class="moduleSpanClass('performance')"
            :style="{ order: moduleOrder('performance') }"
          >
            <div class="panel-head">
              <div><span class="kicker">PERFORMANCE</span><h3>数据表现 <small>{{ performanceScopeLabel }}</small></h3></div>
              <div class="panel-head-actions">
                <div class="chart-summary">
                  <div><b>{{ performanceSummary }}</b><span>{{ performanceLabel }}</span></div>
                </div>
                <el-button link :icon="Setting" aria-label="数据设置" @click="openSettings" />
              </div>
            </div>

            <div v-if="data.performance.viz === 'donut'" class="donut-wrap">
              <div class="donut" :style="{ background: donutStyle }">
                <div class="donut-center"><b>{{ donutTotal }}</b><span>安打</span></div>
              </div>
              <div class="donut-legend">
                <div v-for="(item, index) in data.performance.items" :key="item.label" class="donut-legend-row">
                  <i :style="{ background: chartColor(index) }"></i><span>{{ item.label }}</span><b>{{ item.value }}</b>
                </div>
              </div>
            </div>

            <div v-else-if="data.performance.viz === 'kpi'" class="kpi-view">
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

            <div class="matrix-grid">
              <div v-for="item in matrixItems" :key="item.label" class="matrix-item">
                <b>{{ item.value }}</b><span>{{ item.label }}</span>
              </div>
            </div>
          </section>

          <section
            v-if="isModuleVisible('schedule')"
            class="panel"
            :class="moduleSpanClass('schedule')"
            :style="{ order: moduleOrder('schedule') }"
          >
            <div class="panel-head"><div><span class="kicker">UP NEXT</span><h3>接下来的比赛 <small>跨赛事汇总</small></h3></div><el-button link type="primary" @click="goEvents">全部赛程</el-button></div>
            <div v-if="nextGame" class="next-game">
              <div class="ng-head"><span class="event-pill">{{ nextGame.eventName || '赛事' }}</span><span class="countdown">{{ countdown(nextGame.gameday) }}</span></div>
              <div class="ng-body">
                <div class="ng-side"><span class="crest">{{ teamCrest(scopeTeam) }}</span><div><b>{{ nextGame.teamName }}</b><span>{{ nextGame.isHome ? '主场' : '客场' }}</span></div></div>
                <span class="ng-vs">VS</span>
                <div class="ng-side right"><div><b>{{ nextGame.opponentName }}</b><span>{{ nextGame.isHome ? '客场' : '主场' }}</span></div><span class="crest dark">{{ opponentCrest(nextGame) }}</span></div>
              </div>
              <div class="ng-foot">
                <div class="ng-meta"><span>{{ formatDay(nextGame.gameday) }} · {{ weekday(nextGame.gameday) }}</span><span>{{ nextGame.venue || '场地待定' }}</span><span><b>{{ nextGame.gameTime || '--:--' }}</b></span></div>
                <el-button link type="primary" @click="goGame(nextGame)">赛事详情 ›</el-button>
              </div>
            </div>
            <div v-else class="data-empty">暂无未开赛的比赛。</div>
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
            class="panel"
            :class="moduleSpanClass('recentGames')"
            :style="{ order: moduleOrder('recentGames') }"
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
            <div v-else class="data-empty">暂无比赛记录。</div>
          </section>
        </div>

        <aside class="side-col">
          <section
            v-if="isModuleVisible('teams')"
            class="panel team-panel"
            :class="moduleSpanClass('teams')"
            :style="{ order: moduleOrder('teams') }"
          >
            <div class="panel-head"><div><span class="kicker">REPRESENTED TEAMS</span><h3>代表球队</h3></div></div>
            <div v-for="team in teams" :key="team.id" class="rep-team" :class="{ active: team.id === teamId }" @click="switchTeam(team)">
              <span class="crest-lg">{{ teamCrest(team) }}</span>
              <div class="rep-name"><b>{{ team.name }}</b><span>{{ teamEventLabel(team) }}</span></div>
              <div class="rep-stat"><b>{{ standingRank(team) }}</b><span>{{ standingRecord(team) }}</span></div>
            </div>
            <div class="rep-scope"><span>球队积分按各队主赛事显示</span><el-button link type="primary" @click="goTeam">球队主页 ›</el-button></div>
          </section>

          <section
            v-if="isModuleVisible('about')"
            class="panel about-panel"
            :class="moduleSpanClass('about')"
            :style="{ order: moduleOrder('about') }"
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
            class="panel"
            :class="moduleSpanClass('quickLinks')"
            :style="{ order: moduleOrder('quickLinks') }"
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
      <div v-else class="panel dashboard-empty">
        <span class="kicker">EMPTY DASHBOARD</span>
        <h3>所有内容模块已隐藏</h3>
        <p>可以随时在数据设置里重新打开需要的内容。</p>
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
        <p class="settings-hint">拖动左侧把手排序，也可用上下按钮微调；宽度本版固定。</p>
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
              <span>{{ moduleWidthLabel(module.span) }}</span>
            </div>
            <div class="module-sort-actions">
              <el-button link :icon="ArrowUp" :disabled="index === 0" aria-label="上移" @click="moveModule(index, -1)" />
              <el-button link :icon="ArrowDown" :disabled="index === draft.modules.length - 1" aria-label="下移" @click="moveModule(index, 1)" />
            </div>
            <div class="module-switch" @click.self="module.enabled = !module.enabled">
              <el-switch v-model="module.enabled" size="small" :aria-label="`显示${module.label}`" />
            </div>
          </div>
        </div>
        <div class="module-sort-count">已启用 {{ enabledDraftCount }} / {{ draft.modules.length }}</div>
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
        <p class="settings-hint">排名按当前代表球队与赛事计算；默认不提供全垒打。</p>
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, ArrowUp, Setting } from '@element-plus/icons-vue';
import Sortable from 'sortablejs';
import dayjs from 'dayjs';
import { clubhouseApi } from '../../api/clubhouse';
import { isAuthSessionError, resolveAssetUrl } from '../../api/request';
import { useTenantRouter } from '../../composables/useTenantRouter';

const router = useRouter();
const { tenantCode } = useTenantRouter();

const DEFAULT_RANK_KEYS = ['batting.avg', 'batting.h', 'batting.obp', 'batting.rbi'];
const DEFAULT_MODULE_KEYS = ['ranks', 'evaluation', 'performance', 'teams', 'schedule', 'about', 'recentGames', 'quickLinks'];
const data = ref(null);
const loading = ref(false);
const loadError = ref('');
const teamId = ref(null);
const eventId = ref(null);
const settingsOpen = ref(false);
const saving = ref(false);
const moduleListEl = ref(null);
let moduleSortable = null;
let requestSeq = 0;
const areaGradientId = `clubhouse-area-${Math.random().toString(36).slice(2)}`;

const draft = reactive({
  performance: { metricKey: 'batting.avg', viz: 'line' },
  ranks: DEFAULT_RANK_KEYS.map((metricKey) => ({ metricKey })),
  modules: []
});

const player = computed(() => data.value?.player || {});
const teams = computed(() => data.value?.teams || []);
const catalog = computed(() => data.value?.catalog || []);
const scopeEvents = computed(() => {
  const team = teams.value.find((item) => item.id === teamId.value) || teams.value[0];
  return team?.events || [];
});
const scopeTeam = computed(() => teams.value.find((item) => item.id === teamId.value) || teams.value[0] || null);
const primaryPosition = computed(() => (player.value.positions || [])[0] || '未填写');
const throwBatLabel = computed(() => {
  const throwHand = String(player.value.throwHand || '-').replace(/投$/, '');
  const batHand = String(player.value.batHand || '-').replace(/打$/, '');
  return `${throwHand}投${batHand}打`;
});
const totalEvents = computed(() => {
  const ids = new Set();
  teams.value.forEach((team) => (team.events || []).forEach((event) => ids.add(event.id)));
  return ids.size;
});
const rankPopulation = computed(() => {
  const totals = (data.value?.ranks || []).map((item) => item.total).filter((total) => total > 0);
  return totals.length && totals.every((total) => total === totals[0]) ? totals[0] : 0;
});
const performance = computed(() => data.value?.performance || {});
const performanceLabel = computed(() => performance.value.label || '数据');
const performanceScopeLabel = computed(() => eventId.value ? (data.value?.scope?.eventName || '所选赛事') : '全部赛事');
const performanceSummary = computed(() => {
  const value = performance.value.summaryValue;
  return value == null || value === '-' || value === '' ? '—' : value;
});
const batting = computed(() => data.value?.stats?.batting || {});
const pitching = computed(() => data.value?.stats?.pitching || {});
const hasBatting = computed(() => batting.value.ab > 0 || batting.value.pa > 0);
const evaluationCards = computed(() => (data.value?.evaluation?.cards || []).map(prepareEvaluationCard));
const enabledDraftCount = computed(() => draft.modules.filter((module) => module.enabled).length);
const moduleCatalog = computed(() => data.value?.moduleCatalog || []);
const moduleMeta = computed(() => Object.fromEntries(
  moduleCatalog.value.map((item) => [item.key, item])
));
function resolveModule(module) {
  const meta = moduleMeta.value[module.key];
  return {
    ...module,
    enabled: module.enabled !== false,
    label: meta?.label || module.label || module.key,
    span: meta?.span || module.span || 4
  };
}
function defaultModules() {
  return DEFAULT_MODULE_KEYS.map((key) => resolveModule({ key }));
}
const activeModules = computed(() => {
  const configured = data.value?.config?.modules;
  const source = configured?.length ? configured : defaultModules();
  return source.filter((module) => module.enabled !== false).map(resolveModule);
});
const moduleIndex = computed(() => new Map(activeModules.value.map((module, index) => [module.key, index])));
function isModuleVisible(key) {
  return moduleIndex.value.has(key);
}
function moduleSpanClass(key) {
  const module = activeModules.value.find((item) => item.key === key);
  return `span-${module?.span || 12}`;
}
function moduleOrder(key) {
  return moduleIndex.value.get(key) ?? 0;
}
const heroStats = computed(() => {
  const b = batting.value;
  if (hasBatting.value) {
    return [
      { label: 'AVG', value: b.avg ?? '-' },
      { label: 'OBP', value: b.obp ?? '-' },
      { label: 'RBI', value: b.rbi ?? '-' }
    ];
  }
  const p = pitching.value;
  return [
    { label: 'ERA', value: p.era ?? '-' },
    { label: 'WHIP', value: p.whip ?? '-' },
    { label: 'SO', value: p.pitchSo ?? '-' }
  ];
});
const matrixItems = computed(() => {
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
  return [
    { label: '防御率 ERA', value: p.era ?? '-' },
    { label: 'WHIP', value: p.whip ?? '-' },
    { label: '夺三振 SO', value: p.pitchSo ?? '-' },
    { label: '胜投 W', value: p.w ?? '-' },
    { label: '救援 SV', value: p.sv ?? '-' },
    { label: '投球局数 IP', value: p.ip ?? '-' }
  ];
});
const nextGame = computed(() => data.value?.nextGame || null);
const upcomingGames = computed(() => {
  const list = data.value?.schedule || [];
  return list.slice(nextGame.value ? 1 : 0);
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

function radarFoot(domain) {
  return {
    batting: '打击指标 · 同队同赛事',
    fielding: '守备指标 · 同队同赛事',
    pitching: '投球指标 · 越低越好已反向'
  }[domain] || '同队同赛事';
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
  if (!data.value) return;
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
}

function moveModule(index, direction) {
  const target = index + direction;
  if (target < 0 || target >= draft.modules.length) return;
  const [module] = draft.modules.splice(index, 1);
  draft.modules.splice(target, 0, module);
}

function moduleWidthLabel(span) {
  switch (span) {
    case 12: return '整行 12 列';
    case 8: return '宽卡 8 列';
    default: return '窄卡 4 列';
  }
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
  saving.value = true;
  try {
    await clubhouseApi.saveConfig({
      version: 2,
      layout: 'v3-modular',
      modules: draft.modules.map(({ key, enabled }) => ({ key, enabled })),
      performance: { ...draft.performance },
      ranks: draft.ranks.map((slot) => ({ metricKey: slot.metricKey }))
    });
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

function onTeamChange() {
  if (!data.value) return;
  eventId.value = scopeEvents.value[0]?.id ?? null;
  load();
}

function switchTeam(team) {
  teamId.value = team.id;
  onTeamChange();
}

async function load() {
  const seq = ++requestSeq;
  loading.value = true;
  loadError.value = '';
  try {
    const result = await clubhouseApi.dashboard({ teamId: teamId.value, eventId: eventId.value });
    if (seq !== requestSeq) return;
    if (!result) {
      await goUnclaimed();
      return;
    }
    data.value = result;
    teamId.value = result.scope?.teamId ?? null;
    eventId.value = result.scope?.eventId ?? null;
    resetDraft();
  } catch (error) {
    if (seq !== requestSeq) return;
    if (isAuthSessionError(error)) {
      await router.replace({
        name: 'PortalAccountLogin',
        params: { tenantCode: tenantCode.value },
        query: { redirect: router.currentRoute.value.fullPath }
      });
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
  if (scopeTeam.value?.id) router.push({ name: 'PortalTeamDetail', params: { tenantCode: tenantCode.value, id: scopeTeam.value.id } });
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
  load();
});
onBeforeUnmount(() => {
  destroyModuleSortable();
  document.documentElement.classList.remove('clubhouse-route');
});
</script>

<style scoped>
/* 页面主体沿用 V3 设计令牌；桌面 12 栏、手机单栏。 */
.clubhouse-page { max-width: 1280px; margin: 0 auto; padding: 24px 32px 56px; color: var(--bs-text); background: var(--bs-canvas); font-family: var(--bs-font-sans); }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.breadcrumb { font-size: 10px; color: var(--bs-text-faint); }
.breadcrumb b { color: var(--bs-text-muted); font-weight: 500; }
.page-head h1 { margin: 6px 0 0; font-size: 27px; line-height: 1.15; font-weight: 800; color: var(--bs-ink); }
.page-sub { margin: 6px 0 0; font-size: 11px; color: var(--bs-text-muted); }
.page-actions { display: flex; gap: 10px; }
.scope-bar { display: flex; align-items: center; gap: 9px; margin-bottom: 16px; padding: 9px 13px; background: var(--bs-surface); border: 1px solid var(--bs-hairline-soft); border-radius: 12px; box-shadow: var(--bs-shadow-sm); }
.scope-title { font-size: 8px; font-weight: 700; letter-spacing: .1em; color: var(--bs-accent-ink); white-space: nowrap; }
.scope-select { width: 148px; }
.scope-select :deep(.el-select__wrapper) { min-height: 30px; border-radius: 7px; box-shadow: 0 0 0 1px var(--bs-hairline) inset; background: var(--bs-surface-soft); }
.scope-hint { font-size: 8px; color: var(--bs-text-faint); margin-left: 4px; }
.scope-right { margin-left: auto; font-size: 9px; color: var(--bs-text-muted); white-space: nowrap; }
.scope-right b { color: var(--bs-ink); }

.hero { position: relative; isolation: isolate; overflow: hidden; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(420px, .85fr); gap: 24px; min-height: 430px; padding: 34px 40px; border-radius: 24px; color: #fff; background: radial-gradient(90% 100% at 90% -10%, rgba(201,164,91,.25), transparent 55%), radial-gradient(80% 100% at 0% 110%, rgba(18,54,95,.78), transparent 64%), linear-gradient(145deg, var(--bs-hero-start), var(--bs-hero-mid) 58%, var(--bs-hero-end)); border: 1px solid var(--bs-hairline); box-shadow: var(--bs-shadow-hero); }
.hero::before { content: ""; position: absolute; inset: 0; z-index: -1; opacity: .45; background: repeating-linear-gradient(115deg, transparent 0 42px, rgba(255,255,255,.018) 42px 43px), radial-gradient(circle at 80% 14%, rgba(255,255,255,.14) 0 1px, transparent 2px); background-size: auto, 28px 28px; }
.hero::after { content: ""; position: absolute; z-index: -1; right: -110px; top: -90px; width: 420px; height: 420px; border-radius: 50%; border: 1px solid rgba(201,164,91,.17); box-shadow: 0 0 0 48px rgba(201,164,91,.028), 0 0 0 108px rgba(201,164,91,.016); }
.hero-27 { position: absolute; z-index: -1; right: 2%; top: -34px; font-family: var(--bs-font-num); font-size: 400px; line-height: .8; font-weight: 800; letter-spacing: -.09em; color: transparent; -webkit-text-stroke: 1px rgba(255,255,255,.09); pointer-events: none; user-select: none; }
.hero-left { display: flex; flex-direction: column; justify-content: space-between; min-width: 0; }
.hero-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.hero-kicker { display: flex; align-items: center; gap: 9px; font-size: 9px; font-weight: 700; letter-spacing: .26em; color: var(--bs-accent-ink); }
.hero-kicker::before { content: ""; width: 26px; height: 1px; background: var(--bs-accent); }
.claim { display: inline-flex; align-items: center; gap: 5px; font-size: 9px; color: var(--bs-hero-muted); padding: 5px 9px; border-radius: 999px; border: 1px solid var(--bs-hero-card-border); background: var(--bs-hero-card); }
.claim i { width: 4px; height: 4px; border-radius: 50%; background: var(--bs-accent); box-shadow: 0 0 0 3px rgba(201,164,91,.15); }
.hero h1 { margin: 16px 0 0; font-family: var(--bs-font-serif); font-size: 66px; line-height: 1; font-weight: 700; letter-spacing: .005em; text-shadow: 0 6px 30px rgba(0,0,0,.3); }
.hero-en { margin: 10px 0 0; font-family: var(--bs-font-num); font-size: 10px; font-weight: 700; letter-spacing: .34em; color: var(--bs-hero-muted); }
.hero-intro { max-width: 470px; margin: 18px 0 0; font-size: 12px; line-height: 1.9; color: var(--bs-hero-muted); }
.hero-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 16px; }
.tag { font-size: 9px; padding: 6px 9px; color: rgba(255,255,255,.78); background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.08); border-radius: 999px; }
.hero-team { display: flex; align-items: center; gap: 10px; margin-top: 19px; padding-top: 14px; border-top: 1px solid var(--bs-hero-line); max-width: 470px; }
.hero-actions { display: flex; gap: 9px; margin-top: 16px; }
.hero-btn { height: 36px; padding: 0 16px; border-radius: 9px; display: inline-flex; align-items: center; justify-content: center; font-family: var(--bs-font-sans); font-size: 11px; font-weight: 700; cursor: pointer; transition: background 160ms ease-out, border-color 160ms ease-out; }
.hero-btn.primary { color: var(--bs-on-accent); border: 1px solid var(--bs-accent); background: linear-gradient(135deg, var(--bs-accent-hover), var(--bs-accent)); }
.hero-btn.primary:hover { background: var(--bs-accent-hover); }
.hero-btn.ghost { color: #fff; border: 1px solid rgba(255,255,255,.22); background: rgba(255,255,255,.06); }
.hero-btn.ghost:hover { border-color: rgba(255,255,255,.4); }
.hero-btn:focus-visible { outline: 2px solid var(--bs-accent-ink); outline-offset: 2px; }
.crest { width: 34px; height: 34px; border-radius: 9px; flex: 0 0 auto; display: grid; place-items: center; font-family: var(--bs-font-num); font-size: 11px; font-weight: 800; color: #071a33; background: linear-gradient(145deg, var(--bs-accent-soft), var(--bs-accent)); }
.crest.dark { color: #fff; background: linear-gradient(145deg, #173e62, #0a1b2e); border: 1px solid rgba(201,164,91,.5); }
.team-copy b { display: block; font-size: 12px; }
.team-copy span { display: block; margin-top: 2px; font-size: 9px; color: var(--bs-hero-muted); }

.card-stage { position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; min-height: 360px; perspective: 1200px; }
.card-back { position: absolute; width: 252px; height: 334px; border-radius: 15px; border: 1px solid rgba(201,164,91,.26); background: rgba(255,255,255,.035); backdrop-filter: blur(3px); }
.card-back.one { transform: rotate(7deg) translate(18px, 8px); }
.card-back.two { transform: rotate(-8deg) translate(-16px, 2px); opacity: .55; }
.player-card { position: relative; z-index: 3; width: 264px; height: 348px; padding: 18px 18px 16px; border-radius: 15px; border: 1px solid rgba(201,164,91,.55); background: radial-gradient(90% 70% at 84% 0%, rgba(201,164,91,.2), transparent 55%), repeating-linear-gradient(135deg, rgba(255,255,255,.025) 0 8px, transparent 8px 16px), linear-gradient(160deg, #0d2b4a, #081524 70%); box-shadow: 0 34px 70px rgba(0,0,0,.4); transform: rotate(-3deg); display: flex; flex-direction: column; justify-content: space-between; }
html.dark .player-card,
html[data-portal-theme="dark"] .player-card { background: radial-gradient(90% 70% at 84% 0%, rgba(212,175,106,.18), transparent 55%), repeating-linear-gradient(135deg, rgba(255,255,255,.025) 0 8px, transparent 8px 16px), linear-gradient(160deg, #132f4c, #0a1727 70%); border-color: rgba(212,175,106,.55); }
.player-card::before { content: ""; position: absolute; inset: 7px; border: 1px solid rgba(255,255,255,.08); border-radius: 10px; pointer-events: none; }
.card-top { display: flex; align-items: center; justify-content: space-between; }
.card-top span { font-size: 7px; font-weight: 700; letter-spacing: .2em; color: var(--bs-accent-ink); }
.card-top b { font-family: var(--bs-font-num); font-size: 8px; color: rgba(255,255,255,.45); }
.card-portrait { display: flex; flex-direction: column; align-items: center; margin-top: 9px; }
.card-avatar { width: 92px; height: 92px; border-radius: 50%; overflow: hidden; display: grid; place-items: center; font-family: var(--bs-font-serif); font-size: 38px; color: #f7ebd4; background: radial-gradient(circle at 35% 22%, rgba(255,255,255,.16), transparent 38%), repeating-linear-gradient(135deg, rgba(255,255,255,.045) 0 7px, transparent 7px 14px), linear-gradient(145deg, #214a74, #0a1c33); border: 1px solid rgba(201,164,91,.6); box-shadow: 0 0 0 5px rgba(201,164,91,.12), inset 0 0 24px rgba(0,0,0,.25); }
.card-avatar img { width: 100%; height: 100%; object-fit: cover; }
.card-name { margin-top: 12px; text-align: center; }
.card-name b { font-family: var(--bs-font-serif); font-size: 23px; font-weight: 700; letter-spacing: .04em; }
.card-name span { display: block; margin-top: 3px; font-family: var(--bs-font-num); font-size: 7px; letter-spacing: .24em; color: var(--bs-hero-muted); }
.card-team { display: flex; align-items: center; justify-content: center; gap: 7px; margin-top: 10px; }
.card-team .crest { width: 22px; height: 22px; border-radius: 6px; font-size: 7px; }
.card-team span:last-child { font-size: 8px; color: rgba(255,255,255,.65); }
.card-stats { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,.1); }
.card-stat { text-align: center; border-right: 1px solid rgba(255,255,255,.09); }
.card-stat:last-child { border-right: 0; }
.card-stat b { display: block; font-family: var(--bs-font-num); font-size: 19px; line-height: 1; color: #fff; }
.card-stat span { display: block; margin-top: 3px; font-size: 7px; color: var(--bs-hero-muted); }
.card-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 11px; padding-top: 9px; border-top: 1px solid rgba(255,255,255,.1); }
.card-foot span { font-size: 7px; color: var(--bs-hero-muted); }
.card-foot b { font-size: 7px; color: var(--bs-accent-ink); letter-spacing: .1em; }

.identity-band { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 18px; border-top: 1px solid var(--bs-hairline); border-bottom: 1px solid var(--bs-hairline); padding: 13px 0; }
.id-item { padding: 0 24px; border-right: 1px solid var(--bs-hairline); }
.id-item:first-child { padding-left: 0; }
.id-item:last-child { border-right: 0; padding-right: 0; }
.id-item span { display: block; font-size: 8px; color: var(--bs-text-faint); letter-spacing: .05em; }
.id-item b { display: block; margin-top: 4px; font-size: 11px; font-weight: 600; color: var(--bs-text); }
.id-item b em { font-style: normal; font-family: var(--bs-font-num); font-size: 14px; color: var(--bs-ink); }

.rank-band { margin-top: 22px; background: var(--bs-surface); border: 1px solid var(--bs-hairline-soft); border-radius: 18px; box-shadow: var(--bs-shadow-sm); padding: 18px 22px 17px; }
.rank-band-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; }
.rank-band-head h2 { margin: 5px 0 0; font-family: var(--bs-font-serif); font-size: 23px; line-height: 1.2; font-weight: 700; color: var(--bs-ink); }
.rank-band-head span { font-size: 9px; color: var(--bs-text-faint); }
.rank-scope a { color: var(--bs-accent-ink); font-weight: 700; margin-left: 5px; }
.rank-band-grid { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 15px; }
.rank-feature { padding: 0 24px; border-right: 1px solid var(--bs-hairline-soft); }
.rank-feature:first-child { padding-left: 0; }
.rank-feature:last-child { border-right: 0; padding-right: 0; }
.rank-feature .line { display: flex; align-items: baseline; gap: 8px; }
.rank-feature .no { font-family: var(--bs-font-num); font-size: 36px; line-height: .9; font-weight: 700; letter-spacing: -.04em; color: var(--bs-ink); }
.rank-feature .no small { font-size: 11px; color: var(--bs-text-faint); font-weight: 500; letter-spacing: 0; }
.rank-feature .value { margin-left: auto; font-family: var(--bs-font-num); font-size: 17px; font-weight: 700; color: var(--bs-ink); }
.rank-feature .metric { margin-top: 8px; font-size: 9px; font-weight: 700; color: var(--bs-text); }
.rank-feature .metric span { display: block; margin-top: 2px; font-size: 8px; font-weight: 400; color: var(--bs-text-faint); }
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
.radar-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 8px; font-size: 10px; color: var(--bs-text-muted); }
.radar-foot b { color: var(--bs-text-muted); font-weight: 500; }
.radar-empty { display: grid; place-items: center; align-content: center; min-height: 270px; text-align: center; }
.radar-empty b { font-family: var(--bs-font-num); font-size: 30px; font-weight: 700; color: var(--bs-text-faint); }
.radar-empty span { margin-top: 6px; font-size: 11px; color: var(--bs-text-muted); }
.eval-note { display: flex; align-items: flex-start; gap: 8px; margin: 11px 0 0; padding: 9px 11px; border: 1px dashed var(--bs-hairline); border-radius: 8px; background: var(--bs-surface-soft); font-size: 11px; line-height: 1.7; color: var(--bs-text-muted); }
.eval-note i { flex: 0 0 auto; width: 5px; height: 5px; margin-top: 4px; border-radius: 50%; background: var(--bs-accent); }
.eval-note span { flex: 1; }

@media (max-width: 1120px) {
  .radar-grid.three-up { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

.dashboard-grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 20px; align-items: start; margin-top: 20px; width: 100%; min-width: 0; }
.dashboard-grid .panel,
.dashboard-grid .rank-band { min-width: 0; }
.dashboard-grid .span-12 { grid-column: span 12; }
.dashboard-grid .span-8 { grid-column: span 8; }
.dashboard-grid .span-4 { grid-column: span 4; }
.dashboard-grid .rank-band,
.dashboard-grid .evaluation-panel { margin-top: 0; }
.content-grid, .main-col, .side-col { display: contents; }
.dashboard-empty { margin-top: 20px; padding: 46px 20px; text-align: center; }
.dashboard-empty h3 { margin: 8px 0; font-family: var(--bs-font-serif); font-size: 22px; color: var(--bs-ink); }
.dashboard-empty p { margin: 0 0 16px; font-size: 11px; color: var(--bs-text-muted); }
.panel { background: var(--bs-surface); border: 1px solid var(--bs-hairline-soft); border-radius: 18px; box-shadow: var(--bs-shadow-sm); padding: 19px 20px; }
.panel-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin-bottom: 13px; }
.kicker { display: block; font-size: 8px; font-weight: 700; letter-spacing: .16em; color: var(--bs-accent-ink); text-transform: uppercase; }
.panel-head h3 { margin: 5px 0 0; font-family: var(--bs-font-serif); font-size: 20px; line-height: 1.2; font-weight: 700; color: var(--bs-ink); }
.panel-head h3 small { font-family: var(--bs-font-sans); font-size: 9px; color: var(--bs-text-faint); font-weight: 400; margin-left: 5px; }
.panel-head-actions { display: flex; align-items: center; gap: 14px; }
.chart-summary { text-align: right; }
.chart-summary b { display: block; font-family: var(--bs-font-num); font-size: 18px; color: var(--bs-ink); }
.chart-summary span { display: block; margin-top: 3px; font-size: 8px; color: var(--bs-text-faint); }
.chart-wrap { margin-top: 4px; }
.trend-chart { width: 100%; height: auto; overflow: visible; }
.trend-chart .grid { stroke: var(--bs-hairline-soft); stroke-width: 1; }
.trend-chart .axis { fill: var(--bs-text-faint); font-family: var(--bs-font-sans); font-size: 9px; }
.trend-chart .line { fill: none; stroke: var(--bs-chart-line); stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; }
.trend-chart .area { fill: var(--bs-chart-fill); }
.trend-chart .dot { fill: var(--bs-surface); stroke: var(--bs-chart-line); stroke-width: 2; }
.trend-chart .bar { fill: var(--bs-chart-1); opacity: .9; }
.chart-note { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 5px; font-size: 8px; color: var(--bs-text-muted); }
.chart-note b { color: var(--bs-text-faint); font-weight: 500; }
.data-empty { padding: 24px 0; text-align: center; font-size: 11px; color: var(--bs-text-muted); }
.kpi-view { padding: 24px 0 18px; }
.kpi-view b { display: block; font-family: var(--bs-font-num); font-size: 64px; line-height: 1; color: var(--bs-ink); letter-spacing: -.04em; }
.kpi-view span { display: block; margin-top: 8px; font-size: 11px; color: var(--bs-text-muted); }
.donut-wrap { display: flex; align-items: center; gap: 28px; padding: 18px 0 12px; }
.donut { width: 150px; height: 150px; border-radius: 50%; display: grid; place-items: center; flex: 0 0 auto; }
.donut-center { width: 104px; height: 104px; border-radius: 50%; display: grid; place-items: center; align-content: center; background: var(--bs-surface); }
.donut-center b { font-family: var(--bs-font-num); font-size: 26px; color: var(--bs-ink); }
.donut-center span { margin-top: 3px; font-size: 8px; color: var(--bs-text-faint); }
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
.matrix-item span { display: block; margin-top: 4px; font-size: 8px; color: var(--bs-text-muted); }

.next-game { position: relative; overflow: hidden; background: linear-gradient(145deg, var(--bs-hero-start), var(--bs-hero-mid) 68%); border-radius: 14px; padding: 17px 19px; color: #fff; border: 1px solid var(--bs-hairline); }
.next-game::after { content: ""; position: absolute; right: -50px; bottom: -76px; width: 190px; height: 190px; border-radius: 50%; border: 1px solid rgba(201,164,91,.17); box-shadow: 0 0 0 24px rgba(201,164,91,.035), 0 0 0 54px rgba(201,164,91,.018); }
.ng-head { display: flex; align-items: center; justify-content: space-between; position: relative; z-index: 1; }
.event-pill { display: inline-flex; align-items: center; padding: 4px 8px; border-radius: 5px; font-size: 8px; font-weight: 700; color: #24180a; background: linear-gradient(135deg, var(--bs-accent-hover), var(--bs-accent)); }
.countdown { font-size: 9px; color: var(--bs-hero-muted); }
.ng-body { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 18px; margin-top: 16px; position: relative; z-index: 1; }
.ng-side { display: flex; align-items: center; gap: 10px; min-width: 0; }
.ng-side.right { justify-content: flex-end; text-align: right; }
.ng-side .crest { width: 38px; height: 38px; border-radius: 11px; }
.ng-side b { display: block; font-size: 13px; font-weight: 700; line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ng-side span:last-child { display: block; margin-top: 2px; font-size: 8px; color: var(--bs-hero-muted); }
.ng-vs { font-family: var(--bs-font-num); font-size: 13px; font-weight: 800; color: var(--bs-accent-ink); letter-spacing: .08em; }
.ng-foot { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 14px; padding-top: 10px; border-top: 1px solid var(--bs-hero-line); position: relative; z-index: 1; }
.ng-meta { display: flex; align-items: center; gap: 18px; font-size: 9px; color: var(--bs-hero-muted); }
.ng-meta b { font-family: var(--bs-font-num); font-size: 11px; color: #fff; }
.ng-foot :deep(.el-button) { color: var(--bs-accent-ink); font-size: 9px; }
.schedule-list { margin-top: 4px; }
.schedule-row { display: grid; grid-template-columns: 58px 1fr auto auto; align-items: center; gap: 14px; padding: 11px 0; border-bottom: 1px solid var(--bs-hairline-soft); }
.schedule-row:last-child { border-bottom: 0; padding-bottom: 0; }
.sch-date { font-family: var(--bs-font-num); font-size: 12px; font-weight: 700; color: var(--bs-ink); }
.sch-date span { display: block; font-family: var(--bs-font-sans); font-size: 8px; color: var(--bs-text-faint); margin-top: 1px; }
.sch-main b { display: block; font-size: 10px; font-weight: 700; color: var(--bs-text); }
.sch-main span { display: block; margin-top: 2px; font-size: 8px; color: var(--bs-text-muted); }
.sch-event { font-size: 8px; font-weight: 700; color: var(--bs-accent-ink); background: var(--bs-accent-soft); padding: 4px 6px; border-radius: 4px; white-space: nowrap; }
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
.result-top .date { font-size: 8px; color: var(--bs-text-faint); }
.res-badge { width: 20px; height: 20px; border-radius: 5px; display: grid; place-items: center; font-family: var(--bs-font-num); font-size: 8px; font-weight: 800; }
.res-badge.w { color: var(--bs-success); background: rgba(31,122,90,.1); }
.res-badge.l { color: var(--bs-danger); background: rgba(181,18,46,.1); }
.result-score { display: flex; align-items: baseline; gap: 6px; margin-top: 10px; }
.result-score b { font-family: var(--bs-font-num); font-size: 24px; line-height: 1; color: var(--bs-ink); }
.result-score span { font-size: 8px; color: var(--bs-text-faint); }
.result-opponent { margin-top: 5px; font-size: 10px; font-weight: 700; color: var(--bs-text); }
.result-meta { margin-top: 3px; font-size: 7px; color: var(--bs-text-faint); }
.result-line { margin-top: 9px; padding-top: 8px; border-top: 1px solid var(--bs-hairline-soft); font-size: 8px; line-height: 1.65; color: var(--bs-text-muted); }

.team-panel { position: relative; overflow: hidden; }
.rep-team { display: flex; align-items: center; gap: 10px; padding: 9px 0; border-bottom: 1px solid var(--bs-hairline-soft); cursor: pointer; }
.rep-team:last-of-type { border-bottom: 0; }
.rep-team.active { background: var(--bs-surface-soft); margin: 0 -8px; padding-left: 8px; padding-right: 8px; border-radius: 8px; }
.crest-lg { width: 38px; height: 38px; border-radius: 11px; display: grid; place-items: center; font-family: var(--bs-font-num); font-size: 12px; font-weight: 800; color: #071a33; background: linear-gradient(145deg, var(--bs-accent-soft), var(--bs-accent)); }
.rep-name { flex: 1; min-width: 0; }
.rep-name b { display: block; font-size: 12px; font-weight: 800; color: var(--bs-ink); }
.rep-name span { display: block; margin-top: 2px; font-size: 8px; color: var(--bs-text-muted); }
.rep-stat { text-align: right; }
.rep-stat b { display: block; font-family: var(--bs-font-num); font-size: 14px; color: var(--bs-ink); }
.rep-stat span { display: block; margin-top: 2px; font-size: 7px; color: var(--bs-text-faint); }
.rep-scope { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 10px; padding-top: 9px; border-top: 1px solid var(--bs-hairline-soft); font-size: 8px; color: var(--bs-text-muted); }
.about-intro { margin: 0; font-size: 10px; line-height: 1.85; color: var(--bs-text-muted); }
.about-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; margin-top: 14px; padding-top: 13px; border-top: 1px solid var(--bs-hairline-soft); }
.about-meta span { display: block; font-size: 8px; color: var(--bs-text-faint); }
.about-meta b { display: block; margin-top: 3px; font-size: 10px; font-weight: 600; color: var(--bs-text); }
.shortcut-list { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.shortcut { padding: 5px 0 !important; font-size: 10px; }
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

/* 手机端：单列，玩家卡全宽，图表/矩阵/赛程堆叠。 */
@media (max-width: 980px) {
  .clubhouse-page { padding: 18px 16px 40px; }
  .hero { grid-template-columns: 1fr; min-height: 0; padding: 22px 18px; }
  .hero h1 { font-size: 44px; }
  .hero-27 { font-size: 260px; right: -20px; }
  .card-stage { min-height: 0; margin-top: 20px; justify-content: flex-start; }
  .card-back { display: none; }
  .player-card { width: 100%; max-width: 330px; height: auto; min-height: 300px; transform: none; }
  .identity-band { grid-template-columns: 1fr 1fr; }
  .id-item { padding: 11px 14px; border-bottom: 1px solid var(--bs-hairline); }
  .id-item:nth-child(odd) { padding-left: 0; border-right: 1px solid var(--bs-hairline); }
  .id-item:nth-child(even) { padding-right: 0; }
  .id-item:nth-last-child(-n+2) { border-bottom: 0; }
  .rank-band { padding: 16px; }
  .rank-band-grid { grid-template-columns: 1fr 1fr; gap: 12px; }
  .rank-feature { padding: 0; border-right: 0; }
  .evaluation-panel { padding: 17px 16px 14px; }
  .eval-head { align-items: flex-start; flex-direction: column; gap: 8px; }
  .eval-meta { align-self: flex-end; }
  .radar-grid.two-up, .radar-grid.three-up { grid-template-columns: 1fr; }
  .radar-card { padding: 16px 15px 13px; }
  .dashboard-grid { grid-template-columns: 1fr; }
  .dashboard-grid .span-12,
  .dashboard-grid .span-8,
  .dashboard-grid .span-4 { grid-column: 1 / -1; }
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
