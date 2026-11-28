import { Button, InputAdornment } from "@mui/material";
import { useState } from "react";
import { colors } from "../theme/tokens";
import { FormField, type FormFieldProps } from "./FormField";

export interface PasswordFieldProps extends Omit<FormFieldProps, "type" | "endAdornment"> {
  readonly label: string;
}

export function PasswordField({ label, sx, ...props }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  return (
    <FormField
      {...props}
      label={label}
      type={visible ? "text" : "password"}
      sx={[{ pr: 0.75 }, ...(Array.isArray(sx) ? sx : [sx])]}
      endAdornment={
        <InputAdornment position="end">
          <Button
            size="small"
            aria-pressed={visible}
            aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
            onClick={() => setVisible((current) => !current)}
            sx={{
              minHeight: 32,
              color: colors.navy,
              bgcolor: colors.surfaceMuted,
              "&:hover": { bgcolor: colors.borderSubtle },
            }}
          >
            {visible ? "Ocultar" : "Mostrar"}
          </Button>
        </InputAdornment>
      }
    />
  );
}
