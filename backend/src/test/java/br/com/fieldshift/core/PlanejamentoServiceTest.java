package br.com.fieldshift.core;

import br.com.fieldshift.core.clima.MockClimaProvider;
import br.com.fieldshift.core.modelo.AnoPlano;
import br.com.fieldshift.core.modelo.Cenario;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Economia;
import br.com.fieldshift.core.modelo.Faixa;
import br.com.fieldshift.core.modelo.GrupoRotacao;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.Planejamento;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Risco;
import br.com.fieldshift.core.modelo.Safra;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.PlanejamentoService;
import br.com.fieldshift.core.servico.SafraCalculadora;
import br.com.fieldshift.core.servico.SimuladorService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Estimativa de safra, cenário com clima ajustado e planejamento com rotação. */
class PlanejamentoServiceTest {

    private static final List<TipoSolo> TODOS = List.of(TipoSolo.ARENOSO, TipoSolo.ARGILOSO, TipoSolo.MISTO);

    private static Cultura cultura(String id, GrupoRotacao grupo, double preco) {
        // Faixas bem largas: todas vão bem em qualquer ano; o que muda entre elas é o dinheiro.
        return new Cultura(id, id, new Faixa(5, 40, 0, 50), new Faixa(100, 3000, 50, 6000), TODOS, 120, "teste",
                grupo, new Economia(1000, preco, 500, "teste"));
    }

    private static final Cultura MILHO = cultura("milho", GrupoRotacao.GRAMINEA, 2.0);
    private static final Cultura SORGO = cultura("sorgo", GrupoRotacao.GRAMINEA, 9.0);
    private static final Cultura FEIJAO = cultura("feijao", GrupoRotacao.LEGUMINOSA, 4.0);
    private static final Cultura MANDIOCA = cultura("mandioca", GrupoRotacao.RAIZ, 3.0);
    private static final Cultura CAFE = cultura("cafe", GrupoRotacao.PERENE, 20.0);

    private final CulturaRepository culturas = new CulturaRepository(List.of(MILHO, SORGO, FEIJAO, MANDIOCA, CAFE));
    private final SimuladorService simulador = new SimuladorService(new MockClimaProvider(), culturas,
            new AptidaoCalculadora(), Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC));
    private final PlanejamentoService servico = new PlanejamentoService(simulador, culturas);

    private Planejamento planejar(String culturaId, int anos) {
        return servico.planejar(-14.866, -40.839, TipoSolo.MISTO, 2.0, culturaId, anos);
    }

    // ---------- safra ----------

    @Test
    void safraEscalaComNotaEHectares() {
        ResultadoAno ano = new ResultadoAno(2030, true, 50, Nivel.ATENCAO, Risco.SECA, "", null);
        Safra s = new SafraCalculadora().estimar(MILHO, ano, 2.0);
        assertThat(s.producaoKg()).isEqualTo(1000);   // 2 ha × 1000 kg/ha × 50%
        assertThat(s.receita()).isEqualTo(2000);       // × R$ 2,00
        assertThat(s.custo()).isEqualTo(1000);         // 2 ha × R$ 500
        assertThat(s.saldo()).isEqualTo(1000);
    }

    @Test
    void culturaSemValoresNaoTemSafra() {
        ResultadoAno ano = new ResultadoAno(2030, true, 50, Nivel.ATENCAO, Risco.SECA, "", null);
        assertThat(new SafraCalculadora().estimar(AptidaoCalculadoraTest.MILHO, ano, 2.0)).isNull();
    }

    @Test
    void simulacaoSoTrazSafraQuandoHaHectares() {
        assertThat(simulador.simular(-14.866, -40.839, TipoSolo.MISTO, "milho").safras()).isEmpty();
        Simulacao s = simulador.simular(-14.866, -40.839, TipoSolo.MISTO, "milho", 3.0);
        assertThat(s.safras()).hasSize(30);
        assertThat(s.safras().get(0).ano()).isEqualTo(s.anos().get(0).ano());
        assertThat(s.hectares()).isEqualTo(3.0);
    }

    // ---------- cenário ----------

    @Test
    void cenarioUsaOsValoresAjustados() {
        Cenario c = simulador.cenario(-14.866, -40.839, TipoSolo.MISTO, "milho", 1.0, 2025, 60.0 - 15, 30.0, 0.1);
        assertThat(c.resultado().clima().temperaturaMedia()).isEqualTo(45.0);
        assertThat(c.resultado().clima().precipitacao()).isEqualTo(30.0);
        assertThat(c.resultado().clima().umidadeSolo()).isEqualTo(0.1);
        assertThat(c.resultado().nivel()).isEqualTo(Nivel.RUIM);
        assertThat(c.safra().producaoKg()).isZero();
    }

    @Test
    void cenarioSemAjusteRepeteOAnoBase() {
        Simulacao s = simulador.simular(-14.866, -40.839, TipoSolo.MISTO, "milho");
        ResultadoAno original = s.anos().stream().filter(a -> a.ano() == 2025).findFirst().orElseThrow();
        Cenario c = simulador.cenario(-14.866, -40.839, TipoSolo.MISTO, "milho", null, 2025, null, null, null);
        assertThat(c.resultado()).isEqualTo(original);
        assertThat(c.safra()).isNull();
    }

    @Test
    void cenarioRecusaValoresAbsurdos() {
        assertThatThrownBy(() -> simulador.cenario(-14.866, -40.839, TipoSolo.MISTO, "milho", null, 2025, 99.0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> simulador.cenario(-14.866, -40.839, TipoSolo.MISTO, "milho", null, 1900, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- planejamento ----------

    @Test
    void planoComecaNoAnoSeguinteComACulturaEscolhida() {
        Planejamento p = planejar("milho", 3);
        assertThat(p.anos()).extracting(AnoPlano::ano).containsExactly(2026, 2027, 2028);
        assertThat(p.anos().get(0).cultura().id()).isEqualTo("milho");
        assertThat(p.anos().get(0).principal()).isTrue();
    }

    @Test
    void naoRepeteOMesmoGrupoEmAnosSeguidos() {
        Planejamento p = planejar("milho", 5);
        for (int i = 1; i < p.anos().size(); i++) {
            assertThat(p.anos().get(i).cultura().grupoRotacao())
                    .isNotEqualTo(p.anos().get(i - 1).cultura().grupoRotacao());
        }
        // A escolhida volta em anos alternados.
        assertThat(p.anos()).extracting(a -> a.cultura().id())
                .containsExactly("milho", "feijao", "milho", "feijao", "milho");
    }

    @Test
    void rotacaoEscolheAMaiorSobraEntreOutrosGrupos() {
        // Sorgo paga mais, mas é do mesmo grupo do milho; café paga mais ainda, mas é perene.
        AnoPlano rotacao = planejar("milho", 3).anos().get(1);
        assertThat(rotacao.cultura().id()).isEqualTo("feijao");
        assertThat(rotacao.principal()).isFalse();
    }

    @Test
    void culturaPereneNaoEntraEmRotacao() {
        Planejamento p = planejar("cafe", 5);
        assertThat(p.anos()).allMatch(a -> a.cultura().id().equals("cafe"));
    }

    @Test
    void saldoTotalSomaOsAnosEContaRiscos() {
        Planejamento p = planejar("milho", 3);
        assertThat(p.saldoTotal()).isEqualTo(p.anos().stream().mapToLong(a -> a.safra().saldo()).sum());
        assertThat(p.anosDeRisco()).isEqualTo(
                (int) p.anos().stream().filter(a -> a.resultado().nivel() != Nivel.BOM).count());
        assertThat(p.hectares()).isEqualTo(2.0);
        assertThat(p.resumo()).contains("Plano para 3 anos");
        assertThat(p.avisos()).isNotEmpty();
    }

    @Test
    void soAceita1ou3ou5Anos() {
        assertThatThrownBy(() -> planejar("milho", 2)).isInstanceOf(IllegalArgumentException.class);
        assertThat(planejar("milho", 1).anos()).hasSize(1);
    }

    @Test
    void anoRuimGanhaPlanoBECuidado() {
        // Café com faixa estreita vai mal neste clima; o plano precisa avisar e apontar a reserva.
        Cultura cafeExigente = new Cultura("cafe", "cafe", new Faixa(15, 16, 14, 17), new Faixa(2500, 3000, 2400, 6000),
                TODOS, 365, "teste", GrupoRotacao.RAIZ, new Economia(1000, 20, 500, "teste"));
        CulturaRepository repo = new CulturaRepository(List.of(MILHO, cafeExigente));
        SimuladorService sim = new SimuladorService(new MockClimaProvider(), repo, new AptidaoCalculadora(),
                Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC));
        Planejamento p = new PlanejamentoService(sim, repo).planejar(-14.866, -40.839, TipoSolo.MISTO, 1.0, "cafe", 1);
        AnoPlano ano = p.anos().get(0);
        assertThat(ano.resultado().nivel()).isEqualTo(Nivel.RUIM);
        assertThat(ano.planoB().id()).isEqualTo("milho");
        assertThat(ano.cuidado()).isNotBlank();
        assertThat(p.avisos()).anyMatch(a -> a.contains("vai mal"));
        assertThat(p.avisos()).anyMatch(a -> a.contains("prejuízo"));
    }
}
