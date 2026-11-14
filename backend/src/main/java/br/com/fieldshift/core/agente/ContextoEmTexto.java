package br.com.fieldshift.core.agente;

import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Resumo;
import br.com.fieldshift.core.modelo.Safra;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.Trimestre;
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

        if (plantada.anos().stream().anyMatch(a -> a.clima().trimestres() != null)) {
            sb.append("CHUVA POR ÉPOCA DO ANO (mm em cada trimestre; serve para dizer em que época faltou ou sobrou água)\n");
            for (ResultadoAno a : plantada.anos()) {
                if (a.clima().trimestres() == null) continue;
                sb.append(a.ano()).append(a.projetado() ? " (previsão)" : "");
                for (Trimestre t : a.clima().trimestres()) {
                    sb.append(String.format(Locale.ROOT, " | %s %.0f mm", t.nome(), t.chuva()));
                }
                sb.append('\n');
            }
            sb.append('\n');
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
        if (!s.safras().isEmpty()) {
            sb.append(String.format(Locale.ROOT, "Área informada: %.2f hectares. ", s.hectares()))
                    .append("A safra e os valores em R$ são ESTIMATIVAS feitas com preços e custos de exemplo.\n");
        }
        sb.append("Ano a ano (nota de 0 a 100):\n");
        for (int i = 0; i < s.anos().size(); i++) {
            ResultadoAno a = s.anos().get(i);
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
                    .append(" | ").append(a.motivo());
            if (!s.safras().isEmpty()) {
                Safra f = s.safras().get(i);
                sb.append(" | safra estimada ").append(f.producaoKg()).append(" kg, venda R$ ").append(f.receita())
                        .append(", custo R$ ").append(f.custo()).append(", sobra R$ ").append(f.saldo());
            }
            sb.append('\n');
        }
        sb.append('\n');
    }

    private static String resumo(Resumo r) {
        return r.anosBons() + " bons, " + r.anosAtencao() + " de atenção, " + r.anosRuins()
                + " ruins (de " + r.total() + " anos; nota média " + r.notaMedia() + ")";
    }
}
