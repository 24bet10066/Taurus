import client from './client';

export const authApi = {
  sendOtp:   (phone)              => client.post('/api/v1/auth/otp/send',   { phone }),
  verifyOtp: (phone, otp)         => client.post('/api/v1/auth/otp/verify', { phone, otp }),
  refresh:   (refreshToken)       => client.post('/api/v1/auth/refresh',    { refreshToken }),
  health:    ()                   => client.get('/api/v1/auth/health'),
};
