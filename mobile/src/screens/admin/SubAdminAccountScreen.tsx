import React, { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { LogOut } from 'lucide-react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Button } from '../../components/Button';
import { useAuth } from '../../context/AuthContext';
import { colors, spacing, typography } from '../../theme';

export function SubAdminAccountScreen() {
  const { user, logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);

  async function handleLogout() {
    setLoggingOut(true);
    await logout();
  }

  return (
    <ScreenContainer edges={['top']}>
      <Text style={typography.h1}>Account</Text>
      <Text style={styles.subtitle}>Signed in as {user?.fullName}</Text>
      <Text style={styles.role}>Sub Admin</Text>

      <Button
        label="Sign Out"
        variant="secondary"
        icon={<LogOut size={18} color={colors.textPrimary} />}
        onPress={handleLogout}
        loading={loggingOut}
        style={styles.logoutButton}
      />
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  subtitle: { ...typography.bodySmall, marginTop: 2 },
  role: { ...typography.caption, color: colors.primaryDark, fontWeight: '700', marginTop: spacing.xs },
  logoutButton: { marginTop: spacing.xxl },
});
