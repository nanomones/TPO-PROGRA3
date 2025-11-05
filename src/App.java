import model.Mercado;
import io.CargadorDatosJson;
import validacion.ValidadorMercado;

import model.Perfil;
import model.Cliente;
import validacion.ValidadorPerfil;
import model.Asignacion;
import model.Activo;

import heuristicas.SemillaFactible;
import heuristicas.GreedyInicial;
import optimizacion.BBPortafolio;

import io.Reporte;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        System.out.println(">>> MODO INTERACTIVO <<<");

        // === 1) Cargar mercado ===
        final String RUTA_JSON = "datos/mercado.json"; // Ajustá si tu carpeta es "data"
        Mercado m = CargadorDatosJson.cargarMercado(RUTA_JSON);
        ValidadorMercado.validar(m);

        System.out.println("Activos: " + m.activos.size());
        System.out.println("Matriz rho: " + m.rho.length + " x " + m.rho[0].length);
        for (int i = 0; i < Math.min(5, m.activos.size()); i++) {
            System.out.println(" - " + m.activos.get(i));
        }
        System.out.println("OK: Mercado válido");

        // === 2) Ingreso de datos por consola ===
        Scanner sc = new Scanner(System.in);

        System.out.print("\nNombre del cliente [Enter = Cliente Demo]: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) nombre = "Cliente Demo";

        System.out.println("Tipo de perfil [Conservador | Moderadamente conservador | Moderado | Moderadamente agresivo | Agresivo]");
        System.out.print("Ingresar tipo de perfil [Enter = Moderado]: ");
        String tipoPerfil = sc.nextLine().trim();
        if (tipoPerfil.isEmpty()) tipoPerfil = "Moderado";

        System.out.print("Presupuesto (ej: 100000): ");
        double presupuesto = leerDouble(sc, 100000.0);

        // Límites generales
        double maxPorActivo = 0.15;
        Map<String, Double> maxPorTipo = Map.of("Accion", 0.70, "Bono", 0.60, "ETF", 0.50);
        Map<String, Double> maxPorSector = Map.of("Tecnologia", 0.60, "Energia", 0.50, "Salud", 0.50, "Consumo", 0.50);

        // === 3) Configuración según tipo de perfil ===
        // Tabla pedida en el enunciado
        double riesgoMax;
        double retornoMin; // solo como variable local, no en Perfil
        switch (tipoPerfil.toLowerCase()) {
            case "conservador":
                riesgoMax = 0.20;
                retornoMin = 0.10;
                break;
            case "moderadamente conservador":
                riesgoMax = 0.30;
                retornoMin = 0.12;
                break;
            case "moderado":
                riesgoMax = 0.40;
                retornoMin = 0.14;
                break;
            case "moderadamente agresivo":
                riesgoMax = 0.50;
                retornoMin = 0.16;
                break;
            case "agresivo":
                riesgoMax = 0.60;
                retornoMin = 0.18;
                break;
            default:
                System.out.println("Tipo no reconocido, se usa 'Moderado' por defecto.");
                riesgoMax = 0.40;
                retornoMin = 0.14;
        }

        // Permitir editar por consola si se desea
        System.out.print("¿Editar riesgo máximo? [Enter = " + riesgoMax + "]: ");
        String rEdit = sc.nextLine().trim();
        if (!rEdit.isEmpty()) riesgoMax = Double.parseDouble(rEdit);

        System.out.print("¿Editar retorno mínimo esperado? [Enter = " + retornoMin + "]: ");
        String retEdit = sc.nextLine().trim();
        if (!retEdit.isEmpty()) retornoMin = Double.parseDouble(retEdit);

        // === 4) Crear perfil y cliente ===
        Perfil perfil = new Perfil(
            presupuesto,
            maxPorActivo,
            maxPorTipo,
            maxPorSector,
            tipoPerfil,
            riesgoMax
        );
        ValidadorPerfil.validar(perfil);
        System.out.println("OK: Perfil válido");

        Cliente c = new Cliente(nombre, perfil);
        System.out.println("Cliente: " + c.nombre + " | Perfil: " + tipoPerfil);
        System.out.println("Retorno mínimo esperado: " + (retornoMin * 100) + "%");

        // === 5) SEMILLA ===
        Asignacion a0 = SemillaFactible.construir(m, perfil);
        System.out.println("\n--- SEMILLA ---");
        Reporte.imprimirResumen(m, perfil, a0);

        // === 6) GREEDY ===
        Asignacion aGreedy = GreedyInicial.construir(m, perfil);
        System.out.println("\n--- GREEDY ---");
        Reporte.imprimirResumen(m, perfil, aGreedy);

        // === 7) BRANCH & BOUND ===
        BBPortafolio.Resultado res = BBPortafolio.maximizarRetorno(m, perfil);
        System.out.println("\n--- BRANCH & BOUND ---");
        Reporte.imprimirResumen(m, perfil, res.mejor);
        System.out.println("Nodos visitados: " + res.nodosVisitados);

        // === 8) Alternativas ===
        System.out.println("\n===== Alternativa 1: Portafolio Greedy =====");
        Reporte.imprimirResumen(m, perfil, aGreedy);

        System.out.println("\n===== Alternativa 2: Portafolio Mutado =====");
        Asignacion alt2 = mutarAsignacion(res.mejor, m, perfil);
        Reporte.imprimirResumen(m, perfil, alt2);

        sc.close();
    }

    private static double leerDouble(Scanner sc, double defecto) {
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return defecto;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return defecto; }
    }

    // === Mutación simple: reemplaza el activo con menor retorno por otro con mejor retorno ===
    private static Asignacion mutarAsignacion(Asignacion original, Mercado mercado, Perfil perfil) {
        Map<String, Double> nueva = new LinkedHashMap<>(original.montoPorTicker);

        String peorTicker = null;
        double peorRetorno = Double.MAX_VALUE;
        for (String t : nueva.keySet()) {
            Activo a = mercado.buscarPorTicker(t);
            if (a != null && a.retorno < peorRetorno) {
                peorRetorno = a.retorno;
                peorTicker = t;
            }
        }

        if (peorTicker == null) return original;

        Activo mejorReemplazo = null;
        for (Activo a : mercado.activos) {
            if (nueva.containsKey(a.ticker)) continue;
            if (a.retorno <= peorRetorno) continue;
            if (a.sigma > perfil.riesgoMax) continue;

            if (mejorReemplazo == null || a.retorno > mejorReemplazo.retorno)
                mejorReemplazo = a;
        }

        if (mejorReemplazo != null) {
            double montoAnterior = original.monto(peorTicker);
            nueva.remove(peorTicker);
            nueva.put(mejorReemplazo.ticker, montoAnterior);
        }

        return new Asignacion(nueva);
    }
}
