export const colors = {
  navy: "#1E3A8A",
  navyRaised: "#24439A",
  onNavy: "#FFFFFF",
  onNavyMuted: "#DBE4FF",
  navyAccent: "#93C5FD",
  primary: "#2563EB",
  primaryHover: "#1D4ED8",
  primaryActive: "#1E40AF",
  onPrimary: "#FFFFFF",
  link: "#1D4ED8",
  text: "#0F172A",
  textMuted: "#475569",
  textSubtle: "#64748B",
  border: "#94A3B8",
  borderSubtle: "#E2E8F0",
  surface: "#FFFFFF",
  surfaceMuted: "#F1F5F9",
  background: "#F8FAFC",
  focusRing: "#93C5FD",
  success: "#15803D",
  successSurface: "#DCFCE7",
  warning: "#92400E",
  warningSurface: "#FFFBEB",
  warningBorder: "#FDE68A",
  danger: "#B91C1C",
  dangerSurface: "#FEF2F2",
  dangerBorder: "#FECACA",
  info: "#1E40AF",
  infoSurface: "#EFF6FF",
  infoBorder: "#BFDBFE",
} as const;

export const fontFamilies = {
  heading: '"Plus Jakarta Sans", "Inter", system-ui, -apple-system, "Segoe UI", sans-serif',
  body: '"Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  mono: '"JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace',
} as const;

export const fontWeights = {
  regular: 400,
  medium: 500,
  semiBold: 600,
  bold: 700,
} as const;

export const fontSizes = {
  display: 32,
  displayCompact: 26,
  title: 20,
  body: 15,
  label: 14,
  caption: 13,
  code: 40,
} as const;

export const radii = {
  control: 8,
  panel: 12,
  card: 16,
} as const;

export const spacing = {
  unit: 8,
  controlHeight: 44,
  cardMaxWidth: 1040,
  contentMaxWidth: 1120,
} as const;

export const shadows = {
  card: "0 1px 2px rgba(15, 23, 42, 0.04), 0 12px 32px -12px rgba(15, 23, 42, 0.12)",
  dialog: "0 24px 48px -12px rgba(15, 23, 42, 0.28)",
} as const;

export const motion = {
  fast: 150,
  standard: 200,
  easing: "cubic-bezier(0.2, 0, 0, 1)",
} as const;

export const designTokens = {
  colors,
  fontFamilies,
  fontWeights,
  fontSizes,
  radii,
  spacing,
  shadows,
  motion,
} as const;

export type DesignTokens = typeof designTokens;
