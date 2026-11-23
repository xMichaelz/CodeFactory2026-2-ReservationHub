export function Brand({ light = false }: { light?: boolean }) {
  return (
    <span className={`brand ${light ? 'brand--light' : ''}`}>
      <img src="/favicon.svg" alt="" className="brand__logo" />
      <span className="brand__name">
        Reserva<strong>Hub</strong>
      </span>
    </span>
  );
}
