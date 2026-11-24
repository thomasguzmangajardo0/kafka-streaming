import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ItemPedido, Pedido } from '../core/models';
import { clp, sondear } from '../core/polling';

@Component({
  selector: 'app-pedidos',
  imports: [DatePipe],
  template: `
    <h1>Pedidos</h1>
    <p class="sub">
      Al crear un pedido se publica un evento en Kafka; inventario lo procesa y el estado cambia solo.
    </p>

    <div class="grid dos-col">
      <section class="card">
        <h2>Nuevo pedido</h2>
        <form (submit)="crear($event)">
          <label for="email">Correo del cliente</label>
          <input id="email" type="email" [value]="emailEfectivo()" (input)="emailManual.set(valor($event))" required />

          <h2 class="items-titulo">Productos</h2>
          @for (item of items(); track $index) {
            <div class="fila-item">
              <div>
                <label [for]="'sku' + $index">SKU</label>
                <select [id]="'sku' + $index" [value]="item.sku" (change)="cambiar($index, 'sku', valor($event))">
                  @for (sku of skus(); track sku) {
                    <option [value]="sku" [selected]="sku === item.sku">{{ sku }}</option>
                  }
                </select>
              </div>
              <div>
                <label [for]="'cant' + $index">Cantidad</label>
                <input [id]="'cant' + $index" type="number" min="1" [value]="item.cantidad"
                       (input)="cambiar($index, 'cantidad', valor($event))" />
              </div>
              <div>
                <label [for]="'precio' + $index">Precio unitario</label>
                <input [id]="'precio' + $index" type="number" min="0" [value]="item.precioUnitario"
                       (input)="cambiar($index, 'precioUnitario', valor($event))" />
              </div>
              <button type="button" class="secundario quitar" (click)="quitar($index)"
                      [disabled]="items().length === 1" aria-label="Quitar producto">✕</button>
            </div>
          }
          <div class="acciones">
            <button type="button" class="secundario" (click)="agregar()">+ Producto</button>
            <span class="total">Total {{ totalTexto() }}</span>
          </div>

          <button type="submit" class="enviar" [disabled]="enviando() || !formularioValido()">
            {{ enviando() ? 'Enviando…' : 'Crear pedido' }}
          </button>
          @if (mensaje(); as m) {
            <p class="aviso" [class.error]="m.tipo === 'error'" [class.ok]="m.tipo === 'ok'" role="status">{{ m.texto }}</p>
          }
        </form>
      </section>

      <section class="card">
        <div class="cabecera">
          <h2>Mis pedidos</h2>
          <span class="en-vivo">actualizando</span>
        </div>
        @if (errorListado(); as e) {
          <p class="aviso error" role="alert">{{ e }}</p>
        }
        @if (pedidos().length === 0) {
          <p class="vacio">Aún no hay pedidos. Crea el primero.</p>
        } @else {
          <div class="tabla-scroll">
            <table>
              <thead>
                <tr><th>Pedido</th><th>Estado</th><th class="num">Total</th><th>Hora</th></tr>
              </thead>
              <tbody>
                @for (p of pedidos(); track p.id) {
                  <tr>
                    <td><span class="mono">{{ p.id.slice(0, 8) }}</span></td>
                    <td>
                      <span class="chip" [class]="'chip ' + p.estado">{{ p.estado }}</span>
                      @if (p.motivo) { <div class="motivo">{{ p.motivo }}</div> }
                    </td>
                    <td class="num">{{ dinero(p.total) }}</td>
                    <td>{{ p.actualizadoEn | date: 'HH:mm:ss' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
  styles: `
    .items-titulo { margin-top: 18px; }
    .fila-item { display: grid; grid-template-columns: 1.3fr 0.8fr 1fr auto; gap: 10px; align-items: end; margin-bottom: 10px; }
    .quitar { padding: 9px 12px; }
    @media (max-width: 520px) {
      .fila-item { grid-template-columns: 1fr 1fr auto; }
      .fila-item > :first-child { grid-column: 1 / -1; }
    }
    .acciones { display: flex; justify-content: space-between; align-items: center; margin: 4px 0 18px; }
    .total { font-weight: 650; font-variant-numeric: tabular-nums; }
    .enviar { width: 100%; }
    .cabecera { display: flex; justify-content: space-between; align-items: baseline; }
    .motivo { color: var(--muted); font-size: 0.82rem; margin-top: 2px; }
  `,
})
export class Pedidos {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly refrescar$ = new Subject<void>();

  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly skus = signal<string[]>(['SKU-001', 'SKU-002', 'SKU-003', 'SKU-004']);
  protected readonly items = signal<ItemPedido[]>([{ sku: 'SKU-001', cantidad: 1, precioUnitario: 1500 }]);
  protected readonly emailManual = signal<string | null>(null);
  protected readonly emailEfectivo = computed(() => this.emailManual() ?? this.auth.email());
  protected readonly enviando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null);
  protected readonly errorListado = signal<string | null>(null);

  protected readonly total = computed(() =>
    this.items().reduce((suma, i) => suma + (Number(i.cantidad) || 0) * (Number(i.precioUnitario) || 0), 0),
  );
  protected readonly totalTexto = computed(() => clp(this.total()));
  protected readonly formularioValido = computed(
    () =>
      this.emailEfectivo().includes('@') &&
      this.items().every((i) => i.sku && Number(i.cantidad) >= 1 && Number(i.precioUnitario) >= 0),
  );

  constructor() {
    // El listado se refresca solo: el cambio de estado llega de forma asíncrona vía eventos
    sondear(() => this.api.listarPedidos(), (m) => this.errorListado.set(m), 3000, this.refrescar$)
      .pipe(takeUntilDestroyed())
      .subscribe((lista) => {
        if (lista) {
          this.errorListado.set(null);
          this.pedidos.set(lista);
        }
      });

    // Los SKU disponibles salen del inventario
    this.api
      .stock()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (stock) => {
          const lista = Object.keys(stock);
          if (lista.length) this.skus.set(lista);
        },
        error: () => undefined, // si falla se usan los SKU por defecto
      });
  }

  protected dinero = clp;

  protected valor(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLSelectElement).value;
  }

  protected cambiar(indice: number, campo: keyof ItemPedido, valor: string): void {
    this.items.update((lista) =>
      lista.map((item, i) =>
        i === indice ? { ...item, [campo]: campo === 'sku' ? valor : Number(valor) } : item,
      ),
    );
  }

  protected agregar(): void {
    this.items.update((lista) => [...lista, { sku: this.skus()[0] ?? 'SKU-001', cantidad: 1, precioUnitario: 1500 }]);
  }

  protected quitar(indice: number): void {
    this.items.update((lista) => lista.filter((_, i) => i !== indice));
  }

  protected crear(evento: Event): void {
    evento.preventDefault();
    if (!this.formularioValido() || this.enviando()) return;
    this.enviando.set(true);
    this.mensaje.set(null);
    this.api
      .crearPedido({
        clienteId: this.auth.clienteId() || 'cliente-demo',
        clienteEmail: this.emailEfectivo(),
        items: this.items(),
      })
      .subscribe({
        next: (pedido) => {
          this.enviando.set(false);
          this.mensaje.set({ tipo: 'ok', texto: `Pedido ${pedido.id.slice(0, 8)} recibido: se está validando el stock.` });
          this.refrescar$.next();
        },
        error: (e: { status?: number }) => {
          this.enviando.set(false);
          this.mensaje.set({
            tipo: 'error',
            texto:
              e.status === 400
                ? 'Datos inválidos: revisa correo, cantidades y precios.'
                : 'No se pudo crear el pedido. Intenta nuevamente.',
          });
        },
      });
  }
}
