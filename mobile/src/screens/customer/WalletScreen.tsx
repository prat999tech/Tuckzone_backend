import React, { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Wallet as WalletIcon, ArrowDownLeft, ArrowUpRight, CreditCard } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { walletApi } from '../../api/wallet';
import { configApi } from '../../api/config';
import { apiErrorMessage } from '../../api/client';
import { formatCurrency, formatDateTime } from '../../utils/format';
import { colors, radius, spacing, typography } from '../../theme';
import type { WalletResponse, WalletTransactionResponse } from '../../api/types';

export function WalletScreen() {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [transactions, setTransactions] = useState<WalletTransactionResponse[]>([]);
  const [quickAmounts, setQuickAmounts] = useState<number[]>([100, 200, 500, 1000]);
  const [maxTopup, setMaxTopup] = useState(10000);
  const [mockPayments, setMockPayments] = useState(true);
  const [customAmount, setCustomAmount] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [processingAmount, setProcessingAmount] = useState<number | null>(null);

  useEffect(() => {
    configApi
      .get()
      .then((config) => {
        setQuickAmounts(config.quickTopupAmounts);
        setMaxTopup(config.maxTopupAmount);
        setMockPayments(config.mockPaymentsEnabled);
      })
      .catch(() => undefined);
  }, []);

  const load = useCallback(async (isRefresh = false) => {
    isRefresh ? setRefreshing(true) : setLoading(true);
    try {
      const [walletData, txData] = await Promise.all([walletApi.getWallet(), walletApi.getTransactions()]);
      setWallet(walletData);
      setTransactions(txData);
    } catch (err) {
      Toast.show({ type: 'error', text1: apiErrorMessage(err, 'Failed to load wallet') });
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

  async function handleTopup(amount: number) {
    if (!amount || amount < 1) {
      setError('Enter a valid amount');
      return;
    }
    if (amount > maxTopup) {
      setError(`Maximum top-up amount is ${formatCurrency(maxTopup)}`);
      return;
    }
    // A real (non-mock) gateway needs a checkout widget to actually collect payment, and
    // that bridge isn't wired up on mobile yet (the web app has it — see WalletPage.jsx).
    // Checked before creating anything server-side, so a customer without card/UPI access
    // in the app never has a real gateway order created (and left to expire) for nothing.
    if (!mockPayments) {
      Toast.show({
        type: 'info',
        text1: 'Card/UPI top-up not available in the app yet',
        text2: 'Please use the web app to add money to your wallet.',
      });
      return;
    }

    setError(undefined);
    setProcessingAmount(amount);
    try {
      // platformFee is 0 unless an admin has enabled one for WALLET_RECHARGE — surfaced
      // here so a customer sees exactly what they'll pay before confirming, computed
      // entirely server-side (see PricingService), never by this screen.
      const topup = await walletApi.initiateTopup(amount);
      // No real gateway integrated yet — see backend app.payment.allow-mock-topup. Once a
      // real gateway is wired up, this call is replaced by the gateway's checkout SDK flow;
      // everything else on this screen stays the same.
      await walletApi.mockCompleteTopup(topup.gatewayOrderId);
      Toast.show({
        type: 'success',
        text1: 'Wallet recharged',
        text2: topup.platformFee > 0
          ? `${formatCurrency(amount)} (+ ${formatCurrency(topup.platformFee)} fee)`
          : formatCurrency(amount),
      });
      setCustomAmount('');
      load();
    } catch (err) {
      Toast.show({ type: 'error', text1: apiErrorMessage(err, 'Top-up failed') });
    } finally {
      setProcessingAmount(null);
    }
  }

  if (loading) return <LoadingView />;

  return (
    <ScreenContainer edges={['top']} refreshing={refreshing} onRefresh={() => load(true)}>
      <Text style={typography.h1}>My Wallet</Text>
      <Text style={styles.subtitle}>Manage your canteen funds</Text>

      <View style={styles.balanceCard}>
        <View>
          <Text style={styles.balanceLabel}>Available Balance</Text>
          <Text style={styles.balanceValue}>{formatCurrency(wallet?.balance ?? 0)}</Text>
        </View>
        <WalletIcon size={40} color="rgba(255,255,255,0.85)" />
      </View>

      <Text style={[typography.h2, styles.sectionTitle]}>Quick Top-up</Text>
      <View style={styles.quickRow}>
        {quickAmounts.map((amount) => (
          <Button
            key={amount}
            label={`+${formatCurrency(amount)}`}
            variant="secondary"
            fullWidth={false}
            size="md"
            loading={processingAmount === amount}
            disabled={processingAmount !== null}
            onPress={() => handleTopup(amount)}
            style={styles.quickButton}
          />
        ))}
      </View>

      <View style={styles.customRow}>
        <View style={{ flex: 1 }}>
          <Input
            label="Custom amount"
            placeholder="Enter amount"
            keyboardType="numeric"
            value={customAmount}
            onChangeText={(text) => {
              setCustomAmount(text);
              if (error) setError(undefined);
            }}
            error={error}
          />
        </View>
      </View>
      <Button
        label="Add Money"
        icon={<CreditCard size={18} color={colors.textOnPrimary} />}
        loading={processingAmount !== null && !quickAmounts.includes(processingAmount)}
        disabled={!customAmount || processingAmount !== null}
        onPress={() => handleTopup(Number(customAmount))}
      />

      <Text style={[typography.h2, styles.sectionTitle]}>Recent Transactions</Text>
      {transactions.length === 0 ? (
        <EmptyState icon={<WalletIcon color={colors.primaryDark} size={26} />} title="No transactions yet" />
      ) : (
        <View style={{ gap: spacing.sm }}>
          {transactions.map((tx) => (
            <Card key={tx.id} style={styles.txRow}>
              <View style={[styles.txIcon, { backgroundColor: tx.type === 'CREDIT' ? colors.successLight : colors.dangerLight }]}>
                {tx.type === 'CREDIT' ? (
                  <ArrowDownLeft size={18} color={colors.success} />
                ) : (
                  <ArrowUpRight size={18} color={colors.danger} />
                )}
              </View>
              <View style={{ flex: 1 }}>
                <Text style={typography.bodyMedium} numberOfLines={1}>{tx.description}</Text>
                <Text style={typography.caption}>{formatDateTime(tx.createdAt)}</Text>
              </View>
              <Text style={[typography.bodyMedium, { color: tx.type === 'CREDIT' ? colors.success : colors.danger }]}>
                {tx.type === 'CREDIT' ? '+' : '-'}{formatCurrency(tx.amount)}
              </Text>
            </Card>
          ))}
        </View>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  subtitle: { ...typography.bodySmall, marginTop: 2, marginBottom: spacing.lg },
  balanceCard: {
    backgroundColor: colors.primary,
    borderRadius: radius.xl,
    padding: spacing.xl,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  balanceLabel: { color: 'rgba(255,255,255,0.85)', fontSize: 13, fontWeight: '600' },
  balanceValue: { color: colors.textOnPrimary, fontSize: 30, fontWeight: '800', marginTop: 4 },
  sectionTitle: { marginTop: spacing.xxl, marginBottom: spacing.md },
  quickRow: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  quickButton: { paddingHorizontal: spacing.lg },
  customRow: { marginTop: spacing.lg },
  txRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, padding: spacing.md },
  txIcon: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
});
