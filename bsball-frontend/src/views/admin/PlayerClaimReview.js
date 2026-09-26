// PlayerClaimReview —— 行为保真移植自编译产物 PlayerClaimReview-Dn1pLlfl（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as X, createElementBlock as k, defineComponent as Z, createTextVNode as p, computed as ee, toDisplayString as s, createElementVNode as x, unref as l, mergeProps as ae, createBlock as le, ref as f, createVNode as a, withDirectives as re, openBlock as b, withCtx as t, Fragment as pe, withKeys as ue, onMounted as ve, KeepAlive as ye, reactive as A } from 'vue';
import { ElDialog as Y, ElTableColumn as te, ElInput as ie, ElFormItem as oe, ElMessage as R, ElTable as ne, ElOption as se, ElButton as de, ElCard as me, ElSelect as ce, ElTag as we, ElForm as _e } from 'element-plus';
import { useI18n as ge } from 'vue-i18n';
import { useMediaQuery as fe } from '../../composables/useMediaQuery';
import { formatDateTime as Ce } from '../../utils/formatDate';
import { useSettingsStore as be } from '../../stores/settings';
import { exportSfc as Re } from '../../utils/exportSfc';
import { accountApi as j } from '../../api/account';
import he from '../../components/admin/AdminListScaffold.js';
import Te from '../../components/admin/AdminPagination.js';
import { useListTableAttrs as ke } from '../../composables/useListTable';
import { DEFAULT_SORT_CREATED_AT as je } from '../../composables/useListTable';
import '../../styles/legacy/player-claim-review.css';
var Se={
  class:"admin-page"
}
,Ve={
  key:1
}
,Ee={
  class:"pagination-wrap"
}
,Pe=Z({
  __name:"PlayerClaimReview",setup(xe){
    const{
      t:e
    }
    =ge(),z=ke({
      defaultSort:je
    }
    ),I=be(),L=fe("(max-width: 768px)"),N=ee(()=>I.fillPageHeight&&!L.value),h=f(!1),S=f([]),m=A({
      page:1,pageSize:20,total:0
    }
    ),u=A({
      keyword:"",reviewerType:"",status:""
    }
    ),_=f(!1),V=f(0),g=f(""),T=f(!1);
    function U(i){
      return i==="team_manager"?e("playerClaimReview.reviewerTeamManager"):i==="platform_admin"?e("playerClaimReview.reviewerPlatformAdmin"):i||"-"
    }
    function D(i){
      switch(i){
        case"pending":return e("playerClaimReview.statusPending");
        case"approved":return e("playerClaimReview.statusApproved");
        case"rejected":return e("playerClaimReview.statusRejected");
        case"cancelled":return e("playerClaimReview.statusCancelled");
        default:return i||"-"
      }
      
    }
    function F(i){
      switch(i){
        case"approved":return"success";
        case"rejected":return"danger";
        case"cancelled":return"info";
        default:return"warning"
      }
      
    }
    async function c(){
      h.value=!0;
      try{
        const i=await j.pendingPlayerClaims({
          page:m.page,pageSize:m.pageSize,keyword:u.keyword.trim()||void 0,reviewerType:u.reviewerType||void 0,status:u.status||void 0
        }
        );
        S.value=i.list??[],m.total=i.total??0
      }
      finally{
        h.value=!1
      }
      
    }
    function v(){
      m.page=1,c()
    }
    function M(){
      u.keyword="",u.reviewerType="",u.status="",m.page=1,c()
    }
    async function B(i){
      try{
        await j.approvePlayerClaim(i),R.success(e("playerClaimReview.approved")),c()
      }
      catch(o){
        R.error(o?.message||e("playerClaimReview.actionFailed"))
      }
      
    }
    function $(i){
      V.value=i,g.value="",_.value=!0
    }
    async function q(){
      T.value=!0;
      try{
        await j.rejectPlayerClaim(V.value,g.value.trim()||void 0),R.success(e("playerClaimReview.rejected")),_.value=!1,c()
      }
      catch(i){
        R.error(i?.message||e("playerClaimReview.actionFailed"))
      }
      finally{
        T.value=!1
      }
      
    }
    return ve(()=>{
      c()
    }
    ),(i,o)=>{
      const E=ie,C=oe,w=se,P=ce,y=de,H=_e,d=te,K=we,O=ne,Q=me,G=Y,J=ye;
      return b(),k("div",Se,[a(Q,null,{
        header:t(()=>[x("span",null,s(l(e)("playerClaimReview.title")),1)]),default:t(()=>[a(he,{
          "fill-mode":N.value
        }
        ,{
          filters:t(()=>[a(H,{
            inline:!0,class:"query-form",onSubmit:X(v,["prevent"])
          }
          ,{
            default:t(()=>[a(C,{
              label:l(e)("playerClaimReview.keyword")
            }
            ,{
              default:t(()=>[a(E,{
                modelValue:u.keyword,"onUpdate:modelValue":o[0]||(o[0]=n=>u.keyword=n),placeholder:l(e)("playerClaimReview.keywordPlaceholder"),clearable:"",style:{
                  width:"240px"
                }
                ,onClear:v,onKeyup:ue(v,["enter"])
              }
              ,null,8,["modelValue","placeholder"])]),_:1
            }
            ,8,["label"]),a(C,{
              label:l(e)("playerClaimReview.reviewerType")
            }
            ,{
              default:t(()=>[a(P,{
                modelValue:u.reviewerType,"onUpdate:modelValue":o[1]||(o[1]=n=>u.reviewerType=n),placeholder:l(e)("playerClaimReview.all"),clearable:"",style:{
                  width:"150px"
                }
                ,onChange:v
              }
              ,{
                default:t(()=>[a(w,{
                  label:l(e)("playerClaimReview.reviewerTeamManager"),value:"team_manager"
                }
                ,null,8,["label"]),a(w,{
                  label:l(e)("playerClaimReview.reviewerPlatformAdmin"),value:"platform_admin"
                }
                ,null,8,["label"])]),_:1
              }
              ,8,["modelValue","placeholder"])]),_:1
            }
            ,8,["label"]),a(C,{
              label:l(e)("playerClaimReview.status")
            }
            ,{
              default:t(()=>[a(P,{
                modelValue:u.status,"onUpdate:modelValue":o[2]||(o[2]=n=>u.status=n),placeholder:l(e)("playerClaimReview.all"),clearable:"",style:{
                  width:"130px"
                }
                ,onChange:v
              }
              ,{
                default:t(()=>[a(w,{
                  label:l(e)("playerClaimReview.statusPending"),value:"pending"
                }
                ,null,8,["label"]),a(w,{
                  label:l(e)("playerClaimReview.statusApproved"),value:"approved"
                }
                ,null,8,["label"]),a(w,{
                  label:l(e)("playerClaimReview.statusRejected"),value:"rejected"
                }
                ,null,8,["label"]),a(w,{
                  label:l(e)("playerClaimReview.statusCancelled"),value:"cancelled"
                }
                ,null,8,["label"])]),_:1
              }
              ,8,["modelValue","placeholder"])]),_:1
            }
            ,8,["label"]),a(C,null,{
              default:t(()=>[a(y,{
                type:"primary",onClick:v
              }
              ,{
                default:t(()=>[p(s(l(e)("playerClaimReview.search")),1)]),_:1
              }
              ),a(y,{
                onClick:M
              }
              ,{
                default:t(()=>[p(s(l(e)("playerClaimReview.reset")),1)]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:t(({
            tableMaxHeight:n
          }
          )=>[re((b(),le(O,ae(l(z),{
            "max-height":n,data:S.value
          }
          ),{
            default:t(()=>[a(d,{
              prop:"id",label:"ID",width:"70"
            }
            ),a(d,{
              prop:"playerName",label:l(e)("playerClaimReview.playerName"),"min-width":"110","show-overflow-tooltip":""
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(r.playerName||`#${r.playerId}`),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"username",label:l(e)("playerClaimReview.username"),"min-width":"120","show-overflow-tooltip":""
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(r.username||"-"),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"realName",label:l(e)("playerClaimReview.realName"),"min-width":"100","show-overflow-tooltip":""
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(r.realName||"-"),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"phone",label:l(e)("playerClaimReview.phone"),width:"130"
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(r.phone||"-"),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"email",label:l(e)("playerClaimReview.email"),"min-width":"160","show-overflow-tooltip":""
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(r.email||"-"),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"reviewerType",label:l(e)("playerClaimReview.reviewerType"),width:"120"
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(U(r.reviewerType)),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"status",label:l(e)("playerClaimReview.status"),width:"100"
            }
            ,{
              default:t(({
                row:r
              }
              )=>[a(K,{
                type:F(r.status),size:"small",effect:"light"
              }
              ,{
                default:t(()=>[p(s(D(r.status)),1)]),_:2
              }
              ,1032,["type"])]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"rejectReason",label:l(e)("playerClaimReview.rejectReason"),"min-width":"140","show-overflow-tooltip":""
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(r.rejectReason||"-"),1)]),_:1
            }
            ,8,["label"]),a(d,{
              prop:"remark",label:l(e)("playerClaimReview.remark"),"min-width":"140","show-overflow-tooltip":""
            }
            ,null,8,["label"]),a(d,{
              prop:"createdAt",label:l(e)("playerClaimReview.createdAt"),width:"160"
            }
            ,{
              default:t(({
                row:r
              }
              )=>[p(s(l(Ce)(r.createdAt)),1)]),_:1
            }
            ,8,["label"]),a(d,{
              label:l(e)("playerClaimReview.actions"),width:"160",fixed:"right","class-name":"col-operation"
            }
            ,{
              default:t(({
                row:r
              }
              )=>[r.status==="pending"?(b(),k(pe,{
                key:0
              }
              ,[a(y,{
                type:"primary",link:"",onClick:W=>B(r.id)
              }
              ,{
                default:t(()=>[p(s(l(e)("playerClaimReview.approve")),1)]),_:1
              }
              ,8,["onClick"]),a(y,{
                type:"danger",link:"",onClick:W=>$(r.id)
              }
              ,{
                default:t(()=>[p(s(l(e)("playerClaimReview.reject")),1)]),_:1
              }
              ,8,["onClick"])],64)):(b(),k("span",Ve,"-"))]),_:1
            }
            ,8,["label"])]),_:1
          }
          ,16,["max-height","data"])),[[J,h.value]])]),pagination:t(()=>[x("div",Ee,[a(Te,{
            "current-page":m.page,"onUpdate:currentPage":o[3]||(o[3]=n=>m.page=n),"page-size":m.pageSize,"onUpdate:pageSize":o[4]||(o[4]=n=>m.pageSize=n),total:m.total,onChange:c
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      ),a(G,{
        modelValue:_.value,"onUpdate:modelValue":o[7]||(o[7]=n=>_.value=n),title:l(e)("playerClaimReview.rejectTitle"),width:"420px"
      }
      ,{
        footer:t(()=>[a(y,{
          onClick:o[6]||(o[6]=n=>_.value=!1)
        }
        ,{
          default:t(()=>[p(s(l(e)("playerClaimReview.cancel")),1)]),_:1
        }
        ),a(y,{
          type:"danger",loading:T.value,onClick:q
        }
        ,{
          default:t(()=>[p(s(l(e)("playerClaimReview.reject")),1)]),_:1
        }
        ,8,["loading"])]),default:t(()=>[a(E,{
          modelValue:g.value,"onUpdate:modelValue":o[5]||(o[5]=n=>g.value=n),type:"textarea",rows:3,placeholder:l(e)("playerClaimReview.rejectReasonPlaceholder")
        }
        ,null,8,["modelValue","placeholder"])]),_:1
      }
      ,8,["modelValue","title"])])
    }
    
  }
  
}
),$e=Re(Pe,[["__scopeId","data-v-47bbbf8a"]]);

export default $e;
