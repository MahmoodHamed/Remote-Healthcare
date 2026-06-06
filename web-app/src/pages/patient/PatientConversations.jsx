import { useEffect, useState, useCallback } from 'react'
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

export default function PatientConversations() {
  const profile = readProfile()
  const myId = profile?.id

  const [conversations, setConversations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)

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
  }, [loadConversations])

  const otherName = (conv) => {
    const other = (conv.participants ?? []).find((p) => p.userId !== myId)
    return conv.name || other?.fullName || 'My Doctor'
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
          <span className="muted">Chat directly with your doctor.</span>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="conv-list">
        {loading && Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="skeleton" style={{ height: 68, borderRadius: 14 }} />
        ))}
        {!loading && conversations.length === 0 && (
          <div className="empty-state">
            <div className="big">💬</div>
            No conversations yet. Your doctor will start a chat with you.
          </div>
        )}
        {conversations.map((conv) => (
          <button
            key={conv.id}
            type="button"
            className="conv-item"
            onClick={() => setSelected(conv)}
          >
            <div className="conv-avatar doctor-avatar">{otherName(conv)[0]?.toUpperCase()}</div>
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
