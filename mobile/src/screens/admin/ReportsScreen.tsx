import React, { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { DateField } from '../../components/DateField';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { formatCurrency } from '../../utils/format';
import { colors, spacing, typography } from '../../theme';
import type { SalesReportResponse, TopItemRow } from '../../api/types';

function isoDaysAgo(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function ReportsScreen() {
  const [from, setFrom] = useState(isoDaysAgo(6));
  const [to, setTo] = useState(isoDaysAgo(0));
  const [report, setReport] = useState<SalesReportResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setReport(await adminApi.getSalesReport(from, to));
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load report') });
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => { load(); }, [load]);

  return (
    <ScreenContainer>
      <View style={styles.dateRow}>
        <View style={{ flex: 1 }}>
          <DateField label="From" value={from} onChange={setFrom} />
        </View>
        <View style={{ flex: 1 }}>
          <DateField label="To" value={to} onChange={setTo} />
        </View>
      </View>

      {loading || !report ? (
        <LoadingView />
      ) : (
        <>
          <Card style={styles.financeCard}>
            <FinanceRow label="Revenue" value={formatCurrency(report.revenue)} />
            <FinanceRow label="Cost of Goods" value={`- ${formatCurrency(report.costOfGoods)}`} muted />
            <FinanceRow label="Gross Profit" value={formatCurrency(report.grossProfit)} />
            <FinanceRow label="Expenses" value={`- ${formatCurrency(report.expenses)}`} muted />
            <View style={styles.divider} />
            <FinanceRow
              label="Net Profit"
              value={formatCurrency(report.netProfit)}
              bold
              color={report.netProfit >= 0 ? colors.success : colors.danger}
            />
            <Text style={styles.orderCount}>{report.orderCount} orders in this period</Text>
          </Card>

          <Text style={[typography.h2, styles.sectionTitle]}>Daily Breakdown</Text>
          <Card>
            {report.daily.length === 0 ? (
              <Text style={typography.bodySmall}>No sales in this period.</Text>
            ) : (
              report.daily.map((day, index) => (
                <View key={day.date} style={[styles.dailyRow, index < report.daily.length - 1 && styles.rowBorder]}>
                  <Text style={typography.bodySmall}>{day.date}</Text>
                  <Text style={typography.caption}>{day.orders} orders</Text>
                  <Text style={typography.bodyMedium}>{formatCurrency(day.revenue)}</Text>
                </View>
              ))
            )}
          </Card>

          <Text style={[typography.h2, styles.sectionTitle]}>Top Daily Menu Items</Text>
          <TopItemsList items={report.topDailyItems} />

          <Text style={[typography.h2, styles.sectionTitle]}>Top Fixed Menu Items</Text>
          <TopItemsList items={report.topFixedItems} />

          <Text style={[typography.h2, styles.sectionTitle]}>Peak Order Hours</Text>
          <Card>
            {report.peakHours.length === 0 ? (
              <Text style={typography.bodySmall}>Not enough data yet.</Text>
            ) : (
              <View style={styles.peakGrid}>
                {report.peakHours.map((slot) => (
                  <View key={slot.hour} style={styles.peakItem}>
                    <Text style={styles.peakHour}>{slot.hour}:00</Text>
                    <Text style={styles.peakOrders}>{slot.orders}</Text>
                  </View>
                ))}
              </View>
            )}
          </Card>
        </>
      )}
    </ScreenContainer>
  );
}

function TopItemsList({ items }: { items: TopItemRow[] }) {
  const top10 = items.slice(0, 10);
  return (
    <Card>
      {top10.length === 0 ? (
        <Text style={typography.bodySmall}>No sales in this period.</Text>
      ) : (
        top10.map((item, index) => (
          <View key={item.itemName} style={[styles.dailyRow, index < top10.length - 1 && styles.rowBorder]}>
            <Text style={[typography.body, { flex: 1 }]}>{item.itemName}</Text>
            <Text style={typography.bodySmall}>{item.quantitySold} sold</Text>
          </View>
        ))
      )}
    </Card>
  );
}

function FinanceRow({ label, value, muted, bold, color }: { label: string; value: string; muted?: boolean; bold?: boolean; color?: string }) {
  return (
    <View style={styles.financeRow}>
      <Text style={[typography.bodySmall, muted && { color: colors.textTertiary }]}>{label}</Text>
      <Text style={[bold ? typography.h3 : typography.bodyMedium, color ? { color } : null]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  dateRow: { flexDirection: 'row', gap: spacing.md },
  financeCard: { gap: 4, marginTop: spacing.xl },
  financeRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 4 },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: colors.border, marginVertical: spacing.sm },
  orderCount: { ...typography.caption, marginTop: spacing.sm },
  sectionTitle: { marginTop: spacing.xxl, marginBottom: spacing.md },
  dailyRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: spacing.sm, gap: spacing.sm },
  rowBorder: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.borderLight },
  peakGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md },
  peakItem: { alignItems: 'center', width: 56 },
  peakHour: { ...typography.caption },
  peakOrders: { fontSize: 16, fontWeight: '800', color: colors.primaryDark },
});
