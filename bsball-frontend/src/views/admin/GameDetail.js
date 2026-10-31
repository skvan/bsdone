// GameDetail —— 行为保真移植自编译产物 GameDetail-C4q9PQKP（recon-gen-b3.mjs 生成，勿手改）
// 别名身份经 recon-probe2.mjs 运行时探针实证；body 为编译产物正文原样
import { ElEmpty as Te, ElButton as de, ElCard as Ve, vLoading as Be, ElTooltip as Fe } from 'element-plus';
import { createElementBlock as _e, defineComponent as oe, createTextVNode as S, computed as G, toDisplayString as r, createElementVNode as n, unref as l, createBlock as q, ref as m, createVNode as a, withDirectives as Se, openBlock as H, withCtx as i, onUnmounted as ke, watch as ue, onMounted as te, createCommentVNode as Ce } from 'vue';
import { useI18n as ce } from 'vue-i18n';
import { useRoute as Le } from 'vue-router';
import { exportSfc as me } from '../../utils/exportSfc';
import { gameStatsApi as ze } from '../../api/business';
import { playerApi as ee } from '../../api/business';
import { gameApi as Ue } from '../../api/business';
import { teamApi as ae } from '../../api/business';
import { fetchPlayersByTeam } from '../../api/business';
import { buildTeamPlayersMap, addTeamEntryPlayers } from '../../utils/teamPlayersMap';
import { attachFieldingPositions } from '../../utils/gameFieldingRows';
import Ze from '../../components/admin/GameDetailContent.js';
import GameDetailHitReplay from '../../components/common/GameDetailHitReplay.vue';
import FieldSettingsDrawer from '../../components/admin/GameDetailFieldSettingsDrawer.js';
import { readFieldSettings as Ge } from '../../utils/gameDetailFieldSettingsStorage';
import { writeFieldSettings as xe } from '../../utils/gameDetailFieldSettingsStorage';
import { DEFAULT_PITCHER_FIELDS as Me, DEFAULT_BATTER_FIELDS as Fa, DEFAULT_FIELDING_FIELDS as FG } from '../../utils/gameDetailFieldSettingsStorage';
import '../../styles/legacy/game-detail.css';
var mt={
  class:"admin-page game-detail-page"
}
,vt={
  class:"header-right field-settings-header"
}
,ie=2,se="leadingZero",ne=!1,re=768,pt=oe({
  __name:"GameDetail",setup(K){
    const{
      t:N
    }
    =ce(),t=Le(),O=G(()=>Number(t.params.gameId));
    G(()=>Number(t.params.eventId));
    const $=G(()=>{
      const u=t.query.hp,b=Array.isArray(u)?u[0]:u,k=Number(b);
      return Number.isFinite(k)&&k>0?k:null
    }
    ),o=m(!0),w=m(null),R=m([]),W=m([]),F=m({
      
    }
    ),C=Fa,z=Me,T=Ge({
      defaultBatterFields:C,defaultPitcherFields:z,defaultFieldingFields:FG,defaultDecimalPlaces:ie,defaultRateDisplayStyle:se,defaultShowTrailingZeros:ne
    }
    ),L=m(T.batterFields),P=m(T.pitcherFields),FF=m(T.fieldingFields),D=m(T.decimalPlaces),p=m(T.rateDisplayStyle),f=m(T.showTrailingZeros),E=G(()=>L.value),V=G(()=>P.value),Vf=G(()=>FF.value),I=m(!1);
    function A(){
      xe({
        batterFields:L.value,pitcherFields:P.value,fieldingFields:FF.value,decimalPlaces:D.value,rateDisplayStyle:p.value,showTrailingZeros:f.value
      }
      )
    }
    ue([L,P,FF,D,p,f],()=>A(),{
      deep:!0
    }
    );
    function Y(u){
      L.value=u.batterItems,P.value=u.pitcherItems,FF.value=u.fieldingItems,D.value=u.decimalPlaces,p.value=u.rateDisplayStyle,f.value=u.showTrailingZeros
    }
    const x=m(typeof window<"u"&&window.innerWidth<re);
    function M(){
      x.value=window.innerWidth<re
    }
    return te(()=>{
      window.addEventListener("resize",M),M()
    }
    ),ke(()=>{
      window.removeEventListener("resize",M)
    }
    ),te(async()=>{
      o.value=!0;
      const u=await Ue.get(O.value);
      if(w.value=u,!u){
        o.value=!1;
        return
      }
      const[b,k,U,fp]=await Promise.all([ze.listByGame(O.value),u.homeTeamId?fetchPlayersByTeam(u.homeTeamId):Promise.resolve([]),u.awayTeamId?fetchPlayersByTeam(u.awayTeamId):Promise.resolve([]),Ue.listFieldingPositions(O.value).catch(()=>[])]);
      R.value=attachFieldingPositions(b.list??[],fp??[]);
      const Z=u.homeTeamId,s=u.awayTeamId,[e,d]=await Promise.all([Z?ae.get(Z):Promise.resolve(null),s?ae.get(s):Promise.resolve(null)]);
      W.value=[e,d].filter(Boolean);
      // H33：playersMap 复合键（teamId:playerId）+ 单键兼容；补集按注册段生成按队副本
      const c=buildTeamPlayersMap(k??[],Z,U??[],s);
      const h=[...new Set(R.value.map(_=>_.playerId).filter(_=>!c[_]))];
      if(h.length){
        addTeamEntryPlayers(c,(await ee.listByIds(h)).list??[])
      }
      F.value=c,o.value=!1
    }
    ),(u,b)=>{
      const k=de,U=Fe,Z=Te,s=Ve,e=Be;
      return H(),_e("div",mt,[Se((H(),q(s,{
        class:"game-detail-card"
      }
      ,{
        header:i(()=>[b[2]||(b[2]=n("div",{
          class:"header-left"
        }
        ,[n("span",null,"比赛详情")],-1)),n("div",vt,[a(U,{
          content:l(N)("fieldSettings.title"),placement:"bottom","show-after":250
        }
        ,{
          default:i(()=>[a(k,{
            size:"small",onClick:b[0]||(b[0]=d=>I.value=!0)
          }
          ,{
            default:i(()=>[S(r(l(N)("fieldSettings.title")),1)]),_:1
          }
          )]),_:1
        }
        ,8,["content"])])]),default:i(()=>[w.value?(H(),q(Ze,{
          key:0,game:w.value,stats:R.value,teams:W.value,"players-map":F.value,"batter-fields":E.value,"pitcher-fields":V.value,"fielding-fields":Vf.value,"decimal-places":D.value,"rate-display-style":p.value,"show-trailing-zeros":f.value,"highlight-player-id":$.value
        }
        ,null,8,["game","stats","teams","players-map","batter-fields","pitcher-fields","fielding-fields","decimal-places","rate-display-style","show-trailing-zeros","highlight-player-id"])):o.value?Ce("",!0):(H(),q(Z,{
          key:1,description:"未找到比赛"
        }
        ))]),_:1
      }
      )),[[e,o.value]]),w.value?(H(),q(s,{
        key:2,class:"game-detail-card"
      }
      ,{
        default:i(()=>[a(GameDetailHitReplay,{
          "game-id":O.value,"players-map":F.value,teams:W.value,"home-team-id":w.value.homeTeamId,"away-team-id":w.value.awayTeamId
        }
        ,null,8,["game-id","players-map","teams","home-team-id","away-team-id"])]),_:1
      }
      )):Ce("",!0),a(FieldSettingsDrawer,{
        modelValue:I.value,"onUpdate:modelValue":b[1]||(b[1]=d=>I.value=d),"batter-items":E.value,"pitcher-items":V.value,"fielding-items":Vf.value,"default-batter-items":C,"default-pitcher-items":z,"default-fielding-items":FG,"decimal-places":D.value,"default-decimal-places":ie,"rate-display-style":p.value,"default-rate-display-style":se,"show-trailing-zeros":f.value,"default-show-trailing-zeros":ne,"is-small-screen":x.value,onSave:Y
      }
      ,null,8,["modelValue","batter-items","pitcher-items","fielding-items","default-pitcher-items","decimal-places","rate-display-style","show-trailing-zeros","is-small-screen"])])
    }
    
  }
  
}
),Tt=me(pt,[["__scopeId","data-v-788349a0"]]);

export default Tt;
