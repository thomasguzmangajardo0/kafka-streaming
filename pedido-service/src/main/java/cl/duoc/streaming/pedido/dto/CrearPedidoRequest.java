package cl.duoc.streaming.pedido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Cuerpo del POST /api/pedidos. */
public record CrearPedidoRequest(
        @NotBlank String clienteId,
        @NotBlank @Email String clienteEmail,
        @NotEmpty @Valid List<ItemRequest> items) {

    public record ItemRequest(
            @NotBlank String sku,
            @Min(1) int cantidad,
            @NotNull @DecimalMin("0.0") BigDecimal precioUnitario) {
    }
}
