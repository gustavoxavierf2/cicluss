package br.com.fieldshift.core.clima;

import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.Trimestre;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

/**
 * Clima real da NASA POWER (https://power.larc.nasa.gov), API mensal por ponto, comunidade AG.
 *
 * Anos observados: valores anuais da própria NASA.
 *   - temperaturaMedia      T2M (média do ano, °C)
 *   - temperaturaMaxima     média das 12 máximas mensais de T2M_MAX (°C)
 *   - temperaturaMinima     média das 12 mínimas mensais de T2M_MIN (°C)
 *   - precipitacao          PRECTOTCORR_SUM (soma do ano, mm)
 *   - umidadeSolo           GWETROOT (umidade da zona de raiz, 0 a 1)
 *   - evapotranspiracao     EVLAND (mm/dia, média do ano) × dias do ano
 *   - trimestres            chuva (PRECTOTCORR_SUM) e temperatura (T2M) dos meses de cada trimestre
 *
 * Anos futuros: a NASA POWER não tem previsão. A projeção aqui é a TENDÊNCIA (reta de mínimos
 * quadrados) dos últimos {@value #ANOS_DE_HISTORICO} anos de cada variável, esticada para frente.
 * Não é modelo climático: não prevê seca ou enchente de um ano específico. Os trimestres dos anos
 * futuros repetem a distribuição média do histórico (quanto do ano costuma chover em cada época).
 */
public class NasaPowerClimaProvider implements ClimaProvider {

    static final int ANOS_DE_HISTORICO = 30;
    private static final String URL_BASE = "https://power.larc.nasa.gov/api/temporal/monthly/point";
    private static final String PARAMETROS = "T2M,T2M_MAX,T2M_MIN,PRECTOTCORR_SUM,GWETROOT,EVLAND";
    private static final double SEM_DADO = -999.0;
    private static final int MAXIMO_EM_CACHE = 500;

    /** Busca o corpo de uma URL. Existe para os testes não chamarem a NASA de verdade. */
    @FunctionalInterface
    interface Buscador {
        String buscar(String url);
    }

    private final ObjectMapper mapper;
    private final Buscador buscador;
    private final Map<String, List<ClimaAnual>> historicos = new ConcurrentHashMap<>();

    public NasaPowerClimaProvider(ObjectMapper mapper) {
        this(mapper, buscadorHttp());
    }

    NasaPowerClimaProvider(ObjectMapper mapper, Buscador buscador) {
        this.mapper = mapper;
        this.buscador = buscador;
    }

    @Override
    public List<ClimaAnual> serieAnual(double latitude, double longitude, int anoInicio, int anoFim, int anoAtual) {
        if (anoFim < anoInicio) {
            throw new IllegalArgumentException("anoFim menor que anoInicio");
        }
        int primeiro = Math.min(anoInicio, anoAtual - ANOS_DE_HISTORICO + 1);
        // A grade da NASA POWER é de 0,5°: arredondar a coordenada não muda o dado e aproveita o cache.
        String chave = String.format(Locale.ROOT, "%.2f,%.2f,%d,%d", latitude, longitude, primeiro, anoAtual);
        if (historicos.size() > MAXIMO_EM_CACHE) {
            historicos.clear();
        }
        List<ClimaAnual> historico = historicos.computeIfAbsent(chave,
                k -> buscarHistorico(latitude, longitude, primeiro, anoAtual));

        Tendencia temperatura = Tendencia.de(historico, ClimaAnual::temperaturaMedia);
        Tendencia maxima = Tendencia.de(historico, ClimaAnual::temperaturaMaxima);
        Tendencia minima = Tendencia.de(historico, ClimaAnual::temperaturaMinima);
        Tendencia chuva = Tendencia.de(historico, ClimaAnual::precipitacao);
        Tendencia umidade = Tendencia.de(historico, ClimaAnual::umidadeSolo);
        Tendencia evapotranspiracao = Tendencia.de(historico, ClimaAnual::evapotranspiracao);

        List<ClimaAnual> serie = new ArrayList<>();
        for (int ano = anoInicio; ano <= anoFim; ano++) {
            if (ano <= anoAtual) {
                serie.add(historico.get(ano - primeiro));
            } else {
                double chuvaDoAno = Math.round(Math.max(0, chuva.em(ano)));
                double temperaturaDoAno = arredondar(temperatura.em(ano), 1);
                serie.add(new ClimaAnual(ano, true,
                        temperaturaDoAno, arredondar(maxima.em(ano), 1), arredondar(minima.em(ano), 1),
                        chuvaDoAno,
                        arredondar(Math.max(0, Math.min(1, umidade.em(ano))), 2),
                        Math.round(Math.max(0, evapotranspiracao.em(ano))),
                        trimestresTipicos(historico, chuvaDoAno, temperaturaDoAno)));
            }
        }
        return serie;
    }

    private List<ClimaAnual> buscarHistorico(double latitude, double longitude, int primeiro, int ultimo) {
        String url = String.format(Locale.ROOT,
                "%s?parameters=%s&community=AG&longitude=%.4f&latitude=%.4f&start=%d&end=%d&format=JSON",
                URL_BASE, PARAMETROS, longitude, latitude, primeiro, ultimo);
        try {
            return lerHistorico(mapper.readTree(buscador.buscar(url)), primeiro, ultimo);
        } catch (IOException e) {
            throw new IllegalStateException("A NASA POWER respondeu em um formato que não entendi", e);
        }
    }

    /** Transforma a resposta mensal da NASA POWER em um {@link ClimaAnual} por ano, de primeiro a ultimo. */
    static List<ClimaAnual> lerHistorico(JsonNode resposta, int primeiro, int ultimo) {
        JsonNode parametros = resposta.path("properties").path("parameter");
        if (parametros.isMissingNode()) {
            throw new IllegalStateException("A NASA POWER não devolveu dados para este ponto");
        }
        List<ClimaAnual> historico = new ArrayList<>();
        for (int ano = primeiro; ano <= ultimo; ano++) {
            double evaporacaoPorDia = anual(parametros, "EVLAND", ano);
            historico.add(new ClimaAnual(ano, false,
                    arredondar(anual(parametros, "T2M", ano), 1),
                    arredondar(mediaDosMeses(parametros, "T2M_MAX", ano), 1),
                    arredondar(mediaDosMeses(parametros, "T2M_MIN", ano), 1),
                    Math.round(anual(parametros, "PRECTOTCORR_SUM", ano)),
                    arredondar(anual(parametros, "GWETROOT", ano), 2),
                    Math.round(evaporacaoPorDia * Year.of(ano).length()),
                    trimestres(parametros, ano)));
        }
        return List.copyOf(historico);
    }

    /** Chuva somada e temperatura média dos 3 meses de cada trimestre; null se faltar algum mês. */
    private static List<Trimestre> trimestres(JsonNode parametros, int ano) {
        List<Trimestre> lista = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            double chuva = 0, temperatura = 0;
            for (int mes = t * 3 + 1; mes <= t * 3 + 3; mes++) {
                String chave = String.format(Locale.ROOT, "%d%02d", ano, mes);
                double c = parametros.path("PRECTOTCORR_SUM").path(chave).asDouble(SEM_DADO);
                double temp = parametros.path("T2M").path(chave).asDouble(SEM_DADO);
                if (c == SEM_DADO || temp == SEM_DADO) {
                    return null;
                }
                chuva += c;
                temperatura += temp;
            }
            lista.add(new Trimestre(Trimestre.NOMES.get(t), Math.round(chuva), arredondar(temperatura / 3, 1)));
        }
        return List.copyOf(lista);
    }

    /**
     * Trimestres de um ano futuro: a chuva do ano repartida como costuma ser no histórico, e a
     * temperatura do ano somada à diferença típica de cada trimestre.
     */
    static List<Trimestre> trimestresTipicos(List<ClimaAnual> historico, double chuvaDoAno, double temperaturaDoAno) {
        List<ClimaAnual> comTrimestres = historico.stream().filter(c -> c.trimestres() != null).toList();
        if (comTrimestres.isEmpty()) {
            return null;
        }
        List<Trimestre> lista = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            final int i = t;
            double parte = comTrimestres.stream()
                    .mapToDouble(c -> c.precipitacao() <= 0 ? 0.25 : c.trimestres().get(i).chuva() / c.precipitacao())
                    .average().orElse(0.25);
            double diferenca = comTrimestres.stream()
                    .mapToDouble(c -> c.trimestres().get(i).temperaturaMedia() - c.temperaturaMedia())
                    .average().orElse(0);
            lista.add(new Trimestre(Trimestre.NOMES.get(t), Math.round(chuvaDoAno * parte),
                    arredondar(temperaturaDoAno + diferenca, 1)));
        }
        return List.copyOf(lista);
    }

    /** Valor anual da NASA (chave AAAA13). Se ela não trouxer, usa a média dos meses (soma, para a chuva). */
    private static double anual(JsonNode parametros, String nome, int ano) {
        double valor = parametros.path(nome).path(ano + "13").asDouble(SEM_DADO);
        if (valor != SEM_DADO) {
            return valor;
        }
        double media = mediaDosMeses(parametros, nome, ano);
        return nome.endsWith("_SUM") ? media * 12 : media;
    }

    private static double mediaDosMeses(JsonNode parametros, String nome, int ano) {
        double soma = 0;
        int meses = 0;
        for (int mes = 1; mes <= 12; mes++) {
            double valor = parametros.path(nome).path(String.format(Locale.ROOT, "%d%02d", ano, mes)).asDouble(SEM_DADO);
            if (valor != SEM_DADO) {
                soma += valor;
                meses++;
            }
        }
        if (meses == 0) {
            throw new IllegalStateException("A NASA POWER ainda não tem " + nome + " para o ano " + ano);
        }
        return soma / meses;
    }

    /** Reta de mínimos quadrados valor = a + b × ano. */
    record Tendencia(double a, double b) {

        static Tendencia de(List<ClimaAnual> historico, ToDoubleFunction<ClimaAnual> valor) {
            int n = historico.size();
            double mediaAno = historico.stream().mapToInt(ClimaAnual::ano).average().orElse(0);
            double mediaValor = historico.stream().mapToDouble(valor).average().orElse(0);
            double cima = 0, baixo = 0;
            for (ClimaAnual c : historico) {
                cima += (c.ano() - mediaAno) * (valor.applyAsDouble(c) - mediaValor);
                baixo += (c.ano() - mediaAno) * (c.ano() - mediaAno);
            }
            double b = n < 2 || baixo == 0 ? 0 : cima / baixo;
            return new Tendencia(mediaValor - b * mediaAno, b);
        }

        double em(int ano) {
            return a + b * ano;
        }
    }

    private static Buscador buscadorHttp() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return url -> {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            try {
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new IllegalStateException("A NASA POWER respondeu " + resp.statusCode()
                            + ". Tente de novo em instantes.");
                }
                return resp.body();
            } catch (IOException e) {
                throw new IllegalStateException("Não consegui falar com a NASA POWER. Confira a internet e tente de novo.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Consulta à NASA POWER interrompida", e);
            }
        };
    }

    private static double arredondar(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }
}
