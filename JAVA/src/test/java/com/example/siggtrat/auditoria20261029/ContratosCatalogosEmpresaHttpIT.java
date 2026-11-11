package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.servico.AditamentoContratualServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/** CT07/CT08: coerência de catálogo histórico e da empresa escolhida no instrumento. */
class ContratosCatalogosEmpresaHttpIT extends ContratosHttpBase {
    @Autowired private AditamentoContratualServico aditamentos;

    @Test
    void contratoRetroativoUsaTipoValidoNaDataMesmoInativoHoje() throws Exception {
        var c = criarCenario(); historico(c, TipoCatalogoTemporal.TIPO_CONTRATACAO, c.tipoA.getId()); c.tipoA.setAtivo(false); em.flush();
        var r = enviar(c.localA, "/api/contratos", rascunho(c, "TIPO-HISTORICO", 5, DATA, DATA.plusMonths(3)),
                201, "CAT-007/CON-014 cadastro retroativo usa disponibilidade histórica do tipo");
        assertThat(r.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        enviar(c.localA, "/api/contratos", rascunho(c, "TIPO-FUTURO", 5, RelogioSistema.hoje().plusDays(1), RelogioSistema.hoje().plusDays(30)),
                400, "CAT-003 tipo inativo não pode ser usado em novo recorte futuro");
    }

    @Test
    void composicaoOriginalRetroativaUsaCatalogosValidosNoPeriodo() throws Exception {
        var c = criarCenario();
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "COMPOSICAO-HISTORICA", 5, DATA, DATA.plusMonths(3)), 201,
                "CON-014 original retroativo de referência").path("id").asLong();
        historico(c, TipoCatalogoTemporal.LOTACAO, c.lotacaoA.getId()); historico(c, TipoCatalogoTemporal.JORNADA, c.jornadaA.getId());
        historico(c, TipoCatalogoTemporal.CARGO, c.cargoA.getId());
        c.lotacaoA.setAtivo(false); c.jornadaA.setAtivo(false); c.cargoA.setAtivo(false); em.flush();
        var comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "CAT-007 composição original respeita Lotação/Turno/Cargo historicamente válidos");
        assertThat(comp.path("efetivoPrevisto").asInt()).isEqualTo(5);
    }

    @Test
    void publicacaoRetroativaValidaTipoNoInicioRealDoContrato() throws Exception {
        var c = criarCenario();
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "PUBLICAR-HISTORICO", 5, DATA, DATA.plusMonths(3)), 201,
                "CON-014 original preparado antes da inativação do tipo").path("id").asLong();
        enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "composição original completa");
        historico(c, TipoCatalogoTemporal.TIPO_CONTRATACAO, c.tipoA.getId()); c.tipoA.setAtivo(false); em.flush();
        var req = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-ORIGINAL-HISTORICO", "justificativa", "Documento antigo recebido posteriormente");
        var previa = enviar(c.localA, base(id) + "/previas/publicar", req, 200, "CAT-007/CON-014 prévia considera disponibilidade histórica");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        assertThat(enviar(c.localA, base(id) + "/publicar", req, 200, "CAT-007 publicação do original historicamente válido")
                .path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
    }

    @Test
    void trocaDeFornecedorNaoPodeConservarSnapshotsDaEmpresaAnterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        long fornecedor = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa sucessora",
                "cnpj", "98765432000198"), 200, "empresa sucessora cadastrada").path("id").asLong();
        var req = aditamento("FORNECEDOR-INCOMPLETO", n.inicio().plusDays(1)); req.put("novoFornecedorId", fornecedor);
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 400,
                "EMP-001/003 novo fornecedor com outro CNPJ exige fotografia completa da nova empresa");
    }

    @Test
    void fornecedorInformadoDeveConferirComCnpjExplicitoDoInstrumento() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        long fornecedor = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa sucessora",
                "cnpj", "98765432000198"), 200, "empresa sucessora cadastrada").path("id").asLong();
        var req = aditamento("FORNECEDOR-DIVERGENTE", n.inicio().plusDays(1)); req.put("novoFornecedorId", fornecedor);
        req.put("novoCnpjEmpresa", "12345678000190"); req.put("novaRazaoSocialEmpresa", "Empresa sucessora"); req.put("novoNomeFantasiaEmpresa", "Sucessora");
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 400, "EMP-003 fornecedor escolhido e CNPJ da empresa não podem divergir");
        req.put("novoCnpjEmpresa", "98765432000198");
        registrarAditamento(c, n.id(), req);
        var pos = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(1), "EMP-003 fotografia coerente após troca real");
        assertThat(pos.path("versaoContratual").path("cnpjEmpresa").asText()).isEqualTo("98765432000198");
        assertThat(pos.path("versaoContratual").path("fornecedorId").asLong()).isEqualTo(fornecedor);
    }

    @Test
    void inativacaoDepoisDaPreviaImpedeNovaTrocaSemAlterarFotografia() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var empresa = mapa("razaoSocial", "Empresa para nova troca", "cnpj", "98765432000198", "ativo", true);
        long fornecedor = enviar(c.localA, "/api/fornecedores", empresa, 200, "Cadastro da nova empresa").path("id").asLong();
        var d = n.inicio().plusDays(1);
        var req = aditamento("TROCA-INATIVA", d);
        req.putAll(mapa("novoFornecedorId", fornecedor, "novoCnpjEmpresa", "98765432000198",
                "novaRazaoSocialEmpresa", "Empresa para nova troca", "novoNomeFantasiaEmpresa", "Nova troca"));
        previaAditamento(c, n.id(), req);
        var versoes = consultar(c.localA, base(n.id()) + "/versoes", "Fotografias antes da inativação");
        empresa.put("ativo", false);
        executar(c.localA, put("/api/fornecedores/{id}", fornecedor).contentType(APPLICATION_JSON).content(json(empresa)),
                200, "Inativação cadastral posterior à prévia");
        enviar(c.localA, base(n.id()) + "/aditamentos", req, 409, "A prévia não torna a empresa inativa selecionável");
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 409, "Nova prévia recusa fornecedor inativo para troca");
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "Não houve ato").isEmpty()).isTrue();
        assertThat(consultar(c.localA, base(n.id()) + "/versoes", "Fotografias intactas")).isEqualTo(versoes);
    }

    @Test
    void retificacaoConservaEmpresaOriginalDocumentadaAposMudancaDoCadastro() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var empresa = mapa("razaoSocial", "Empresa documentada", "cnpj", "98765432000198");
        long fornecedor = enviar(c.localA, "/api/fornecedores", empresa, 200, "Cadastro da empresa documentada").path("id").asLong();
        var d = n.inicio().plusDays(1);
        var req = aditamento("TROCA-DOCUMENTADA", d);
        req.putAll(mapa("novoFornecedorId", fornecedor, "novoCnpjEmpresa", "98765432000198",
                "novaRazaoSocialEmpresa", "Empresa documentada", "novoNomeFantasiaEmpresa", "Documento original"));
        var original = registrarAditamento(c, n.id(), req);
        empresa.putAll(mapa("ativo", false, "cnpj", "11223344000181", "razaoSocial", "Cadastro atual"));
        executar(c.localA, put("/api/fornecedores/{id}", fornecedor).contentType(APPLICATION_JSON).content(json(empresa)),
                200, "Cadastro atual muda sem alterar a fonte anterior");
        req.putAll(mapa("eventoPrecedenteId", original.path("eventoId").asLong(), "relacaoPrecedencia", "RETIFICA",
                "justificativa", "Retifica a referência documental sem trocar empresa", "referenciaSei", "SEI-ADI-RETIFICADO"));
        var incompleta = new java.util.LinkedHashMap<>(req);
        incompleta.remove("novaRazaoSocialEmpresa");
        incompleta.remove("novoNomeFantasiaEmpresa");
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", incompleta, 400,
                "Retificação não pode substituir a fotografia da empresa por nomes herdados de outra");
        previaAditamento(c, n.id(), req);
        var novo = enviar(c.localA, base(n.id()) + "/aditamentos/" + original.path("id").asLong() + "/retificar",
                req, 200, "Retificação mantém o fornecedor e CNPJ do ato documentado");
        assertThat(novo.path("novoFornecedorId").asLong()).isEqualTo(fornecedor);
        assertThat(novo.path("novoCnpjEmpresa").asText()).isEqualTo("98765432000198");
        assertThat(novo.path("versaoInstrumento").asInt()).isEqualTo(2);
    }

    @Test
    void atoCanceladoNaoAutorizaReutilizarFornecedorInativo() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var empresa = mapa("razaoSocial", "Empresa de troca cancelada", "cnpj", "98765432000198");
        long fornecedor = enviar(c.localA, "/api/fornecedores", empresa, 200, "Cadastro do caso cancelado").path("id").asLong();
        var d = n.inicio().plusDays(1);
        var req = aditamento("TROCA-CANCELADA", d);
        req.putAll(mapa("novoFornecedorId", fornecedor, "novoCnpjEmpresa", "98765432000198",
                "novaRazaoSocialEmpresa", "Empresa de troca cancelada", "novoNomeFantasiaEmpresa", "Ato cancelado"));
        var original = registrarAditamento(c, n.id(), req);
        enviar(c.localA, base(n.id()) + "/aditamentos/" + original.path("id").asLong() + "/cancelar",
                mapa("motivo", "Cancelamento documentado antes da data efetiva", "referenciaSei", "SEI-CANCELA-TROCA"), 200, "Troca cancelada");
        empresa.put("ativo", false);
        executar(c.localA, put("/api/fornecedores/{id}", fornecedor).contentType(APPLICATION_JSON).content(json(empresa)),
                200, "Fornecedor não pode retornar como nova troca");
        req.putAll(mapa("eventoPrecedenteId", original.path("eventoId").asLong(), "relacaoPrecedencia", "RETIFICA"));
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 409, "Ato cancelado não é prova para reutilizar cadastro inativo");
        var itens = consultar(c.localA, base(n.id()) + "/aditamentos", "Permanece só o aditamento cancelado");
        assertThat(itens.size()).isEqualTo(1);
        assertThat(itens.get(0).path("situacao").asText()).isEqualTo("CANCELADO");
    }

    @Test
    void retificacaoSemIdNaoMisturaCnpjENomesNemAlteraAFotografiaExistente() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        long fornecedor = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa B da troca",
                "cnpj", "98765432000198"), 200, "Cadastro de B").path("id").asLong();
        var d = n.inicio().plusDays(1);
        String base = base(n.id());
        var req = aditamento("TROCA-COM-FOTOGRAFIA", d);
        req.putAll(mapa("novoFornecedorId", fornecedor, "novoCnpjEmpresa", "98765432000198",
                "novaRazaoSocialEmpresa", "Razão documentada de B", "novoNomeFantasiaEmpresa", "Fantasia documentada de B"));
        var original = registrarAditamento(c, n.id(), req);
        var fatos = consultar(c.localA, base + "/aditamentos", "Ato anterior à retificação");
        var versoes = consultar(c.localA, base + "/versoes", "Fotografias anteriores à retificação");
        var posicao = consultar(c.localA, base + "/posicao-temporal?data=" + d, "Empresa efetiva de B").path("versaoContratual");
        assertThat(posicao.path("fornecedorId").asLong()).isEqualTo(fornecedor);
        assertThat(posicao.path("nomeFantasiaEmpresa").asText()).isEqualTo("Fantasia documentada de B");
        req.putAll(mapa("eventoPrecedenteId", original.path("eventoId").asLong(), "relacaoPrecedencia", "RETIFICA",
                "novaDescricao", "Correção que não apaga a fotografia da empresa"));
        previaAditamento(c, n.id(), req);
        var parcial = new java.util.LinkedHashMap<>(req);
        parcial.remove("novoFornecedorId");
        parcial.remove("novaRazaoSocialEmpresa");
        parcial.remove("novoNomeFantasiaEmpresa");
        assertAll("Omissão do ID não desativa a validação documental",
                () -> enviar(c.localA, base + "/aditamentos/previa", parcial, 400, "Prévia não pode misturar CNPJ de B com nomes de A"),
                () -> enviar(c.localA, base + "/aditamentos/" + original.path("id").asLong() + "/retificar", parcial, 400,
                        "Confirmação valida os dados empresariais mesmo com token e sem ID"));
        assertThat(consultar(c.localA, base + "/aditamentos", "Recusa não cria ato")).isEqualTo(fatos);
        assertThat(consultar(c.localA, base + "/versoes", "Recusa não altera as fotografias")).isEqualTo(versoes);
        assertThat(consultar(c.localA, base + "/posicao-temporal?data=" + d, "Identidade empresarial permanece íntegra")
                .path("versaoContratual")).isEqualTo(posicao);
    }

    @Test
    void retificacaoDocumentalSemFornecedorNovoPreservaCamposDoAto() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var d = n.inicio().plusDays(1);
        var req = aditamento("NOME-DOCUMENTAL-SEM-TROCA", d);
        req.put("novaRazaoSocialEmpresa", "Razão documentada sem troca de cadastro");
        var original = registrarAditamento(c, n.id(), req);
        req.putAll(mapa("eventoPrecedenteId", original.path("eventoId").asLong(), "relacaoPrecedencia", "RETIFICA",
                "referenciaSei", "SEI-RETIFICA-SEM-TROCA"));
        previaAditamento(c, n.id(), req);
        var novo = enviar(c.localA, base(n.id()) + "/aditamentos/" + original.path("id").asLong() + "/retificar", req, 200,
                "Ato que só documentou o nome pode ser retificado sem selecionar fornecedor");
        assertThat(novo.hasNonNull("novoFornecedorId")).isFalse();
        assertThat(novo.path("novaRazaoSocialEmpresa").asText()).isEqualTo("Razão documentada sem troca de cadastro");
    }

    @Test
    void substituicaoNaoAceitaMeiaFotografiaMasPodeRetirarATrocaPorInteiro() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        long fornecedor = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa B do ato substituível",
                "cnpj", "98765432000198"), 200, "Cadastro de B").path("id").asLong();
        var d = n.inicio().plusDays(1);
        String base = base(n.id()), temporal = base + "/posicao-temporal?data=" + d;
        var a = consultar(c.localA, temporal, "Empresa da publicação original").path("versaoContratual");
        var req = aditamento("TROCA-SUBSTITUIVEL", d);
        req.putAll(mapa("novoFornecedorId", fornecedor, "novoCnpjEmpresa", "98765432000198",
                "novaRazaoSocialEmpresa", "Razão documentada de B", "novoNomeFantasiaEmpresa", "Fantasia documentada de B"));
        var original = registrarAditamento(c, n.id(), req);
        String antesDaSubstituicao = java.time.Instant.now().toString();
        var b = consultar(c.localA, temporal, "Empresa projetada pelo ato original").path("versaoContratual");
        var fatos = consultar(c.localA, base + "/aditamentos", "Fatos antes da substituição");
        var versoes = consultar(c.localA, base + "/versoes", "Fotografias antes da substituição");
        assertThat(b.path("fornecedorId").asLong()).isEqualTo(fornecedor);
        req.putAll(mapa("eventoPrecedenteId", original.path("eventoId").asLong(), "relacaoPrecedencia", "SUBSTITUI",
                "novaDescricao", "Novo instrumento documentado"));
        previaAditamento(c, n.id(), req);
        var parcial = new java.util.LinkedHashMap<>(req);
        parcial.remove("novoFornecedorId");
        parcial.remove("novaRazaoSocialEmpresa");
        parcial.remove("novoNomeFantasiaEmpresa");
        assertAll("Substituir a versão não pode mesclar duas identidades empresariais",
                () -> enviar(c.localA, base + "/aditamentos/previa", parcial, 400, "CNPJ de B isolado não forma uma fotografia"),
                () -> enviar(c.localA, base + "/aditamentos", parcial, 400, "O comando genérico também recusa a fotografia parcial"));
        assertThat(consultar(c.localA, base + "/aditamentos", "Recusa não cria ato")).isEqualTo(fatos);
        assertThat(consultar(c.localA, base + "/versoes", "Recusa não cria fotografia")).isEqualTo(versoes);
        assertThat(consultar(c.localA, temporal, "Recusa mantém B").path("versaoContratual")).isEqualTo(b);
        parcial.remove("novoCnpjEmpresa");
        parcial.put("justificativa", "Substitui integralmente o ato; a nova fonte não prevê troca de empresa");
        var substituto = registrarAditamento(c, n.id(), parcial);
        assertThat(substituto.path("versaoInstrumento").asInt()).isEqualTo(2);
        var atual = consultar(c.localA, temporal, "Substituição sem empresa volta à identidade A inteira").path("versaoContratual");
        for (String campo : java.util.List.of("fornecedorId", "cnpjEmpresa", "razaoSocialEmpresa", "nomeFantasiaEmpresa")) {
            assertThat(atual.path(campo)).isEqualTo(a.path(campo));
        }
        assertThat(consultar(c.localA, temporal + "&conhecidoAte=" + antesDaSubstituicao, "História anterior preserva B")
                .path("versaoContratual").path("fornecedorId").asLong()).isEqualTo(fornecedor);
    }

    @Test
    void novaIdentidadeNaSubstituicaoExigeOsDoisNomesMesmoComAntecessorParcial() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var d = n.inicio().plusDays(1);
        String base = base(n.id()), posicao = base + "/posicao-temporal?data=" + d;
        var req = aditamento("FONTE-PARCIAL-SO-NOME", d);
        req.put("novaRazaoSocialEmpresa", "Nome de A em documento anterior");
        var original = registrarAditamento(c, n.id(), req);
        var anterior = consultar(c.localA, posicao, "Fotografia de A com o nome documentado").path("versaoContratual");
        assertThat(anterior.hasNonNull("fornecedorId")).isFalse();
        var fotos = consultar(c.localA, base + "/versoes", "Fotografias antes da nova empresa");
        req.putAll(mapa("eventoPrecedenteId", original.path("eventoId").asLong(), "relacaoPrecedencia", "SUBSTITUI",
                "novoCnpjEmpresa", "98765432000198", "novaRazaoSocialEmpresa", "Empresa B documentada",
                "novoNomeFantasiaEmpresa", "B documental"));
        previaAditamento(c, n.id(), req);
        var parcial = new java.util.LinkedHashMap<>(req);
        parcial.remove("novoNomeFantasiaEmpresa");
        assertAll("Os dados exigidos pertencem à empresa resultante e não ao antecessor parcial",
                () -> enviar(c.localA, base + "/aditamentos/previa", parcial, 400, "Sem nome fantasia de B não existe fotografia completa"),
                () -> enviar(c.localA, base + "/aditamentos", parcial, 400, "O comando recusa a mesma fotografia parcial antes do token"));
        assertThat(consultar(c.localA, posicao, "Os dados de A permanecem").path("versaoContratual")).isEqualTo(anterior);
        assertThat(consultar(c.localA, base + "/versoes", "Recusa não cria fotografia")).isEqualTo(fotos);
        registrarAditamento(c, n.id(), req);
        var depois = consultar(c.localA, posicao, "Troca documentada completa sem vínculo cadastral prévio").path("versaoContratual");
        assertThat(depois.hasNonNull("fornecedorId")).isFalse();
        assertThat(depois.path("cnpjEmpresa").asText()).isEqualTo("98765432000198");
        assertThat(depois.path("razaoSocialEmpresa").asText()).isEqualTo("Empresa B documentada");
        assertThat(depois.path("nomeFantasiaEmpresa").asText()).isEqualTo("B documental");
    }

    @Test
    void trocaDeCnpjNaoConservaVinculoComFornecedorDeOutraEmpresa() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().plusDays(10);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "CONTRATO-COM-FORNECEDOR", 5, inicio, inicio.plusDays(60)),
                201, "Rascunho para associação documentada").path("id").asLong();
        long a = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa Original", "cnpj", "12345678000190"),
                200, "Fornecedor A").path("id").asLong();
        long b = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa B", "cnpj", "98765432000198"),
                200, "Fornecedor B").path("id").asLong();
        String base = base(id);
        executar(c.localA, put(base + "/fornecedor/" + a), 200, "A associado antes de publicar");
        enviar(c.localA, base + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "Composição original");
        enviar(c.localA, base + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-PUB-CNPJ",
                "justificativa", "Publicação da empresa A"), 200, "Publicação com associação A");
        var d = inicio.plusDays(1);
        String posicao = base + "/posicao-temporal?data=" + d;
        var antes = consultar(c.localA, posicao, "A efetiva na data").path("versaoContratual");
        assertThat(antes.path("fornecedorId").asLong()).isEqualTo(a);
        var req = aditamento("CNPJ-NAO-HERDA-OUTRO-FORNECEDOR", d);
        req.putAll(mapa("novoCnpjEmpresa", "98765432000198", "novaRazaoSocialEmpresa", "Empresa B", "novoNomeFantasiaEmpresa", "B"));
        enviar(c.localA, base + "/aditamentos/previa", req, 400, "CNPJ de B não pode continuar vinculado ao fornecedor A");
        assertThat(consultar(c.localA, posicao, "Recusa preserva A").path("versaoContratual")).isEqualTo(antes);
        req.put("novoFornecedorId", b);
        registrarAditamento(c, id, req);
        var depois = consultar(c.localA, posicao, "O mesmo ato com fornecedor B é coerente").path("versaoContratual");
        assertThat(depois.path("fornecedorId").asLong()).isEqualTo(b);
        assertThat(depois.path("cnpjEmpresa").asText()).isEqualTo("98765432000198");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void retiradaVigenteSincronizaAusenciaComprovadaSemLimparDadoDesconhecido(boolean fotoLegada) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        if (fotoLegada) {
            // Somente esta fixture simula a fotografia anterior à captura integral.
            int legada = emTransacao(() -> em.createQuery("""
                    update VersaoContratoNegocial v set v.matriculaVinculoObrigatoriaEfetiva=null
                    where v.contrato.id=:contrato and v.numeroVersao=1
                    """).setParameter("contrato", n.id()).executeUpdate());
            assertThat(legada).isEqualTo(1);
        }
        String base = base(n.id());
        var d = RelogioSistema.hoje();
        String posicao = base + "/posicao-temporal?data=" + d;
        var original = consultar(c.localA, posicao, "Empresa da publicação sem fornecedor na fotografia");
        var fotoA = original.path("versaoContratual");
        assertThat(fotoA.hasNonNull("fornecedorId")).isFalse();
        String cnpjB = fotoLegada ? "74001234000236" : "74001234000155";
        long b = enviar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa B", "cnpj", cnpjB),
                200, "Fornecedor sintético B exclusivo da variante").path("id").asLong();
        var req = aditamento("TROCA-COM-EFEITO-JA-VIGENTE", d);
        req.putAll(mapa("novoFornecedorId", b, "novoCnpjEmpresa", cnpjB,
                "novaRazaoSocialEmpresa", "Empresa B documentada", "novoNomeFantasiaEmpresa", "B documental"));
        var previa = enviar(c.localA, base + "/aditamentos/previa", req, 200, "Prévia da troca vigente");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        var troca = enviar(c.localA, base + "/aditamentos", req, 201, "Troca para B efetiva hoje");
        var emB = emTransacao(() -> em.find(Contrato.class, n.id()).getFornecedor().getId());
        assertThat(emB).isEqualTo(b);
        var k = java.time.Instant.now();
        var substituir = aditamento("TROCA-COM-EFEITO-JA-VIGENTE", d);
        substituir.putAll(mapa("eventoPrecedenteId", troca.path("eventoId").asLong(), "relacaoPrecedencia", "SUBSTITUI",
                "novaDescricao", "Novo ato retira integralmente o efeito empresarial anterior"));
        previa = enviar(c.localA, base + "/aditamentos/previa", substituir, 200, "Prévia de retirada integral");
        substituir.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(c.localA, base + "/aditamentos", substituir, 201, "Substituição efetiva, confirmada em transação HTTP própria");
        var depois = consultar(c.localA, posicao, "Posição após a substituição");
        assertThat(depois.path("versaoContratual").hasNonNull("fornecedorId")).isFalse();
        var persistida = emTransacao(() -> {
            Contrato entidade = em.find(Contrato.class, n.id());
            var prova = aditamentos.projetarLeituraContrato(entidade, d, java.time.Instant.now()).provaCampos().get("fornecedorId");
            return mapa("id", entidade.getFornecedor() == null ? null : entidade.getFornecedor().getId(),
                    "cnpj", entidade.getCnpjEmpresa(), "razao", entidade.getRazaoSocialEmpresa(), "fantasia", entidade.getNomeFantasiaEmpresa(),
                    "prova", prova.estado().name());
        });
        assertAll("Persistência pós-commit distingue ausência de informação desconhecida",
                () -> assertThat(persistida.get("id")).isEqualTo(fotoLegada ? b : null),
                () -> assertThat(persistida.get("prova")).isEqualTo(fotoLegada ? "NAO_COMPROVADO" : "AUSENCIA_COMPROVADA"));
        assertThat(persistida.get("cnpj")).isEqualTo(fotoA.path("cnpjEmpresa").asText());
        assertThat(persistida.get("razao")).isEqualTo(fotoA.path("razaoSocialEmpresa").asText());
        assertThat(persistida.get("fantasia")).isEqualTo(fotoA.path("nomeFantasiaEmpresa").asText());
        assertThat(consultar(c.localA, posicao + "&conhecidoAte=" + k, "O histórico anterior ainda prova a associação B")
                .path("versaoContratual").path("fornecedorId").asLong()).isEqualTo(b);
    }

    private void historico(Cenario c, TipoCatalogoTemporal tipo, long id) {
        for (boolean ativo : new boolean[]{true, false}) {
            var e = new EventoDisponibilidadeCatalogo(); e.setTipoCatalogo(tipo); e.setCatalogoId(id);
            e.setDataEfetiva(ativo ? LocalDate.of(2026, 1, 1) : RelogioSistema.hoje()); e.setAtivo(ativo);
            e.setMotivo("Disponibilidade documentalmente conhecida"); e.setResponsavelId(c.localA.getId()); e.setResponsavelUsername(c.localA.getUsername());
            em.persist(e);
        }
    }
}
