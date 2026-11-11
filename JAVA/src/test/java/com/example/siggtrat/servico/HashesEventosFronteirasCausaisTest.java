package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo;
import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.repositorio.EventoAlocacaoVinculoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Causal: motores públicos reais; somente a fronteira de repositório fornece a fixture em memória. */
class HashesEventosFronteirasCausaisTest {

    @Test
    void motorDeVinculoRecusaMudancaCompensatoriaEntreEvidenciaEAutorizacao() {
        EventoVinculoContratual e = vinculo();
        e.setInclusaoExcedente(true); e.setJustificativa("Excedente autorizado");
        e.setLotacaoExcedenteId(10L); e.setJornadaExcedenteId(11L); e.setCargoExcedenteId(12L);
        e.setEfetivoPrevistoExcedente(1); e.setOcupacaoAposInclusaoExcedente(2L);
        e.setEvidencia("SEI-X|ATO-Y"); e.setAutorizacao("ATO-Z");
        var motor = new MotorTemporalVinculoServico(repo(EventoVinculoContratualRepositorio.class, e));
        e.setHashEvento(seloDoControle(motor, e));
        assertTrue(motor.cadeiaHashIntegra(7L), "Controle válido selado antes da mutação; não é oráculo de canonicalização");
        e.setEvidencia("SEI-X"); e.setAutorizacao("ATO-Y|ATO-Z");
        assertFalse(motor.cadeiaHashIntegra(7L), "Mudar dois campos não pode conservar a validade da prova");
    }

    @Test
    void motorDeAlocacaoRecusaMudancaCompensatoriaEntreEvidenciaEAutorizacao() throws Exception {
        EventoAlocacaoVinculo e = alocacao();
        e.setInclusaoExcedente(true); e.setJustificativa("Excedente autorizado");
        e.setEfetivoPrevistoExcedente(1); e.setOcupacaoAposInclusaoExcedente(2L);
        e.setEvidencia("SEI-X|ATO-Y"); e.setAutorizacao("ATO-Z");
        var motor = new MotorTemporalAlocacaoServico(repo(EventoAlocacaoVinculoRepositorio.class, e), null);
        e.setHashEvento(seloDoControle(motor, e));
        assertTrue(motor.cadeiaHashIntegra(7L), "Controle válido selado antes da mutação; não é oráculo de canonicalização");
        e.setEvidencia("SEI-X"); e.setAutorizacao("ATO-Y|ATO-Z");
        assertFalse(motor.cadeiaHashIntegra(7L), "A camada de Alocação também preserva a fronteira documental");
    }

    @Test
    void motorDeVinculoDistingueNuloDoTextoLiteralAngulosNull() {
        EventoVinculoContratual e = vinculo();
        var motor = new MotorTemporalVinculoServico(repo(EventoVinculoContratualRepositorio.class, e));
        assertNull(e.getMatriculaAnterior());
        e.setHashEvento(seloDoControle(motor, e));
        assertTrue(motor.cadeiaHashIntegra(7L));
        e.setMatriculaAnterior("<null>");
        assertFalse(motor.cadeiaHashIntegra(7L), "Texto literal não representa ausência do campo");
    }

    /**
     * Apenas preparação da entrada: usa o cálculo existente para selar o estado original.
     * Não recalcula o selo depois da mutação nem prevê o resultado da assertion negativa.
     * Não é oráculo de campos/bytes/digest; vetores independentes e CMP009 exercem esse contrato.
     */
    private String seloDoControle(Object motor, Object evento) {
        String selo = ReflectionTestUtils.invokeMethod(motor, "hashEsperado", evento);
        assertNotNull(selo, "A fixture precisa ter todos os fatos exigidos pelo cálculo existente");
        assertEquals(64, selo.length());
        return selo;
    }

    private EventoVinculoContratual vinculo() {
        Pessoa p = new Pessoa(); p.setCpf("52998224725");
        Contrato c = new Contrato(); c.setId(2L);
        VinculoContratual v = new VinculoContratual(); v.setId(7L); v.setPessoa(p); v.setContrato(c);
        EventoVinculoContratual e = new EventoVinculoContratual(); e.setVinculo(v);
        e.setTipoEvento(TipoEventoVinculoContratual.CRIACAO); e.setDataEfetiva(LocalDate.of(2026, 6, 1));
        e.setInicioNovo(LocalDate.of(2026, 6, 1)); e.setFimNovo(LocalDate.of(2026, 6, 30));
        e.setMatriculaNova("MAT-001"); e.setPessoaCpfFotografia("52998224725"); e.setResponsavelId(42L);
        e.setJustificativa("Cadastro integrado de Pessoa, Vínculo e Alocação inicial"); return e;
    }

    private EventoAlocacaoVinculo alocacao() throws Exception {
        EventoAlocacaoVinculo e = new EventoAlocacaoVinculo(); e.setVinculo(vinculo().getVinculo());
        AlocacaoVinculo faixa = new AlocacaoVinculo();
        var id = AlocacaoVinculo.class.getDeclaredField("id"); id.setAccessible(true); id.set(faixa, 21L);
        e.setAlocacaoNova(faixa); e.setTipoEvento(TipoEventoAlocacaoVinculo.CRIACAO);
        e.setDataEfetiva(LocalDate.of(2026, 6, 1)); e.setResponsavelId(42L);
        e.setComposicaoNovaId(9L); e.setLotacaoNovaId(10L); e.setJornadaNovaId(11L); e.setCargoNovoId(12L);
        e.setInicioNovo(LocalDate.of(2026, 6, 1)); e.setFimNovo(LocalDate.of(2026, 6, 30));
        e.setEstadoNovo(EstadoNegocialAlocacao.REGISTRADA); return e;
    }

    private <T> T repo(Class<T> tipo, Object evento) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (proxy, method, args) -> {
            if (method.getName().equals("findByVinculoIdOrderByRegistradoEmAscIdAsc")) {
                assertEquals(Long.valueOf(7L), args[0]); return List.of(evento);
            }
            throw new AssertionError("Operação fora do cenário causal: " + method.getName());
        }));
    }
}
