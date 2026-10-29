package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.EventoDisponibilidadeCatalogoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class DisponibilidadeCatalogoTemporalServico {
    private final EventoDisponibilidadeCatalogoRepositorio repositorio;
    public DisponibilidadeCatalogoTemporalServico(EventoDisponibilidadeCatalogoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void registrar(TipoCatalogoTemporal tipo, Long catalogoId, boolean ativo,
                          String motivo, UsuarioAplicacao responsavel) {
        EventoDisponibilidadeCatalogo evento = new EventoDisponibilidadeCatalogo();
        evento.setTipoCatalogo(tipo); evento.setCatalogoId(catalogoId);
        evento.setDataEfetiva(RelogioSistema.hoje()); evento.setAtivo(ativo);
        evento.setMotivo(motivo == null || motivo.isBlank() ? "Alteração administrativa do catálogo" : motivo);
        evento.setResponsavelId(responsavel == null ? null : responsavel.getId());
        evento.setResponsavelUsername(responsavel == null ? null : responsavel.getUsername());
        repositorio.save(evento);
    }

    @Transactional(readOnly = true)
    public boolean ativoEm(TipoCatalogoTemporal tipo, Long catalogoId, LocalDate dataEfetiva,
                           Boolean estadoAtualSemHistorico) {
        if (tipo == null || catalogoId == null || dataEfetiva == null) return false;
        return repositorio
                .findByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
                        tipo, catalogoId, dataEfetiva)
                .stream().findFirst().map(EventoDisponibilidadeCatalogo::getAtivo)
                .orElse(Boolean.TRUE.equals(estadoAtualSemHistorico));
    }
}
