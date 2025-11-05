package optimizacion;

import model.*;
import validacion.ValidadorAsignacion;
import java.util.*;

public final class BBPortafolio {

    public static final class Resultado {
        public final Asignacion mejor;
        public final double retorno;
        public final double riesgo;
        public final int nodosVisitados;
        public Resultado(Asignacion a, double r, double s, int nv){
            this.mejor=a; this.retorno=r; this.riesgo=s; this.nodosVisitados=nv;
        }
    }

    private BBPortafolio(){}

    public static Resultado maximizarRetorno(Mercado m, Perfil p){
        final int n = m.activos.size();

        // Orden de exploración por score (ret/sigma) desc
        List<Integer> orden = new ArrayList<>();
        for (int i=0;i<n;i++) orden.add(i);
        orden.sort((i,j)->{
            Activo ai = m.activos.get(i);
            Activo aj = m.activos.get(j);
            double si = ai.sigma>1e-12 ? ai.retorno/ai.sigma : ai.retorno;
            double sj = aj.sigma>1e-12 ? aj.retorno/aj.sigma : aj.retorno;
            return Double.compare(sj, si);
        });

        // Cota inferior inicial con Greedy
        Asignacion best = heuristicas.GreedyInicial.construir(m, p);
        double bestRet  = CalculadoraRetorno.retornoCartera(m, best, p.presupuesto);
        double bestRisk = CalculadoraRiesgo.riesgoCartera(m, best, p.presupuesto);

        // Estado mutable para backtracking
        LinkedHashMap<String,Double> asig = new LinkedHashMap<>();
        Map<String,Double> usoTipo   = new HashMap<>();
        Map<String,Double> usoSector = new HashMap<>();

        int[] nodos = new int[]{0};
        backtrack(0, m, p, orden, asig, p.presupuesto, usoTipo, usoSector,
                  new double[]{bestRet}, new Asignacion[]{best}, new double[]{bestRisk}, nodos);

        return new Resultado(best, bestRet, bestRisk, nodos[0]);
    }

    private static void backtrack(int k, Mercado m, Perfil p, List<Integer> ord,
                                  LinkedHashMap<String,Double> asig, double presupuestoRest,
                                  Map<String,Double> usoTipo, Map<String,Double> usoSector,
                                  double[] bestRet, Asignacion[] best, double[] bestRisk, int[] nodos) {
        nodos[0]++;

        // Hoja o sin presupuesto útil
        if (k == ord.size() || presupuestoRest < 1e-6) {
            evaluarYActualizar(m, p, asig, bestRet, best, bestRisk);
            return;
        }

        // Poda por cota superior (bound optimista)
        double ub = boundOptimista(m, p, ord, k, asig, usoTipo, usoSector, presupuestoRest);
        if (ub <= bestRet[0] + 1e-12) return;

        // Nodo actual
        Activo act = m.activos.get(ord.get(k));
        double topeActivoAbs = p.maxPorActivo * p.presupuesto;
        double unit = act.montoMin;

        // qmax según presupuesto y tope por activo
        int qmax = (int)Math.floor(Math.min(presupuestoRest, topeActivoAbs) / unit);

        // Exploramos q = qmax..0 (primero más monto ⇒ mejor cota inferior temprano)
        for (int q = qmax; q >= 0; q--) {
            double delta = q * unit;  // monto a sumar para este activo

            if (delta < 1e-9) {
                // Rama q=0: NO TOMAR este activo
                backtrack(k+1, m, p, ord, asig, presupuestoRest, usoTipo, usoSector, bestRet, best, bestRisk, nodos);
                continue;
            }

            // Chequeo de límites por tipo/sector con delta
            double nuevoTipo   = usoTipo.getOrDefault(act.tipo, 0.0) + delta;
            double nuevoSector = usoSector.getOrDefault(act.sector, 0.0) + delta;
            double limTipo     = p.maxPorTipo.getOrDefault(act.tipo, 1.0)*p.presupuesto;
            double limSector   = p.maxPorSector.getOrDefault(act.sector,1.0)*p.presupuesto;
            if (nuevoTipo > limTipo + 1e-9 || nuevoSector > limSector + 1e-9) continue;

            // Aplicar delta
            asig.put(act.ticker, asig.getOrDefault(act.ticker, 0.0) + delta);
            usoTipo.put(act.tipo, nuevoTipo);
            usoSector.put(act.sector, nuevoSector);

            // Poda por riesgo (tentativa)
            Asignacion parcial = new Asignacion(asig);
            double risk = CalculadoraRiesgo.riesgoCartera(m, parcial, p.presupuesto);
            if (risk <= p.riesgoMax + 1e-9) {
                backtrack(k+1, m, p, ord, asig, presupuestoRest - delta,
                          usoTipo, usoSector, bestRet, best, bestRisk, nodos);
            }

            // Deshacer
            double prev = asig.get(act.ticker) - delta;
            if (prev <= 1e-12) asig.remove(act.ticker); else asig.put(act.ticker, prev);
            usoTipo.put(act.tipo, nuevoTipo - delta);
            usoSector.put(act.sector, nuevoSector - delta);
        }
    }

    // Cota superior: retorno parcial + fraccional optimista con presupuesto y límites (sin riesgo, rápido)
    private static double boundOptimista(Mercado m, Perfil p, List<Integer> ord, int k,
                                         Map<String,Double> asig,
                                         Map<String,Double> usoTipo, Map<String,Double> usoSector,
                                         double presupuestoRest) {
        double retParcial = 0.0;
        for (Map.Entry<String,Double> e : asig.entrySet()) {
            int idx = m.indexOf(e.getKey());
            double w = e.getValue() / p.presupuesto;
            retParcial += w * m.activos.get(idx).retorno;
        }

        double retExtra = 0.0;
        double resto = presupuestoRest;

        for (int i=k;i<ord.size() && resto>1e-9;i++){
            Activo a = m.activos.get(ord.get(i));
            double limTipoRest   = p.maxPorTipo.getOrDefault(a.tipo, 1.0)*p.presupuesto - usoTipo.getOrDefault(a.tipo, 0.0);
            double limSectorRest = p.maxPorSector.getOrDefault(a.sector,1.0)*p.presupuesto - usoSector.getOrDefault(a.sector,0.0);
            double topeActivo    = p.maxPorActivo * p.presupuesto;

            double cap = Math.max(0.0, Math.min(Math.min(resto, topeActivo), Math.min(limTipoRest, limSectorRest)));
            if (cap <= 1e-12) continue;

            double w = cap / p.presupuesto; // fraccional optimista
            retExtra += w * a.retorno;
            resto -= cap;
        }
        return retParcial + retExtra;
    }

    private static void evaluarYActualizar(Mercado m, Perfil p,
                                           Map<String,Double> asig,
                                           double[] bestRet, Asignacion[] best, double[] bestRisk){
        Asignacion a = new Asignacion(asig);
        try {
            ValidadorAsignacion.validar(m, p, a);
            double r = CalculadoraRetorno.retornoCartera(m, a, p.presupuesto);

            boolean mejor = false;
            if (r > bestRet[0] + 1e-12) {
                mejor = true;
            } else if (Math.abs(r - bestRet[0]) <= 1e-12) {
                // Empate en retorno: desempatar por menor correlación media
                double corrA = correlacionMedia(m, a);
                double corrBest = (best[0] == null) ? Double.POSITIVE_INFINITY : correlacionMedia(m, best[0]);
                if (corrA < corrBest - 1e-12) mejor = true;
            }

            if (mejor) {
                bestRet[0]  = r;
                best[0]     = a;
                bestRisk[0] = CalculadoraRiesgo.riesgoCartera(m, a, p.presupuesto);
            }
        } catch (IllegalArgumentException ignore) {
            // nodo no factible
        }
    }

    // --- helper local: correlación media de la cartera (solo pares seleccionados) ---
    private static double correlacionMedia(Mercado m, Asignacion a){
        List<Integer> idx = new ArrayList<>();
        for (String t : a.getMontos().keySet()){
            if (a.getMonto(t) > 0.0) idx.add(m.indexOf(t));
        }
        if (idx.size() < 2) return 0.0;
        double sum=0.0; int cnt=0;
        for (int i=0;i<idx.size();i++){
            for (int j=i+1;j<idx.size();j++){
                sum += m.rho[idx.get(i)][idx.get(j)];
                cnt++;
            }
        }
        return cnt==0?0.0:sum/cnt;
    }
}

