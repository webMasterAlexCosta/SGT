package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Não declarar como fotografia histórica um valor de configuração que não foi fotografado. */
class ContratoConfiguracaoLegadaHttpIT extends ContratosHttpBase {
    @Test
    void posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico() throws Exception {
        var c = criarCenario(); var n = legadoSemConfiguracaoFotografada(c);
        var foto = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio(),
                "CAT004/HIS006 configuração histórica desconhecida deve permanecer desconhecida").path("versaoContratual");
        assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").isNull()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoria").isNull()).isTrue();
        assertThat(foto.path("matriculaTipoContratacaoObrigatoria").isNull()).isTrue();
        assertThat(foto.path("diaSemanaReduzido").isNull()).isTrue();
        assertThat(foto.path("horasDiaReduzido").isNull()).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla() throws Exception {
        var c = criarCenarioPersistido(); var n = emTransacao(() -> legadoSemConfiguracaoFotografada(c));
        String cpfNovo = emTransacao(() -> {
            String candidato;
            do {
                candidato = cpf(100000000 + Math.floorMod(UUID.randomUUID().hashCode(), 800000000));
            } while (candidato.matches("(\\d)\\1{10}") || em.createQuery(
                    "select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", candidato).getSingleResult() != 0L);
            return candidato;
        });
        var req = mapa("pessoa", mapa("nome", "Pessoa de política desconhecida", "cpf", cpfNovo),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", n.inicio().plusDays(1));
        var negativa = executar(c.localA, post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)), 409,
                "MAT005 não dispensar matrícula com base em configuração atual sem prova do período");
        assertThat(mapper.readTree(negativa.getResponse().getContentAsString()).path("message").asText())
                .contains("política de matrícula não está comprovada");
        assertThat(emTransacao(() -> em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                .setParameter("cpf", cpfNovo).getSingleResult())).isZero();
        assertThat(emTransacao(() -> em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getSingleResult())).isZero();
        req.put("matricula", "LEGADO-COMPROVADO-001");
        enviar(c.localA, "/api/vinculos-contratuais", req, 201,
                "MAT005 matrícula válida permite a operação independentemente de obrigatoriedade desconhecida");
    }

    @Test
    void aditamentoPodeRemoverExplicitamenteConfiguracaoDeDiaReduzido() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var configurar = aditamento("ADI-DIA-CONFIGURAR", n.inicio().plusDays(1));
        configurar.put("novoDiaSemanaReduzido", "FRIDAY"); configurar.put("novasHorasDiaReduzido", 6);
        registrarAditamento(c, n.id(), configurar);
        var remover = aditamento("ADI-DIA-REMOVER", n.inicio().plusDays(2));
        remover.put("novaDescricao", "Remoção documentada da redução diária"); remover.put("removerConfiguracaoDiaReduzido", true);
        registrarAditamento(c, n.id(), remover);
        var antes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(1), "HIS001 configuração anterior preservada");
        var depois = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(2), "§5.9 limpeza é alteração explícita do ato");
        assertThat(antes.path("versaoContratual").path("horasDiaReduzido").asInt()).isEqualTo(6);
        assertThat(depois.path("versaoContratual").path("diaSemanaReduzido").isNull()).isTrue();
        assertThat(depois.path("versaoContratual").path("horasDiaReduzido").isNull()).isTrue();
    }

    @Test
    void aditamentoPodeRestaurarHerancaDaPoliticaDeMatriculaDoTipoFotografado() throws Exception {
        var c = criarCenario(); c.tipoA.setMatriculaVinculoObrigatoria(true); em.flush();
        var n = publicado(c, 5, null, false);
        var dispensar = aditamento("ADI-MATRICULA-DISPENSAR", n.inicio().plusDays(1));
        dispensar.put("novaMatriculaVinculoObrigatoria", false); registrarAditamento(c, n.id(), dispensar);
        var herdar = aditamento("ADI-MATRICULA-HERDAR", n.inicio().plusDays(2));
        herdar.put("novaDescricao", "Restauração documentada da política do Tipo"); herdar.put("restaurarPoliticaMatriculaDoTipo", true);
        registrarAditamento(c, n.id(), herdar);
        var antes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(1), "HIS001 dispensa contratual anterior preservada");
        var depois = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(2), "§5.9 retorno explícito à herança").path("versaoContratual");
        assertThat(antes.path("versaoContratual").path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        assertThat(depois.path("matriculaVinculoObrigatoria").isNull()).isTrue();
        assertThat(depois.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dia", "matricula"})
    void aditamentoRejeitaRemocaoEConfiguracaoContraditoriasNoMesmoAto(String campo) throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var req = aditamento("ADI-CONTRADITORIO", RelogioSistema.hoje()); req.put("novaDescricao", "Dados contraditórios");
        if (campo.equals("dia")) {
            req.put("removerConfiguracaoDiaReduzido", true); req.put("novoDiaSemanaReduzido", "FRIDAY"); req.put("novasHorasDiaReduzido", 6);
        } else {
            req.put("restaurarPoliticaMatriculaDoTipo", true); req.put("novaMatriculaVinculoObrigatoria", false);
        }
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 400,
                "§5.9 o ato deve determinar um único valor pretendido por campo");
    }

    private NovoContrato legadoSemConfiguracaoFotografada(Cenario c) {
        var contrato = new Contrato(); contrato.setGestaoLocal(c.gestaoA); contrato.setTipoContratacao(c.tipoA);
        contrato.setTipo(c.tipoA.getDescricao()); contrato.setNumeroContrato("LEGADO-" + UUID.randomUUID().toString().substring(0, 8));
        contrato.setAnoContrato(2026); contrato.setDescricao("Contrato importado sem fotografia de configurações");
        contrato.setCnpjEmpresa("11222333000181"); contrato.setRazaoSocialEmpresa("Empresa histórica"); contrato.setNomeFantasiaEmpresa("Histórica");
        contrato.setDataInicio(RelogioSistema.hoje().minusDays(30)); contrato.setDataFim(RelogioSistema.hoje().plusDays(120));
        contrato.setEfetivoPrevisto(5); contrato.setEfetivoOriginalPublicado(5); contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setPublicadoEm(java.time.Instant.now().minusSeconds(3600)); contrato.setReferenciaSeiPublicacao("SEI-LEGADO");
        contrato.setMatriculaVinculoObrigatoria(false); contrato.setDiaSemanaReduzido(DayOfWeek.FRIDAY); contrato.setHorasDiaReduzido(6);
        em.persist(contrato);
        var foto = new VersaoContratoNegocial(); foto.setContrato(contrato); foto.setNumeroVersao(1); foto.setDataEfetiva(contrato.getDataInicio());
        foto.setNumeroContrato(contrato.getNumeroContrato()); foto.setAnoContrato(2026); foto.setDescricao(contrato.getDescricao());
        foto.setDataInicio(contrato.getDataInicio()); foto.setDataFim(contrato.getDataFim()); foto.setEfetivoPrevisto(5);
        // A lacuna que este cenário isola é a política de matrícula. A habilitação
        // possui sua própria prova; não é inferida a partir do flag atual.
        foto.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);
        foto.setGestaoLocalId(c.gestaoA.getId()); foto.setGestaoLocalNome(c.gestaoA.getNome());
        foto.setTipoContratacaoId(c.tipoA.getId()); foto.setTipoContratacaoDescricao(c.tipoA.getDescricao()); em.persist(foto);
        var comp = composicao(contrato, c.lotacaoA, c.jornadaA, c.cargoA, 5); comp.setDataInicioVigencia(contrato.getDataInicio());
        comp.setDataFimVigencia(contrato.getDataFim()); em.flush();
        return new NovoContrato(contrato.getId(), comp.getId(), null, contrato.getDataInicio(), contrato.getDataFim());
    }
}
