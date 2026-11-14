package br.com.fieldshift.core.modelo;

/** Como o ano aparece para o agricultor: verde, amarelo ou vermelho. */
public enum Nivel {
    BOM,
    ATENCAO,
    RUIM;

    public static Nivel daNota(int nota) {
        if (nota >= 70) return BOM;
        if (nota >= 40) return ATENCAO;
        return RUIM;
    }
}
