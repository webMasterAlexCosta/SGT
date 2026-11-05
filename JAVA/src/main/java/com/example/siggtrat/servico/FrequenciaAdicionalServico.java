package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.ContextoComplementoFrequenciaDTO;
import com.example.siggtrat.dto.FrequenciaAdicionalDTO;
import com.example.siggtrat.dto.VersaoComplementoFrequenciaDTO;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.FrequenciaAdicionalRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class FrequenciaAdicionalServico {
    private static final long MAX_RANGE_DAYS = 366;
    private final FrequenciaAdicionalRepositorio repositorio;
    private final UsuarioAplicacaoRepositorio usuarios;
    private final AcessoOrganizacionalServico acesso;
    private final ComplementoFrequenciaTemporalServico temporal;
    private final EntityManager em;

    public FrequenciaAdicionalServico(FrequenciaAdicionalRepositorio repositorio,
            UsuarioAplicacaoRepositorio usuarios, AcessoOrganizacionalServico acesso,
            ComplementoFrequenciaTemporalServico temporal, EntityManager em) {
        this.repositorio = repositorio; this.usuarios = usuarios; this.acesso = acesso;
        this.temporal = temporal; this.em = em;
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaAdicionalDTO> listar(LocalDate data, LocalDate inicio, LocalDate fim,
            String tipo, Long lotacaoId, Long jornadaId, Long contratoId, Pageable pageable) {
        boolean consultaDia = data != null, consultaRange = inicio != null || fim != null;
        if (!consultaDia && !consultaRange) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe data ou o período completo (início e fim).");
        if (consultaDia && consultaRange) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Use data ou período, não ambos.");
        if (consultaRange) {
            if (inicio == null || fim == null || inicio.isAfter(fim))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
            if (ChronoUnit.DAYS.between(inicio, fim) > MAX_RANGE_DAYS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período máximo para adicionais é de " + MAX_RANGE_DAYS + " dias.");
        } else { inicio = data; fim = data; }
        UsuarioAplicacao user = usuarioAtual();
        if (acesso.ehGlobal(user) && lotacaoId == null && jornadaId == null && contratoId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos lotação, turno ou contrato para consultar adicionais.");
        LocalDate desde = inicio, ate = fim;
        Specification<FrequenciaAdicional> spec = (root, query, cb) -> cb.and(
                cb.between(root.get("dataReferencia"), desde, ate),
                cb.notEqual(cb.upper(root.get("status")), ConstantesDominio.STATUS_CANCELADO));
        if (tipo != null && !tipo.isBlank()) {
            String normalizado = normalizar(tipo);
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("tipo")), normalizado));
        }
        Pageable limitado = UtilitarioPaginacao.limitar(pageable,
                Sort.by("dataReferencia").ascending().and(Sort.by("empregado.nome")));
        Sort ordem = limitado.getSort().getOrderFor("id") == null
                ? limitado.getSort().and(Sort.by("id")) : limitado.getSort();
        List<FrequenciaAdicionalDTO> selecionados = new ArrayList<>();
        long total = 0; int lote = 0;
        Page<FrequenciaAdicional> candidatos;
        do {
            candidatos = repositorio.findAll(spec, PageRequest.of(lote++, 200, ordem));
            for (FrequenciaAdicional a : candidatos) {
                var contextos = contexto(a);
                if (!temporal.podeConsultar(user, contextos)
                        || !temporal.atendeFiltros(contextos, a.getDataReferencia(), lotacaoId, jornadaId, contratoId)) continue;
                if (total >= limitado.getOffset() && selecionados.size() < limitado.getPageSize())
                    selecionados.add(paraConsulta(a, user));
                total++;
            }
            for (FrequenciaAdicional a : candidatos) if (em.contains(a)) em.detach(a);
        } while (candidatos.hasNext());
        return new PageImpl<>(selecionados, limitado, total);
    }

    public FrequenciaAdicionalDTO criar(FrequenciaAdicionalDTO dto) {
        UsuarioAplicacao user = usuarioAtual(); temporal.exigirGestor(user);
        if (dto == null || dto.empregadoId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do empregado obrigatório.");
        if (dto.dataReferencia() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data obrigatória.");
        if (dto.tipo() == null || dto.tipo().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo obrigatório.");
        String motivo = JustificativaFonteFrequencia.validar(dto.justificativa(), false);
        Empregado empregado = temporal.bloquearEmpregados(dto.empregadoId()).get(dto.empregadoId());
        acesso.revalidarAposEspera(user);
        var contextos = temporal.validarCriacao(user, empregado, dto.dataReferencia(), dto.dataReferencia());
        String tipo = normalizar(dto.tipo());
        validarDuplicidade(empregado.getId(), dto.dataReferencia(), tipo, null);
        FrequenciaAdicional obj = new FrequenciaAdicional();
        obj.setEmpregado(empregado);
        obj.setEmpregadoNomeFotografia(temporal.capturarNomeComprovado(empregado, contextos));
        obj.setOrigemManual(true);
        obj.setDataReferencia(dto.dataReferencia()); obj.setTipo(tipo);
        obj.setMinutos(dto.minutos()); obj.setQuantidade(dto.quantidade());
        obj.setStatus(dto.status() == null || dto.status().isBlank() ? ConstantesDominio.STATUS_CALCULADO : normalizar(dto.status()));
        obj.setJustificativa(motivo);
        obj = repositorio.saveAndFlush(obj);
        FrequenciaAdicionalDTO foto = paraDTO(obj);
        temporal.registrar(ComplementoFrequenciaTemporalServico.ADICIONAL, obj.getId(), "CRIACAO",
                obj.getDataReferencia(), foto, contextos, user, motivo);
        return foto;
    }

    public FrequenciaAdicionalDTO atualizar(Long id, FrequenciaAdicionalDTO dto) {
        FrequenciaAdicional obj = obter(id);
        UsuarioAplicacao user = usuarioAtual(); temporal.exigirGestor(user);
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do adicional.");
        Long origemId = obj.getEmpregado().getId();
        Long destinoId = dto.empregadoId() == null ? origemId : dto.empregadoId();
        var empregados = temporal.bloquearEmpregados(origemId, destinoId);
        bloquearFonte(obj, origemId);
        acesso.revalidarAposEspera(user);
        var contextosAnteriores = contexto(obj);
        temporal.exigirEscrita(user, contextosAnteriores);
        LocalDate dataAnterior = obj.getDataReferencia();
        LocalDate data = dto.dataReferencia() == null ? dataAnterior : dto.dataReferencia();
        Empregado destino = empregados.get(destinoId);
        boolean mesmoRecorte = origemId.equals(destinoId) && data.equals(dataAnterior);
        var contextos = mesmoRecorte ? contextosAnteriores : temporal.validarCriacao(user, destino, data, data);
        String tipo = dto.tipo() == null || dto.tipo().isBlank() ? obj.getTipo() : normalizar(dto.tipo());
        String status = dto.status() == null ? obj.getStatus() : normalizar(dto.status());
        if (ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(obj.getStatus())
                && !ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(status))
            contextos = temporal.validarCriacao(user, destino, data, data);
        validarDuplicidade(destinoId, data, tipo, id);
        FrequenciaAdicionalDTO antes = paraDTO(obj);
        String nome = mesmoRecorte ? obj.getEmpregadoNomeFotografia()
                : temporal.capturarNomeComprovado(destino, contextos);
        String justificativaNova = dto.justificativa() == null ? obj.getJustificativa()
                : JustificativaFonteFrequencia.validar(dto.justificativa(), false);
        FrequenciaAdicionalDTO depois = new FrequenciaAdicionalDTO(id, destinoId, nome, data, tipo,
                dto.minutos() == null ? obj.getMinutos() : dto.minutos(),
                dto.quantidade() == null || obj.getQuantidade() != null && dto.quantidade().compareTo(obj.getQuantidade()) == 0
                        ? obj.getQuantidade() : dto.quantidade(),
                status, justificativaNova,
                antes.frequenciaId(), antes.origem());
        if (temporal.mesmoConteudo(antes, depois)) return depois;
        String motivo = JustificativaFonteFrequencia.validar(dto.justificativa(), true);
        temporal.preservarLegado(ComplementoFrequenciaTemporalServico.ADICIONAL, id, dataAnterior, antes, contextosAnteriores);
        obj.setEmpregado(destino);
        obj.setEmpregadoNomeFotografia(nome);
        obj.setDataReferencia(data); obj.setTipo(tipo); obj.setStatus(status);
        if (dto.minutos() != null) obj.setMinutos(dto.minutos());
        if (dto.quantidade() != null && (obj.getQuantidade() == null || dto.quantidade().compareTo(obj.getQuantidade()) != 0))
            obj.setQuantidade(dto.quantidade());
        obj.setJustificativa(motivo);
        // Intervenção manual em um derivado passa a ser fonte manual, conservando o vínculo de proveniência.
        obj.setOrigemManual(true);
        repositorio.saveAndFlush(obj);
        depois = paraDTO(obj);
        temporal.registrar(ComplementoFrequenciaTemporalServico.ADICIONAL, id, "RETIFICACAO", data,
                depois, contextos, user, motivo);
        return depois;
    }

    public void excluir(Long id) { excluir(id, null); }

    public void excluir(Long id, String justificativa) {
        FrequenciaAdicional obj = obter(id);
        UsuarioAplicacao user = usuarioAtual(); temporal.exigirGestor(user);
        Long empregadoId = obj.getEmpregado().getId();
        temporal.bloquearEmpregados(empregadoId); bloquearFonte(obj, empregadoId);
        acesso.revalidarAposEspera(user);
        var contextos = contexto(obj); temporal.exigirEscrita(user, contextos);
        if (ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(obj.getStatus())) return;
        justificativa = JustificativaFonteFrequencia.validar(justificativa, true);
        temporal.preservarLegado(ComplementoFrequenciaTemporalServico.ADICIONAL, id, obj.getDataReferencia(), paraDTO(obj), contextos);
        obj.setOrigemManual(true); obj.setStatus(ConstantesDominio.STATUS_CANCELADO);
        repositorio.saveAndFlush(obj);
        temporal.registrar(ComplementoFrequenciaTemporalServico.ADICIONAL, id, "CANCELAMENTO", obj.getDataReferencia(),
                paraDTO(obj), contextos, user, justificativa);
    }

    @Transactional(readOnly = true)
    public List<VersaoComplementoFrequenciaDTO> listarEventos(Long id, Instant conhecidoAte) {
        FrequenciaAdicional obj = obter(id);
        return temporal.listarEventos(ComplementoFrequenciaTemporalServico.ADICIONAL, id, conhecidoAte, usuarioAtual(), contexto(obj));
    }

    private List<ContextoComplementoFrequenciaDTO> contexto(FrequenciaAdicional obj) {
        return temporal.resolver(ComplementoFrequenciaTemporalServico.ADICIONAL, obj.getId(), obj.getEmpregado(),
                obj.getDataReferencia(), obj.getDataReferencia());
    }
    private void bloquearFonte(FrequenciaAdicional obj, Long empregadoEsperado) {
        em.refresh(obj, LockModeType.PESSIMISTIC_WRITE);
        if (!Objects.equals(empregadoEsperado, obj.getEmpregado().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A fonte foi alterada por outro comando; recarregue antes de retificar.");
    }
    private void validarDuplicidade(Long empregadoId, LocalDate data, String tipo, Long ignorarId) {
        repositorio.findByEmpregadoIdAndDataReferenciaAndTipoIgnoreCase(empregadoId, data, tipo)
                .filter(a -> !Objects.equals(a.getId(), ignorarId)).ifPresent(a -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Já existe adicional desta combinação, inclusive se cancelado; retifique a fonte identificada.");
                });
    }
    private FrequenciaAdicional obter(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado."));
    }
    private UsuarioAplicacao usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarios.findByUsername(auth == null ? "" : auth.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado."));
    }
    private String normalizar(String valor) { return valor.trim().toUpperCase(Locale.ROOT); }
    private FrequenciaAdicionalDTO paraDTO(FrequenciaAdicional e) {
        return new FrequenciaAdicionalDTO(e.getId(), e.getEmpregado().getId(),
                e.getEmpregadoNomeFotografia(),
                e.getDataReferencia(), e.getTipo(), e.getMinutos(), e.getQuantidade(), e.getStatus(), e.getJustificativa(),
                e.getFrequencia() == null ? null : e.getFrequencia().getId(), e.isFonteManual() ? "MANUAL" : "DERIVADO");
    }
    private FrequenciaAdicionalDTO paraConsulta(FrequenciaAdicional e, UsuarioAplicacao user) {
        FrequenciaAdicionalDTO foto = paraDTO(e);
        return new FrequenciaAdicionalDTO(foto.id(), foto.empregadoId(), temporal.nomeParaConsulta(foto.empregadoNome(), user), foto.dataReferencia(),
                foto.tipo(), foto.minutos(), foto.quantidade(), foto.status(),
                temporal.narrativaParaConsulta(foto.justificativa(), user), foto.frequenciaId(), foto.origem());
    }
}
