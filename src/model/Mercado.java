package model;

import java.util.*;

public final class Mercado {
    public final List<Activo> activos;
    public final double[][] rho;                  // matriz de correlaciones n x n
    public final Map<String,Integer> idxPorTicker;

    public Mercado(List<Activo> activos, double[][] rho) {
        this.activos = List.copyOf(activos);
        this.rho = rho;
        var map = new HashMap<String,Integer>();
        for (int i = 0; i < activos.size(); i++) map.put(activos.get(i).ticker, i);
        this.idxPorTicker = Collections.unmodifiableMap(map);
    }

    public int indexOf(String ticker){ return idxPorTicker.getOrDefault(ticker, -1); }
    /** Busca un activo por su ticker (retorna null si no existe) */
public Activo buscarPorTicker(String ticker) {
    for (Activo a : this.activos) {
        if (a.ticker.equalsIgnoreCase(ticker)) {
            return a;
        }
    }
    return null;
}

}
