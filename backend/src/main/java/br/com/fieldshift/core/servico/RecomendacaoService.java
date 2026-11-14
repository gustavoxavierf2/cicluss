package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Risco;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ranking das culturas mais aptas para o terreno, olhando os anos passados (dado observado).
 * Desempate: mais anos bons primeiro.
 */
public class RecomendacaoService {

    private final SimuladorService simulador;
    private final CulturaRepository culturas;

    public RecomendacaoService(SimuladorService simulador, CulturaRepository culturas) {
        this.simulador = simulador;
        this.culturas = culturas;
    }

    public List<ItemRanking> recomendar(double latitude, double longitude, TipoSolo solo) {
        record Parcial(Cultura cultura, int nota, int bons, int total, Risco risco) {}

        List<Parcial> parciais = new ArrayList<>();
        for (Cultura c : culturas.todas()) {
            Simulacao s = simulador.simular(latitude, longitude, solo, c.id());
            List<ResultadoAno> passado = s.anos().stream().filter(a -> !a.projetado()).toList();
            parciais.add(new Parcial(c, s.resumoPassado().notaMedia(), s.resumoPassado().anosBons(),
                    passado.size(), riscoMaisFrequente(passado)));
        }
        parciais.sort(Comparator.comparingInt(Parcial::nota).reversed()
                .thenComparing(Comparator.comparingInt(Parcial::bons).reversed()));

        List<ItemRanking> ranking = new ArrayList<>();
        for (int i = 0; i < parciais.size(); i++) {
            Parcial p = parciais.get(i);
            ranking.add(new ItemRanking(p.cultura(), i + 1, p.nota(), Nivel.daNota(p.nota()),
                    p.bons(), p.total(), p.risco(), motivo(p.bons(), p.total(), p.risco())));
        }
        return ranking;
    }

    static Risco riscoMaisFrequente(List<ResultadoAno> anos) {
        Map<Risco, Integer> contagem = new EnumMap<>(Risco.class);
        for (ResultadoAno a : anos) {
            if (a.risco() != Risco.NENHUM) contagem.merge(a.risco(), 1, Integer::sum);
        }
        return contagem.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Risco.NENHUM);
    }

    private static String motivo(int bons, int total, Risco risco) {
        String base = String.format(Locale.ROOT, "%d de %d anos %s", bons, total, bons == 1 ? "bom" : "bons");
        return risco == Risco.NENHUM
                ? base + ", sem risco recorrente"
                : base + ", risco mais comum: " + Textos.risco(risco);
    }
}
