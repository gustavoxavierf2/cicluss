package br.com.fieldshift.core.clima;

import br.com.fieldshift.core.modelo.ClimaAnual;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DADOS FALSOS para desenvolver o site enquanto os dados reais da NASA não chegam.
 *
 * Gera um clima plausível para o Sudoeste da Bahia, sempre igual para o mesmo ponto e ano
 * (a semente vem da coordenada), com variação entre anos, alguns anos de seca e um
 * aquecimento leve nos anos projetados. Não use estes números no pitch como se fossem reais.
 */
public class MockClimaProvider implements ClimaProvider {

    private static final double PROBABILIDADE_SECA = 0.15;

    @Override
    public List<ClimaAnual> serieAnual(double latitude, double longitude, int anoInicio, int anoFim, int anoAtual) {
        if (anoFim < anoInicio) {
            throw new IllegalArgumentException("anoFim menor que anoInicio");
        }
        long sementeLocal = semente(latitude, longitude);
        Random local = new Random(sementeLocal);
        // Cada ponto do mapa tem um "jeitão" próprio de temperatura e chuva.
        double temperaturaBase = 20.5 + (local.nextDouble() - 0.5) * 4.0;   // ~18.5 a 22.5 °C
        double chuvaBase = 780 + (local.nextDouble() - 0.5) * 500;           // ~530 a 1030 mm

        List<ClimaAnual> serie = new ArrayList<>();
        for (int ano = anoInicio; ano <= anoFim; ano++) {
            Random r = new Random(sementeLocal * 31 + ano);
            boolean projetado = ano > anoAtual;

            double aquecimento = 0.025 * (ano - 2000);
            double temperatura = temperaturaBase + aquecimento + r.nextGaussian() * 0.6;

            double chuva = chuvaBase * (1 + r.nextGaussian() * 0.2);
            if (r.nextDouble() < PROBABILIDADE_SECA) {
                chuva *= 0.55;
            }
            if (projetado) {
                chuva *= 1 - 0.004 * (ano - anoAtual); // tendência leve de menos chuva
            }
            chuva = Math.max(150, chuva);

            double evapotranspiracao = 1000 + 30 * (temperatura - 20) + r.nextGaussian() * 40;
            double umidade = limitar(0.12 + 0.4 * chuva / evapotranspiracao + r.nextGaussian() * 0.03, 0.05, 0.6);
            double maxima = temperatura + 7 + r.nextGaussian() * 0.5;
            double minima = temperatura - 6 + r.nextGaussian() * 0.5;

            serie.add(new ClimaAnual(ano, projetado,
                    arredondar(temperatura, 1), arredondar(maxima, 1), arredondar(minima, 1),
                    Math.round(chuva), arredondar(umidade, 2), Math.round(evapotranspiracao)));
        }
        return serie;
    }

    private static long semente(double latitude, double longitude) {
        long lat = Math.round(latitude * 100);
        long lon = Math.round(longitude * 100);
        return lat * 100_003L + lon;
    }

    private static double limitar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double arredondar(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }
}
