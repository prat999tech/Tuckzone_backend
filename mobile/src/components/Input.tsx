import React from 'react';
import {
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
} from 'react-native';
import { colors, radius, spacing, typography } from '../theme';

interface InputProps extends TextInputProps {
  label: string;
  error?: string;
  required?: boolean;
  leftIcon?: React.ReactNode;
  rightElement?: React.ReactNode;
}

/**
 * A labelled text field with a consistent error state. Required fields get a visible
 * asterisk — every registration/edit form uses this so the "required" convention is
 * identical across the whole app, not just on some screens.
 */
export function Input({
  label,
  error,
  required,
  leftIcon,
  rightElement,
  style,
  ...textInputProps
}: InputProps) {
  return (
    <View style={styles.wrapper}>
      <Text style={styles.label}>
        {label}
        {required && <Text style={styles.asterisk}> *</Text>}
      </Text>
      <View style={[styles.inputRow, error && styles.inputRowError]}>
        {leftIcon}
        <TextInput
          style={[styles.input, leftIcon ? { marginLeft: spacing.sm } : null, style]}
          placeholderTextColor={colors.textTertiary}
          {...textInputProps}
        />
        {rightElement}
      </View>
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: spacing.lg },
  label: { ...typography.bodySmall, color: colors.textPrimary, fontWeight: '600', marginBottom: spacing.xs },
  asterisk: { color: colors.danger },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.md,
  },
  inputRowError: { borderColor: colors.danger },
  input: {
    flex: 1,
    paddingVertical: 12,
    fontSize: 15,
    color: colors.textPrimary,
  },
  errorText: {
    ...typography.caption,
    color: colors.danger,
    marginTop: spacing.xs,
    fontWeight: '600',
  },
});
