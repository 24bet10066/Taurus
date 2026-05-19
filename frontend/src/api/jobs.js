import client from './client';

export const jobsApi = {
  list: (params = {}) => client.get('/api/v1/jobs', { params }),
  get:  (id)          => client.get(`/api/v1/jobs/${id}`),
  mine: ()            => client.get('/api/v1/jobs/my'),

  // Customer history — all past jobs for a phone, newest first.
  byCustomerPhone: (phone, page = 0, size = 50) =>
    client.get('/api/v1/jobs', { params: { customerPhone: phone, page, size } }),

  create: (data)      => client.post('/api/v1/jobs', data),
  publicBook: (data)  => client.post('/api/v1/jobs/public', data),

  updateStatus: (id, status, reason) =>
    client.put(`/api/v1/jobs/${id}/status`, { status, reason }),

  assign: (id, primaryTechId, assistantTechId = null) =>
    client.post(`/api/v1/jobs/${id}/assign`, { primaryTechId, assistantTechId }),

  autoAssign: (id) =>
    client.post(`/api/v1/jobs/${id}/auto-assign`),

  invoice: (id) => client.get(`/api/v1/jobs/${id}/invoice`),
};

export const JOB_STATUS_FLOW = {
  REQUESTED:   { label: 'Requested',   color: '#FFB547', next: ['ASSIGNED',    'CANCELLED'] },
  ASSIGNED:    { label: 'Assigned',    color: '#4C8EFF', next: ['IN_TRANSIT',  'CANCELLED'] },
  IN_TRANSIT:  { label: 'In Transit',  color: '#9B6BFF', next: ['AT_CUSTOMER', 'CANCELLED'] },
  AT_CUSTOMER: { label: 'At Customer', color: '#06B6D4', next: ['IN_PROGRESS', 'CANCELLED'] },
  IN_PROGRESS: { label: 'In Progress', color: '#A855F7', next: ['COMPLETED',   'PARTS_NEEDED', 'CANCELLED'] },
  PARTS_NEEDED:{ label: 'Parts Needed',color: '#F97316', next: ['IN_PROGRESS', 'CANCELLED'] },
  COMPLETED:   { label: 'Completed',   color: '#22D37F', next: [] },
  CANCELLED:   { label: 'Cancelled',   color: '#FF4D6D', next: [] },
};

export const PIPELINE_COLUMNS = ['REQUESTED','ASSIGNED','IN_TRANSIT','AT_CUSTOMER','IN_PROGRESS','PARTS_NEEDED','COMPLETED'];
