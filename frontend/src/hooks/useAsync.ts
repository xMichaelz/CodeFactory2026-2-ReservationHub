import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react';
import { describeError } from '../api/client';

interface AsyncState<T> {
  data: T | undefined;
  error: string | null;
  loading: boolean;
  reload: () => Promise<void>;
  setData: (data: T | undefined) => void;
}

/**
 * Ejecuta una carga asíncrona y expone datos, error y estado de carga.
 * Con `enabled = false` no se ejecuta (útil para consultas que esperan un formulario).
 */
export function useAsync<T>(loader: () => Promise<T>, deps: DependencyList, enabled = true): AsyncState<T> {
  const [data, setData] = useState<T | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(enabled);
  const requestId = useRef(0);

  const run = useCallback(loader, deps);

  const reload = useCallback(async () => {
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    try {
      const result = await run();
      if (id === requestId.current) setData(result);
    } catch (err) {
      if (id === requestId.current) {
        setError(describeError(err));
        setData(undefined);
      }
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, [run]);

  useEffect(() => {
    if (enabled) void reload();
    else setLoading(false);
  }, [reload, enabled]);

  return { data, error, loading, reload, setData };
}

/** Envía un formulario/acción: maneja el estado "enviando" y el mensaje de error. */
export function useSubmit<A extends unknown[], R>(action: (...args: A) => Promise<R>) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = useCallback(
    async (...args: A): Promise<R | undefined> => {
      setSubmitting(true);
      setError(null);
      try {
        return await action(...args);
      } catch (err) {
        setError(describeError(err));
        return undefined;
      } finally {
        setSubmitting(false);
      }
    },
    [action],
  );

  return { submit, submitting, error, setError };
}
