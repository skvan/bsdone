// Home —— 行为保真移植自编译产物 Home-tIruTPFU（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { nextTick as Se, withModifiers as ce, createElementBlock as i, normalizeStyle as ue, defineComponent as st, createTextVNode as A, computed as g, toDisplayString as c, createElementVNode as a, unref as t, normalizeClass as Q, vShow as rt, createBlock as z, ref as f, createVNode as s, defineAsyncComponent as it, withDirectives as dt, openBlock as o, withCtx as d, Fragment as x, resolveComponent as mt, watch as Me, onMounted as ht, renderList as b, onBeforeUnmount as ft, createCommentVNode as _, defineAsyncComponent as N } from 'vue';
import { DataLine as ot, ArrowRight as lt, Calendar as vt, Trophy as _t, ArrowLeft as wt, Document as Y } from '@element-plus/icons-vue';
import { ElIcon as nt, ElSkeleton as ct, ElEmpty as ut, ElTag as pt, ElSkeletonItem as yt } from 'element-plus';
import { useI18n as kt } from 'vue-i18n';
import { useRouter as gt } from 'vue-router';
import { resolveAssetUrl as Z } from '../../api/request';
import { formatSmartDateTime as Ae } from '../../utils/dateExtras';
import { formatDateDot as Le } from '../../utils/formatDate';
import { filterBannerItems as Ce } from '../../utils/bannerItems';
import { useAppConfigStore as xt } from '../../stores/appConfig';
import { bannerItemKey as Ie } from '../../utils/bannerItems';
import { exportSfc as Tt } from '../../utils/exportSfc';
import { articleApi as Pe } from '../../api/system';
import { eventApi as Et } from '../../api/business';
import { teamApi as bt } from '../../api/business';
import { useTenantRouter as St } from '../../composables/useTenantRouter';
import { onTeamLogoError as He } from '../../utils/placeholderAssets';
import { DEFAULT_TEAM_SVG_PATH as Mt } from '../../utils/placeholderAssets';
import { resolveYearQuery as At } from '../../utils/portalYearQuery';
import { getDefaultYearQuery as J } from '../../utils/portalYearQuery';
import { resolveNoticeCoverUrl as B } from '../../utils/noticeContentImage';
import { appendCacheVersion as De } from '../../utils/assetCacheUrl';
import { parseCacheTimestamp as Lt } from '../../utils/assetCacheUrl';
import '../../styles/legacy/home.css';
var Ct={
  class:"portal-home"
}
,It={
  key:0,class:"portal-home-section carousel-section"
}
,Pt={
  key:0,class:"home-carousel home-carousel--skeleton","aria-busy":"true"
}
,Ht={
  class:"carousel-skeleton-stage"
}
,Dt={
  class:"carousel-skeleton-caption"
}
,Ot=["onClick"],Rt={
  class:"carousel-cover"
}
,zt=["src","alt","loading","fetchpriority"],Nt={
  key:1,class:"carousel-placeholder"
}
,Bt={
  class:"carousel-overlay"
}
,Vt={
  key:0,class:"carousel-summary"
}
,Ft=["aria-label"],Wt=["aria-label"],$t={
  key:2,class:"carousel-indicators"
}
,Gt=["onClick"],Ut=["aria-label"],qt={
  key:1,class:"portal-home-section nav-cards-section"
}
,Xt={
  class:"nav-cards"
}
,jt={
  class:"nav-card-icon"
}
,Kt={
  class:"nav-card-label"
}
,Qt={
  class:"nav-card-icon"
}
,Yt={
  class:"nav-card-label"
}
,Zt={
  class:"nav-card-icon"
}
,Jt={
  class:"nav-card-label"
}
,ea={
  class:"nav-card-icon"
}
,ta={
  class:"nav-card-label"
}
,aa={
  key:2,class:"portal-home-section home-promo-dual"
}
,sa={
  class:"home-promo-dual__grid"
}
,oa={
  class:"home-promo-card"
}
,ra={
  class:"home-promo-card"
}
,na={
  key:3,class:"portal-home-section recent-events"
}
,la={
  class:"home-section-card"
}
,ia={
  class:"section-card-head"
}
,da={
  class:"section-card-title"
}
,ca={
  class:"section-card-body"
}
,ua={
  key:0,class:"home-events-grid home-events-row home-events-row--skeleton"
}
,va={
  class:"sk-event-head"
}
,ma={
  class:"sk-event-meta"
}
,_a={
  key:1,class:"home-events-grid home-events-row"
}
,ha={
  class:"event-header"
}
,pa={
  class:"event-card-title"
}
,fa={
  class:"desc"
}
,wa={
  class:"event-meta"
}
,ya={
  class:"event-meta-dates"
}
,ka={
  key:2,class:"recent-events-empty"
}
,ga={
  class:"section-card-more-wrap section-card-more--mobile"
}
,xa={
  key:4,class:"portal-home-section recent-teams"
}
,Ta={
  class:"home-section-card"
}
,Ea={
  class:"section-card-head"
}
,ba={
  class:"section-card-title"
}
,Sa={
  class:"section-card-body"
}
,Ma={
  class:"sk-team-inner"
}
,Aa={
  class:"card-inner"
}
,La={
  class:"logo-wrap"
}
,Ca=["src","alt"],Ia={
  class:"team-card-title"
}
,Pa=["src","alt"],Ha={
  key:1,class:"meta meta-row"
}
,Da={
  key:0
}
,Oa={
  key:2,class:"recent-teams-empty"
}
,Ra={
  class:"section-card-more-wrap section-card-more--mobile"
}
,za={
  key:5,class:"portal-home-section recent-news"
}
,Na={
  class:"home-section-card"
}
,Ba={
  class:"section-card-head"
}
,Va={
  class:"section-card-title"
}
,Fa={
  class:"section-card-body section-card-body--flush"
}
,Wa={
  key:0,class:"index-news-panel index-news-panel--embedded","aria-hidden":"true"
}
,$a={
  class:"index-news-skeleton-grid"
}
,Ga={
  class:"index-news-skeleton-primary"
}
,Ua={
  class:"index-news-skeleton-side"
}
,qa={
  class:"sk-news-side-row"
}
,Xa={
  class:"sk-news-side-txt"
}
,ja={
  key:1,class:"index-news-panel index-news-panel--embedded"
}
,Ka={
  class:"focus-visual"
}
,Qa={
  key:1,class:"focus-img focus-img--ph"
}
,Ya={
  class:"focus-cont"
}
,Za={
  class:"focus-title"
}
,Ja={
  class:"focus-date"
}
,es={
  key:0,class:"focus-desc"
}
,ts={
  key:1,class:"focus-meta"
}
,as={
  key:1,class:"index-news-list"
}
,ss={
  class:"item-img-wrap"
}
,os={
  key:1,class:"item-img item-img--ph"
}
,rs={
  class:"item-cont"
}
,ns={
  class:"item-title"
}
,ls={
  class:"focus-date"
}
,is={
  key:0,class:"focus-meta"
}
,ds={
  key:2,class:"recent-news-empty"
}
,cs={
  key:3,class:"section-card-more-wrap section-card-more--mobile"
}
,V=10,Oe=200,us=20,vs=6,F=24,ms=6,_s=6,ee=768,hs=50,ps=st({
  __name:"Home",setup(fs){
    const ve=it(()=>N(()=>import("../../components/portal/HomePromoColumn.js"))),T=xt(),W=g(()=>{
      const e=new Set(T.portalHomeSectionHidden),l=T.portalHomeSectionOrder.filter(n=>!e.has(n));
      return l.includes("news")?[...l.filter(n=>n!=="news"),"news"]:l
    }
    ),Re=gt(),{
      portalPath:h
    }
    =St(),{
      t:u
    }
    =kt(),ze=Mt,Ne=[0,1];
    function me(e,l,n){
      const v=(e??"").trim()||l;
      let m;
      return v.startsWith("http")||v.startsWith("data:")?m=v:v.startsWith("/files/")||v.startsWith("/api/")?m=Z(v):m=T.resolveAssetUrl(v.startsWith("/")?v.slice(1):v),De(m,n)
    }
    function te(e){
      const l=B(e);
      return l?De(Z(l),Lt(e.updatedAt,e.createdAt)):""
    }
    const Be=g(()=>Ce([...T.portalPromoAdSlides]).map(e=>({
      src:me(e.imageUrl,"advertising/ad.jpg",Ie(e)),linkUrl:e.linkUrl??""
    }
    ))),Ve=g(()=>Ce([...T.portalPromoTicketSlides]).map(e=>({
      src:me(e.imageUrl,"ticket/tk.jpg",Ie(e)),linkUrl:e.linkUrl??""
    }
    ))),w=f([]),S=f(!0),$=f([]),y=g(()=>$.value[0]??null),ae=g(()=>$.value.slice(1,3)),se=f([]),oe=f([]);
    function Fe(e,l,n){
      return Math.max(1,Math.floor((Math.max(0,e)+n)/(l+n)))
    }
    function G(e){
      const l=Math.min(Math.max(0,e),4096),n=96,v=Oe;
      if(T.portalLayoutWidthMode==="boxed"){
        const m=Math.max(320,Number(T.portalContentMaxWidth)||1440);
        return Math.max(v,Math.min(l,m)-n)
      }
      return Math.max(v,l-n)
    }
    function _e(e,l){
      if(l<=ee)return Math.min(vs,V);
      const n=Fe(e>0?e:G(l),Oe,us);
      return Math.min(n,V)
    }
    const re=f(typeof window<"u"?_e(0,window.innerWidth):V);
    function he(){
      return typeof window>"u"?F:window.innerWidth<=ee?ms:F
    }
    const ne=f(typeof window<"u"?he():F);
    function I(){
      ne.value=he()
    }
    const U=f(null);
    let L=null,M=null,q=null;
    function pe(e){
      if(e==null)return null;
      if(e instanceof HTMLElement)return e;
      if(Array.isArray(e)){
        for(const l of e)if(l instanceof HTMLElement)return l;
        return null
      }
      return null
    }
    function fe(){
      const e=pe(U.value);
      if(!e)return 0;
      const l=e.getBoundingClientRect(),n=l.width>0?l.width:e.clientWidth||e.offsetWidth;
      return Number.isFinite(n)?n:0
    }
    function We(e,l){
      const n=e.borderBoxSize;
      if(n&&n.length>0&&n[0]){
        const m=n[0].inlineSize;
        if(m>0)return m
      }
      const v=e.contentRect.width;
      return v>0?v:l()
    }
    function X(e){
      if(typeof window>"u")return;
      const l=window.innerWidth;
      let n=e!=null&&e>0?e:fe();
      n<=0&&l>ee&&(n=G(l)),re.value=_e(n,l)
    }
    function D(){
      typeof window>"u"||(M!=null&&cancelAnimationFrame(M),M=requestAnimationFrame(()=>{
        M=requestAnimationFrame(()=>{
          M=null,X(),I()
        }
        )
      }
      ))
    }
    function we(){
      if(typeof window>"u"||typeof ResizeObserver>"u")return;
      L?.disconnect(),L=null;
      const e=pe(U.value);
      if(e){
        L=new ResizeObserver(l=>{
          const n=l[0];
          if(!n)return;
          const v=We(n,fe);
          X(v>0?v:void 0)
        }
        );
        try{
          L.observe(e,{
            box:"border-box"
          }
          )
        }
        catch{
          L.observe(e)
        }
        
      }
      
    }
    function j(){
      X(G(window.innerWidth)),I(),D()
    }
    function ye(){
      X(G(window.innerWidth)),I(),D()
    }
    const $e=g(()=>oe.value.slice(0,re.value)),Ge=g(()=>Math.min(V,re.value)),Ue=g(()=>se.value.slice(0,ne.value)),qe=g(()=>Math.min(_s,ne.value)),P=f(!0),k=f(0),O=f(!1);
    let H=null;
    const ke=f(0),R=f(0),le=f(!1),Xe=g(()=>{
      const e=-k.value*100,l=R.value;
      return{
        transform:`translateX(calc(${e}% + ${l}px))`,transition:l!==0?"none":"transform 0.4s ease-in-out"
      }
      
    }
    );
    function ge(){
      const e=w.value.length;
      e<=1||(k.value=(k.value-1+e)%e,K())
    }
    function xe(){
      const e=w.value.length;
      e<=1||(k.value=(k.value+1)%e,K())
    }
    function ie(){
      w.value.length<=1||O.value||(H=setInterval(()=>{
        k.value=(k.value+1)%w.value.length
      }
      ,5e3))
    }
    function Te(){
      H&&(clearInterval(H),H=null)
    }
    function K(){
      Te(),ie()
    }
    function je(e){
      ke.value=e.touches[0].clientX,R.value=0
    }
    function Ke(e){
      R.value=e.touches[0].clientX-ke.value
    }
    function Qe(){
      const e=R.value;
      R.value=0,Math.abs(e)>=hs&&(le.value=!0,e>0?ge():xe(),K(),setTimeout(()=>{
        le.value=!1
      }
      ,300))
    }
    function Ye(e){
      return u(`events.eventStatus.${e}`)||e
    }
    function Ze(e){
      return{
        draft:"info",ongoing:"success",ended:""
      }
      [e]||"info"
    }
    function Je(){
      if(typeof window>"u"||typeof navigator>"u")return!1;
      const e=navigator.connection;
      if(!e)return!0;
      if(e.saveData)return!1;
      const l=String(e.effectiveType||"").toLowerCase();
      return l!=="slow-2g"&&l!=="2g"
    }
    function et(){
      if(!Je())return;
      const e=()=>{
        Promise.resolve(),N(()=>import("../../views/portal/News.js")),Promise.resolve(),Promise.resolve()
      };
      if(typeof window<"u"&&"requestIdleCallback"in window){
        window.requestIdleCallback(e,{
          timeout:1500
        }
        );
        return
      }
      setTimeout(e,400)
    }
    function tt(){
      const e=W.value;
      return e.includes("events")||e.includes("teams")
    }
    function Ee(e=4){
      typeof window>"u"||(window.scrollTo({
        top:0,behavior:"auto"
      }
      ),!(e<=0)&&window.setTimeout(()=>{
        (window.scrollY||0)>0&&Ee(e-1)
      }
      ,80))
    }
    return Me(W,e=>{
      !e.includes("events")&&!e.includes("teams")&&(P.value=!1),e.includes("carousel")||(S.value=!1)
    }
    ),Me(P,()=>{
      Se(()=>{
        I(),D(),we()
      }
      )
    }
    ),ht(async()=>{
      Ee(),W.value.includes("carousel")||(S.value=!1);
      try{
        const[l,n]=await Promise.all([Pe.list({
          publishTarget:"portal",showInCarousel:1,forPortal:!0
        }
        ),Pe.list({
          publishTarget:"portal",page:1,pageSize:100,forPortal:!0
        }
        )]),v=new Set((l.list||[]).map(m=>m.id));
        w.value=l.list||[],k.value=0,ie(),$.value=(n.list||[]).filter(m=>!v.has(m.id)).slice(0,3)
      }
      finally{
        S.value=!1
      }
      if(tt()||(P.value=!1),(async()=>{
        try{
          const[l,n]=await Promise.all([Et.list({
            page:1,pageSize:F
          }
          ),bt.list({
            page:1,pageSize:V,showInPortal:1,sortProp:"id",sortOrder:"asc"
          }
          )]);
          se.value=(l.list||[]).sort((v,m)=>(m.startDate||"").localeCompare(v.startDate||"")).slice(0,F),oe.value=n.list||[]
        }
        finally{
          P.value=!1
        }
        
      }
      )(),et(),typeof window<"u"){
        I(),D(),window.addEventListener("resize",j,{
          passive:!0
        }
        );
        const l=window.visualViewport;
        l&&l.addEventListener("resize",j,{
          passive:!0
        }
        ),q=window.matchMedia(`(max-width: ${ee}px)`),q.addEventListener("change",ye),Se(()=>{
          I(),D(),we()
        }
        )
      }
      
    }
    ),ft(()=>{
      H&&clearInterval(H),M!=null&&(cancelAnimationFrame(M),M=null),L?.disconnect(),L=null,typeof window<"u"&&(window.removeEventListener("resize",j),window.visualViewport?.removeEventListener("resize",j),q?.removeEventListener("change",ye),q=null)
    }
    ),(e,l)=>{
      const n=yt,v=ct,m=nt,p=mt("router-link"),be=pt,de=ut;
      return o(),i("div",Ct,[(o(!0),i(x,null,b(W.value,C=>(o(),i(x,{
        key:C
      }
      ,[C==="carousel"&&(S.value||w.value.length)?(o(),i("section",It,[S.value?(o(),i("div",Pt,[a("div",Ht,[s(v,{
        animated:"",throttle:0
      }
      ,{
        template:d(()=>[s(n,{
          variant:"rect",class:"carousel-skeleton-img"
        }
        )]),_:1
      }
      )]),a("div",Dt,[s(v,{
        animated:"",throttle:0
      }
      ,{
        template:d(()=>[s(n,{
          variant:"h3",style:{
            width:"52%","max-width":"320px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"78%","max-width":"480px","margin-top":"10px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"40%","max-width":"200px","margin-top":"8px"
          }
          
        }
        )]),_:1
      }
      )])])):(o(),i("div",{
        key:1,class:"home-carousel",onMouseenter:l[0]||(l[0]=r=>{
          O.value=!0,Te()
        }
        ),onMouseleave:l[1]||(l[1]=r=>{
          O.value=!1,ie()
        }
        )
      }
      ,[a("div",{
        class:"carousel-viewport",onTouchstart:je,onTouchmove:Ke,onTouchend:Qe
      }
      ,[a("div",{
        class:"carousel-track",style:ue(Xe.value)
      }
      ,[(o(!0),i(x,null,b(w.value,(r,E)=>(o(),i("div",{
        key:r.id,class:"carousel-slide",onClick:at=>!le.value&&t(Re).push(t(h)(`article/${r.id}`))
      }
      ,[a("div",Rt,[t(B)(r)?(o(),i("img",{
        key:0,class:"carousel-cover-img",src:te(r),alt:r.title||t(u)("home.newsTitle"),loading:E===0?"eager":"lazy",fetchpriority:E===0?"high":"auto",decoding:"async"
      }
      ,null,8,zt)):_("",!0),t(B)(r)?_("",!0):(o(),i("div",Nt,[s(m,null,{
        default:d(()=>[s(t(Y))]),_:1
      }
      )]))]),a("div",Bt,[a("h3",null,c(r.title),1),r.summary?(o(),i("p",Vt,c(r.summary),1)):_("",!0)])],8,Ot))),128))],4)],32),w.value.length>1?(o(),i("button",{
        key:0,type:"button",class:Q(["carousel-arrow carousel-arrow-left",{
          visible:O.value
        }
        ]),"aria-label":t(u)("nav.prevSlide"),onClick:ce(ge,["stop"])
      }
      ,[s(m,null,{
        default:d(()=>[s(t(wt))]),_:1
      }
      )],10,Ft)):_("",!0),w.value.length>1?(o(),i("button",{
        key:1,type:"button",class:Q(["carousel-arrow carousel-arrow-right",{
          visible:O.value
        }
        ]),"aria-label":t(u)("nav.nextSlide"),onClick:ce(xe,["stop"])
      }
      ,[s(m,null,{
        default:d(()=>[s(t(lt))]),_:1
      }
      )],10,Wt)):_("",!0),w.value.length>1?(o(),i("ul",$t,[(o(!0),i(x,null,b(w.value,(r,E)=>(o(),i("li",{
        key:E,class:Q(["carousel-indicator",{
          active:E===k.value
        }
        ]),onClick:ce(at=>{
          k.value=E,K()
        }
        ,["stop"])
      }
      ,[a("button",{
        type:"button","aria-label":t(u)("nav.slideIndex",{
          n:E+1
        }
        )
      }
      ,null,8,Ut)],10,Gt))),128))])):_("",!0)],32))])):_("",!0),C==="nav"?(o(),i("section",qt,[a("div",Xt,[s(p,{
        to:{
          path:t(h)("events"),query:t(J)()
        }
        ,class:"nav-card"
      }
      ,{
        default:d(()=>[a("div",jt,[s(m,null,{
          default:d(()=>[s(t(vt))]),_:1
        }
        )]),a("span",Kt,c(t(u)("home.eventsTitle")),1)]),_:1
      }
      ,8,["to"]),s(p,{
        to:t(h)("teams"),class:"nav-card"
      }
      ,{
        default:d(()=>[a("div",Qt,[s(m,null,{
          default:d(()=>[s(t(_t))]),_:1
        }
        )]),a("span",Yt,c(t(u)("home.teamsTitle")),1)]),_:1
      }
      ,8,["to"]),s(p,{
        to:{
          path:t(h)("stats"),query:t(J)()
        }
        ,class:"nav-card"
      }
      ,{
        default:d(()=>[a("div",Zt,[s(m,null,{
          default:d(()=>[s(t(ot))]),_:1
        }
        )]),a("span",Jt,c(t(u)("nav.stats")),1)]),_:1
      }
      ,8,["to"]),s(p,{
        to:t(h)("article"),class:"nav-card"
      }
      ,{
        default:d(()=>[a("div",ea,[s(m,null,{
          default:d(()=>[s(t(Y))]),_:1
        }
        )]),a("span",ta,c(t(u)("home.newsTitle")),1)]),_:1
      }
      ,8,["to"])])])):_("",!0),C==="promo"?(o(),i("section",aa,[a("div",sa,[a("div",oa,[s(t(ve),{
        slides:Be.value,alt:t(u)("home.promoAdAlt"),"fallback-text":"AD","portal-path":t(h)
      }
      ,null,8,["slides","alt","portal-path"])]),a("div",ra,[s(t(ve),{
        slides:Ve.value,alt:t(u)("home.promoTicketAlt"),"fallback-text":"TK","portal-path":t(h)
      }
      ,null,8,["slides","alt","portal-path"])])])])):_("",!0),C==="events"?(o(),i("section",na,[a("div",la,[a("div",ia,[a("h2",da,c(t(u)("home.eventsTitle")),1),s(p,{
        to:{
          path:t(h)("events"),query:t(J)()
        }
        ,class:"more-link section-card-more section-card-more--desktop"
      }
      ,{
        default:d(()=>[A(c(t(u)("home.more")),1)]),_:1
      }
      ,8,["to"])]),a("div",ca,[P.value?(o(),i("div",ua,[(o(!0),i(x,null,b(qe.value,r=>(o(),i("div",{
        key:"ev-sk-"+r,class:"event-card event-card--skeleton"
      }
      ,[s(v,{
        animated:"",throttle:0
      }
      ,{
        template:d(()=>[a("div",va,[s(n,{
          variant:"text",style:{
            width:"68%",height:"18px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"52px",height:"22px","border-radius":"4px"
          }
          
        }
        )]),s(n,{
          variant:"text",style:{
            width:"100%","margin-top":"10px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"88%","margin-top":"8px"
          }
          
        }
        ),a("div",ma,[s(n,{
          variant:"text",style:{
            width:"46%"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"46%"
          }
          
        }
        )])]),_:1
      }
      )]))),128))])):se.value.length>0?(o(),i("div",_a,[(o(!0),i(x,null,b(Ue.value,r=>(o(),z(p,{
        key:r.id,to:{
          path:t(h)("events"),query:{
            eventId:String(r.id),...t(At)(r)
          }
          
        }
        ,class:"event-card portal-card-link"
      }
      ,{
        default:d(()=>[a("div",ha,[a("h3",pa,c(r.name),1),r.status?(o(),z(be,{
          key:0,type:Ze(r.status),size:"small"
        }
        ,{
          default:d(()=>[A(c(Ye(r.status)),1)]),_:2
        }
        ,1032,["type"])):_("",!0)]),a("p",fa,c(r.description||" "),1),a("div",wa,[a("div",ya,[a("span",null,c(r.startDate?t(u)("home.eventStart")+": "+t(Ae)(r.startDate):" "),1),a("span",null,c(r.endDate?t(u)("home.eventEnd")+": "+t(Ae)(r.endDate):" "),1)]),s(be,{
          type:"info",size:"small",class:"event-game-mode-tag"
        }
        ,{
          default:d(()=>[A(c((r.gameMode||"BASEBALL")==="SOFTBALL"?t(u)("stats.gameModeSoftball"):t(u)("stats.gameModeBaseball")),1)]),_:2
        }
        ,1024)])]),_:2
      }
      ,1032,["to"]))),128))])):(o(),i("div",ka,[s(de,{
        description:t(u)("home.noEvents"),"image-size":88
      }
      ,null,8,["description"])]))]),a("div",ga,[s(p,{
        to:{
          path:t(h)("events"),query:t(J)()
        }
        ,class:"more-link"
      }
      ,{
        default:d(()=>[A(c(t(u)("home.more")),1)]),_:1
      }
      ,8,["to"])])])])):_("",!0),C==="teams"?(o(),i("section",xa,[a("div",Ta,[a("div",Ea,[a("h2",ba,c(t(u)("home.teamsTitle")),1),s(p,{
        to:t(h)("teams"),class:"more-link section-card-more section-card-more--desktop"
      }
      ,{
        default:d(()=>[A(c(t(u)("home.more")),1)]),_:1
      }
      ,8,["to"])]),a("div",Sa,[P.value?(o(),i("div",{
        key:0,ref_for:!0,ref_key:"homeTeamsGridRef",ref:U,class:"teams-grid teams-grid--skeleton"
      }
      ,[(o(!0),i(x,null,b(Ge.value,r=>(o(),i("div",{
        key:"tm-sk-"+r,class:"team-card team-card--skeleton"
      }
      ,[s(v,{
        animated:"",throttle:0
      }
      ,{
        template:d(()=>[a("div",Ma,[s(n,{
          class:"sk-team-logo",variant:"rect"
        }
        ),s(n,{
          variant:"text",style:{
            width:"72%",height:"16px","margin-top":"12px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"48%",height:"13px","margin-top":"8px"
          }
          
        }
        )])]),_:1
      }
      )]))),128))],512)):oe.value.length>0?(o(),i("div",{
        key:1,ref_for:!0,ref_key:"homeTeamsGridRef",ref:U,class:"teams-grid"
      }
      ,[(o(!0),i(x,null,b($e.value,r=>(o(),z(p,{
        key:r.id,to:t(h)(`teams/${r.id}`),class:"team-card portal-card-link"
      }
      ,{
        default:d(()=>[a("div",Aa,[a("div",La,[a("img",{
          src:t(Z)(r.logo)||t(ze),alt:r.name,onError:l[2]||(l[2]=(...E)=>t(He)&&t(He)(...E))
        }
        ,null,40,Ca)]),a("h3",Ia,c(r.name),1),r.wordmark?(o(),i("img",{
          key:0,src:t(Z)(r.wordmark),alt:`${r.name} 文字Logo`,class:"card-wordmark"
        }
        ,null,8,Pa)):_("",!0),r.city?(o(),i("p",Ha,[r.city?(o(),i("span",Da,c(r.city),1)):_("",!0)])):_("",!0)])]),_:2
      }
      ,1032,["to"]))),128))],512)):(o(),i("div",Oa,[s(de,{
        description:t(u)("home.noTeams"),"image-size":88
      }
      ,null,8,["description"])]))]),a("div",Ra,[s(p,{
        to:t(h)("teams"),class:"more-link"
      }
      ,{
        default:d(()=>[A(c(t(u)("home.more")),1)]),_:1
      }
      ,8,["to"])])])])):_("",!0),C==="news"?(o(),i("section",za,[a("div",Na,[a("div",Ba,[a("h2",Va,c(t(u)("home.newsTitle")),1),dt(s(p,{
        to:t(h)("article"),class:"more-link section-card-more section-card-more--desktop"
      }
      ,{
        default:d(()=>[A(c(t(u)("home.more")),1)]),_:1
      }
      ,8,["to"]),[[rt,!S.value]])]),a("div",Fa,[S.value?(o(),i("div",Wa,[a("div",$a,[a("div",Ga,[s(v,{
        animated:"",throttle:0
      }
      ,{
        template:d(()=>[s(n,{
          variant:"rect",class:"sk-news-focus"
        }
        ),s(n,{
          variant:"h3",style:{
            width:"90%","max-width":"22em","margin-top":"12px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"58%","max-width":"10em","margin-top":"8px"
          }
          
        }
        )]),_:1
      }
      )]),a("div",Ua,[(o(),i(x,null,b(Ne,r=>s(v,{
        key:"nsks-"+r,animated:"",throttle:0
      }
      ,{
        template:d(()=>[a("div",qa,[s(n,{
          variant:"rect",class:"sk-news-thumb"
        }
        ),a("div",Xa,[s(n,{
          variant:"h3",style:{
            width:"100%",height:"15px"
          }
          
        }
        ),s(n,{
          variant:"text",style:{
            width:"45%","margin-top":"8px"
          }
          
        }
        )])])]),_:1
      }
      )),64))])])])):$.value.length>0?(o(),i("div",ja,[a("div",{
        class:Q(["index-news-wrap",{
          "index-news-wrap--solo":!ae.value.length
        }
        ])
      }
      ,[y.value?(o(),z(p,{
        key:0,to:t(h)(`article/${y.value.id}`),class:"index-news-focus portal-card-link"
      }
      ,{
        default:d(()=>[a("div",Ka,[t(B)(y.value)?(o(),i("div",{
          key:0,class:"focus-img",style:ue({
            backgroundImage:`url(${te(y.value)})`
          }
          )
        }
        ,null,4)):(o(),i("div",Qa,[s(m,null,{
          default:d(()=>[s(t(Y))]),_:1
        }
        )]))]),a("div",Ya,[a("div",Za,c(y.value.title),1),a("div",Ja,c(t(Le)(y.value.createdAt)),1),y.value.summary?(o(),i("p",es,c(y.value.summary),1)):_("",!0),t(T).publicViewCount&&y.value.viewCount!=null?(o(),i("div",ts,c(t(u)("news.viewCount",{
          n:y.value.viewCount
        }
        )),1)):_("",!0)])]),_:1
      }
      ,8,["to"])):_("",!0),ae.value.length?(o(),i("div",as,[(o(!0),i(x,null,b(ae.value,r=>(o(),z(p,{
        key:r.id,to:t(h)(`article/${r.id}`),class:"index-news-item portal-card-link"
      }
      ,{
        default:d(()=>[a("div",ss,[t(B)(r)?(o(),i("div",{
          key:0,class:"item-img",style:ue({
            backgroundImage:`url(${te(r)})`
          }
          )
        }
        ,null,4)):(o(),i("div",os,[s(m,null,{
          default:d(()=>[s(t(Y))]),_:1
        }
        )]))]),a("div",rs,[a("div",ns,c(r.title),1),a("div",ls,c(t(Le)(r.createdAt)),1),t(T).publicViewCount&&r.viewCount!=null?(o(),i("div",is,c(t(u)("news.viewCount",{
          n:r.viewCount
        }
        )),1)):_("",!0)])]),_:2
      }
      ,1032,["to"]))),128))])):_("",!0)],2)])):(o(),i("div",ds,[s(de,{
        description:t(u)("home.noNews"),"image-size":88
      }
      ,null,8,["description"])])),S.value?_("",!0):(o(),i("div",cs,[s(p,{
        to:t(h)("article"),class:"more-link"
      }
      ,{
        default:d(()=>[A(c(t(u)("home.more")),1)]),_:1
      }
      ,8,["to"])]))])])])):_("",!0)],64))),128))])
    }
    
  }
  
}
),Ds=Tt(ps,[["__scopeId","data-v-f8bfa89c"]]);

export default Ds;
