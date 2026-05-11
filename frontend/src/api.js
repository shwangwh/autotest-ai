export const AUTH_STORAGE_KEY = 'authToken'
export const USER_STORAGE_KEY = 'authUser'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082').replace(/\/$/, '')

export function getStoredToken() {
  return localStorage.getItem(AUTH_STORAGE_KEY)
}

export function setStoredToken(token) {
  localStorage.setItem(AUTH_STORAGE_KEY, token)
}

export function clearStoredToken() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

export function getStoredUser() {
  return localStorage.getItem(USER_STORAGE_KEY)
}

export function setStoredUser(username) {
  localStorage.setItem(USER_STORAGE_KEY, username)
}

export function clearStoredUser() {
  localStorage.removeItem(USER_STORAGE_KEY)
}

async function request(path, options = {}) {
  const { method = 'GET', body, headers = {}, auth = true } = options
  const finalHeaders = { Accept: 'application/json', ...headers }

  if (auth) {
    const token = getStoredToken()
    if (token) {
      finalHeaders.Authorization = token
    }
  }

  let finalBody = body
  if (body instanceof FormData) {
    delete finalHeaders['Content-Type']
  } else if (body && typeof body === 'object') {
    finalHeaders['Content-Type'] = 'application/json'
    finalBody = JSON.stringify(body)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: finalHeaders,
    body: method === 'GET' || method === 'HEAD' ? undefined : finalBody
  })

  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message = payload?.message || payload?.error || (typeof payload === 'string' && payload) || `Request failed (${response.status})`
    throw new Error(message)
  }

  return payload
}

export const api = {
  get(path, options = {}) {
    return request(path, { ...options, method: 'GET' })
  },
  post(path, body, options = {}) {
    return request(path, { ...options, method: 'POST', body })
  },
  put(path, body, options = {}) {
    return request(path, { ...options, method: 'PUT', body })
  },
  delete(path, options = {}) {
    return request(path, { ...options, method: 'DELETE' })
  },
  upload(path, formData, options = {}) {
    return request(path, { ...options, method: 'POST', body: formData })
  }
}
