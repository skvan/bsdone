// 安打类型派生与汇总：1B 为安打扣除二垒打、三垒打、本垒打、场内本垒打后的余数
function toInt(value) {
  return Number(value) || 0;
}

export function resolveSingles(row) {
  const extraBases = toInt(row?.doubles) + toInt(row?.triples) + toInt(row?.hr) + toInt(row?.insideParkHr);
  return Math.max(0, toInt(row?.h) - extraBases);
}

// 合计按逐行派生值累加（每行 1B 先夹到 0），保证合计等于表格各单元格之和
export function sumHitTypes(rows) {
  const total = { singles: 0, doubles: 0, triples: 0, hr: 0, insideParkHr: 0 };
  for (const row of rows || []) {
    total.singles += resolveSingles(row);
    total.doubles += toInt(row?.doubles);
    total.triples += toInt(row?.triples);
    total.hr += toInt(row?.hr);
    total.insideParkHr += toInt(row?.insideParkHr);
  }
  return total;
}
