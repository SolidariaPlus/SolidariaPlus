import { Box } from "@mui/material";
import { colors } from "../theme/tokens";

const CELLS: ReadonlyArray<readonly [number, number]> = [
  [2, 0],
  [2, 1],
  [0, 2],
  [1, 2],
  [2, 2],
  [3, 2],
  [4, 2],
  [2, 3],
  [2, 4],
];

const STEP = 20;

export function PlusPattern() {
  return (
    <Box
      component="svg"
      width={STEP * 5}
      height={STEP * 5}
      viewBox={`0 0 ${STEP * 5} ${STEP * 5}`}
      aria-hidden
      focusable={false}
      sx={{
        display: "block",
        "@media (prefers-reduced-motion: no-preference)": {
          "& path": { animation: "plus-fade 600ms ease-out both" },
        },
        "@keyframes plus-fade": {
          from: { opacity: 0, transform: "scale(0.6)" },
          to: { opacity: 1, transform: "scale(1)" },
        },
      }}
    >
      {CELLS.map(([column, row], index) => {
        const x = column * STEP + STEP / 2;
        const y = row * STEP + STEP / 2;
        return (
          <path
            key={`${column}-${row}`}
            d={`M${x} ${y - 4.5}v9M${x - 4.5} ${y}h9`}
            stroke={colors.navyAccent}
            strokeWidth="2"
            strokeLinecap="round"
            style={{ animationDelay: `${index * 40}ms`, transformOrigin: `${x}px ${y}px` }}
          />
        );
      })}
    </Box>
  );
}
