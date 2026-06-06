import { useEffect, useState, useCallback } from 'react'
import { useLocation } from 'react-router-dom'
import { api } from '../../utils/api'
import { readProfile } from '../../utils/auth'
import ChatRoom from '../../components/ChatRoom.jsx'

const relativeTime = (iso) => {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'Just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return new Date(iso).toLocaleDateString()
}

export default function DoctorConversations() {
  const profile = readProfile()
  const myId = profile?.id
  const location = useLocation()

  const [conversations, setConversations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [patients, setPatients] = useState([])
  const [showNew, setShowNew] = useState(false)
  const [creating, setCreating] = useState(false)
  const [newPatientId, setNewPatientId] = useState('')

  const loadConversations = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await api.get('/api/chat/conversations')
      setConversations(data ?? [])
    } catch (err) {
      setError(err.message || 'Could not load conversations.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadConversations()
    api.get('/api/patients').then((data) => setPatients(data ?? [])).catch(() => {})
  }, [loadConversations])

  useEffect(() => {
    const openId = location.state?.openConvId
    if (openId && conversations.length > 0) {
      const conv = conversations.find((c) => c.id === openId)
      if (conv) setSelected(conv)
    }
  }, [conversations, location.state])

  const startConversation = async () => {
    if (!newPatientId) return
    setCreating(true)
    setError('')
    try {
      const patient = patients.find((p) => p.userId === newPatientId)
      const conv = await api.post('/api/chat/conversations', {
        type: 'DoctorPatient',
        name: patient ? `Chat with ${patient.fullName}` : null,
        participantIds: [myId, newPatientId],
      })
      setConversations((prev) => {
        if (prev.find((c) => c.id === conv.id)) return prev
        return [conv, ...prev]
      })
      setSelected(conv)
      setShowNew(false)
      setNewPatientId('')
    } catch (err) {
      setError(err.message || 'Could not start conversation.')
    } finally {
      setCreating(false)
    }
  }

  const otherName = (conv) => {
    const other = (conv.participants ?? []).find((p) => p.userId !== myId)
    return conv.name || other?.fullName || 'Conversation'
  }

  if (selected) {
    return (
      <ChatRoom
        conversation={selected}
        onBack={() => {
          setSelected(null)
          loadConversations()
        }}
      />
    )
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Messages</h2>
          <span className="muted">Chat with your patients in real time.</span>
        </div>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setShowNew((v) => !v)}>
          + New chat
        </button>
      </div>

      {showNew && (
        <div className="conv-new-panel">
          <label className="form-label">Select patient to chat with</label>
          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <select
              className="form-input"
              value={newPatientId}
              onChange={(e) => setNewPatientId(e.target.value)}
              style={{ flex: 1, minWidth: 200 }}
            >
              <option value="">— Choose a patient —</option>
              {patients.map((p) => (
                <option key={p.userId} value={p.userId}>{p.fullName}</option>
              ))}
            </select>
            <button
              type="button"
              className="btn btn-primary"
              onClick={startConversation}
              disabled={!newPatientId || creating}
            >
              {creating ? 'Starting…' : 'Start chat'}
            </button>
          </div>
        </div>
      )}

      {error && <div className="form-error">{error}</div>}

      <div className="conv-list">
        {loading && Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="skeleton" style={{ height: 68, borderRadius: 14 }} />
        ))}
        {!loading && conversations.length === 0 && (
          <div className="empty-state">
            <div className="big">💬</div>
            No conversations yet. Click <strong>+ New chat</strong> to message a patient.
          </div>
        )}
        {conversations.map((conv) => (
          <button
            key={conv.id}
            type="button"
            className="conv-item"
            onClick={() => setSelected(conv)}
          >
            <div className="conv-avatar">{otherName(conv)[0]?.toUpperCase()}</div>
            <div className="conv-meta">
              <strong className="conv-name">{otherName(conv)}</strong>
              <span className="conv-time muted">{relativeTime(conv.lastMessageAt)}</span>
            </div>
            <span className="conv-arrow">›</span>
          </button>
        ))}
      </div>
    </section>
  )
}
