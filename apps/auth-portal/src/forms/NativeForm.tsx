import { Box } from "@mui/material";
import { useRef, useState, type FormEvent, type ReactNode } from "react";
import type { CsrfToken } from "../page-data/page-data";

export interface NativeFormProps {
  readonly action: string;
  readonly csrf: CsrfToken;
  readonly validate?: () => boolean;
  readonly children: (pending: boolean) => ReactNode;
}

export function NativeForm({ action, csrf, validate, children }: NativeFormProps) {
  const [pending, setPending] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    if (pending) {
      event.preventDefault();
      return;
    }
    if (validate && !validate()) {
      event.preventDefault();
      requestAnimationFrame(() => {
        formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
      });
      return;
    }
    setPending(true);
  };

  return (
    <Box
      component="form"
      ref={formRef}
      method="post"
      action={action}
      noValidate
      onSubmit={handleSubmit}
      sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}
    >
      <input type="hidden" name={csrf.parameterName} value={csrf.token} />
      {children(pending)}
    </Box>
  );
}
