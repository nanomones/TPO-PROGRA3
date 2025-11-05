package validacion;

import model.Perfil;

public final class ValidadorPerfil {
    private ValidadorPerfil(){}

    public static void validar(Perfil p){
        if (p == null) throw new IllegalArgumentException("Perfil nulo");

        // presupuesto > 0
        if (p.presupuesto <= 0)
            throw new IllegalArgumentException("El presupuesto debe ser > 0");

        // riesgoMax en rango razonable
        if (p.riesgoMax <= 0.0 || p.riesgoMax > 1.5)
            throw new IllegalArgumentException("riesgoMax fuera de rango (0, 1.5]");

        // maxPorActivo en (0, 1]
        if (p.maxPorActivo <= 0.0 || p.maxPorActivo > 1.0)
            throw new IllegalArgumentException("maxPorActivo fuera de rango (0, 1]");

        // límites por tipo y sector entre 0..1
        for (var e : p.maxPorTipo.entrySet()) {
            double v = e.getValue();
            if (v < 0.0 || v > 1.0)
                throw new IllegalArgumentException("maxPorTipo inválido para '" + e.getKey() + "': " + v);
        }
        for (var e : p.maxPorSector.entrySet()) {
            double v = e.getValue();
            if (v < 0.0 || v > 1.0)
                throw new IllegalArgumentException("maxPorSector inválido para '" + e.getKey() + "': " + v);
        }
    }
}
