const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const TOKEN_KEY = 'remediot_token'

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    // fetch só rejeita em falha de rede (servidor fora do ar, sem internet, CORS)
    const error = new Error('Não foi possível conectar ao servidor. Verifique sua internet e tente novamente.')
    error.status = 0
    throw error
  }

  if (response.status === 204) {
    return null
  }

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const message = data?.message || 'Não foi possível completar a operação. Tente novamente.'
    const error = new Error(message)
    error.status = response.status
    error.fields = data?.fields
    throw error
  }

  return data
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
}

export const authApi = {
  login: (email, password) =>
    request('/api/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  register: (fullName, email, password, phone) =>
    request('/api/auth/register', { method: 'POST', body: { fullName, email, password, phone }, auth: false }),
}

export const patientsApi = {
  list: () => api.get('/api/patients'),
  get: (id) => api.get(`/api/patients/${id}`),
  create: (data) => api.post('/api/patients', data),
  update: (id, data) => api.put(`/api/patients/${id}`, data),
}

export const medicationsApi = {
  list: (patientId) => api.get(`/api/patients/${patientId}/medications`),
  create: (patientId, data) => api.post(`/api/patients/${patientId}/medications`, data),
  update: (patientId, medicationId, data) =>
    api.put(`/api/patients/${patientId}/medications/${medicationId}`, data),
  remove: (patientId, medicationId) =>
    api.delete(`/api/patients/${patientId}/medications/${medicationId}`),
}

export const dosesApi = {
  listForDate: (patientId, date) =>
    api.get(`/api/patients/${patientId}/doses${date ? `?date=${date}` : ''}`),
  simulateOpening: (doseId) => api.post(`/api/doses/${doseId}/simulate-opening`),
}

export const dashboardApi = {
  get: () => api.get('/api/dashboard'),
}

export { TOKEN_KEY }
