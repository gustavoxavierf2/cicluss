package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Resultado do simulador para uma cultura em um terreno.
 *
 * @param anoAtual último ano com dado observado; anos depois dele são projeção
 */
public record Simulacao(
        Cultura cultura,
        double latitude,
        double longitude,
        TipoSolo solo,
        int anoAtual,
        List<ResultadoAno> anos,
        Resumo resumoPassado,
        Resumo resumoFuturo) {
}
