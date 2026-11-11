package com.example.siggtrat.diagnostico;

import com.example.siggtrat.configuracao.propriedades.*;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.PersistenciaRegistroAuditoriaServico;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditoriaFalhaDuplaRetencaoTest {
    @TempDir Path temporario;
    private RegistroAuditoria evento(String id) {var e=new RegistroAuditoria();e.setRequestId(id);return e;}
    @ParameterizedTest(name="recuperarNoEncerramento={0}") @ValueSource(booleans={false,true})
    void bancoEArquivoIndisponiveisNaoDescartamLoteJaDrenado(boolean encerrar) throws Exception {
        var persistencia=mock(PersistenciaRegistroAuditoriaServico.class);
        var fila=new FilaRegistroAuditoria(persistencia,
                new PropriedadesRegistroAuditoria(new PropriedadesRegistroAuditoria.Fila(1000),new PropriedadesRegistroAuditoria.Descarga(50,1000,1,1000)),
                new PropriedadesAuditoria("segredo-somente-ficticio",Files.createDirectory(temporario.resolve("destino-e-diretorio")),
                        new PropriedadesAuditoria.ConteudoRequisicao(1024),"password"));
        var esperados=new HashSet<String>();
        for(int i=0;i<1000;i++){String id="original-"+i;esperados.add(id);fila.enfileirarOuPersistir(evento(id));}
        var primeiraTentativa=new java.util.concurrent.atomic.AtomicBoolean(true);
        doAnswer(inv->{
            // Produtores ocupam o espaço uma vez, enquanto o banco está sendo tentado.
            if(primeiraTentativa.getAndSet(false))
                for(int i=0;i<50;i++){String id="concorrente-"+i;esperados.add(id);fila.enfileirarOuPersistir(evento(id));}
            throw new IllegalStateException("banco ficticio indisponivel");
        }).when(persistencia).salvarLote(anyList());
        fila.descarregar(); // banco falhou, reposição recusada, arquivo inválido.
        fila.descarregar(); // falha persiste: o lote retido não autoriza outra drenagem.
        fila.descarregar();
        reset(persistencia);
        var gravados=new HashSet<String>();
        doAnswer(inv->{List<RegistroAuditoria> lote=inv.getArgument(0);lote.forEach(e->assertTrue(gravados.add(e.getRequestId()),"Evento duplicado"));return null;})
                .when(persistencia).salvarLote(anyList());
        if(encerrar) fila.descarregarAoEncerrar();
        else for(int i=0;i<25;i++)fila.descarregar();
        assertEquals(1050,esperados.size());
        assertEquals(esperados,gravados,"Lote drenado precisa sobreviver à falha dupla e ser recuperado junto aos demais eventos");
    }
}
