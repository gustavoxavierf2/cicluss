package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Clima de um trimestre do ano.
 *
 * @param nome             "jan–mar", "abr–jun", "jul–set" ou "out–dez"
 * @param chuva            soma da chuva do trimestre, mm
 * @param temperaturaMedia °C
 */
public record Trimestre(String nome, double chuva, double temperaturaMedia) {

    public static final List<String> NOMES = List.of("jan–mar", "abr–jun", "jul–set", "out–dez");
}
