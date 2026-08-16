import http from './http'
import { unwrapResult } from './result'

export const authApi = {
  login(payload) {
    return http.post('/api/auth/login', payload).then(unwrapResult)
  },
  register(payload) {
    return http.post('/api/auth/register', payload).then(unwrapResult)
  },
  me() {
    return http.get('/api/auth/me').then(unwrapResult)
  }
}

export const bookApi = {
  list() {
    return http.get('/api/books').then(unwrapResult)
  },
  detail(id) {
    return http.get(`/api/books/${id}`).then(unwrapResult)
  },
  search(params) {
    return http.get('/api/books/search', { params }).then(unwrapResult)
  },
  page(params) {
    return http.get('/api/books/page', { params }).then(unwrapResult)
  },
  categories() {
    return http.get('/api/books/categories').then(unwrapResult)
  },
  categoryTree() {
    return http.get('/api/books/categories/tree').then(unwrapResult)
  }
}

export const cartApi = {
  list(userId) {
    return http.get('/api/cart', { params: { userId } }).then(unwrapResult)
  },
  add(payload) {
    return http.post('/api/cart/items', payload).then(unwrapResult)
  },
  update(id, quantity) {
    return http.put(`/api/cart/items/${id}`, { quantity }).then(unwrapResult)
  },
  remove(id) {
    return http.delete(`/api/cart/items/${id}`).then(unwrapResult)
  }
}

export const addressApi = {
  list(userId) {
    return http.get('/api/address/list', { params: { userId } }).then(unwrapResult)
  },
  detail(id) {
    return http.get(`/api/address/${id}`).then(unwrapResult)
  },
  create(payload) {
    return http.post('/api/address', payload).then(unwrapResult)
  },
  update(id, payload) {
    return http.put(`/api/address/${id}`, payload).then(unwrapResult)
  },
  remove(id) {
    return http.delete(`/api/address/${id}`).then(unwrapResult)
  },
  setDefault(id) {
    return http.put(`/api/address/${id}/default`).then(unwrapResult)
  }
}

export const orderApi = {
  list(userId) {
    return http.get('/api/orders', { params: { userId } }).then(unwrapResult)
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
