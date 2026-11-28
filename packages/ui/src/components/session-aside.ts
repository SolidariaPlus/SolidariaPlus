import type { AsidePanelProps } from "./AsidePanel";

export const SESSION_DURATION_ASIDE: AsidePanelProps = {
  title: "Cuánto dura una sesión",
  items: [
    "Personal: hasta 10 horas; después se pide ingresar con el segundo factor.",
    "Socios en la web: hasta 12 horas.",
    "Socios en el teléfono: si no usas la app por 30 días, te pediremos ingresar de nuevo.",
  ],
  footnote: "Cerrar sesión en todos tus dispositivos tarda unos segundos.",
};

export const DESKTOP_RETURN_ASIDE: AsidePanelProps = {
  title: "¿No volviste a la aplicación?",
  items: [
    "Busca la ventana de Solidaria+ en la barra de tareas.",
    "Si la cerraste, ábrela e inicia sesión otra vez.",
  ],
  footnote: "Esta pestaña no guarda tu sesión.",
};
