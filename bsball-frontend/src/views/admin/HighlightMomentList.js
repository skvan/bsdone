// HighlightMomentList —— 行为保真移植自编译产物 HighlightMomentList-B0xM9i0G（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { ElRadioGroup as ge, ElDialog as Ve, ElDatePicker as Se, ElRadio as he, ElTableColumn as we, ElInput as xe, ElFormItem as Ue, ElMessage as y, ElUpload as Ce, ElTable as Ee, ElOption as Ie, ElButton as Me, ElCard as Le, ElSelect as Ae, ElMessageBox as Pe, vLoading as Fe, ElForm as De } from 'element-plus';
import { withModifiers as _e, createElementBlock as L, defineComponent as je, createTextVNode as r, computed as w, toDisplayString as A, createElementVNode as O, unref as v, mergeProps as Te, createBlock as T, ref as f, createVNode as l, withDirectives as ke, openBlock as b, withCtx as a, Fragment as H, onMounted as Oe, renderList as K, reactive as P, createCommentVNode as Lt } from 'vue';
import { useMediaQuery as $e } from '../../composables/useMediaQuery';
import { useSettingsStore as ze } from '../../stores/settings';
import { exportSfc as Be } from '../../utils/exportSfc';
import { uploadResource as Ge } from '../../api/system';
import { fetchAllLeagues as He } from '../../api/business';
import { highlightMomentApi as k } from '../../api/business';
import { playerApi as Ke } from '../../api/business';
import { coachApi as qe } from '../../api/business';
import { teamApi as Ne } from '../../api/business';
import Qe from '../../components/admin/AdminListScaffold.js';
import Re from '../../components/admin/AdminPagination.js';
import { useFixedOperationColumn as We } from '../../composables/useListTable';
import { useListTableAttrs as Ye } from '../../composables/useListTable';
import { useModalClose as Je } from '../../composables/useModalClose';
import { useSubmitLock as q } from '../../composables/useSubmitLock';
import { usePermission as Wa } from '../../composables/usePermission';
import '../../styles/legacy/highlight-moments.css';
var Ze={
  class:"admin-page"
}
,Xe={
  class:"pagination-wrap"
}
,el={
  class:"upload-url"
}
,ll=je({
  __name:"HighlightMomentList",setup(al){
    const{
      submitting:F,withSubmitLock:N
    }
    =q(),{
      submitting:Q,withSubmitLock:R
    }
    =q(),{
      hasPerm:Ha
    }
    =Wa(),W=ze(),Y=$e("(max-width: 768px)"),J=w(()=>W.fillPageHeight&&!Y.value),Z=Ye(),X=We(),x=f(!1),$=f([]),U=f([]),C=f([]),E=f([]),I=f([]),s=P({
      subjectType:"player",subjectId:void 0,mediaType:void 0
    }
    ),p=P({
      page:1,pageSize:20,total:0
    }
    ),g=f(!1),_=f(null),ee=Je(),t=P({
      subjectType:"player",subjectId:void 0,displayKey:"player_profile",title:"",description:"",happenedAt:"",mediaType:"image",mediaSource:"upload",mediaUrl:"",status:"published"
    }
    ),le=w(()=>D(s.subjectType)),ae=w(()=>D(t.subjectType||"player")),te=w(()=>t.mediaType==="video"?"video/*":"image/*");
    function D(i){
      return i==="league"?I.value.map(e=>({
        id:e.id,label:e.name
      }
      )):i==="team"?C.value.map(e=>({
        id:e.id,label:e.name
      }
      )):i==="coach"?E.value.map(e=>({
        id:e.id,label:e.name
      }
      )):U.value.map(e=>({
        id:e.id,label:`${e.name}${e.number?" #"+e.number:""}`
      }
      ))
    }
    function oe(i,e){
      return{
        player:Object.fromEntries(U.value.map(n=>[n.id,`${n.name}${n.number?" #"+n.number:""}`])),coach:Object.fromEntries(E.value.map(n=>[n.id,n.name])),team:Object.fromEntries(C.value.map(n=>[n.id,n.name])),league:Object.fromEntries(I.value.map(n=>[n.id,n.name]))
      }
      [i]?.[e]??`#${e}`
    }
    async function V(){
      x.value=!0;
      const i=await k.list({
        subjectType:s.subjectType,subjectId:s.subjectId,mediaType:s.mediaType,page:p.page,pageSize:p.pageSize
      }
      );
      $.value=i.list??[],p.total=i.total??0,x.value=!1
    }
    function S(){
      p.page=1,V()
    }
    function ie(){
      s.subjectId=void 0,S()
    }
    function z(i){
      _.value=i?.id??null,t.subjectType=i?.subjectType??"player",t.subjectId=i?.subjectId,t.displayKey=i?.displayKey??"player_profile",t.title=i?.title??"",t.description=i?.description??"",t.happenedAt=i?.happenedAt??"",t.mediaType=i?.mediaType??"image",t.mediaSource=i?.mediaSource??"upload",t.mediaUrl=i?.mediaUrl??"",t.status=i?.status??"published",g.value=!0
    }
    function ne(){
      t.subjectId=void 0
    }
    function ue(i){
      return t.mediaType==="image"&&!i.type.startsWith("image/")?(y.error("请上传图片文件（如 JPG、PNG、GIF、WebP）"),!1):t.mediaType==="video"&&!i.type.startsWith("video/")?(y.error("请上传视频文件（如 MP4、WebM、MOV）"),!1):!0
    }
    async function de(i){
      const e=i.file,{
        url:n
      }
      =await Ge(e);
      t.mediaUrl=n
    }
    async function se(){
      await N(async()=>{
        if(!t.title||!t.subjectId||!t.mediaUrl){
          y.warning("请填写完整信息");
          return
        }
        try{
          const i={
            ...t,historyEventId:null
          };
          _.value?await k.update(_.value,i):await k.create(i),y.success("保存成功"),g.value=!1,_.value=null,V()
        }
        catch(i){
          y.error(i?.message||"保存失败")
        }
        
      }
      )
    }
    async function re(i){
      await R(async()=>{
        try{
          await Pe.confirm(`确认删除高光「${i.title}」吗？`,"删除确认",{
            type:"warning"
          }
          ),await k.delete(i.id),y.success("删除成功"),V()
        }
        catch(e){
          e!=="cancel"&&y.error(e?.message||"删除失败")
        }
        
      }
      )
    }
    async function reReturn(i){
      await R(async()=>{
        try{
          await Pe.confirm(`确认归还高光「${i.title}」吗？归还后将从本租户运营面移除，数据由平台留存。`,"归还确认",{
            type:"warning"
          }
          ),await k.delete(i.id),y.success("已归还"),V()
        }
        catch(e){
          e!=="cancel"&&y.error(e?.message||"归还失败")
        }
        
      }
      )
    }
    return Oe(async()=>{
      const[i,e,n,u]=await Promise.all([Ke.selectOptions(),Ne.selectOptions(),qe.selectOptions(),He()]);
      U.value=i??[],C.value=e??[],E.value=n??[],I.value=u.map(m=>({
        id:m.id,name:m.name
      }
      )),V()
    }
    ),(i,e)=>{
      const n=Me,u=Ie,m=Ae,d=Ue,B=De,c=we,pe=Ee,me=Le,M=xe,fe=Se,h=he,G=ge,be=Ce,ce=Ve,ye=Fe;
      return b(),L("div",Ze,[l(me,null,{
        header:a(()=>[e[17]||(e[17]=O("span",null,"高光时刻",-1)),l(n,{
          type:"primary",style:{
            float:"right"
          }
          ,onClick:z
        }
        ,{
          default:a(()=>[...e[16]||(e[16]=[r("新增高光",-1)])]),_:1
        }
        )]),default:a(()=>[l(Qe,{
          class:"highlight-moment-list-scaffold","fill-mode":J.value
        }
        ,{
          filters:a(()=>[l(B,{
            inline:!0,class:"query-form",onSubmit:_e(S,["prevent"])
          }
          ,{
            default:a(()=>[l(d,{
              label:"对象类型"
            }
            ,{
              default:a(()=>[l(m,{
                modelValue:s.subjectType,"onUpdate:modelValue":e[0]||(e[0]=o=>s.subjectType=o),clearable:"",style:{
                  width:"140px"
                }
                ,onChange:ie
              }
              ,{
                default:a(()=>[l(u,{
                  label:"联盟",value:"league"
                }
                ),l(u,{
                  label:"球队",value:"team"
                }
                ),l(u,{
                  label:"教练",value:"coach"
                }
                ),l(u,{
                  label:"球员",value:"player"
                }
                )]),_:1
              }
              ,8,["modelValue"])]),_:1
            }
            ),l(d,{
              label:"对象"
            }
            ,{
              default:a(()=>[l(m,{
                modelValue:s.subjectId,"onUpdate:modelValue":e[1]||(e[1]=o=>s.subjectId=o),clearable:"",filterable:"",style:{
                  width:"220px"
                }
                ,onChange:S
              }
              ,{
                default:a(()=>[(b(!0),L(H,null,K(le.value,o=>(b(),T(u,{
                  key:o.id,label:o.label,value:o.id
                }
                ,null,8,["label","value"]))),128))]),_:1
              }
              ,8,["modelValue"])]),_:1
            }
            ),l(d,{
              label:"类型"
            }
            ,{
              default:a(()=>[l(m,{
                modelValue:s.mediaType,"onUpdate:modelValue":e[2]||(e[2]=o=>s.mediaType=o),clearable:"",style:{
                  width:"120px"
                }
                ,onChange:S
              }
              ,{
                default:a(()=>[l(u,{
                  label:"图片",value:"image"
                }
                ),l(u,{
                  label:"视频",value:"video"
                }
                )]),_:1
              }
              ,8,["modelValue"])]),_:1
            }
            )]),_:1
          }
          )]),default:a(({
            tableMaxHeight:o
          }
          )=>[ke((b(),T(pe,Te(v(Z),{
            "max-height":o,data:$.value
          }
          ),{
            default:a(()=>[l(c,{
              prop:"id",label:"ID",width:"80"
            }
            ),l(c,{
              prop:"title",label:"标题","min-width":"220"
            }
            ),l(c,{
              label:"对象",width:"180"
            }
            ,{
              default:a(({
                row:j
              }
              )=>[r(A(oe(j.subjectType,j.subjectId)),1)]),_:1
            }
            ),l(c,{
              prop:"mediaType",label:"类型",width:"90"
            }
            ),l(c,{
              prop:"status",label:"状态",width:"110"
            }
            ),l(c,{
              prop:"happenedAt",label:"发生时间",width:"140"
            }
            ),l(c,{
              label:"操作",width:"160",fixed:v(X),"class-name":"col-operation"
            }
            ,{
              default:a(({
                row:j
              }
              )=>[l(n,{
                text:"",type:"primary",onClick:ve=>z(j)
              }
              ,{
                default:a(()=>[...e[18]||(e[18]=[r("编辑",-1)])]),_:1
              }
              ,8,["onClick"]),v(Ha)("business:highlight-moment:delete")?(b(),T(n,{
                key:0,text:"",type:"danger",disabled:v(Q),onClick:ve=>re(j)
              }
              ,{
                default:a(()=>[...e[19]||(e[19]=[r("删除",-1)])]),_:1
              }
              ,8,["disabled","onClick"])):Lt("",!0),v(Ha)("business:highlight-moment:return")?(b(),T(n,{
                key:1,text:"",type:"warning",disabled:v(Q),onClick:ve=>reReturn(j)
              }
              ,{
                default:a(()=>[...e[26]||(e[26]=[r("归还",-1)])]),_:1
              }
              ,8,["disabled","onClick"])):Lt("",!0)]),_:1
            }
            ,8,["fixed"])]),_:1
          }
          ,16,["max-height","data"])),[[ye,x.value]])]),pagination:a(()=>[O("div",Xe,[l(Re,{
            "current-page":p.page,"onUpdate:currentPage":e[3]||(e[3]=o=>p.page=o),"page-size":p.pageSize,"onUpdate:pageSize":e[4]||(e[4]=o=>p.pageSize=o),total:p.total,onChange:V
          }
          ,null,8,["current-page","page-size","total"])])]),_:1
        }
        ,8,["fill-mode"])]),_:1
      }
      ),l(ce,{
        modelValue:g.value,"onUpdate:modelValue":e[15]||(e[15]=o=>g.value=o),title:_.value?"编辑高光":"新增高光",width:"560px","close-on-click-modal":v(ee)
      }
      ,{
        footer:a(()=>[l(n,{
          onClick:e[14]||(e[14]=o=>g.value=!1)
        }
        ,{
          default:a(()=>[...e[24]||(e[24]=[r("取消",-1)])]),_:1
        }
        ),l(n,{
          type:"primary",loading:v(F),disabled:v(F),onClick:se
        }
        ,{
          default:a(()=>[...e[25]||(e[25]=[r("保存",-1)])]),_:1
        }
        ,8,["loading","disabled"])]),default:a(()=>[l(B,{
          model:t,"label-width":"110px"
        }
        ,{
          default:a(()=>[l(d,{
            label:"标题"
          }
          ,{
            default:a(()=>[l(M,{
              modelValue:t.title,"onUpdate:modelValue":e[5]||(e[5]=o=>t.title=o)
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"描述"
          }
          ,{
            default:a(()=>[l(M,{
              modelValue:t.description,"onUpdate:modelValue":e[6]||(e[6]=o=>t.description=o),type:"textarea",rows:3,maxlength:"2000","show-word-limit":""
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"发生时间"
          }
          ,{
            default:a(()=>[l(fe,{
              modelValue:t.happenedAt,"onUpdate:modelValue":e[7]||(e[7]=o=>t.happenedAt=o),type:"date","value-format":"YYYY-MM-DD",placeholder:"选择发生时间",style:{
                width:"100%"
              }
              
            }
            ,null,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"对象类型"
          }
          ,{
            default:a(()=>[l(m,{
              modelValue:t.subjectType,"onUpdate:modelValue":e[8]||(e[8]=o=>t.subjectType=o),style:{
                width:"100%"
              }
              ,onChange:ne
            }
            ,{
              default:a(()=>[l(u,{
                label:"联盟",value:"league"
              }
              ),l(u,{
                label:"球队",value:"team"
              }
              ),l(u,{
                label:"教练",value:"coach"
              }
              ),l(u,{
                label:"球员",value:"player"
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"对象"
          }
          ,{
            default:a(()=>[l(m,{
              modelValue:t.subjectId,"onUpdate:modelValue":e[9]||(e[9]=o=>t.subjectId=o),filterable:"",style:{
                width:"100%"
              }
              
            }
            ,{
              default:a(()=>[(b(!0),L(H,null,K(ae.value,o=>(b(),T(u,{
                key:o.id,label:o.label,value:o.id
              }
              ,null,8,["label","value"]))),128))]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"展示位键"
          }
          ,{
            default:a(()=>[l(m,{
              modelValue:t.displayKey,"onUpdate:modelValue":e[10]||(e[10]=o=>t.displayKey=o),style:{
                width:"100%"
              }
              
            }
            ,{
              default:a(()=>[l(u,{
                label:"球员资料",value:"player_profile"
              }
              ),l(u,{
                label:"球员生涯记录",value:"player_career"
              }
              ),l(u,{
                label:"球员沿革时间线",value:"player_timeline"
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"素材类型"
          }
          ,{
            default:a(()=>[l(G,{
              modelValue:t.mediaType,"onUpdate:modelValue":e[11]||(e[11]=o=>t.mediaType=o)
            }
            ,{
              default:a(()=>[l(h,{
                value:"image"
              }
              ,{
                default:a(()=>[...e[20]||(e[20]=[r("图片",-1)])]),_:1
              }
              ),l(h,{
                value:"video"
              }
              ,{
                default:a(()=>[...e[21]||(e[21]=[r("视频",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),l(d,{
            label:"素材来源"
          }
          ,{
            default:a(()=>[l(G,{
              modelValue:t.mediaSource,"onUpdate:modelValue":e[12]||(e[12]=o=>t.mediaSource=o)
            }
            ,{
              default:a(()=>[l(h,{
                value:"upload"
              }
              ,{
                default:a(()=>[...e[22]||(e[22]=[r("上传",-1)])]),_:1
              }
              ),l(h,{
                value:"external"
              }
              ,{
                default:a(()=>[...e[23]||(e[23]=[r("外链",-1)])]),_:1
              }
              )]),_:1
            }
            ,8,["modelValue"])]),_:1
          }
          ),t.mediaSource==="external"?(b(),T(d,{
            key:0,label:"素材地址"
          }
          ,{
            default:a(()=>[l(M,{
              modelValue:t.mediaUrl,"onUpdate:modelValue":e[13]||(e[13]=o=>t.mediaUrl=o)
            }
            ,null,8,["modelValue"])]),_:1
          }
          )):(b(),T(d,{
            key:1,label:"上传素材"
          }
          ,{
            default:a(()=>[l(be,{
              "show-file-list":!1,accept:te.value,"before-upload":ue,"http-request":de
            }
            ,{
              default:a(()=>[l(n,null,{
                default:a(()=>[r(A(t.mediaType==="video"?"上传视频":"上传图片"),1)]),_:1
              }
              )]),_:1
            }
            ,8,["accept"]),O("span",el,A(t.mediaUrl),1)]),_:1
          }
          ))]),_:1
        }
        ,8,["model"])]),_:1
      }
      ,8,["modelValue","title","close-on-click-modal"])])
    }
    
  }
  
}
),yl=Be(ll,[["__scopeId","data-v-8c7ae450"]]);

export default yl;
