import React, { useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { Clock } from 'lucide-react-native';
import { colors, radius, spacing, typography } from '../theme';

interface TimeFieldProps {
  label: string;
  value: string; // "HH:mm" or "HH:mm:ss"
  onChange: (value: string) => void;
}

function toTimeString(date: Date): string {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}:00`;
}

/** Native time picker, same platform-specific wiring as DateField: Android reports a
 *  single 'set' event and closes itself, iOS stays open inline until dismissed. */
export function TimeField({ label, value, onChange }: TimeFieldProps) {
  const [showPicker, setShowPicker] = useState(false);
  const timeValue = value ? new Date(`1970-01-01T${value.length === 5 ? `${value}:00` : value}`) : new Date();

  function handleChange(_event: unknown, selected?: Date) {
    if (Platform.OS === 'android') {
      setShowPicker(false);
    }
    if (selected) {
      onChange(toTimeString(selected));
    }
  }

  return (
    <View>
      <Text style={styles.label}>{label}</Text>
      <Pressable style={styles.field} onPress={() => setShowPicker(true)}>
        <Clock size={18} color={colors.primaryDark} />
        <Text style={styles.value}>
          {timeValue.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true })}
        </Text>
      </Pressable>
      {showPicker && (
        <DateTimePicker
          value={timeValue}
          mode="time"
          display={Platform.OS === 'ios' ? 'spinner' : 'default'}
          onChange={handleChange}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  label: { ...typography.bodySmall, fontWeight: '600', color: colors.textPrimary, marginBottom: spacing.xs },
  field: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.primarySurface,
    borderRadius: radius.md,
    paddingVertical: spacing.sm + 2,
    paddingHorizontal: spacing.md,
    alignSelf: 'flex-start',
  },
  value: { fontSize: 14, fontWeight: '700', color: colors.textPrimary },
});
