import { Checkbox, FormControlLabel, Typography } from "@mui/material";
import { ConfirmDialog } from "@solidariaplus/ui";
import { useState } from "react";
import { useAuth } from "../auth/hooks/useAuth";

export interface SignOutDialogProps {
  readonly open: boolean;
  readonly onClose: () => void;
}

export function SignOutDialog({ open, onClose }: SignOutDialogProps) {
  const { signOut } = useAuth();
  const [everywhere, setEverywhere] = useState(false);
  const [pending, setPending] = useState(false);

  const confirm = () => {
    setPending(true);
    signOut(everywhere ? "all" : "current").finally(() => setPending(false));
  };

  return (
    <ConfirmDialog
      open={open}
      title="¿Cerrar sesión?"
      confirmLabel="Cerrar sesión"
      pending={pending}
      onConfirm={confirm}
      onCancel={onClose}
    >
      <Typography variant="body2" color="text.secondary">
        Cerraremos tu sesión en este navegador. Tus datos quedan guardados.
      </Typography>
      <FormControlLabel
        control={
          <Checkbox
            checked={everywhere}
            onChange={(event) => setEverywhere(event.target.checked)}
          />
        }
        label={
          <Typography variant="body2">
            <strong>En todos mis dispositivos</strong>
            <br />
            También en el teléfono y en la aplicación de escritorio. Tarda unos segundos.
          </Typography>
        }
        sx={{ alignItems: "flex-start", mt: 1, "& .MuiCheckbox-root": { pt: 0.25 } }}
      />
    </ConfirmDialog>
  );
}
