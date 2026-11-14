package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Comparação de estratégias de rotação para um terreno.
 *
 * @param recomendadaId estratégia indicada
 * @param porque        motivo da indicação, em uma frase
 * @param epocaSeca     época do ano que costuma ser a mais seca no terreno (null sem dado por trimestre)
 */
public record ComparacaoEstrategias(
        List<Estrategia> estrategias,
        String recomendadaId,
        String porque,
        String epocaSeca,
        double hectares) {
}
