import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Minus, Plus } from 'lucide-react-native';
import { colors, radius, spacing } from '../theme';

interface QuantityStepperProps {
  quantity: number;
  onIncrement: () => void;
  onDecrement: () => void;
  incrementDisabled?: boolean;
  compact?: boolean;
}

/** The +/- control used in the menu grid and the cart. Same size and spacing in both
 *  places so the cart doesn't feel like a different app from the menu it came from. */
export function QuantityStepper({
  quantity,
  onIncrement,
  onDecrement,
  incrementDisabled,
  compact,
}: QuantityStepperProps) {
  const size = compact ? 28 : 32;
  return (
    <View style={[styles.container, { height: size }]}>
      <Pressable onPress={onDecrement} style={[styles.button, { width: size }]} hitSlop={8}>
        <Minus size={16} color={colors.primaryDark} />
      </Pressable>
      <Text style={styles.quantity}>{quantity}</Text>
      <Pressable
        onPress={onIncrement}
        disabled={incrementDisabled}
        style={[styles.button, { width: size }, incrementDisabled && styles.buttonDisabled]}
        hitSlop={8}
      >
        <Plus size={16} color={incrementDisabled ? colors.textTertiary : colors.primaryDark} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.primarySurface,
    borderRadius: radius.md,
    paddingHorizontal: 2,
  },
  button: { alignItems: 'center', justifyContent: 'center', height: '100%' },
  buttonDisabled: { opacity: 0.4 },
  quantity: {
    minWidth: 24,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '700',
    color: colors.textPrimary,
    marginHorizontal: spacing.xs,
  },
});
