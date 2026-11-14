package br.com.fieldshift.api;

import br.com.fieldshift.core.modelo.TipoSolo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpos das requisições da API. */
public final class Requisicoes {

    private Requisicoes() {
    }

    public record Simulacao(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            TipoSolo solo,
            @NotBlank String culturaId) {
    }

    public record Recomendacao(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            TipoSolo solo) {
    }

    public record Pergunta(
            @NotBlank String pergunta,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            TipoSolo solo,
            @NotBlank String culturaPlantadaId,
            String culturaTesteId) {
    }

    public record Resposta(String resposta, boolean demonstracao) {
    }
}
