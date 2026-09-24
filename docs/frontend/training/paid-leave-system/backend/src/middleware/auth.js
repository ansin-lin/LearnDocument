import { AppError } from '../errors.js'

export function requireLogin(req, res, next) {
  if (!req.session.userId) {
    next(new AppError(401, 'AUTH_REQUIRED', 'ログインが必要です'))
    return
  }

  next()
}
