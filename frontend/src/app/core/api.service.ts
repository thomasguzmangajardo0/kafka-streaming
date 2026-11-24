import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CrearPedidoRequest,
  Despacho,
  Notificacion,
  Pedido,
  RegistroAuditoria,
  StockPorSku,
} from './models';

/**
 * Todas las llamadas pasan por el API Manager (Amazon API Gateway). El interceptor de
 * autenticación agrega el JWT (Authorization: Bearer ...) porque la URL base está en secureRoutes.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl.replace(/\/+$/, '');

  listarPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.base}/api/pedidos`);
  }

  crearPedido(request: CrearPedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.base}/api/pedidos`, request);
  }

  stock(): Observable<StockPorSku> {
    return this.http.get<StockPorSku>(`${this.base}/api/inventario`);
  }

  despachos(): Observable<Despacho[]> {
    return this.http.get<Despacho[]>(`${this.base}/api/inventario/despachos`);
  }

  notificaciones(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.base}/api/notificaciones`);
  }

  auditoria(): Observable<RegistroAuditoria[]> {
    return this.http.get<RegistroAuditoria[]>(`${this.base}/api/notificaciones/auditoria`);
  }
}
