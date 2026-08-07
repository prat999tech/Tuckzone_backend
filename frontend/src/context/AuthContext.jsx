import React, { createContext, useContext, useEffect, useState } from 'react';
import {
  getMe,
  login as loginRequest,
  loginWithOtp as loginWithOtpRequest,
  requestOtp as requestOtpRequest,
} from '../api/auth';

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

  const applySession = (data) => {
    localStorage.setItem('canteen_token', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('canteen_refresh_token', data.refreshToken);
    }
    localStorage.setItem('canteen_user', JSON.stringify(data.user));
    setUser(data.user);
    return data;
  };

  const login = async (email, password) => {
    const data = await loginRequest({ email, password });
    return applySession(data);
  };

  /** Emails a one-time passcode for sign-in; resendAfterSeconds drives the UI's cooldown. */
  const requestOtp = (email, purpose = 'LOGIN') => requestOtpRequest(email, purpose);

  const loginWithOtp = async (email, code) => {
    const data = await loginWithOtpRequest(email, code);
    return applySession(data);
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
        requestOtp,
        loginWithOtp,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
