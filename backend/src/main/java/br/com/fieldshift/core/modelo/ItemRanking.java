package br.com.fieldshift.core.modelo;

/** Uma cultura na lista de recomendação, com o motivo em uma linha. */
public record ItemRanking(
        Cultura cultura,
        int posicao,
        int notaMedia,
        Nivel nivel,
        int anosBons,
        int totalAnos,
        Risco principalRisco,
        String motivo) {
}
