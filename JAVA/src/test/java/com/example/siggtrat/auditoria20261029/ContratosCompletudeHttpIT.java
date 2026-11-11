package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Asserções explícitas dos critérios que antes possuíam somente evidência indireta. */
class ContratosCompletudeHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoTemporal;
    @Test
    void prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().minusDays(30); var fimAntigo = RelogioSistema.hoje().minusDays(5);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "FIM-ANTIGO-" + c.contratoA.getId(), 5, inicio, fimAntigo),
                201, "VIN-012 contrato original com vigência já alcançada").path("id").asLong();
        long comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "VIN-012 composição original").path("id").asLong();
        var publicar = mapa("referenciaSei", "SEI-FIM-ANTIGO", "dataPublicacao", RelogioSistema.hoje(), "justificativa", "Original recebido posteriormente");
        var p = enviar(c.localA, base(id) + "/previas/publicar", publicar, 200, "VIN-012 prévia original retroativa");
        publicar.put("tokenPrevia", p.path("tokenPrevia").asText()); publicar.put("confirmarImpacto", true);
        enviar(c.localA, base(id) + "/publicar", publicar, 200, "VIN-012 publicação preserva vigência antiga");
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", id, "composicaoId", comp,
                "dataInicio", inicio.plusDays(1), "pessoa", mapa("nome", "Pessoa da prorrogação tardia", "cpf", "52998224725")),
                201, "VIN-012 vínculo registrado no período válido");
        long vinculoId = vinculo.path("id").asLong();
        var motivo = new MotivoDesligamento(); motivo.setGestaoLocal(c.gestaoA); motivo.setDescricao("Fim natural documentado");
        motivo.setCategoria(CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO); em.persist(motivo);
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(inicio); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita da causa natural desde o início do contrato");
        em.persist(origem); em.flush();
        FonteMotivoSintetica.declarar(em, gestaoTemporal, id, motivo.getId(), c.gestaoA.getId(), c.localA,
                "Fim natural documentado", CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                fimAntigo, fimAntigo, java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                "SEI-SINTETICO-PRORROGACAO-TARDIA-FIM-ANTIGO");
        var fim = enviar(c.localA, "/api/vinculos-contratuais/contratos/" + id + "/tratar-fim-natural", null, 200,
                "VIN-009/012 encerra vínculo no último dia original");
        assertThat(fim.get(0).path("dataFim").asText()).isEqualTo(fimAntigo.toString());
        assertThat(fim.get(0).path("motivoDesligamentoId").asLong()).isEqualTo(motivo.getId());
        em.flush(); em.clear();
        var eventosAntes = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/eventos", "VIN-012 trilha do fim natural");
        var inativar = mapa("dataEfetiva",fimAntigo.minusDays(2),"motivo","Suspensão administrativa independente da vigência");
        var previaInativar = enviar(c.localA,base(id)+"/previas/inativar",inativar,200,"CON012 prévia da inativação explícita");
        inativar.put("tokenPrevia",previaInativar.path("tokenPrevia").asText()); inativar.put("confirmarImpacto",true);
        enviar(c.localA,base(id)+"/inativar",inativar,200,"CON012 contrato inativo antes da prorrogação");
        assertThat(consultar(c.localA,base(id)+"/posicao-temporal?data="+RelogioSistema.hoje(),
                "CON012 controle anterior de habilitação").path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        var extensao = aditamento("PRORROGACAO-TARDIA", fimAntigo.minusDays(1));
        var fimNovo = RelogioSistema.hoje().plusDays(30); extensao.put("novaDataFim", fimNovo);
        var previa = previaAditamento(c, id, extensao);
        assertThat(previa.path("alertas").toString()).contains("retroativo");
        enviar(c.localA, base(id) + "/aditamentos", extensao, 201, "VIN-012 extensão explícita da vigência contratual");
        em.flush(); em.clear();
        var contrato = consultar(c.localA, base(id) + "/posicao-temporal?data=" + RelogioSistema.hoje(), "VIN-012 contrato prorrogado está vigente");
        assertThat(contrato.path("vigenciaFim").asText()).isEqualTo(fimNovo.toString());
        assertThat(contrato.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(contrato.path("habilitacaoAdministrativa").asText()).as("CON012 prorrogação não reativa contrato").isEqualTo("INATIVO");
        var reativar = mapa("dataEfetiva",RelogioSistema.hoje(),"motivo","Retomada administrativa explícita após prorrogação");
        var previaReativar = enviar(c.localA,base(id)+"/previas/reativar",reativar,200,"CON012 prévia autônoma da retomada");
        reativar.put("tokenPrevia",previaReativar.path("tokenPrevia").asText()); reativar.put("confirmarImpacto",true);
        enviar(c.localA,base(id)+"/reativar",reativar,200,"CON012 somente evento explícito reativa");
        em.flush(); em.clear();
        var ativo = consultar(c.localA,base(id)+"/posicao-temporal?data="+RelogioSistema.hoje(),"CON012 posição após retomada");
        assertThat(ativo.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(ativo.path("vigenciaFim").asText()).isEqualTo(fimNovo.toString());
        assertThat(em.find(com.example.siggtrat.entidade.Contrato.class,id).getAtivo()).isTrue();
        assertThat(consultar(c.localA,base(id)+"/posicao-temporal?data="+fimAntigo.minusDays(1),
                "CON012 reativação não apaga o intervalo inativo").path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        var depois = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId, "VIN-012 vínculo encerrado não é reaberto automaticamente");
        assertThat(depois.path("dataFim").asText()).isEqualTo(fimAntigo.toString());
        assertThat(depois.path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
        assertThat(depois.path("motivoDesligamentoId").asLong()).isEqualTo(motivo.getId());
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/eventos", "VIN-012 histórico original preservado"))
                .isEqualTo(eventosAntes);
        assertThat(consultar(c.localA, base(id) + "/composicoes/excedentes?data=" + RelogioSistema.hoje(),
                "VIN-012 ampliação de capacidade não reabre ocupação").get(0).path("quantidadeVinculadaAtual").asInt()).isZero();
    }

    @Test
    void consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 50, false);
        var adi = aditamento("CONTEUDO-COMPLETO", n.inicio().plusDays(1)); adi.put("novaDescricao", "Descrição atualizada por instrumento");
        var instrumento = registrarAditamento(c, n.id(), adi);
        var rem = remanejamento(n, "DISTRIBUICAO-COMPLETA", n.inicio().plusDays(2), 40); previaRemanejamento(c, n.id(), rem);
        var redistribuicao = enviar(c.localA, base(n.id()) + "/remanejamentos", rem, 201, "CON-005 remanejamento próprio");
        var dto = consultar(c.localA, base(n.id()), "CON-005 campos contratuais completos");
        assertThat(dto.path("descricao").asText()).isEqualTo("Descrição atualizada por instrumento");
        assertThat(dto.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        assertThat(dto.path("tipoContratacaoDescricao").asText()).isEqualTo(c.tipoA.getDescricao());
        assertThat(dto.path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(dto.path("razaoSocialEmpresa").asText()).isEqualTo("Empresa Original");
        assertThat(dto.path("nomeFantasiaEmpresa").asText()).isEqualTo("Original");
        assertThat(dto.path("dataInicio").asText()).isEqualTo(n.inicio().toString());
        assertThat(dto.path("dataFim").asText()).isEqualTo(n.fim().toString());
        assertThat(dto.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(dto.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(dto.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(dto.path("efetivoPrevisto").asInt()).isEqualTo(100);
        assertThat(dto.path("efetivoOriginalPublicado").asInt()).isEqualTo(100);
        assertThat(dto.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(dto.path("publicadoPorId").asLong()).isEqualTo(c.localA.getId());
        assertThat(dto.path("publicadoEm").asText()).isNotBlank();
        assertThat(dto.path("referenciaSeiPublicacao").asText()).isEqualTo("SEI-ORIGINAL");
        var composicoes = consultar(c.localA, base(n.id()) + "/composicoes", "CON-005 composição atual completa");
        assertThat(composicoes).hasSize(2);
        assertThat(composicoes.get(0).path("efetivoPrevisto").asInt() + composicoes.get(1).path("efetivoPrevisto").asInt()).isEqualTo(100);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "CON-005 instrumentos associados").get(0).path("id").asLong())
                .isEqualTo(instrumento.path("id").asLong());
        assertThat(consultar(c.localA, base(n.id()) + "/remanejamentos", "CON-005 redistribuições associadas").get(0).path("id").asLong())
                .isEqualTo(redistribuicao.path("id").asLong());
        var pendencias = consultar(c.localA, "/api/pendencias?contratoIds=" + n.id(), "CON-005 consulta das pendências do contrato");
        assertThat(pendencias.path("registros").isArray()).isTrue();
        assertThat(pendencias.path("totalElementos").asLong()).isZero();
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "CON-005 histórico completo dos três fatos");
        assertThat(eventos).hasSize(3);
        assertThat(eventos.toString()).contains("PUBLICACAO", "ADITAMENTO", "REMANEJAMENTO");
        var versoes = consultar(c.localA, base(n.id()) + "/versoes", "CON-005 fotografia original e alterações preservadas");
        assertThat(versoes.size()).isGreaterThanOrEqualTo(2);
        assertThat(versoes.get(0).path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
        var outro=publicado(c,100,50,false);
        var outroAdi=aditamento("CONTEUDO-OUTRO-B10",outro.inicio().plusDays(1));outroAdi.put("novaDescricao","Outro contrato com fatos concorrentes");
        var instrumentoOutro=registrarAditamento(c,outro.id(),outroAdi);
        var outroRem=remanejamento(outro,"DISTRIBUICAO-OUTRA-B10",outro.inicio().plusDays(2),10);previaRemanejamento(c,outro.id(),outroRem);
        var redistribuicaoOutro=enviar(c.localA,base(outro.id())+"/remanejamentos",outroRem,201,"CON005 controle positivo dos fatos do outro contrato");
        long pendenciaN=pendenciaIsolamentoB10(c,n.id(),instrumento.path("eventoId").asLong());
        long pendenciaOutro=pendenciaIsolamentoB10(c,outro.id(),instrumentoOutro.path("eventoId").asLong());
        em.flush();em.clear();
        for(boolean selecionaN:List.of(true,false)) {
            NovoContrato selecionado=selecionaN?n:outro,alheio=selecionaN?outro:n;
            var a=selecionaN?instrumento:instrumentoOutro;var r=selecionaN?redistribuicao:redistribuicaoOutro;
            long pendenciaId=selecionaN?pendenciaN:pendenciaOutro;
            var comps=consultar(c.localA,base(selecionado.id())+"/composicoes","CON005 composições pertencem ao contrato selecionado");
            assertThat(idsB10(comps)).containsExactlyInAnyOrder(selecionado.composicao1(),selecionado.composicao2())
                    .doesNotContain(alheio.composicao1(),alheio.composicao2());
            for(var comp:comps) assertThat(comp.path("contratoId").asLong()).isEqualTo(selecionado.id());
            var adis=consultar(c.localA,base(selecionado.id())+"/aditamentos","CON005 ADI do contrato selecionado");
            assertThat(idsB10(adis)).containsExactly(a.path("id").asLong());
            assertThat(adis.get(0).path("contratoId").asLong()).isEqualTo(selecionado.id());
            var rems=consultar(c.localA,base(selecionado.id())+"/remanejamentos","CON005 REM do contrato selecionado");
            assertThat(idsB10(rems)).containsExactly(r.path("id").asLong());
            assertThat(rems.get(0).path("contratoId").asLong()).isEqualTo(selecionado.id());
            var ps=consultar(c.localA,"/api/pendencias?contratoIds="+selecionado.id(),"CON005 filtro discrimina pendências não vazias").path("registros");
            assertThat(idsB10(ps)).containsExactly(pendenciaId);
            assertThat(ps.get(0).path("contratoId").asLong()).isEqualTo(selecionado.id());
            assertThat(ps.get(0).path("origemId").asLong()).isEqualTo(a.path("eventoId").asLong());
            var es=consultar(c.localA,base(selecionado.id())+"/eventos","CON005 histórico expõe IDs associados ao mesmo contrato");
            var idsPersistidos=em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id",Long.class)
                    .setParameter("c",selecionado.id()).getResultList();
            var idsAlheios=em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id",Long.class)
                    .setParameter("c",alheio.id()).getResultList();
            assertThat(idsPersistidos).hasSize(3);assertThat(idsAlheios).hasSize(3);
            assertThat(idsB10(es)).containsExactlyInAnyOrderElementsOf(idsPersistidos).doesNotContainAnyElementsOf(idsAlheios);
            var vs=consultar(c.localA,base(selecionado.id())+"/versoes","CON005 versões são do mesmo contrato");
            assertThat(idsB10(vs)).containsExactlyInAnyOrderElementsOf(em.createQuery(
                    "select v.id from VersaoContratoNegocial v where v.contrato.id=:c order by v.id",Long.class)
                    .setParameter("c",selecionado.id()).getResultList());
        }
    }

    private java.util.List<Long> idsB10(com.fasterxml.jackson.databind.JsonNode itens) {
        java.util.List<Long> ids=new java.util.ArrayList<>();itens.forEach(i->ids.add(i.path("id").asLong()));return ids;
    }

    private long pendenciaIsolamentoB10(Cenario c,long contratoId,long eventoId) {
        // Pendência sintética exclusiva do filtro; não declara conflito real do instrumento.
        var p=new com.example.siggtrat.entidade.PendenciaAdministrativa();
        p.setChaveOrigem("B10-FILTRO-SINTETICO:"+contratoId);
        p.setTipo(com.example.siggtrat.dominio.pendencia.TipoPendencia.CONFLITO_REPLAY_TEMPORAL);
        p.setEstado(com.example.siggtrat.dominio.pendencia.EstadoPendencia.ABERTA);
        p.setMotivo("Pendência sintética explícita para isolamento de consultas por contrato");p.setDataEfetiva(RelogioSistema.hoje());
        p.setOperacaoOrigem("ADITAMENTO");p.setOrigemEntidade("EventoCicloVidaContrato");p.setOrigemId(eventoId);
        p.setPerfilResponsavel(com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_LOCAL);
        p.setEscopoBloqueio(com.example.siggtrat.dominio.pendencia.EscopoBloqueioPendencia.CONTRATO_TEMPORAL);
        p.setContratoId(contratoId);p.setGestaoLocalId(c.gestaoA.getId());p.setSuperintendenciaId(c.seA.getId());
        em.persist(p);em.flush();return p.getId();
    }

    @Test
    void cadaRotaDocumentalPreservaPerfilEContextoDoDocumento() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        long id = consultar(c.localA, base(n.id()) + "/documentos", "DOC-010 documento publicado de referência").get(0).path("id").asLong();
        String lista = base(n.id()) + "/documentos", doc = lista + "/" + id;
        for (String path : List.of(lista, doc, doc + "/eventos", doc + "/arquivo", doc + "/evidencias-integridade")) {
            executar(c.operacionalA, get(path), 403, "DOC-010 perfil Operacional recusado: " + path);
            executar(c.localB, get(path), 404, "DOC-010 documento fora da Gestão: " + path);
            executar(null, get(path), 401, "DOC-010 consulta autenticada: " + path);
        }
        for (String suffix : List.of("/permissoes", "/acessos")) {
            executar(c.nacional, get(doc + suffix), 403, "DOC-010 auditoria exige responsável ou administrador: " + suffix);
            executar(c.localB, get(doc + suffix), 403, "DOC-010 responsável de outra Gestão não consulta auditoria: " + suffix);
            executar(null, get(doc + suffix), 401, "DOC-010 auditoria exige autenticação: " + suffix);
        }
        var corpo = mapa("novaReferenciaSei", "SEI-RETIFICADO", "motivo", "Correção documental", "referenciaSei", "SEI-ESCOPO",
                "tipoFato", "CONTRATO_ORIGINAL", "fatoChave", "ORIGINAL", "titulo", "Escopo documental", "classificacaoAcesso", "NEGOCIAL");
        for (String suffix : List.of("/retificar-sei", "/invalidar")) {
            for (var usuario : List.of(c.nacional, c.operacionalA, c.admin))
                executar(usuario, post(doc + suffix).contentType(MediaType.APPLICATION_JSON).content(json(corpo)), 403,
                        "DOC-010 versão documental exige Gestor: " + suffix);
            executar(c.localB, post(doc + suffix).contentType(MediaType.APPLICATION_JSON).content(json(corpo)), 404,
                    "DOC-010 escrita documental exige Gestão: " + suffix);
        }
        for (String path : List.of(lista, doc + "/substituir")) {
            executar(c.nacional, multipart(path).file(metadados(corpo)), 403, "DOC-010 multipart exige Gestor: " + path);
            executar(c.localB, multipart(path).file(metadados(corpo)), 404, "DOC-010 multipart mantém escopo: " + path);
        }
        executar(c.nacional, delete(doc), 403, "DOC-010 descarte exige responsável");
        executar(c.localB, delete(doc), 404, "DOC-010 descarte fora da Gestão");
        for (String suffix : List.of("/permissoes", "/permissoes/1/revogar"))
            for (var usuario : List.of(c.localA, c.localB, c.nacional, c.operacionalA))
                executar(usuario, post(doc + suffix).contentType(MediaType.APPLICATION_JSON).content(json(corpo)), 403,
                        "DOC-010 concessão/revogação exige administrador técnico: " + suffix);
        executar(c.localA, get(base(c.contratoB.getId()) + "/documentos/" + id + "/eventos"), 404,
                "DOC-010 identificador do documento não pode ser combinado com outro contrato");
    }
}
