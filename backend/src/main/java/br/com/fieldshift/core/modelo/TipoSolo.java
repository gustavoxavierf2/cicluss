package br.com.fieldshift.core.modelo;

/** Tipos de solo que o agricultor consegue informar sem análise de laboratório. */
public enum TipoSolo {
    ARENOSO,
    ARGILOSO,
    MISTO,
    /** O agricultor não sabe: a aptidão não é penalizada pelo solo. */
    NAO_SEI
}
