package tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

// Conversor CSV -> JSON para el TPO (activos + matriz de correlaciones)
// Requisitos de CSV:
//  - activos_financieros_60.csv: columnas (flexibles en nombre)
//      ticker, tipo, sector, retorno, sigma, montoMin
//    * retorno/sigma pueden venir como 0.12 o 12 o "12%": se normalizan a decimales
//  - correlaciones_60.csv: matriz con encabezados y primera columna de tickers, cuadrada, simétrica (o casi)

public class ConvertCsvToJson {

    // Ajustá las rutas si tus archivos están en otra carpeta:
private static final String RUTA_ACTIVOS = "activos.csv";
private static final String RUTA_CORR    = "correlaciones.csv";
private static final String RUTA_SALIDA  = "data/mercado.json";

    public static void main(String[] args) {
        try {
            // Si pasás las rutas por argumentos, se usan esas:
            String inAct  = args.length > 0 ? args[0] : RUTA_ACTIVOS;
            String inCorr = args.length > 1 ? args[1] : RUTA_CORR;
            String out    = args.length > 2 ? args[2] : RUTA_SALIDA;

            List<Map<String,String>> activosRows = leerCsvComoMaps(inAct);
            Map<String,String> col = mapearColumnas(activosRows.get(0).keySet());

            List<Map<String,Object>> activos = new ArrayList<>();
            for (Map<String,String> row : activosRows) {
                String ticker = req(row, col.get("ticker"));
                String tipo   = req(row, col.get("tipo"));
                String sector = req(row, col.get("sector"));
                Double retorno= toDecimal(req(row, col.get("retorno")));
                Double sigma  = toDecimal(req(row, col.get("sigma")));
                Double monto  = Double.valueOf(req(row, col.get("montoMin")));
                Map<String,Object> a = new LinkedHashMap<>();
                a.put("ticker", ticker.trim());
                a.put("tipo", tipo.trim());
                a.put("sector", sector.trim());
                a.put("retorno", retorno);
                a.put("sigma", sigma);
                a.put("montoMin", monto);
                activos.add(a);
            }

            Correlacion corr = leerMatrizCorrelaciones(inCorr);
            // Alineamos activos al orden de la matriz (por tickers de encabezados)
            Map<String,Map<String,Object>> byTicker = new HashMap<>();
            for (var a : activos) byTicker.put((String)a.get("ticker"), a);

            List<Map<String,Object>> activosOrdenados = new ArrayList<>();
            List<String> orden = corr.tickers;
            for (String t : orden) {
                Map<String,Object> a = byTicker.get(t);
                if (a == null)
                    throw new IllegalArgumentException("Ticker en correlaciones no encontrado en activos: " + t);
                activosOrdenados.add(a);
            }

            // Ajustamos diagonal a 1.0 por seguridad
            for (int i = 0; i < corr.matriz.length; i++) corr.matriz[i][i] = 1.0;

            // Armamos JSON
            Map<String,Object> mercado = new LinkedHashMap<>();
            mercado.put("activos", activosOrdenados);
            mercado.put("correlaciones", toList2D(corr.matriz));

            // Guardamos
            Path outPath = Path.of(out);
            Files.createDirectories(outPath.getParent());
            try (Writer w = Files.newBufferedWriter(outPath)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(mercado, w);
            }

            // Preview
            System.out.println("OK -> " + outPath.toAbsolutePath());
            System.out.println("Activos: " + activosOrdenados.size());
            System.out.println("Matriz:  " + corr.matriz.length + " x " + corr.matriz.length);
            for (int i = 0; i < Math.min(5, activosOrdenados.size()); i++) {
                System.out.println(" - " + activosOrdenados.get(i).get("ticker"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    // ---------- Utilidades ----------

    private static String req(Map<String,String> row, String key) {
        String v = row.get(key);
        if (v == null) throw new IllegalArgumentException("Falta columna requerida: " + key);
        return v;
    }

    private static Double toDecimal(String s) {
        s = s.trim().replace("%","").replace(",",".");
        double v = Double.parseDouble(s);
        if (v > 1.5) return v / 100.0; // si parece porcentaje (12 -> 0.12)
        return v;
    }
private static Map<String,String> mapearColumnas(Set<String> cols) {
    Map<String,String> m = new HashMap<>();
    for (String c : cols) {
        String k = c.toLowerCase().trim();

        // ticker
        if (k.equals("ticker") || k.equals("simbolo") || k.equals("símbolo"))
            m.putIfAbsent("ticker", c);

        // tipo
        else if (k.equals("tipo") || k.contains("clase"))
            m.putIfAbsent("tipo", c);

        // sector
        else if (k.equals("sector") || k.equals("industria"))
            m.putIfAbsent("sector", c);

        // retorno (agrego RetornoEsperado)
        else if (k.contains("retorno") || k.contains("rendimi") || k.contains("expected_return")
                 || k.equals("mu") || k.equals("r") || k.equals("retornoesperado"))
            m.putIfAbsent("retorno", c);

        // sigma (agrego Riesgo)
        else if (k.equals("sigma") || k.contains("volat") || k.contains("desvio") || k.contains("desvío")
                 || k.equals("std") || k.equals("vol") || k.equals("riesgo"))
            m.putIfAbsent("sigma", c);

        // monto mínimo (agrego InversionMinima)
        else if (k.contains("montomin") || k.contains("monto_min") || k.contains("minimo") || k.contains("mínimo")
                 || k.contains("inversion_min") || k.equals("inversionminima"))
            m.putIfAbsent("montoMin", c);
        // cualquier otra columna (2019..2023) se ignora
    }

    List<String> req = List.of("ticker","tipo","sector","retorno","sigma","montoMin");
    for (String r : req) if (!m.containsKey(r))
        throw new IllegalArgumentException("No pude inferir columna: " + r + " a partir de " + cols);
    return m;
}

    private static class Correlacion {
        List<String> tickers;
        double[][] matriz;
    }

    private static Correlacion leerMatrizCorrelaciones(String ruta) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ruta));
        if (lines.isEmpty()) throw new IllegalArgumentException("correlaciones CSV vacío");

        // Parse simple: primera fila = encabezados (vacía + tickers), primera col de cada fila = ticker
        String[] header = splitCsv(lines.get(0));
        List<String> tickers = new ArrayList<>();
        for (int i = 1; i < header.length; i++) tickers.add(header[i].trim());

        int n = tickers.size();
        double[][] m = new double[n][n];

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsv(lines.get(i));
            if (parts.length < n+1) throw new IllegalArgumentException("Fila de correlaciones con menos columnas de las esperadas en línea " + (i+1));
            String rowTicker = parts[0].trim();
            if (!rowTicker.equals(tickers.get(i-1)))
                throw new IllegalArgumentException("El orden de fila no coincide con encabezados: " + rowTicker + " vs " + tickers.get(i-1));
            for (int j = 0; j < n; j++) {
                String val = parts[j+1].trim().replace(",",".");
                m[i-1][j] = Double.parseDouble(val);
            }
        }

        Correlacion c = new Correlacion();
        c.tickers = tickers;
        c.matriz = m;
        return c;
    }

    private static String[] splitCsv(String line) {
        // CSV simple (sin comillas escapadas). Si tus archivos tienen comillas con comas, usá un parser más robusto.
        return line.split(";", -1).length > 1 ? line.split(";", -1) : line.split(",", -1);
    }

    private static List<List<Double>> toList2D(double[][] a) {
        List<List<Double>> out = new ArrayList<>();
        for (double[] row : a) {
            List<Double> r = new ArrayList<>(row.length);
            for (double v : row) r.add(Math.round(v*1_000_000d)/1_000_000d);
            out.add(r);
        }
        return out;
    }
        private static List<Map<String,String>> leerCsvComoMaps(String ruta) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ruta));
        if (lines.isEmpty()) throw new IllegalArgumentException("CSV vacío: " + ruta);

        String[] headers = splitCsv(lines.get(0));
        List<Map<String,String>> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] values = splitCsv(lines.get(i));
            Map<String,String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            rows.add(row);
        }
        return rows;
    }

}
