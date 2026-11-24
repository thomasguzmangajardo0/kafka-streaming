package cl.duoc.streaming.inventario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** Stock inicial por SKU (prefijo {@code app.inventario}). */
@ConfigurationProperties(prefix = "app.inventario")
public record InventarioProperties(Map<String, Integer> stockInicial) {

    public InventarioProperties {
        stockInicial = stockInicial == null ? Map.of() : Map.copyOf(stockInicial);
    }
}
