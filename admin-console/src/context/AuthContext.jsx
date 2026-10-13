import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Api, getToken, setToken } from '../api/client.js'

const AuthContext = createContext(null)
const USER_KEY = 'sst.admin.user'

/** Admin-only auth: a non-admin login is rejected client-side (and by the API on every admin call). */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null') } catch { return null }
  })

  const persist = useCallback((auth) => {
    if (auth) {
      setToken(auth.token)
      const profile = { userId: auth.userId, fullName: auth.fullName, email: auth.email, role: auth.role }
      try { localStorage.setItem(USER_KEY, JSON.stringify(profile)) } catch { /* ignore */ }
      setUser(profile)
    } else {
      setToken(null)
      try { localStorage.removeItem(USER_KEY) } catch { /* ignore */ }
      setUser(null)
    }
  }, [])

  useEffect(() => {
    if (!getToken()) return
    Api.me().then(persist).catch((e) => { if (e.status === 401) persist(null) })
  }, [persist])

  const value = useMemo(() => ({
    user,
    isAdmin: user?.role === 'ROLE_ADMIN',
    login: async (email, password) => {
      const auth = await Api.login(email, password)
      if (auth.role !== 'ROLE_ADMIN') {
        const err = new Error('NOT_ADMIN'); err.code = 'NOT_ADMIN'; throw err
      }
      persist(auth)
    },
    logout: () => persist(null),
  }), [user, persist])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
