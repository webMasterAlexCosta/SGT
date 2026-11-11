package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.BloqueioEscritaFrequenciaServico;
import com.example.siggtrat.servico.ElegibilidadeFechamentoFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Proposta P03: CPF é dado preservado, não prova de associação Pessoa/Empregado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdentidadeCanonicaFechamentoHttpIT extends ContratosHttpBase {
    @Autowired ElegibilidadeFechamentoFrequenciaServico elegibilidade;
    @Autowired BloqueioEscritaFrequenciaServico bloqueios;

    @Test
    void legadoComCpfCoincidenteNaoContaNaoProjetaENaoCriaFechamentoOuFonte() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "P03-CPF-NAO-ASSOCIA");
        LocalDate data = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        emTransacao(() -> {
            HistoricoVinculoContrato h = new HistoricoVinculoContrato();
            h.setEmpregado(em.find(Empregado.class, c.empregadoA.getId())); h.setContrato(em.find(Contrato.class, c.contratoA.getId()));
            h.setLotacao(em.find(Lotacao.class, c.lotacaoA.getId())); h.setJornada(em.find(Jornada.class, c.jornadaA.getId()));
            h.setCargo(em.find(Cargo.class, c.cargoA.getId())); h.setDataInicio(INICIO.atStartOfDay(ZoneId.of("America/Sao_Paulo")));
            em.persist(h);
            em.find(Empregado.class, c.empregadoA.getId()).setPessoaCanonica(null); em.flush(); return null;
        });
        Estado antes = estado(c);
        assertNull(antes.pessoaAssociada());
        assertEquals(c.pessoaA.getCpf(), antes.cpfEmpregado(), "Pré-condição: coincidência existe, associação não");
        assertEquals(0L, elegibilidade.contarExplicitos(List.of(c.empregadoA.getId()), data,
                c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
        assertEquals(0L, elegibilidade.contarSemFrequencia(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
        assertFalse(elegibilidade.existe(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
        elegibilidade.prepararProjecoesApos(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId()), 0, 5);
        ResponseStatusException identidade = assertThrows(ResponseStatusException.class,
                () -> emTransacao(() -> bloqueios.bloquearEmpregados(List.of(c.empregadoA.getId()))));
        assertEquals(409, identidade.getStatusCode().value());
        ResponseStatusException colisao = assertThrows(ResponseStatusException.class, () -> emTransacao(() -> {
            projecao.projetar(em.find(VinculoContratual.class, c.vinculoA.getId()), em.find(AlocacaoVinculo.class, c.alocacaoA.getId())); return null;
        }));
        assertEquals(409, colisao.getStatusCode().value(), "Guarda existente de projeção continua recusando fusão");
        executar(c.operacionalA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(mapa("dataReferencia", data, "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                        "contratoIds", List.of(c.contratoA.getId()), "alteracoes", List.of()))),
                400, "Escopo sem identidade projetável não cria trabalho nem fonte por CPF coincidente");
        assertEquals(antes, estado(c), "CPF, cadastros, vínculos e história permanecem; nenhuma fusão/fonte/job");
    }

    @Test
    void associacaoExplicitaContaBloqueiaEGravaSomenteNaGestaoAutorizada() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "P03-ASSOCIACAO-EXPLICITA");
        LocalDate data = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        assertEquals(1L, elegibilidade.contarExplicitos(List.of(c.empregadoA.getId()), data,
                c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
        assertEquals(1L, elegibilidade.contarSemFrequencia(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
        emTransacao(() -> {
            Empregado e = bloqueios.bloquearEmpregados(List.of(c.empregadoA.getId())).get(c.empregadoA.getId());
            assertEquals(c.pessoaA.getId(), e.getPessoaCanonica().getId()); return null;
        });
        var corpo = mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Controle explícito da Gestão responsável", "chaveIdempotencia", "p03-" + UUID.randomUUID());
        executar(c.localB, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(corpo)), 403,
                "CPF e identidade explícita não ampliam autorização para outra Gestão");
        assertEquals(0L, estado(c).frequencias());
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(corpo)),
                200, "Pessoa explicitamente associada grava no próprio contexto histórico"));
        assertEquals(c.pessoaA.getId().longValue(), criada.path("pessoaId").asLong());
        assertEquals(c.empregadoA.getId().longValue(), criada.path("empregado").path("id").asLong());
        assertEquals(0L, elegibilidade.contarSemFrequencia(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
    }

    @Test
    void ausenciaTotalDeEmpregadoPermiteNovaProjecaoDoVinculoExplicitoSemRecadastrarPessoa() {
        Cenario c = criarCenarioPersistido();
        var novo = emTransacao(() -> {
            Contrato contrato = contrato(c.gestaoA, c.tipoA, "P03-NOVO-" + UUID.randomUUID().toString().substring(0, 8));
            ComposicaoContratual comp = composicao(contrato, c.lotacaoA, c.jornadaA, c.cargoA, 5);
            Pessoa pessoa = pessoa("Pessoa canônica sem projeção " + c.gestaoA.getId(), cpf(Math.toIntExact(870000000L + c.gestaoA.getId())));
            VinculoContratual v = vinculo(pessoa, contrato); alocacao(v, comp); em.flush();
            return new Novo(pessoa.getId(), v.getId(), contrato.getId(), pessoa.getCpf());
        });
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(e) from Empregado e where e.cpf=:cpf", Long.class)
                .setParameter("cpf", novo.cpf()).getSingleResult()).longValue());
        LocalDate data = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        assertEquals(1L, elegibilidade.contarSemFrequencia(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(novo.contrato())));
        elegibilidade.prepararProjecoesApos(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(novo.contrato()), 0, 5);
        emTransacao(() -> {
            Empregado e = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:p", Empregado.class)
                    .setParameter("p", novo.pessoa()).getSingleResult();
            assertEquals(novo.cpf(), e.getCpf());
            assertEquals(novo.pessoa(), e.getPessoaCanonica().getId());
            assertEquals(1L, em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", novo.cpf()).getSingleResult());
            assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.pessoa.id=:p", Long.class)
                    .setParameter("p", novo.pessoa()).getSingleResult(), "Preparar projeção não fabrica frequência");
            return null;
        });
    }

    private Estado estado(Cenario c) {
        return emTransacao(() -> {
            Empregado e = em.find(Empregado.class, c.empregadoA.getId());
            List<String> historicos = em.createQuery("select h.id,h.contratoSnapshotNumero,h.gestaoLocalSnapshotId,h.empregadoSnapshotNome,h.dataInicio,h.dataFim,h.version from HistoricoVinculoContrato h where h.empregado.id=:e order by h.id", Object[].class)
                    .setParameter("e", e.getId()).getResultList().stream().map(Arrays::toString).toList();
            List<String> hashes = em.createQuery("select h.hashEvento,h.pessoaCpfFotografia,h.pessoaNomeFotografia from EventoVinculoContratual h where h.vinculo.id=:v order by h.id", Object[].class)
                    .setParameter("v", c.vinculoA.getId()).getResultList().stream().map(Arrays::toString).toList();
            return new Estado(e.getCpf(), em.find(Pessoa.class, c.pessoaA.getId()).getCpf(), e.getPessoaCanonica() == null ? null : e.getPessoaCanonica().getId(), e.getVersion(),
                    em.createQuery("select count(p) from Pessoa p where p.id=:id", Long.class).setParameter("id", c.pessoaA.getId()).getSingleResult(),
                    em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:id", Long.class).setParameter("id", c.pessoaA.getId()).getSingleResult(),
                    em.createQuery("select count(f) from Frequencia f where f.empregado.id=:id or f.pessoa.id=:p", Long.class).setParameter("id", e.getId()).setParameter("p", c.pessoaA.getId()).getSingleResult(),
                    em.createQuery("select count(f) from FechamentoFrequencia f where f.lotacao.id=:id", Long.class).setParameter("id", c.lotacaoA.getId()).getSingleResult(),
                    hashes, historicos);
        });
    }
    private record Novo(Long pessoa, Long vinculo, Long contrato, String cpf) {}
    private record Estado(String cpfEmpregado, String cpfPessoa, Long pessoaAssociada, long versaoEmpregado, long pessoas, long vinculos,
                          long frequencias, long fechamentos, List<String> hashesVinculo, List<String> historicos) {}
}
