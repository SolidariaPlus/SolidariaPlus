import { FormField } from "@solidariaplus/ui";
import { fontFamilies } from "@solidariaplus/ui/tokens";
import type { ReactNode } from "react";

export const OTP_LENGTH = 6;

export interface OneTimeCodeFieldProps {
  readonly label: string;
  readonly value: string;
  readonly onChange: (value: string) => void;
  readonly onBlur?: () => void;
  readonly errorText?: string;
  readonly helperText?: ReactNode;
  readonly autoFocus?: boolean;
}

export function OneTimeCodeField({
  label,
  value,
  onChange,
  onBlur,
  errorText,
  helperText,
  autoFocus,
}: OneTimeCodeFieldProps) {
  return (
    <FormField
      label={label}
      name="otp"
      value={value}
      autoFocus={autoFocus}
      autoComplete="one-time-code"
      onChange={(event) => onChange(event.target.value.replace(/\D/g, "").slice(0, OTP_LENGTH))}
      onBlur={onBlur}
      errorText={errorText}
      helperText={helperText}
      inputProps={{
        inputMode: "numeric",
        pattern: "[0-9]{6}",
        maxLength: OTP_LENGTH,
        spellCheck: false,
      }}
      sx={{
        "& input": {
          fontFamily: fontFamilies.heading,
          fontSize: 22,
          fontWeight: 600,
          letterSpacing: "0.4em",
          fontVariantNumeric: "tabular-nums",
        },
      }}
    />
  );
}

export function otpError(value: string): string | undefined {
  if (value.length === 0) return "Escribe el código de 6 dígitos que muestra tu app.";
  if (value.length !== OTP_LENGTH) return "El código tiene 6 dígitos.";
  return undefined;
}
