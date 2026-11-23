import type { ReactNode } from 'react';

type Kind = 'error' | 'success' | 'info' | 'warning';

interface AlertProps {
  kind?: Kind;
  title?: string;
  children?: ReactNode;
  onRetry?: () => void;
}

export function Alert({ kind = 'info', title, children, onRetry }: AlertProps) {
  if (!children && !title) return null;
  return (
    <div className={`alert alert--${kind}`} role={kind === 'error' ? 'alert' : 'status'} data-testid={`alert-${kind}`}>
      <div className="alert__body">
        {title && <strong className="alert__title">{title}</strong>}
        {children && <div>{children}</div>}
      </div>
      {onRetry && (
        <button type="button" className="btn btn--ghost btn--sm" onClick={onRetry}>
          Reintentar
        </button>
      )}
    </div>
  );
}
