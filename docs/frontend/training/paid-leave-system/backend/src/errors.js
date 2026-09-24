export class AppError extends Error {
  constructor(status, code, message, details) {
    super(message)
    this.status = status
    this.code = code
    this.details = details
  }
}

export function notFoundHandler(req, res) {
  res.status(404).json({
    error: {
      code: 'NOT_FOUND',
      message: '指定されたAPIが見つかりません'
    }
  })
}

export function errorHandler(error, req, res, next) {
  if (res.headersSent) {
    next(error)
    return
  }

  if (error instanceof AppError) {
    res.status(error.status).json({
      error: {
        code: error.code,
        message: error.message,
        ...(error.details ? { details: error.details } : {})
      }
    })
    return
  }

  if (error?.code === 'ER_DUP_ENTRY') {
    res.status(409).json({
      error: {
        code: 'DUPLICATE_DATA',
        message: '既に登録されているデータです'
      }
    })
    return
  }

  console.error(error)
  res.status(500).json({
    error: {
      code: 'INTERNAL_ERROR',
      message: 'サーバー処理中にエラーが発生しました'
    }
  })
}
