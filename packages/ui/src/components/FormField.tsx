import {
  FormControl,
  FormHelperText,
  FormLabel,
  OutlinedInput,
  type OutlinedInputProps,
} from "@mui/material";
import { useId, type ReactNode } from "react";

export interface FormFieldProps extends Omit<OutlinedInputProps, "error"> {
  readonly label: string;
  readonly helperText?: ReactNode;
  readonly errorText?: ReactNode;
}

export function FormField({ label, helperText, errorText, id, ...inputProps }: FormFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const describedBy = errorText || helperText ? `${inputId}-description` : undefined;
  return (
    <FormControl fullWidth error={Boolean(errorText)}>
      <FormLabel htmlFor={inputId}>{label}</FormLabel>
      <OutlinedInput
        id={inputId}
        {...inputProps}
        inputProps={{ ...inputProps.inputProps, "aria-describedby": describedBy }}
      />
      {(errorText || helperText) && (
        <FormHelperText id={describedBy}>{errorText ?? helperText}</FormHelperText>
      )}
    </FormControl>
  );
}
