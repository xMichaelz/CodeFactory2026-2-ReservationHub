import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: string;
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}

export function EmptyState({ icon = '📭', title, children, action }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <div className="empty-state__icon" aria-hidden="true">
        {icon}
      </div>
      <h3 className="empty-state__title">{title}</h3>
      {children && <p className="empty-state__text">{children}</p>}
      {action && <div className="empty-state__action">{action}</div>}
    </div>
  );
}
