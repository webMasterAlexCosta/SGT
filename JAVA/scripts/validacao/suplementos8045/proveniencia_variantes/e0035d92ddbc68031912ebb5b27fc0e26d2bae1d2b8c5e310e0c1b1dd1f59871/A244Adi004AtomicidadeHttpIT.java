package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Explicit fault-injected transaction integration test. The real service runs,
 * writes and flushes, then a test-only aspect raises a deliberate failure.
 * This is neither TCP/Chrome nor evidence of an application defect on its own.
 */
@Import(A244Adi004AtomicidadeHttpIT.FaultConfiguration.class)
class A244Adi004AtomicidadeHttpIT extends A244Suporte {
    @Autowired private LateFault fault;
    @Test void adi004_004_lateFailureRollsBackAllBusinessRowsSourcesAndFiles() throws Exception {
        var c=fixture();var n=published(c,100,null,true);String key="A244-FAIL-AFTER-REAL-WRITES";
        byte[] document="%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);
        var source=dados(executar(c.localA,multipart(base(n.id())+"/documentos")
                .file(metadados(mapa("tipoFato","ADITAMENTO","fatoChave",key,"titulo","Prova sintética A244 sem valor administrativo",
                        "classificacaoAcesso","NEGOCIAL","dataEfetiva",n.inicio().plusDays(1))))
                .file(new MockMultipartFile("arquivo","a244-sintetico.pdf","application/pdf",document)),201,"A244 draft document exists before attempt"));
        long documentId=source.path("id").asLong();assertThat(source.path("estado").asText()).isEqualTo("RASCUNHO");
        var request=aditamento(key,n.inicio().plusDays(1));request.remove("referenciaSei");request.put("novaDescricao","Descrição A244 após ato válido");
        request.put("novoEfetivoTotal",101);request.put("alteracoesComposicao",List.of(mapa("composicaoId",n.composicao1(),"quantidadeResultante",101)));
        previaAditamento(c,n.id(),request);
        var before=physicalImage();
        assertThat(before.files()).isNotEmpty();
        assertThat(source.path("sha256Arquivo").asText()).isEqualTo(digest(document));
        assertThat(before.files()).containsValue(digest(document));
        fault.arm(n.id(),key,documentId);
        try {
            enviar(c.localA,base(n.id())+"/aditamentos",request,500,"A244 deliberate test fault after real source publication and flush");
        } finally {fault.disarm();}
        assertThat(fault.observedActiveTransaction).isTrue();
        assertThat(fault.observedInstrumentRows).isEqualTo(1);
        assertThat(fault.observedPublishedDocument).isTrue();
        assertThat(fault.flushedBeforeFailure).isTrue();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        var after=physicalImage();
        assertThat(after).as("All physical rows/columns and persistent file bytes are identical after rollback; only named technical tables excluded").isEqualTo(before);
        var rejected=consultar(c.localA,base(n.id())+"/aditamentos","A244 no partial instrument after independent transaction");
        assertThat(rejected).isEmpty();
        var draft=consultar(c.localA,base(n.id())+"/documentos/"+documentId,"A244 prepared source remains draft after rollback");
        assertThat(draft.path("estado").asText()).isEqualTo("RASCUNHO");
        assertThat(draft.path("sha256Arquivo")).isEqualTo(source.path("sha256Arquivo"));
        // Known completed failure + independent unchanged snapshot; new preview
        // is an explicit positive control, never a blind retry of uncertain HTTP.
        previaAditamento(c,n.id(),request);
        var success=enviar(c.localA,base(n.id())+"/aditamentos",request,201,"A244 disarmed real commit positive control");
        assertThat(success.path("novoEfetivoTotal").asInt()).isEqualTo(101);
        readTx(() -> {
            var stored=em.find(DocumentoComprobatorio.class,documentId);
            assertThat(stored.getEstado().name()).isEqualTo("PUBLICADO");
            assertThat(stored.getFatoId()).isEqualTo(success.path("id").asLong());return null;
        });
        record("ADI-004:004",mapa("fault","TEST_ONLY_AFTER_REAL_DOCUMENT_PUBLICATION_AND_FLUSH",
                "before",before,"afterRejectedCommit",after,"realInstrumentRowsVisibleInsideFailedTransaction",fault.observedInstrumentRows,
                "realDocumentPublishedInsideFailedTransaction",fault.observedPublishedDocument,"noAmbientTestTransaction",true,
                "positiveControlInstrument",success,"excludedTechnicalEffects",before.exclusions(),
                "limits","Tests one explicit late exception after real writes plus existing domain-refusal tests. Does not claim HTTP TCP, Chrome or every possible infrastructure fault."));
    }

    @TestConfiguration(proxyBeanMethods=false)
    static class FaultConfiguration {
        @Bean LateFault a244LateFault(EntityManager em) {return new LateFault(em);}
    }
    @Aspect
    static class LateFault {
        private final EntityManager em;
        private Long contractId,documentId;private String factKey;private boolean armed;
        boolean observedActiveTransaction,observedPublishedDocument,flushedBeforeFailure;
        long observedInstrumentRows;
        LateFault(EntityManager em) {this.em=em;}
        void arm(long contract,String key,long document) {
            if(armed)throw new IllegalStateException("A244 fault already armed");
            contractId=contract;factKey=key;documentId=document;armed=true;
        }
        void disarm() {armed=false;}
        @Around("execution(* com.example.siggtrat.servico.DocumentoComprobatorioServico.publicarFontesDoFato(..))")
        Object afterActualPublication(ProceedingJoinPoint invocation) throws Throwable {
            Object result=invocation.proceed();Object[] args=invocation.getArgs();
            if(!armed || !(args[0] instanceof Contrato contract) || !contract.getId().equals(contractId)
                    || args[1]!=TipoFatoDocumental.ADITAMENTO || !factKey.equals(args[2]))return result;
            observedActiveTransaction=TransactionSynchronizationManager.isActualTransactionActive();
            if(!observedActiveTransaction)throw new AssertionError("A244 expected the real enclosing transaction");
            em.flush();flushedBeforeFailure=true;
            observedInstrumentRows=em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:c and a.identificacao=:i",Long.class)
                    .setParameter("c",contractId).setParameter("i",factKey).getSingleResult();
            observedPublishedDocument="PUBLICADO".equals(em.find(DocumentoComprobatorio.class,documentId).getEstado().name());
            throw new DeliberateA244Failure();
        }
    }
    static final class DeliberateA244Failure extends ResponseStatusException {
        DeliberateA244Failure(){super(HttpStatus.INTERNAL_SERVER_ERROR,"A244_TEST_ONLY_FAILURE_AFTER_REAL_WRITES");}
    }
}
