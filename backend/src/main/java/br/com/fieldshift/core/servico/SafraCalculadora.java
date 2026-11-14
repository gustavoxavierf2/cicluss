package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.Economia;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Safra;

/**
 * Estimativa simples de safra e dinheiro: colheita de um ano bom × nota do ano (0 a 100) × hectares.
 * Serve para o agricultor ter ideia de grandeza; não substitui um modelo de produtividade calibrado.
 */
public class SafraCalculadora {

    /** Devolve null quando a cultura não tem valores cadastrados. */
    public Safra estimar(Cultura cultura, ResultadoAno ano, double hectares) {
        Economia e = cultura.economia();
        if (e == null) {
            return null;
        }
        if (hectares <= 0) {
            throw new IllegalArgumentException("A área em hectares precisa ser maior que zero");
        }
        double producao = hectares * e.produtividadeKgHa() * ano.nota() / 100.0;
        double receita = producao * e.precoPorKg();
        double custo = hectares * e.custoPorHa();
        return new Safra(ano.ano(), hectares, Math.round(producao), Math.round(receita),
                Math.round(custo), Math.round(receita - custo));
    }
}
