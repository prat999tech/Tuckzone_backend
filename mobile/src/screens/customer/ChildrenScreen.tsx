import React, { useCallback, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Link as LinkIcon, Trash2, GraduationCap, Users } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { Button } from '../../components/Button';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { parentApi } from '../../api/parent';
import { apiErrorMessage } from '../../api/client';
import { validateMobile, validateRequired } from '../../utils/validation';
import { colors, radius, spacing, typography } from '../../theme';
import type { ChildResponse } from '../../api/types';

export function ChildrenScreen() {
  const [children, setChildren] = useState<ChildResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [admissionNumber, setAdmissionNumber] = useState('');
  const [parentMobile, setParentMobile] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [linking, setLinking] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await parentApi.getChildren();
      setChildren(data);
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load children') });
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleLink() {
    const nextErrors: Record<string, string> = {};
    const admissionError = validateRequired(admissionNumber, 'Admission number');
    const mobileError = validateMobile(parentMobile);
    if (admissionError) nextErrors.admissionNumber = admissionError;
    if (mobileError) nextErrors.parentMobile = mobileError;
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setLinking(true);
    try {
      await parentApi.linkChild(admissionNumber.trim(), parentMobile.trim());
      setAdmissionNumber('');
      setParentMobile('');
      Toast.show({ type: 'success', text1: 'Child linked successfully' });
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not link child') });
    } finally {
      setLinking(false);
    }
  }

  function confirmUnlink(child: ChildResponse) {
    Alert.alert('Unlink child?', `Remove ${child.fullName} from your account?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Unlink',
        style: 'destructive',
        onPress: async () => {
          try {
            await parentApi.unlinkChild(child.linkId);
            Toast.show({ type: 'success', text1: 'Child unlinked' });
            load();
          } catch (error) {
            Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Could not unlink') });
          }
        },
      },
    ]);
  }

  if (loading) return <LoadingView />;

  return (
    <ScreenContainer edges={['top']}>
      <Text style={typography.h1}>Linked Children</Text>
      <Text style={styles.subtitle}>Manage canteen access for your kids</Text>

      <Card style={styles.linkCard}>
        <View style={styles.linkHeader}>
          <LinkIcon size={18} color={colors.primaryDark} />
          <Text style={typography.h3}>Link a Child</Text>
        </View>
        <Input
          label="Admission Number"
          placeholder="e.g. ADM-2026-001"
          value={admissionNumber}
          onChangeText={setAdmissionNumber}
          error={errors.admissionNumber}
        />
        <Input
          label="Registered Parent Mobile"
          placeholder="10-digit mobile number"
          value={parentMobile}
          onChangeText={setParentMobile}
          keyboardType="number-pad"
          maxLength={10}
          error={errors.parentMobile}
        />
        <Button label="Link Account" onPress={handleLink} loading={linking} />
      </Card>

      <Text style={[typography.h2, styles.sectionTitle]}>Your Children</Text>
      {children.length === 0 ? (
        <EmptyState
          icon={<Users color={colors.primaryDark} size={26} />}
          title="No children linked yet"
          message="Link your child's account above to order on their behalf."
        />
      ) : (
        <View style={{ gap: spacing.md }}>
          {children.map((child) => (
            <Card key={child.linkId} style={styles.childCard}>
              <View style={styles.childAvatar}>
                <GraduationCap size={24} color={colors.primaryDark} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={typography.h3}>{child.fullName}</Text>
                <Text style={styles.childMeta}>
                  Class {child.studentClass}-{child.section} · Roll {child.rollNumber}
                </Text>
                <Text style={styles.childAdmission}>Adm No: {child.admissionNumber}</Text>
              </View>
              <Button
                label=""
                variant="ghost"
                fullWidth={false}
                icon={<Trash2 size={18} color={colors.danger} />}
                onPress={() => confirmUnlink(child)}
                style={styles.unlinkButton}
              />
            </Card>
          ))}
        </View>
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  subtitle: { ...typography.bodySmall, marginTop: 2, marginBottom: spacing.lg },
  linkCard: { gap: spacing.sm },
  linkHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.sm },
  sectionTitle: { marginTop: spacing.xxl, marginBottom: spacing.md },
  childCard: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  childAvatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  childMeta: { ...typography.bodySmall, marginTop: 2 },
  childAdmission: { ...typography.caption, marginTop: 2 },
  unlinkButton: { paddingHorizontal: 0, paddingVertical: 0, width: 36 },
});
