package br.com.fieldshift.core.modelo;

/** Como uma cultura teria ido (ou deve ir) em um ano, em linguagem de agricultor. */
public record ResultadoAno(
        int ano,
        boolean projetado,
        int nota,
        Nivel nivel,
        Risco risco,
        String motivo,
        ClimaAnual clima) {
}
