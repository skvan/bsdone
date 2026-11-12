<template>
  <Teleport to="body">
    <div v-if="visible" class="lgpm" :class="{ 'lgpm--no-icons': !showMenuOptionIcons }">
      <div class="lgpm__backdrop" @click="emit('close')"></div>
      <div
        ref="panelEl"
        class="lgpm__panel"
        :class="{ 'floating-panel--dragging': dragging }"
        :style="panelStyle"
        role="dialog"
        :aria-label="ariaLabel"
        @click.stop
      >
        <header class="lgpm__head" @mousedown="onHeaderMouseDown" @dblclick="onHeaderDblClick">
          <div class="lgpm__head-left">
            <img class="lgpm__ball" :src="ballIcon" alt="">
            <div class="lgpm__title-wrap">
              <h2 class="lgpm__title">投球与裁定</h2>
              <span class="lgpm__title-en">PITCH &amp; RULING</span>
            </div>
          </div>
          <div class="lgpm__state">
            <span v-if="inning > 0" class="lgpm__state-chip">第 {{ inning }} 局{{ isTop ? '上' : '下' }}</span>
            <span class="lgpm__count lgpm__count--b">B <b>{{ balls }}</b></span>
            <span class="lgpm__count lgpm__count--s">S <b>{{ strikes }}</b></span>
            <span class="lgpm__count lgpm__count--o">O <b>{{ currentOuts }}</b></span>
          </div>
          <div class="lgpm__actions">
            <button type="button" class="lgpm__iconbtn" :disabled="!canUndo" title="撤销上一步" @click="emit('undo')">↩</button>
            <button type="button" class="lgpm__iconbtn" title="关闭" @click="emit('close')">✕</button>
          </div>
        </header>

        <div class="lgpm__body">
          <!-- 投球判定 -->
          <section class="lgpm__col lgpm__col--pitch" :class="{ 'lgpm__col--active': activeColumn === 'pitch' }">
            <template v-if="pitchDrill">
              <div class="lgpm__col-head lgpm__col-head--crumb">
                <button type="button" class="lgpm__crumb-back" title="返回投球判定" @click="backPitch">‹</button>
                <div class="lgpm__crumb">
                  <div class="lgpm__crumb-line">
                    <span class="lgpm__crumb-parent">投球判定</span>
                    <span class="lgpm__crumb-sep">›</span>
                    <span class="lgpm__crumb-current">{{ pitchDrill.title }}</span>
                  </div>
                  <div class="lgpm__crumb-sub">{{ pitchDrill.sub }}</div>
                </div>
                <span class="lgpm__crumb-count">{{ pitchDrill.count }}</span>
              </div>
              <div v-if="pitchDrill.stage === 'choose'" class="lgpm__list">
                <button
                  v-for="choice in DK3_CHOICES"
                  :key="choice.key"
                  type="button"
                  class="lgpm__item"
                  @click="openDk3Choice(choice.key)"
                >
                  <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(choice)">{{ choice.glyph }}</span>
                  <span class="lgpm__copy">
                    <span class="lgpm__cn">{{ choice.cn }}</span>
                    <span class="lgpm__en">{{ choice.en }}</span>
                  </span>
                  <span class="lgpm__more">›</span>
                </button>
              </div>
              <div v-else class="lgpm__list lgpm__list--results">
                <button
                  v-for="item in dk3Results"
                  :key="item.cmd"
                  type="button"
                  class="lgpm__result"
                  :class="{ 'lgpm__result--danger': item.tone === 'danger' }"
                  :title="item.en"
                  @click="runCommand(item.cmd)"
                >
                  {{ item.cn }}
                </button>
              </div>
              <div class="lgpm__col-foot">{{ pitchDrill.foot }}</div>
            </template>
            <template v-else>
              <div class="lgpm__col-head">
                <span class="lgpm__col-title">投球判定</span>
                <span class="lgpm__col-en">PITCH CALL</span>
              </div>
              <div class="lgpm__list">
                <template v-for="item in pitchItems" :key="item.key">
                  <button v-if="!item.chips" type="button" class="lgpm__item" @click="runCommand(item.cmd)">
                    <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(item)">{{ item.glyph }}</span>
                    <span class="lgpm__copy">
                      <span class="lgpm__cn">{{ item.cn }}</span>
                      <span class="lgpm__en">{{ item.en }}</span>
                    </span>
                    <span v-if="item.badge" class="lgpm__badge">{{ item.badge }}</span>
                  </button>
                  <div v-else class="lgpm__item lgpm__item--static">
                    <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(item)">{{ item.glyph }}</span>
                    <span class="lgpm__copy">
                      <span class="lgpm__cn">{{ item.cn }}</span>
                      <span class="lgpm__en">{{ item.en }}</span>
                      <span class="lgpm__chips">
                        <button
                          v-for="chip in item.chips"
                          :key="chip.key"
                          type="button"
                          class="lgpm__chip"
                          :title="chip.full"
                          @click.stop="openDk3Choice(chip.key)"
                        >
                          {{ chip.label }}
                        </button>
                      </span>
                    </span>
                  </div>
                </template>
              </div>
            </template>
          </section>

          <!-- 界内球 -->
          <section class="lgpm__col lgpm__col--bip" :class="{ 'lgpm__col--active': activeColumn === 'bip' }">
            <template v-if="bipDrill">
              <div class="lgpm__col-head lgpm__col-head--crumb">
                <button type="button" class="lgpm__crumb-back" :title="'返回' + bipCrumb.backTitle" @click="backBip">‹</button>
                <div class="lgpm__crumb">
                  <div class="lgpm__crumb-line">
                    <span class="lgpm__crumb-parent">{{ bipCrumb.parent }}</span>
                    <span class="lgpm__crumb-sep">›</span>
                    <span class="lgpm__crumb-current">{{ bipCrumb.current }}</span>
                  </div>
                  <div class="lgpm__crumb-sub">{{ bipCrumb.sub }}</div>
                </div>
                <span class="lgpm__crumb-count">{{ bipCrumb.count }}</span>
              </div>

              <div v-if="bipStage === 'out'" class="lgpm__result-grid lgpm__result-grid--outs">
                <button
                  v-for="item in bipData.outs"
                  :key="item.cmd"
                  type="button"
                  class="lgpm__result lgpm__result--danger"
                  :title="item.en"
                  @click="runCommand(item.cmd)"
                >
                  {{ item.cn }}
                </button>
              </div>
              <div v-else-if="bipStage === 'sac'" class="lgpm__result-grid lgpm__result-grid--single">
                <button
                  v-for="item in BIP_SAC_ITEMS"
                  :key="item.cmd"
                  type="button"
                  class="lgpm__result lgpm__result--sub-static"
                  :title="item.en"
                  @click="runCommand(item.cmd)"
                >
                  {{ item.cn }}
                </button>
              </div>
              <div v-else class="lgpm__result-grid">
                <button type="button" class="lgpm__result lgpm__result--out" @click="openBipSub(bipData.outSub)">
                  <span>出局</span>
                  <span class="lgpm__result-more">›</span>
                </button>
                <button
                  v-for="(item, index) in bipResultItems"
                  :key="item.cmd || item.sub"
                  type="button"
                  class="lgpm__result"
                  :class="bipResultClass(item, index)"
                  :title="item.en"
                  @click="onBipResult(item)"
                >
                  <span>{{ item.cn }}</span>
                  <span v-if="item.kind === 'sub'" class="lgpm__result-more">›</span>
                </button>
              </div>
              <div class="lgpm__col-foot">{{ bipCrumb.foot }}</div>
            </template>
            <template v-else>
              <div class="lgpm__col-head">
                <span class="lgpm__col-title">界内球</span>
                <span class="lgpm__col-en">BALL IN PLAY</span>
              </div>
              <div class="lgpm__col-hint">选球种 → 记录结果</div>
              <div class="lgpm__list">
                <button
                  v-for="type in BIP_TYPES"
                  :key="type.key"
                  type="button"
                  class="lgpm__item"
                  :disabled="pitchPlayDisabled"
                  @click="openBipType(type.key)"
                >
                  <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(type)">{{ type.glyph }}</span>
                  <span class="lgpm__copy">
                    <span class="lgpm__cn">{{ type.cn }}</span>
                    <span class="lgpm__en">{{ type.en }}</span>
                  </span>
                  <span class="lgpm__more">›</span>
                </button>
              </div>
            </template>
          </section>

          <!-- 上垒与妨碍 -->
          <section class="lgpm__col lgpm__col--onbase">
            <div class="lgpm__col-head">
              <span class="lgpm__col-title">上垒与妨碍</span>
              <span class="lgpm__col-en">ON BASE</span>
            </div>
            <div class="lgpm__list">
              <template v-for="item in ONBASE_ROOT" :key="item.key || item.cmd">
                <button v-if="!item.chips" type="button" class="lgpm__item" @click="runCommand(item.cmd)">
                  <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(item)">{{ item.glyph }}</span>
                  <span class="lgpm__copy">
                    <span class="lgpm__cn" :class="cnToneClass(item)">{{ item.cn }}</span>
                    <span class="lgpm__en">{{ item.en }}</span>
                  </span>
                </button>
                <div v-else class="lgpm__item lgpm__item--static">
                  <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(item)">{{ item.glyph }}</span>
                  <span class="lgpm__copy">
                    <span class="lgpm__cn">{{ item.cn }}</span>
                    <span class="lgpm__en">{{ item.en }}</span>
                    <span class="lgpm__chips">
                      <button
                        v-for="chip in item.chips"
                        :key="chip.cmd"
                        type="button"
                        class="lgpm__chip"
                        :title="chip.full"
                        @click.stop="runCommand(chip.cmd)"
                      >
                        {{ chip.label }}
                      </button>
                    </span>
                  </span>
                </div>
              </template>
            </div>
          </section>

          <!-- 其他裁定 -->
          <section class="lgpm__col lgpm__col--other">
            <div class="lgpm__col-head">
              <span class="lgpm__col-title">其他裁定</span>
              <span class="lgpm__col-en">OTHER</span>
            </div>
            <div class="lgpm__list">
              <button
                v-for="item in otherRoot"
                :key="item.cmd"
                type="button"
                class="lgpm__item"
                @click="runCommand(item.cmd)"
              >
                <span v-if="showMenuOptionIcons" class="lgpm__glyph" aria-hidden="true" :class="glyphClass(item)">{{ item.glyph }}</span>
                <span class="lgpm__copy">
                  <span class="lgpm__cn" :class="cnToneClass(item)">{{ item.cn }}</span>
                  <span class="lgpm__en">{{ item.en }}</span>
                </span>
              </button>
            </div>
            <div class="lgpm__col-foot">条件项按当前局面显隐</div>
          </section>
        </div>

        <footer class="lgpm__foot">
          <span class="lgpm__foot-hint">点选项即记录 · 带 › 的进入下一步</span>
          <button type="button" class="lgpm__close" @click="emit('close')">关闭</button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  pitchPlayDisabled: { type: Boolean, default: false },
  showFoulTipOut: { type: Boolean, default: false },
  showDroppedThirdStrike: { type: Boolean, default: false },
  showDk3OtherRunnerOut: { type: Boolean, default: false },
  initialPanelStack: { type: Array, default: null },
  showMenuOptionIcons: { type: Boolean, default: true },
  gameMode: { type: String, default: 'BASEBALL' },
  canUndo: { type: Boolean, default: false },
  blockClickMsAfterOpen: { type: Number, default: 0 },
  centerOffsetX: { type: Number, default: 0 },
  runnerCountOnBase: { type: Number, default: 0 },
  currentOuts: { type: Number, default: 0 },
  firstBaseOccupied: { type: Boolean, default: false },
  secondBaseOccupied: { type: Boolean, default: false },
  runnerOnThird: { type: Boolean, default: false },
  inning: { type: Number, default: 0 },
  isTop: { type: Boolean, default: true },
  balls: { type: Number, default: 0 },
  strikes: { type: Number, default: 0 },
});

const emit = defineEmits(['close', 'command', 'undo']);
const ballIcon = '/bs-ball/icons/baseball-color.svg';

const PITCH_ROOT = [
  { key: 'ball', cmd: 'pitch:B', cn: '坏球', en: 'Ball', glyph: 'B' },
  { key: 'strike', cmd: 'pitch:S', cn: '判进好球', en: 'Called Strike', glyph: 'S' },
  { key: 'swing', cmd: 'pitch:SM', cn: '挥空', en: 'Swing & Miss', glyph: 'SM' },
  {
    key: 'dropped3',
    cn: '漏接三振',
    en: 'Dropped 3rd',
    glyph: 'K',
    chips: [
      { key: 'dropped_3rd_swing', label: '挥空', full: '挥空三振' },
      { key: 'dropped_3rd_look', label: '目送', full: '目送三振' },
    ],
  },
  { key: 'foul', cmd: 'pitch:F', cn: '界外球', en: 'Foul Ball', glyph: 'F' },
  {
    key: 'foulTip',
    cmd: 'pitch:FOUL_TIP_OUT',
    cn: '指尖接杀',
    en: 'Foul Tip Out',
    glyph: 'FT',
    badge: '两好球',
  },
];

const BIP_TYPES = [
  { key: 'bip_g', cn: '滚地球', en: 'Ground Ball', glyph: 'G' },
  { key: 'bip_hg', cn: '强袭滚地球', en: 'Hard Ground', glyph: 'HG' },
  { key: 'bip_fly', cn: '飞球', en: 'Fly Ball', glyph: 'F' },
  { key: 'bip_ld', cn: '平飞球', en: 'Line Drive', glyph: 'LD' },
  { key: 'bip_bunt', cn: '短打', en: 'Bunt', glyph: 'BU' },
  { key: 'bip_pop', cn: '内野高飞球', en: 'Pop Fly', glyph: 'PF' },
];

const DK3_CHOICES = [
  { key: 'dropped_3rd_swing', cn: '挥空三振', en: 'Swinging', glyph: 'SM' },
  { key: 'dropped_3rd_look', cn: '目送三振', en: 'Looking', glyph: 'S' },
];

const DK3_RESULTS = {
  dropped_3rd_swing: [
    { cmd: 'dk3:sw:wp', cn: '暴投上一垒', en: 'Safe on First (WP)' },
    { cmd: 'dk3:sw:pb', cn: '捕逸上一垒', en: 'Safe on First (PB)' },
    { cmd: 'dk3:sw:e', cn: '守备失误上一垒', en: 'Safe on First (E)' },
    { cmd: 'dk3:sw:o1', cn: '在一垒出局', en: 'Batter Out at 1st', tone: 'danger' },
  ],
  dropped_3rd_look: [
    { cmd: 'dk3:lk:wp', cn: '暴投上一垒', en: 'Safe on First (WP)' },
    { cmd: 'dk3:lk:pb', cn: '捕逸上一垒', en: 'Safe on First (PB)' },
    { cmd: 'dk3:lk:e', cn: '守备失误上一垒', en: 'Safe on First (E)' },
    { cmd: 'dk3:lk:o1', cn: '在一垒出局', en: 'Batter Out at 1st', tone: 'danger' },
  ],
};

const BIP_SAC_ITEMS = [
  { cmd: 'bip:b:sac:e', cn: '因失误安全上垒', en: 'Safe on Error' },
  { cmd: 'bip:b:sac:fc', cn: '因野手选择安全上垒', en: 'Safe on FC' },
];

const BIP_RESULTS = {
  bip_g: {
    cn: '滚地球',
    outSub: 'bip_g_bo',
    outs: [
      { cmd: 'bip:g:bo:1', cn: '一垒前出局', en: 'Out at 1st' },
      { cmd: 'bip:g:bo:2', cn: '二垒前出局', en: 'Out at 2nd' },
      { cmd: 'bip:g:bo:3', cn: '三垒前出局', en: 'Out at 3rd' },
      { cmd: 'bip:g:bo:h', cn: '本垒前出局', en: 'Out at Home' },
    ],
    items: [
      { cmd: 'bip:g:1b', cn: '一垒安打', en: 'Single' },
      { cmd: 'bip:g:2b', cn: '二垒安打', en: 'Double' },
      { cmd: 'bip:g:3b', cn: '三垒安打', en: 'Triple' },
      { cmd: 'bip:g:iphr', cn: '场内全垒打', en: 'In-the-park HR' },
      { cmd: 'bip:g:fc', cn: '野手选择', en: "Fielder's Choice", cond: 'runner' },
      { cmd: 'bip:g:dp', cn: '双杀', en: 'Double Play', cond: 'dp' },
      { cmd: 'bip:g:tp', cn: '三杀', en: 'Triple Play', cond: 'tp' },
      { cmd: 'bip:g:e', cn: '失误', en: 'Error' },
      { cmd: 'bip:g:f', cn: '界外球', en: 'Foul Ball' },
      { cmd: 'bip:g:bi', cn: '击球员妨碍', en: 'Batter Interference' },
    ],
  },
  bip_hg: {
    cn: '强袭滚地球',
    outSub: 'bip_hg_bo',
    outs: [
      { cmd: 'bip:hg:o1', cn: '一垒前出局', en: 'Out at 1st' },
      { cmd: 'bip:hg:o2', cn: '二垒前出局', en: 'Out at 2nd' },
      { cmd: 'bip:hg:o3', cn: '三垒前出局', en: 'Out at 3rd' },
      { cmd: 'bip:hg:oh', cn: '本垒前出局', en: 'Out at Home' },
    ],
    items: [
      { cmd: 'bip:hg:1b', cn: '一垒安打', en: 'Single' },
      { cmd: 'bip:hg:2b', cn: '二垒安打', en: 'Double' },
      { cmd: 'bip:hg:3b', cn: '三垒安打', en: 'Triple' },
      { cmd: 'bip:hg:iphr', cn: '场内全垒打', en: 'In-the-park HR' },
      { cmd: 'bip:hg:fc', cn: '野手选择', en: "Fielder's Choice", cond: 'runner' },
      { cmd: 'bip:hg:dp', cn: '双杀', en: 'Double Play', cond: 'dp' },
      { cmd: 'bip:hg:tp', cn: '三杀', en: 'Triple Play', cond: 'tp' },
      { cmd: 'bip:hg:e', cn: '失误', en: 'Error' },
      { cmd: 'bip:hg:f', cn: '界外球', en: 'Foul Ball' },
      { cmd: 'bip:hg:bi', cn: '击球员妨碍', en: 'Batter Interference' },
    ],
  },
  bip_fly: {
    cn: '飞球',
    outSub: 'bip_fly_bo',
    outs: [
      { cmd: 'bip:fly:o1', cn: '一垒前出局', en: 'Out at 1st' },
      { cmd: 'bip:fly:o2', cn: '二垒前出局', en: 'Out at 2nd' },
      { cmd: 'bip:fly:o3', cn: '三垒前出局', en: 'Out at 3rd' },
      { cmd: 'bip:fly:oh', cn: '本垒前出局', en: 'Out at Home' },
    ],
    items: [
      { cmd: 'bip:fly:sf', cn: '高飞牺牲（SF）', en: 'Sacrifice Fly', cond: 'sf' },
      { cmd: 'bip:fly:1b', cn: '一垒安打', en: 'Single' },
      { cmd: 'bip:fly:2b', cn: '二垒安打', en: 'Double' },
      { cmd: 'bip:fly:3b', cn: '三垒安打', en: 'Triple' },
      { cmd: 'bip:fly:hr', cn: '全垒打', en: 'Home Run' },
      { cmd: 'bip:fly:iphr', cn: '场内全垒打', en: 'In-the-park HR' },
      { cmd: 'bip:fly:fc', cn: '野手选择', en: "Fielder's Choice", cond: 'runner' },
      { cmd: 'bip:fly:dp', cn: '双杀', en: 'Double Play', cond: 'dp' },
      { cmd: 'bip:fly:tp', cn: '三杀', en: 'Triple Play', cond: 'tp' },
      { cmd: 'bip:fly:e', cn: '失误', en: 'Error' },
      { cmd: 'bip:fly:sac:e', cn: '失误 + 牺牲打', en: 'Error + Sac' },
      { cmd: 'bip:fly:f', cn: '界外球', en: 'Foul Ball' },
      { cmd: 'bip:fly:fe', cn: '界外球 + 失误', en: 'Foul Ball + Error' },
      { cmd: 'bip:fly:bi', cn: '击球员妨碍', en: 'Batter Interference' },
    ],
  },
  bip_ld: {
    cn: '平飞球',
    outSub: 'bip_ld_bo',
    outs: [
      { cmd: 'bip:ld:o1', cn: '一垒前出局', en: 'Out at 1st' },
      { cmd: 'bip:ld:o2', cn: '二垒前出局', en: 'Out at 2nd' },
      { cmd: 'bip:ld:o3', cn: '三垒前出局', en: 'Out at 3rd' },
      { cmd: 'bip:ld:oh', cn: '本垒前出局', en: 'Out at Home' },
    ],
    items: [
      { cmd: 'bip:ld:1b', cn: '一垒安打', en: 'Single' },
      { cmd: 'bip:ld:2b', cn: '二垒安打', en: 'Double' },
      { cmd: 'bip:ld:3b', cn: '三垒安打', en: 'Triple' },
      { cmd: 'bip:ld:hr', cn: '全垒打', en: 'Home Run' },
      { cmd: 'bip:ld:iphr', cn: '场内全垒打', en: 'In-the-park HR' },
      { cmd: 'bip:ld:fc', cn: '野手选择', en: "Fielder's Choice", cond: 'runner' },
      { cmd: 'bip:ld:dp', cn: '双杀', en: 'Double Play', cond: 'dp' },
      { cmd: 'bip:ld:tp', cn: '三杀', en: 'Triple Play', cond: 'tp' },
      { cmd: 'bip:ld:e', cn: '失误', en: 'Error' },
      { cmd: 'bip:ld:sac:e', cn: '失误 + 牺牲打', en: 'Error + Sac' },
      { cmd: 'bip:ld:f', cn: '界外球', en: 'Foul Ball' },
      { cmd: 'bip:ld:fe', cn: '界外球 + 失误', en: 'Foul Ball + Error' },
      { cmd: 'bip:ld:bi', cn: '击球员妨碍', en: 'Batter Interference' },
    ],
  },
  bip_bunt: {
    cn: '短打',
    outSub: 'bip_bunt_bo',
    outs: [
      { cmd: 'bip:b:o1', cn: '一垒前出局', en: 'Out at 1st' },
      { cmd: 'bip:b:o2', cn: '二垒前出局', en: 'Out at 2nd' },
      { cmd: 'bip:b:o3', cn: '三垒前出局', en: 'Out at 3rd' },
      { cmd: 'bip:b:oh', cn: '本垒前出局', en: 'Out at Home' },
    ],
    items: [
      { kind: 'sub', sub: 'bip_bunt_sac', cn: '牺牲短打（安全）', en: 'Sac Bunt (Safe)', cond: 'runner' },
      { cmd: 'bip:b:sh', cn: '牺牲触击（SH）', en: 'Sacrifice Bunt', cond: 'sh' },
      { cmd: 'bip:b:1b', cn: '一垒安打', en: 'Single' },
      { cmd: 'bip:b:2b', cn: '二垒安打', en: 'Double' },
      { cmd: 'bip:b:3b', cn: '三垒安打', en: 'Triple' },
      { cmd: 'bip:b:iphr', cn: '场内全垒打', en: 'In-the-park HR' },
      { cmd: 'bip:b:fc', cn: '野手选择', en: "Fielder's Choice", cond: 'runner' },
      { cmd: 'bip:b:dp', cn: '双杀', en: 'Double Play', cond: 'dp' },
      { cmd: 'bip:b:tp', cn: '三杀', en: 'Triple Play', cond: 'tp' },
      { cmd: 'bip:b:e', cn: '失误', en: 'Error' },
      { cmd: 'bip:b:f', cn: '界外球', en: 'Foul Ball' },
      { cmd: 'bip:b:fe', cn: '界外球 + 失误', en: 'Foul Ball + Error' },
      { cmd: 'bip:b:bi', cn: '击球员妨碍', en: 'Batter Interference' },
    ],
  },
  bip_pop: {
    cn: '内野高飞球',
    outSub: 'bip_pop_bo',
    outs: [
      { cmd: 'bip:pop:o1', cn: '一垒前出局', en: 'Out at 1st' },
      { cmd: 'bip:pop:o2', cn: '二垒前出局', en: 'Out at 2nd' },
      { cmd: 'bip:pop:o3', cn: '三垒前出局', en: 'Out at 3rd' },
      { cmd: 'bip:pop:oh', cn: '本垒前出局', en: 'Out at Home' },
    ],
    items: [
      { cmd: 'bip:pop:1b', cn: '一垒安打', en: 'Single' },
      { cmd: 'bip:pop:2b', cn: '二垒安打', en: 'Double' },
      { cmd: 'bip:pop:3b', cn: '三垒安打', en: 'Triple' },
      { cmd: 'bip:pop:hr', cn: '全垒打', en: 'Home Run' },
      { cmd: 'bip:pop:iphr', cn: '场内全垒打', en: 'In-the-park HR' },
      { cmd: 'bip:pop:fc', cn: '野手选择', en: "Fielder's Choice", cond: 'runner' },
      { cmd: 'bip:pop:dp', cn: '双杀', en: 'Double Play', cond: 'dp' },
      { cmd: 'bip:pop:tp', cn: '三杀', en: 'Triple Play', cond: 'tp' },
      { cmd: 'bip:pop:e', cn: '失误', en: 'Error' },
      { cmd: 'bip:pop:sac:e', cn: '失误 + 牺牲打', en: 'Error + Sac' },
      { cmd: 'bip:pop:f', cn: '界外球', en: 'Foul Ball' },
      { cmd: 'bip:pop:fe', cn: '界外球 + 失误', en: 'Foul Ball + Error' },
      { cmd: 'bip:pop:bi', cn: '击球员妨碍', en: 'Batter Interference' },
    ],
  },
};

const ONBASE_ROOT = [
  { cmd: 'pitch:HBP', cn: '触身球', en: 'Hit By Pitch', glyph: 'HP' },
  { cmd: 'pitch:INT_BALL', cn: '故意坏球', en: 'Intentional Ball', glyph: 'IB' },
  { cmd: 'walk:IBB', cn: '故意四坏保送', en: 'Intentional Walk', glyph: 'IW', cnTone: 'ok' },
  { cmd: 'extra:CI', cn: '捕手妨碍', en: 'C. Interference', glyph: 'CI', cnTone: 'warn' },
  {
    key: 'putOn',
    cn: '保送上垒',
    en: 'Put On Base',
    glyph: 'ON',
    chips: [
      { label: '一垒', full: '进一垒', cmd: 'puton:1' },
      { label: '二垒', full: '进二垒', cmd: 'puton:2' },
      { label: '三垒', full: '进三垒', cmd: 'puton:3' },
    ],
  },
];

const OTHER_ROOT = [
  { cmd: 'evt:BK', cn: '投手犯规 / 不合法投球', en: 'Balk / Illegal Pitch', glyph: 'BK' },
  { cmd: 'evt:COACH_TIMEOUT', cn: '教练暂停', en: 'Coach Timeout', glyph: 'TO' },
  {
    cmd: 'extra:BATTER_OUT_OTHER',
    cn: '出局（其他）',
    en: 'Batter Out: Other',
    glyph: 'BO',
    cnTone: 'danger',
    glyphTone: 'danger',
  },
];

const isSoftball = computed(() => String(props.gameMode ?? '').toUpperCase() === 'SOFTBALL');
const otherRoot = computed(() => OTHER_ROOT.map((item) => (
  item.cmd === 'evt:BK' && isSoftball.value
    ? { ...item, cn: '不合法投球', en: 'Illegal Pitch', glyph: 'IP' }
    : item
)));

const panelStack = ref(['root']);
const panelEl = ref(null);
const dragging = ref(false);
const panelWidth = ref(940);
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth);
let blockedUntil = 0;

const runnerOnBase = computed(() => Number(props.runnerCountOnBase ?? 0) >= 1);
const dpAllowed = computed(() => Number(props.currentOuts ?? 0) < 2 && !!props.firstBaseOccupied);
const tpAllowed = computed(
  () => Number(props.currentOuts ?? 0) === 0 && !!props.firstBaseOccupied && !!props.secondBaseOccupied,
);
const sfAllowed = computed(() => Number(props.currentOuts ?? 0) < 2 && !!props.runnerOnThird);
const shAllowed = computed(() => Number(props.currentOuts ?? 0) < 2 && runnerOnBase.value);

const pitchItems = computed(() =>
  PITCH_ROOT.filter((item) => {
    if (item.key === 'dropped3') return !!props.showDroppedThirdStrike;
    if (item.key === 'foulTip') return !!props.showFoulTipOut;
    return true;
  }),
);

const activeColumn = computed(() => {
  const route = panelStack.value[1];
  if (route === 'dropped_3rd') return 'pitch';
  if (route === 'bip') return 'bip';
  return '';
});

const pitchDrill = computed(() => {
  if (panelStack.value[1] !== 'dropped_3rd') return null;
  const variant = panelStack.value[2];
  const results = DK3_RESULTS[variant];
  if (!results) {
    return { stage: 'choose', title: '漏接三振', sub: '选择三振方式', count: DK3_CHOICES.length, foot: '先选三振方式' };
  }
  return {
    stage: 'results',
    title: variant === 'dropped_3rd_swing' ? '挥空' : '目送',
    sub: '选择三振结果',
    count: results.length,
    foot: '选择三振后的结果',
  };
});

const dk3Results = computed(() => DK3_RESULTS[panelStack.value[2]] ?? []);

const bipData = computed(() => {
  if (panelStack.value[1] !== 'bip') return null;
  return BIP_RESULTS[panelStack.value[2]] ?? null;
});
const bipDrill = computed(() => !!bipData.value);
const bipStage = computed(() => {
  if (!bipData.value) return '';
  const stage = panelStack.value[3];
  if (stage === bipData.value.outSub) return 'out';
  if (stage === 'bip_bunt_sac') return 'sac';
  return 'results';
});

const bipResultItems = computed(() => bipData.value?.items.filter((item) => condMet(item.cond)) ?? []);

const bipCrumb = computed(() => {
  const type = bipData.value;
  if (!type) return { parent: '界内球', current: '', sub: '', count: 0, foot: '', backTitle: '球种' };
  if (bipStage.value === 'out') {
    return {
      parent: `界内球 › ${type.cn}`,
      current: '出局',
      sub: '选择出局垒位',
      count: type.outs.length,
      foot: '选择出局发生的垒位',
      backTitle: type.cn,
    };
  }
  if (bipStage.value === 'sac') {
    return {
      parent: '界内球 › 短打',
      current: '牺牲短打',
      sub: '选择上垒方式',
      count: BIP_SAC_ITEMS.length,
      foot: '选择安全上垒的原因',
      backTitle: '短打结果',
    };
  }
  return {
    parent: '界内球',
    current: type.cn,
    sub: '选择打席结果',
    count: 1 + bipResultItems.value.length,
    foot: '点结果即记录；“出局”再选具体垒位',
    backTitle: '球种',
  };
});

const routeLabel = computed(() => {
  if (pitchDrill.value) {
    return pitchDrill.value.stage === 'choose' ? '漏接三振' : `漏接三振 > ${pitchDrill.value.title}`;
  }
  if (panelStack.value[1] !== 'bip') return '';
  const crumb = bipCrumb.value;
  if (!crumb.current) return '界内球';
  return `${crumb.parent.replace(' › ', ' > ')} > ${crumb.current}`;
});

const ariaLabel = computed(() => (routeLabel.value ? `投球与裁定，${routeLabel.value}` : '投球与裁定'));

function condMet(cond) {
  if (cond === 'runner') return runnerOnBase.value;
  if (cond === 'dp') return dpAllowed.value;
  if (cond === 'tp') return tpAllowed.value;
  if (cond === 'sf') return sfAllowed.value;
  if (cond === 'sh') return shAllowed.value;
  return true;
}

function glyphClass(item) {
  const sizeClass = item.glyph.length >= 2 ? 'lgpm__glyph--sm' : '';
  const toneClass = item.glyphTone ? `lgpm__glyph--${item.glyphTone}` : '';
  return [sizeClass, toneClass].filter(Boolean).join(' ');
}

function cnToneClass(item) {
  return item.cnTone ? `lgpm__cn--${item.cnTone}` : '';
}

function bipResultClass(item, index) {
  const singleCount = bipResultItems.value.filter((entry) => entry.kind !== 'sub').length;
  const isLastOddSingle = index === bipResultItems.value.length - 1 && singleCount % 2 === 1;
  return {
    'lgpm__result--wide': item.kind === 'sub' || isLastOddSingle,
    'lgpm__result--sub': item.kind === 'sub',
  };
}

function normalizeStack(stack) {
  if (!Array.isArray(stack) || !stack.length) return ['root'];
  if (stack[1] === 'dropped_3rd') {
    const variant = stack[2];
    if (DK3_RESULTS[variant]) return ['root', 'dropped_3rd', variant];
    return ['root', 'dropped_3rd'];
  }
  if (stack[1] === 'bip') {
    const type = stack[2];
    const data = BIP_RESULTS[type];
    if (!data) return ['root', 'bip'];
    const stage = stack[3];
    if (stage === data.outSub || (stage === 'bip_bunt_sac' && type === 'bip_bunt')) {
      return ['root', 'bip', type, stage];
    }
    return ['root', 'bip', type];
  }
  return ['root'];
}

function canRun() {
  return performance.now() >= blockedUntil;
}

function runCommand(cmd) {
  if (!cmd || !canRun()) return;
  emit('command', cmd);
  if (cmd === 'evt:COACH_TIMEOUT') emit('close');
}

function openDk3Choice(key) {
  panelStack.value = ['root', 'dropped_3rd', key];
  afterStackChange();
}

function backPitch() {
  panelStack.value = ['root'];
  afterStackChange();
}

function openBipType(key) {
  if (props.pitchPlayDisabled) return;
  if (!BIP_RESULTS[key]) return;
  panelStack.value = ['root', 'bip', key];
  afterStackChange();
}

function openBipSub(sub) {
  if (!sub) return;
  panelStack.value = [...panelStack.value, sub];
  afterStackChange();
}

function onBipResult(item) {
  if (!item) return;
  if (item.kind === 'sub') {
    openBipSub(item.sub);
    return;
  }
  runCommand(item.cmd);
}

function backBip() {
  const stack = panelStack.value;
  if (stack[3]) {
    panelStack.value = stack.slice(0, 3);
  } else {
    panelStack.value = ['root'];
  }
  afterStackChange();
}

function afterStackChange() {
  nextTick(() => {
    syncPanelWidth();
    clampCurrent();
  });
}

/* ---------- 浮层拖拽 ---------- */
const POSITION_KEY = 'bs-ball-panel-pos-pitch-menu';
const posX = ref(null);
const posY = ref(null);
let dragStartX = 0;
let dragStartY = 0;
let dragBaseX = 0;
let dragBaseY = 0;
let resizeTimer = null;

const panelStyle = computed(() => {
  if (posX.value === null || posY.value === null) {
    const width = panelWidth.value || 940;
    const center = viewportWidth.value / 2 + (Number(props.centerOffsetX) || 0);
    const left = Math.max(12, Math.min(center - width / 2, viewportWidth.value - width - 12));
    return { left: `${left}px`, top: '50%', transform: 'translateY(-50%)' };
  }
  return { left: `${posX.value}px`, top: `${posY.value}px` };
});

function syncPanelWidth() {
  const width = panelEl.value?.offsetWidth;
  if (width) panelWidth.value = width;
}

function panelSize() {
  return {
    width: panelEl.value?.offsetWidth || panelWidth.value || 940,
    height: panelEl.value?.offsetHeight || 520,
  };
}

function clampToViewport(x, y) {
  const { width, height } = panelSize();
  return {
    x: Math.max(0, Math.min(x, window.innerWidth - width)),
    y: Math.max(0, Math.min(y, window.innerHeight - height)),
  };
}

function clampCurrent() {
  if (posX.value === null || posY.value === null) return;
  const next = clampToViewport(posX.value, posY.value);
  posX.value = next.x;
  posY.value = next.y;
}

function readPosition() {
  try {
    const raw = localStorage.getItem(POSITION_KEY);
    if (!raw) return;
    const value = JSON.parse(raw);
    if (typeof value.x === 'number' && typeof value.y === 'number') {
      posX.value = value.x;
      posY.value = value.y;
    }
  } catch {
    /* ignore malformed stored position */
  }
}

function savePosition() {
  if (posX.value === null || posY.value === null) return;
  try {
    localStorage.setItem(POSITION_KEY, JSON.stringify({ x: posX.value, y: posY.value }));
  } catch {
    /* ignore storage errors */
  }
}

function isInteractiveTarget(target) {
  return !!target?.closest?.('button, [role="button"], .el-icon, input, select, textarea, a');
}

function onHeaderMouseDown(event) {
  if (!props.visible || !panelEl.value || isInteractiveTarget(event.target)) return;
  dragging.value = true;
  dragStartX = event.clientX;
  dragStartY = event.clientY;
  const rect = panelEl.value.getBoundingClientRect();
  dragBaseX = rect.left;
  dragBaseY = rect.top;
  if (posX.value === null || posY.value === null) {
    posX.value = rect.left;
    posY.value = rect.top;
  }
  event.preventDefault();
}

function onMouseMove(event) {
  if (!dragging.value) return;
  const next = clampToViewport(dragBaseX + event.clientX - dragStartX, dragBaseY + event.clientY - dragStartY);
  posX.value = next.x;
  posY.value = next.y;
}

function onMouseUp() {
  if (!dragging.value) return;
  dragging.value = false;
  savePosition();
}

function onHeaderDblClick(event) {
  if (isInteractiveTarget(event.target)) return;
  posX.value = null;
  posY.value = null;
  try {
    localStorage.removeItem(POSITION_KEY);
  } catch {
    /* ignore storage errors */
  }
}

function onWindowResize() {
  viewportWidth.value = window.innerWidth;
  if (resizeTimer != null) clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    resizeTimer = null;
    syncPanelWidth();
    clampCurrent();
    savePosition();
  }, 200);
}

watch(
  () => props.visible,
  (open) => {
    if (!open) {
      dragging.value = false;
      return;
    }
    panelStack.value = normalizeStack(props.initialPanelStack);
    blockedUntil = performance.now() + Math.max(0, Number(props.blockClickMsAfterOpen) || 0);
    nextTick(() => {
      syncPanelWidth();
      requestAnimationFrame(clampCurrent);
    });
  },
);

onMounted(() => {
  viewportWidth.value = window.innerWidth;
  readPosition();
  syncPanelWidth();
  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('mouseup', onMouseUp);
  window.addEventListener('resize', onWindowResize);
});

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove);
  document.removeEventListener('mouseup', onMouseUp);
  window.removeEventListener('resize', onWindowResize);
  if (resizeTimer != null) clearTimeout(resizeTimer);
});
</script>

<style scoped>
.lgpm {
  position: fixed;
  inset: 0;
  z-index: 3100;
  box-sizing: border-box;
  font-family: inherit;
}
.lgpm * {
  box-sizing: border-box;
}
.lgpm__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(1px);
}
.lgpm__panel {
  position: absolute;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: min(940px, calc(100vw - 24px));
  max-height: min(86vh, 760px);
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.28), 0 4px 16px rgba(15, 23, 42, 0.12);
  user-select: none;
}
.lgpm__panel.floating-panel--dragging {
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.36);
  transition: box-shadow 0.15s ease;
}
.lgpm__head {
  display: flex;
  flex: none;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: grab;
}
.lgpm__head:active {
  cursor: grabbing;
}
.lgpm__head-left {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 220px;
}
.lgpm__ball {
  width: 22px;
  height: 22px;
  flex: none;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.14));
}
.lgpm__title-wrap {
  min-width: 0;
}
.lgpm__title {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 0.01em;
  white-space: nowrap;
}
.lgpm__title-en {
  display: block;
  margin-top: 2px;
  color: var(--el-text-color-placeholder);
  font-size: 8.5px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.12em;
  white-space: nowrap;
}
.lgpm__state {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 auto;
}
.lgpm__state-chip {
  padding: 3px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 10.5px;
  line-height: 1.2;
  white-space: nowrap;
}
.lgpm__count {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-text-color-secondary);
  font-size: 10px;
  line-height: 1.2;
  white-space: nowrap;
}
.lgpm__count b {
  color: var(--el-text-color-primary);
  font-size: 11px;
}
.lgpm__count--b b {
  color: var(--el-color-primary);
}
.lgpm__count--o b {
  color: var(--el-color-danger);
}
.lgpm__actions {
  display: flex;
  justify-content: flex-end;
  gap: 2px;
  min-width: 220px;
}
.lgpm__iconbtn {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--el-text-color-secondary);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
}
.lgpm__iconbtn:hover:not(:disabled) {
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.lgpm__iconbtn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.lgpm__body {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
  background: var(--el-bg-color);
}
.lgpm__col {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-light);
}
.lgpm__col::before {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  background: var(--lgpm-accent, var(--el-color-primary));
}
.lgpm__col--pitch {
  --lgpm-accent: var(--el-color-primary);
}
.lgpm__col--bip {
  --lgpm-accent: var(--el-color-success);
}
.lgpm__col--onbase {
  --lgpm-accent: var(--el-color-warning);
}
.lgpm__col--other {
  --lgpm-accent: var(--el-color-info);
}
.lgpm__col--active {
  background: var(--el-bg-color);
}
.lgpm__col--pitch.lgpm__col--active {
  border-color: var(--el-color-primary-light-5);
  box-shadow: inset 0 0 0 1px var(--el-color-primary-light-7);
}
.lgpm__col--bip.lgpm__col--active {
  border-color: var(--el-color-success-light-5);
  box-shadow: inset 0 0 0 1px var(--el-color-success-light-7);
}
.lgpm__col-head {
  display: flex;
  align-items: baseline;
  gap: 7px;
  padding: 10px 11px 7px;
}
.lgpm__col-head--crumb {
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px 7px;
}
.lgpm__col-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}
.lgpm__col-en {
  overflow: hidden;
  color: var(--lgpm-accent, var(--el-color-primary));
  font-size: 8.5px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.09em;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lgpm__col-hint {
  margin-top: -3px;
  padding: 0 11px 6px;
  color: var(--el-text-color-placeholder);
  font-size: 9.5px;
  line-height: 1.3;
}
.lgpm__list {
  display: flex;
  flex-direction: column;
}
.lgpm__list--results {
  gap: 5px;
  padding: 0 10px 8px;
}
.lgpm__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  min-height: 42px;
  padding: 5px 9px 5px 10px;
  border: 0;
  border-top: 1px solid var(--el-border-color-lighter);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s ease, box-shadow 0.12s ease;
}
.lgpm__item:first-child {
  border-top: 0;
}
button.lgpm__item:hover:not(:disabled) {
  z-index: 1;
  border-radius: 7px;
  background: var(--el-bg-color);
  box-shadow: inset 0 0 0 1px var(--el-color-primary-light-7), 0 1px 3px rgba(64, 158, 255, 0.08);
}
.lgpm__item--static {
  cursor: default;
}
.lgpm__item:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.lgpm__glyph {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #6a6f77;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.01em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.28);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18), 0 1px 2px rgba(15, 23, 42, 0.14);
}
.lgpm__glyph--sm {
  font-size: 8.5px;
}
.lgpm--no-icons .lgpm__item {
  padding-left: 14px;
}
.lgpm__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.lgpm__cn {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lgpm__cn--ok {
  color: var(--el-color-success);
}
.lgpm__cn--warn {
  color: var(--el-color-warning);
}
.lgpm__cn--danger {
  color: var(--el-color-danger);
}
.lgpm__en {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-weight: 400;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lgpm__more {
  flex: none;
  margin-left: auto;
  color: var(--el-color-primary-light-3);
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
}
.lgpm__badge {
  flex: none;
  margin-left: auto;
  padding: 2px 5px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);
  color: var(--el-text-color-secondary);
  font-size: 9px;
  line-height: 1.2;
  white-space: nowrap;
}
.lgpm__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}
.lgpm__chip {
  padding: 2px 6px;
  border: 1px solid var(--el-border-color);
  border-radius: 5px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1.25;
  white-space: nowrap;
  cursor: pointer;
  transition: border-color 0.12s ease, color 0.12s ease, background 0.12s ease;
}
.lgpm__chip:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.lgpm__col-foot {
  margin-top: auto;
  padding: 7px 11px 8px;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-placeholder);
  font-size: 9.5px;
  line-height: 1.35;
}
.lgpm__crumb-back {
  flex: none;
  width: 23px;
  height: 23px;
  margin-top: 1px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
}
.lgpm__crumb-back:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}
.lgpm__crumb {
  flex: 1;
  min-width: 0;
}
.lgpm__crumb-line {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}
.lgpm__crumb-parent {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  text-overflow: ellipsis;
}
.lgpm__crumb-sep {
  flex: none;
  color: var(--el-text-color-placeholder);
  font-size: 10px;
}
.lgpm__crumb-current {
  overflow: hidden;
  text-overflow: ellipsis;
}
.lgpm__crumb-sub {
  margin-top: 2px;
  color: var(--el-text-color-placeholder);
  font-size: 9.5px;
  line-height: 1.2;
}
.lgpm__crumb-count {
  flex: none;
  margin-top: 3px;
  padding: 2px 5px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  background: var(--el-bg-color);
  color: var(--el-text-color-secondary);
  font-size: 9px;
  line-height: 1.15;
}
.lgpm__result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
  padding: 0 10px 8px;
}
.lgpm__result-grid--outs {
  padding-top: 2px;
}
.lgpm__result-grid--single {
  grid-template-columns: 1fr;
  padding-top: 2px;
}
.lgpm__result {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 32px;
  padding: 0 6px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.1;
  white-space: nowrap;
  cursor: pointer;
  transition: border-color 0.12s ease, color 0.12s ease, background 0.12s ease;
}
.lgpm__result:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.lgpm__result--wide {
  grid-column: 1 / -1;
}
.lgpm__result--danger,
.lgpm__result--out {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}
.lgpm__result--danger:hover,
.lgpm__result--out:hover {
  border-color: var(--el-color-danger);
  background: var(--el-color-danger-light-8);
  color: var(--el-color-danger);
}
.lgpm__result--out,
.lgpm__result--sub {
  justify-content: space-between;
  padding: 0 10px;
}
.lgpm__result--sub,
.lgpm__result--sub-static {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}
.lgpm__result--sub:hover,
.lgpm__result--sub-static:hover {
  border-color: var(--el-color-success);
  background: var(--el-color-success-light-8);
  color: var(--el-color-success);
}
.lgpm__result-more {
  color: currentColor;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  opacity: 0.7;
}
.lgpm__foot {
  display: flex;
  flex: none;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 14px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}
.lgpm__foot-hint {
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  line-height: 1.3;
}
.lgpm__close {
  margin-left: auto;
  padding: 5px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 7px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 11.5px;
  line-height: 1.2;
  cursor: pointer;
}
.lgpm__close:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

@media (max-width: 1180px) {
  .lgpm__panel {
    width: min(720px, calc(100vw - 24px));
  }
  .lgpm__body {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .lgpm__head-left,
  .lgpm__actions {
    min-width: 0;
  }
}
@media (max-width: 760px) {
  .lgpm__state {
    display: none;
  }
  .lgpm__head {
    height: 52px;
  }
  .lgpm__panel {
    width: calc(100vw - 16px);
    max-height: 88vh;
  }
  .lgpm__body {
    grid-template-columns: 1fr;
    padding: 10px;
  }
  .lgpm__title-en {
    display: none;
  }
  .lgpm__col-en {
    font-size: 8px;
  }
  .lgpm__head-left {
    flex: 1;
  }
  .lgpm__actions {
    flex: none;
  }
}
</style>
