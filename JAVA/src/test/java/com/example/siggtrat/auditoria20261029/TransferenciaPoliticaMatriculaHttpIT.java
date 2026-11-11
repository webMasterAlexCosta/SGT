package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;

/** RI06/CON023/MAT005: a política resultante deve ser compatível com todos vínculos atingidos. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferenciaPoliticaMatriculaHttpIT extends ContratosHttpBase {
    private record Fixture(Cenario c, NovoContrato n, long vinculo, LocalDate transferencia) {}
    private record Estado(Long gestao, Long tipo, String numero, Integer ano, List<Long> eventos, List<Long> versoes,
                          List<Long> transferencias, List<Long> usos, List<String> alocacoes, String vinculo) {}

    @ParameterizedTest
    @ValueSource(strings = {"VIGENTE", "PROGRAMADO"})
    void transferenciaParaTipoObrigatorioRecusaVinculoSemMatriculaSemMudarFatos(String situacao) throws Exception {
        var f = preparar(situacao, false, null); var antes = estado(f);
        var negativa = executar(f.c().localA, post(base(f.n().id()) + "/transferir-gestao").contentType(MediaType.APPLICATION_JSON)
                        .content(json(requisicao(f))), 409,
                "RI06 Tipo destino obrigatório não pode tornar inválido vínculo " + situacao + " sem matrícula");
        assertThat(mapper.readTree(negativa.getResponse().getContentAsString()).path("message").asText())
                .containsIgnoringCase("matrícula");
        assertThat(estado(f)).as("Nova transação comprova que a recusa não consumiu concessão nem mudou fatos ou faixas").isEqualTo(antes);
        assertThat(antes.transferencias()).isEmpty(); assertThat(antes.usos()).isEmpty();
    }

    @Test
    void overrideContratualOpcionalPermiteTransferirParaTipoObrigatorio() throws Exception {
        var f = preparar("PROGRAMADO", true, null);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f), 200,
                "RI06 override contratual false explícito mantém matrícula opcional no destino");
        var foto = consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + f.transferencia(),
                "RI06 política resultante respeita o override conhecido").path("versaoContratual");
        assertThat(foto.path("matriculaVinculoObrigatoria").isBoolean()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoria").asBoolean()).isFalse();
        assertThat(foto.path("matriculaTipoContratacaoObrigatoria").asBoolean()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        var depois = estado(f); assertThat(depois.transferencias()).hasSize(1); assertThat(depois.usos()).hasSize(1);
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculo()).getMatricula())).isNull();
    }

    @Test
    void matriculaInformadaPermiteTransferenciaComHerancaObrigatoria() throws Exception {
        String matricula = "MAT-TRANSFERENCIA-" + UUID.randomUUID().toString().substring(0, 8);
        var f = preparar("PROGRAMADO", false, matricula);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f), 200,
                "RI06 vínculo já tratado com matrícula pode seguir para Tipo obrigatório");
        var foto = consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + f.transferencia(),
                "RI06 herança do destino é obrigatória e comprovada").path("versaoContratual");
        assertThat(foto.path("matriculaVinculoObrigatoria").isNull()).isTrue();
        assertThat(foto.path("matriculaTipoContratacaoObrigatoria").asBoolean()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculo()).getMatricula()))
                .isEqualTo(matricula.toUpperCase(Locale.ROOT));
        assertThat(estado(f).transferencias()).hasSize(1);
    }

    @Test
    void regularizarMatriculaAposRecusaPermiteMesmaTransferenciaSemConsumirConcessaoDuasVezes() throws Exception {
        var f = preparar("VIGENTE", false, null); var antes = estado(f);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f), 409,
                "RI06 primeira tentativa exige tratar a matrícula");
        assertThat(estado(f)).isEqualTo(antes);
        String matricula = "MAT-REGULARIZADA-" + f.vinculo();
        enviar(f.c().localA, "/api/vinculos-contratuais/" + f.vinculo() + "/alterar-matricula",
                mapa("dataEfetiva", RelogioSistema.hoje(), "novaMatricula", matricula,
                        "justificativa", "Regularização documental anterior à transferência",
                        "evidencia", "SEI-MATRICULA-REGULARIZADA", "autorizacao", "ATO-MATRICULA"), 200,
                "RI06 ato explícito regulariza a matrícula sem substituir vínculo ou pessoa");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f), 200,
                "RI06 mesma intenção e mesma concessão passam após tratamento da matrícula");
        var depois = estado(f);
        assertThat(depois.transferencias()).hasSize(1); assertThat(depois.usos()).hasSize(1);
        assertThat(depois.eventos()).containsAll(antes.eventos()).hasSize(antes.eventos().size() + 1);
        assertThat(depois.versoes()).containsAll(antes.versoes()).hasSize(antes.versoes().size() + 1);
        assertThat(depois.numero()).isEqualTo(antes.numero()); assertThat(depois.ano()).isEqualTo(antes.ano());
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculo()).getMatricula())).isEqualTo(matricula);
    }

    @Test
    void aditamentoPosteriorDispensandoMatriculaAntesDoInicioProgramadoEhRespeitadoNaTransferencia() throws Exception {
        var f = preparar("PROGRAMADO", false, null);
        var dispensa = aditamento("RI06-DISPENSA-POSTERIOR", f.transferencia().plusDays(2));
        dispensa.put("novaMatriculaVinculoObrigatoria", false);
        registrarPolitica(f, dispensa);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f), 200,
                "RI06 a obrigação anterior ao início não ignora dispensa válida posterior já registrada");
        var durante = consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + f.transferencia(),
                "RI06 Tipo exige matrícula no intervalo anterior à dispensa").path("versaoContratual");
        var inicio = consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + RelogioSistema.hoje().plusDays(5),
                "RI06 vínculo inicia sob override documentado posterior").path("versaoContratual");
        assertThat(durante.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
        assertThat(inicio.path("matriculaVinculoObrigatoria").isBoolean()).isTrue();
        assertThat(inicio.path("matriculaVinculoObrigatoria").asBoolean()).isFalse();
        assertThat(inicio.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculo()).getMatricula())).isNull();
    }

    @Test
    void restauracaoPosteriorDaHerancaObrigatoriaImpedeTransferenciaMesmoComOverrideOpcionalInicial() throws Exception {
        var f = preparar("PROGRAMADO", true, null);
        var restauracao = aditamento("RI06-RESTAURACAO-POSTERIOR", f.transferencia().plusDays(6));
        restauracao.put("restaurarPoliticaMatriculaDoTipo", true);
        registrarPolitica(f, restauracao);
        var antes = estado(f);
        var negativa = executar(f.c().localA, post(base(f.n().id()) + "/transferir-gestao").contentType(MediaType.APPLICATION_JSON)
                        .content(json(requisicao(f))), 409,
                "RI06 verifica também a restauração da herança depois do início do vínculo");
        assertThat(mapper.readTree(negativa.getResponse().getContentAsString()).path("message").asText())
                .containsIgnoringCase("matrícula");
        assertThat(estado(f)).as("ADI posterior conhecido permanece intacto depois do rollback da transferência").isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aditamentoPosteriorDeTipoDaOrigemExigeTratamentoAntesDaTransferencia(boolean alteraTipo) throws Exception {
        var f = preparar("VIGENTE", false, "MATRICULA-TIPO-POSTERIOR");
        long tipoPosterior = emTransacao(() -> {
            var tipo = new TipoContratacao(); tipo.setGestaoLocal(em.find(GestaoLocal.class, f.c().gestaoA.getId()));
            tipo.setDescricao("Tipo posterior da origem " + UUID.randomUUID()); tipo.setAtivo(true);
            em.persist(tipo); em.flush(); return tipo.getId();
        });
        LocalDate efeitoAdi = f.transferencia().plusDays(6);
        var req = aditamento("TIPO-POSTERIOR-" + f.n().id(), efeitoAdi);
        req.put("novaDescricao", "Descrição documental posterior preservada");
        if (alteraTipo) req.put("novoTipoContratacaoId", tipoPosterior);
        long adi = emTransacao(() -> {
            try { return registrarAditamento(f.c(), f.n().id(), req).path("id").asLong(); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        var antes = estado(f);
        if (alteraTipo) {
            var recusada = executar(f.c().localA, post(base(f.n().id()) + "/transferir-gestao")
                            .contentType(MediaType.APPLICATION_JSON).content(json(requisicao(f))), 409,
                    "CON023 CAT001 transferência não pode tornar válido Tipo de outra Gestão em ato posterior");
            assertThat(mapper.readTree(recusada.getResponse().getContentAsString()).path("message").asText())
                    .containsIgnoringCase("Tipo");
            assertThat(estado(f)).as("Recusa preserva ADI, versões, vínculos, mapas e concessão em nova transação").isEqualTo(antes);
            assertThat(emTransacao(() -> em.find(AditamentoContratual.class, adi).getNovoTipoContratacao().getId()))
                    .isEqualTo(tipoPosterior);
            enviar(f.c().localA, base(f.n().id()) + "/aditamentos/" + adi + "/cancelar",
                    mapa("motivo", "Revogar a mudança de Tipo incompatível antes da transferência", "referenciaSei", "SEI-TIPO-CANCELADO"),
                    200, "CON023 tratamento explícito do ato posterior preserva sua história");
        }
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f), 200,
                "CON023 transferência permite ato posterior compatível ou já tratado");
        var foto = consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + efeitoAdi,
                "CAT001 Gestão e Tipo permanecem coerentes depois da fronteira posterior").path("versaoContratual");
        assertThat(foto.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        assertThat(foto.path("tipoContratacaoId").asLong()).isEqualTo(f.c().tipoB.getId());
        if (!alteraTipo) assertThat(foto.path("descricao").asText()).isEqualTo("Descrição documental posterior preservada");
        assertThat(estado(f).transferencias()).hasSize(1); assertThat(estado(f).usos()).hasSize(1);
    }

    private void registrarPolitica(Fixture f, Map<String, Object> req) {
        emTransacao(() -> {
            try { registrarAditamento(f.c(), f.n().id(), req); return null; }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private Fixture preparar(String situacao, boolean overrideOpcional, String matricula) throws Exception {
        Cenario c = criarCenarioPersistido(); LocalDate hoje = RelogioSistema.hoje();
        emTransacao(() -> { em.find(TipoContratacao.class, c.tipoB.getId()).setMatriculaVinculoObrigatoria(true); return null; });
        NovoContrato n = emTransacao(() -> {
            try {
                var inicio = hoje.minusDays(10); var fim = hoje.plusDays(40);
                var body = rascunho(c, "RI06-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim);
                if (overrideOpcional) body.put("matriculaVinculoObrigatoria", false);
                long id = enviar(c.localA, "/api/contratos", body, 201, "RI06 original opcional conhecido").path("id").asLong();
                long comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                        "RI06 composição original explícita").path("id").asLong();
                var pub = mapa("dataPublicacao", hoje, "referenciaSei", "SEI-RI06-ORIGINAL", "justificativa", "Original documentado");
                var previa = enviar(c.localA, base(id) + "/previas/publicar", pub, 200, "RI06 original retroativo confirmado");
                pub.put("tokenPrevia", previa.path("tokenPrevia").asText()); pub.put("confirmarImpacto", true);
                enviar(c.localA, base(id) + "/publicar", pub, 200, "RI06 fotografia de política original");
                return new NovoContrato(id, comp, null, inicio, fim);
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        var req = mapa("pessoa", mapa("nome", "Pessoa da transferência " + c.gestaoA.getId(),
                        "cpf", cpf(850000000 + c.pessoaA.getId().intValue())),
                "contratoId", n.id(), "composicaoId", n.composicao1(),
                "dataInicio", situacao.equals("VIGENTE") ? hoje.minusDays(1) : hoje.plusDays(5));
        if (matricula != null) req.put("matricula", matricula);
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", req, 201,
                "RI06 vínculo inicialmente válido sob política opcional").path("id").asLong();
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        return new Fixture(c, n, vinculo, hoje.plusDays(1));
    }

    private Map<String, Object> requisicao(Fixture f) {
        return mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", f.transferencia(), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", f.c().gestaoB.getId(), "tipoContratacaoDestinoId", f.c().tipoB.getId(),
                "motivo", "Transferência documentada para Tipo com matrícula obrigatória", "referenciaSei", "SEI-RI06",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa("composicaoOrigemId", f.n().composicao1(),
                        "lotacaoDestinoId", f.c().lotacaoB.getId(), "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId())));
    }

    private Estado estado(Fixture f) {
        return emTransacao(() -> {
            var c = em.find(Contrato.class, f.n().id()); var v = em.find(VinculoContratual.class, f.vinculo());
            List<String> faixas = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id order by a.id", AlocacaoVinculo.class)
                    .setParameter("id", f.vinculo()).getResultList().stream().map(a -> a.getId() + ":" + a.getDataInicio() + ":" + a.getDataFim()
                            + ":" + a.getLotacao().getId() + ":" + a.getJornada().getId() + ":" + a.getCargo().getId()).toList();
            return new Estado(c.getGestaoLocal().getId(), c.getTipoContratacao().getId(), c.getNumeroContrato(), c.getAnoContrato(),
                    ids("EventoCicloVidaContrato", "contrato.id", f.n().id()), ids("VersaoContratoNegocial", "contrato.id", f.n().id()),
                    ids("TransferenciaGestaoContrato", "contrato.id", f.n().id()),
                    em.createQuery("select u.id from UsoPermissaoExcepcional u where u.recursoId=:id and u.tipoRecurso='Contrato' order by u.id", Long.class)
                            .setParameter("id", f.n().id()).getResultList(), faixas,
                    v.getId() + ":" + v.getPessoa().getId() + ":" + v.getDataInicio() + ":" + v.getDataFim() + ":" + v.getEstadoNegocial() + ":" + v.getMatricula());
        });
    }
    private List<Long> ids(String entidade, String propriedade, long id) {
        return em.createQuery("select e.id from " + entidade + " e where e." + propriedade + "=:id order by e.id", Long.class)
                .setParameter("id", id).getResultList();
    }
}
