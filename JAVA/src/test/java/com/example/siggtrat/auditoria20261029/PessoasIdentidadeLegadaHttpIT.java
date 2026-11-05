package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasIdentidadeLegadaHttpIT extends ContratosHttpBase {
    @Test
    void cpfCorrigidoNaoAutorizaAdotarProjecaoLegadaSemAssociacao() throws Exception {
        Preparado f = emTransacao(() -> {
            Cenario c = criarCenario(); c.empregadoA.setPessoaCanonica(null);
            Ocorrencia legado = new Ocorrencia(); legado.setEmpregado(c.empregadoA); legado.setDataOcorrencia(DATA);
            legado.setQuantidadeDias(1); legado.setMotivo("FALTA"); legado.setObservacao("HISTORIA-DA-IDENTIDADE-ANTERIOR");
            em.persist(legado); em.flush();
            return new Preparado(c, legado.getId(), c.empregadoA.getCpf(), c.empregadoA.getNome());
        });
        String corrigido = cpf(Math.toIntExact(741000000L + f.c().pessoaA.getId()));
        enviar(f.c().localA, "/api/pessoas/" + f.c().pessoaA.getId() + "/correcoes-identidade",
                mapa("tipo", "CPF", "novoValor", corrigido, "motivo", "Erro material da raiz original",
                        "evidencia", "SEI-NAO-FUNDIR-LEGADO", "autorizacao", "ATO-NAO-FUNDIR-LEGADO",
                        "impactosEncontrados", "Legado sem associação exige identificação documental própria"),
                200, "Correção de Pessoa preserva a necessidade de tratar identidade legada não associada");
        enviar(f.c().localB, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Titular real distinto do cadastro legado", "cpf", f.cpfAnterior()),
                        "contratoId", f.c().contratoB.getId(), "composicaoId", f.c().composicaoB.getId(), "dataInicio", DATA),
                409, "COL007 coincidência de CPF não autoriza adoção automática do Empregado e de sua história");
        emTransacao(() -> {
            Empregado legado = em.find(Empregado.class, f.c().empregadoA.getId());
            assertThat(legado.getPessoaCanonica()).isNull();
            assertThat(legado.getNome()).isEqualTo(f.nomeAnterior());
            assertThat(legado.getCpf()).isEqualTo(f.cpfAnterior());
            assertThat(legado.getGestaoLocal().getId()).isEqualTo(f.c().gestaoA.getId());
            assertThat(em.find(Pessoa.class, f.c().pessoaA.getId()).getCpf()).isEqualTo(corrigido);
            assertThat(em.find(Ocorrencia.class, f.ocorrenciaId()).getObservacao()).isEqualTo("HISTORIA-DA-IDENTIDADE-ANTERIOR");
            assertThat(em.find(Ocorrencia.class, f.ocorrenciaId()).getEmpregado().getId()).isEqualTo(legado.getId());
            assertThat(em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", f.cpfAnterior()).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                    .setParameter("c", f.c().contratoB.getId()).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    private record Preparado(Cenario c, long ocorrenciaId, String cpfAnterior, String nomeAnterior) {}
}
