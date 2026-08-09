import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Pencil, CreditCard } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Badge } from '../../components/Badge';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { SegmentedControl } from '../../components/SegmentedControl';
import { FormModal } from '../../components/FormModal';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { colors, spacing, typography } from '../../theme';
import type { PaymentUseCase, PlatformFeeSettingsResponse, PlatformFeeType } from '../../api/types';

const USE_CASE_LABELS: Record<PaymentUseCase, string> = {
  WALLET_RECHARGE: 'Wallet Recharge',
  CHECKOUT: 'Checkout',
  SUBSCRIPTION: 'Subscription (not yet available)',
};

const FEE_TYPE_OPTIONS: { value: PlatformFeeType; label: string }[] = [
  { value: 'PERCENTAGE', label: 'Percentage' },
  { value: 'FIXED', label: 'Fixed Amount' },
];

function describeFee(row: PlatformFeeSettingsResponse): string {
  if (!row.enabled) return 'Disabled';
  const amount = row.feeType === 'PERCENTAGE' ? `${row.feeValue}%` : `₹${row.feeValue}`;
  const bounds: string[] = [];
  if (row.minFee != null) bounds.push(`min ₹${row.minFee}`);
  if (row.maxFee != null) bounds.push(`max ₹${row.maxFee}`);
  return bounds.length ? `${amount} (${bounds.join(', ')})` : amount;
}

export function PaymentSettingsScreen() {
  const [settings, setSettings] = useState<PlatformFeeSettingsResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [editingUseCase, setEditingUseCase] = useState<PaymentUseCase | null>(null);
  const [enabled, setEnabled] = useState(false);
  const [feeType, setFeeType] = useState<PlatformFeeType>('PERCENTAGE');
  const [feeValue, setFeeValue] = useState('0');
  const [minFee, setMinFee] = useState('');
  const [maxFee, setMaxFee] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSettings(await adminApi.listPaymentSettings());
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load payment settings') });
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  function openEdit(row: PlatformFeeSettingsResponse) {
    setErrors({});
    setEnabled(row.enabled);
    setFeeType(row.feeType);
    setFeeValue(String(row.feeValue ?? '0'));
    setMinFee(row.minFee != null ? String(row.minFee) : '');
    setMaxFee(row.maxFee != null ? String(row.maxFee) : '');
    setEditingUseCase(row.useCase);
  }

  async function handleSave() {
    const nextErrors: Record<string, string> = {};
    const value = Number(feeValue);
    if (feeValue === '' || Number.isNaN(value) || value < 0) {
      nextErrors.feeValue = 'Enter a fee value of 0 or more';
    } else if (feeType === 'PERCENTAGE' && value > 100) {
      nextErrors.feeValue = 'A percentage fee cannot exceed 100';
    }
    if (minFee !== '' && maxFee !== '' && Number(minFee) > Number(maxFee)) {
      nextErrors.maxFee = 'Maximum fee must be at least the minimum fee';
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0 || !editingUseCase) return;

    setSaving(true);
    try {
      await adminApi.updatePaymentSettings(editingUseCase, {
        enabled,
        feeType,
        feeValue: value,
        minFee: minFee === '' ? null : Number(minFee),
        maxFee: maxFee === '' ? null : Number(maxFee),
      });
      Toast.show({ type: 'success', text1: 'Payment settings updated' });
      setEditingUseCase(null);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Update failed') });
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <ScreenContainer edges={['top']}>
        <Text style={typography.h1}>Payment Settings</Text>
        <Text style={styles.subtitle}>
          Configure the platform fee — your revenue on top of wallet recharges and order
          checkouts. Disabled by default; nothing changes for customers until you enable it here.
        </Text>

        {loading ? (
          <LoadingView />
        ) : settings.length === 0 ? (
          <EmptyState icon={<CreditCard color={colors.primaryDark} size={26} />} title="No payment settings found" />
        ) : (
          <View style={{ gap: spacing.sm, marginTop: spacing.xl }}>
            {settings.map((row) => (
              <Card key={row.useCase} style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={typography.bodyMedium}>{USE_CASE_LABELS[row.useCase] || row.useCase}</Text>
                  <View style={styles.badgeRow}>
                    <Badge label={row.enabled ? 'ACTIVE' : 'DISABLED'} displayText={row.enabled ? 'Enabled' : 'Disabled'} />
                  </View>
                  <Text style={styles.feeText}>{describeFee(row)}</Text>
                </View>
                {row.useCase !== 'SUBSCRIPTION' && (
                  <Pressable onPress={() => openEdit(row)} hitSlop={8} style={styles.editButton}>
                    <Pencil size={18} color={colors.primaryDark} />
                  </Pressable>
                )}
              </Card>
            ))}
          </View>
        )}
      </ScreenContainer>

      <FormModal
        visible={editingUseCase !== null}
        title={editingUseCase ? `${USE_CASE_LABELS[editingUseCase]} Fee` : ''}
        onClose={() => setEditingUseCase(null)}
      >
        <Button
          label={enabled ? 'Enabled' : 'Disabled'}
          variant={enabled ? 'primary' : 'secondary'}
          onPress={() => setEnabled((prev) => !prev)}
          style={styles.enableToggle}
        />

        <Text style={styles.fieldLabel}>Fee Type</Text>
        <SegmentedControl options={FEE_TYPE_OPTIONS} value={feeType} onChange={setFeeType} />

        <Input
          label={feeType === 'PERCENTAGE' ? 'Fee Percentage (%)' : 'Fee Amount (₹)'}
          required
          keyboardType="decimal-pad"
          value={feeValue}
          onChangeText={(v) => {
            setFeeValue(v);
            if (errors.feeValue) setErrors((e) => ({ ...e, feeValue: '' }));
          }}
          error={errors.feeValue}
          style={styles.fieldSpacing}
        />

        <View style={styles.row2}>
          <View style={styles.half}>
            <Input
              label="Min Fee (₹, optional)"
              keyboardType="decimal-pad"
              value={minFee}
              onChangeText={setMinFee}
            />
          </View>
          <View style={styles.half}>
            <Input
              label="Max Fee (₹, optional)"
              keyboardType="decimal-pad"
              value={maxFee}
              onChangeText={(v) => {
                setMaxFee(v);
                if (errors.maxFee) setErrors((e) => ({ ...e, maxFee: '' }));
              }}
              error={errors.maxFee}
            />
          </View>
        </View>

        <Button label="Save Changes" onPress={handleSave} loading={saving} style={styles.saveButton} />
      </FormModal>
    </>
  );
}

const styles = StyleSheet.create({
  subtitle: { ...typography.bodySmall, marginTop: spacing.xs },
  row: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm },
  badgeRow: { marginTop: spacing.xs },
  feeText: { ...typography.bodySmall, marginTop: spacing.xs },
  editButton: { padding: spacing.xs },
  enableToggle: { marginBottom: spacing.lg },
  fieldLabel: { ...typography.bodySmall, fontWeight: '600', color: colors.textPrimary, marginBottom: spacing.xs },
  fieldSpacing: { marginTop: spacing.md },
  row2: { flexDirection: 'row', gap: spacing.sm },
  half: { flex: 1 },
  saveButton: { marginTop: spacing.md },
});
