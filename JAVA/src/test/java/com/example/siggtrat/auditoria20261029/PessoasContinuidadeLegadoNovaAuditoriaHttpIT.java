package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Provas RED propostas; não executadas pelo autor da revisão independente. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasContinuidadeLegadoNovaAuditoriaHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void prorrogacaoTempestivaMantemAlocacaoDoVinculoAbertoNoPrimeiroDiaEstendido() throws Exception {
        Preparado f = preparar();
        JsonNode vinculo = cadastrar(f);
        long vinculoId = vinculo.path("id").asLong();
        long pessoaId = vinculo.path("pessoaId").asLong();
        LocalDate primeiroDiaProrrogado = f.contrato().fim().plusDays(1);
        LocalDate novoFim = f.contrato().fim().plusDays(30);
        var req = aditamento("PRORROGACAO-TEMPESTIVA-ALOCACAO", RelogioSistema.hoje());
        req.put("novaDataFim", novoFim);
        JsonNode previa = enviar(f.cenario().localA, base(f.contrato().id()) + "/aditamentos/previa", req, 200,
                "TMP015 prévia de prorrogação tempestiva com vínculo aberto");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(f.cenario().localA, base(f.contrato().id()) + "/aditamentos", req, 201,
                "ADI005 extensão registrada antes do fim original não é prorrogação tardia");

        JsonNode posicaoVinculo = consultar(f.cenario().localA, V + "/pessoas/" + pessoaId + "/posicao?data=" + primeiroDiaProrrogado,
                "VIN008 vínculo aberto continua coberto pela vigência prorrogada");
        assertThat(posicaoVinculo.path("vinculos").get(0).path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        JsonNode posicao = consultar(f.cenario().localA, V + "/" + vinculoId + "/alocacoes/posicao?data=" + primeiroDiaProrrogado,
                "ALO007 todo dia do vínculo vigente deve possuir uma única alocação");
        assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicao.path("lotacaoId").asLong()).isEqualTo(f.cenario().lotacaoA.getId());
        assertThat(posicao.path("composicaoBaseId").asLong()).isEqualTo(f.contrato().composicao1());
    }

    @Test
    void cadastroCanonicoFicaVisivelNaGradeDeFrequenciaLegada() throws Exception {
        Preparado f = preparar();
        JsonNode vinculo = cadastrar(f);
        long pessoaId = vinculo.path("pessoaId").asLong();
        long empregadoId = emTransacao(() -> em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult());
        String filtro = "?dataRef=" + RelogioSistema.hoje() + "&contratoId=" + f.contrato().id()
                + "&lotacaoId=" + f.cenario().lotacaoA.getId() + "&turnoId=" + f.cenario().jornadaA.getId();
        JsonNode grade = consultar(f.cenario().operacionalA, "/api/empregados/filtrados-frequencia" + filtro,
                "TMP001 HIS005 grade legada deve refletir vínculo/alocação canônicos sem segundo cadastro");
        List<Long> ids = new ArrayList<>();
        grade.path("content").forEach(item -> ids.add(item.path("id").asLong()));
        assertThat(ids).contains(empregadoId);
        assertThat(grade.path("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void posicaoDeAlocacaoSemEventosPreservaFotografiasMesmoAposRenomearCatalogos() throws Exception {
        Cenario c = criarCenarioPersistido();
        String lotacaoAntes = c.lotacaoA.getNome();
        String jornadaAntes = c.jornadaA.getDescricao();
        String cargoAntes = c.cargoA.getNome();
        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult()).isZero();
            em.find(Lotacao.class, c.lotacaoA.getId()).setNome("Lotação posterior sem reescrever passado");
            em.find(Jornada.class, c.jornadaA.getId()).setDescricao("Turno posterior sem reescrever passado");
            em.find(Cargo.class, c.cargoA.getId()).setNome("Cargo posterior sem reescrever passado");
            return null;
        });
        JsonNode posicao = consultar(c.localA, V + "/" + c.vinculoA.getId() + "/alocacoes/posicao?data=" + DATA,
                "CAT004 HIS004 fallback de faixa sem eventos deve usar fotografia existente");
        assertThat(posicao.path("lotacaoNome").asText()).isEqualTo(lotacaoAntes);
        assertThat(posicao.path("jornadaDescricao").asText()).isEqualTo(jornadaAntes);
        assertThat(posicao.path("cargoNome").asText()).isEqualTo(cargoAntes);
    }

    private Preparado preparar() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                return new Preparado(c, publicado(c, 2, null, false));
            } catch (Exception e) {
                throw new IllegalStateException("Falha na preparação HTTP de continuidade/legado", e);
            }
        });
    }

    private JsonNode cadastrar(Preparado f) throws Exception {
        String cpfNovo = cpf(Math.toIntExact(910000000L + f.contrato().id()));
        return enviar(f.cenario().localA, V, mapa("pessoa", mapa("nome", "Pessoa continuidade " + f.contrato().id(), "cpf", cpfNovo),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(), "dataInicio", RelogioSistema.hoje()),
                201, "VIN001 cadastro integrado canônico pela API");
    }

    private record Preparado(Cenario cenario, NovoContrato contrato) {}
}
