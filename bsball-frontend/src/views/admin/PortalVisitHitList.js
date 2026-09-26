// PortalVisitHitList —— 行为保真移植自编译产物 PortalVisitHitList-DqSzj6sM（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as O, createElementBlock as j, defineComponent as J, createTextVNode as g, computed as W, toDisplayString as _, createElementVNode as u, unref as w, mergeProps as X, createBlock as C, ref as y, createVNode as t, withDirectives as $, openBlock as D, withCtx as a, withKeys as P, onMounted as ie, KeepAlive as ne, reactive as E, createCommentVNode as ue } from 'vue';
import { ElDatePicker as Y, ElTableColumn as Z, ElInput as ee, ElFormItem as te, ElTable as ae, ElButton as le, ElCard as oe, ElTooltip as se, ElForm as de } from 'element-plus';
import { useMediaQuery as re } from '../../composables/useMediaQuery';
import { formatDateTime as pe } from '../../utils/formatDate';
import { useSettingsStore as me } from '../../stores/settings';
import { exportSfc as _e } from '../../utils/exportSfc';
import { portalMonitorApi as fe } from '../../api/system';
import { formatYmd as L } from '../../utils/visitRangeOptions';
import { startOfDay as f } from '../../utils/visitRangeOptions';
import ce from '../../components/admin/AdminListScaffold.js';
import ge from '../../components/admin/AdminPagination.js';
import { useListTableAttrs as ve } from '../../composables/useListTable';
import { useSuperAdminTenantColumn as he } from '../../composables/useSuperAdminTenantColumn';
import we from '../../components/admin/AdminDeviceInfoCell.js';
import '../../styles/legacy/portal-visit-hit.css';
var ye={
  class:"admin-page"
}
,be={
  class:"portal-hit-date-range"
}
,De={
  class:"visitor-id-tip admin-list-cell-nowrap"
}
,xe={
  class:"list-ip-with-region"
}
,Se={
  class:"list-ip-with-region__ip"
}
,Te={
  class:"list-ip-with-region__loc"
}
,Ve={
  class:"pagination-wrap"
}
,ke=J({
  __name:"PortalVisitHitList",setup(Ie){
    const z=ve(),{
      isSuperAdmin:A,tenantLabel:H
    }
    =he(),K=me(),U=re("(max-width: 768px)"),B=W(()=>K.fillPageHeight&&!U.value),x=y([]),b=y(!1),p=E({
      keyword:"",excludeKeyword:""
    }
    ),n=y(null),s=y(null),i=E({
      page:1,pageSize:10,total:0
    }
    );
    function S(d){
      return f(d).getTime()>f(new Date).getTime()
    }
    function T(){
      if(!n.value||!s.value)return;
      const d=f(new Date);
      let e=f(n.value),r=f(s.value);
      e.getTime()>d.getTime()&&(e=d),r.getTime()>d.getTime()&&(r=d),e.getTime()>r.getTime()?(n.value=r,s.value=e):(n.value=e,s.value=r)
    }
    function R(){
      return!n.value||!s.value?{
        
      }
      :(T(),{
        hitDateFrom:L(f(n.value)),hitDateTo:L(f(s.value))
      }
      )
    }
    function V(){
      !n.value||!s.value||(T(),i.page=1,c())
    }
    function v(){
      i.page=1,c()
    }
    function F(){
      p.keyword="",p.excludeKeyword="",n.value=null,s.value=null,i.page=1,c()
    }
    async function c(){
      b.value=!0;
      try{
        const d=R(),{
          list:e,total:r
        }
        =await fe.visitHitList({
          page:i.page,pageSize:i.pageSize,keyword:p.keyword||void 0,excludeKeyword:p.excludeKeyword||void 0,...d
        }
        );
        x.value=e,i.total=r
      }
      finally{
        b.value=!1
      }
      
    }
    return ie(()=>{
      c()
    }
    ),(d,e)=>{
      const r=Y,h=te,k=ee,I=le,M=de,m=Z,N=se,q=ae,Q=oe,G=ne;
      return D(),j("div",ye,[t(Q,null,{
        header:a(()=>[...e[6]||(e[6]=[u("span",null,"门户访问打点",-1)])]),default:a(()=>[t(ce,{
          class:"portal-table-scaffold","fill-mode":B.value
        }
        ,{
          filters:a(()=>[t(M,{
            inline:!0,class:"query-form portal-visit-hit-query",onSubmit:O(v,["prevent"])
          }
          ,{
            default:a(()=>[t(h,{
              label:"统计日"
            }
            ,{
              default:a(()=>[u("div",be,[t(r,{
                modelValue:n.value,"onUpdate:modelValue":e[0]||(e[0]=l=>n.value=l),type:"date",class:"portal-hit-date-picker",placeholder:"开始日期","disabled-date":S,onChange:V
              }
              ,null,8,["modelValue"]),e[7]||(e[7]=u("span",{
                class:"portal-hit-date-sep","aria-hidden":"true"
              }
              ,"至",-1)),t(r,{
                modelValue:s.value,"onUpdate:modelValue":e[1]||(e[1]=l=>s.value=l),type:"date",class:"portal-hit-date-picker",placeholder:"结束日期","disabled-date":S,onChange:V
              }
              ,null,8,["modelValue"])])]),_:1
            }
            ),t(h,{
              label:"关键词"
            }
            ,{
              default:a(()=>[t(k,{
                modelValue:p.keyword,"onUpdate:modelValue":e[2]||(e[2]=l=>p.keyword=l),placeholder:"访客 ID / IP / 归属地 / 路径 / 设备 UA（模糊匹配，任一命中即可）",clearable:"",style:{
                  width:"min(100%, 380px)"
                }
                ,onKeyup:P(v,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),t(h,{
              label:"排除"
            }
            ,{
              default:a(()=>[t(k,{
                modelValue:p.excludeKeyword,"onUpdate:modelValue":e[3]||(e[3]=l=>p.excludeKeyword=l),placeholder:"以上字段或归属地任一处含此串则整行排除",clearable:"",style:{
                  width:"260px"
                }
                ,onKeyup:P(v,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),t(h,null,{
              default:a(()=>[t(I,{
                type:"primary",onClick:v
              }
              ,{
                default:a(()=>[...e[8]||(e[8]=[g("查询",-1)])]),_:1
              }
              ),t(I,{
                onClick:F
              }
              ,{
                default:a(()=>[...e[9]||(e[9]=[g("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:a(({
            tableMaxHeight:l
          }
          )=>[$((D(),C(q,X(w(z),{
            class:"admin-monitor-wide-table","max-height":l,data:x.value
          }
          ),{
            default:a(()=>[t(m,{
              prop:"id",label:"ID",width:"72",align:"right"
            }
            ),w(A)?(D(),C(m,{
              key:0,label:"所属租户",width:"120","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:o
              }
              )=>[g(_(w(H)(o.tenantId)),1)]),_:1
            }
            )):ue("",!0),t(m,{
              label:"统计日",width:"118"
            }
            ,{
              default:a(({
                row:o
              }
              )=>[g(_(o.hitDate||"-"),1)]),_:1
            }
            ),t(m,{
              label:"访客 ID","min-width":"220"
            }
            ,{
              default:a(({
                row:o
              }
              )=>[t(N,{
                placement:"top","show-after":200
              }
              ,{
                content:a(()=>[u("div",null,_(o.visitorId),1)]),default:a(()=>[u("span",De,_(o.visitorId),1)]),_:2
              }
              ,1024)]),_:1
            }
            ),t(m,{
              label:"IP / 归属地","min-width":"300"
            }
            ,{
              default:a(({
                row:o
              }
              )=>[u("div",xe,[u("span",Se,_(o.ip||"—"),1),u("span",Te,_(o.ipRegion||"—"),1)])]),_:1
            }
            ),t(m,{
              label:"设备信息","min-width":"280"
            }
            ,{
              default:a(({
                row:o
              }
              )=>[t(we,{
                "user-agent":o.userAgent
              }
              ,null,8,["user-agent"])]),_:1
            }
            ),t(m,{
              prop:"path",label:"路径","min-width":"260","show-overflow-tooltip":""
            }
            ),t(m,{
              label:"记录时间",width:"172"
            }
            ,{
              default:a(({
                row:o
              }
              )=>[g(_(w(pe)(o.createdAt)),1)]),_:1
            }
            )]),_:1
          }
          ,16,["max-height","data"])),[[G,b.value]])]),pagination:a(()=>[u("div",Ve,[t(ge,{
            "current-page":i.page,"onUpdate:currentPage":e[4]||(e[4]=l=>i.page=l),"page-size":i.pageSize,"onUpdate:pageSize":e[5]||(e[5]=l=>i.pageSize=l),total:i.total,onChange:c
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      )])
    }
    
  }
  
}
),Me=_e(ke,[["__scopeId","data-v-865adfb7"]]);

export default Me;
