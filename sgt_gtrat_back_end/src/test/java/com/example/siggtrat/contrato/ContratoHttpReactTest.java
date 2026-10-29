package com.example.siggtrat.contrato;

import com.example.siggtrat.controlador.OcorrenciaControlador;
import com.example.siggtrat.controlador.PerfilControlador;
import com.example.siggtrat.controlador.RegistroAuditoriaControlador;
import com.example.siggtrat.controlador.StatusEmpregadoControlador;
import com.example.siggtrat.controlador.UsuarioAplicacaoControlador;
import com.example.siggtrat.dto.AlteracaoSenhaInicialAutenticacaoDTO;
import com.example.siggtrat.dto.AlteracaoSenhaPerfilDTO;
import com.example.siggtrat.dto.AtualizacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.CadastroAutenticacaoDTO;
import com.example.siggtrat.dto.CalendarioEventoRequisicao;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.dto.CriacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.DesativacaoSegundoFatorDTO;
import com.example.siggtrat.dto.EmpregadoContaDTO;
import com.example.siggtrat.dto.EmpregadoContratoDTO;
import com.example.siggtrat.dto.EmpregadoDTO;
import com.example.siggtrat.dto.EmpregadoEnderecoDTO;
import com.example.siggtrat.dto.EmpregadoTelefoneDTO;
import com.example.siggtrat.dto.FaturaGeracaoDTO;
import com.example.siggtrat.dto.FaturaStatusDTO;
import com.example.siggtrat.dto.FolhaHorasGeracaoDTO;
import com.example.siggtrat.dto.FornecedorGovernancaEntradaDTO;
import com.example.siggtrat.dto.MotivoGovernancaDTO;
import com.example.siggtrat.dto.ProcessoTerceiroEntradaDTO;
import com.example.siggtrat.dto.RedefinicaoSenhaUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.RenovacaoSessaoDTO;
import com.example.siggtrat.dto.RequisicaoCriacaoFechamentoFrequencia;
import com.example.siggtrat.dto.RiscoFornecedorEntradaDTO;
import com.example.siggtrat.dto.StatusGovernancaDTO;
import com.example.siggtrat.dto.TabelaTarifariaGovernancaEntradaDTO;
import com.example.siggtrat.dto.TokenTotpDTO;
import com.example.siggtrat.dto.ValidacaoSenhaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContratoHttpReactTest {

    @Test
    void permiteQueTodoUsuarioAutenticadoConsulteOProprioPerfil() throws Exception {
        RequestMapping base = UsuarioAplicacaoControlador.class.getAnnotation(RequestMapping.class);
        Method metodo = Arrays.stream(UsuarioAplicacaoControlador.class.getDeclaredMethods())
                .filter(candidato -> candidato.getName().equals("obterMeuPerfil"))
                .findFirst()
                .orElseThrow();
        GetMapping recurso = metodo.getAnnotation(GetMapping.class);
        String rota = base.value()[0] + recurso.value()[0];
        String seguranca = Files.readString(Path.of(
                "src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"
        ));
        String filtroCache = Files.readString(Path.of(
                "src/main/java/com/example/siggtrat/configuracao/FiltroCacheConsultaHttpRedis.java"
        ));

        assertEquals("/api/usuarios/meu-perfil", rota);
        assertTrue(seguranca.contains("\"" + rota + "\""));
        assertTrue(seguranca.indexOf("\"" + rota + "\"") < seguranca.indexOf("\"/api/usuarios/**\""));
        assertTrue(filtroCache.contains("uri.equals(\"" + rota + "\")"));
    }

    @Test
    void recebeOsNomesDosParametrosEnviadosPelosFiltrosDeAuditoria() {
        assertEquals(
                Set.of("action", "username", "entityType", "entityId", "requestId", "startDate", "endDate",
                        "includeEntityType", "includeEntityIds"),
                parametrosDeRequisicao(RegistroAuditoriaControlador.class, "pesquisar")
        );
    }

    @Test
    void recebeAnexosComOMesmoNomeDoFormularioMultipart() {
        Set<String> parametros = parametrosDeRequisicao(OcorrenciaControlador.class, "criar");
        assertTrue(parametros.containsAll(Set.of(
                "empregadoId", "motivo", "dataOcorrencia", "observacao", "tipoRo", "numeroSei", "dias", "anexos"
        )));
    }

    @Test
    void recebeAvatarComOMesmoNomeDoFormularioMultipart() {
        assertTrue(parametrosDeRequisicao(PerfilControlador.class, "enviarAvatar").contains("file"));
    }

    @Test
    void publicaARotaDeStatusUsadaNosDadosAuxiliaresDoEmpregado() {
        RequestMapping mapeamento = StatusEmpregadoControlador.class.getAnnotation(RequestMapping.class);
        assertTrue(Arrays.asList(mapeamento.value()).contains("/api/status-empregados"));
    }

    @Test
    void mantemOsCorposDeAutenticacaoEPerfilCompativeisComOReact() {
        assertComponentes(CredenciaisLoginDTO.class, "matricula", "password", "totp");
        assertComponentes(AlteracaoSenhaInicialAutenticacaoDTO.class, "newPassword");
        assertComponentes(CadastroAutenticacaoDTO.class, "username", "email", "matricula", "superintendenciaId", "gestaoLocalId");
        assertComponentes(RenovacaoSessaoDTO.class, "password");
        assertComponentes(ValidacaoSenhaDTO.class, "password");
        assertComponentes(TokenTotpDTO.class, "token");
        assertComponentes(DesativacaoSegundoFatorDTO.class, "currentPassword", "token");
        assertComponentes(AlteracaoSenhaPerfilDTO.class, "currentPassword", "newPassword", "token");
    }

    @Test
    void mantemOsCorposDeUsuariosCompativeisComOReact() {
        assertPossuiComponentes(CriacaoUsuarioAplicacaoDTO.class,
                "username", "email", "matricula", "password", "perfil", "active", "lotacaoId", "jornadaId",
                "vinculosGestao", "superintendenciaId", "gestaoLocalId");
        assertPossuiComponentes(AtualizacaoUsuarioAplicacaoDTO.class,
                "email", "active", "matricula", "perfil", "lotacaoId", "jornadaId", "vinculosGestao",
                "superintendenciaId", "gestaoLocalId");
        assertComponentes(AtualizacaoUsuarioAplicacaoDTO.VinculoGestaoDTO.class, "lotacaoId", "jornadaId");
        assertComponentes(RedefinicaoSenhaUsuarioAplicacaoDTO.class, "newPassword");
    }

    @Test
    void mantemOCadastroDeEmpregadoCompativelComOReact() {
        assertPossuiComponentes(EmpregadoDTO.class,
                "id", "nome", "matricula", "cpf", "rg", "ctpsSerie", "nitPisPasep", "formacaoEscolar",
                "email", "dataNascimento", "dataAdmissao", "optanteFgts", "dataOpcaoFgts", "qtdDependenteIr",
                "qtdDependenteSalFamilia", "optanteVa", "valorVa", "outrosBeneficios", "observacao", "lotacaoId",
                "cargoId", "statusId", "jornadaId", "contratos", "telefones", "conta", "endereco");
        assertPossuiComponentes(EmpregadoContaDTO.class, "numeroConta", "bancoId", "agenciaId", "tipoContaId");
        assertPossuiComponentes(EmpregadoEnderecoDTO.class,
                "logradouro", "numero", "bairro", "cidade", "uf", "cep");
        assertPossuiComponentes(EmpregadoTelefoneDTO.class, "numero");
        assertPossuiComponentes(EmpregadoContratoDTO.class, "id");
    }

    @Test
    void mantemOsCorposDeFrequenciaECalendarioCompativeisComOReact() {
        assertComponentes(RequisicaoCriacaoFechamentoFrequencia.class,
                "dataReferencia", "lotacaoId", "jornadaId", "contratoIds", "alteracoes");
        assertComponentes(CalendarioEventoRequisicao.class,
                "data", "tipo", "descricao", "abrangencia", "uf", "municipio", "lotacaoId",
                "superintendenciaId", "gestaoLocalId", "ativo");
    }

    @Test
    void mantemOsCorposDeGovernancaCompativeisComOReact() {
        assertComponentes(FornecedorGovernancaEntradaDTO.class, "razaoSocial", "cnpj", "email", "ativo");
        assertComponentes(TabelaTarifariaGovernancaEntradaDTO.class,
                "contratoId", "cargoId", "vigenciaInicio", "vigenciaFim", "valorHora", "adicionalNoturnoPercent");
        assertComponentes(ProcessoTerceiroEntradaDTO.class,
                "empregadoId", "contratoId", "tipo", "prazo", "checklistJson", "observacao");
        assertComponentes(StatusGovernancaDTO.class, "status");
        assertComponentes(FolhaHorasGeracaoDTO.class, "contratoId", "inicio", "fim");
        assertComponentes(MotivoGovernancaDTO.class, "motivo");
        assertComponentes(FaturaGeracaoDTO.class, "glosas");
        assertComponentes(FaturaStatusDTO.class, "status", "numeroDocumento");
        assertComponentes(RiscoFornecedorEntradaDTO.class,
                "fornecedorId", "scoreTrabalhista", "scoreFinanceiro", "scoreCompliance", "scoreSeguranca",
                "proximaAvaliacao", "evidencias");
    }

    private Set<String> parametrosDeRequisicao(Class<?> controlador, String nomeMetodo) {
        return Arrays.stream(controlador.getDeclaredMethods())
                .filter(metodo -> metodo.getName().equals(nomeMetodo))
                .findFirst()
                .stream()
                .map(Method::getParameters)
                .flatMap(Arrays::stream)
                .map(parametro -> parametro.getAnnotation(RequestParam.class))
                .filter(anotacao -> anotacao != null)
                .map(anotacao -> anotacao.name().isBlank() ? anotacao.value() : anotacao.name())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertComponentes(Class<?> tipo, String... esperados) {
        assertEquals(Set.of(esperados), nomesComponentes(tipo));
    }

    private void assertPossuiComponentes(Class<?> tipo, String... esperados) {
        assertTrue(nomesComponentes(tipo).containsAll(Set.of(esperados)));
    }

    private Set<String> nomesComponentes(Class<?> tipo) {
        return Arrays.stream(tipo.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
