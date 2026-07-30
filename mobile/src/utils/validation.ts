/**
 * Validation rules mirrored exactly from the backend
 * (`com.school.canteen.dto.auth.ValidationRules`), so the app never shows "success" only to
 * have the API reject it with a 400 the user can't make sense of.
 */
export const MOBILE_PATTERN = /^[6-9]\d{9}$/;
export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72;
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

export function validateMobile(value: string): string | undefined {
  if (!value.trim()) return 'Mobile number is required';
  if (!MOBILE_PATTERN.test(value.trim())) return 'Enter a valid 10-digit mobile number';
  return undefined;
}

export function validateEmail(value: string): string | undefined {
  if (!value.trim()) return 'Email address is required';
  if (!EMAIL_PATTERN.test(value.trim())) return 'Enter a valid email address';
  return undefined;
}

export function validatePassword(value: string): string | undefined {
  if (!value) return 'Password is required';
  if (value.length < PASSWORD_MIN || value.length > PASSWORD_MAX) {
    return `Password must be between ${PASSWORD_MIN} and ${PASSWORD_MAX} characters`;
  }
  return undefined;
}

export function validateRequired(value: string, label: string): string | undefined {
  return value.trim() ? undefined : `${label} is required`;
}

export function validateOtpCode(value: string, length: number): string | undefined {
  if (!value.trim()) return 'Enter the code sent to your email';
  if (value.trim().length !== length) return `Enter the ${length}-digit code`;
  return undefined;
}
