import { Button } from "@mui/material";
import { useState } from "react";
import { useAuth } from "../hooks/useAuth";

export function SignInButton({ label }: { readonly label: string }) {
  const { signIn } = useAuth();
  const [pending, setPending] = useState(false);
  return (
    <Button
      variant="contained"
      size="large"
      loading={pending}
      onClick={() => {
        setPending(true);
        signIn().catch(() => setPending(false));
      }}
    >
      {label}
    </Button>
  );
}
