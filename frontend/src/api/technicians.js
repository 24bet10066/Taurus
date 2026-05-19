import client from './client';

export const techApi = {
  list:       (params = {}) => client.get('/api/v1/technicians', { params }),
  get:        (id)          => client.get(`/api/v1/technicians/${id}`),
  create:     (data)        => client.post('/api/v1/technicians', data),
  update:     (id, data)    => client.patch(`/api/v1/technicians/${id}`, data),
  creditPage: (id)          => client.get(`/api/v1/parts/credit/${id}`),
};
