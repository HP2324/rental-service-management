import client from './client'

export const register = (data) =>
  client.post('/auth/register', data).then((r) => r.data.data)

export const login = (data) =>
  client.post('/auth/login', data).then((r) => r.data.data)

export const getMe = () =>
  client.get('/users/me').then((r) => r.data.data)

export const updateProfile = (data) =>
  client.put('/users/me', data).then((r) => r.data.data)
