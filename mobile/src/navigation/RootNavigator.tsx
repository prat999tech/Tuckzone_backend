import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { useAuth } from '../context/AuthContext';
import { usePushRegistration } from '../hooks/usePushRegistration';
import { LoadingView } from '../components/LoadingView';
import { AuthNavigator } from './AuthNavigator';
import { CustomerNavigator } from './CustomerNavigator';
import { AdminNavigator } from './AdminNavigator';

/**
 * The single place that decides which world the user is in: signed-out, customer
 * (student/teacher/parent), or canteen admin. Everything else assumes it is already in
 * the right world and never has to re-check the role.
 */
export function RootNavigator() {
  const { user, loading, isAdmin } = useAuth();
  usePushRegistration();

  if (loading) {
    return <LoadingView />;
  }

  return (
    <NavigationContainer>
      {!user ? <AuthNavigator /> : isAdmin ? <AdminNavigator /> : <CustomerNavigator />}
    </NavigationContainer>
  );
}
