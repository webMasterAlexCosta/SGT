package com.example.siggtrat.entidade;

import com.example.siggtrat.regra.IntencaoAtoTransferencia;
import jakarta.persistence.*;
import java.util.Objects;

/** Declaração humana restrita ao ato registrado. Nenhum cadastro de autorização reutilizável. */
@Embeddable
public class ConferenciaDocumentalTransferencia {
    public static final String VERSAO_DECLARACAO = "transferencia-gestao-conferencia-v1";
    public static final String DECLARACAO = "Declaro que conferi as referências informadas e que a autorização administrativa do ato abrange esta operação, suas datas, as Gestões de origem e destino, o Tipo de Contratação, todos os mapeamentos e o ato anterior indicado, quando houver. Esta declaração registra minha conferência e não comprova leitura, autenticidade ou validação jurídica pelo SGT.";

    @Column(name = "conferencia_transferencia_versao", length = 64, updatable = false)
    private String versaoDeclaracao;
    @Column(name = "conferencia_transferencia_texto", length = 1200, updatable = false)
    private String declaracao;
    @Column(name = "intencao_transferencia_versao", length = 64, updatable = false)
    private String versaoIntencao;
    @Column(name = "intencao_transferencia_canonica", columnDefinition = "text", updatable = false)
    private String intencaoCanonica;
    @Column(name = "intencao_transferencia_hash", length = 64, updatable = false)
    private String hashIntencao;
    @Column(name = "autorizacao_administrativa_ato", length = 500, updatable = false)
    private String autorizacaoAdministrativaAto;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_uso_permissao_ato_transferencia", unique = true, updatable = false)
    private UsoPermissaoExcepcional usoPermissaoExcepcional;

    protected ConferenciaDocumentalTransferencia() { }
    public ConferenciaDocumentalTransferencia(IntencaoAtoTransferencia.Material intencao,
            String autorizacaoAdministrativaAto, UsoPermissaoExcepcional uso) {
        Objects.requireNonNull(intencao, "intenção do ato");
        this.versaoDeclaracao = VERSAO_DECLARACAO;
        this.declaracao = DECLARACAO;
        this.versaoIntencao = intencao.versao();
        this.intencaoCanonica = intencao.canonico();
        this.hashIntencao = intencao.hash();
        this.autorizacaoAdministrativaAto = Objects.requireNonNull(autorizacaoAdministrativaAto, "referência própria do ato");
        this.usoPermissaoExcepcional = Objects.requireNonNull(uso, "recibo exato da concessão");
    }
    public String getVersaoDeclaracao() { return versaoDeclaracao; }
    public String getDeclaracao() { return declaracao; }
    public String getVersaoIntencao() { return versaoIntencao; }
    public String getIntencaoCanonica() { return intencaoCanonica; }
    public String getHashIntencao() { return hashIntencao; }
    public String getAutorizacaoAdministrativaAto() { return autorizacaoAdministrativaAto; }
    public UsoPermissaoExcepcional getUsoPermissaoExcepcional() { return usoPermissaoExcepcional; }
}
