/** Station and line names follow the interface language when an English name exists. */
export const nameOf = (item, lang) => (item ? (lang === 'en' && item.nameEn ? item.nameEn : item.name) : '')
/** The other-language name, shown as a secondary line. */
export const altNameOf = (item, lang) => (item ? (lang === 'en' ? (item.nameEn ? item.name : null) : item.nameEn || null) : null)
