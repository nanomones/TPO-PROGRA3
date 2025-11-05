package validacion;

import model.*;

public final class ValidadorMercado {
    private static final double EPS = 1e-9;

    private ValidadorMercado(){}

    public static void validar(Mercado m){
        if (m == null) throw new IllegalArgumentException("Mercado nulo");
        int n = m.activos.size();
        if (n == 0) throw new IllegalArgumentException("No hay activos");
        if (m.rho == null) throw new IllegalArgumentException("Matriz de correlaciones nula");
        if (m.rho.length != n) throw new IllegalArgumentException("rho no es n x n (filas != n)");

        for (int i=0;i<n;i++){
            if (m.rho[i] == null || m.rho[i].length != n)
                throw new IllegalArgumentException("rho no es n x n (fila "+i+")");
        }

        // 1) Chequeos de matriz
        for (int i=0;i<n;i++){
            // diagonal ~ 1
            if (Math.abs(m.rho[i][i] - 1.0) > 1e-6)
                throw new IllegalArgumentException("rho["+i+"]["+i+"] != 1");
            for (int j=i+1;j<n;j++){
                double a = m.rho[i][j], b = m.rho[j][i];
                if (Math.abs(a - b) > 1e-6)
                    throw new IllegalArgumentException("rho no simétrica en ("+i+","+j+")");
                if (a < -1.0 - EPS || a > 1.0 + EPS)
                    throw new IllegalArgumentException("rho fuera de [-1,1] en ("+i+","+j+"): "+a);
            }
        }

        // 2) Chequeos de activos
        var vistos = new java.util.HashSet<String>();
        for (int i=0;i<n;i++){
            var a = m.activos.get(i);
            if (a.ticker == null || a.ticker.isBlank())
                throw new IllegalArgumentException("Ticker vacío en índice " + i);
            if (!vistos.add(a.ticker))
                throw new IllegalArgumentException("Ticker duplicado: " + a.ticker);

            if (a.retorno < -1.0 || a.retorno > 1.0)
                throw new IllegalArgumentException("Retorno fuera de rango en "+a.ticker+": "+a.retorno);

            if (a.sigma <= 0.0 || a.sigma > 1.5)
                throw new IllegalArgumentException("Sigma fuera de rango en "+a.ticker+": "+a.sigma);

            if (a.montoMin <= 0.0)
                throw new IllegalArgumentException("montoMin debe ser > 0 en "+a.ticker+": "+a.montoMin);
        }
    }
}
