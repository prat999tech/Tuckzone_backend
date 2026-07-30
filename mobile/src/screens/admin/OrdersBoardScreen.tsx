import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { RefreshCw } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Badge } from '../../components/Badge';
import { Button } from '../../components/Button';
import { Chip } from '../../components/Chip';
import { Input } from '../../components/Input';
import { DateField } from '../../components/DateField';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { colors, spacing, typography } from '../../theme';
import type { OrderResponse, OrderStatus } from '../../api/types';

function todayIso(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

const STATUS_FILTERS: (OrderStatus | 'ALL')[] = [
  'ALL',
  'PLACED',
  'ACCEPTED',
  'PREPARING',
  'PACKED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'REJECTED',
  'CANCELLED',
];

export function OrdersBoardScreen() {
  const [date, setDate] = useState(todayIso());
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'ALL'>('ALL');
  const [deliveryPersonByOrder, setDeliveryPersonByOrder] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listOrders(date);
      setOrders(data);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load orders') });
    } finally {
      setLoading(false);
    }
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function updateStatus(order: OrderResponse, status: OrderStatus) {
    if (status === 'OUT_FOR_DELIVERY' && !deliveryPersonByOrder[order.id]?.trim()) {
      Toast.show({ type: 'error', text1: 'Enter a delivery person name first' });
      return;
    }
    setUpdatingId(order.id);
    try {
      await adminApi.updateOrderStatus(order.id, {
        status,
        deliveryPersonName: deliveryPersonByOrder[order.id]?.trim(),
      });
      Toast.show({ type: 'success', text1: 'Order updated' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Status update failed') });
    } finally {
      setUpdatingId(null);
    }
  }

  const filteredOrders = orders.filter((order) => statusFilter === 'ALL' || order.status === statusFilter);

  function renderActions(order: OrderResponse) {
    const busy = updatingId === order.id;
    switch (order.status) {
      case 'PLACED':
        return (
          <View style={styles.actionRow}>
            <View style={{ flex: 1 }}>
              <Button label="Accept" variant="primary" loading={busy} onPress={() => updateStatus(order, 'ACCEPTED')} />
            </View>
            <View style={{ flex: 1 }}>
              <Button label="Reject" variant="danger" loading={busy} onPress={() => updateStatus(order, 'REJECTED')} />
            </View>
          </View>
        );
      case 'ACCEPTED':
        return <Button label="Start Preparing" loading={busy} onPress={() => updateStatus(order, 'PREPARING')} />;
      case 'PREPARING':
        return <Button label="Mark Packed" loading={busy} onPress={() => updateStatus(order, 'PACKED')} />;
      case 'PACKED':
        return (
          <View style={{ gap: spacing.sm }}>
            <Input
              label="Delivery person"
              placeholder="Who is delivering this?"
              value={deliveryPersonByOrder[order.id] ?? ''}
              onChangeText={(text) => setDeliveryPersonByOrder((prev) => ({ ...prev, [order.id]: text }))}
            />
            <Button label="Dispatch" loading={busy} onPress={() => updateStatus(order, 'OUT_FOR_DELIVERY')} />
          </View>
        );
      case 'OUT_FOR_DELIVERY':
        return <Button label="Mark Delivered" variant="primary" loading={busy} onPress={() => updateStatus(order, 'DELIVERED')} />;
      default:
        return <Text style={styles.noActions}>No actions available</Text>;
    }
  }

  return (
    <ScreenContainer edges={['top']} scroll={false} contentStyle={{ padding: 0 }}>
      <View style={styles.header}>
        <View>
          <Text style={typography.h1}>Kitchen Orders</Text>
          <Text style={styles.subtitle}>Accept, prepare, and dispatch orders</Text>
        </View>
        <Pressable onPress={load} hitSlop={8}>
          <RefreshCw size={20} color={colors.textSecondary} />
        </Pressable>
      </View>

      <View style={styles.dateRow}>
        <DateField label="Date" value={date} onChange={setDate} />
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterRow} contentContainerStyle={styles.filterRowContent}>
        {STATUS_FILTERS.map((status) => (
          <Chip
            key={status}
            label={`${status.replace(/_/g, ' ')} (${status === 'ALL' ? orders.length : orders.filter((o) => o.status === status).length})`}
            active={statusFilter === status}
            onPress={() => setStatusFilter(status)}
          />
        ))}
      </ScrollView>

      {loading ? (
        <LoadingView />
      ) : filteredOrders.length === 0 ? (
        <EmptyState icon={<RefreshCw color={colors.primaryDark} size={26} />} title="No orders found" message="Nothing matches this filter for the selected date." />
      ) : (
        <ScrollView style={styles.fill} contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
          {filteredOrders.map((order) => (
            <Card key={order.id} style={styles.orderCard}>
              <View style={styles.orderHeader}>
                <Text style={typography.h3}>{order.orderNumber}</Text>
                <Badge label={order.status} />
              </View>
              <Text style={styles.slotBadge}>{order.slotName} · {order.deliveryTime}</Text>
              <Text style={styles.recipient}>
                To: <Text style={styles.recipientName}>{order.recipientName}</Text>
              </Text>
              <Text style={styles.location}>
                {order.orderType === 'TAKEAWAY' ? `Takeaway · Code ${order.pickupCode}` : order.deliveryLocation}
              </Text>

              <View style={styles.itemsList}>
                {order.items.map((item) => (
                  <Text key={item.menuItemId} style={styles.itemLine}>
                    <Text style={styles.itemQty}>{item.quantity}x</Text> {item.itemName}
                  </Text>
                ))}
              </View>

              <View style={styles.actionsWrap}>{renderActions(order)}</View>
            </Card>
          ))}
        </ScrollView>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  // Lets the list shrink inside the flex:1 screen container instead of overflowing it.
  fill: { flex: 1 },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.lg,
  },
  subtitle: { ...typography.bodySmall, marginTop: 2 },
  dateRow: { paddingHorizontal: spacing.lg, marginTop: spacing.md },
  filterRow: { marginTop: spacing.md, flexGrow: 0 },
  filterRowContent: { paddingHorizontal: spacing.lg },
  list: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxxl },
  orderCard: { gap: 2 },
  orderHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  slotBadge: { ...typography.caption, marginTop: 2 },
  recipient: { ...typography.bodySmall, marginTop: spacing.sm },
  recipientName: { fontWeight: '700', color: colors.textPrimary },
  location: { ...typography.bodySmall },
  itemsList: { marginTop: spacing.sm, marginBottom: spacing.md },
  itemLine: { ...typography.bodySmall, color: colors.textPrimary },
  itemQty: { fontWeight: '700', color: colors.primaryDark },
  actionsWrap: { marginTop: spacing.xs },
  actionRow: { flexDirection: 'row', gap: spacing.sm },
  noActions: { ...typography.caption, textAlign: 'center', paddingVertical: spacing.sm },
});
