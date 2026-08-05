import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SubAdminTabs } from './SubAdminTabs';
import type { SubAdminStackParamList } from './types';

const Stack = createNativeStackNavigator<SubAdminStackParamList>();

export function SubAdminNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="SubAdminTabs" component={SubAdminTabs} />
    </Stack.Navigator>
  );
}
