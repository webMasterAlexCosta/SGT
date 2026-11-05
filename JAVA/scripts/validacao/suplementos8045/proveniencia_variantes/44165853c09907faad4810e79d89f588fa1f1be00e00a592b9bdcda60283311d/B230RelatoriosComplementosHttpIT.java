package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class B230RelatoriosComplementosHttpIT extends B230Suporte {
    private Map<String,Object> filtro(Cenario c,LocalDate inicio,LocalDate fim) {
        return mapa("tipo","FREQUENCIA_CONSOLIDADA","formato","VISUALIZACAO","inicio",inicio,"fim",fim,
                "contratoIds",List.of(c.contratoA.getId()),"chaveIdempotencia","b230-relatorio-"+UUID.randomUUID());
    }
    private JsonNode emitir(Cenario c,LocalDate data) throws Exception {
        return enviar(c.localA,"/api/relatorios/administrativos/emissoes",filtro(c,data,data),201,
                "Emissão real anterior à retificação do início");
    }
    private JsonNode calcular(Cenario c,LocalDate inicio,LocalDate fim) throws Exception {
        return enviar(c.localA,"/api/relatorios/administrativos/previa",filtro(c,inicio,fim),200,
                "Cálculo administrativo real no recorte explícito");
    }
    private JsonNode reconsultar(Cenario c,long id) throws Exception {
        return consultar(c.localA,"/api/relatorios/administrativos/emissoes/"+id,"Emissão preservada após o fato superveniente");
    }
    private String bytesEmitidos(long id) {
        return nova(() -> HexFormat.of().formatHex(em.find(EmissaoRelatorioAdministrativo.class,id).getConteudoSnapshotGzip()));
    }

    @Test
    void vin014RetificacaoDoInicioSuperaSomenteEmissaoAfetadaPreservaBytesERecalculaRecorte() throws Exception {
        Cenario c=cenario(); declararFonteNominalSintetica(c,"B230-RETIFICACAO-RELATORIO");
        LocalDate atingido=INICIO,controle=INICIO.plusDays(4);
        long frequenciaId=frequencia(c.localA,c.vinculoA.getId(),atingido,"FALTA",200).path("id").asLong();
        long controleFreq=frequencia(c.localA,c.vinculoA.getId(),controle,"FALTA",200).path("id").asLong();
        JsonNode beforeEvents=consultar(c.localA,"/api/frequencias/"+frequenciaId+"/eventos","Fonte antes de retirar cobertura");
        JsonNode emitida=emitir(c,atingido),naoAtingida=emitir(c,controle);
        assertThat(emitida.path("conteudo").path("totais").path("faltas").asInt()).isEqualTo(1);
        assertThat(naoAtingida.path("conteudo").path("totais").path("faltas").asInt()).isEqualTo(1);
        long id=emitida.path("metadados").path("id").asLong(),controlId=naoAtingida.path("metadados").path("id").asLong();
        String bytes=bytesEmitidos(id),controlBytes=bytesEmitidos(controlId);
        retificarInicio(c,c.vinculoA.getId(),INICIO.plusDays(1));
        nova(() -> {
            Frequencia f=em.find(Frequencia.class,frequenciaId);
            assertThat(f.getStatus()).isEqualTo("FALTA"); assertThat(f.getVinculoContratual().getId()).isEqualTo(c.vinculoA.getId());
            assertThat(f.getAlocacaoVinculo().getId()).isEqualTo(c.alocacaoA.getId());
            assertThat(f.getValidadeOperacional().name()).isEqualTo("INVALIDA_COBERTURA");
            assertThat(f.getEstadoAuditoria().name()).isEqualTo("SUJEITA_REVISAO");
            assertThat(f.getBloqueadaEdicaoOperacional()).isTrue();
            assertThat(em.find(Frequencia.class,controleFreq).getValidadeOperacional().name()).isEqualTo("VALIDA");
            assertThat(em.createQuery("select count(p) from PendenciaAdministrativa p where p.frequenciaId=:id",Long.class)
                    .setParameter("id",frequenciaId).getSingleResult()).isEqualTo(1);
            assertThat(em.createQuery("select count(p) from PendenciaAdministrativa p where p.frequenciaId=:id",Long.class)
                    .setParameter("id",controleFreq).getSingleResult()).isZero(); return null;
        });
        JsonNode after=reconsultar(c,id),controlAfter=reconsultar(c,controlId);
        assertThat(after.path("metadados").path("estado").asText()).isEqualTo("SUPERADA");
        assertThat(after.path("conteudo")).isEqualTo(emitida.path("conteudo"));
        assertThat(after.path("metadados").path("hashConteudo")).isEqualTo(emitida.path("metadados").path("hashConteudo"));
        assertThat(bytesEmitidos(id)).isEqualTo(bytes);
        assertThat(controlAfter.path("metadados").path("estado").asText()).isEqualTo("VIGENTE");
        assertThat(controlAfter.path("conteudo")).isEqualTo(naoAtingida.path("conteudo"));
        assertThat(controlAfter.path("metadados").path("hashConteudo")).isEqualTo(naoAtingida.path("metadados").path("hashConteudo"));
        assertThat(bytesEmitidos(controlId)).isEqualTo(controlBytes);
        JsonNode now=calcular(c,atingido,atingido),controlNow=calcular(c,controle,controle);
        assertThat(now.path("conteudo").path("registros")).isEmpty();
        assertThat(now.path("conteudo").path("totais").path("faltas").asInt()).isZero();
        assertThat(now.path("metadados").path("sujeitoARevisao").asBoolean()).isTrue();
        assertThat(now.path("metadados").path("quantidadePendencias").asLong()).isEqualTo(1);
        assertThat(controlNow.path("conteudo").path("totais").path("faltas").asInt()).isEqualTo(1);
        assertThat(controlNow.path("metadados").path("quantidadePendencias").asLong()).isZero();
        JsonNode afterEvents=consultar(c.localA,"/api/frequencias/"+frequenciaId+"/eventos","Trilha da fonte reavaliada");
        assertThat(afterEvents.get(0)).isEqualTo(beforeEvents.get(0));
        assertThat(afterEvents.size()).isGreaterThan(beforeEvents.size());
        registrarB230("VIN-014:003",mapa("emissaoAfetadaId",id,"emissaoControleId",controlId,"estados",List.of("SUPERADA","VIGENTE"),
                "bytesOriginaisPreservados",true,"faltasApos",0,"faltasControle",1,"pendenciasApos",1));
    }

    @Test
    void fre011XlsxBaixadoContemQuantidadeDePendenciasReaisEControleNaoAfetadoNaoAvisa() throws Exception {
        Cenario c=cenario(); declararFonteNominalSintetica(c,"B230-XLSX-PENDENCIAS-REAIS");
        long first=frequencia(c.localA,c.vinculoA.getId(),INICIO,"FALTA",200).path("id").asLong();
        long second=frequencia(c.localA,c.vinculoA.getId(),INICIO.plusDays(1),"FALTA",200).path("id").asLong();
        LocalDate controlDay=INICIO.plusDays(4);
        frequencia(c.localA,c.vinculoA.getId(),controlDay,"FALTA",200);
        retificarInicio(c,c.vinculoA.getId(),INICIO.plusDays(2));
        long count=nova(() -> em.createQuery("select count(p) from PendenciaAdministrativa p where p.frequenciaId in :ids",Long.class)
                .setParameter("ids",List.of(first,second)).getSingleResult());
        assertThat(count).isEqualTo(2);
        JsonNode live=calcular(c,INICIO,INICIO.plusDays(1));
        assertThat(live.path("metadados").path("quantidadePendencias").asLong()).isEqualTo(count);
        assertThat(live.path("metadados").path("sujeitoARevisao").asBoolean()).isTrue();
        byte[] affected=xlsx(c,INICIO,INICIO.plusDays(1)); String affectedText=textoPlanilha(affected);
        assertThat(affectedText).contains("SUJEITO A REVISÃO",count+" pendências");
        JsonNode control=calcular(c,controlDay,controlDay);
        assertThat(control.path("metadados").path("quantidadePendencias").asLong()).isZero();
        byte[] clean=xlsx(c,controlDay,controlDay); String cleanText=textoPlanilha(clean);
        assertThat(cleanText).doesNotContain("SUJEITO A REVISÃO");
        Path dir=Path.of("target/auditoria-http/b230-exportacoes"); Files.createDirectories(dir);
        String suffix=Long.toString(c.contratoA.getId());
        Files.write(dir.resolve("pendencias-"+suffix+".xlsx"),affected);
        Files.write(dir.resolve("controle-"+suffix+".xlsx"),clean);
        registrarB230("FRE-011:002",mapa("contratoId",c.contratoA.getId(),"pendenciasSQL",count,"pendenciasJSON",count,
                "textoXlsx",count+" pendências","arquivoSha256",hash(affected),"controleSha256",hash(clean),
                "bytesArquivo",affected.length,"bytesControle",clean.length,"servicosDeRelatorioMockados",false));
    }

    private byte[] xlsx(Cenario c,LocalDate inicio,LocalDate fim) throws Exception {
        var response=executar(c.localA,get("/api/relatorios/frequencia-consolidada/exportacao")
                .param("inicio",inicio.toString()).param("fim",fim.toString())
                .param("contratoIds",c.contratoA.getId().toString()).param("formato","XLSX"),200,
                "Download real XLSX para conferir o aviso dentro dos bytes").getResponse();
        assertThat(response.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition")).contains(".xlsx");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        return response.getContentAsByteArray();
    }
    private String textoPlanilha(byte[] bytes) throws Exception {
        StringBuilder content=new StringBuilder(); boolean workbook=false; int sheets=0;
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        try (ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)) {
            for (var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
                if (entry.getName().equals("xl/workbook.xml")) workbook=true;
                if (entry.getName().startsWith("xl/worksheets/") && entry.getName().endsWith(".xml")) sheets++;
                if (entry.getName().equals("xl/sharedStrings.xml") || entry.getName().startsWith("xl/worksheets/") && entry.getName().endsWith(".xml"))
                    content.append(factory.newDocumentBuilder().parse(new ByteArrayInputStream(zip.readAllBytes())).getDocumentElement().getTextContent());
            }
        }
        assertThat(workbook).isTrue(); assertThat(sheets).isGreaterThan(0); return content.toString();
    }
    private String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
}
