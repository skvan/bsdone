// AnnouncementDoc —— 行为保真移植自编译产物 AnnouncementDoc-niNwN-Dz（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as n, defineComponent as y, createTextVNode as v, toDisplayString as s, createElementVNode as _, unref as e, createBlock as c, createVNode as E, openBlock as r, withCtx as m, createCommentVNode as D } from 'vue';
import { ElSkeleton as h, ElEmpty as g, ElButton as B } from 'element-plus';
import { useRoute as b } from 'vue-router';
import { fetchResult as w } from '../../api/request';
import { formatDateTimeDotWithWeek as x } from '../../utils/dateExtras';
import { exportSfc as A } from '../../utils/exportSfc';
import { useDetailLoad as C } from '../../composables/useDetailLoad';
import '../../styles/legacy/announcement-doc.css';
var N={
  class:"portal-doc"
}
,S={
  key:0,class:"doc-article"
}
,V={
  key:0,class:"meta"
}
,I={
  class:"content"
}
,L=y({
  __name:"AnnouncementDoc",setup(M){
    const p=b(),{
      loading:u,error:i,result:o,retry:d
    }
    =C(async()=>{
      const a=Number(p.params.id);
      if(!a)return null;
      const t=await w(`/api/sys/notice/${a}`);
      if(t.error)throw new Error(t.error);
      return t.data??null
    }
    );
    return(a,t)=>{
      const f=h,k=B,l=g;
      return r(),n("div",N,[e(o)?(r(),n("article",S,[_("h1",null,s(e(o).title),1),e(o).createdAt?(r(),n("p",V,s(e(x)(e(o).createdAt)),1)):D("",!0),_("div",I,s(e(o).content),1)])):e(u)?(r(),c(f,{
        key:1,rows:6,animated:""
      }
      )):e(i)?(r(),c(l,{
        key:2,description:e(i)
      }
      ,{
        default:m(()=>[E(k,{
          type:"primary",onClick:e(d)
        }
        ,{
          default:m(()=>[...t[0]||(t[0]=[v("重试",-1)])]),_:1
        }
        ,8,["onClick"])]),_:1
      }
      ,8,["description"])):(r(),c(l,{
        key:3,description:"公告不存在或已删除"
      }
      ))])
    }
    
  }
  
}
),j=A(L,[["__scopeId","data-v-4ebaa877"]]);

export default j;
