import { Box } from "@mui/material";
import { colors, radii } from "@solidariaplus/ui/tokens";
import QRCode from "qrcode";
import { useMemo } from "react";

const QUIET_ZONE = 2;

export function qrPath(text: string): { readonly size: number; readonly path: string } {
  const { modules } = QRCode.create(text, { errorCorrectionLevel: "M" });
  const commands: string[] = [];
  for (let row = 0; row < modules.size; row += 1) {
    for (let column = 0; column < modules.size; column += 1) {
      if (modules.get(row, column)) {
        commands.push(`M${column + QUIET_ZONE} ${row + QUIET_ZONE}h1v1h-1z`);
      }
    }
  }
  return { size: modules.size + QUIET_ZONE * 2, path: commands.join("") };
}

export interface TotpQrCodeProps {
  readonly otpauthUri: string;
}

export function TotpQrCode({ otpauthUri }: TotpQrCodeProps) {
  const { size, path } = useMemo(() => qrPath(otpauthUri), [otpauthUri]);
  return (
    <Box
      sx={{
        width: 148,
        p: 1,
        bgcolor: colors.surface,
        border: `1px solid ${colors.borderSubtle}`,
        borderRadius: `${radii.panel}px`,
      }}
    >
      <svg
        viewBox={`0 0 ${size} ${size}`}
        width="130"
        height="130"
        role="img"
        aria-label="Código QR para agregar Solidaria+ a tu app de autenticación"
        shapeRendering="crispEdges"
      >
        <rect width={size} height={size} fill={colors.surface} />
        <path d={path} fill={colors.text} />
      </svg>
    </Box>
  );
}
