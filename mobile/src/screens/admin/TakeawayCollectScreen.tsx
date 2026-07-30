import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { CheckCircle2, KeyRound } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { Button } from '../../components/Button';
import { DateField } from '../../components/DateField';
import { adminApi } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { colors, spacing, typography } from '../../theme';
import type { OrderResponse } from '../../api/types';

function todayIso(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

export function TakeawayCollectScreen() {
  const [date, setDate] = useState(todayIso());
  const [code, setCode] = useState('');
  const [collecting, setCollecting] = useState(false);
  const [result, setResult] = useState<OrderResponse | null>(null);

  async function handleCollect() {
    if (!code.trim()) {
      Toast.show({ type: 'error', text1: 'Enter the pickup code' });
      return;
    }
    setCollecting(true);
    try {
      const order = await adminApi.collectTakeaway(date, code.trim().toUpperCase());
      setResult(order);
      setCode('');
      Toast.show({ type: 'success', text1: 'Order handed over' });
    } catch (error) {
      setResult(null);
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not find that pickup code') });
    } finally {
      setCollecting(false);
    }
  }

  return (
    <ScreenContainer>
      <Text style={typography.h1}>Collect Takeaway</Text>
      <Text style={styles.subtitle}>Enter the code shown on the customer&apos;s app</Text>

      <View style={styles.form}>
        <DateField label="Order date" value={date} onChange={setDate} />
        <Input
          label="Pickup Code"
          required
          placeholder="e.g. W7DF2S"
          value={code}
          onChangeText={setCode}
          autoCapitalize="characters"
          leftIcon={<KeyRound size={18} color={colors.textTertiary} />}
        />
        <Button label="Collect Order" onPress={handleCollect} loading={collecting} />
      </View>

      {result && (
        <Card style={styles.resultCard}>
          <View style={styles.resultHeader}>
            <CheckCircle2 size={22} color={colors.success} />
            <Text style={typography.h3}>{result.orderNumber} handed over</Text>
          </View>
          <Text style={styles.resultDetail}>For {result.recipientName}</Text>
          {result.items.map((item) => (
            <Text key={item.menuItemId} style={styles.resultItem}>
              {item.quantity}x {item.itemName}
            </Text>
          ))}
        </Card>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  subtitle: { ...typography.bodySmall, marginTop: 2, marginBottom: spacing.xl },
  form: { gap: spacing.md },
  resultCard: { marginTop: spacing.xl, backgroundColor: colors.successLight, borderColor: colors.success },
  resultHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  resultDetail: { ...typography.bodySmall, marginTop: spacing.xs },
  resultItem: { ...typography.bodySmall, marginTop: 2 },
});
