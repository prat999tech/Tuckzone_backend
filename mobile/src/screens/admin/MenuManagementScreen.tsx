import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Plus, Pencil, Trash2, Power } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Chip } from '../../components/Chip';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { SegmentedControl } from '../../components/SegmentedControl';
import { FormModal } from '../../components/FormModal';
import { DateField } from '../../components/DateField';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi, MenuItemRequest } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { formatCurrency } from '../../utils/format';
import { colors, spacing, typography } from '../../theme';
import type { DailyMenuItemResponse, MenuItemResponse, MenuType } from '../../api/types';

function todayIso(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

type ViewMode = 'daily' | 'fixed';

export function MenuManagementScreen() {
  const [view, setView] = useState<ViewMode>('daily');
  return (
    <ScreenContainer edges={['top']} scroll={false} contentStyle={{ padding: 0 }}>
      <View style={styles.header}>
        <Text style={typography.h1}>Menu Management</Text>
        <Text style={styles.subtitle}>Manage items, prices and availability</Text>
        <View style={styles.segmentWrap}>
          <SegmentedControl
            options={[
              { value: 'daily', label: 'Meal of the Day' },
              { value: 'fixed', label: 'Daily Delights' },
            ]}
            value={view}
            onChange={setView}
          />
        </View>
      </View>
      {/* One ScreenContainer/ScrollView per tab — CatalogSection and DailyScheduleView
          render their content inline (no ScreenContainer of their own) so the whole tab
          scrolls as a single list instead of nesting two independent scroll views. */}
      <ScreenContainer contentStyle={{ paddingTop: spacing.md }}>
        {view === 'daily' ? (
          <>
            <CatalogSection menuType="DAILY" />
            <View style={styles.sectionGap} />
            <DailyScheduleView />
          </>
        ) : (
          <CatalogSection menuType="FIXED" />
        )}
      </ScreenContainer>
    </ScreenContainer>
  );
}

// ─── Catalog (shared by both menu types) ──────────────────────────────────

function CatalogSection({ menuType }: { menuType: MenuType }) {
  const { user } = useAuth();
  // Sub Admin can edit and toggle availability, but retiring (deleting) items is
  // reserved for Canteen Admin on the backend (DELETE /admin/menu-items/{id} is 403 for
  // SUB_ADMIN), so the button is hidden rather than shown-then-failing.
  const canRetire = user?.role !== 'SUB_ADMIN';
  const [items, setItems] = useState<MenuItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingItem, setEditingItem] = useState<MenuItemResponse | null>(null);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [costPrice, setCostPrice] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await adminApi.listMenuItems(true, menuType));
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load catalog') });
    } finally {
      setLoading(false);
    }
  }, [menuType]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  function openCreate() {
    setEditingItem(null);
    setName('');
    setDescription('');
    setPrice('');
    setCostPrice('');
    setImageUrl('');
    setErrors({});
    setModalVisible(true);
  }

  function openEdit(item: MenuItemResponse) {
    setEditingItem(item);
    setName(item.name);
    setDescription(item.description ?? '');
    setPrice(String(item.price));
    setCostPrice(item.costPrice != null ? String(item.costPrice) : '');
    setImageUrl(item.imageUrl ?? '');
    setErrors({});
    setModalVisible(true);
  }

  async function handleSave() {
    const nextErrors: Record<string, string> = {};
    if (!name.trim()) nextErrors.name = 'Item name is required';
    const priceNum = parseFloat(price);
    if (!price || Number.isNaN(priceNum) || priceNum <= 0) nextErrors.price = 'Enter a valid price greater than ₹0';
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    const payload: MenuItemRequest = {
      name: name.trim(),
      description: description.trim() || undefined,
      price: priceNum,
      costPrice: costPrice ? parseFloat(costPrice) : undefined,
      menuType,
      available: editingItem?.available ?? true,
      imageUrl: imageUrl.trim() || undefined,
    };

    setSaving(true);
    try {
      if (editingItem) {
        await adminApi.updateMenuItem(editingItem.id, payload);
        Toast.show({ type: 'success', text1: 'Item updated' });
      } else {
        await adminApi.createMenuItem(payload);
        Toast.show({ type: 'success', text1: 'Item created' });
      }
      setModalVisible(false);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Save failed') });
    } finally {
      setSaving(false);
    }
  }

  async function toggleAvailable(item: MenuItemResponse) {
    try {
      await adminApi.updateMenuItem(item.id, {
        name: item.name,
        description: item.description ?? undefined,
        price: item.price,
        costPrice: item.costPrice ?? undefined,
        menuType: item.menuType,
        available: !item.available,
        imageUrl: item.imageUrl ?? undefined,
        allergens: item.allergens ?? undefined,
      });
      Toast.show({ type: 'success', text1: item.available ? 'Marked out of stock' : 'Marked available' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Update failed') });
    }
  }

  function confirmDeactivate(item: MenuItemResponse) {
    Alert.alert('Retire this item?', `${item.name} will no longer appear in future menus.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Retire',
        style: 'destructive',
        onPress: async () => {
          try {
            await adminApi.deactivateMenuItem(item.id);
            Toast.show({ type: 'success', text1: 'Item retired' });
            load();
          } catch (error) {
            Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not retire item') });
          }
        },
      },
    ]);
  }

  if (loading) return <LoadingView />;

  return (
    <>
      <Text style={typography.h2}>{menuType === 'DAILY' ? 'Daily Item Catalog' : 'Daily Delights Catalog'}</Text>
      <Button
        label={menuType === 'DAILY' ? 'Add Daily Item' : 'Add Fixed Item'}
        icon={<Plus size={18} color={colors.textOnPrimary} />}
        onPress={openCreate}
        style={{ marginBottom: 0, marginTop: spacing.md }}
      />

      {items.length === 0 ? (
        <EmptyState icon={<Plus color={colors.primaryDark} size={26} />} title="No catalog items yet" />
      ) : (
        <View style={{ gap: spacing.md, marginTop: spacing.lg }}>
          {items.map((item) => (
            <Card key={item.id} style={[styles.itemCard, !item.active && styles.itemCardInactive]}>
              {menuType === 'FIXED' && (
                <View style={[styles.dot, { backgroundColor: item.available ? colors.success : colors.danger }]} />
              )}
              <View style={{ flex: 1 }}>
                <Text style={typography.h3}>{item.name}</Text>
                <Text style={typography.caption}>{formatCurrency(item.price)}</Text>
                {!item.active && <Text style={styles.inactiveLabel}>Retired</Text>}
                {menuType === 'FIXED' && item.active && (
                  <Text style={[styles.inactiveLabel, { color: item.available ? colors.success : colors.danger }]}>
                    {item.available ? 'Available' : 'Out of Stock'}
                  </Text>
                )}
              </View>
              {menuType === 'FIXED' && item.active && (
                <Pressable onPress={() => toggleAvailable(item)} hitSlop={8} style={styles.iconButton}>
                  <Power size={16} color={colors.textSecondary} />
                </Pressable>
              )}
              <Pressable onPress={() => openEdit(item)} hitSlop={8} style={styles.iconButton}>
                <Pencil size={16} color={colors.textSecondary} />
              </Pressable>
              {item.active && canRetire && (
                <Pressable onPress={() => confirmDeactivate(item)} hitSlop={8} style={styles.iconButton}>
                  <Trash2 size={16} color={colors.danger} />
                </Pressable>
              )}
            </Card>
          ))}
        </View>
      )}

      <FormModal
        visible={modalVisible}
        title={editingItem ? 'Edit Item' : menuType === 'DAILY' ? 'Add New Daily Item' : 'Add New Fixed Item'}
        onClose={() => setModalVisible(false)}
      >
        <Input label="Item Name" required value={name} onChangeText={setName} error={errors.name} />
        <Input label="Description" value={description} onChangeText={setDescription} multiline />
        <View style={styles.row}>
          <View style={styles.half}>
            <Input label="Price (₹)" required keyboardType="decimal-pad" value={price} onChangeText={setPrice} error={errors.price} />
          </View>
          <View style={styles.half}>
            <Input label="Cost Price (₹)" keyboardType="decimal-pad" value={costPrice} onChangeText={setCostPrice} />
          </View>
        </View>
        <Input label="Image URL" value={imageUrl} onChangeText={setImageUrl} autoCapitalize="none" />
        <Button label={editingItem ? 'Save Changes' : 'Create Item'} onPress={handleSave} loading={saving} style={{ marginTop: spacing.md }} />
      </FormModal>
    </>
  );
}

// ─── Daily schedule (rendered below the Daily catalog on the same scroll) ──

function DailyScheduleView() {
  const [date, setDate] = useState(todayIso());
  const [dailyItems, setDailyItems] = useState<DailyMenuItemResponse[]>([]);
  const [catalog, setCatalog] = useState<MenuItemResponse[]>([]);
  const [selectedItemId, setSelectedItemId] = useState('');
  const [quantity, setQuantity] = useState('50');
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [editingQuantity, setEditingQuantity] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [daily, catalogItems] = await Promise.all([
        adminApi.listDailyMenu(date),
        adminApi.listMenuItems(false, 'DAILY'),
      ]);
      setDailyItems(daily);
      setCatalog(catalogItems);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load Meal of the Day') });
    } finally {
      setLoading(false);
    }
  }, [date]);

  useEffect(() => { load(); }, [load]);

  const usedIds = new Set(dailyItems.map((d) => d.menuItem.id));
  const availableToAdd = catalog.filter((item) => !usedIds.has(item.id));

  async function handleAdd() {
    if (!selectedItemId) {
      Toast.show({ type: 'error', text1: 'Select an item from the catalog' });
      return;
    }
    const qty = parseInt(quantity, 10);
    if (Number.isNaN(qty) || qty <= 0) {
      Toast.show({ type: 'error', text1: 'Quantity must be at least 1' });
      return;
    }
    setAdding(true);
    try {
      await adminApi.addDailyMenuItem(date, selectedItemId, qty);
      Toast.show({ type: 'success', text1: 'Item scheduled' });
      setSelectedItemId('');
      setQuantity('50');
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to schedule item') });
    } finally {
      setAdding(false);
    }
  }

  async function handleUpdate(dayItem: DailyMenuItemResponse, field: 'quantity' | 'available', value: number | boolean) {
    try {
      await adminApi.updateDailyMenuItem(
        dayItem.id,
        field === 'quantity' ? (value as number) : dayItem.totalQuantity,
        field === 'available' ? (value as boolean) : dayItem.available,
      );
      Toast.show({ type: 'success', text1: 'Updated' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Update failed') });
    }
  }

  if (loading) return <LoadingView />;

  return (
    <>
      <Text style={typography.h2}>Today&apos;s Schedule</Text>
      <DateField label="Menu date" value={date} onChange={setDate} />

      <Card style={styles.addCard}>
        <Text style={typography.h3}>Schedule an item</Text>
        {availableToAdd.length === 0 ? (
          <Text style={typography.bodySmall}>Every daily catalog item is already scheduled for this date.</Text>
        ) : (
          <>
            <View style={styles.chipRow}>
              {availableToAdd.map((item) => (
                <Chip key={item.id} label={item.name} active={selectedItemId === item.id} onPress={() => setSelectedItemId(item.id)} />
              ))}
            </View>
            <View style={styles.row}>
              <View style={{ flex: 1 }}>
                <Input label="Quantity" keyboardType="number-pad" value={quantity} onChangeText={setQuantity} />
              </View>
              <View style={styles.scheduleButton}>
                <Button label="Schedule" onPress={handleAdd} loading={adding} />
              </View>
            </View>
          </>
        )}
      </Card>

      {dailyItems.length === 0 ? (
        <EmptyState icon={<Plus color={colors.primaryDark} size={26} />} title="Nothing scheduled yet" message={`Add items above for ${date}.`} />
      ) : (
        <View style={{ gap: spacing.md, marginTop: spacing.lg }}>
          {dailyItems.map((dayItem) => {
            const progress = dayItem.totalQuantity > 0 ? dayItem.remainingQuantity / dayItem.totalQuantity : 0;
            return (
              <Card key={dayItem.id}>
                <View style={styles.dailyHeader}>
                  <Text style={typography.h3}>{dayItem.menuItem.name}</Text>
                  <Text style={typography.caption}>{formatCurrency(dayItem.menuItem.price)}</Text>
                </View>
                <View style={styles.progressTrack}>
                  <View style={[styles.progressFill, { width: `${progress * 100}%` }]} />
                </View>
                <Text style={styles.progressLabel}>{dayItem.remainingQuantity} / {dayItem.totalQuantity} remaining</Text>

                <View style={styles.dailyActions}>
                  <View style={{ flex: 1 }}>
                    <Input
                      label="Total stock"
                      keyboardType="number-pad"
                      value={editingQuantity[dayItem.id] ?? String(dayItem.totalQuantity)}
                      onChangeText={(text) => setEditingQuantity((prev) => ({ ...prev, [dayItem.id]: text }))}
                      onEndEditing={() => {
                        const qty = parseInt(editingQuantity[dayItem.id] ?? String(dayItem.totalQuantity), 10);
                        if (!Number.isNaN(qty) && qty !== dayItem.totalQuantity) handleUpdate(dayItem, 'quantity', qty);
                      }}
                    />
                  </View>
                  <Button
                    label={dayItem.available ? 'Available' : 'Disabled'}
                    variant={dayItem.available ? 'primary' : 'secondary'}
                    fullWidth={false}
                    onPress={() => handleUpdate(dayItem, 'available', !dayItem.available)}
                    style={styles.toggleButton}
                  />
                </View>
              </Card>
            );
          })}
        </View>
      )}
    </>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: spacing.lg, paddingTop: spacing.lg },
  subtitle: { ...typography.bodySmall, marginTop: 2 },
  segmentWrap: { marginTop: spacing.lg },
  sectionGap: { height: spacing.xl },
  itemCard: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  itemCardInactive: { opacity: 0.55 },
  dot: { width: 10, height: 10, borderRadius: 5 },
  inactiveLabel: { ...typography.caption, color: colors.danger, fontWeight: '700', marginTop: 2 },
  iconButton: { padding: spacing.xs },
  row: { flexDirection: 'row', gap: spacing.md, alignItems: 'flex-end' },
  half: { flex: 1 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm, marginBottom: spacing.md },
  addCard: { gap: spacing.sm, marginTop: spacing.lg },
  scheduleButton: { paddingBottom: spacing.lg },
  dailyHeader: { flexDirection: 'row', justifyContent: 'space-between' },
  progressTrack: { height: 6, borderRadius: 3, backgroundColor: colors.borderLight, marginTop: spacing.sm, overflow: 'hidden' },
  progressFill: { height: '100%', backgroundColor: colors.primary, borderRadius: 3 },
  progressLabel: { ...typography.caption, marginTop: spacing.xs },
  dailyActions: { flexDirection: 'row', gap: spacing.sm, alignItems: 'flex-end', marginTop: spacing.sm },
  toggleButton: { paddingBottom: spacing.lg },
});
