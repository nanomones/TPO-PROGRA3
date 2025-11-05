package model;

public final class CalculadoraRetorno {
    private CalculadoraRetorno(){}

    // Retorno esperado de la cartera en decimales (ej: 0.12 = 12%)
    // w_i = monto_i / presupuesto
    public static double retornoCartera(Mercado m, Asignacion a, double presupuesto){
        int n = m.activos.size();
        double suma = 0.0;
        for (int i = 0; i < n; i++){
            var act = m.activos.get(i);
            double w = a.monto(act.ticker) / presupuesto; // peso del activo
            suma += w * act.retorno;
        }
        return suma;
    }
}
