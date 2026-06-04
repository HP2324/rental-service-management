import client from './client'

export const getListings = () =>
  client.get('/listings').then((r) => r.data.data)

export const searchListings = ({ city, maxPrice, minBedrooms = 0 }) =>
  client
    .get('/listings/search', { params: { city, maxPrice, minBedrooms } })
    .then((r) => r.data.data)

export const getListing = (id) =>
  client.get(`/listings/${id}`).then((r) => r.data.data)

export const getMyListings = () =>
  client.get('/listings/my-listings').then((r) => r.data.data)

export const createListing = (data) =>
  client.post('/listings', data).then((r) => r.data.data)

export const updateListing = (id, data) =>
  client.put(`/listings/${id}`, data).then((r) => r.data.data)

export const deleteListing = (id) =>
  client.delete(`/listings/${id}`).then((r) => r.data)

export const updateListingStatus = (id, status) =>
  client.patch(`/listings/${id}/status`, { status }).then((r) => r.data.data)

export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return client
    .post('/listings/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data.data) // returns the /uploads/filename.jpg path
}
