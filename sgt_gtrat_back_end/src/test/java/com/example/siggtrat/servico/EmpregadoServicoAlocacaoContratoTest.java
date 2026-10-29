package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.configuracao.propriedades.PropriedadesEmpregado;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpregadoServicoAlocacaoContratoTest {

    @Mock EmpregadoRepositorio empregadoRepositorio;
    @Mock EmpregadoListaConsultaRepositorio empregadoListaConsultaRepositorio;
    @Mock EmpregadoFrequenciaConsultaRepositorio empregadoFrequenciaConsultaRepositorio;
    @Mock HistoricoEmpregadoRepositorio historicoEmpregadoRepositorio;
    @Mock HistoricoVinculoContratoRepositorio historicoVinculoRepositorio;
    @Mock UsuarioAplicacaoRepositorio usuarioAplicacaoRepositorio;
    @Mock StatusEmpregadoRepositorio statusEmpregadoRepositorio;
    @Mock LotacaoRepositorio lotacaoRepositorio;
    @Mock CargoRepositorio cargoRepositorio;
    @Mock ContaRepositorio contaRepositorio;
    @Mock AgenciaRepositorio agenciaRepositorio;
    @Mock TipoContaRepositorio tipoContaRepositorio;
    @Mock EnderecoRepositorio enderecoRepositorio;
    @Mock ContratoRepositorio contratoRepositorio;
    @Mock JornadaRepositorio jornadaRepositorio;
    @Mock RegistroAuditoriaServico registroAuditoriaServico;
    @Mock EscopoAcessoUsuarioServico escopoAcessoUsuarioServico;
    @Mock AcessoOrganizacionalServico acessoOrganizacionalServico;
    @Mock HistoricoIntegridadeServico historicoIntegridadeServico;
    @Mock ConsultaCacheRedisServico consultaCacheRedisServico;
    @Mock ServicoChaveCacheRedis servicoChaveCacheRedis;
    @Mock PropriedadesEmpregado propriedadesEmpregado;

    @InjectMocks EmpregadoServico servico;

    @Test
    void trocarLotacaoETurnoMantendoContratoNaoEncerraNemRecriaVinculo() throws Exception {
        Contrato contrato = contrato(10L);
        Empregado empregado = empregado(1L, contrato, 22L, 33L, 44L);

        HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
        historico.setId(100L);
        historico.setEmpregado(empregado);
        historico.setContrato(contrato);
        historico.setDataInicio(ZonedDateTime.parse("2026-01-10T10:00:00-03:00"));
        historico.setLotacao(lotacao(2L));
        historico.setJornada(jornada(3L));
        historico.setCargo(cargo(4L));

        when(historicoVinculoRepositorio.findByEmpregadoIdAndDataFimIsNullOrderByDataInicioDesc(1L))
                .thenReturn(List.of(historico));

        invocarGerenciamento(empregado, List.of(contrato), "Atualização Cadastral");

        assertNull(historico.getDataFim(), "o vínculo com o mesmo contrato deve continuar ativo");
        assertEquals(22L, historico.getLotacao().getId());
        assertEquals(33L, historico.getJornada().getId());
        assertEquals(44L, historico.getCargo().getId());
        assertEquals(ZonedDateTime.parse("2026-01-10T10:00:00-03:00"), historico.getDataInicio());
        verify(historicoVinculoRepositorio).save(historico);
        verify(historicoVinculoRepositorio, never()).saveAll(any());
        verify(historicoVinculoRepositorio, never()).flush();
    }

    @Test
    void trocarContratoFechaEFlushVinculoAnteriorAntesDeCriarNovo() throws Exception {
        Contrato anterior = contrato(10L);
        Contrato novo = contrato(20L);
        Empregado empregado = empregado(1L, novo, 22L, 33L, 44L);

        HistoricoVinculoContrato historicoAnterior = new HistoricoVinculoContrato();
        historicoAnterior.setId(100L);
        historicoAnterior.setEmpregado(empregado);
        historicoAnterior.setContrato(anterior);
        historicoAnterior.setDataInicio(ZonedDateTime.parse("2026-01-10T10:00:00-03:00"));

        when(historicoVinculoRepositorio.findByEmpregadoIdAndDataFimIsNullOrderByDataInicioDesc(1L))
                .thenReturn(List.of(historicoAnterior));

        invocarGerenciamento(empregado, List.of(novo), "Troca contratual");

        assertNotNull(historicoAnterior.getDataFim());
        InOrder ordem = inOrder(historicoVinculoRepositorio);
        ordem.verify(historicoVinculoRepositorio).saveAll(any());
        ordem.verify(historicoVinculoRepositorio).flush();
        ArgumentCaptor<HistoricoVinculoContrato> captor = ArgumentCaptor.forClass(HistoricoVinculoContrato.class);
        ordem.verify(historicoVinculoRepositorio).save(captor.capture());
        assertEquals(20L, captor.getValue().getContrato().getId());
        assertNull(captor.getValue().getDataFim());
    }

    private void invocarGerenciamento(Empregado empregado, List<Contrato> contratos, String observacao) throws Exception {
        Method metodo = EmpregadoServico.class.getDeclaredMethod(
                "gerenciarHistoricoContratos", Empregado.class, List.class, String.class);
        metodo.setAccessible(true);
        metodo.invoke(servico, empregado, contratos, observacao);
    }

    private static Empregado empregado(Long id, Contrato contrato, Long lotacaoId, Long jornadaId, Long cargoId) {
        Empregado empregado = new Empregado();
        empregado.setId(id);
        empregado.setNome("Empregado Teste");
        empregado.setLotacao(lotacao(lotacaoId));
        empregado.setJornada(jornada(jornadaId));
        empregado.setCargo(cargo(cargoId));
        empregado.setContratos(List.of(contrato));
        return empregado;
    }

    private static Contrato contrato(Long id) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setNumeroContrato("CTR-" + id);
        return contrato;
    }

    private static Lotacao lotacao(Long id) {
        Lotacao lotacao = new Lotacao();
        lotacao.setId(id);
        lotacao.setNome("Lotação " + id);
        return lotacao;
    }

    private static Jornada jornada(Long id) {
        Jornada jornada = new Jornada();
        jornada.setId(id);
        jornada.setDescricao("Turno " + id);
        return jornada;
    }

    private static Cargo cargo(Long id) {
        Cargo cargo = new Cargo();
        cargo.setId(id);
        cargo.setNome("Cargo " + id);
        return cargo;
    }
}
