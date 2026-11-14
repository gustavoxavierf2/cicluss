package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Risco;
import br.com.fieldshift.core.modelo.TipoSolo;

import java.util.Locale;

/**
 * Coração do simulador: compara o clima de um ano com a tabela da cultura e devolve
 * uma nota de 0 a 100, um nível (bom/atenção/ruim), o risco principal e uma frase simples.
 *
 * Regra (simples de propósito, para caber no pitch):
 * <ol>
 *   <li>Temperatura e chuva recebem uma pontuação de 0 a 1 pela faixa da cultura (EcoCrop).</li>
 *   <li>Vale a pior das duas (a planta é limitada pelo fator mais fraco).</li>
 *   <li>Estresse hídrico: solo seco ou evapotranspiração muito maior que a chuva tira 15%.</li>
 *   <li>Solo não indicado para a cultura tira 25%.</li>
 * </ol>
 */
public class AptidaoCalculadora {

    static final double UMIDADE_SOLO_SECO = 0.25;
    static final double INDICE_HIDRICO_MINIMO = 0.6;   // chuva / evapotranspiração
    static final double FATOR_ESTRESSE_HIDRICO = 0.85;
    static final double FATOR_SOLO_INADEQUADO = 0.75;

    public ResultadoAno avaliar(Cultura cultura, ClimaAnual clima, TipoSolo solo) {
        double pTemp = cultura.temperatura().pontuar(clima.temperaturaMedia());
        double pChuva = cultura.chuva().pontuar(clima.precipitacao());
        double indiceHidrico = clima.evapotranspiracao() > 0
                ? clima.precipitacao() / clima.evapotranspiracao()
                : 1.0;
        boolean estresseHidrico = clima.umidadeSolo() < UMIDADE_SOLO_SECO || indiceHidrico < INDICE_HIDRICO_MINIMO;
        boolean soloOk = cultura.aceitaSolo(solo);

        double base = Math.min(pTemp, pChuva);
        double fatorHidrico = estresseHidrico ? FATOR_ESTRESSE_HIDRICO : 1.0;
        double fatorSolo = soloOk ? 1.0 : FATOR_SOLO_INADEQUADO;
        int nota = (int) Math.round(100 * base * fatorHidrico * fatorSolo);

        Risco risco = riscoPrincipal(cultura, clima, pTemp, pChuva, estresseHidrico, soloOk);
        return new ResultadoAno(clima.ano(), clima.projetado(), nota, Nivel.daNota(nota), risco,
                motivo(risco, cultura, clima, solo), clima);
    }

    private Risco riscoPrincipal(Cultura c, ClimaAnual clima, double pTemp, double pChuva,
                                 boolean estresseHidrico, boolean soloOk) {
        if (pTemp < 1 && pTemp <= pChuva) {
            return c.temperatura().abaixoDoIdeal(clima.temperaturaMedia()) ? Risco.FRIO : Risco.CALOR;
        }
        if (pChuva < 1) {
            return c.chuva().abaixoDoIdeal(clima.precipitacao()) ? Risco.SECA : Risco.CHUVA_EXCESSIVA;
        }
        if (estresseHidrico) return Risco.SECA;
        if (!soloOk) return Risco.SOLO_INADEQUADO;
        return Risco.NENHUM;
    }

    private String motivo(Risco risco, Cultura c, ClimaAnual clima, TipoSolo solo) {
        String nome = c.nome().toLowerCase(Locale.ROOT);
        return switch (risco) {
            case NENHUM -> "Temperatura e chuva dentro do ideal para " + nome + ".";
            case SECA -> c.chuva().abaixoDoIdeal(clima.precipitacao())
                    ? String.format(Locale.ROOT, "Choveu %d mm, abaixo do ideal para %s (%d a %d mm).",
                            Math.round(clima.precipitacao()), nome,
                            Math.round(c.chuva().idealMin()), Math.round(c.chuva().idealMax()))
                    : "A chuva até deu, mas o solo ficou seco: a planta perderia mais água do que recebeu.";
            case CHUVA_EXCESSIVA -> String.format(Locale.ROOT,
                    "Choveu %d mm, acima do ideal para %s (%d a %d mm).",
                    Math.round(clima.precipitacao()), nome,
                    Math.round(c.chuva().idealMin()), Math.round(c.chuva().idealMax()));
            case CALOR -> String.format(Locale.ROOT,
                    "Temperatura média de %.1f °C, acima do ideal para %s (%.0f a %.0f °C).",
                    clima.temperaturaMedia(), nome, c.temperatura().idealMin(), c.temperatura().idealMax());
            case FRIO -> String.format(Locale.ROOT,
                    "Temperatura média de %.1f °C, abaixo do ideal para %s (%.0f a %.0f °C).",
                    clima.temperaturaMedia(), nome, c.temperatura().idealMin(), c.temperatura().idealMax());
            case SOLO_INADEQUADO -> "O clima ajudou, mas solo " + solo.name().toLowerCase(Locale.ROOT)
                    + " não é o mais indicado para " + nome + ".";
        };
    }
}
