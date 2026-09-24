import bcrypt from 'bcryptjs'
import { Router } from 'express'
import { z } from 'zod'
import { pool } from '../db.js'
import { AppError } from '../errors.js'
import { requireLogin } from '../middleware/auth.js'
import { destroySession, regenerateSession, saveSession } from '../session.js'
import { parseRequest } from '../validation.js'

const router = Router()

const loginSchema = z.object({
  account: z.string().trim().toLowerCase().min(4).max(20),
  password: z.string().min(8).max(32)
})

function toCurrentUser(row) {
  return {
    employeeNumber: row.employeeNumber,
    account: row.account,
    name: row.name,
    department: row.department,
    departmentName: row.departmentName
  }
}

router.post('/login', async (req, res) => {
  const input = parseRequest(loginSchema, req.body)
  const [rows] = await pool.execute(
    `SELECT
       u.id,
       u.employee_number AS employeeNumber,
       u.account,
       u.password_hash AS passwordHash,
       u.name,
       u.department_code AS department,
       d.display_name AS departmentName
     FROM users u
     JOIN departments d ON d.code = u.department_code
     WHERE u.account = ? AND u.active = TRUE`,
    [input.account]
  )

  const user = rows[0]
  const passwordMatches = user
    ? await bcrypt.compare(input.password, user.passwordHash)
    : false

  if (!passwordMatches) {
    throw new AppError(401, 'INVALID_CREDENTIALS', 'アカウントまたはパスワードが正しくありません')
  }

  await regenerateSession(req)
  req.session.userId = user.id
  await saveSession(req)

  res.json({ data: toCurrentUser(user) })
})

router.get('/me', async (req, res) => {
  if (!req.session.userId) {
    res.json({ data: null })
    return
  }

  const [rows] = await pool.execute(
    `SELECT
       u.employee_number AS employeeNumber,
       u.account,
       u.name,
       u.department_code AS department,
       d.display_name AS departmentName
     FROM users u
     JOIN departments d ON d.code = u.department_code
     WHERE u.id = ? AND u.active = TRUE`,
    [req.session.userId]
  )

  if (!rows[0]) {
    await destroySession(req)
    res.clearCookie('paidLeaveSession')
    res.json({ data: null })
    return
  }

  res.json({ data: toCurrentUser(rows[0]) })
})

router.post('/logout', requireLogin, async (req, res) => {
  await destroySession(req)
  res.clearCookie('paidLeaveSession')
  res.status(204).end()
})

export default router
