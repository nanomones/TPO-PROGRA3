package model;

import java.util.Map;

public final class Perfil {

    public final double presupuesto;
    public final double riesgoMax;               // sigma máximo permitido (decimal: 0.20 = 20%)
    public final double maxPorActivo;
    public final Map<String, Double> maxPorTipo;
    public final Map<String, Double> maxPorSector;

    public final String tipoPerfil;              // Conservador, Moderadamente conservador, Moderado, Moderadamente agresivo, Agresivo
    public final double retornoMin;              // mínimo del perfil (decimal: 0.10 = 10%)
    public final double retornoMinDeseado;       // mínimo pedido por el cliente (>= retornoMin del perfil)
    public final int plazoAnios = 1;             // fijo 

    /**
     * Constructor que configura riesgoMax y retornoMin automáticamente según el tipo de perfil.
     * @param presupuesto       Presupuesto total (moneda)
     * @param maxPorActivo      Tope por activo (fracción 0..1)
     * @param maxPorTipo        Topes por tipo (fracción 0..1)
     * @param maxPorSector      Topes por sector (fracción 0..1)
     * @param tipoPerfil        Uno de: "Conservador", "Moderadamente conservador", "Moderado",
     *                          "Moderadamente agresivo", "Agresivo"
     * @param retornoDeseado    Retorno mínimo deseado por el cliente (decimal). Se forzará a ser >= retornoMin del perfil.
     */
    public Perfil(double presupuesto,
                  double maxPorActivo,
                  Map<String, Double> maxPorTipo,
                  Map<String, Double> maxPorSector,
                  String tipoPerfil,
                  double retornoDeseado) {

        this.presupuesto = presupuesto;
        this.maxPorActivo = maxPorActivo;
        this.maxPorTipo = maxPorTipo;
        this.maxPorSector = maxPorSector;
        this.tipoPerfil = tipoPerfil;

        String t = tipoPerfil.trim().toLowerCase();
        if (t.equals("conservador")) {
            this.riesgoMax = 0.20;
            this.retornoMin = 0.10;
        } else if (t.equals("moderadamente conservador")) {
            this.riesgoMax = 0.30;
            this.retornoMin = 0.12;
        } else if (t.equals("moderado")) {
            this.riesgoMax = 0.40;
            this.retornoMin = 0.14;
        } else if (t.equals("moderadamente agresivo")) {
            this.riesgoMax = 0.50;
            this.retornoMin = 0.16;
        } else if (t.equals("agresivo")) {
            this.riesgoMax = 0.60;
            this.retornoMin = 0.18;
        } else {
            throw new IllegalArgumentException("Tipo de perfil no reconocido: " + tipoPerfil);
        }

        this.retornoMinDeseado = Math.max(retornoDeseado, this.retornoMin);
    }
}

