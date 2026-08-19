const API_BASE_URL = 'http://127.0.0.1:8000/api';
const ACCESS_KEY = 'employee_api_access';
const REFRESH_KEY = 'employee_api_refresh';

export class ApiError extends Error {
  constructor(status, body) {
    super(`API request failed: ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

function saveTokens(tokens) {
  sessionStorage.setItem(ACCESS_KEY, tokens.access);
  sessionStorage.setItem(REFRESH_KEY, tokens.refresh);
}

export function clearTokens() {
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}

async function readBody(response) {
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') ?? '';
  return contentType.includes('application/json')
    ? response.json()
    : response.text();
}

async function fetchWithTimeout(url, options) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 10000);
  try {
    return await fetch(url, {...options, signal: controller.signal});
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new ApiError(0, {detail: '请求超时'});
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

export async function login(username, password) {
  const response = await fetchWithTimeout(
    `${API_BASE_URL}/auth/token/`,
    {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({username, password}),
    },
  );
  const body = await readBody(response);
  if (!response.ok) throw new ApiError(response.status, body);
  saveTokens(body);
}

async function refreshAccessToken() {
  const refresh = sessionStorage.getItem(REFRESH_KEY);
  if (!refresh) return false;
  const response = await fetchWithTimeout(
    `${API_BASE_URL}/auth/token/refresh/`,
    {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({refresh}),
    },
  );
  const body = await readBody(response);
  if (!response.ok) {
    clearTokens();
    return false;
  }
  sessionStorage.setItem(ACCESS_KEY, body.access);
  return true;
}

export async function apiFetch(path, options = {}, retry = true) {
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');
  if (
    options.body
    && !(options.body instanceof FormData)
    && !headers.has('Content-Type')
  ) {
    headers.set('Content-Type', 'application/json');
  }

  const access = sessionStorage.getItem(ACCESS_KEY);
  if (access) headers.set('Authorization', `Bearer ${access}`);
  const response = await fetchWithTimeout(
    `${API_BASE_URL}${path}`,
    {...options, headers},
  );
  if (response.status === 401 && retry && await refreshAccessToken()) {
    return apiFetch(path, options, false);
  }
  const body = await readBody(response);
  if (!response.ok) throw new ApiError(response.status, body);
  return body;
}

export async function apiDownload(path, retry = true) {
  const headers = new Headers();
  const access = sessionStorage.getItem(ACCESS_KEY);
  if (access) headers.set('Authorization', `Bearer ${access}`);
  const response = await fetchWithTimeout(
    `${API_BASE_URL}${path}`,
    {headers},
  );
  if (response.status === 401 && retry && await refreshAccessToken()) {
    return apiDownload(path, false);
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readBody(response));
  }
  return response.blob();
}
