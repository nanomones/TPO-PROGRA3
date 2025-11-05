package io.dto;

import java.util.List;

public class MercadoJson {
    public List<ActivoJson> activos;          // mismo orden que la matriz
    public List<List<Double>> correlaciones;  // n x n, diagonal 1.0, simétrica
}
