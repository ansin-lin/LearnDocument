import 'dotenv/config'
import { z } from 'zod'

const environmentSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().min(1).max(65535).default(3000),
  FRONTEND_ORIGIN: z.string().url().default('http://localhost:5174'),
  DB_HOST: z.string().min(1).default('127.0.0.1'),
  DB_PORT: z.coerce.number().int().min(1).max(65535).default(3306),
  DB_NAME: z.string().min(1).default('paid_leave_training'),
  DB_USER: z.string().min(1),
  DB_PASSWORD: z.string(),
  SESSION_SECRET: z.string().min(32)
})

const parsed = environmentSchema.safeParse(process.env)

if (!parsed.success) {
  console.error('環境変数の設定が正しくありません', parsed.error.flatten().fieldErrors)
  process.exit(1)
}

export const config = parsed.data
