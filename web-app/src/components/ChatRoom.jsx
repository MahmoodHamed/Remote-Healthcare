import { useEffect, useRef, useState, useCallback } from 'react'
import { api } from '../utils/api'
import { buildChatHubConnection, startChatHub, stopChatHub } from '../utils/signalr'
import { readProfile } from '../utils/auth'

/** Add or replace — avoids duplicate when REST response and SignalR race. */
const upsertMessage = (prev, incoming) => {
  if (prev.some((m) => m.id === incoming.id)) return prev

  const optimisticIdx = prev.findIndex(
    (m) =>
      m.optimistic &&
      m.senderId === incoming.senderId &&
      m.content === incoming.content,
  )
  if (optimisticIdx >= 0) {
    const next = [...prev]
    next[optimisticIdx] = incoming
    return next
  }

  return [...prev, incoming]
}

const formatTime = (iso) => {
  const d = new Date(iso)
  const today = new Date()
  const sameDay =
    d.getFullYear() === today.getFullYear() &&
    d.getMonth() === today.getMonth() &&
    d.getDate() === today.getDate()
  return sameDay
    ? d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : d.toLocaleDateString([], { month: 'short', day: 'numeric' }) +
        ' ' +
        d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export default function ChatRoom({ conversation, onBack }) {
  const profile = readProfile()
  const myId = profile?.id

  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [hubStatus, setHubStatus] = useState('connecting')

  const bottomRef = useRef(null)
  const connectionRef = useRef(null)
  const startTaskRef = useRef(null)
  const mountedRef = useRef(true)

  useEffect(() => () => { mountedRef.current = false }, [])

  const scrollToBottom = useCallback(() => {
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 60)
  }, [])

  useEffect(() => {
    if (!conversation?.id) return
    let cancelled = false

    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const result = await api.get(
          `/api/chat/conversations/${conversation.id}/messages?page=1&pageSize=80`
        )
        if (!cancelled) {
          setMessages((result?.items ?? []).slice().reverse())
          scrollToBottom()
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Could not load messages.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()

    const conn = buildChatHubConnection({
      onMessage: (msg) => {
        if (!mountedRef.current) return
        setMessages((prev) => upsertMessage(prev, msg))
        scrollToBottom()
      },
    })
    connectionRef.current = conn

    const startTask = startChatHub(conn, conversation.id)
      .then(() => { if (!cancelled) setHubStatus('connected') })
      .catch(() => { if (!cancelled) setHubStatus('error') })
    startTaskRef.current = startTask

    return () => {
      cancelled = true
      const c = connectionRef.current
      const t = startTaskRef.current
      connectionRef.current = null
      startTaskRef.current = null
      void (async () => {
        try { if (t) await t } catch { /* ignore */ }
        if (c) await stopChatHub(c, conversation.id)
      })()
    }
  }, [conversation?.id, scrollToBottom])

  const send = async (e) => {
    e.preventDefault()
    const content = text.trim()
    if (!content || sending) return
    setSending(true)
    const optimistic = {
      id: `opt-${Date.now()}`,
      conversationId: conversation.id,
      senderId: myId,
      senderName: profile?.fullName ?? 'Me',
      content,
      type: 'Text',
      mediaUrl: null,
      isDeleted: false,
      sentAt: new Date().toISOString(),
      optimistic: true,
    }
    setMessages((prev) => [...prev, optimistic])
    setText('')
    scrollToBottom()
    try {
      const msg = await api.post(`/api/chat/conversations/${conversation.id}/messages`, {
        content,
        type: 'Text',
      })
      setMessages((prev) => upsertMessage(prev, msg))
    } catch (err) {
      setMessages((prev) => prev.filter((m) => m.id !== optimistic.id))
      setError(err.message || 'Could not send message.')
      setText(content)
    } finally {
      setSending(false)
    }
  }

  const otherParticipants = (conversation?.participants ?? []).filter(
    (p) => p.userId !== myId
  )
  const title =
    conversation?.name ||
    otherParticipants.map((p) => p.fullName).join(', ') ||
    'Conversation'

  return (
    <section className="card chat-room-card">
      <div className="chat-room-header">
        {onBack && (
          <button type="button" className="btn btn-outline btn-sm" onClick={onBack}>
            ←
          </button>
        )}
        <div className="chat-room-title">
          <strong>{title}</strong>
          <span className={`chat-status-dot ${hubStatus}`} title={hubStatus} />
        </div>
      </div>

      {error && <div className="form-error" style={{ margin: '0 0 0.5rem' }}>{error}</div>}

      <div className="chat-messages">
        {loading &&
          Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className={`skeleton chat-skel ${i % 2 === 0 ? 'left' : 'right'}`} />
          ))}
        {!loading && messages.length === 0 && (
          <div className="empty-state" style={{ padding: '2rem' }}>
            <div className="big">💬</div>
            No messages yet. Say hello!
          </div>
        )}
        {messages.map((msg) => {
          const mine = msg.senderId === myId
          return (
            <div key={msg.id} className={`chat-bubble-row ${mine ? 'mine' : 'theirs'}`}>
              {!mine && (
                <div className="chat-avatar">{(msg.senderName ?? '?')[0].toUpperCase()}</div>
              )}
              <div className={`chat-bubble ${mine ? 'bubble-mine' : 'bubble-theirs'} ${msg.optimistic ? 'bubble-sending' : ''}`}>
                {!mine && <span className="bubble-sender">{msg.senderName}</span>}
                <span className="bubble-text">{msg.isDeleted ? <em className="muted">Message deleted</em> : msg.content}</span>
                <span className="bubble-time">{formatTime(msg.sentAt)}</span>
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      <form className="chat-input-row" onSubmit={send}>
        <input
          className="chat-input"
          type="text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Type a message…"
          disabled={sending}
          autoComplete="off"
        />
        <button
          type="submit"
          className="btn btn-primary"
          disabled={!text.trim() || sending}
        >
          Send
        </button>
      </form>
    </section>
  )
}
