package br.com.fieldshift.core.solo;

import br.com.fieldshift.core.modelo.SoloEstimado;
import br.com.fieldshift.core.modelo.TipoSolo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Estima o tipo de terra de um ponto pelo SoilGrids (ISRIC, https://soilgrids.org): teor de argila,
 * areia e silte na camada de 0 a 30 cm. É um mapa global de 250 m; serve como sugestão para o
 * agricultor que não sabe o tipo da terra, não substitui análise de solo.
 */
public class SoilGridsSoloProvider {

    private static final String URL_BASE = "https://rest.isric.org/soilgrids/v2.0/properties/query";
    public static final String FONTE = "SoilGrids (ISRIC), camada de 0 a 30 cm";

    private final ObjectMapper mapper;
    private final UnaryOperator<String> buscador;
    private final Map<String, SoloEstimado> cache = new ConcurrentHashMap<>();

    public SoilGridsSoloProvider(ObjectMapper mapper) {
        this(mapper, buscadorHttp());
    }

    /** @param buscador recebe a URL e devolve o corpo; existe para os testes não chamarem o serviço */
    public SoilGridsSoloProvider(ObjectMapper mapper, UnaryOperator<String> buscador) {
        this.mapper = mapper;
        this.buscador = buscador;
    }

    public SoloEstimado estimar(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenada fora do mapa: " + latitude + ", " + longitude);
        }
        String chave = String.format(Locale.ROOT, "%.3f,%.3f", latitude, longitude);
        if (cache.size() > 500) {
            cache.clear();
        }
        return cache.computeIfAbsent(chave, k -> buscar(latitude, longitude));
    }

    private SoloEstimado buscar(double latitude, double longitude) {
        String url = String.format(Locale.ROOT,
                "%s?lon=%.4f&lat=%.4f&property=clay&property=sand&property=silt"
                        + "&depth=0-5cm&depth=5-15cm&depth=15-30cm&value=mean",
                URL_BASE, longitude, latitude);
        try {
            return ler(mapper.readTree(buscador.apply(url)));
        } catch (IOException e) {
            throw new IllegalStateException("O SoilGrids respondeu em um formato que não entendi", e);
        }
    }

    static SoloEstimado ler(JsonNode resposta) {
        double argila = media(resposta, "clay");
        double areia = media(resposta, "sand");
        double silte = media(resposta, "silt");
        return new SoloEstimado(classificar(argila, areia), argila, areia, silte, FONTE);
    }

    /** Regra simples de textura: muita areia = arenoso; muita argila = argiloso; o resto = misto. */
    static TipoSolo classificar(double argila, double areia) {
        if (areia >= 70) return TipoSolo.ARENOSO;
        if (argila >= 35) return TipoSolo.ARGILOSO;
        return TipoSolo.MISTO;
    }

    /** Média das camadas, em %. O SoilGrids entrega em g/kg; dividir pelo d_factor resulta em %. */
    private static double media(JsonNode resposta, String propriedade) {
        for (JsonNode camada : resposta.path("properties").path("layers")) {
            if (!propriedade.equals(camada.path("name").asText())) continue;
            double fator = camada.path("unit_measure").path("d_factor").asDouble(10);
            double soma = 0;
            int n = 0;
            for (JsonNode profundidade : camada.path("depths")) {
                JsonNode valor = profundidade.path("values").path("mean");
                if (valor.isNumber()) {
                    soma += valor.asDouble() / fator;
                    n++;
                }
            }
            if (n > 0) {
                return Math.round(soma / n * 10) / 10.0;
            }
        }
        throw new IllegalStateException("O SoilGrids não tem dado de solo para este ponto");
    }

    private static UnaryOperator<String> buscadorHttp() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return url -> {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            try {
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new IllegalStateException("O SoilGrids respondeu " + resp.statusCode());
                }
                return resp.body();
            } catch (IOException e) {
                throw new IllegalStateException("Não consegui falar com o SoilGrids", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Consulta ao SoilGrids interrompida", e);
            }
        };
    }
}
