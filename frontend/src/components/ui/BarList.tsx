interface BarItem {
  key: string;
  label: string;
  value: number;
  /** Texto a la derecha; si no se da, se muestra el valor. */
  display?: string;
}

/** Gráfico de barras horizontal sencillo, sin librerías externas. */
export function BarList({
  items,
  max,
  emptyText = 'Sin datos en el periodo.',
}: {
  items: BarItem[];
  max?: number;
  emptyText?: string;
}) {
  if (items.length === 0) return <p className="muted">{emptyText}</p>;
  const top = max ?? Math.max(...items.map((i) => i.value), 1);
  return (
    <ul className="bar-list">
      {items.map((item) => (
        <li key={item.key} className="bar-list__row">
          <span className="bar-list__label" title={item.label}>
            {item.label}
          </span>
          <span className="bar-list__track">
            <span
              className="bar-list__fill"
              style={{ width: `${top > 0 ? Math.min((item.value / top) * 100, 100) : 0}%` }}
            />
          </span>
          <span className="bar-list__value">{item.display ?? item.value}</span>
        </li>
      ))}
    </ul>
  );
}

interface StackedItem {
  key: string;
  label: string;
  a: number;
  b: number;
}

/** Barras apiladas de dos series (p. ej. cancelaciones del cliente frente a las del proveedor). */
export function StackedBarList({ items, labelA, labelB }: { items: StackedItem[]; labelA: string; labelB: string }) {
  if (items.length === 0) return <p className="muted">Sin datos en el periodo.</p>;
  const top = Math.max(...items.map((i) => i.a + i.b), 1);
  return (
    <>
      <div className="legend">
        <span className="legend__item">
          <span className="legend__swatch legend__swatch--a" />
          {labelA}
        </span>
        <span className="legend__item">
          <span className="legend__swatch legend__swatch--b" />
          {labelB}
        </span>
      </div>
      <ul className="bar-list">
        {items.map((item) => (
          <li key={item.key} className="bar-list__row">
            <span className="bar-list__label" title={item.label}>
              {item.label}
            </span>
            <span className="bar-list__track">
              <span className="bar-list__fill bar-list__fill--a" style={{ width: `${(item.a / top) * 100}%` }} />
              <span className="bar-list__fill bar-list__fill--b" style={{ width: `${(item.b / top) * 100}%` }} />
            </span>
            <span className="bar-list__value">
              {item.a} / {item.b}
            </span>
          </li>
        ))}
      </ul>
    </>
  );
}
