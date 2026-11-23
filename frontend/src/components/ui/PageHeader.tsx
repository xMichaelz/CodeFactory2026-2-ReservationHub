import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  tag?: string;
}

export function PageHeader({ title, description, actions, tag }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div>
        {tag && <span className="page-header__tag">{tag}</span>}
        <h1 className="page-header__title">{title}</h1>
        {description && <p className="page-header__description">{description}</p>}
      </div>
      {actions && <div className="page-header__actions">{actions}</div>}
    </div>
  );
}
