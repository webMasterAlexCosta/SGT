package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.repositorio.RegistroAuditoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Persistência isolada da transação de negócio, inclusive após seu commit. */
@Service
public class PersistenciaRegistroAuditoriaServico {
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
