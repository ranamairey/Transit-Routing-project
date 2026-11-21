import { describe, expect, it } from 'vitest'
import { decodeEndpoint, encodeEndpoint, metersBetween } from '../utils/endpoints.js'

const stationsById = { abc: { id: 'abc', name: 'البرامكة', location: { lat: 33.5, lng: 36.29 } } }

describe('trip links', () => {
  it('round-trips a stop and a map point', () => {
    const stop = { mode: 'station', stationId: 'abc', name: 'البرامكة', point: { lat: 33.5, lng: 36.29 } }
    expect(encodeEndpoint(stop)).toBe('s:abc')
    expect(decodeEndpoint('s:abc', stationsById, 'pin')).toMatchObject({ mode: 'station', stationId: 'abc' })
    const pin = { mode: 'pin', point: { lat: 33.51234, lng: 36.28765 } }
    expect(decodeEndpoint(encodeEndpoint(pin), stationsById, 'pin').point).toEqual({ lat: 33.51234, lng: 36.28765 })
  })
  it('rejects unknown stops and bad coordinates', () => {
    expect(decodeEndpoint('s:nope', stationsById, 'pin')).toBeNull()
    expect(decodeEndpoint('999,1', stationsById, 'pin')).toBeNull()
    expect(decodeEndpoint('abc', stationsById, 'pin')).toBeNull()
  })
  it('measures short distances', () => {
    expect(Math.round(metersBetween({ lat: 33.5, lng: 36.29 }, { lat: 33.501, lng: 36.29 }))).toBe(111)
  })
})
