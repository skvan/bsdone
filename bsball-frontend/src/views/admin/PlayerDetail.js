// PlayerDetail —— 行为保真移植自编译产物 PlayerDetail-DYtBNMn0（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as d, defineComponent as c, createElementVNode as n, unref as f, createBlock as o, ref as s, createVNode as v, openBlock as l, withCtx as u, onMounted as k } from 'vue';
import { ElSkeleton as y, ElCard as g } from 'element-plus';
import { useRoute as A } from 'vue-router';
import { playerApi as B } from '../../api/business';
import { teamApi as D } from '../../api/business';
import { DEFAULT_AVATAR_SVG_PATH as E } from '../../utils/placeholderAssets';
import w from '../../components/admin/PlayerDetailContent.js';
var C={
  class:"admin-page"
}
,I=c({
  __name:"PlayerDetail",setup(M){
    const m=A(),e=s(null),t=s(null),i=E;
    return k(async()=>{
      const r=Number(m.params.id),a=await B.get(r);
      e.value=a??null,a?.teamId?t.value=await D.get(a.teamId)??null:t.value=null
    }
    ),(r,a)=>{
      const p=y,_=g;
      return l(),d("div",C,[v(_,null,{
        header:u(()=>[...a[0]||(a[0]=[n("div",{
          class:"header-left"
        }
        ,[n("span",null,"球员详情")],-1)])]),default:u(()=>[e.value?(l(),o(w,{
          key:0,player:e.value,team:t.value,"default-avatar-img":f(i)
        }
        ,null,8,["player","team","default-avatar-img"])):(l(),o(p,{
          key:1,rows:8,animated:""
        }
        ))]),_:1
      }
      )])
    }
    
  }
  
}
),G=I;

export default G;
