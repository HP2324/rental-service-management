import client from './client'

export const initiatePayment = (data) =>
  client.post('/payments', data).then((r) => r.data.data)

export const syncStripeStatus = (paymentId) =>
  client.post(`/payments/${paymentId}/sync-status`).then((r) => r.data.data)

export const getPayment = (id) =>
  client.get(`/payments/${id}`).then((r) => r.data.data)

export const getMyPayments = () =>
  client.get('/payments/my-payments').then((r) => r.data.data)

export const capturePayPalPayment = (orderId) =>
  client.post(`/payments/paypal/capture?orderId=${orderId}`).then((r) => r.data.data)
