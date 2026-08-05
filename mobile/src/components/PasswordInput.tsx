import React, { useState } from 'react';
import { Pressable } from 'react-native';
import { Eye, EyeOff } from 'lucide-react-native';
import { Input } from './Input';
import { colors } from '../theme';

type PasswordInputProps = Omit<React.ComponentProps<typeof Input>, 'secureTextEntry' | 'rightElement'>;

/**
 * Drop-in replacement for `<Input secureTextEntry />`. Renders masked by default; the
 * eye/eye-slash button only ever flips local visibility state — value/onChange/etc. all
 * pass straight through, so nothing about how the password is captured or submitted
 * changes.
 */
export function PasswordInput(props: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <Input
      {...props}
      secureTextEntry={!visible}
      rightElement={
        <Pressable
          onPress={() => setVisible((prev) => !prev)}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? (
            <EyeOff size={18} color={colors.textTertiary} />
          ) : (
            <Eye size={18} color={colors.textTertiary} />
          )}
        </Pressable>
      }
    />
  );
}
