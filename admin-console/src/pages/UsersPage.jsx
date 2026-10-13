import { useCallback, useEffect, useState } from 'react'
import { Api } from '../api/client.js'
import Select from '../components/Select.jsx'
import { Initials, Modal, PageHead, Panel, SearchBox, Stat, TableFooter, toneFor, useConfirm, useToast } from '../components/ui.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

const SIZE = 25
const EMPTY = { fullName: '', email: '', password: '', role: 'ROLE_USER' }

export default function UsersPage() {
  const { t } = useI18n()
  const { user: me } = useAuth()
  const notify = useToast()
  const confirm = useConfirm()
  const [data, setData] = useState({ items: [], total: 0 })
  const [counts, setCounts] = useState({ total: 0, admins: 0 })
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [role, setRole] = useState('')
  const [modal, setModal] = useState(null) // { mode: 'add' } | { mode: 'reset', user }
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      const [list, all, admins] = await Promise.all([
        Api.users.list({ q, role, page, size: SIZE }), Api.users.list({ size: 1 }), Api.users.list({ role: 'ROLE_ADMIN', size: 1 }),
      ])
      setData(list); setCounts({ total: all.total, admins: admins.total }); setError('')
    } catch (e) { setError(e.message) }
  }, [q, role, page])
  useEffect(() => { load() }, [load])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const open = (m) => { setModal(m); setForm(EMPTY); setError('') }
  const close = () => setModal(null)

  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    try {
      if (modal.mode === 'add') { await Api.users.create(form); notify(t('users.created')) }
      else { await Api.users.update(modal.user.id, { newPassword: form.password }); notify(t('users.passwordUpdated')) }
      close(); await load()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const changeRole = async (u, newRole) => {
    try { await Api.users.update(u.id, { role: newRole }); notify(t('users.roleUpdated', { name: u.fullName })); await load() } catch (err) { notify(err.message, 'error') }
  }
  const remove = async (u) => {
    if (!(await confirm({ title: t('common.delete'), message: t('users.confirmDelete', { email: u.email }), danger: true, okLabel: t('common.delete') }))) return
    try { await Api.users.remove(u.id); notify(t('users.deletedMsg')); await load() } catch (err) { notify(err.message, 'error') }
  }

  return (
    <div>
      <PageHead eyebrow={t('users.eyebrow')} title={t('users.title')} description={t('users.desc')}
        action={<button className="button" onClick={() => open({ mode: 'add' })}><span style={{ fontSize: 18, lineHeight: 0 }}>＋</span> {t('users.addNew')}</button>} />
      <section className="stats">
        <Stat label={t('users.total')} value={counts.total} detail={t('users.registered')} icon="♧" />
        <Stat label={t('users.admins')} value={counts.admins} detail={t('users.adminsDetail')} icon="✓" tone="mint" />
        <Stat label={t('users.riders')} value={counts.total - counts.admins} detail={t('users.ridersDetail')} icon="⌁" tone="orange" />
      </section>
      <Panel title={t('users.all')} count={counts.total} subtitle={t('users.allDesc')}
        tools={<>
          <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0) }} placeholder={t('users.searchPlaceholder')} />
          <Select value={role} onChange={(v) => { setRole(v); setPage(0) }} style={{ width: 160 }} ariaLabel={t('users.role')}
            options={[{ value: '', label: t('users.allRoles') }, { value: 'ROLE_ADMIN', label: t('users.ROLE_ADMIN') }, { value: 'ROLE_USER', label: t('users.ROLE_USER') }]} />
        </>}
        footer={<TableFooter page={page} size={SIZE} total={data.total} onPage={setPage} />}>
        {error && !modal && <p className="error" style={{ padding: '0 25px' }}>{error}</p>}
        <div className="table-wrap">
          <table>
            <thead><tr><th>{t('users.user')}</th><th>{t('users.email')}</th><th>{t('users.role')}</th><th>{t('users.status')}</th><th style={{ width: 210 }}>{t('common.actions')}</th></tr></thead>
            <tbody>
              {data.items.map((u) => {
                const own = u.email === me?.email
                return (
                  <tr key={u.id}>
                    <td><div className="user-cell"><Initials name={u.fullName} tone={toneFor(u.id)} /><div><div className="user-name">{u.fullName}{own && <span className="tag-you">{t('users.you')}</span>}</div></div></div></td>
                    <td dir="ltr" style={{ textAlign: 'start' }}>{u.email}</td>
                    <td>{own ? <span className="badge-role">{t('users.ROLE_ADMIN')}</span> : (
                      <Select small value={u.role} onChange={(v) => changeRole(u, v)} style={{ width: 130 }} ariaLabel={t('users.role')}
                        options={[{ value: 'ROLE_USER', label: t('users.ROLE_USER') }, { value: 'ROLE_ADMIN', label: t('users.ROLE_ADMIN') }]} />)}</td>
                    <td><span className="status-chip">{t('common.active')}</span></td>
                    <td><div className="actions">
                      <button className="action" onClick={() => open({ mode: 'reset', user: u })}>{t('users.resetPassword')}</button>
                      <button className="action delete" disabled={own} title={own ? t('users.cannotDeleteSelf') : undefined} onClick={() => remove(u)}>{t('common.delete')}</button>
                    </div></td>
                  </tr>
                )
              })}
              {data.items.length === 0 && <tr><td colSpan={5} className="empty">{t('users.noMatch')}</td></tr>}
            </tbody>
          </table>
        </div>
      </Panel>

      {modal && (
        <Modal title={modal.mode === 'add' ? t('users.addNew') : t('users.resetPassword')}
          subtitle={modal.mode === 'add' ? t('users.createSub') : t('users.resetSub', { name: modal.user.fullName })} onClose={close}>
          <form onSubmit={submit}>
            {modal.mode === 'add' && (<>
              <label className="field-label" htmlFor="u-name">{t('users.fullName')}</label>
              <input className="field" id="u-name" required autoFocus placeholder={t('users.namePlaceholder')} value={form.fullName} onChange={set('fullName')} />
              <label className="field-label" htmlFor="u-email">{t('users.email')}</label>
              <input className="field" id="u-email" type="email" required placeholder={t('users.emailPlaceholder')} value={form.email} onChange={set('email')} dir="ltr" />
            </>)}
            <label className="field-label" htmlFor="u-pass">{t('users.password')}</label>
            <input className="field" id="u-pass" type="password" minLength={8} required autoFocus={modal.mode === 'reset'} placeholder={t('users.passwordPlaceholder')} value={form.password} onChange={set('password')} autoComplete="new-password" />
            {modal.mode === 'add' && (<>
              <label className="field-label" htmlFor="u-role">{t('users.role')}</label>
              <div style={{ marginBottom: 18 }}><Select value={form.role} onChange={(v) => setForm((f) => ({ ...f, role: v }))} ariaLabel={t('users.role')}
                options={[{ value: 'ROLE_USER', label: t('users.ROLE_USER') }, { value: 'ROLE_ADMIN', label: t('users.ROLE_ADMIN') }]} /></div>
            </>)}
            {error && <p className="error" style={{ marginTop: -8 }}>{error}</p>}
            <div className="modal-buttons">
              <button type="button" className="button secondary" onClick={close}>{t('common.cancel')}</button>
              <button type="submit" className="button" disabled={busy}>{modal.mode === 'add' ? t('users.createBtn') : t('users.updatePassword')}</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
