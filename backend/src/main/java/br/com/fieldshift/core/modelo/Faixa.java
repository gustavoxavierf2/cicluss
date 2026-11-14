package br.com.fieldshift.core.modelo;

/**
 * Faixa de tolerância de uma cultura para uma variável (no formato do EcoCrop/FAO):
 * entre idealMin e idealMax a cultura vai bem; entre os limites absolutos ela sobrevive
 * com perda; fora deles não produz.
 */
public record Faixa(double idealMin, double idealMax, double absMin, double absMax) {

    public Faixa {
        if (!(absMin <= idealMin && idealMin <= idealMax && idealMax <= absMax)) {
            throw new IllegalArgumentException(
                    "Faixa inválida: esperado absMin <= idealMin <= idealMax <= absMax, recebido "
                            + absMin + ", " + idealMin + ", " + idealMax + ", " + absMax);
        }
    }

    /** 1.0 dentro do ideal, cai em linha reta até 0.0 no limite absoluto, 0.0 fora dele. */
    public double pontuar(double valor) {
        if (valor >= idealMin && valor <= idealMax) return 1.0;
        if (valor <= absMin || valor >= absMax) return 0.0;
        if (valor < idealMin) return (valor - absMin) / (idealMin - absMin);
        return (absMax - valor) / (absMax - idealMax);
    }

    public boolean abaixoDoIdeal(double valor) {
        return valor < idealMin;
    }
}
