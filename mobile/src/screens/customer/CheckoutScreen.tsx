import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import * as Crypto from 'expo-crypto';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AlertTriangle, ShoppingCart, Trash2 } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Chip } from '../../components/Chip';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { SegmentedControl } from '../../components/SegmentedControl';
import { QuantityStepper } from '../../components/QuantityStepper';
import { EmptyState } from '../../components/EmptyState';
import { menuApi } from '../../api/menu';
import { ordersApi } from '../../api/orders';
import { parentApi } from '../../api/parent';
import { apiErrorMessage } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { formatCurrency, formatDate } from '../../utils/format';
import { colors, spacing, typography } from '../../theme';
import type { DeliverySlotResponse, ChildResponse, OrderingWindowResponse, OrderType } from '../../api/types';
import type { CustomerStackParamList } from '../../navigation/types';

type Props = NativeStackScreenProps<CustomerStackParamList, 'Checkout'>;

export function CheckoutScreen({ navigation }: Props) {
  const { user } = useAuth();
  const { lines, total, cartDate, clearCart, addToCart, removeFromCart, removeLine } = useCart();

  // Generated exactly once per checkout attempt (this screen mount), and reused across
  // retries. A key regenerated on every tap would make double-tap protection worthless —
  // two different keys look like two legitimate orders to the backend.
  const idempotencyKeyRef = useRef(Crypto.randomUUID());

  // Taken from the cart itself, which now guarantees every line shares one date. Reading
  // it off lines[0] used to submit the first-added item's date after the shopper switched
  // days, producing "you can't order for <the old date>" at the final step.
  const menuDate = cartDate ?? '';

  const [slots, setSlots] = useState<DeliverySlotResponse[]>([]);
  const [selectedSlotId, setSelectedSlotId] = useState<string>('');
  const [orderingStatus, setOrderingStatus] = useState<OrderingWindowResponse | null>(null);

  const [orderType, setOrderType] = useState<OrderType>('DELIVERY');
  const [deliveryLocation, setDeliveryLocation] = useState('');
  const [children, setChildren] = useState<ChildResponse[]>([]);
  const [selectedChildId, setSelectedChildId] = useState('');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [placing, setPlacing] = useState(false);

  const isParent = user?.role === 'PARENT';
  const isTeacher = user?.role === 'TEACHER';

  useEffect(() => {
    menuApi.getDeliverySlots().then((data) => {
      setSlots(data);
      if (data.length > 0) setSelectedSlotId(data[0].id);
    });
    if (isParent) {
      parentApi.getChildren().then(setChildren).catch(() => undefined);
    }
  }, [isParent]);

  useEffect(() => {
    if (!selectedSlotId || !menuDate) return;
    menuApi
      .getOrderingStatus(menuDate)
      .then((statuses) => {
        setOrderingStatus(statuses.find((s) => s.slotId === selectedSlotId) ?? null);
      })
      .catch(() => setOrderingStatus(null));
  }, [selectedSlotId, menuDate]);

  const selectedSlot = useMemo(() => slots.find((s) => s.id === selectedSlotId), [slots, selectedSlotId]);
  const closedForOrdering = orderingStatus !== null && !orderingStatus.acceptingOrders;

  function validate(): boolean {
    const next: Record<string, string> = {};
    if (!selectedSlotId) next.slot = 'Please select a delivery slot';
    if (isParent && !selectedChildId) next.child = 'Please select which child this order is for';
    if (!isParent && orderType === 'DELIVERY' && !deliveryLocation.trim()) {
      next.location = 'Delivery location is required';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handlePlaceOrder() {
    if (lines.length === 0) return;
    if (!validate()) return;
    if (closedForOrdering) {
      Toast.show({ type: 'error', text1: 'Ordering is closed for this slot' });
      return;
    }

    setPlacing(true);
    try {
      await ordersApi.place({
        slotId: selectedSlotId,
        orderType: isTeacher ? orderType : 'DELIVERY',
        menuDate,
        beneficiaryStudentProfileId: isParent ? selectedChildId : undefined,
        deliveryLocation: isParent ? undefined : deliveryLocation.trim(),
        items: lines.map((line) => ({ menuItemId: line.dayItem.menuItem.id, quantity: line.quantity })),
        idempotencyKey: idempotencyKeyRef.current,
      });
      clearCart();
      Toast.show({ type: 'success', text1: 'Order placed!', text2: 'Track it from the Orders tab' });
      navigation.navigate('CustomerTabs', { screen: 'Orders' } as never);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to place order') });
    } finally {
      setPlacing(false);
    }
  }

  function confirmRemoveLine(dayItemId: string, name: string) {
    Alert.alert('Remove item?', `${name} will be removed from your cart.`, [
      { text: 'Keep', style: 'cancel' },
      { text: 'Remove', style: 'destructive', onPress: () => removeLine(dayItemId) },
    ]);
  }

  function confirmClearCart() {
    Alert.alert('Empty your cart?', 'All items will be removed.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Empty cart', style: 'destructive', onPress: clearCart },
    ]);
  }

  if (lines.length === 0) {
    return (
      <ScreenContainer>
        <EmptyState
          icon={<ShoppingCart color={colors.primaryDark} size={30} />}
          title="Your cart is empty"
          message="Add something from the menu and it will show up here."
        />
        <Button label="Browse the menu" onPress={() => navigation.goBack()} />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <View style={styles.summaryHeader}>
        <View style={{ flex: 1 }}>
          <Text style={typography.h2}>Order Summary</Text>
          {menuDate ? <Text style={styles.forDate}>For {formatDate(menuDate)}</Text> : null}
        </View>
        <Pressable onPress={confirmClearCart} hitSlop={8}>
          <Text style={styles.clearCart}>Empty cart</Text>
        </Pressable>
      </View>

      <Card style={styles.itemsCard}>
        {lines.map((line, index) => (
          <View key={line.dayItem.id}>
            {index > 0 ? <View style={styles.divider} /> : null}
            <View style={styles.itemRow}>
              <View style={styles.itemInfo}>
                <Text style={styles.itemName} numberOfLines={1}>
                  {line.dayItem.menuItem.name}
                </Text>
                <Text style={styles.itemUnit}>{formatCurrency(line.dayItem.menuItem.price)} each</Text>
              </View>
              <Text style={styles.itemPrice}>
                {formatCurrency(line.dayItem.menuItem.price * line.quantity)}
              </Text>
            </View>
            <View style={styles.itemControls}>
              <QuantityStepper
                compact
                quantity={line.quantity}
                onIncrement={() => addToCart(line.dayItem)}
                onDecrement={() => removeFromCart(line.dayItem.id)}
                incrementDisabled={line.quantity >= line.dayItem.remainingQuantity}
              />
              <Pressable
                onPress={() => confirmRemoveLine(line.dayItem.id, line.dayItem.menuItem.name)}
                hitSlop={8}
                style={styles.removeButton}
              >
                <Trash2 size={15} color={colors.danger} />
                <Text style={styles.removeText}>Remove</Text>
              </Pressable>
            </View>
          </View>
        ))}
        <View style={styles.divider} />
        <View style={styles.itemRow}>
          <Text style={styles.totalLabel}>Total</Text>
          <Text style={styles.totalValue}>{formatCurrency(total)}</Text>
        </View>
      </Card>

      <Text style={[typography.h2, styles.sectionTitle]}>Delivery Slot</Text>
      <View style={styles.slotRow}>
        {slots.map((slot) => (
          <Chip
            key={slot.id}
            label={`${slot.name} (${slot.deliveryTime})`}
            active={selectedSlotId === slot.id}
            onPress={() => setSelectedSlotId(slot.id)}
          />
        ))}
      </View>
      {errors.slot ? <Text style={styles.errorText}>{errors.slot}</Text> : null}

      {closedForOrdering && (
        <Card style={styles.warningCard}>
          <AlertTriangle size={18} color={colors.warning} />
          <Text style={styles.warningText}>
            Ordering for {selectedSlot?.name} on {menuDate} is currently closed
            {orderingStatus?.reason ? ` (${orderingStatus.reason})` : ''}.
          </Text>
        </Card>
      )}

      {isTeacher && (
        <>
          <Text style={[typography.h2, styles.sectionTitle]}>How will you get it?</Text>
          <SegmentedControl
            options={[
              { value: 'DELIVERY', label: 'Delivery' },
              { value: 'TAKEAWAY', label: 'Takeaway' },
            ]}
            value={orderType}
            onChange={setOrderType}
          />
        </>
      )}

      {isParent ? (
        <>
          <Text style={[typography.h2, styles.sectionTitle]}>Order For</Text>
          <View style={styles.slotRow}>
            {children.map((child) => (
              <Chip
                key={child.linkId}
                label={`${child.fullName} (${child.studentClass}-${child.section})`}
                active={selectedChildId === child.studentProfileId}
                onPress={() => setSelectedChildId(child.studentProfileId)}
              />
            ))}
          </View>
          {errors.child ? <Text style={styles.errorText}>{errors.child}</Text> : null}
        </>
      ) : orderType === 'DELIVERY' ? (
        <View style={styles.sectionTitle}>
          <Input
            label="Delivery Location"
            required
            placeholder={isTeacher ? 'e.g. Staff Room / Science Dept' : 'e.g. Class 10-A'}
            value={deliveryLocation}
            onChangeText={setDeliveryLocation}
            error={errors.location}
          />
        </View>
      ) : (
        <Card style={[styles.warningCard, { marginTop: spacing.lg, backgroundColor: colors.primarySurface }]}>
          <Text style={styles.pickupText}>
            Show your pickup code at the canteen counter once your order is packed.
          </Text>
        </Card>
      )}

      <Button
        label={`Pay ${formatCurrency(total)} & Place Order`}
        onPress={handlePlaceOrder}
        loading={placing}
        disabled={closedForOrdering}
        style={styles.placeButton}
      />
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  summaryHeader: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md },
  forDate: { ...typography.bodySmall, marginTop: 2 },
  clearCart: { ...typography.bodySmall, color: colors.danger, fontWeight: '700' },
  itemsCard: { marginTop: spacing.md },
  itemRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: spacing.xs },
  itemInfo: { flex: 1, paddingRight: spacing.md },
  itemName: { ...typography.body },
  itemUnit: { ...typography.caption, marginTop: 1 },
  itemControls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.xs,
    marginBottom: spacing.xs,
  },
  removeButton: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  removeText: { ...typography.caption, color: colors.danger, fontWeight: '700' },
  itemPrice: { ...typography.bodyMedium },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: colors.border, marginVertical: spacing.sm },
  totalLabel: { ...typography.h3 },
  totalValue: { ...typography.h3, color: colors.primaryDark },
  sectionTitle: { marginTop: spacing.xl, marginBottom: spacing.sm },
  slotRow: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  errorText: { ...typography.caption, color: colors.danger, marginTop: spacing.xs, fontWeight: '600' },
  warningCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginTop: spacing.md,
    backgroundColor: colors.warningLight,
    borderColor: colors.warning,
  },
  warningText: { ...typography.bodySmall, color: colors.textPrimary, flex: 1 },
  pickupText: { ...typography.body },
  placeButton: { marginTop: spacing.xxl },
});
