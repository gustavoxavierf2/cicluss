package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.AnoPlano;
import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Fase;
import br.com.fieldshift.core.modelo.GrupoRotacao;
import br.com.fieldshift.core.modelo.Janela;
import br.com.fieldshift.core.modelo.Trimestre;
import br.com.fieldshift.core.modelo.Nivel;
import br.com.fieldshift.core.modelo.Planejamento;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Safra;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Plano de plantio para os próximos 1, 3 ou 5 anos, a partir da cultura que o agricultor escolheu.
 *
 * Regras, em ordem:
 * 1. A cultura escolhida entra no 1º ano e volta sempre que a rotação deixa.
 * 2. Rotação: não se repete o mesmo grupo (gramínea, leguminosa, raiz) em anos seguidos.
 *    Cultura perene (ex.: café) fica no terreno e não entra em rotação.
 * 3. No ano de rotação, entre as culturas que aguentam o clima previsto (não "ruim"), entra a que
 *    deixa a maior sobra estimada; se nenhuma aguenta, entra a de maior nota.
 * 4. Quando a previsão do ano não é boa, o plano aponta uma cultura reserva (plano B) e o cuidado
 *    contra o risco previsto.
 * 5. Linha do tempo: cada ano vira 3 épocas de 4 meses. A mais seca é a entressafra (planta de
 *    cobertura); das outras duas, a mais chuvosa é a safra (a cultura do ano) e a outra é a safrinha
 *    (cultura de ciclo curto, de grupo diferente da última colheita, se a chuva der). A cobertura da
 *    entressafra é escolhida para repor o que a última colheita gastou. Cultura de ciclo longo ocupa o ano todo.
 */
public class PlanejamentoService {

    public static final List<Integer> HORIZONTES = List.of(1, 3, 5);

    private final SimuladorService simulador;
    private final CulturaRepository culturas;

    public PlanejamentoService(SimuladorService simulador, CulturaRepository culturas) {
        this.simulador = simulador;
        this.culturas = culturas;
    }

    public Planejamento planejar(double latitude, double longitude, TipoSolo solo, Double hectares,
                                 String culturaPrincipalId, int anos) {
        if (!HORIZONTES.contains(anos)) {
            throw new IllegalArgumentException("O planejamento é para 1, 3 ou 5 anos");
        }
        double area = hectares == null ? 1.0 : hectares;
        Cultura principal = culturas.obter(culturaPrincipalId);

        Map<String, Simulacao> simulacoes = new LinkedHashMap<>();
        for (Cultura c : culturas.todas()) {
            simulacoes.put(c.id(), simulador.simular(latitude, longitude, solo, c.id(), area));
        }
        // O ano em curso já está plantado: o plano começa em janeiro do próximo ano do calendário.
        int primeiroAno = simulacoes.get(principal.id()).anoAtual() + 2;

        List<AnoPlano> plano = new ArrayList<>();
        Cultura anterior = null;
        for (int ano = primeiroAno; ano < primeiroAno + anos; ano++) {
            Cultura escolhida;
            String porque;
            boolean perene = principal.grupoRotacao() == GrupoRotacao.PERENE;
            boolean podePrincipal = anterior == null || perene || !mesmoGrupo(anterior, principal);
            if (podePrincipal) {
                escolhida = principal;
                porque = perene
                        ? principal.nome() + " fica no terreno vários anos, então não entra em rotação."
                        : anterior == null
                                ? "É a cultura que você escolheu."
                                : "A cultura que você escolheu volta depois de um ano com outra planta no lugar.";
            } else {
                Optional<Cultura> rotacao = culturaDeRotacao(principal, simulacoes, ano);
                if (rotacao.isPresent()) {
                    escolhida = rotacao.get();
                    porque = "Rotação: trocar o tipo de planta ajuda a terra e corta pragas. "
                            + motivoDaRotacao(escolhida, simulacoes, ano);
                } else {
                    escolhida = principal;
                    porque = "Não há outra cultura na tabela para fazer a rotação neste ano.";
                }
            }

            ResultadoAno resultado = noAno(simulacoes.get(escolhida.id()), ano);
            Safra safra = safraNoAno(simulacoes.get(escolhida.id()), ano);
            Cultura planoB = resultado.nivel() == Nivel.BOM
                    ? null
                    : planoB(escolhida, anterior, simulacoes, ano, resultado.nota()).orElse(null);
            plano.add(new AnoPlano(ano, escolhida, escolhida.id().equals(principal.id()), resultado, safra,
                    porque, Textos.cuidado(resultado.risco()), planoB));
            anterior = escolhida;
        }

        boolean temValores = plano.stream().allMatch(a -> a.safra() != null);
        Long saldoTotal = temValores ? plano.stream().mapToLong(a -> a.safra().saldo()).sum() : null;
        int anosDeRisco = (int) plano.stream().filter(a -> a.resultado().nivel() != Nivel.BOM).count();
        return new Planejamento(principal, area, plano, saldoTotal, anosDeRisco,
                resumo(plano, saldoTotal), avisos(principal, plano, saldoTotal), janelas(plano, simulacoes));
    }

    // ---------- linha do tempo da rotação ----------

    private static final String[] MESES = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
    /** Quantos meses de cada trimestre caem em cada época de 4 meses (jan–abr, mai–ago, set–dez). */
    private static final int[][] MESES_DO_TRIMESTRE = {{3, 1, 0, 0}, {0, 2, 2, 0}, {0, 0, 1, 3}};
    /** Sem dado por época, vale o costume da região: chuva no começo e no fim do ano, seca no meio. */
    private static final Fase[] FASES_PADRAO = {Fase.SAFRA, Fase.ENTRESSAFRA, Fase.SAFRINHA};
    /** Cultura com ciclo maior que isto (em dias) fica na terra o ano todo. */
    private static final int CICLO_LONGO = 240;
    private static final int CICLO_DE_SAFRINHA = 120;
    /** Com menos chuva que isto (mm em 4 meses) não vale arriscar uma segunda colheita. */
    private static final double CHUVA_MINIMA_SAFRINHA = 150;

    private List<Janela> janelas(List<AnoPlano> plano, Map<String, Simulacao> simulacoes) {
        List<Janela> janelas = new ArrayList<>();
        Cultura ultimaColheita = null;
        for (int i = 0; i < plano.size(); i++) {
            AnoPlano a = plano.get(i);
            ClimaAnual clima = a.resultado().clima();
            List<Trimestre> trimestres = clima.trimestres();
            boolean porEpoca = trimestres != null && trimestres.size() == 4;
            double[] chuva = new double[3], temperatura = new double[3], umidade = new double[3];
            for (int j = 0; j < 3; j++) {
                if (!porEpoca) {
                    chuva[j] = clima.precipitacao() / 3;
                    temperatura[j] = clima.temperaturaMedia();
                    umidade[j] = clima.umidadeSolo();
                    continue;
                }
                for (int t = 0; t < 4; t++) {
                    int meses = MESES_DO_TRIMESTRE[j][t];
                    Trimestre tri = trimestres.get(t);
                    chuva[j] += tri.chuva() * meses / 3.0;
                    temperatura[j] += tri.temperaturaMedia() * meses / 4.0;
                    umidade[j] += (tri.umidadeSolo() == null ? clima.umidadeSolo() : tri.umidadeSolo()) * meses / 4.0;
                }
            }
            Fase[] fases = porEpoca ? fases(chuva) : FASES_PADRAO;
            Cultura cultura = a.cultura();
            String nome = cultura.nome();
            boolean anoTodo = cultura.grupoRotacao() == GrupoRotacao.PERENE || cultura.cicloDias() > CICLO_LONGO;
            String sufixo = "/" + String.format(Locale.ROOT, "%02d", a.ano() % 100);
            for (int j = 0; j < 3; j++) {
                Fase fase = fases[j];
                String plantio = nome;
                String culturaId = cultura.id();
                String porque;
                if (anoTodo) {
                    porque = cultura.grupoRotacao() == GrupoRotacao.PERENE
                            ? nome + " fica no terreno vários anos. Plante cobertura no meio das linhas para proteger a terra."
                            : nome + " leva o ano todo para ficar pronta, então segue na terra. Deixe palha no chão para guardar a umidade.";
                } else if (fase == Fase.SAFRA) {
                    porque = a.principal()
                            ? "É a colheita principal do ano, na época que mais chove. É a que mais traz renda."
                            : "Planta diferente da do ano passado: descansa a terra, corta o ciclo das pragas e ainda traz renda.";
                } else {
                    // A safrinha fica entre duas colheitas: não repete o grupo da anterior e, se der, nem o da próxima.
                    boolean safraAindaVem = List.of(fases).subList(j, 3).contains(Fase.SAFRA);
                    Cultura proxima = safraAindaVem ? cultura : i + 1 < plano.size() ? plano.get(i + 1).cultura() : null;
                    Optional<Cultura> segunda = fase == Fase.SAFRINHA && (!porEpoca || chuva[j] >= CHUVA_MINIMA_SAFRINHA)
                            ? culturaDeSafrinha(ultimaColheita == null ? cultura : ultimaColheita, proxima, simulacoes, a.ano())
                            : Optional.empty();
                    if (segunda.isPresent()) {
                        plantio = segunda.get().nome();
                        culturaId = segunda.get().id();
                        porque = segunda.get().grupoRotacao() == GrupoRotacao.LEGUMINOSA
                                ? "Segunda colheita do ano. Essa planta devolve nitrogênio (adubo natural) para a terra e traz uma renda a mais."
                                : "Segunda colheita do ano, com uma planta de outro tipo: aproveita o resto da chuva, deixa palha na terra e traz uma renda a mais.";
                    } else {
                        boolean gastaNitrogenio = (ultimaColheita == null ? cultura : ultimaColheita).grupoRotacao() == GrupoRotacao.GRAMINEA;
                        plantio = gastaNitrogenio ? "Crotalária + milheto (cobertura)" : "Braquiária + milheto (cobertura)";
                        culturaId = null;
                        porque = fase == Fase.SAFRINHA
                                ? "Chove pouco para uma segunda colheita. Melhor a terra coberta do que pelada: a palha guarda a umidade e evita a erosão."
                                : "Época seca. Não é para colher: é para proteger a terra da erosão, guardar a umidade e quebrar o ciclo das pragas"
                                        + (gastaNitrogenio ? ". A crotalária ainda repõe o adubo que a última colheita gastou." : " até a próxima safra.");
                    }
                }
                if (culturaId != null) {
                    ultimaColheita = culturas.obter(culturaId);
                }
                janelas.add(new Janela(a.ano(), MESES[j * 4] + sufixo, MESES[j * 4 + 3] + sufixo, fase, plantio, culturaId,
                        porque, arredondar(temperatura[j], 1), arredondar(umidade[j], 2), Math.round(chuva[j]), !porEpoca));
            }
        }
        return janelas;
    }

    /** A época mais seca é a entressafra; das outras duas, a mais chuvosa é a safra. */
    private static Fase[] fases(double[] chuva) {
        int seca = 0, chuvosa = 0;
        for (int j = 1; j < 3; j++) {
            if (chuva[j] < chuva[seca]) seca = j;
            if (chuva[j] > chuva[chuvosa]) chuvosa = j;
        }
        if (seca == chuvosa) {
            return FASES_PADRAO;
        }
        Fase[] fases = {Fase.SAFRINHA, Fase.SAFRINHA, Fase.SAFRINHA};
        fases[seca] = Fase.ENTRESSAFRA;
        fases[chuvosa] = Fase.SAFRA;
        return fases;
    }

    /**
     * Cultura de ciclo curto para a safrinha: nunca do grupo da colheita anterior; de preferência também
     * diferente da próxima (no grupo ou, pelo menos, na planta), depois leguminosa (repõe nitrogênio) e, por fim, a de maior nota no ano.
     */
    private Optional<Cultura> culturaDeSafrinha(Cultura anterior, Cultura proxima, Map<String, Simulacao> simulacoes, int ano) {
        Comparator<Cultura> diferenteDaProxima = Comparator.comparing(c -> proxima == null || !mesmoGrupo(c, proxima));
        Comparator<Cultura> outraPlanta = Comparator.comparing(c -> proxima == null || !c.id().equals(proxima.id()));
        Comparator<Cultura> leguminosa = Comparator.comparing(c -> c.grupoRotacao() == GrupoRotacao.LEGUMINOSA);
        return culturas.todas().stream()
                .filter(c -> c.grupoRotacao() != GrupoRotacao.PERENE && c.cicloDias() <= CICLO_DE_SAFRINHA)
                .filter(c -> !mesmoGrupo(c, anterior))
                .filter(c -> noAno(simulacoes.get(c.id()), ano).nivel() != Nivel.RUIM)
                .max(diferenteDaProxima.thenComparing(outraPlanta).thenComparing(leguminosa)
                        .thenComparingInt(c -> noAno(simulacoes.get(c.id()), ano).nota()));
    }

    private static double arredondar(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }

    /** Melhor cultura de outro grupo para o ano: aguenta o clima previsto e deixa a maior sobra. */
    private Optional<Cultura> culturaDeRotacao(Cultura principal, Map<String, Simulacao> simulacoes, int ano) {
        List<Cultura> candidatas = culturas.todas().stream()
                .filter(c -> c.grupoRotacao() != GrupoRotacao.PERENE && !mesmoGrupo(c, principal))
                .toList();
        List<Cultura> aguentam = candidatas.stream()
                .filter(c -> noAno(simulacoes.get(c.id()), ano).nivel() != Nivel.RUIM)
                .toList();
        Comparator<Cultura> porNota = Comparator.comparingInt(c -> noAno(simulacoes.get(c.id()), ano).nota());
        if (aguentam.isEmpty()) {
            return candidatas.stream().max(porNota);
        }
        Comparator<Cultura> porSaldo = Comparator.comparingLong(c -> {
            Safra s = safraNoAno(simulacoes.get(c.id()), ano);
            return s == null ? Long.MIN_VALUE : s.saldo();
        });
        return aguentam.stream().max(porSaldo.thenComparing(porNota));
    }

    private String motivoDaRotacao(Cultura c, Map<String, Simulacao> simulacoes, int ano) {
        ResultadoAno r = noAno(simulacoes.get(c.id()), ano);
        return r.nivel() == Nivel.RUIM
                ? "Nenhuma opção vai bem com o clima previsto; " + c.nome().toLowerCase(Locale.ROOT) + " é a que menos sofre."
                : "Entre as que aguentam o clima previsto, " + c.nome().toLowerCase(Locale.ROOT)
                        + " é a que deixa a maior sobra estimada.";
    }

    /** Cultura reserva: a de maior nota no ano, melhor que a escolhida e sem repetir o grupo do ano anterior. */
    private Optional<Cultura> planoB(Cultura escolhida, Cultura anterior, Map<String, Simulacao> simulacoes,
                                     int ano, int notaEscolhida) {
        return culturas.todas().stream()
                .filter(c -> !c.id().equals(escolhida.id()))
                .filter(c -> c.grupoRotacao() != GrupoRotacao.PERENE)
                .filter(c -> anterior == null || !mesmoGrupo(c, anterior))
                .filter(c -> noAno(simulacoes.get(c.id()), ano).nota() > notaEscolhida)
                .max(Comparator.comparingInt(c -> noAno(simulacoes.get(c.id()), ano).nota()));
    }

    private static String resumo(List<AnoPlano> plano, Long saldoTotal) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (AnoPlano a : plano) {
            contagem.merge(a.cultura().nome().toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        List<String> partes = new ArrayList<>();
        contagem.forEach((nome, n) -> partes.add(n + (n == 1 ? " ano de " : " anos de ") + nome));
        String texto = "Plano para " + plano.size() + (plano.size() == 1 ? " ano: " : " anos: ")
                + String.join(", ", partes) + ".";
        if (saldoTotal != null) {
            texto += saldoTotal >= 0
                    ? " Sobra estimada no período: R$ " + Textos.dinheiro(saldoTotal) + "."
                    : " Prejuízo estimado no período: R$ " + Textos.dinheiro(-saldoTotal) + ".";
        }
        return texto;
    }

    private static List<String> avisos(Cultura principal, List<AnoPlano> plano, Long saldoTotal) {
        List<String> avisos = new ArrayList<>();
        List<AnoPlano> anosDaPrincipal = plano.stream().filter(AnoPlano::principal).toList();
        long ruinsDaPrincipal = anosDaPrincipal.stream().filter(a -> a.resultado().nivel() == Nivel.RUIM).count();
        if (!anosDaPrincipal.isEmpty() && ruinsDaPrincipal * 2 >= anosDaPrincipal.size()) {
            avisos.add("Pela previsão, " + principal.nome().toLowerCase(Locale.ROOT)
                    + " vai mal neste terreno na maior parte dos anos do plano. Veja a aba \"O que plantar\" antes de decidir.");
        }
        List<Integer> anosNoVermelho = plano.stream()
                .filter(a -> a.safra() != null && a.safra().saldo() < 0)
                .map(AnoPlano::ano)
                .toList();
        if (!anosNoVermelho.isEmpty()) {
            avisos.add("Previsão de prejuízo em " + juntar(anosNoVermelho)
                    + ". Guarde parte da sobra dos anos bons para atravessar " + (anosNoVermelho.size() == 1 ? "esse ano." : "esses anos."));
        }
        if (saldoTotal != null && saldoTotal < 0) {
            avisos.add("Somando todos os anos, o plano fecha no prejuízo. Vale testar outra cultura.");
        }
        if (plano.size() > 1 && plano.stream().map(a -> a.cultura().id()).distinct().count() > 1) {
            avisos.add("Plantar culturas diferentes ao longo dos anos protege a renda: se o clima castigar uma, a outra segura.");
        }
        avisos.add("Tudo aqui é previsão: quanto mais longe o ano, menor a certeza. Refaça o plano todo ano.");
        return avisos;
    }

    private static String juntar(List<Integer> anos) {
        if (anos.size() == 1) return String.valueOf(anos.get(0));
        String inicio = String.join(", ", anos.subList(0, anos.size() - 1).stream().map(String::valueOf).toList());
        return inicio + " e " + anos.get(anos.size() - 1);
    }

    private static boolean mesmoGrupo(Cultura a, Cultura b) {
        if (a.id().equals(b.id())) return true;
        return a.grupoRotacao() != null && a.grupoRotacao() == b.grupoRotacao();
    }

    private static ResultadoAno noAno(Simulacao s, int ano) {
        return s.anos().stream().filter(a -> a.ano() == ano).findFirst()
                .orElseThrow(() -> new IllegalStateException("Sem previsão para " + ano));
    }

    private static Safra safraNoAno(Simulacao s, int ano) {
        return s.safras().stream().filter(a -> a.ano() == ano).findFirst().orElse(null);
    }
}
