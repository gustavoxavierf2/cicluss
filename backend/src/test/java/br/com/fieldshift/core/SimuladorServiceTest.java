package br.com.fieldshift.core;

import br.com.fieldshift.core.clima.MockClimaProvider;
import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.RecomendacaoService;
import br.com.fieldshift.core.servico.SimuladorService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimuladorServiceTest {

    static final double LAT = -14.866;
    static final double LON = -40.839;

    private final CulturaRepository culturas = new CulturaRepository(List.of(AptidaoCalculadoraTest.MILHO));
    private final Clock relogio = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC);
    private final SimuladorService simulador =
            new SimuladorService(new MockClimaProvider(), culturas, new AptidaoCalculadora(), relogio);

    @Test
    void quinzeAnosParaTrasEQuinzeParaFrente() {
        Simulacao s = simulador.simular(LAT, LON, TipoSolo.MISTO, "milho");

        assertThat(s.anoAtual()).isEqualTo(2025);
        assertThat(s.anos()).hasSize(30);
        assertThat(s.anos().get(0).ano()).isEqualTo(2011);
        assertThat(s.anos().get(29).ano()).isEqualTo(2040);
        assertThat(s.anos().stream().filter(a -> a.projetado()).count()).isEqualTo(15);
        assertThat(s.resumoPassado().total()).isEqualTo(15);
        assertThat(s.resumoFuturo().total()).isEqualTo(15);
    }

    @Test
    void mesmoPontoDaSempreOMesmoResultado() {
        Simulacao a = simulador.simular(LAT, LON, TipoSolo.MISTO, "milho");
        Simulacao b = simulador.simular(LAT, LON, TipoSolo.MISTO, "milho");
        assertThat(a.anos()).isEqualTo(b.anos());
    }

    @Test
    void culturaDesconhecidaEhErro() {
        assertThatThrownBy(() -> simulador.simular(LAT, LON, TipoSolo.MISTO, "cacau"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coordenadaForaDoMapaEhErro() {
        assertThatThrownBy(() -> simulador.simular(120, LON, TipoSolo.MISTO, "milho"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rankingNumeraAsPosicoes() {
        List<ItemRanking> ranking = new RecomendacaoService(simulador, culturas).recomendar(LAT, LON, TipoSolo.MISTO);
        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).posicao()).isEqualTo(1);
        assertThat(ranking.get(0).totalAnos()).isEqualTo(15);
    }
}
