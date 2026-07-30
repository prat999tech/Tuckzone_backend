import React from 'react';
import { StyleSheet, View, ViewProps } from 'react-native';
import { colors, radius, shadow, spacing } from '../theme';

interface CardProps extends ViewProps {
  padded?: boolean;
  elevated?: boolean;
}

/** The one surface component every list row, section, and panel sits on. */
export function Card({ padded = true, elevated = true, style, children, ...rest }: CardProps) {
  return (
    <View
      style={[styles.base, padded && styles.padded, elevated && shadow, style]}
      {...rest}
    >
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.borderLight,
  },
  padded: { padding: spacing.lg },
});
