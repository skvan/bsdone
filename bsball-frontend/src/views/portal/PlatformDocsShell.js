// PlatformDocsShell —— 行为保真移植自编译产物 PlatformDocsShell-BmH_HX23（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as _, defineComponent as p, toDisplayString as u, createElementVNode as e, unref as t, ref as d, createVNode as s, openBlock as h, resolveComponent as v, provide as b } from 'vue';
import { HomeFilled as f } from '@element-plus/icons-vue';
import { ElButton as D } from 'element-plus';
import { useI18n as P } from 'vue-i18n';
import { useRouter as k, useRoute as T } from 'vue-router';
import { DEFAULT_TENANT_CODE as x } from '../../utils/tenantRoute';
import { exportSfc as y } from '../../utils/exportSfc';
import { PLATFORM_DOCS_LIST_REFRESH as C } from '../../utils/platformDocsRefresh';
import '../../styles/legacy/platform-docs-shell.css';
var E={
  class:"platform-docs-shell"
}
,B={
  class:"platform-docs-header"
}
,I=["title"],S={
  class:"platform-docs-main"
}
,g=p({
  __name:"PlatformDocsShell",setup(L){
    const{
      t:o
    }
    =P(),l=T(),a=k(),r=d(0);
    b(C,r);
    function n(){
      a.push(`/${x}/`)
    }
    function c(){
      if(l.name!=="PlatformDocsIndex"){
        a.push({
          name:"PlatformDocsIndex"
        }
        );
        return
      }
      r.value+=1
    }
    return(N,R)=>{
      const i=D,m=v("router-view");
      return h(),_("div",E,[e("header",B,[s(i,{
        type:"primary",text:"",circle:"",class:"platform-docs-home-btn",icon:t(f),"aria-label":t(o)("platformDocs.backToPortal"),title:t(o)("platformDocs.backToPortal"),onClick:n
      }
      ,null,8,["icon","aria-label","title"]),e("button",{
        type:"button",class:"platform-docs-title-btn",title:t(o)("platformDocs.refreshListHint"),onClick:c
      }
      ,u(t(o)("route.platformDocs")),9,I)]),e("main",S,[s(m)])])
    }
    
  }
  
}
),F=y(g,[["__scopeId","data-v-047da023"]]);

export default F;
