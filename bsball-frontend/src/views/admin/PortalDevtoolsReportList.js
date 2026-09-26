// PortalDevtoolsReportList —— 行为保真移植自编译产物 PortalDevtoolsReportList-B3l-OAm7（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as U, createElementBlock as B, defineComponent as N, createTextVNode as f, computed as F, toDisplayString as p, createElementVNode as r, unref as g, mergeProps as q, createBlock as S, ref as V, createVNode as e, withDirectives as Q, openBlock as h, withCtx as t, withKeys as v, onMounted as X, KeepAlive as Y, reactive as I, createCommentVNode as te } from 'vue';
import { ElTableColumn as H, ElInput as G, ElFormItem as j, ElTable as J, ElButton as O, ElCard as W, ElTooltip as Z, ElForm as ee } from 'element-plus';
import { useMediaQuery as $ } from '../../composables/useMediaQuery';
import { formatDateTime as le } from '../../utils/formatDate';
import { useSettingsStore as ae } from '../../stores/settings';
import { exportSfc as oe } from '../../utils/exportSfc';
import { portalMonitorApi as ie } from '../../api/system';
import ne from '../../components/admin/AdminListScaffold.js';
import se from '../../components/admin/AdminPagination.js';
import { useListTableAttrs as re } from '../../composables/useListTable';
import { useSuperAdminTenantColumn as de } from '../../composables/useSuperAdminTenantColumn';
import pe from '../../components/admin/AdminDeviceInfoCell.js';
import '../../styles/legacy/portal-devtools-report.css';
var ue={
  class:"admin-page"
}
,me={
  class:"visitor-id-tip admin-list-cell-nowrap"
}
,_e={
  class:"list-ip-with-region"
}
,ce={
  class:"list-ip-with-region__ip"
}
,fe={
  class:"list-ip-with-region__loc"
}
,ge={
  class:"admin-table-td-clip"
}
,ve={
  class:"meta-preview-line admin-list-cell-nowrap"
}
,we={
  class:"pagination-wrap"
}
,he=N({
  __name:"PortalDevtoolsReportList",setup(be){
    const k=re(),{
      isSuperAdmin:R,tenantLabel:P
    }
    =de(),D=ae(),K=$("(max-width: 768px)"),L=F(()=>D.fillPageHeight&&!K.value),b=V([]),w=V(!1),a=I({
      keyword:"",excludeKeyword:"",ip:"",ipRegion:""
    }
    ),n=I({
      page:1,pageSize:10,total:0
    }
    );
    function T(d){
      return d==null||d===""?"-":d.length>80?d.slice(0,80)+"…":d
    }
    function u(){
      n.page=1,_()
    }
    function C(){
      a.keyword="",a.excludeKeyword="",a.ip="",a.ipRegion="",n.page=1,_()
    }
    async function _(){
      w.value=!0;
      const{
        list:d,total:l
      }
      =await ie.devtoolsReportList({
        page:n.page,pageSize:n.pageSize,keyword:a.keyword||void 0,excludeKeyword:a.excludeKeyword||void 0,ip:a.ip||void 0,ipRegion:a.ipRegion||void 0
      }
      );
      b.value=d,n.total=l,w.value=!1
    }
    return X(()=>{
      _()
    }
    ),(d,l)=>{
      const c=G,m=j,y=O,E=ee,s=H,x=Z,M=J,z=W,A=Y;
      return h(),B("div",ue,[e(z,null,{
        header:t(()=>[...l[6]||(l[6]=[r("span",null,"DevTools 上报",-1)])]),default:t(()=>[e(ne,{
          class:"portal-table-scaffold","fill-mode":L.value
        }
        ,{
          filters:t(()=>[e(E,{
            inline:!0,class:"query-form",onSubmit:U(u,["prevent"])
          }
          ,{
            default:t(()=>[e(m,{
              label:"关键词"
            }
            ,{
              default:t(()=>[e(c,{
                modelValue:a.keyword,"onUpdate:modelValue":l[0]||(l[0]=i=>a.keyword=i),placeholder:"访客 ID / IP / 路径 / 路由名 / 设备信息（包含）",clearable:"",style:{
                  width:"300px"
                }
                ,onKeyup:v(u,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(m,{
              label:"排除"
            }
            ,{
              default:t(()=>[e(c,{
                modelValue:a.excludeKeyword,"onUpdate:modelValue":l[1]||(l[1]=i=>a.excludeKeyword=i),placeholder:"任一字段含此串则排除",clearable:"",style:{
                  width:"220px"
                }
                ,onKeyup:v(u,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(m,{
              label:"IP"
            }
            ,{
              default:t(()=>[e(c,{
                modelValue:a.ip,"onUpdate:modelValue":l[2]||(l[2]=i=>a.ip=i),placeholder:"支持模糊匹配",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:v(u,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(m,{
              label:"归属地"
            }
            ,{
              default:t(()=>[e(c,{
                modelValue:a.ipRegion,"onUpdate:modelValue":l[3]||(l[3]=i=>a.ipRegion=i),placeholder:"如 广东 深圳",clearable:"",style:{
                  width:"200px"
                }
                ,onKeyup:v(u,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(m,null,{
              default:t(()=>[e(y,{
                type:"primary",onClick:u
              }
              ,{
                default:t(()=>[...l[7]||(l[7]=[f("查询",-1)])]),_:1
              }
              ),e(y,{
                onClick:C
              }
              ,{
                default:t(()=>[...l[8]||(l[8]=[f("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:t(({
            tableMaxHeight:i
          }
          )=>[Q((h(),S(M,q(g(k),{
            class:"admin-monitor-wide-table","max-height":i,data:b.value
          }
          ),{
            default:t(()=>[e(s,{
              prop:"id",label:"ID",width:"72",align:"right"
            }
            ),g(R)?(h(),S(s,{
              key:0,label:"所属租户",width:"120","show-overflow-tooltip":""
            }
            ,{
              default:t(({
                row:o
              }
              )=>[f(p(g(P)(o.tenantId)),1)]),_:1
            }
            )):te("",!0),e(s,{
              label:"访客 ID","min-width":"220"
            }
            ,{
              default:t(({
                row:o
              }
              )=>[e(x,{
                placement:"top","show-after":200
              }
              ,{
                content:t(()=>[r("div",null,p(o.visitorId),1)]),default:t(()=>[r("span",me,p(o.visitorId),1)]),_:2
              }
              ,1024)]),_:1
            }
            ),e(s,{
              label:"IP / 归属地","min-width":"300"
            }
            ,{
              default:t(({
                row:o
              }
              )=>[r("div",_e,[r("span",ce,p(o.ip||"—"),1),r("span",fe,p(o.ipRegion||"—"),1)])]),_:1
            }
            ),e(s,{
              label:"设备信息","min-width":"280"
            }
            ,{
              default:t(({
                row:o
              }
              )=>[e(pe,{
                "user-agent":o.userAgent
              }
              ,null,8,["user-agent"])]),_:1
            }
            ),e(s,{
              prop:"path",label:"路径","min-width":"260","show-overflow-tooltip":""
            }
            ),e(s,{
              prop:"routeName",label:"路由 name","min-width":"200","show-overflow-tooltip":""
            }
            ),e(s,{
              label:"记录时间",width:"172"
            }
            ,{
              default:t(({
                row:o
              }
              )=>[f(p(g(le)(o.createdAt)),1)]),_:1
            }
            ),e(s,{
              label:"client_meta","min-width":"240"
            }
            ,{
              default:t(({
                row:o
              }
              )=>[r("div",ge,[e(x,{
                disabled:!o.clientMeta||o.clientMeta.length<=90,content:o.clientMeta,placement:"top","show-after":200
              }
              ,{
                default:t(()=>[r("span",ve,p(T(o.clientMeta)),1)]),_:2
              }
              ,1032,["disabled","content"])])]),_:1
            }
            )]),_:1
          }
          ,16,["max-height","data"])),[[A,w.value]])]),pagination:t(()=>[r("div",we,[e(se,{
            "current-page":n.page,"onUpdate:currentPage":l[4]||(l[4]=i=>n.page=i),"page-size":n.pageSize,"onUpdate:pageSize":l[5]||(l[5]=i=>n.pageSize=i),total:n.total,onChange:_
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      )])
    }
    
  }
  
}
),Te=oe(he,[["__scopeId","data-v-56d1ae0d"]]);

export default Te;
