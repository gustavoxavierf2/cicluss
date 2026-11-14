package br.com.fieldshift.core.modelo;

/**
 * Clima de um ponto do mapa em um ano. É o formato que o pessoal dos dados entrega
 * (ver docs/contrato-dados.md).
 *
 * @param projetado          true para anos futuros (previsão), false para dado observado
 * @param temperaturaMedia   °C
 * @param temperaturaMaxima  média das máximas diárias, °C
 * @param temperaturaMinima  média das mínimas diárias, °C
 * @param precipitacao       chuva total no ano, mm
 * @param umidadeSolo        umidade da zona de raiz, de 0 (seco) a 1 (saturado)
 * @param evapotranspiracao  total no ano, mm
 */
public record ClimaAnual(
        int ano,
        boolean projetado,
        double temperaturaMedia,
        double temperaturaMaxima,
        double temperaturaMinima,
        double precipitacao,
        double umidadeSolo,
        double evapotranspiracao) {
}
