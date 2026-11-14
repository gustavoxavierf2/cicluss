package br.com.fieldshift.core.modelo;

/** Grupo da cultura para a rotação: não se repete o mesmo grupo em anos seguidos. */
public enum GrupoRotacao {
    GRAMINEA,
    LEGUMINOSA,
    RAIZ,
    /** Fica no terreno vários anos (ex.: café); não entra em rotação. */
    PERENE
}
