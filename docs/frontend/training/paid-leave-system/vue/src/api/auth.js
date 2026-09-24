import { http } from './http.js'

export async function login(input) {
  const response = await http.post('/auth/login', input)
  return response.data.data
}

export async function getCurrentUser() {
  const response = await http.get('/auth/me')
  return response.data.data
}

export async function logout() {
  await http.post('/auth/logout')
}

export async function getDepartments() {
  const response = await http.get('/master/departments')
  return response.data.data
}

export async function registerUser(input) {
  const response = await http.post('/users', input)
  return response.data.data
}
