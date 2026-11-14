package br.com.fieldshift.core;

import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Faixa;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Risco;
import br.com.fieldshift.core.modelo.TipoSolo;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AptidaoCalculadoraTest {

    static final Cultura MILHO = new Cultura("milho", "Milho",
            new Faixa(18, 33, 10, 47), new Faixa(600, 1200, 400, 1800),
            List.of(TipoSolo.ARGILOSO, TipoSolo.MISTO), 120, "teste");

    private final AptidaoCalculadora calc = new AptidaoCalculadora();

    static ClimaAnual clima(double temp, double chuva, double umidade, double et) {
        return new ClimaAnual(2020, false, temp, temp + 7, temp - 6, chuva, umidade, et);
    }

    @Test
    void anoIdealDaNotaCem() {
        ResultadoAno r = calc.avaliar(MILHO, clima(24, 900, 0.4, 1000), TipoSolo.MISTO);
        assertThat(r.nota()).isEqualTo(100);
        assertThat(r.nivel()).isEqualTo(Nivel.BOM);
        assertThat(r.risco()).isEqualTo(Risco.NENHUM);
    }

    @Test
    void poucaChuvaViraSeca() {
        ResultadoAno r = calc.avaliar(MILHO, clima(24, 450, 0.2, 1100), TipoSolo.MISTO);
        assertThat(r.risco()).isEqualTo(Risco.SECA);
        assertThat(r.nivel()).isEqualTo(Nivel.RUIM);
        assertThat(r.motivo()).contains("450 mm");
    }

    @Test
    void chuvaDemaisViraChuvaExcessiva() {
        ResultadoAno r = calc.avaliar(MILHO, clima(24, 1500, 0.5, 1000), TipoSolo.MISTO);
        assertThat(r.risco()).isEqualTo(Risco.CHUVA_EXCESSIVA);
        assertThat(r.nota()).isEqualTo(50);
    }

    @Test
    void temperaturaAltaViraCalor() {
        ResultadoAno r = calc.avaliar(MILHO, clima(40, 900, 0.4, 1000), TipoSolo.MISTO);
        assertThat(r.risco()).isEqualTo(Risco.CALOR);
        assertThat(r.nivel()).isEqualTo(Nivel.ATENCAO);
    }

    @Test
    void soloSecoComChuvaOkTiraQuinzePorCento() {
        ResultadoAno r = calc.avaliar(MILHO, clima(24, 700, 0.1, 1000), TipoSolo.MISTO);
        assertThat(r.nota()).isEqualTo(85);
        assertThat(r.risco()).isEqualTo(Risco.SECA);
    }

    @Test
    void soloNaoIndicadoTiraVinteECincoPorCento() {
        ResultadoAno r = calc.avaliar(MILHO, clima(24, 900, 0.4, 1000), TipoSolo.ARENOSO);
        assertThat(r.nota()).isEqualTo(75);
        assertThat(r.risco()).isEqualTo(Risco.SOLO_INADEQUADO);
    }

    @Test
    void naoSeiOSoloNaoPenaliza() {
        ResultadoAno r = calc.avaliar(MILHO, clima(24, 900, 0.4, 1000), TipoSolo.NAO_SEI);
        assertThat(r.nota()).isEqualTo(100);
    }
}
