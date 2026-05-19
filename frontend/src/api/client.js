import axios from 'axios';

const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const client = axios.create({ baseURL: BASE, timeout: 15000 });

// ── Request: attach token ──────────────────────────────────────────────────
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Response: 401 → refresh → retry once ──────────────────────────────────
let refreshing = false;
let waitQueue = [];

const drain = (err, token) => {
  waitQueue.forEach(({ resolve, reject }) => err ? reject(err) : resolve(token));
  waitQueue = [];
};

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status !== 401 || original._retry) return Promise.reject(error);

    original._retry = true;

    if (refreshing) {
      return new Promise((resolve, reject) => {
        waitQueue.push({ resolve, reject });
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`;
        return client(original);
      });
    }

    refreshing = true;
    const refreshToken = localStorage.getItem('refreshToken');

    if (!refreshToken) {
      clearTokens();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    try {
      const { data } = await axios.post(`${BASE}/api/v1/auth/refresh`, { refreshToken });
      const newAccess = data.data?.accessToken || data.accessToken;
      localStorage.setItem('accessToken', newAccess);
      if (data.data?.refreshToken) localStorage.setItem('refreshToken', data.data.refreshToken);
      drain(null, newAccess);
      original.headers.Authorization = `Bearer ${newAccess}`;
      return client(original);
    } catch (err) {
      drain(err, null);
      clearTokens();
      window.location.href = '/login';
      return Promise.reject(err);
    } finally {
      refreshing = false;
    }
  }
);

export function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

export default client;
