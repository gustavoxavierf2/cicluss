package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Plano de plantio com rotação para os próximos anos, a partir da cultura que o agricultor escolheu.
 *
 * @param saldoTotal  soma do saldo estimado dos anos do plano, em R$ (null se faltar valor de alguma cultura)
 * @param anosDeRisco quantos anos do plano têm previsão de atenção ou ruim
 * @param avisos      alertas sobre dinheiro e clima, em frases simples
 */
public record Planejamento(
        Cultura principal,
        double hectares,
        List<AnoPlano> anos,
        Long saldoTotal,
        int anosDeRisco,
        String resumo,
        List<String> avisos) {
}
