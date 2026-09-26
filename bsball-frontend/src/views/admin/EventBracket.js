// EventBracket —— 行为保真移植自编译产物 EventBracket-DvdCIoBY（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { ElRadioGroup as L, ElRadioButton as O, ElEmpty as P, ElCard as $, ElAlert as F } from 'element-plus';
import { createElementBlock as y, defineComponent as D, createTextVNode as c, computed as v, toDisplayString as w, createElementVNode as _, createBlock as d, ref as u, createVNode as m, withDirectives as T, openBlock as l, withCtx as s, watch as U, onMounted as j, KeepAlive as z, createCommentVNode as S } from 'vue';
import { useRoute as H } from 'vue-router';
import { fetchAllPages as K } from '../../api/request';
import { exportSfc as Q } from '../../utils/exportSfc';
import { eventApi as W } from '../../api/business';
import { teamApi as X } from '../../api/business';
import { EventScheduleSubTable as q } from '../../components/admin/EventScheduleListTable.js';
import { excludeScheduleRows as J } from '../../components/admin/EventScheduleListTable.js';
import { default as Y } from '../../components/admin/EventScheduleListTable.js';
import '../../styles/legacy/event-bracket.css';
var Z={
  class:"admin-page"
}
,ee={
  class:"card-header-row"
}
,ae={
  class:"header-left"
}
,te={
  class:"bracket-body"
}
,ne="bsball.admin.eventBracket.viewMode.",le=D({
  __name:"EventBracket",setup(se){
    const I=H(),o=v(()=>Number(I.params.eventId)),p=u("当前赛事"),t=u([]),f=u([]),b=v(()=>Object.fromEntries(f.value.map(e=>[e.id,e.name]))),i=u(!1),n=u("list");
    function g(){
      return ne+String(o.value)
    }
    function B(){
      try{
        const e=sessionStorage.getItem(g());
        (e==="calendar"||e==="list")&&(n.value=e)
      }
      catch{
        
      }
      
    }
    function V(e){
      try{
        sessionStorage.setItem(g(),e)
      }
      catch{
        
      }
      
    }
    U(n,e=>V(e));
    const h=v(()=>J(t.value));
    function k(e){
      return b.value[e]??"#"+e
    }
    function E(e){
      return{
        name:"AdminGameDetail",params:{
          eventId:String(o.value),gameId:String(e.id)
        }
        
      }
      
    }
    async function G(){
      i.value=!0;
      const[e,a,r]=await Promise.all([W.get(o.value),X.selectOptions(),K("/api/game/list",{
        eventId:o.value
      }
      )]);
      p.value=e?.name??"当前赛事",f.value=a??[],t.value=r.error?[]:r.list,i.value=!1
    }
    return j(()=>{
      B(),G()
    }
    ),(e,a)=>{
      const r=O,M=L,N=F,R=P,x=$,A=z;
      return l(),y("div",Z,[m(x,{
        class:"bracket-card"
      }
      ,{
        header:s(()=>[_("div",ee,[_("div",ae,[_("span",null,"对战详情 · "+w(p.value),1)]),m(M,{
          modelValue:n.value,"onUpdate:modelValue":a[0]||(a[0]=C=>n.value=C),class:"view-toggle"
        }
        ,{
          default:s(()=>[m(r,{
            value:"calendar"
          }
          ,{
            default:s(()=>[...a[1]||(a[1]=[c("月历显示",-1)])]),_:1
          }
          ),m(r,{
            value:"list"
          }
          ,{
            default:s(()=>[...a[2]||(a[2]=[c("列表显示",-1)])]),_:1
          }
          )]),_:1
        }
        ,8,["modelValue"])])]),default:s(()=>[h.value.length>0?(l(),d(N,{
          key:0,type:"info",closable:!1,"show-icon":"",class:"undated-alert"
        }
        ,{
          default:s(()=>[c(" 有 "+w(h.value.length)+" 场比赛未填写比赛日或开始时间，月历中不会显示，仍可在下方列表「未指定日期」分组中查看。 ",1)]),_:1
        }
        )):S("",!0),T((l(),y("div",te,[!i.value&&t.value.length===0?(l(),d(R,{
          key:0,description:"暂无比赛数据"
        }
        )):n.value==="calendar"&&t.value.length>0?(l(),d(q,{
          key:o.value,games:t.value,"team-name":k,"game-link":E
        }
        ,null,8,["games"])):n.value==="list"&&t.value.length>0?(l(),d(Y,{
          key:2,games:t.value,"team-name":k,"game-link":E
        }
        ,null,8,["games"])):S("",!0)])),[[A,i.value]])]),_:1
      }
      )])
    }
    
  }
  
}
),pe=Q(le,[["__scopeId","data-v-8f9a3012"]]);

export default pe;
