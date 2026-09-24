import session from 'express-session'

export class MySqlSessionStore extends session.Store {
  constructor(pool) {
    super()
    this.pool = pool
  }

  get(sessionId, callback) {
    this.pool.execute(
      `SELECT session_data AS sessionData
       FROM user_sessions
       WHERE session_id = ? AND expires_at > ?`,
      [sessionId, Date.now()]
    ).then(([rows]) => {
      callback(null, rows[0] ? JSON.parse(rows[0].sessionData) : null)
    }).catch(callback)
  }

  set(sessionId, sessionData, callback = () => {}) {
    const expiresAt = sessionData.cookie?.expires
      ? new Date(sessionData.cookie.expires).getTime()
      : Date.now() + 30 * 60 * 1000

    this.pool.execute(
      `INSERT INTO user_sessions (session_id, expires_at, session_data)
       VALUES (?, ?, ?)
       ON DUPLICATE KEY UPDATE
         expires_at = VALUES(expires_at),
         session_data = VALUES(session_data)`,
      [sessionId, expiresAt, JSON.stringify(sessionData)]
    ).then(() => callback()).catch(callback)
  }

  destroy(sessionId, callback = () => {}) {
    this.pool.execute(
      'DELETE FROM user_sessions WHERE session_id = ?',
      [sessionId]
    ).then(() => callback()).catch(callback)
  }

  touch(sessionId, sessionData, callback = () => {}) {
    const expiresAt = sessionData.cookie?.expires
      ? new Date(sessionData.cookie.expires).getTime()
      : Date.now() + 30 * 60 * 1000

    this.pool.execute(
      'UPDATE user_sessions SET expires_at = ? WHERE session_id = ?',
      [expiresAt, sessionId]
    ).then(() => callback()).catch(callback)
  }
}
