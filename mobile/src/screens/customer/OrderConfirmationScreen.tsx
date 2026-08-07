import React, { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { CheckCircle2, Clock, Store, Zap } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { LoadingView } from '../../components/LoadingView';
import { ordersApi } from '../../api/orders';
import { apiErrorMessage } from '../../api/client';
import { classLabel, formatCurrency, orderStatusLabel } from '../../utils/format';
import { colors, radius, spacing, typography } from '../../theme';
import type { OrderResponse } from '../../api/types';
import type { CustomerStackParamList } from '../../navigation/types';

type Props = NativeStackScreenProps<CustomerStackParamList, 'OrderConfirmation'>;

/**
 * Shown immediately after an order is placed. Checkout `replace()`s to this screen rather
 * than pushing, so the hardware back button cannot return to a cart that has already been
 * submitted.
 */
export function OrderConfirmationScreen({ navigation, route }: Props) {
  const { orderId } = route.params;
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      ordersApi
        .getById(orderId)
        .then((data) => {
          if (!cancelled) setOrder(data);
        })
        .catch((error) => {
          if (!cancelled) Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not load your order') });
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
      return () => {
        cancelled = true;
      };
    }, [orderId]),
  );

  function goHome() {
    navigation.reset({ index: 0, routes: [{ name: 'CustomerTabs' }] });
  }

  if (loading) return <ScreenContainer><LoadingView /></ScreenContainer>;
  if (!order) {
    return (
      <ScreenContainer>
        <Text style={typography.h2}>Order not found</Text>
        <Button label="Return to Home" onPress={goHome} style={{ marginTop: spacing.xl }} />
      </ScreenContainer>
    );
  }

  const isTakeaway = order.orderType === 'TAKEAWAY';

  return (
    <ScreenContainer scroll={false} contentStyle={{ padding: 0 }}>
      <ScrollView style={styles.fill} contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {/* ── Hero ───────────────────────────────────────────────────── */}
        <View style={styles.hero}>
          <View style={styles.heroBadge}>
            <CheckCircle2 size={40} color={colors.textOnPrimary} strokeWidth={2.5} />
          </View>
          <Text style={styles.heroTitle}>Order Placed!</Text>
          <Text style={styles.heroSubtitle}>
            Your TuckZone order #{order.orderNumber} is confirmed.
          </Text>
          <View style={styles.statusPill}>
            <Zap size={14} color={colors.success} fill={colors.success} />
            <Text style={styles.statusPillText}>STATUS: {orderStatusLabel(order.status).toUpperCase()}</Text>
          </View>
        </View>

        {/* ── Recess slot ────────────────────────────────────────────── */}
        <Card style={styles.infoCard}>
          <View style={styles.infoIcon}>
            <Clock size={22} color={colors.textSecondary} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.infoValue}>{order.slotName}</Text>
          </View>
        </Card>

        {/* ── Location ───────────────────────────────────────────────── */}
        <Card style={[styles.infoCard, styles.stackGap]}>
          <View style={styles.infoIcon}>
            <Store size={22} color={colors.textSecondary} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.infoLabel}>{isTakeaway ? 'PICK UP AT' : 'DELIVER TO'}</Text>
            <Text style={styles.infoValue}>{isTakeaway ? 'Main Canteen' : order.deliveryLocation}</Text>
            <Text style={styles.infoSub}>
              {isTakeaway
                ? `Show pickup code ${order.pickupCode ?? '—'} at the counter`
                : `For ${order.recipientName}${order.recipientClass ? ` · Class ${classLabel(order.recipientClass, order.recipientSection)}` : ''}`}
            </Text>
          </View>
        </Card>

        {/* ── Order summary ──────────────────────────────────────────── */}
        <Card style={[styles.summaryCard, styles.stackGap]}>
          <Text style={typography.h2}>Order Summary</Text>
          <View style={styles.divider} />

          {order.items.map((item, index) => (
            <View key={`${item.menuItemId}-${index}`}>
              {index > 0 ? <View style={styles.divider} /> : null}
              <View style={styles.itemRow}>
                <View style={styles.thumb}>
                  <Text style={styles.thumbLetter}>{item.itemName.charAt(0)}</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.itemName}>{item.itemName}</Text>
                  <Text style={styles.itemQty}>Qty: {item.quantity}</Text>
                </View>
                <Text style={styles.itemPrice}>{formatCurrency(item.lineTotal)}</Text>
              </View>
            </View>
          ))}

          <View style={styles.divider} />
          <View style={styles.totalRow}>
            <Text style={styles.subtotalLabel}>Subtotal</Text>
            <Text style={styles.subtotalValue}>{formatCurrency(order.totalAmount)}</Text>
          </View>
          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Total Paid</Text>
            <Text style={styles.totalValue}>{formatCurrency(order.totalAmount)}</Text>
          </View>
        </Card>

        <Button
          label="View Receipt"
          size="lg"
          onPress={() => navigation.navigate('OrderDetail', { orderId: order.id })}
          style={styles.primaryAction}
        />
        <Button label="Return to Home" size="lg" variant="secondary" onPress={goHome} />
      </ScrollView>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  scroll: { padding: spacing.xl, paddingBottom: spacing.xxxl },

  hero: {
    alignItems: 'center',
    backgroundColor: colors.successLight,
    borderRadius: radius.xl,
    paddingVertical: spacing.xxxl,
    paddingHorizontal: spacing.xl,
  },
  heroBadge: {
    width: 84,
    height: 84,
    borderRadius: radius.full,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  heroTitle: { ...typography.display, marginTop: spacing.xl, textAlign: 'center' },
  heroSubtitle: {
    ...typography.body,
    color: colors.textSecondary,
    textAlign: 'center',
    marginTop: spacing.sm,
  },
  statusPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginTop: spacing.xl,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.full,
    backgroundColor: colors.primaryLight,
  },
  statusPillText: { ...typography.caption, color: colors.success, letterSpacing: 0.5 },

  stackGap: { marginTop: spacing.lg },
  infoCard: { flexDirection: 'row', alignItems: 'center', gap: spacing.lg, marginTop: spacing.xl },
  infoIcon: {
    width: 48,
    height: 48,
    borderRadius: radius.full,
    backgroundColor: colors.infoLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  infoLabel: { ...typography.caption, letterSpacing: 0.6, color: colors.textSecondary },
  infoValue: { ...typography.h2, marginTop: 2 },
  infoSub: { ...typography.bodySmall, marginTop: 2 },

  summaryCard: { backgroundColor: colors.background, gap: spacing.md },
  divider: { height: 1, backgroundColor: colors.borderLight },
  itemRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.lg, paddingVertical: spacing.md },
  thumb: {
    width: 56,
    height: 56,
    borderRadius: radius.lg,
    backgroundColor: colors.surfaceContainer,
    alignItems: 'center',
    justifyContent: 'center',
  },
  thumbLetter: { ...typography.h2, color: colors.primary },
  itemName: { ...typography.bodyMedium },
  itemQty: { ...typography.bodySmall, marginTop: 2 },
  itemPrice: { ...typography.h3, color: colors.primary },

  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  subtotalLabel: { ...typography.body, color: colors.textSecondary },
  subtotalValue: { ...typography.body },
  totalLabel: { ...typography.h2 },
  totalValue: { ...typography.h2, color: colors.primary },

  primaryAction: { marginTop: spacing.xxxl, marginBottom: spacing.md },
});
