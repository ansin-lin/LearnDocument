import { app } from './app.js'
import { config } from './config.js'
import { pool } from './db.js'

const server = app.listen(config.PORT, () => {
  console.log(`Paid Leave API started: http://localhost:${config.PORT}`)
})

async function shutdown(signal) {
  console.log(`${signal} received. Shutting down...`)
  server.close(async () => {
    await pool.end()
    process.exit(0)
  })
}

process.on('SIGINT', () => shutdown('SIGINT'))
process.on('SIGTERM', () => shutdown('SIGTERM'))
