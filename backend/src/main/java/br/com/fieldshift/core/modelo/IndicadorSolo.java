package br.com.fieldshift.core.modelo;

/** Um sinal simples de saúde do solo de uma estratégia de rotação (nitrogênio, pragas, cobertura, palha). */
public record IndicadorSolo(String nome, Nivel nivel, String texto) {
}
