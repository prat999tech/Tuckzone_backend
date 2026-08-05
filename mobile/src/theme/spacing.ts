/** 4px-based spacing scale. Using only these values everywhere is what makes margins and
 *  padding look intentional instead of accidentally-close-but-not-quite across screens. */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 12, // grid-gutter
  lg: 16, // stack-gap
  xl: 20, // container-padding
  xxl: 24,
  xxxl: 32, // section-margin
} as const;

/**
 * Corner radii, matching the design system's `rounded` scale (rem values x16).
 * Cards use `xl`, thumbnails and info panels `lg`, and buttons/chips are fully rounded.
 */
export const radius = {
  sm: 4, // 0.25rem
  DEFAULT: 8, // 0.5rem — inputs
  md: 12, // 0.75rem
  lg: 16, // 1rem — thumbnails, info boxes
  xl: 24, // 1.5rem — cards
  full: 9999,
} as const;

/** Level 1 elevation from the design system: a very soft, diffused ambient shadow.
 *  Cards pair this with a 1px outline-variant border rather than relying on depth alone. */
export const shadow = {
  shadowColor: '#121c28',
  shadowOffset: { width: 0, height: 4 },
  shadowOpacity: 0.05,
  shadowRadius: 20,
  elevation: 2,
} as const;

/** Level 2 elevation — floating actions, bottom bars and active modals. */
export const shadowFloating = {
  shadowColor: '#121c28',
  shadowOffset: { width: 0, height: 8 },
  shadowOpacity: 0.12,
  shadowRadius: 30,
  elevation: 8,
} as const;
