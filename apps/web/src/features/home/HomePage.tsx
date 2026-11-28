import { Box } from "@mui/material";
import { ActionCard, PlaceholderCard } from "@solidariaplus/ui";
import { Link as RouterLink } from "react-router";
import {
  CAN_ISSUE_ACTIVATION_CODES,
  CAN_RESET_STAFF_AUTHENTICATOR,
  satisfies,
} from "../auth/access/roles";
import { useAuth } from "../auth/hooks/useAuth";
import { sessionWindow } from "../auth/session/session-start";
import { AppShell } from "../shell/AppShell";
import { PageTitle } from "../shell/PageTitle";
import { formatTime } from "../../shared/format";

export function HomePage({ storage }: { readonly storage?: Storage }) {
  const { claims } = useAuth();
  const session = claims ? sessionWindow(claims, storage) : undefined;

  return (
    <AppShell>
      <PageTitle
        title="Tu inicio"
        description={
          session
            ? `Sesión iniciada hoy a las ${formatTime(session.startedAt)}. Se cerrará a las ${formatTime(session.endsAt)}, al cumplir ${session.maxHours} horas.`
            : undefined
        }
      />
      <Box
        sx={{
          display: "grid",
          gap: 2,
          gridTemplateColumns: {
            xs: "1fr",
            sm: "repeat(2, minmax(0, 1fr))",
            lg: "repeat(3, minmax(0, 1fr))",
          },
        }}
      >
        {satisfies(claims, CAN_ISSUE_ACTIVATION_CODES) && (
          <ActionCard
            title="Emitir código de activación"
            description="Para que un socio active su cuenta o cree una nueva contraseña. El socio debe estar en ventanilla."
            actionLabel="Emitir código"
            actionComponent={RouterLink}
            actionTo="/activation-codes/new"
          />
        )}
        {satisfies(claims, CAN_RESET_STAFF_AUTHENTICATOR) && (
          <ActionCard
            title="Restablecer segundo factor"
            description="Para un miembro del personal que perdió o cambió su teléfono. La persona debe estar presente."
            actionLabel="Restablecer"
            actionComponent={RouterLink}
            actionTo="/staff/authenticator-resets"
          />
        )}
        <PlaceholderCard
          title="Tus módulos aparecerán aquí"
          description="Ahorros, créditos y caja se mostrarán según tu rol cuando estén disponibles."
        />
      </Box>
    </AppShell>
  );
}
