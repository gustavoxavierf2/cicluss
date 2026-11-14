package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Resultado do simulador para uma cultura em um terreno.
 *
 * @param anoAtual último ano com dado observado; anos depois dele são projeção
 * @param hectares área informada pelo agricultor (null se não informou)
 * @param safras   estimativa de safra e dinheiro de cada ano, na mesma ordem de {@code anos};
 *                 vazia quando não há hectares ou a cultura não tem valores cadastrados
 */
public record Simulacao(
        Cultura cultura,
        double latitude,
        double longitude,
        TipoSolo solo,
        int anoAtual,
        List<ResultadoAno> anos,
        Resumo resumoPassado,
        Resumo resumoFuturo,
        Double hectares,
        List<Safra> safras) {
}
