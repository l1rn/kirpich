import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from './api';

/**
 * Загружает данные и перезапрашивает их каждые intervalMs (0 — без опроса).
 * Так страница заявки сама показывает, как заявка идёт по конвейеру.
 */
export function usePolling<T>(load: () => Promise<T>, intervalMs: (data: T | null) => number) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const loadRef = useRef(load);
  loadRef.current = load;

  const refresh = useCallback(async () => {
    try {
      const next = await loadRef.current();
      setData(next);
      setError(null);
      return next;
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Не удалось загрузить данные');
      return null;
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const delay = intervalMs(data);
  useEffect(() => {
    if (!delay) return;
    const timer = window.setInterval(() => void refresh(), delay);
    return () => window.clearInterval(timer);
  }, [delay, refresh]);

  return { data, error, refresh, setData };
}
