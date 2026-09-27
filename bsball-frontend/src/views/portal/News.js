// News —— 行为保真移植自编译产物 News-CpHbOyy0（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as s, defineComponent as L, createTextVNode as x, computed as N, toDisplayString as v, createElementVNode as i, unref as c, createBlock as A, ref as p, createVNode as r, openBlock as a, withCtx as w, Fragment as y, resolveComponent as R, watch as $, renderList as b, createCommentVNode as f } from 'vue';
import { ElIcon as U, ElSkeleton as F, ElEmpty as M, ElSkeletonItem as j } from 'element-plus';
import { Document as G } from '@element-plus/icons-vue';
import { useI18n as H } from 'vue-i18n';
import { resolveAssetUrl as P } from '../../api/request';
import { formatDateDot as W } from '../../utils/formatDate';
import { useAppConfigStore as q } from '../../stores/appConfig';
import { exportSfc as J } from '../../utils/exportSfc';
import { articleApi as K } from '../../api/system';
import { useTenantRouter as O } from '../../composables/useTenantRouter';
import Q from '../../components/admin/AdminPagination.js';
import { resolveNoticeCoverUrl as X } from '../../utils/noticeContentImage';
import { appendCacheVersion as Y } from '../../utils/assetCacheUrl';
import { parseCacheTimestamp as Z } from '../../utils/assetCacheUrl';
import '../../styles/legacy/news.css';
var ee={
  class:"portal-news"
}
,te={
  key:0,class:"news-grid news-grid--skeleton","aria-busy":"true"
}
,ae={
  class:"news-sk-row"
}
,se={
  class:"news-sk-info"
}
,re={
  key:1,class:"news-grid"
}
,oe={
  key:0,class:"cover"
}
,ne=["src","alt"],le={
  key:1,class:"cover placeholder"
}
,ie={
  class:"info"
}
,ce={
  class:"news-card-title"
}
,ue={
  key:0,class:"summary"
}
,pe={
  class:"meta"
}
,de={
  key:2,class:"news-empty-wrap"
}
,g=10,ve=L({
  __name:"News",setup(_e){
    const E=q(),{
      t:o
    }
    =H(),{
      portalPath:I
    }
    =O(),u=p("all"),h=p([]),_=p(!1),d=p(1),m=p(0),k=N(()=>h.value.map(e=>({
      id:e.id,title:e.title,summary:e.summary,date:e.createdAt,cover:B(e),viewCount:e.viewCount
    }
    ))),S=N(()=>{
      if(u.value==="all")return o("news.noNews");
      const e={
        news:o("news.categoryNews"),announcement:o("news.categoryAnnouncement"),notice:o("news.categoryNotice"),publicity:o("news.categoryPublicity")
      }
      [u.value];
      return e?`暂无${e}`:o("news.noNews")
    }
    );
    function B(e){
      const n=X(e);
      if(n)return Y(P(n),Z(e.updatedAt,e.createdAt))
    }
    function C(){
      _.value=!0;
      const e=u.value==="all"?void 0:u.value;
      K.list({
        page:d.value,pageSize:g,publishTarget:"portal",forPortal:!0,...e?{
          type:e
        }
        :{
          
        }
        
      }
      ).then(({
        list:n,total:l
      }
      )=>{
        h.value=n,m.value=l
      }
      ).finally(()=>{
        _.value=!1
      }
      )
    }
    return $([u],()=>{
      d.value=1,C()
    }
    ,{
      immediate:!0
    }
    ),(e,n)=>{
      const l=j,T=F,z=U,V=R("router-link"),D=M;
      return a(),s("div",ee,[_.value?(a(),s("div",te,[(a(),s(y,null,b(5,t=>i("div",{
        key:"nw-sk-"+t,class:"news-card news-card--skeleton"
      }
      ,[r(T,{
        animated:"",throttle:0
      }
      ,{
        template:w(()=>[i("div",ae,[r(l,{
          variant:"rect",class:"news-sk-cover"
        }
        ),i("div",se,[r(l,{
          variant:"h3",style:{
            width:"88%",height:"18px"
          }
          
        }
        ),r(l,{
          variant:"text",style:{
            width:"100%","margin-top":"10px"
          }
          
        }
        ),r(l,{
          variant:"text",style:{
            width:"72%","margin-top":"8px"
          }
          
        }
        ),r(l,{
          variant:"text",style:{
            width:"36%","margin-top":"12px"
          }
          
        }
        )])])]),_:1
      }
      )])),64))])):k.value.length>0?(a(),s("div",re,[(a(!0),s(y,null,b(k.value,t=>(a(),A(V,{
        key:t.id,to:c(I)(`article/${t.id}`),class:"news-card portal-card-link"
      }
      ,{
        default:w(()=>[t.cover?(a(),s("div",oe,[i("img",{
          src:c(P)(t.cover),alt:t.title
        }
        ,null,8,ne)])):(a(),s("div",le,[r(z,null,{
          default:w(()=>[r(c(G))]),_:1
        }
        )])),i("div",ie,[i("h3",ce,v(t.title),1),t.summary?(a(),s("p",ue,v(t.summary),1)):f("",!0),i("span",pe,[x(v(c(W)(t.date))+" ",1),c(E).publicViewCount&&t.viewCount!=null?(a(),s(y,{
          key:0
        }
        ,[x(" · "+v(c(o)("news.viewCount",{
          n:t.viewCount
        }
        )),1)],64)):f("",!0)])])]),_:2
      }
      ,1032,["to"]))),128))])):(a(),s("div",de,[r(D,{
        description:S.value
      }
      ,null,8,["description"])])),m.value>g?(a(),A(Q,{
        key:3,class:"pagination","current-page":d.value,"onUpdate:currentPage":n[0]||(n[0]=t=>d.value=t),"page-size":g,total:m.value,"page-sizes":[],onChange:C
      }
      ,null,8,["current-page","total"])):f("",!0)])
    }
    
  }
  
}
),Ee=J(ve,[["__scopeId","data-v-4469fea8"]]);

export default Ee;
