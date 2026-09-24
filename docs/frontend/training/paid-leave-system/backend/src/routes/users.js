import bcrypt from 'bcryptjs'
import { Router } from 'express'
import { z } from 'zod'
import { pool } from '../db.js'
import { AppError } from '../errors.js'
import { parseRequest } from '../validation.js'

const router = Router()

const registrationSchema = z.object({
  account: z.string().trim().toLowerCase().regex(/^[a-z0-9._-]{4,20}$/, '半角英数字と . _ - を4～20文字で入力してください'),
  password: z.string().min(8, '8文字以上で入力してください').max(32, '32文字以内で入力してください'),
  passwordConfirmation: z.string(),
  name: z.string().trim().min(2, '2文字以上で入力してください').max(40, '40文字以内で入力してください'),
  department: z.enum(['development', 'quality', 'sales', 'general-affairs', 'human-resources'])
}).superRefine((value, context) => {
  if (value.password !== value.passwordConfirmation) {
    context.addIssue({
      code: 'custom',
      path: ['passwordConfirmation'],
      message: 'パスワードが一致しません'
    })
  }
})

router.post('/', async (req, res) => {
  const input = parseRequest(registrationSchema, req.body)
  const connection = await pool.getConnection()

  try {
    await connection.beginTransaction()

    const [existing] = await connection.execute(
      'SELECT id FROM users WHERE account = ? FOR UPDATE',
      [input.account]
    )
    if (existing.length > 0) {
      throw new AppError(409, 'ACCOUNT_ALREADY_EXISTS', 'このアカウントは既に使用されています')
    }

    const passwordHash = await bcrypt.hash(input.password, 12)
    const [result] = await connection.execute(
      `INSERT INTO users (account, password_hash, name, department_code)
       VALUES (?, ?, ?, ?)`,
      [input.account, passwordHash, input.name, input.department]
    )
    const employeeNumber = `EMP-${String(result.insertId).padStart(5, '0')}`

    await connection.execute(
      'UPDATE users SET employee_number = ? WHERE id = ?',
      [employeeNumber, result.insertId]
    )
    await connection.commit()

    res.status(201).json({
      data: {
        employeeNumber,
        account: input.account,
        name: input.name,
        department: input.department
      }
    })
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    connection.release()
  }
})

export default router
