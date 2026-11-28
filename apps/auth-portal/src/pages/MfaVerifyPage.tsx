import { Button } from "@mui/material";
import { messageForCode } from "@solidariaplus/auth";
import { AsidePanel, AuthShell, BackLink, NoticeBanner } from "@solidariaplus/ui";
import { useState } from "react";
import { OneTimeCodeField, otpError } from "../components/OneTimeCodeField";
import { PageHeading } from "../components/PageHeading";
import { TotpCountdown } from "../components/TotpCountdown";
import { NativeForm } from "../forms/NativeForm";
import type { MfaPageData } from "../page-data/page-data";

const VERIFY_ASIDE = {
  title: "Por qué pedimos un segundo paso",
  items: [
    "El personal maneja el dinero de todos los socios: el código evita que alguien entre solo con una contraseña.",
    "Cada código sirve una sola vez y cambia cada 30 segundos.",
    "Tu jornada de sesión dura hasta 10 horas; después volverás a ingresar.",
  ],
  footnote: "Nadie de la caja te pedirá este código.",
};

export function MfaVerifyPage({ data }: { readonly data: MfaPageData }) {
  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string>();

  return (
    <AuthShell aside={<AsidePanel {...VERIFY_ASIDE} />}>
      <BackLink href={data.loginUrl}>Volver a iniciar sesión</BackLink>
      <PageHeading
        title="Ingresa tu código de verificación"
        description="Abre tu app de autenticación y escribe el código de 6 dígitos de Solidaria+."
      />
      {data.error && <NoticeBanner tone="error">{messageForCode(data.error)}</NoticeBanner>}
      <NativeForm
        action={data.action}
        csrf={data.csrf}
        validate={() => {
          const message = otpError(otp);
          setError(message);
          return message === undefined;
        }}
      >
        {(pending) => (
          <>
            <OneTimeCodeField
              label="Código de 6 dígitos"
              value={otp}
              autoFocus
              onChange={(value) => {
                setOtp(value);
                setError(undefined);
              }}
              errorText={error}
            />
            <TotpCountdown />
            <Button type="submit" variant="contained" size="large" loading={pending}>
              Verificar
            </Button>
          </>
        )}
      </NativeForm>
      <NoticeBanner tone="neutral" title="¿Perdiste o cambiaste tu teléfono?">
        El Presidente o el delegado de Cumplimiento restablecen tu segundo factor en persona.
      </NoticeBanner>
    </AuthShell>
  );
}
