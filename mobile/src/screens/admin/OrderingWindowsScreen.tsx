import React, { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { CalendarClock } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Badge } from '../../components/Badge';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { DateField } from '../../components/DateField';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { colors, spacing, typography } from '../../theme';
import type { DemandRow, OrderingWindowResponse } from '../../api/types';

function todayIso(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

export function OrderingWindowsScreen() {
  const [date, setDate] = useState(todayIso());
  const [windows, setWindows] = useState<OrderingWindowResponse[]>([]);
  const [demand, setDemand] = useState<DemandRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedSlotId, setExpandedSlotId] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [overrideCutoff, setOverrideCutoff] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [statusList, demandList] = await Promise.all([
        adminApi.getOrderingStatus(date),
        adminApi.getDemand(date),
      ]);
      setWindows(statusList);
      setDemand(demandList);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load ordering status') });
    } finally {
      setLoading(false);
    }
  }, [date]);

  useEffect(() => { load(); }, [load]);

  function toggleExpand(slotId: string) {
    setExpandedSlotId((current) => (current === slotId ? null : slotId));
    setReason('');
    setOverrideCutoff('');
  }

  async function handleClose(slotId: string) {
    setSubmitting(true);
    try {
      await adminApi.closeOrdering({ menuDate: date, slotId, reason: reason.trim() || undefined });
      Toast.show({ type: 'success', text1: 'Ordering closed', text2: 'Customers have been notified' });
      setExpandedSlotId(null);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not close ordering') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleOpen(slotId: string) {
    if (overrideCutoff && !/^\d{2}:\d{2}$/.test(overrideCutoff.trim())) {
      Toast.show({ type: 'error', text1: 'Cutoff time must be in HH:mm format, e.g. 14:30' });
      return;
    }
    setSubmitting(true);
    try {
      await adminApi.openOrdering({
        menuDate: date,
        slotId,
        overrideCutoffTime: overrideCutoff.trim() ? `${overrideCutoff.trim()}:00` : undefined,
        reason: reason.trim() || undefined,
      });
      Toast.show({ type: 'success', text1: 'Ordering reopened', text2: 'Customers have been notified' });
      setExpandedSlotId(null);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not reopen ordering') });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <LoadingView />;

  return (
    <ScreenContainer>
      <DateField label="Menu date" value={date} onChange={setDate} minimumDate={new Date()} />

      <Text style={[typography.h2, styles.sectionTitle]}>Delivery Slots</Text>
      <View style={{ gap: spacing.md }}>
        {windows.map((window) => (
          <Card key={window.slotId}>
            <View style={styles.slotHeader}>
              <View>
                <Text style={typography.h3}>{window.slotName}</Text>
                <Text style={typography.caption}>Cutoff: {window.effectiveCutoffTime}</Text>
                {window.reason ? <Text style={styles.reasonText}>{window.reason}</Text> : null}
              </View>
              <Badge label={window.acceptingOrders ? 'OPEN' : 'CLOSED'} />
            </View>

            {expandedSlotId === window.slotId ? (
              <View style={styles.form}>
                {window.acceptingOrders ? (
                  <>
                    <Input label="Reason (shown to customers)" placeholder="e.g. Kitchen at capacity" value={reason} onChangeText={setReason} />
                    <Button label="Close Ordering" variant="danger" loading={submitting} onPress={() => handleClose(window.slotId)} />
                  </>
                ) : (
                  <>
                    <Input label="New cutoff time (optional, HH:mm)" placeholder="e.g. 14:30" value={overrideCutoff} onChangeText={setOverrideCutoff} />
                    <Input label="Reason (shown to customers)" placeholder="e.g. Extra stock added" value={reason} onChangeText={setReason} />
                    <Button label="Reopen Ordering" loading={submitting} onPress={() => handleOpen(window.slotId)} />
                  </>
                )}
                <Button label="Cancel" variant="ghost" onPress={() => setExpandedSlotId(null)} />
              </View>
            ) : (
              <Button
                label={window.acceptingOrders ? 'Stop Taking Orders' : 'Reopen Ordering'}
                variant={window.acceptingOrders ? 'danger' : 'primary'}
                onPress={() => toggleExpand(window.slotId)}
                style={styles.actionButton}
              />
            )}
          </Card>
        ))}
      </View>

      <Text style={[typography.h2, styles.sectionTitle]}>Demand for {date}</Text>
      {demand.length === 0 ? (
        <EmptyState icon={<CalendarClock color={colors.primaryDark} size={26} />} title="No orders yet for this date" />
      ) : (
        <Card>
          {demand.map((row, index) => (
            <View key={row.menuItemId} style={[styles.demandRow, index < demand.length - 1 && styles.demandRowBorder]}>
              <Text style={[typography.body, { flex: 1 }]}>{row.itemName}</Text>
              <Text style={typography.bodySmall}>{row.orderedQuantity} ordered</Text>
              <Text style={typography.bodySmall}>{row.remainingQuantity}/{row.totalQuantity} in stock</Text>
              {row.shortfall > 0 && (
                <View style={styles.shortfallBadge}>
                  <Text style={styles.shortfallText}>Short {row.shortfall}</Text>
                </View>
              )}
            </View>
          ))}
        </Card>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  sectionTitle: { marginTop: spacing.xxl, marginBottom: spacing.md },
  slotHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  reasonText: { ...typography.caption, marginTop: 2, fontStyle: 'italic' },
  actionButton: { marginTop: spacing.md },
  form: { gap: spacing.sm, marginTop: spacing.md },
  demandRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, paddingVertical: spacing.sm, flexWrap: 'wrap' },
  demandRowBorder: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.borderLight },
  shortfallBadge: { backgroundColor: colors.dangerLight, paddingHorizontal: spacing.sm, paddingVertical: 2, borderRadius: 999 },
  shortfallText: { fontSize: 11, fontWeight: '700', color: colors.danger },
});
