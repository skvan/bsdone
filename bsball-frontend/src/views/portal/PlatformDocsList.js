// PlatformDocsList —— 行为保真移植自编译产物 PlatformDocsList-CFCyR-3f（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as e, defineComponent as u, createTextVNode as f, toDisplayString as o, unref as n, ref as i, createVNode as v, inject as h, withDirectives as g, openBlock as a, withCtx as k, Fragment as y, resolveComponent as D, watch as L, renderList as A, createCommentVNode as x } from 'vue';
import { vLoading as P } from 'element-plus';
import { useI18n as C } from 'vue-i18n';
import { formatDateTimeDotWithWeek as S } from '../../utils/dateExtras';
import { exportSfc as T } from '../../utils/exportSfc';
import { articleApi as B } from '../../api/system';
import { PLATFORM_DOCS_LIST_REFRESH as I } from '../../utils/platformDocsRefresh';
import '../../styles/legacy/platform-docs-list.css';
var N={
  class:"platform-docs-list"
}
,V={
  key:0,class:"empty-hint"
}
,b={
  key:1,class:"doc-link-list"
}
,j={
  key:0,class:"doc-meta"
}
,z=u({
  __name:"PlatformDocsList",setup(E){
    const{
      t:c
    }
    =C(),s=i([]),r=i(!0),d=h(I,i(0));
    async function m(){
      r.value=!0;
      try{
        const{
          list:l
        }
        =await B.platformList({
          page:1,pageSize:200,sortProp:"updatedAt",sortOrder:"desc"
        }
        );
        s.value=l
      }
      finally{
        r.value=!1
      }
      
    }
    return L(d,()=>{
      m()
    }
    ,{
      immediate:!0
    }
    ),(l,M)=>{
      const p=D("router-link"),_=P;
      return g((a(),e("div",N,[!r.value&&s.value.length===0?(a(),e("p",V,o(n(c)("platformDocs.empty")),1)):(a(),e("ul",b,[(a(!0),e(y,null,A(s.value,t=>(a(),e("li",{
        key:t.id
      }
      ,[v(p,{
        to:{
          name:"PlatformArticleDetail",params:{
            id:String(t.id)
          }
          
        }
        ,class:"doc-link"
      }
      ,{
        default:k(()=>[f(o(t.title),1)]),_:2
      }
      ,1032,["to"]),t.updatedAt||t.createdAt?(a(),e("span",j,o(n(S)(t.updatedAt||t.createdAt)),1)):x("",!0)]))),128))]))])),[[_,r.value]])
    }
    
  }
  
}
),q=T(z,[["__scopeId","data-v-ef11dbd6"]]);

export default q;
