/** Arabic-aware normalisation (mirrors StationSearchService on the backend). */
export function normalizeArabic(text) {
  if (!text) return ''
  return text
    .normalize('NFKC')
    .toLowerCase()
    .trim()
    .replace(/[ً-ْٰـ]/g, '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/[ىی]/g, 'ي')
    .replace(/[ءئؤ]/g, '')
    .replace(/(^|\s)ال/g, '$1')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

// Sound-alike keys let Latin spellings find Arabic names ("Baramkeh" → البرامكة, "Kadam" → القدم):
// both scripts are reduced to a consonant skeleton with the letters Syrians use interchangeably merged.
const AR_SOUNDS = {
  ب: 'b', پ: 'b', ت: 't', ط: 't', ث: 's', س: 's', ص: 's', ش: 's', ج: 'j', چ: 'j', ح: 'h', ه: 'h',
  خ: 'k', ق: 'k', ك: 'k', د: 'd', ض: 'd', ذ: 'z', ز: 'z', ظ: 'z', ر: 'r', غ: 'g', گ: 'g', ف: 'f',
  ڤ: 'f', ل: 'l', م: 'm', ن: 'n',
}
const LATIN_ARTICLE = /(^|\s)(al|el|a[dnrstz]|ash)-/g

function collapse(key) {
  return key.replace(/(.)\1+/g, '$1')
}

/** Consonant skeleton of each word of a name or query, in Arabic or Latin script. */
export function soundWords(text) {
  if (!text) return []
  if (/\p{Script=Arabic}/u.test(text)) {
    return normalizeArabic(text).split(' ')
      .map((w) => (/^[اع]/.test(w) ? "'" : '') + collapse([...w.replace(/ه$/, '')].map((c) => AR_SOUNDS[c] ?? '').join('')))
      .filter((w) => w && w !== "'")
  }
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
    .replace(/['’`ʿʾ]/g, '')
    .replace(LATIN_ARTICLE, '$1')
    .replace(/[^a-z0-9]+/g, ' ')
    // "Sahat al-Marjeh": a taa marbuta followed by another word is spoken as -at/-et.
    .replace(/([^aeiou\s][ae])t(?= )/g, '$1')
    .trim().split(' ')
    .filter((w) => w && w !== 'al' && w !== 'el')
    .map((w) => (/^[aeiou]/.test(w) ? "'" : '') + collapse(w
      .replace(/sh|ch|th/g, 's').replace(/dh/g, 'z').replace(/kh/g, 'k').replace(/gh/g, 'g').replace(/ph/g, 'f')
      .replace(/h$/, '')
      .replace(/[qc]/g, 'k').replace(/x/g, 'ks').replace(/v/g, 'f').replace(/p/g, 'b')
      .replace(/[aeiouyw]/g, '')))
    .filter((w) => w && w !== "'")
}

const termCache = new WeakMap()

function stationTerms(s) {
  let terms = termCache.get(s)
  if (!terms) {
    const aliases = s.aliases ? s.aliases.split(/[،,;/]/) : []
    const named = [[s.name, 100], [s.nameEn, 100], ...aliases.map((a) => [a, 80])].filter(([t]) => t && t.trim())
    terms = named.map(([text, base]) => {
      const words = soundWords(text)
      return { text: normalizeArabic(text), base, words, key: words.join('') }
    })
    termCache.set(s, terms)
  }
  return terms
}

function soundScore(term, qWords, qKey) {
  if (!term.key || !qKey) return 0
  const cut = term.base === 100 ? 30 : 40
  if (term.key === qKey) return term.base - cut
  if (qWords.every((w) => term.words.includes(w))) return term.base - cut - 3
  if (term.key.startsWith(qKey)) return term.base - cut - 5
  if (qWords.every((w) => term.words.some((tw) => tw.startsWith(w)))) return term.base - cut - 10
  if (qKey.length >= 4 && term.key.includes(qKey)) return term.base - cut - 20
  return 0
}

/**
 * Station search over Arabic names, English names and local aliases (works offline on the cached
 * network). Latin queries also match Arabic names by sound, so riders can type the usual spellings.
 */
export function searchStations(stations, query, limit = 8) {
  const q = normalizeArabic(query)
  if (!q) return []
  const latin = /[a-z]/.test(q)
  const qWords = latin ? soundWords(query) : []
  const qKey = qWords.join('')
  const scored = []
  for (const s of stations) {
    let best = 0
    for (const term of stationTerms(s)) {
      const { text, base } = term
      if (text === q) best = Math.max(best, base)
      else if (text.startsWith(q)) best = Math.max(best, base - 10)
      else if (text.includes(q)) best = Math.max(best, base - 20)
      else if (q.split(' ').every((w) => text.includes(w))) best = Math.max(best, base - 30)
      if (latin && qKey.length >= 2) best = Math.max(best, soundScore(term, qWords, qKey))
    }
    if (best > 0) scored.push([best, s])
  }
  return scored
    .sort((a, b) => b[0] - a[0] || a[1].name.length - b[1].name.length)
    .slice(0, limit)
    .map(([, s]) => s)
}

export const REPORT_TYPE_KEYS = ['CROWDING', 'DELAY', 'ROUTE_CHANGE', 'FARE_CHANGE', 'OTHER']
export const ROUTE_STATUS_KEYS = ['NORMAL', 'CROWDED', 'DIVERTED', 'DELAYED', 'FARE_UPDATED']
export const VEHICLE_KEYS = ['SERVIS', 'BUS']
export const STATION_KIND_KEYS = ['STOP', 'POINT', 'TERMINAL', 'TRANSFER_HUB', 'HUB', 'GARAGE']
