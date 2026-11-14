package br.com.fieldshift.core.solo;

import br.com.fieldshift.core.modelo.SoloEstimado;
import br.com.fieldshift.core.modelo.TipoSolo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Leitura da resposta do SoilGrids e classificação da terra. Nenhum teste chama o serviço de verdade. */
class SoilGridsSoloProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Resposta no formato do SoilGrids (valores em g/kg, d_factor 10). */
    private static String resposta(int argila, int areia, int silte) {
        String camada = "{\"name\":\"%s\",\"unit_measure\":{\"d_factor\":10},\"depths\":["
                + "{\"label\":\"0-5cm\",\"values\":{\"mean\":%d}},{\"label\":\"5-15cm\",\"values\":{\"mean\":%d}}]}";
        return "{\"properties\":{\"layers\":["
                + camada.formatted("clay", argila, argila) + ","
                + camada.formatted("sand", areia, areia) + ","
                + camada.formatted("silt", silte, silte) + "]}}";
    }

    @Test
    void leArgilaAreiaESilteEmPorcentagem() {
        AtomicInteger chamadas = new AtomicInteger();
        SoilGridsSoloProvider p = new SoilGridsSoloProvider(MAPPER, url -> {
            chamadas.incrementAndGet();
            assertThat(url).contains("lat=-14.8660", "lon=-40.8390", "property=clay", "property=sand");
            return resposta(272, 556, 172);
        });
        SoloEstimado s = p.estimar(-14.866, -40.839);
        assertThat(s.argila()).isEqualTo(27.2);
        assertThat(s.areia()).isEqualTo(55.6);
        assertThat(s.silte()).isEqualTo(17.2);
        assertThat(s.tipo()).isEqualTo(TipoSolo.MISTO);
        assertThat(s.fonte()).contains("SoilGrids");

        p.estimar(-14.866, -40.839);
        assertThat(chamadas.get()).isEqualTo(1);   // segunda vez vem do cache
    }

    @Test
    void classificaPelaTextura() {
        assertThat(SoilGridsSoloProvider.classificar(10, 80)).isEqualTo(TipoSolo.ARENOSO);
        assertThat(SoilGridsSoloProvider.classificar(45, 30)).isEqualTo(TipoSolo.ARGILOSO);
        assertThat(SoilGridsSoloProvider.classificar(25, 50)).isEqualTo(TipoSolo.MISTO);
    }

    @Test
    void pontoSemDadoViraErroClaro() {
        SoilGridsSoloProvider p = new SoilGridsSoloProvider(MAPPER,
                url -> "{\"properties\":{\"layers\":[{\"name\":\"clay\",\"depths\":[{\"values\":{\"mean\":null}}]}]}}");
        assertThatThrownBy(() -> p.estimar(0, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SoilGrids");
    }
}
