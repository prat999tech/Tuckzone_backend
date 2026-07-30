import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { UtensilsCrossed, ShoppingBag, Wallet, Users, User } from 'lucide-react-native';
import { colors } from '../theme';
import { useAuth } from '../context/AuthContext';
import { MenuScreen } from '../screens/customer/MenuScreen';
import { OrdersScreen } from '../screens/customer/OrdersScreen';
import { WalletScreen } from '../screens/customer/WalletScreen';
import { ChildrenScreen } from '../screens/customer/ChildrenScreen';
import { ProfileScreen } from '../screens/customer/ProfileScreen';
import type { CustomerTabParamList } from './types';

const Tab = createBottomTabNavigator<CustomerTabParamList>();

/** Parents get an extra "Children" tab; students and teachers do not need it. */
export function CustomerTabs() {
  const { user } = useAuth();
  const isParent = user?.role === 'PARENT';
  const insets = useSafeAreaInsets();

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primaryDark,
        tabBarInactiveTintColor: colors.textTertiary,
        // A hardcoded height overrides the inset React Navigation would otherwise add,
        // which is what pushed the tabs underneath the 3-button navigation bar. Adding
        // insets.bottom covers both cases: it is the nav-bar height on button phones and
        // the smaller gesture-pill allowance on gesture phones (0 when neither exists).
        tabBarStyle: {
          borderTopColor: colors.border,
          height: 60 + insets.bottom,
          paddingBottom: 8 + insets.bottom,
          paddingTop: 8,
        },
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
      }}
    >
      <Tab.Screen
        name="Menu"
        component={MenuScreen}
        options={{ tabBarIcon: ({ color, size }) => <UtensilsCrossed color={color} size={size} /> }}
      />
      <Tab.Screen
        name="Orders"
        component={OrdersScreen}
        options={{ tabBarIcon: ({ color, size }) => <ShoppingBag color={color} size={size} /> }}
      />
      <Tab.Screen
        name="Wallet"
        component={WalletScreen}
        options={{ tabBarIcon: ({ color, size }) => <Wallet color={color} size={size} /> }}
      />
      {isParent && (
        <Tab.Screen
          name="Children"
          component={ChildrenScreen}
          options={{ tabBarIcon: ({ color, size }) => <Users color={color} size={size} /> }}
        />
      )}
      <Tab.Screen
        name="Profile"
        component={ProfileScreen}
        options={{ tabBarIcon: ({ color, size }) => <User color={color} size={size} /> }}
      />
    </Tab.Navigator>
  );
}
