package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.modelo.Cultura;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/** Tabela de culturas carregada do JSON (resources/culturas.json). */
public class CulturaRepository {

    private final List<Cultura> culturas;

    public CulturaRepository(List<Cultura> culturas) {
        this.culturas = List.copyOf(culturas);
    }

    public static CulturaRepository deJson(InputStream json, ObjectMapper mapper) {
        try (json) {
            return new CulturaRepository(mapper.readValue(json, new TypeReference<List<Cultura>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui ler a tabela de culturas", e);
        }
    }

    public List<Cultura> todas() {
        return culturas;
    }

    public Optional<Cultura> porId(String id) {
        return culturas.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public Cultura obter(String id) {
        return porId(id).orElseThrow(() -> new IllegalArgumentException("Cultura desconhecida: " + id));
    }
}
