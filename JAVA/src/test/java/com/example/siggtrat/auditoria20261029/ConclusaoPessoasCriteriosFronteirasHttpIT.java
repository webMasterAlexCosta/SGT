package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.TipoContratacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Fronteiras identificadas pela revisão dos critérios, com confirmação em novas transações MySQL. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoPessoasCriteriosFronteirasHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @ParameterizedTest
    @ValueSource(strings = {"CPF_AUSENTE", "CPF_NULO", "CPF_BRANCO", "CPF_CURTO", "CPF_LONGO",
            "CPF_DIGITOS_INVALIDOS", "CPF_REPETIDO", "NOME_AUSENTE", "NOME_NULO", "NOME_BRANCO"})
    void nomeECpfInvalidosRecusamCadastroSemPessoaProjecaoVinculoAlocacaoOuReservaParcial(String caso) throws Exception {
        Preparado f = preparar();
        String cpfValido = cpf(Math.toIntExact(661000000L + f.n().id()));
        Map<String, Object> pessoa = mapa("nome", "Pessoa fronteira obrigatória " + f.n().id(), "cpf", cpfValido);
        switch (caso) {
            case "CPF_AUSENTE" -> pessoa.remove("cpf");
            case "CPF_NULO" -> pessoa.put("cpf", null);
            case "CPF_BRANCO" -> pessoa.put("cpf", "   ");
            case "CPF_CURTO" -> pessoa.put("cpf", cpfValido.substring(1));
            case "CPF_LONGO" -> pessoa.put("cpf", cpfValido + "0");
            case "CPF_DIGITOS_INVALIDOS" -> pessoa.put("cpf", cpfValido.substring(0, 10)
                    + ((cpfValido.charAt(10) - '0' + 1) % 10));
            case "CPF_REPETIDO" -> pessoa.put("cpf", "11111111111");
            case "NOME_AUSENTE" -> pessoa.remove("nome");
            case "NOME_NULO" -> pessoa.put("nome", null);
            case "NOME_BRANCO" -> pessoa.put("nome", "   ");
            default -> throw new IllegalArgumentException(caso);
        }
        Map<String, Object> req = mapa("pessoa", pessoa, "contratoId", f.n().id(),
                "composicaoId", f.n().composicao1(), "dataInicio", RelogioSistema.hoje(),
                "matricula", "FRONTEIRA-" + f.n().id());
        List<Long> antes = estadoCadastro();
        var recusado = executar(f.c().localA, post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),
                400, "COL001 COL005 entrada inválida não confirma cadastro integrado: " + caso);
        assertThat(mapper.readTree(recusado.getResponse().getContentAsString()).path("message").asText())
                .containsIgnoringCase(caso.startsWith("NOME") ? "nome" : "CPF");
        assertThat(estadoCadastro()).as("Nova transação: Pessoa, Empregado, Vínculo, Alocação, reservas e eventos intactos")
                .isEqualTo(antes);
    }

    @Test
    void inicioNoMesmoUltimoDiaConflitaMasDiaSeguintePermiteRecontratacaoEntreGestoes() throws Exception {
        Preparado f = preparar();
        LocalDate ultimoDia = RelogioSistema.hoje().minusDays(1);
        JsonNode anterior = enviar(f.c().localA, V, mapa(
                "pessoa", mapa("nome", "Pessoa fronteira inclusiva " + f.n().id(),
                        "cpf", cpf(Math.toIntExact(662000000L + f.n().id()))),
                "contratoId", f.n().id(), "composicaoId", f.n().composicao1(),
                "dataInicio", ultimoDia.minusDays(5), "dataFim", ultimoDia),
                201, "VIN004 vínculo original termina no último dia humano expressamente informado");
        long pessoaId = anterior.path("pessoaId").asLong(), vinculoId = anterior.path("id").asLong();
        Map<String, Object> proximo = mapa("pessoaId", pessoaId, "contratoId", f.c().contratoB.getId(),
                "composicaoId", f.c().composicaoB.getId(), "dataInicio", ultimoDia);
        List<Long> antes = estadoCadastro();
        enviar(f.c().localB, V, proximo, 409,
                "VIN003 VIN004 o dia D já pertence ao vínculo anterior, inclusive entre Gestões distintas");
        assertThat(estadoCadastro()).as("Recusa da fronteira inclusiva não deixa vínculo, faixa ou evento parcial")
                .isEqualTo(antes);
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculoId).getDataFim())).isEqualTo(ultimoDia);

        proximo.put("dataInicio", ultimoDia.plusDays(1));
        JsonNode novo = enviar(f.c().localB, V, proximo, 201,
                "VIN004 D+1 mantém troca direta sem exigir dia vazio entre contratos");
        assertThat(novo.path("pessoaId").asLong()).isEqualTo(pessoaId);
        assertThat(novo.path("id").asLong()).isNotEqualTo(vinculoId);
        assertThat(novo.path("dataInicio").asText()).isEqualTo(ultimoDia.plusDays(1).toString());
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, vinculoId).getDataFim()).isEqualTo(ultimoDia);
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:id", Long.class)
                    .setParameter("id", pessoaId).getSingleResult()).isEqualTo(2L);
            assertThat(em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:id", Long.class)
                    .setParameter("id", pessoaId).getSingleResult()).isEqualTo(1L);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"cargos", "lotacoes", "jornadas", "tipos-contratacao", "motivos-desligamento"})
    void catalogoInativadoPermaneceNaListagemAutorizadaComMesmoId(String recurso) throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String, Object> body = catalogo(recurso, c, "Catálogo inativo consultável " + UUID.randomUUID());
        long id = enviar(c.localA, "/api/" + recurso, body, recurso.equals("cargos") ? 200 : 201,
                "CAT003 catálogo isolado sem programação dependente").path("id").asLong();
        if (Set.of("cargos", "lotacoes", "jornadas").contains(recurso)) {
            executar(c.localA, delete("/api/" + recurso + "/" + id), 204, "CAT003 inativação preserva o cadastro");
        } else {
            body.put("ativo", false);
            executar(c.localA, put("/api/" + recurso + "/" + id).contentType(MediaType.APPLICATION_JSON)
                    .content(json(body)), 200, "CAT003 inativação explícita do catálogo");
        }
        JsonNode resposta = consultar(c.localA, "/api/" + recurso, "CAT003 listagem depois da inativação");
        JsonNode itens = resposta.has("content") ? resposta.path("content") : resposta;
        List<JsonNode> encontrados = new ArrayList<>();
        itens.forEach(item -> { if (item.path("id").asLong() == id) encontrados.add(item); });
        assertThat(encontrados).as("Catálogo inativo permanece exatamente uma vez na listagem de sua Gestão").hasSize(1);
        assertThat(encontrados.get(0).path("ativo").asBoolean()).isFalse();
        assertThat(encontrados.get(0).path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(consultar(c.localA, "/api/" + recurso + "/" + id, "CAT003 detalhe histórico continua acessível")
                .path("id").asLong()).isEqualTo(id);
        assertThat(emTransacao(() -> switch (recurso) {
            case "cargos" -> em.find(Cargo.class, id).getAtivo();
            case "lotacoes" -> em.find(Lotacao.class, id).getAtivo();
            case "jornadas" -> em.find(Jornada.class, id).getAtivo();
            case "tipos-contratacao" -> em.find(TipoContratacao.class, id).getAtivo();
            default -> em.find(MotivoDesligamento.class, id).getAtivo();
        })).isFalse();
    }

    @Test
    void todosOpcionaisInformadosPossuemValoresExatosPersistidosERecontratacaoNaoOsApaga() throws Exception {
        Preparado f = preparar();
        LocalDate fimAnterior = RelogioSistema.hoje().minusDays(1);
        Map<String, Object> pessoa = mapa("nome", "Pessoa opcionais exatos " + f.n().id(),
                "cpf", cpf(Math.toIntExact(663000000L + f.n().id())), "rg", "RG-EXATO", "ctpsSerie", "CTPS-EXATA",
                "nitPisPasep", "12345678901", "dataNascimento", "1990-03-04", "email", "exato@example.test",
                "endereco", mapa("logradouro", "Rua Exata", "numero", "7", "bairro", "Centro", "cidade", "Curitiba",
                        "uf", "PR", "cep", "80000000"));
        JsonNode primeiro = enviar(f.c().localA, V, mapa("pessoa", pessoa, "contratoId", f.n().id(),
                        "composicaoId", f.n().composicao1(), "dataInicio", fimAnterior.minusDays(5), "dataFim", fimAnterior),
                201, "COL002 COL003 opcionais informados possuem conteúdo verificável");
        long pessoaId = primeiro.path("pessoaId").asLong();
        conferirOpcionais(consultar(f.c().localA, "/api/pessoas/" + pessoaId, "COL002 valores efetivamente cadastrados"));
        emTransacao(() -> {
            Pessoa p = em.find(Pessoa.class, pessoaId);
            assertThat(p.getRg()).isEqualTo("RG-EXATO");
            assertThat(p.getCtpsSerie()).isEqualTo("CTPS-EXATA");
            assertThat(p.getNitPisPasep()).isEqualTo("12345678901");
            assertThat(p.getDataNascimento()).isEqualTo(LocalDate.of(1990, 3, 4));
            assertThat(p.getEmail()).isEqualTo("exato@example.test");
            assertThat(p.getEndereco().getLogradouro()).isEqualTo("RUA EXATA");
            return null;
        });
        JsonNode novo = enviar(f.c().localA, V, mapa("pessoaId", pessoaId, "contratoId", f.n().id(),
                        "composicaoId", f.n().composicao1(), "dataInicio", fimAnterior.plusDays(1)),
                201, "COL011 recontratação com pessoaId omite e preserva todos os opcionais");
        assertThat(novo.path("pessoaId").asLong()).isEqualTo(pessoaId);
        assertThat(novo.path("id").asLong()).isNotEqualTo(primeiro.path("id").asLong());
        conferirOpcionais(consultar(f.c().localA, "/api/pessoas/" + pessoaId, "COL011 omissão não apaga conteúdo conhecido"));
        JsonNode limpo = dados(executar(f.c().localA, put("/api/pessoas/" + pessoaId + "/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(mapa("limparNitPisPasep", true))), 200,
                "COL011 limpeza explícita remove só o NIT/PIS escolhido"));
        assertThat(limpo.path("nitPisPasep").isNull()).isTrue();
        assertThat(limpo.path("dataNascimento").asText()).isEqualTo("1990-03-04");
        assertThat(limpo.path("ctpsSerie").asText()).isEqualTo("CTPS-EXATA");
        assertThat(emTransacao(() -> em.find(Pessoa.class, pessoaId).getNitPisPasep())).isNull();
    }

    private void conferirOpcionais(JsonNode pessoa) {
        assertThat(pessoa.path("rg").asText()).isEqualTo("RG-EXATO");
        assertThat(pessoa.path("ctpsSerie").asText()).isEqualTo("CTPS-EXATA");
        assertThat(pessoa.path("nitPisPasep").asText()).isEqualTo("12345678901");
        assertThat(pessoa.path("dataNascimento").asText()).isEqualTo("1990-03-04");
        assertThat(pessoa.path("email").asText()).isEqualTo("exato@example.test");
        assertThat(pessoa.path("endereco").path("logradouro").asText()).isEqualTo("RUA EXATA");
    }

    private Preparado preparar() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                return new Preparado(c, publicado(c, 5, null, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private List<Long> estadoCadastro() {
        return emTransacao(() -> List.of("Pessoa", "Empregado", "VinculoContratual", "AlocacaoVinculo",
                        "ReservaMatriculaContrato", "EventoVinculoContratual", "EventoAlocacaoVinculo").stream()
                .map(entidade -> em.createQuery("select count(e) from " + entidade + " e", Long.class).getSingleResult())
                .toList());
    }

    private Map<String, Object> catalogo(String recurso, Cenario c, String nome) {
        Map<String, Object> req = mapa(Set.of("cargos", "lotacoes").contains(recurso) ? "nome" : "descricao", nome,
                "ativo", true, "gestaoLocalId", c.gestaoA.getId());
        if (recurso.equals("motivos-desligamento")) req.put("categoria", "DESLIGAMENTO_INDIVIDUAL");
        if (recurso.equals("tipos-contratacao")) req.put("matriculaVinculoObrigatoria", false);
        if (recurso.equals("lotacoes")) {
            req.put("municipio", "CURITIBA"); req.put("turnos", List.of(Map.of("id", c.jornadaA.getId())));
        }
        if (recurso.equals("jornadas")) {
            List<Map<String, Object>> dias = new ArrayList<>();
            for (DayOfWeek dia : DayOfWeek.values()) {
                Map<String, Object> item = mapa("diaSemana", dia.name(), "diaTrabalho", dia.getValue() <= 5);
                if (dia.getValue() <= 5) { item.put("entrada1", "08:00"); item.put("saida1", "16:00"); }
                dias.add(item);
            }
            req.put("dias", dias);
        }
        return req;
    }

    private record Preparado(Cenario c, NovoContrato n) {}
}
