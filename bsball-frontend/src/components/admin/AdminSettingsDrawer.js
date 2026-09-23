// AdminSettingsDrawer —— 行为保真移植自编译产物 AdminLayout chunk（recon-gen-admin-layout.mjs 生成，勿手改）
// 生成源：.qoder/frontend-recon/out/beautified/AdminLayout-Cx0XHwPP.pretty.js
// 别名与编译产物局部名一致（经 recon-probe-ep.mjs 运行时探针核实）
import { createVNode as t, createElementVNode as n, createBlock as b, createElementBlock as x, openBlock as d, withCtx as a, createTextVNode as U, createCommentVNode as _, toDisplayString as F, unref as r, normalizeClass as te, defineComponent as Ve, ref as $, computed as f, watch as be, onMounted as ct, onBeforeUnmount as vt, reactive as Rt, useModel as Vn } from 'vue';
import { ElDrawer as bn, ElTabs as $n, ElTabPane as Pn, ElRadioGroup as sn, ElRadioButton as _n, ElInputNumber as mn, ElSwitch as Bn, ElTooltip as Ln, ElSelect as Bt, ElOption as Wt, ElInput as pt, ElButton as Ut, ElMessage as Ce } from 'element-plus';
import { Close as dt } from '@element-plus/icons-vue';
import { useSettingsStore as Ht } from '../../stores/settings';
import { useTabsStore as ft } from '../../stores/tabs';
import { useMediaQuery as It } from '../../composables/useMediaQuery';
import { parseBrowserInfo as jn } from '../../utils/browserInfo';
import { exportSfc as Ee } from '../../utils/exportSfc';
import va from './AdminPresetColorField.vue';
var Dt=["#409EFF","#0d6efd","#1677ff","#67C23A","#E6A23C","#F56C6C","#626aef","#7c3aed","#0891b2","#0d9488","#ea580c","#64748b"],Oo=Dt.length,ca={
  class:"settings-body settings-body--pane"
}
,fa={
  class:"setting-group setting-row setting-row--controls"
}
,ga={
  class:"group-title-cell"
}
,ba={
  class:"setting-group setting-row"
}
,wa={
  class:"group-title-cell"
}
,_a={
  key:0,class:"setting-group"
}
,ha={
  class:"setting-row"
}
,ya={
  class:"group-title-cell"
}
,ka={
  class:"setting-group"
}
,xa={
  class:"setting-row"
}
,Ca={
  class:"group-title-cell"
}
,Ma={
  class:"setting-group setting-row"
}
,Ta={
  class:"group-title-cell"
}
,Va={
  key:1,class:"setting-group setting-row"
}
,Sa={
  class:"group-title-cell"
}
,$a={
  key:2,class:"setting-group setting-row"
}
,Aa={
  class:"group-title-cell"
}
,Ea={
  class:"setting-group"
}
,Oa={
  class:"setting-row"
}
,Pa={
  class:"group-title-cell"
}
,Wa={
  key:0,class:"setting-row",style:{
    "margin-top":"8px"
  }
  
}
,Ua={
  class:"group-title-cell"
}
,Ba={
  key:1,class:"setting-row setting-row--controls",style:{
    "margin-top":"8px"
  }
  
}
,La={
  class:"group-title-cell"
}
,Ra={
  class:"setting-group setting-row"
}
,Ia={
  class:"group-title-cell"
}
,Fa={
  class:"setting-group setting-row"
}
,Ha={
  class:"group-title-cell"
}
,Da={
  class:"setting-group setting-row"
}
,Na={
  class:"group-title-cell"
}
,za={
  class:"setting-group"
}
,qa={
  class:"setting-row"
}
,Ga={
  class:"group-title-cell"
}
,Ya={
  class:"setting-group setting-row"
}
,ja={
  class:"group-title-cell"
}
,Xa={
  class:"setting-group setting-row setting-row--controls"
}
,Qa={
  class:"group-title-cell"
}
,Ka={
  class:"settings-body settings-body--pane"
}
,Ja={
  class:"setting-group setting-row setting-row--controls"
}
,Za={
  class:"group-title-cell"
}
,el={
  class:"setting-group setting-row setting-row--color-block"
}
,tl={
  class:"group-title-cell"
}
,nl={
  class:"setting-group setting-row"
}
,al={
  class:"group-title-cell"
}
,ll={
  class:"setting-group setting-row"
}
,ol={
  class:"group-title-cell"
}
,sl={
  class:"setting-group"
}
,il={
  class:"setting-row"
}
,rl={
  class:"group-title-cell"
}
,dl={
  class:"settings-body settings-body--pane"
}
,ul={
  class:"env-info-list"
}
,ml={
  class:"env-info-row"
}
,pl={
  class:"env-info-row"
}
,vl={
  class:"env-info-row"
}
,cl={
  class:"env-info-row"
}
,fl={
  class:"env-info-row"
}
,gl={
  class:"env-info-row"
}
,bl={
  class:"env-info-row"
}
,wl={
  class:"env-info-row"
}
,_l=Ve({
  __name:"AdminSettingsDrawer",props:{
    modelValue:{
      type:Boolean,default:!1
    }
    ,modelModifiers:{
      
    }
    
  }
  ,emits:["update:modelValue"],setup(B){
    const w="1.0.0",D="2026-07-01T14:33:17.536+08:00",A="3.5.38",p="2.14.2",C="8.0.13",y=f(()=>{
      const m=new Date(D);
      if(Number.isNaN(m.getTime()))return D;
      const e=V=>String(V).padStart(2,"0");
      return`${m.getFullYear()}-${e(m.getMonth()+1)}-${e(m.getDate())} ${e(m.getHours())}:${e(m.getMinutes())}:${e(m.getSeconds())}`
    }
    ),E=Vn(B,"modelValue"),u=Ht(),h=$(!1),S=It("(max-width: 768px)"),L=f(()=>S.value?"92vw":"450px"),R=$("layout"),O=$("—"),q=$("—"),G=$("—"),Y=$("—");
    function oe(){
      if(typeof window>"u")return;
      const m=jn(navigator.userAgent||"");
      O.value=m.name,q.value=m.version,G.value=`${window.screen.width} × ${window.screen.height}`,Y.value=`${window.innerWidth} × ${window.innerHeight}`
    }
    ct(()=>{
      oe(),window.addEventListener("resize",oe)
    }
    ),vt(()=>{
      window.removeEventListener("resize",oe)
    }
    );
    const l=Rt((()=>{
      const m=u.getSnapshot();
      return{
        theme:m.theme,primaryColor:m.primaryColor,menuMode:m.menuMode,showTabs:m.showTabs,retainOpenTabs:typeof m.retainOpenTabs=="boolean"?m.retainOpenTabs:typeof m.cacheOpenTabs=="boolean"?m.cacheOpenTabs:!1,showBreadcrumb:m.showBreadcrumb,listRenderMode:m.listRenderMode,listStripe:m.listStripe,pageTransition:m.pageTransition,modalCloseOnClickMask:m.modalCloseOnClickMask,showWatermark:m.showWatermark,watermarkText:m.watermarkText,greyMode:m.greyMode,colorWeakMode:m.colorWeakMode,sidebarWidth:m.sidebarWidth,constrainContentWidth:m.constrainContentWidth,contentMaxWidth:m.contentMaxWidth,contentAlign:m.contentAlign,fillPageHeight:m.fillPageHeight,menuUniqueOpened:typeof m.menuUniqueOpened=="boolean"?m.menuUniqueOpened:!1,fixedOperationColumn:typeof m.fixedOperationColumn=="boolean"?m.fixedOperationColumn:!1,mobileFixedHeader:typeof m.mobileFixedHeader=="boolean"?m.mobileFixedHeader:!1
      }
      
    }
    )()),se=f({
      get:()=>!l.modalCloseOnClickMask,set:m=>{
        l.modalCloseOnClickMask=!m
      }
      
    }
    );
    let ve={
      
    };
    function ke(){
      u.setTheme(l.theme),u.setPrimaryColor(l.primaryColor),u.setMenuMode(l.menuMode),u.setShowTabs(l.showTabs),u.setShowBreadcrumb(l.showBreadcrumb),u.setListRenderMode(l.listRenderMode),u.setListStripe(l.listStripe),u.setPageTransition(l.pageTransition),u.setModalCloseOnClickMask(l.modalCloseOnClickMask),u.setShowWatermark(l.showWatermark),u.setWatermarkText(l.watermarkText),u.setGreyMode(l.greyMode),u.setColorWeakMode(l.colorWeakMode),u.setSidebarWidth(l.sidebarWidth),u.setConstrainContentWidth(l.constrainContentWidth),u.setContentMaxWidth(l.contentMaxWidth),u.setContentAlign(l.contentAlign),u.setFillPageHeight(l.fillPageHeight),u.setMenuUniqueOpened(l.menuUniqueOpened),u.setFixedOperationColumn(l.fixedOperationColumn),u.setMobileFixedHeader(l.mobileFixedHeader)
    }
    function ce(){
      ve=u.getSnapshot()
    }
    function ie(){
      u.restoreFromSnapshot(ve)
    }
    be(E,m=>{
      if(m){
        const e=u.getSnapshot();
        l.theme=e.theme,l.primaryColor=e.primaryColor,l.menuMode=e.menuMode,l.showTabs=e.showTabs,l.retainOpenTabs=typeof e.retainOpenTabs=="boolean"?e.retainOpenTabs:typeof e.cacheOpenTabs=="boolean"?e.cacheOpenTabs:!1,l.showBreadcrumb=e.showBreadcrumb,l.listRenderMode=e.listRenderMode,l.listStripe=e.listStripe,l.pageTransition=e.pageTransition,l.modalCloseOnClickMask=e.modalCloseOnClickMask,l.showWatermark=e.showWatermark,l.watermarkText=e.watermarkText,l.greyMode=e.greyMode,l.colorWeakMode=e.colorWeakMode,l.sidebarWidth=e.sidebarWidth,l.constrainContentWidth=e.constrainContentWidth,l.contentMaxWidth=e.contentMaxWidth,l.contentAlign=e.contentAlign,l.fillPageHeight=e.fillPageHeight,l.menuUniqueOpened=typeof e.menuUniqueOpened=="boolean"?e.menuUniqueOpened:!1,l.fixedOperationColumn=typeof e.fixedOperationColumn=="boolean"?e.fixedOperationColumn:!1,l.mobileFixedHeader=typeof e.mobileFixedHeader=="boolean"?e.mobileFixedHeader:!1,R.value="layout",ce(),h.value=!1
      }
      else h.value||(ie(),u.apply()),h.value=!1
    }
    ),be(()=>({
      ...l
    }
    ),()=>{
      E.value&&(ke(),u.applyToDOM())
    }
    ,{
      deep:!0
    }
    );
    function re(){
      ke(),u.setRetainOpenTabs(l.retainOpenTabs),u.apply();
      const m=ft();
      l.retainOpenTabs?m.persistNow():m.clearPersistedStorage(),Ce.success("保存成功"),h.value=!0,E.value=!1
    }
    function N(){
      ie(),u.apply(),E.value=!1
    }
    function Oe(){
      u.resetToDefault();
      const m=u.getSnapshot();
      l.theme=m.theme,l.primaryColor=m.primaryColor,l.menuMode=m.menuMode,l.showTabs=m.showTabs,l.retainOpenTabs=typeof m.retainOpenTabs=="boolean"?m.retainOpenTabs:typeof m.cacheOpenTabs=="boolean"?m.cacheOpenTabs:!1,l.showBreadcrumb=m.showBreadcrumb,l.listRenderMode=m.listRenderMode,l.listStripe=m.listStripe,l.pageTransition=m.pageTransition,l.modalCloseOnClickMask=m.modalCloseOnClickMask,l.showWatermark=m.showWatermark,l.watermarkText=m.watermarkText,l.greyMode=m.greyMode,l.colorWeakMode=m.colorWeakMode,l.sidebarWidth=m.sidebarWidth,l.constrainContentWidth=m.constrainContentWidth,l.contentMaxWidth=m.contentMaxWidth,l.contentAlign=m.contentAlign,l.fillPageHeight=m.fillPageHeight,l.menuUniqueOpened=typeof m.menuUniqueOpened=="boolean"?m.menuUniqueOpened:!1,l.fixedOperationColumn=typeof m.fixedOperationColumn=="boolean"?m.fixedOperationColumn:!1,l.mobileFixedHeader=typeof m.mobileFixedHeader=="boolean"?m.mobileFixedHeader:!1,ce()
    }
    return(m,e)=>{
      const V=Ln,J=_n,ne=sn,le=mn,H=Bn,z=Wt,de=Bt,Q=Pn,ue=pt,Pe=$n,fe=Ut,v=bn;
      return d(),b(v,{
        modelValue:E.value,"onUpdate:modelValue":e[23]||(e[23]=s=>E.value=s),title:"界面设置",direction:"rtl",class:"admin-settings-drawer",size:L.value,"close-on-click-modal":l.modalCloseOnClickMask
      }
      ,{
        footer:a(()=>[n("div",{
          class:te(["drawer-footer",{
            "drawer-footer--compact":r(S)
          }
          ])
        }
        ,[t(fe,{
          onClick:Oe
        }
        ,{
          default:a(()=>[...e[83]||(e[83]=[U("恢复默认",-1)])]),_:1
        }
        ),t(fe,{
          onClick:N
        }
        ,{
          default:a(()=>[...e[84]||(e[84]=[U("取消",-1)])]),_:1
        }
        ),t(fe,{
          type:"primary",onClick:re
        }
        ,{
          default:a(()=>[...e[85]||(e[85]=[U("保存",-1)])]),_:1
        }
        )],2)]),default:a(()=>[t(Pe,{
          modelValue:R.value,"onUpdate:modelValue":e[22]||(e[22]=s=>R.value=s),class:"settings-tabs","tab-position":"top",stretch:r(S)
        }
        ,{
          default:a(()=>[t(Q,{
            label:"布局",name:"layout"
          }
          ,{
            default:a(()=>[n("div",ca,[n("div",fa,[n("div",ga,[e[25]||(e[25]=n("span",{
              class:"group-title"
            }
            ,"菜单布局",-1)),t(V,{
              content:"选择左侧竖栏、顶部横栏或「左侧 + 顶部」组合方式；可随时切换对比。",placement:"top"
            }
            ,{
              default:a(()=>[...e[24]||(e[24]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(ne,{
              modelValue:l.menuMode,"onUpdate:modelValue":e[0]||(e[0]=s=>l.menuMode=s),size:"small",class:"settings-segmented settings-segmented--inline settings-segmented--menu"
            }
            ,{
              default:a(()=>[t(J,{
                value:"mix"
              }
              ,{
                default:a(()=>[...e[26]||(e[26]=[U("左侧菜单",-1)])]),_:1
              }
              ),t(J,{
                value:"top"
              }
              ,{
                default:a(()=>[...e[27]||(e[27]=[U("顶部菜单",-1)])]),_:1
              }
              ),t(J,{
                value:"sidebar"
              }
              ,{
                default:a(()=>[...e[28]||(e[28]=[U("组合菜单",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])]),n("div",ba,[n("div",wa,[e[30]||(e[30]=n("span",{
              class:"group-title"
            }
            ,"左侧栏宽度",-1)),t(V,{
              content:"左侧菜单展开时的宽度（像素）；仅在「左侧菜单」「组合菜单」下生效，数值越大侧栏越宽。",placement:"top"
            }
            ,{
              default:a(()=>[...e[29]||(e[29]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(le,{
              modelValue:l.sidebarWidth,"onUpdate:modelValue":e[1]||(e[1]=s=>l.sidebarWidth=s),min:170,max:400,step:10,style:{
                width:"120px"
              }
              
            }
            ,null,8,["modelValue"])]),l.menuMode!=="top"?(d(),x("div",_a,[n("div",ha,[n("div",ya,[e[32]||(e[32]=n("span",{
              class:"group-title"
            }
            ,"左侧栏手风琴模式",-1)),t(V,{
              content:"开启后，左侧一级菜单同时只展开一组：点开新的一级菜单时，会自动收起其它组。",placement:"top"
            }
            ,{
              default:a(()=>[...e[31]||(e[31]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.menuUniqueOpened,"onUpdate:modelValue":e[2]||(e[2]=s=>l.menuUniqueOpened=s)
            }
            ,null,8,["modelValue"])])])):_("",!0),n("div",ka,[n("div",xa,[n("div",Ca,[e[34]||(e[34]=n("span",{
              class:"group-title"
            }
            ,"窄屏顶栏固定",-1)),t(V,{
              content:"在手机、窄浏览器宽度下，向下滚动页面时顶部导航保持贴在屏幕上沿，便于随时点菜单。",placement:"top"
            }
            ,{
              default:a(()=>[...e[33]||(e[33]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.mobileFixedHeader,"onUpdate:modelValue":e[3]||(e[3]=s=>l.mobileFixedHeader=s)
            }
            ,null,8,["modelValue"])])]),n("div",Ma,[n("div",Ta,[e[36]||(e[36]=n("span",{
              class:"group-title"
            }
            ,"显示标签栏",-1)),t(V,{
              content:"在页面上方显示已打开页面的标签，点击可快速切换；关闭后仅通过左侧菜单打开页面。",placement:"top"
            }
            ,{
              default:a(()=>[...e[35]||(e[35]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.showTabs,"onUpdate:modelValue":e[4]||(e[4]=s=>l.showTabs=s)
            }
            ,null,8,["modelValue"])]),l.showTabs?(d(),x("div",Va,[n("div",Sa,[e[38]||(e[38]=n("span",{
              class:"group-title"
            }
            ,"保留标签页",-1)),t(V,{
              content:"开启后会在本地记住已打开的标签，刷新或下次进入后台时尽量恢复（依赖浏览器本地存储）。",placement:"top"
            }
            ,{
              default:a(()=>[...e[37]||(e[37]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.retainOpenTabs,"onUpdate:modelValue":e[5]||(e[5]=s=>l.retainOpenTabs=s)
            }
            ,null,8,["modelValue"])])):_("",!0),l.menuMode!=="top"&&l.menuMode!=="sidebar"?(d(),x("div",$a,[n("div",Aa,[e[40]||(e[40]=n("span",{
              class:"group-title"
            }
            ,"显示导航路径",-1)),t(V,{
              content:"在内容区上方显示「首页 / 模块 / 页面」路径，方便确认当前位置和返回上级。",placement:"top"
            }
            ,{
              default:a(()=>[...e[39]||(e[39]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.showBreadcrumb,"onUpdate:modelValue":e[6]||(e[6]=s=>l.showBreadcrumb=s)
            }
            ,null,8,["modelValue"])])):_("",!0),n("div",Ea,[n("div",Oa,[n("div",Pa,[e[42]||(e[42]=n("span",{
              class:"group-title"
            }
            ,"主内容区限制最大宽度",-1)),t(V,{
              content:"开启后，列表、表单等主内容不会无限拉满超宽屏，可配合下方数值与对齐方式；推荐宽度约 1200–1600（大屏更易读）。",placement:"top"
            }
            ,{
              default:a(()=>[...e[41]||(e[41]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.constrainContentWidth,"onUpdate:modelValue":e[7]||(e[7]=s=>l.constrainContentWidth=s)
            }
            ,null,8,["modelValue"])]),l.constrainContentWidth?(d(),x("div",Wa,[n("div",Ua,[e[44]||(e[44]=n("span",{
              class:"group-title"
            }
            ,"最大宽度（px）",-1)),t(V,{
              content:"主内容区域允许的最大像素宽度；数值越大，可编辑表格等横向空间越大。",placement:"top"
            }
            ,{
              default:a(()=>[...e[43]||(e[43]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(le,{
              modelValue:l.contentMaxWidth,"onUpdate:modelValue":e[8]||(e[8]=s=>l.contentMaxWidth=s),min:1200,max:1800,step:40,style:{
                width:"120px"
              }
              
            }
            ,null,8,["modelValue"])])):_("",!0),l.constrainContentWidth?(d(),x("div",Ba,[n("div",La,[e[46]||(e[46]=n("span",{
              class:"group-title"
            }
            ,"内容区对齐",-1)),t(V,{
              content:"在限制宽度后，整块主内容在可用区域里的水平位置：靠左最省空间，居中适合阅读型页面。",placement:"top"
            }
            ,{
              default:a(()=>[...e[45]||(e[45]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(ne,{
              modelValue:l.contentAlign,"onUpdate:modelValue":e[9]||(e[9]=s=>l.contentAlign=s),size:"small",class:"settings-segmented settings-segmented--inline"
            }
            ,{
              default:a(()=>[t(J,{
                value:"left"
              }
              ,{
                default:a(()=>[...e[47]||(e[47]=[U("靠左",-1)])]),_:1
              }
              ),t(J,{
                value:"center"
              }
              ,{
                default:a(()=>[...e[48]||(e[48]=[U("居中",-1)])]),_:1
              }
              ),t(J,{
                value:"right"
              }
              ,{
                default:a(()=>[...e[49]||(e[49]=[U("靠右",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])])):_("",!0)]),n("div",Ra,[n("div",Ia,[e[51]||(e[51]=n("span",{
              class:"group-title"
            }
            ,"固定列表筛选与分页",-1)),t(V,{
              content:"开启后：筛选条件固定在上方、分页条固定在下方，只有中间表格区域纵向滚动；窄屏下会自动关闭以免遮挡。",placement:"top"
            }
            ,{
              default:a(()=>[...e[50]||(e[50]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.fillPageHeight,"onUpdate:modelValue":e[10]||(e[10]=s=>l.fillPageHeight=s)
            }
            ,null,8,["modelValue"])]),n("div",Fa,[n("div",Ha,[e[53]||(e[53]=n("span",{
              class:"group-title"
            }
            ,"表格竖分割线",-1)),t(V,{
              content:"关闭后表格更简洁；开启后列与列之间有竖线，便于对准同一列阅读。",placement:"top"
            }
            ,{
              default:a(()=>[...e[52]||(e[52]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.listRenderMode,"onUpdate:modelValue":e[11]||(e[11]=s=>l.listRenderMode=s),"active-value":"bordered","inactive-value":"plain"
            }
            ,null,8,["modelValue"])]),n("div",Da,[n("div",Na,[e[55]||(e[55]=n("span",{
              class:"group-title"
            }
            ,"表格隔行底色",-1)),t(V,{
              content:"俗称斑马纹：奇偶行背景色交替，横向扫读长列表时更容易对齐同一行。",placement:"top"
            }
            ,{
              default:a(()=>[...e[54]||(e[54]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.listStripe,"onUpdate:modelValue":e[12]||(e[12]=s=>l.listStripe=s)
            }
            ,null,8,["modelValue"])]),n("div",za,[n("div",qa,[n("div",Ga,[e[57]||(e[57]=n("span",{
              class:"group-title"
            }
            ,"固定右侧操作列",-1)),t(V,{
              content:"开启后，列表最右侧「操作」列在横向滚动时保持可见；关闭后操作列随表格一起左右滚动。",placement:"top"
            }
            ,{
              default:a(()=>[...e[56]||(e[56]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.fixedOperationColumn,"onUpdate:modelValue":e[13]||(e[13]=s=>l.fixedOperationColumn=s)
            }
            ,null,8,["modelValue"])])]),n("div",Ya,[n("div",ja,[e[59]||(e[59]=n("span",{
              class:"group-title"
            }
            ,"弹框仅按钮关闭",-1)),t(V,{
              content:"开启：点击弹窗外的半透明遮罩不会关闭弹框/抽屉，只能通过底部按钮或右上角关闭；关闭：点击遮罩即可关闭。",placement:"top"
            }
            ,{
              default:a(()=>[...e[58]||(e[58]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:se.value,"onUpdate:modelValue":e[14]||(e[14]=s=>se.value=s)
            }
            ,null,8,["modelValue"])]),n("div",Xa,[n("div",Qa,[e[61]||(e[61]=n("span",{
              class:"group-title"
            }
            ,"页面切换动画",-1)),t(V,{
              content:"切换菜单打开不同页面时，主内容区的过渡效果；选「无」可减少动画、略减轻卡顿感。",placement:"top"
            }
            ,{
              default:a(()=>[...e[60]||(e[60]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(de,{
              modelValue:l.pageTransition,"onUpdate:modelValue":e[15]||(e[15]=s=>l.pageTransition=s),class:"settings-select-grow",placeholder:"选择"
            }
            ,{
              default:a(()=>[t(z,{
                label:"无",value:"none"
              }
              ),t(z,{
                label:"淡入淡出",value:"fade"
              }
              ),t(z,{
                label:"滑动",value:"slide"
              }
              ),t(z,{
                label:"缩放",value:"zoom"
              }
              )]),_:1
            }
            ,8,["modelValue"])])])]),_:1
          }
          ),t(Q,{
            label:"外观",name:"appearance"
          }
          ,{
            default:a(()=>[n("div",Ka,[n("div",Ja,[n("div",Za,[e[63]||(e[63]=n("span",{
              class:"group-title"
            }
            ,"外观深浅",-1)),t(V,{
              content:"浅色 / 深色界面，或跟随系统昼夜模式自动切换。",placement:"top"
            }
            ,{
              default:a(()=>[...e[62]||(e[62]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(ne,{
              modelValue:l.theme,"onUpdate:modelValue":e[16]||(e[16]=s=>l.theme=s),size:"small",class:"settings-segmented settings-segmented--inline"
            }
            ,{
              default:a(()=>[t(J,{
                value:"light"
              }
              ,{
                default:a(()=>[...e[64]||(e[64]=[U("浅色",-1)])]),_:1
              }
              ),t(J,{
                value:"dark"
              }
              ,{
                default:a(()=>[...e[65]||(e[65]=[U("深色",-1)])]),_:1
              }
              ),t(J,{
                value:"auto"
              }
              ,{
                default:a(()=>[...e[66]||(e[66]=[U("跟随系统",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])]),n("div",el,[n("div",tl,[e[68]||(e[68]=n("span",{
              class:"group-title"
            }
            ,"主题强调色",-1)),t(V,{
              content:"按钮、选中菜单、链接等强调色；可从预设色块选，或输入 #RRGGBB 自定义。",placement:"top"
            }
            ,{
              default:a(()=>[...e[67]||(e[67]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(va,{
              modelValue:l.primaryColor,"onUpdate:modelValue":e[17]||(e[17]=s=>l.primaryColor=s),presets:r(Dt),"leading-gutter":"","input-placeholder":"#RRGGBB","aria-label":"主题色",class:"preset-field-grow"
            }
            ,null,8,["modelValue","presets"])]),n("div",nl,[n("div",al,[e[70]||(e[70]=n("span",{
              class:"group-title"
            }
            ,"灰度显示",-1)),t(V,{
              content:"整站变为黑白灰度，多用于哀悼、纪念日等场景；关闭后恢复彩色。",placement:"top"
            }
            ,{
              default:a(()=>[...e[69]||(e[69]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.greyMode,"onUpdate:modelValue":e[18]||(e[18]=s=>l.greyMode=s)
            }
            ,null,8,["modelValue"])]),n("div",ll,[n("div",ol,[e[72]||(e[72]=n("span",{
              class:"group-title"
            }
            ,"色弱友好增强对比",-1)),t(V,{
              content:"加强红绿色对比与边界，辅助红绿色弱用户区分状态色（如成功/警告）。",placement:"top"
            }
            ,{
              default:a(()=>[...e[71]||(e[71]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.colorWeakMode,"onUpdate:modelValue":e[19]||(e[19]=s=>l.colorWeakMode=s)
            }
            ,null,8,["modelValue"])]),n("div",sl,[n("div",il,[n("div",rl,[e[74]||(e[74]=n("span",{
              class:"group-title"
            }
            ,"页面水印",-1)),t(V,{
              content:"在页面上平铺半透明文字，常用于标注测试环境、提醒勿外传等；可关闭水印或修改下方文字。",placement:"top"
            }
            ,{
              default:a(()=>[...e[73]||(e[73]=[n("span",{
                class:"setting-info-mark",tabindex:"0",role:"note"
              }
              ,"i",-1)])]),_:1
            }
            )]),t(H,{
              modelValue:l.showWatermark,"onUpdate:modelValue":e[20]||(e[20]=s=>l.showWatermark=s)
            }
            ,null,8,["modelValue"])]),l.showWatermark?(d(),b(ue,{
              key:0,modelValue:l.watermarkText,"onUpdate:modelValue":e[21]||(e[21]=s=>l.watermarkText=s),placeholder:"水印文字",style:{
                "margin-top":"8px",width:"100%"
              }
              
            }
            ,null,8,["modelValue"])):_("",!0)])])]),_:1
          }
          ),t(Q,{
            label:"通用",name:"general"
          }
          ,{
            default:a(()=>[n("div",dl,[n("dl",ul,[n("div",ml,[e[75]||(e[75]=n("dt",null,"前端版本",-1)),n("dd",null,F(r(w)),1)]),n("div",pl,[e[76]||(e[76]=n("dt",null,"前端发布时间",-1)),n("dd",null,F(y.value),1)]),n("div",vl,[e[77]||(e[77]=n("dt",null,"前端框架",-1)),n("dd",null,"Vue "+F(r(A)),1)]),n("div",cl,[e[78]||(e[78]=n("dt",null,"UI 组件库",-1)),n("dd",null,"Element Plus "+F(r(p)),1)]),n("div",fl,[e[79]||(e[79]=n("dt",null,"构建工具",-1)),n("dd",null,"Vite "+F(r(C)),1)]),n("div",gl,[e[80]||(e[80]=n("dt",null,"浏览器",-1)),n("dd",null,F(O.value)+" "+F(q.value),1)]),n("div",bl,[e[81]||(e[81]=n("dt",null,"屏幕分辨率",-1)),n("dd",null,F(G.value),1)]),n("div",wl,[e[82]||(e[82]=n("dt",null,"视口分辨率",-1)),n("dd",null,F(Y.value),1)])])])]),_:1
          }
          )]),_:1
        }
        ,8,["modelValue","stretch"])]),_:1
      }
      ,8,["modelValue","size","close-on-click-modal"])
    }
    
  }
  
}
),hl=Ee(_l,[["__scopeId","data-v-cffd51b2"]])

export default hl;
