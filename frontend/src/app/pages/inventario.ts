import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/api.service';
import { Despacho, StockPorSku } from '../core/models';
import { sondear } from '../core/polling';

@Component({
  selector: 'app-inventario',
  imports: [DatePipe],
  template: `
    <h1>Inventario</h1>
    <p class="sub">El stock baja al procesar los eventos de pedidos; los pedidos confirmados llegan a despacho por RabbitMQ.</p>
    @if (error(); as e) { <p class="aviso error" role="alert">{{ e }}</p> }

    <div class="grid dos-col">
      <section class="card">
        <div class="cabecera"><h2>Stock por SKU</h2><span class="en-vivo">en vivo</span></div>
        <table>
          <thead><tr><th>SKU</th><th class="num">Disponible</th></tr></thead>
          <tbody>
            @for (fila of filas(); track fila[0]) {
              <tr><td class="mono">{{ fila[0] }}</td><td class="num">{{ fila[1] }}</td></tr>
            } @empty {
              <tr><td colspan="2" class="vacio">Sin datos de stock.</td></tr>
            }
          </tbody>
        </table>
      </section>

      <section class="card">
        <h2>Despachos programados</h2>
        @if (despachos().length === 0) {
          <p class="vacio">Todavía no hay despachos.</p>
        } @else {
          <table>
            <thead><tr><th>Pedido</th><th>Cliente</th><th>Hora</th></tr></thead>
            <tbody>
              @for (d of despachos(); track d.pedidoId + d.programadoEn) {
                <tr>
                  <td class="mono">{{ d.pedidoId.slice(0, 8) }}</td>
                  <td>{{ d.clienteEmail }}</td>
                  <td>{{ d.programadoEn | date: 'HH:mm:ss' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>
    </div>
  `,
  styles: `.cabecera { display: flex; justify-content: space-between; align-items: baseline; }`,
})
export class Inventario {
  private readonly api = inject(ApiService);
  protected readonly filas = signal<[string, number][]>([]);
  protected readonly despachos = signal<Despacho[]>([]);
  protected readonly error = signal<string | null>(null);

  constructor() {
    sondear(() => this.api.stock(), (m) => this.error.set(m))
      .pipe(takeUntilDestroyed())
      .subscribe((stock: StockPorSku | null) => {
        if (stock) {
          this.error.set(null);
          this.filas.set(Object.entries(stock));
        }
      });
    sondear(() => this.api.despachos(), (m) => this.error.set(m))
      .pipe(takeUntilDestroyed())
      .subscribe((lista) => lista && this.despachos.set(lista));
  }
}
