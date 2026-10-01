// PlayerClaimInvite —— 行为保真移植自编译产物 PlayerClaimInvite-BJgZBu1J（recon-gen-b2.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
// 批 4b-2（#154）：门户邀请「建档入队」交互——
//   · 邀请已绑定档案（playerId 非空）：维持现状，直接认领（claimViaInvite(token, undefined) → body {}）；
//   · 邀请未绑定档案（playerId 空）：展示轻量建档表单（姓名必填 / 出生日期选填 / 守位选填），
//     提交 claimViaInvite(token, { draft })，后端按 mode 分支（claim=待审核 / registered=建档并入队）。
import { createElementBlock as i, defineComponent as V, createTextVNode as c, computed as o, toDisplayString as a, createElementVNode as d, unref as t, createBlock as C, ref as y, reactive as re, createVNode as m, withDirectives as w, openBlock as r, withCtx as s, Fragment as I, resolveComponent as T, onMounted as L, KeepAlive as W, createCommentVNode as n } from 'vue';
import { ElMessage as A, ElEmpty as M, ElButton as D, ElForm as Ef, ElFormItem as Efi, ElInput as Ei, ElDatePicker as Edp, ElSelect as Es, ElOption as Eo } from 'element-plus';
import { useI18n as q } from 'vue-i18n';
import { useRoute as z } from 'vue-router';
import { resolveTenantCodeFromRoute as H } from '../../utils/tenantRoute';
import { useAuthStore as $ } from '../../stores/auth';
import { formatDateTimeDotWithWeek as j } from '../../utils/dateExtras';
import { exportSfc as F } from '../../utils/exportSfc';
import { accountApi as N } from '../../api/account';
import { POSITION_LABELS as PL, formatPosition as fp } from '../../utils/playerOptions';
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
    )),u=y(!0),p=y(!1),e=y(null),Dk=y(!1);
    // 建档草稿（无档案者提交）：name 必填，birthDate/positions 选填（与后端 Player 口径一致）。
    const dr=re({
      name:"",birthDate:"",positions:[]
    }
    ),Zi=y(),Rl={
      name:[{
        required:!0,whitespace:!0,message:()=>l("playerClaimInvite.nameRequired"),trigger:"blur"
      }
      ]
    }
    // 守位选项 label 取自 formatPosition（与 admin 球员表单同源口径）。
    ,Po=Object.keys(PL).map(k=>({
      value:k,label:fp(k)
    }
    ));
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
      // 邀请未绑定档案 → 需建档（先过表单必填校验）；已绑定档案 → 维持现状直接认领。
      const Yd=!e.value?.playerId;
      // M5：需建档但表单 ref 尚未挂载时直接返回，避免绕过校验。
      if(Yd&&!Zi.value){
        return
      }
      if(Yd&&Zi.value&&!await Zi.value.validate().catch(()=>!1)){
        return
      }
      // C1：空白姓名防线——校验规则(whitespace) + trim 守卫，绝不构造 {name:""} 提交。
      const nm=(dr.name||"").trim();
      if(Yd&&!nm){
        A.error(l("playerClaimInvite.nameRequired"));
        return
      }
      let Od;
      if(Yd){
        const d={
          name:nm
        }
        ;
        dr.birthDate&&(d.birthDate=dr.birthDate);
        Array.isArray(dr.positions)&&dr.positions.length&&(d.positions=[...dr.positions]);
        Od={
          draft:d
        }
        
      }
      p.value=!0;
      try{
        const res=await N.claimViaInvite(f.value,Yd?Od:void 0),mode=res?.data?.mode;
        // 成功分支按后端返回 mode 区分：claim=认领落库待审核；registered=建档并入队 / 存量直接入队。
        mode==="claim"?A.success(l("playerClaimInvite.submitted")):A.success(Yd?l("playerClaimInvite.registered"):l("playerClaimInvite.joined"));
        // M4：成功后置 done，禁用提交按钮防止二次点击 400。
        Dk.value=!0
      }
      catch(err){
        A.error(err?.message||l("playerClaimInvite.submitFailed"))
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
      ,[d("h1",null,a(t(l)("playerClaimInvite.title")),1),e.value.teamName?(r(),i("p",U,a(t(l)("playerClaimInvite.team"))+"："+a(e.value.teamName),1)):n("",!0),e.value.playerName?(r(),i("p",J,[c(a(t(l)("playerClaimInvite.player"))+"："+a(e.value.playerName)+" ",1),e.value.playerNumber?(r(),i("span",K,"#"+a(e.value.playerNumber),1)):n("",!0)])):n("",!0),e.value.remark?(r(),i("p",O,a(e.value.remark),1)):n("",!0),e.value.expiresAt?(r(),i("p",Q,a(t(l)("playerClaimInvite.expires"))+"："+a(t(j)(e.value.expiresAt)),1)):n("",!0),t(x).user?(r(),i(I,{
        key:4
      }
      ,[e.value.playerId?(r(),C(_,{
        key:0,type:"primary",size:"large",loading:p.value,onClick:P
      }
      ,{
        default:s(()=>[c(a(t(l)("playerClaimInvite.claimBtn")),1)]),_:1
      }
      ,8,["loading"])):(r(),i(I,{
        key:1
      }
      ,[d("p",X,a(t(l)("playerClaimInvite.draftHint")),1),m(Ef,{
        ref_key:"formRef",ref:Zi,model:dr,rules:Rl,"label-position":"top",class:"draft-form"
      }
      ,{
        default:s(()=>[m(Efi,{
          label:t(l)("playerClaimInvite.name"),prop:"name"
        }
        ,{
          default:s(()=>[m(Ei,{
            modelValue:dr.name,"onUpdate:modelValue":te[0]||(te[0]=k=>dr.name=k),placeholder:t(l)("playerClaimInvite.namePlaceholder"),size:"large",maxlength:"50"
          }
          ,null,8,["modelValue","placeholder"])]),_:1
        }
        ,8,["label"]),m(Efi,{
          label:t(l)("playerClaimInvite.birthDate"),prop:"birthDate"
        }
        ,{
          default:s(()=>[m(Edp,{
            modelValue:dr.birthDate,"onUpdate:modelValue":te[1]||(te[1]=k=>dr.birthDate=k),type:"date","value-format":"YYYY-MM-DD",placeholder:t(l)("playerClaimInvite.birthDatePlaceholder"),size:"large"
          }
          ,null,8,["modelValue","placeholder"])]),_:1
        }
        ,8,["label"]),m(Efi,{
          label:t(l)("playerClaimInvite.positions"),prop:"positions"
        }
        ,{
          default:s(()=>[m(Es,{
            modelValue:dr.positions,"onUpdate:modelValue":te[2]||(te[2]=k=>dr.positions=k),multiple:!0,collapseTags:!0,clearable:!0,placeholder:t(l)("playerClaimInvite.positionsPlaceholder"),size:"large"
          }
          ,{
            default:s(()=>Po.map(k=>m(Eo,{
              key:k.value,label:k.label,value:k.value
            }
            ))),_:1
          }
          ,8,["modelValue","placeholder"])]),_:1
        }
        ,8,["label"]),m(_,{
          type:"primary",size:"large",loading:p.value,disabled:Dk.value,onClick:P,class:"submit-btn"
        }
        ,{
          default:s(()=>[c(a(t(l)("playerClaimInvite.createBtn")),1)]),_:1
        }
        ,8,["loading","disabled"])]),_:1
      }
      ,8,["model","rules"])],64))],64)):(r(),i(I,{
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
