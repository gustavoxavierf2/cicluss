package br.com.fieldshift.core;

import br.com.fieldshift.core.agente.AgenteDemonstracao;
import br.com.fieldshift.core.agente.ContextoAgente;
import br.com.fieldshift.core.clima.MockClimaProvider;
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

class AgenteDemonstracaoTest {

    private final CulturaRepository culturas = new CulturaRepository(List.of(AptidaoCalculadoraTest.MILHO));
    private final SimuladorService simulador = new SimuladorService(new MockClimaProvider(), culturas,
            new AptidaoCalculadora(), Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC));
    private final Simulacao milho = simulador.simular(-14.866, -40.839, TipoSolo.MISTO, "milho");
    private final ContextoAgente ctx = new ContextoAgente(milho, null,
            new RecomendacaoService(simulador, culturas).recomendar(-14.866, -40.839, TipoSolo.MISTO));
    private final AgenteDemonstracao agente = new AgenteDemonstracao();

    @Test
    void perguntaSobreUmAnoExplicaAqueleAno() {
        assertThat(agente.responder("O que aconteceu em 2015?", ctx)).startsWith("Em 2015,").contains("milho");
    }

    @Test
    void anoForaDoIntervaloAvisa() {
        assertThat(agente.responder("e em 1990?", ctx)).contains("2011").contains("2040");
    }

    @Test
    void perguntaDeRecomendacaoTrazORanking() {
        assertThat(agente.responder("Qual a melhor cultura pra mim?", ctx)).contains("1º Milho");
    }

    @Test
    void perguntaSemTemaDaUmResumo() {
        assertThat(agente.responder("oi", ctx)).startsWith("Nos últimos 15 anos");
    }
}
