<template>
  <div ref="rootEl" class="intro-page" @click="onAnchorClick">
    <!-- Navigation -->
    <nav class="nav" :class="{ scrolled: navScrolled }">
      <div class="nav-inner">
        <a href="#top" class="logo">
          <div class="logo-mark">BSD</div>
          <div>
            <span class="logo-text">棒垒球数据平台</span>
            <span class="logo-sub">Baseball & Softball Database</span>
          </div>
        </a>
        <ul class="nav-links" :class="{ open: navOpen }">
          <li><a href="#pain">行业痛点</a></li>
          <li><a href="#features">核心功能</a></li>
          <li><a href="#roles">应用场景</a></li>
          <li><a href="#case">成功案例</a></li>
          <li><a href="#pricing">定价方案</a></li>
          <li><a href="#about">关于我们</a></li>
          <li><a href="#contact" class="nav-cta">立即咨询</a></li>
        </ul>
        <button class="nav-toggle" aria-label="菜单" @click="navOpen = !navOpen">☰</button>
      </div>
    </nav>

    <!-- Hero -->
    <section class="hero" id="top">
      <div class="hero-bg"></div>
      <div class="hero-diamond"></div>
      <div class="hero-inner">
        <div>
          <div class="hero-badge"><span class="hero-badge-dot"></span> 国内领先 · 专业级棒垒球 SaaS 平台</div>
          <h1>让每一场业余比赛<br>都有<em>职业级</em>数字化呈现</h1>
          <p class="hero-desc">BSD 是基于 SaaS 模式的棒垒球专属数字化平台，集实时记录、数据统计、深度分析、赛事直播与球队管理于一体，打通数据采集到展示的全链路。</p>
          <div class="hero-actions">
            <a href="#pricing" class="btn btn-primary">查看定价方案 →</a>
            <a :href="portalHref" class="btn btn-outline">查看系统</a>
          </div>
        </div>
        <div class="hero-stats">
          <div class="hero-stat fade-in">
            <div class="hero-stat-num">100<span>+</span></div>
            <div class="hero-stat-label">专业数据维度</div>
          </div>
          <div class="hero-stat fade-in">
            <div class="hero-stat-num">0</div>
            <div class="hero-stat-label">纸本记录门槛</div>
          </div>
          <div class="hero-stat fade-in">
            <div class="hero-stat-num">∞</div>
            <div class="hero-stat-label">云端永久存档</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Pain Points -->
    <section class="pain" id="pain">
      <div class="section-inner">
        <div class="section-header fade-in">
          <span class="section-tag">01 · 市场洞察</span>
          <h2 class="section-title">传统记录方式的困境</h2>
          <p class="section-desc">纸本 Scorebook 门槛高、数据难沉淀、传播受限——国内棒垒球数字化亟需破局。</p>
        </div>
        <div class="pain-grid">
          <div class="pain-card fade-in">
            <div class="pain-icon">📝</div>
            <h3>效率低下</h3>
            <p>传统纸本记录流程繁琐，需专业人员培训。比赛节奏瞬息万变，人工手写极易疏漏，基础数据误差频发。</p>
          </div>
          <div class="pain-card fade-in">
            <div class="pain-icon">🏝️</div>
            <h3>数据孤岛</h3>
            <p>分散在纸质记录册中的数据难以汇总，跨场次对比与长期共享无从谈起，无法形成可复用的历史档案。</p>
          </div>
          <div class="pain-card fade-in">
            <div class="pain-icon">📊</div>
            <h3>缺乏深度分析</h3>
            <p>仅能记录基础比分，无法提供进阶统计与落点球分析。教练难以基于数据制定科学训练计划。</p>
          </div>
          <div class="pain-card fade-in">
            <div class="pain-icon">📡</div>
            <h3>信息传播受限</h3>
            <p>比赛进程无法实时同步给远方亲友与球迷，赛事影响力局限于现场，精彩瞬间难以二次传播。</p>
          </div>
          <div class="pain-card fade-in">
            <div class="pain-icon">🛡️</div>
            <h3>青少年保护缺失</h3>
            <p>无法精确追踪青少年投球数量与运动负荷，过度使用风险难以及时预警，影响运动生涯健康。</p>
          </div>
          <div class="pain-card fade-in">
            <div class="pain-icon">⏳</div>
            <h3>策略滞后</h3>
            <p>低效的记录方式导致关键数据资产流失，战术调整缺乏量化支撑，难以形成针对性竞争优势。</p>
          </div>
        </div>

        <div style="margin-top: 48px;" class="fade-in">
          <div class="compare-wrap">
            <table class="compare-table">
              <thead>
                <tr>
                  <th>对比维度</th>
                  <th>传统纸本 / 一般软件</th>
                  <th>BSD 数据系统</th>
                </tr>
              </thead>
              <tbody>
                <tr><td>记录门槛</td><td>需专业背景，统计耗时</td><td>直观 UI，非专业人士亦可快速上手</td></tr>
                <tr><td>数据时效</td><td>赛后人工计算，发布慢</td><td>实时生成 Box Score，一键发布</td></tr>
                <tr><td>传播属性</td><td>封闭数据，难以分享</td><td>云端同步转播，专属球员卡分享</td></tr>
                <tr><td>分析深度</td><td>仅限基础数据</td><td>100+ 指标，含落点球图与战术洞察</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section id="features">
      <div class="section-inner">
        <div class="section-header fade-in">
          <span class="section-tag">02 · 核心功能</span>
          <h2 class="section-title">五大模块，覆盖赛事全生命周期</h2>
          <p class="section-desc">拒绝「简易记录」，追求「专业体验」——将每一场业余比赛转化为接近职业联赛般的数字化呈现。</p>
        </div>
        <div class="features-grid">
          <div class="feature-card fade-in">
            <div class="feature-num">01</div>
            <h3>专业级数位记分板</h3>
            <p>全面取代传统纸本记录，高效录入与精准同步。比赛终场自动生成专业 Box Score，无需人工二次核对。</p>
            <div class="feature-tags">
              <span class="tag">实时录入</span>
              <span class="tag">Box Score</span>
              <span class="tag">Pitch Count 投球保护</span>
            </div>
          </div>
          <div class="feature-card fade-in">
            <div class="feature-num">02</div>
            <h3>免费线上逐球转播</h3>
            <p>Play-by-Play 毫秒级文字直播，支持 1080P 影像推流接入。打破时空限制，让无法到场的球迷身临其境。</p>
            <div class="feature-tags">
              <span class="tag">文字直播</span>
              <span class="tag">视频推流</span>
            </div>
          </div>
          <div class="feature-card fade-in">
            <div class="feature-num">03</div>
            <h3>深度数据分析</h3>
            <p>100+ 专业数据维度，含 Spray Charts 落点球图与可视化图表。从打击率到进阶赛博计量学指标，为战术决策提供坚实基础。</p>
            <div class="feature-tags">
              <span class="tag">落点球图</span>
              <span class="tag">热力图</span>
              <span class="tag tag-beta">AI 阵容建议 · 测试中</span>
            </div>
          </div>
          <div class="feature-card fade-in">
            <div class="feature-num">04</div>
            <h3>球队管理系统</h3>
            <p>一站式赛程管理、球员大名单维护与出勤统计。赛程一键发布，球员名单智能汇入，大幅降低管理成本。</p>
            <div class="feature-tags">
              <span class="tag">赛程编排</span>
              <span class="tag">名单管理</span>
            </div>
          </div>
          <div class="feature-card fade-in feature-card-wide">
            <div class="feature-num">05</div>
            <h3>个人球员档案</h3>
            <p>为每位注册球员建立永久数字化生涯档案，完整记录从青年队到业余联赛的成长轨迹。精彩瞬间生成专属高光纪念，增强归属感与职业荣誉感。</p>
            <div class="feature-tags">
              <span class="tag">生涯数据</span>
              <span class="tag">高光时刻</span>
              <span class="tag">专属球员卡</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Roles -->
    <section class="roles" id="roles">
      <div class="section-inner">
        <div class="section-header fade-in">
          <span class="section-tag" style="color: var(--gold);">03 · 应用场景</span>
          <h2 class="section-title">为棒垒球生态的每一位参与者赋能</h2>
          <p class="section-desc">覆盖业余联赛、校园赛事、专业俱乐部及商业活动等多元场景。</p>
        </div>
        <div class="roles-grid">
          <div class="role-card fade-in">
            <div class="role-emoji">🧢</div>
            <h3>教练 · 战术智囊团</h3>
            <p>依托专业数据模型拆解对手特点，精准制定训练计划，临场快速调整策略，让每一次部署有据可依。</p>
          </div>
          <div class="role-card fade-in">
            <div class="role-emoji">⚾</div>
            <h3>球员 · 成长记录者</h3>
            <p>全维度记录技术表现与数据变化，清晰呈现成长轨迹，激发竞争意识与训练主动性。</p>
          </div>
          <div class="role-card fade-in">
            <div class="role-emoji">👨‍👩‍👦</div>
            <h3>家长 · 赛场之窗</h3>
            <p>无需亲临现场，实时关注孩子的赛场表现与成长瞬间，让亲情陪伴跨越物理距离。</p>
          </div>
          <div class="role-card fade-in">
            <div class="role-emoji">🏆</div>
            <h3>主办方 · 品质引擎</h3>
            <p>数字化重构赛事运营体系，简化执行流程，挖掘数据价值，持续提升赛事专业度与商业影响力。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Case Study -->
    <section class="case" id="case">
      <div class="section-inner">
        <div class="section-header fade-in">
          <span class="section-tag">04 · 成功案例</span>
          <h2 class="section-title">花城联盟的数字化实践</h2>
          <p class="section-desc">BSD 已成功赋能粤港澳大湾区多家顶尖棒垒球俱乐部，打造区域联赛数字化范本。</p>
        </div>
        <div class="case-box fade-in">
          <div>
            <p style="font-size: 15px; color: var(--gray-600); line-height: 1.8; margin-bottom: 8px;">
              2026 SSK 花城联盟春季赛采用 BSD 平台实现赛事全周期数字化管理——从赛程编排、实时战报到赛后数据复盘，为运营者与参赛队伍提供连贯、流畅的赛事体验。
            </p>
            <div class="case-teams">
              <span class="case-team">广州 · 暴力羊</span>
              <span class="case-team">广州 · 不死鸟</span>
              <span class="case-team">深圳 · 野钻</span>
              <span class="case-team">深圳 · Hustlers</span>
              <span class="case-team">东莞 · Lancers</span>
            </div>
          </div>
          <div class="case-metrics">
            <div class="case-metric">
              <div class="case-metric-num">5+</div>
              <div class="case-metric-label">大湾区顶尖俱乐部</div>
            </div>
            <div class="case-metric">
              <div class="case-metric-num">全周期</div>
              <div class="case-metric-label">赛事数字化管理</div>
            </div>
            <div class="case-metric">
              <div class="case-metric-num">实时</div>
              <div class="case-metric-label">数据驱动排名</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Pricing -->
    <section id="pricing" style="background: var(--gray-50);">
      <div class="section-inner">
        <div class="section-header fade-in">
          <span class="section-tag">05 · 定价方案</span>
          <h2 class="section-title">灵活的按需付费，零门槛开始体验</h2>
          <p class="section-desc">消除前期重资产投入，让赛事组织者根据实际规模与阶段精准控制成本。</p>
        </div>

        <!-- Promo -->
        <div class="pricing-promo fade-in">
          <div class="promo-card event">
            <div class="promo-label">🎉 推广期特惠 · 赛事主办方</div>
            <div class="promo-price">¥500 <small>/ 首个赛会</small></div>
            <h3>不受限制的全功能试用</h3>
            <p>不论规模大小，首个赛事享完整体验。</p>
            <ul>
              <li>赛事设立与全流程管理</li>
              <li>联盟 / 球队 / 球员登录</li>
              <li>赛事实时录入系统完整使用</li>
              <li>赛事结果实时写入数据库</li>
            </ul>
          </div>
          <div class="promo-card club">
            <div class="promo-label">🎁 推广期特惠 · 俱乐部运营方</div>
            <div class="promo-price">6 个月 <small>免费试用</small></div>
            <h3>低成本开启数字化管理</h3>
            <p>试用期满后 ¥50/人/年。</p>
            <ul>
              <li>创立球队专属品牌页面</li>
              <li>球员档案数字化管理</li>
              <li>个人数据登录与录入</li>
              <li>实时录入功能试用期内不可用</li>
            </ul>
          </div>
        </div>

        <!-- Standard pricing -->
        <div class="pricing-grid fade-in">
          <div class="price-card">
            <div class="price-tier">免费推广版</div>
            <div class="price-amount">¥0</div>
            <p class="price-desc">零成本启动赛事数字化，适合小型活动与初次体验。</p>
            <ul class="price-features">
              <li><span class="check">✓</span> 赛事设立与球队管理</li>
              <li><span class="check">✓</span> 基础数据录入</li>
              <li><span class="cross">—</span> 实时比赛录入（体验版）</li>
              <li><span class="cross">—</span> 高级战术分析</li>
            </ul>
            <a href="#contact" class="price-btn price-btn-outline">免费体验</a>
          </div>
          <div class="price-card popular">
            <div class="popular-badge">推荐方案</div>
            <div class="price-tier">基础服务包</div>
            <div class="price-amount">¥2,000 <small>/ 赛会</small></div>
            <p class="price-desc">性价比之选，含首 50 名球员数据接入，满足常规赛事完整管理需求。</p>
            <ul class="price-features">
              <li><span class="check">✓</span> 赛事全流程管理</li>
              <li><span class="check">✓</span> 50 名球员档案 + 永久存档</li>
              <li><span class="check">✓</span> 赛后 2 周编辑窗口</li>
              <li><span class="cross">—</span> 实时录入（需另购）</li>
            </ul>
            <a href="#contact" class="price-btn price-btn-fill">立即开通</a>
          </div>
          <div class="price-card">
            <div class="price-tier">灵活增值项</div>
            <div class="price-amount">按需 <small>叠加</small></div>
            <p class="price-desc">弹性扩展，拒绝功能浪费。在基础包之上按需增购。</p>
            <ul class="price-features">
              <li><span class="check">✓</span> 球员扩容 ¥40/人（第 51 名起）</li>
              <li><span class="check">✓</span> 实时录入 ¥100/场</li>
              <li><span class="check">✓</span> 需赛前 7 天申报开通</li>
              <li><span class="check">✓</span> 无限人数 / 永久保存</li>
            </ul>
            <a href="#contact" class="price-btn price-btn-outline">咨询增值</a>
          </div>
          <div class="price-card">
            <div class="price-tier">企业定制版</div>
            <div class="price-amount">定制 <small>报价</small></div>
            <p class="price-desc">大型体育联盟、省级协会及教育机构的专属数字化解决方案。</p>
            <ul class="price-features">
              <li><span class="check">✓</span> 深度功能定制开发</li>
              <li><span class="check">✓</span> 私有化部署</li>
              <li><span class="check">✓</span> 独立数据接口</li>
              <li><span class="check">✓</span> 专属客服响应</li>
            </ul>
            <a href="#contact" class="price-btn price-btn-outline">获取报价</a>
          </div>
        </div>

        <div class="pricing-note fade-in">
          <strong>费用估算示例：</strong>一场 8 队 × 15 人（120 名球员）、20 场比赛的业余联赛 ≈ 基础 ¥2,000 + 球员扩容 70×¥40 = ¥2,800 + 实时录入 20×¥100 = ¥2,000，合计约 <strong>¥6,800/赛季</strong>。具体报价以商务对接为准，推广期价格请尽早锁定。
        </div>
      </div>
    </section>

    <!-- About -->
    <section id="about">
      <div class="section-inner">
        <div class="about-grid fade-in">
          <div>
            <span class="section-tag">06 · 关于我们</span>
            <blockquote class="about-quote">「既然没有完美的工具，那我们就自己创造一个！」</blockquote>
            <p class="about-text">我们是广州康胜体育，由资深棒垒球爱好者与行业从业者组成。从赛场征战到赛事运营，我们深知每一位运动者的真实需求。</p>
            <p class="about-text">在试用了市面上几乎所有相关记录软件后，始终没有一款工具能完美兼顾专业数据采集、操作便捷性与深度分析——BSD 应运而生。</p>
            <div class="about-company">🏟️ 广州康胜体育 · 源于热爱，不止于热爱</div>
          </div>
          <div class="about-visual">
            <div class="about-visual-icon">⚾</div>
            <h3>共创中国棒垒球数字化未来</h3>
            <p>以粤港澳大湾区为起点，逐步构建全国性棒垒球数据网络，让每一次挥棒都被精准记录，让每一项数据都产生价值。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Contact CTA -->
    <section class="cta" id="contact">
      <h2>加入 BSD，开启您的赛事数字化之旅</h2>
      <p>期待与您携手，共启竞技新未来</p>
      <div class="contact-grid">
        <a href="tel:13560023706" class="contact-item">
          <span class="contact-icon">📞</span>
          <div>
            <div class="contact-label">咨询热线</div>
            <div class="contact-value">135-6002-3706 徐先生</div>
          </div>
        </a>
        <a href="mailto:6990189@qq.com" class="contact-item">
          <span class="contact-icon">✉️</span>
          <div>
            <div class="contact-label">联系邮箱</div>
            <div class="contact-value">6990189@qq.com</div>
          </div>
        </a>
        <a href="mailto:claylee49@gmail.com" class="contact-item">
          <span class="contact-icon">✉️</span>
          <div>
            <div class="contact-label">联系邮箱</div>
            <div class="contact-value">claylee49@gmail.com</div>
          </div>
        </a>
      </div>
      <a href="mailto:6990189@qq.com?subject=BSD系统咨询" class="btn btn-primary" style="font-size: 16px; padding: 16px 36px;">获取专属报价方案 →</a>
    </section>

    <footer>
      <p>© 2026 <strong>BSD · Baseball & Softball Database</strong> — 版权所有 © 2026 广州康胜体育棒球<span v-if="appVersion"> · v{{ appVersion }}</span></p>
    </footer>

    <!-- Right dot navigation -->
    <nav class="dot-nav">
      <a
        v-for="s in NAV_SECTIONS"
        :key="s.id"
        :href="'#' + s.id"
        :data-label="s.label"
        :class="{ active: activeId === s.id }"
      >
        <span class="dot"></span><span class="dot-label">{{ s.label }}</span>
      </a>
    </nav>
  </div>
</template>

<script setup>
// Intro —— 新版系统介绍页（H27：官网介绍页完整移植为新版 SPA 页面；「系统介绍」卡与旧版介绍页「进入新版」均指此页）
// 版式沿用官网（src/styles/legacy/portal-intro.css，scoped）；交互（滚动阴影/移动菜单/fade-in/右侧锚点导航）在组件内实现。
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { DEFAULT_TENANT_CODE } from '../../utils/tenantRoute';

const router = useRouter();
// 「查看系统」直指新版门户首页（命名路由自动携带部署 base）
const portalHref = router.resolve({ name: 'PortalHome', params: { tenantCode: DEFAULT_TENANT_CODE } }).href;
const appVersion = import.meta.env.VITE_APP_VERSION || '';

const NAV_SECTIONS = [
  { id: 'top', label: '首页' },
  { id: 'pain', label: '行业痛点' },
  { id: 'features', label: '核心功能' },
  { id: 'roles', label: '应用场景' },
  { id: 'case', label: '成功案例' },
  { id: 'pricing', label: '定价方案' },
  { id: 'about', label: '关于我们' },
  { id: 'contact', label: '联系我们' }
];

const rootEl = ref(null);
const navScrolled = ref(false);
const navOpen = ref(false);
const activeId = ref('top');
let observer = null;

function onScroll() {
  navScrolled.value = window.scrollY > 20;
  const pos = window.scrollY + window.innerHeight / 2;
  let id = 'top';
  for (const s of NAV_SECTIONS) {
    const el = document.getElementById(s.id);
    if (el && el.offsetTop <= pos) id = s.id;
  }
  activeId.value = id;
}

// 页内锚点平滑滚动（替代官网的 html{scroll-behavior:smooth}，避免全局样式泄漏）
function onAnchorClick(e) {
  const a = e.target && e.target.closest ? e.target.closest('a[href^="#"]') : null;
  if (!a) return;
  const id = a.getAttribute('href').slice(1);
  const el = id && rootEl.value ? rootEl.value.querySelector('#' + id) : null;
  if (!el) return;
  e.preventDefault();
  navOpen.value = false;
  el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

onMounted(() => {
  window.scrollTo(0, 0);
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
  observer = new IntersectionObserver((entries) => {
    entries.forEach((en) => { if (en.isIntersecting) en.target.classList.add('visible'); });
  }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
  if (rootEl.value) rootEl.value.querySelectorAll('.fade-in').forEach((el) => observer.observe(el));
});

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll);
  if (observer) observer.disconnect();
});
</script>

<style scoped src="../../styles/legacy/portal-intro.css"></style>
