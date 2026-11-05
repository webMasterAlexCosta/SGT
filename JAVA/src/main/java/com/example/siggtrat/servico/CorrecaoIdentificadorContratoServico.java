package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.CorrecaoIdentificadorContratoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.time.Instant;
import com.example.siggtrat.utilitario.RelogioSistema;

/** Fluxo exclusivo para corrigir número/ano sem reescrever a identidade histórica. */
@Service
@Transactional
public class CorrecaoIdentificadorContratoServico {
    private final ContratoRepositorio contratoRepositorio;
    private final CorrecaoIdentificadorContratoRepositorio correcaoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final PermissaoExcepcionalServico permissaoServico;
    private final MotorTemporalContratoServico motorTemporal;
    private final DocumentoComprobatorioServico documentoServico;
    private final RegistroAuditoriaServico auditoria;
    private final IdentidadeContratualServico identidadeContratualServico;
    private GestaoContratoTemporalServico gestaoContratoTemporal;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGestaoContratoTemporal(GestaoContratoTemporalServico servico) {
        this.gestaoContratoTemporal = servico;
    }

    public CorrecaoIdentificadorContratoServico(
            ContratoRepositorio contratoRepositorio,
            CorrecaoIdentificadorContratoRepositorio correcaoRepositorio,
            AcessoOrganizacionalServico acesso,
            PermissaoExcepcionalServico permissaoServico,
            MotorTemporalContratoServico motorTemporal,
            DocumentoComprobatorioServico documentoServico,
            RegistroAuditoriaServico auditoria,
            IdentidadeContratualServico identidadeContratualServico) {
        this.contratoRepositorio = contratoRepositorio;
        this.correcaoRepositorio = correcaoRepositorio;
        this.acesso = acesso;
        this.permissaoServico = permissaoServico;
        this.motorTemporal = motorTemporal;
        this.documentoServico = documentoServico;
        this.auditoria = auditoria;
        this.identidadeContratualServico = identidadeContratualServico;
    }

    public CorrecaoIdentificadorContratoDTO corrigir(Long contratoId,
                                                      CorrecaoIdentificadorContratoRequisicao requisicao) {
        if (requisicao == null) throw new IllegalArgumentException("Informe a correção do identificador.");
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoContrato(usuario);

        String chave = texto(requisicao.chaveIdempotencia(), "Informe a chave de idempotência.");
        validarRequisicao(requisicao);
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        // A sessão pode ter perdido autorização enquanto aguardava a raiz.
        // Isso também vale para o retorno idempotente, que não consome nova permissão.
        acesso.revalidarAposEspera(usuario);
        acesso.exigirGestaoContrato(usuario);
        acesso.exigirAcessoContrato(usuario, contrato);
        exigirGestorLocalResponsavel(usuario, contrato);
        Optional<CorrecaoIdentificadorContrato> repetida = correcaoRepositorio.findByChaveIdempotencia(chave);
        if (repetida.isPresent()) return validarRepeticao(repetida.get(), contratoId, usuario, requisicao);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A correção controlada do identificador exige contrato publicado.");
        }
        validarRequisicao(requisicao);

        String numeroNovo = texto(requisicao.novoNumero(), "Informe o novo número do contrato.");
        identidadeContratualServico.validarDisponibilidade(numeroNovo, requisicao.novoAno(), contrato.getId());

        documentoServico.validarFonteObrigatoria(contrato, TipoFatoDocumental.CORRECAO_IDENTIFICADOR,
                chave, requisicao.referenciaSei());
        PermissaoExcepcional permissao = permissaoServico.exigirERegistrarUso(usuario,
                TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO,
                contrato, gestaoAtual(contrato), requisicao.motivo(), "Contrato", contrato.getId());
        String autorizacaoConcedida = permissao.getAutorizacaoAdministrativa();
        if (!autorizacaoConcedida.equals(texto(requisicao.autorizacaoAdministrativa(),
                "Informe a autorização administrativa."))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A autorização informada não corresponde à concessão administrativa vigente.");
        }

        VersaoContratoNegocialDTO antes = motorTemporal.reconstruirPosicao(
                contrato, requisicao.dataEfetiva(), Instant.now()).versaoContratual();
        if (antes == null || vazio(antes.numeroContrato()) || antes.anoContrato() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O identificador anterior não está comprovado no alcance efetivo da correção.");
        String numeroAnterior = texto(antes.numeroContrato(), "Número histórico ausente.");
        Integer anoAnterior = antes.anoContrato();
        if (numeroAnterior.equalsIgnoreCase(numeroNovo) && anoAnterior.equals(requisicao.novoAno())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O novo número/ano deve ser diferente do identificador vigente no alcance informado.");
        }

        identidadeContratualServico.reservar(contrato, numeroAnterior, anoAnterior);
        identidadeContratualServico.reservar(contrato, numeroNovo, requisicao.novoAno());
        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                requisicao.dataEfetiva(), requisicao.dataAto(), requisicao.motivo(), requisicao.referenciaSei(),
                requisicao.eventoPrecedenteId(), requisicao.relacaoPrecedencia(), requisicao.ordemDocumental());
        String anterior = identificador(numeroAnterior, anoAnterior);
        String novo = identificador(numeroNovo, requisicao.novoAno());
        EventoCicloVidaContrato evento = motorTemporal.registrarEvento(contrato,
                TipoEventoCicloVidaContrato.CORRECAO_IDENTIFICADOR, acao, usuario, anterior, novo);

        motorTemporal.registrarFotografiaProjetada(contrato, evento, requisicao.dataEfetiva(),
                antes.comIdentificador(numeroNovo, requisicao.novoAno()));
        // Inserir um ato em D1 não substitui o identificador determinado por D2>D1.
        VersaoContratoNegocialDTO atual = motorTemporal.reconstruirPosicao(
                contrato, RelogioSistema.hoje(), Instant.now()).versaoContratual();
        if (atual != null && atual.numeroContrato() != null && atual.anoContrato() != null) {
            contrato.setNumeroContrato(atual.numeroContrato());
            contrato.setAnoContrato(atual.anoContrato());
        }
        contratoRepositorio.save(contrato);

        CorrecaoIdentificadorContrato correcao = new CorrecaoIdentificadorContrato();
        correcao.setContrato(contrato);
        correcao.setEvento(evento);
        correcao.setPermissaoUtilizada(permissao);
        correcao.setNumeroAnterior(numeroAnterior);
        correcao.setAnoAnterior(anoAnterior);
        correcao.setNumeroNovo(numeroNovo);
        correcao.setAnoNovo(requisicao.novoAno());
        correcao.setDataEfetiva(requisicao.dataEfetiva());
        correcao.setDataAto(requisicao.dataAto());
        correcao.setMotivo(requisicao.motivo());
        correcao.setReferenciaSei(requisicao.referenciaSei());
        correcao.setAutorizacaoAdministrativa(autorizacaoConcedida);
        correcao.setChaveIdempotencia(chave);
        correcao.setResponsavelId(usuario.getId());
        correcao.setResponsavelUsername(usuario.getUsername());
        correcao = correcaoRepositorio.save(correcao);

        documentoServico.publicarFontesDoFato(contrato, TipoFatoDocumental.CORRECAO_IDENTIFICADOR,
                chave, correcao.getId(), requisicao.referenciaSei(), requisicao.dataEfetiva(), usuario);
        auditoria.registrarAcao("CORRIGIR_IDENTIFICADOR_CONTRATO", "Contrato", contrato.getId(), novo,
                Map.of("correcaoId", correcao.getId(), "permissaoId", permissao.getId(),
                        "anterior", anterior, "novo", novo, "autorizacao", autorizacaoConcedida));
        return paraDTO(correcao);
    }

    @Transactional(readOnly = true)
    public List<CorrecaoIdentificadorContratoDTO> listar(Long contratoId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        Contrato contrato = contratoRepositorio.findById(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(usuario, contrato);
        return correcaoRepositorio.findByContratoIdOrderByRegistradoEmAscIdAsc(contratoId)
                .stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CorrecaoIdentificadorContratoDTO> pesquisarHistorico(String numero, Integer ano) {
        if (ano == null || ano < 1900 || ano > 9999) throw new IllegalArgumentException("Informe um ano válido.");
        String numeroNormalizado = texto(numero, "Informe o número do contrato.");
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        return correcaoRepositorio.pesquisarHistorico(numeroNormalizado, ano).stream()
                .filter(c -> acesso.podeAcessarContrato(usuario, c.getContrato()))
                .map(this::paraDTO).toList();
    }

    private CorrecaoIdentificadorContratoDTO validarRepeticao(CorrecaoIdentificadorContrato correcao,
                                                               Long contratoId, UsuarioAplicacao usuario,
                                                               CorrecaoIdentificadorContratoRequisicao requisicao) {
        if (!Objects.equals(correcao.getContrato().getId(), contratoId)
                || !Objects.equals(correcao.getResponsavelId(), usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência já foi utilizada em outra operação.");
        }
        acesso.exigirAcessoContrato(usuario, correcao.getContrato());
        boolean mesmaIntencao = texto(correcao.getNumeroNovo(), "Identificador histórico ausente.")
                        .equalsIgnoreCase(texto(requisicao.novoNumero(), "Informe o novo número."))
                && Objects.equals(correcao.getAnoNovo(), requisicao.novoAno())
                && Objects.equals(correcao.getDataEfetiva(), requisicao.dataEfetiva())
                && Objects.equals(correcao.getDataAto(), requisicao.dataAto())
                && mesmosTextos(correcao.getMotivo(), requisicao.motivo())
                && mesmosTextos(correcao.getReferenciaSei(), requisicao.referenciaSei())
                && mesmosTextos(correcao.getAutorizacaoAdministrativa(), requisicao.autorizacaoAdministrativa())
                && Objects.equals(correcao.getEvento().getOrdemDocumental(), requisicao.ordemDocumental());
        if (!mesmaIntencao || !mesmaPrecedencia(correcao, requisicao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência foi reutilizada para uma correção diferente.");
        }
        return paraDTO(correcao);
    }

    private boolean mesmosTextos(String a, String b) {
        return Objects.equals(a == null ? null : a.trim().replaceAll("\\s+", " "),
                b == null ? null : b.trim().replaceAll("\\s+", " "));
    }

    private boolean mesmaPrecedencia(CorrecaoIdentificadorContrato correcao,
                                      CorrecaoIdentificadorContratoRequisicao requisicao) {
        List<RelacaoPrecedenciaEventoContrato> relacoes = motorTemporal
                .relacoesConhecidas(correcao.getContrato().getId(), correcao.getRegistradoEm()).stream()
                .filter(r -> r.getEventoFonte() != null
                        && Objects.equals(r.getEventoFonte().getId(), correcao.getEvento().getId()))
                .toList();
        if (requisicao.eventoPrecedenteId() == null) return relacoes.isEmpty();
        return relacoes.size() == 1 && relacoes.get(0).getEventoAlvo() != null
                && Objects.equals(relacoes.get(0).getEventoAlvo().getId(), requisicao.eventoPrecedenteId())
                && relacoes.get(0).getTipoRelacao() == requisicao.relacaoPrecedencia();
    }

    private void validarRequisicao(CorrecaoIdentificadorContratoRequisicao r) {
        if (vazio(r.novoNumero()) || r.novoAno() == null || r.novoAno() < 1900 || r.novoAno() > 9999
                || r.dataEfetiva() == null || r.dataAto() == null || vazio(r.motivo())
                || vazio(r.referenciaSei()) || vazio(r.autorizacaoAdministrativa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Novo número/ano, datas, motivo, fonte e autorização administrativa são obrigatórios.");
        }
        if ((r.eventoPrecedenteId() == null) != (r.relacaoPrecedencia() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eventoPrecedenteId e relacaoPrecedencia devem ser informados juntos.");
        }
    }

    private void exigirGestorLocalResponsavel(UsuarioAplicacao usuario, Contrato contrato) {
        GestaoLocal gestao = gestaoAtual(contrato);
        if (!acesso.ehGestaoLocal(usuario) || usuario.getGestaoLocal() == null
                || gestao == null
                || !Objects.equals(usuario.getGestaoLocal().getId(), gestao.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A correção deve ser executada pelo Gestor Local responsável pelo contrato.");
        }
    }

    private GestaoLocal gestaoAtual(Contrato contrato) {
        return gestaoContratoTemporal == null ? contrato.getGestaoLocal() : gestaoContratoTemporal.gestaoAtual(contrato);
    }

    private String identificador(String numero, Integer ano) { return numero + "/" + ano; }
    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }
    private String texto(String valor, String mensagem) {
        if (vazio(valor)) throw new IllegalArgumentException(mensagem);
        return valor.trim().replaceAll("\\s+", " ");
    }

    private CorrecaoIdentificadorContratoDTO paraDTO(CorrecaoIdentificadorContrato c) {
        return new CorrecaoIdentificadorContratoDTO(c.getId(), c.getContrato().getId(), c.getEvento().getId(),
                c.getPermissaoUtilizada().getId(), c.getNumeroAnterior(), c.getAnoAnterior(), c.getNumeroNovo(),
                c.getAnoNovo(), c.getDataEfetiva(), c.getDataAto(), c.getMotivo(), c.getReferenciaSei(),
                c.getAutorizacaoAdministrativa(), c.getChaveIdempotencia(), c.getRegistradoEm(),
                c.getResponsavelId(), c.getResponsavelUsername());
    }
}
