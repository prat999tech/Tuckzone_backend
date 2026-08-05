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
    // 24px (rounded-xl) is the design's card radius — noticeably softer than the 8px used
    // for inputs, which is what gives the layout its retail feel.
    borderRadius: radius.xl,
    // A full 1px outline-variant border, not a hairline: the design leans on outlines
    // rather than shadows to separate surfaces, so it has to actually read as a line.
    borderWidth: 1,
    borderColor: colors.border,
  },
  padded: { padding: spacing.xl },
});
