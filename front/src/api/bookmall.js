import http from './http'
import { unwrapResult } from './result'

export const authApi = {
  login(payload) {
    return http.post('/api/auth/login', payload).then(unwrapResult)
  },
  register(payload) {
    return http.post('/api/auth/register', payload).then(unwrapResult)
  }
}

export const bookApi = {
  list() {
    return http.get('/api/books').then(unwrapResult)
  },
  detail(id) {
    return http.get(`/api/books/${id}`).then(unwrapResult)
  },
  page(params) {
    return http.get('/api/books/page', { params }).then(unwrapResult)
  },
  categories() {
    return http.get('/api/books/categories').then(unwrapResult)
  }
}

export const orderApi = {
  list() {
    return http.get('/api/orders').then(unwrapResult)
  },
  detail(id) {
    return http.get(`/api/orders/${id}`).then(unwrapResult)
  },
  create(payload) {
    return http.post('/api/orders', payload).then(unwrapResult)
  },
  cancel(id) {
    return http.put(`/api/orders/${id}/cancel`).then(unwrapResult)
  }
}
