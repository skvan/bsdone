// Apis —— 行为保真移植自编译产物 Apis-C6T1TXVc（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as W, createElementBlock as X, defineComponent as Z, createTextVNode as d, computed as ee, createElementVNode as L, unref as f, mergeProps as te, createBlock as le, ref as w, createVNode as t, withDirectives as oe, openBlock as P, withCtx as l, withKeys as re, onMounted as fe, KeepAlive as ce, reactive as x } from 'vue';
import { ElDialog as Y, ElTableColumn as ae, ElInput as ie, ElFormItem as ne, ElMessage as r, ElTable as se, ElOption as de, ElButton as ue, ElCard as pe, ElSelect as me, ElMessageBox as ge, ElForm as ve } from 'element-plus';
import { useMediaQuery as _e } from '../../composables/useMediaQuery';
import { useSettingsStore as be } from '../../stores/settings';
import { exportSfc as we } from '../../utils/exportSfc';
import { apiResourceApi as y } from '../../api/system';
import ye from '../../components/admin/AdminListScaffold.js';
import ke from '../../components/admin/AdminPagination.js';
import { useFixedOperationColumn as he } from '../../composables/useListTable';
import { useListTableAttrs as xe } from '../../composables/useListTable';
import { useModalClose as Ee } from '../../composables/useModalClose';
import { useSubmitLock as U } from '../../composables/useSubmitLock';
import '../../styles/legacy/apis.css';
var Se={
  class:"admin-page"
}
,Ve={
  class:"pagination-wrap"
}
,Ce=Z({
  __name:"Apis",setup(Te){
    const{
      submitting:E,withSubmitLock:A
    }
    =U(),{
      submitting:M,withSubmitLock:B
    }
    =U(),I=xe(),N=he(),z=Ee(),D=be(),F=_e("(max-width: 768px)"),O=ee(()=>D.fillPageHeight&&!F.value),S=w([]),k=w(!1),g=w(!1),c=w(null),_=x({
      keyword:""
    }
    ),n=x({
      page:1,pageSize:10,total:0
    }
    ),o=x({
      path:"",method:"GET",description:"",groupName:""
    }
    );
    function V(i){
      c.value=i?.id??null,o.path=i?.path??"",o.method=i?.method??"GET",o.description=i?.description??"",o.groupName=i?.groupName??"",g.value=!0
    }
    async function $(){
      await A(async()=>{
        try{
          if(!o.path.trim()){
            r.warning("请输入路径");
            return
          }
          if((o.description?.length??0)>200){
            r.warning("描述不能超过200字");
            return
          }
          c.value?(await y.update(c.value,o),r.success("修改成功")):(await y.create(o),r.success("新增成功")),g.value=!1,u()
        }
        catch(i){
          r.error(i?.message||"保存失败")
        }
        
      }
      )
    }
    async function G(i){
      await B(async()=>{
        try{
          await ge.confirm(`确定要删除接口「${i.method} ${i.path}」吗？`,"删除确认",{
            confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"
          }
          ),await y.delete(i.id),r.success("删除成功"),u()
        }
        catch(e){
          e!=="cancel"&&r.error(e?.message||"删除失败")
        }
        
      }
      )
    }
    function h(){
      n.page=1,u()
    }
    function q(){
      _.keyword="",n.page=1,u()
    }
    async function u(){
      k.value=!0;
      const{
        list:i,total:e
      }
      =await y.list({
        page:n.page,pageSize:n.pageSize,keyword:_.keyword||void 0
      }
      );
      S.value=i,n.total=e,k.value=!1
    }
    return fe(()=>u()),(i,e)=>{
      const s=ue,v=ie,p=ne,C=ve,m=ae,H=se,K=pe,b=de,Q=me,R=Y,j=ce;
      return P(),X("div",Se,[t(K,null,{
        header:l(()=>[e[11]||(e[11]=L("span",null,"API 管理",-1)),t(s,{
          type:"primary",style:{
            float:"right"
          }
          ,onClick:e[0]||(e[0]=a=>V())
        }
        ,{
          default:l(()=>[...e[10]||(e[10]=[d("新增",-1)])]),_:1
        }
        )]),default:l(()=>[t(ye,{
          class:"api-list-scaffold","fill-mode":O.value
        }
        ,{
          filters:l(()=>[t(C,{
            inline:!0,class:"query-form",onSubmit:W(h,["prevent"])
          }
          ,{
            default:l(()=>[t(p,{
              label:"关键词"
            }
            ,{
              default:l(()=>[t(v,{
                modelValue:_.keyword,"onUpdate:modelValue":e[1]||(e[1]=a=>_.keyword=a),placeholder:"路径/描述/分组",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:re(h,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),t(p,null,{
              default:l(()=>[t(s,{
                type:"primary",onClick:h
              }
              ,{
                default:l(()=>[...e[12]||(e[12]=[d("查询",-1)])]),_:1
              }
              ),t(s,{
                onClick:q
              }
              ,{
                default:l(()=>[...e[13]||(e[13]=[d("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:l(({
            tableMaxHeight:a
          }
          )=>[oe((P(),le(H,te(f(I),{
            "max-height":a,data:S.value
          }
          ),{
            default:l(()=>[t(m,{
              prop:"id",label:"ID",width:"80",sortable:""
            }
            ),t(m,{
              prop:"method",label:"方法",width:"90"
            }
            ),t(m,{
              prop:"path",label:"路径","min-width":"220","show-overflow-tooltip":""
            }
            ),t(m,{
              prop:"description",label:"描述","min-width":"120","show-overflow-tooltip":""
            }
            ),t(m,{
              prop:"groupName",label:"分组",width:"100","show-overflow-tooltip":""
            }
            ),t(m,{
              label:"操作","min-width":"60",fixed:f(N),"class-name":"col-operation"
            }
            ,{
              default:l(({
                row:T
              }
              )=>[t(s,{
                link:"",type:"primary",onClick:J=>V(T)
              }
              ,{
                default:l(()=>[...e[14]||(e[14]=[d("编辑",-1)])]),_:1
              }
              ,8,["onClick"]),t(s,{
                link:"",type:"danger",disabled:f(M),onClick:J=>G(T)
              }
              ,{
                default:l(()=>[...e[15]||(e[15]=[d("删除",-1)])]),_:1
              }
              ,8,["disabled","onClick"])]),_:1
            }
            ,8,["fixed"])]),_:1
          }
          ,16,["max-height","data"])),[[j,k.value]])]),pagination:l(()=>[L("div",Ve,[t(ke,{
            "current-page":n.page,"onUpdate:currentPage":e[2]||(e[2]=a=>n.page=a),"page-size":n.pageSize,"onUpdate:pageSize":e[3]||(e[3]=a=>n.pageSize=a),total:n.total,onChange:u
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      ),t(R,{
        modelValue:g.value,"onUpdate:modelValue":e[9]||(e[9]=a=>g.value=a),title:c.value?"编辑 API":"新增 API",width:"520","close-on-click-modal":f(z)
      }
      ,{
        footer:l(()=>[t(s,{
          onClick:e[8]||(e[8]=a=>g.value=!1)
        }
        ,{
          default:l(()=>[...e[16]||(e[16]=[d("取消",-1)])]),_:1
        }
        ),t(s,{
          type:"primary",loading:f(E),disabled:f(E),onClick:$
        }
        ,{
          default:l(()=>[...e[17]||(e[17]=[d("确定",-1)])]),_:1
        }
        ,8,["loading","disabled"])]),default:l(()=>[t(C,{
          model:o,"label-width":"90px"
        }
        ,{
          default:l(()=>[t(p,{
            label:"路径"
          }
          ,{
            default:l(()=>[t(v,{
              modelValue:o.path,"onUpdate:modelValue":e[4]||(e[4]=a=>o.path=a),placeholder:"/api/xxx"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(p,{
            label:"方法"
          }
          ,{
            default:l(()=>[t(Q,{
              modelValue:o.method,"onUpdate:modelValue":e[5]||(e[5]=a=>o.method=a),placeholder:"方法",style:{
                width:"100%"
              }
              
            }
            ,{
              default:l(()=>[t(b,{
                label:"GET",value:"GET"
              }
              ),t(b,{
                label:"POST",value:"POST"
              }
              ),t(b,{
                label:"PUT",value:"PUT"
              }
              ),t(b,{
                label:"DELETE",value:"DELETE"
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),t(p,{
            label:"描述"
          }
          ,{
            default:l(()=>[t(v,{
              modelValue:o.description,"onUpdate:modelValue":e[6]||(e[6]=a=>o.description=a),placeholder:"接口说明，最多200字",maxlength:"200","show-word-limit":""
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(p,{
            label:"分组"
          }
          ,{
            default:l(()=>[t(v,{
              modelValue:o.groupName,"onUpdate:modelValue":e[7]||(e[7]=a=>o.groupName=a),placeholder:"如：用户、角色、菜单"
            }
            ,null,8,["modelValue"])]),_:1
          }
          )]),_:1
        }
        ,8,["model"])]),_:1
      }
      ,8,["modelValue","title","close-on-click-modal"])])
    }
    
  }
  
}
),$e=we(Ce,[["__scopeId","data-v-b4e46fa6"]]);

export default $e;
