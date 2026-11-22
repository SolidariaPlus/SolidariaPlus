import type { ButtonHTMLAttributes } from "react";

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

/** Base button shared by web and desktop. */
export function Button({ type = "button", ...props }: ButtonProps) {
  return <button type={type} {...props} />;
}
