import { Router } from 'express'
import { pool } from '../db.js'
import { AppError } from '../errors.js'
import { requireLogin } from '../middleware/auth.js'

const router = Router()

router.get('/', requireLogin, async (req, res) => {
  const [rows] = await pool.execute(
    `SELECT
       u.employee_number AS employeeNumber,
       u.name,
       u.department_code AS department,
       d.display_name AS departmentName,
       u.paid_leave_days
         - COALESCE(SUM(CASE
             WHEN a.status = 'approved'
              AND a.leave_type IN ('paid', 'half-am', 'half-pm')
             THEN a.leave_days ELSE 0 END), 0) AS remainingPaidLeaveDays,
       COUNT(CASE WHEN a.status = 'pending' THEN 1 END) AS pendingCount,
       COALESCE(SUM(CASE
         WHEN a.status = 'approved'
          AND YEAR(CONVERT_TZ(a.submitted_at, '+00:00', '+09:00'))
              = YEAR(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'))
          AND MONTH(CONVERT_TZ(a.submitted_at, '+00:00', '+09:00'))
              = MONTH(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'))
         THEN a.leave_days ELSE 0 END), 0) AS approvedDaysThisMonth
     FROM users u
     JOIN departments d ON d.code = u.department_code
     LEFT JOIN leave_applications a ON a.user_id = u.id
     WHERE u.id = ?
     GROUP BY
       u.id,
       u.employee_number,
       u.name,
       u.department_code,
       d.display_name,
       u.paid_leave_days`,
    [req.session.userId]
  )

  if (!rows[0]) {
    throw new AppError(401, 'AUTH_REQUIRED', 'ログインが必要です')
  }

  res.json({ data: rows[0] })
})

export default router
