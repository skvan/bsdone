// 实时观赛本地/会话存储（liveGameStorage-m-Uy-PI9）——行为保真移植，正文逐字保留短名
// 逐字对应：c/l/p=键前缀、u=liveKey、s=watchLocalKey、f=parseStored、i=readSavedAt、
//   v=findForeignWatchRecord、S=pickPreferredRecord、y/h/m=bootstrap 键/写/读、I=clearLiveRecords

var c="bs-ball-live-",l="bs-ball-live-watch-local-";
function u(e,t){
  return t?`${c}${e}-${t}`:`${c}${e}`
}
function s(e,t){
  return t?`${l}${e}-${t}`:`${l}${e}`
}
function f(e){
  try{
    if(!e?.trim())return null;
    const t=JSON.parse(e);
    return t?.v!==1?null:t
  }
  catch{
    return null
  }
  
}
function i(e){
  const t=Number(e?.savedAt);
  return Number.isFinite(t)&&t>0?t:0
}
function v(e,t){
  try{
    const r=[s(e,t),s(e)];
    for(const n of r){
      const o=f(localStorage.getItem(n));
      if(!o)continue;
      const a=Number(o.gameId);
      if(!(Number.isFinite(a)&&a>0&&a!==t))return o
    }
    
  }
  catch{
    
  }
  return null
}
function S(e,t){
  const r=i(e),n=i(t);
  return e&&t?r>=n?{
    source:"local",payload:e
  }
  :{
    source:"server",payload:t
  }
  :e?{
    source:"local",payload:e
  }
  :t?{
    source:"server",payload:t
  }
  :null
}
var p="bs-ball-live-entry-bootstrap-";
function y(e,t){
  return`${p}${e}-${t}`
}
function h(e,t,r){
  try{
    sessionStorage.setItem(y(e,t),JSON.stringify(r))
  }
  catch{
    
  }
  
}
function m(e,t){
  try{
    const r=y(e,t),n=sessionStorage.getItem(r);
    return sessionStorage.removeItem(r),f(n)
  }
  catch{
    return null
  }
  
}
function I(e,t){
  try{
    localStorage.removeItem(u(e,t)),localStorage.removeItem(s(e,t));
    const r=u(e,null)+"-",n=[];
    for(let o=0;
    o<localStorage.length;
    o++){
      const a=localStorage.key(o);
      a?.startsWith(r)&&a.endsWith(`-${t}`)&&n.push(a)
    }
    n.forEach(o=>localStorage.removeItem(o))
  }
  catch{
    
  }
  
}

export {
  f as parseStoredLiveRecord,
  h as writeEntryBootstrap,
  s as watchLocalKey,
  m as takeEntryBootstrap,
  S as pickPreferredLiveRecord,
  u as liveStorageKey,
  v as findForeignWatchRecord,
  I as clearLiveRecords,
};
