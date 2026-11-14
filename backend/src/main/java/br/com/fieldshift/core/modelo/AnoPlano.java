package br.com.fieldshift.core.modelo;

/**
 * Um ano do planejamento.
 *
 * @param principal true quando é a cultura que o agricultor escolheu; false quando é a da rotação
 * @param porque    por que essa cultura entra neste ano, em uma frase
 * @param cuidado   o que fazer para se proteger do risco previsto
 * @param planoB    cultura reserva quando a previsão do ano não é boa (pode ser null)
 */
public record AnoPlano(
        int ano,
        Cultura cultura,
        boolean principal,
        ResultadoAno resultado,
        Safra safra,
        String porque,
        String cuidado,
        Cultura planoB) {
}
