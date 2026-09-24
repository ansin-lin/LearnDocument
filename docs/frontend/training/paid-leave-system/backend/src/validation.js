import { AppError } from './errors.js'

export function parseRequest(schema, value) {
  const parsed = schema.safeParse(value)

  if (!parsed.success) {
    const details = parsed.error.issues.map((issue) => ({
      field: issue.path.join('.'),
      message: issue.message
    }))
    throw new AppError(400, 'VALIDATION_ERROR', '入力内容を確認してください', details)
  }

  return parsed.data
}
