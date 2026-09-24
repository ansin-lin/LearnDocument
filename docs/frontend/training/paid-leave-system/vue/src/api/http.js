import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:3000/api',
  timeout: 10000,
  withCredentials: true
})

export function toApiError(error) {
  if (!error.response) {
    return {
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'サーバーに接続できませんでした。しばらくしてから再度お試しください。',
      details: []
    }
  }

  return {
    status: error.response.status,
    code: error.response.data?.error?.code ?? 'UNKNOWN_ERROR',
    message: error.response.data?.error?.message ?? '処理に失敗しました。',
    details: error.response.data?.error?.details ?? []
  }
}
