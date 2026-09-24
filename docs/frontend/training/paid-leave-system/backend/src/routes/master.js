import { Router } from 'express'
import { pool } from '../db.js'

const router = Router()

router.get('/departments', async (req, res) => {
  const [rows] = await pool.execute(
    `SELECT code AS value, display_name AS label
     FROM departments
     ORDER BY sort_order`
  )

  res.json({ data: rows })
})

export default router
