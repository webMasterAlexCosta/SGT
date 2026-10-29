package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.constante.EsquemaPainel;
import com.example.siggtrat.dto.PainelCalculoDTO;
import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.evento.EventoPainelCalculoSolicitado;
import com.example.siggtrat.entidade.PainelCalculo;
import com.example.siggtrat.entidade.PainelCalculoStatus;
import com.example.siggtrat.repositorio.PainelCalculoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class PainelCalculoServico {

    private static final Logger log = LoggerFactory.getLogger(PainelCalculoServico.class);
    private final PainelCalculoRepositorio repositorio;
    private final PainelServico painelServico;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final InvalidacaoCacheServico cacheInvalidationServico;
    private final long cacheMinutos;
    private final ZoneId zoneId = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public PainelCalculoServico(PainelCalculoRepositorio repositorio,
                                PainelServico painelServico,
                                ObjectMapper objectMapper,
                                ApplicationEventPublisher eventPublisher,
                                InvalidacaoCacheServico cacheInvalidationServico,
                                @Value("${app.painel.cache-minutos:10}") long cacheMinutos) {
        this.repositorio = repositorio;
        this.painelServico = painelServico;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.cacheInvalidationServico = cacheInvalidationServico;
        this.cacheMinutos = Math.max(1, cacheMinutos);
    }

    @Transactional
    public synchronized ResultadoSolicitacao solicitar(
            LocalDate referencia,
            Integer periodoDias,
            boolean forcarAtualizacao
    ) {
        var contexto = painelServico.contextoAtual();
        LocalDate referenciaNormalizada = referencia == null
                ? LocalDate.now(zoneId).minusDays(1)
                : referencia;
        int periodoNormalizado = periodoDias == null ? 30 : periodoDias;
        // Reutiliza as validações oficiais antes de criar trabalho persistido.
        validarSemExecutarConsultas(referenciaNormalizada, periodoNormalizado);

        if (forcarAtualizacao) {
            // Forçar atualização precisa ignorar o agregado Redis antes de o
            // processador assíncrono iniciar; caso contrário ele pode republicar
            // exatamente o payload antigo que o usuário pediu para recalcular.
            cacheInvalidationServico.invalidarPainel();
        }

        String chave = chave(contexto, referenciaNormalizada, periodoNormalizado);
        PainelCalculo calculo = repositorio.buscarPorChaveParaAtualizacao(chave).orElseGet(() -> novo(
                chave, contexto, referenciaNormalizada, periodoNormalizado));

        PainelGestaoDTO dadosPersistidos = desserializar(calculo.getPayloadJson(), calculo.getId());
        boolean payloadValido = dadosPersistidos != null;
        if (calculo.getStatus() == PainelCalculoStatus.CONCLUIDO && !payloadValido) {
            log.warn("Cálculo do painel id={} estava CONCLUIDO sem payload utilizável; será recalculado.", calculo.getId());
            prepararRecalculo(calculo, "Cache do painel incompatível ou incompleto. Recalculando indicadores.");
            calculo.setPayloadJson(null);
            repositorio.save(calculo);
        }

        boolean fresco = calculo.getStatus() == PainelCalculoStatus.CONCLUIDO
                && payloadValido
                && calculo.getConcluidoEm() != null
                && calculo.getConcluidoEm().isAfter(agora().minusMinutes(cacheMinutos));

        if (fresco && !forcarAtualizacao) {
            return new ResultadoSolicitacao(HttpStatus.OK, paraDTO(calculo, false, dadosPersistidos));
        }

        boolean processando = calculo.getStatus() == PainelCalculoStatus.PROCESSANDO;
        boolean deveEnfileirar = calculo.getStatus() == PainelCalculoStatus.PENDENTE
                || !processando
                || forcarAtualizacao;
        if (!processando || forcarAtualizacao) {
            prepararRecalculo(calculo, calculo.getPayloadJson() == null
                    ? "Painel enfileirado para consolidação."
                    : "Atualizando indicadores em segundo plano.");
            repositorio.save(calculo);
        }
        if (deveEnfileirar) {
            eventPublisher.publishEvent(new EventoPainelCalculoSolicitado(calculo.getId()));
        }

        return new ResultadoSolicitacao(HttpStatus.ACCEPTED, paraDTO(calculo, dadosPersistidos != null, dadosPersistidos));
    }

    @Transactional
    public PainelCalculoDTO consultar(Long id) {
        PainelCalculo calculo = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cálculo do painel não encontrado."));
        var contexto = painelServico.contextoAtual();
        if (!mesmoEscopo(calculo, contexto)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este cálculo pertence a outro escopo organizacional.");
        }
        PainelGestaoDTO dados = desserializar(calculo.getPayloadJson(), calculo.getId());
        if (calculo.getStatus() == PainelCalculoStatus.CONCLUIDO && dados == null) {
            log.warn("Consulta encontrou cálculo id={} concluído sem dados válidos; reenfileirando automaticamente.", id);
            prepararRecalculo(calculo, "Cache do painel incompatível ou incompleto. Recalculando indicadores.");
            calculo.setPayloadJson(null);
            repositorio.save(calculo);
            eventPublisher.publishEvent(new EventoPainelCalculoSolicitado(calculo.getId()));
            return paraDTO(calculo, false, null);
        }
        boolean desatualizado = calculo.getStatus() != PainelCalculoStatus.CONCLUIDO && dados != null;
        return paraDTO(calculo, desatualizado, dados);
    }

    private PainelCalculo novo(String chave,
                               PainelServico.PainelCalculoContexto contexto,
                               LocalDate referencia,
                               int periodoDias) {
        PainelCalculo p = new PainelCalculo();
        p.setChaveConsulta(chave);
        p.setDataReferencia(referencia);
        p.setPeriodoDias(periodoDias);
        p.setNivelEscopo(contexto.nivel());
        p.setNomeEscopo(contexto.nome());
        p.setSuperintendenciaId(contexto.superintendenciaId());
        p.setGestaoLocalId(contexto.gestaoLocalId());
        p.setPermiteDetalhamentoOperacional(contexto.permiteDetalhamentoOperacional());
        p.setPodeVisualizarContratos(contexto.podeVisualizarContratos());
        p.setPodeVisualizarEmpregados(contexto.podeVisualizarEmpregados());
        p.setPodeOperarFrequencia(contexto.podeOperarFrequencia());
        p.setPodeAcessarRelatorios(contexto.podeAcessarRelatorios());
        p.setStatus(PainelCalculoStatus.PENDENTE);
        p.setMensagem("Painel enfileirado para consolidação.");
        return repositorio.saveAndFlush(p);
    }

    private PainelCalculoDTO paraDTO(PainelCalculo p, boolean desatualizado, PainelGestaoDTO dados) {
        boolean concluidoComDados = p.getStatus() == PainelCalculoStatus.CONCLUIDO && dados != null;
        return new PainelCalculoDTO(
                p.getId(), p.getStatus().name(), p.getProgresso(),
                concluidoComDados,
                desatualizado,
                p.getMensagem(),
                p.getConcluidoEm(),
                dados
        );
    }

    /**
     * Lê payloads atuais e também os gerados pela versão intermediária que
     * renomeou registrosEsperados para registrosLancados. A normalização evita
     * que um cache antigo derrube o painel inteiro durante uma implantação.
     */
    private PainelGestaoDTO desserializar(String json, Long calculoId) {
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json.trim())) return null;
        try {
            JsonNode raiz = objectMapper.readTree(json);
            if (raiz == null || raiz.isNull()) return null;
            normalizarRegistrosLancados(raiz);
            return objectMapper.treeToValue(raiz, PainelGestaoDTO.class);
        } catch (Exception erro) {
            log.warn("Não foi possível desserializar o payload do painel id={}: {}", calculoId, erro.getMessage());
            return null;
        }
    }

    private void normalizarRegistrosLancados(JsonNode raiz) {
        if (!(raiz instanceof ObjectNode objeto)) return;
        normalizarCampoRegistros(objeto.get("frequencia"));
        JsonNode unidades = objeto.get("unidades");
        if (unidades instanceof ArrayNode lista) {
            for (JsonNode unidade : lista) normalizarCampoRegistros(unidade);
        }
    }

    private void normalizarCampoRegistros(JsonNode no) {
        if (!(no instanceof ObjectNode objeto)) return;
        JsonNode novo = objeto.get("registrosLancados");
        if (!objeto.has("registrosEsperados") && novo != null) {
            objeto.set("registrosEsperados", novo);
        }
        objeto.remove("registrosLancados");
    }

    private void prepararRecalculo(PainelCalculo calculo, String mensagem) {
        calculo.setStatus(PainelCalculoStatus.PENDENTE);
        calculo.setProgresso(0);
        calculo.setMensagem(mensagem);
        calculo.setDetalheErro(null);
        calculo.setHeartbeatEm(null);
        calculo.setConcluidoEm(null);
    }

    private boolean mesmoEscopo(PainelCalculo p, PainelServico.PainelCalculoContexto c) {
        return Objects.equals(p.getNivelEscopo(), c.nivel())
                && Objects.equals(p.getSuperintendenciaId(), c.superintendenciaId())
                && Objects.equals(p.getGestaoLocalId(), c.gestaoLocalId());
    }

    private void validarSemExecutarConsultas(LocalDate referencia, int periodoDias) {
        LocalDate hoje = LocalDate.now(zoneId);
        if (referencia.isAfter(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de referência não pode estar no futuro.");
        }
        if (referencia.isBefore(hoje.minusYears(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de referência deve estar dentro dos últimos dois anos.");
        }
        if (!java.util.Set.of(7, 15, 30, 60, 90).contains(periodoDias)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período deve ser um dos valores permitidos: 7, 15, 30, 60 ou 90 dias.");
        }
    }

    private String chave(PainelServico.PainelCalculoContexto c, LocalDate referencia, int periodoDias) {
        String canonica = String.join("|",
                "schema=" + EsquemaPainel.VERSION,
                c.nivel(), String.valueOf(c.superintendenciaId()), String.valueOf(c.gestaoLocalId()),
                String.valueOf(c.permiteDetalhamentoOperacional()),
                String.valueOf(c.podeVisualizarContratos()), String.valueOf(c.podeVisualizarEmpregados()),
                String.valueOf(c.podeOperarFrequencia()), String.valueOf(c.podeAcessarRelatorios()),
                referencia.toString(), String.valueOf(periodoDias));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonica.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar a chave do painel.", e);
        }
    }

    private LocalDateTime agora() { return LocalDateTime.now(zoneId); }

    public record ResultadoSolicitacao(HttpStatus status, PainelCalculoDTO corpo) {}
}
