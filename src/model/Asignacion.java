package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Asignacion {
    // Monto invertido por ticker (en la misma moneda que el presupuesto)
    private final Map<String, Double> montoPorTicker;

    public Asignacion(Map<String, Double> montoPorTicker) {
        // Normalizamos: sin nulls, sin negativos, en el mismo orden de inserción
        LinkedHashMap<String, Double> tmp = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : montoPorTicker.entrySet()) {
            String t = e.getKey();
            double v = (e.getValue() == null ? 0.0 : e.getValue());
            if (v < 0) throw new IllegalArgumentException("Monto negativo en " + t);
            tmp.put(t, v);
        }
        this.montoPorTicker = Collections.unmodifiableMap(tmp);
    }

    /** Mapa inmutable ticker -> monto */
    public Map<String, Double> getMontos() {
        return this.montoPorTicker;
    }

    /** Monto asignado a un ticker (0.0 si no existe) */
    public double getMonto(String ticker) {
        Double v = this.montoPorTicker.get(ticker);
        return (v == null ? 0.0 : v);
    }
        /** Compatibilidad con código existente: devuelve el monto por ticker */
    public double monto(String ticker) {
        return getMonto(ticker);
    }

    /** Total invertido (suma de todos los montos) */
    public double totalInvertido() {
        double sum = 0.0;
        for (Double v : this.montoPorTicker.values()) {
            if (v != null) sum += v;
        }
        return sum;
    }

    /** Conjunto de tickers presentes en la asignación */
    public Set<String> tickers() {
        return this.montoPorTicker.keySet();
    }
}
