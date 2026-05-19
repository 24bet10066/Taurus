import client from './client';

export const paymentsApi = {
  // Cash payment — the dominant flow for SK Electronics.
  // Sends jobId + amount + optional notes + collectedBy (admin can specify which tech).
  collectCash: (jobId, amount, notes, collectedBy) =>
    client.post('/api/v1/payments/cash', { jobId, amount, notes, collectedBy }),

  // Razorpay order + verification (online flow).
  createOnlineOrder: (jobId) =>
    client.post('/api/v1/payments/online/create-order', { jobId }),
  verifyOnline: (razorpayPayload) =>
    client.post('/api/v1/payments/online/verify', razorpayPayload),

  forJob: (jobId) => client.get(`/api/v1/payments/${jobId}`),

  // End-of-day summary: revenue, cash vs online, parts vs labour.
  dailySummary: (date) =>
    client.get('/api/v1/payments/daily-summary', { params: date ? { date } : {} }),
};
