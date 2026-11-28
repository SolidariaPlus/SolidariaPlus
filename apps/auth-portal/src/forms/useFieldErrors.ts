import { useCallback, useState } from "react";

export type FieldValidator<TValues> = (values: TValues) => string | undefined;

export type FieldValidators<TValues, TField extends string> = Readonly<
  Record<TField, FieldValidator<TValues>>
>;

export function useFieldErrors<TValues, TField extends string>(
  validators: FieldValidators<TValues, TField>,
) {
  const [errors, setErrors] = useState<Partial<Record<TField, string>>>({});

  const validateField = useCallback(
    (field: TField, values: TValues) => {
      setErrors((current) => ({ ...current, [field]: validators[field](values) }));
    },
    [validators],
  );

  const clearField = useCallback((field: TField) => {
    setErrors((current) => (current[field] ? { ...current, [field]: undefined } : current));
  }, []);

  const validateAll = useCallback(
    (values: TValues): boolean => {
      const next: Partial<Record<TField, string>> = {};
      for (const field of Object.keys(validators) as TField[]) {
        next[field] = validators[field](values);
      }
      setErrors(next);
      return Object.values(next).every((message) => message === undefined);
    },
    [validators],
  );

  return { errors, validateField, clearField, validateAll };
}
