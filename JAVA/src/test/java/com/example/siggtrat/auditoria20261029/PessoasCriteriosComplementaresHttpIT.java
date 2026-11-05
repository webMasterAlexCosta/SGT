package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasCriteriosComplementaresHttpIT extends ContratosHttpBase {
    @Test
    void recontratacaoMantemTodosOpcionaisEApenasLimpezaExplicitaRemoveCampo() throws Exception {
        Cenario c = emTransacao(this::criarCenario);
        LocalDate hoje = RelogioSistema.hoje();
        Map<String, Object> pessoa = mapa("nome", "Pessoa com opcionais conservados", "cpf", cpf(seed(c, 734000000)),
                "rg", "RG-PRESERVADO", "ctpsSerie", "CTPS-PRESERVADA", "nitPisPasep", "NIT-PRESERVADO",
                "dataNascimento", "1990-03-04", "email", "mantido@example.test",
                "endereco", mapa("logradouro", "Rua Mantida", "numero", "7", "bairro", "Centro",
                        "cidade", "Cidade", "uf", "SP", "cep", "12345678"));
        JsonNode primeiro = enviar(c.localA, "/api/vinculos-contratuais",
                mapa("pessoa", pessoa, "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                        "dataInicio", hoje.minusDays(10), "dataFim", hoje.minusDays(2)), 201,
                "COL011 cadastro original completo com término humano explícito");
        long pessoaId = primeiro.path("pessoaId").asLong();
        JsonNode antes = consultar(c.localA, "/api/pessoas/" + pessoaId, "Opcionais anteriores à recontratação");
        JsonNode novo = enviar(c.localA, "/api/vinculos-contratuais",
                mapa("pessoaId", pessoaId, "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                        "dataInicio", hoje.minusDays(1)), 201, "COL011 recontratação omite todos os opcionais");
        assertThat(novo.path("pessoaId").asLong()).isEqualTo(pessoaId);
        assertThat(novo.path("id").asLong()).isNotEqualTo(primeiro.path("id").asLong());
        JsonNode depois = consultar(c.localA, "/api/pessoas/" + pessoaId, "Mesma Pessoa mantém dados pessoais");
        for (String campo : List.of("rg", "ctpsSerie", "nitPisPasep", "dataNascimento", "email", "endereco"))
            assertThat(depois.path(campo)).as("Campo omitido na recontratação: %s", campo).isEqualTo(antes.path(campo));
        JsonNode limpa = dados(executar(c.localA, put("/api/pessoas/" + pessoaId + "/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(mapa("limparRg", true))), 200,
                "COL011 limpeza intencional é específica ao campo"));
        assertThat(limpa.path("rg").isNull()).isTrue();
        for (String campo : List.of("ctpsSerie", "nitPisPasep", "dataNascimento", "email", "endereco"))
            assertThat(limpa.path(campo)).as("Limpeza de RG preserva %s", campo).isEqualTo(antes.path(campo));
        assertThat(emTransacao(() -> em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult())).isEqualTo(1L);
    }

    @Test
    void correcaoCpfDocumentadaEColisaoPreservamAsDuasIdentidades() throws Exception {
        Cenario c = emTransacao(this::criarCenario);
        String antigo = c.pessoaA.getCpf(), novo = cpf(seed(c, 735000000));
        Map<String, Object> correcao = mapa("tipo", "CPF", "novoValor", novo,
                "motivo", "Erro material comprovado de CPF", "evidencia", "SEI-CRITERIO-CPF",
                "autorizacao", "ATO-CRITERIO-CPF", "impactosEncontrados", "Pessoa e projeção do vínculo existente");
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", correcao, 200,
                "COL006 correção excepcional documentada preserva identidade");
        JsonNode trilha = consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade",
                "COL006 valores, documentos, autoria e registro auditáveis");
        assertThat(trilha).hasSize(1);
        JsonNode evento = trilha.get(0);
        assertThat(evento.path("valorAnterior").asText()).isEqualTo(antigo);
        assertThat(evento.path("valorNovo").asText()).isEqualTo(novo);
        assertThat(evento.path("responsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(evento.path("registradoEm").asText()).isNotBlank();
        for (String campo : List.of("motivo", "evidencia", "autorizacao", "impactosEncontrados"))
            assertThat(evento.path(campo).asText()).isEqualTo(correcao.get(campo));
        List<String> antes = identidades(c);
        correcao.put("novoValor", c.pessoaB.getCpf());
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", correcao, 409,
                "COL007 CPF de outra Pessoa não pode fundir nem reassociar identidades");
        assertThat(identidades(c)).as("Ambas as Pessoas, Empregados, vínculos e correção anterior preservados em nova transação")
                .isEqualTo(antes);
        correcao.put("novoValor", "11111111111");
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", correcao, 400,
                "COL006 o novo CPF também passa pela validação de dígitos");
        assertThat(identidades(c)).isEqualTo(antes);
    }

    @Test
    void matriculaNormalizadaConflitaSomenteDentroDoMesmoContrato() throws Exception {
        Cenario c = emTransacao(this::criarCenario);
        String duplicado = cpf(seed(c, 737000000));
        JsonNode primeiro = enviar(c.localA, "/api/vinculos-contratuais",
                cadastro(c, cpf(seed(c, 736000000)), "  mat   compartilhada  ", DATA), 201,
                "MAT002 matrícula informada é normalizada dentro do contrato");
        assertThat(primeiro.path("matricula").asText()).isEqualTo("MAT COMPARTILHADA");
        enviar(c.localA, "/api/vinculos-contratuais", cadastro(c, duplicado, "MAT COMPARTILHADA", DATA), 409,
                "MAT002 mesmo valor normalizado no mesmo contrato deve conflitar");
        assertThat(emTransacao(() -> em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                .setParameter("cpf", duplicado).getSingleResult())).isZero();
        Map<String, Object> outroContrato = cadastro(c, cpf(seed(c, 738000000)), "mat compartilhada", DATA);
        outroContrato.put("contratoId", c.contratoB.getId()); outroContrato.put("composicaoId", c.composicaoB.getId());
        JsonNode outro = enviar(c.localB, "/api/vinculos-contratuais", outroContrato, 201,
                "MAT002 contratos distintos possuem espaços de matrícula independentes");
        assertThat(outro.path("matricula").asText()).isEqualTo("MAT COMPARTILHADA");
        assertThat(emTransacao(() -> em.createQuery("select r.contrato.id from ReservaMatriculaContrato r "
                        + "where r.matricula=:m and r.contrato.id in :ids", Long.class)
                .setParameter("m", "MAT COMPARTILHADA").setParameter("ids", List.of(c.contratoA.getId(), c.contratoB.getId()))
                .getResultList())).containsExactlyInAnyOrder(c.contratoA.getId(), c.contratoB.getId());
    }

    @Test
    void pesquisaDeInicioUsaMesEfetivoEmVezDoRegistro() throws Exception {
        Cenario c = emTransacao(this::criarCenario);
        LocalDate inicio = RelogioSistema.hoje().minusMonths(1).withDayOfMonth(15);
        JsonNode criado = enviar(c.localA, "/api/vinculos-contratuais", cadastro(c, cpf(seed(c, 739000000)), null, inicio),
                201, "VIN002 cadastro tardio informa início efetivo do mês anterior");
        long pessoaId = criado.path("pessoaId").asLong();
        String rota = "/api/vinculos-contratuais?pessoaId=" + pessoaId;
        YearMonth mesFato = YearMonth.from(inicio), mesRegistro = YearMonth.from(RelogioSistema.hoje());
        JsonNode porFato = consultar(c.localA, rota + "&inicioDe=" + mesFato.atDay(1) + "&inicioAte=" + mesFato.atEndOfMonth(),
                "VIN002 pesquisa de iniciados inclui a data efetiva, embora cadastrada posteriormente");
        assertThat(porFato.path("content")).hasSize(1);
        assertThat(porFato.path("content").get(0).path("id").asLong()).isEqualTo(criado.path("id").asLong());
        JsonNode porRegistro = consultar(c.localA, rota + "&inicioDe=" + mesRegistro.atDay(1) + "&inicioAte=" + mesRegistro.atEndOfMonth(),
                "VIN002 mês de registro não substitui o mês de início");
        assertThat(porRegistro.path("content")).isEmpty();
        assertThat(porRegistro.path("totalElements").asLong()).isZero();
    }

    private Map<String, Object> cadastro(Cenario c, String cpf, String matricula, LocalDate inicio) {
        return mapa("pessoa", mapa("nome", "Pessoa critérios " + cpf, "cpf", cpf), "contratoId", c.contratoA.getId(),
                "composicaoId", c.composicaoA.getId(), "matricula", matricula, "dataInicio", inicio);
    }

    private List<String> identidades(Cenario c) {
        return emTransacao(() -> List.of(identidade(c.pessoaA.getId(), c.empregadoA.getId()),
                identidade(c.pessoaB.getId(), c.empregadoB.getId())));
    }

    private String identidade(long pessoaId, long empregadoId) {
        Pessoa p = em.find(Pessoa.class, pessoaId); Empregado e = em.find(Empregado.class, empregadoId);
        return p.getId() + "|" + p.getNome() + "|" + p.getCpf() + "|" + e.getId() + "|" + e.getPessoaCanonica().getId()
                + "|" + e.getCpf() + "|" + em.createQuery("select v.id from VinculoContratual v where v.pessoa.id=:p order by v.id", Long.class)
                .setParameter("p", pessoaId).getResultList()
                + "|" + em.createQuery("select c.id from CorrecaoPessoa c where c.pessoa.id=:p order by c.id", Long.class)
                .setParameter("p", pessoaId).getResultList();
    }

    private int seed(Cenario c, int base) { return Math.toIntExact(base + c.contratoA.getId()); }
}
