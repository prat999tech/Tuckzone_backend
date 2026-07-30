import React, { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Package, ChevronRight } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Badge } from '../../components/Badge';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { ordersApi } from '../../api/orders';
import { apiErrorMessage } from '../../api/client';
import { formatCurrency, formatDate } from '../../utils/format';
import { colors, spacing, typography } from '../../theme';
import type { OrderResponse } from '../../api/types';
import type { CustomerStackParamList } from '../../navigation/types';

export function OrdersScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<CustomerStackParamList>>();
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async (isRefresh = false) => {
    isRefresh ? setRefreshing(true) : setLoading(true);
    try {
      const data = await ordersApi.myOrders();
      setOrders(data);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load orders') });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Real-time-enough tracking: re-fetch every time this tab is focused, so a push
  // notification about a status change is reflected the moment the user checks the tab.
  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  if (loading) return <LoadingView />;

  return (
    <ScreenContainer edges={['top']} refreshing={refreshing} onRefresh={() => load(true)}>
      <Text style={typography.h1}>My Orders</Text>
      <Text style={styles.subtitle}>Track your food deliveries</Text>

      {orders.length === 0 ? (
        <EmptyState
          icon={<Package color={colors.primaryDark} size={30} />}
          title="No orders yet"
          message="Head to the Menu tab to place your first order."
        />
      ) : (
        <View style={styles.list}>
          {orders.map((order) => (
            <Pressable key={order.id} onPress={() => navigation.navigate('OrderDetail', { orderId: order.id })}>
              <Card style={styles.card}>
                <View style={styles.cardHeader}>
                  <View>
                    <Text style={typography.h3}>{order.orderNumber}</Text>
                    <Text style={styles.date}>{formatDate(order.createdAt)}</Text>
                  </View>
                  <Badge label={order.status} />
                </View>
                <View style={styles.cardFooter}>
                  <Text style={styles.slotText}>
                    {order.slotName} · {order.orderType === 'TAKEAWAY' ? 'Takeaway' : order.deliveryLocation}
                  </Text>
                  <View style={styles.footerRight}>
                    <Text style={styles.amount}>{formatCurrency(order.totalAmount)}</Text>
                    <ChevronRight size={18} color={colors.textTertiary} />
                  </View>
                </View>
              </Card>
            </Pressable>
          ))}
        </View>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  subtitle: { ...typography.bodySmall, marginTop: 2, marginBottom: spacing.lg },
  list: { gap: spacing.md },
  card: { marginBottom: 0 },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  date: { ...typography.caption, marginTop: 2 },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: spacing.md,
    paddingTop: spacing.md,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.borderLight,
  },
  slotText: { ...typography.bodySmall, flex: 1 },
  footerRight: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  amount: { ...typography.bodyMedium },
});
