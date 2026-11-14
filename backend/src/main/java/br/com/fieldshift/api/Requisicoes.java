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
            @NotBlank String culturaId,
            @DecimalMin("0.01") @DecimalMax("100000") Double hectares) {
    }

    /** "E se o clima fosse assim?": valores nulos mantêm os do ano base. */
    public record Cenario(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            TipoSolo solo,
            @NotBlank String culturaId,
            @DecimalMin("0.01") @DecimalMax("100000") Double hectares,
            @NotNull Integer anoBase,
            Double temperaturaMedia,
            Double precipitacao,
            Double umidadeSolo) {
    }

    public record Planejamento(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            TipoSolo solo,
            @DecimalMin("0.01") @DecimalMax("100000") Double hectares,
            @NotBlank String culturaPrincipalId,
            @NotNull Integer anos) {
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
            String culturaTesteId,
            @DecimalMin("0.01") @DecimalMax("100000") Double hectares) {
    }

    public record Resposta(String resposta, boolean demonstracao) {
    }

    /** De onde vêm os dados, para o site mostrar o aviso certo. fonteClima: nasa, http ou mock. */
    public record Info(String fonteClima, boolean climaDeDemonstracao) {
    }
}
