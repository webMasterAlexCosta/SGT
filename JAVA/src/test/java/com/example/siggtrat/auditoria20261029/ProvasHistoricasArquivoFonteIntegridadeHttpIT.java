package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** RED: metadados publicados não certificam bytes físicos ausentes ou adulterados.
 * O teste altera exclusivamente o arquivo sintético que acabou de publicar por API
 * e restaura seus bytes originais em finally, inclusive se a hipótese falhar. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties={"spring.cache.type=redis","spring.sql.init.mode=never","app.cache.redis.http-get.enabled=true"})
class ProvasHistoricasArquivoFonteIntegridadeHttpIT extends ContratosHttpBase {
    @Autowired PropriedadesArmazenamentoArquivo armazenamento;
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private static final String FINALIDADE = "Conferir o arquivo sintético de identidade/contexto antes de incorporar prova histórica";

    @ParameterizedTest
    @CsvSource({"NOME,false","NOME,true","MOTIVO,false","MOTIVO,true"})
    void confirmacaoRevalidaBytesDaFonteAposPreviaSemIncorporarProvaParcial(String alvo,boolean apagar) throws Exception {
        Cenario c=criarCenarioPersistido();
        conceder(c,c.contratoA.getId(),"INCORPORACAO_PROVA_HISTORICA");
        String nonce=UUID.randomUUID().toString();
        JsonNode documento=dados(executar(c.localA,multipart(base(c.contratoA.getId())+"/documentos")
                .file(metadados(mapa("tipoFato","VINCULO","fatoId",c.vinculoA.getId(),
                        "fatoChave","VINCULO-"+c.vinculoA.getId(),"titulo","Arquivo sintético exclusivo "+nonce,
                        "descricao","Fonte exclusiva da hipótese de integridade "+nonce,"dataEfetiva",DATA,
                        "classificacaoAcesso","PESSOAL_RESTRITO","chaveIdempotencia",nonce)))
                .file(new MockMultipartFile("arquivo","prova-"+nonce+".pdf","application/pdf",PDF)),
                201,"Arquivo sintético publicado pelo fluxo documental canônico"));
        long documentoId=documento.path("id").asLong();
        assertThat(documento.path("estado").asText()).isEqualTo("PUBLICADO");
        assertThat(documento.path("downloadDisponivel").asBoolean()).isTrue();
        String nome=emTransacao(()->em.find(DocumentoComprobatorio.class,documentoId).getNomeArmazenado());
        Path raiz=armazenamento.uploadDir().toAbsolutePath().normalize();
        Path arquivo=raiz.resolve(nome).normalize();
        assertThat(arquivo.getParent()).isEqualTo(raiz);
        assertThat(Files.readAllBytes(arquivo)).isEqualTo(PDF);
        assertThat(executar(c.localA,get(base(c.contratoA.getId())+"/documentos/"+documentoId+"/arquivo")
                .param("finalidade",FINALIDADE),200,"Pré-condição: fonte física original acessível e íntegra")
                .getResponse().getContentAsByteArray()).isEqualTo(PDF);

        String rota;
        Map<String,Object> r=mapa("dataInicioEfetiva",DATA,"dataFimEfetiva",DATA.plusDays(1),
                "documentoComprobatorioId",documentoId,"finalidadeAcessoDocumento",FINALIDADE,
                "motivo","Fonte física sintética deve conferir no momento da incorporação",
                "autorizacao","ATO-SINTETICO-INTEGRIDADE","chaveIdempotencia",UUID.randomUUID().toString());
        if(alvo.equals("NOME")) {
            rota="/api/pessoas/"+c.pessoaA.getId()+"/provas-nome";
            r.put("vinculoId",c.vinculoA.getId()); r.put("nomeComprovado","Nome documentado na fonte sintética");
            r.put("versaoPessoaEsperada",consultar(c.localA,"/api/pessoas/"+c.pessoaA.getId(),"Versão Pessoa").path("version").asLong());
            r.put("versaoVinculoEsperada",consultar(c.localA,"/api/vinculos-contratuais/"+c.vinculoA.getId(),"Versão Vínculo").path("version").asLong());
        } else {
            long motivo=enviar(c.localA,"/api/motivos-desligamento",mapa("descricao","Motivo fonte "+nonce,
                    "categoria","DESLIGAMENTO_INDIVIDUAL","ativo",true),201,"Catálogo de Motivo da Gestão comprovada").path("id").asLong();
            rota="/api/motivos-desligamento/"+motivo+"/provas-historicas";
            r.put("contratoId",c.contratoA.getId()); r.put("descricaoComprovada","Causa documentada na fonte sintética");
            r.put("categoriaComprovada","DESLIGAMENTO_INDIVIDUAL");
            r.put("versaoContratoEsperada",consultar(c.localA,base(c.contratoA.getId()),"Versão Contrato").path("version").asLong());
            r.put("versaoMotivoEsperada",consultar(c.localA,"/api/motivos-desligamento/"+motivo,"Versão Motivo").path("version").asLong());
        }
        JsonNode previa=enviar(c.localA,rota+"/previa",r,200,"Prévia com arquivo íntegro, fonte autorizada e finalidade explícita");
        assertThat(previa.path("tokenPrevia").asText()).isNotBlank();
        r.put("confirmarImpacto",true); r.put("tokenPrevia",previa.path("tokenPrevia").asText());
        try {
            if(apagar) Files.delete(arquivo);
            else Files.write(arquivo,"%PDF-1.4\nBYTES-SINTETICOS-ADULTERADOS\n%%EOF".getBytes(StandardCharsets.UTF_8));
            enviar(c.localA,rota,r,409,"DOC: confirmação não pode certificar fonte física ausente/adulterada após a prévia");
            String entidade=alvo.equals("NOME")?"ProvaNomePessoaHistorico":"ProvaMotivoDesligamentoHistorico";
            assertThat(emTransacao(()->em.createQuery("select count(p) from "+entidade+" p where p.contrato.id=:c",Long.class)
                    .setParameter("c",c.contratoA.getId()).getSingleResult())).isZero();
        } finally {
            Files.write(arquivo,PDF);
        }
        assertThat(Files.readAllBytes(arquivo)).isEqualTo(PDF);
    }
}
