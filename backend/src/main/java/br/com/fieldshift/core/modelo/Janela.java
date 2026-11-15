package br.com.fieldshift.core.modelo;

/**
 * Uma época (4 meses) da linha do tempo da rotação.
 *
 * @param inicio           primeiro mês, como "Jan/27"
 * @param fim              último mês, como "Abr/27"
 * @param plantio          o que fica na terra nessa época (cultura ou planta de cobertura)
 * @param culturaId        id da cultura da tabela; null quando é planta de cobertura
 * @param porque           para que serve esse plantio, em linguagem simples
 * @param temperaturaMedia média prevista da época, °C
 * @param umidadeSolo      umidade média prevista da zona de raiz na época, de 0 a 1
 * @param chuva            chuva prevista na época, mm
 * @param mediaDoAno       true quando a fonte do clima não separa as épocas e os valores são os do ano inteiro
 */
public record Janela(
        int ano,
        String inicio,
        String fim,
        Fase fase,
        String plantio,
        String culturaId,
        String porque,
        double temperaturaMedia,
        double umidadeSolo,
        double chuva,
        boolean mediaDoAno) {
}
