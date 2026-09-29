// PortalGameDetail —— 行为保真移植自编译产物 GameDetail-d0FcqgN2（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { nextTick as N, createElementBlock as oe, defineComponent as ze, createTextVNode as ue, computed as m, toDisplayString as k, createElementVNode as L, unref as r, normalizeClass as Ye, createBlock as W, ref as a, createVNode as d, withDirectives as Ze, openBlock as I, withCtx as c, onUnmounted as qe, watch as A, onMounted as Je, onBeforeUnmount as Qe, createCommentVNode as tt } from 'vue';
import { ElIcon as xe, ElPopover as We, ElEmpty as Ke, ElButton as Xe, ElBadge as je, vLoading as et } from 'element-plus';
import { Setting as $e } from '@element-plus/icons-vue';
import { useI18n as at } from 'vue-i18n';
import { useRoute as it } from 'vue-router';
import { fetchResult as de } from '../../api/request';
import { fetchList as $ } from '../../api/request';
import { DISPLAY_SETTINGS_SYNC_KEY as ce } from '../../utils/portalSyncKeys';
import { isPortalGuideDismissed as ve } from '../../utils/portalGuide';
import { FIELD_SETTINGS_GUIDE_SYNC_KEY as me } from '../../utils/portalSyncKeys';
import { usePortalThemeStore as st } from '../../stores/portalTheme';
import { markPortalGuideSeen as lt } from '../../utils/portalGuide';
import { exportSfc as nt } from '../../utils/exportSfc';
import { playerApi as rt } from '../../api/business';
import { teamApi as fe } from '../../api/business';
import { fetchPlayersByTeam } from '../../api/business';
import { buildTeamPlayersMap, addTeamEntryPlayers } from '../../utils/teamPlayersMap';
import { useTenantRouter as ot } from '../../composables/useTenantRouter';
import ut from '../../components/admin/GameDetailContent.js';
import { buildInitialFieldSettings as pe } from '../../utils/gameDetailFieldSettingsStorage';
import { saveDetailPageSettings as dt } from '../../utils/gameDetailFieldSettingsStorage';
import { saveValueControlSettings as ct } from '../../utils/gameDetailFieldSettingsStorage';
import { useDetailLoad as vt } from '../../composables/useDetailLoad';
import { DEFAULT_RATE_DISPLAY_STYLE as mt } from '../../components/portal/PortalStatsColumnSettingsDrawer.js';
import { DEFAULT_BATTER_STAT_COLUMNS as ft } from '../../components/portal/PortalStatsColumnSettingsDrawer.js';
import { DEFAULT_PITCHER_FIELDS as pt } from '../../components/portal/PortalStatsColumnSettingsDrawer.js';
import gt from '../../components/portal/PortalStatsColumnSettingsDrawer.js';
import '../../styles/legacy/portal-game-detail.css';
var _t={
  class:"portal-game-detail"
}
,St=["data-capture-ready"],Et={
  class:"guide-content"
}
,wt={
  class:"guide-title"
}
,yt={
  class:"guide-desc"
}
,Tt={
  class:"guide-actions"
}
,ht=5e3,Dt=140,Ft=5e3,ge=768,Lt=ze({
  __name:"GameDetail",setup(It){
    const{
      t:f
    }
    =at(),P=it(),{
      tenantCode:p
    }
    =ot(),_e=m(()=>Number(P.params.gameId)),Se=m(()=>{
      const e=P.query.hp,s=Array.isArray(e)?e[0]:e,t=Number(s);
      return Number.isFinite(t)&&t>0?t:null
    }
    ),Ee=m(()=>{
      const e=P.query.hideGameMetaStaff,s=Array.isArray(e)?e[0]:e,t=String(s??"").trim().toLowerCase();
      return!(t==="1"||t==="true"||t==="yes"||t==="on")
    }
    ),we=m(()=>{
      const e=P.query.captureTheme,s=Array.isArray(e)?e[0]:e,t=String(s??"").trim().toLowerCase();
      return t==="light"||t==="dark"?t:null
    }
    ),q=st();
    A(we,e=>{
      e?(document.documentElement.classList.toggle("dark",e==="dark"),document.documentElement.setAttribute("data-portal-theme",e)):q.applyToDOM()
    }
    ,{
      immediate:!0
    }
    ),Qe(()=>{
      q.applyToDOM()
    }
    );
    const b=a(null),B=a([]),K=a([]),X=a({
      
    }
    ),j=a(""),J=2,Q=mt,ee=!0,te=ft,ae=pt,g=pe({
      defaultBatterFields:te,defaultPitcherFields:ae,defaultDecimalPlaces:J,defaultRateDisplayStyle:Q,defaultShowTrailingZeros:ee,tenantCode:p.value
    }
    ),S=a(g.batterFields),E=a(g.pitcherFields),w=a(g.decimalPlaces),y=a(g.rateDisplayStyle),T=a(g.showTrailingZeros),v=a(!1),ye=a("pitching"),ie=a("gameDetail"),i=a(!1),l=a(g.guideDismissed),o=a(!0);
    let h=null;
    const D=a(!1),F=a(!1),Te=m(()=>S.value),he=m(()=>E.value);
    function De(){
      dt({
        batterFields:S.value,pitcherFields:E.value
      }
      )
    }
    function Fe(){
      ct({
        decimalPlaces:w.value,rateDisplayStyle:y.value,showTrailingZeros:T.value
      }
      )
    }
    A([S,E],()=>De(),{
      deep:!0
    }
    ),A([w,y,T],()=>Fe()),A(v,e=>{
      e||N(()=>{
        l.value||(i.value=!0)
      }
      )
    }
    );
    function Le(e){
      S.value=e.batterItems,E.value=e.pitcherItems,w.value=e.decimalPlaces,y.value=e.rateDisplayStyle,T.value=e.showTrailingZeros
    }
    function Ie(){
      const e=pe({
        defaultBatterFields:te,defaultPitcherFields:ae,defaultDecimalPlaces:J,defaultRateDisplayStyle:Q,defaultShowTrailingZeros:ee,tenantCode:p.value
      }
      );
      S.value=e.batterFields,E.value=e.pitcherFields,w.value=e.decimalPlaces,y.value=e.rateDisplayStyle,T.value=e.showTrailingZeros,l.value=e.guideDismissed,N(()=>{
        !l.value&&!v.value?i.value=!0:i.value=!1
      }
      ),n()
    }
    function Ae(){
      l.value=ve(p.value),N(()=>{
        !l.value&&!v.value?i.value=!0:i.value=!1
      }
      ),n()
    }
    const M=a(typeof window<"u"&&window.innerWidth<ge);
    function O(){
      M.value=window.innerWidth<ge,M.value||(o.value=i.value,_())
    }
    function se(){
      Ie()
    }
    function le(){
      Ae()
    }
    Je(()=>{
      window.addEventListener("resize",O),window.addEventListener("scroll",n,{
        passive:!0
      }
      ),document.addEventListener("scroll",n,{
        passive:!0,capture:!0
      }
      ),window.addEventListener("touchmove",n,{
        passive:!0
      }
      ),window.addEventListener(ce,se),window.addEventListener(me,le),window.addEventListener("mousemove",ne),O(),n(),i.value=!l.value
    }
    ),A(p,()=>{
      l.value=ve(p.value),N(()=>{
        !l.value&&!v.value?i.value=!0:i.value=!1
      }
      )
    }
    ),qe(()=>{
      window.removeEventListener("resize",O),window.removeEventListener("scroll",n),document.removeEventListener("scroll",n,!0),window.removeEventListener("touchmove",n),window.removeEventListener(ce,se),window.removeEventListener(me,le),window.removeEventListener("mousemove",ne),_()
    }
    );
    function _(){
      h&&(clearTimeout(h),h=null)
    }
    function n(){
      o.value=!0,_(),!(!l.value||i.value)&&(h=setTimeout(()=>{
        D.value||F.value||(o.value=!1)
      }
      ,ht))
    }
    function ne(e){
      M.value||e.clientX>=window.innerWidth-Dt&&(o.value=!0,_(),h=setTimeout(()=>{
        !l.value||i.value||D.value||F.value||(o.value=!1)
      }
      ,Ft))
    }
    function Pe(){
      D.value=!0,_(),o.value=!0
    }
    function be(){
      D.value=!1,F.value||n()
    }
    function Ce(){
      F.value=!0,_(),o.value=!0
    }
    function Ge(e){
      const s=e.currentTarget,t=e.relatedTarget;
      t&&s.contains(t)||(F.value=!1,D.value||n())
    }
    function Re(){
      l.value||(l.value=!0,lt(p.value))
    }
    function Ne(){
      i.value=!1,Re(),n()
    }
    function ke(){
      i.value=!1,ie.value="gameDetail",v.value=!0,n()
    }
    const{
      loading:U,error:V,retry:Be
    }
    =vt(async()=>{
      const e=_e.value,s=await de(`/api/game/${e}`);
      if(s.error)throw new Error(s.error);
      const t=s.data??null;
      if(b.value=t,!t)return null;
      const[C,H,z,Y]=await Promise.all([$(`/api/game/${e}/stats`,{
        
      }
      ),de(`/api/event/${t.eventId}`),t.homeTeamId?fetchPlayersByTeam(t.homeTeamId):Promise.resolve([]),t.awayTeamId?fetchPlayersByTeam(t.awayTeamId):Promise.resolve([])]);
      B.value=C.list;
      const G=t.homeTeamId,R=t.awayTeamId,[Oe,Ue]=await Promise.all([G?fe.get(G):Promise.resolve(null),R?fe.get(R):Promise.resolve(null)]);
      K.value=[Oe,Ue].filter(Boolean),j.value=H.data?.name??"";
      // H33：playersMap 复合键（teamId:playerId）+ 单键兼容；补集按注册段生成按队副本
      const x=buildTeamPlayersMap(z??[],G,Y??[],R);
      const re=[...new Set(B.value.map(u=>u.playerId))].filter(u=>!x[u]);
      if(re.length){
        addTeamEntryPlayers(x,(await rt.listByIds(re))?.list??[])
      }
      return X.value=x,t
    }
    ),Me=m(()=>!U.value&&!V.value&&b.value!=null);
    return(e,s)=>{
      const t=Xe,C=Ke,H=xe,z=je,Y=We,G=et;
      return I(),oe("div",_t,[Ze((I(),oe("div",{
        class:"detail-card","data-capture-ready":Me.value?"1":void 0
      }
      ,[b.value?(I(),W(ut,{
        key:0,game:b.value,stats:B.value,teams:K.value,"players-map":X.value,"event-name":j.value,"batter-fields":Te.value,"pitcher-fields":he.value,"decimal-places":w.value,"rate-display-style":y.value,"show-trailing-zeros":T.value,"highlight-player-id":Se.value,"show-game-meta-staff":Ee.value
      }
      ,null,8,["game","stats","teams","players-map","event-name","batter-fields","pitcher-fields","decimal-places","rate-display-style","show-trailing-zeros","highlight-player-id","show-game-meta-staff"])):r(V)?(I(),W(C,{
        key:1,description:r(V)
      }
      ,{
        default:c(()=>[d(t,{
          type:"primary",onClick:r(Be)
        }
        ,{
          default:c(()=>[ue(k(r(f)("common.retry")),1)]),_:1
        }
        ,8,["onClick"])]),_:1
      }
      ,8,["description"])):r(U)?tt("",!0):(I(),W(C,{
        key:2,description:r(f)("gameDetail.gameNotFound")
      }
      ,null,8,["description"]))],8,St)),[[G,r(U)]]),d(gt,{
        modelValue:v.value,"onUpdate:modelValue":s[0]||(s[0]=R=>v.value=R),"active-tab":ye.value,scope:ie.value,"include-game-detail-option":!0,onSaveGameDetailSettings:Le
      }
      ,null,8,["modelValue","active-tab","scope"]),d(Y,{
        visible:i.value,placement:"left",width:220,trigger:"manual","popper-class":"field-settings-guide"
      }
      ,{
        reference:c(()=>[L("div",{
          class:Ye(["floating-settings-wrap",{
            "mobile-overlay-hidden":!o.value&&!i.value
          }
          ]),onMouseenter:Pe,onMouseleave:be,onFocusin:Ce,onFocusout:Ge
        }
        ,[d(z,{
          "is-dot":!l.value,type:"danger"
        }
        ,{
          default:c(()=>[d(t,{
            circle:"",class:"floating-settings-btn",title:r(f)("gameDetail.fieldSettings"),onClick:ke
          }
          ,{
            default:c(()=>[d(H,null,{
              default:c(()=>[d(r($e))]),_:1
            }
            )]),_:1
          }
          ,8,["title"])]),_:1
        }
        ,8,["is-dot"])],34)]),default:c(()=>[L("div",Et,[L("div",wt,k(r(f)("fieldSettings.guideTitle")),1),L("div",yt,k(r(f)("fieldSettings.guideDesc")),1),L("div",Tt,[d(t,{
          link:"",type:"primary",size:"small",onClick:Ne
        }
        ,{
          default:c(()=>[ue(k(r(f)("fieldSettings.guideConfirm")),1)]),_:1
        }
        )])])]),_:1
      }
      ,8,["visible"])])
    }
    
  }
  
}
),zt=nt(Lt,[["__scopeId","data-v-60783935"]]);

export default zt;
