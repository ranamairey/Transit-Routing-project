import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react'

/**
 * Themed dropdown replacing <select>. options: [{ value, label }]. Keyboard: arrows / Enter / Esc.
 * On narrow screens the menu opens as a bottom sheet.
 */
export default function Select({ value, onChange, options, placeholder = '—', className = '', small = false, disabled = false, ariaLabel, style }) {
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const [up, setUp] = useState(false)
  const root = useRef(null)
  const listRef = useRef(null)
  const id = useId()
  const current = options.find((o) => String(o.value) === String(value))

  useEffect(() => {
    if (!open) return
    const onDoc = (e) => { if (root.current && !root.current.contains(e.target)) setOpen(false) }
    const onKey = (e) => { if (e.key === 'Escape') setOpen(false) }
    document.addEventListener('mousedown', onDoc)
    document.addEventListener('touchstart', onDoc)
    document.addEventListener('keydown', onKey)
    return () => { document.removeEventListener('mousedown', onDoc); document.removeEventListener('touchstart', onDoc); document.removeEventListener('keydown', onKey) }
  }, [open])

  useLayoutEffect(() => {
    if (!open || !root.current) return
    const r = root.current.getBoundingClientRect()
    setUp(window.innerHeight - r.bottom < 260 && r.top > 260)
    const i = options.findIndex((o) => String(o.value) === String(value))
    setActive(i)
    const el = listRef.current?.children?.[i]
    el?.scrollIntoView?.({ block: 'nearest' })
  }, [open, options, value])

  const choose = (o) => { onChange(o.value); setOpen(false) }
  const onKeyDown = (e) => {
    if (disabled) return
    if (!open && (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); setOpen(true); return }
    if (!open) return
    if (e.key === 'ArrowDown') { e.preventDefault(); setActive((a) => Math.min(options.length - 1, a + 1)) }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setActive((a) => Math.max(0, a - 1)) }
    else if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); if (options[active]) choose(options[active]) }
    else if (e.key === 'Tab') setOpen(false)
  }

  return (
    <div ref={root} className={`sel ${small ? 'sel-small' : ''} ${open ? 'open' : ''} ${disabled ? 'disabled' : ''} ${className}`} style={style}>
      <button type="button" className="sel-btn" aria-haspopup="listbox" aria-expanded={open} aria-controls={id} aria-label={ariaLabel}
        disabled={disabled} onClick={() => setOpen(!open)} onKeyDown={onKeyDown}>
        <span className={`sel-value ${current ? '' : 'placeholder'}`}>{current ? current.label : placeholder}</span>
        <span className="sel-arrow" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 20 20" fill="none"><path d="M5 8l5 5 5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
        </span>
      </button>
      {open && (
        <>
          <div className="sel-backdrop" onClick={() => setOpen(false)} />
          <ul ref={listRef} id={id} role="listbox" className={`sel-menu ${up ? 'up' : ''}`} onKeyDown={onKeyDown} tabIndex={-1}>
            {options.map((o, i) => (
              <li key={String(o.value)} role="option" aria-selected={current?.value === o.value}
                className={`sel-opt ${current?.value === o.value ? 'selected' : ''} ${active === i ? 'active' : ''}`}
                onMouseEnter={() => setActive(i)} onClick={() => choose(o)}>
                <span>{o.label}</span>
                {current?.value === o.value && <span className="sel-check">✓</span>}
              </li>
            ))}
            {options.length === 0 && <li className="sel-opt empty">—</li>}
          </ul>
        </>
      )}
    </div>
  )
}
