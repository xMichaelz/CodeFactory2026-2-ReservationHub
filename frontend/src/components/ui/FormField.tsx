import {
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';

interface WrapperProps {
  label: string;
  hint?: ReactNode;
  error?: string | null;
  required?: boolean;
  children: (id: string) => ReactNode;
}

function FieldWrapper({ label, hint, error, required, children }: WrapperProps) {
  const id = useId();
  return (
    <div className={`field ${error ? 'field--error' : ''}`}>
      <label htmlFor={id} className="field__label">
        {label}
        {required && (
          <span className="field__required" aria-hidden="true">
            {' '}
            *
          </span>
        )}
      </label>
      {children(id)}
      {error ? <p className="field__error">{error}</p> : hint ? <p className="field__hint">{hint}</p> : null}
    </div>
  );
}

type Common = { label: string; hint?: ReactNode; error?: string | null };

export function TextField({ label, hint, error, required, ...rest }: Common & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      {(id) => <input id={id} className="input" required={required} {...rest} />}
    </FieldWrapper>
  );
}

export function TextAreaField({
  label,
  hint,
  error,
  required,
  ...rest
}: Common & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      {(id) => <textarea id={id} className="input input--textarea" required={required} {...rest} />}
    </FieldWrapper>
  );
}

export function SelectField({
  label,
  hint,
  error,
  required,
  children,
  ...rest
}: Common & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      {(id) => (
        <select id={id} className="input" required={required} {...rest}>
          {children}
        </select>
      )}
    </FieldWrapper>
  );
}
