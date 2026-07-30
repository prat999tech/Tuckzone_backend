import React, { useCallback, useState } from 'react';
import {
  Alert,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Bell, CalendarClock, UtensilsCrossed } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Chip } from '../../components/Chip';
import { Button } from '../../components/Button';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { QuantityStepper } from '../../components/QuantityStepper';
import { DateField } from '../../components/DateField';
import { CartBar } from '../../components/CartBar';
import { menuApi } from '../../api/menu';
import { apiErrorMessage } from '../../api/client';
import { useCart } from '../../context/CartContext';
import { formatCurrency, formatDate } from '../../utils/format';
import { colors, radius, spacing, typography } from '../../theme';
import type {
  DailyMenuItemResponse,
  FoodType,
  MenuCategory,
  OrderingWindowResponse,
} from '../../api/types';
import type { CustomerStackParamList } from '../../navigation/types';

const CATEGORY_OPTIONS: { value: MenuCategory | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'MEALS', label: 'Meals' },
  { value: 'SNACKS', label: 'Snacks' },
  { value: 'DRINKS', label: 'Drinks' },
  { value: 'COMBOS', label: 'Combos' },
];

function toIso(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function todayIso(): string {
  return toIso(new Date());
}

function tomorrowIso(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return toIso(date);
}

export function MenuScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<CustomerStackParamList>>();
  const { addToCart, startNewCartWith, removeFromCart, quantityOf, itemCount, total } = useCart();

  const [date, setDate] = useState(todayIso());
  const [foodType, setFoodType] = useState<FoodType | 'ALL'>('ALL');
  const [category, setCategory] = useState<MenuCategory | 'ALL'>('ALL');
  const [items, setItems] = useState<DailyMenuItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [windows, setWindows] = useState<OrderingWindowResponse[] | null>(null);
  const [lastAdded, setLastAdded] = useState<string | null>(null);

  const isToday = date === todayIso();

  /**
   * Closed only when the canteen has published slots and none of them is still accepting.
   * An empty list means no slots are configured yet, which is a setup gap rather than a
   * closed canteen — treating it as closed would lock everyone out of a working menu.
   */
  const orderingClosed = windows !== null && windows.length > 0
    && !windows.some((window) => window.acceptingOrders);

  const loadMenu = useCallback(async () => {
    setLoading(true);
    try {
      // Fetched together so the menu never renders "Add" buttons for a date whose ordering
      // window has already shut — previously that only surfaced as a failure at checkout.
      const [data, status] = await Promise.all([
        menuApi.getMenu({
          date,
          foodType: foodType === 'ALL' ? undefined : foodType,
          category: category === 'ALL' ? undefined : category,
        }),
        menuApi.getOrderingStatus(date).catch(() => null),
      ]);
      setItems(data);
      setWindows(status);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load menu') });
    } finally {
      setLoading(false);
    }
  }, [date, foodType, category]);

  useFocusEffect(
    useCallback(() => {
      loadMenu();
    }, [loadMenu]),
  );

  const announceAdded = useCallback((name: string) => {
    setLastAdded(name);
  }, []);

  const handleAdd = useCallback(
    (dayItem: DailyMenuItemResponse) => {
      const result = addToCart(dayItem);

      if (result.status === 'added') {
        announceAdded(dayItem.menuItem.name);
        return;
      }

      if (result.status === 'stock-limit') {
        Toast.show({
          type: 'info',
          text1: `Only ${result.remaining} left`,
          text2: 'Your cart already has every remaining portion.',
        });
        return;
      }

      // One order covers one day. Rather than silently mixing dates — which used to make
      // checkout submit whichever date was added first — offer the Swiggy-style reset.
      Alert.alert(
        'Start a new cart?',
        `Your cart has items for ${formatDate(result.currentDate)}. An order covers a single day, so ordering for ${formatDate(result.attemptedDate)} means clearing it first.`,
        [
          { text: 'Keep my cart', style: 'cancel' },
          {
            text: 'Clear & add',
            style: 'destructive',
            onPress: () => {
              startNewCartWith(dayItem);
              announceAdded(dayItem.menuItem.name);
            },
          },
        ],
      );
    },
    [addToCart, startNewCartWith, announceAdded],
  );

  return (
    <ScreenContainer edges={['top']} scroll={false} contentStyle={{ padding: 0 }}>
      <View style={styles.header}>
        <View style={{ flex: 1 }}>
          <Text style={typography.h1}>Today&apos;s Menu</Text>
          <Text style={styles.subtitle}>Order fresh meals for your classroom</Text>
        </View>
        <Pressable style={styles.bellButton} onPress={() => navigation.navigate('Notifications')} hitSlop={8}>
          <Bell size={22} color={colors.textPrimary} />
        </Pressable>
      </View>

      <View style={styles.controls}>
        <DateField label="Menu date" value={date} onChange={setDate} minimumDate={new Date()} />
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow} contentContainerStyle={styles.chipRowContent}>
        <Chip label="All" active={foodType === 'ALL'} onPress={() => setFoodType('ALL')} />
        <Chip label="Veg" dotColor={colors.veg} active={foodType === 'VEG'} onPress={() => setFoodType('VEG')} />
        <Chip label="Non-Veg" dotColor={colors.nonVeg} active={foodType === 'NON_VEG'} onPress={() => setFoodType('NON_VEG')} />
      </ScrollView>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow} contentContainerStyle={styles.chipRowContent}>
        {CATEGORY_OPTIONS.map((option) => (
          <Chip key={option.value} label={option.label} active={category === option.value} onPress={() => setCategory(option.value)} />
        ))}
      </ScrollView>

      {!loading && orderingClosed && (
        <Card style={styles.closedCard}>
          <View style={styles.closedIcon}>
            <CalendarClock size={20} color={colors.warning} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.closedTitle}>
              {isToday ? 'Canteen is closed for today' : `Ordering closed for ${formatDate(date)}`}
            </Text>
            <Text style={styles.closedMessage}>
              {isToday
                ? 'School hours are over and today’s slots have stopped taking orders. You can pre-order for tomorrow.'
                : 'All slots for this date have stopped taking orders.'}
            </Text>
            {isToday && (
              <Button
                label="Pre-order for tomorrow"
                size="md"
                variant="secondary"
                style={styles.closedButton}
                onPress={() => setDate(tomorrowIso())}
              />
            )}
          </View>
        </Card>
      )}

      {loading ? (
        <LoadingView />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<UtensilsCrossed color={colors.primaryDark} size={30} />}
          title="No menu published"
          message={`The canteen hasn't published a menu for ${date} yet.`}
        />
      ) : (
        <ScrollView
          style={styles.fill}
          contentContainerStyle={styles.grid}
          showsVerticalScrollIndicator={false}
        >
          {items.map((dayItem) => {
            const soldOut = !dayItem.available || dayItem.remainingQuantity <= 0;
            const qty = quantityOf(dayItem.id);
            return (
              <Card key={dayItem.id} style={[styles.foodCard, soldOut && styles.foodCardSoldOut]}>
                <View style={styles.imageWrap}>
                  {dayItem.menuItem.imageUrl ? (
                    <Image source={{ uri: dayItem.menuItem.imageUrl }} style={styles.image} />
                  ) : (
                    <View style={styles.imageFallback}>
                      <Text style={styles.imageFallbackText}>{dayItem.menuItem.name.charAt(0)}</Text>
                    </View>
                  )}
                  <View
                    style={[
                      styles.dot,
                      { borderColor: dayItem.menuItem.foodType === 'VEG' ? colors.veg : colors.nonVeg },
                    ]}
                  >
                    <View
                      style={[
                        styles.dotInner,
                        { backgroundColor: dayItem.menuItem.foodType === 'VEG' ? colors.veg : colors.nonVeg },
                      ]}
                    />
                  </View>
                  {soldOut && (
                    <View style={styles.soldOutBadge}>
                      <Text style={styles.soldOutText}>Sold Out</Text>
                    </View>
                  )}
                </View>

                <Text style={styles.foodName} numberOfLines={1}>
                  {dayItem.menuItem.name}
                </Text>
                {dayItem.menuItem.description ? (
                  <Text style={styles.foodDesc} numberOfLines={1}>
                    {dayItem.menuItem.description}
                  </Text>
                ) : null}

                <View style={styles.metaRow}>
                  <Text style={styles.price}>{formatCurrency(dayItem.menuItem.price)}</Text>
                  <Text style={styles.stock}>{dayItem.remainingQuantity} left</Text>
                </View>

                {qty > 0 ? (
                  <QuantityStepper
                    quantity={qty}
                    onIncrement={() => handleAdd(dayItem)}
                    onDecrement={() => removeFromCart(dayItem.id)}
                    incrementDisabled={qty >= dayItem.remainingQuantity || orderingClosed}
                  />
                ) : (
                  <Button
                    label={orderingClosed ? 'Closed' : 'Add'}
                    size="md"
                    disabled={soldOut || orderingClosed}
                    onPress={() => handleAdd(dayItem)}
                  />
                )}
              </Card>
            );
          })}
        </ScrollView>
      )}

      <CartBar
        itemCount={itemCount}
        total={total}
        lastAdded={lastAdded}
        onPress={() => navigation.navigate('Checkout')}
      />
    </ScreenContainer>
  );
}

const CARD_WIDTH = '48%';

const styles = StyleSheet.create({
  // Lets the list shrink inside the flex:1 screen container instead of overflowing it.
  fill: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.lg,
  },
  subtitle: { ...typography.bodySmall, marginTop: 2 },
  bellButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.border,
  },
  controls: { paddingHorizontal: spacing.lg, marginTop: spacing.md },
  chipRow: { marginTop: spacing.md, flexGrow: 0 },
  chipRowContent: { paddingHorizontal: spacing.lg },
  closedCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.md,
    marginHorizontal: spacing.lg,
    marginTop: spacing.lg,
    backgroundColor: colors.warningLight,
    borderColor: colors.warning,
  },
  closedIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closedTitle: { ...typography.h3 },
  closedMessage: { ...typography.bodySmall, marginTop: 2 },
  closedButton: { marginTop: spacing.md, alignSelf: 'flex-start' },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.lg,
    paddingBottom: 100,
  },
  foodCard: { width: CARD_WIDTH, marginBottom: spacing.lg, padding: spacing.md },
  foodCardSoldOut: { opacity: 0.6 },
  imageWrap: { position: 'relative', marginBottom: spacing.sm },
  image: { width: '100%', height: 90, borderRadius: radius.md, backgroundColor: colors.borderLight },
  imageFallback: {
    width: '100%',
    height: 90,
    borderRadius: radius.md,
    backgroundColor: colors.primarySurface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  imageFallbackText: { fontSize: 30, fontWeight: '700', color: colors.primaryDark },
  dot: {
    position: 'absolute',
    top: 6,
    left: 6,
    width: 18,
    height: 18,
    borderRadius: 4,
    borderWidth: 1.5,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dotInner: { width: 8, height: 8, borderRadius: 4 },
  soldOutBadge: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderRadius: radius.md,
    backgroundColor: colors.overlay,
    alignItems: 'center',
    justifyContent: 'center',
  },
  soldOutText: { color: colors.textOnPrimary, fontWeight: '700', fontSize: 12 },
  foodName: { ...typography.h3 },
  foodDesc: { ...typography.caption, marginTop: 2 },
  metaRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: spacing.sm, marginBottom: spacing.sm },
  price: { fontSize: 15, fontWeight: '700', color: colors.textPrimary },
  stock: { ...typography.caption },
});
