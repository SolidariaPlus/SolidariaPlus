export const colors = {
  primary: "#515DEF",
  onPrimary: "#F3F3F3",
  accent: "#FF8682",
  text: "#313131",
  inputText: "#1C1B1F",
  outline: "#79747E",
  surface: "#FFFFFF",
  surfaceMuted: "#D9D9D9",
  carouselInactive: "#FFFFFF",
} as const;

export const fontFamilies = {
  body: '"Poppins", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  brand: '"Red Hat Display", "Poppins", system-ui, sans-serif',
} as const;

export const fontWeights = {
  regular: 400,
  medium: 500,
  semiBold: 600,
  bold: 700,
} as const;

export const fontSizes = {
  display: 40,
  displayCompact: 32,
  brand: 35.333,
  body: 16,
  label: 14,
} as const;

export const radii = {
  control: 4,
  card: 30,
} as const;

export const spacing = {
  unit: 8,
  pageGutter: 104,
  controlHeight: 48,
} as const;

export const opacities = {
  secondaryText: 0.75,
  mutedText: 0.5,
  divider: 0.25,
} as const;

export const designTokens = {
  colors,
  fontFamilies,
  fontWeights,
  fontSizes,
  radii,
  spacing,
  opacities,
} as const;

export type DesignTokens = typeof designTokens;
