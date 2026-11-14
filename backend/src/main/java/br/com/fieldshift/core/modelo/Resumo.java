package br.com.fieldshift.core.modelo;

import java.util.List;

/** Contagem de anos bons/atenção/ruins em um período. */
public record Resumo(int anosBons, int anosAtencao, int anosRuins, int notaMedia) {

    public static Resumo de(List<ResultadoAno> anos) {
        int bons = 0, atencao = 0, ruins = 0, soma = 0;
        for (ResultadoAno r : anos) {
            soma += r.nota();
            switch (r.nivel()) {
                case BOM -> bons++;
                case ATENCAO -> atencao++;
                case RUIM -> ruins++;
            }
        }
        int media = anos.isEmpty() ? 0 : Math.round((float) soma / anos.size());
        return new Resumo(bons, atencao, ruins, media);
    }

    public int total() {
        return anosBons + anosAtencao + anosRuins;
    }
}
