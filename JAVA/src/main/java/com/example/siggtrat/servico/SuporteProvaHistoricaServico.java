package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.documento.EstadoDocumentoComprobatorio;
import com.example.siggtrat.dto.DocumentoComprobatorioDTO;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Controles comuns de custódia, fonte e metadados; não resolve valores históricos. */
@Service
public class SuporteProvaHistoricaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(SuporteProvaHistoricaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final EntityManager em;
    private final AcessoOrganizacionalServico acesso;
    private final GestaoContratoTemporalServico gestao;
    private final PermissaoExcepcionalServico permissoes;
    private final DocumentoComprobatorioServico documentos;
    public SuporteProvaHistoricaServico(EntityManager em, AcessoOrganizacionalServico acesso,
            GestaoContratoTemporalServico gestao, PermissaoExcepcionalServico permissoes,
            DocumentoComprobatorioServico documentos) {
        this.em = em; this.acesso = acesso; this.gestao = gestao; this.permissoes = permissoes; this.documentos = documentos;
    }
    public UsuarioAplicacao executor() {
        UsuarioAplicacao u = acesso.usuarioAtual();
        if (!Boolean.TRUE.equals(u.getActive()) || !acesso.ehGestaoLocal(u)
                || u.getGestaoLocal() == null || !Boolean.TRUE.equals(u.getGestaoLocal().getAtivo()))
            throw erro(HttpStatus.FORBIDDEN, "Somente Gestor Local ativo incorpora prova histórica.");
        return u;
    }
    public Contrato contrato(Long id, boolean lock) {
        if (id == null) throw erro(HttpStatus.BAD_REQUEST, "Contrato é obrigatório.");
        Contrato c = em.find(Contrato.class, id);
        if (c == null) throw erro(HttpStatus.NOT_FOUND, "Contrato não encontrado.");
        if (lock) em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
        return c;
    }
    public void exigirCustodia(UsuarioAplicacao u, Contrato c) {
        acesso.revalidarAposEspera(u);
        if (!acesso.ehGestaoLocal(u) || u.getGestaoLocal() == null || !Boolean.TRUE.equals(u.getGestaoLocal().getAtivo()))
            throw erro(HttpStatus.FORBIDDEN, "Somente Gestor Local ativo incorpora prova histórica.");
        GestaoLocal atual = gestao.gestaoAtual(c);
        if (atual == null || !Objects.equals(atual.getId(), u.getGestaoLocal().getId()))
            throw erro(HttpStatus.NOT_FOUND, "Contrato não encontrado na sua Gestão responsável atual.");
        if (!permissoes.possuiPermissaoAtiva(u, TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA, c, atual))
            throw erro(HttpStatus.FORBIDDEN, "Incorporação exige concessão específica vigente do Contrato.");
    }
    public void registrarUso(UsuarioAplicacao u, Contrato c, String tipo, Long id, String motivo) {
        permissoes.exigirERegistrarUso(u, TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA,
                c, gestao.gestaoAtual(c), motivo, tipo, id);
    }
    public DocumentoComprobatorioDTO fonte(Contrato c, Long documentoId, String referencia, String finalidade, boolean lock) {
        if (documentoId == null) {
            texto(referencia, 1000, "Fonte SEI ou Documento Comprobatório é obrigatória.");
            return null;
        }
        DocumentoComprobatorioDTO d = documentos.buscar(c.getId(), documentoId, finalidade);
        DocumentoComprobatorio entidade = em.find(DocumentoComprobatorio.class, documentoId);
        if (lock) {
            em.refresh(entidade, LockModeType.PESSIMISTIC_READ);
            d = documentos.buscar(c.getId(), documentoId, finalidade);
        }
        if (d.estado() != EstadoDocumentoComprobatorio.PUBLICADO)
            throw erro(HttpStatus.CONFLICT, "A fonte documental deve estar publicada e válida.");
        // Os metadados preservam o checksum esperado, mas não comprovam que os
        // bytes ainda existem e conferem. Reusar a leitura documental autorizada
        // também na confirmação detecta perda/adulteração posterior à prévia.
        if (d.downloadDisponivel()) documentos.baixar(c.getId(), documentoId, finalidade);
        return d;
    }
    public void validar(LocalDate inicio, LocalDate fim, String motivo, String autorizacao, String chave) {
        if (inicio == null || inicio.isBefore(LocalDate.of(1000,1,1)) || inicio.isAfter(LocalDate.of(9999,12,31))
                || fim != null && (fim.isBefore(inicio) || fim.isAfter(LocalDate.of(9999,12,31))))
            throw erro(HttpStatus.BAD_REQUEST, "Informe intervalo efetivo válido e inclusivo.");
        texto(motivo, 1000, "Motivo é obrigatório."); texto(autorizacao, 1000, "Autorização é obrigatória.");
        texto(chave, 80, "Chave de idempotência é obrigatória.");
    }
    public static String texto(String valor, int limite, String mensagem) {
        if (valor == null || valor.isBlank() || valor.trim().length() > limite) throw erro(HttpStatus.BAD_REQUEST, mensagem);
        return valor.trim();
    }
    public static String normalizar(String valor) { return valor == null ? null : valor.trim(); }
    public static String hash(String dominio, Object... valores) {
        return AssinaturaPreviaNegocial.sha256(dominio, AssinaturaPreviaNegocial.partes(valores));
    }
    public void preencher(ProvaHistoricaBase p, Contrato c, LocalDate inicio, LocalDate fim, DocumentoComprobatorioDTO d,
            String referencia, String motivo, String autorizacao, String chave, String intencao, UsuarioAplicacao u) {
        p.setContrato(c); p.setDataInicioEfetiva(inicio); p.setDataFimEfetiva(fim);
        p.setDocumentoComprobatorio(d == null ? null : em.getReference(DocumentoComprobatorio.class, d.id()));
        p.setVersaoDocumento(d == null ? null : d.versaoDocumento());
        p.setHashDocumento(d == null ? hash("sgt.prova-historica.documento-sei.v1", referencia) : hash("sgt.prova-historica.documento.v1", d.id(),d.versaoDocumento(),d.sha256Arquivo(),d.referenciaSei()));
        p.setReferenciaSei(normalizar(referencia)); p.setMotivo(motivo.trim()); p.setAutorizacao(autorizacao.trim());
        p.setChaveIdempotencia(chave.trim()); p.setHashIntencao(intencao); p.setRegistradoEm(Instant.now());
        p.setResponsavelId(u.getId()); p.setResponsavelUsername(u.getUsername());
        p.setHashConteudo(hash("sgt.prova-historica.conteudo.v1", intencao,p.getHashDocumento(),p.getRegistradoEm()));
    }
    public static ResponseStatusException erro(HttpStatus status, String mensagem) { return new ResponseStatusException(status, mensagem); }
}
