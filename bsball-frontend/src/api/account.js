// 账户与认证 API —— 行为与端点移植自编译产物 account chunk（B0 契约）
import { get, post, put } from './request';

export const accountApi = {
  legalDocs: () => get('/api/auth/legal-docs'),
  sendSms: (data) => post('/api/auth/sms/send', data),
  register: (data) => post('/api/auth/register', data),
  loginByPhone: (data) => post('/api/auth/login-by-phone', data),
  getProfile: () => get('/api/account/profile'),
  // 当前用户信息（App 启动时恢复登录态；编译产物 authApi.me）
  me: () => get('/api/auth/me'),
  updateProfile: (data) => put('/api/account/profile', data),
  sendEmailCode: (data) => post('/api/account/email/send-code', data),
  bindEmail: (data) => post('/api/account/email/bind', data),
  changePhone: (data) => post('/api/account/phone/change', data),
  changePassword: (data) => post('/api/account/password/change', data),
  forgotPassword: (data) => post('/api/auth/password/forgot', data),
  resetPassword: (data) => post('/api/auth/password/reset', data),
  submitPlayerClaim: (data) => post('/api/account/player-claim', data),
  cancelPlayerClaim: (id) => post(`/api/account/player-claims/${id}/cancel`, {}),
  myPlayerClaims: () => get('/api/account/player-claims'),
  pendingPlayerClaims: (params) => {
    const query = new URLSearchParams();
    if (params?.page != null) query.set('page', String(params.page));
    if (params?.pageSize != null) query.set('pageSize', String(params.pageSize));
    if (params?.keyword) query.set('keyword', params.keyword);
    if (params?.reviewerType) query.set('reviewerType', params.reviewerType);
    if (params?.status) query.set('status', params.status);
    const qs = query.toString();
    return get(`/api/account/player-claims/pending${qs ? `?${qs}` : ''}`);
  },
  approvePlayerClaim: (id) => post(`/api/account/player-claims/${id}/approve`, {}),
  rejectPlayerClaim: (id, reason) => post(`/api/account/player-claims/${id}/reject`, { reason }),
  createClaimInvite: (teamId, data) => post(`/api/team/${teamId}/player-claim-invite`, data ?? {}),
  getClaimInvite: (token) => get(`/api/portal/player-claim-invite/${token}`),
  claimViaInvite: (token, data) => post(`/api/portal/player-claim-invite/${token}/claim`, data ?? {})
};
