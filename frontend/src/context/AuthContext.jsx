import React, { createContext, useContext, useEffect, useState } from 'react';
import {
  getMe,
  login as loginRequest,
  loginWithOtp as loginWithOtpRequest,
  requestOtp as requestOtpRequest,
  firebaseExchange,
  firebaseRegisterStudent,
  firebaseRegisterTeacher,
  firebaseRegisterParent,
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

  /** Signs in with a verified Firebase ID token. Throws with response.data.code ===
   *  'FIREBASE_USER_NOT_REGISTERED' when the identity needs to complete registration first
   *  — callers should catch that specifically and route to one of the calls below. */
  const loginWithFirebase = async (idToken) => {
    const data = await firebaseExchange(idToken);
    return applySession(data);
  };

  const registerStudentWithFirebase = async (payload) => {
    const data = await firebaseRegisterStudent(payload);
    return applySession(data);
  };

  const registerTeacherWithFirebase = async (payload) => {
    const data = await firebaseRegisterTeacher(payload);
    return applySession(data);
  };

  const registerParentWithFirebase = async (payload) => {
    const data = await firebaseRegisterParent(payload);
    return applySession(data);
  };

  /** Re-fetches the caller's own profile and refreshes the cached copy — call after a
   *  successful profile edit so the sidebar/header reflect the new name immediately. */
  const refreshProfile = async () => {
    const userData = await getMe();
    setUser(userData);
    localStorage.setItem('canteen_user', JSON.stringify(userData));
    return userData;
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
        loginWithFirebase,
        registerStudentWithFirebase,
        registerTeacherWithFirebase,
        registerParentWithFirebase,
        refreshProfile,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
