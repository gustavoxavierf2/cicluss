package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Clima de um ponto do mapa em um ano. É o formato que o pessoal dos dados entrega
 * (ver docs/contrato-dados.md).
 *
 * @param projetado          true para anos futuros (previsão), false para dado observado
 * @param temperaturaMedia   °C
 * @param temperaturaMaxima  média das máximas, °C
 * @param temperaturaMinima  média das mínimas, °C
 * @param precipitacao       chuva total no ano, mm
 * @param umidadeSolo        umidade da zona de raiz, de 0 (seco) a 1 (saturado)
 * @param evapotranspiracao  total no ano, mm
 * @param trimestres         chuva e temperatura dos 4 trimestres, para mostrar a época do ano
 *                           que pesou; null quando a fonte só tem o valor do ano
 */
public record ClimaAnual(
        int ano,
        boolean projetado,
        double temperaturaMedia,
        double temperaturaMaxima,
        double temperaturaMinima,
        double precipitacao,
        double umidadeSolo,
        double evapotranspiracao,
        List<Trimestre> trimestres) {

    /** Clima só com os valores do ano, sem a divisão por trimestre. */
    public ClimaAnual(int ano, boolean projetado, double temperaturaMedia, double temperaturaMaxima,
                      double temperaturaMinima, double precipitacao, double umidadeSolo, double evapotranspiracao) {
        this(ano, projetado, temperaturaMedia, temperaturaMaxima, temperaturaMinima, precipitacao, umidadeSolo,
                evapotranspiracao, null);
    }
}
