// AdminWatermark —— 行为保真移植自编译产物 AdminLayout chunk（recon-gen-admin-layout.mjs 生成，勿手改）
// 生成源：.qoder/frontend-recon/out/beautified/AdminLayout-Cx0XHwPP.pretty.js
// 别名与编译产物局部名一致（经 recon-probe-ep.mjs 运行时探针核实）
import { createBlock as b, createElementBlock as x, openBlock as d, createCommentVNode as _, normalizeStyle as ut, defineComponent as Ve, ref as $, watch as be, Teleport as rt } from 'vue';
import { exportSfc as Ee } from '../../utils/exportSfc';
var yl=Ve({
  __name:"AdminWatermark",props:{
    visible:{
      type:Boolean
    }
    ,text:{
      
    }
    
  }
  ,setup(B){
    const w=B,D=$({
      
    }
    );
    function A(){
      if(!w.text)return;
      const p=document.createElement("canvas"),C=p.getContext("2d");
      if(!C)return;
      const y=18,E=-22;
      C.font=`${y}px sans-serif`;
      const u=C.measureText(w.text),h=220,S=h*2;
      Math.max(Math.ceil(u.width)+100,h),Math.max(y*2+80,h),p.width=S,p.height=S,C.font=`${y}px sans-serif`,C.fillStyle="rgba(0,0,0,0.12)";
      for(let L=0;
      L<2;
      L++)for(let R=0;
      R<2;
      R++)if((L+R)%2===1){
        const O=R*h,q=L*h;
        C.save(),C.translate(O+h/2,q+h/2),C.rotate(E*Math.PI/180),C.translate(-(O+h/2),-(q+h/2)),C.fillText(w.text,O+(h-u.width)/2,q+h/2+y/2),C.restore()
      }
      D.value={
        position:"fixed",top:"0",left:"0",right:"0",bottom:"0",backgroundImage:`url(${p.toDataURL("image/png")})`,backgroundRepeat:"repeat",backgroundPosition:"0 0",backgroundSize:`${S}px ${S}px`,pointerEvents:"none",zIndex:"9999"
      }
      
    }
    return be(()=>[w.visible,w.text],()=>{
      w.visible&&w.text?A():D.value={
        
      }
      
    }
    ,{
      immediate:!0
    }
    ),(p,C)=>(d(),b(rt,{
      to:"body"
    }
    ,[B.visible&&B.text?(d(),x("div",{
      key:0,class:"admin-watermark-overlay",style:ut(D.value),"aria-hidden":"true"
    }
    ,null,4)):_("",!0)]))
  }
  
}
),Ot=Ee(yl,[["__scopeId","data-v-deb632ad"]])

export default Ot;
