import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Plus, Trash2, Receipt } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Chip } from '../../components/Chip';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { DateField } from '../../components/DateField';
import { FormModal } from '../../components/FormModal';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { formatCurrency, formatDate } from '../../utils/format';
import { colors, spacing, typography } from '../../theme';
import type { ExpenseCategory, ExpenseResponse } from '../../api/types';

function isoDaysAgo(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const CATEGORIES: ExpenseCategory[] = [
  'INGREDIENTS', 'STAFF_WAGES', 'GAS_FUEL', 'RENT', 'UTILITIES', 'PACKAGING', 'EQUIPMENT', 'OTHER',
];

export function ExpensesScreen() {
  const [from, setFrom] = useState(isoDaysAgo(29));
  const [to, setTo] = useState(isoDaysAgo(0));
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);

  const [expenseDate, setExpenseDate] = useState(isoDaysAgo(0));
  const [category, setCategory] = useState<ExpenseCategory>('INGREDIENTS');
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setExpenses(await adminApi.listExpenses(from, to));
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load expenses') });
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => { load(); }, [load]);

  const total = expenses.reduce((sum, e) => sum + e.amount, 0);

  function openAdd() {
    setExpenseDate(isoDaysAgo(0));
    setCategory('INGREDIENTS');
    setDescription('');
    setAmount('');
    setErrors({});
    setModalVisible(true);
  }

  async function handleSave() {
    const amountNum = parseFloat(amount);
    if (!amount || Number.isNaN(amountNum) || amountNum <= 0) {
      setErrors({ amount: 'Enter a valid amount' });
      return;
    }
    setSaving(true);
    try {
      await adminApi.addExpense({ expenseDate, category, description: description.trim() || undefined, amount: amountNum });
      Toast.show({ type: 'success', text1: 'Expense recorded' });
      setModalVisible(false);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not save expense') });
    } finally {
      setSaving(false);
    }
  }

  function confirmDelete(expense: ExpenseResponse) {
    Alert.alert('Delete expense?', `Remove this ${expense.category.replace('_', ' ').toLowerCase()} entry?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await adminApi.deleteExpense(expense.id);
            load();
          } catch (error) {
            Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not delete') });
          }
        },
      },
    ]);
  }

  return (
    <>
      <ScreenContainer>
        <View style={styles.dateRow}>
          <View style={{ flex: 1 }}>
            <DateField label="From" value={from} onChange={setFrom} />
          </View>
          <View style={{ flex: 1 }}>
            <DateField label="To" value={to} onChange={setTo} />
          </View>
        </View>

        <Card style={styles.totalCard}>
          <Text style={styles.totalLabel}>Total in this period</Text>
          <Text style={styles.totalValue}>{formatCurrency(total)}</Text>
        </Card>

        <Button label="Record Expense" icon={<Plus size={18} color={colors.textOnPrimary} />} onPress={openAdd} style={styles.addButton} />

        {loading ? (
          <LoadingView />
        ) : expenses.length === 0 ? (
          <EmptyState icon={<Receipt color={colors.primaryDark} size={26} />} title="No expenses recorded" message="Track gas, wages, rent and supplies for accurate profit." />
        ) : (
          <View style={{ gap: spacing.sm, marginTop: spacing.lg }}>
            {expenses.map((expense) => (
              <Card key={expense.id} style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={typography.bodyMedium}>{expense.category.replace(/_/g, ' ')}</Text>
                  {expense.description ? <Text style={typography.caption}>{expense.description}</Text> : null}
                  <Text style={typography.caption}>{formatDate(expense.expenseDate)}</Text>
                </View>
                <Text style={styles.amount}>{formatCurrency(expense.amount)}</Text>
                <Pressable onPress={() => confirmDelete(expense)} hitSlop={8} style={styles.deleteButton}>
                  <Trash2 size={16} color={colors.danger} />
                </Pressable>
              </Card>
            ))}
          </View>
        )}
      </ScreenContainer>

      <FormModal visible={modalVisible} title="Record Expense" onClose={() => setModalVisible(false)}>
        <DateField label="Date" value={expenseDate} onChange={setExpenseDate} />
        <Text style={styles.fieldLabel}>Category</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: spacing.md }}>
          {CATEGORIES.map((cat) => (
            <Chip key={cat} label={cat.replace(/_/g, ' ')} active={category === cat} onPress={() => setCategory(cat)} />
          ))}
        </ScrollView>
        <Input label="Description" value={description} onChangeText={setDescription} placeholder="e.g. LPG cylinder refill" />
        <Input label="Amount (₹)" required keyboardType="decimal-pad" value={amount} onChangeText={setAmount} error={errors.amount} />
        <Button label="Save Expense" onPress={handleSave} loading={saving} style={{ marginTop: spacing.md }} />
      </FormModal>
    </>
  );
}

const styles = StyleSheet.create({
  dateRow: { flexDirection: 'row', gap: spacing.md },
  totalCard: { marginTop: spacing.lg, backgroundColor: colors.primarySurface, borderColor: colors.primary },
  totalLabel: { ...typography.bodySmall },
  totalValue: { fontSize: 24, fontWeight: '800', color: colors.primaryDark, marginTop: 2 },
  addButton: { marginTop: spacing.lg },
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  amount: { ...typography.bodyMedium },
  deleteButton: { padding: spacing.xs },
  fieldLabel: { ...typography.bodySmall, fontWeight: '600', color: colors.textPrimary, marginBottom: spacing.xs },
});
