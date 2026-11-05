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
        var extensao = aditamento("PRORROGACAO-TARDIA", fimAntigo.minusDays(1));
        var fimNovo = RelogioSistema.hoje().plusDays(30); extensao.put("novaDataFim", fimNovo);
        var previa = previaAditamento(c, id, extensao);
        assertThat(previa.path("alertas").toString()).contains("retroativo");
        enviar(c.localA, base(id) + "/aditamentos", extensao, 201, "VIN-012 extensão explícita da vigência contratual");
        em.flush(); em.clear();
        var contrato = consultar(c.localA, base(id) + "/posicao-temporal?data=" + RelogioSistema.hoje(), "VIN-012 contrato prorrogado está vigente");
        assertThat(contrato.path("vigenciaFim").asText()).isEqualTo(fimNovo.toString());
        assertThat(contrato.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
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
