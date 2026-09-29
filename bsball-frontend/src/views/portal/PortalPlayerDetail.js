// PortalPlayerDetail —— 行为保真移植自编译产物 PlayerDetail-KzwyC-QG（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as I, defineComponent as L, createTextVNode as C, computed as N, toDisplayString as _, createElementVNode as V, unref as e, createBlock as f, ref as o, createVNode as b, openBlock as n, withCtx as v, watch as G, createCommentVNode as E } from 'vue';
import { ElSkeleton as M, ElMessage as w, ElEmpty as $, ElButton as z } from 'element-plus';
import { useI18n as U } from 'vue-i18n';
import { useRouter as q, useRoute as F } from 'vue-router';
import { useAuthStore as H } from '../../stores/auth';
import { fetchResult as y } from '../../api/request';
import { exportSfc as j } from '../../utils/exportSfc';
import { accountApi as J } from '../../api/account';
import { useTenantRouter as K } from '../../composables/useTenantRouter';
import { DEFAULT_AVATAR_SVG_PATH as O } from '../../utils/placeholderAssets';
import Q from '../../components/admin/PlayerDetailContent.js';
import { useDetailLoad as W } from '../../composables/useDetailLoad';
import '../../styles/legacy/portal-player-detail.css';
var X={
  class:"portal-player-detail"
}
,Y={
  key:0,class:"player-claim-banner"
}
,Z={
  class:"player-claim-banner__text"
}
,ee=L({
  __name:"PlayerDetail",setup(ae){
    const{
      t:r
    }
    =U(),i=F(),A=q(),g=H(),{
      portalPath:P
    }
    =K(),a=o(null),u=o(null),D=N(()=>a.value?P(`teams/${a.value.id}`):""),c=o([]),p=o([]),S=O,{
      loading:x,error:h,result:t,retry:k
    }
    =W(async()=>{
      const s=Number(i.params.id),m=await y(`/api/player/${s}`);
      if(m.error)throw new Error(m.error);
      const l=m.data??null;
      return l?.teamId?(a.value=(await y(`/api/team/${l.teamId}`)).data??null,a.value?.leagueId?u.value=(await y(`/api/league/${a.value.leagueId}`)).data??null:u.value=null):(a.value=null,u.value=null),s?(c.value=[],p.value=[]):(c.value=[],p.value=[]),l
    }
    ),d=o(!1);
    async function B(){
      if(t.value){
        if(!g.user){
          A.push({
            name:"PortalAccountLogin",params:{
              tenantCode:i.params.tenantCode
            }
            ,query:{
              redirect:i.fullPath
            }
            
          }
          );
          return
        }
        d.value=!0;
        try{
          await J.submitPlayerClaim({
            playerId:t.value.id
          }
          ),w.success(r("player.claim.submitted"))
        }
        catch(s){
          w.error(s?.message||r("player.claim.failed"))
        }
        finally{
          d.value=!1
        }
        
      }
      
    }
    return G(()=>i.params.id,()=>k(),{
      immediate:!1
    }
    ),(s,m)=>{
      const l=z,R=M,T=$;
      return n(),I("div",X,[e(g).user&&e(t)&&!e(t).userId?(n(),I("div",Y,[V("span",Z,_(e(r)("player.claim.prompt")),1),b(l,{
        type:"primary",size:"small",loading:d.value,onClick:B
      }
      ,{
        default:v(()=>[C(_(e(r)("player.claim.action")),1)]),_:1
      }
      ,8,["loading"])])):E("",!0),e(t)?(n(),f(Q,{
        key:1,player:e(t),team:a.value,league:u.value,highlights:c.value,histories:p.value,"default-avatar-img":e(S),"team-link-to":D.value
      }
      ,null,8,["player","team","league","highlights","histories","default-avatar-img","team-link-to"])):e(x)?(n(),f(R,{
        key:2,rows:8,animated:""
      }
      )):e(h)?(n(),f(T,{
        key:3,description:e(h)
      }
      ,{
        default:v(()=>[b(l,{
          type:"primary",onClick:e(k)
        }
        ,{
          default:v(()=>[C(_(e(r)("common.retry")),1)]),_:1
        }
        ,8,["onClick"])]),_:1
      }
      ,8,["description"])):E("",!0)])
    }
    
  }
  
}
),_e=j(ee,[["__scopeId","data-v-4b84f8d2"]]);

export default _e;
