import { useCallback, useEffect, useState } from 'react'
import { Api } from '../api/client'

const KEY = 'sst.network'

/**
 * NFR-1.2 offline-first: the static network (stations + routes + polylines) is cached in
 * localStorage and revalidated with the ETag; when the server is unreachable the cached
 * copy keeps the map, station search and route browsing working.
 */
export function useNetwork() {
  const [network, setNetwork] = useState(() => {
    try { return JSON.parse(localStorage.getItem(KEY) || 'null') } catch { return null }
  })
  const [offline, setOffline] = useState(!navigator.onLine)
  const [loading, setLoading] = useState(!network)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      let cached = null
      try { cached = JSON.parse(localStorage.getItem(KEY) || 'null') } catch { /* ignore */ }
      const res = await Api.network(cached?.etag)
      if (res.status === 304) {
        setOffline(false)
        return
      }
      if (!res.ok) throw new Error('network fetch failed')
      const data = await res.json()
      const snapshot = { ...data, etag: res.headers.get('ETag'), fetchedAt: Date.now() }
      try { localStorage.setItem(KEY, JSON.stringify(snapshot)) } catch { /* ignore */ }
      setNetwork(snapshot)
      setOffline(false)
    } catch {
      setOffline(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    const on = () => refresh()
    const off = () => setOffline(true)
    window.addEventListener('online', on)
    window.addEventListener('offline', off)
    return () => { window.removeEventListener('online', on); window.removeEventListener('offline', off) }
  }, [refresh])

  return { network, offline, loading, refresh }
}
