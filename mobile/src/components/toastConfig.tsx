import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { CheckCircle2, Info, XCircle } from 'lucide-react-native';
import type { ToastConfig } from 'react-native-toast-message';
import { colors, radius, shadow, spacing, typography } from '../theme';

/** Restyles react-native-toast-message to match our design system instead of its default
 *  look, so feedback toasts feel like part of this app rather than a third-party widget. */
function ToastCard({
  icon,
  accent,
  text1,
  text2,
}: {
  icon: React.ReactNode;
  accent: string;
  text1?: string;
  text2?: string;
}) {
  return (
    <View style={[styles.card, shadow, { borderLeftColor: accent }]}>
      {icon}
      <View style={styles.textWrap}>
        {text1 ? <Text style={typography.bodyMedium}>{text1}</Text> : null}
        {text2 ? <Text style={typography.bodySmall}>{text2}</Text> : null}
      </View>
    </View>
  );
}

export const toastConfig: ToastConfig = {
  success: ({ text1, text2 }) => (
    <ToastCard icon={<CheckCircle2 color={colors.success} size={22} />} accent={colors.success} text1={text1} text2={text2} />
  ),
  error: ({ text1, text2 }) => (
    <ToastCard icon={<XCircle color={colors.danger} size={22} />} accent={colors.danger} text1={text1} text2={text2} />
  ),
  // Without this entry a `Toast.show({ type: 'info' })` call renders nothing at all, so
  // every non-success, non-error message has to be silently dropped or mislabelled.
  info: ({ text1, text2 }) => (
    <ToastCard icon={<Info color={colors.info} size={22} />} accent={colors.info} text1={text1} text2={text2} />
  ),
};

const styles = StyleSheet.create({
  card: {
    width: '92%',
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderLeftWidth: 4,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.sm,
  },
  textWrap: { flex: 1 },
});
