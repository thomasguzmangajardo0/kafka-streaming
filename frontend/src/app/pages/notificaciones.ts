import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/api.service';
import { Notificacion, RegistroAuditoria } from '../core/models';
import { sondear } from '../core/polling';

@Component({
  selector: 'app-notificaciones',
  imports: [DatePipe],
  template: `
    <h1>Notificaciones</h1>
    <p class="sub">Correos simulados (Kafka y RabbitMQ) y registro de auditoría recibido por la cola <span class="mono">cola.auditoria</span>.</p>
    @if (error(); as e) { <p class="aviso error" role="alert">{{ e }}</p> }

    <section class="card">
      <div class="cabecera"><h2>Correos generados</h2><span class="en-vivo">en vivo</span></div>
      @if (notificaciones().length === 0) {
        <p class="vacio">Aún no hay notificaciones.</p>
      } @else {
        <div class="tabla-scroll">
          <table>
            <thead><tr><th>Tipo</th><th>Asunto</th><th>Destinatario</th><th>Hora</th></tr></thead>
            <tbody>
              @for (n of notificaciones(); track n.id) {
                <tr>
                  <td class="mono">{{ n.tipo }}</td>
                  <td>{{ n.asunto }}<div class="detalle">{{ n.mensaje }}</div></td>
                  <td>{{ n.destinatario }}</td>
                  <td>{{ n.creadaEn | date: 'HH:mm:ss' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    <section class="card">
      <h2>Auditoría (RabbitMQ)</h2>
      @if (auditoria().length === 0) {
        <p class="vacio">Sin registros todavía.</p>
      } @else {
        <div class="tabla-scroll">
          <table>
            <thead><tr><th>Pedido</th><th>Estado</th><th>Routing key</th><th>Hora</th></tr></thead>
            <tbody>
              @for (a of auditoria(); track a.mensajeId + a.routingKey) {
                <tr>
                  <td class="mono">{{ a.pedidoId.slice(0, 8) }}</td>
                  <td><span class="chip" [class]="'chip ' + a.estado">{{ a.estado }}</span></td>
                  <td class="mono">{{ a.routingKey }}</td>
                  <td>{{ a.registradoEn | date: 'HH:mm:ss' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    .cabecera { display: flex; justify-content: space-between; align-items: baseline; }
    .detalle { color: var(--muted); font-size: 0.82rem; }
  `,
})
export class Notificaciones {
  private readonly api = inject(ApiService);
  protected readonly notificaciones = signal<Notificacion[]>([]);
  protected readonly auditoria = signal<RegistroAuditoria[]>([]);
  protected readonly error = signal<string | null>(null);

  constructor() {
    sondear(() => this.api.notificaciones(), (m) => this.error.set(m))
      .pipe(takeUntilDestroyed())
      .subscribe((lista) => {
        if (lista) {
          this.error.set(null);
          this.notificaciones.set(lista);
        }
      });
    sondear(() => this.api.auditoria(), (m) => this.error.set(m))
      .pipe(takeUntilDestroyed())
      .subscribe((lista) => lista && this.auditoria.set(lista));
  }
}
