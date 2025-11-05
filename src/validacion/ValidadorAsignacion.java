package validacion;

import model.*;
import java.util.*;

public final class ValidadorAsignacion {
    private ValidadorAsignacion(){}

    public static void validar(Mercado m, Perfil p, Asignacion a){
        Objects.requireNonNull(m); Objects.requireNonNull(p); Objects.requireNonNull(a);

        // --- sumas básicas
        double total = 0.0;
        for (double v : a.getMontos().values()) total += v;

        if (total - p.presupuesto > 1e-9) {
            throw new IllegalArgumentException(String.format("Excede presupuesto: %.2f > %.2f", total, p.presupuesto));
        }

        // --- por activo: montoMin y tope por activo
        double topePorActivoAbs = p.maxPorActivo * p.presupuesto;
        for (Map.Entry<String,Double> e : a.getMontos().entrySet()) {
            String tkr = e.getKey();
            double monto = e.getValue();
            if (monto <= 0) continue;

            int idx = m.indexOf(tkr);
            if (idx < 0) throw new IllegalArgumentException("Ticker inexistente: " + tkr);
            Activo act = m.activos.get(idx);

            if (monto + 1e-9 < act.montoMin) {
                throw new IllegalArgumentException(String.format("Monto < montoMin en %s: %.2f < %.2f", tkr, monto, act.montoMin));
            }
            if (monto - topePorActivoAbs > 1e-9) {
                throw new IllegalArgumentException(String.format("Excede tope por activo en %s: %.2f > %.2f", tkr, monto, topePorActivoAbs));
            }
        }

        // --- límites por tipo/sector
        Map<String,Double> usoTipo = new HashMap<>();
        Map<String,Double> usoSector = new HashMap<>();
        for (Map.Entry<String,Double> e : a.getMontos().entrySet()) {
            String tkr = e.getKey();
            double monto = e.getValue();
            if (monto <= 0) continue;

            int idx = m.indexOf(tkr);
            Activo act = m.activos.get(idx);

            double nuevoTipo   = usoTipo.getOrDefault(act.tipo, 0.0) + monto;
            double nuevoSector = usoSector.getOrDefault(act.sector, 0.0) + monto;

            double limTipo   = p.maxPorTipo.getOrDefault(act.tipo,   1.0) * p.presupuesto;
            double limSector = p.maxPorSector.getOrDefault(act.sector,1.0) * p.presupuesto;

            if (nuevoTipo - limTipo > 1e-9) {
                throw new IllegalArgumentException(String.format("Excede tope por tipo (%s): %.2f > %.2f",
                        act.tipo, nuevoTipo, limTipo));
            }
            if (nuevoSector - limSector > 1e-9) {
                throw new IllegalArgumentException(String.format("Excede tope por sector (%s): %.2f > %.2f",
                        act.sector, nuevoSector, limSector));
            }

            usoTipo.put(act.tipo, nuevoTipo);
            usoSector.put(act.sector, nuevoSector);
        }

        // --- riesgo
        double sigma = CalculadoraRiesgo.riesgoCartera(m, a, p.presupuesto);
        if (sigma - p.riesgoMax > 1e-9) {
            throw new IllegalArgumentException(String.format("Riesgo excedido: sigma %.3f > max %.3f", sigma, p.riesgoMax));
        }

        // === NUEVAS VALIDACIONES (TPO) ===

        // 1) cantidad de activos entre 3 y 6
        int cantidadActivos = 0;
        for (double monto : a.getMontos().values()) {
            if (monto > 0.0) cantidadActivos++;
        }
        if (cantidadActivos < 3 || cantidadActivos > 6) {
            throw new IllegalArgumentException("Cantidad de activos debe estar entre 3 y 6 (actual: " + cantidadActivos + ")");
        }

        // 2) retorno mínimo exigido (perfil y cliente)
        double retornoCartera = CalculadoraRetorno.retornoCartera(m, a, p.presupuesto);
        double retornoRequerido = Math.max(p.retornoMin, p.retornoMinDeseado);
        if (retornoCartera + 1e-12 < retornoRequerido) {
            throw new IllegalArgumentException(
                String.format("Retorno insuficiente: %.3f < mínimo requerido %.3f", retornoCartera, retornoRequerido)
            );
        }
    }
}
