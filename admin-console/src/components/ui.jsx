import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'

/* ---------- toast ---------- */
const ToastContext = createContext(() => {})

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)
  const timer = useRef()
  const notify = useCallback((message, kind = 'info') => {
    setToast({ message, kind })
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setToast(null), 3200)
  }, [])
  return (
    <ToastContext.Provider value={notify}>
      {children}
      <div className={`toast ${toast ? 'show' : ''} ${toast?.kind === 'error' ? 'error' : ''}`} role="status">{toast?.message}</div>
    </ToastContext.Provider>
  )
}
export const useToast = () => useContext(ToastContext)

/* ---------- modal ---------- */
export function Modal({ title, subtitle, onClose, children, wide }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="modal-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true">
        <div className="modal-head">
          <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
          <button className="close" onClick={onClose} aria-label="close">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}

/** Promise-based confirm dialog: const ok = await confirm({ title, message, danger }) */
const ConfirmContext = createContext(() => Promise.resolve(false))
export function ConfirmProvider({ children }) {
  const { t } = useI18n()
  const [state, setState] = useState(null)
  const confirm = useCallback((opts) => new Promise((resolve) => setState({ ...opts, resolve })), [])
  const close = (v) => { state?.resolve(v); setState(null) }
  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state && (
        <Modal title={state.title || t('common.confirm')} subtitle={state.message} onClose={() => close(false)}>
          <div className="modal-buttons">
            <button className="button secondary" onClick={() => close(false)}>{t('common.cancel')}</button>
            <button className={`button ${state.danger ? 'danger' : ''}`} onClick={() => close(true)} autoFocus>{state.okLabel || t('common.ok')}</button>
          </div>
        </Modal>
      )}
    </ConfirmContext.Provider>
  )
}
export const useConfirm = () => useContext(ConfirmContext)

/* ---------- page pieces ---------- */
export function PageHead({ eyebrow, title, description, action }) {
  return (
    <div className="page-head">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="description">{description}</p>}
      </div>
      {action}
    </div>
  )
}

export function Stat({ label, value, detail, icon, tone }) {
  return (
    <div className="stat">
      <div><small>{label}</small><h2>{value ?? '—'}</h2>{detail && <div className="detail">{detail}</div>}</div>
      <div className={`stat-ico ${tone || ''}`}>{icon}</div>
    </div>
  )
}

export function Panel({ title, count, subtitle, tools, children, footer, body }) {
  return (
    <section className="panel">
      {(title || tools) && (
        <div className="panel-top">
          <div>{title && <h3>{title} {count != null && <span className="n">({count})</span>}</h3>}{subtitle && <p>{subtitle}</p>}</div>
          {tools && <div className="tools">{tools}</div>}
        </div>
      )}
      {body ? <div className="panel-body">{children}</div> : children}
      {footer}
    </section>
  )
}

export function SearchBox({ value, onChange, placeholder, id }) {
  return (
    <div className="search-box">
      <span className="magnify">⌕</span>
      <input id={id} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
    </div>
  )
}

export function TableFooter({ page, size, total, onPage, label }) {
  const { t } = useI18n()
  const pages = Math.max(1, Math.ceil(total / size))
  const from = total === 0 ? 0 : page * size + 1
  const to = Math.min(total, (page + 1) * size)
  return (
    <div className="footer">
      <span>{label || t('common.showing', { from, to, total })}</span>
      <div className="pager">
        <button disabled={page <= 0} onClick={() => onPage(page - 1)}>{t('common.prev')}</button>
        <button className="current">{page + 1}</button>
        <button disabled={page >= pages - 1} onClick={() => onPage(page + 1)}>{t('common.next')}</button>
      </div>
    </div>
  )
}

export function Initials({ name, tone }) {
  const text = useMemo(() => (name || '?').trim().split(/\s+/).slice(0, 2).map((x) => x[0]).join('').toUpperCase(), [name])
  return <div className={`user-initials ${tone || ''}`}>{text}</div>
}

export const toneFor = (key) => ['', 'mint', 'orange'][(key || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 3]
