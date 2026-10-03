// GameDetailFieldSettingsDrawer —— 「数据项调整」抽屉（#211：自 GameDetail.js 内联组件原样抽取，供观赛页复用）
// 来源：GameDetail-C4q9PQKP 编译产物移植件中的内联组件 GameDetailFieldSettingsDrawer（样式域 data-v-724ded90）
// 纯展示组件：勾选/拖拽字段、数值控制；保存经 save 事件交宿主页面落库，本组件不读写任何存储
import { ElRadioGroup as fe, ElDrawer as be, ElRadioButton as ge, ElIcon as he, ElTableColumn as ye, ElMessage as X, ElCheckbox as we, ElTable as De, ElTabs as Ie, ElButton as de, ElTabPane as Ee } from 'element-plus';
import { withModifiers as j, defineComponent as oe, createTextVNode as S, computed as G, toDisplayString as r, createElementVNode as n, unref as l, createBlock as q, ref as m, createVNode as a, openBlock as H, withCtx as i, toRef as Pe, watch as ue, onMounted as te, onBeforeUnmount as Ae } from 'vue';
import { Rank as le } from '@element-plus/icons-vue';
import { useI18n as ce } from 'vue-i18n';
import { exportSfc as me } from '../../utils/exportSfc';
import Re from 'sortablejs';
import { useHtmlScrollLock as He } from '../../composables/useHtmlScrollLock';
import '../../styles/legacy/game-detail.css';
var Ne={
  class:"drawer-inner"
}
,Oe={
  class:"drawer-body"
}
,$e={
  class:"decimal-settings"
}
,We={
  class:"section-title"
}
,je={
  class:"decimal-option"
}
,qe={
  class:"decimal-label-inline"
}
,Ke={
  class:"decimal-option"
}
,Ye={
  class:"decimal-label-inline"
}
,Qe={
  class:"decimal-option"
}
,Je={
  class:"decimal-label-inline"
}
,Xe={
  class:"section-title"
}
,et={
  class:"hint hint-min-visible"
}
,tt={
  class:"tab-label"
}
,lt={
  class:"hint"
}
,at=["title"],it={
  class:"col-desc-cell"
}
,st={
  class:"tab-label"
}
,nt={
  class:"hint"
}
,rt=["title"],ot={
  class:"col-desc-cell"
}
,dt={
  class:"drawer-footer"
}
,ut=oe({
  __name:"GameDetailFieldSettingsDrawer",props:{
    modelValue:{
      type:Boolean
    }
    ,batterItems:{
      
    }
    ,pitcherItems:{
      
    }
    ,defaultBatterItems:{
      
    }
    ,defaultPitcherItems:{
      
    }
    ,decimalPlaces:{
      
    }
    ,defaultDecimalPlaces:{
      
    }
    ,rateDisplayStyle:{
      
    }
    ,defaultRateDisplayStyle:{
      
    }
    ,showTrailingZeros:{
      type:Boolean
    }
    ,defaultShowTrailingZeros:{
      type:Boolean
    }
    ,isSmallScreen:{
      type:Boolean
    }
    
  }
  ,emits:["update:modelValue","save"],setup(K,{
    emit:N
  }
  ){
    const{
      t,te:O
    }
    =ce();
    function $(s,e){
      const d=`fieldSettings.gameDetailColDesc.${s}.${e}`;
      return O(d)?t(d):""
    }
    const o=K;
    He(Pe(o,"modelValue"));
    const w=N,R=m("batter"),W=G(()=>o.isSmallScreen?"100%":"420px"),F=6,C=6;
    function z(s){
      return s.filter(e=>e.visible).length
    }
    const T=m(null),L=m(null);
    let P=null,D=null;
    const p=m([]),f=m([]),E=m(o.decimalPlaces),V=m(o.rateDisplayStyle),I=m(o.showTrailingZeros);
    function A(s){
      return s.map(e=>({
        ...e
      }
      ))
    }
    function Y(){
      p.value=A(o.batterItems||[]),f.value=A(o.pitcherItems||[]),E.value=o.decimalPlaces,V.value=o.rateDisplayStyle,I.value=o.showTrailingZeros
    }
    function x(s,e,d){
      const g=s==="batter"?p.value:f.value,y=s==="batter"?F:C;
      if(!d&&z(g)<=y){
        X.warning(t("fieldSettings.minVisibleUncheckReject",{
          n:y
        }
        ));
        return
      }
      const c=g.map(h=>h.key===e?{
        ...h,visible:d
      }
      :h);
      s==="batter"?p.value=c:f.value=c
    }
    function M(s){
      return s.length>0&&s.every(e=>e.visible)
    }
    function u(s){
      const e=s==="batter"?p.value:f.value;
      if(!e.length)return;
      const d=!M(e),g=e.map(y=>({
        ...y,visible:d
      }
      ));
      s==="batter"?p.value=g:f.value=g
    }
    function b(){
      const s=(e,d)=>{
        if(!e)return null;
        const g=e.querySelector("tbody");
        return g?Re.create(g,{
          animation:150,handle:".drag-handle",onEnd:y=>{
            const c=y.oldIndex??-1,h=y.newIndex??-1;
            if(c<0||h<0||c===h)return;
            const _=[...d==="batter"?p.value:f.value],B=_.splice(c,1)[0];
            _.splice(h,0,B),d==="batter"?p.value=_:f.value=_
          }
          
        }
        ):null
      };
      P?.destroy(),D?.destroy(),P=s(T.value,"batter"),D=s(L.value,"pitcher")
    }
    te(()=>b()),Ae(()=>{
      P?.destroy(),D?.destroy()
    }
    ),ue(()=>o.modelValue,s=>{
      s&&(Y(),setTimeout(()=>b(),0))
    }
    );
    function k(){
      p.value=A(o.defaultBatterItems||[]),f.value=A(o.defaultPitcherItems||[]),E.value=o.defaultDecimalPlaces,V.value=o.defaultRateDisplayStyle,I.value=o.defaultShowTrailingZeros,setTimeout(()=>b(),0)
    }
    function U(){
      w("update:modelValue",!1)
    }
    function Z(){
      const s=z(p.value),e=z(f.value);
      if(s<F){
        X.warning(t("fieldSettings.minVisibleSaveBatter",{
          n:F
        }
        ));
        return
      }
      if(e<C){
        X.warning(t("fieldSettings.minVisibleSavePitcher",{
          n:C
        }
        ));
        return
      }
      w("save",{
        batterItems:A(p.value),pitcherItems:A(f.value),decimalPlaces:E.value,rateDisplayStyle:V.value,showTrailingZeros:I.value
      }
      ),w("update:modelValue",!1)
    }
    return(s,e)=>{
      const d=ge,g=fe,y=he,c=ye,h=we,_=De,B=Ee,ve=Ie,Q=de,pe=be;
      return H(),q(pe,{
        "model-value":K.modelValue,title:l(t)("fieldSettings.title"),size:W.value,direction:"rtl","with-header":!0,"lock-scroll":"",onClose:U,class:"field-settings-drawer portal-game-field-settings-drawer"
      }
      ,{
        default:i(()=>[n("div",Ne,[n("div",Oe,[n("div",$e,[n("div",We,r(l(t)("fieldSettings.sectionNumberControl")),1),n("div",je,[n("span",qe,r(l(t)("fieldSettings.rateDisplayStyle")),1),a(g,{
          modelValue:V.value,"onUpdate:modelValue":e[0]||(e[0]=v=>V.value=v),size:"small"
        }
        ,{
          default:i(()=>[a(d,{
            value:"dot"
          }
          ,{
            default:i(()=>[S(r(l(t)("fieldSettings.rateStyleDot")),1)]),_:1
          }
          ),a(d,{
            value:"leadingZero"
          }
          ,{
            default:i(()=>[S(r(l(t)("fieldSettings.rateStyleLeadingZero")),1)]),_:1
          }
          )]),_:1
        }
        ,8,["modelValue"])]),n("div",Ke,[n("span",Ye,r(l(t)("fieldSettings.trailingZeros")),1),a(g,{
          modelValue:I.value,"onUpdate:modelValue":e[1]||(e[1]=v=>I.value=v),size:"small"
        }
        ,{
          default:i(()=>[a(d,{
            value:!0
          }
          ,{
            default:i(()=>[S(r(l(t)("fieldSettings.keep")),1)]),_:1
          }
          ),a(d,{
            value:!1
          }
          ,{
            default:i(()=>[S(r(l(t)("fieldSettings.noKeep")),1)]),_:1
          }
          )]),_:1
        }
        ,8,["modelValue"])]),n("div",Qe,[n("span",Je,r(l(t)("fieldSettings.decimalPlaces")),1),a(g,{
          modelValue:E.value,"onUpdate:modelValue":e[2]||(e[2]=v=>E.value=v),size:"small"
        }
        ,{
          default:i(()=>[a(d,{
            value:2
          }
          ,{
            default:i(()=>[S(r(l(t)("fieldSettings.twoPlaces")),1)]),_:1
          }
          ),a(d,{
            value:3
          }
          ,{
            default:i(()=>[S(r(l(t)("fieldSettings.threePlaces")),1)]),_:1
          }
          )]),_:1
        }
        ,8,["modelValue"])])]),n("div",Xe,r(l(t)("fieldSettings.sectionFieldControl")),1),n("div",et,r(l(t)("fieldSettings.hintMinVisible",{
          batterMin:l(F),pitcherMin:l(C)
        }
        )),1),a(ve,{
          modelValue:R.value,"onUpdate:modelValue":e[7]||(e[7]=v=>R.value=v),class:"settings-tabs"
        }
        ,{
          default:i(()=>[a(B,{
            name:"batter"
          }
          ,{
            label:i(()=>[n("span",tt,r(l(t)("fieldSettings.batter")),1)]),default:i(()=>[n("div",lt,r(l(t)("fieldSettings.hintDrag")),1),n("div",{
              ref_key:"batterTableWrap",ref:T,class:"settings-table"
            }
            ,[a(_,{
              data:p.value,size:"small","row-key":"key"
            }
            ,{
              default:i(()=>[a(c,{
                width:"45",align:"center"
              }
              ,{
                default:i(()=>[a(y,{
                  class:"drag-handle"
                }
                ,{
                  default:i(()=>[a(l(le))]),_:1
                }
                )]),_:1
              }
              ),a(c,{
                width:"62",align:"center"
              }
              ,{
                header:i(()=>[n("button",{
                  type:"button",class:"show-col-header",title:l(t)("fieldSettings.showColumnToggleHint"),onMousedown:e[3]||(e[3]=j(()=>{
                    
                  }
                  ,["stop","prevent"])),onClick:e[4]||(e[4]=j(v=>u("batter"),["stop","prevent"]))
                }
                ,r(l(t)("fieldSettings.show")),41,at)]),default:i(({
                  row:v
                }
                )=>[a(h,{
                  "model-value":v.visible,"onUpdate:modelValue":J=>x("batter",v.key,!!J)
                }
                ,null,8,["model-value","onUpdate:modelValue"])]),_:1
              }
              ),a(c,{
                prop:"label",label:l(t)("fieldSettings.field"),width:"80"
              }
              ,null,8,["label"]),a(c,{
                label:l(t)("fieldSettings.desc"),"min-width":"140"
              }
              ,{
                default:i(({
                  row:v
                }
                )=>[n("span",it,r($("batter",v.key)),1)]),_:1
              }
              ,8,["label"])]),_:1
            }
            ,8,["data"])],512)]),_:1
          }
          ),a(B,{
            name:"pitcher"
          }
          ,{
            label:i(()=>[n("span",st,r(l(t)("fieldSettings.pitcher")),1)]),default:i(()=>[n("div",nt,r(l(t)("fieldSettings.hintDrag")),1),n("div",{
              ref_key:"pitcherTableWrap",ref:L,class:"settings-table"
            }
            ,[a(_,{
              data:f.value,size:"small","row-key":"key"
            }
            ,{
              default:i(()=>[a(c,{
                width:"45",align:"center"
              }
              ,{
                default:i(()=>[a(y,{
                  class:"drag-handle"
                }
                ,{
                  default:i(()=>[a(l(le))]),_:1
                }
                )]),_:1
              }
              ),a(c,{
                width:"62",align:"center"
              }
              ,{
                header:i(()=>[n("button",{
                  type:"button",class:"show-col-header",title:l(t)("fieldSettings.showColumnToggleHint"),onMousedown:e[5]||(e[5]=j(()=>{
                    
                  }
                  ,["stop","prevent"])),onClick:e[6]||(e[6]=j(v=>u("pitcher"),["stop","prevent"]))
                }
                ,r(l(t)("fieldSettings.show")),41,rt)]),default:i(({
                  row:v
                }
                )=>[a(h,{
                  "model-value":v.visible,"onUpdate:modelValue":J=>x("pitcher",v.key,!!J)
                }
                ,null,8,["model-value","onUpdate:modelValue"])]),_:1
              }
              ),a(c,{
                prop:"label",label:l(t)("fieldSettings.field"),width:"80"
              }
              ,null,8,["label"]),a(c,{
                label:l(t)("fieldSettings.desc"),"min-width":"140"
              }
              ,{
                default:i(({
                  row:v
                }
                )=>[n("span",ot,r($("pitcher",v.key)),1)]),_:1
              }
              ,8,["label"])]),_:1
            }
            ,8,["data"])],512)]),_:1
          }
          )]),_:1
        }
        ,8,["modelValue"])]),n("div",dt,[a(Q,{
          onClick:k
        }
        ,{
          default:i(()=>[S(r(l(t)("fieldSettings.resetDefault")),1)]),_:1
        }
        ),a(Q,{
          onClick:U
        }
        ,{
          default:i(()=>[S(r(l(t)("common.cancel")),1)]),_:1
        }
        ),a(Q,{
          type:"primary",onClick:Z
        }
        ,{
          default:i(()=>[S(r(l(t)("fieldSettings.save")),1)]),_:1
        }
        )])])]),_:1
      }
      ,8,["model-value","title","size"])
    }
    
  }
  
}
)
export default me(ut, [["__scopeId", "data-v-724ded90"]]);
