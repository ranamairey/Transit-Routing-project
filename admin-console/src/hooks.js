import { useCallback, useEffect, useState } from 'react'
import { Api } from './api/client.js'

/** Network (stations + routes) shared by the console pages; refresh after any change. */
export function useNetwork() {
  const [network, setNetwork] = useState(null)
  const [error, setError] = useState('')
  const refresh = useCallback(async () => {
    try { setNetwork(await Api.network()); setError('') } catch (e) { setError(e.message) }
  }, [])
  useEffect(() => { refresh() }, [refresh])
  return { network, stations: network?.stations || [], routes: network?.routes || [], refresh, error }
}

/** Pagination helper for PageDto responses. */
export function usePage(initialSize = 25) {
  const [page, setPage] = useState(0)
  const [size] = useState(initialSize)
  return { page, size, setPage, reset: () => setPage(0) }
}
