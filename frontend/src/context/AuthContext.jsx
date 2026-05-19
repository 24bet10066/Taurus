import { createContext, useContext, useState, useCallback } from 'react';
import { clearTokens } from '../api/client';

const Ctx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch { return null; }
  });

  const login = useCallback((userData, accessToken, refreshToken) => {
    localStorage.setItem('accessToken',  accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setUser(null);
  }, []);

  return (
    <Ctx.Provider value={{ user, isAuthenticated: !!user, login, logout, role: user?.role }}>
      {children}
    </Ctx.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
};
