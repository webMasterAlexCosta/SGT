package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import java.util.*;

import static com.example.siggtrat.dto.ProvaCampoHistoricoDTO.Estado.*;
import static com.example.siggtrat.dto.ProvaCampoHistoricoDTO.Visibilidade.*;

/** Operações de evidência: não consultam cadastro atual nem persistem uma projeção. */
final class ProvasCamposHistoricos {
    private ProvasCamposHistoricos() { }

    static ProvaCampoHistoricoDTO desconhecido() {
        return new ProvaCampoHistoricoDTO(NAO_COMPROVADO, DISPONIVEL, List.of());
    }
    static ProvaCampoHistoricoDTO restrito() { return new ProvaCampoHistoricoDTO(null, RESTRITA, List.of()); }
    static boolean conhecido(ProvaCampoHistoricoDTO p) {
        return p != null && p.visibilidade() == DISPONIVEL && p.estado() != null
                && p.estado() != NAO_COMPROVADO && !p.origens().isEmpty();
    }
    static ProvaCampoHistoricoDTO comprovado(Object valor, OrigemProvaCampoDTO fonte) {
        return fonte == null ? desconhecido() : new ProvaCampoHistoricoDTO(
                ausente(valor) ? AUSENCIA_COMPROVADA : VALOR_COMPROVADO, DISPONIVEL, List.of(fonte));
    }
    static ProvaCampoHistoricoDTO derivado(Object valor, ProvaCampoHistoricoDTO... premissas) {
        return derivado(valor, Arrays.asList(premissas));
    }
    static ProvaCampoHistoricoDTO derivado(Object valor, Collection<ProvaCampoHistoricoDTO> premissas) {
        if (premissas.isEmpty() || premissas.stream().anyMatch(p -> !conhecido(p))) return desconhecido();
        List<OrigemProvaCampoDTO> fontes = premissas.stream().flatMap(p -> p.origens().stream()).distinct().toList();
        return new ProvaCampoHistoricoDTO(ausente(valor) ? AUSENCIA_COMPROVADA : VALOR_COMPROVADO, DISPONIVEL, fontes);
    }
    static boolean ausente(Object valor) { return valor == null || valor instanceof String s && s.isBlank(); }
    static Map<String, ProvaCampoHistoricoDTO> desconhecidos(Collection<String> campos) {
        Map<String, ProvaCampoHistoricoDTO> resultado = new LinkedHashMap<>();
        campos.forEach(c -> resultado.put(c, desconhecido()));
        return resultado;
    }
    static OrigemProvaCampoDTO versao(VersaoContratoNegocialDTO v) {
        return v == null || v.id() == null ? null : new OrigemProvaCampoDTO(
                OrigemProvaCampoDTO.Tipo.FOTOGRAFIA_CONTRATUAL, null, v.eventoOrigemId(), v.id(),
                null, null, null, v.dataEfetiva(), v.registradoEm());
    }
    static OrigemProvaCampoDTO evento(EventoCicloVidaContrato e) {
        return e == null || e.getId() == null || e.getId() <= 0 ? null : new OrigemProvaCampoDTO(
                OrigemProvaCampoDTO.Tipo.EVENTO_CONTRATUAL, null, e.getId(), null, null, null, null,
                e.getDataEfetiva(), e.getRegistradoEm());
    }
    static OrigemProvaCampoDTO prova(ProvaHistoricaContrato p, Long composicao, Long item, Long mapa) {
        return new OrigemProvaCampoDTO(OrigemProvaCampoDTO.Tipo.PROVA_HISTORICA, p.getId(),
                p.getEventoOrigem().getId(), p.getVersaoGerada().getId(), composicao, item, mapa,
                p.getEventoOrigem().getDataEfetiva(), p.getRegistradoEm());
    }
    static OrigemProvaCampoDTO item(AditamentoComposicao item) {
        var evento = item.getAditamento().getEvento();
        return new OrigemProvaCampoDTO(OrigemProvaCampoDTO.Tipo.ITEM_ADITAMENTO, null,
                evento == null ? null : evento.getId(), null,
                item.getComposicaoBase() == null ? null : item.getComposicaoBase().getId(), item.getId(), null,
                item.getAditamento().getDataEfetiva(), evento == null ? null : evento.getRegistradoEm());
    }
    static OrigemProvaCampoDTO mapa(MapeamentoTransferenciaGestao m) {
        var evento = m.getTransferencia().getEvento();
        return new OrigemProvaCampoDTO(OrigemProvaCampoDTO.Tipo.MAPEAMENTO_TRANSFERENCIA, null,
                evento == null ? null : evento.getId(), null, null, null, m.getId(),
                evento == null ? null : evento.getDataEfetiva(), evento == null ? null : evento.getRegistradoEm());
    }
    static Map<String, Object> valores(VersaoContratoNegocialDTO f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dataEfetiva", f.dataEfetiva()); m.put("numeroContrato", f.numeroContrato());
        m.put("anoContrato", f.anoContrato()); m.put("descricao", f.descricao());
        m.put("tipoContratacaoId", f.tipoContratacaoId()); m.put("tipoContratacaoDescricao", f.tipoContratacaoDescricao());
        m.put("fornecedorId", f.fornecedorId()); m.put("cnpjEmpresa", f.cnpjEmpresa());
        m.put("razaoSocialEmpresa", f.razaoSocialEmpresa()); m.put("nomeFantasiaEmpresa", f.nomeFantasiaEmpresa());
        m.put("dataInicio", f.dataInicio()); m.put("dataFim", f.dataFim()); m.put("efetivoPrevisto", f.efetivoPrevisto());
        m.put("gestaoLocalId", f.gestaoLocalId()); m.put("gestaoLocalNome", f.gestaoLocalNome());
        m.put("matriculaVinculoObrigatoria", f.matriculaVinculoObrigatoria());
        m.put("matriculaTipoContratacaoObrigatoria", f.matriculaTipoContratacaoObrigatoria());
        m.put("matriculaVinculoObrigatoriaEfetiva", f.matriculaVinculoObrigatoriaEfetiva());
        m.put("diaSemanaReduzido", f.diaSemanaReduzido()); m.put("horasDiaReduzido", f.horasDiaReduzido());
        m.put("habilitacaoAdministrativa", f.habilitacaoAdministrativa());
        return m;
    }
    static VersaoContratoNegocialDTO fotografia(VersaoContratoNegocial v) {
        return new VersaoContratoNegocialDTO(v.getId(), v.getNumeroVersao(),
                v.getEventoOrigem() == null ? null : v.getEventoOrigem().getId(), v.getDataEfetiva(), v.getRegistradoEm(),
                v.getNumeroContrato(), v.getAnoContrato(), v.getDescricao(), v.getTipoContratacaoId(),
                v.getTipoContratacaoDescricao(), v.getFornecedorId(), v.getCnpjEmpresa(), v.getRazaoSocialEmpresa(),
                v.getNomeFantasiaEmpresa(), v.getDataInicio(), v.getDataFim(), v.getEfetivoPrevisto(),
                v.getGestaoLocalId(), v.getGestaoLocalNome(), v.getMatriculaVinculoObrigatoria(),
                v.getMatriculaTipoContratacaoObrigatoria(), v.getMatriculaVinculoObrigatoriaEfetiva(),
                v.getDiaSemanaReduzido(), v.getHorasDiaReduzido(), v.getHabilitacaoAdministrativa());
    }
}
