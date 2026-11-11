package com.example.sgtfixture.ind08;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapeamento exclusivo do controle negativo IND-08, fora do pacote escaneado
 * pela aplicação. Permite simular/restaurar uma revisão danificada por JPA,
 * sem mudar a entidade de produção ou sua política de imutabilidade.
 */
@Entity(name = "ProvaHistoricaContratoFalhaFixture")
@Table(name = "prova_historica_contrato")
public class ProvaHistoricaContratoFalhaFixture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prova_historica_contrato")
    private Long id;

    @Column(name = "id_contrato", nullable = false)
    private Long contratoId;

    @Column(name = "id_evento_origem", nullable = false)
    private Long eventoOrigemId;

    @Column(name = "numero_revisao", nullable = false)
    private Integer numeroRevisao;

    @Column(name = "id_prova_anterior", unique = true)
    private Long provaAnteriorId;

    protected ProvaHistoricaContratoFalhaFixture() { }
}
