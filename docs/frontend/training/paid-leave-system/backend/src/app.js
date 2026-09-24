import cors from 'cors'
import express from 'express'
import session from 'express-session'
import helmet from 'helmet'
import { config } from './config.js'
import { pool } from './db.js'
import { errorHandler, notFoundHandler } from './errors.js'
import { MySqlSessionStore } from './mysql-session-store.js'
import applicationsRouter from './routes/applications.js'
import authRouter from './routes/auth.js'
import dashboardRouter from './routes/dashboard.js'
import masterRouter from './routes/master.js'
import usersRouter from './routes/users.js'

const sessionStore = new MySqlSessionStore(pool)

export const app = express()

app.disable('x-powered-by')
app.use(helmet())
app.use(cors({
  origin: config.FRONTEND_ORIGIN,
  credentials: true,
  methods: ['GET', 'POST', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type']
}))
app.use(express.json({ limit: '100kb' }))
app.use(session({
  name: 'paidLeaveSession',
  secret: config.SESSION_SECRET,
  store: sessionStore,
  resave: false,
  saveUninitialized: false,
  rolling: true,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: config.NODE_ENV === 'production',
    maxAge: 30 * 60 * 1000
  }
}))

app.get('/api/health', async (req, res) => {
  await pool.query('SELECT 1')
  res.json({ data: { status: 'ok' } })
})
app.use('/api/auth', authRouter)
app.use('/api/users', usersRouter)
app.use('/api/master', masterRouter)
app.use('/api/dashboard', dashboardRouter)
app.use('/api/leave-applications', applicationsRouter)
app.use(notFoundHandler)
app.use(errorHandler)
