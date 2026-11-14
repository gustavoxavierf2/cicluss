package br.com.fieldshift.core.agente;

import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Resumo;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.servico.Textos;

import java.util.Locale;

/** Transforma o {@link ContextoAgente} no texto que vai para o modelo de IA. Não calcula nada. */
public final class ContextoEmTexto {

    private ContextoEmTexto() {
    }

    public static String de(ContextoAgente ctx) {
        Simulacao plantada = ctx.plantada();
        StringBuilder sb = new StringBuilder();
        sb.append("TERRENO\n");
        sb.append(String.format(Locale.ROOT, "Local: latitude %.4f, longitude %.4f%n", plantada.latitude(), plantada.longitude()));
        sb.append("Tipo de solo informado: ").append(plantada.solo() == null ? "NAO_SEI" : plantada.solo()).append('\n');
        sb.append("Último ano com clima observado: ").append(plantada.anoAtual())
                .append(". Anos depois dele são PREVISÃO.\n\n");

        simulacao(sb, "CULTURA QUE O AGRICULTOR PLANTOU", plantada);
        if (ctx.alternativa() != null) {
            simulacao(sb, "CULTURA QUE ELE QUER TESTAR NO LUGAR", ctx.alternativa());
        } else {
            sb.append("CULTURA QUE ELE QUER TESTAR NO LUGAR: nenhuma escolhida.\n\n");
        }

        sb.append("RANKING DAS CULTURAS PARA ESTE TERRENO (pelos anos que já aconteceram)\n");
        if (ctx.ranking() == null || ctx.ranking().isEmpty()) {
            sb.append("Ranking não calculado.\n");
        } else {
            for (ItemRanking i : ctx.ranking()) {
                sb.append(i.posicao()).append("º ").append(i.cultura().nome())
                        .append(" | nota média ").append(i.notaMedia())
                        .append(" | ").append(Textos.nivel(i.nivel()))
                        .append(" | ").append(i.motivo()).append('\n');
            }
        }
        return sb.toString();
    }

    private static void simulacao(StringBuilder sb, String titulo, Simulacao s) {
        sb.append(titulo).append(": ").append(s.cultura().nome()).append('\n');
        sb.append("Resumo dos anos que já aconteceram: ").append(resumo(s.resumoPassado())).append('\n');
        sb.append("Resumo dos anos de previsão: ").append(resumo(s.resumoFuturo())).append('\n');
        sb.append("Ano a ano (nota de 0 a 100):\n");
        for (ResultadoAno a : s.anos()) {
            ClimaAnual c = a.clima();
            sb.append(a.ano()).append(a.projetado() ? " | previsão" : " | aconteceu")
                    .append(" | nota ").append(a.nota())
                    .append(" | ").append(Textos.nivel(a.nivel()))
                    .append(" | risco principal: ").append(Textos.risco(a.risco()))
                    .append(String.format(Locale.ROOT,
                            " | chuva %.0f mm | temperatura média %.1f °C (mínima %.1f, máxima %.1f)"
                                    + " | umidade do solo %.2f (0 seco, 1 encharcado) | água que a planta perde %.0f mm",
                            c.precipitacao(), c.temperaturaMedia(), c.temperaturaMinima(), c.temperaturaMaxima(),
                            c.umidadeSolo(), c.evapotranspiracao()))
                    .append(" | ").append(a.motivo()).append('\n');
        }
        sb.append('\n');
    }

    private static String resumo(Resumo r) {
        return r.anosBons() + " bons, " + r.anosAtencao() + " de atenção, " + r.anosRuins()
                + " ruins (de " + r.total() + " anos; nota média " + r.notaMedia() + ")";
    }
}
