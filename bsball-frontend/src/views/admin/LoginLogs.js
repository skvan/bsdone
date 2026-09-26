// LoginLogs —— 行为保真移植自编译产物 LoginLogs-Dc7Lv4cL（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as N, createElementBlock as x, defineComponent as F, createTextVNode as r, computed as q, toDisplayString as m, createElementVNode as _, unref as f, mergeProps as H, createBlock as c, ref as L, createVNode as e, withDirectives as G, openBlock as p, withCtx as a, withKeys as y, onMounted as $, KeepAlive as te, reactive as E, createCommentVNode as oe } from 'vue';
import { ElTableColumn as Q, ElInput as O, ElFormItem as j, ElTable as J, ElOption as W, ElButton as X, ElCard as Y, ElSelect as Z, ElTag as ee, ElForm as le } from 'element-plus';
import { useMediaQuery as ae } from '../../composables/useMediaQuery';
import { formatDateTime as se } from '../../utils/formatDate';
import { useSettingsStore as ne } from '../../stores/settings';
import { exportSfc as ie } from '../../utils/exportSfc';
import { loginLogApi as re } from '../../api/system';
import pe from '../../components/admin/AdminListScaffold.js';
import de from '../../components/admin/AdminPagination.js';
import { useListTableAttrs as ue } from '../../composables/useListTable';
import { useSuperAdminTenantColumn as me } from '../../composables/useSuperAdminTenantColumn';
import _e from '../../components/admin/AdminDeviceInfoCell.js';
import '../../styles/legacy/login-logs.css';
var ge={
  class:"admin-page"
}
,fe={
  class:"list-ip-with-region"
}
,ce={
  class:"list-ip-with-region__ip"
}
,ve={
  class:"list-ip-with-region__loc"
}
,we={
  key:2
}
,be={
  class:"pagination-wrap"
}
,ye=F({
  __name:"LoginLogs",setup(he){
    const z=ue(),{
      isSuperAdmin:I,tenantLabel:C
    }
    =me(),T=ne(),R=ae("(max-width: 768px)"),P=q(()=>T.fillPageHeight&&!R.value),h=L([]),v=L(!1),l=E({
      keyword:"",status:"",ip:"",ipRegion:""
    }
    ),n=E({
      page:1,pageSize:10,total:0
    }
    );
    function d(){
      n.page=1,g()
    }
    function A(){
      l.keyword="",l.status="",l.ip="",l.ipRegion="",n.page=1,g()
    }
    async function g(){
      v.value=!0;
      const{
        list:S,total:t
      }
      =await re.list({
        page:n.page,pageSize:n.pageSize,keyword:l.keyword||void 0,status:l.status||void 0,ip:l.ip||void 0,ipRegion:l.ipRegion||void 0
      }
      );
      h.value=S,n.total=t,v.value=!1
    }
    return $(()=>{
      g()
    }
    ),(S,t)=>{
      const w=O,u=j,b=W,U=Z,k=X,B=le,i=Q,V=ee,D=J,M=Y,K=te;
      return p(),x("div",ge,[e(M,null,{
        header:a(()=>[...t[6]||(t[6]=[_("span",null,"登录历史",-1)])]),default:a(()=>[e(pe,{
          class:"login-log-list-scaffold","fill-mode":P.value
        }
        ,{
          filters:a(()=>[e(B,{
            inline:!0,class:"query-form",onSubmit:N(d,["prevent"])
          }
          ,{
            default:a(()=>[e(u,{
              label:"用户名"
            }
            ,{
              default:a(()=>[e(w,{
                modelValue:l.keyword,"onUpdate:modelValue":t[0]||(t[0]=o=>l.keyword=o),placeholder:"用户名",clearable:"",style:{
                  width:"160px"
                }
                ,onKeyup:y(d,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(u,{
              label:"状态"
            }
            ,{
              default:a(()=>[e(U,{
                modelValue:l.status,"onUpdate:modelValue":t[1]||(t[1]=o=>l.status=o),placeholder:"全部",clearable:"",style:{
                  width:"120px"
                }
                
              }
              ,{
                default:a(()=>[e(b,{
                  label:"全部",value:""
                }
                ),e(b,{
                  label:"成功",value:"success"
                }
                ),e(b,{
                  label:"失败",value:"fail"
                }
                )]),_:1
              }
              ,8,["modelValue"])]),_:1
            }
            ),e(u,{
              label:"IP"
            }
            ,{
              default:a(()=>[e(w,{
                modelValue:l.ip,"onUpdate:modelValue":t[2]||(t[2]=o=>l.ip=o),placeholder:"支持模糊匹配",clearable:"",style:{
                  width:"160px"
                }
                ,onKeyup:y(d,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(u,{
              label:"归属地"
            }
            ,{
              default:a(()=>[e(w,{
                modelValue:l.ipRegion,"onUpdate:modelValue":t[3]||(t[3]=o=>l.ipRegion=o),placeholder:"如 北京",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:y(d,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),e(u,null,{
              default:a(()=>[e(k,{
                type:"primary",onClick:d
              }
              ,{
                default:a(()=>[...t[7]||(t[7]=[r("查询",-1)])]),_:1
              }
              ),e(k,{
                onClick:A
              }
              ,{
                default:a(()=>[...t[8]||(t[8]=[r("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:a(({
            tableMaxHeight:o
          }
          )=>[G((p(),c(D,H(f(z),{
            class:"admin-monitor-wide-table","max-height":o,data:h.value
          }
          ),{
            default:a(()=>[e(i,{
              prop:"id",label:"ID",width:"72",align:"right",sortable:""
            }
            ),f(I)?(p(),c(i,{
              key:0,label:"所属租户",width:"120","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:s
              }
              )=>[r(m(f(C)(s.tenantId)),1)]),_:1
            }
            )):oe("",!0),e(i,{
              prop:"username",label:"用户名","min-width":"120","show-overflow-tooltip":""
            }
            ),e(i,{
              label:"登录时间",width:"172"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[r(m(f(se)(s.loginTime)),1)]),_:1
            }
            ),e(i,{
              label:"IP / 位置","min-width":"300"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[_("div",fe,[_("span",ce,m(s.ip||"—"),1),_("span",ve,m(s.location||"—"),1)])]),_:1
            }
            ),e(i,{
              label:"设备信息","min-width":"280"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[e(_e,{
                "user-agent":s.deviceInfo
              }
              ,null,8,["user-agent"])]),_:1
            }
            ),e(i,{
              prop:"status",label:"状态",width:"88"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[s.status==="success"?(p(),c(V,{
                key:0,type:"success",size:"small"
              }
              ,{
                default:a(()=>[...t[9]||(t[9]=[r("成功",-1)])]),_:1
              }
              )):s.status==="fail"?(p(),c(V,{
                key:1,type:"danger",size:"small"
              }
              ,{
                default:a(()=>[...t[10]||(t[10]=[r("失败",-1)])]),_:1
              }
              )):(p(),x("span",we,m(s.status||"-"),1))]),_:1
            }
            ),e(i,{
              prop:"failReason",label:"失败原因","min-width":"200","show-overflow-tooltip":""
            }
            )]),_:1
          }
          ,16,["max-height","data"])),[[K,v.value]])]),pagination:a(()=>[_("div",be,[e(de,{
            "current-page":n.page,"onUpdate:currentPage":t[4]||(t[4]=o=>n.page=o),"page-size":n.pageSize,"onUpdate:pageSize":t[5]||(t[5]=o=>n.pageSize=o),total:n.total,onChange:g
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      )])
    }
    
  }
  
}
),Ae=ie(ye,[["__scopeId","data-v-e68a1383"]]);

export default Ae;
