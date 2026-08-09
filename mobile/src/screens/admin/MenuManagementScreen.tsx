import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Plus, Pencil, Trash2, Power, UtensilsCrossed, Archive, RotateCcw, ChevronDown, ChevronRight } from 'lucide-react-native';
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
import { adminApi, MenuItemRequest, PickedImage } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { ImagePickerField } from '../../components/ImagePickerField';
import { useAuth } from '../../context/AuthContext';
import { formatCurrency, formatDate } from '../../utils/format';
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
  const [showRetired, setShowRetired] = useState(false);
  const [actionTarget, setActionTarget] = useState<MenuItemResponse | null>(null);
  const [permanentDeleteTarget, setPermanentDeleteTarget] = useState<MenuItemResponse | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [costPrice, setCostPrice] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  // The item's existing photo (edit mode only), plus whatever's staged in this modal
  // session — none of it touches the server until Save. See ImagePickerField.
  const [currentImageUrl, setCurrentImageUrl] = useState<string | null>(null);
  const [pickedImage, setPickedImage] = useState<PickedImage | null>(null);
  const [imageRemoved, setImageRemoved] = useState(false);

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
    setCurrentImageUrl(null);
    setPickedImage(null);
    setImageRemoved(false);
    setErrors({});
    setModalVisible(true);
  }

  function openEdit(item: MenuItemResponse) {
    setEditingItem(item);
    setName(item.name);
    setDescription(item.description ?? '');
    setPrice(String(item.price));
    setCostPrice(item.costPrice != null ? String(item.costPrice) : '');
    setCurrentImageUrl(item.imageUrl ?? null);
    setPickedImage(null);
    setImageRemoved(false);
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
    };

    setSaving(true);
    try {
      const saved = editingItem
        ? await adminApi.updateMenuItem(editingItem.id, payload)
        : await adminApi.createMenuItem(payload);

      // The item itself is saved at this point regardless of what happens to its image —
      // an image upload/removal failure below must not look like the whole save failed.
      if (pickedImage) {
        try {
          await adminApi.uploadMenuItemImage(saved.id, pickedImage);
        } catch (imageError) {
          Toast.show({ type: 'error', text1: apiErrorMessage(imageError, 'Item saved, but the image upload failed') });
        }
      } else if (imageRemoved) {
        await adminApi.removeMenuItemImage(saved.id).catch(() => undefined);
      }

      Toast.show({ type: 'success', text1: editingItem ? 'Item updated' : 'Item created' });
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
        allergens: item.allergens ?? undefined,
      });
      Toast.show({ type: 'success', text1: item.available ? 'Marked out of stock' : 'Marked available' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Update failed') });
    }
  }

  async function handleRetire(item: MenuItemResponse) {
    setActionTarget(null);
    try {
      await adminApi.deactivateMenuItem(item.id);
      Toast.show({ type: 'success', text1: 'Item retired', text2: 'Hidden from students; find it under Retired Items.' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not retire item') });
    }
  }

  async function handleRestore(item: MenuItemResponse) {
    try {
      await adminApi.activateMenuItem(item.id);
      Toast.show({ type: 'success', text1: 'Item restored to Active' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not restore item') });
    }
  }

  async function handlePermanentDelete() {
    if (!permanentDeleteTarget) return;
    setDeleting(true);
    try {
      await adminApi.permanentlyDeleteMenuItem(permanentDeleteTarget.id);
      Toast.show({ type: 'success', text1: 'Item permanently deleted' });
      setPermanentDeleteTarget(null);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not permanently delete item') });
    } finally {
      setDeleting(false);
    }
  }

  if (loading) return <LoadingView />;

  const activeItems = items.filter((item) => item.active);
  const retiredItems = items.filter((item) => !item.active);

  return (
    <>
      <Text style={typography.h2}>{menuType === 'DAILY' ? 'Daily Item Catalog' : 'Daily Delights Catalog'}</Text>
      <Button
        label={menuType === 'DAILY' ? 'Add Daily Item' : 'Add Fixed Item'}
        icon={<Plus size={18} color={colors.textOnPrimary} />}
        onPress={openCreate}
        style={{ marginBottom: 0, marginTop: spacing.md }}
      />

      {activeItems.length === 0 ? (
        <EmptyState
          icon={<UtensilsCrossed color={colors.primaryDark} size={26} />}
          title="No Menu Available"
          message="No menu items have been added yet."
          action={<Button label="Add Menu" icon={<Plus size={18} color={colors.textOnPrimary} />} onPress={openCreate} />}
        />
      ) : (
        <View style={{ gap: spacing.md, marginTop: spacing.lg }}>
          {activeItems.map((item) => (
            <Card key={item.id} style={styles.itemCard}>
              {menuType === 'FIXED' && (
                <View style={[styles.dot, { backgroundColor: item.available ? colors.success : colors.danger }]} />
              )}
              <View style={{ flex: 1 }}>
                <Text style={typography.h3}>{item.name}</Text>
                <Text style={typography.caption}>{formatCurrency(item.price)}</Text>
                {menuType === 'FIXED' && (
                  <Text style={[styles.inactiveLabel, { color: item.available ? colors.success : colors.danger }]}>
                    {item.available ? 'Available' : 'Out of Stock'}
                  </Text>
                )}
              </View>
              {menuType === 'FIXED' && (
                <Pressable onPress={() => toggleAvailable(item)} hitSlop={8} style={styles.iconButton}>
                  <Power size={16} color={colors.textSecondary} />
                </Pressable>
              )}
              <Pressable onPress={() => openEdit(item)} hitSlop={8} style={styles.iconButton}>
                <Pencil size={16} color={colors.textSecondary} />
              </Pressable>
              {canRetire && (
                <Pressable onPress={() => setActionTarget(item)} hitSlop={8} style={styles.iconButton}>
                  <Trash2 size={16} color={colors.danger} />
                </Pressable>
              )}
            </Card>
          ))}
        </View>
      )}

      {canRetire && retiredItems.length > 0 && (
        <View style={styles.retiredSection}>
          <Pressable style={styles.retiredHeader} onPress={() => setShowRetired((prev) => !prev)}>
            {showRetired ? (
              <ChevronDown size={18} color={colors.textSecondary} />
            ) : (
              <ChevronRight size={18} color={colors.textSecondary} />
            )}
            <Text style={typography.h3}>Retired Items ({retiredItems.length})</Text>
          </Pressable>

          {showRetired && (
            <View style={{ gap: spacing.md, marginTop: spacing.md }}>
              {retiredItems.map((item) => (
                <Card key={item.id} style={[styles.itemCard, styles.itemCardInactive]}>
                  <View style={{ flex: 1 }}>
                    <Text style={typography.h3}>{item.name}</Text>
                    <Text style={typography.caption}>{formatCurrency(item.price)}</Text>
                    <Text style={styles.inactiveLabel}>Retired</Text>
                  </View>
                  <Pressable onPress={() => openEdit(item)} hitSlop={8} style={styles.iconButton}>
                    <Pencil size={16} color={colors.textSecondary} />
                  </Pressable>
                  <Pressable onPress={() => handleRestore(item)} hitSlop={8} style={styles.iconButton}>
                    <RotateCcw size={16} color={colors.primary} />
                  </Pressable>
                  <Pressable onPress={() => setPermanentDeleteTarget(item)} hitSlop={8} style={styles.iconButton}>
                    <Trash2 size={16} color={colors.danger} />
                  </Pressable>
                </Card>
              ))}
            </View>
          )}
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
        <ImagePickerField
          currentImageUrl={currentImageUrl}
          image={pickedImage}
          removed={imageRemoved}
          onChange={(image, removed) => {
            setPickedImage(image);
            setImageRemoved(removed);
          }}
        />
        <Button label={editingItem ? 'Save Changes' : 'Create Item'} onPress={handleSave} loading={saving} style={{ marginTop: spacing.md }} />
      </FormModal>

      <FormModal
        visible={actionTarget !== null}
        title="What would you like to do with this menu item?"
        onClose={() => setActionTarget(null)}
      >
        <Text style={styles.dialogItemName}>{actionTarget?.name}</Text>
        <Button
          label="Retire"
          variant="secondary"
          icon={<Archive size={18} color={colors.warning} />}
          onPress={() => actionTarget && handleRetire(actionTarget)}
          style={styles.dialogButton}
        />
        <Button
          label="Permanently Delete"
          variant="danger"
          icon={<Trash2 size={18} color={colors.textOnPrimary} />}
          onPress={() => {
            setPermanentDeleteTarget(actionTarget);
            setActionTarget(null);
          }}
          style={styles.dialogButton}
        />
        <Button label="Cancel" variant="ghost" onPress={() => setActionTarget(null)} />
      </FormModal>

      <FormModal
        visible={permanentDeleteTarget !== null}
        title="Permanently delete this item?"
        onClose={() => setPermanentDeleteTarget(null)}
      >
        <Text style={styles.dialogBody}>
          Are you sure you want to permanently delete{' '}
          <Text style={{ fontWeight: '700' }}>{permanentDeleteTarget?.name}</Text>? This action
          cannot be undone.
        </Text>
        <View style={styles.row}>
          <View style={styles.half}>
            <Button label="Cancel" variant="secondary" onPress={() => setPermanentDeleteTarget(null)} />
          </View>
          <View style={styles.half}>
            <Button label="Delete Permanently" variant="danger" onPress={handlePermanentDelete} loading={deleting} />
          </View>
        </View>
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
      <Text style={typography.h2}>Schedule for {formatDate(date)}</Text>
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
  retiredSection: { marginTop: spacing.xl },
  retiredHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  dialogItemName: { ...typography.bodyMedium, color: colors.textSecondary, marginBottom: spacing.md },
  dialogButton: { marginBottom: spacing.sm },
  dialogBody: { ...typography.body, marginBottom: spacing.lg },
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
