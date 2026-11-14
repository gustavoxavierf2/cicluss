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

    /** O que fazer para se proteger do risco previsto, em uma frase. */
    public static String cuidado(Risco risco) {
        return switch (risco) {
            case NENHUM -> "Sem risco forte previsto para este ano.";
            case SECA -> "Risco de seca: plante logo no começo das chuvas e, se puder, guarde água e cubra o solo com palha.";
            case CHUVA_EXCESSIVA -> "Risco de chuva demais: prefira a parte mais alta do terreno e abra valetas para a água escoar.";
            case CALOR -> "Risco de calor: plante mais cedo para fugir dos meses mais quentes e mantenha o solo coberto.";
            case FRIO -> "Risco de frio: evite plantar nos meses mais frios e fuja das baixadas.";
            case SOLO_INADEQUADO -> "A terra não é a mais indicada: adube com matéria orgânica e, se der, converse com um técnico.";
        };
    }

    /** Valor em reais sem centavos, com ponto de milhar: 12500 vira "12.500". */
    public static String dinheiro(long valor) {
        return String.format(java.util.Locale.ROOT, "%,d", valor).replace(',', '.');
    }

    public static String nivel(Nivel nivel) {
        return switch (nivel) {
            case BOM -> "bom";
            case ATENCAO -> "de atenção";
            case RUIM -> "ruim";
        };
    }
}
