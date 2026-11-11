package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.repositorio.RegistroAuditoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Persistência isolada da transação de negócio, inclusive após seu commit. */
@Service
public class PersistenciaRegistroAuditoriaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PersistenciaRegistroAuditoriaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final RegistroAuditoriaRepositorio repositorio;

    public PersistenciaRegistroAuditoriaServico(RegistroAuditoriaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvar(RegistroAuditoria registro) {
        repositorio.saveAndFlush(registro);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarLote(List<RegistroAuditoria> registros) {
        repositorio.saveAllAndFlush(registros);
    }
}
