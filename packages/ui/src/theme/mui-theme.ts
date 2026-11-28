import { createTheme, type Theme } from "@mui/material/styles";
import {
  colors,
  fontFamilies,
  fontSizes,
  fontWeights,
  motion,
  radii,
  shadows,
  spacing,
} from "./tokens";

const focusRing = {
  outline: `3px solid ${colors.focusRing}`,
  outlineOffset: 2,
};

export function createSolidariaTheme(): Theme {
  const base = createTheme({
    palette: {
      mode: "light",
      primary: {
        main: colors.primary,
        dark: colors.primaryHover,
        contrastText: colors.onPrimary,
      },
      secondary: { main: colors.navy, contrastText: colors.onNavy },
      success: { main: colors.success, light: colors.successSurface },
      warning: { main: colors.warning, light: colors.warningSurface },
      error: { main: colors.danger, light: colors.dangerSurface },
      info: { main: colors.info, light: colors.infoSurface },
      text: { primary: colors.text, secondary: colors.textMuted, disabled: colors.textSubtle },
      divider: colors.borderSubtle,
      background: { default: colors.background, paper: colors.surface },
    },
    spacing: spacing.unit,
    shape: { borderRadius: radii.control },
    typography: {
      fontFamily: fontFamilies.body,
      fontSize: fontSizes.label,
      h1: {
        fontFamily: fontFamilies.heading,
        fontSize: fontSizes.display,
        fontWeight: fontWeights.bold,
        lineHeight: 1.2,
        letterSpacing: "-0.02em",
      },
      h2: {
        fontFamily: fontFamilies.heading,
        fontSize: fontSizes.title,
        fontWeight: fontWeights.semiBold,
        lineHeight: 1.3,
        letterSpacing: "-0.01em",
      },
      h3: {
        fontFamily: fontFamilies.heading,
        fontSize: 17,
        fontWeight: fontWeights.semiBold,
        lineHeight: 1.35,
      },
      subtitle1: { fontSize: fontSizes.body, lineHeight: 1.55, color: colors.textMuted },
      body1: { fontSize: fontSizes.body, lineHeight: 1.55 },
      body2: { fontSize: fontSizes.label, lineHeight: 1.5 },
      caption: { fontSize: fontSizes.caption, lineHeight: 1.45, color: colors.textMuted },
      button: {
        fontSize: fontSizes.label,
        fontWeight: fontWeights.semiBold,
        textTransform: "none",
        letterSpacing: 0,
      },
    },
    transitions: {
      duration: { shortest: motion.fast, shorter: motion.fast, short: motion.standard },
    },
  });

  return createTheme(base, {
    typography: {
      h1: { [base.breakpoints.down("sm")]: { fontSize: fontSizes.displayCompact } },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: { backgroundColor: colors.background, color: colors.text },
          "::selection": { backgroundColor: colors.infoBorder },
          "@media (prefers-reduced-motion: reduce)": {
            "*, *::before, *::after": {
              animationDuration: "0.01ms !important",
              animationIterationCount: "1 !important",
              transitionDuration: "0.01ms !important",
              scrollBehavior: "auto !important",
            },
          },
        },
      },
      MuiButtonBase: {
        defaultProps: { disableRipple: true },
        styleOverrides: { root: { "&.Mui-focusVisible": focusRing } },
      },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: {
            minHeight: spacing.controlHeight,
            borderRadius: radii.control,
            paddingInline: base.spacing(2.5),
            transition: `background-color ${motion.standard}ms ${motion.easing}, border-color ${motion.standard}ms ${motion.easing}, color ${motion.standard}ms ${motion.easing}`,
          },
          containedPrimary: {
            "&:hover": { backgroundColor: colors.primaryHover },
            "&:active": { backgroundColor: colors.primaryActive },
          },
          outlined: {
            borderColor: colors.borderSubtle,
            color: colors.text,
            backgroundColor: colors.surface,
            "&:hover": { borderColor: colors.border, backgroundColor: colors.surfaceMuted },
          },
          text: { "&:hover": { backgroundColor: colors.infoSurface } },
          sizeSmall: { minHeight: 36, paddingInline: base.spacing(1.5) },
        },
      },
      MuiOutlinedInput: {
        defaultProps: { notched: false },
        styleOverrides: {
          root: {
            minHeight: spacing.controlHeight,
            borderRadius: radii.control,
            backgroundColor: colors.surface,
            fontSize: fontSizes.body,
            "& .MuiOutlinedInput-notchedOutline": { borderColor: colors.border, top: 0 },
            "& .MuiOutlinedInput-notchedOutline legend": { display: "none" },
            "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: colors.textSubtle },
            "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
              borderColor: colors.primary,
              borderWidth: 2,
            },
            "&.Mui-focused": { boxShadow: `0 0 0 3px ${colors.infoSurface}` },
            "&.Mui-error .MuiOutlinedInput-notchedOutline": { borderColor: colors.danger },
          },
          input: { paddingBlock: 11, paddingInline: 14 },
        },
      },
      MuiFormLabel: {
        styleOverrides: {
          root: {
            color: colors.text,
            fontSize: fontSizes.label,
            fontWeight: fontWeights.semiBold,
            marginBottom: base.spacing(0.75),
            "&.Mui-focused": { color: colors.text },
            "&.Mui-error": { color: colors.text },
          },
        },
      },
      MuiFormHelperText: {
        styleOverrides: {
          root: {
            marginInline: 0,
            marginTop: base.spacing(0.75),
            fontSize: fontSizes.caption,
            color: colors.textMuted,
            "&.Mui-error": { color: colors.danger },
          },
        },
      },
      MuiLink: {
        defaultProps: { underline: "always" },
        styleOverrides: {
          root: {
            color: colors.link,
            fontWeight: fontWeights.semiBold,
            textUnderlineOffset: 3,
            borderRadius: 4,
            "&:hover": { color: colors.primaryActive },
            "&:focus-visible": focusRing,
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: {
            height: 26,
            borderRadius: 6,
            fontSize: 12,
            fontWeight: fontWeights.semiBold,
            backgroundColor: colors.infoSurface,
            color: colors.info,
          },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: { borderRadius: radii.card, boxShadow: shadows.dialog, padding: base.spacing(1) },
        },
      },
      MuiDialogTitle: {
        styleOverrides: {
          root: {
            fontFamily: fontFamilies.heading,
            fontSize: fontSizes.title,
            fontWeight: fontWeights.bold,
          },
        },
      },
      MuiAlert: {
        styleOverrides: {
          root: { borderRadius: radii.panel, fontSize: fontSizes.label, alignItems: "flex-start" },
        },
      },
      MuiLinearProgress: {
        styleOverrides: {
          root: { height: 4, borderRadius: 2, backgroundColor: colors.borderSubtle },
        },
      },
    },
  });
}

export const solidariaTheme: Theme = createSolidariaTheme();
