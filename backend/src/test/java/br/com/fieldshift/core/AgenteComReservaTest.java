package br.com.fieldshift.core;

import br.com.fieldshift.core.agente.Agente;
import br.com.fieldshift.core.agente.AgenteComReserva;
import br.com.fieldshift.core.agente.ContextoAgente;
import br.com.fieldshift.core.agente.ContextoEmTexto;
import br.com.fieldshift.core.agente.RespostaAgente;
import br.com.fieldshift.core.clima.MockClimaProvider;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.RecomendacaoService;
import br.com.fieldshift.core.servico.SimuladorService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Escolha entre o agente de IA e o de demonstração. Nenhum teste chama a API de verdade. */
class AgenteComReservaTest {

    private static final Duration LIMITE = Duration.ofSeconds(5);
    private static final Agente RESERVA = (pergunta, ctx) -> "resposta de demonstração";

    private final CulturaRepository culturas = new CulturaRepository(List.of(AptidaoCalculadoraTest.MILHO));
    private final SimuladorService simulador = new SimuladorService(new MockClimaProvider(), culturas,
            new AptidaoCalculadora(), Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC));
    private final Simulacao milho = simulador.simular(-14.866, -40.839, TipoSolo.MISTO, "milho");
    private final ContextoAgente ctx = new ContextoAgente(milho, null,
            new RecomendacaoService(simulador, culturas).recomendar(-14.866, -40.839, TipoSolo.MISTO));

    @Test
    void semIaUsaDemonstracao() {
        RespostaAgente r = new AgenteComReserva(null, RESERVA, LIMITE).responder("oi", ctx);
        assertThat(r.texto()).isEqualTo("resposta de demonstração");
        assertThat(r.demonstracao()).isTrue();
    }

    @Test
    void comIaFuncionandoUsaAIa() {
        Agente ia = (pergunta, c) -> "  resposta da IA  ";
        RespostaAgente r = new AgenteComReserva(ia, RESERVA, LIMITE).responder("oi", ctx);
        assertThat(r.texto()).isEqualTo("resposta da IA");
        assertThat(r.demonstracao()).isFalse();
    }

    @Test
    void iaComErroCaiNaDemonstracao() {
        Agente ia = (pergunta, c) -> {
            throw new IllegalStateException("API fora do ar");
        };
        RespostaAgente r = new AgenteComReserva(ia, RESERVA, LIMITE).responder("oi", ctx);
        assertThat(r.texto()).isEqualTo("resposta de demonstração");
        assertThat(r.demonstracao()).isTrue();
    }

    @Test
    void iaLentaCaiNaDemonstracao() {
        Agente ia = (pergunta, c) -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "tarde demais";
        };
        long inicio = System.nanoTime();
        RespostaAgente r = new AgenteComReserva(ia, RESERVA, Duration.ofMillis(200)).responder("oi", ctx);
        assertThat(r.demonstracao()).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void iaComRespostaVaziaCaiNaDemonstracao() {
        Agente ia = (pergunta, c) -> "   ";
        assertThat(new AgenteComReserva(ia, RESERVA, LIMITE).responder("oi", ctx).demonstracao()).isTrue();
    }

    @Test
    void semChaveNaConfiguracaoNaoLigaAIa() {
        assertThat(AgenteComReserva.deConfiguracao("", "claude-sonnet-5-5", true).temIa()).isFalse();
        assertThat(AgenteComReserva.deConfiguracao(null, "claude-sonnet-5-5", true).temIa()).isFalse();
        assertThat(AgenteComReserva.deConfiguracao("  ", "claude-sonnet-5-5", true).responder("oi", ctx).demonstracao())
                .isTrue();
    }

    @Test
    void comChaveNaConfiguracaoLigaAIa() {
        // Só monta o cliente; nenhuma chamada é feita.
        assertThat(AgenteComReserva.deConfiguracao("chave-de-teste", "claude-sonnet-5-5", true).temIa()).isTrue();
    }

    @Test
    void contextoEmTextoLevaOs30AnosResumosERanking() {
        String texto = ContextoEmTexto.de(ctx);
        assertThat(texto).contains("Milho", "2011 | aconteceu", "2025 | aconteceu", "2026 | previsão",
                "2040 | previsão", "Resumo dos anos que já aconteceram", "Resumo dos anos de previsão", "1º Milho");
        assertThat(texto.lines().filter(l -> l.matches("^20\\d\\d \\| .*")).count()).isEqualTo(30);
    }
}
