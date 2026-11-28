import { Button, Stack } from "@mui/material";
import { messageForCode } from "@solidariaplus/auth";
import {
  evaluatePassword,
  formatActivationCode,
  isActivationCode,
  isPasswordAccepted,
  MEMBER_NUMBER_PATTERN,
  normalizeActivationCode,
} from "@solidariaplus/identity-api";
import {
  AsidePanel,
  AuthShell,
  BackLink,
  FormField,
  NoticeBanner,
  PasswordChecklist,
  PasswordField,
} from "@solidariaplus/ui";
import { fontFamilies } from "@solidariaplus/ui/tokens";
import { useMemo, useState } from "react";
import { PageHeading } from "../components/PageHeading";
import { NativeForm } from "../forms/NativeForm";
import { useFieldErrors, type FieldValidators } from "../forms/useFieldErrors";
import type { ActivatePageData } from "../page-data/page-data";

const ACTIVATE_ASIDE = {
  title: "Cómo funciona el código de ventanilla",
  items: [
    "Lo entrega un cajero en persona; nunca llega por mensaje ni correo.",
    "Vale 72 horas y se usa una sola vez. Si pides otro, el anterior deja de servir.",
    "Tu contraseña es solo tuya: nadie en la caja puede verla.",
  ],
  footnote: "Sirve también si olvidaste tu contraseña.",
};

interface ActivationValues {
  readonly memberNumber: string;
  readonly code: string;
  readonly password: string;
  readonly confirmation: string;
}

type ActivationField = keyof ActivationValues;

export function ActivatePage({ data }: { readonly data: ActivatePageData }) {
  const [values, setValues] = useState<ActivationValues>({
    memberNumber: data.memberNumber ?? "",
    code: "",
    password: "",
    confirmation: "",
  });
  const rules = useMemo(
    () => evaluatePassword(values.password, values.confirmation),
    [values.password, values.confirmation],
  );
  const validators = useMemo<FieldValidators<ActivationValues, ActivationField>>(
    () => ({
      memberNumber: ({ memberNumber }) =>
        MEMBER_NUMBER_PATTERN.test(memberNumber.trim())
          ? undefined
          : "Escribe tu número de socio: solo números, hasta 10 dígitos.",
      code: ({ code }) =>
        isActivationCode(code) ? undefined : "El código tiene 8 letras y números, como K8M4-P9X2.",
      password: ({ password }) => {
        if (!password) return "Crea tu nueva contraseña.";
        return isPasswordAccepted(evaluatePassword(password, password))
          ? undefined
          : "Tu contraseña aún no cumple todas las reglas de abajo.";
      },
      confirmation: ({ password, confirmation }) =>
        confirmation && confirmation === password
          ? undefined
          : "Las dos contraseñas deben coincidir.",
    }),
    [],
  );
  const { errors, validateField, clearField, validateAll } = useFieldErrors(validators);

  const update = (field: ActivationField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    clearField(field);
  };

  return (
    <AuthShell aside={<AsidePanel {...ACTIVATE_ASIDE} />}>
      <BackLink href={data.loginUrl}>Volver a iniciar sesión</BackLink>
      <PageHeading
        title="Activa tu cuenta"
        description="Usa el código que te entregaron en ventanilla y crea tu contraseña."
      />
      {data.error && <NoticeBanner tone="error">{messageForCode(data.error)}</NoticeBanner>}
      <NativeForm action={data.action} csrf={data.csrf} validate={() => validateAll(values)}>
        {(pending) => (
          <>
            <FormField
              label="Número de socio"
              name="memberNumber"
              autoComplete="username"
              autoFocus
              value={values.memberNumber}
              onChange={(event) => update("memberNumber", event.target.value.replace(/\D/g, ""))}
              onBlur={() => validateField("memberNumber", values)}
              errorText={errors.memberNumber}
              helperText="Está en tu libreta o en tu certificado de socio."
              inputProps={{ inputMode: "numeric", maxLength: 10 }}
            />
            <FormField
              label="Código de ventanilla"
              value={formatActivationCode(values.code)}
              onChange={(event) =>
                update("code", normalizeActivationCode(event.target.value).slice(0, 8))
              }
              onBlur={() => validateField("code", values)}
              errorText={errors.code}
              helperText="Tiene 8 letras y números. Puedes escribirlo con o sin guion."
              inputProps={{
                autoCapitalize: "characters",
                autoComplete: "off",
                spellCheck: false,
                maxLength: 9,
              }}
              sx={{
                "& input": {
                  fontFamily: fontFamilies.heading,
                  fontWeight: 600,
                  letterSpacing: "0.12em",
                  textTransform: "uppercase",
                },
              }}
            />
            <input
              type="hidden"
              name="activationCode"
              value={normalizeActivationCode(values.code)}
            />
            <PasswordField
              label="Nueva contraseña"
              name="newPassword"
              autoComplete="new-password"
              value={values.password}
              onChange={(event) => update("password", event.target.value)}
              onBlur={() => validateField("password", values)}
              errorText={errors.password}
              inputProps={{ maxLength: 128 }}
            />
            <PasswordField
              label="Repite la contraseña"
              autoComplete="new-password"
              value={values.confirmation}
              onChange={(event) => update("confirmation", event.target.value)}
              onBlur={() => values.confirmation && validateField("confirmation", values)}
              errorText={errors.confirmation}
              inputProps={{ maxLength: 128 }}
            />
            <PasswordChecklist rules={rules} />
            <Stack>
              <Button type="submit" variant="contained" size="large" loading={pending}>
                Activar cuenta
              </Button>
            </Stack>
          </>
        )}
      </NativeForm>
    </AuthShell>
  );
}
