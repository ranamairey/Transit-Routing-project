import { describe, expect, it } from 'vitest'
import { normalizeArabic, searchStations } from '../utils/text.js'

const stations = [
  { id: '1', name: 'جسر الرئيس', aliases: 'الرئيس، تحت الجسر' },
  { id: '2', name: 'البرامكة', aliases: 'كراجات البرامكة' },
  { id: '3', name: 'قطنا المركز', nameEn: 'Qatana', aliases: '' },
]

describe('normalizeArabic', () => {
  it('unifies hamza, taa marbuta and the definite article', () => {
    expect(normalizeArabic('جسر الرئيس')).toBe(normalizeArabic('جسر الرءيس'))
    expect(normalizeArabic('البرامكة')).toBe(normalizeArabic('برامكه'))
    expect(normalizeArabic('  مَزّة  ')).toBe('مزه')
  })
})

describe('searchStations', () => {
  it('ranks exact names above alias matches', () => {
    expect(searchStations(stations, 'الجسر').map((s) => s.id)).toEqual(['1'])
    expect(searchStations(stations, 'برامكه')[0].id).toBe('2')
  })
  it('returns nothing for an empty query', () => {
    expect(searchStations(stations, '   ')).toEqual([])
  })
})
