// PlayerClaimInvite —— 行为保真移植自编译产物 PlayerClaimInvite-BJgZBu1J（recon-gen-b2.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as i, defineComponent as V, createTextVNode as c, computed as o, toDisplayString as a, createElementVNode as d, unref as t, createBlock as C, ref as y, createVNode as m, withDirectives as w, openBlock as r, withCtx as s, Fragment as I, resolveComponent as T, onMounted as L, KeepAlive as W, createCommentVNode as n } from 'vue';
import { ElMessage as A, ElEmpty as M, ElButton as D } from 'element-plus';
import { useI18n as q } from 'vue-i18n';
import { useRoute as z } from 'vue-router';
import { resolveTenantCodeFromRoute as H } from '../../utils/tenantRoute';
import { useAuthStore as $ } from '../../stores/auth';
import { formatDateDot as j } from '../../utils/formatDate';
import { exportSfc as F } from '../../utils/exportSfc';
import { accountApi as N } from '../../api/account';
import '../../styles/legacy/auth-player-claim-invite.css';
var G={
  class:"player-claim-invite"
}
,U={
  key:0,class:"meta"
}
,J={
  key:1,class:"meta"
}
,K={
  key:0
}
,O={
  key:2,class:"remark"
}
,Q={
  key:3,class:"meta"
}
,X={
  class:"hint"
}
,Y={
  class:"actions"
}
,Z=V({
  __name:"PlayerClaimInvite",setup(ee){
    const{
      t:l
    }
    =q(),v=z(),x=$(),f=o(()=>String(v.params.token||"")),h=o(()=>H(v)),g=o(()=>v.fullPath),B=o(()=>({
      path:`/${h.value}/account/login`,query:{
        redirect:g.value
      }
      
    }
    )),b=o(()=>({
      path:`/${h.value}/account/register`,query:{
        redirect:g.value
      }
      
    }
    )),u=y(!0),p=y(!1),e=y(null);
    async function E(){
      u.value=!0;
      try{
        e.value=(await N.getClaimInvite(f.value)).data??null
      }
      catch{
        e.value=null
      }
      finally{
        u.value=!1
      }
      
    }
    async function P(){
      p.value=!0;
      try{
        await N.claimViaInvite(f.value,e.value?.playerId?void 0:{
          
        }
        ),A.success(l("playerClaimInvite.submitted"))
      }
      finally{
        p.value=!1
      }
      
    }
    return L(()=>{
      E()
    }
    ),(ae,te)=>{
      const _=D,k=T("router-link"),R=M,S=W;
      return w((r(),i("div",G,[e.value?(r(),i(I,{
        key:0
      }
      ,[d("h1",null,a(t(l)("playerClaimInvite.title")),1),e.value.teamName?(r(),i("p",U,a(t(l)("playerClaimInvite.team"))+"："+a(e.value.teamName),1)):n("",!0),e.value.playerName?(r(),i("p",J,[c(a(t(l)("playerClaimInvite.player"))+"："+a(e.value.playerName)+" ",1),e.value.playerNumber?(r(),i("span",K,"#"+a(e.value.playerNumber),1)):n("",!0)])):n("",!0),e.value.remark?(r(),i("p",O,a(e.value.remark),1)):n("",!0),e.value.expiresAt?(r(),i("p",Q,a(t(l)("playerClaimInvite.expires"))+"："+a(t(j)(e.value.expiresAt)),1)):n("",!0),t(x).user?(r(),C(_,{
        key:4,type:"primary",size:"large",loading:p.value,onClick:P
      }
      ,{
        default:s(()=>[c(a(t(l)("playerClaimInvite.claimBtn")),1)]),_:1
      }
      ,8,["loading"])):(r(),i(I,{
        key:5
      }
      ,[d("p",X,a(t(l)("playerClaimInvite.loginHint")),1),d("div",Y,[m(k,{
        to:B.value
      }
      ,{
        default:s(()=>[m(_,{
          type:"primary"
        }
        ,{
          default:s(()=>[c(a(t(l)("accountLogin.title")),1)]),_:1
        }
        )]),_:1
      }
      ,8,["to"]),m(k,{
        to:b.value
      }
      ,{
        default:s(()=>[m(_,null,{
          default:s(()=>[c(a(t(l)("accountRegister.title")),1)]),_:1
        }
        )]),_:1
      }
      ,8,["to"])])],64))],64)):u.value?n("",!0):(r(),C(R,{
        key:1,description:t(l)("playerClaimInvite.invalid")
      }
      ,null,8,["description"]))])),[[S,u.value]])
    }
    
  }
  
}
),ve=F(Z,[["__scopeId","data-v-50f7ac7b"]]);

export default ve;
