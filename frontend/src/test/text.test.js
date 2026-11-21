import { describe, expect, it } from 'vitest'
import { normalizeArabic, searchStations, soundWords } from '../utils/text.js'

const stations = [
  { id: '1', name: 'جسر الرئيس', aliases: 'الرئيس، تحت الجسر' },
  { id: '2', name: 'البرامكة', aliases: 'كراجات البرامكة' },
  { id: '3', name: 'قطنا المركز', nameEn: 'Qatana', aliases: '' },
  { id: '4', name: 'القدم', aliases: '' },
  { id: '5', name: 'سوق الحميدية', aliases: 'الحميدية' },
  { id: '6', name: 'الأحمدية', aliases: '' },
  { id: '7', name: 'ساحة المرجة', aliases: '' },
  { id: '8', name: 'كفر سوسة', aliases: '' },
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

describe('English spellings', () => {
  it('matches English names', () => {
    expect(searchStations(stations, 'qatana')[0].id).toBe('3')
  })
  it('matches Arabic names by sound, whatever the spelling', () => {
    for (const q of ['Baramkeh', 'baramke', 'Al-Baramkeh']) expect(searchStations(stations, q)[0].id).toBe('2')
    for (const q of ['Qadam', 'kadam', 'al-Qadam']) expect(searchStations(stations, q)[0].id).toBe('4')
    expect(searchStations(stations, 'Kafr Souseh')[0].id).toBe('8')
    expect(searchStations(stations, 'Kafarsousa')[0].id).toBe('8')
    expect(searchStations(stations, 'Sahat al-Marjeh')[0].id).toBe('7')
  })
  it('keeps a leading vowel apart from a consonant', () => {
    expect(searchStations(stations, 'Hamidiyeh')[0].id).toBe('5')
    expect(searchStations(stations, 'Ahmadiyeh')[0].id).toBe('6')
  })
  it('reduces both scripts to the same skeleton', () => {
    expect(soundWords('المزة')).toEqual(soundWords('Mezzeh'))
    expect(soundWords('باب توما')).toEqual(soundWords('Bab Touma'))
  })
  it('finds nothing for unrelated text', () => {
    expect(searchStations(stations, 'xyzq')).toEqual([])
  })
})
