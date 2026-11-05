package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;
import java.time.LocalDate;

/** Incorporação documental append-only. Origem e versões anteriores nunca são atualizadas. */
@Entity @Immutable
@Table(name="prova_historica_contrato", uniqueConstraints={
 @UniqueConstraint(name="uk_prova_contrato_intencao", columnNames={"id_contrato","chave_idempotencia"}),
 @UniqueConstraint(name="uk_prova_contrato_revisao", columnNames={"id_evento_origem","numero_revisao"})
}, indexes={@Index(name="idx_prova_contrato_conhecimento", columnList="id_contrato,registrado_em")})
public class ProvaHistoricaContrato {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_prova_historica_contrato")
 private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="id_contrato", nullable=false, updatable=false)
 private Contrato contrato;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="id_evento_origem", nullable=false, updatable=false)
 private EventoCicloVidaContrato eventoOrigem;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_prova_anterior", updatable=false, unique=true)
 private ProvaHistoricaContrato provaAnterior;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_versao_anterior", updatable=false)
 private VersaoContratoNegocial versaoAnterior;
 @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="id_versao_gerada", nullable=false, updatable=false, unique=true)
 private VersaoContratoNegocial versaoGerada;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_documento_fonte", updatable=false)
 private DocumentoComprobatorio documentoComprobatorio;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="id_permissao", nullable=false, updatable=false)
 private PermissaoExcepcional permissao;
 @Column(name="numero_revisao", nullable=false, updatable=false)
 private Integer numeroRevisao;
 @Column(name="registrado_em", nullable=false, updatable=false)
    @org.hibernate.annotations.FractionalSeconds(6)
 private Instant registradoEm;
 @Column(name="data_documento", nullable=false, updatable=false)
 private LocalDate dataDocumento;
 @Column(name="autor_id", nullable=false, updatable=false)
 private Long autorId;
 @Column(name="referencia_sei", length=300, updatable=false)
 private String referenciaSei;
 @Column(name="motivo", nullable=false, length=1500, updatable=false)
 private String motivo;
 @Column(name="autorizacao_administrativa", nullable=false, length=180, updatable=false)
 private String autorizacaoAdministrativa;
 @Column(name="chave_idempotencia", nullable=false, length=100, updatable=false)
 private String chaveIdempotencia;
 @Column(name="hash_conteudo", nullable=false, length=64, updatable=false)
 private String hashConteudo;
 @Column(name="hash_fonte_arquivo", length=64, updatable=false)
 private String hashFonteArquivo;
 @Column(name="versao_fonte_documento", updatable=false)
 private Long versaoFonteDocumento;
 @Column(name="campos_comprovados", nullable=false, length=1500, updatable=false)
 private String camposComprovados;
 @Column(name="configuracao_completa_comprovada", nullable=false, updatable=false)
 private boolean configuracaoCompletaComprovada;
 protected ProvaHistoricaContrato() { }
 public ProvaHistoricaContrato(Contrato contrato, EventoCicloVidaContrato eventoOrigem, ProvaHistoricaContrato provaAnterior, VersaoContratoNegocial versaoAnterior, VersaoContratoNegocial versaoGerada, DocumentoComprobatorio documentoComprobatorio, PermissaoExcepcional permissao, Integer numeroRevisao, Instant registradoEm, LocalDate dataDocumento, Long autorId, String referenciaSei, String motivo, String autorizacaoAdministrativa, String chaveIdempotencia, String hashConteudo, String hashFonteArquivo, Long versaoFonteDocumento, String camposComprovados, boolean configuracaoCompletaComprovada) {
  this.contrato=contrato;
  this.eventoOrigem=eventoOrigem;
  this.provaAnterior=provaAnterior;
  this.versaoAnterior=versaoAnterior;
  this.versaoGerada=versaoGerada;
  this.documentoComprobatorio=documentoComprobatorio;
  this.permissao=permissao;
  this.numeroRevisao=numeroRevisao;
  this.registradoEm=registradoEm;
  this.dataDocumento=dataDocumento;
  this.autorId=autorId;
  this.referenciaSei=referenciaSei;
  this.motivo=motivo;
  this.autorizacaoAdministrativa=autorizacaoAdministrativa;
  this.chaveIdempotencia=chaveIdempotencia;
  this.hashConteudo=hashConteudo;
  this.hashFonteArquivo=hashFonteArquivo;
  this.versaoFonteDocumento=versaoFonteDocumento;
  this.camposComprovados=camposComprovados;
  this.configuracaoCompletaComprovada=configuracaoCompletaComprovada;
 }
 public Long getId(){return id;}
 public Contrato getContrato(){return contrato;}
 public EventoCicloVidaContrato getEventoOrigem(){return eventoOrigem;}
 public ProvaHistoricaContrato getProvaAnterior(){return provaAnterior;}
 public VersaoContratoNegocial getVersaoAnterior(){return versaoAnterior;}
 public VersaoContratoNegocial getVersaoGerada(){return versaoGerada;}
 public DocumentoComprobatorio getDocumentoComprobatorio(){return documentoComprobatorio;}
 public PermissaoExcepcional getPermissao(){return permissao;}
 public Integer getNumeroRevisao(){return numeroRevisao;}
 public Instant getRegistradoEm(){return registradoEm;}
 public LocalDate getDataDocumento(){return dataDocumento;}
 public Long getAutorId(){return autorId;}
 public String getReferenciaSei(){return referenciaSei;}
 public String getMotivo(){return motivo;}
 public String getAutorizacaoAdministrativa(){return autorizacaoAdministrativa;}
 public String getChaveIdempotencia(){return chaveIdempotencia;}
 public String getHashConteudo(){return hashConteudo;}
 public String getHashFonteArquivo(){return hashFonteArquivo;}
 public Long getVersaoFonteDocumento(){return versaoFonteDocumento;}
 public String getCamposComprovados(){return camposComprovados;}
 public boolean getConfiguracaoCompletaComprovada(){return configuracaoCompletaComprovada;}
}
