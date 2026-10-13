// 电子秩序册（Issue #310）：参赛状态展示映射（EventTeams / TeamEventRegistrations 共用）
const STATUS_I18N_KEYS = {
  invited: 'orderBook.status.invited',
  confirmed: 'orderBook.status.confirmed',
  declined: 'orderBook.status.declined'
};

const STATUS_TAG_TYPES = {
  invited: 'info',
  confirmed: 'success',
  declined: 'danger'
};

/** 状态 → 展示文案（未知状态原样返回，空值回落 "-"）。 */
export function displayStatus(status, t) {
  const key = STATUS_I18N_KEYS[status];
  return key ? t(key) : status || '-';
}

/** 状态 → el-tag 类型。 */
export function statusTagType(status) {
  return STATUS_TAG_TYPES[status] ?? 'info';
}
