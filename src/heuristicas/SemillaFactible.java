package heuristicas;

import model.*;
import java.util.*;

public final class SemillaFactible {
    private SemillaFactible(){}

    /**
     * Construye una semilla factible con 3..6 activos, 1× montoMin cada uno,
     * eligiendo por score (retorno/sigma) descendente y respetando
     * presupuesto, tope por activo, y topes por tipo/sector.
     */
    public static Asignacion construir(Mercado m, Perfil p){
        int n = m.activos.size();

        // orden por score retorno/sigma (si sigma=0, usa retorno)
        List<Integer> ord = new ArrayList<>();
        for (int i=0;i<n;i++) ord.add(i);
        ord.sort((i,j)->{
            Activo ai=m.activos.get(i), aj=m.activos.get(j);
            double si = ai.sigma>1e-12? ai.retorno/ai.sigma : ai.retorno;
            double sj = aj.sigma>1e-12? aj.retorno/aj.sigma : aj.retorno;
            return Double.compare(sj, si);
        });

        LinkedHashMap<String,Double> asig = new LinkedHashMap<>();
        Map<String,Double> usoTipo   = new HashMap<>();
        Map<String,Double> usoSector = new HashMap<>();

        double presupuestoRest = p.presupuesto;
        int distintos = 0;
        double topePorActivoAbs = p.maxPorActivo * p.presupuesto;

        // 1) Agregar hasta 6 distintos (1× montoMin)
        for (int k=0; k<ord.size() && distintos < 6; k++){
            Activo a = m.activos.get(ord.get(k));
            double unit = a.montoMin;

            if (unit > presupuestoRest + 1e-9) continue;
            if (unit > topePorActivoAbs + 1e-9) continue;

            double nuevoTipo   = usoTipo.getOrDefault(a.tipo,0.0) + unit;
            double nuevoSector = usoSector.getOrDefault(a.sector,0.0) + unit;
            double limTipo     = p.maxPorTipo.getOrDefault(a.tipo,1.0)*p.presupuesto;
            double limSector   = p.maxPorSector.getOrDefault(a.sector,1.0)*p.presupuesto;
            if (nuevoTipo - limTipo > 1e-9 || nuevoSector - limSector > 1e-9) continue;

            // aplicar
            asig.put(a.ticker, unit);
            usoTipo.put(a.tipo, nuevoTipo);
            usoSector.put(a.sector, nuevoSector);
            presupuestoRest -= unit;
            distintos++;
        }

        // asegurar mínimo 3
        if (distintos < 3) {
            throw new IllegalArgumentException("No se pudo construir semilla con al menos 3 activos dentro de las restricciones.");
        }

        // validar riesgo (si se pasa, intentá quitar el último agregado hasta quedar factible)
        Asignacion seed = new Asignacion(asig);
        double sigma = CalculadoraRiesgo.riesgoCartera(m, seed, p.presupuesto);
        while (sigma - p.riesgoMax > 1e-9 && distintos > 3) {
            // quitar el último
            String lastTicker = null;
            for (String t : asig.keySet()) lastTicker = t; // último por orden de inserción
            if (lastTicker == null) break;
            int idx = m.indexOf(lastTicker);
            Activo a = m.activos.get(idx);
            double unit = asig.remove(lastTicker);
            presupuestoRest += unit;
            usoTipo.put(a.tipo, usoTipo.get(a.tipo) - unit);
            usoSector.put(a.sector, usoSector.get(a.sector) - unit);
            distintos--;
            seed = new Asignacion(asig);
            sigma = CalculadoraRiesgo.riesgoCartera(m, seed, p.presupuesto);
        }

        // revalida: si aún excede riesgo, no hay semilla factible que cumpla 3 mínimos
        if (sigma - p.riesgoMax > 1e-9) {
            throw new IllegalArgumentException("No se pudo construir semilla factible dentro del riesgo máximo.");
        }

        return seed;
    }
}
