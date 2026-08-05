import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Plus, Pencil, Trash2, ShieldPlus } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Card } from '../../components/Card';
import { Badge } from '../../components/Badge';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { PasswordInput } from '../../components/PasswordInput';
import { FormModal } from '../../components/FormModal';
import { EmptyState } from '../../components/EmptyState';
import { LoadingView } from '../../components/LoadingView';
import { adminApi, SubAdminCreateRequest, SubAdminUpdateRequest } from '../../api/admin';
import { apiErrorMessage } from '../../api/client';
import { validateEmail, validateMobile, validatePassword, validateRequired } from '../../utils/validation';
import { colors, spacing, typography } from '../../theme';
import type { UserSummary } from '../../api/types';

export function SubAdminsScreen() {
  const [subAdmins, setSubAdmins] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingSubAdmin, setEditingSubAdmin] = useState<UserSummary | null>(null);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSubAdmins(await adminApi.listSubAdmins());
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Failed to load sub admins') });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  function openCreate() {
    setEditingSubAdmin(null);
    setFullName('');
    setEmail('');
    setMobile('');
    setPassword('');
    setErrors({});
    setModalVisible(true);
  }

  function openEdit(subAdmin: UserSummary) {
    setEditingSubAdmin(subAdmin);
    setFullName(subAdmin.fullName);
    setEmail(subAdmin.email);
    setMobile(subAdmin.mobile);
    setPassword('');
    setErrors({});
    setModalVisible(true);
  }

  async function handleSave() {
    const nextErrors: Record<string, string> = {};
    const nameError = validateRequired(fullName, 'Full name');
    if (nameError) nextErrors.fullName = nameError;
    const emailError = validateEmail(email);
    if (emailError) nextErrors.email = emailError;
    const mobileError = validateMobile(mobile);
    if (mobileError) nextErrors.mobile = mobileError;
    // Password is required to create, optional to edit (blank = leave unchanged).
    if (!editingSubAdmin || password) {
      const passwordError = validatePassword(password);
      if (passwordError) nextErrors.password = passwordError;
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSaving(true);
    try {
      if (editingSubAdmin) {
        const payload: SubAdminUpdateRequest = {
          fullName: fullName.trim(),
          email: email.trim(),
          mobile: mobile.trim(),
          newPassword: password || undefined,
        };
        await adminApi.updateSubAdmin(editingSubAdmin.id, payload);
        Toast.show({ type: 'success', text1: 'Sub admin updated' });
      } else {
        const payload: SubAdminCreateRequest = {
          fullName: fullName.trim(),
          email: email.trim(),
          mobile: mobile.trim(),
          password,
        };
        await adminApi.createSubAdmin(payload);
        Toast.show({ type: 'success', text1: 'Sub admin created' });
      }
      setModalVisible(false);
      load();
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Save failed') });
    } finally {
      setSaving(false);
    }
  }

  function confirmToggle(subAdmin: UserSummary) {
    const disabling = subAdmin.status === 'ACTIVE';
    Alert.alert(
      disabling ? 'Deactivate sub admin?' : 'Activate sub admin?',
      disabling
        ? `${subAdmin.fullName} will be signed out and unable to sign in.`
        : `${subAdmin.fullName} will be able to sign in again.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: disabling ? 'Deactivate' : 'Activate',
          style: disabling ? 'destructive' : 'default',
          onPress: async () => {
            setBusyId(subAdmin.id);
            try {
              if (disabling) await adminApi.deactivateSubAdmin(subAdmin.id);
              else await adminApi.activateSubAdmin(subAdmin.id);
              load();
            } catch (error) {
              Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Action failed') });
            } finally {
              setBusyId(null);
            }
          },
        },
      ],
    );
  }

  function confirmDelete(subAdmin: UserSummary) {
    Alert.alert('Delete sub admin?', `${subAdmin.fullName}'s account will be permanently removed.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          setBusyId(subAdmin.id);
          try {
            await adminApi.deleteSubAdmin(subAdmin.id);
            Toast.show({ type: 'success', text1: 'Sub admin deleted' });
            load();
          } catch (error) {
            Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Delete failed') });
          } finally {
            setBusyId(null);
          }
        },
      },
    ]);
  }

  return (
    <ScreenContainer scroll={false} contentStyle={{ padding: 0 }}>
      <View style={styles.header}>
        <Text style={typography.h1}>Sub Admins</Text>
        <Text style={styles.subtitle}>Restricted accounts for orders, menu and export only</Text>
        <Button label="Add Sub Admin" icon={<Plus size={18} color={colors.textOnPrimary} />} onPress={openCreate} style={styles.addButton} />
      </View>

      {loading ? (
        <LoadingView />
      ) : (
        <ScrollView style={styles.fill} contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
          {subAdmins.length === 0 ? (
            <EmptyState icon={<ShieldPlus color={colors.primaryDark} size={26} />} title="No sub admins yet" />
          ) : (
            subAdmins.map((subAdmin) => (
              <Card key={subAdmin.id} style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={typography.bodyMedium}>{subAdmin.fullName}</Text>
                  <Text style={typography.caption}>{subAdmin.email} · {subAdmin.mobile}</Text>
                  <Badge label={subAdmin.status} />
                </View>
                <Pressable onPress={() => openEdit(subAdmin)} hitSlop={8} style={styles.iconButton}>
                  <Pencil size={16} color={colors.textSecondary} />
                </Pressable>
                <Button
                  label={subAdmin.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                  variant={subAdmin.status === 'ACTIVE' ? 'danger' : 'primary'}
                  fullWidth={false}
                  size="md"
                  loading={busyId === subAdmin.id}
                  onPress={() => confirmToggle(subAdmin)}
                />
                <Pressable onPress={() => confirmDelete(subAdmin)} hitSlop={8} style={styles.iconButton}>
                  <Trash2 size={16} color={colors.danger} />
                </Pressable>
              </Card>
            ))
          )}
        </ScrollView>
      )}

      <FormModal
        visible={modalVisible}
        title={editingSubAdmin ? 'Edit Sub Admin' : 'Add Sub Admin'}
        onClose={() => setModalVisible(false)}
      >
        <Input label="Full Name" required value={fullName} onChangeText={setFullName} error={errors.fullName} />
        <Input
          label="Email Address"
          required
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          keyboardType="email-address"
          error={errors.email}
        />
        <Input
          label="Mobile Number (10 digits)"
          required
          value={mobile}
          onChangeText={setMobile}
          keyboardType="number-pad"
          maxLength={10}
          error={errors.mobile}
        />
        <PasswordInput
          label={editingSubAdmin ? 'New Password (leave blank to keep unchanged)' : 'Password'}
          required={!editingSubAdmin}
          value={password}
          onChangeText={setPassword}
          error={errors.password}
        />
        <Button label={editingSubAdmin ? 'Save Changes' : 'Create Sub Admin'} onPress={handleSave} loading={saving} style={{ marginTop: spacing.md }} />
      </FormModal>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  header: { paddingHorizontal: spacing.lg, paddingTop: spacing.lg },
  subtitle: { ...typography.bodySmall, marginTop: 2 },
  addButton: { marginTop: spacing.md },
  list: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxxl },
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  iconButton: { padding: spacing.xs },
});
