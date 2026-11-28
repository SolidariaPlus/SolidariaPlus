import { Button, Stack } from "@mui/material";
import { NoticeBanner } from "@solidariaplus/ui";
import { Link as RouterLink } from "react-router";
import { AppShell } from "../shell/AppShell";
import { PageTitle } from "../shell/PageTitle";

export function ForbiddenPage() {
  return (
    <AppShell>
      <Stack spacing={3} sx={{ maxWidth: 560 }}>
        <PageTitle
          title="No tienes acceso a esta sección"
          description="Tu rol en la caja no incluye esta función. Si crees que deberías tenerla, consúltalo con la Junta directiva."
        />
        <NoticeBanner tone="info">
          Los permisos se asignan en el directorio de la caja. Cuando cambien, cierra sesión e
          ingresa de nuevo para verlos.
        </NoticeBanner>
        <Button
          component={RouterLink}
          to="/"
          variant="contained"
          size="large"
          sx={{ alignSelf: "flex-start" }}
        >
          Volver a mi inicio
        </Button>
      </Stack>
    </AppShell>
  );
}
