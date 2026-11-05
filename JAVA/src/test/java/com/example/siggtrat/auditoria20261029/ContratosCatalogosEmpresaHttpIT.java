package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** CT07/CT08: coerência de catálogo histórico e da empresa escolhida no instrumento. */
class ContratosCatalogosEmpresaHttpIT extends ContratosHttpBase {
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

    private void historico(Cenario c, TipoCatalogoTemporal tipo, long id) {
        for (boolean ativo : new boolean[]{true, false}) {
            var e = new EventoDisponibilidadeCatalogo(); e.setTipoCatalogo(tipo); e.setCatalogoId(id);
            e.setDataEfetiva(ativo ? LocalDate.of(2026, 1, 1) : RelogioSistema.hoje()); e.setAtivo(ativo);
            e.setMotivo("Disponibilidade documentalmente conhecida"); e.setResponsavelId(c.localA.getId()); e.setResponsavelUsername(c.localA.getUsername());
            em.persist(e);
        }
    }
}
