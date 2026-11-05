package com.example.siggtrat.constante;

/**
 * Vocabulário fixo do domínio.
 *
 * Estes valores não são configuração de infraestrutura. Eles participam de
 * consultas, validações, permissões e estados persistidos no banco; por isso
 * permanecem constantes Java e não podem ser alterados por ambiente.
 */
public final class ConstantesDominio {

    private ConstantesDominio() {
    }

    public static final String TIMEZONE_PADRAO = "America/Sao_Paulo";

    public static final String PERFIL_ADMINISTRADOR = "PERFIL_ADMINISTRADOR";
    public static final String PERFIL_GESTOR_NACIONAL = "PERFIL_GESTOR_NACIONAL";
    public static final String PERFIL_GESTOR_ESTADUAL = "PERFIL_GESTOR_ESTADUAL";
    public static final String PERFIL_GESTOR_LOCAL = "PERFIL_GESTOR_LOCAL";
    public static final String PERFIL_PRESIDENTE = "PERFIL_PRESIDENTE";
    public static final String PERFIL_OPERACIONAL = "PERFIL_OPERACIONAL";

    public static final java.util.Set<String> PERFIS_ORGANIZACIONAIS = java.util.Set.of(
            PERFIL_GESTOR_NACIONAL, PERFIL_GESTOR_ESTADUAL,
            PERFIL_GESTOR_LOCAL, PERFIL_PRESIDENTE
    );

    /** Unidades federativas atendidas pela estrutura nacional. */
    public static final java.util.List<String> UFS_BRASIL = java.util.List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
            "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    public static final String COOKIE_AUTH = "autenticacao";
    public static final String COOKIE_DEVICE = "id_dispositivo";
    public static final String DUMMY_HASH = "$2a$10$DUMMYHASHFORSECURITYPURPOSESONLY0000000000";

    public static final String STATUS_ATIVO = "ATIVO";
    public static final String STATUS_INATIVO = "INATIVO";
    public static final String STATUS_FERIAS = "FERIAS";
    public static final String STATUS_AFASTADO_INSS = "AFASTADO_INSS";

    public static final String FREQ_PRESENCA = "PRESENCA";
    public static final String FREQ_PRESENCA_PARCIAL = "PRESENCA_PARCIAL";
    public static final String FREQ_FALTA = "FALTA";
    public static final String FREQ_ATRASO = "ATRASO";
    public static final String FREQ_ATESTADO = "ATESTADO";
    public static final String FREQ_SUSPENSAO = "SUSPENSAO";
    public static final String FREQ_DESCANSO = "DESCANSO";
    public static final String FREQ_DSR_PERDIDO = "DSR_PERDIDO";
    public static final String FREQ_FOLGA = "FOLGA";
    public static final String FREQ_RT = "RT";
    public static final String FREQ_TR = "TR";

    /** Valores aceitos no campo frequencia.status. Mantido fechado no domínio e no banco. */
    public static final java.util.Set<String> FREQUENCIA_STATUS_PERMITIDOS = java.util.Set.of(
            FREQ_PRESENCA, FREQ_PRESENCA_PARCIAL, FREQ_FALTA, FREQ_ATRASO,
            FREQ_ATESTADO, FREQ_SUSPENSAO, FREQ_DESCANSO, FREQ_DSR_PERDIDO,
            FREQ_FOLGA, FREQ_RT, FREQ_TR
    );

    public static final String OCORRENCIA_LICENCA_MEDICA = "LICENÇA MÉDICA";

    public static final String STATUS_CALCULADO = "CALCULADO";
    public static final String STATUS_APROVADO = "APROVADO";
    public static final String STATUS_CANCELADO = "CANCELADO";

    public static final String EVENTO_FERIADO = "FERIADO";
    public static final String EVENTO_DIA_DESCANSO = "DIA_DESCANSO";
    public static final String EXCECAO_FOLGA_REMUNERADA = "FOLGA_REMUNERADA";

    public static final String USUARIO_SISTEMA = "SISTEMA";
    public static final String PREFIX_CONTRATO_TEMP = "TEMP-";
    public static final String PREFIX_HASH_FREQ = "FREQ_";

    public static final String AUDIT_CREATE = "CREATE";
    public static final String AUDIT_UPDATE = "UPDATE";
    public static final String AUDIT_DELETE = "DELETE";
    public static final String AUDIT_TIPO_SIMPLE = "SIMPLE";
    public static final String AUDIT_TIPO_REFERENCE = "REFERENCE";
    public static final String AUDIT_TIPO_ENTITY = "ENTITY";
    public static final String AUDIT_TIPO_BOOLEAN = "BOOLEAN";
    public static final String LABEL_ATIVO = "Ativo";
}
