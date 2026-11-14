package br.com.fieldshift.core;

import br.com.fieldshift.core.clima.HttpClimaProvider;
import br.com.fieldshift.core.modelo.ClimaAnual;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpClimaProviderTest {

    private HttpServer servidor;
    private String resposta;

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/clima", ex -> {
            byte[] corpo = resposta.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, corpo.length);
            ex.getResponseBody().write(corpo);
            ex.close();
        });
        servidor.start();
    }

    @AfterEach
    void derrubar() {
        servidor.stop(0);
    }

    private HttpClimaProvider provider() {
        return new HttpClimaProvider("http://127.0.0.1:" + servidor.getAddress().getPort(), new ObjectMapper());
    }

    private static String ano(int ano, boolean projetado) {
        return "{\"ano\":" + ano + ",\"projetado\":" + projetado + ",\"temperaturaMedia\":21,"
                + "\"temperaturaMaxima\":28,\"temperaturaMinima\":15,\"precipitacao\":800,"
                + "\"umidadeSolo\":0.35,\"evapotranspiracao\":1000}";
    }

    @Test
    void leASerieNoFormatoDoContrato() {
        resposta = "[" + ano(2025, false) + "," + ano(2026, true) + "]";
        List<ClimaAnual> serie = provider().serieAnual(-14.86, -40.83, 2025, 2026, 2025);
        assertThat(serie).hasSize(2);
        assertThat(serie.get(1).projetado()).isTrue();
        assertThat(serie.get(0).precipitacao()).isEqualTo(800);
    }

    @Test
    void recusaSerieComAnoFaltando() {
        resposta = "[" + ano(2025, false) + "]";
        assertThatThrownBy(() -> provider().serieAnual(-14.86, -40.83, 2025, 2026, 2025))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("esperado 2");
    }
}
