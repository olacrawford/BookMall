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

export const addressApi = {
  list() {
    return http.get('/api/auth/addresses').then(unwrapResult)
  },
  create(payload) {
    return http.post('/api/auth/addresses', payload).then(unwrapResult)
  },
  update(id, payload) {
    return http.put(`/api/auth/addresses/${id}`, payload).then(unwrapResult)
  },
  setDefault(id) {
    return http.put(`/api/auth/addresses/${id}/default`).then(unwrapResult)
  },
  remove(id) {
    return http.delete(`/api/auth/addresses/${id}`).then(unwrapResult)
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

export const stockApi = {
  detail(bookId) {
    return http.get(`/api/stock/${bookId}`).then(unwrapResult)
  }
}

export const cartApi = {
  list() {
    return http.get('/api/cart').then(unwrapResult)
  },
  add(payload) {
    return http.post('/api/cart', payload).then(unwrapResult)
  },
  update(id, payload) {
    return http.put(`/api/cart/${id}`, payload).then(unwrapResult)
  },
  remove(id) {
    return http.delete(`/api/cart/${id}`).then(unwrapResult)
  },
  clear() {
    return http.delete('/api/cart').then(unwrapResult)
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
  createFromCart(payload) {
    return http.post('/api/orders/from-cart', payload).then(unwrapResult)
  },
  cancel(id) {
    return http.put(`/api/orders/${id}/cancel`).then(unwrapResult)
  },
  complete(id) {
    return http.put(`/api/orders/${id}/complete`).then(unwrapResult)
  }
}

export const afterSaleApi = {
  list() {
    return http.get('/api/after-sales').then(unwrapResult)
  },
  create(payload, idempotencyKey) {
    return http.post('/api/after-sales', payload, { headers: { 'Idempotency-Key': idempotencyKey } }).then(unwrapResult)
  },
  detail(id) {
    return http.get(`/api/after-sales/${id}`).then(unwrapResult)
  },
  analysis(id) {
    return http.get(`/api/after-sales/${id}/analysis`).then(unwrapResult)
  },
  approvalQueue(status = 'WAITING') {
    return http.get('/api/approval-tasks', { params: { status } }).then(unwrapResult)
  },
  approve(id, comment) {
    return http.post(`/api/approval-tasks/${id}/approve`, { comment }).then(unwrapResult)
  },
  reject(id, comment) {
    return http.post(`/api/approval-tasks/${id}/reject`, { comment }).then(unwrapResult)
  }
}

export const paymentApi = {
  pay(orderId) {
    return http.post('/api/payment/pay', { orderId }).then(unwrapResult)
  },
  detail(orderId) {
    return http.get(`/api/payment/order/${orderId}`).then(unwrapResult)
  }
}
