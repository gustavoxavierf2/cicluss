package br.com.fieldshift.core.modelo;

/**
 * Números para estimar safra e dinheiro de uma cultura.
 *
 * @param produtividadeKgHa quanto a cultura colhe por hectare em um ano bom (nota 100), em kg
 * @param precoPorKg        preço de venda, em R$ por kg
 * @param custoPorHa        custo para plantar e colher um hectare, em R$
 * @param fonte             de onde vieram os valores (CONAB, IBGE etc.)
 */
public record Economia(double produtividadeKgHa, double precoPorKg, double custoPorHa, String fonte) {
}
