package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dto.EventoFrequenciaDTO;
import com.example.siggtrat.dto.EvidenciasIntegridadeFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoFrequenciaRepositorio;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class AuditoriaFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AuditoriaFrequenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final String SNAPSHOT_V2 = "snapshotVersao=2;";
    private static final Set<String> CAMPOS_AUDITORIA = Set.of(
            "snapshotVersao", "status", "dataReferencia", "entrada1", "saida1", "entrada2", "saida2",
            "horas", "horarioSaida", "horasNoturnas", "minutosFisicos", "minutosNoturnosReais",
            "minutosAdicionais", "minutosPagaveis", "calculoVersao", "statusBaseAntesOcorrencia",
            "ocorrenciaOverrideAtivo", "pessoa", "empregado", "vinculo", "alocacao", "contrato",
            "contratoNumero", "lotacao", "lotacaoNome", "lotacaoMunicipio", "jornada", "jornadaDescricao",
            "jornadaDiaTrabalho", "jornadaEntrada1", "jornadaSaida1", "jornadaEntrada2", "jornadaSaida2",
            "cargo", "cargoNome", "empregadoNome", "gestaoLocal", "superintendencia", "estado", "validade",
            "auditoria", "bloqueada", "versaoNegocial", "autorOriginalId", "ultimaRetificacaoPorId");
    private final EventoFrequenciaRepositorio repositorio;
    private final MinimizadorDadosPessoaisServico minimizador;

    public AuditoriaFrequenciaServico(EventoFrequenciaRepositorio repositorio,
                                      MinimizadorDadosPessoaisServico minimizador) {
        this.repositorio = repositorio;
        this.minimizador = minimizador;
    }

    @Transactional(readOnly = true)
    public List<EventoFrequenciaDTO> listar(Long frequenciaId) {
        List<EventoFrequencia> eventos = repositorio.findByFrequenciaIdOrderByNumeroVersaoAscIdAsc(frequenciaId);
        validarCadeia(eventos);
        return eventos.stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EventoFrequenciaDTO> listar(Long frequenciaId, NivelExposicaoDadosPessoais nivel) {
        // A prova integral é validada antes de limitar a representação autorizada.
        return listar(frequenciaId).stream().map(e -> minimizar(e, nivel)).toList();
    }

    @Transactional(readOnly = true)
    public EvidenciasIntegridadeFrequenciaDTO evidenciasIntegridade(Long frequenciaId,
                                                                   NivelExposicaoDadosPessoais nivel) {
        EvidenciasIntegridadeFrequenciaDTO original = evidenciasIntegridade(frequenciaId);
        return new EvidenciasIntegridadeFrequenciaDTO(original.frequenciaId(), original.estado(),
                original.cadeiaIntegra(), original.eventos().stream().map(e ->
                new EvidenciasIntegridadeFrequenciaDTO.Evento(minimizar(e.eventoOriginal(), nivel),
                        e.hashArmazenado(), e.hashCalculado(), e.hashConfere(), e.encadeamentoConfere())).toList());
    }

    private EventoFrequenciaDTO minimizar(EventoFrequenciaDTO e, NivelExposicaoDadosPessoais nivel) {
        if (minimizador.completo(nivel)) return e;
        return new EventoFrequenciaDTO(e.id(), e.frequenciaId(), e.numeroVersao(), e.tipoEvento(), e.dataReferencia(),
                minimizarSnapshot(e.snapshotAnterior(), nivel), minimizarSnapshot(e.snapshotNovo(), nivel),
                minimizador.restrito(e.motivo(), nivel), e.usuarioId(),
                minimizador.restrito(e.usuarioUsername(), nivel), e.registradoEm(), e.hashAnterior(), e.hashEvento(), true);
    }

    private String minimizarSnapshot(String original, NivelExposicaoDadosPessoais nivel) {
        if (original == null) return null;
        boolean escapado = original.startsWith(SNAPSHOT_V2);
        boolean barra = false;
        int inicio = 0;
        StringJoiner campos = new StringJoiner(";");
        for (int i = 0; i <= original.length(); i++) {
            char atual = i == original.length() ? ';' : original.charAt(i);
            if (i == original.length() || atual == ';' && !barra) {
                int separador = original.indexOf('=', inicio);
                if (separador >= inicio && separador < i) {
                    String nome = original.substring(inicio, separador);
                    if (CAMPOS_AUDITORIA.contains(nome)
                            && !(nome.equals("empregadoNome")
                            && nivel == NivelExposicaoDadosPessoais.INSTITUCIONAL_SEM_DADOS_PESSOAIS)) {
                        campos.add(original.substring(inicio, i));
                    }
                }
                inicio = i + 1;
            }
            barra = escapado && atual == '\\' && !barra;
        }
        return campos.toString();
    }

    @Transactional(readOnly = true)
    public EvidenciasIntegridadeFrequenciaDTO evidenciasIntegridade(Long frequenciaId) {
        List<EventoFrequencia> eventos = repositorio.findByFrequenciaIdOrderByNumeroVersaoAscIdAsc(frequenciaId);
        List<EvidenciasIntegridadeFrequenciaDTO.Evento> evidencias = new ArrayList<>();
        String anterior = null;
        Integer versaoAnterior = null;
        // Ausência de eventos não constitui prova de integridade do histórico.
        boolean integra = !eventos.isEmpty();
        for (EventoFrequencia evento : eventos) {
            String calculado = calcularHash(evento);
            boolean hashConfere = evento.getHashEvento() != null && Objects.equals(evento.getHashEvento(), calculado);
            boolean encadeamentoConfere = Objects.equals(anterior, evento.getHashAnterior())
                    && evento.getNumeroVersao() != null
                    && evento.getNumeroVersao() == (versaoAnterior == null ? 1 : versaoAnterior + 1);
            integra &= hashConfere && encadeamentoConfere;
            evidencias.add(new EvidenciasIntegridadeFrequenciaDTO.Evento(paraDTO(evento),
                    evento.getHashEvento(), calculado, hashConfere, encadeamentoConfere));
            anterior = evento.getHashEvento();
            versaoAnterior = evento.getNumeroVersao();
        }
        integra &= terminalConfereComRaiz(eventos);
        return new EvidenciasIntegridadeFrequenciaDTO(frequenciaId,
                integra ? "INTEGRA" : "INCONSISTENTE_OU_NAO_VERIFICAVEL", integra, evidencias);
    }

    @Transactional(readOnly = true)
    public boolean chaveJaAplicada(Long frequenciaId, String chave, String hashIntencao) {
        String normalizada = normalizar(chave);
        if (normalizada == null) return false;
        Optional<EventoFrequencia> existente = repositorio.findByChaveIdempotencia(normalizada);
        if (existente.isEmpty()) return false;
        EventoFrequencia evento = existente.get();
        if (!Objects.equals(evento.getFrequencia().getId(), frequenciaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência já pertence a outra frequência.");
        }
        if (!Objects.equals(evento.getHashIntencao(), hashIntencao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência foi reutilizada para uma intenção diferente.");
        }
        return true;
    }

    @Transactional
    public EventoFrequencia registrar(Frequencia frequencia,
                                      TipoEventoFrequencia tipo,
                                      String snapshotAnterior,
                                      String motivo,
                                      UsuarioAplicacao usuario,
                                      String chaveIdempotencia,
                                      String hashIntencao) {
        String chave = normalizar(chaveIdempotencia);
        if (chave != null && repositorio.findByChaveIdempotencia(chave).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chave de idempotência já utilizada.");
        }
        EventoFrequencia evento = new EventoFrequencia();
        evento.setFrequencia(frequencia);
        evento.setNumeroVersao(frequencia.getVersaoNegocial());
        evento.setTipoEvento(tipo);
        evento.setDataReferencia(frequencia.getDataReferencia());
        evento.setSnapshotAnterior(snapshotAnterior);
        evento.setSnapshotNovo(snapshot(frequencia));
        evento.setMotivo(normalizar(motivo));
        evento.setUsuarioId(usuario == null ? null : usuario.getId());
        evento.setUsuarioUsername(usuario == null ? "SISTEMA" : usuario.getUsername());
        // MySQL preserva microssegundos. O digest precisa usar exatamente o
        // instante que será recuperado do banco, antes de qualquer persistência.
        evento.setRegistradoEm(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        evento.setChaveIdempotencia(chave);
        evento.setHashIntencao(hashIntencao);
        String hashAnterior = repositorio.findTopByFrequenciaIdOrderByNumeroVersaoDescIdDesc(frequencia.getId())
                .map(EventoFrequencia::getHashEvento).orElse(null);
        evento.setHashAnterior(hashAnterior);
        evento.setHashEvento(calcularHash(evento));
        return repositorio.save(evento);
    }

    @Transactional(readOnly = true)
    public boolean ultimaIntencaoOperacionalJaAplicada(Long frequenciaId, String hashIntencao) {
        if (frequenciaId == null || hashIntencao == null) return false;
        return repositorio.findTopByFrequenciaIdOrderByNumeroVersaoDescIdDesc(frequenciaId)
                .filter(e -> e.getTipoEvento() == TipoEventoFrequencia.REGISTRO
                        || e.getTipoEvento() == TipoEventoFrequencia.ALTERACAO_OPERACIONAL)
                .filter(e -> Objects.equals(e.getHashIntencao(), hashIntencao))
                .isPresent();
    }

    /** Um prefixo com hashes válidos não prova a última versão persistida. */
    private boolean terminalConfereComRaiz(List<EventoFrequencia> eventos) {
        if (eventos.isEmpty()) return false;
        EventoFrequencia ultimo = eventos.get(eventos.size() - 1);
        return ultimo.getFrequencia() != null && ultimo.getNumeroVersao() != null
                && Objects.equals(ultimo.getNumeroVersao(), ultimo.getFrequencia().getVersaoNegocial());
    }

    private void validarCadeia(List<EventoFrequencia> eventos) {
        String anterior = null;
        Integer versaoAnterior = null;
        for (EventoFrequencia evento : eventos) {
            if (!Objects.equals(anterior, evento.getHashAnterior())
                    || evento.getHashEvento() == null
                    || !Objects.equals(evento.getHashEvento(), calcularHash(evento))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A cadeia de auditoria da frequência está inconsistente.");
            }
            if (evento.getNumeroVersao() == null
                    || versaoAnterior != null && evento.getNumeroVersao() <= versaoAnterior) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A sequência de versões da frequência está inconsistente.");
            }
            anterior = evento.getHashEvento();
            versaoAnterior = evento.getNumeroVersao();
        }
    }

    private String calcularHash(EventoFrequencia evento) {
        if (evento.getSnapshotNovo() != null && evento.getSnapshotNovo().startsWith(SNAPSHOT_V2)) {
            // O formato novo assina os valores completos, com fronteiras
            // inequívocas. A representação e o digest legados permanecem intactos.
            StringBuilder conteudo = new StringBuilder("FREQUENCIA-EVENTO-2;");
            for (Object campo : new Object[]{evento.getHashAnterior(),
                    evento.getFrequencia() == null ? null : evento.getFrequencia().getId(),
                    evento.getNumeroVersao(), evento.getTipoEvento(), evento.getDataReferencia(),
                    evento.getSnapshotAnterior(), evento.getSnapshotNovo(), evento.getMotivo(),
                    evento.getRegistradoEm(), evento.getUsuarioId(), evento.getUsuarioUsername(),
                    evento.getChaveIdempotencia(), evento.getHashIntencao()}) {
                if (campo == null) conteudo.append("-1:");
                else {
                    String texto = String.valueOf(campo);
                    conteudo.append(texto.length()).append(':').append(texto);
                }
            }
            return UtilitarioHashSeguranca.sha256Hexadecimal(conteudo.toString());
        }
        return UtilitarioHashSeguranca.sha256Hexadecimal(
                valor(evento.getHashAnterior()) + "|"
                        + (evento.getFrequencia() == null ? "" : valor(evento.getFrequencia().getId())) + "|"
                        + valor(evento.getNumeroVersao()) + "|" + valor(evento.getTipoEvento()) + "|"
                        + valor(evento.getDataReferencia()) + "|" + valor(evento.getSnapshotAnterior()) + "|"
                        + valor(evento.getSnapshotNovo()) + "|" + valor(evento.getMotivo()) + "|"
                        + valor(evento.getRegistradoEm()) + "|" + valor(evento.getUsuarioId()) + "|"
                        + valor(evento.getUsuarioUsername()) + "|" + valor(evento.getChaveIdempotencia()) + "|"
                        + valor(evento.getHashIntencao()));
    }

    public String snapshot(Frequencia f) {
        if (f == null) return null;
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("status", f.getStatus());
        campos.put("dataReferencia", f.getDataReferencia());
        campos.put("entrada1", f.getEntrada1()); campos.put("saida1", f.getSaida1());
        campos.put("entrada2", f.getEntrada2()); campos.put("saida2", f.getSaida2());
        campos.put("horas", f.getHorasTrabalhadas()); campos.put("horarioSaida", f.getHorarioSaida());
        campos.put("horasNoturnas", f.getHorasNoturnas());
        campos.put("minutosFisicos", f.getMinutosFisicos());
        campos.put("minutosNoturnosReais", f.getMinutosNoturnosReais());
        campos.put("minutosAdicionais", f.getMinutosAdicionais());
        campos.put("minutosPagaveis", f.getMinutosPagaveis());
        campos.put("calculoVersao", f.getCalculoVersao());
        campos.put("observacao", f.getObservacao());
        campos.put("statusBaseAntesOcorrencia", f.getStatusBaseAntesOcorrencia());
        campos.put("ocorrenciaOverrideAtivo", f.getOcorrenciaOverrideAtivo());
        campos.put("pessoa", id(f.getPessoa())); campos.put("empregado", id(f.getEmpregado()));
        campos.put("vinculo", id(f.getVinculoContratual())); campos.put("alocacao", id(f.getAlocacaoVinculo()));
        campos.put("contrato", f.getContratoSnapshotId()); campos.put("contratoNumero", f.getContratoSnapshotNumero());
        campos.put("lotacao", f.getLotacaoSnapshotId()); campos.put("lotacaoNome", f.getLotacaoSnapshotNome());
        campos.put("lotacaoMunicipio", f.getLotacaoSnapshotMunicipio());
        campos.put("jornada", f.getJornadaSnapshotId()); campos.put("jornadaDescricao", f.getJornadaSnapshotDescricao());
        campos.put("jornadaDiaTrabalho", f.getJornadaSnapshotDiaTrabalho());
        campos.put("jornadaEntrada1", f.getJornadaSnapshotEntrada1()); campos.put("jornadaSaida1", f.getJornadaSnapshotSaida1());
        campos.put("jornadaEntrada2", f.getJornadaSnapshotEntrada2()); campos.put("jornadaSaida2", f.getJornadaSnapshotSaida2());
        campos.put("cargo", f.getCargoSnapshotId()); campos.put("cargoNome", f.getCargoSnapshotNome());
        campos.put("empregadoNome", f.getEmpregadoSnapshotNome());
        campos.put("gestaoLocal", f.getGestaoLocalSnapshotId()); campos.put("superintendencia", f.getSuperintendenciaSnapshotId());
        campos.put("estado", f.getEstadoNegocial()); campos.put("validade", f.getValidadeOperacional());
        campos.put("auditoria", f.getEstadoAuditoria()); campos.put("bloqueada", f.getBloqueadaEdicaoOperacional());
        if (f.getPrazoLancamentoEm() != null) {
        campos.put("prazoLancamentoEm", f.getPrazoLancamentoEm());
        campos.put("prazoEdicaoEm", f.getPrazoEdicaoEm());
        }
        if (f.getRevisaoSolicitadaStatus() != null) {
        campos.put("revisaoSolicitadaStatus", f.getRevisaoSolicitadaStatus());
        campos.put("revisaoSolicitadaEm", f.getRevisaoSolicitadaEm());
        campos.put("revisaoSolicitanteId", f.getRevisaoSolicitanteId());
        campos.put("revisaoSolicitadaMotivo", f.getRevisaoSolicitadaMotivo());
        campos.put("revisaoDecididaEm", f.getRevisaoDecididaEm());
        campos.put("revisaoDecisorId", f.getRevisaoDecisorId());
        campos.put("revisaoDecisaoMotivo", f.getRevisaoDecisaoMotivo());
        }
        campos.put("versaoNegocial", f.getVersaoNegocial()); campos.put("motivoRevisao", f.getMotivoRevisao());
        campos.put("autorOriginalId", f.getAutorOriginalId()); campos.put("autorOriginalUsername", f.getAutorOriginalUsername());
        campos.put("ultimaRetificacaoPorId", f.getUltimaRetificacaoPorId());
        campos.put("ultimaRetificacaoPorUsername", f.getUltimaRetificacaoPorUsername());
        StringJoiner snapshot = new StringJoiner(";", SNAPSHOT_V2, "");
        campos.forEach((nome, valor) -> snapshot.add(nome + "=" + valorSnapshot(valor)));
        return snapshot.toString();
    }

    private EventoFrequenciaDTO paraDTO(EventoFrequencia e) {
        return new EventoFrequenciaDTO(e.getId(), e.getFrequencia().getId(), e.getNumeroVersao(), e.getTipoEvento(),
                e.getDataReferencia(), e.getSnapshotAnterior(), e.getSnapshotNovo(), e.getMotivo(), e.getUsuarioId(),
                e.getUsuarioUsername(), e.getRegistradoEm(), e.getHashAnterior(), e.getHashEvento());
    }

    private static Long id(Object entidade) {
        if (entidade instanceof Pessoa p) return p.getId();
        if (entidade instanceof Empregado e) return e.getId();
        if (entidade instanceof VinculoContratual v) return v.getId();
        if (entidade instanceof AlocacaoVinculo a) return a.getId();
        return null;
    }
    /** Escape reversível: null é \\N, enquanto uma barra literal é duplicada. */
    private static String valorSnapshot(Object o) {
        return o == null ? "\\N" : String.valueOf(o).replace("\\", "\\\\")
                .replace(";", "\\;").replace("=", "\\=")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
    private static String valor(Object o) { return o == null ? "" : String.valueOf(o).replace(";", ","); }
    private static String normalizar(String s) {
        if (s == null) return null;
        String n = s.trim().replaceAll("\\s+", " ");
        return n.isBlank() ? null : n;
    }
}
