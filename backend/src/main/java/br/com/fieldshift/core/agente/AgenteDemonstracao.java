package br.com.fieldshift.core.agente;

import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Resumo;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.servico.Textos;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Agente de demonstração, sem IA: reconhece alguns tipos de pergunta e monta a resposta
 * a partir dos números da simulação. Serve para a demo funcionar sem chave de API.
 */
public class AgenteDemonstracao implements Agente {

    private static final Pattern ANO = Pattern.compile("\\b(19|20)\\d{2}\\b");

    @Override
    public String responder(String pergunta, ContextoAgente ctx) {
        String p = normalizar(pergunta == null ? "" : pergunta);
        Simulacao plantada = ctx.plantada();
        Simulacao alternativa = ctx.alternativa();

        Matcher m = ANO.matcher(p);
        if (m.find()) {
            return explicarAno(Integer.parseInt(m.group()), plantada, alternativa);
        }
        if (contem(p, "melhor", "recomend", "o que plantar", "o que eu planto", "qual cultura")) {
            return recomendar(ctx.ranking());
        }
        if (contem(p, "futuro", "proxim", "vai ser", "previs", "daqui")) {
            return futuro(plantada, alternativa);
        }
        if (alternativa != null && contem(p, "compar", "troc", "vale a pena", "e se", "mudar", "em vez")) {
            return comparar(plantada, alternativa);
        }
        return resumoGeral(plantada, alternativa);
    }

    private String explicarAno(int ano, Simulacao plantada, Simulacao alternativa) {
        Optional<ResultadoAno> r1 = noAno(plantada, ano);
        if (r1.isEmpty()) {
            int inicio = plantada.anos().get(0).ano();
            int fim = plantada.anos().get(plantada.anos().size() - 1).ano();
            return "Eu só tenho dados de " + inicio + " até " + fim + ". Escolha um ano nesse intervalo.";
        }
        ResultadoAno a = r1.get();
        StringBuilder sb = new StringBuilder();
        sb.append(a.projetado() ? "Em " + ano + " (previsão), " : "Em " + ano + ", ");
        sb.append(nome(plantada)).append(" teria um ano ").append(Textos.nivel(a.nivel()))
                .append(" (nota ").append(a.nota()).append("). ").append(a.motivo());
        if (alternativa != null) {
            noAno(alternativa, ano).ifPresent(b -> sb.append(" Já ").append(nome(alternativa))
                    .append(" teria um ano ").append(Textos.nivel(b.nivel()))
                    .append(" (nota ").append(b.nota()).append("). ").append(b.motivo()));
        }
        return sb.toString();
    }

    private String recomendar(List<ItemRanking> ranking) {
        if (ranking == null || ranking.isEmpty()) {
            return "Ainda não calculei a recomendação para esse terreno.";
        }
        String top = ranking.stream().limit(3)
                .map(i -> i.posicao() + "º " + i.cultura().nome() + " (" + i.motivo() + ")")
                .collect(Collectors.joining("; "));
        return "Olhando o clima dos últimos anos nesse terreno, as culturas que mais se dariam bem são: "
                + top + ". Lembre que isso considera só clima e solo, não preço nem mercado.";
    }

    private String futuro(Simulacao plantada, Simulacao alternativa) {
        String texto = "Para os próximos " + plantada.resumoFuturo().total() + " anos, a previsão para "
                + nome(plantada) + " é de " + descrever(plantada.resumoFuturo()) + ".";
        if (alternativa != null) {
            texto += " Para " + nome(alternativa) + ": " + descrever(alternativa.resumoFuturo()) + ".";
        }
        return texto + " É uma projeção, então serve como tendência, não como certeza.";
    }

    private String comparar(Simulacao plantada, Simulacao alternativa) {
        Resumo a = plantada.resumoPassado();
        Resumo b = alternativa.resumoPassado();
        String veredito;
        if (b.notaMedia() > a.notaMedia() + 5) {
            veredito = "Pelo clima, " + nome(alternativa) + " teria se saído melhor que " + nome(plantada) + ".";
        } else if (a.notaMedia() > b.notaMedia() + 5) {
            veredito = "Pelo clima, " + nome(plantada) + " continua sendo a escolha mais segura.";
        } else {
            veredito = "As duas ficam parecidas; a decisão vai depender de outros fatores, como preço e mão de obra.";
        }
        return "Nos últimos " + a.total() + " anos, " + nome(plantada) + " teve " + descrever(a)
                + ", e " + nome(alternativa) + " teria " + descrever(b) + ". " + veredito;
    }

    private String resumoGeral(Simulacao plantada, Simulacao alternativa) {
        String texto = "Nos últimos " + plantada.resumoPassado().total() + " anos, " + nome(plantada)
                + " teve " + descrever(plantada.resumoPassado()) + ".";
        if (alternativa != null) {
            texto += " " + capitalizar(nome(alternativa)) + " teria " + descrever(alternativa.resumoPassado()) + ".";
        }
        return texto + " Você pode me perguntar sobre um ano específico (ex.: \"o que aconteceu em "
                + (plantada.anoAtual() - 3) + "?\"), sobre o futuro ou qual cultura é a melhor.";
    }

    private static String descrever(Resumo r) {
        return plural(r.anosBons(), "ano bom", "anos bons") + ", "
                + r.anosAtencao() + " de atenção e " + r.anosRuins() + (r.anosRuins() == 1 ? " ruim" : " ruins");
    }

    private static String plural(int n, String singular, String pluralForma) {
        return n + " " + (n == 1 ? singular : pluralForma);
    }

    private static Optional<ResultadoAno> noAno(Simulacao s, int ano) {
        return s.anos().stream().filter(a -> a.ano() == ano).findFirst();
    }

    private static String nome(Simulacao s) {
        return s.cultura().nome().toLowerCase(Locale.ROOT);
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean contem(String texto, String... termos) {
        for (String t : termos) {
            if (texto.contains(t)) return true;
        }
        return false;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
