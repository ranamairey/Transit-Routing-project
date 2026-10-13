import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Api, getToken, setToken } from '../api/client'

const AuthContext = createContext(null)
const USER_KEY = 'sst.user'

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

  // Revalidate the stored token once on start (silently logs out when expired).
  useEffect(() => {
    if (!getToken()) return
    Api.me().then(persist).catch((e) => { if (e.status === 401) persist(null) })
  }, [persist])

  const value = useMemo(() => ({
    user,
    isAdmin: user?.role === 'ROLE_ADMIN',
    login: async (email, password) => persist(await Api.login(email, password)),
    register: async (fullName, email, password) => persist(await Api.register(fullName, email, password)),
    logout: () => persist(null),
  }), [user, persist])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
