// AdminLayout —— 行为保真移植自编译产物 AdminLayout chunk（recon-gen-admin-layout.mjs 生成，勿手改）
// 生成源：.qoder/frontend-recon/out/beautified/AdminLayout-Cx0XHwPP.pretty.js
// 别名与编译产物局部名一致（经 recon-probe-ep.mjs 运行时探针核实）
import { createVNode as t, createElementVNode as n, createBlock as b, createElementBlock as x, openBlock as d, withCtx as a, createTextVNode as U, createCommentVNode as _, toDisplayString as F, unref as r, normalizeClass as te, Fragment as pe, renderList as Ae, withDirectives as Te, resolveComponent as Re, resolveDynamicComponent as $e, Suspense as qe, Transition as Le, defineComponent as Ve, ref as $, computed as f, watch as be, onMounted as ct, onBeforeUnmount as vt, nextTick as ye, reactive as Rt, vShow as Me, Teleport as rt } from 'vue';
import { ElSelect as Bt, ElOption as Wt, ElInput as pt, ElButton as Ut, ElIcon as mt, ElMessage as Ce, ElMessageBox as Un, ElDropdown as rn, ElDropdownMenu as gn, ElDropdownItem as En, ElHeader as fn, ElAside as Fn, ElFooter as xn, ElMain as kn, ElContainer as dn, ElDialog as vn, ElForm as In, ElFormItem as Tn, ElMenu as un, ElMenuItem as Lt } from 'element-plus';
import { Close as dt, Monitor as Ct, Trophy as Tt, ArrowDown as ot, Setting as _e, ArrowUp as pn, Menu as hn } from '@element-plus/icons-vue';
import { useRoute as je } from 'vue-router';
import { useRouter as Ft } from 'vue-router';
import { useSettingsStore as Ht } from '../../stores/settings';
import { useAppConfigStore as qn } from '../../stores/appConfig';
import { useTabsStore as ft } from '../../stores/tabs';
import { normalizeAdminPath as Ye } from '../../utils/tenantRoute';
import { buildPortalPath as Hn } from '../../utils/tenantRoute';
import { normalizeTenantCode as Vt } from '../../api/tokenStorage';
import { useAuthStore as St } from '../../stores/auth';
import { setActiveTenant as Dn } from '../../api/tokenStorage';
import { fetchList as Nn } from '../../api/request';
import { resolveAssetUrl as zn } from '../../api/request';
import { authApi as $t } from '../../api/account';
import { tenantApi as Xn } from '../../api/system';
import { useAnnouncementNotifications as Kn } from '../../composables/useAnnouncementNotifications';
import { useMenuMediaIconsStore as Qn } from '../../stores/menuMediaIcons';
import { useEventListener as Mn } from '../../composables/useEventListener';
import { exportSfc as Ee } from '../../utils/exportSfc';
import Et from './AdminBreadcrumb.vue';
import it from './AdminTagsView.vue';
import Ot from './AdminWatermark.js';
import hl from './AdminSettingsDrawer.js';
import Ge from './AdminMenu.js';
// 值解包（等价编译产物 Gn：兼容 ref 与普通值）
const Gn = (value) => (value && typeof value === "object" && "value" in value ? value.value : value);
import '../../styles/legacy/admin-layout.css';
var Cl={
  key:0,class:"admin-layout admin-layout--standalone-content"
}
,Ml={
  class:"route-wrap"
}
,Tl=["data-menu-mode"],Vl={
  class:"header-logo"
}
,Sl=["src"],$l={
  class:"header-logo-text"
}
,Al={
  class:"header-right"
}
,El={
  class:"user-trigger"
}
,Ol=["src"],Pl={
  class:"user-name-text"
}
,Wl={
  class:"route-wrap"
}
,Ul=["innerHTML"],Bl={
  key:2,class:"admin-layout-combo"
}
,Ll={
  class:"admin-header admin-header--combo"
}
,Rl={
  class:"header-left"
}
,Il={
  class:"header-logo"
}
,Fl=["src"],Hl={
  class:"header-logo-text"
}
,Dl={
  class:"header-right"
}
,Nl={
  class:"user-trigger"
}
,zl=["src"],ql={
  class:"user-name-text"
}
,Gl={
  class:"admin-combo-row"
}
,Yl={
  key:0,class:"sidebar-header"
}
,jl={
  class:"sidebar-header-title-wrap"
}
,Xl={
  class:"sidebar-section-title"
}
,Ql={
  class:"admin-body admin-body--combo"
}
,Kl={
  class:"route-wrap"
}
,Jl={
  key:0,class:"sidebar-header"
}
,Zl={
  class:"sidebar-header-title-wrap"
}
,eo={
  class:"sidebar-section-title"
}
,to={
  class:"admin-header"
}
,no={
  class:"header-left"
}
,ao={
  key:0,class:"header-logo header-logo--mobile"
}
,lo=["src"],oo={
  class:"header-logo-text"
}
,so={
  key:1,class:"header-logo"
}
,io=["src"],ro={
  class:"header-logo-text"
}
,uo={
  class:"header-right"
}
,mo=["src"],po={
  class:"user-name-text"
}
,vo={
  class:"route-wrap"
}
,co=768,fo=Ve({
  __name:"AdminLayout",setup(B){
    Kn("admin");
    const w=je(),D=Ft(),A=f(()=>!!w.matched[w.matched.length-1]?.meta?.standaloneContent),p=Ht(),C=ft(),y=qn(),E=f(()=>Gn(y.footerTextAdmin)),u=St(),h=$(null),S=$(null);
    $(!1);
    const L=$(!1),R=$(!1),O=$(!1),q=$(!1),G=$(!1),Y=$(!1),oe=$(),X=$(!1),l=$(!1),se=f(()=>l.value?"click":"hover");
    function ve(){
      const i=h.value;
      return i?"$el"in i&&i.$el?i.$el:i:null
    }
    function ke(){
      return l.value?p.mobileFixedHeader?ve():S.value:ve()
    }
    const ce=f(()=>l.value?p.mobileFixedHeader?ve():S.value:null),ie=$(!1),re=$(!0);
    let N=null;
    const Oe=f(()=>ie.value&&!re.value);
    function m(){
      const i=ce.value;
      if(i){
        if(i.scrollTop<=88){
          ie.value=!1,re.value=!0,N&&(clearTimeout(N),N=null);
          return
        }
        ie.value=!0,re.value=!1,N&&clearTimeout(N),N=setTimeout(()=>{
          N=null,re.value=!0
        }
        ,1100)
      }
      
    }
    function e(){
      const i=ce.value;
      i&&"scrollTo"in i&&i.scrollTo({
        top:0,behavior:"smooth"
      }
      ),ie.value=!1,re.value=!0,N&&(clearTimeout(N),N=null)
    }
    Mn(ce,"scroll",m,{
      passive:!0
    }
    ),be([l,()=>p.mobileFixedHeader],()=>{
      ye(()=>m())
    }
    );
    const V=$([]),J=Qn(),ne=f(()=>J.mergedMap),le=f(()=>new Set(u.user?.menuIds??[])),H=$(!1),z=$(!1),de=$(null),Q=$([]),ue=f(()=>{
      const i=u.user;
      return i?!!(i.superAdmin===!0||(i.roleIds??[]).includes(1)||String(i.username||"").trim().toLowerCase()==="admin"):!1
    }
    ),Pe=f(()=>u.user?.tenantId??null);
    f(()=>{
      const i=u.user;
      if(!i)return"-";
      if(ue.value&&(u.noTenantLimit||Number(i.tenantId)===0))return"不限制（全租户）";
      const o=i.tenants??[],c=Number(i.tenantId),k=o.find(Z=>Number(Z.id)===c);
      if(k?.name)return k.name;
      const I=fe.value.find(Z=>Number(Z.id)===c);
      return I?.name?I.name:"-"
    }
    ),f(()=>{
      const i=u.user;
      return i?ue.value?"超级管理员":i.tenantAdmin?"租户管理员":"普通管理员":"-"
    }
    );
    const fe=f(()=>{
      if(!ue.value)return[];
      const i=new Map;
      for(const o of u.user?.tenants??[])o?.id!=null&&i.set(o.id,{
        id:o.id,name:o.name,code:o.code
      }
      );
      for(const o of Q.value)o?.id!=null&&i.set(o.id,o);
      return Array.from(i.values())
    }
    ),v=f(()=>String(w.params.tenantCode||"")),s=f(()=>Hn(v.value,"")),M=`${"/bs-ball/".replace(/\/$/,"")}/default-avatar.svg`,P=$(!1);
    be(()=>u.user?.avatar,()=>{
      P.value=!1
    }
    );
    const T=f(()=>{
      if(P.value)return M;
      const i=u.user?.avatar;
      return i?zn(i):M
    }
    ),W=f(()=>!u.user?.avatar||P.value);
    function K(){
      u.user?.avatar&&(P.value=!0)
    }
    const j=Rt({
      oldPassword:"",newPassword:"",confirmPassword:""
    }
    ),Ie=(i,o,c)=>{
      o!==j.newPassword?c(new Error("两次输入的新密码不一致")):c()
    }
    ,Se=f(()=>({
      oldPassword:[{
        required:!0,message:"请输入原密码",trigger:"blur"
      }
      ],newPassword:[{
        required:!0,message:"请输入新密码",trigger:"blur"
      }
      ,{
        min:6,message:"至少 6 位",trigger:"blur"
      }
      ],confirmPassword:[{
        required:!0,message:"请再次输入新密码",trigger:"blur"
      }
      ,{
        validator:Ie,trigger:"blur"
      }
      ]
    }
    ));
    function Fe(){
      j.oldPassword="",j.newPassword="",j.confirmPassword="",oe.value?.clearValidate()
    }
    async function He(){
      await oe.value?.validate().catch(()=>{
        
      }
      ),X.value=!0;
      try{
        await $t.changePassword({
          oldPassword:j.oldPassword,newPassword:j.newPassword
        }
        ),Ce.success("密码已修改，请重新登录"),Y.value=!1,Fe(),_t()
      }
      catch(i){
        Ce.error(i?.message??"修改失败")
      }
      finally{
        X.value=!1
      }
      
    }
    function xe(){
      l.value=window.innerWidth<co,l.value&&(L.value=!1)
    }
    function we(){
      l.value&&(L.value=!1)
    }
    function We(i){
      const o=[];
      for(const c of i||[])(c.menuType??2)!==3&&(c.path?.startsWith("/admin")&&o.push(c.path),c.children?.length&&o.push(...We(c.children)));
      return o
    }
    const gt=f(()=>We(V.value)),Nt=f(()=>{
      const i=V.value.find(o=>o.path==="/system"||o.id===2);
      return i?We(i.children||[]):["/admin/users","/admin/roles","/admin/menus","/admin/apis","/admin/ip-location-cache","/admin/dict","/admin/articles","/admin/announcements","/admin/config","/admin/resources","/admin/media-icons","/admin/media-gallery","/admin/login-logs","/admin/operation-logs"]
    }
    ),zt=f(()=>{
      const i=V.value.find(o=>o.path==="/monitor"||o.id===3);
      return i?We(i.children||[]):["/admin/monitor/data","/admin/monitor/server","/admin/monitor/cache","/admin/monitor/cache-list","/admin/monitor/portal-devtools-report","/admin/monitor/portal-visit-hit","/admin/monitor/ip-access-policy"]
    }
    ),qt=f(()=>{
      const i=V.value.find(o=>o.path==="/business"||o.id===4);
      return i?We(i.children||[]):["/admin/leagues","/admin/teams","/admin/coaches","/admin/players","/admin/events","/admin/history-records","/admin/content"]
    }
    );
    function Xe(i){
      const o=le.value;
      return o.size===0?i.id===1:o.has(i.id)?!0:(i.children||[]).some(c=>Xe(c))
    }
    function bt(i){
      if((i.menuType??2)===3)return null;
      if(i.path&&i.path.startsWith("/admin"))return i.path;
      for(const o of i.children||[])if((o.menuType??2)!==3&&Xe(o)){
        const c=bt(o);
        if(c)return c
      }
      return null
    }
    const Gt=f(()=>{
      const i=me.value,o=V.value;
      return i==="system"?o.find(c=>c.path==="/system"||c.id===2)?.children||[]:i==="monitor"?o.find(c=>c.path==="/monitor"||c.id===3)?.children||[]:i==="business"?o.find(c=>c.path==="/business"||c.id===4)?.children||[]:[]
    }
    ),Yt=f(()=>{
      const i=v.value,o=i?`/${i}`:"";
      return V.value.filter(Xe).map(c=>{
        const k=bt(c);
        return{
          id:c.id,name:c.name||c.title||"-",index:k?o+k:void 0
        }
        
      }
      ).filter(c=>c.index)
    }
    ),De=f(()=>{
      const i=Ye(w.path),o=(gt.value.length?gt.value:["/admin/dashboard","/admin/users","/admin/roles","/admin/menus","/admin/apis","/admin/ip-location-cache","/admin/dict","/admin/articles","/admin/announcements","/admin/config","/admin/resources","/admin/media-icons","/admin/media-gallery","/admin/login-logs","/admin/operation-logs","/admin/monitor/data","/admin/monitor/server","/admin/monitor/cache","/admin/monitor/cache-list","/admin/monitor/ip-access-policy","/admin/leagues","/admin/teams","/admin/coaches","/admin/players","/admin/events","/admin/history-records","/admin/content"]).filter(k=>i===k||i.startsWith(k+"/")).sort((k,I)=>I.length-k.length)[0],c=v.value;
      return o&&c?`/${c}${o}`:w.path
    }
    ),Ne=f(()=>p.pageTransition==="none"?"":p.pageTransition),jt=f(()=>p.showBreadcrumb&&!l.value&&p.menuMode!=="sidebar"&&p.menuMode!=="top"),Xt=f(()=>p.fillPageHeight&&!l.value),me=f(()=>{
      const i=Ye(w.path);
      return Nt.value.some(o=>i===o||i.startsWith(o+"/"))?"system":zt.value.some(o=>i===o||i.startsWith(o+"/"))?"monitor":qt.value.some(o=>i===o||i.startsWith(o+"/"))?"business":null
    }
    ),Ue=f(()=>!!me.value),wt=f(()=>{
      const i=me.value,o=V.value;
      if(i==="system"){
        const c=o.find(k=>k.path==="/system"||k.id===2);
        return c?.name||c?.title||"系统管理"
      }
      if(i==="monitor"){
        const c=o.find(k=>k.path==="/monitor"||k.id===3);
        return c?.name||c?.title||"系统监控"
      }
      if(i==="business"){
        const c=o.find(k=>k.path==="/business"||k.id===4);
        return c?.name||c?.title||"业务管理"
      }
      return""
    }
    ),Qt=f(()=>{
      const i=me.value,o=v.value?`/${v.value}`:"";
      return i==="system"?o+u.systemFirstPath:i==="monitor"?o+u.monitorFirstPath:i==="business"?o+u.businessFirstPath:w.path
    }
    ),Qe=f(()=>(p.theme,getComputedStyle(document.documentElement).getPropertyValue("--admin-sidebar-bg").trim()||"#fff")),Ke=f(()=>(p.theme,getComputedStyle(document.documentElement).getPropertyValue("--admin-sidebar-text").trim()||"#bfcbd9")),Je=f(()=>(p.theme,getComputedStyle(document.documentElement).getPropertyValue("--admin-sidebar-active").trim()||"#409EFF"));
    be(()=>w.path,()=>{
      A.value||C.addTab(w),ye(()=>{
        const i=ke();
        i&&"scrollTo"in i&&i.scrollTo({
          top:0
        }
        )
      }
      )
    }
    ,{
      immediate:!0
    }
    );
    function _t(){
      const i=String(w.params.tenantCode||"").trim();
      St().clear(i?Vt(i):void 0),C.clearAllTabsAndPins(),D.push({
        name:"AdminLogin",params:{
          tenantCode:w.params.tenantCode
        }
        
      }
      )
    }
    function Kt(i,o){
      const c=String(i?.tenantCode||"").trim();
      if(c)return c;
      const k=(i?.tenants??[]).find(I=>I.id===o);
      return k?.code?String(k.code).trim():null
    }
    async function ht(i){
      if(!H.value){
        H.value=!0;
        try{
          const o=(await $t.switchTenant(i)).data,c=o?.user,k=o?.token;
          if(!c||!k)throw new Error("切换租户失败：登录态返回异常");
          const I=Vt(i===0?String(w.params.tenantCode||"").trim():Kt(c,i)||String(w.params.tenantCode||"").trim());
          if(u.completeLogin(I,k,c),Dn(I),C.clearAllTabsAndPins(),i!==0){
            const Z=Ye(w.path)||"/admin/dashboard",ee=u.canAccess(Z)?Z:"/admin/dashboard";
            await D.push({
              path:`/${I}${ee}`
            }
            )
          }
          if(i===0)Ce.success("已切换为：不限制租户");
          else{
            const Z=(c.tenants??[]).find(ee=>ee.id===i);
            Ce.success(`已切换到租户：${Z?`${
              Z.name
            }
            （${
              Z.code
            }
            ）`:I}`)
          }
          
        }
        catch(o){
          Ce.error(o?.message||"切换租户失败")
        }
        finally{
          H.value=!1
        }
        
      }
      
    }
    async function Ze(){
      try{
        await Un.confirm("租户身份已切换。为避免缓存状态导致数据混用，建议立即刷新页面。","建议刷新",{
          confirmButtonText:"立即刷新",cancelButtonText:"稍后",distinguishCancelAndClose:!0,type:"warning"
        }
        ),window.location.reload()
      }
      catch{
        
      }
      
    }
    async function Jt(){
      if(ue.value&&!(Q.value.length>0))try{
        Q.value=((await Xn.list({
          page:1,pageSize:500
        }
        )).list??[]).filter(i=>i?.id!=null).map(i=>({
          id:i.id,name:String(i.name??""),code:String(i.code??"")
        }
        ))
      }
      catch{
        
      }
      
    }
    async function Zt(){
      const i=de.value;
      if(i===-1){
        if(await ht(0),H.value)return;
        u.setNoTenantLimit(!0),z.value=!1,await Ze();
        return
      }
      if(!i){
        z.value=!1;
        return
      }
      if(i===Pe.value){
        if(u.noTenantLimit){
          u.setNoTenantLimit(!1),Ce.success("已切换为：当前租户"),z.value=!1,await Ze();
          return
        }
        z.value=!1;
        return
      }
      u.setNoTenantLimit(!1),await ht(i),H.value||(z.value=!1,await Ze())
    }
    function et(i){
      if(i==="portal"){
        D.push(s.value);
        return
      }
      if(i==="password"){
        Y.value=!0;
        return
      }
      if(i==="logout"){
        _t();
        return
      }
      
    }
    return ct(async()=>{
      p.init(),p.apply(),xe(),window.addEventListener("resize",xe),J.ensureLoaded();
      const{
        list:i
      }
      =await Nn("/api/sys/menu/list");
      V.value=i||[],Jt(),ye(()=>m())
    }
    ),vt(()=>{
      window.removeEventListener("resize",xe),N&&(clearTimeout(N),N=null)
    }
    ),(i,o)=>{
      const c=Re("router-view"),k=mt,I=Ut,Z=Re("router-link"),ee=En,tt=gn,nt=rn,en=fn,Be=un,tn=Fn,nn=kn,an=xn,at=dn,ln=Lt,yt=Wt,on=Bt,ze=Tn,kt=In,xt=vn,lt=pt;
      return A.value?(d(),x("div",Cl,[n("main",{
        ref_key:"adminMainRef",ref:h,class:"admin-main admin-main--standalone-content"
      }
      ,[t(c,null,{
        default:a(({
          Component:g
        }
        )=>[(d(),b(qe,null,{
          default:a(()=>[t(Le,{
            name:Ne.value
          }
          ,{
            default:a(()=>[n("div",Ml,[g?(d(),b($e(g),{
              key:r(w).path
            }
            )):_("",!0)])]),_:2
          }
          ,1032,["name"])]),fallback:a(()=>[...o[24]||(o[24]=[n("div",{
            class:"route-loading"
          }
          ,"加载中…",-1)])]),_:2
        }
        ,1024))]),_:1
      }
      )],512),t(Ot,{
        visible:r(p).showWatermark,text:r(p).watermarkText||"BS Ball"
      }
      ,null,8,["visible","text"])])):(d(),x("div",{
        key:1,class:te(["admin-layout",{
          "admin-grey-mode":r(p).greyMode,"admin-color-weak":r(p).colorWeakMode,"admin-limit-width":r(p).constrainContentWidth,"admin-content-align-center":r(p).contentAlign==="center","admin-content-align-left":r(p).contentAlign==="left","admin-content-align-right":r(p).contentAlign==="right","admin-fill-page":Xt.value,"admin-layout--mobile-chrome-fixed":l.value&&r(p).mobileFixedHeader,"admin-layout--mobile-chrome-fluid":l.value&&!r(p).mobileFixedHeader
        }
        ]),"data-menu-mode":r(p).menuMode
      }
      ,[l.value&&L.value?(d(),x("div",{
        key:0,class:"sidebar-mask",onClick:o[0]||(o[0]=g=>L.value=!1)
      }
      )):_("",!0),r(p).menuMode==="mix"&&!l.value?(d(),b(at,{
        key:1,class:"admin-container-mix"
      }
      ,{
        default:a(()=>[t(en,{
          class:"admin-header admin-header--mix"
        }
        ,{
          default:a(()=>[n("div",Vl,[r(y).adminLogoUrl?(d(),x("img",{
            key:0,src:r(y).resolveAssetUrl(r(y).adminLogoUrl),class:"header-logo-img",alt:"logo"
          }
          ,null,8,Sl)):_("",!0),n("span",$l,F(r(y).getAdminTitle()),1)]),r(p).showBreadcrumb?(d(),b(Et,{
            key:0,class:"breadcrumb breadcrumb--mix"
          }
          )):_("",!0),n("div",Al,[l.value?(d(),b(I,{
            key:0,link:"",class:"icon-btn icon-btn--circle",onClick:o[1]||(o[1]=g=>G.value=!0),title:"界面设置"
          }
          ,{
            default:a(()=>[t(k,null,{
              default:a(()=>[t(r(_e))]),_:1
            }
            )]),_:1
          }
          )):(d(),b(I,{
            key:1,link:"",class:"icon-btn icon-btn--circle",onClick:o[2]||(o[2]=g=>G.value=!0),title:"界面设置"
          }
          ,{
            default:a(()=>[t(k,null,{
              default:a(()=>[t(r(_e))]),_:1
            }
            )]),_:1
          }
          )),t(nt,{
            trigger:se.value,onCommand:et
          }
          ,{
            dropdown:a(()=>[t(tt,null,{
              default:a(()=>[t(ee,null,{
                default:a(()=>[t(Z,{
                  class:"admin-dropdown-nav-link",to:s.value
                }
                ,{
                  default:a(()=>[...o[25]||(o[25]=[U("进入前台首页",-1)])]),_:1
                }
                ,8,["to"])]),_:1
              }
              ),t(ee,{
                divided:"",command:"password"
              }
              ,{
                default:a(()=>[...o[26]||(o[26]=[U("修改密码",-1)])]),_:1
              }
              ),t(ee,{
                command:"logout"
              }
              ,{
                default:a(()=>[...o[27]||(o[27]=[U("退出登录",-1)])]),_:1
              }
              )]),_:1
            }
            )]),default:a(()=>[n("span",El,[n("img",{
              src:T.value,class:te(["user-avatar",{
                "user-avatar--default":W.value
              }
              ]),alt:"avatar",onError:K
            }
            ,null,42,Ol),n("span",Pl,F(r(u).displayName||"admin"),1),t(k,null,{
              default:a(()=>[t(r(ot))]),_:1
            }
            )])]),_:1
          }
          ,8,["trigger"])])]),_:1
        }
        ),t(at,null,{
          default:a(()=>[t(tn,{
            width:`${r(p).sidebarWidth}px`,class:te(["admin-aside-mix",{
              "sidebar-scroll-visible":R.value
            }
            ]),onMouseenter:o[3]||(o[3]=g=>R.value=!0),onMouseleave:o[4]||(o[4]=g=>R.value=!1)
          }
          ,{
            default:a(()=>[t(Be,{
              "default-active":De.value,"unique-opened":r(p).menuUniqueOpened,router:"","background-color":Qe.value,"text-color":Ke.value,"active-text-color":Je.value,onSelect:we
            }
            ,{
              default:a(()=>[t(Ge,{
                menus:V.value,"menu-ids":le.value,"icons-map":ne.value
              }
              ,null,8,["menus","menu-ids","icons-map"])]),_:1
            }
            ,8,["default-active","unique-opened","background-color","text-color","active-text-color"])]),_:1
          }
          ,8,["width","class"]),t(at,{
            direction:"vertical"
          }
          ,{
            default:a(()=>[r(p).showTabs?(d(),b(it,{
              key:0
            }
            )):_("",!0),t(nn,{
              ref_key:"adminMainRef",ref:h,class:"admin-main admin-main--mix"
            }
            ,{
              default:a(()=>[t(c,null,{
                default:a(({
                  Component:g
                }
                )=>[(d(),b(qe,null,{
                  default:a(()=>[t(Le,{
                    name:Ne.value
                  }
                  ,{
                    default:a(()=>[n("div",Wl,[g?(d(),b($e(g),{
                      key:r(w).path
                    }
                    )):_("",!0)])]),_:2
                  }
                  ,1032,["name"])]),fallback:a(()=>[...o[28]||(o[28]=[n("div",{
                    class:"route-loading"
                  }
                  ,"加载中…",-1)])]),_:2
                }
                ,1024))]),_:1
              }
              )]),_:1
            }
            ,512),r(y).showFooterAdmin&&E.value?(d(),b(an,{
              key:1,class:"admin-footer"
            }
            ,{
              default:a(()=>[n("div",{
                class:"admin-footer__html",innerHTML:r(y).footerTextAdmin
              }
              ,null,8,Ul)]),_:1
            }
            )):_("",!0)]),_:1
          }
          )]),_:1
        }
        )]),_:1
      }
      )):r(p).menuMode==="sidebar"&&!l.value?(d(),x("div",Bl,[n("header",Ll,[n("div",Rl,[n("div",Il,[r(y).adminLogoUrl?(d(),x("img",{
        key:0,src:r(y).resolveAssetUrl(r(y).adminLogoUrl),class:"header-logo-img",alt:"logo"
      }
      ,null,8,Fl)):_("",!0),n("span",Hl,F(r(y).getAdminTitle()),1)]),t(Be,{
        mode:"horizontal","default-active":Qt.value,router:"",class:"top-menu top-menu--combo","background-color":"transparent"
      }
      ,{
        default:a(()=>[(d(!0),x(pe,null,Ae(Yt.value,g=>(d(),x(pe,{
          key:g.id
        }
        ,[g.index?(d(),b(ln,{
          key:0,index:g.index
        }
        ,{
          default:a(()=>[U(F(g.name),1)]),_:2
        }
        ,1032,["index"])):_("",!0)],64))),128))]),_:1
      }
      ,8,["default-active"])]),n("div",Dl,[l.value?(d(),b(I,{
        key:0,link:"",class:"icon-btn icon-btn--circle",onClick:o[5]||(o[5]=g=>G.value=!0),title:"界面设置"
      }
      ,{
        default:a(()=>[t(k,null,{
          default:a(()=>[t(r(_e))]),_:1
        }
        )]),_:1
      }
      )):(d(),b(I,{
        key:1,link:"",class:"icon-btn icon-btn--circle",onClick:o[6]||(o[6]=g=>G.value=!0),title:"界面设置"
      }
      ,{
        default:a(()=>[t(k,null,{
          default:a(()=>[t(r(_e))]),_:1
        }
        )]),_:1
      }
      )),t(nt,{
        trigger:se.value,onCommand:et
      }
      ,{
        dropdown:a(()=>[t(tt,null,{
          default:a(()=>[t(ee,null,{
            default:a(()=>[t(Z,{
              class:"admin-dropdown-nav-link",to:s.value
            }
            ,{
              default:a(()=>[...o[29]||(o[29]=[U("进入前台首页",-1)])]),_:1
            }
            ,8,["to"])]),_:1
          }
          ),t(ee,{
            divided:"",command:"password"
          }
          ,{
            default:a(()=>[...o[30]||(o[30]=[U("修改密码",-1)])]),_:1
          }
          ),t(ee,{
            command:"logout"
          }
          ,{
            default:a(()=>[...o[31]||(o[31]=[U("退出登录",-1)])]),_:1
          }
          )]),_:1
        }
        )]),default:a(()=>[n("span",Nl,[n("img",{
          src:T.value,class:te(["user-avatar",{
            "user-avatar--default":W.value
          }
          ]),alt:"avatar",onError:K
        }
        ,null,42,zl),n("span",ql,F(r(u).displayName||"admin"),1),t(k,null,{
          default:a(()=>[t(r(ot))]),_:1
        }
        )])]),_:1
      }
      ,8,["trigger"])])]),n("div",Gl,[n("aside",{
        class:te(["admin-sidebar admin-sidebar--combo",{
          "sidebar-combo-empty":!Ue.value,"sidebar-scroll-visible":O.value
        }
        ]),onMouseenter:o[7]||(o[7]=g=>O.value=!0),onMouseleave:o[8]||(o[8]=g=>O.value=!1)
      }
      ,[Ue.value?(d(),x("div",Yl,[n("div",jl,[me.value==="system"?(d(),b(k,{
        key:0,class:"sidebar-section-icon"
      }
      ,{
        default:a(()=>[t(r(_e))]),_:1
      }
      )):me.value==="monitor"?(d(),b(k,{
        key:1,class:"sidebar-section-icon"
      }
      ,{
        default:a(()=>[t(r(Ct))]),_:1
      }
      )):me.value==="business"?(d(),b(k,{
        key:2,class:"sidebar-section-icon"
      }
      ,{
        default:a(()=>[t(r(Tt))]),_:1
      }
      )):_("",!0),n("span",Xl,F(wt.value),1)])])):_("",!0),Te(t(Be,{
        "default-active":De.value,"unique-opened":r(p).menuUniqueOpened,router:"","background-color":Qe.value,"text-color":Ke.value,"active-text-color":Je.value,onSelect:we
      }
      ,{
        default:a(()=>[t(Ge,{
          menus:Gt.value,"menu-ids":le.value,"icons-map":ne.value
        }
        ,null,8,["menus","menu-ids","icons-map"])]),_:1
      }
      ,8,["default-active","unique-opened","background-color","text-color","active-text-color"]),[[Me,Ue.value]])],34),n("div",Ql,[r(p).showTabs?(d(),b(it,{
        key:0
      }
      )):_("",!0),n("main",{
        ref_key:"adminMainRef",ref:h,class:"admin-main"
      }
      ,[t(c,null,{
        default:a(({
          Component:g
        }
        )=>[(d(),b(qe,null,{
          default:a(()=>[t(Le,{
            name:Ne.value
          }
          ,{
            default:a(()=>[n("div",Kl,[g?(d(),b($e(g),{
              key:r(w).path
            }
            )):_("",!0)])]),_:2
          }
          ,1032,["name"])]),fallback:a(()=>[...o[32]||(o[32]=[n("div",{
            class:"route-loading"
          }
          ,"加载中…",-1)])]),_:2
        }
        ,1024))]),_:1
      }
      )],512)])])])):(d(),x(pe,{
        key:3
      }
      ,[l.value?(d(),x("aside",{
        key:0,class:te(["admin-sidebar",{
          "drawer-open":L.value,"sidebar-scroll-visible":q.value
        }
        ]),onMouseenter:o[10]||(o[10]=g=>q.value=!0),onMouseleave:o[11]||(o[11]=g=>q.value=!1)
      }
      ,[Ue.value?(d(),x("div",Jl,[n("div",Zl,[me.value==="system"?(d(),b(k,{
        key:0,class:"sidebar-section-icon"
      }
      ,{
        default:a(()=>[t(r(_e))]),_:1
      }
      )):me.value==="monitor"?(d(),b(k,{
        key:1,class:"sidebar-section-icon"
      }
      ,{
        default:a(()=>[t(r(Ct))]),_:1
      }
      )):me.value==="business"?(d(),b(k,{
        key:2,class:"sidebar-section-icon"
      }
      ,{
        default:a(()=>[t(r(Tt))]),_:1
      }
      )):_("",!0),n("span",eo,F(wt.value),1)]),l.value?(d(),b(I,{
        key:0,link:"",type:"primary",class:"sidebar-toggle",onClick:o[9]||(o[9]=g=>L.value=!1)
      }
      ,{
        default:a(()=>[t(k,null,{
          default:a(()=>[t(r(dt))]),_:1
        }
        )]),_:1
      }
      )):_("",!0)])):_("",!0),Te(t(Be,{
        "default-active":De.value,"unique-opened":r(p).menuUniqueOpened,collapse:!1,router:"","background-color":Qe.value,"text-color":Ke.value,"active-text-color":Je.value,onSelect:we
      }
      ,{
        default:a(()=>[t(Ge,{
          menus:V.value,"menu-ids":le.value,"icons-map":ne.value
        }
        ,null,8,["menus","menu-ids","icons-map"])]),_:1
      }
      ,8,["default-active","unique-opened","background-color","text-color","active-text-color"]),[[Me,Ue.value||l.value]])],34)):_("",!0),n("div",{
        ref_key:"adminBodyRef",ref:S,class:"admin-body"
      }
      ,[n("header",to,[n("div",no,[l.value?(d(),x("div",ao,[r(y).adminLogoUrl?(d(),x("img",{
        key:0,src:r(y).resolveAssetUrl(r(y).adminLogoUrl),class:"header-logo-img",alt:"logo"
      }
      ,null,8,lo)):_("",!0),n("span",oo,F(r(y).getAdminTitle()),1)])):r(p).menuMode==="top"||r(p).menuMode==="mix"||r(p).menuMode==="sidebar"?(d(),x("div",so,[r(y).adminLogoUrl?(d(),x("img",{
        key:0,src:r(y).resolveAssetUrl(r(y).adminLogoUrl),class:"header-logo-img",alt:"logo"
      }
      ,null,8,io)):_("",!0),n("span",ro,F(r(y).getAdminTitle()),1)])):_("",!0)]),r(p).menuMode==="top"&&!l.value?(d(),b(Be,{
        key:0,mode:"horizontal","default-active":De.value,router:"",class:"top-menu","background-color":"transparent"
      }
      ,{
        default:a(()=>[t(Ge,{
          menus:V.value,"menu-ids":le.value,"icons-map":ne.value
        }
        ,null,8,["menus","menu-ids","icons-map"])]),_:1
      }
      ,8,["default-active"])):jt.value?(d(),b(Et,{
        key:1,class:"breadcrumb"
      }
      )):_("",!0),n("div",uo,[l.value?(d(),b(I,{
        key:0,link:"",class:"icon-btn icon-btn--circle",onClick:o[12]||(o[12]=g=>G.value=!0),title:"界面设置"
      }
      ,{
        default:a(()=>[t(k,null,{
          default:a(()=>[t(r(_e))]),_:1
        }
        )]),_:1
      }
      )):(d(),b(I,{
        key:1,link:"",class:"icon-btn icon-btn--circle",onClick:o[13]||(o[13]=g=>G.value=!0),title:"界面设置"
      }
      ,{
        default:a(()=>[t(k,null,{
          default:a(()=>[t(r(_e))]),_:1
        }
        )]),_:1
      }
      )),t(nt,{
        trigger:se.value,onCommand:et
      }
      ,{
        dropdown:a(()=>[t(tt,null,{
          default:a(()=>[t(ee,null,{
            default:a(()=>[t(Z,{
              class:"admin-dropdown-nav-link",to:s.value
            }
            ,{
              default:a(()=>[...o[33]||(o[33]=[U("进入前台首页",-1)])]),_:1
            }
            ,8,["to"])]),_:1
          }
          ),t(ee,{
            divided:"",command:"password"
          }
          ,{
            default:a(()=>[...o[34]||(o[34]=[U("修改密码",-1)])]),_:1
          }
          ),t(ee,{
            command:"logout"
          }
          ,{
            default:a(()=>[...o[35]||(o[35]=[U("退出登录",-1)])]),_:1
          }
          )]),_:1
        }
        )]),default:a(()=>[n("span",{
          class:te(["user-trigger",{
            "user-trigger--mobile":l.value
          }
          ]),role:"button",tabindex:"0","aria-label":"用户菜单"
        }
        ,[n("img",{
          src:T.value,class:te(["user-avatar",{
            "user-avatar--default":W.value
          }
          ]),alt:"avatar",onError:K
        }
        ,null,42,mo),n("span",po,F(r(u).displayName||"admin"),1),l.value?_("",!0):(d(),b(k,{
          key:0,class:"user-trigger__caret"
        }
        ,{
          default:a(()=>[t(r(ot))]),_:1
        }
        ))],2)]),_:1
      }
      ,8,["trigger"]),l.value?(d(),b(I,{
        key:2,link:"",class:"hamburger",onClick:o[14]||(o[14]=g=>L.value=!0),title:"展开菜单"
      }
      ,{
        default:a(()=>[t(k,null,{
          default:a(()=>[t(r(hn))]),_:1
        }
        )]),_:1
      }
      )):_("",!0)])]),r(p).showTabs?(d(),b(it,{
        key:0
      }
      )):_("",!0),n("main",{
        ref_key:"adminMainRef",ref:h,class:"admin-main"
      }
      ,[t(c,null,{
        default:a(({
          Component:g
        }
        )=>[(d(),b(qe,null,{
          default:a(()=>[t(Le,{
            name:Ne.value
          }
          ,{
            default:a(()=>[n("div",vo,[g?(d(),b($e(g),{
              key:r(w).path
            }
            )):_("",!0)])]),_:2
          }
          ,1032,["name"])]),fallback:a(()=>[...o[36]||(o[36]=[n("div",{
            class:"route-loading"
          }
          ,"加载中…",-1)])]),_:2
        }
        ,1024))]),_:1
      }
      )],512)],512)],64)),(d(),b(rt,{
        to:"body"
      }
      ,[t(hl,{
        modelValue:G.value,"onUpdate:modelValue":o[15]||(o[15]=g=>G.value=g)
      }
      ,null,8,["modelValue"])])),t(xt,{
        modelValue:z.value,"onUpdate:modelValue":o[18]||(o[18]=g=>z.value=g),title:"切换租户",width:"420","close-on-click-modal":r(p).modalCloseOnClickMask
      }
      ,{
        footer:a(()=>[t(I,{
          onClick:o[17]||(o[17]=g=>z.value=!1)
        }
        ,{
          default:a(()=>[...o[37]||(o[37]=[U("取消",-1)])]),_:1
        }
        ),t(I,{
          type:"primary",loading:H.value,onClick:Zt
        }
        ,{
          default:a(()=>[...o[38]||(o[38]=[U("切换",-1)])]),_:1
        }
        ,8,["loading"])]),default:a(()=>[t(kt,{
          "label-width":"88px"
        }
        ,{
          default:a(()=>[t(ze,{
            label:"目标租户"
          }
          ,{
            default:a(()=>[t(on,{
              modelValue:de.value,"onUpdate:modelValue":o[16]||(o[16]=g=>de.value=g),style:{
                width:"100%"
              }
              ,placeholder:"请选择租户"
            }
            ,{
              default:a(()=>[ue.value?(d(),b(yt,{
                key:0,label:"不限制租户（全局）",value:-1
              }
              )):_("",!0),(d(!0),x(pe,null,Ae(fe.value,g=>(d(),b(yt,{
                key:`switch-${g.id}`,label:`${g.name}（${g.code}）`,value:g.id
              }
              ,null,8,["label","value"]))),128))]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          )]),_:1
        }
        )]),_:1
      }
      ,8,["modelValue","close-on-click-modal"]),t(xt,{
        modelValue:Y.value,"onUpdate:modelValue":o[23]||(o[23]=g=>Y.value=g),title:"修改密码",width:"400","close-on-click-modal":r(p).modalCloseOnClickMask,onClose:Fe
      }
      ,{
        footer:a(()=>[t(I,{
          onClick:o[22]||(o[22]=g=>Y.value=!1)
        }
        ,{
          default:a(()=>[...o[39]||(o[39]=[U("取消",-1)])]),_:1
        }
        ),t(I,{
          type:"primary",loading:X.value,onClick:He
        }
        ,{
          default:a(()=>[...o[40]||(o[40]=[U("确定",-1)])]),_:1
        }
        ,8,["loading"])]),default:a(()=>[t(kt,{
          model:j,rules:Se.value,ref_key:"passwordFormRef",ref:oe,"label-width":"100px",class:"password-form"
        }
        ,{
          default:a(()=>[t(ze,{
            label:"原密码",prop:"oldPassword"
          }
          ,{
            default:a(()=>[t(lt,{
              modelValue:j.oldPassword,"onUpdate:modelValue":o[19]||(o[19]=g=>j.oldPassword=g),type:"password","show-password":"",placeholder:"请输入原密码"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(ze,{
            label:"新密码",prop:"newPassword"
          }
          ,{
            default:a(()=>[t(lt,{
              modelValue:j.newPassword,"onUpdate:modelValue":o[20]||(o[20]=g=>j.newPassword=g),type:"password","show-password":"",placeholder:"至少 6 位"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(ze,{
            label:"确认新密码",prop:"confirmPassword"
          }
          ,{
            default:a(()=>[t(lt,{
              modelValue:j.confirmPassword,"onUpdate:modelValue":o[21]||(o[21]=g=>j.confirmPassword=g),type:"password","show-password":"",placeholder:"请再次输入新密码"
            }
            ,null,8,["modelValue"])]),_:1
          }
          )]),_:1
        }
        ,8,["model","rules"])]),_:1
      }
      ,8,["modelValue","close-on-click-modal"]),t(Ot,{
        visible:r(p).showWatermark,text:r(p).watermarkText||"BS Ball"
      }
      ,null,8,["visible","text"]),(d(),b(rt,{
        to:"body"
      }
      ,[t(Le,{
        name:"admin-back-top-fade"
      }
      ,{
        default:a(()=>[Te(n("button",{
          type:"button",class:"floating-backtop-btn admin-layout-backtop-fab",title:"返回顶部","aria-label":"返回顶部",onClick:e
        }
        ,[t(k,null,{
          default:a(()=>[t(r(pn))]),_:1
        }
        )],512),[[Me,l.value&&Oe.value]])]),_:1
      }
      )]))],10,Tl))
    }
    
  }
  
}
),Po=Ee(fo,[["__scopeId","data-v-c4075566"]])

export default Po;
