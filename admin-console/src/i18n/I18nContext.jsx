import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { translations } from './translations.js'

const I18nContext = createContext(null)
const LANG_KEY = 'sst.admin.lang'
const THEME_KEY = 'sst.admin.theme'
const systemDark = () => window.matchMedia?.('(prefers-color-scheme: dark)').matches

function lookup(dict, path) {
  return path.split('.').reduce((o, k) => (o && o[k] !== undefined ? o[k] : undefined), dict)
}

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(() => {
    try {
      const saved = localStorage.getItem(LANG_KEY)
      if (saved && translations[saved]) return saved
    } catch { /* ignore */ }
    return navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'ar'
  })

  const [theme, setThemeState] = useState(() => { try { return localStorage.getItem(THEME_KEY) || 'auto' } catch { return 'auto' } })
  const [sysDark, setSysDark] = useState(systemDark())
  useEffect(() => {
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)')
    const on = (e) => setSysDark(e.matches)
    mq?.addEventListener?.('change', on)
    return () => mq?.removeEventListener?.('change', on)
  }, [])
  const dark = theme === 'dark' || (theme === 'auto' && sysDark)
  const setTheme = useCallback((next) => { setThemeState(next); try { localStorage.setItem(THEME_KEY, next) } catch { /* ignore */ } }, [])
  useEffect(() => { document.documentElement.dataset.theme = dark ? 'dark' : 'light' }, [dark])

  const setLang = useCallback((next) => {
    setLangState(next)
    try { localStorage.setItem(LANG_KEY, next) } catch { /* ignore */ }
  }, [])

  const dict = translations[lang]

  useEffect(() => {
    document.documentElement.lang = lang
    document.documentElement.dir = dict.dir
    document.title = dict.appName
  }, [lang, dict])

  const value = useMemo(() => {
    /** t('planner.title') or t('trips.title', { n: 3 }); falls back to the key. */
    const t = (key, vars) => {
      let text = lookup(dict, key)
      if (text === undefined) text = lookup(translations.ar, key) ?? key
      if (vars) for (const [k, v] of Object.entries(vars)) text = text.replace(`{${k}}`, v)
      return text
    }
    const locale = lang === 'ar' ? 'ar-SY' : 'en'
    return {
      lang, dir: dict.dir, setLang, t, locale, theme, setTheme, dark,
      cycleTheme: () => setTheme(theme === 'auto' ? 'dark' : theme === 'dark' ? 'light' : 'auto'),
      toggle: () => setLang(lang === 'ar' ? 'en' : 'ar'),
      formatDuration: (seconds) => {
        const m = Math.round(seconds / 60)
        if (m < 60) return `${m} ${t('units.min')}`
        return `${Math.floor(m / 60)} ${t('units.hour')} ${m % 60} ${t('units.min')}`
      },
      formatMeters: (m) => (m >= 1000 ? `${(m / 1000).toFixed(1)} ${t('units.km')}` : `${Math.round(m)} ${t('units.m')}`),
      formatFare: (fare) => (fare == null ? '' : `${Number(fare).toLocaleString(locale)} ${t('units.currency')}`),
      timeAgo: (iso) => {
        const diff = (Date.now() - new Date(iso).getTime()) / 60000
        if (diff < 1) return t('units.now')
        if (diff < 60) return t('units.agoMin', { n: Math.round(diff) })
        if (diff < 60 * 24) return t('units.agoHour', { n: Math.round(diff / 60) })
        return t('units.agoDay', { n: Math.round(diff / 1440) })
      },
    }
  }, [lang, dict, setLang, theme, setTheme, dark])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n() {
  return useContext(I18nContext)
}
