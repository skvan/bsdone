// Dict —— 行为保真移植自编译产物 Dict-CkH3mfLH（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { ElRadioGroup as Ee, ElInputNumber as Ie, ElDialog as Pe, ElRadio as Me, ElTableColumn as Fe, ElInput as Oe, ElFormItem as Ae, ElMessage as b, ElTable as Ne, ElButton as Re, ElCard as Qe, ElMessageBox as ge, ElForm as He } from 'element-plus';
import { withModifiers as re, createElementBlock as R, defineComponent as $e, createTextVNode as n, computed as Be, toDisplayString as I, createElementVNode as P, unref as g, mergeProps as pe, createBlock as Q, ref as f, createVNode as a, withDirectives as me, openBlock as _, withCtx as t, withKeys as fe, onMounted as Ke, KeepAlive as qe, reactive as V, createCommentVNode as Ze } from 'vue';
import { useMediaQuery as Ge } from '../../composables/useMediaQuery';
import { useSettingsStore as je } from '../../stores/settings';
import { exportSfc as Je } from '../../utils/exportSfc';
import { dictTypeApi as $ } from '../../api/system';
import { dictDataApi as B } from '../../api/system';
import ve from '../../components/admin/AdminListScaffold.js';
import ye from '../../components/admin/AdminPagination.js';
import { useFixedOperationColumn as We } from '../../composables/useListTable';
import { useListTableAttrs as Xe } from '../../composables/useListTable';
import { useModalClose as Ye } from '../../composables/useModalClose';
import { useSubmitLock as K } from '../../composables/useSubmitLock';
import '../../styles/legacy/dict.css';
var he={
  class:"admin-page admin-page-dict"
}
,ea={
  class:"pagination-wrap"
}
,aa={
  key:0,class:"selected-type"
}
,ta={
  class:"pagination-wrap"
}
,la={
  key:1,class:"hint"
}
,oa=$e({
  __name:"Dict",setup(na){
    const{
      submitting:q,withSubmitLock:be
    }
    =K(),{
      submitting:G,withSubmitLock:ce
    }
    =K(),{
      submitting:H,withSubmitLock:Z
    }
    =K(),j=Xe(),J=We(),W=Ye(),ke=je(),we=Ge("(max-width: 768px)"),X=Be(()=>ke.fillPageHeight&&!we.value),Y=f([]),M=f(!1),S=f(!1),x=f(null),s=V({
      name:"",type:"",status:1,remark:""
    }
    ),D=V({
      keyword:""
    }
    ),v=V({
      page:1,pageSize:10,total:0
    }
    ),h=f("id"),ee=f("asc"),r=f(null),F=f([]),O=f(!1),C=f(!1),T=f(null),i=V({
      dictTypeId:0,label:"",value:"",sort:0,status:1,remark:""
    }
    ),L=V({
      keyword:""
    }
    ),p=V({
      page:1,pageSize:10,total:0
    }
    ),ae=f("id"),te=f("asc");
    function _e({
      prop:o,order:e
    }
    ){
      h.value=o||"id",ee.value=e==="descending"?"desc":"asc",v.page=1,w()
    }
    function Ve({
      prop:o,order:e
    }
    ){
      ae.value=o||"id",te.value=e==="descending"?"desc":"asc",p.page=1,k()
    }
    function le(o){
      x.value=o?.id??null,s.name=o?.name??"",s.type=o?.type??"",s.status=o?.status??1,s.remark=o?.remark??"",S.value=!0
    }
    function Se(o){
      r.value=o,p.page=1,k()
    }
    async function Ce(){
      await be(async()=>{
        try{
          x.value?(await $.update(x.value,s),b.success("修改成功")):(await $.create(s),b.success("新增成功")),S.value=!1,w()
        }
        catch(o){
          b.error(o?.message||"保存失败")
        }
        
      }
      )
    }
    async function xe(o){
      await Z(async()=>{
        try{
          await ge.confirm(`确定要删除字典类型「${o.name}」吗？此操作不可恢复。`,"删除确认",{
            confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"
          }
          ),await $.delete(o.id),r.value?.id===o.id&&(r.value=null),w(),k(),b.success("删除成功")
        }
        catch(e){
          e!=="cancel"&&b.error(e?.message||"删除失败")
        }
        
      }
      )
    }
    function A(){
      v.page=1,w()
    }
    function De(){
      D.keyword="",v.page=1,w()
    }
    async function w(){
      M.value=!0;
      const{
        list:o,total:e
      }
      =await $.list({
        page:v.page,pageSize:v.pageSize,keyword:D.keyword||void 0,sortProp:h.value,sortOrder:ee.value
      }
      );
      Y.value=o,v.total=e,M.value=!1
    }
    function oe(o){
      r.value&&(T.value=o?.id??null,i.dictTypeId=r.value.id,i.label=o?.label??"",i.value=o?.value??"",i.sort=o?.sort??0,i.status=o?.status??1,i.remark=o?.remark??"",C.value=!0)
    }
    async function Te(){
      await ce(async()=>{
        try{
          T.value?(await B.update(T.value,i),b.success("修改成功")):(await B.create(i),b.success("新增成功")),C.value=!1,k()
        }
        catch(o){
          b.error(o?.message||"保存失败")
        }
        
      }
      )
    }
    async function Le(o){
      await Z(async()=>{
        try{
          await ge.confirm(`确定要删除字典数据「${o.label}」吗？此操作不可恢复。`,"删除确认",{
            confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"
          }
          ),await B.delete(o.id),b.success("删除成功"),k()
        }
        catch(e){
          e!=="cancel"&&b.error(e?.message||"删除失败")
        }
        
      }
      )
    }
    function N(){
      p.page=1,k()
    }
    function Ue(){
      L.keyword="",p.page=1,k()
    }
    async function k(){
      if(!r.value){
        F.value=[],p.total=0;
        return
      }
      O.value=!0;
      const{
        list:o,total:e
      }
      =await B.list({
        dictTypeId:r.value.id,page:p.page,pageSize:p.pageSize,keyword:L.keyword||void 0,sortProp:ae.value,sortOrder:te.value
      }
      );
      F.value=o,p.total=e,O.value=!1
    }
    return Ke(()=>w()),(o,e)=>{
      const d=Re,c=Oe,u=Ae,U=He,m=Fe,ne=Ne,ie=Qe,z=Me,se=Ee,de=Pe,ze=Ie,ue=qe;
      return _(),R("div",he,[a(ie,{
        class:"section-card"
      }
      ,{
        header:t(()=>[e[22]||(e[22]=P("span",null,"字典类型",-1)),a(d,{
          type:"primary",style:{
            float:"right"
          }
          ,onClick:e[0]||(e[0]=l=>le())
        }
        ,{
          default:t(()=>[...e[21]||(e[21]=[n("新增类型",-1)])]),_:1
        }
        )]),default:t(()=>[a(ve,{
          class:"dict-type-list-scaffold","fill-mode":X.value
        }
        ,{
          filters:t(()=>[a(U,{
            inline:!0,class:"query-form",onSubmit:re(A,["prevent"])
          }
          ,{
            default:t(()=>[a(u,{
              label:"关键词"
            }
            ,{
              default:t(()=>[a(c,{
                modelValue:D.keyword,"onUpdate:modelValue":e[1]||(e[1]=l=>D.keyword=l),placeholder:"名称/类型编码",clearable:"",style:{
                  width:"160px"
                }
                ,onKeyup:fe(A,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),a(u,null,{
              default:t(()=>[a(d,{
                type:"primary",onClick:A
              }
              ,{
                default:t(()=>[...e[23]||(e[23]=[n("查询",-1)])]),_:1
              }
              ),a(d,{
                onClick:De
              }
              ,{
                default:t(()=>[...e[24]||(e[24]=[n("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:t(({
            tableMaxHeight:l
          }
          )=>[me((_(),Q(ne,pe(g(j),{
            "max-height":l,data:Y.value,"default-sort":{
              prop:"id",order:"ascending"
            }
            ,onSortChange:_e
          }
          ),{
            default:t(()=>[a(m,{
              prop:"id",label:"ID",width:"80",sortable:""
            }
            ),a(m,{
              prop:"name",label:"名称"
            }
            ),a(m,{
              prop:"type",label:"类型编码",width:"160"
            }
            ),a(m,{
              prop:"status",label:"状态",width:"80"
            }
            ,{
              default:t(({
                row:y
              }
              )=>[n(I(y.status===1?"启用":"禁用"),1)]),_:1
            }
            ),a(m,{
              prop:"remark",label:"备注","show-overflow-tooltip":""
            }
            ),a(m,{
              label:"操作","min-width":"100",fixed:g(J),"class-name":"col-operation"
            }
            ,{
              default:t(({
                row:y
              }
              )=>[a(d,{
                link:"",type:"primary",onClick:E=>le(y)
              }
              ,{
                default:t(()=>[...e[25]||(e[25]=[n("编辑",-1)])]),_:1
              }
              ,8,["onClick"]),a(d,{
                link:"",type:"primary",onClick:E=>Se(y)
              }
              ,{
                default:t(()=>[...e[26]||(e[26]=[n("管理数据",-1)])]),_:1
              }
              ,8,["onClick"]),a(d,{
                link:"",type:"danger",disabled:g(H),onClick:E=>xe(y)
              }
              ,{
                default:t(()=>[...e[27]||(e[27]=[n("删除",-1)])]),_:1
              }
              ,8,["disabled","onClick"])]),_:1
            }
            ,8,["fixed"])]),_:1
          }
          ,16,["max-height","data"])),[[ue,M.value]])]),pagination:t(()=>[P("div",ea,[a(ye,{
            "current-page":v.page,"onUpdate:currentPage":e[2]||(e[2]=l=>v.page=l),"page-size":v.pageSize,"onUpdate:pageSize":e[3]||(e[3]=l=>v.pageSize=l),total:v.total,onChange:w
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      ),a(ie,{
        class:"section-card"
      }
      ,{
        header:t(()=>[e[29]||(e[29]=P("span",null,"字典数据",-1)),r.value?(_(),R("span",aa,"当前类型："+I(r.value.name)+"（"+I(r.value.type)+"）",1)):Ze("",!0),a(d,{
          type:"primary",style:{
            float:"right"
          }
          ,disabled:!r.value,onClick:e[4]||(e[4]=l=>oe())
        }
        ,{
          default:t(()=>[...e[28]||(e[28]=[n("新增数据",-1)])]),_:1
        }
        ,8,["disabled"])]),default:t(()=>[r.value?(_(),Q(ve,{
          key:0,class:"dict-data-list-scaffold","fill-mode":X.value
        }
        ,{
          filters:t(()=>[a(U,{
            inline:!0,class:"query-form",onSubmit:re(N,["prevent"])
          }
          ,{
            default:t(()=>[a(u,{
              label:"关键词"
            }
            ,{
              default:t(()=>[a(c,{
                modelValue:L.keyword,"onUpdate:modelValue":e[5]||(e[5]=l=>L.keyword=l),placeholder:"标签/值",clearable:"",style:{
                  width:"140px"
                }
                ,onKeyup:fe(N,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),a(u,null,{
              default:t(()=>[a(d,{
                type:"primary",onClick:N
              }
              ,{
                default:t(()=>[...e[30]||(e[30]=[n("查询",-1)])]),_:1
              }
              ),a(d,{
                onClick:Ue
              }
              ,{
                default:t(()=>[...e[31]||(e[31]=[n("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:t(({
            tableMaxHeight:l
          }
          )=>[me((_(),Q(ne,pe(g(j),{
            "max-height":l,data:F.value,"default-sort":{
              prop:"id",order:"ascending"
            }
            ,onSortChange:Ve
          }
          ),{
            default:t(()=>[a(m,{
              prop:"id",label:"ID",width:"80",sortable:""
            }
            ),a(m,{
              prop:"label",label:"标签"
            }
            ),a(m,{
              prop:"value",label:"值",width:"120"
            }
            ),a(m,{
              prop:"sort",label:"排序",width:"80"
            }
            ),a(m,{
              prop:"status",label:"状态",width:"80"
            }
            ,{
              default:t(({
                row:y
              }
              )=>[n(I(y.status===1?"启用":"禁用"),1)]),_:1
            }
            ),a(m,{
              prop:"remark",label:"备注","show-overflow-tooltip":""
            }
            ),a(m,{
              label:"操作",width:"160",fixed:g(J),"class-name":"col-operation"
            }
            ,{
              default:t(({
                row:y
              }
              )=>[a(d,{
                link:"",type:"primary",onClick:E=>oe(y)
              }
              ,{
                default:t(()=>[...e[32]||(e[32]=[n("编辑",-1)])]),_:1
              }
              ,8,["onClick"]),a(d,{
                link:"",type:"danger",disabled:g(H),onClick:E=>Le(y)
              }
              ,{
                default:t(()=>[...e[33]||(e[33]=[n("删除",-1)])]),_:1
              }
              ,8,["disabled","onClick"])]),_:1
            }
            ,8,["fixed"])]),_:1
          }
          ,16,["max-height","data"])),[[ue,O.value]])]),pagination:t(()=>[P("div",ta,[a(ye,{
            "current-page":p.page,"onUpdate:currentPage":e[6]||(e[6]=l=>p.page=l),"page-size":p.pageSize,"onUpdate:pageSize":e[7]||(e[7]=l=>p.pageSize=l),total:p.total,onChange:k
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])):(_(),R("p",la,"请在上方「字典类型」中点击「管理数据」选择要维护的字典类型。"))]),_:1
      }
      ),a(de,{
        modelValue:S.value,"onUpdate:modelValue":e[13]||(e[13]=l=>S.value=l),title:x.value?"编辑字典类型":"新增字典类型",width:"500","close-on-click-modal":g(W)
      }
      ,{
        footer:t(()=>[a(d,{
          onClick:e[12]||(e[12]=l=>S.value=!1)
        }
        ,{
          default:t(()=>[...e[36]||(e[36]=[n("取消",-1)])]),_:1
        }
        ),a(d,{
          type:"primary",loading:g(q),disabled:g(q),onClick:Ce
        }
        ,{
          default:t(()=>[...e[37]||(e[37]=[n("确定",-1)])]),_:1
        }
        ,8,["loading","disabled"])]),default:t(()=>[a(U,{
          model:s,"label-width":"90px"
        }
        ,{
          default:t(()=>[a(u,{
            label:"名称"
          }
          ,{
            default:t(()=>[a(c,{
              modelValue:s.name,"onUpdate:modelValue":e[8]||(e[8]=l=>s.name=l)
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"类型编码"
          }
          ,{
            default:t(()=>[a(c,{
              modelValue:s.type,"onUpdate:modelValue":e[9]||(e[9]=l=>s.type=l),placeholder:"如 sys_user_sex"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"状态"
          }
          ,{
            default:t(()=>[a(se,{
              modelValue:s.status,"onUpdate:modelValue":e[10]||(e[10]=l=>s.status=l)
            }
            ,{
              default:t(()=>[a(z,{
                value:1
              }
              ,{
                default:t(()=>[...e[34]||(e[34]=[n("启用",-1)])]),_:1
              }
              ),a(z,{
                value:0
              }
              ,{
                default:t(()=>[...e[35]||(e[35]=[n("禁用",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"备注"
          }
          ,{
            default:t(()=>[a(c,{
              modelValue:s.remark,"onUpdate:modelValue":e[11]||(e[11]=l=>s.remark=l),type:"textarea",rows:3,maxlength:"2000","show-word-limit":"",placeholder:"最多2000字"
            }
            ,null,8,["modelValue"])]),_:1
          }
          )]),_:1
        }
        ,8,["model"])]),_:1
      }
      ,8,["modelValue","title","close-on-click-modal"]),a(de,{
        modelValue:C.value,"onUpdate:modelValue":e[20]||(e[20]=l=>C.value=l),title:T.value?"编辑字典数据":"新增字典数据",width:"500","close-on-click-modal":g(W)
      }
      ,{
        footer:t(()=>[a(d,{
          onClick:e[19]||(e[19]=l=>C.value=!1)
        }
        ,{
          default:t(()=>[...e[40]||(e[40]=[n("取消",-1)])]),_:1
        }
        ),a(d,{
          type:"primary",loading:g(G),disabled:g(G),onClick:Te
        }
        ,{
          default:t(()=>[...e[41]||(e[41]=[n("确定",-1)])]),_:1
        }
        ,8,["loading","disabled"])]),default:t(()=>[a(U,{
          model:i,"label-width":"90px"
        }
        ,{
          default:t(()=>[a(u,{
            label:"所属类型"
          }
          ,{
            default:t(()=>[a(c,{
              "model-value":r.value?.name,disabled:""
            }
            ,null,8,["model-value"])]),_:1
          }
          ),a(u,{
            label:"标签"
          }
          ,{
            default:t(()=>[a(c,{
              modelValue:i.label,"onUpdate:modelValue":e[14]||(e[14]=l=>i.label=l)
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"值"
          }
          ,{
            default:t(()=>[a(c,{
              modelValue:i.value,"onUpdate:modelValue":e[15]||(e[15]=l=>i.value=l)
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"排序"
          }
          ,{
            default:t(()=>[a(ze,{
              modelValue:i.sort,"onUpdate:modelValue":e[16]||(e[16]=l=>i.sort=l),min:0
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"状态"
          }
          ,{
            default:t(()=>[a(se,{
              modelValue:i.status,"onUpdate:modelValue":e[17]||(e[17]=l=>i.status=l)
            }
            ,{
              default:t(()=>[a(z,{
                value:1
              }
              ,{
                default:t(()=>[...e[38]||(e[38]=[n("启用",-1)])]),_:1
              }
              ),a(z,{
                value:0
              }
              ,{
                default:t(()=>[...e[39]||(e[39]=[n("禁用",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),a(u,{
            label:"备注"
          }
          ,{
            default:t(()=>[a(c,{
              modelValue:i.remark,"onUpdate:modelValue":e[18]||(e[18]=l=>i.remark=l),type:"textarea",rows:3,maxlength:"2000","show-word-limit":"",placeholder:"最多2000字"
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
),ca=Je(oa,[["__scopeId","data-v-78a67b05"]]);

export default ca;
