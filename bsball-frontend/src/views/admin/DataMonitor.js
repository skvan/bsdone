// DataMonitor —— 行为保真移植自编译产物 DataMonitor-C7l0WNHh（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as s, defineComponent as u, toDisplayString as d, createElementVNode as n, ref as l, createVNode as p, openBlock as o, withCtx as _, onMounted as m, createCommentVNode as v } from 'vue';
import { ElCard as c } from 'element-plus';
import { getApiOrigin as f } from '../../api/request';
import { exportSfc as g } from '../../utils/exportSfc';
import { monitorApi as h } from '../../api/system';
import '../../styles/legacy/data-monitor.css';
var y={
  class:"admin-page"
}
,D={
  key:0,class:"tip"
}
,k={
  class:"iframe-wrap"
}
,B=["src"],M={
  key:1,class:"loading"
}
,w=u({
  __name:"DataMonitor",setup(x){
    const e=l(""),t=l("");
    return m(async()=>{
      try{
        const i=await h.getDatasourceUrl(),a=i?.url??"";
        if(a&&a.startsWith("/")){
          const r=f();
          e.value=r?r+a:a
        }
        else e.value=a;
        t.value=i?.tip??""
      }
      catch{
        t.value="获取监控地址失败"
      }
      
    }
    ),(i,a)=>{
      const r=c;
      return o(),s("div",y,[p(r,null,{
        header:_(()=>[...a[0]||(a[0]=[n("span",null,"数据监控",-1),n("span",{
          class:"header-tip"
        }
        ,"Druid 连接池与 SQL 统计",-1)])]),default:_(()=>[t.value?(o(),s("p",D,d(t.value),1)):v("",!0),n("div",k,[e.value?(o(),s("iframe",{
          key:0,src:e.value,class:"druid-iframe",title:"Druid 数据监控"
        }
        ,null,8,B)):(o(),s("div",M,"加载中..."))])]),_:1
      }
      )])
    }
    
  }
  
}
),L=g(w,[["__scopeId","data-v-b478b8a6"]]);

export default L;
