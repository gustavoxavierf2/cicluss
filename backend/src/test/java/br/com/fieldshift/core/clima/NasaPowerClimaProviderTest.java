package br.com.fieldshift.core.clima;

import br.com.fieldshift.core.modelo.ClimaAnual;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Leitura da resposta da NASA POWER e projeção por tendência. Nenhum teste chama a NASA de verdade. */
class NasaPowerClimaProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int ANO_ATUAL = 2025;
    private static final int PRIMEIRO = ANO_ATUAL - NasaPowerClimaProvider.ANOS_DE_HISTORICO + 1;

    private final AtomicInteger chamadas = new AtomicInteger();
    private String ultimaUrl;

    /** Resposta no formato da NASA: temperatura sobe 0,1 °C por ano; chuva cai 10 mm por ano. */
    private static String resposta(boolean comAnual) {
        ObjectNode raiz = MAPPER.createObjectNode();
        ObjectNode p = raiz.putObject("properties").putObject("parameter");
        for (String nome : List.of("T2M", "T2M_MAX", "T2M_MIN", "PRECTOTCORR_SUM", "GWETROOT", "EVLAND")) {
            ObjectNode serie = p.putObject(nome);
            for (int ano = PRIMEIRO; ano <= ANO_ATUAL; ano++) {
                int k = ano - PRIMEIRO;
                double mensal = switch (nome) {
                    case "T2M" -> 20 + 0.1 * k;
                    case "T2M_MAX" -> 30;
                    case "T2M_MIN" -> 12;
                    case "PRECTOTCORR_SUM" -> (1200 - 10.0 * k) / 12;
                    case "GWETROOT" -> 0.5;
                    default -> 2.0;
                };
                for (int mes = 1; mes <= 12; mes++) {
                    serie.put(String.format(Locale.ROOT, "%d%02d", ano, mes), mensal);
                }
                double anual = nome.equals("PRECTOTCORR_SUM") ? mensal * 12 : mensal;
                serie.put(ano + "13", comAnual ? anual : -999.0);
            }
        }
        return raiz.toString();
    }

    private NasaPowerClimaProvider provider(String corpo) {
        return new NasaPowerClimaProvider(MAPPER, url -> {
            chamadas.incrementAndGet();
            ultimaUrl = url;
            return corpo;
        });
    }

    @Test
    void devolveUmAnoPorAnoComPassadoObservadoEFuturoProjetado() {
        List<ClimaAnual> serie = provider(resposta(true)).serieAnual(-14.866, -40.839, 2011, 2040, ANO_ATUAL);
        assertThat(serie).hasSize(30);
        assertThat(serie).extracting(ClimaAnual::ano).startsWith(2011).endsWith(2040);
        assertThat(serie.stream().filter(c -> !c.projetado())).hasSize(15);
        assertThat(serie.stream().filter(ClimaAnual::projetado)).allMatch(c -> c.ano() > ANO_ATUAL);
    }

    @Test
    void leOsValoresAnuaisDaNasa() {
        ClimaAnual c = provider(resposta(true)).serieAnual(-14.866, -40.839, 2025, 2025, ANO_ATUAL).get(0);
        assertThat(c.temperaturaMedia()).isEqualTo(22.9);
        assertThat(c.temperaturaMaxima()).isEqualTo(30.0);
        assertThat(c.temperaturaMinima()).isEqualTo(12.0);
        assertThat(c.precipitacao()).isEqualTo(910);
        assertThat(c.umidadeSolo()).isEqualTo(0.5);
        assertThat(c.evapotranspiracao()).isEqualTo(730);   // 2 mm/dia × 365 dias
    }

    @Test
    void semValorAnualUsaOsMeses() {
        ClimaAnual c = provider(resposta(false)).serieAnual(-14.866, -40.839, 2025, 2025, ANO_ATUAL).get(0);
        assertThat(c.temperaturaMedia()).isEqualTo(22.9);
        assertThat(c.precipitacao()).isEqualTo(910);
    }

    @Test
    void futuroSegueATendenciaDoHistorico() {
        List<ClimaAnual> serie = provider(resposta(true)).serieAnual(-14.866, -40.839, 2026, 2030, ANO_ATUAL);
        assertThat(serie.get(0).temperaturaMedia()).isEqualTo(23.0);   // +0,1 °C por ano
        assertThat(serie.get(4).temperaturaMedia()).isEqualTo(23.4);
        assertThat(serie.get(0).precipitacao()).isEqualTo(900);        // −10 mm por ano
        assertThat(serie.get(4).precipitacao()).isEqualTo(860);
        assertThat(serie.get(4).umidadeSolo()).isEqualTo(0.5);         // sem tendência, fica igual
    }

    @Test
    void pedeOsParametrosCertosEGuardaEmCache() {
        NasaPowerClimaProvider p = provider(resposta(true));
        p.serieAnual(-14.866, -40.839, 2011, 2040, ANO_ATUAL);
        p.serieAnual(-14.866, -40.839, 2011, 2040, ANO_ATUAL);
        p.serieAnual(-14.8661, -40.8391, 2011, 2040, ANO_ATUAL);   // mesmo ponto na prática
        assertThat(chamadas.get()).isEqualTo(1);
        assertThat(ultimaUrl)
                .startsWith("https://power.larc.nasa.gov/api/temporal/monthly/point?")
                .contains("parameters=T2M,T2M_MAX,T2M_MIN,PRECTOTCORR_SUM,GWETROOT,EVLAND")
                .contains("community=AG", "latitude=-14.8660", "longitude=-40.8390",
                        "start=" + PRIMEIRO, "end=" + ANO_ATUAL);
    }

    @Test
    void respostaSemDadosViraErroClaro() {
        assertThatThrownBy(() -> provider("{\"messages\":[\"erro\"]}").serieAnual(-14.866, -40.839, 2011, 2040, ANO_ATUAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NASA POWER");
    }
}
