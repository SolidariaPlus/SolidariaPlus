import { Button, Stack } from "@mui/material";
import { messageForCode } from "@solidariaplus/auth";
import { AsidePanel, AuthShell, CopyableCode, NoticeBanner } from "@solidariaplus/ui";
import { useState } from "react";
import { OneTimeCodeField, otpError } from "../components/OneTimeCodeField";
import { PageHeading } from "../components/PageHeading";
import { StepList } from "../components/StepList";
import { TotpQrCode } from "../components/TotpQrCode";
import { NativeForm } from "../forms/NativeForm";
import type { MfaPageData } from "../page-data/page-data";

const ENROL_ASIDE = {
  title: "Cuida tu app de autenticación",
  items: [
    "Si cambias o pierdes el teléfono, tendrás que configurarla de nuevo.",
    "Para el personal, el restablecimiento lo hacen en persona el Presidente o el delegado de Cumplimiento.",
    "No compartas los códigos con nadie, ni con el personal de la caja.",
  ],
  footnote: "Es un paso único: después solo escribirás el código al ingresar.",
};

const DEADLINE_FORMAT = new Intl.DateTimeFormat("es-EC", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

export function groupSecret(secret: string): string {
  return (
    secret
      .replace(/\s+/g, "")
      .match(/.{1,4}/g)
      ?.join(" ") ?? secret
  );
}

export function MfaEnrolPage({ data }: { readonly data: MfaPageData }) {
  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string>();
  const secret = data.secret ?? "";
  const otpauthUri = data.otpauthUri ?? "";

  return (
    <AuthShell aside={<AsidePanel {...ENROL_ASIDE} />}>
      {data.enrolmentDeadline && (
        <NoticeBanner tone="warning">
          Tu segundo factor fue restablecido. Configúralo antes del{" "}
          {DEADLINE_FORMAT.format(data.enrolmentDeadline)}; después necesitarás otro
          restablecimiento.
        </NoticeBanner>
      )}
      <PageHeading
        title="Configura tu app de autenticación"
        description="El personal lo necesita antes de su primer ingreso. Es un paso único."
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
            <StepList
              steps={[
                {
                  title: "Instala una app de autenticación",
                  description:
                    "Por ejemplo Google Authenticator o Microsoft Authenticator, desde la tienda de tu teléfono.",
                },
                {
                  title: "Agrega Solidaria+ a la app",
                  description:
                    "Escanea el código con la app. Si estás en el mismo teléfono, usa el botón o copia la clave.",
                  content: (
                    <Stack spacing={1.25} sx={{ alignItems: "stretch" }}>
                      <TotpQrCode otpauthUri={otpauthUri} />
                      <Button
                        variant="outlined"
                        href={otpauthUri}
                        sx={{ justifyContent: "flex-start" }}
                      >
                        Abrir en mi app de autenticación
                      </Button>
                      <CopyableCode
                        value={secret}
                        display={groupSecret(secret)}
                        size="compact"
                        copyLabel="Copiar"
                      />
                    </Stack>
                  ),
                },
                {
                  title: "Escribe el primer código que muestra la app",
                  content: (
                    <OneTimeCodeField
                      label="Primer código de 6 dígitos"
                      value={otp}
                      onChange={(value) => {
                        setOtp(value);
                        setError(undefined);
                      }}
                      errorText={error}
                    />
                  ),
                },
              ]}
            />
            <Button type="submit" variant="contained" size="large" loading={pending}>
              Confirmar y continuar
            </Button>
          </>
        )}
      </NativeForm>
    </AuthShell>
  );
}
