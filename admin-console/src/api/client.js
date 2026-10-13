const BASE = (import.meta.env.VITE_API_BASE || '') + '/api/v1'
const TOKEN_KEY = 'sst.admin.token'

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

export async function api(path, { method = 'GET', body, headers = {} } = {}) {
  const token = getToken()
  const res = await fetch(BASE + path, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (res.status === 204) return null
  const text = await res.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = { error: text } }
  if (!res.ok) throw new ApiError(res.status, data?.error || `HTTP ${res.status}`)
  return data
}

const qs = (params) => new URLSearchParams(Object.fromEntries(Object.entries(params || {}).filter(([, v]) => v !== '' && v != null))).toString()

export const Api = {
  login: (email, password) => api('/auth/login', { method: 'POST', body: { email, password } }),
  me: () => api('/auth/me'),
  health: () => api('/health'),
  network: () => api('/network'),
  stats: () => api('/admin/stats'),
  stations: {
    create: (s) => api('/admin/stations', { method: 'POST', body: s }),
    update: (id, s) => api(`/admin/stations/${id}`, { method: 'PUT', body: s }),
    remove: (id) => api(`/admin/stations/${id}`, { method: 'DELETE' }),
  },
  routes: {
    create: (r) => api('/admin/routes', { method: 'POST', body: r }),
    update: (id, r) => api(`/admin/routes/${id}`, { method: 'PUT', body: r }),
    status: (id, body) => api(`/admin/routes/${id}/status`, { method: 'PATCH', body }),
    remove: (id) => api(`/admin/routes/${id}`, { method: 'DELETE' }),
  },
  reports: {
    list: (params) => api('/admin/reports?' + qs(params)),
    summary: () => api('/admin/reports/summary'),
    approve: (id) => api(`/admin/reports/${id}/approve`, { method: 'POST' }),
    reject: (id) => api(`/admin/reports/${id}/reject`, { method: 'POST' }),
    remove: (id) => api(`/admin/reports/${id}`, { method: 'DELETE' }),
  },
  users: {
    list: (params) => api('/admin/users?' + qs(params)),
    create: (u) => api('/admin/users', { method: 'POST', body: u }),
    update: (id, u) => api(`/admin/users/${id}`, { method: 'PATCH', body: u }),
    remove: (id) => api(`/admin/users/${id}`, { method: 'DELETE' }),
  },
  importNetwork: (body) => api('/admin/network/import', { method: 'POST', body }),
  exportNetwork: () => api('/admin/network/export'),
}
