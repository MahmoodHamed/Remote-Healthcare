import { useEffect, useMemo, useState } from 'react'
import { api } from '../../utils/api'

const roles = ['Admin', 'Doctor', 'Patient', 'Relative']

export default function AdminUsers() {
  const [users, setUsers] = useState([])
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', role: 'Patient' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const data = await api.get('/api/admin/users')
      setUsers(data ?? [])
    } catch (err) {
      setError(err.message || 'Could not load users.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return users
    return users.filter((u) =>
      [u.fullName, u.email, u.phone, u.role]
        .filter(Boolean)
        .some((field) => field.toLowerCase().includes(q)),
    )
  }, [users, search])

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setFeedback('')
    try {
      await api.post('/api/admin/users', form)
      setFeedback('User created successfully.')
      setShowCreate(false)
      setForm({ fullName: '', email: '', phone: '', password: '', role: 'Patient' })
      load()
    } catch (err) {
      setError(err.message || 'Could not create user.')
    }
  }

  const toggle = async (user) => {
    try {
      await api.put(`/api/admin/users/${user.id}`, {
        fullName: user.fullName,
        phone: user.phone,
        role: user.role,
        isActive: !user.isActive,
      })
      load()
    } catch (err) {
      setError(err.message || 'Could not update user.')
    }
  }

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <h2>All users</h2>
            <span className="muted">{loading ? 'Loading…' : `${filtered.length} of ${users.length} accounts`}</span>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <input
              type="search"
              placeholder="Search…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ minWidth: '220px', padding: '0.55rem 0.85rem', borderRadius: 10, border: '1px solid var(--line-strong)' }}
            />
            <button type="button" className="btn btn-primary btn-sm" onClick={() => setShowCreate((v) => !v)}>
              {showCreate ? 'Cancel' : '+ New user'}
            </button>
          </div>
        </div>

        {error && <div className="form-error">{error}</div>}
        {feedback && <div className="form-success">{feedback}</div>}

        {showCreate && (
          <form className="form" onSubmit={submit} style={{ marginTop: '0.5rem' }}>
            <div className="form-row">
              <label>Full name<input value={form.fullName} onChange={update('fullName')} required /></label>
              <label>Email<input type="email" value={form.email} onChange={update('email')} required /></label>
            </div>
            <div className="form-row">
              <label>Phone<input value={form.phone} onChange={update('phone')} required /></label>
              <label>Role
                <select value={form.role} onChange={update('role')}>
                  {roles.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </label>
            </div>
            <label>Password<input type="password" value={form.password} onChange={update('password')} required minLength={8} /></label>
            <button className="btn btn-primary">Create user</button>
          </form>
        )}

        <div className="table-wrap" style={{ marginTop: '1rem' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Created</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading && Array.from({ length: 4 }).map((_, idx) => (
                <tr key={idx}><td colSpan={6}><div className="skeleton" style={{ height: 18 }} /></td></tr>
              ))}
              {!loading && filtered.length === 0 && (
                <tr><td colSpan={6} className="empty-state">No users.</td></tr>
              )}
              {!loading && filtered.map((user) => (
                <tr key={user.id}>
                  <td><strong>{user.fullName}</strong></td>
                  <td>{user.email}</td>
                  <td><span className={`tag ${user.role.toLowerCase()}`}>{user.role}</span></td>
                  <td>{user.isActive
                    ? <span className="tag success">Active</span>
                    : <span className="tag danger">Disabled</span>}</td>
                  <td className="muted" style={{ fontSize: '0.85rem' }}>{new Date(user.createdAt).toLocaleDateString()}</td>
                  <td>
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => toggle(user)}>
                      {user.isActive ? 'Disable' : 'Enable'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}
