import client from './client';

export const configApi = {
  getAll: () => client.get('/api/v1/config').then((r) => r.data.data ?? r.data),
  get: (key) => client.get(`/api/v1/config/${key}`).then((r) => r.data.data?.value ?? r.data.value),
};
