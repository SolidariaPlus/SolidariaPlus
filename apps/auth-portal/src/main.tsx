import "@fontsource/inter/400.css";
import "@fontsource/inter/500.css";
import "@fontsource/inter/600.css";
import "@fontsource/plus-jakarta-sans/600.css";
import "@fontsource/plus-jakarta-sans/700.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { parsePageData } from "./page-data/page-data";
import { PortalApp } from "./PortalApp";

const container = document.getElementById("root");
if (container) {
  const data = parsePageData(container.dataset);
  container.replaceChildren();
  createRoot(container).render(
    <StrictMode>
      <PortalApp data={data} />
    </StrictMode>,
  );
}
