package br.com.fieldshift.core;

import br.com.fieldshift.core.clima.MockClimaProvider;
import br.com.fieldshift.core.modelo.ComparacaoEstrategias;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Economia;
import br.com.fieldshift.core.modelo.Estrategia;
import br.com.fieldshift.core.modelo.Faixa;
import br.com.fieldshift.core.modelo.GrupoRotacao;
import br.com.fieldshift.core.modelo.IndicadorSolo;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.TipoSolo;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.EstrategiaService;
import br.com.fieldshift.core.servico.SimuladorService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Comparação de estratégias de rotação: sempre inclui o que o agricultor faz hoje e olha a saúde do solo. */
class EstrategiaServiceTest {

    private static final List<TipoSolo> TODOS = List.of(TipoSolo.ARENOSO, TipoSolo.ARGILOSO, TipoSolo.MISTO);

    private static Cultura cultura(String id, GrupoRotacao grupo, int ciclo) {
        // Faixas largas: todas vão bem no clima; o que muda é o grupo e o ciclo.
        return new Cultura(id, id, new Faixa(5, 40, 0, 50), new Faixa(100, 3000, 50, 6000), TODOS, ciclo, "teste",
                grupo, new Economia(1000, 2.0, 500, "teste"));
    }

    private static final Cultura MILHO = cultura("milho", GrupoRotacao.GRAMINEA, 120);
    private static final Cultura SORGO = cultura("sorgo", GrupoRotacao.GRAMINEA, 110);
    private static final Cultura FEIJAO = cultura("feijao", GrupoRotacao.LEGUMINOSA, 90);
    private static final Cultura MANDIOCA = cultura("mandioca", GrupoRotacao.RAIZ, 360);
    private static final Cultura CAFE = cultura("cafe", GrupoRotacao.PERENE, 365);

    private final CulturaRepository culturas = new CulturaRepository(List.of(MILHO, SORGO, FEIJAO, MANDIOCA, CAFE));
    private final SimuladorService simulador = new SimuladorService(new MockClimaProvider(), culturas,
            new AptidaoCalculadora(), Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC));
    private final EstrategiaService servico = new EstrategiaService(simulador, culturas);

    private ComparacaoEstrategias comparar(String plantada, String teste) {
        return servico.comparar(-14.866, -40.839, TipoSolo.MISTO, 2.0, plantada, teste);
    }

    private static List<String> ids(Estrategia e) {
        return e.sequencia().stream().map(Cultura::id).toList();
    }

    @Test
    void comparaPeloMenosTresEstrategiasEAPrimeiraEOQueEleFazHoje() {
        ComparacaoEstrategias c = comparar("milho", null);
        assertThat(c.estrategias()).hasSizeGreaterThanOrEqualTo(3);
        Estrategia atual = c.estrategias().get(0);
        assertThat(atual.atual()).isTrue();
        assertThat(ids(atual)).containsExactly("milho");
        assertThat(c.estrategias().stream().filter(Estrategia::atual)).hasSize(1);
    }

    @Test
    void montaRotacaoComLeguminosaEDeTresGrupos() {
        ComparacaoEstrategias c = comparar("milho", null);
        assertThat(c.estrategias()).extracting(EstrategiaServiceTest::ids)
                .contains(List.of("milho", "feijao"), List.of("milho", "feijao", "mandioca"));
    }

    @Test
    void culturaDeTesteViraUmaEstrategiaAMais() {
        ComparacaoEstrategias c = comparar("milho", "sorgo");
        assertThat(c.estrategias()).extracting(EstrategiaServiceTest::ids).contains(List.of("milho", "sorgo"));
        assertThat(c.estrategias()).hasSize(4);
    }

    @Test
    void cadaEstrategiaCobreOs15AnosObservadosAlternandoAsCulturas() {
        Estrategia rotacao = comparar("milho", null).estrategias().stream()
                .filter(e -> ids(e).equals(List.of("milho", "feijao"))).findFirst().orElseThrow();
        assertThat(rotacao.anos()).hasSize(15);
        assertThat(rotacao.anos().get(0).ano()).isEqualTo(2011);
        assertThat(rotacao.anos()).extracting(Estrategia.AnoEstrategia::cultura)
                .startsWith("milho", "feijao", "milho", "feijao");
        assertThat(rotacao.resumo().total()).isEqualTo(15);
        assertThat(rotacao.saldoTotal()).isNotNull();
    }

    @Test
    void monocultivoPerdeNosSinaisDeSolo() {
        List<IndicadorSolo> mono = EstrategiaService.indicadoresDeSolo(List.of(MILHO));
        assertThat(mono).extracting(IndicadorSolo::nome)
                .containsExactly("Nitrogênio", "Pragas e doenças", "Terra coberta", "Palha e matéria orgânica");
        assertThat(mono.get(0).nivel()).isEqualTo(Nivel.RUIM);   // sem leguminosa
        assertThat(mono.get(1).nivel()).isEqualTo(Nivel.RUIM);   // mesma planta todo ano
        assertThat(mono.get(2).nivel()).isEqualTo(Nivel.RUIM);   // 8 meses descoberta

        List<IndicadorSolo> tres = EstrategiaService.indicadoresDeSolo(List.of(MILHO, FEIJAO, MANDIOCA));
        assertThat(tres.get(0).nivel()).isEqualTo(Nivel.BOM);
        assertThat(tres.get(1).nivel()).isEqualTo(Nivel.BOM);
        assertThat(tres.get(2).nivel()).isEqualTo(Nivel.ATENCAO);
        assertThat(tres.get(3).nivel()).isEqualTo(Nivel.BOM);
    }

    @Test
    void trocarPorCulturaDoMesmoGrupoQuebraPoucoAsPragas() {
        List<IndicadorSolo> lista = EstrategiaService.indicadoresDeSolo(List.of(MILHO, SORGO));
        assertThat(lista.get(1).nivel()).isEqualTo(Nivel.ATENCAO);
    }

    @Test
    void mesesDescobertoVemDoCicloDasCulturas() {
        ComparacaoEstrategias c = comparar("milho", null);
        assertThat(c.estrategias().get(0).mesesDescoberto()).isEqualTo(8);   // milho: 4 meses no chão
        Estrategia tres = c.estrategias().stream().filter(e -> e.sequencia().size() == 3).findFirst().orElseThrow();
        assertThat(tres.mesesDescoberto()).isEqualTo(6);                     // (4 + 3 + 12) / 3 ≈ 6 cobertos
    }

    @Test
    void comClimaParecidoIndicaAQueCuidaMelhorDaTerra() {
        ComparacaoEstrategias c = comparar("milho", null);
        Estrategia indicada = c.estrategias().stream()
                .filter(e -> e.id().equals(c.recomendadaId())).findFirst().orElseThrow();
        assertThat(indicada.atual()).isFalse();
        assertThat(indicada.pontosSolo()).isEqualTo(
                c.estrategias().stream().mapToInt(Estrategia::pontosSolo).max().orElseThrow());
        assertThat(c.porque()).contains("cuida melhor da terra");
        assertThat(c.hectares()).isEqualTo(2.0);
    }

    @Test
    void culturaPereneTambemGanhaAlternativasDeRotacao() {
        ComparacaoEstrategias c = comparar("cafe", null);
        assertThat(ids(c.estrategias().get(0))).containsExactly("cafe");
        assertThat(c.estrategias()).hasSizeGreaterThanOrEqualTo(3);
    }
}
