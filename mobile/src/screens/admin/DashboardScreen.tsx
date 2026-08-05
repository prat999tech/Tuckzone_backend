import React, { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  ShoppingBag,
  Clock,
  CheckCircle2,
  IndianRupee,
  TrendingUp,
  Users,
  AlertTriangle,
  Bell,
} from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { formatCurrency } from '../../utils/format';
import { colors, radius, spacing, typography } from '../../theme';
import type { DashboardResponse, TopItemRow } from '../../api/types';
import type { AdminStackParamList } from '../../navigation/types';

/** Shared row rendering for a ranked best-seller list, reused for the Daily and Fixed
 *  menu sections so both read as one consistent list style. */
function TopItemsCard({ items, emptyMessage }: { items: TopItemRow[]; emptyMessage: string }) {
  return (
    <Card>
      {items.length === 0 ? (
        <Text style={typography.bodySmall}>{emptyMessage}</Text>
      ) : (
        items.map((item, index) => (
          <View
            key={item.itemName}
            style={[styles.topItemRow, index < items.length - 1 && styles.topItemBorder]}
          >
            <View style={styles.rankBadge}>
              <Text style={styles.rankText}>{index + 1}</Text>
            </View>
            <Text style={[typography.body, { flex: 1 }]}>{item.itemName}</Text>
            <Text style={typography.bodySmall}>{item.quantitySold} sold</Text>
          </View>
        ))
      )}
    </Card>
  );
}

interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  tint: string;
  tintBg: string;
}

/** One consistent stat-tile shape for every KPI, so the dashboard reads as one grid
 *  rather than a collage of differently-styled numbers. */
function StatCard({ icon, label, value, tint, tintBg }: StatCardProps) {
  return (
    <Card style={styles.statCard}>
      <View style={[styles.statIcon, { backgroundColor: tintBg }]}>{icon}</View>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </Card>
  );
}

export function DashboardScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<AdminStackParamList>>();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async (isRefresh = false) => {
    isRefresh ? setRefreshing(true) : setLoading(true);
    try {
      const response = await adminApi.getDashboard();
      setData(response);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load dashboard') });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  if (loading || !data) return <LoadingView />;

  const netProfitPositive = data.netProfit >= 0;

  return (
    <ScreenContainer edges={['top']} refreshing={refreshing} onRefresh={() => load(true)}>
      <View style={styles.header}>
        <View>
          <Text style={typography.h1}>Dashboard</Text>
          <Text style={styles.subtitle}>{data.date}</Text>
        </View>
        <Pressable style={styles.bellButton} onPress={() => navigation.navigate('Notifications')} hitSlop={8}>
          <Bell size={22} color={colors.textPrimary} />
        </Pressable>
      </View>

      <View style={styles.grid}>
        <StatCard
          icon={<ShoppingBag size={20} color={colors.info} />}
          label="Today's Orders"
          value={String(data.totalOrders)}
          tint={colors.info}
          tintBg={colors.infoLight}
        />
        <StatCard
          icon={<Clock size={20} color={colors.warning} />}
          label="Pending"
          value={String(data.pendingOrders)}
          tint={colors.warning}
          tintBg={colors.warningLight}
        />
        <StatCard
          icon={<CheckCircle2 size={20} color={colors.success} />}
          label="Completed"
          value={String(data.completedOrders)}
          tint={colors.success}
          tintBg={colors.successLight}
        />
        <StatCard
          icon={<Users size={20} color={colors.primaryDark} />}
          label="Total Users"
          value={String(data.totalCustomers)}
          tint={colors.primaryDark}
          tintBg={colors.primaryLight}
        />
      </View>

      <Text style={[typography.h2, styles.sectionTitle]}>Revenue &amp; Profit</Text>
      <Card style={styles.financeCard}>
        <FinanceRow label="Revenue" value={formatCurrency(data.revenue)} />
        <FinanceRow label="Cost of Goods" value={`- ${formatCurrency(data.costOfGoods)}`} muted />
        <FinanceRow label="Gross Profit" value={formatCurrency(data.grossProfit)} />
        <FinanceRow label="Expenses" value={`- ${formatCurrency(data.expenses)}`} muted />
        <View style={styles.divider} />
        <FinanceRow
          label="Net Profit"
          value={formatCurrency(data.netProfit)}
          bold
          color={netProfitPositive ? colors.success : colors.danger}
        />
      </Card>

      {data.lowStock.length > 0 && (
        <>
          <Text style={[typography.h2, styles.sectionTitle]}>Low Stock</Text>
          <Card style={styles.warningCard}>
            {data.lowStock.map((item) => (
              <View key={item.itemName} style={styles.lowStockRow}>
                <AlertTriangle size={16} color={colors.warning} />
                <Text style={styles.lowStockText}>{item.itemName}</Text>
                <Text style={styles.lowStockCount}>
                  {item.remainingQuantity}/{item.totalQuantity} left
                </Text>
              </View>
            ))}
          </Card>
        </>
      )}

      <Text style={[typography.h2, styles.sectionTitle]}>Best-Selling Meal of the Day Items</Text>
      <TopItemsCard items={data.topDailyItems} emptyMessage="No Meal of the Day sales recorded yet today." />

      <Text style={[typography.h2, styles.sectionTitle]}>Best-Selling Daily Delights Items</Text>
      <TopItemsCard items={data.topFixedItems} emptyMessage="No Daily Delights sales recorded yet today." />
    </ScreenContainer>
  );
}

function FinanceRow({
  label,
  value,
  muted,
  bold,
  color,
}: {
  label: string;
  value: string;
  muted?: boolean;
  bold?: boolean;
  color?: string;
}) {
  return (
    <View style={styles.financeRow}>
      <Text style={[typography.bodySmall, muted && { color: colors.textTertiary }]}>{label}</Text>
      <Text style={[bold ? typography.h3 : typography.bodyMedium, color ? { color } : null]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
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
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md, marginTop: spacing.xl },
  statCard: { width: '47%', alignItems: 'flex-start' },
  statIcon: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.sm },
  statValue: { fontSize: 22, fontWeight: '800', color: colors.textPrimary },
  statLabel: { ...typography.caption, marginTop: 2 },
  sectionTitle: { marginTop: spacing.xxl, marginBottom: spacing.md },
  financeCard: { gap: 4 },
  financeRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 4 },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: colors.border, marginVertical: spacing.sm },
  warningCard: { backgroundColor: colors.warningLight, borderColor: colors.warning, gap: spacing.sm },
  lowStockRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  lowStockText: { ...typography.bodyMedium, flex: 1 },
  lowStockCount: { ...typography.bodySmall, fontWeight: '700' },
  topItemRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingVertical: spacing.sm },
  topItemBorder: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.borderLight },
  rankBadge: { width: 24, height: 24, borderRadius: 12, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  rankText: { fontSize: 12, fontWeight: '800', color: colors.primaryDark },
});
