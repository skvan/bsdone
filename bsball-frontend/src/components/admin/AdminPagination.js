// AdminPagination —— 行为保真移植自编译产物 AdminPagination-Cw2wDwmq（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { createElementBlock as x, defineComponent as w, computed as r, ref as m, createVNode as B, openBlock as y, watch as C, onMounted as I, onBeforeUnmount as A } from 'vue';
import { ElPagination as E } from 'element-plus';
import { exportSfc as U } from '../../utils/exportSfc';
import '../../styles/legacy/admin-pagination.css';
var k={
  class:"admin-pagination-wrap"
}
,L=768,b=w({
  __name:"AdminPagination",props:{
    currentPage:{
      
    }
    ,pageSize:{
      
    }
    ,total:{
      
    }
    ,pageSizes:{
      default:()=>[10,20,30,50,100]
    }
    
  }
  ,emits:["update:currentPage","update:pageSize","change"],setup(i,{
    emit:v
  }
  ){
    const a=i,n=v,u=m(!1),s=m(a.pageSize);
    function p(){
      u.value=typeof window<"u"&&window.innerWidth<L
    }
    const z=r(()=>a.pageSizes.length>0?"total, sizes, prev, pager, next, jumper":"total, prev, pager, next, jumper"),f=r(()=>u.value?5:7),_=r(()=>u.value?"small":"default");
    I(()=>{
      p(),window.addEventListener("resize",p)
    }
    ),A(()=>{
      window.removeEventListener("resize",p)
    }
    ),C(()=>a.pageSize,e=>{
      s.value=e
    }
    );
    const g=r({
      get:()=>a.currentPage,set:e=>n("update:currentPage",e)
    }
    ),c=r({
      get:()=>a.pageSize,set:e=>n("update:pageSize",e)
    }
    );
    function h(){
      n("change")
    }
    function P(e){
      const t=s.value||e,l=a.currentPage,o=Math.max(0,(l-1)*t),M=Math.floor(o/Math.max(1,e))+1,S=Math.max(1,Math.ceil((a.total||0)/Math.max(1,e))),d=Math.min(Math.max(1,M),S);
      d!==a.currentPage&&n("update:currentPage",d),s.value=e,n("change")
    }
    return(e,t)=>{
      const l=E;
      return y(),x("div",k,[B(l,{
        "current-page":g.value,"onUpdate:currentPage":t[0]||(t[0]=o=>g.value=o),"page-size":c.value,"onUpdate:pageSize":t[1]||(t[1]=o=>c.value=o),"page-sizes":i.pageSizes,total:i.total,layout:z.value,"pager-count":f.value,size:_.value,onSizeChange:P,onCurrentChange:h
      }
      ,null,8,["current-page","page-size","page-sizes","total","layout","pager-count","size"])])
    }
    
  }
  
}
),G=U(b,[["__scopeId","data-v-57d3899f"]]);

export default G;
