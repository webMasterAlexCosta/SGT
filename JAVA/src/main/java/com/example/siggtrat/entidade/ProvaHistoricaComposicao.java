package com.example.siggtrat.entidade;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Check;

/** Complemento tipado de configuração, associado à identidade canônica preservada. */
@Entity @Immutable
@Check(constraints="(id_composicao is null) <> (id_mapeamento is null)")
@Table(name="prova_historica_composicao",uniqueConstraints={
 @UniqueConstraint(name="uk_prova_item_composicao",columnNames={"id_prova","id_composicao"}),
 @UniqueConstraint(name="uk_prova_item_mapeamento",columnNames={"id_prova","id_mapeamento"})})
public class ProvaHistoricaComposicao {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_prova_historica_composicao")
 private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="id_prova", nullable=false, updatable=false)
 private ProvaHistoricaContrato prova;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_composicao", updatable=false)
 private ComposicaoContratual composicao;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_mapeamento", updatable=false)
 private MapeamentoTransferenciaGestao mapeamento;
 @Column(name="lotacao_nome", length=150, updatable=false)
 private String lotacaoNome;
 @Column(name="jornada_descricao", length=150, updatable=false)
 private String jornadaDescricao;
 @Column(name="cargo_nome", length=150, updatable=false)
 private String cargoNome;
 @Column(name="lotacao_municipio", length=180, updatable=false)
 private String lotacaoMunicipio;
 @Column(name="municipio_comprovado", nullable=false, updatable=false)
 private boolean municipioComprovado;
 @Column(name="jornada_semanal", length=1000, updatable=false)
 private String jornadaSemanal;
 @Column(name="jornada_comprovada", nullable=false, updatable=false)
 private boolean jornadaComprovada;
 protected ProvaHistoricaComposicao() { }
 public ProvaHistoricaComposicao(ProvaHistoricaContrato prova, ComposicaoContratual composicao, MapeamentoTransferenciaGestao mapeamento, String lotacaoNome, String jornadaDescricao, String cargoNome, String lotacaoMunicipio, boolean municipioComprovado, String jornadaSemanal, boolean jornadaComprovada) {
  this.prova=prova;
  this.composicao=composicao;
  this.mapeamento=mapeamento;
  this.lotacaoNome=lotacaoNome;
  this.jornadaDescricao=jornadaDescricao;
  this.cargoNome=cargoNome;
  this.lotacaoMunicipio=lotacaoMunicipio;
  this.municipioComprovado=municipioComprovado;
  this.jornadaSemanal=jornadaSemanal;
  this.jornadaComprovada=jornadaComprovada;
 }
 public Long getId(){return id;}
 public ProvaHistoricaContrato getProva(){return prova;}
 public ComposicaoContratual getComposicao(){return composicao;}
 public MapeamentoTransferenciaGestao getMapeamento(){return mapeamento;}
 public String getLotacaoNome(){return lotacaoNome;}
 public String getJornadaDescricao(){return jornadaDescricao;}
 public String getCargoNome(){return cargoNome;}
 public String getLotacaoMunicipio(){return lotacaoMunicipio;}
 public boolean getMunicipioComprovado(){return municipioComprovado;}
 public String getJornadaSemanal(){return jornadaSemanal;}
 public boolean getJornadaComprovada(){return jornadaComprovada;}
}
