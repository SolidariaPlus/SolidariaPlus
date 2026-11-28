import { Box, Button, Stack, Typography } from "@mui/material";
import { isAuthError, toUserMessage } from "@solidariaplus/auth";
import type { AuthenticatorReset } from "@solidariaplus/identity-api";
import { BackLink, ConfirmDialog, FormField, NoticeBanner, SurfaceCard } from "@solidariaplus/ui";
import { useState, type FormEvent } from "react";
import { Link as RouterLink } from "react-router";
import { formatDateTime } from "../../shared/format";
import { useIdentityApi } from "../identity/IdentityApiContext";
import { AppShell } from "../shell/AppShell";
import { PageTitle } from "../shell/PageTitle";

const ENROLMENT_WINDOW_MS = 24 * 3_600_000;
const FIELD_ERRORS = new Set(["NOT_FOUND", "SAME_PERSON_NOT_ALLOWED", "VALIDATION_FAILED"]);

interface CompletedReset {
  readonly account: string;
  readonly reset: AuthenticatorReset;
}

export function ResetAuthenticatorPage({ clock = Date.now }: { readonly clock?: () => number }) {
  const api = useIdentityApi();
  const [account, setAccount] = useState("");
  const [fieldError, setFieldError] = useState<string>();
  const [bannerError, setBannerError] = useState<string>();
  const [confirming, setConfirming] = useState(false);
  const [pending, setPending] = useState(false);
  const [completed, setCompleted] = useState<CompletedReset>();

  const requestConfirmation = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBannerError(undefined);
    if (!account.trim()) {
      setFieldError("Escribe el usuario institucional, por ejemplo COOP\\jvaldez.");
      return;
    }
    setConfirming(true);
  };

  const confirm = () => {
    setPending(true);
    api
      .resetStaffAuthenticator(account)
      .then((reset) => {
        setCompleted({ account: account.trim(), reset });
        setAccount("");
      })
      .catch((error: unknown) => {
        const message = toUserMessage(error);
        if (isAuthError(error) && FIELD_ERRORS.has(error.code)) setFieldError(message);
        else setBannerError(message);
      })
      .finally(() => {
        setPending(false);
        setConfirming(false);
      });
  };

  return (
    <AppShell>
      <BackLink component={RouterLink} to="/">
        Volver a mi inicio
      </BackLink>
      <Box sx={{ mt: 2 }}>
        <PageTitle
          title="Restablecer segundo factor"
          description="Para un miembro del personal que perdió o cambió su teléfono. La persona debe estar presente."
        />
      </Box>
      <Stack spacing={2} sx={{ maxWidth: 640 }}>
        {completed && (
          <NoticeBanner
            tone="success"
            title={`Listo: restableciste el segundo factor de ${completed.account}`}
          >
            Debe configurar su app de autenticación antes del{" "}
            {formatDateTime(completed.reset.enrolmentDeadline)}. Cerramos{" "}
            {completed.reset.sessionsRevoked === 1
              ? "1 sesión abierta"
              : `${completed.reset.sessionsRevoked} sesiones abiertas`}
            .
          </NoticeBanner>
        )}
        {bannerError && <NoticeBanner tone="error">{bannerError}</NoticeBanner>}
        <SurfaceCard>
          <Stack component="form" spacing={2.5} noValidate onSubmit={requestConfirmation}>
            <FormField
              label="Usuario institucional de la persona"
              value={account}
              autoFocus
              autoComplete="off"
              onChange={(event) => {
                setAccount(event.target.value);
                setFieldError(undefined);
              }}
              errorText={fieldError}
              helperText="No puedes restablecer tu propia cuenta: lo hace la otra función de la Junta."
              inputProps={{ autoCapitalize: "none", spellCheck: false, maxLength: 64 }}
            />
            <Button type="submit" variant="contained" size="large" sx={{ alignSelf: "flex-start" }}>
              Restablecer segundo factor
            </Button>
          </Stack>
        </SurfaceCard>
      </Stack>
      <ConfirmDialog
        open={confirming}
        title={`¿Restablecer el segundo factor de ${account.trim()}?`}
        confirmLabel="Sí, restablecer"
        pending={pending}
        onConfirm={confirm}
        onCancel={() => setConfirming(false)}
      >
        <Typography variant="body2">Se cerrarán todas sus sesiones abiertas.</Typography>
        <Typography variant="body2">
          Deberá configurar su app de autenticación antes del{" "}
          {formatDateTime(new Date(clock() + ENROLMENT_WINDOW_MS))}.
        </Typography>
        <Typography variant="body2">Quedará registrado que tú hiciste este cambio.</Typography>
      </ConfirmDialog>
    </AppShell>
  );
}
