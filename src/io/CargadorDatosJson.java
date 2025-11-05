package io;

import com.google.gson.*;
import io.dto.*;
import model.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class CargadorDatosJson {
    private CargadorDatosJson(){}

    public static Mercado cargarMercado(String rutaJson){
        try {
            String json = Files.readString(Path.of(rutaJson));
            MercadoJson mj = new Gson().fromJson(json, MercadoJson.class);

            if (mj.activos == null || mj.activos.isEmpty())
                throw new IllegalArgumentException("'activos' vacío");
            if (mj.correlaciones == null || mj.correlaciones.isEmpty())
                throw new IllegalArgumentException("'correlaciones' vacía");

            int n = mj.activos.size();
            if (mj.correlaciones.size() != n) throw new IllegalArgumentException("ρ no es n x n");
            for (var fila : mj.correlaciones) if (fila.size() != n) throw new IllegalArgumentException("ρ no es n x n");

            var lista = new ArrayList<Activo>(n);
            var tickers = new HashSet<String>();
            for (var a : mj.activos){
                if (a.ticker == null || a.ticker.isBlank()) throw new IllegalArgumentException("ticker vacío");
                if (!tickers.add(a.ticker)) throw new IllegalArgumentException("ticker duplicado: "+a.ticker);
                lista.add(new Activo(a.ticker, a.tipo, a.sector, a.retorno, a.sigma, a.montoMin));
            }

            double[][] rho = new double[n][n];
            for (int i=0;i<n;i++) for (int j=0;j<n;j++) rho[i][j] = mj.correlaciones.get(i).get(j);

            // chequeos básicos
            for (int i=0;i<n;i++){
                if (Math.abs(rho[i][i]-1.0) > 1e-9) throw new IllegalArgumentException("ρ[i][i]!=1 en i="+i);
                for (int j=i+1;j<n;j++){
                    if (Math.abs(rho[i][j]-rho[j][i]) > 1e-9) throw new IllegalArgumentException("ρ no simétrica ("+i+","+j+")");
                    if (rho[i][j] < -1.0 || rho[i][j] > 1.0) throw new IllegalArgumentException("ρ fuera de [-1,1] ("+i+","+j+")");
                }
            }

            return new Mercado(lista, rho);
        } catch (IOException e){
            throw new RuntimeException("No pude leer " + rutaJson, e);
        } catch (JsonSyntaxException e){
            throw new RuntimeException("JSON inválido: " + e.getMessage(), e);
        }
    }
}
