// OperationLogs —— 行为保真移植自编译产物 OperationLogs-B2S6BfQQ（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as q, createElementBlock as L, defineComponent as H, createTextVNode as d, computed as Q, toDisplayString as m, createElementVNode as u, unref as v, mergeProps as G, createBlock as c, ref as T, createVNode as e, withDirectives as J, openBlock as p, withCtx as a, withKeys as S, onMounted as ae, KeepAlive as oe, reactive as z, createCommentVNode as C } from 'vue';
import { ElTableColumn as j, ElInput as W, ElFormItem as X, ElTable as Y, ElOption as Z, ElButton as $, ElCard as ee, ElSelect as le, ElTag as te, ElForm as ie } from 'element-plus';
import { useMediaQuery as ne } from '../../composables/useMediaQuery';
import { stripApiBase as se } from '../../api/request';
import { formatDateTime as re } from '../../utils/formatDate';
import { useSettingsStore as de } from '../../stores/settings';
import { exportSfc as ue } from '../../utils/exportSfc';
import { operationLogApi as pe } from '../../api/system';
import me from '../../components/admin/AdminListScaffold.js';
import _e from '../../components/admin/AdminPagination.js';
import { useListTableAttrs as fe } from '../../composables/useListTable';
import { useSuperAdminTenantColumn as ce } from '../../composables/useSuperAdminTenantColumn';
import '../../styles/legacy/operation-logs.css';
var ge={
  class:"admin-page"
}
,ve={
  key:3
}
,be={
  class:"desc-with-method"
}
,we={
  class:"desc-with-method__method"
}
,ye={
  class:"list-ip-with-region"
}
,he={
  class:"list-ip-with-region__ip"
}
,Se={
  class:"list-ip-with-region__loc"
}
,xe={
  class:"pagination-wrap"
}
,Ve=H({
  __name:"OperationLogs",setup(ke){
    const P=fe(),{
      isSuperAdmin:I,tenantLabel:U
    }
    =ce(),A=de(),R=ne("(max-width: 768px)"),M=Q(()=>A.fillPageHeight&&!R.value),x=T([]),y=T(!1),o=z({
      keyword:"",module:"",action:"",ip:"",ipRegion:""
    }
    ),s=z({
      page:1,pageSize:10,total:0
    }
    );
    function g(){
      s.page=1,b()
    }
    function B(){
      o.keyword="",o.module="",o.action="",o.ip="",o.ipRegion="",s.page=1,b()
    }
    async function b(){
      y.value=!0;
      const{
        list:_,total:l
      }
      =await pe.list({
        page:s.page,pageSize:s.pageSize,keyword:o.keyword||void 0,module:o.module||void 0,action:o.action||void 0,ip:o.ip||void 0,ipRegion:o.ipRegion||void 0
      }
      );
      x.value=_,s.total=l,y.value=!1
    }
    ae(()=>{
      b()
    }
    );
    function V(_){
      const l=(_||"").trim().toLowerCase();
      return l==="create"?"POST":l==="update"?"PUT":l==="delete"?"DELETE":l?l.toUpperCase():""
    }
    function D(_){
      const l=(_||"").trim().toLowerCase();
      return l==="create"?"success":l==="update"?"warning":l==="delete"?"danger":"info"
    }
    return(_,l)=>{
      const h=W,f=X,t=Z,k=le,E=$,O=ie,r=j,w=te,K=Y,N=ee,F=oe;
      return p(),L("div",ge,[e(N,null,{
        header:a(()=>[...l[7]||(l[7]=[u("span",null,"操作历史",-1)])]),default:a(()=>[e(me,{
          class:"operation-log-list-scaffold","fill-mode":M.value
        }
        ,{
          filters:a(()=>[e(O,{
            inline:!0,class:"query-form",onSubmit:q(g,["prevent"])
          }
          ,{
            default:a(()=>[e(f,{
              label:"关键词"
            }
            ,{
              default:a(()=>[e(h,{
                modelValue:o.keyword,"onUpdate:modelValue":l[0]||(l[0]=i=>o.keyword=i),placeholder:"用户名/模块/描述",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:S(g,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(f,{
              label:"模块"
            }
            ,{
              default:a(()=>[e(k,{
                modelValue:o.module,"onUpdate:modelValue":l[1]||(l[1]=i=>o.module=i),placeholder:"全部",clearable:"",filterable:"","allow-create":"","default-first-option":"",style:{
                  width:"200px"
                }
                
              }
              ,{
                default:a(()=>[e(t,{
                  label:"用户管理",value:"用户管理"
                }
                ),e(t,{
                  label:"角色管理",value:"角色管理"
                }
                ),e(t,{
                  label:"菜单管理",value:"菜单管理"
                }
                ),e(t,{
                  label:"API管理",value:"API管理"
                }
                ),e(t,{
                  label:"字典管理",value:"字典管理"
                }
                ),e(t,{
                  label:"公告管理",value:"公告管理"
                }
                ),e(t,{
                  label:"系统公告",value:"系统公告"
                }
                ),e(t,{
                  label:"配置管理",value:"配置管理"
                }
                ),e(t,{
                  label:"资源文件",value:"资源文件"
                }
                ),e(t,{
                  label:"认证",value:"认证"
                }
                ),e(t,{
                  label:"联盟管理",value:"联盟管理"
                }
                ),e(t,{
                  label:"球队管理",value:"球队管理"
                }
                ),e(t,{
                  label:"教练管理",value:"教练管理"
                }
                ),e(t,{
                  label:"球员管理",value:"球员管理"
                }
                ),e(t,{
                  label:"赛事管理",value:"赛事管理"
                }
                ),e(t,{
                  label:"比赛管理",value:"比赛管理"
                }
                ),e(t,{
                  label:"新闻管理",value:"新闻管理"
                }
                ),e(t,{
                  label:"人事变动",value:"人事变动"
                }
                ),e(t,{
                  label:"操作历史",value:"操作历史"
                }
                ),e(t,{
                  label:"系统",value:"系统"
                }
                )]),_:1
              }
              ,8,["modelValue"])]),_:1
            }
            ),e(f,{
              label:"操作"
            }
            ,{
              default:a(()=>[e(k,{
                modelValue:o.action,"onUpdate:modelValue":l[2]||(l[2]=i=>o.action=i),placeholder:"全部",clearable:"",style:{
                  width:"100px"
                }
                
              }
              ,{
                default:a(()=>[e(t,{
                  label:"全部",value:""
                }
                ),e(t,{
                  label:"新增",value:"create"
                }
                ),e(t,{
                  label:"修改",value:"update"
                }
                ),e(t,{
                  label:"删除",value:"delete"
                }
                )]),_:1
              }
              ,8,["modelValue"])]),_:1
            }
            ),e(f,{
              label:"IP"
            }
            ,{
              default:a(()=>[e(h,{
                modelValue:o.ip,"onUpdate:modelValue":l[3]||(l[3]=i=>o.ip=i),placeholder:"支持模糊匹配",clearable:"",style:{
                  width:"160px"
                }
                ,onKeyup:S(g,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(f,{
              label:"归属地"
            }
            ,{
              default:a(()=>[e(h,{
                modelValue:o.ipRegion,"onUpdate:modelValue":l[4]||(l[4]=i=>o.ipRegion=i),placeholder:"如 江苏 南京",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:S(g,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(f,null,{
              default:a(()=>[e(E,{
                type:"primary",onClick:g
              }
              ,{
                default:a(()=>[...l[8]||(l[8]=[d("查询",-1)])]),_:1
              }
              ),e(E,{
                onClick:B
              }
              ,{
                default:a(()=>[...l[9]||(l[9]=[d("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:a(({
            tableMaxHeight:i
          }
          )=>[J((p(),c(K,G(v(P),{
            "max-height":i,data:x.value
          }
          ),{
            default:a(()=>[e(r,{
              prop:"id",label:"ID",width:"70",sortable:""
            }
            ),v(I)?(p(),c(r,{
              key:0,label:"所属租户",width:"120","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:n
              }
              )=>[d(m(v(U)(n.tenantId)),1)]),_:1
            }
            )):C("",!0),e(r,{
              prop:"username",label:"用户名",width:"110"
            }
            ),e(r,{
              prop:"module",label:"模块",width:"100"
            }
            ),e(r,{
              prop:"action",label:"操作",width:"90"
            }
            ,{
              default:a(({
                row:n
              }
              )=>[n.action==="create"?(p(),c(w,{
                key:0,type:"success",size:"small"
              }
              ,{
                default:a(()=>[...l[10]||(l[10]=[d("新增",-1)])]),_:1
              }
              )):n.action==="update"?(p(),c(w,{
                key:1,type:"warning",size:"small"
              }
              ,{
                default:a(()=>[...l[11]||(l[11]=[d("修改",-1)])]),_:1
              }
              )):n.action==="delete"?(p(),c(w,{
                key:2,type:"danger",size:"small"
              }
              ,{
                default:a(()=>[...l[12]||(l[12]=[d("删除",-1)])]),_:1
              }
              )):(p(),L("span",ve,m(n.action||"-"),1))]),_:1
            }
            ),e(r,{
              label:"描述","min-width":"260","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:n
              }
              )=>[u("div",be,[u("span",we,[V(n.action)?(p(),c(w,{
                key:0,type:D(n.action),size:"small",effect:"plain",class:"desc-with-method__tag"
              }
              ,{
                default:a(()=>[d(m(V(n.action)),1)]),_:2
              }
              ,1032,["type"])):C("",!0)]),u("span",null,m(v(se)(n.description)||"-"),1)])]),_:1
            }
            ),e(r,{
              label:"IP / 归属地","min-width":"240","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:n
              }
              )=>[u("div",ye,[u("span",he,m(n.ip||"—"),1),u("span",Se,m(n.ipRegion||"—"),1)])]),_:1
            }
            ),e(r,{
              label:"操作时间",width:"180"
            }
            ,{
              default:a(({
                row:n
              }
              )=>[d(m(v(re)(n.createdAt)),1)]),_:1
            }
            )]),_:1
          }
          ,16,["max-height","data"])),[[F,y.value]])]),pagination:a(()=>[u("div",xe,[e(_e,{
            "current-page":s.page,"onUpdate:currentPage":l[5]||(l[5]=i=>s.page=i),"page-size":s.pageSize,"onUpdate:pageSize":l[6]||(l[6]=i=>s.pageSize=i),total:s.total,onChange:b
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      )])
    }
    
  }
  
}
),De=ue(Ve,[["__scopeId","data-v-16c124f5"]]);

export default De;
