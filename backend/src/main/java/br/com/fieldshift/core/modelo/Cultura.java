package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Uma linha da tabela de culturas.
 *
 * @param temperatura  temperatura média do ar em °C
 * @param chuva        chuva anual em mm
 * @param solos        solos em que a cultura vai bem
 * @param fonte        de onde vieram os valores (ficha EcoCrop, ZARC etc.)
 * @param grupoRotacao grupo para a rotação de plantio (pode ser null)
 * @param economia     valores para estimar safra e dinheiro (pode ser null)
 */
public record Cultura(
        String id,
        String nome,
        Faixa temperatura,
        Faixa chuva,
        List<TipoSolo> solos,
        int cicloDias,
        String fonte,
        GrupoRotacao grupoRotacao,
        Economia economia) {

    /** Cultura sem dados de rotação nem de dinheiro. */
    public Cultura(String id, String nome, Faixa temperatura, Faixa chuva, List<TipoSolo> solos,
                   int cicloDias, String fonte) {
        this(id, nome, temperatura, chuva, solos, cicloDias, fonte, null, null);
    }

    public boolean aceitaSolo(TipoSolo solo) {
        return solo == null || solo == TipoSolo.NAO_SEI || solos.contains(solo);
    }
}
