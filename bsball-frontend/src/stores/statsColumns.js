// 统计列定义与键常量 —— 行为移植自编译产物入口 chunk（区间切片 + 引用重写）
// 由 recon-gen-stats-columns.mjs 生成（勿手改）；供 useStatsTableMeta 与门户统计页共享
import { formatInningsPitched } from '../utils/inningsFormat';

// 键常量（入口 chunk 同名逻辑键）
export const KEY_BATTING_ORDER = 'battingOrder';
export const KEY_BATTING_VISIBILITY = 'battingVisibility';
export const KEY_PITCHING_ORDER = 'pitchingOrder';
export const KEY_PITCHING_VISIBILITY = 'pitchingVisibility';
export const KEY_FIELDING_ORDER = 'fieldingOrder';
export const KEY_FIELDING_VISIBILITY = 'fieldingVisibility';
export const KEY_TEAM_BATTING_ORDER = 'teamBattingOrder';
export const KEY_TEAM_BATTING_VISIBILITY = 'teamBattingVisibility';
export const KEY_TEAM_PITCHING_ORDER = 'teamPitchingOrder';
export const KEY_TEAM_PITCHING_VISIBILITY = 'teamPitchingVisibility';
export const KEY_TEAM_FIELDING_ORDER = 'teamFieldingOrder';
export const KEY_TEAM_FIELDING_VISIBILITY = 'teamFieldingVisibility';

// 比值列集合（入口 chunk Jr）
export const STATS_RATE_COLS = new Set(['avg', 'obp', 'slg', 'ops', 'era', 'whip', 'tcPct']);

export const STATS_BATTING_COLS = [{key:"gp",prop:"gp",width:70,colKey:"gp",sortable:!0},{key:"pa",prop:"pa",width:70,colKey:"pa",sortable:!0},{key:"ab",prop:"ab",width:70,colKey:"ab",sortable:!0},{key:"avg",prop:"avg",width:90,colKey:"avg",sortable:!0},{key:"obp",prop:"obp",width:90,colKey:"obp",sortable:!0},{key:"slg",prop:"slg",width:90,colKey:"slg",sortable:!0},{key:"ops",prop:"ops",width:90,colKey:"ops",sortable:!0},{key:"r",prop:"r",width:70,colKey:"r",sortable:!0},{key:"h",prop:"h",width:70,colKey:"h",sortable:!0},{key:"doubles",prop:"doubles",width:70,colKey:"twoB",sortable:!0},{key:"triples",prop:"triples",width:70,colKey:"threeB",sortable:!0},{key:"hr",prop:"hr",width:80,colKey:"hr",sortable:!0},{key:"insideParkHr",prop:"insideParkHr",width:104,colKey:"iphr",sortable:!0},{key:"rbi",prop:"rbi",width:80,colKey:"rbi",sortable:!0},{key:"bb",prop:"bb",width:70,colKey:"bb",sortable:!0},{key:"so",prop:"so",width:70,colKey:"so",sortable:!0},{key:"sb",prop:"sb",width:70,colKey:"sb",sortable:!0},{key:"cs",prop:"cs",width:70,colKey:"cs",sortable:!0}];

export const STATS_PITCHING_COLS = [{key:"gp",prop:"gp",width:70,colKey:"gp",sortable:!0},{key:"gs",prop:"gs",width:70,colKey:"gs",sortable:!0},{key:"ip",prop:"ip",width:80,colKey:"ip",sortable:!0,formatter:e=>formatInningsPitched(e.ip)??"-"},{key:"w",prop:"w",width:70,colKey:"w",sortable:!0},{key:"l",prop:"l",width:70,colKey:"l",sortable:!0},{key:"era",prop:"era",width:80,colKey:"era",sortable:!0},{key:"cg",prop:"cg",width:70,colKey:"cg",sortable:!0},{key:"sv",prop:"sv",width:80,colKey:"sv",sortable:!0},{key:"svo",prop:"svo",width:80,colKey:"svo",sortable:!0},{key:"pitchBf",prop:"pitchBf",width:70,colKey:"pitchBf",sortable:!0},{key:"pitchH",prop:"pitchH",width:70,colKey:"h",sortable:!0},{key:"pitchR",prop:"pitchR",width:70,colKey:"r",sortable:!0},{key:"er",prop:"er",width:70,colKey:"er",sortable:!0},{key:"pitchHr",prop:"pitchHr",width:80,colKey:"hr",sortable:!0},{key:"pitchInsideParkHr",prop:"pitchInsideParkHr",width:104,colKey:"pitchInsideParkHr",sortable:!0},{key:"pitchHbp",prop:"pitchHbp",width:80,colKey:"hbp",sortable:!0},{key:"pitchBb",prop:"pitchBb",width:70,colKey:"bb",sortable:!0},{key:"pitchSo",prop:"pitchSo",width:70,colKey:"so",sortable:!0},{key:"whip",prop:"whip",width:100,colKey:"whip",sortable:!0}];

export const STATS_FIELDING_COLS = [{key:"gp",prop:"gp",width:70,colKey:"gp",sortable:!0},{key:"gs",prop:"gs",width:70,colKey:"gs",sortable:!0},{key:"inn",prop:"inn",width:80,colKey:"inn",sortable:!0,formatter:e=>formatInningsPitched(e.inn)??"-"},{key:"tc",prop:"tc",width:80,colKey:"tc",sortable:!0},{key:"po",prop:"po",width:70,colKey:"po",sortable:!0},{key:"a",prop:"a",width:70,colKey:"a",sortable:!0},{key:"e",prop:"e",width:70,colKey:"e",sortable:!0},{key:"dp",prop:"dp",width:70,colKey:"dp",sortable:!0},{key:"tcPct",prop:"tcPct",width:90,colKey:"tcPct",sortable:!0}];

export const TEAM_STATS_BATTING_COLS = [{key:"gp",prop:"gp",width:72,colKey:"gp",sortable:!0},{key:"ab",prop:"ab",width:72,colKey:"ab",sortable:!0},{key:"avg",prop:"avg",width:84,colKey:"avg",sortable:!0},{key:"obp",prop:"obp",width:84,colKey:"obp",sortable:!0},{key:"slg",prop:"slg",width:84,colKey:"slg",sortable:!0},{key:"ops",prop:"ops",width:84,colKey:"ops",sortable:!0},{key:"r",prop:"r",width:72,colKey:"r",sortable:!0},{key:"h",prop:"h",width:72,colKey:"h",sortable:!0},{key:"hr",prop:"hr",width:72,colKey:"hr",sortable:!0},{key:"rbi",prop:"rbi",width:76,colKey:"rbi",sortable:!0},{key:"tb",prop:"tb",width:76,colKey:"tb",sortable:!0},{key:"so",prop:"so",width:72,colKey:"so",sortable:!0},{key:"bb",prop:"bb",width:72,colKey:"bb",sortable:!0},{key:"sb",prop:"sb",width:72,colKey:"sb",sortable:!0}];

export const TEAM_STATS_PITCHING_COLS = [{key:"gp",prop:"gp",width:72,colKey:"gp",sortable:!0},{key:"pitchBf",prop:"pitchBf",width:84,colKey:"pitchBf",sortable:!0},{key:"era",prop:"era",width:84,colKey:"era",sortable:!0},{key:"whip",prop:"whip",width:112,colKey:"whip",sortable:!0},{key:"ip",prop:"ip",width:80,colKey:"ip",sortable:!0},{key:"pitchSo",prop:"pitchSo",width:72,colKey:"pitchSo",sortable:!0},{key:"wp",prop:"wp",width:72,colKey:"wp",sortable:!0},{key:"bk",prop:"bk",width:84,colKey:"bk",sortable:!0},{key:"pitchBb",prop:"pitchBb",width:72,colKey:"pitchBb",sortable:!0},{key:"pitchH",prop:"pitchH",width:72,colKey:"pitchH",sortable:!0},{key:"pitchHr",prop:"pitchHr",width:84,colKey:"pitchHr",sortable:!0},{key:"er",prop:"er",width:72,colKey:"er",sortable:!0},{key:"pitchR",prop:"pitchR",width:72,colKey:"pitchR",sortable:!0},{key:"w",prop:"w",width:64,colKey:"win",sortable:!0},{key:"l",prop:"l",width:64,colKey:"loss",sortable:!0}];

export const TEAM_STATS_FIELDING_COLS = [{key:"gp",prop:"gp",width:72,colKey:"gp",sortable:!0},{key:"tcPct",prop:"tcPct",width:84,colKey:"tcPct",sortable:!0},{key:"tc",prop:"tc",width:72,colKey:"tc",sortable:!0},{key:"po",prop:"po",width:72,colKey:"po",sortable:!0},{key:"a",prop:"a",width:72,colKey:"a",sortable:!0},{key:"e",prop:"e",width:72,colKey:"e",sortable:!0},{key:"dp",prop:"dp",width:72,colKey:"dp",sortable:!0},{key:"pb",prop:"pb",width:72,colKey:"pb",sortable:!0},{key:"catcherCs",prop:"catcherCs",width:72,colKey:"catcherCs",sortable:!0},{key:"inn",prop:"inn",width:80,colKey:"inn",sortable:!0}];

