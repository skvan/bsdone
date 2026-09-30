/*
 * 账号权限重构（批次 2，Task 2.1）：管理上下文持有器（ScopeContextHolder）。
 *
 * 职责：在单次请求线程内持有「是否处于管理上下文」标志。
 *  - 前端在后台/管理视图请求时自动附加请求头 X-Scope-Context: manage（前端批 4 实现）；
 *  - ApiPermissionFilter 解析该头并写入本持有器，供后续 ScopeQuerySupport（T2.2）判定：
 *    管理上下文读 = 窄域过滤；门户只读 = 宽读公开数据。
 *
 * 语义与安全默认：仅当请求头「去除首尾空白后忽略大小写等于 manage」时为 true，其余一律为 false。
 * 漏带该头时「按门户语义宽读公开数据、不暴露私密数据」为预期语义，由 ScopeQuerySupport 等消费方保证（非本持有器既有事实）。
 *
 * 生命周期：由 ApiPermissionFilter 在进入业务链前 setManage(...)，并在 finally 中 clear()，
 * 避免线程池复用导致的线程状态泄漏（异常路径 / 早退路径均经 finally 回收）。
 */
package com.bsball.core;

public final class ScopeContextHolder {
    private static final ThreadLocal<Boolean> MANAGE_CONTEXT = new ThreadLocal();

    private ScopeContextHolder() {
    }

    public static void setManage(boolean manage) {
        if (manage) {
            MANAGE_CONTEXT.set(Boolean.TRUE);
        } else {
            MANAGE_CONTEXT.remove();
        }
    }

    public static boolean isManage() {
        return Boolean.TRUE.equals(MANAGE_CONTEXT.get());
    }

    public static void clear() {
        MANAGE_CONTEXT.remove();
    }
}
