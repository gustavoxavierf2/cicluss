package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.ComparacaoEstrategias;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Estrategia;
import br.com.fieldshift.core.modelo.Estrategia.AnoEstrategia;
import br.com.fieldshift.core.modelo.GrupoRotacao;
import br.com.fieldshift.core.modelo.IndicadorSolo;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Resumo;
import br.com.fieldshift.core.modelo.Safra;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;
import br.com.fieldshift.core.modelo.Trimestre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Compara estratégias de rotação de culturas para o terreno, sempre incluindo o que o agricultor
 * faz hoje. Cada estratégia é uma sequência de culturas que se repete, uma por ano, avaliada com o
 * clima que realmente aconteceu (anos observados) e com sinais simples de saúde do solo.
 *
 * Estratégias montadas:
 * 1. O que ele faz hoje: a mesma cultura todo ano.
 * 2. A cultura dele alternada com a que ele quer testar (se escolheu uma).
 * 3. Rotação com leguminosa: alterna gramínea e leguminosa (repõe nitrogênio).
 * 4. Rotação de três grupos: gramínea, leguminosa e raiz.
 * Em cada grupo entra a cultura que melhor se deu com o clima do terreno.
 *
 * Indicada: entre as estratégias com nota de clima perto da melhor (até 10 pontos abaixo), a que
 * faz mais pela saúde do solo; empate, a de maior sobra estimada.
 */
public class EstrategiaService {

    static final int FOLGA_DE_NOTA = 10;

    private final SimuladorService simulador;
    private final CulturaRepository culturas;

    public EstrategiaService(SimuladorService simulador, CulturaRepository culturas) {
        this.simulador = simulador;
        this.culturas = culturas;
    }

    public ComparacaoEstrategias comparar(double latitude, double longitude, TipoSolo solo, Double hectares,
                                          String culturaPlantadaId, String culturaTesteId) {
        double area = hectares == null ? 1.0 : hectares;
        Cultura plantada = culturas.obter(culturaPlantadaId);
        Cultura teste = culturaTesteId == null || culturaTesteId.isBlank() ? null : culturas.obter(culturaTesteId);

        Map<String, Simulacao> simulacoes = new LinkedHashMap<>();
        for (Cultura c : culturas.todas()) {
            simulacoes.put(c.id(), simulador.simular(latitude, longitude, solo, c.id(), area));
        }

        // sequência -> (nome, descrição); LinkedHashMap mantém a ordem e tira repetidas
        Map<List<Cultura>, String[]> candidatas = new LinkedHashMap<>();
        candidatas.put(List.of(plantada), new String[]{
                "Só " + minusculo(plantada),
                "O que você faz hoje: " + minusculo(plantada) + " todo ano no mesmo lugar."});

        boolean perene = plantada.grupoRotacao() == GrupoRotacao.PERENE;
        if (teste != null && !perene && teste.grupoRotacao() != GrupoRotacao.PERENE) {
            candidatas.putIfAbsent(List.of(plantada, teste), new String[]{
                    nomes(List.of(plantada, teste)),
                    "Um ano de " + minusculo(plantada) + ", um ano de " + minusculo(teste) + "."});
        }

        Optional<Cultura> graminea = doGrupo(GrupoRotacao.GRAMINEA, plantada, simulacoes);
        Optional<Cultura> leguminosa = doGrupo(GrupoRotacao.LEGUMINOSA, plantada, simulacoes);
        Optional<Cultura> raiz = doGrupo(GrupoRotacao.RAIZ, plantada, simulacoes);

        if (leguminosa.isPresent()) {
            Cultura par = plantada.grupoRotacao() == GrupoRotacao.LEGUMINOSA || perene
                    ? graminea.orElse(null)
                    : plantada;
            if (par != null) {
                List<Cultura> seq = plantada.grupoRotacao() == GrupoRotacao.LEGUMINOSA
                        ? List.of(leguminosa.get(), par)
                        : List.of(par, leguminosa.get());
                candidatas.putIfAbsent(seq, new String[]{
                        nomes(seq),
                        "Rotação com leguminosa: " + minusculo(leguminosa.get()) + " entra ano sim, ano não, e repõe nitrogênio na terra."});
            }
        }
        if (graminea.isPresent() && leguminosa.isPresent() && raiz.isPresent()) {
            List<Cultura> seq = switch (plantada.grupoRotacao() == null ? GrupoRotacao.GRAMINEA : plantada.grupoRotacao()) {
                case LEGUMINOSA -> List.of(leguminosa.get(), graminea.get(), raiz.get());
                case RAIZ -> List.of(raiz.get(), graminea.get(), leguminosa.get());
                default -> List.of(graminea.get(), leguminosa.get(), raiz.get());
            };
            candidatas.putIfAbsent(seq, new String[]{
                    nomes(seq),
                    "Rotação de três tipos de planta: cada uma usa e devolve coisas diferentes para a terra."});
        }

        List<Estrategia> estrategias = new ArrayList<>();
        int n = 0;
        for (Map.Entry<List<Cultura>, String[]> e : candidatas.entrySet()) {
            estrategias.add(avaliar("e" + (++n), e.getValue()[0], e.getValue()[1], n == 1, e.getKey(), simulacoes));
        }

        int melhorNota = estrategias.stream().mapToInt(e -> e.resumo().notaMedia()).max().orElse(0);
        Estrategia indicada = estrategias.stream()
                .filter(e -> e.resumo().notaMedia() >= melhorNota - FOLGA_DE_NOTA)
                .max(Comparator.comparingInt(Estrategia::pontosSolo)
                        .thenComparingLong(e -> e.saldoTotal() == null ? Long.MIN_VALUE : e.saldoTotal())
                        .thenComparingInt(e -> e.resumo().notaMedia()))
                .orElse(estrategias.get(0));

        return new ComparacaoEstrategias(estrategias, indicada.id(), porque(indicada, estrategias.get(0)),
                epocaSeca(simulacoes.get(plantada.id())), area);
    }

    private Estrategia avaliar(String id, String nome, String descricao, boolean atual, List<Cultura> sequencia,
                               Map<String, Simulacao> simulacoes) {
        List<ResultadoAno> resultados = new ArrayList<>();
        List<AnoEstrategia> anos = new ArrayList<>();
        long saldo = 0;
        boolean temValores = true;

        List<ResultadoAno> observados = simulacoes.get(sequencia.get(0).id()).anos().stream()
                .filter(a -> !a.projetado()).toList();
        for (int i = 0; i < observados.size(); i++) {
            Cultura cultura = sequencia.get(i % sequencia.size());
            Simulacao s = simulacoes.get(cultura.id());
            int ano = observados.get(i).ano();
            ResultadoAno r = s.anos().stream().filter(a -> a.ano() == ano).findFirst().orElseThrow();
            resultados.add(r);
            anos.add(new AnoEstrategia(ano, cultura.nome(), r.nota(), r.nivel(), r.risco()));
            Optional<Safra> safra = s.safras().stream().filter(f -> f.ano() == ano).findFirst();
            if (safra.isPresent()) {
                saldo += safra.get().saldo();
            } else {
                temValores = false;
            }
        }

        List<IndicadorSolo> solo = indicadoresDeSolo(sequencia);
        int pontos = solo.stream().mapToInt(i -> i.nivel() == Nivel.BOM ? 2 : i.nivel() == Nivel.ATENCAO ? 1 : 0).sum();
        return new Estrategia(id, nome, descricao, atual, sequencia, anos, Resumo.de(resultados),
                temValores ? saldo : null, solo, pontos, mesesDescoberto(sequencia));
    }

    /** Sinais simples de saúde do solo, tirados do grupo e do ciclo de cada cultura da sequência. */
    public static List<IndicadorSolo> indicadoresDeSolo(List<Cultura> sequencia) {
        List<IndicadorSolo> lista = new ArrayList<>();

        Optional<Cultura> leguminosa = sequencia.stream()
                .filter(c -> c.grupoRotacao() == GrupoRotacao.LEGUMINOSA).findFirst();
        lista.add(leguminosa
                .map(c -> new IndicadorSolo("Nitrogênio", Nivel.BOM,
                        capitalizar(minusculo(c)) + " é leguminosa: tira nitrogênio do ar e deixa na terra para a próxima cultura."))
                .orElseGet(() -> new IndicadorSolo("Nitrogênio", Nivel.RUIM,
                        "Sem leguminosa: a terra não ganha nitrogênio natural e pede mais adubo.")));

        long grupos = sequencia.stream().map(c -> c.grupoRotacao() == null ? c.id() : c.grupoRotacao().name())
                .distinct().count();
        if (sequencia.size() == 1) {
            lista.add(new IndicadorSolo("Pragas e doenças", Nivel.RUIM,
                    "A mesma planta todo ano no mesmo lugar: pragas e doenças se acumulam."));
        } else if (grupos < sequencia.size()) {
            lista.add(new IndicadorSolo("Pragas e doenças", Nivel.ATENCAO,
                    "Troca de cultura, mas repete o mesmo tipo de planta: quebra pouco o ciclo das pragas."));
        } else {
            lista.add(new IndicadorSolo("Pragas e doenças", Nivel.BOM,
                    "Troca o tipo de planta de um ano para o outro: quebra o ciclo das pragas e doenças."));
        }

        int descoberto = mesesDescoberto(sequencia);
        Nivel cobertura = descoberto <= 3 ? Nivel.BOM : descoberto <= 7 ? Nivel.ATENCAO : Nivel.RUIM;
        lista.add(new IndicadorSolo("Terra coberta", cobertura, descoberto == 0
                ? "A terra fica coberta o ano todo."
                : "A terra fica descoberta cerca de " + descoberto + (descoberto == 1 ? " mês" : " meses")
                        + " por ano. Deixar a palha ou plantar adubo verde nesse tempo protege do sol e da enxurrada."));

        Optional<Cultura> graminea = sequencia.stream()
                .filter(c -> c.grupoRotacao() == GrupoRotacao.GRAMINEA).findFirst();
        lista.add(graminea.isPresent()
                ? new IndicadorSolo("Palha e matéria orgânica", Nivel.BOM,
                        capitalizar(minusculo(graminea.get())) + " deixa bastante palha, que vira matéria orgânica e segura a umidade.")
                : new IndicadorSolo("Palha e matéria orgânica", Nivel.ATENCAO,
                        "Sobra pouca palha depois da colheita para proteger e alimentar a terra."));
        return lista;
    }

    /** Média de meses por ano sem cultura no terreno, pelo ciclo (em dias) de cada cultura da sequência. */
    static int mesesDescoberto(List<Cultura> sequencia) {
        double cobertos = sequencia.stream()
                .mapToDouble(c -> Math.min(12, Math.round(c.cicloDias() / 30.0)))
                .average().orElse(0);
        return (int) Math.round(12 - cobertos);
    }

    /** A cultura do grupo que melhor se deu com o clima observado; a do agricultor tem preferência. */
    private Optional<Cultura> doGrupo(GrupoRotacao grupo, Cultura plantada, Map<String, Simulacao> simulacoes) {
        if (plantada.grupoRotacao() == grupo) {
            return Optional.of(plantada);
        }
        return culturas.todas().stream()
                .filter(c -> c.grupoRotacao() == grupo)
                .max(Comparator.comparingInt(c -> simulacoes.get(c.id()).resumoPassado().notaMedia()));
    }

    private static String porque(Estrategia indicada, Estrategia atual) {
        if (indicada.id().equals(atual.id())) {
            return "Pelo clima e pelo solo, nenhuma rotação testada ficou melhor do que o que você já faz.";
        }
        List<String> ganhos = new ArrayList<>();
        if (indicada.pontosSolo() > atual.pontosSolo()) {
            ganhos.add("cuida melhor da terra");
        }
        if (indicada.resumo().anosRuins() < atual.resumo().anosRuins()) {
            ganhos.add("teria tido menos anos ruins");
        }
        if (indicada.saldoTotal() != null && atual.saldoTotal() != null && indicada.saldoTotal() > atual.saldoTotal()) {
            ganhos.add("teria deixado mais sobra");
        }
        String texto = capitalizar(indicada.nome().toLowerCase(Locale.ROOT)) + " é a mais indicada";
        return ganhos.isEmpty()
                ? texto + ": fica perto da melhor no clima e faz mais pela terra."
                : texto + ": comparada ao que você faz hoje, " + juntar(ganhos) + ".";
    }

    /** Trimestre que, na média dos anos observados, é o mais seco no terreno. */
    private static String epocaSeca(Simulacao s) {
        List<ClimaAnual> comTrimestres = s.anos().stream()
                .filter(a -> !a.projetado()).map(ResultadoAno::clima)
                .filter(c -> c.trimestres() != null).toList();
        if (comTrimestres.isEmpty()) {
            return null;
        }
        int maisSeco = 0;
        double menor = Double.MAX_VALUE, total = 0;
        double[] medias = new double[4];
        for (int t = 0; t < 4; t++) {
            final int i = t;
            medias[t] = comTrimestres.stream().mapToDouble(c -> c.trimestres().get(i).chuva()).average().orElse(0);
            total += medias[t];
            if (medias[t] < menor) {
                menor = medias[t];
                maisSeco = t;
            }
        }
        long parte = total <= 0 ? 0 : Math.round(100 * menor / total);
        return "A época mais seca neste terreno costuma ser " + Trimestre.NOMES.get(maisSeco) + ": em média "
                + Math.round(menor) + " mm, só " + parte + "% da chuva do ano. É quando a terra descoberta mais sofre.";
    }

    private static String nomes(List<Cultura> sequencia) {
        return sequencia.stream().map(Cultura::nome).collect(Collectors.joining(" + "));
    }

    private static String juntar(List<String> partes) {
        if (partes.size() == 1) return partes.get(0);
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " e " + partes.get(partes.size() - 1);
    }

    private static String minusculo(Cultura c) {
        return c.nome().toLowerCase(Locale.ROOT);
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
