package model;

public final class Activo {
    public final String ticker, tipo, sector;
    public final double retorno, sigma, montoMin;

    public Activo(String ticker, String tipo, String sector,
                  double retorno, double sigma, double montoMin) {
        this.ticker = ticker;
        this.tipo = tipo;
        this.sector = sector;
        this.retorno = retorno;
        this.sigma = sigma;
        this.montoMin = montoMin;
    }

    @Override
    public String toString() {
        return "%s (%s/%s) r=%.3f σ=%.3f".formatted(ticker, tipo, sector, retorno, sigma);
    }
}
