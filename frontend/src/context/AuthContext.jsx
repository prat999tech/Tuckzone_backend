import React, { createContext, useContext, useEffect, useState } from 'react';
import { getMe, login as loginRequest } from '../api/auth';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('canteen_token');
      if (token) {
        try {
          const userData = await getMe();
          setUser(userData);
          localStorage.setItem('canteen_user', JSON.stringify(userData));
        } catch (error) {
          localStorage.removeItem('canteen_token');
          localStorage.removeItem('canteen_refresh_token');
          localStorage.removeItem('canteen_user');
          setUser(null);
        }
      }
      setLoading(false);
    };
    initAuth();
  }, []);

  const login = async (email, password) => {
    const data = await loginRequest({ email, password });
    localStorage.setItem('canteen_token', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('canteen_refresh_token', data.refreshToken);
    }
    localStorage.setItem('canteen_user', JSON.stringify(data.user));
    setUser(data.user);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('canteen_token');
    localStorage.removeItem('canteen_refresh_token');
    localStorage.removeItem('canteen_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        role: user?.role,
        loading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
