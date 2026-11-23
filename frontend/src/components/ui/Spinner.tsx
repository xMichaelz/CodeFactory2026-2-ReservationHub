export function Spinner({ size = 'md', label }: { size?: 'sm' | 'md' | 'lg'; label?: string }) {
  return (
    <span className={`spinner-wrap spinner-wrap--${size}`} role="status">
      <span className={`spinner spinner--${size}`} aria-hidden="true" />
      {label ? <span className="spinner__label">{label}</span> : <span className="sr-only">Cargando…</span>}
    </span>
  );
}

export function PageLoader({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="page-loader">
      <Spinner size="lg" label={label} />
    </div>
  );
}
