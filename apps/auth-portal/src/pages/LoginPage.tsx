import { Box, Button, Divider, Link, Stack, Typography } from "@mui/material";
import { messageForCode } from "@solidariaplus/auth";
import {
  AsidePanel,
  AuthShell,
  FormField,
  NoticeBanner,
  PasswordField,
  StatusBadge,
} from "@solidariaplus/ui";
import { useMemo, useState } from "react";
import { NativeForm } from "../forms/NativeForm";
import { useFieldErrors, type FieldValidators } from "../forms/useFieldErrors";
import { PageHeading } from "../components/PageHeading";
import type { LoginPageData } from "../page-data/page-data";

const SIGN_IN_ASIDE = {
  title: "Antes de entrar",
  items: [
    "¿Eres socio y aún no tienes contraseña? Pide tu código de activación en ventanilla.",
    "El personal entra con su usuario institucional y luego con el código de su app de autenticación.",
    "Solidaria+ nunca te pedirá tu contraseña por teléfono, mensaje ni correo.",
  ],
  footnote: "¿Necesitas ayuda? Acércate a la ventanilla de tu caja.",
};

const ACTIVATED_ASIDE = {
  title: "Un paso más, si quieres",
  items: [
    "Guarda tu contraseña en el gestor de contraseñas de tu navegador o teléfono.",
    "Si algún día olvidas tu contraseña, pide un código nuevo en ventanilla.",
  ],
  footnote: "Tu código de ventanilla ya no se puede volver a usar.",
};

interface Credentials {
  readonly username: string;
  readonly password: string;
}

type CredentialField = keyof Credentials;

export function LoginPage({ data }: { readonly data: LoginPageData }) {
  const [showForm, setShowForm] = useState(!data.activated);

  if (!showForm) {
    return (
      <AuthShell aside={<AsidePanel {...ACTIVATED_ASIDE} />}>
        <Stack spacing={2.5} sx={{ my: { md: "auto" }, maxWidth: 420 }}>
          <StatusBadge tone="success" />
          <PageHeading
            title="Tu cuenta está activa"
            description="Ya puedes iniciar sesión con tu número de socio y tu nueva contraseña."
          />
          <Button variant="contained" size="large" onClick={() => setShowForm(true)}>
            Iniciar sesión
          </Button>
        </Stack>
      </AuthShell>
    );
  }

  return (
    <AuthShell aside={<AsidePanel {...SIGN_IN_ASIDE} />}>
      <PageHeading title="Inicia sesión" description="Para socios y personal de la caja." />
      <SignInForm data={data} autoFocus={data.activated} />
    </AuthShell>
  );
}

function SignInForm({
  data,
  autoFocus,
}: {
  readonly data: LoginPageData;
  readonly autoFocus: boolean;
}) {
  const [values, setValues] = useState<Credentials>({ username: "", password: "" });
  const validators = useMemo<FieldValidators<Credentials, CredentialField>>(
    () => ({
      username: ({ username }) =>
        username.trim() ? undefined : "Escribe tu número de socio o tu usuario institucional.",
      password: ({ password }) => (password ? undefined : "Escribe tu contraseña."),
    }),
    [],
  );
  const { errors, validateField, clearField, validateAll } = useFieldErrors(validators);

  const update = (field: CredentialField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    clearField(field);
  };

  return (
    <Stack spacing={2.5}>
      {data.error && (
        <NoticeBanner tone="error" title="No pudimos iniciar tu sesión">
          {messageForCode(data.error)}
        </NoticeBanner>
      )}
      <NativeForm action={data.action} csrf={data.csrf} validate={() => validateAll(values)}>
        {(pending) => (
          <>
            <FormField
              label="Número de socio o usuario institucional"
              name="username"
              autoComplete="username"
              autoFocus={autoFocus || Boolean(data.error)}
              value={values.username}
              onChange={(event) => update("username", event.target.value)}
              onBlur={() => validateField("username", values)}
              errorText={errors.username}
              helperText="Socios: tu número de socio. Personal: tu usuario de la caja, como COOP\jvaldez."
              inputProps={{ autoCapitalize: "none", spellCheck: false, maxLength: 128 }}
            />
            <PasswordField
              label="Contraseña"
              name="password"
              autoComplete="current-password"
              value={values.password}
              onChange={(event) => update("password", event.target.value)}
              onBlur={() => validateField("password", values)}
              errorText={errors.password}
              helperText="Después de 5 intentos fallidos, el acceso se pausa 15 minutos."
              inputProps={{ maxLength: 128 }}
            />
            <Button type="submit" variant="contained" size="large" loading={pending}>
              Iniciar sesión
            </Button>
          </>
        )}
      </NativeForm>
      <Divider />
      <Box>
        <Typography variant="body2" color="text.secondary">
          ¿Es tu primera vez o olvidaste tu contraseña?{" "}
          <Link href={data.activateUrl}>Activa tu cuenta con el código de ventanilla</Link>
        </Typography>
      </Box>
    </Stack>
  );
}
