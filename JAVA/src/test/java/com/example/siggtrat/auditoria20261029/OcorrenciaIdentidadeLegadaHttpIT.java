package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** A ausência legítima de associação no legado não comprova identidade por coincidência de CPF. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaIdentidadeLegadaHttpIT extends ContratosHttpBase {
    @Test
    void ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado() throws Exception {
        Preparado f = emTransacao(() -> {
            Cenario c = criarCenario();
            // Formato admitido pelo esquema legado: relação canônica e fotografias não existiam.
            c.empregadoA.setPessoaCanonica(null);
            Ocorrencia legado = new Ocorrencia(); legado.setEmpregado(c.empregadoA);
            legado.setDataOcorrencia(DATA); legado.setQuantidadeDias(1); legado.setMotivo("FALTA");
            legado.setObservacao("NARRATIVA-LEGADA-SEM-PROVA-DE-IDENTIDADE"); em.persist(legado); em.flush();
            assertThat(legado.getContextos()).isEmpty();
            assertThat(em.createQuery("select count(f) from Frequencia f where f.empregado.id=:e", Long.class)
                    .setParameter("e", c.empregadoA.getId()).getSingleResult()).isZero();
            return new Preparado(c, legado.getId(), c.empregadoA.getCpf());
        });
        enviar(f.c().localA, "/api/pessoas/" + f.c().pessoaA.getId() + "/correcoes-identidade",
                mapa("tipo", "CPF", "novoValor", cpf(Math.toIntExact(740000000L + f.c().pessoaA.getId())),
                        "motivo", "Erro material comprovado", "evidencia", "SEI-LEGADO-CPF", "autorizacao", "ATO-LEGADO-CPF",
                        "impactosEncontrados", "Identidade canônica corrigida; legado sem associação permanece sem prova"),
                200, "COL006 correção auditada não permite inventar associação de legado");
        emTransacao(() -> {
            // Uma raiz canônica importada com vínculo em B é outra Pessoa. Nenhum dado conecta o fato legado a ela.
            Pessoa titular = pessoa("Titular atual do CPF em outra Gestão", f.cpfAnterior());
            VinculoContratual v = vinculo(titular, em.find(Contrato.class, f.c().contratoB.getId()));
            alocacao(v, em.find(ComposicaoContratual.class, f.c().composicaoB.getId())); em.flush();
            assertThat(titular.getId()).isNotEqualTo(f.c().pessoaA.getId());
            assertThat(em.find(Empregado.class, f.c().empregadoA.getId()).getPessoaCanonica()).isNull();
            return null;
        });
        executar(f.c().localB, get("/api/ocorrencias/" + f.ocorrenciaId()), 403,
                "HIS005 COL007 PER001 CPF atual sem prova não transfere ocorrência legada para outra identidade/Gestão");
        emTransacao(() -> {
            Ocorrencia o = em.find(Ocorrencia.class, f.ocorrenciaId());
            assertThat(o.getEmpregado().getId()).isEqualTo(f.c().empregadoA.getId());
            assertThat(o.getEmpregado().getPessoaCanonica()).isNull();
            assertThat(o.getContextos()).isEmpty();
            assertThat(o.getObservacao()).isEqualTo("NARRATIVA-LEGADA-SEM-PROVA-DE-IDENTIDADE");
            return null;
        });
    }

    private record Preparado(Cenario c, long ocorrenciaId, String cpfAnterior) {}
}
