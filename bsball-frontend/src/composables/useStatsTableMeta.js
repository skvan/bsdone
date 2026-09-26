// 统计表元数据（列/顺序/可见性/草稿/行构造）—— 行为移植自编译产物入口 chunk 组合式 ll（useStatsTableMeta）
// 由「区间切片 + 依赖映射重写」组装（勿手改内部短名逻辑）；外部依赖映射：
//   c→ref  E→computed  Dt→useI18n  G/ol→statsDisplayStorage 读写  li→formatInningsPitched
//   ae/te/ie/re/oe/ne→STATS_*_COLS（stores/statsColumns）  Jr→STATS_RATE_COLS  V→sortByOrder（本地逐字）
//   $t/Ua/...→KEY_* 常量（stores/statsColumns）  qa/Qa/Za→显示设置三键（valueControl）
import { ref as c, computed as E } from 'vue';
import { useI18n } from 'vue-i18n';
import { readLocalJson as G, writeLocalJson as ol } from '../utils/statsDisplayStorage';
import { formatInningsPitched as li } from '../utils/inningsFormat';
import {
  STATS_BATTING_COLS as ae, STATS_PITCHING_COLS as te, STATS_FIELDING_COLS as ie,
  TEAM_STATS_BATTING_COLS as re, TEAM_STATS_PITCHING_COLS as oe, TEAM_STATS_FIELDING_COLS as ne,
  STATS_RATE_COLS as Jr,
  KEY_BATTING_ORDER as $t, KEY_BATTING_VISIBILITY as Ua,
  KEY_PITCHING_ORDER as zt, KEY_PITCHING_VISIBILITY as Ya,
  KEY_FIELDING_ORDER as jt, KEY_FIELDING_VISIBILITY as Ka,
  KEY_TEAM_BATTING_ORDER as qt, KEY_TEAM_BATTING_VISIBILITY as $a,
  KEY_TEAM_PITCHING_ORDER as Qt, KEY_TEAM_PITCHING_VISIBILITY as za,
  KEY_TEAM_FIELDING_ORDER as Zt, KEY_TEAM_FIELDING_VISIBILITY as ja
} from '../stores/statsColumns';

const Dt = useI18n;
const qa = 'rateDisplayStyle';
const Qa = 'showTrailingZeros';
const Za = 'decimalPlaces';

// —— 状态（入口 chunk 同名 refs）——
const Ee = c(null), dt = c([]), Oe = c(null), ut = c([]), Re = c(null), pt = c([]),
  Fe = c(null), mt = c([]), Ge = c(null), ht = c([]), Me = c(null), ft = c([]),
  bt = c('leadingZero'), gt = c(true), vt = c(2),
  x = c(null), ot = c([]), W = c(null), rt = c([]), U = c(null), nt = c([]),
  Y = c(null), lt = c([]), K = c(null), st = c([]), $ = c(null), ct = c([]),
  aa = c('leadingZero'), ia = c(true), oa = c(2);
let si = false;

// —— 行构造/归一化（入口 chunk fe/be/en/tn）——
function fe(e, t) {
  if (t == null) return null;
  const a = new Set(t), o = [...t];
  for (const r of e) a.has(r.key) || o.push(r.key);
  return o;
}
function be(e, t) {
  const a = new Set(t), o = [...t];
  for (const r of e) a.has(r.key) || o.push(r.key);
  return o;
}
function en(e) {
  return e === 'dot' || e === 'leadingZero' ? e : 'leadingZero';
}
function tn(e) {
  return e === 2 || e === 3 ? e : 2;
}

// —— 排序（入口 chunk V）——
function V(e, t) {
  const a = new Map(e.map((l) => [l.key, l])), o = new Set(), r = [];
  for (const l of t) {
    const s = a.get(l);
    s && !o.has(l) && (r.push(s), o.add(l));
  }
  for (const l of e) o.has(l.key) || r.push(l);
  return r;
}

// —— 可见性判定/设置（入口 chunk un/pn/mn/hn/fn/bn/gn/vn/yn/Pn/Sn/Tn）——
function un(e) {
  const t = x.value;
  return t == null || t.includes(e);
}
function pn(e, t) {
  if (t) {
    if (x.value == null) return;
    x.value = [...new Set([...x.value, e])];
  } else x.value = x.value == null ? ae.map((a) => a.key).filter((a) => a !== e) : x.value.filter((a) => a !== e);
}
function mn(e) {
  const t = W.value;
  return t == null || t.includes(e);
}
function hn(e, t) {
  if (t) {
    if (W.value == null) return;
    W.value = [...new Set([...W.value, e])];
  } else W.value = W.value == null ? te.map((a) => a.key).filter((a) => a !== e) : W.value.filter((a) => a !== e);
}
function fn(e) {
  const t = U.value;
  return t == null || t.includes(e);
}
function bn(e, t) {
  if (t) {
    if (U.value == null) return;
    U.value = [...new Set([...U.value, e])];
  } else U.value = U.value == null ? ie.map((a) => a.key).filter((a) => a !== e) : U.value.filter((a) => a !== e);
}
function gn(e) {
  const t = Y.value;
  return t == null || t.includes(e);
}
function vn(e, t) {
  if (t) {
    if (Y.value == null) return;
    Y.value = [...new Set([...Y.value, e])];
  } else Y.value = Y.value == null ? re.map((a) => a.key).filter((a) => a !== e) : Y.value.filter((a) => a !== e);
}
function yn(e) {
  const t = K.value;
  return t == null || t.includes(e);
}
function Pn(e, t) {
  if (t) {
    if (K.value == null) return;
    K.value = [...new Set([...K.value, e])];
  } else K.value = K.value == null ? oe.map((a) => a.key).filter((a) => a !== e) : K.value.filter((a) => a !== e);
}
function Sn(e) {
  const t = $.value;
  return t == null || t.includes(e);
}
function Tn(e, t) {
  if (t) {
    if ($.value == null) return;
    $.value = [...new Set([...$.value, e])];
  } else $.value = $.value == null ? ne.map((a) => a.key).filter((a) => a !== e) : $.value.filter((a) => a !== e);
}

// —— 可见列（排序后，入口 chunk on/rn/nn/ln/sn/cn）——
const on = E(() => {
  le();
  const e = Ee.value;
  return V(e == null ? [...ae] : ae.filter((t) => e.includes(t.key)), dt.value);
});
const rn = E(() => {
  le();
  const e = Oe.value;
  return V(e == null ? [...te] : te.filter((t) => e.includes(t.key)), ut.value);
});
const nn = E(() => {
  le();
  const e = Re.value;
  return V(e == null ? [...ie] : ie.filter((t) => e.includes(t.key)), pt.value);
});
const ln = E(() => {
  le();
  const e = Fe.value;
  return V(e == null ? [...re] : re.filter((t) => e.includes(t.key)), mt.value);
});
const sn = E(() => {
  le();
  const e = Ge.value;
  return V(e == null ? [...oe] : oe.filter((t) => e.includes(t.key)), ht.value);
});
const cn = E(() => {
  le();
  const e = Me.value;
  return V(e == null ? [...ne] : ne.filter((t) => e.includes(t.key)), ft.value);
});

// —— 数列判定与格式化（入口 chunk Xr/dn）——
function Xr(e) {
  return Jr.has(e);
}
function dn(e) {
  if (e == null || e === '') return '-';
  const t = typeof e == 'number' ? e : parseFloat(String(e));
  if (!Number.isFinite(t)) return '-';
  const a = vt.value;
  let o = t.toFixed(a);
  return gt.value || (o = o.replace(/\.?0+$/, '')), bt.value === 'dot' && (o = o.startsWith('0.') ? '.' + o.slice(2) : o), o;
}

// —— 加载（入口 chunk ci → le）——
function ci() {
  Ee.value = fe(ae, G(Ua, null));
  dt.value = be(ae, G($t, ae.map((e) => e.key)));
  Oe.value = fe(te, G(Ya, null));
  ut.value = be(te, G(zt, te.map((e) => e.key)));
  Re.value = fe(ie, G(Ka, null));
  pt.value = be(ie, G(jt, ie.map((e) => e.key)));
  Fe.value = fe(re, G($a, null));
  mt.value = be(re, G(qt, re.map((e) => e.key)));
  Ge.value = fe(oe, G(za, null));
  ht.value = be(oe, G(Qt, oe.map((e) => e.key)));
  Me.value = fe(ne, G(ja, null));
  ft.value = be(ne, G(Zt, ne.map((e) => e.key)));
  bt.value = en(G(qa, 'leadingZero'));
  gt.value = G(Qa, true) === true;
  vt.value = tn(G(Za, 2));
  si = true;
}
function le() {
  si || ci();
}
function an() {
  le();
  x.value = Ee.value == null ? null : [...Ee.value];
  ot.value = [...dt.value];
  W.value = Oe.value == null ? null : [...Oe.value];
  rt.value = [...ut.value];
  U.value = Re.value == null ? null : [...Re.value];
  nt.value = [...pt.value];
  Y.value = Fe.value == null ? null : [...Fe.value];
  lt.value = [...mt.value];
  K.value = Ge.value == null ? null : [...Ge.value];
  st.value = [...ht.value];
  $.value = Me.value == null ? null : [...Me.value];
  ct.value = [...ft.value];
  aa.value = bt.value;
  ia.value = gt.value;
  oa.value = vt.value;
}

// —— 组合式主体（入口 chunk ll）——
export function useStatsTableMeta() {
  const { t: e } = Dt();
  le();
  const { te: t } = Dt();
  const a = { pitchSo: 'so', pitchBb: 'bb', pitchH: 'h', pitchHr: 'hr', pitchR: 'r' };
  function o(h) {
    const P = a[h] ?? h;
    const T = 'stats.colEn.' + P;
    if (t(T)) return e(T);
    const y = 'stats.col.' + P;
    return t(y) ? e(y) : String(h).toUpperCase();
  }
  function r(h) {
    const P = a[h] ?? h;
    const T = 'stats.col.' + P;
    if (t(T)) return e(T);
    const y = 'stats.colEn.' + P;
    return t(y) ? e(y) : String(h);
  }
  const l = E(() => V(ae, ot.value));
  const s = E(() => V(te, rt.value));
  const b = E(() => V(ie, nt.value));
  const d = E(() => V(re, lt.value));
  const L = E(() => V(oe, st.value));
  const _ = E(() => V(ne, ct.value));
  return {
    STATS_BATTING_COLS: ae,
    STATS_PITCHING_COLS: te,
    STATS_FIELDING_COLS: ie,
    TEAM_STATS_BATTING_COLS: re,
    TEAM_STATS_PITCHING_COLS: oe,
    TEAM_STATS_FIELDING_COLS: ne,
    isStatsRateCol: Xr,
    formatStatsRate: dn,
    draftBattingOrder: ot,
    draftBattingVisibility: x,
    draftPitchingOrder: rt,
    draftPitchingVisibility: W,
    draftFieldingOrder: nt,
    draftFieldingVisibility: U,
    draftTeamBattingOrder: lt,
    draftTeamBattingVisibility: Y,
    draftTeamPitchingOrder: st,
    draftTeamPitchingVisibility: K,
    draftTeamFieldingOrder: ct,
    draftTeamFieldingVisibility: $,
    draftRateDisplayStyle: aa,
    draftShowTrailingZeros: ia,
    draftDecimalPlaces: oa,
    visibleBattingCols: on,
    visiblePitchingCols: rn,
    visibleFieldingCols: nn,
    visibleTeamBattingCols: ln,
    visibleTeamPitchingCols: sn,
    visibleTeamFieldingCols: cn,
    setDraftBattingColVisible: pn,
    setDraftPitchingColVisible: hn,
    setDraftFieldingColVisible: bn,
    setDraftTeamBattingColVisible: vn,
    setDraftTeamPitchingColVisible: Pn,
    setDraftTeamFieldingColVisible: Tn,
    statsBattingDraftTableRows: E(() => l.value.map((h) => ({ key: h.key, label: o(h.colKey), visible: un(h.key), desc: r(h.colKey) }))),
    statsPitchingDraftTableRows: E(() => s.value.map((h) => ({ key: h.key, label: o(h.colKey), visible: mn(h.key), desc: r(h.colKey) }))),
    statsFieldingDraftTableRows: E(() => b.value.map((h) => ({ key: h.key, label: o(h.colKey), visible: fn(h.key), desc: r(h.colKey) }))),
    teamStatsBattingDraftTableRows: E(() => d.value.map((h) => ({ key: h.key, label: o(h.colKey), visible: gn(h.key), desc: r(h.colKey) }))),
    teamStatsPitchingDraftTableRows: E(() => L.value.map((h) => ({ key: h.key, label: o(h.colKey), visible: yn(h.key), desc: r(h.colKey) }))),
    teamStatsFieldingDraftTableRows: E(() => _.value.map((h) => ({ key: h.key, label: o(h.colKey), visible: Sn(h.key), desc: r(h.colKey) }))),
    PORTAL_STATS_BATTING_ORDER_KEY: $t,
    PORTAL_STATS_PITCHING_ORDER_KEY: zt,
    PORTAL_STATS_FIELDING_ORDER_KEY: jt,
    PORTAL_STATS_TEAM_BATTING_ORDER_KEY: qt,
    PORTAL_STATS_TEAM_PITCHING_ORDER_KEY: Qt,
    PORTAL_STATS_TEAM_FIELDING_ORDER_KEY: Zt
  };
}

// 供外部（如门户统计页）复用：手动重载与草稿复位
export { le as reloadStatsTableMeta, an as resetStatsTableDrafts };
