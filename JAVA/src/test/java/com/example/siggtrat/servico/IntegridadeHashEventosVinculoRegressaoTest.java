package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo;
import com.example.siggtrat.dominio.alocacao.TipoRelacaoEventoAlocacao;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.dominio.vinculo.TipoRelacaoEventoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoAlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regra pura: repositórios fornecem listas em memória; motores e entidades são
 * reais. Os digests literais foram calculados separadamente em SHA-256/UTF-8,
 * a partir dos materiais históricos dos geradores C25. Não é prova JPA/MySQL.
 */
class IntegridadeHashEventosVinculoRegressaoTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 6, 30);
    private static final String CADASTRO = "Cadastro integrado de Pessoa, Vínculo e Alocação inicial";
    private static final String V_ORIGEM = "48b1c1c35649535a84cbf4f007178be3133e18a9c6cfdc548f234da9014f376f";
    private static final String V_MATRICULA = "90e041905a2bdf1e15424345c738b64d58ed25245f1d53a5ba89950eb361ea7d";
    private static final String V_EXCEDENTE = "d279887a01b002e653ec13b2c89a3de2735de7ae1b0f6356ad63956bd21b2f63";
    private static final String V_RETIFICACAO = "0391b3ad0b42ca62574661fba6faa46ccc80fbfbc7de00f96cd98e55f2dcde68";
    private static final String A_ORIGEM = "a853308d27183ca5ca4dc1d254ec83c2ef3e7f0ef5d9c629040c73106c44feef";
    private static final String A_TRANSFERENCIA = "1316c8231ce0f78eba7df25c8eb9635c48707cb045d8bcccffda770385ce308e";
    private static final String A_EXCEDENTE = "7f9fb9f49721a291dfb7473ec909ee07c6a3b3424baad6c32d7864cc1781fca2";

    @Test
    void vinculoPreservaCadeiaValidaMesmoAposCorrecaoDoCpfCadastral() {
        EventoVinculoContratual a = origemVinculo(), b = matricula(a);
        a.getVinculo().getPessoa().setCpf("11144477735");
        assertTrue(integraVinculo(a, b), "O hash usa o CPF fotografado, não o cadastro atual.");
    }

    @Test
    void vinculoRecusaJustificativaAlteradaSemRecalcularHash() {
        EventoVinculoContratual a = origemVinculo();
        a.setJustificativa("Justificativa modificada sem novo digest");
        assertFalse(integraVinculo(a));
    }

    @Test
    void vinculoRecusaHashHexadecimalArbitrario() {
        EventoVinculoContratual a = origemVinculo(); a.setHashEvento("a".repeat(64));
        assertFalse(integraVinculo(a));
    }

    @Test
    void vinculoRecusaHashNaoHexadecimal() {
        EventoVinculoContratual a = origemVinculo(); a.setHashEvento("Z".repeat(64));
        assertFalse(integraVinculo(a));
    }

    @Test
    void vinculoContinuaRecusandoEncadeamentoQuebrado() {
        EventoVinculoContratual a = origemVinculo(), b = matricula(a);
        b.setHashAnterior("b".repeat(64));
        // Digest válido para o conteúdo adulterado: a recusa exige conferir o predecessor real.
        b.setHashEvento("81e3c84acd643bc3568c05b092b69615a7c270ac6863d15cc10bd0bb50b74164");
        assertFalse(integraVinculo(a, b));
    }

    @Test
    void vinculoPreservaHashDeCriacaoExcedenteComJustificativaBaseOriginal() {
        assertTrue(integraVinculo(excedenteVinculo()));
    }

    @Test
    void vinculoRecusaJustificativaAlteradaNaCamadaDeExcedente() {
        EventoVinculoContratual a = excedenteVinculo();
        a.setJustificativa("Excedente sem a justificativa originalmente documentada");
        assertFalse(integraVinculo(a));
    }

    @Test
    void vinculoPreservaHashDeRetificacaoExcedenteSemConfundirComCriacao() {
        EventoVinculoContratual a = origemVinculo(), b = matricula(a);
        b.setTipoEvento(TipoEventoVinculoContratual.RETIFICACAO_INICIO);
        b.setDataEfetiva(LocalDate.of(2026, 5, 28)); b.setInicioNovo(b.getDataEfetiva());
        b.setMatriculaNova("MAT-001"); b.setJustificativa("Antecipação autorizada");
        preencherExcedente(b); b.setComposicaoExcedenteId(9L); b.setHashEvento(V_RETIFICACAO);
        assertTrue(integraVinculo(a, b));
    }

    @Test
    void vinculoRecusaMudancaDoMotivoFotografado() {
        EventoVinculoContratual a = origemVinculo(), b = matricula(a);
        b.setTipoEvento(TipoEventoVinculoContratual.DESLIGAMENTO);
        b.setDataEfetiva(LocalDate.of(2026, 6, 20)); b.setFimNovo(b.getDataEfetiva());
        b.setMatriculaNova("MAT-001"); b.setJustificativa("Término documentado");
        b.setMotivoDesligamentoIdFotografia(71L);
        b.setMotivoDesligamentoDescricaoFotografia("Desligamento sintético");
        b.setMotivoDesligamentoCategoriaFotografia(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        b.setHashEvento("11c79605b6b01e8aef0a3bf1f37d74b1f41d254f2aaa87c3be5e5907fbb3b34e");
        assertTrue(integraVinculo(a, b));
        b.setMotivoDesligamentoDescricaoFotografia("Descrição adulterada");
        assertFalse(integraVinculo(a, b));
    }

    @Test
    void alocacaoPreservaCadeiaDeTransferenciaComFotografiasHistoricas() throws Exception {
        EventoAlocacaoVinculo a = origemAlocacao(), b = transferencia(a);
        a.getAlocacaoNova().setDataInicio(INICIO.minusMonths(1));
        a.getAlocacaoNova().setDataFim(FIM.plusMonths(1));
        assertTrue(integraAlocacao(a, b), "Datas da projeção atual não reescrevem a fotografia do evento.");
    }

    @Test
    void alocacaoRecusaJustificativaAlteradaSemRecalcularHash() throws Exception {
        EventoAlocacaoVinculo a = origemAlocacao(); a.setJustificativa("Conteúdo adulterado");
        assertFalse(integraAlocacao(a));
    }

    @Test
    void alocacaoRecusaHashHexadecimalArbitrario() throws Exception {
        EventoAlocacaoVinculo a = origemAlocacao(); a.setHashEvento("a".repeat(64));
        assertFalse(integraAlocacao(a));
    }

    @Test
    void alocacaoRecusaHashNaoHexadecimal() throws Exception {
        EventoAlocacaoVinculo a = origemAlocacao(); a.setHashEvento("Z".repeat(64));
        assertFalse(integraAlocacao(a));
    }

    @Test
    void alocacaoContinuaRecusandoEncadeamentoQuebrado() throws Exception {
        EventoAlocacaoVinculo a = origemAlocacao(), b = transferencia(a);
        b.setHashAnterior("b".repeat(64));
        b.setHashEvento("e0696f3634c42fd31b4be8722dacc439bc49a20c62b1e4eb37bb40c118b9630e");
        assertFalse(integraAlocacao(a, b));
    }

    @Test
    void alocacaoPreservaHashDaCamadaExcedente() throws Exception {
        assertTrue(integraAlocacao(excedenteAlocacao()));
    }

    @Test
    void alocacaoRecusaEvidenciaAlteradaNaCamadaExcedente() throws Exception {
        EventoAlocacaoVinculo a = excedenteAlocacao(); a.setEvidencia("SEI-DIVERGENTE");
        assertFalse(integraAlocacao(a));
    }

    @Test
    void vetoresDeVinculoCorrespondemAoGeradorC25SemAlterarOsBytesHistoricos() throws Exception {
        EventoVinculoContratualRepositorio repo = repositorio(EventoVinculoContratualRepositorio.class, List.of());
        VinculoContratualServico gerador = new VinculoContratualServico(null, null, null, null, null, null,
                repo, null, null, null, null);
        Method montar = VinculoContratualServico.class.getDeclaredMethod("montarEvento", VinculoContratual.class,
                TipoEventoVinculoContratual.class, LocalDate.class, UsuarioAplicacao.class,
                String.class, String.class, String.class, LocalDate.class, LocalDate.class,
                LocalDate.class, LocalDate.class, String.class, String.class, TipoRelacaoEventoVinculo.class);
        montar.setAccessible(true);
        UsuarioAplicacao usuario = new UsuarioAplicacao(); usuario.setId(42L);
        EventoVinculoContratual evento = (EventoVinculoContratual) montar.invoke(gerador, vinculo(),
                TipoEventoVinculoContratual.CRIACAO, INICIO, usuario, CADASTRO, null, null,
                null, INICIO, null, FIM, null, "MAT-001", TipoRelacaoEventoVinculo.ORIGEM);
        assertEquals(V_ORIGEM, evento.getHashEvento());
        assertTrue(integraVinculo(evento));

        Lotacao lotacao = new Lotacao(); lotacao.setId(10L);
        Jornada jornada = new Jornada(); jornada.setId(11L);
        Cargo cargo = new Cargo(); cargo.setId(12L);
        var combinacao = new AlocacaoVinculoServico.CombinacaoResolvida(null, lotacao, jornada, cargo, 1);
        var capacidade = new AlocacaoVinculoServico.CapacidadeInclusao(1, 1, 2, 1);
        Method completar = VinculoContratualServico.class.getDeclaredMethod("completarEventoExcedente",
                EventoVinculoContratual.class, AlocacaoVinculoServico.CombinacaoResolvida.class,
                AlocacaoVinculoServico.CapacidadeInclusao.class, String.class, String.class, String.class);
        completar.setAccessible(true);
        completar.invoke(gerador, evento, combinacao, capacidade, "Excedente autorizado", "SEI-TESTE", "ATO-TESTE");
        assertEquals(V_EXCEDENTE, evento.getHashEvento());
        assertTrue(integraVinculo(evento));
    }

    @Test
    void vetoresDeAlocacaoCorrespondemAoGeradorC25SemAlterarOsBytesHistoricos() throws Exception {
        EventoAlocacaoVinculoRepositorio repo = repositorio(EventoAlocacaoVinculoRepositorio.class, List.of());
        AlocacaoVinculoServico gerador = new AlocacaoVinculoServico(null, repo, null, null, null, null, null, null);
        Class<?> fotoTipo = Class.forName(AlocacaoVinculoServico.class.getName() + "$FotoAlocacao");
        Constructor<?> construtor = fotoTipo.getDeclaredConstructors()[0]; construtor.setAccessible(true);
        Object foto = construtor.newInstance(21L, 9L, 10L, "Lotação histórica", 11L, "Jornada histórica",
                12L, "Cargo histórico", INICIO, FIM, EstadoNegocialAlocacao.REGISTRADA);
        Method registrar = AlocacaoVinculoServico.class.getDeclaredMethod("registrarEvento", VinculoContratual.class,
                AlocacaoVinculo.class, TipoEventoAlocacaoVinculo.class, TipoRelacaoEventoAlocacao.class,
                LocalDate.class, UsuarioAplicacao.class, String.class, String.class, String.class,
                fotoTipo, fotoTipo, TransferenciaGestaoContrato.class, AlocacaoVinculoServico.CapacidadeInclusao.class);
        registrar.setAccessible(true);
        UsuarioAplicacao usuario = new UsuarioAplicacao(); usuario.setId(42L);
        AlocacaoVinculo nova = alocacao(21L);
        EventoAlocacaoVinculo evento = (EventoAlocacaoVinculo) registrar.invoke(gerador, vinculo(), nova,
                TipoEventoAlocacaoVinculo.CRIACAO, TipoRelacaoEventoAlocacao.ORIGEM, INICIO, usuario,
                CADASTRO, null, null, null, foto, null, null);
        assertEquals(A_ORIGEM, evento.getHashEvento());
        assertTrue(integraAlocacao(evento));

        var capacidade = new AlocacaoVinculoServico.CapacidadeInclusao(1, 1, 2, 1);
        EventoAlocacaoVinculo excedente = (EventoAlocacaoVinculo) registrar.invoke(gerador, vinculo(), nova,
                TipoEventoAlocacaoVinculo.CRIACAO, TipoRelacaoEventoAlocacao.ORIGEM, INICIO, usuario,
                "Excedente autorizado", "SEI-TESTE", "ATO-TESTE", null, foto, null, capacidade);
        assertEquals(A_EXCEDENTE, excedente.getHashEvento());
        assertTrue(integraAlocacao(excedente));
    }

    private EventoVinculoContratual origemVinculo() {
        EventoVinculoContratual e = new EventoVinculoContratual(); e.setVinculo(vinculo());
        e.setTipoEvento(TipoEventoVinculoContratual.CRIACAO); e.setDataEfetiva(INICIO);
        e.setInicioNovo(INICIO); e.setFimNovo(FIM); e.setMatriculaNova("MAT-001");
        e.setPessoaCpfFotografia("52998224725"); e.setResponsavelId(42L); e.setJustificativa(CADASTRO);
        e.setHashEvento(V_ORIGEM); return e;
    }

    private EventoVinculoContratual matricula(EventoVinculoContratual anterior) {
        EventoVinculoContratual e = origemVinculo(); e.setVinculo(anterior.getVinculo());
        e.setTipoEvento(TipoEventoVinculoContratual.ALTERACAO_MATRICULA);
        e.setDataEfetiva(LocalDate.of(2026, 6, 15)); e.setInicioAnterior(INICIO); e.setFimAnterior(FIM);
        e.setMatriculaAnterior("MAT-001"); e.setMatriculaNova("MAT-002"); e.setJustificativa("Matrícula retificada");
        e.setHashAnterior(anterior.getHashEvento()); e.setHashEvento(V_MATRICULA); return e;
    }

    private EventoVinculoContratual excedenteVinculo() {
        EventoVinculoContratual e = origemVinculo(); preencherExcedente(e);
        e.setJustificativa("Excedente autorizado"); e.setHashEvento(V_EXCEDENTE); return e;
    }

    private void preencherExcedente(EventoVinculoContratual e) {
        e.setInclusaoExcedente(true); e.setLotacaoExcedenteId(10L); e.setJornadaExcedenteId(11L);
        e.setCargoExcedenteId(12L); e.setEfetivoPrevistoExcedente(1); e.setOcupacaoAposInclusaoExcedente(2L);
        e.setEvidencia("SEI-TESTE"); e.setAutorizacao("ATO-TESTE");
    }

    private EventoAlocacaoVinculo origemAlocacao() throws Exception {
        EventoAlocacaoVinculo e = new EventoAlocacaoVinculo(); e.setVinculo(vinculo());
        e.setAlocacaoNova(alocacao(21L)); e.setTipoEvento(TipoEventoAlocacaoVinculo.CRIACAO);
        e.setDataEfetiva(INICIO); e.setComposicaoNovaId(9L); e.setLotacaoNovaId(10L);
        e.setJornadaNovaId(11L); e.setCargoNovoId(12L); e.setInicioNovo(INICIO); e.setFimNovo(FIM);
        e.setEstadoNovo(EstadoNegocialAlocacao.REGISTRADA); e.setResponsavelId(42L);
        e.setJustificativa(CADASTRO); e.setHashEvento(A_ORIGEM); return e;
    }

    private EventoAlocacaoVinculo transferencia(EventoAlocacaoVinculo anterior) throws Exception {
        EventoAlocacaoVinculo e = origemAlocacao(); e.setAlocacaoNova(alocacao(22L));
        e.setTipoEvento(TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO);
        e.setDataEfetiva(LocalDate.of(2026, 6, 15)); e.setAlocacaoAnteriorId(21L);
        e.setComposicaoAnteriorId(9L); e.setLotacaoAnteriorId(10L); e.setJornadaAnteriorId(11L);
        e.setCargoAnteriorId(12L); e.setInicioAnterior(INICIO); e.setFimAnterior(FIM);
        e.setEstadoAnterior(EstadoNegocialAlocacao.REGISTRADA); e.setComposicaoNovaId(null);
        e.setLotacaoNovaId(110L); e.setJornadaNovaId(111L); e.setCargoNovoId(112L);
        e.setInicioNovo(e.getDataEfetiva()); e.setTransferenciaGestaoId(77L);
        e.setJustificativa("Transferência documentada"); e.setHashAnterior(anterior.getHashEvento());
        e.setHashEvento(A_TRANSFERENCIA); return e;
    }

    private EventoAlocacaoVinculo excedenteAlocacao() throws Exception {
        EventoAlocacaoVinculo e = origemAlocacao(); e.setJustificativa("Excedente autorizado");
        e.setInclusaoExcedente(true); e.setEfetivoPrevistoExcedente(1); e.setOcupacaoAposInclusaoExcedente(2L);
        e.setEvidencia("SEI-TESTE"); e.setAutorizacao("ATO-TESTE"); e.setHashEvento(A_EXCEDENTE); return e;
    }

    private VinculoContratual vinculo() {
        Pessoa pessoa = new Pessoa(); pessoa.setCpf("52998224725");
        Contrato contrato = new Contrato(); contrato.setId(2L);
        VinculoContratual v = new VinculoContratual(); v.setId(7L); v.setPessoa(pessoa); v.setContrato(contrato);
        return v;
    }

    private AlocacaoVinculo alocacao(Long id) throws Exception {
        AlocacaoVinculo a = new AlocacaoVinculo();
        var campo = AlocacaoVinculo.class.getDeclaredField("id"); campo.setAccessible(true); campo.set(a, id);
        return a;
    }

    private boolean integraVinculo(EventoVinculoContratual... eventos) {
        return new MotorTemporalVinculoServico(repositorio(EventoVinculoContratualRepositorio.class, List.of(eventos)))
                .cadeiaHashIntegra(7L);
    }

    private boolean integraAlocacao(EventoAlocacaoVinculo... eventos) {
        return new MotorTemporalAlocacaoServico(repositorio(EventoAlocacaoVinculoRepositorio.class, List.of(eventos)), null)
                .cadeiaHashIntegra(7L);
    }

    private <T> T repositorio(Class<T> tipo, List<?> eventos) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (proxy, method, args) -> {
            if (method.getName().equals("findByVinculoIdOrderByRegistradoEmAscIdAsc")) {
                if (!Long.valueOf(7L).equals(args[0])) throw new AssertionError("Vínculo inesperado");
                return eventos;
            }
            if (method.getName().equals("findTopByVinculoIdOrderByRegistradoEmDescIdDesc")) return Optional.empty();
            if (method.getName().equals("save")) return args[0];
            throw new AssertionError("O teste puro não autoriza esta operação de repositório: " + method.getName());
        }));
    }
}
