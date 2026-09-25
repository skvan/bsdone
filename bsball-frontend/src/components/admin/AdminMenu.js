// AdminMenu —— 行为保真移植自编译产物 AdminLayout chunk（recon-gen-admin-layout.mjs 生成，勿手改）
// 生成源：.qoder/frontend-recon/out/beautified/AdminLayout-Cx0XHwPP.pretty.js
// 别名与编译产物局部名一致（经 recon-probe-ep.mjs 运行时探针核实）
import { createVNode as t, createElementVNode as n, createBlock as b, createElementBlock as x, openBlock as d, withCtx as a, createCommentVNode as _, toDisplayString as F, Fragment as pe, renderList as Ae, resolveComponent as Re, resolveDynamicComponent as $e, defineComponent as Ve, ref as $, computed as f } from 'vue';
import { ElIcon as mt, ElMenuItem as Lt, ElSubMenu as yn } from 'element-plus';
import { useRoute as je } from 'vue-router';
var kl=3,xl=Ve({
  __name:"AdminMenu",props:{
    menus:{
      
    }
    ,menuIds:{
      
    }
    ,iconsMap:{
      
    }
    
  }
  ,setup(B){
    const w=je(),D=f(()=>String(w.params.tenantCode||"")),A=B;
    function p(E,u){
      const h=[];
      for(const S of E||[]){
        if((S.menuType??2)===kl||S.hideMenu===!0||S.hideMenu===1)continue;
        const L=p(S.children||[],u);
        !(u.size===0?S.id===1:u.has(S.id))&&L.length===0||h.push({
          ...S,children:L
        }
        )
      }
      return h
    }
    const C=f(()=>p(A.menus,A.menuIds));
    function y(E){
      const u=E.path;
      if(u?.startsWith("/admin")){
        const h=D.value;
        return h?`/${h}${u}`:u
      }
      return`sub-${E.id}`
    }
    return(E,u)=>{
      const h=mt,S=Lt,L=Re("AdminMenu",!0),R=yn;
      return d(!0),x(pe,null,Ae(C.value,O=>(d(),x(pe,{
        key:O.id
      }
      ,[O.children?.length?(d(),b(R,{
        key:1,index:y(O)
      }
      ,{
        title:a(()=>[O.icon&&B.iconsMap[O.icon]?(d(),b(h,{
          key:0
        }
        ,{
          default:a(()=>[(d(),b($e(B.iconsMap[O.icon])))]),_:2
        }
        ,1024)):_("",!0),n("span",null,F(O.name||O.title||"-"),1)]),default:a(()=>[t(L,{
          menus:O.children||[],"menu-ids":B.menuIds,"icons-map":B.iconsMap
        }
        ,null,8,["menus","menu-ids","icons-map"])]),_:2
      }
      ,1032,["index"])):(d(),b(S,{
        key:0,index:y(O)
      }
      ,{
        default:a(()=>[O.icon&&B.iconsMap[O.icon]?(d(),b(h,{
          key:0
        }
        ,{
          default:a(()=>[(d(),b($e(B.iconsMap[O.icon])))]),_:2
        }
        ,1024)):_("",!0),n("span",null,F(O.name||O.title||"-"),1)]),_:2
      }
      ,1032,["index"]))],64))),128)
    }
    
  }
  
}
),Ge=xl

export default Ge;
