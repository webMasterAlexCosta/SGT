package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** MAT005/TMP008/012: a matrícula deve ser compatível com toda a faixa que o ato torna válida. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasPoliticaMatriculaFuturaHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void cadastroSemMatriculaRecusaObrigacaoJaProgramadaDentroDoPeriodo() throws Exception {
        Fixture f = preparar(false, true);
        List<String> antes = estado(f);
        String cpf = cpf(Math.toIntExact(745000000L + f.n().id()));
        negarPorMatricula(f, V, cadastro(f, cpf, RelogioSistema.hoje(), f.fronteira().plusDays(5), null),
                "MAT005 cadastro não pode atravessar obrigação futura já documentada sem matrícula");
        assertThat(estado(f)).as("Recusa não grava vínculo, faixa, reserva, evento ou versão adicional").isEqualTo(antes);
        assertThat(emTransacao(() -> em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                .setParameter("cpf", cpf).getSingleResult())).isZero();
        assertThat(emTransacao(() -> em.createQuery("select count(e) from Empregado e where e.cpf=:cpf", Long.class)
                .setParameter("cpf", cpf).getSingleResult())).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FIM_ANTERIOR", "MATRICULA_INFORMADA"})
    void cadastroRespeitaFimAnteriorOuMatriculaInformadaSemBloqueioGenerico(String controle) throws Exception {
        Fixture f = preparar(false, true);
        LocalDate fim = controle.equals("FIM_ANTERIOR") ? f.fronteira().minusDays(1) : f.fronteira().plusDays(5);
        String matricula = controle.equals("MATRICULA_INFORMADA") ? "MAT-FRONTEIRA-" + f.n().id() : null;
        JsonNode criado = enviar(f.c().localA, V, cadastro(f, cpf(Math.toIntExact(746000000L + f.n().id())),
                        RelogioSistema.hoje(), fim, matricula), 201,
                "MAT005 controle legítimo: " + controle);
        long id = criado.path("id").asLong();
        emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, id);
            assertThat(v.getDataFim()).isEqualTo(fim); assertThat(v.getMatricula()).isEqualTo(matricula);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"RETIFICAR", "CANCELAR"})
    void ampliarOuCancelarTerminoRevalidaObrigacaoProgramadaNaFaixaRestaurada(String comando) throws Exception {
        Fixture f = preparar(false, true);
        LocalDate fimAnterior = f.fronteira().minusDays(1);
        long id = enviar(f.c().localA, V, cadastro(f, cpf(Math.toIntExact(747000000L + f.n().id())),
                        RelogioSistema.hoje(), fimAnterior, null), 201,
                "Preparação: vínculo opcional termina antes da obrigação futura").path("id").asLong();
        List<String> antes = estado(f);
        Map<String, Object> req = mapa("justificativa", "Continuidade documentada sujeita à política válida",
                "evidencia", "SEI-MATRICULA-FRONTEIRA", "autorizacao", "ATO-MATRICULA-FRONTEIRA",
                "confirmarImpacto", true);
        String rota;
        if (comando.equals("RETIFICAR")) {
            req.put("dataFim", f.fronteira().plusDays(5)); req.put("motivoDesligamentoId", f.motivoId());
            JsonNode previa = enviar(f.c().localA, V + "/" + id + "/previas/retificacao-desligamento", req, 200,
                    "Prévia da ampliação não grava a nova faixa");
            req.put("tokenPrevia", previa.path("token").asText());
            rota = V + "/" + id + "/retificar-desligamento";
        } else rota = V + "/" + id + "/cancelar-desligamento-programado";
        negarPorMatricula(f, rota, req,
                "MAT005 " + comando + " não pode restaurar faixa com matrícula obrigatória ausente");
        assertThat(estado(f)).as("Rollback preserva fim anterior, faixas, decisões e fotografia da política").isEqualTo(antes);
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, id).getDataFim())).isEqualTo(fimAnterior);
    }

    @Test
    void cancelarDispensaProgramadaRecusaVinculoFuturoQuePerderiaPoliticaOpcional() throws Exception {
        Fixture f = preparar(true, false);
        long id = enviar(f.c().localA, V, cadastro(f, cpf(Math.toIntExact(748000000L + f.n().id())),
                        f.fronteira().plusDays(1), null, null), 201,
                "Preparação: vínculo programado nasce após dispensa futura válida").path("id").asLong();
        List<String> antes = estado(f);
        negarPorMatricula(f, base(f.n().id()) + "/aditamentos/" + f.aditamentoId() + "/cancelar",
                mapa("dataAto", RelogioSistema.hoje(), "motivo", "Cancelamento da dispensa precisa tratar dependentes",
                        "referenciaSei", "SEI-CANCELAMENTO-DISPENSA"),
                "MAT005 ADI006 cancelamento não pode retirar a política que sustenta o vínculo programado");
        assertThat(estado(f)).as("Aditamento, evento vigente, versões e vínculo mantidos depois da recusa").isEqualTo(antes);
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, id).getMatricula())).isNull();
        JsonNode posicao = consultar(f.c().localA, base(f.n().id()) + "/posicao-temporal?data=" + f.fronteira().plusDays(1),
                "A dispensa permanece efetiva no período do vínculo após rollback").path("versaoContratual");
        assertThat(posicao.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
        assertThat(posicao.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SEM_MATRICULA", "MATRICULA_INFORMADA"})
    void anteciparInicioRevalidaObrigacaoIntermediariaMesmoComPoliticaOpcionalNasDuasPontas(String controle) throws Exception {
        Fixture f = preparar(false, true);
        LocalDate dispensa = f.fronteira().plusDays(5);
        emTransacao(() -> {
            try {
                Map<String, Object> req = aditamento("ADI-DISPENSA-INTERMEDIARIA-" + f.n().id(), dispensa);
                req.put("novaMatriculaVinculoObrigatoria", false);
                registrarAditamento(f.c(), f.n().id(), req);
                return null;
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate inicioOriginal = dispensa.plusDays(2), inicioAntecipado = f.fronteira().minusDays(3);
        String matricula = controle.equals("MATRICULA_INFORMADA") ? "MAT-INTERVALO-" + f.n().id() : null;
        JsonNode criado = enviar(f.c().localA, V, cadastro(f, cpf(Math.toIntExact(749000000L + f.n().id())),
                        inicioOriginal, null, matricula), 201,
                "Preparação: vínculo inicia após a dispensa, fora do intervalo de obrigação intermediária");
        long id = criado.path("id").asLong();
        long alocacaoId = emTransacao(() -> em.createQuery("select a.id from AlocacaoVinculo a where a.vinculo.id=:v", Long.class)
                .setParameter("v", id).getSingleResult());
        List<String> antes = estado(f);
        JsonNode eventoOriginal = consultar(f.c().localA, V + "/" + id + "/eventos",
                "Fotografia do evento original antes da antecipação").get(0);
        Map<String, Object> req = mapa("novoInicio", inicioAntecipado, "justificativa", "Antecipação documental do vínculo programado",
                "evidencia", "SEI-INICIO-POLITICA-INTERMEDIARIA", "autorizacao", "ATO-INICIO-POLITICA-INTERMEDIARIA");
        JsonNode previa = enviar(f.c().localA, V + "/" + id + "/previas/retificacao-inicio", req, 200,
                "VIN014 prévia da extensão inicial atravessa intervalo obrigatório já registrado");
        assertThat(estado(f)).as("Prévia não grava limites, reservas, eventos ou versões").isEqualTo(antes);
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        String rota = V + "/" + id + "/retificar-inicio";
        if (matricula == null) {
            negarPorMatricula(f, rota, req,
                    "MAT005 VIN014 início opcional não dispensa a matrícula exigida na fronteira intermediária");
            assertThat(estado(f)).as("Recusa preserva toda a linha após rollback em nova transação").isEqualTo(antes);
            emTransacao(() -> {
                assertThat(em.find(VinculoContratual.class, id).getDataInicio()).isEqualTo(inicioOriginal);
                assertThat(em.find(AlocacaoVinculo.class, alocacaoId).getDataInicio()).isEqualTo(inicioOriginal);
                return null;
            });
        } else {
            JsonNode retificado = enviar(f.c().localA, rota, req, 200,
                    "MAT005 matrícula válida permite antecipar sobre a obrigação intermediária");
            assertThat(retificado.path("id").asLong()).isEqualTo(id);
            assertThat(retificado.path("pessoaId").asLong()).isEqualTo(criado.path("pessoaId").asLong());
            emTransacao(() -> {
                VinculoContratual v = em.find(VinculoContratual.class, id);
                assertThat(v.getDataInicio()).isEqualTo(inicioAntecipado); assertThat(v.getMatricula()).isEqualTo(matricula);
                assertThat(em.find(AlocacaoVinculo.class, alocacaoId).getDataInicio()).isEqualTo(inicioAntecipado);
                assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:v", Long.class)
                        .setParameter("v", id).getSingleResult()).isEqualTo(1);
                return null;
            });
            JsonNode eventos = consultar(f.c().localA, V + "/" + id + "/eventos",
                    "Antecipação válida preserva criação e acrescenta retificação encadeada");
            assertThat(eventos).hasSize(2);
            assertThat(eventos.get(0)).isEqualTo(eventoOriginal);
            assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("RETIFICACAO_INICIO");
            assertThat(eventos.get(1).path("eventoPrecedenteId").asLong()).isEqualTo(eventoOriginal.path("id").asLong());
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "VIGENTE,SEM_MATRICULA", "PROGRAMADO,SEM_MATRICULA", "FIM_ANTERIOR,SEM_MATRICULA",
            "VIGENTE,MATRICULA_INFORMADA", "PROGRAMADO,MATRICULA_INFORMADA"})
    void novaObrigacaoFuturaRevalidaVinculoExistenteOuProgramadoNaFronteira(String situacao,String controle) throws Exception {
        Fixture f=preparar(false,false);
        LocalDate fronteira=f.fronteira().plusDays(1);
        LocalDate inicio=situacao.equals("PROGRAMADO")?fronteira:RelogioSistema.hoje();
        LocalDate fim=situacao.equals("FIM_ANTERIOR")?fronteira.minusDays(1):null;
        String matricula=controle.equals("MATRICULA_INFORMADA")?"MAT-B07-FUTURA-"+f.n().id():null;
        JsonNode criado=enviar(f.c().localA,V,cadastro(f,cpf(Math.toIntExact(754000000L+f.n().id())),inicio,fim,matricula),201,
                "MAT005 vínculo "+situacao+" existe antes do ato futuro de exigência");
        long id=criado.path("id").asLong(), pessoa=criado.path("pessoaId").asLong();
        List<String> antes=estado(f);
        Map<String,Object> adi=aditamento("ADI-OBRIGACAO-SOBRE-EXISTENTE-"+f.n().id(),fronteira);
        adi.put("novaMatriculaVinculoObrigatoria",true);
        JsonNode previa=emTransacao(() -> {
            try { return previaAditamento(f.c(),f.n().id(),adi); }
            catch(Exception e) { throw new IllegalStateException(e); }
        });
        assertThat(previa.path("tokenPrevia").asText()).hasSize(64);
        assertThat(estado(f)).as("Prévia não altera pessoas, vínculos, faixas, política ou eventos").isEqualTo(antes);
        if(matricula==null && fim==null) {
            negarPorMatricula(f,base(f.n().id())+"/aditamentos",adi,
                    "MAT005 nova exigência futura não pode deixar vínculo "+situacao+" sem matrícula em D");
            assertThat(estado(f)).as("Recusa do ato conserva estado em transação independente").isEqualTo(antes);
            JsonNode politica=consultar(f.c().localA,base(f.n().id())+"/posicao-temporal?data="+fronteira,
                    "MAT005 rollback mantém dispensa anterior em D").path("versaoContratual");
            assertThat(politica.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
            assertThat(politica.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        } else {
            enviar(f.c().localA,base(f.n().id())+"/aditamentos",adi,201,
                    "MAT005 controle positivo: matrícula informada ou fim inclusivo em D-1");
            JsonNode anterior=consultar(f.c().localA,base(f.n().id())+"/posicao-temporal?data="+fronteira.minusDays(1),
                    "MAT005 nova obrigação não retroage para D-1").path("versaoContratual");
            JsonNode naFronteira=consultar(f.c().localA,base(f.n().id())+"/posicao-temporal?data="+fronteira,
                    "MAT005 obrigação passa a valer inclusive em D").path("versaoContratual");
            assertThat(anterior.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
            assertThat(anterior.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
            assertThat(naFronteira.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
            assertThat(naFronteira.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
        }
        emTransacao(() -> {
            VinculoContratual v=em.find(VinculoContratual.class,id);
            assertThat(v.getPessoa().getId()).isEqualTo(pessoa);assertThat(v.getContrato().getId()).isEqualTo(f.n().id());
            assertThat(v.getDataInicio()).isEqualTo(inicio);assertThat(v.getDataFim()).isEqualTo(fim);
            assertThat(v.getMatricula()).isEqualTo(matricula);
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:p",Long.class)
                    .setParameter("p",pessoa).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    private Fixture preparar(boolean originalObrigatoria, boolean futuraObrigatoria) {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario(); c.tipoA.setMatriculaVinculoObrigatoria(originalObrigatoria); em.flush();
                NovoContrato n = publicado(c, 5, null, false);
                LocalDate fronteira = RelogioSistema.hoje().plusDays(5);
                Map<String, Object> req = aditamento("ADI-MATRICULA-FUTURA-" + n.id(), fronteira);
                req.put("novaMatriculaVinculoObrigatoria", futuraObrigatoria);
                JsonNode adi = registrarAditamento(c, n.id(), req);
                MotivoDesligamento motivo = new MotivoDesligamento(); motivo.setGestaoLocal(c.gestaoA);
                motivo.setDescricao("Término documentado matrícula futura " + n.id());
                motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(motivo); em.flush();
                return new Fixture(c, n, fronteira, adi.path("id").asLong(), motivo.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private Map<String, Object> cadastro(Fixture f, String cpf, LocalDate inicio, LocalDate fim, String matricula) {
        return mapa("pessoa", mapa("nome", "Pessoa política temporal " + cpf, "cpf", cpf),
                "contratoId", f.n().id(), "composicaoId", f.n().composicao1(),
                "dataInicio", inicio, "dataFim", fim, "matricula", matricula);
    }

    private void negarPorMatricula(Fixture f, String rota, Map<String, Object> req, String regra) throws Exception {
        var r = executar(f.c().localA, post(rota).contentType(MediaType.APPLICATION_JSON).content(json(req)), 409, regra);
        assertThat(mapper.readTree(r.getResponse().getContentAsString()).path("message").asText())
                .containsIgnoringCase("matrícula");
    }

    private List<String> estado(Fixture f) {
        return emTransacao(() -> {
            List<String> r = new ArrayList<>();
            for (String jpql : List.of(
                    "select v.id,v.pessoa.id,v.dataInicio,v.dataFim,v.estadoNegocial,v.matricula,v.version from VinculoContratual v where v.contrato.id=:c order by v.id",
                    "select a.id,a.vinculo.id,a.dataInicio,a.dataFim,a.estadoNegocial,a.version from AlocacaoVinculo a where a.vinculo.contrato.id=:c order by a.id",
                    "select e.id,e.hashEvento from EventoVinculoContratual e where e.vinculo.contrato.id=:c order by e.id",
                    "select e.id,e.hashEvento from EventoAlocacaoVinculo e where e.vinculo.contrato.id=:c order by e.id",
                    "select e.id,e.situacaoFato,e.dataEfetiva from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id",
                    "select v.id,v.numeroVersao,v.matriculaVinculoObrigatoriaEfetiva from VersaoContratoNegocial v where v.contrato.id=:c order by v.id",
                    "select a.id,a.evento.id,a.novaMatriculaVinculoObrigatoria from AditamentoContratual a where a.contrato.id=:c order by a.id",
                    "select r.id,r.matricula from ReservaMatriculaContrato r where r.contrato.id=:c order by r.id"))
                em.createQuery(jpql, Object[].class).setParameter("c", f.n().id()).getResultList().stream()
                        .map(Arrays::toString).forEach(r::add);
            return r;
        });
    }

    private record Fixture(Cenario c, NovoContrato n, LocalDate fronteira, long aditamentoId, long motivoId) {}
}
