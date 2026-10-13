// 电子秩序册（Issue #310）：二次确认工具（取消/关闭返回 false，供 if (!(await confirmAction(...))) return 使用）
import { ElMessageBox } from 'element-plus';

export async function confirmAction(message, title, type = 'warning') {
  try {
    await ElMessageBox.confirm(message, title, { type });
    return true;
  } catch {
    return false;
  }
}
