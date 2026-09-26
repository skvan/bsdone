// Leagues —— 行为保真移植自编译产物 Leagues-BIaLS4-C（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { nextTick as oe, withModifiers as ne, createElementBlock as ie, defineComponent as de, createTextVNode as s, computed as ue, toDisplayString as B, createElementVNode as x, unref as d, mergeProps as me, createBlock as D, ref as f, createVNode as t, withDirectives as fe, openBlock as C, withCtx as a, withKeys as _e, onMounted as ye, KeepAlive as Ve, reactive as E, createCommentVNode as Ee } from 'vue';
import { ElInputNumber as se, ElDialog as re, ElTableColumn as pe, ElInput as ce, ElFormItem as ge, ElMessage as v, ElTable as ve, ElButton as be, ElCard as we, ElMessageBox as ke, ElSwitch as Se, ElForm as Ce } from 'element-plus';
import { useMediaQuery as xe } from '../../composables/useMediaQuery';
import { formatDateTime as Le } from '../../utils/formatDate';
import { useSettingsStore as Te } from '../../stores/settings';
import { exportSfc as Me } from '../../utils/exportSfc';
import { leagueApi as k } from '../../api/business';
import Ue from '../../components/admin/AdminListScaffold.js';
import he from '../../components/admin/AdminPagination.js';
import { useFixedOperationColumn as ze } from '../../composables/useListTable';
import { useListTableAttrs as Be } from '../../composables/useListTable';
import { useModalClose as De } from '../../composables/useModalClose';
import { useSuperAdminTenantColumn as Ie } from '../../composables/useSuperAdminTenantColumn';
import { useSubmitLock as I } from '../../composables/useSubmitLock';
import '../../styles/legacy/leagues.css';
var Ae={
  class:"admin-page"
}
,Pe={
  class:"pagination-wrap"
}
,Fe=de({
  __name:"Leagues",setup($e){
    const{
      submitting:L,withSubmitLock:A
    }
    =I(),{
      submitting:Ne,withSubmitLock:P
    }
    =I(),{
      isSuperAdmin:F,tenantLabel:$
    }
    =Ie(),N=Be(),O=ze(),R=De(),q=Te(),H=xe("(max-width: 768px)"),K=ue(()=>q.fillPageHeight&&!H.value),T=f([]),V=f(!1),g=f(!1),_=f(null),b=f(),o=E({
      name:"",nameEn:"",description:"",verified:0,sort:0
    }
    ),Q={
      name:[{
        required:!0,message:"请输入联盟名称",trigger:"blur"
      }
      ],description:[{
        max:2e3,message:"描述不能超过2000字",trigger:"blur"
      }
      ]
    }
    ,w=E({
      keyword:""
    }
    ),i=E({
      page:1,pageSize:10,total:0
    }
    ),M=f("id"),U=f("asc");
    function G({
      prop:n,order:e
    }
    ){
      M.value=n||"id",U.value=e==="descending"?"desc":"asc",i.page=1,u()
    }
    function h(n){
      _.value=n?.id??null,o.name=n?.name??"",o.nameEn=n?.nameEn??"",o.description=n?.description??"",o.verified=n?.verified?1:0,o.sort=n?.sort??0,g.value=!0,oe(()=>b.value?.clearValidate())
    }
    async function j(){
      await A(async()=>{
        if(b.value)try{
          await b.value.validate(),_.value?(await k.update(_.value,o),v.success("修改成功")):(await k.create(o),v.success("新增成功")),g.value=!1,u()
        }
        catch(n){
          v.error(n?.message||"保存失败")
        }
        
      }
      )
    }
    async function J(n){
      await P(async()=>{
        try{
          await ke.confirm(`确定要删除联盟「${n.name}」吗？关联的球队与赛事将变为未分组。`,"删除确认",{
            confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"
          }
          ),await k.delete(n.id),v.success("删除成功"),u()
        }
        catch(e){
          e!=="cancel"&&v.error(e?.message||"删除失败")
        }
        
      }
      )
    }
    function S(){
      i.page=1,u()
    }
    function W(){
      w.keyword="",i.page=1,u()
    }
    async function u(){
      V.value=!0;
      const{
        list:n,total:e
      }
      =await k.list({
        page:i.page,pageSize:i.pageSize,keyword:w.keyword||void 0,sortProp:M.value,sortOrder:U.value
      }
      );
      T.value=n,i.total=e,V.value=!1
    }
    return ye(()=>u()),(n,e)=>{
      const m=be,y=ce,p=ge,z=Ce,r=pe,X=ve,Y=we,Z=Se,ee=se,te=re,ae=Ve;
      return C(),ie("div",Ae,[t(Y,null,{
        header:a(()=>[e[12]||(e[12]=x("span",null,"联盟管理",-1)),t(m,{
          type:"primary",style:{
            float:"right"
          }
          ,onClick:e[0]||(e[0]=l=>h())
        }
        ,{
          default:a(()=>[...e[11]||(e[11]=[s("新增联盟",-1)])]),_:1
        }
        )]),default:a(()=>[t(Ue,{
          class:"league-list-scaffold","fill-mode":K.value
        }
        ,{
          filters:a(()=>[t(z,{
            inline:!0,class:"query-form",onSubmit:ne(S,["prevent"])
          }
          ,{
            default:a(()=>[t(p,{
              label:"关键词"
            }
            ,{
              default:a(()=>[t(y,{
                modelValue:w.keyword,"onUpdate:modelValue":e[1]||(e[1]=l=>w.keyword=l),placeholder:"联盟名称",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:_e(S,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),t(p,null,{
              default:a(()=>[t(m,{
                type:"primary",onClick:S
              }
              ,{
                default:a(()=>[...e[13]||(e[13]=[s("查询",-1)])]),_:1
              }
              ),t(m,{
                onClick:W
              }
              ,{
                default:a(()=>[...e[14]||(e[14]=[s("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:a(({
            tableMaxHeight:l
          }
          )=>[fe((C(),D(X,me(d(N),{
            "max-height":l,data:T.value,"default-sort":{
              prop:"id",order:"ascending"
            }
            ,onSortChange:G
          }
          ),{
            default:a(()=>[t(r,{
              prop:"id",label:"ID",width:"80",sortable:""
            }
            ),d(F)?(C(),D(r,{
              key:0,label:"所属租户",width:"120","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:c
              }
              )=>[s(B(d($)(c.tenantId)),1)]),_:1
            }
            )):Ee("",!0),t(r,{
              prop:"name",label:"联盟名称","min-width":"140"
            }
            ),t(r,{
              prop:"nameEn",label:"英文名",width:"180"
            }
            ),t(r,{
              prop:"description",label:"描述","min-width":"160","show-overflow-tooltip":""
            }
            ),t(r,{
              prop:"sort",label:"排序",width:"80"
            }
            ),t(r,{
              prop:"createdAt",label:"创建时间",width:"160"
            }
            ,{
              default:a(({
                row:c
              }
              )=>[s(B(d(Le)(c.createdAt)),1)]),_:1
            }
            ),t(r,{
              label:"操作","min-width":"60",fixed:d(O),"class-name":"col-operation"
            }
            ,{
              default:a(({
                row:c
              }
              )=>[t(m,{
                link:"",type:"primary",onClick:le=>h(c)
              }
              ,{
                default:a(()=>[...e[15]||(e[15]=[s("编辑",-1)])]),_:1
              }
              ,8,["onClick"]),t(m,{
                link:"",type:"danger",onClick:le=>J(c)
              }
              ,{
                default:a(()=>[...e[16]||(e[16]=[s("删除",-1)])]),_:1
              }
              ,8,["onClick"])]),_:1
            }
            ,8,["fixed"])]),_:1
          }
          ,16,["max-height","data"])),[[ae,V.value]])]),pagination:a(()=>[x("div",Pe,[t(he,{
            "current-page":i.page,"onUpdate:currentPage":e[2]||(e[2]=l=>i.page=l),"page-size":i.pageSize,"onUpdate:pageSize":e[3]||(e[3]=l=>i.pageSize=l),total:i.total,onChange:u
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      ),t(te,{
        modelValue:g.value,"onUpdate:modelValue":e[10]||(e[10]=l=>g.value=l),title:_.value?"编辑联盟":"新增联盟",width:"480","close-on-click-modal":d(R)
      }
      ,{
        footer:a(()=>[t(m,{
          onClick:e[9]||(e[9]=l=>g.value=!1)
        }
        ,{
          default:a(()=>[...e[18]||(e[18]=[s("取消",-1)])]),_:1
        }
        ),t(m,{
          type:"primary",loading:d(L),disabled:d(L),onClick:j
        }
        ,{
          default:a(()=>[...e[19]||(e[19]=[s("确定",-1)])]),_:1
        }
        ,8,["loading","disabled"])]),default:a(()=>[t(z,{
          ref_key:"formRef",ref:b,model:o,rules:Q,"label-width":"90px"
        }
        ,{
          default:a(()=>[t(p,{
            label:"联盟名称",prop:"name"
          }
          ,{
            default:a(()=>[t(y,{
              modelValue:o.name,"onUpdate:modelValue":e[4]||(e[4]=l=>o.name=l),placeholder:"如：示例联盟"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(p,{
            label:"英文名"
          }
          ,{
            default:a(()=>[t(y,{
              modelValue:o.nameEn,"onUpdate:modelValue":e[5]||(e[5]=l=>o.nameEn=l),placeholder:"如：Demo League"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(p,{
            label:"描述"
          }
          ,{
            default:a(()=>[t(y,{
              modelValue:o.description,"onUpdate:modelValue":e[6]||(e[6]=l=>o.description=l),type:"textarea",rows:3,placeholder:"联盟简介，最多2000字",maxlength:"2000","show-word-limit":""
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(p,{
            label:"平台认证"
          }
          ,{
            default:a(()=>[t(Z,{
              modelValue:o.verified,"onUpdate:modelValue":e[7]||(e[7]=l=>o.verified=l),"active-value":1,"inactive-value":0
            }
            ,null,8,["modelValue"]),e[17]||(e[17]=x("span",{
              class:"form-hint"
            }
            ,"开启后门户该联盟名称显示蓝色认证对勾",-1))]),_:1
          }
          ),t(p,{
            label:"排序"
          }
          ,{
            default:a(()=>[t(ee,{
              modelValue:o.sort,"onUpdate:modelValue":e[8]||(e[8]=l=>o.sort=l),min:0
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
),et=Me(Fe,[["__scopeId","data-v-ef171b03"]]);

export default et;
