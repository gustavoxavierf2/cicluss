package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.Risco;

/** Nomes amigáveis para mostrar ao agricultor. */
public final class Textos {

    private Textos() {
    }

    public static String risco(Risco risco) {
        return switch (risco) {
            case NENHUM -> "nenhum";
            case SECA -> "seca";
            case CHUVA_EXCESSIVA -> "chuva demais";
            case CALOR -> "calor demais";
            case FRIO -> "frio demais";
            case SOLO_INADEQUADO -> "solo pouco indicado";
        };
    }

    public static String nivel(Nivel nivel) {
        return switch (nivel) {
            case BOM -> "bom";
            case ATENCAO -> "de atenção";
            case RUIM -> "ruim";
        };
    }
}
