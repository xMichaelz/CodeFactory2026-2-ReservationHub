-- V6__reports.sql
-- Sprint 3 (HU-17 .. HU-21). El ciclo de vida del historial no se guarda en reservations:
-- se deriva del estado persistido y de la hora de la consulta, en la zona del negocio.
-- reservation_lifecycle replica ReservationLifecycle (Java). p_now es hora local del negocio, sin zona.

CREATE OR REPLACE FUNCTION reservation_lifecycle(
    p_status varchar,
    p_date date,
    p_start time,
    p_end time,
    p_now timestamp
) RETURNS varchar
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN p_status LIKE 'CANCELLED%' THEN 'CANCELLED'
        WHEN p_now >= (p_date + p_end) THEN 'COMPLETED'
        WHEN p_now < (p_date + p_start) THEN 'PENDING'
        ELSE 'CONFIRMED'
    END;
$$;

-- Una reserva cancelada no vuelve a confirmada. La reprogramación solo toca filas CONFIRMED.
CREATE OR REPLACE FUNCTION prevent_reopen_cancelled_reservation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status LIKE 'CANCELLED%' AND NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'A cancelled reservation cannot change status';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reservations_no_reopen
    BEFORE UPDATE ON reservations
    FOR EACH ROW
    EXECUTE FUNCTION prevent_reopen_cancelled_reservation();
