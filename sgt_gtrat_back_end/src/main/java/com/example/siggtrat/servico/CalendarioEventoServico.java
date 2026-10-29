package com.example.siggtrat.servico;

import com.example.siggtrat.dto.CalendarioEventoRequisicao;
import com.example.siggtrat.dto.CalendarioEventoDTO;
import com.example.siggtrat.dto.GestaoLocalDTO;
import com.example.siggtrat.dto.SuperintendenciaDTO;
import com.example.siggtrat.excecao.ExcecaoConflitoDados;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@org.springframework.stereotype.Service
@Transactional
public class CalendarioEventoServico {

    private final CalendarioEventoRepositorio repositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final EscopoAcessoUsuarioServico scopeServico;

    public CalendarioEventoServico(CalendarioEventoRepositorio repositorio,
                                      LotacaoRepositorio lotacaoRepositorio,
                                      AcessoOrganizacionalServico acessoOrganizacionalServico,
                                      EscopoAcessoUsuarioServico scopeServico) {
        this.repositorio = repositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.scopeServico = scopeServico;
    }
    public List<CalendarioEventoDTO> listar(
            LocalDate data,
            LocalDate inicio,
            LocalDate fim,
            Long lotacaoId) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        List<CalendarioEvento> eventos;
        if (data != null) eventos = repositorio.findByData(data);
        else if (inicio != null && fim != null) {
            if (inicio.isAfter(fim)) throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
            eventos = repositorio.findByDataBetween(inicio, fim);
        } else if (inicio != null || fim != null) {
            throw new IllegalArgumentException("Informe as datas inicial e final para pesquisar um período.");
        } else eventos = repositorio.findAll();

        Lotacao lotacaoFiltro = lotacaoId == null ? null : lotacaoRepositorio.findById(lotacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada."));
        if (lotacaoFiltro != null && lotacaoFiltro.getGestaoLocal() != null
                && !acessoOrganizacionalServico.podeAcessarGestaoLocal(user, lotacaoFiltro.getGestaoLocal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lotação fora do escopo do usuário.");
        }

        return eventos.stream()
                .filter(evento -> podeVisualizar(user, evento))
                .filter(evento -> lotacaoFiltro == null || aplicaNaLotacao(evento, lotacaoFiltro))
                .sorted(Comparator.comparing(CalendarioEvento::getData).thenComparing(CalendarioEvento::getTipo))
                .map(this::paraDTO).toList();
    }
    public CalendarioEventoDTO criar(CalendarioEventoRequisicao dados) {
        if (dados == null) throw new IllegalArgumentException("Informe os dados do evento.");
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.podeGerenciarCalendario(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente a Gestão Local pode cadastrar o calendário do seu escopo.");
        CalendarioEvento evento = novoEvento(dados);
        aplicarEscopoDeEscrita(user, dados, evento);
        normalizarEvento(evento);
        validarDuplicidade(evento, null);
        return paraDTO(repositorio.save(evento));
    }
    public CalendarioEventoDTO atualizar(Long id, CalendarioEventoRequisicao dados) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        CalendarioEvento existente = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário não encontrado."));
        if (!podeAlterar(user, existente)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário não encontrado no seu escopo.");

        existente.setData(dados.data());
        existente.setTipo(dados.tipo());
        existente.setDescricao(dados.descricao());
        existente.setAbrangencia(dados.abrangencia());
        existente.setUf(dados.uf());
        existente.setMunicipio(dados.municipio());
        if (dados.ativo() != null) existente.setAtivo(dados.ativo());
        aplicarEscopoDeEscrita(user, dados, existente);
        normalizarEvento(existente);
        validarDuplicidade(existente, id);
        return paraDTO(repositorio.save(existente));
    }
    public void excluir(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        CalendarioEvento evento = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário não encontrado."));
        if (!podeAlterar(user, evento)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário não encontrado no seu escopo.");
        repositorio.delete(evento);
    }

    private boolean aplicaNaLotacao(CalendarioEvento evento, Lotacao lotacao) {
        if (evento.getSuperintendencia() == null && evento.getGestaoLocal() == null) return true;
        if (evento.getSuperintendencia() != null) {
            Long seLotacao = lotacao.getGestaoLocal() != null && lotacao.getGestaoLocal().getSuperintendencia() != null
                    ? lotacao.getGestaoLocal().getSuperintendencia().getId() : null;
            if (!Objects.equals(evento.getSuperintendencia().getId(), seLotacao)) return false;
        }
        if (evento.getGestaoLocal() != null
                && (lotacao.getGestaoLocal() == null || !Objects.equals(evento.getGestaoLocal().getId(), lotacao.getGestaoLocal().getId()))) {
            return false;
        }

        String abrangencia = normalizarMaiusculo(evento.getAbrangencia());
        if ("LOTACAO".equals(abrangencia)) return Objects.equals(evento.getLotacaoId(), lotacao.getId());
        if ("MUNICIPAL".equals(abrangencia)) {
            return Objects.equals(normalizarMaiusculo(evento.getMunicipio()), normalizarMaiusculo(lotacao.getMunicipio()));
        }
        return true;
    }

    private boolean podeVisualizar(UsuarioAplicacao user, CalendarioEvento evento) {
        if (acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(user)) return true;

        // Eventos nacionais não carregam vínculo regional e afetam todos os escopos.
        if (evento.getSuperintendencia() == null && evento.getGestaoLocal() == null) return true;

        SuperintendenciaEstadual seUsuario = acessoOrganizacionalServico.superintendenciaEfetiva(user);
        if (evento.getSuperintendencia() != null
                && (seUsuario == null || !Objects.equals(seUsuario.getId(), evento.getSuperintendencia().getId()))) {
            return false;
        }
        if (evento.getGestaoLocal() != null && !acessoOrganizacionalServico.podeAcessarGestaoLocal(user, evento.getGestaoLocal())) {
            return false;
        }

        // Perfis nacionais já foram tratados acima. SE e Gestão Local consultam todo o
        // calendário regional dentro do próprio escopo de gestão.
        if (acessoOrganizacionalServico.ehGestorEstadual(user) || acessoOrganizacionalServico.ehGestaoLocal(user)) return true;
        if (!acessoOrganizacionalServico.ehOperacional(user) || user.getGestaoLocal() == null) return false;

        String abrangencia = normalizarMaiusculo(evento.getAbrangencia());
        if (abrangencia == null || "ESTADUAL".equals(abrangencia)) return true;
        if ("LOTACAO".equals(abrangencia)) {
            return evento.getLotacaoId() != null && scopeServico.obterIdsLotacoes(user).contains(evento.getLotacaoId());
        }
        if ("MUNICIPAL".equals(abrangencia)) {
            String municipioEvento = normalizarMaiusculo(evento.getMunicipio());
            if (municipioEvento == null) return false;
            Set<Long> lotacaoIds = scopeServico.obterIdsLotacoes(user);
            if (lotacaoIds.isEmpty()) return false;
            return lotacaoRepositorio.findAllById(lotacaoIds).stream()
                    .map(Lotacao::getMunicipio)
                    .map(this::normalizarMaiusculo)
                    .anyMatch(municipioEvento::equals);
        }
        return false;
    }

    private boolean podeAlterar(UsuarioAplicacao user, CalendarioEvento evento) {
        return acessoOrganizacionalServico.ehGestaoLocal(user) && evento.getGestaoLocal() != null && acessoOrganizacionalServico.podeAcessarGestaoLocal(user, evento.getGestaoLocal());
    }

    private CalendarioEvento novoEvento(CalendarioEventoRequisicao dados) {
        CalendarioEvento evento = new CalendarioEvento();
        evento.setData(dados.data()); evento.setTipo(dados.tipo()); evento.setDescricao(dados.descricao());
        evento.setAbrangencia(dados.abrangencia()); evento.setUf(dados.uf()); evento.setMunicipio(dados.municipio());
        evento.setAtivo(dados.ativo() == null || dados.ativo());
        return evento;
    }

    private void aplicarEscopoDeEscrita(UsuarioAplicacao user, CalendarioEventoRequisicao dados, CalendarioEvento evento) {
        if (!acessoOrganizacionalServico.ehGestaoLocal(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente a Gestão Local pode manter o calendário operacional.");
        }
        String abrangencia = normalizarMaiusculo(dados.abrangencia());
        if (abrangencia == null) abrangencia = "ESTADUAL";
        GestaoLocal gestaoLocal = user.getGestaoLocal();
        if (gestaoLocal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário Gestão Local sem vínculo organizacional.");
        }
        evento.setGestaoLocal(gestaoLocal);
        evento.setSuperintendencia(gestaoLocal.getSuperintendencia());
        evento.setUf(gestaoLocal.getSuperintendencia() != null
                ? gestaoLocal.getSuperintendencia().getUf() : null);
        if ("NACIONAL".equals(abrangencia)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A Gestão Local não pode criar evento nacional.");
        }

        evento.setLotacao(null);
        if (dados.lotacaoId() != null) {
            Lotacao lotacao = lotacaoRepositorio.findById(dados.lotacaoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lotação inválida."));
            if (evento.getGestaoLocal() == null || lotacao.getGestaoLocal() == null || !Objects.equals(evento.getGestaoLocal().getId(), lotacao.getGestaoLocal().getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lotação não pertence à Gestão Local do evento.");
            evento.setLotacao(lotacao);
            evento.setMunicipio(lotacao.getMunicipio());
            abrangencia = "LOTACAO";
        } else if (evento.getMunicipio() != null && !evento.getMunicipio().isBlank()) {
            if (evento.getGestaoLocal() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Eventos municipais precisam estar vinculados a uma Gestão Local.");
            }
            String municipioInformado = normalizarMaiusculo(evento.getMunicipio());
            boolean municipioPertenceAoEscopo = lotacaoRepositorio
                    .findByGestaoLocalIdOrderByNomeAsc(evento.getGestaoLocal().getId())
                    .stream()
                    .map(Lotacao::getMunicipio)
                    .map(this::normalizarMaiusculo)
                    .anyMatch(municipioInformado::equals);
            if (!municipioPertenceAoEscopo) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O município informado não possui lotação cadastrada na Gestão Local do evento.");
            }
            abrangencia = "MUNICIPAL";
        } else if (!"NACIONAL".equals(abrangencia)) {
            abrangencia = "ESTADUAL";
        }
        evento.setAbrangencia(abrangencia);
    }

    private void validarDuplicidade(CalendarioEvento evento, Long idAtual) {
        String tipo = normalizarMaiusculo(evento.getTipo());
        String abrangencia = normalizarMaiusculo(evento.getAbrangencia());
        String municipio = normalizarMaiusculo(evento.getMunicipio());
        Long gestaoLocalId = evento.getGestaoLocal() != null ? evento.getGestaoLocal().getId() : null;
        Long seId = evento.getSuperintendencia() != null ? evento.getSuperintendencia().getId() : null;

        boolean duplicado = repositorio.findByData(evento.getData()).stream().anyMatch(existente -> {
            if (existente == null || Objects.equals(existente.getId(), idAtual)) return false;
            if (!Objects.equals(tipo, normalizarMaiusculo(existente.getTipo()))) return false;
            if (!Objects.equals(abrangencia, normalizarMaiusculo(existente.getAbrangencia()))) return false;

            Long existenteGestaoLocalId = existente.getGestaoLocal() != null ? existente.getGestaoLocal().getId() : null;
            Long existenteSeId = existente.getSuperintendencia() != null ? existente.getSuperintendencia().getId() : null;
            return Objects.equals(gestaoLocalId, existenteGestaoLocalId)
                    && Objects.equals(seId, existenteSeId)
                    && Objects.equals(evento.getLotacaoId(), existente.getLotacaoId())
                    && Objects.equals(municipio, normalizarMaiusculo(existente.getMunicipio()));
        });
        if (duplicado) throw new ExcecaoConflitoDados("Já existe um evento deste tipo e escopo na data informada.");
    }

    private void normalizarEvento(CalendarioEvento evento) {
        evento.setTipo(normalizarMaiusculo(evento.getTipo()));
        evento.setDescricao(normalizarTexto(evento.getDescricao()));
        evento.setAbrangencia(normalizarMaiusculo(evento.getAbrangencia()));
        evento.setUf(normalizarMaiusculo(evento.getUf()));
        evento.setMunicipio(normalizarTexto(evento.getMunicipio()));
        if (evento.getAtivo() == null) evento.setAtivo(true);
    }
    private String normalizarTexto(String valor) { if (valor == null) return null; String v = valor.trim().replaceAll("\\s+", " "); return v.isEmpty() ? null : v; }
    private String normalizarMaiusculo(String valor) { String v = normalizarTexto(valor); return v == null ? null : v.toUpperCase(Locale.ROOT); }

    private CalendarioEventoDTO paraDTO(CalendarioEvento evento) {
        SuperintendenciaEstadual se = evento.getSuperintendencia();
        GestaoLocal gestaoLocal = evento.getGestaoLocal();
        SuperintendenciaDTO seDto = se == null ? null
                : new SuperintendenciaDTO(se.getId(), se.getNome(), se.getUf(), se.getAtivo());
        GestaoLocalDTO gestaoLocalDto = gestaoLocal == null ? null
                : new GestaoLocalDTO(gestaoLocal.getId(), gestaoLocal.getNome(), gestaoLocal.getCodigo(), gestaoLocal.getAtivo(),
                se == null ? null : se.getId(), se == null ? null : se.getNome(), se == null ? null : se.getUf());
        return new CalendarioEventoDTO(evento.getId(), evento.getData(), evento.getTipo(), evento.getDescricao(),
                evento.getAbrangencia(), evento.getUf(), evento.getMunicipio(), seDto, gestaoLocalDto,
                evento.getLotacaoId(), evento.getAtivo(), evento.getVersion());
    }
}
