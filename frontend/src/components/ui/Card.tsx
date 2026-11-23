import type { HTMLAttributes, ReactNode } from 'react';

interface CardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  title?: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}

export function Card({ title, subtitle, actions, children, className = '', ...rest }: CardProps) {
  return (
    <section className={`card ${className}`.trim()} {...rest}>
      {(title || actions) && (
        <header className="card__header">
          <div>
            {title && <h2 className="card__title">{title}</h2>}
            {subtitle && <p className="card__subtitle">{subtitle}</p>}
          </div>
          {actions && <div className="card__actions">{actions}</div>}
        </header>
      )}
      <div className="card__body">{children}</div>
    </section>
  );
}
