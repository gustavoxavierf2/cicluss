package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Uma estratégia de rotação avaliada com o clima que realmente aconteceu no terreno.
 *
 * @param atual           true para o que o agricultor faz hoje
 * @param sequencia       culturas na ordem em que se repetem, uma por ano
 * @param anos            como teria sido cada ano observado seguindo a estratégia
 * @param resumo          anos bons, de atenção e ruins no período
 * @param saldoTotal      soma da sobra estimada no período, em R$ (null sem valores cadastrados)
 * @param solo            sinais de saúde do solo
 * @param pontosSolo      de 0 a 8: 2 pontos por sinal bom, 1 por sinal de atenção
 * @param mesesDescoberto meses por ano, em média, com a terra sem cultura
 */
public record Estrategia(
        String id,
        String nome,
        String descricao,
        boolean atual,
        List<Cultura> sequencia,
        List<AnoEstrategia> anos,
        Resumo resumo,
        Long saldoTotal,
        List<IndicadorSolo> solo,
        int pontosSolo,
        int mesesDescoberto) {

    public record AnoEstrategia(int ano, String cultura, int nota, Nivel nivel, Risco risco) {
    }
}
