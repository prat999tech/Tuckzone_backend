import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { User, Mail, Phone, Lock, Hash, BookOpen, KeyRound, Layers, Users } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { ScreenContainer } from '../../components/ScreenContainer';
import { Input } from '../../components/Input';
import { PasswordInput } from '../../components/PasswordInput';
import { Button } from '../../components/Button';
import { SegmentedControl } from '../../components/SegmentedControl';
import { authApi } from '../../api/auth';
import { apiErrorMessage } from '../../api/client';
import {
  validateEmail,
  validateMobile,
  validatePassword,
  validateRequired,
} from '../../utils/validation';
import { colors, spacing, typography } from '../../theme';
import type { AuthStackParamList } from '../../navigation/types';

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;
type Role = 'STUDENT' | 'TEACHER' | 'PARENT' | 'ADMIN';

export function RegisterScreen({ navigation }: Props) {
  const [role, setRole] = useState<Role>('STUDENT');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');

  // Student-only
  const [admissionNumber, setAdmissionNumber] = useState('');
  const [studentClass, setStudentClass] = useState('');
  const [section, setSection] = useState('');
  const [rollNumber, setRollNumber] = useState('');
  const [seatNumber, setSeatNumber] = useState('');
  const [parentMobile, setParentMobile] = useState('');
  const [studentMobile, setStudentMobile] = useState('');

  // Teacher-only
  const [employeeId, setEmployeeId] = useState('');
  const [department, setDepartment] = useState('');

  // Admin-only. The endpoint is public, so this invite code is the only thing stopping
  // anyone from granting themselves canteen-admin rights.
  const [signupCode, setSignupCode] = useState('');

  function validate(): Record<string, string> {
    const next: Record<string, string> = {};
    const set = (key: string, error?: string) => {
      if (error) next[key] = error;
    };

    set('fullName', validateRequired(fullName, 'Full name'));
    set('email', validateEmail(email));
    set('mobile', validateMobile(mobile));
    set('password', validatePassword(password));

    if (role === 'STUDENT') {
      set('admissionNumber', validateRequired(admissionNumber, 'Admission number'));
      set('studentClass', validateRequired(studentClass, 'Class'));
      set('section', validateRequired(section, 'Section'));
      set('rollNumber', validateRequired(rollNumber, 'Roll number'));
      // Optional: only checked for a valid format when the student actually enters one.
      if (parentMobile) set('parentMobile', validateMobile(parentMobile));
      if (studentMobile) set('studentMobile', validateMobile(studentMobile));
    }
    if (role === 'TEACHER') {
      set('employeeId', validateRequired(employeeId, 'Employee ID'));
      set('department', validateRequired(department, 'Department'));
    }
    if (role === 'ADMIN') {
      set('signupCode', validateRequired(signupCode, 'Canteen signup code'));
    }
    return next;
  }

  async function handleSubmit() {
    const validationErrors = validate();
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      Toast.show({ type: 'error', text1: 'Please fix the fields highlighted in red' });
      return;
    }

    setLoading(true);
    try {
      if (role === 'STUDENT') {
        await authApi.registerStudent({
          fullName,
          email: email.trim(),
          mobile: mobile.trim(),
          password,
          admissionNumber,
          studentClass,
          section,
          rollNumber,
          seatNumber: seatNumber || undefined,
          parentMobile: parentMobile.trim() || undefined,
          studentMobile: studentMobile.trim() || undefined,
        });
      } else if (role === 'TEACHER') {
        await authApi.registerTeacher({
          fullName,
          email: email.trim(),
          mobile: mobile.trim(),
          password,
          employeeId,
          department,
        });
      } else if (role === 'ADMIN') {
        await authApi.registerAdmin({
          fullName,
          email: email.trim(),
          mobile: mobile.trim(),
          password,
          signupCode: signupCode.trim(),
        });
      } else {
        await authApi.registerParent({ fullName, email: email.trim(), mobile: mobile.trim(), password });
      }
      Toast.show({
        type: 'success',
        text1: 'Account created',
        text2: 'Enter the code we emailed you to finish.',
      });
      // Registration is only half the flow now — the account cannot sign in with a
      // password until the emailed code is entered.
      navigation.navigate('VerifyEmail', { email: email.trim() });
    } catch (error) {
      Toast.show({ type: 'error', text1: apiErrorMessage(error, 'Registration failed') });
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScreenContainer>
        <SegmentedControl
          options={[
            { value: 'STUDENT', label: 'Student' },
            { value: 'TEACHER', label: 'Teacher' },
            { value: 'PARENT', label: 'Parent' },
            { value: 'ADMIN', label: 'Canteen' },
          ]}
          value={role}
          onChange={(next) => {
            setRole(next);
            setErrors({});
          }}
        />

        <View style={styles.form}>
          <Input
            label="Full Name"
            required
            value={fullName}
            onChangeText={setFullName}
            error={errors.fullName}
            leftIcon={<User size={18} color={colors.textTertiary} />}
          />
          <Input
            label="Email Address"
            required
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
            error={errors.email}
            leftIcon={<Mail size={18} color={colors.textTertiary} />}
          />
          <Input
            label={role === 'STUDENT' ? 'Login Mobile (10 digits)' : 'Mobile Number (10 digits)'}
            required
            value={mobile}
            onChangeText={setMobile}
            keyboardType="number-pad"
            maxLength={10}
            error={errors.mobile}
            leftIcon={<Phone size={18} color={colors.textTertiary} />}
          />
          <PasswordInput
            label="Password"
            required
            value={password}
            onChangeText={setPassword}
            error={errors.password}
            leftIcon={<Lock size={18} color={colors.textTertiary} />}
          />

          {role === 'STUDENT' && (
            <>
              <View style={styles.row}>
                <View style={styles.half}>
                  <Input
                    label="Admission No."
                    required
                    value={admissionNumber}
                    onChangeText={setAdmissionNumber}
                    error={errors.admissionNumber}
                    leftIcon={<Hash size={18} color={colors.textTertiary} />}
                  />
                </View>
                <View style={styles.half}>
                  <Input
                    label="Roll No."
                    required
                    value={rollNumber}
                    onChangeText={setRollNumber}
                    error={errors.rollNumber}
                  />
                </View>
              </View>
              <View style={styles.row}>
                <View style={styles.third}>
                  <Input
                    label="Class"
                    required
                    value={studentClass}
                    onChangeText={setStudentClass}
                    error={errors.studentClass}
                    leftIcon={<BookOpen size={18} color={colors.textTertiary} />}
                  />
                </View>
                <View style={styles.third}>
                  <Input
                    label="Section"
                    required
                    value={section}
                    onChangeText={setSection}
                    error={errors.section}
                    leftIcon={<Layers size={18} color={colors.textTertiary} />}
                  />
                </View>
                <View style={styles.third}>
                  <Input label="Seat No." value={seatNumber} onChangeText={setSeatNumber} />
                </View>
              </View>
              <Input
                label="Parent's Mobile (for linking, optional)"
                value={parentMobile}
                onChangeText={setParentMobile}
                keyboardType="number-pad"
                maxLength={10}
                error={errors.parentMobile}
                leftIcon={<Phone size={18} color={colors.textTertiary} />}
              />
              <Input
                label="Student Contact (optional)"
                value={studentMobile}
                onChangeText={setStudentMobile}
                keyboardType="number-pad"
                maxLength={10}
                error={errors.studentMobile}
              />
            </>
          )}

          {role === 'TEACHER' && (
            <View style={styles.row}>
              <View style={styles.half}>
                <Input
                  label="Employee ID"
                  required
                  value={employeeId}
                  onChangeText={setEmployeeId}
                  error={errors.employeeId}
                  leftIcon={<Hash size={18} color={colors.textTertiary} />}
                />
              </View>
              <View style={styles.half}>
                <Input
                  label="Department"
                  required
                  value={department}
                  onChangeText={setDepartment}
                  error={errors.department}
                  leftIcon={<Users size={18} color={colors.textTertiary} />}
                />
              </View>
            </View>
          )}

          {role === 'ADMIN' && (
            <>
              <Input
                label="Canteen Signup Code"
                required
                value={signupCode}
                onChangeText={setSignupCode}
                autoCapitalize="none"
                error={errors.signupCode}
                leftIcon={<KeyRound size={18} color={colors.textTertiary} />}
              />
              <Text style={styles.adminHint}>
                Whoever set up TuckZone for your school chose this code when installing the
                server. Ask them for it — it is what stops anyone from giving themselves
                canteen access.
              </Text>
            </>
          )}

          <Button label="Register" onPress={handleSubmit} loading={loading} style={styles.submit} />
        </View>
      </ScreenContainer>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  form: { marginTop: spacing.xl },
  row: { flexDirection: 'row', gap: spacing.md },
  half: { flex: 1 },
  third: { flex: 1 },
  adminHint: { ...typography.caption, marginTop: -spacing.xs, marginBottom: spacing.md },
  submit: { marginTop: spacing.sm },
});
