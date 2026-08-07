import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { radius, spacing, statusColor } from '../theme';

/** Small pill used everywhere a status word needs to stand out: order status, account
 *  status, ordering-window status. Color is derived once in the theme so "DELIVERED"
 *  is always the same green, wherever it appears. `displayText`, when given, overrides
 *  only the printed word — `label` still drives the color lookup, so an order simplified
 *  to "Placed" for display still tints correctly per its real underlying status. */
export function Badge({ label, displayText }: { label: string; displayText?: string }) {
  const { fg, bg } = statusColor(label);
  return (
    <View style={[styles.badge, { backgroundColor: bg }]}>
      <Text style={[styles.label, { color: fg }]}>{(displayText ?? label).replace(/_/g, ' ')}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: radius.full,
    alignSelf: 'flex-start',
  },
  label: { fontSize: 12, fontWeight: '700', letterSpacing: 0.2 },
});
