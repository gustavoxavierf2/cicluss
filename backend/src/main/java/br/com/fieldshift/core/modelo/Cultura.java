package br.com.fieldshift.core.modelo;

import java.util.List;

/**
 * Uma linha da tabela de culturas.
 *
 * @param temperatura temperatura média do ar em °C
 * @param chuva       chuva anual em mm
 * @param solos       solos em que a cultura vai bem
 * @param fonte       de onde vieram os valores (ficha EcoCrop, ZARC etc.)
 */
public record Cultura(
        String id,
        String nome,
        Faixa temperatura,
        Faixa chuva,
        List<TipoSolo> solos,
        int cicloDias,
        String fonte) {

    public boolean aceitaSolo(TipoSolo solo) {
        return solo == null || solo == TipoSolo.NAO_SEI || solos.contains(solo);
    }
}
