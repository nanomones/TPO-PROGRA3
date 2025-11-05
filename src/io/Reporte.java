package io;

import model.*;

public final class Reporte {
    private Reporte(){}
    
private static double correlacionMedia(Mercado m, Asignacion a){
    // índices de los activos seleccionados
    java.util.List<Integer> idx = new java.util.ArrayList<>();
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
   public static void imprimirResumen(Mercado m, Perfil p, Asignacion a){
    System.out.println("Perfil: " + p.tipoPerfil);
System.out.printf(java.util.Locale.US,
    "Retorno mínimo requerido: %.3f%%%n",
    Math.max(p.retornoMin, p.retornoMinDeseado));
        double ret = CalculadoraRetorno.retornoCartera(m, a, p.presupuesto);
        double sig = CalculadoraRiesgo.riesgoCartera(m, a, p.presupuesto);

        System.out.println("=== CARTERA ===");
        System.out.printf(java.util.Locale.US, "Presupuesto: %.2f%n", p.presupuesto);
        System.out.printf(java.util.Locale.US, "Invertido:   %.2f%n", a.totalInvertido());
        System.out.printf(java.util.Locale.US, "Retorno esp: %.3f%n", ret);
        System.out.printf(java.util.Locale.US, "Riesgo (sigma): %.3f (max %.3f)%n", sig, p.riesgoMax);
        System.out.printf(java.util.Locale.US, "Correlación media: %.3f%n%n", correlacionMedia(m, a));

        System.out.println("\nDetalle (ticker, monto, % del presupuesto):");
        for (var act : m.activos){
            double monto = a.monto(act.ticker);
            if (monto <= 0) continue;
            double w = monto / p.presupuesto * 100.0;
            System.out.printf(java.util.Locale.US, " - %s: %.2f  (%.2f%%)%n", act.ticker, monto, w);
        }
    }
}
