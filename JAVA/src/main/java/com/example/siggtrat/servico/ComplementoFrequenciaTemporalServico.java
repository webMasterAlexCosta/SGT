package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.dto.ContextoComplementoFrequenciaDTO;
import com.example.siggtrat.dto.VersaoComplementoFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Política única de autorização temporal e prova das fontes manuais de frequência. */
@Service
public class ComplementoFrequenciaTemporalServico {
    public static final String EXCECAO = "EXCECAO";
    public static final String ADICIONAL = "ADICIONAL";
    private final ResolvedorContextoTemporalFrequenciaServico operacional;
    private final PessoaRepositorio pessoas;
    private final VinculoContratualRepositorio vinculos;
    private final ContratoRepositorio contratos;
    private final EmpregadoRepositorio empregados;
    private final FrequenciaRepositorio frequencias;
    private final MotorTemporalVinculoServico motorVinculo;
    private final MotorTemporalAlocacaoServico motorAlocacao;
    private final GestaoContratoTemporalServico gestoes;
    private final EscopoAcessoUsuarioServico escopos;
    private final VersaoComplementoFrequenciaRepositorio versoes;
    private final ObjectMapper mapper;
    private final RegistroAuditoriaServico auditoria;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final EntityManager em;
    private final ResolvedorEscopoProvaFrequenciaServico escopoProva;
    private final IdentidadeHistoricaServico identidadeHistorica;

    public ComplementoFrequenciaTemporalServico(ResolvedorContextoTemporalFrequenciaServico operacional,
            PessoaRepositorio pessoas, VinculoContratualRepositorio vinculos, ContratoRepositorio contratos,
            EmpregadoRepositorio empregados, FrequenciaRepositorio frequencias, MotorTemporalVinculoServico motorVinculo,
            MotorTemporalAlocacaoServico motorAlocacao, GestaoContratoTemporalServico gestoes,
            EscopoAcessoUsuarioServico escopos, VersaoComplementoFrequenciaRepositorio versoes,
            ObjectMapper mapper, RegistroAuditoriaServico auditoria, MinimizadorDadosPessoaisServico minimizador,
            EntityManager em, ResolvedorEscopoProvaFrequenciaServico escopoProva,
            IdentidadeHistoricaServico identidadeHistorica) {
        this.operacional = operacional; this.pessoas = pessoas; this.vinculos = vinculos; this.contratos = contratos;
        this.empregados = empregados; this.frequencias = frequencias; this.motorVinculo = motorVinculo;
        this.motorAlocacao = motorAlocacao; this.gestoes = gestoes; this.escopos = escopos; this.versoes = versoes;
        this.mapper = mapper; this.auditoria = auditoria; this.minimizador = minimizador; this.em = em;
        this.escopoProva = escopoProva;
        this.identidadeHistorica = identidadeHistorica;
    }

    /** Contrato antes de Empregado/fonte, como nos demais comandos de frequência. */
    public Map<Long, Empregado> bloquearEmpregados(Long... ids) {
        SortedSet<Long> empregadosIds = new TreeSet<>();
        for (Long id : ids) {
            if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do empregado obrigatório.");
            empregadosIds.add(id);
        }
        SortedSet<Long> contratosIds = new TreeSet<>();
        for (Long id : empregadosIds) {
            Empregado empregado = empregados.findById(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empregado não encontrado."));
            for (VinculoContratual v : candidatos(empregado))
                if (v.getContrato() != null) contratosIds.add(v.getContrato().getId());
        }
        for (Long id : contratosIds) {
            Contrato contrato = contratos.buscarPorIdParaAtualizacao(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Contrato da fonte histórica não encontrado."));
            em.refresh(contrato, LockModeType.PESSIMISTIC_WRITE);
        }
        Map<Long, Empregado> resultado = new LinkedHashMap<>();
        for (Long id : empregadosIds) resultado.put(id, empregados.buscarPorIdParaAtualizacao(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empregado não encontrado.")));
        return resultado;
    }

    public void exigirGestor(UsuarioAplicacao usuario) {
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActive())
                || usuario.getPerfil() != PerfilAcesso.GESTOR_LOCAL) negarEscrita();
    }

    public List<ContextoComplementoFrequenciaDTO> validarCriacao(UsuarioAplicacao usuario, Empregado empregado,
            LocalDate inicio, LocalDate fim) {
        exigirGestor(usuario);
        validarPeriodo(inicio, fim);
        List<ContextoComplementoFrequenciaDTO> resultado = new ArrayList<>();
        for (LocalDate data = inicio; ; data = data.plusDays(1)) {
            var contexto = operacional.resolver(empregado, data);
            var a = contexto.posicaoAlocacao(); var g = contexto.gestaoHistorica();
            resultado.add(new ContextoComplementoFrequenciaDTO(data, contexto.vinculo().getContrato().getId(),
                    contexto.vinculo().getId(), a.alocacaoId(), g.getId(), g.getSuperintendencia().getId(),
                    a.lotacaoId(), a.jornadaId()));
            if (data.equals(fim)) break;
        }
        exigirEscrita(usuario, resultado);
        return List.copyOf(resultado);
    }

    /**
     * Uma fotografia única só afirma o nome quando todos os dias do recorte o
     * comprovam. A identidade é ancorada no Vínculo; o cadastro atual não é fonte
     * de período passado. Alterações sem novo recorte preservam a foto anterior.
     */
    public String capturarNomeComprovado(Empregado empregado,
                                        List<ContextoComplementoFrequenciaDTO> contextos) {
        Pessoa pessoa = empregado == null ? null : empregado.getPessoaCanonica();
        if (pessoa == null || contextos == null || contextos.isEmpty()
                || contextos.stream().anyMatch(c -> c.vinculoId() == null || c.dataReferencia() == null))
            return null;
        Map<Long, List<ContextoComplementoFrequenciaDTO>> porVinculo = contextos.stream()
                .collect(java.util.stream.Collectors.groupingBy(ContextoComplementoFrequenciaDTO::vinculoId));
        Map<Long, VinculoContratual> ancoras = vinculos.findAllById(porVinculo.keySet()).stream()
                .collect(java.util.stream.Collectors.toMap(VinculoContratual::getId, v -> v));
        Instant conhecimento = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        LocalDate hoje = com.example.siggtrat.utilitario.RelogioSistema.hoje();
        String nome = null;
        for (var entrada : porVinculo.entrySet()) {
            VinculoContratual vinculo = ancoras.get(entrada.getKey());
            if (vinculo == null) return null;
            LocalDate inicio = entrada.getValue().stream().map(ContextoComplementoFrequenciaDTO::dataReferencia)
                    .min(LocalDate::compareTo).orElseThrow();
            LocalDate fim = entrada.getValue().stream().map(ContextoComplementoFrequenciaDTO::dataReferencia)
                    .max(LocalDate::compareTo).orElseThrow();
            var historicas = identidadeHistorica.resolverPeriodo(pessoa, vinculo, inicio, fim, conhecimento);
            for (var contexto : entrada.getValue()) {
                LocalDate data = contexto.dataReferencia();
                var identidade = data.isBefore(hoje) ? historicas.get(data)
                        : identidadeHistorica.capturarNovoFato(pessoa, vinculo, data, conhecimento);
                String comprovado = identidade == null ? null : identidade.nome();
                if (comprovado == null || comprovado.isBlank()
                        || nome != null && !Objects.equals(nome, comprovado)) return null;
                nome = comprovado;
            }
        }
        return nome;
    }

    /** A prova histórica não exige que o fato continue válido para a operação de hoje. */
    public List<ContextoComplementoFrequenciaDTO> resolver(String tipo, Long id, Empregado empregado,
            LocalDate inicio, LocalDate fim) {
        if (empregado == null || inicio == null || fim == null || inicio.isAfter(fim)) return List.of();
        List<VinculoContratual> candidatos = candidatos(empregado);
        Map<LocalDate, ContextoComplementoFrequenciaDTO> fotos = new HashMap<>();
        if (id != null) versoes.findTopByTipoComplementoAndComplementoIdOrderByNumeroVersaoDesc(tipo, id)
                .ifPresent(v -> contextos(v).forEach(c -> fotos.put(c.dataReferencia(), c)));
        Instant conhecimento = Instant.now();
        List<ContextoComplementoFrequenciaDTO> resultado = new ArrayList<>();
        for (LocalDate data = inicio; ; data = data.plusDays(1)) {
            ContextoComplementoFrequenciaDTO foto = fotos.get(data);
            if (foto == null) {
                LocalDate dia = data;
                foto = frequencias.findByEmpregadoIdAndDataReferencia(empregado.getId(), data)
                        .filter(f -> f.getContratoSnapshotId() != null && f.getGestaoLocalSnapshotId() != null
                                && f.getSuperintendenciaSnapshotId() != null && f.getLotacaoSnapshotId() != null
                                && f.getJornadaSnapshotId() != null)
                        .map(f -> new ContextoComplementoFrequenciaDTO(dia, f.getContratoSnapshotId(),
                                f.getVinculoContratual() == null ? null : f.getVinculoContratual().getId(),
                                f.getAlocacaoVinculo() == null ? null : f.getAlocacaoVinculo().getId(),
                                f.getGestaoLocalSnapshotId(), f.getSuperintendenciaSnapshotId(),
                                f.getLotacaoSnapshotId(), f.getJornadaSnapshotId())).orElse(null);
            }
            // A prova ancora a identidade original. Não a migra para uma recontratação
            // encontrada por busca livre depois de cancelamento/retificação do vínculo.
            ContextoComplementoFrequenciaDTO contexto = foto == null
                    ? resolverCanonico(candidatos, data, conhecimento) : escopoProva.realinhar(foto, conhecimento);
            if (contexto == null) return List.of();
            resultado.add(contexto);
            if (data.equals(fim)) break;
        }
        return List.copyOf(resultado);
    }

    private ContextoComplementoFrequenciaDTO resolverCanonico(List<VinculoContratual> candidatos,
            LocalDate data, Instant conhecimento) {
        ContextoComplementoFrequenciaDTO resultado = null;
        for (VinculoContratual v : candidatos) {
            if (v.getContrato() == null || motorVinculo.reconstruir(v, data, conhecimento).situacaoTemporal()
                    != SituacaoTemporalVinculo.VIGENTE) continue;
            var a = motorAlocacao.reconstruir(v, data, conhecimento);
            if (a.situacaoTemporal() != SituacaoTemporalAlocacao.VIGENTE || a.alocacaoId() == null) continue;
            GestaoLocal g = gestoes.gestaoEm(v.getContrato(), data, conhecimento);
            if (g == null || g.getSuperintendencia() == null || a.lotacaoId() == null || a.jornadaId() == null) continue;
            if (resultado != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mais de um contexto temporal foi encontrado para o complemento de frequência.");
            resultado = new ContextoComplementoFrequenciaDTO(data, v.getContrato().getId(), v.getId(), a.alocacaoId(),
                    g.getId(), g.getSuperintendencia().getId(), a.lotacaoId(), a.jornadaId());
        }
        return resultado;
    }

    private List<VinculoContratual> candidatos(Empregado empregado) {
        Pessoa pessoa = empregado.getPessoaCanonica();
        // CPF é dado corrigível, não prova de identidade entre raízes. Fontes
        // sem associação só podem usar sua própria fotografia comprovada.
        return pessoa == null ? List.of() : vinculos.findByPessoaIdOrderByDataInicioAscIdAsc(pessoa.getId());
    }

    public boolean podeConsultar(UsuarioAplicacao usuario, List<ContextoComplementoFrequenciaDTO> contextos) {
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActive()) || contextos.isEmpty()) return false;
        return contextos.stream().allMatch(c -> {
            if (usuario.getPerfil() == PerfilAcesso.ADMINISTRADOR || usuario.getPerfil() == PerfilAcesso.GESTOR_NACIONAL
                    || usuario.getPerfil() == PerfilAcesso.PRESIDENTE)
                return true;
            if (usuario.getPerfil() == PerfilAcesso.GESTOR_ESTADUAL)
                return usuario.getSuperintendencia() != null
                        && Objects.equals(usuario.getSuperintendencia().getId(), c.superintendenciaId());
            return (usuario.getPerfil() == PerfilAcesso.GESTOR_LOCAL || usuario.getPerfil() == PerfilAcesso.OPERACIONAL)
                    && escopos.podeGerenciar(usuario, c.gestaoLocalId(), c.lotacaoId(), c.jornadaId());
        });
    }

    public void exigirEscrita(UsuarioAplicacao usuario, List<ContextoComplementoFrequenciaDTO> contextos) {
        exigirGestor(usuario);
        if (!podeConsultar(usuario, contextos)) negarEscrita();
    }

    public boolean atendeFiltros(List<ContextoComplementoFrequenciaDTO> contextos, LocalDate data,
            Long lotacaoId, Long jornadaId, Long contratoId) {
        return contextos.stream().filter(c -> c.dataReferencia().equals(data)).anyMatch(c ->
                (lotacaoId == null || Objects.equals(lotacaoId, c.lotacaoId()))
                && (jornadaId == null || Objects.equals(jornadaId, c.jornadaId()))
                && (contratoId == null || Objects.equals(contratoId, c.contratoId())));
    }

    public void preservarLegado(String tipo, Long id, LocalDate data, Object foto,
            List<ContextoComplementoFrequenciaDTO> contextos) {
        if (versoes.findTopByTipoComplementoAndComplementoIdOrderByNumeroVersaoDesc(tipo, id).isEmpty())
            registrar(tipo, id, "INCORPORACAO_LEGADO", data, foto, contextos, null, null);
    }

    public boolean mesmoConteudo(Object antes, Object depois) {
        return mapper.valueToTree(antes).equals(mapper.valueToTree(depois));
    }

    public void registrar(String tipo, Long id, String operacao, LocalDate data, Object foto,
            List<ContextoComplementoFrequenciaDTO> contextos, UsuarioAplicacao usuario, String justificativa) {
        String declarada = JustificativaFonteFrequencia.validar(justificativa,
                "RETIFICACAO".equals(operacao) || "CANCELAMENTO".equals(operacao));
        List<VersaoComplementoFrequencia> cadeia = versoes.findByTipoComplementoAndComplementoIdOrderByNumeroVersaoAsc(tipo, id);
        validarCadeia(cadeia);
        VersaoComplementoFrequencia anterior = cadeia.isEmpty() ? null : cadeia.get(cadeia.size() - 1);
        int numero = anterior == null ? 1 : anterior.getNumeroVersao() + 1;
        String motivoTecnico = switch (operacao) {
            case "CRIACAO" -> "Cadastro manual da fonte de frequência.";
            case "RETIFICACAO" -> "Alteração manual da fonte de frequência, com preservação da versão anterior.";
            case "CANCELAMENTO" -> "Cancelamento lógico da fonte manual, sem apagamento do histórico.";
            default -> "Conteúdo legado incorporado neste instante; autoria e instante do registro original não comprovados.";
        };
        Instant registro = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Long usuarioId = usuario == null ? null : usuario.getId();
        String username = usuario == null ? null : usuario.getUsername();
        Long anteriorId = anterior == null ? null : anterior.getId();
        String hashAnterior = anterior == null ? null : anterior.getHashVersao();
        String fotografiaJson = json(foto), contextosJson = json(contextos);
        String hash = hash(tipo, id, numero, operacao, data, registro, usuarioId, username, declarada,
                motivoTecnico, anteriorId, fotografiaJson, contextosJson, hashAnterior);
        VersaoComplementoFrequencia nova = versoes.save(new VersaoComplementoFrequencia(tipo, id, numero,
                operacao, data, registro, usuarioId, username, declarada, motivoTecnico, anteriorId,
                fotografiaJson, contextosJson, hashAnterior, hash));
        Map<String,Object> detalhes = new LinkedHashMap<>();
        detalhes.put("versaoId", nova.getId()); detalhes.put("numeroVersao", numero);
        detalhes.put("versaoAnteriorId", anteriorId); detalhes.put("dataEfetiva", data);
        detalhes.put("justificativaDeclarada", declarada); detalhes.put("motivoTecnico", motivoTecnico);
        detalhes.put("fotografia", lerJson(fotografiaJson)); detalhes.put("contextos", contextos);
        detalhes.put("hashAnterior", hashAnterior); detalhes.put("hashVersao", hash);
        auditoria.registrarAcaoComoUsuario(operacao, "ComplementoFrequencia" + tipo, id,
                "Fonte manual de frequência " + id, detalhes, usuario);
    }

    public List<VersaoComplementoFrequenciaDTO> listarEventos(String tipo, Long id, Instant conhecidoAte,
            UsuarioAplicacao usuario, List<ContextoComplementoFrequenciaDTO> contextoAtual) {
        List<VersaoComplementoFrequencia> cadeia = versoes.findByTipoComplementoAndComplementoIdOrderByNumeroVersaoAsc(tipo, id);
        validarCadeia(cadeia);
        var nivel = minimizador.nivelPadrao(usuario);
        Instant conhecimentoAutorizacao = Instant.now();
        List<VersaoComplementoFrequenciaDTO> resultado = new ArrayList<>();
        for (VersaoComplementoFrequencia v : cadeia) {
            if (conhecidoAte != null && v.getRegistradoEm().isAfter(conhecidoAte)) continue;
            List<ContextoComplementoFrequenciaDTO> contextos = contextos(v);
            if (!podeConsultar(usuario, realinharContextos(contextos, conhecimentoAutorizacao))) continue;
            JsonNode foto = lerJson(v.getFotografiaJson());
            if (!minimizador.completo(nivel) && foto instanceof ObjectNode obj) {
                obj.putNull("observacao"); obj.putNull("justificativa");
                if (obj.has("empregadoNome"))
                    obj.put("empregadoNome", minimizador.nome(obj.path("empregadoNome").asText(null), nivel));
            }
            resultado.add(new VersaoComplementoFrequenciaDTO(v.getId(), tipo, id, v.getNumeroVersao(),
                    v.getTipoOperacao(), v.getDataEfetiva(), v.getRegistradoEm(), v.getUsuarioId(),
                    minimizador.restrito(v.getUsuarioUsername(), nivel), minimizador.restrito(v.getJustificativa(), nivel),
                    v.getMotivoTecnico(), v.getVersaoAnteriorId(), foto, contextos, v.getHashAnterior(), v.getHashVersao()));
        }
        if (resultado.isEmpty() && !podeConsultar(usuario, contextoAtual)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Complemento não encontrado no seu escopo histórico comprovado.");
        return List.copyOf(resultado);
    }

    private List<ContextoComplementoFrequenciaDTO> realinharContextos(
            List<ContextoComplementoFrequenciaDTO> fotografias, Instant conhecimento) {
        List<ContextoComplementoFrequenciaDTO> resultado = new ArrayList<>();
        for (ContextoComplementoFrequenciaDTO foto : fotografias) {
            ContextoComplementoFrequenciaDTO contexto = escopoProva.realinhar(foto, conhecimento);
            if (contexto == null) return List.of();
            resultado.add(contexto);
        }
        return List.copyOf(resultado);
    }

    private List<ContextoComplementoFrequenciaDTO> contextos(VersaoComplementoFrequencia v) {
        try { return mapper.readValue(v.getContextosJson(), new TypeReference<>() { }); }
        catch (Exception e) { throw inconsistente(); }
    }

    private void validarCadeia(List<VersaoComplementoFrequencia> cadeia) {
        String hashAnterior = null; Long anteriorId = null; int numero = 0;
        for (VersaoComplementoFrequencia v : cadeia) {
            if (!Objects.equals(v.getHashAnterior(), hashAnterior) || !Objects.equals(v.getVersaoAnteriorId(), anteriorId)
                    || v.getNumeroVersao() != ++numero || !Objects.equals(v.getHashVersao(), hash(
                    v.getTipoComplemento(), v.getComplementoId(), v.getNumeroVersao(), v.getTipoOperacao(), v.getDataEfetiva(),
                    v.getRegistradoEm(), v.getUsuarioId(), v.getUsuarioUsername(), v.getJustificativa(), v.getMotivoTecnico(),
                    v.getVersaoAnteriorId(), v.getFotografiaJson(), v.getContextosJson(), v.getHashAnterior()))) throw inconsistente();
            hashAnterior = v.getHashVersao(); anteriorId = v.getId();
        }
    }

    private String hash(Object... valores) { return UtilitarioHashSeguranca.sha256Hexadecimal(json(Arrays.asList(valores))); }
    private String json(Object objeto) {
        try { return mapper.writeValueAsString(objeto); }
        catch (Exception e) { throw new IllegalStateException("Não foi possível fotografar o complemento.", e); }
    }
    private JsonNode lerJson(String json) {
        try { return mapper.readTree(json); }
        catch (Exception e) { throw inconsistente(); }
    }
    public String narrativaParaConsulta(String valor, UsuarioAplicacao usuario) {
        return minimizador.restrito(valor, minimizador.nivelPadrao(usuario));
    }
    public String nomeParaConsulta(String valor, UsuarioAplicacao usuario) {
        return minimizador.nome(valor, minimizador.nivelPadrao(usuario));
    }
    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Período do complemento inválido.");
    }
    private ResponseStatusException inconsistente() { return new ResponseStatusException(HttpStatus.CONFLICT,
            "A cadeia de versões do complemento está inconsistente."); }
    private void negarEscrita() { throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Sem permissão para gerir o complemento no contexto histórico de todos os dias atingidos."); }
}
