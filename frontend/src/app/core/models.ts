export type EstadoPedido = 'PENDIENTE' | 'CONFIRMADO' | 'RECHAZADO';

export interface ItemPedido {
  sku: string;
  cantidad: number;
  precioUnitario: number;
}

export interface Pedido {
  id: string;
  clienteId: string;
  clienteEmail: string;
  items: ItemPedido[];
  total: number;
  estado: EstadoPedido;
  motivo: string | null;
  creadoEn: string;
  actualizadoEn: string;
}

export interface CrearPedidoRequest {
  clienteId: string;
  clienteEmail: string;
  items: ItemPedido[];
}

export interface Notificacion {
  id: string;
  pedidoId: string;
  destinatario: string;
  tipo: string;
  asunto: string;
  mensaje: string;
  creadaEn: string;
}

export interface RegistroAuditoria {
  mensajeId: string;
  pedidoId: string;
  estado: string;
  routingKey: string;
  registradoEn: string;
}

export interface Despacho {
  pedidoId: string;
  clienteEmail: string;
  programadoEn: string;
}

export type StockPorSku = Record<string, number>;
