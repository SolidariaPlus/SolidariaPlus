import { Box, Button, Stack, Typography } from "@mui/material";
import { isAuthError, toUserMessage } from "@solidariaplus/auth";
import {
  formatActivationCode,
  MEMBER_NUMBER_PATTERN,
  type IssuedActivationCode,
} from "@solidariaplus/identity-api";
import {
  BackLink,
  ClockIcon,
  CopyableCode,
  FormField,
  NoticeBanner,
  SurfaceCard,
} from "@solidariaplus/ui";
import { colors } from "@solidariaplus/ui/tokens";
import { useState, type FormEvent } from "react";
import { Link as RouterLink, useNavigate } from "react-router";
import { formatDateTime, hoursBetween } from "../../shared/format";
import { useIdentityApi } from "../identity/IdentityApiContext";
import { AppShell } from "../shell/AppShell";
import { PageTitle } from "../shell/PageTitle";

const FIELD_ERRORS = new Set(["MEMBER_NOT_FOUND", "MEMBER_NOT_ELIGIBLE"]);

export function IssueActivationCodePage() {
  const [issued, setIssued] = useState<IssuedActivationCode>();

  return (
    <AppShell>
      <BackLink component={RouterLink} to="/">
        Volver a mi inicio
      </BackLink>
      <Box sx={{ mt: 2 }}>
        <PageTitle
          title="Emitir código de activación"
          description="El socio lo usará para activar su cuenta o crear una nueva contraseña. Debe estar en ventanilla."
        />
      </Box>
      <Box sx={{ maxWidth: 640 }}>
        <SurfaceCard>
          {issued ? <IssuedCodeView issued={issued} /> : <IssueForm onIssued={setIssued} />}
        </SurfaceCard>
      </Box>
    </AppShell>
  );
}

function IssueForm({ onIssued }: { readonly onIssued: (code: IssuedActivationCode) => void }) {
  const api = useIdentityApi();
  const [memberNumber, setMemberNumber] = useState("");
  const [fieldError, setFieldError] = useState<string>();
  const [bannerError, setBannerError] = useState<string>();
  const [pending, setPending] = useState(false);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBannerError(undefined);
    if (!MEMBER_NUMBER_PATTERN.test(memberNumber)) {
      setFieldError("Escribe el número de socio: solo números, hasta 10 dígitos.");
      return;
    }
    setPending(true);
    api
      .issueActivationCode(memberNumber)
      .then(onIssued)
      .catch((error: unknown) => {
        const message = toUserMessage(error);
        if (isAuthError(error) && FIELD_ERRORS.has(error.code)) setFieldError(message);
        else setBannerError(message);
      })
      .finally(() => setPending(false));
  };

  return (
    <Stack component="form" spacing={2.5} noValidate onSubmit={submit}>
      {bannerError && <NoticeBanner tone="error">{bannerError}</NoticeBanner>}
      <FormField
        label="Número de socio"
        value={memberNumber}
        autoFocus
        autoComplete="off"
        onChange={(event) => {
          setMemberNumber(event.target.value.replace(/\D/g, "").slice(0, 10));
          setFieldError(undefined);
        }}
        errorText={fieldError}
        helperText="Pide al socio su libreta o certificado y verifica su identidad antes de emitir."
        inputProps={{ inputMode: "numeric", maxLength: 10 }}
      />
      <Button
        type="submit"
        variant="contained"
        size="large"
        loading={pending}
        sx={{ alignSelf: "flex-start" }}
      >
        Emitir código
      </Button>
    </Stack>
  );
}

function IssuedCodeView({ issued }: { readonly issued: IssuedActivationCode }) {
  const navigate = useNavigate();
  return (
    <Stack spacing={2.5}>
      <Typography variant="body2" color="text.secondary" role="status">
        Código para el socio{" "}
        <Box component="strong" sx={{ color: colors.text }}>
          {issued.memberNumber}
        </Box>
      </Typography>
      <CopyableCode value={issued.code} display={formatActivationCode(issued.code)} />
      <Typography
        variant="body2"
        color="text.secondary"
        sx={{ display: "flex", alignItems: "center", gap: 1 }}
      >
        <ClockIcon size={16} />
        Vence el {formatDateTime(issued.expiresAt)}, en{" "}
        {hoursBetween(issued.issuedAt, issued.expiresAt)} horas. Sirve una sola vez.
      </Typography>
      <NoticeBanner tone="warning">
        Este código no se volverá a mostrar. Entrégalo ahora: el socio lo escribe en Activa tu
        cuenta, en la web o en la app.
      </NoticeBanner>
      <Button
        variant="contained"
        size="large"
        sx={{ alignSelf: "flex-start" }}
        onClick={() => navigate("/", { replace: true })}
      >
        Listo, ya lo entregué
      </Button>
    </Stack>
  );
}
