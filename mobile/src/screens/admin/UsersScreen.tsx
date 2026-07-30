import React, { useCallback, useEffect, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Users as UsersIcon } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Chip } from '../../components/Chip';
import { Button } from '../../components/Button';
import { Badge } from '../../components/Badge';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { colors, spacing, typography } from '../../theme';
import type { Role, UserSummary } from '../../api/types';

const ROLE_FILTERS: (Role | 'ALL')[] = ['ALL', 'STUDENT', 'TEACHER', 'PARENT', 'CANTEEN_ADMIN'];

export function UsersScreen() {
  const [role, setRole] = useState<Role | 'ALL'>('ALL');
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setUsers(await adminApi.listUsers(role === 'ALL' ? undefined : role));
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load accounts') });
    } finally {
      setLoading(false);
    }
  }, [role]);

  useEffect(() => { load(); }, [load]);

  function confirmToggle(user: UserSummary) {
    const disabling = user.status === 'ACTIVE';
    Alert.alert(
      disabling ? 'Disable account?' : 'Enable account?',
      disabling
        ? `${user.fullName} will be signed out and unable to use the app.`
        : `${user.fullName} will be able to sign in again.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: disabling ? 'Disable' : 'Enable',
          style: disabling ? 'destructive' : 'default',
          onPress: async () => {
            setBusyId(user.id);
            try {
              if (disabling) await adminApi.disableUser(user.id);
              else await adminApi.enableUser(user.id);
              load();
            } catch (error) {
              Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Action failed') });
            } finally {
              setBusyId(null);
            }
          },
        },
      ],
    );
  }

  return (
    <ScreenContainer scroll={false} contentStyle={{ padding: 0 }}>
      <View style={styles.header}>
        <Text style={typography.h1}>Accounts</Text>
        <Text style={styles.subtitle}>Enable or disable access</Text>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterRow} contentContainerStyle={styles.filterRowContent}>
        {ROLE_FILTERS.map((r) => (
          <Chip key={r} label={r.replace('_', ' ')} active={role === r} onPress={() => setRole(r)} />
        ))}
      </ScrollView>

      {loading ? (
        <LoadingView />
      ) : (
        <ScrollView style={styles.fill} contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
          {users.length === 0 ? (
            <EmptyState icon={<UsersIcon color={colors.primaryDark} size={26} />} title="No accounts found" />
          ) : (
            users.map((user) => (
              <Card key={user.id} style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={typography.bodyMedium}>{user.fullName}</Text>
                  <Text style={typography.caption}>{user.email} · {user.mobile}</Text>
                  <View style={styles.badgeRow}>
                    <Badge label={user.role} />
                    <Badge label={user.status} />
                  </View>
                </View>
                <Button
                  label={user.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                  variant={user.status === 'ACTIVE' ? 'danger' : 'primary'}
                  fullWidth={false}
                  size="md"
                  loading={busyId === user.id}
                  onPress={() => confirmToggle(user)}
                />
              </Card>
            ))
          )}
        </ScrollView>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  // Lets the list shrink inside the flex:1 screen container instead of overflowing it.
  fill: { flex: 1 },
  header: { paddingHorizontal: spacing.lg, paddingTop: spacing.lg },
  subtitle: { ...typography.bodySmall, marginTop: 2 },
  filterRow: { marginTop: spacing.md, flexGrow: 0 },
  filterRowContent: { paddingHorizontal: spacing.lg },
  list: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxxl },
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  badgeRow: { flexDirection: 'row', gap: spacing.xs, marginTop: spacing.xs },
});
