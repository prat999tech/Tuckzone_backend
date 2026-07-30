/**
 * Single color palette for the whole app. Every screen pulls from here instead of
 * hardcoding hex values, which is what keeps the app looking like one product instead of
 * a pile of independently-styled screens.
 *
 * Warm amber as the primary color (food/canteen appropriate, matches the existing
 * TuckZone web branding) on a light, airy background — deliberately not a dark theme.
 */
export const colors = {
  // Brand
  primary: '#F59E0B', // amber-500 — primary buttons, active states, links
  primaryDark: '#D97706', // amber-600 — pressed states
  primaryLight: '#FEF3C7', // amber-100 — chips, subtle highlights
  primarySurface: '#FFFBEB', // amber-50 — very light tinted backgrounds

  // Neutrals
  background: '#F9FAFB', // screen background
  surface: '#FFFFFF', // cards, sheets
  border: '#E5E7EB',
  borderLight: '#F3F4F6',

  // Text
  textPrimary: '#1F2937',
  textSecondary: '#6B7280',
  textTertiary: '#9CA3AF',
  textOnPrimary: '#FFFFFF',

  // Semantic
  success: '#16A34A',
  successLight: '#DCFCE7',
  danger: '#DC2626',
  dangerLight: '#FEE2E2',
  warning: '#D97706',
  warningLight: '#FEF3C7',
  info: '#2563EB',
  infoLight: '#DBEAFE',

  // Domain-specific
  veg: '#16A34A',
  nonVeg: '#DC2626',

  // Overlays
  overlay: 'rgba(17, 24, 39, 0.45)',
  shadow: '#0F172A',
} as const;

/** Maps an order/ordering status word to a semantic color, used for badges everywhere. */
export function statusColor(status: string): { fg: string; bg: string } {
  switch (status) {
    case 'PLACED':
      return { fg: colors.info, bg: colors.infoLight };
    case 'ACCEPTED':
    case 'PREPARING':
    case 'PACKED':
    case 'OUT_FOR_DELIVERY':
      return { fg: colors.warning, bg: colors.warningLight };
    case 'DELIVERED':
    case 'ACTIVE':
    case 'OPEN':
      return { fg: colors.success, bg: colors.successLight };
    case 'CANCELLED':
    case 'REJECTED':
    case 'DISABLED':
    case 'CLOSED':
      return { fg: colors.danger, bg: colors.dangerLight };
    default:
      return { fg: colors.textSecondary, bg: colors.borderLight };
  }
}
