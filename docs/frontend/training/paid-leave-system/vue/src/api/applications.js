import { http } from './http.js'

export async function getDashboard() {
  const response = await http.get('/dashboard')
  return response.data.data
}

export async function getApplications(params = {}) {
  const response = await http.get('/leave-applications', { params })
  return response.data
}

export async function getApplication(id) {
  const response = await http.get(`/leave-applications/${id}`)
  return response.data.data
}

export async function createApplication(input) {
  const response = await http.post('/leave-applications', input)
  return response.data.data
}

export async function cancelApplication(id) {
  const response = await http.patch(`/leave-applications/${id}/cancel`)
  return response.data.data
}
