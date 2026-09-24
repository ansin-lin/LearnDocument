import { Router } from 'express'
import { z } from 'zod'
import { pool } from '../db.js'
import { AppError } from '../errors.js'
import { requireLogin } from '../middleware/auth.js'
import { calculateLeaveDays, compactDate, isValidIsoDate } from '../utils/dates.js'
import { parseRequest } from '../validation.js'

const router = Router()
router.use(requireLogin)

const dateField = z.string().refine(isValidIsoDate, '日付はYYYY-MM-DD形式で入力してください')
const applicationSchema = z.object({
  leaveType: z.enum(['paid', 'half-am', 'half-pm', 'special']),
  startDate: dateField,
  endDate: dateField,
  reason: z.string().trim().min(1).max(200),
  handoverStatus: z.enum(['done', 'not-required']),
  note: z.string().trim().max(300).default('')
}).superRefine((value, context) => {
  if (value.endDate < value.startDate) {
    context.addIssue({ code: 'custom', path: ['endDate'], message: '終了日は開始日以降にしてください' })
  }
  if ((value.leaveType === 'half-am' || value.leaveType === 'half-pm')
      && value.startDate !== value.endDate) {
    context.addIssue({ code: 'custom', path: ['endDate'], message: '半日休暇は開始日と終了日を同じ日にしてください' })
  }
  if (value.leaveType === 'special' && value.note.length === 0) {
    context.addIssue({ code: 'custom', path: ['note'], message: '特別休暇の制度名を入力してください' })
  }
})

const listQuerySchema = z.object({
  status: z.enum(['pending', 'approved', 'returned', 'cancelled']).optional(),
  keyword: z.string().trim().max(100).optional(),
  startDate: z.union([dateField, z.literal('')]).optional(),
  endDate: z.union([dateField, z.literal('')]).optional()
}).superRefine((value, context) => {
  if (value.startDate && value.endDate && value.endDate < value.startDate) {
    context.addIssue({ code: 'custom', path: ['endDate'], message: '終了日は開始日以降にしてください' })
  }
})

const idSchema = z.coerce.number().int().positive()

function mapApplication(row) {
  const toIsoUtc = (value) => value ? `${value.replace(' ', 'T')}Z` : null

  return {
    id: row.id,
    receiptNumber: row.receiptNumber,
    leaveType: row.leaveType,
    startDate: row.startDate,
    endDate: row.endDate,
    leaveDays: row.leaveDays,
    reason: row.reason,
    handoverStatus: row.handoverStatus,
    note: row.note,
    status: row.status,
    submittedAt: toIsoUtc(row.submittedAt),
    cancelledAt: toIsoUtc(row.cancelledAt)
  }
}

const applicationSelect = `SELECT
  id,
  receipt_number AS receiptNumber,
  leave_type AS leaveType,
  start_date AS startDate,
  end_date AS endDate,
  leave_days AS leaveDays,
  reason,
  handover_status AS handoverStatus,
  note,
  status,
  submitted_at AS submittedAt,
  cancelled_at AS cancelledAt
FROM leave_applications`

router.get('/', async (req, res) => {
  const query = parseRequest(listQuerySchema, req.query)
  const conditions = ['user_id = ?']
  const parameters = [req.session.userId]

  if (query.status) {
    conditions.push('status = ?')
    parameters.push(query.status)
  }
  if (query.keyword) {
    conditions.push('(receipt_number LIKE ? OR reason LIKE ?)')
    const escaped = query.keyword.replaceAll('\\', '\\\\').replaceAll('%', '\\%').replaceAll('_', '\\_')
    parameters.push(`%${escaped}%`, `%${escaped}%`)
  }
  if (query.startDate) {
    conditions.push('end_date >= ?')
    parameters.push(query.startDate)
  }
  if (query.endDate) {
    conditions.push('start_date <= ?')
    parameters.push(query.endDate)
  }

  const [rows] = await pool.execute(
    `${applicationSelect}
     WHERE ${conditions.join(' AND ')}
     ORDER BY submitted_at DESC, id DESC`,
    parameters
  )

  res.json({ data: rows.map(mapApplication), meta: { count: rows.length } })
})

router.get('/:id', async (req, res) => {
  const id = parseRequest(idSchema, req.params.id)
  const [rows] = await pool.execute(
    `${applicationSelect} WHERE id = ? AND user_id = ?`,
    [id, req.session.userId]
  )

  if (!rows[0]) {
    throw new AppError(404, 'APPLICATION_NOT_FOUND', '申請が見つかりません')
  }

  res.json({ data: mapApplication(rows[0]) })
})

router.post('/', async (req, res) => {
  const input = parseRequest(applicationSchema, req.body)
  const connection = await pool.getConnection()
  try {
    await connection.beginTransaction()

    const [[clock]] = await connection.query(
      `SELECT DATE_FORMAT(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'), '%Y-%m-%d') AS today`
    )
    if (input.startDate < clock.today) {
      throw new AppError(400, 'START_DATE_IN_PAST', '開始日は本日以降にしてください')
    }

    const leaveDays = calculateLeaveDays(input.leaveType, input.startDate, input.endDate)
    const [[user]] = await connection.execute(
      'SELECT paid_leave_days AS paidLeaveDays FROM users WHERE id = ? FOR UPDATE',
      [req.session.userId]
    )
    if (!user) {
      throw new AppError(401, 'AUTH_REQUIRED', 'ログインが必要です')
    }

    const [[usedLeave]] = await connection.execute(
      `SELECT COALESCE(SUM(leave_days), 0) AS usedDays
       FROM leave_applications
       WHERE user_id = ?
         AND status = 'approved'
         AND leave_type IN ('paid', 'half-am', 'half-pm')`,
      [req.session.userId]
    )
    const remainingDays = user.paidLeaveDays - usedLeave.usedDays
    if (leaveDays > remainingDays) {
      throw new AppError(409, 'INSUFFICIENT_LEAVE_BALANCE', '有給残日数を超えています')
    }

    await connection.execute(
      `INSERT INTO leave_receipt_counters (receipt_date, last_sequence)
       VALUES (?, 0)
       ON DUPLICATE KEY UPDATE receipt_date = VALUES(receipt_date)`,
      [clock.today]
    )
    const [[counter]] = await connection.execute(
      'SELECT last_sequence AS lastSequence FROM leave_receipt_counters WHERE receipt_date = ? FOR UPDATE',
      [clock.today]
    )
    const nextSequence = counter.lastSequence + 1
    await connection.execute(
      'UPDATE leave_receipt_counters SET last_sequence = ? WHERE receipt_date = ?',
      [nextSequence, clock.today]
    )

    const receiptNumber = `REQ-${compactDate(clock.today)}-${String(nextSequence).padStart(3, '0')}`
    // 本练习不实现审批人画面，因此由后台模拟申请状态。
    const statuses = ['pending', 'approved', 'returned']
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    const [result] = await connection.execute(
      `INSERT INTO leave_applications (
         receipt_number, user_id, leave_type, start_date, end_date,
         leave_days, reason, handover_status, note, status
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        receiptNumber,
        req.session.userId,
        input.leaveType,
        input.startDate,
        input.endDate,
        leaveDays,
        input.reason,
        input.handoverStatus,
        input.note,
        status
      ]
    )

    const [created] = await connection.execute(
      `${applicationSelect} WHERE id = ?`,
      [result.insertId]
    )
    await connection.commit()

    res.status(201).json({ data: mapApplication(created[0]) })
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    connection.release()
  }
})

router.patch('/:id/cancel', async (req, res) => {
  const id = parseRequest(idSchema, req.params.id)
  const [result] = await pool.execute(
    `UPDATE leave_applications
     SET status = 'cancelled', cancelled_at = UTC_TIMESTAMP(3)
     WHERE id = ? AND user_id = ? AND status = 'pending'`,
    [id, req.session.userId]
  )

  if (result.affectedRows === 0) {
    const [rows] = await pool.execute(
      'SELECT status FROM leave_applications WHERE id = ? AND user_id = ?',
      [id, req.session.userId]
    )
    if (!rows[0]) {
      throw new AppError(404, 'APPLICATION_NOT_FOUND', '申請が見つかりません')
    }
    throw new AppError(409, 'APPLICATION_NOT_PENDING', '申請中のデータだけ取消できます')
  }

  const [rows] = await pool.execute(
    `${applicationSelect} WHERE id = ? AND user_id = ?`,
    [id, req.session.userId]
  )
  res.json({ data: mapApplication(rows[0]) })
})

export default router
