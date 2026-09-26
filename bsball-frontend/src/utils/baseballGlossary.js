// 棒球术语表与公式 —— 行为移植自编译产物 baseballGlossary chunk（逐字）
const GLOSSARY_KEYS = new Set([
  'pa', 'gp', 'g', 'ab', 'oneB', 'r', 'h', 'pitchH', 'twoB', 'threeB', 'hr', 'iphr', 'pitchHr', 'pitchInsideParkHr',
  'rbi', 'bb', 'pitchBb', 'ibb', 'hbp', 'hb', 'so', 'pitchSo', 'w', 'l', 'gs', 'cg', 'pg', 'sho', 'sv', 'svo',
  'ip', 'tbf', 'bf', 'pitchBf', 'np', 'pIp', 'qs', 'gf', 'pitchR', 'er', 'era', 'whip', 'so9', 'bb9', 'h9',
  'kBb', 'avgAgainst', 'sbAllowed', 'csAllowed', 'pk', 'sh', 'sac', 'sf', 'gdp', 'gidp', 'goFo', 'goAo', 'xbh',
  'tb', 'babip', 'iso', 'abHr', 'bbK', 'bbPct', 'kPct', 'fip', 'eraPlus', 'inn', 'tc', 'po', 'a', 'e', 'dp',
  'pb', 'catcherCs', 'tcPct', 'sb', 'cs', 'avg', 'obp', 'slg', 'ops', 'opsPlus', 'bipPct'
]);

const GLOSSARY_FORMULAS = {
  pIp: 'P/IP', era: '(ERx9)/IP', whip: '(BB+H)/IP', so9: '9xSO/IP', bb9: '9xBB/IP', h9: '9xH/IP', kBb: 'SO/BB',
  avgAgainst: 'H/AB', goFo: 'GO/AO', goAo: 'GO/AO', xbh: '2B+3B+HR', babip: '(H-HR)/(AB-K-HR+SF)',
  iso: '(2B+(2x3B)+(3xHR))/AB', abHr: 'AB/HR', bbK: 'BB/SO', bbPct: 'BB/PA', kPct: 'K/PA',
  tcPct: '(PO+A)/(PO+A+E)', avg: 'H/AB', obp: '(H+BB+HBP)/(AB+BB+HBP+SF)', slg: '(1B+2Bx2+3Bx3+HRx4)/AB',
  ops: 'OBP+SLG', ip: '@glossary.formula.ip', inn: '@glossary.formula.inn', tb: '1B+2×2B+3×3B+4×HR'
};

function normalizeSubject(value) {
  return value === 'batters' ? 'batter' : value === 'pitchers' ? 'pitcher' : value ?? 'neutral';
}

function normalizeFormulaKey(input) {
  const raw = String(input).trim();
  if (!raw) return null;
  const lower = raw.toLowerCase();
  const compact = lower.replace(/[^a-z0-9]/g, '');
  const spaced = lower.replace(/\s+/g, '');
  const aliases = {
    k9: 'so9', so9: 'so9', 'k/9': 'so9', 'so/9': 'so9', bb9: 'bb9', 'bb/9': 'bb9',
    kbb: 'kBb', 'k/bb': 'kBb', 'so/bb': 'kBb', bbb: 'bbPct', bbpct: 'bbPct', 'bb%': 'bbPct',
    kpct: 'kPct', 'k%': 'kPct', pipa: 'pIp', pip: 'pIp', 'p/ip': 'pIp',
    goao: 'goAo', 'go/ao': 'goAo', abhr: 'abHr', 'ab/hr': 'abHr', tbf: 'tbf', hld: 'hld', sho: 'sho'
  };
  return aliases[spaced] ?? aliases[compact] ?? compact;
}

function canonicalizeKey(key) {
  if (!key) return null;
  if (GLOSSARY_KEYS.has(key)) return key;
  const lower = key.toLowerCase();
  for (const candidate of GLOSSARY_KEYS) {
    if (candidate.toLowerCase() === lower) return candidate;
  }
  return null;
}

// 术语键归一化（含投手域别名映射）
export function normalizeGlossaryKey(input, options) {
  const domain = options?.domain ?? 'general';
  const subject = normalizeSubject(options?.subject);
  const compact = normalizeFormulaKey(input);
  if (!compact) return null;
  const canonical = canonicalizeKey(compact);
  if (!canonical) return null;
  if (domain === 'pitching' && subject === 'pitcher') {
    const mapped =
      {
        h: 'pitchH', r: 'pitchR', bb: 'pitchBb', so: 'pitchSo', avg: 'avgAgainst', hr: 'pitchHr', bf: 'pitchBf'
      }[canonical] ?? canonical;
    return GLOSSARY_KEYS.has(mapped) ? mapped : null;
  }
  return canonical;
}

// 术语条目（键 + 公式）
export function getGlossaryEntry(input, options) {
  const key = normalizeGlossaryKey(input, options);
  return key ? { key, formula: GLOSSARY_FORMULAS[key] } : null;
}
