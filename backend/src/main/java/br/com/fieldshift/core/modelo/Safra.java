package br.com.fieldshift.core.modelo;

/**
 * Estimativa simples de safra e dinheiro de uma cultura em um ano. Não é previsão de produtividade
 * calibrada: é a colheita de um ano bom multiplicada pela nota do ano.
 *
 * @param producaoKg colheita estimada na área informada, em kg
 * @param receita    venda estimada, em R$
 * @param custo      custo estimado, em R$
 * @param saldo      receita menos custo, em R$
 */
public record Safra(int ano, double hectares, long producaoKg, long receita, long custo, long saldo) {
}
