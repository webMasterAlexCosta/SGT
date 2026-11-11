package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.configuracao.propriedades.PropriedadesEnvioArquivo;
import com.example.siggtrat.configuracao.propriedades.PropriedadesOcorrencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OcorrenciaInicializacaoTest {
    @TempDir Path temporario;

    private OcorrenciaServico servico(Path raiz) {
        return new OcorrenciaServico(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                new PropriedadesEnvioArquivo(1024, 2048, 2),
                new PropriedadesArmazenamentoArquivo(raiz), new PropriedadesOcorrencia(30));
    }

    @Test void preservaCausaQuandoRaizNaoPodeSerCriada() throws Exception {
        Path raiz = Files.writeString(temporario.resolve("uploads"), "arquivo preservado");

        RuntimeException erro = assertThrows(RuntimeException.class, () -> servico(raiz).inicializar());

        var causa = assertInstanceOf(FileAlreadyExistsException.class, erro.getCause());
        assertEquals(raiz.toString(), causa.getFile());
        assertEquals("arquivo preservado", Files.readString(raiz));
    }

    @Test void preservaCausaQuandoStagingNaoPodeSerCriado() throws Exception {
        Path raiz = Files.createDirectory(temporario.resolve("uploads"));
        Path staging = Files.writeString(raiz.resolve(".staging"), "arquivo preservado");

        RuntimeException erro = assertThrows(RuntimeException.class, () -> servico(raiz).inicializar());

        var causa = assertInstanceOf(FileAlreadyExistsException.class, erro.getCause());
        assertEquals(staging.toString(), causa.getFile());
        assertEquals("arquivo preservado", Files.readString(staging));
    }

    @Test void inicializacaoRepetidaPreservaArquivosExistentes() throws Exception {
        Path raiz = temporario.resolve("uploads");
        OcorrenciaServico servico = servico(raiz);
        servico.inicializar();
        Path existente = Files.writeString(raiz.resolve(".staging").resolve("existente"), "preservado");

        servico.inicializar();

        assertTrue(Files.isDirectory(raiz));
        assertTrue(Files.isDirectory(raiz.resolve(".staging")));
        assertEquals("preservado", Files.readString(existente));
    }
}
