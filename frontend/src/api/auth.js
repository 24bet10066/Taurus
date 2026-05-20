import client from './client';

export const authApi = {
  sendOtp: (phone) => client.post('/api/v1/auth/otp/send', { phone, purpose: 'LOGIN' }),

  verifyOtp: (phone, code) => client.post('/api/v1/auth/otp/verify', { phone, otp: code, purpose: 'LOGIN' }),

  refresh: (refreshToken) => client.post('/api/v1/auth/refresh', { refreshToken }),

  logout: () => client.post('/api/v1/auth/logout'),

  me: () => client.get('/api/v1/auth/me'),
};
