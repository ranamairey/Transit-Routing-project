const BASE = (import.meta.env.VITE_API_BASE || '') + '/api/v1'
const TOKEN_KEY = 'sst.token'
const LANG_KEY = 'sst.lang'
const currentLang = () => { try { return localStorage.getItem(LANG_KEY) || 'ar' } catch { return 'ar' } }

export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
}
export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch { /* ignore */ }
}

export class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status }
}

export async function api(path, { method = 'GET', body, headers = {}, raw = false } = {}) {
  const token = getToken()
  const res = await fetch(BASE + path, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      'Accept-Language': currentLang(),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (raw) return res
  if (res.status === 204) return null
  const text = await res.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = { error: text } }
  if (!res.ok) throw new ApiError(res.status, data?.error || `خطأ ${res.status}`)
  return data
}

export const Api = {
  login: (email, password) => api('/auth/login', { method: 'POST', body: { email, password } }),
  register: (fullName, email, password) => api('/auth/register', { method: 'POST', body: { fullName, email, password } }),
  me: () => api('/auth/me'),
  forgotPassword: (email) => api('/auth/forgot-password', { method: 'POST', body: { email } }),
  resetPassword: (token, newPassword) => api('/auth/reset-password', { method: 'POST', body: { token, newPassword } }),
  verifyEmail: (token) => api('/auth/verify-email', { method: 'POST', body: { token } }),
  resendVerification: () => api('/auth/resend-verification', { method: 'POST' }),
  network: (etag) => api('/network', { raw: true, headers: etag ? { 'If-None-Match': etag } : {} }),
  searchStations: (q) => api(`/stations/search?q=${encodeURIComponent(q)}`),
  routing: (request) => api('/routing/search', { method: 'POST', body: request }),
  recentReports: (routeId) => api(`/reports?routeId=${encodeURIComponent(routeId)}`),
  submitReport: (report) => api('/reports', { method: 'POST', body: report }),
  admin: {
    createStation: (s) => api('/admin/stations', { method: 'POST', body: s }),
    updateStation: (id, s) => api(`/admin/stations/${id}`, { method: 'PUT', body: s }),
    deleteStation: (id) => api(`/admin/stations/${id}`, { method: 'DELETE' }),
    createRoute: (r) => api('/admin/routes', { method: 'POST', body: r }),
    updateRoute: (id, r) => api(`/admin/routes/${id}`, { method: 'PUT', body: r }),
    updateRouteStatus: (id, body) => api(`/admin/routes/${id}/status`, { method: 'PATCH', body }),
    deleteRoute: (id) => api(`/admin/routes/${id}`, { method: 'DELETE' }),
    reports: (params) => api('/admin/reports?' + new URLSearchParams(Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null)))),
    summary: () => api('/admin/reports/summary'),
    approve: (id) => api(`/admin/reports/${id}/approve`, { method: 'POST' }),
    reject: (id) => api(`/admin/reports/${id}/reject`, { method: 'POST' }),
    deleteReport: (id) => api(`/admin/reports/${id}`, { method: 'DELETE' }),
  },
}
