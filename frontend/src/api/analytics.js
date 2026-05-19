import client from './client';

export const analyticsApi = {
  dashboard:       ()       => client.get('/api/v1/analytics/dashboard'),
  jobTrend:        (days=7) => client.get('/api/v1/analytics/jobs/trend', { params: { days } }),
  techPerformance: (month)  => client.get('/api/v1/analytics/technicians/performance', { params: { month } }),
  topParts:        (days=30)=> client.get('/api/v1/analytics/parts/top', { params: { days } }),
  revenueBreakdown:()       => client.get('/api/v1/analytics/revenue/breakdown'),
};
