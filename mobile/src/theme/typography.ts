import { TextStyle } from 'react-native';
import { colors } from './colors';

/** One type scale for the whole app. Pick a variant by name rather than setting
 *  fontSize/fontWeight inline, so headings are the same size everywhere they appear. */
export const typography: Record<string, TextStyle> = {
  display: { fontSize: 28, fontWeight: '700', color: colors.textPrimary },
  h1: { fontSize: 22, fontWeight: '700', color: colors.textPrimary },
  h2: { fontSize: 18, fontWeight: '600', color: colors.textPrimary },
  h3: { fontSize: 16, fontWeight: '600', color: colors.textPrimary },
  body: { fontSize: 15, fontWeight: '400', color: colors.textPrimary },
  bodyMedium: { fontSize: 15, fontWeight: '600', color: colors.textPrimary },
  bodySmall: { fontSize: 13, fontWeight: '400', color: colors.textSecondary },
  caption: { fontSize: 12, fontWeight: '500', color: colors.textTertiary },
  button: { fontSize: 15, fontWeight: '600' },
};
