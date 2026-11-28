import { alpha, createTheme, type Theme } from "@mui/material/styles";
import { colors, fontFamilies, fontSizes, fontWeights, opacities, radii, spacing } from "./tokens";

export function createSolidariaTheme(): Theme {
  const base = createTheme({
    palette: {
      mode: "light",
      primary: { main: colors.primary, contrastText: colors.onPrimary },
      secondary: { main: colors.accent, contrastText: colors.text },
      text: {
        primary: colors.text,
        secondary: alpha(colors.text, opacities.secondaryText),
        disabled: alpha(colors.text, opacities.mutedText),
      },
      divider: alpha(colors.text, opacities.divider),
      background: { default: colors.surface, paper: colors.surface },
    },
    spacing: spacing.unit,
    shape: { borderRadius: radii.control },
    typography: {
      fontFamily: fontFamilies.body,
      h1: { fontSize: fontSizes.display, fontWeight: fontWeights.semiBold, lineHeight: 1.5 },
      subtitle1: { fontSize: fontSizes.body, fontWeight: fontWeights.regular, lineHeight: 1.5 },
      body1: { fontSize: fontSizes.body, fontWeight: fontWeights.regular },
      body2: { fontSize: fontSizes.label, fontWeight: fontWeights.medium },
      button: {
        fontSize: fontSizes.label,
        fontWeight: fontWeights.semiBold,
        textTransform: "none",
      },
    },
  });

  return createTheme(base, {
    typography: {
      h1: { [base.breakpoints.down("sm")]: { fontSize: fontSizes.displayCompact } },
    },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          sizeLarge: { minHeight: spacing.controlHeight, padding: base.spacing(1, 2) },
        },
      },
      MuiLink: {
        defaultProps: { underline: "hover", color: "secondary" },
        styleOverrides: {
          root: {
            fontWeight: fontWeights.semiBold,
            "&:focus-visible": {
              outline: `2px solid ${colors.primary}`,
              outlineOffset: 2,
              borderRadius: radii.control,
            },
          },
        },
      },
      MuiTextField: { defaultProps: { variant: "outlined", fullWidth: true } },
      MuiOutlinedInput: {
        styleOverrides: {
          root: { color: colors.inputText },
          notchedOutline: { borderColor: colors.outline },
        },
      },
    },
  });
}

export const solidariaTheme: Theme = createSolidariaTheme();
