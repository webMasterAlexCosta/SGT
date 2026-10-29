package com.example.siggtrat.frequencia;

import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FrequenciaEtapa09Test {

    @Test
    void janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia() {
        var p = new PoliticaTemporalFrequenciaServico();
        LocalDate d = LocalDate.of(2026, 1, 1);
        ZoneId z = ZoneId.of("America/Sao_Paulo");
        assertTrue(p.abertaParaOperacional(d, ZonedDateTime.of(2026,1,7,23,59,59,0,z)));
        assertTrue(p.fechada(d, ZonedDateTime.of(2026,1,8,0,0,0,0,z)));
        assertEquals(LocalDateTime.of(2026,1,8,0,0), p.prazoLocal(d));
    }

    @Test
    void politicaConverteInstanteParaSaoPauloSemOffsetFixo() {
        var p = new PoliticaTemporalFrequenciaServico();
        LocalDate d = LocalDate.of(2026, 1, 1);
        assertFalse(p.fechada(d, ZonedDateTime.parse("2026-01-08T02:59:59Z")));
        assertTrue(p.fechada(d, ZonedDateTime.parse("2026-01-08T03:00:00Z")));
    }

    @Test
    void resolvedorUsaContratoVinculoAlocacaoEGestaoHistoricos() {
        var pessoaRepo = mock(PessoaRepositorio.class);
        var vincRepo = mock(VinculoContratualRepositorio.class);
        var alocRepo = mock(AlocacaoVinculoRepositorio.class);
        var motorContrato = mock(MotorTemporalContratoServico.class);
        var motorVinc = mock(MotorTemporalVinculoServico.class);
        var motorAloc = mock(MotorTemporalAlocacaoServico.class);
        var gestaoRepo = mock(GestaoLocalRepositorio.class);
        var s = new ResolvedorContextoTemporalFrequenciaServico(pessoaRepo, vincRepo, alocRepo,
                motorContrato, motorVinc, motorAloc, gestaoRepo);

        Empregado emp = mock(Empregado.class); when(emp.getCpf()).thenReturn("123.456.789-09");
        Pessoa pessoa = mock(Pessoa.class); when(pessoa.getId()).thenReturn(10L);
        Contrato contrato = mock(Contrato.class); when(contrato.getId()).thenReturn(20L);
        VinculoContratual vinc = mock(VinculoContratual.class); when(vinc.getId()).thenReturn(30L); when(vinc.getContrato()).thenReturn(contrato);
        GestaoLocal gestao = mock(GestaoLocal.class); when(gestao.getId()).thenReturn(40L);
        Lotacao lot = mock(Lotacao.class); when(lot.getGestaoLocal()).thenReturn(gestao);
        Jornada jorn = mock(Jornada.class); when(jorn.getGestaoLocal()).thenReturn(gestao);
        Cargo cargo = mock(Cargo.class); when(cargo.getGestaoLocal()).thenReturn(gestao);
        AlocacaoVinculo aloc = mock(AlocacaoVinculo.class); when(aloc.getId()).thenReturn(50L); when(aloc.getVinculo()).thenReturn(vinc);
        when(aloc.getLotacao()).thenReturn(lot); when(aloc.getJornada()).thenReturn(jorn); when(aloc.getCargo()).thenReturn(cargo);
        LocalDate data = LocalDate.of(2026, 8, 10);

        when(pessoaRepo.findByCpf("12345678909")).thenReturn(Optional.of(pessoa));
        when(vincRepo.findByPessoaIdOrderByDataInicioAscIdAsc(10L)).thenReturn(List.of(vinc));
        when(motorVinc.reconstruir(eq(vinc), eq(data), any())).thenReturn(new PosicaoVinculoTemporalDTO(
                30L,10L,20L,data,Instant.now(),data.minusMonths(1),null,"M1",EstadoNegocialVinculo.REGISTRADO,SituacaoTemporalVinculo.VIGENTE,1L));
        var versao = new VersaoContratoNegocialDTO(1L,1,1L,data.minusMonths(1),Instant.now(),"CTR-HIST",2026,"x",1L,"t",1L,
                "1","r","n",data.minusMonths(1),data.plusMonths(2),10,40L,"Gestão histórica");
        when(motorContrato.reconstruirPosicao(eq(contrato),eq(data),any())).thenReturn(new PosicaoTemporalContratoDTO(
                20L,data,Instant.now(),EstadoNegocialContrato.PUBLICADO,SituacaoTemporalContrato.VIGENTE,
                HabilitacaoAdministrativaContrato.ATIVO,data.minusMonths(1),data.plusMonths(2),versao,List.of(),List.of()));
        when(motorAloc.reconstruir(eq(vinc),eq(data),any())).thenReturn(new PosicaoAlocacaoTemporalDTO(
                30L,data,Instant.now(),SituacaoTemporalAlocacao.VIGENTE,50L,60L,70L,"L",80L,"T",90L,"C",data.minusDays(1),null,2L));
        when(alocRepo.findById(50L)).thenReturn(Optional.of(aloc));
        when(gestaoRepo.findById(40L)).thenReturn(Optional.of(gestao));

        var ctx = s.resolver(emp,data);
        assertSame(vinc,ctx.vinculo()); assertSame(aloc,ctx.alocacao()); assertSame(gestao,ctx.gestaoHistorica());
        assertEquals("CTR-HIST",ctx.posicaoContrato().versaoContratual().numeroContrato());
        verify(emp, never()).getLotacao();
        verify(emp, never()).getJornada();
        verify(emp, never()).getCargo();
    }

    @Test
    void resolvedorBloqueiaContratoHistoricamenteInativo() {
        var pessoaRepo=mock(PessoaRepositorio.class); var vincRepo=mock(VinculoContratualRepositorio.class);
        var alocRepo=mock(AlocacaoVinculoRepositorio.class); var mc=mock(MotorTemporalContratoServico.class);
        var mv=mock(MotorTemporalVinculoServico.class); var ma=mock(MotorTemporalAlocacaoServico.class); var gr=mock(GestaoLocalRepositorio.class);
        var s=new ResolvedorContextoTemporalFrequenciaServico(pessoaRepo,vincRepo,alocRepo,mc,mv,ma,gr);
        Empregado e=mock(Empregado.class); when(e.getCpf()).thenReturn("12345678909"); Pessoa p=mock(Pessoa.class); when(p.getId()).thenReturn(1L);
        Contrato c=mock(Contrato.class); VinculoContratual v=mock(VinculoContratual.class); when(v.getContrato()).thenReturn(c);
        LocalDate d=LocalDate.of(2026,8,1); when(pessoaRepo.findByCpf(anyString())).thenReturn(Optional.of(p)); when(vincRepo.findByPessoaIdOrderByDataInicioAscIdAsc(1L)).thenReturn(List.of(v));
        when(mv.reconstruir(eq(v),eq(d),any())).thenReturn(new PosicaoVinculoTemporalDTO(2L,1L,3L,d,Instant.now(),d.minusDays(1),null,null,EstadoNegocialVinculo.REGISTRADO,SituacaoTemporalVinculo.VIGENTE,1L));
        when(mc.reconstruirPosicao(eq(c),eq(d),any())).thenReturn(new PosicaoTemporalContratoDTO(3L,d,Instant.now(),EstadoNegocialContrato.PUBLICADO,SituacaoTemporalContrato.VIGENTE,HabilitacaoAdministrativaContrato.INATIVO,d.minusDays(1),d.plusDays(2),null,List.of(),List.of()));
        ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->s.resolver(e,d));
        assertEquals(HttpStatus.CONFLICT,ex.getStatusCode()); verifyNoInteractions(ma);
    }

    @Test
    void impactoRetroativoSemCoberturaPreservaRegistroEMarcaInvalido() {
        FrequenciaRepositorio repo=mock(FrequenciaRepositorio.class); ResolvedorContextoTemporalFrequenciaServico res=mock(ResolvedorContextoTemporalFrequenciaServico.class); AuditoriaFrequenciaServico aud=mock(AuditoriaFrequenciaServico.class);
        var s=new RevalidacaoFrequenciaTemporalServico(repo,res,aud); Frequencia f=frequenciaBase();
        when(repo.findByVinculoContratualIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(eq(20L),any())).thenReturn(List.of(f));
        when(aud.snapshot(f)).thenReturn("antes"); when(res.resolver(any(),any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"sem cobertura"));
        assertEquals(1,s.revalidarPorVinculo(20L,f.getDataReferencia(),"Desligamento retroativo"));
        assertEquals(ValidadeOperacionalFrequencia.INVALIDA_COBERTURA,f.getValidadeOperacional());
        assertEquals(EstadoAuditoriaFrequencia.SUJEITA_REVISAO,f.getEstadoAuditoria()); assertTrue(f.getBloqueadaEdicaoOperacional());
        verify(aud).registrar(eq(f),eq(TipoEventoFrequencia.IMPACTO_TEMPORAL),eq("antes"),contains("Desligamento"),isNull(),isNull(),isNull());
    }

    @Test
    void reativacaoNaoRestauraValidadeSilenciosamente() {
        FrequenciaRepositorio repo=mock(FrequenciaRepositorio.class); ResolvedorContextoTemporalFrequenciaServico res=mock(ResolvedorContextoTemporalFrequenciaServico.class); AuditoriaFrequenciaServico aud=mock(AuditoriaFrequenciaServico.class);
        var s=new RevalidacaoFrequenciaTemporalServico(repo,res,aud); Frequencia f=frequenciaBase(); f.setValidadeOperacional(ValidadeOperacionalFrequencia.INVALIDA_COBERTURA); f.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        var ctx=contextoCoincidente(); when(repo.findByContratoSnapshotIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(eq(10L),any())).thenReturn(List.of(f)); when(res.resolver(any(),any())).thenReturn(ctx); when(aud.snapshot(f)).thenReturn("antes");
        assertEquals(1,s.revalidarPorContrato(10L,f.getDataReferencia(),"Reativação retroativa"));
        assertEquals(ValidadeOperacionalFrequencia.SUJEITA_REVISAO,f.getValidadeOperacional()); assertTrue(f.getBloqueadaEdicaoOperacional());
    }

    @Test
    void contextoMudadoNaoReassociaFrequenciaAutomaticamente() {
        FrequenciaRepositorio repo=mock(FrequenciaRepositorio.class); ResolvedorContextoTemporalFrequenciaServico res=mock(ResolvedorContextoTemporalFrequenciaServico.class); AuditoriaFrequenciaServico aud=mock(AuditoriaFrequenciaServico.class);
        var s=new RevalidacaoFrequenciaTemporalServico(repo,res,aud); Frequencia f=frequenciaBase(); AlocacaoVinculo original=f.getAlocacaoVinculo();
        var ctx=contextoCoincidente(); AlocacaoVinculo nova=mock(AlocacaoVinculo.class); when(nova.getId()).thenReturn(999L);
        ctx=new ResolvedorContextoTemporalFrequenciaServico.Contexto(ctx.pessoa(),ctx.vinculo(),nova,ctx.posicaoContrato(),ctx.posicaoVinculo(),ctx.posicaoAlocacao(),ctx.gestaoHistorica());
        when(repo.findByVinculoContratualIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(eq(20L),any())).thenReturn(List.of(f)); when(res.resolver(any(),any())).thenReturn(ctx); when(aud.snapshot(f)).thenReturn("antes");
        assertEquals(1,s.revalidarPorVinculo(20L,f.getDataReferencia(),"Alocação retificada"));
        assertSame(original,f.getAlocacaoVinculo()); assertEquals(ValidadeOperacionalFrequencia.SUJEITA_REVISAO,f.getValidadeOperacional());
    }

    @Test
    void auditoriaGeraHashEValidaCadeia() {
        EventoFrequenciaRepositorio repo=mock(EventoFrequenciaRepositorio.class); AuditoriaFrequenciaServico s=new AuditoriaFrequenciaServico(repo);
        Frequencia f=frequenciaBase(); f.setId(100L); f.setVersaoNegocial(1); when(repo.findTopByFrequenciaIdOrderByNumeroVersaoDescIdDesc(100L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));
        EventoFrequencia ev=s.registrar(f,TipoEventoFrequencia.REGISTRO,null,null,null,null,null);
        assertNotNull(ev.getHashEvento()); assertEquals(64,ev.getHashEvento().length());
        when(repo.findByFrequenciaIdOrderByNumeroVersaoAscIdAsc(100L)).thenReturn(List.of(ev));
        assertEquals(1,s.listar(100L).size());
        ev.setHashEvento("0".repeat(64));
        assertThrows(ResponseStatusException.class,()->s.listar(100L));
    }

    @Test
    void idempotenciaAceitaMesmaIntencaoERejeitaOutra() {
        EventoFrequenciaRepositorio repo=mock(EventoFrequenciaRepositorio.class); AuditoriaFrequenciaServico s=new AuditoriaFrequenciaServico(repo);
        Frequencia f=frequenciaBase(); f.setId(10L); EventoFrequencia ev=new EventoFrequencia(); ev.setFrequencia(f); ev.setHashIntencao("abc");
        when(repo.findByChaveIdempotencia("chave-123")).thenReturn(Optional.of(ev));
        assertTrue(s.chaveJaAplicada(10L,"chave-123","abc"));
        assertThrows(ResponseStatusException.class,()->s.chaveJaAplicada(10L,"chave-123","xyz"));
        assertThrows(ResponseStatusException.class,()->s.chaveJaAplicada(11L,"chave-123","abc"));
    }

    @Test
    void fechamentoAutomaticoPersisteEstadoEEvento() {
        FrequenciaRepositorio repo=mock(FrequenciaRepositorio.class); AuditoriaFrequenciaServico aud=mock(AuditoriaFrequenciaServico.class); PoliticaTemporalFrequenciaServico pol=new PoliticaTemporalFrequenciaServico();
        var s=new FechamentoAutomaticoFrequenciaNegocialServico(repo,pol,aud); Frequencia f=frequenciaBase(); f.setDataReferencia(LocalDate.now(pol.zona()).minusDays(8)); f.setEstadoNegocial(EstadoNegocialFrequencia.ABERTA);
        when(repo.findByEstadoNegocialAndDataReferenciaLessThanEqual(eq(EstadoNegocialFrequencia.ABERTA),any())).thenReturn(List.of(f)); when(aud.snapshot(f)).thenReturn("antes");
        assertEquals(1,s.fecharVencidas()); assertEquals(EstadoNegocialFrequencia.FECHADA,f.getEstadoNegocial()); verify(repo).saveAndFlush(f); verify(aud).registrar(eq(f),eq(TipoEventoFrequencia.FECHAMENTO_AUTOMATICO),eq("antes"),anyString(),isNull(),isNull(),isNull());
    }

    @Test
    void todaEscritaDoGestorLocalEhTratadaComoRetificacaoFormal() throws Exception {
        String codigo=Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/FrequenciaServico.java"));
        assertTrue(codigo.contains("boolean intervencaoGerencial = gestor;"));
        assertFalse(codigo.contains("gestor && (isUpdate || fechadaPeloTempo)"));
        assertTrue(codigo.contains("exigirRetificacaoGerencial(dto)"));
    }

    @Test
    void fonteDaFrequenciaNaoUsaHistoricoVinculoLegadoNemPrazoPorUpdatedAt() throws Exception {
        String codigo=Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/FrequenciaServico.java"));
        assertFalse(codigo.contains("HistoricoVinculoContrato")); assertFalse(codigo.contains("FREQUENCIA_PRAZO_EDICAO_HORAS"));
        assertTrue(codigo.contains("resolvedorContextoTemporal.resolver")); assertTrue(codigo.contains("politicaTemporal.fechada"));
    }

    @Test
    void consumidoresOperacionaisIgnoramFrequenciaForaDaVerdadeOficial() throws Exception {
        String painel=Files.readString(Path.of("src/main/java/com/example/siggtrat/repositorio/PainelRepositorio.java"));
        String efetivo=Files.readString(Path.of("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java"));
        String relatorio=Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/RelatorioConsultaServico.java"));
        String consolidado=Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/RelatorioFrequenciaConsolidadoServico.java"));
        assertTrue(painel.contains("validade_operacional = 'VALIDA'"));
        assertTrue(efetivo.contains("validade_operacional = 'VALIDA'"));
        assertTrue(relatorio.contains("validade_operacional = 'VALIDA'"));
        assertTrue(consolidado.contains("ValidadeOperacionalFrequencia.VALIDA"));
    }

    @Test
    void retroatividadeEstaConectadaAosQuatroConsumidoresTemporais() throws Exception {
        for (String arquivo : List.of("VinculoContratualServico.java", "AlocacaoVinculoServico.java",
                "ContratoServico.java", "AditamentoContratualServico.java")) {
            String codigo=Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/" + arquivo));
            assertTrue(codigo.contains("RevalidacaoFrequenciaTemporalServico"), arquivo);
            assertTrue(codigo.contains("revalidarPor"), arquivo);
        }
    }

    @Test
    void apiExpoeCadeiaDeEventosDaFrequencia() throws Exception {
        String controlador=Files.readString(Path.of("src/main/java/com/example/siggtrat/controlador/FrequenciaControlador.java"));
        assertTrue(controlador.contains("@GetMapping(\"/{id}/eventos\")"));
        assertTrue(controlador.contains("listarEventos"));
    }

    @Test
    void jornadaNoturnaMantemDataNegocialDeInicioComoChave() {
        LocalDate inicio=LocalDate.of(2026,9,10); LocalDate fimFisico=inicio.plusDays(1);
        Frequencia f=new Frequencia(); f.setDataReferencia(inicio); f.antesPersistirOuAtualizar();
        assertEquals(inicio,f.getDataReferencia()); assertEquals(10,f.getDataReferencia().getDayOfMonth()); assertEquals(11,fimFisico.getDayOfMonth());
    }

    private static Frequencia frequenciaBase() {
        Frequencia f=new Frequencia(); Empregado e=mock(Empregado.class); when(e.getId()).thenReturn(1L); f.setEmpregado(e); f.setDataReferencia(LocalDate.of(2026,8,10));
        VinculoContratual v=mock(VinculoContratual.class); when(v.getId()).thenReturn(20L); AlocacaoVinculo a=mock(AlocacaoVinculo.class); when(a.getId()).thenReturn(30L);
        f.setVinculoContratual(v); f.setAlocacaoVinculo(a); f.setContratoSnapshotId(10L); f.setContratoSnapshotNumero("CTR-HIST"); f.setLotacaoSnapshotId(70L); f.setJornadaSnapshotId(80L); f.setCargoSnapshotId(90L); f.setGestaoLocalSnapshotId(40L); f.setEstadoNegocial(EstadoNegocialFrequencia.ABERTA); f.setValidadeOperacional(ValidadeOperacionalFrequencia.VALIDA); f.setEstadoAuditoria(EstadoAuditoriaFrequencia.ORIGINAL); f.setBloqueadaEdicaoOperacional(false); f.setVersaoNegocial(1); f.setStatus("PRESENCA"); return f;
    }

    private static ResolvedorContextoTemporalFrequenciaServico.Contexto contextoCoincidente() {
        Pessoa p=mock(Pessoa.class); VinculoContratual v=mock(VinculoContratual.class); when(v.getId()).thenReturn(20L); Contrato c=mock(Contrato.class); when(c.getId()).thenReturn(10L); when(v.getContrato()).thenReturn(c);
        AlocacaoVinculo a=mock(AlocacaoVinculo.class); when(a.getId()).thenReturn(30L); GestaoLocal g=mock(GestaoLocal.class); when(g.getId()).thenReturn(40L); LocalDate d=LocalDate.of(2026,8,10);
        var versao=new VersaoContratoNegocialDTO(1L,1,1L,d.minusDays(2),Instant.now(),"CTR-HIST",2026,"",1L,"",1L,"","","",d.minusDays(2),d.plusDays(20),1,40L,"G");
        var pc=new PosicaoTemporalContratoDTO(10L,d,Instant.now(),EstadoNegocialContrato.PUBLICADO,SituacaoTemporalContrato.VIGENTE,HabilitacaoAdministrativaContrato.ATIVO,d.minusDays(2),d.plusDays(20),versao,List.of(),List.of());
        var pv=new PosicaoVinculoTemporalDTO(20L,1L,10L,d,Instant.now(),d.minusDays(2),null,null,EstadoNegocialVinculo.REGISTRADO,SituacaoTemporalVinculo.VIGENTE,1L);
        var pa=new PosicaoAlocacaoTemporalDTO(20L,d,Instant.now(),SituacaoTemporalAlocacao.VIGENTE,30L,60L,70L,"L",80L,"T",90L,"C",d.minusDays(1),null,2L);
        return new ResolvedorContextoTemporalFrequenciaServico.Contexto(p,v,a,pc,pv,pa,g);
    }
}
