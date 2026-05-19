import client from './client';

export const customersApi = {
  // Search/list with optional q (name or phone fragment)
  list: (params = {}) => client.get('/api/v1/customers', { params }),

  // Lookup by exact phone — primary entry point from the Customer page
  byPhone: (phone) => client.get(`/api/v1/customers/phone/${phone}`),

  get:        (id) => client.get(`/api/v1/customers/${id}`),
  profile:    (id) => client.get(`/api/v1/customers/${id}/profile`),
  appliances: (id) => client.get(`/api/v1/customers/${id}/appliances`),

  upsert: (data)     => client.post('/api/v1/customers/upsert', data),
  update: (id, data) => client.patch(`/api/v1/customers/${id}`, data),

  addAppliance: (id, data) =>
    client.post(`/api/v1/customers/${id}/appliances`, data),
};
