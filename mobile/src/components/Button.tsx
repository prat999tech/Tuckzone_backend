import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
  ViewStyle,
} from 'react-native';
import { colors, radius, spacing, typography } from '../theme';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost';
type Size = 'md' | 'lg';

interface ButtonProps {
  label: string;
  onPress: () => void;
  variant?: Variant;
  size?: Size;
  disabled?: boolean;
  loading?: boolean;
  icon?: React.ReactNode;
  fullWidth?: boolean;
  style?: ViewStyle;
}

/**
 * The one button component every screen uses. Centralising this means "disabled while
 * loading", spacing, and press feedback are consistent everywhere instead of each screen
 * reinventing (and slightly mismatching) its own button.
 */
export function Button({
  label,
  onPress,
  variant = 'primary',
  size = 'md',
  disabled = false,
  loading = false,
  icon,
  fullWidth = true,
  style,
}: ButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.base,
        variantStyles[variant].container,
        size === 'lg' ? styles.lg : styles.md,
        fullWidth && styles.fullWidth,
        isDisabled && styles.disabled,
        pressed && !isDisabled && variantStyles[variant].pressed,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={variantStyles[variant].text.color as string} size="small" />
      ) : (
        <View style={styles.content}>
          {icon}
          <Text style={[typography.button, variantStyles[variant].text]}>{label}</Text>
        </View>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  md: { paddingVertical: 12, paddingHorizontal: spacing.xl },
  lg: { paddingVertical: 16, paddingHorizontal: spacing.xxl },
  fullWidth: { width: '100%' },
  disabled: { opacity: 0.5 },
});

const variantStyles: Record<
  Variant,
  { container: ViewStyle; pressed: ViewStyle; text: { color: string } }
> = {
  primary: {
    container: { backgroundColor: colors.primary },
    pressed: { backgroundColor: colors.primaryDark },
    text: { color: colors.textOnPrimary },
  },
  secondary: {
    container: { backgroundColor: colors.surface, borderWidth: 1.5, borderColor: colors.border },
    pressed: { backgroundColor: colors.borderLight },
    text: { color: colors.textPrimary },
  },
  danger: {
    container: { backgroundColor: colors.danger },
    pressed: { backgroundColor: '#B91C1C' },
    text: { color: colors.textOnPrimary },
  },
  ghost: {
    container: { backgroundColor: 'transparent' },
    pressed: { backgroundColor: colors.borderLight },
    text: { color: colors.primaryDark },
  },
};
