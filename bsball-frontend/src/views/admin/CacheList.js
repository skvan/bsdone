// CacheList —— 行为保真移植自编译产物 CacheList-eKDv_qbZ（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { withModifiers as M, createElementBlock as p, defineComponent as Y, createTextVNode as _, toDisplayString as w, createElementVNode as h, createBlock as B, ref as v, createVNode as t, withDirectives as W, openBlock as i, withCtx as l, withKeys as ee, watch as le, createCommentVNode as C } from 'vue';
import { ElDescriptions as j, ElCol as q, ElDescriptionsItem as F, ElRow as O, ElTableColumn as Q, ElInput as X, ElMessage as x, ElTable as Z, ElButton as ae, ElCard as te, ElMessageBox as R, vLoading as ne } from 'element-plus';
import { exportSfc as se } from '../../utils/exportSfc';
import { monitorApi as y } from '../../api/system';
import '../../styles/legacy/cache-list.css';
var ue={
  class:"admin-page"
}
,oe={
  key:0,class:"empty-tip"
}
,re={
  key:0,class:"empty-tip"
}
,ie={
  key:1,class:"empty-tip"
}
,ce={
  key:1,class:"cache-value-wrap"
}
,ve={
  class:"cache-value"
}
,de={
  key:0,class:"value-msg"
}
,me={
  key:2,class:"empty-tip"
}
,pe=Y({
  __name:"CacheList",setup(_e){
    const g=v(!1),f=v([]),n=v(""),E=v(""),d=v([]),K=v(!1),u=v(null),c=v(""),o=v("");
    async function V(){
      g.value=!0;
      try{
        f.value=((await y.getCacheInfo()).caches??[]).map(a=>({
          name:a.name,remark:a.remark??""
        }
        )),f.value.length&&!n.value&&(n.value=f.value[0].name)
      }
      catch{
        f.value=[]
      }
      finally{
        g.value=!1
      }
      
    }
    async function k(){
      const a=n.value;
      if(!a){
        d.value=[];
        return
      }
      K.value=!0;
      try{
        d.value=(await y.getCacheKeys(a,E.value||void 0,500))?.keys??[]
      }
      catch{
        d.value=[]
      }
      finally{
        K.value=!1
      }
      u.value=null,c.value="",o.value=""
    }
    function D(a){
      a&&(n.value=a.name)
    }
    function T(a){
      if(u.value=a,!a||!n.value){
        c.value="",o.value="";
        return
      }
      y.getCacheValue(n.value,a).then(e=>{
        c.value=e?.value??"",o.value=e?.message??""
      }
      ).catch(()=>{
        c.value="",o.value="获取失败"
      }
      )
    }
    function N(a){
      R.confirm(`确定清空缓存「${a.name}」吗？`,"确认",{
        type:"warning"
      }
      ).then(()=>y.clearCache(a.name)).then(e=>{
        const s=e?.data;
        s?.ok?(x.success("已清空"),n.value===a.name&&(n.value="",u.value=null,d.value=[],c.value="",o.value=""),V()):x.warning(s?.message??"操作失败")
      }
      ).catch(()=>{
        
      }
      )
    }
    function $(){
      const a=n.value;
      a&&N({
        name:a
      }
      )
    }
    function A(a){
      const e=n.value;
      e&&R.confirm(`确定删除键「${a}」吗？`,"确认",{
        type:"warning"
      }
      ).then(()=>y.removeCacheKey(e,a)).then(s=>{
        const r=s?.data;
        r?.ok?(x.success("已删除"),u.value===a&&(u.value=null,c.value="",o.value=""),k()):x.warning(r?.message??"操作失败")
      }
      ).catch(()=>{
        
      }
      )
    }
    le(n,a=>{
      a&&k()
    }
    );
    async function S(){
      const a=n.value,e=u.value;
      g.value=!0;
      try{
        if(await V(),a)if(await k(),e&&d.value.includes(e)){
          u.value=e;
          const s=await y.getCacheValue(a,e);
          c.value=s?.value??"",o.value=s?.message??""
        }
        else u.value=null,c.value="",o.value=""
      }
      finally{
        g.value=!1
      }
      
    }
    return V(),(a,e)=>{
      const s=ae,r=Q,z=Z,b=te,L=q,U=X,I=F,G=j,H=O,J=ne;
      return i(),p("div",ue,[W((i(),B(b,null,{
        header:l(()=>[e[2]||(e[2]=h("span",null,"缓存列表",-1)),t(s,{
          type:"primary",style:{
            float:"right"
          }
          ,onClick:S
        }
        ,{
          default:l(()=>[...e[1]||(e[1]=[_("刷新",-1)])]),_:1
        }
        )]),default:l(()=>[t(H,{
          gutter:16,class:"cache-panels"
        }
        ,{
          default:l(()=>[t(L,{
            xs:24,sm:24,md:8
          }
          ,{
            default:l(()=>[t(b,{
              shadow:"hover",class:"panel-card"
            }
            ,{
              header:l(()=>[...e[3]||(e[3]=[h("span",null,"缓存列表",-1)])]),default:l(()=>[t(z,{
                data:f.value,border:"","highlight-current-row":"","max-height":"360","row-key":"name","current-row-key":n.value,onCurrentChange:D
              }
              ,{
                default:l(()=>[t(r,{
                  type:"index",label:"序号",width:"60"
                }
                ),t(r,{
                  prop:"name",label:"缓存名称","min-width":"90"
                }
                ),t(r,{
                  prop:"remark",label:"备注","min-width":"80","show-overflow-tooltip":""
                }
                ),t(r,{
                  label:"操作",width:"70",align:"center"
                }
                ,{
                  default:l(({
                    row:m
                  }
                  )=>[t(s,{
                    link:"",type:"danger",size:"small",onClick:M(P=>N(m),["stop"])
                  }
                  ,{
                    default:l(()=>[...e[4]||(e[4]=[_("删除",-1)])]),_:1
                  }
                  ,8,["onClick"])]),_:1
                }
                )]),_:1
              }
              ,8,["data","current-row-key"]),f.value.length===0?(i(),p("p",oe,"暂无缓存")):C("",!0)]),_:1
            }
            )]),_:1
          }
          ),t(L,{
            xs:24,sm:24,md:8
          }
          ,{
            default:l(()=>[t(b,{
              shadow:"hover",class:"panel-card"
            }
            ,{
              header:l(()=>[e[5]||(e[5]=h("span",null,"键名列表",-1)),t(U,{
                modelValue:E.value,"onUpdate:modelValue":e[0]||(e[0]=m=>E.value=m),placeholder:"过滤 key",clearable:"",size:"small",style:{
                  width:"100px",float:"right"
                }
                ,onKeyup:ee(k,["enter"])
              }
              ,null,8,["modelValue"])]),default:l(()=>[t(z,{
                data:d.value,border:"","highlight-current-row":"","max-height":"320",onCurrentChange:T
              }
              ,{
                default:l(()=>[t(r,{
                  type:"index",label:"序号",width:"60"
                }
                ),t(r,{
                  prop:"key",label:"缓存键名","min-width":"100","show-overflow-tooltip":""
                }
                ,{
                  default:l(({
                    row:m
                  }
                  )=>[_(w(m),1)]),_:1
                }
                ),t(r,{
                  label:"操作",width:"70",align:"center"
                }
                ,{
                  default:l(({
                    row:m
                  }
                  )=>[t(s,{
                    link:"",type:"danger",size:"small",onClick:M(P=>A(m),["stop"])
                  }
                  ,{
                    default:l(()=>[...e[6]||(e[6]=[_("删除",-1)])]),_:1
                  }
                  ,8,["onClick"])]),_:1
                }
                )]),_:1
              }
              ,8,["data"]),n.value&&d.value.length===0&&!K.value?(i(),p("p",re,"暂无键或未查询")):n.value?C("",!0):(i(),p("p",ie,"请先在左侧选择缓存"))]),_:1
            }
            )]),_:1
          }
          ),t(L,{
            xs:24,sm:24,md:8
          }
          ,{
            default:l(()=>[t(b,{
              shadow:"hover",class:"panel-card"
            }
            ,{
              header:l(()=>[e[8]||(e[8]=h("span",null,"缓存内容",-1)),n.value?(i(),B(s,{
                key:0,type:"danger",size:"small",style:{
                  float:"right"
                }
                ,onClick:$
              }
              ,{
                default:l(()=>[...e[7]||(e[7]=[_("清理全部",-1)])]),_:1
              }
              )):C("",!0)]),default:l(()=>[n.value&&u.value!==null?(i(),B(G,{
                key:0,column:1,border:"",size:"small"
              }
              ,{
                default:l(()=>[t(I,{
                  label:"缓存名称"
                }
                ,{
                  default:l(()=>[_(w(n.value),1)]),_:1
                }
                ),t(I,{
                  label:"缓存键名"
                }
                ,{
                  default:l(()=>[_(w(u.value),1)]),_:1
                }
                )]),_:1
              }
              )):C("",!0),n.value&&u.value!==null?(i(),p("div",ce,[e[9]||(e[9]=h("label",{
                class:"value-label"
              }
              ,"缓存内容",-1)),h("pre",ve,w(c.value),1),o.value?(i(),p("p",de,w(o.value),1)):C("",!0)])):(i(),p("p",me,"请先在左侧选择缓存，再在中间选择键名"))]),_:1
            }
            )]),_:1
          }
          )]),_:1
        }
        )]),_:1
      }
      )),[[J,g.value]])])
    }
    
  }
  
}
),ke=se(pe,[["__scopeId","data-v-0b9526bc"]]);

export default ke;
