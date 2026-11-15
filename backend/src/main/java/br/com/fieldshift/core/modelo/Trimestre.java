package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Clima de um trimestre do ano.
 *
 * @param nome             "jan–mar", "abr–jun", "jul–set" ou "out–dez"
 * @param chuva            soma da chuva do trimestre, mm
 * @param temperaturaMedia °C
 * @param umidadeSolo      umidade da zona de raiz no trimestre, de 0 a 1 (null quando a fonte não tem)
 */
public record Trimestre(String nome, double chuva, double temperaturaMedia, Double umidadeSolo) {

    /** Trimestre sem a umidade do solo. */
    public Trimestre(String nome, double chuva, double temperaturaMedia) {
        this(nome, chuva, temperaturaMedia, null);
    }

    public static final List<String> NOMES = List.of("jan–mar", "abr–jun", "jul–set", "out–dez");
}
