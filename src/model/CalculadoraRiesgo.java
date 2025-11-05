package model;

public final class CalculadoraRiesgo {
    private CalculadoraRiesgo(){}

    public static double riesgoCartera(Mercado m, Asignacion a, double presupuesto) {
        int n = m.activos.size();
        double[] w = new double[n];
        for (int i = 0; i < n; i++) {
            var t = m.activos.get(i).ticker;
            w[i] = a.monto(t) / presupuesto;
        }
        // w^T * (Dσ * ρ * Dσ) * w
        // Primero v = (Dσ * w)
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = m.activos.get(i).sigma * w[i];
        }
        // Luego u = ρ * v
        double[] u = new double[n];
        for (int i = 0; i < n; i++) {
            double acc = 0.0;
            for (int j = 0; j < n; j++) {
                acc += m.rho[i][j] * v[j];
            }
            u[i] = acc;
        }
        // Finalmente v^T * u
        double quad = 0.0;
        for (int i = 0; i < n; i++) quad += v[i] * u[i];
        return Math.sqrt(Math.max(0.0, quad));
    }
}
