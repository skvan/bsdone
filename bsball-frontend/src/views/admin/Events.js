// Events —— 行为保真移植自编译产物 Events-gQbZiz40（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { nextTick as ye, withModifiers as ke, createElementBlock as R, defineComponent as Ve, createTextVNode as i, computed as Se, toDisplayString as _, createElementVNode as N, unref as d, mergeProps as De, createBlock as g, ref as c, createVNode as t, withDirectives as Ee, openBlock as p, withCtx as a, Fragment as Ie, withKeys as Te, onMounted as he, renderList as Oe, KeepAlive as $e, reactive as I, createCommentVNode as y } from 'vue';
import { ElDialog as we, ElDatePicker as Ce, ElTableColumn as xe, ElInput as Le, ElFormItem as Ae, ElMessage as V, ElTable as Me, ElOption as Be, ElButton as Ue, ElCard as ze, ElSelect as Pe, ElMessageBox as Fe, ElTag as Ye, ElForm as Ne } from 'element-plus';
import { useRouter as qe } from 'vue-router';
import { useMediaQuery as Re } from '../../composables/useMediaQuery';
import { formatDateTime as q } from '../../utils/formatDate';
import { formatDateYmd as G } from '../../utils/dateExtras';
import { useSettingsStore as Ge } from '../../stores/settings';
import { exportSfc as He } from '../../utils/exportSfc';
import { leagueApi as Ke } from '../../api/business';
import { eventApi as E } from '../../api/business';
import Qe from '../../components/admin/AdminListScaffold.js';
import je from '../../components/admin/AdminPagination.js';
import { useFixedOperationColumn as Je } from '../../composables/useListTable';
import { useListTableAttrs as We } from '../../composables/useListTable';
import { useModalClose as Xe } from '../../composables/useModalClose';
import { useSuperAdminTenantColumn as Ze } from '../../composables/useSuperAdminTenantColumn';
import { useSubmitLock as H } from '../../composables/useSubmitLock';
import { usePermission as et } from '../../composables/usePermission';
import '../../styles/legacy/events.css';
var tt={
  class:"admin-page"
}
,at={
  class:"pagination-wrap"
}
,lt=Ve({
  __name:"Events",setup(nt){
    const{
      submitting:T,withSubmitLock:K
    }
    =H(),{
      submitting:Q,withSubmitLock:j
    }
    =H(),{
      isSuperAdmin:J,tenantLabel:W
    }
    =Ze(),{
      hasPerm:k
    }
    =et(),X=We(),Z=Je(),ee=Xe(),te=Ge(),ae=Re("(max-width: 768px)"),le=Se(()=>te.fillPageHeight&&!ae.value),S=c(),ne={
      name:[{
        required:!0,message:"请输入赛事名称",trigger:"blur"
      }
      ],description:[{
        max:2e3,message:"描述不能超过2000字",trigger:"blur"
      }
      ]
    }
    ,U=qe(),z=c([]),P=c([]),L=c(!1),w=c(!1),D=c(null),o=I({
      name:"",leagueId:null,season:"",startDate:"",endDate:"",status:"draft",gameMode:"BASEBALL",description:""
    }
    ),C=I({
      keyword:""
    }
    ),u=I({
      page:1,pageSize:10,total:0
    }
    ),h=c("id"),F=c("desc");
    function oe({
      prop:n,order:e
    }
    ){
      h.value=n||"id",F.value=e==="descending"?"desc":e==="ascending"?"asc":"desc",u.page=1,v()
    }
    function se(n){
      return{
        draft:"未开始",ongoing:"进行中",ended:"已结束"
      }
      [n||"draft"]||n
    }
    function de(n){
      return{
        draft:"info",ongoing:"success",ended:""
      }
      [n||"draft"]||"info"
    }
    function O(n){
      D.value=n?.id??null,o.name=n?.name??"",o.leagueId=n?.leagueId??null,o.season=n?.season??"",o.startDate=n?.startDate??"",o.endDate=n?.endDate??"",o.status=n?.status??"draft",o.gameMode=n?.gameMode??"BASEBALL",o.description=n?.description??"",w.value=!0,ye(()=>S.value?.clearValidate())
    }
    async function ie(){
      await K(async()=>{
        if(S.value)try{
          await S.value.validate(),D.value?(await E.update(D.value,o),V.success("修改成功")):(await E.create(o),V.success("新增成功")),w.value=!1,v()
        }
        catch(n){
          V.error(n?.message||"保存失败")
        }
        
      }
      )
    }
    function ue(n){
      U.push({
        name:"AdminGames",params:{
          eventId:n.id
        }
        
      }
      )
    }
    function re(n){
      U.push({
        name:"AdminEventBracket",params:{
          eventId:n.id
        }
        
      }
      )
    }
    async function me(n){
      await j(async()=>{
        try{
          await Fe.confirm(`确定要删除赛事「${n.name}」吗？此操作不可恢复。`,"删除确认",{
            confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"
          }
          ),await E.delete(n.id),V.success("删除成功"),v()
        }
        catch(e){
          e!=="cancel"&&V.error(e?.message||"删除失败")
        }
        
      }
      )
    }
    function A(){
      u.page=1,v()
    }
    function pe(){
      C.keyword="",u.page=1,v()
    }
    async function v(){
      L.value=!0;
      const{
        list:n,total:e
      }
      =await E.list({
        page:u.page,pageSize:u.pageSize,keyword:C.keyword||void 0,sortProp:h.value,sortOrder:F.value
      }
      );
      z.value=n,u.total=e,L.value=!1
    }
    async function fe(){
      const{
        list:n
      }
      =await Ke.list({
        pageSize:100
      }
      );
      P.value=n.map(e=>({
        id:e.id,name:e.name
      }
      ))
    }
    return he(()=>{
      fe(),v()
    }
    ),(n,e)=>{
      const f=Ue,x=Le,r=Ae,Y=Ne,m=xe,ge=Ye,ce=Me,ve=ze,b=Be,M=Pe,$=Ce,be=we,_e=$e;
      return p(),R("div",tt,[t(ve,null,{
        header:a(()=>[e[15]||(e[15]=N("span",null,"赛事管理",-1)),d(k)("business:event:create")?(p(),g(f,{
          key:0,type:"primary",style:{
            float:"right"
          }
          ,onClick:e[0]||(e[0]=l=>O())
        }
        ,{
          default:a(()=>[...e[14]||(e[14]=[i("发布赛事",-1)])]),_:1
        }
        )):y("",!0)]),default:a(()=>[t(Qe,{
          class:"event-list-scaffold","fill-mode":le.value
        }
        ,{
          filters:a(()=>[t(Y,{
            inline:!0,class:"query-form",onSubmit:ke(A,["prevent"])
          }
          ,{
            default:a(()=>[t(r,{
              label:"关键词"
            }
            ,{
              default:a(()=>[t(x,{
                modelValue:C.keyword,"onUpdate:modelValue":e[1]||(e[1]=l=>C.keyword=l),placeholder:"赛事名/赛季",clearable:"",style:{
                  width:"180px"
                }
                ,onKeyup:Te(A,["enter"])
              }
              ,null,8,["modelValue"])]),_:1
            }
            ),t(r,null,{
              default:a(()=>[t(f,{
                type:"primary",onClick:A
              }
              ,{
                default:a(()=>[...e[16]||(e[16]=[i("查询",-1)])]),_:1
              }
              ),t(f,{
                onClick:pe
              }
              ,{
                default:a(()=>[...e[17]||(e[17]=[i("重置",-1)])]),_:1
              }
              )]),_:1
            }
            )]),_:1
          }
          )]),default:a(({
            tableMaxHeight:l
          }
          )=>[Ee((p(),g(ce,De(d(X),{
            "max-height":l,data:z.value,"default-sort":{
              prop:"id",order:"descending"
            }
            ,onSortChange:oe
          }
          ),{
            default:a(()=>[t(m,{
              prop:"id",label:"ID",width:"80",sortable:""
            }
            ),d(J)?(p(),g(m,{
              key:0,label:"所属租户",width:"120","show-overflow-tooltip":""
            }
            ,{
              default:a(({
                row:s
              }
              )=>[i(_(d(W)(s.tenantId)),1)]),_:1
            }
            )):y("",!0),t(m,{
              prop:"name",label:"赛事名称","min-width":"130"
            }
            ),t(m,{
              prop:"season",label:"赛季",width:"100"
            }
            ),t(m,{
              prop:"startDate",label:"开始日期",width:"120"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[i(_(d(G)(s.startDate)),1)]),_:1
            }
            ),t(m,{
              prop:"endDate",label:"结束日期",width:"120"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[i(_(d(G)(s.endDate)),1)]),_:1
            }
            ),t(m,{
              prop:"status",label:"状态",width:"90"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[t(ge,{
                type:de(s.status),size:"small"
              }
              ,{
                default:a(()=>[i(_(se(s.status)),1)]),_:2
              }
              ,1032,["type"])]),_:1
            }
            ),t(m,{
              prop:"createdAt",label:"创建时间",width:"160",sortable:"custom","sort-orders":["ascending","descending"]
            }
            ,{
              default:a(({
                row:s
              }
              )=>[i(_(d(q)(s.createdAt)),1)]),_:1
            }
            ),t(m,{
              prop:"createdAt",label:"修改时间",width:"160",sortable:"custom","sort-orders":["ascending","descending"]
            }
            ,{
              default:a(({
                row:s
              }
              )=>[i(_(d(q)(s.updatedAt)),1)]),_:1
            }
            ),t(m,{
              label:"操作","min-width":"100",fixed:d(Z),"class-name":"col-operation"
            }
            ,{
              default:a(({
                row:s
              }
              )=>[d(k)("business:event:games")?(p(),g(f,{
                key:0,link:"",type:"primary",onClick:B=>ue(s)
              }
              ,{
                default:a(()=>[...e[18]||(e[18]=[i("赛程/结果",-1)])]),_:1
              }
              ,8,["onClick"])):y("",!0),d(k)("business:event:bracket")?(p(),g(f,{
                key:1,link:"",type:"primary",onClick:B=>re(s)
              }
              ,{
                default:a(()=>[...e[19]||(e[19]=[i("对战详情",-1)])]),_:1
              }
              ,8,["onClick"])):y("",!0),d(k)("business:event:edit")?(p(),g(f,{
                key:2,link:"",type:"primary",onClick:B=>O(s)
              }
              ,{
                default:a(()=>[...e[20]||(e[20]=[i("编辑",-1)])]),_:1
              }
              ,8,["onClick"])):y("",!0),d(k)("business:event:delete")?(p(),g(f,{
                key:3,link:"",type:"danger",disabled:d(Q),onClick:B=>me(s)
              }
              ,{
                default:a(()=>[...e[21]||(e[21]=[i("删除",-1)])]),_:1
              }
              ,8,["disabled","onClick"])):y("",!0)]),_:1
            }
            ,8,["fixed"])]),_:1
          }
          ,16,["max-height","data"])),[[_e,L.value]])]),pagination:a(()=>[N("div",at,[t(je,{
            "current-page":u.page,"onUpdate:currentPage":e[2]||(e[2]=l=>u.page=l),"page-size":u.pageSize,"onUpdate:pageSize":e[3]||(e[3]=l=>u.pageSize=l),total:u.total,onChange:v
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      ),t(be,{
        modelValue:w.value,"onUpdate:modelValue":e[13]||(e[13]=l=>w.value=l),title:D.value?"编辑赛事":"发布赛事",width:"520","close-on-click-modal":d(ee)
      }
      ,{
        footer:a(()=>[t(f,{
          onClick:e[12]||(e[12]=l=>w.value=!1)
        }
        ,{
          default:a(()=>[...e[22]||(e[22]=[i("取消",-1)])]),_:1
        }
        ),t(f,{
          type:"primary",loading:d(T),disabled:d(T),onClick:ie
        }
        ,{
          default:a(()=>[...e[23]||(e[23]=[i("确定",-1)])]),_:1
        }
        ,8,["loading","disabled"])]),default:a(()=>[t(Y,{
          ref_key:"formRef",ref:S,model:o,rules:ne,"label-width":"90px"
        }
        ,{
          default:a(()=>[t(r,{
            label:"赛事名称",prop:"name"
          }
          ,{
            default:a(()=>[t(x,{
              modelValue:o.name,"onUpdate:modelValue":e[4]||(e[4]=l=>o.name=l),placeholder:"赛事名称"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"联盟"
          }
          ,{
            default:a(()=>[t(M,{
              modelValue:o.leagueId,"onUpdate:modelValue":e[5]||(e[5]=l=>o.leagueId=l),placeholder:"请选择联盟",clearable:"",style:{
                width:"100%"
              }
              
            }
            ,{
              default:a(()=>[(p(!0),R(Ie,null,Oe(P.value,l=>(p(),g(b,{
                key:l.id,label:l.name,value:l.id
              }
              ,null,8,["label","value"]))),128))]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"赛季"
          }
          ,{
            default:a(()=>[t(x,{
              modelValue:o.season,"onUpdate:modelValue":e[6]||(e[6]=l=>o.season=l),placeholder:"如 2025春"
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"开始日期"
          }
          ,{
            default:a(()=>[t($,{
              modelValue:o.startDate,"onUpdate:modelValue":e[7]||(e[7]=l=>o.startDate=l),type:"date","value-format":"YYYY-MM-DD",style:{
                width:"100%"
              }
              
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"结束日期"
          }
          ,{
            default:a(()=>[t($,{
              modelValue:o.endDate,"onUpdate:modelValue":e[8]||(e[8]=l=>o.endDate=l),type:"date","value-format":"YYYY-MM-DD",style:{
                width:"100%"
              }
              
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"状态"
          }
          ,{
            default:a(()=>[t(M,{
              modelValue:o.status,"onUpdate:modelValue":e[9]||(e[9]=l=>o.status=l),placeholder:"请选择",style:{
                width:"100%"
              }
              
            }
            ,{
              default:a(()=>[t(b,{
                label:"未开始",value:"draft"
              }
              ),t(b,{
                label:"进行中",value:"ongoing"
              }
              ),t(b,{
                label:"已结束",value:"ended"
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"比赛模式"
          }
          ,{
            default:a(()=>[t(M,{
              modelValue:o.gameMode,"onUpdate:modelValue":e[10]||(e[10]=l=>o.gameMode=l),placeholder:"选择比赛模式",style:{
                width:"100%"
              }
              
            }
            ,{
              default:a(()=>[t(b,{
                label:"棒球",value:"BASEBALL"
              }
              ),t(b,{
                label:"垒球",value:"SOFTBALL"
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),t(r,{
            label:"描述"
          }
          ,{
            default:a(()=>[t(x,{
              modelValue:o.description,"onUpdate:modelValue":e[11]||(e[11]=l=>o.description=l),type:"textarea",rows:3,placeholder:"最多2000字",maxlength:"2000","show-word-limit":""
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
),kt=He(lt,[["__scopeId","data-v-fe58ff6b"]]);

export default kt;
