import React, { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Bell } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { notificationsApi } from '../../api/notifications';
import { apiErrorMessage } from '../../api/client';
import { formatDateTime } from '../../utils/format';
import { colors, spacing, typography } from '../../theme';
import type { NotificationResponse } from '../../api/types';

/** Shared by both the customer and admin stacks — a notification feed reads the same
 *  whichever role you are. */
export function NotificationsScreen() {
  const [items, setItems] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async (isRefresh = false) => {
    isRefresh ? setRefreshing(true) : setLoading(true);
    try {
      const data = await notificationsApi.getFeed();
      setItems(data);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load notifications') });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  if (loading) return <LoadingView />;

  return (
    <ScreenContainer refreshing={refreshing} onRefresh={() => load(true)}>
      {items.length === 0 ? (
        <EmptyState icon={<Bell color={colors.primaryDark} size={26} />} title="No notifications yet" />
      ) : (
        <View style={{ gap: spacing.md }}>
          {items.map((item) => (
            <Card key={item.id} style={styles.card}>
              <View style={styles.dot} />
              <View style={{ flex: 1 }}>
                <Text style={typography.h3}>{item.title}</Text>
                <Text style={styles.body}>{item.body}</Text>
                <Text style={styles.time}>{formatDateTime(item.createdAt)}</Text>
              </View>
            </Card>
          ))}
        </View>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: 'row', gap: spacing.md },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.primary, marginTop: 6 },
  body: { ...typography.body, color: colors.textSecondary, marginTop: 2 },
  time: { ...typography.caption, marginTop: spacing.xs },
});
