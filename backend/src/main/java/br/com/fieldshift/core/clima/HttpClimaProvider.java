package br.com.fieldshift.core.clima;

import br.com.fieldshift.core.modelo.ClimaAnual;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Busca o clima no serviço do pessoal dos dados, seguindo o contrato de docs/contrato-dados.md:
 * {@code GET {urlBase}/clima?lat=..&lon=..&inicio=..&fim=..&anoAtual=..} devolvendo uma lista de
 * {@link ClimaAnual} em JSON.
 */
public class HttpClimaProvider implements ClimaProvider {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String urlBase;

    public HttpClimaProvider(String urlBase, ObjectMapper mapper) {
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public List<ClimaAnual> serieAnual(double latitude, double longitude, int anoInicio, int anoFim, int anoAtual) {
        String url = String.format(Locale.ROOT, "%s/clima?lat=%s&lon=%s&inicio=%d&fim=%d&anoAtual=%d",
                urlBase, codificar(latitude), codificar(longitude), anoInicio, anoFim, anoAtual);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("Serviço de clima respondeu " + resp.statusCode() + ": " + resp.body());
            }
            List<ClimaAnual> serie = mapper.readValue(resp.body(), new TypeReference<List<ClimaAnual>>() {});
            validar(serie, anoInicio, anoFim);
            return serie;
        } catch (IOException e) {
            throw new IllegalStateException("Não consegui falar com o serviço de clima em " + urlBase, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta ao serviço de clima interrompida", e);
        }
    }

    static void validar(List<ClimaAnual> serie, int anoInicio, int anoFim) {
        int esperado = anoFim - anoInicio + 1;
        if (serie.size() != esperado) {
            throw new IllegalStateException("Serviço de clima devolveu " + serie.size()
                    + " anos, esperado " + esperado + " (" + anoInicio + " a " + anoFim + ")");
        }
        for (int i = 0; i < serie.size(); i++) {
            if (serie.get(i).ano() != anoInicio + i) {
                throw new IllegalStateException("Anos fora de ordem ou faltando no serviço de clima: esperado "
                        + (anoInicio + i) + ", veio " + serie.get(i).ano());
            }
        }
    }

    private static String codificar(double v) {
        return URLEncoder.encode(String.format(Locale.ROOT, "%.4f", v), StandardCharsets.UTF_8);
    }
}
