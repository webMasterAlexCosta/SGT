package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.CorrecaoCnpjContratoRepositorio;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class CorrecaoCnpjContratoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CorrecaoCnpjContratoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final ContratoRepositorio contratos;
    private final CorrecaoCnpjContratoRepositorio correcoes;
    private final AcessoOrganizacionalServico acesso;
    private final MotorTemporalContratoServico motor;
    private final AditamentoContratualServico projecao;
    private final DocumentoComprobatorioServico documentos;
    private final RegistroAuditoriaServico auditoria;

    public CorrecaoCnpjContratoServico(ContratoRepositorio contratos, CorrecaoCnpjContratoRepositorio correcoes,
            AcessoOrganizacionalServico acesso, MotorTemporalContratoServico motor, AditamentoContratualServico projecao,
            DocumentoComprobatorioServico documentos, RegistroAuditoriaServico auditoria) {
        this.contratos=contratos; this.correcoes=correcoes; this.acesso=acesso; this.motor=motor;
        this.projecao=projecao; this.documentos=documentos; this.auditoria=auditoria;
    }

    public CorrecaoCnpjContratoDTO corrigir(Long contratoId, CorrecaoCnpjContratoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        acesso.exigirGestaoContrato(user);
        Contrato contrato = contratos.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(user, contrato);
        if (!acesso.ehGestaoLocal(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o Gestor Local responsável corrige CNPJ.");
        String cnpj = req.cnpjCorrigido().replaceAll("\\D", "");
        if (cnpj.length() != 14) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ corrigido deve conter 14 dígitos.");
        String chave = req.chaveIdempotencia().trim();
        String intencao = assinatura(req, cnpj, user.getId());
        var repetida = correcoes.findByContratoIdAndChaveIdempotencia(contratoId, chave);
        if (repetida.isPresent()) {
            CorrecaoCnpjContrato c = repetida.get();
            if (!Objects.equals(c.getAssinaturaRequisicao(), intencao)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Chave de idempotência já utilizada com outra correção.");
            }
            return dto(c);
        }
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correção material de CNPJ exige contrato publicado.");
        }
        VersaoContratoNegocialDTO antes = projecao.projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), Set.of());
        if (antes.dataInicio() == null || req.dataEfetiva().isBefore(antes.dataInicio())
                || antes.dataFim() == null || req.dataEfetiva().isAfter(antes.dataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O alcance da correção deve estar contido na vigência reconstruída.");
        }
        if (Objects.equals(cnpj, antes.cnpjEmpresa())) throw new ResponseStatusException(HttpStatus.CONFLICT, "O CNPJ corrigido é igual ao já vigente no alcance informado.");
        documentos.validarFonteObrigatoria(contrato, TipoFatoDocumental.CORRECAO_CNPJ, chave, req.referenciaSei());
        var acao = new AcaoCicloVidaContratoDTO(req.dataEfetiva(), req.dataAto(), req.motivo(), req.referenciaSei(),
                req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental());
        var evento = motor.registrarEvento(contrato, TipoEventoCicloVidaContrato.CORRECAO_CNPJ, acao, user, antes.cnpjEmpresa(), cnpj);
        var c = new CorrecaoCnpjContrato(); c.setContrato(contrato); c.setEvento(evento); c.setCnpjAnterior(antes.cnpjEmpresa());
        c.setCnpjCorrigido(cnpj); c.setDataEfetiva(req.dataEfetiva()); c.setDataAto(req.dataAto()); c.setMotivo(req.motivo().trim());
        c.setReferenciaSei(req.referenciaSei().trim()); c.setChaveIdempotencia(chave); c.setResponsavelId(user.getId()); c.setResponsavelUsername(user.getUsername());
        c.setAssinaturaRequisicao(intencao);
        c = correcoes.save(c);
        var foto = projecao.projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), Set.of());
        motor.registrarFotografiaProjetada(contrato, evento, req.dataEfetiva(), foto);
        // Uma troca real posterior continua prevalecendo na projeção corrente.
        contrato.setCnpjEmpresa(projecao.projetarVersaoContrato(contrato, RelogioSistema.hoje(), Instant.now(), Set.of()).cnpjEmpresa());
        contratos.save(contrato);
        documentos.publicarFontesDoFato(contrato, TipoFatoDocumental.CORRECAO_CNPJ, chave, c.getId(), req.referenciaSei(), req.dataEfetiva(), user);
        auditoria.registrarAcao("CORRIGIR_CNPJ_CONTRATO", "Contrato", contratoId, cnpj,
                Map.of("correcaoId", c.getId(), "cnpjAnterior", c.getCnpjAnterior(), "cnpjCorrigido", cnpj,
                        "motivo", c.getMotivo(), "referenciaSei", c.getReferenciaSei()));
        return dto(c);
    }

    @Transactional(readOnly = true)
    public List<CorrecaoCnpjContratoDTO> listar(Long contratoId) {
        var user = acesso.usuarioAtual(); acesso.exigirConsultaContrato(user);
        var contrato = contratos.findById(contratoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(user, contrato);
        return correcoes.findByContratoIdOrderByRegistradoEmAscIdAsc(contratoId).stream().map(this::dto).toList();
    }

    private CorrecaoCnpjContratoDTO dto(CorrecaoCnpjContrato c) {
        return new CorrecaoCnpjContratoDTO(c.getId(), c.getContrato().getId(), c.getEvento().getId(), c.getCnpjAnterior(),
                c.getCnpjCorrigido(), c.getDataEfetiva(), c.getDataAto(), c.getMotivo(), c.getReferenciaSei(), c.getChaveIdempotencia(),
                c.getResponsavelId(), c.getResponsavelUsername(), c.getRegistradoEm());
    }

    private String assinatura(CorrecaoCnpjContratoRequisicao req, String cnpj, Long usuarioId) {
        return AssinaturaPreviaNegocial.sha256("sgt.correcao-cnpj.intencao.v1", AssinaturaPreviaNegocial.partes(cnpj, req.dataEfetiva(), req.dataAto(),
                req.motivo().trim(), req.referenciaSei().trim(), usuarioId, req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental()));
    }
}
