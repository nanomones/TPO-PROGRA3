import model.Mercado;
import io.CargadorDatosJson;
import validacion.ValidadorMercado;

import model.Perfil;
import model.Cliente;
import validacion.ValidadorPerfil;
import model.Activo;
import model.Asignacion;
import heuristicas.SemillaFactible;
import heuristicas.GreedyInicial;

import io.Reporte;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        // 1) Cargar y validar mercado
        Mercado m = CargadorDatosJson.cargarMercado("data/mercado.json");
        ValidadorMercado.validar(m);

        System.out.println("Activos: " + m.activos.size());
        System.out.println("Matriz rho: " + m.rho.length + " x " + m.rho[0].length);
        for (int i = 0; i < Math.min(5, m.activos.size()); i++) {
            System.out.println(" - " + m.activos.get(i));
        }
        System.out.println("OK: Mercado valido");
        
        // Perfil del cliente (plazo 1 año fijo por cátedra)
Perfil perfil = new Perfil(
    100_000.0,  // presupuesto
    0.15,       // maxPorActivo (15%)
    Map.of("Accion", 0.70, "Bono", 0.60, "ETF", 0.50),
    Map.of("Tecnologia", 0.60, "Energia", 0.50, "Salud", 0.50),
    "Moderadamente agresivo", // Conservador / Moderadamente conservador / Moderado / Moderadamente agresivo / Agresivo
    0.18        // retorno mínimo deseado por el cliente (>= al mínimo del perfil)
 );
        ValidadorPerfil.validar(perfil);
        System.out.println("OK: Perfil valido");

        Cliente c = new Cliente("Cliente Demo", perfil);
        System.out.println("Cliente: " + c.nombre);

        // 3) SEMILLA (siempre valida)
        Asignacion a0 = SemillaFactible.construir(m, perfil);
        System.out.println("\n--- SEMILLA ---");
        Reporte.imprimirResumen(m, perfil, a0);

        // 4) GREEDY (mejora retorno manteniendo restricciones)
        Asignacion aGreedy = GreedyInicial.construir(m, perfil);
        System.out.println("\n--- GREEDY ---");
        Reporte.imprimirResumen(m, perfil, aGreedy);
        // --- BRANCH & BOUND ---
optimizacion.BBPortafolio.Resultado res = optimizacion.BBPortafolio.maximizarRetorno(m, perfil);
System.out.println("\n--- BRANCH & BOUND ---");
io.Reporte.imprimirResumen(m, perfil, res.mejor);
// ------- Mostrar alternativas (Parte B) -------

// Alternativa 1: solución Greedy (ya implementada)
System.out.println("\n===== Alternativa 1: Portafolio Greedy =====");
Reporte.imprimirResumen(m, perfil, aGreedy);

// Alternativa 2: mutación del óptimo
System.out.println("\n===== Alternativa 2: Portafolio Mutado =====");
Asignacion alternativa2 = mutarAsignacion(res.mejor, m, perfil);
Reporte.imprimirResumen(m, perfil, alternativa2);
System.out.println("Nodos visitados: " + res.nodosVisitados);
    }
    private static Asignacion mutarAsignacion(Asignacion original, Mercado mercado, Perfil perfil) {
    // Copiar los montos originales
    Map<String, Double> nuevaAsignacion = new java.util.LinkedHashMap<>(original.getMontos());

    // Buscar el ticker con menor retorno
    String tickerMenor = null;
    double retornoMenor = Double.MAX_VALUE;

    for (String t : nuevaAsignacion.keySet()) {
        Activo a = mercado.buscarPorTicker(t);
        if (a != null && a.retorno < retornoMenor) {
            retornoMenor = a.retorno;
            tickerMenor = t;
        }
    }

    if (tickerMenor != null) {
        // Quitamos ese activo
        nuevaAsignacion.remove(tickerMenor);

        // Buscamos un reemplazo con mejor retorno y riesgo aceptable
        Activo mejorReemplazo = null;
        for (Activo a : mercado.activos) {
            if (!nuevaAsignacion.containsKey(a.ticker)
                    && a.sigma <= perfil.riesgoMax
                    && a.retorno > retornoMenor) {
                if (mejorReemplazo == null || a.retorno > mejorReemplazo.retorno) {
                    mejorReemplazo = a;
                }
            }
        }

        // Si encontramos un reemplazo, le asignamos el mismo monto
        if (mejorReemplazo != null) {
            double montoAnterior = original.getMonto(tickerMenor);
            nuevaAsignacion.put(mejorReemplazo.ticker, montoAnterior);
        } else {
            // Si no hay reemplazo, reponemos el original
            nuevaAsignacion.put(tickerMenor, original.getMonto(tickerMenor));
        }
    }

    // Crear una nueva Asignacion con el mapa modificado
    return new Asignacion(nuevaAsignacion);
}

}
