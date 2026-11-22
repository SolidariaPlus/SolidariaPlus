import type { ButtonHTMLAttributes } from "react";

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

/** Botón base compartido entre web y desktop. */
export function Button({ type = "button", ...props }: ButtonProps) {
  return <button type={type} {...props} />;
}
