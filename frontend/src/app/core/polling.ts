import { Observable, catchError, merge, of, switchMap, timer } from 'rxjs';

/**
 * Consulta el backend cada `intervaloMs` (y cuando `ahora$` emite) para mostrar cambios casi en tiempo real:
 * el estado de los pedidos cambia solo cuando llegan los eventos de Kafka / mensajes de RabbitMQ.
 * Un error de red no corta la consulta periódica: se informa por `alError` y se reintenta en el siguiente ciclo.
 */
export function sondear<T>(
  fuente: () => Observable<T>,
  alError: (mensaje: string) => void,
  intervaloMs = 3000,
  ahora$?: Observable<unknown>,
): Observable<T> {
  const disparadores = ahora$ ? merge(timer(0, intervaloMs), ahora$) : timer(0, intervaloMs);
  return disparadores.pipe(
    switchMap(() =>
      fuente().pipe(
        catchError((error: { status?: number }) => {
          alError(
            error?.status === 0
              ? 'Sin conexión con el API. Reintentando…'
              : `El API respondió con error ${error?.status ?? ''}. Reintentando…`,
          );
          return of(null as unknown as T);
        }),
      ),
    ),
  );
}

export const clp = (valor: number): string =>
  new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(valor);
