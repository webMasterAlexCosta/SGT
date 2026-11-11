package com.example.siggtrat.servico;

import com.example.siggtrat.dto.CalendarioEventoRequisicao;
import com.example.siggtrat.dto.CalendarioEventoDTO;
import com.example.siggtrat.dto.GestaoLocalDTO;
import com.example.siggtrat.dto.SuperintendenciaDTO;
import com.example.siggtrat.dto.VersaoCalendarioEventoDTO;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.siggtrat.excecao.ExcecaoConflitoDados;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@org.springframework.stereotype.Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CalendarioEventoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CalendarioEventoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final CalendarioEventoRepositorio repositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final EscopoAcessoUsuarioServico scopeServico;
    private VersaoCalendarioEventoRepositorio versoes;
    private ObjectMapper objectMapper;
    private RegistroAuditoriaServico auditoria;
    private InvalidacaoEmissaoRelatorioServico invalidacao;
    private RelatorioFrequenciaConsolidadoServico fontesRelatorio;
    private EntityManager entityManager;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequencias;

    @Autowired
    public void configurarRevalidacaoFrequencias(RevalidacaoFrequenciaTemporalServico revalidacaoFrequencias) {
        this.revalidacaoFrequencias = revalidacaoFrequencias;
    }

    @Autowired
    public void configurarHistoria(VersaoCalendarioEventoRepositorio versoes, ObjectMapper objectMapper,
            RegistroAuditoriaServico auditoria, InvalidacaoEmissaoRelatorioServico invalidacao,
            RelatorioFrequenciaConsolidadoServico fontesRelatorio, EntityManager entityManager) {
        this.versoes = versoes; this.objectMapper = objectMapper; this.auditoria = auditoria;
        this.invalidacao = invalidacao; this.fontesRelatorio = fontesRelatorio; this.entityManager = entityManager;
    }

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
            if (inicio.isAfter(fim)) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A data inicial não pode ser posterior à data final.");
            eventos = repositorio.findByDataBetween(inicio, fim);
        } else if (inicio != null || fim != null) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe as datas inicial e final para pesquisar um período.");
        } else eventos = repositorio.findAll();

        Lotacao lotacaoFiltro = lotacaoId == null ? null : lotacaoRepositorio.findById(lotacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada."));
        if (lotacaoFiltro != null && lotacaoFiltro.getGestaoLocal() != null
                && !acessoOrganizacionalServico.podeAcessarGestaoLocal(user, lotacaoFiltro.getGestaoLocal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lotação fora do escopo do usuário.");
        }

        return eventos.stream()
                .filter(evento -> !evento.isCancelado())
                .filter(evento -> podeVisualizar(user, evento))
                .filter(evento -> lotacaoFiltro == null || aplicaNaLotacao(evento, lotacaoFiltro))
                .sorted(Comparator.comparing(CalendarioEvento::getData).thenComparing(CalendarioEvento::getTipo))
                .map(this::paraDTO).toList();
    }
    public CalendarioEventoDTO criar(CalendarioEventoRequisicao dados) {
        if (dados == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe os dados do evento.");
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.podeGerenciarCalendario(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Somente a Gestão Local pode cadastrar o calendário do seu escopo.");
        JustificativaFonteFrequencia.validar(dados.justificativa(), false);
        CalendarioEvento evento = novoEvento(dados);
        aplicarEscopoDeEscrita(user, dados, evento);
        normalizarEvento(evento);
        bloquearFontes();
        acessoOrganizacionalServico.revalidarAposEspera(user);
        validarDuplicidade(evento, null);
        evento = repositorio.save(evento);
        if (evento.getId() != null) {
            entityManager.flush();
            registrarVersao(evento, "CRIACAO", user, dados.justificativa(), null);
            invalidacao.invalidarPorCalendario(null, evento, "Inclusão de fonte do calendário", user);
            revalidarDependentes(null, evento, "Inclusão da fonte de calendário " + evento.getId(), user);
        }
        return paraDTO(evento);
    }

    public CalendarioEventoDTO atualizar(Long id, CalendarioEventoRequisicao dados) {
        if (dados == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe os dados do evento.");
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirGestorCalendario(user);
        bloquearFontes();
        CalendarioEvento existente = obterParaAlteracao(id, user);
        CalendarioEvento anterior = reconstruir(paraDTO(existente));
        CalendarioEvento proposta = reconstruir(paraDTO(existente));
        proposta.setData(dados.data());
        proposta.setTipo(dados.tipo());
        proposta.setDescricao(dados.descricao());
        proposta.setAbrangencia(dados.abrangencia());
        proposta.setUf(dados.uf());
        proposta.setMunicipio(dados.municipio());
        if (dados.ativo() != null) proposta.setAtivo(dados.ativo());
        aplicarEscopoDeEscrita(user, dados, proposta);
        normalizarEvento(proposta);
        validarDuplicidade(proposta, id);
        String justificativa = normalizarTexto(dados.justificativa());
        boolean mesmaIntencaoDeclarada = justificativa == null || versoes
                .findTopByCalendarioEventoIdOrderByNumeroVersaoDesc(id)
                .map(v -> Objects.equals(justificativa, v.getJustificativa())).orElse(false);
        if (Objects.equals(paraDTO(anterior), paraDTO(proposta)) && mesmaIntencaoDeclarada)
            return paraDTO(existente);
        String motivo = JustificativaFonteFrequencia.validar(justificativa, true);
        preservarOriginalLegado(existente);
        copiarConteudo(proposta, existente);
        repositorio.save(existente);
        entityManager.flush();
        registrarVersao(existente, "RETIFICACAO", user, motivo, null);
        invalidacao.invalidarPorCalendario(anterior, existente, "Retificação de fonte do calendário", user);
        revalidarDependentes(anterior, existente, "Retificação da fonte de calendário " + id + ": " + motivo, user);
        return paraDTO(existente);
    }

    public void excluir(Long id) { excluir(id, null); }

    /** Mantém o contrato DELETE, conservando a raiz e acrescentando um cancelamento. */
    public void excluir(Long id, String justificativa) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirGestorCalendario(user);
        bloquearFontes();
        CalendarioEvento evento = obterParaAlteracao(id, user);
        justificativa = JustificativaFonteFrequencia.validar(justificativa, true);
        CalendarioEvento anterior = reconstruir(paraDTO(evento));
        preservarOriginalLegado(evento);
        evento.setCancelado(true);
        evento.setAtivo(false);
        repositorio.save(evento);
        entityManager.flush();
        registrarVersao(evento, "CANCELAMENTO", user, justificativa, null);
        invalidacao.invalidarPorCalendario(anterior, evento, "Cancelamento de fonte do calendário", user);
        revalidarDependentes(anterior, evento, "Cancelamento da fonte de calendário " + id + ": " + justificativa, user);
    }

    private void revalidarDependentes(CalendarioEvento anterior, CalendarioEvento atual,
            String motivo, UsuarioAplicacao usuario) {
        if (revalidacaoFrequencias != null)
            revalidacaoFrequencias.revalidarPorCalendario(anterior, atual, motivo, usuario);
    }

    @Transactional(readOnly = true)
    public List<CalendarioEventoDTO> listar(LocalDate data, LocalDate inicio, LocalDate fim,
            Long lotacaoId, Instant conhecidoAte) {
        if (conhecidoAte == null) return listar(data, inicio, fim, lotacaoId);
        validarPeriodo(data, inicio, fim);
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Lotacao lotacaoFiltro = lotacaoId == null ? null : lotacaoRepositorio.findById(lotacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada."));
        if (lotacaoFiltro != null && lotacaoFiltro.getGestaoLocal() != null
                && !acessoOrganizacionalServico.podeAcessarGestaoLocal(user, lotacaoFiltro.getGestaoLocal()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lotação fora do escopo do usuário.");
        List<VersaoCalendarioEvento> conhecidas = versoes.buscarUltimasConhecidas(conhecidoAte);
        Set<Long> fontesConhecidas = new HashSet<>();
        conhecidas.forEach(v -> fontesConhecidas.add(v.getCalendarioEvento().getId()));
        // O banco recebido não traz instante de publicação para fatos antigos.
        // Não se apresenta sua projeção atual como uma fotografia conhecida no passado.
        for (CalendarioEvento legado : repositorio.findAll()) {
            if (legado.getCriadoEm() != null || fontesConhecidas.contains(legado.getId()) || !podeVisualizar(user, legado)) continue;
            boolean recorte = noPeriodo(legado.getData(), data, inicio, fim)
                    || versoes.findByCalendarioEventoIdOrderByNumeroVersaoAsc(legado.getId()).stream()
                        .anyMatch(v -> noPeriodo(v.getDataEfetiva(), data, inicio, fim));
            if (recorte) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O calendário legado não possui evidência do conhecimento nesse instante. Consulte sua cadeia de regularização.");
        }
        return conhecidas.stream().map(this::fotografia)
                .filter(dto -> !dto.cancelado())
                .filter(dto -> noPeriodo(dto.data(), data, inicio, fim))
                .filter(dto -> podeVisualizar(user, reconstruir(dto)))
                .filter(dto -> lotacaoFiltro == null || aplicaNaLotacao(reconstruir(dto), lotacaoFiltro))
                .sorted(Comparator.comparing(CalendarioEventoDTO::data).thenComparing(CalendarioEventoDTO::tipo))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VersaoCalendarioEventoDTO> listarEventos(Long id, Instant conhecidoAte) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        CalendarioEvento raiz = repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário não encontrado."));
        List<VersaoCalendarioEvento> cadeia = versoes.findByCalendarioEventoIdOrderByNumeroVersaoAsc(id);
        validarCadeia(cadeia);
        List<VersaoCalendarioEventoDTO> visiveis = cadeia.stream()
                .filter(v -> conhecidoAte == null || !v.getRegistradoEm().isAfter(conhecidoAte))
                .filter(v -> podeVisualizar(user, reconstruir(fotografia(v))))
                .map(v -> new VersaoCalendarioEventoDTO(v.getId(), id, v.getNumeroVersao(), v.getTipoOperacao(),
                        v.getDataEfetiva(), v.getRegistradoEm(), v.getUsuarioId(), v.getUsuarioUsername(),
                        v.getJustificativa(), v.getMotivoTecnico(), v.getVersaoAnteriorId(), fotografia(v),
                        v.getHashAnterior(), v.getHashVersao())).toList();
        if (visiveis.isEmpty() && !podeVisualizar(user, raiz)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Evento do calendário não encontrado no seu escopo.");
        return visiveis;
    }

    private void bloquearFontes() {
        // Contrato → Gestão → fonte → emissão. A Gestão também existe quando
        // ainda não há Contratos, e serializa a identidade do calendário nesse caso.
        if (fontesRelatorio != null) fontesRelatorio.bloquearFontesParaEmissao(null);
        if (entityManager != null) {
            UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
            if (usuario != null && usuario.getGestaoLocal() != null) {
                GestaoLocal gestao = entityManager.find(GestaoLocal.class, usuario.getGestaoLocal().getId());
                if (gestao == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A Gestão Local deixou de estar disponível. Refaça a operação.");
                entityManager.refresh(gestao, LockModeType.PESSIMISTIC_WRITE);
            }
        }
    }

    private void exigirGestorCalendario(UsuarioAplicacao usuario) {
        if (!acessoOrganizacionalServico.podeGerenciarCalendario(usuario)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Somente a Gestão Local pode manter o calendário do seu escopo.");
    }

    private CalendarioEvento obterParaAlteracao(Long id, UsuarioAplicacao usuario) {
        CalendarioEvento evento = repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário não encontrado."));
        if (!podeAlterar(usuario, evento) || evento.isCancelado()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Evento do calendário não encontrado no seu escopo.");
        entityManager.refresh(evento, LockModeType.PESSIMISTIC_WRITE);
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        exigirGestorCalendario(usuario);
        if (!podeAlterar(usuario, evento)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Evento do calendário não encontrado no seu escopo.");
        if (evento.isCancelado()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento do calendário já cancelado.");
        return evento;
    }

    private void preservarOriginalLegado(CalendarioEvento evento) {
        if (versoes.findTopByCalendarioEventoIdOrderByNumeroVersaoDesc(evento.getId()).isPresent()) return;
        registrarVersao(evento, "INCORPORACAO_LEGADO", null, null, evento.getCriadoEm());
    }

    private void registrarVersao(CalendarioEvento evento, String operacao, UsuarioAplicacao usuario,
            String justificativa, Instant registroOriginalConhecido) {
        String declarada = JustificativaFonteFrequencia.validar(normalizarTexto(justificativa),
                "RETIFICACAO".equals(operacao) || "CANCELAMENTO".equals(operacao));
        List<VersaoCalendarioEvento> cadeia = versoes.findByCalendarioEventoIdOrderByNumeroVersaoAsc(evento.getId());
        validarCadeia(cadeia);
        VersaoCalendarioEvento anterior = cadeia.isEmpty() ? null : cadeia.get(cadeia.size() - 1);
        int numero = anterior == null ? 1 : anterior.getNumeroVersao() + 1;
        Instant registro = (registroOriginalConhecido == null ? Instant.now() : registroOriginalConhecido).truncatedTo(ChronoUnit.MICROS);
        String tecnico = switch (operacao) {
            case "CRIACAO" -> "Publicação de evento do calendário operacional.";
            case "RETIFICACAO" -> "Retificação versionada de evento do calendário operacional.";
            case "CANCELAMENTO" -> "Cancelamento lógico de evento do calendário operacional.";
            default -> "Preservação da projeção legada antes da primeira alteração versionada; autoria original não disponível.";
        };
        String foto = serializar(paraDTO(evento));
        Long usuarioId = usuario == null ? null : usuario.getId();
        String username = usuario == null ? null : usuario.getUsername();
        Long anteriorId = anterior == null ? null : anterior.getId();
        String hashAnterior = anterior == null ? null : anterior.getHashVersao();
        String hash = hash(evento.getId(), numero, operacao, evento.getData(), registro, usuarioId, username,
                declarada, tecnico, anteriorId, foto, hashAnterior);
        VersaoCalendarioEvento versao = versoes.save(new VersaoCalendarioEvento(evento, numero, operacao,
                evento.getData(), registro, usuarioId, username, declarada, tecnico, anteriorId, foto, hashAnterior, hash));
        auditoria.registrarAcaoComoUsuario("CALENDARIO_" + operacao, "CalendarioEvento", evento.getId(),
                evento.getDescricao(), RegistroAuditoriaServico.detalhes("dataEfetiva", evento.getData(),
                        "numeroVersao", numero, "versaoId", versao.getId(), "versaoAnteriorId", anteriorId,
                        "justificativaDeclarada", declarada, "motivoTecnico", tecnico,
                        "fotografiaAnterior", anterior == null ? null : anterior.getFotografiaJson(), "fotografiaNova", foto,
                        "hashVersao", hash), usuario);
    }

    private void validarCadeia(List<VersaoCalendarioEvento> cadeia) {
        VersaoCalendarioEvento anterior = null;
        for (VersaoCalendarioEvento v : cadeia) {
            String esperado = hash(v.getCalendarioEvento().getId(), v.getNumeroVersao(), v.getTipoOperacao(), v.getDataEfetiva(),
                    v.getRegistradoEm(), v.getUsuarioId(), v.getUsuarioUsername(), v.getJustificativa(), v.getMotivoTecnico(),
                    v.getVersaoAnteriorId(), v.getFotografiaJson(), v.getHashAnterior());
            if (v.getNumeroVersao() != (anterior == null ? 1 : anterior.getNumeroVersao() + 1)
                    || !Objects.equals(v.getVersaoAnteriorId(), anterior == null ? null : anterior.getId())
                    || !Objects.equals(v.getHashAnterior(), anterior == null ? null : anterior.getHashVersao())
                    || !Objects.equals(v.getHashVersao(), esperado))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A cadeia publicada do calendário perdeu integridade.");
            anterior = v;
        }
    }

    private String hash(Long id, int numero, String tipo, LocalDate data, Instant registro, Long usuarioId,
            String username, String justificativa, String tecnico, Long anteriorId, String foto, String hashAnterior) {
        return UtilitarioHashSeguranca.sha256Hexadecimal(serializar(Arrays.asList(id, numero, tipo, data,
                registro, usuarioId, username, justificativa, tecnico, anteriorId, foto, hashAnterior)));
    }

    private String serializar(Object valor) {
        try { return objectMapper.writeValueAsString(valor); }
        catch (JsonProcessingException erro) { throw new IllegalStateException("Falha ao fotografar calendário.", erro); }
    }

    private CalendarioEventoDTO fotografia(VersaoCalendarioEvento versao) {
        try { return objectMapper.readValue(versao.getFotografiaJson(), CalendarioEventoDTO.class); }
        catch (JsonProcessingException erro) { throw new IllegalStateException("Fotografia do calendário inválida.", erro); }
    }

    private CalendarioEvento reconstruir(CalendarioEventoDTO dto) {
        CalendarioEvento e = new CalendarioEvento(); e.setId(dto.id()); e.setData(dto.data()); e.setTipo(dto.tipo());
        e.setDescricao(dto.descricao()); e.setAbrangencia(dto.abrangencia()); e.setUf(dto.uf()); e.setMunicipio(dto.municipio());
        e.setAtivo(dto.ativo()); e.setCancelado(dto.cancelado()); e.setVersion(dto.version());
        if (dto.superintendencia() != null) {
            SuperintendenciaEstadual se = new SuperintendenciaEstadual(); se.setId(dto.superintendencia().id());
            se.setNome(dto.superintendencia().nome()); se.setUf(dto.superintendencia().uf()); se.setAtivo(dto.superintendencia().ativo());
            e.setSuperintendencia(se);
        }
        if (dto.gestaoLocal() != null) {
            GestaoLocal gl = new GestaoLocal(); gl.setId(dto.gestaoLocal().id()); gl.setNome(dto.gestaoLocal().nome());
            gl.setCodigo(dto.gestaoLocal().codigo()); gl.setAtivo(dto.gestaoLocal().ativo()); gl.setSuperintendencia(e.getSuperintendencia());
            e.setGestaoLocal(gl);
        }
        if (dto.lotacaoId() != null) { Lotacao l = new Lotacao(); l.setId(dto.lotacaoId()); e.setLotacao(l); }
        return e;
    }

    private void validarPeriodo(LocalDate data, LocalDate inicio, LocalDate fim) {
        if (data != null) return;
        if ((inicio == null) != (fim == null)) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe as datas inicial e final para pesquisar um período.");
        if (inicio != null && inicio.isAfter(fim)) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A data inicial não pode ser posterior à data final.");
    }
    private boolean noPeriodo(LocalDate valor, LocalDate data, LocalDate inicio, LocalDate fim) {
        if (data != null) return data.equals(valor);
        return (inicio == null || !valor.isBefore(inicio)) && (fim == null || !valor.isAfter(fim));
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
            if (existente == null || existente.isCancelado() || Objects.equals(existente.getId(), idAtual)) return false;
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
    private void copiarConteudo(CalendarioEvento origem, CalendarioEvento destino) {
        destino.setData(origem.getData()); destino.setTipo(origem.getTipo()); destino.setDescricao(origem.getDescricao());
        destino.setAbrangencia(origem.getAbrangencia()); destino.setUf(origem.getUf()); destino.setMunicipio(origem.getMunicipio());
        destino.setLotacao(origem.getLotacao()); destino.setGestaoLocal(origem.getGestaoLocal());
        destino.setSuperintendencia(origem.getSuperintendencia()); destino.setAtivo(origem.getAtivo());
        destino.setCancelado(origem.isCancelado());
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
                evento.getLotacaoId(), evento.getAtivo(), evento.getVersion(), evento.isCancelado());
    }
}
