package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Proposta independente para VIN-005:002, VIN-007:002 e DES-004:002.
 * Pessoa, vínculo, alocação, cancelamento e desligamento nascem nas rotas canônicas.
 * As fontes sintéticas de catálogo pertencem à fixture declarada em AuditoriaHttpSuporte.
 * Sem transação externa: cada oráculo JPA abaixo relê o estado depois do commit HTTP.
 * ConclusaoAud03 já verifica o corte de alocação após retificação. Aqui, o oráculo
 * complementar reconstrói a posição integral dos vínculos antes de cada ato.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaFinalPessoasLacunasEspecificasHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void programacaoNaoAtivaProjecaoNemPermiteFrequenciaAntesDoInicio() throws Exception {
        // FrequenciaServico também barra futuro civil usando LocalDate.now.
        // Datas civis já passadas deixam o teste atingir a guarda do vínculo,
        // com hoje negocial controlado no dia anterior ao início e depois no início.
        LocalDate inicio = RelogioSistema.hoje()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        LocalDate anterior = inicio.minusDays(1);
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(anterior);
            Cenario c = criarCenarioPersistido();
            NovoContrato n = tx(() -> publicado(c, 5, null, false));
            JsonNode criado = cadastrar(c, n, inicio, "MAT-PROGRAMADA-" + n.id(), 756000000L);
            long vinculoId = criado.path("id").asLong();
            long pessoaId = criado.path("pessoaId").asLong();
            assertThat(criado.path("situacaoTemporal").asText()).isEqualTo("PROGRAMADO");
            assertThat(tx(() -> em.find(VinculoContratual.class, vinculoId).getDataInicio()))
                    .as("Pré-condição persistida: início estritamente posterior ao hoje negocial")
                    .isEqualTo(inicio).isAfter(anterior);
            JsonNode contrato = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + anterior,
                    "Pré-condição: contrato publicado, vigente e habilitado antes do início do vínculo");
            assertThat(contrato.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
            assertThat(contrato.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
            assertThat(contrato.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
            Projecao antes = projecaoPersistida(pessoaId);
            assertThat(antes.status()).isEqualTo("INATIVO");
            assertThat(antes.contratos()).isEmpty();
            var historiaAntes = historiaPersistida(vinculoId);
            assertThat(frequencias(vinculoId)).isEmpty();

            JsonNode recusada = enviar(c.localA, "/api/frequencias", frequencia(vinculoId, anterior), 409,
                    "VIN-005: programação não autoriza jornada anterior ao início");
            assertThat(recusada.toString()).contains("não está operacional na data da jornada");
            assertThat(frequencias(vinculoId)).as("Recusa não persiste frequência em nova transação").isEmpty();
            assertThat(projecaoPersistida(pessoaId)).as("Recusa não ativa a projeção nem associa contrato operacional")
                    .isEqualTo(antes);
            assertThat(historiaPersistida(vinculoId)).as("Recusa preserva vínculo, faixas e eventos persistidos")
                    .isEqualTo(historiaAntes);

            clock.when(RelogioSistema::hoje).thenReturn(inicio);
            JsonNode vigente = consultar(c.localA, V + "/" + vinculoId,
                    "Controle positivo: o mesmo vínculo torna-se vigente na data efetiva");
            assertThat(vigente.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
            JsonNode registrada = enviar(c.localA, "/api/frequencias", frequencia(vinculoId, inicio), 200,
                    "VIN-005: a operação é admitida no primeiro dia válido do mesmo vínculo");
            var linhas = frequencias(vinculoId);
            assertThat(linhas).hasSize(1);
            assertThat(linhas.get(0)).containsExactly(registrada.path("id").asLong(), pessoaId, vinculoId, inicio);
            Projecao depois = projecaoPersistida(pessoaId);
            assertThat(depois.empregadoId()).isEqualTo(antes.empregadoId());
            assertThat(depois.status()).isEqualTo("ATIVO");
            assertThat(depois.contratos()).containsExactly(n.id());
        }
    }

    @Test
    void cortesAnterioresAoCancelamentoERetificacaoRecuperamCadaProgramacaoConhecida() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = tx(() -> publicado(c, 5, null, false));
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate inicioOriginal = hoje.plusDays(10);
        String matriculaOriginal = "MAT-ORIGINAL-" + n.id();
        JsonNode original = cadastrar(c, n, inicioOriginal, matriculaOriginal, 757000000L);
        long vinculoId = original.path("id").asLong();
        long pessoaId = original.path("pessoaId").asLong();
        JsonNode eventosOriginais = consultar(c.localA, V + "/" + vinculoId + "/eventos",
                "Pré-condição: evento original persistido antes do cancelamento");
        assertThat(eventosOriginais).hasSize(1);
        assertThat(eventosOriginais.get(0).path("tipo").asText()).isEqualTo("CRIACAO");
        long eventoOriginalId = eventosOriginais.get(0).path("id").asLong();
        Instant corte = tx(() -> em.find(EventoVinculoContratual.class, eventoOriginalId).getRegistradoEm());
        JsonNode conhecidaAntes = consultar(c.localA, posicaoPessoa(pessoaId, hoje, corte),
                "VIN-007: corte usa o instante do evento original efetivamente gravado no MySQL");
        assertThat(conhecidaAntes.path("vinculos")).hasSize(1);
        JsonNode programada = conhecidaAntes.path("vinculos").get(0);
        assertThat(programada.path("vinculoId").asLong()).isEqualTo(vinculoId);
        assertThat(programada.path("dataInicio").asText()).isEqualTo(inicioOriginal.toString());
        assertThat(programada.path("matricula").asText()).isEqualTo(matriculaOriginal);
        assertThat(programada.path("estadoNegocial").asText()).isEqualTo("REGISTRADO");
        assertThat(programada.path("situacaoTemporal").asText()).isEqualTo("PROGRAMADO");
        assertThat(programada.path("eventoConhecidoMaisRecenteId").asLong()).isEqualTo(eventoOriginalId);

        enviar(c.localA, V + "/" + vinculoId + "/cancelar-programacao", acao("cancelamento"), 200,
                "Cancelamento documentado da programação futura");
        JsonNode eventosDepois = consultar(c.localA, V + "/" + vinculoId + "/eventos",
                "Origem e cancelamento continuam encadeados e consultáveis");
        assertThat(eventosDepois).hasSize(2);
        assertThat(eventosDepois.get(0)).isEqualTo(eventosOriginais.get(0));
        JsonNode cancelamento = eventosDepois.get(1);
        assertThat(cancelamento.path("tipo").asText()).isEqualTo("CANCELAMENTO_PROGRAMACAO");
        assertThat(cancelamento.path("eventoPrecedenteId").asLong()).isEqualTo(eventoOriginalId);
        assertThat(cancelamento.path("relacaoPrecedencia").asText()).isEqualTo("CANCELA");
        assertThat(cancelamento.path("hashAnterior")).isEqualTo(eventosOriginais.get(0).path("hashEvento"));
        assertThat(tx(() -> em.find(EventoVinculoContratual.class, cancelamento.path("id").asLong()).getRegistradoEm()))
                .as("Pré-condição: cancelamento efetivamente posterior ao corte escolhido").isAfter(corte);
        JsonNode nova = enviar(c.localA, V, mapa("pessoaId", pessoaId, "contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", inicioOriginal.plusDays(1),
                "matricula", "MAT-REPROGRAMADA-" + n.id()), 201,
                "Programação vigente conserva a identidade da Pessoa e possui matrícula própria");
        long novoVinculoId = nova.path("id").asLong();
        assertThat(novoVinculoId).isNotEqualTo(vinculoId);
        assertThat(nova.path("pessoaId").asLong()).isEqualTo(pessoaId);
        assertThat(tx(() -> em.find(VinculoContratual.class, novoVinculoId).getCriadoEm()))
                .as("Pré-condição: nova programação também surgiu depois do corte").isAfter(corte);

        JsonNode reconstruida = consultar(c.localA, posicaoPessoa(pessoaId, hoje, corte),
                "VIN-007: o mesmo corte recupera integralmente a programação conhecida antes dos novos atos");
        assertThat(reconstruida).isEqualTo(conhecidaAntes);
        JsonNode atual = consultar(c.localA, V + "/pessoas/" + pessoaId + "/posicao?data=" + hoje,
                "Conhecimento atual distingue programação cancelada e nova programação");
        assertThat(atual.path("vinculos")).hasSize(2);
        assertThat(posicaoDoVinculo(atual, vinculoId).path("situacaoTemporal").asText()).isEqualTo("CANCELADO");
        assertThat(posicaoDoVinculo(atual, novoVinculoId).path("situacaoTemporal").asText()).isEqualTo("PROGRAMADO");
        tx(() -> {
            VinculoContratual preservado = em.find(VinculoContratual.class, vinculoId);
            assertThat(preservado.getDataInicio()).isEqualTo(inicioOriginal);
            assertThat(preservado.getEstadoNegocial().name()).isEqualTo("CANCELADO");
            assertThat(preservado.getDataFim()).isNull();
            assertThat(preservado.getMotivoDesligamento()).isNull();
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:p", Long.class)
                    .setParameter("p", pessoaId).getSingleResult()).isEqualTo(2L);
            assertThat(em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                    .setParameter("p", pessoaId).getSingleResult()).isEqualTo(1L);
            return null;
        });
        assertThat(consultar(c.localA, V + "/" + vinculoId + "/eventos",
                "A reprogramação não reescreve a cadeia anterior")).isEqualTo(eventosDepois);

        // O AUD-03 existente consulta o corte da alocação. Este controle adicional
        // exige também o início/matrícula/estado do vínculo então conhecido.
        JsonNode origemNova = consultar(c.localA, V + "/" + novoVinculoId + "/eventos",
                "Origem da nova programação antes de retificar seu início");
        assertThat(origemNova).hasSize(1);
        long eventoNovaOrigemId = origemNova.get(0).path("id").asLong();
        Instant corteNova = tx(() -> em.find(EventoVinculoContratual.class, eventoNovaOrigemId).getRegistradoEm());
        JsonNode conhecidaNaReprogramacao = consultar(c.localA, posicaoPessoa(pessoaId, hoje, corteNova),
                "Pré-condição: segundo corte conhece cancelamento e nova programação, ainda não retificada");
        assertThat(conhecidaNaReprogramacao.path("vinculos")).hasSize(2);
        JsonNode novaConhecida = posicaoDoVinculo(conhecidaNaReprogramacao, novoVinculoId);
        assertThat(novaConhecida.path("dataInicio").asText()).isEqualTo(inicioOriginal.plusDays(1).toString());
        assertThat(novaConhecida.path("matricula").asText()).isEqualTo("MAT-REPROGRAMADA-" + n.id());
        assertThat(novaConhecida.path("situacaoTemporal").asText()).isEqualTo("PROGRAMADO");
        Map<String, Object> retificacao = acao("retificacao-da-nova-programacao");
        retificacao.put("novoInicio", inicioOriginal.plusDays(2));
        JsonNode previa = enviar(c.localA, V + "/" + novoVinculoId + "/previas/retificacao-inicio", retificacao, 200,
                "Prévia canônica para retificar a programação ainda futura");
        retificacao.put("confirmarImpacto", true);
        retificacao.put("tokenPrevia", previa.path("token").asText());
        enviar(c.localA, V + "/" + novoVinculoId + "/retificar-inicio", retificacao, 200,
                "Retificação confirmada com a prévia exata");
        assertThat(tx(() -> em.find(VinculoContratual.class, novoVinculoId).getDataInicio()))
                .as("Pré-condição persistida: retificação efetivamente alterou o início atual")
                .isEqualTo(inicioOriginal.plusDays(2));
        JsonNode cadeiaNova = consultar(c.localA, V + "/" + novoVinculoId + "/eventos",
                "Origem e retificação preservam sua precedência");
        assertThat(cadeiaNova).hasSize(2);
        assertThat(cadeiaNova.get(0)).isEqualTo(origemNova.get(0));
        assertThat(cadeiaNova.get(1).path("tipo").asText()).isEqualTo("RETIFICACAO_INICIO");
        assertThat(cadeiaNova.get(1).path("eventoPrecedenteId").asLong()).isEqualTo(eventoNovaOrigemId);
        assertThat(tx(() -> em.find(EventoVinculoContratual.class, cadeiaNova.get(1).path("id").asLong()).getRegistradoEm()))
                .isAfter(corteNova);
        assertThat(consultar(c.localA, posicaoPessoa(pessoaId, hoje, corteNova),
                "VIN-007: segundo corte recupera integralmente a programação conhecida antes da retificação"))
                .isEqualTo(conhecidaNaReprogramacao);
        assertThat(consultar(c.localA, posicaoPessoa(pessoaId, hoje, corte),
                "VIN-007: primeiro corte continua sem conhecer cancelamento, reprogramação ou retificação"))
                .isEqualTo(conhecidaAntes);
    }

    @Test
    void renomearCausaPreservaResponsavelInstanteEFotografiaDoDesligamento() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = tx(() -> publicado(c, 5, null, false));
        LocalDate hoje = RelogioSistema.hoje();
        long vinculoId = cadastrar(c, n, hoje.minusDays(2), "MAT-DES-" + n.id(), 758000000L)
                .path("id").asLong();
        String descricaoOriginal = "Desligamento individual documentado " + n.id();
        JsonNode motivo = enviar(c.localA, "/api/motivos-desligamento",
                mapa("descricao", descricaoOriginal, "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true),
                201, "Causa válida nasce por HTTP na Gestão e está disponível na data do desligamento");
        long motivoId = motivo.path("id").asLong();
        Instant antes = Instant.now().minusSeconds(1);
        Map<String, Object> req = acao("desligamento");
        req.put("dataFim", hoje);
        req.put("motivoDesligamentoId", motivoId);
        enviar(c.localA, V + "/" + vinculoId + "/desligar", req, 200,
                "DES-004: desligamento identifica causa, responsável e instante de registro");
        Instant depois = Instant.now().plusSeconds(1);
        JsonNode vinculoAntes = consultar(c.localA, V + "/" + vinculoId,
                "Fotografia do vínculo relida depois do commit do desligamento");
        JsonNode eventosAntes = consultar(c.localA, V + "/" + vinculoId + "/eventos",
                "Fotografia e metadados do evento de desligamento depois do commit");
        JsonNode evento = eventoDoTipo(eventosAntes, "DESLIGAMENTO");
        assertThat(evento.path("responsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(evento.path("responsavelUsername").asText()).isEqualTo(c.localA.getUsername());
        assertThat(Instant.parse(evento.path("registradoEm").asText())).isBetween(antes, depois);
        assertThat(evento.path("dataEfetiva").asText()).isEqualTo(hoje.toString());
        assertThat(evento.path("motivoDesligamentoIdFotografia").asLong()).isEqualTo(motivoId);
        assertThat(evento.path("motivoDesligamentoDescricaoFotografia").asText()).isEqualTo(descricaoOriginal);
        assertThat(evento.path("motivoDesligamentoCategoriaFotografia").asText()).isEqualTo("DESLIGAMENTO_INDIVIDUAL");
        DesligamentoPersistido fotoAntes = desligamentoPersistido(vinculoId, evento.path("id").asLong());
        assertThat(fotoAntes.fim()).isEqualTo(hoje);
        assertThat(fotoAntes.motivoId()).isEqualTo(motivoId);
        assertThat(fotoAntes.responsavelId()).isEqualTo(c.localA.getId());
        assertThat(fotoAntes.responsavelUsername()).isEqualTo(c.localA.getUsername());
        assertThat(fotoAntes.registradoEm()).isBetween(antes, depois);
        assertThat(fotoAntes.registradoEm()).isEqualTo(Instant.parse(vinculoAntes.path("desligamentoRegistradoEm").asText()));
        assertThat(fotoAntes.descricao()).isEqualTo(descricaoOriginal);
        assertThat(fotoAntes.categoria()).isEqualTo("DESLIGAMENTO_INDIVIDUAL");
        assertThat(fotoAntes.eventoResponsavelId()).isEqualTo(c.localA.getId());
        assertThat(fotoAntes.eventoResponsavelUsername()).isEqualTo(c.localA.getUsername());
        assertThat(fotoAntes.eventoRegistradoEm()).isEqualTo(Instant.parse(evento.path("registradoEm").asText()));
        assertThat(fotoAntes.eventoDescricao()).isEqualTo(descricaoOriginal);
        assertThat(fotoAntes.eventoCategoria()).isEqualTo("DESLIGAMENTO_INDIVIDUAL");
        assertThat(fotoAntes.eventoHash()).hasSize(64);

        String descricaoNova = "Causa renomeada posteriormente " + n.id();
        executar(c.localA, put("/api/motivos-desligamento/" + motivoId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(mapa("descricao", descricaoNova, "categoria", "OUTRO", "ativo", true,
                                "version", motivo.path("version").asLong()))), 200,
                "Alteração posterior real do catálogo por HTTP");
        tx(() -> {
            MotivoDesligamento atual = em.find(MotivoDesligamento.class, motivoId);
            assertThat(atual.getDescricao()).as("Pré-condição: novo nome efetivamente persistido").isEqualTo(descricaoNova);
            assertThat(atual.getCategoria().name()).isEqualTo("OUTRO");
            return null;
        });
        JsonNode vinculoDepois = consultar(c.localA, V + "/" + vinculoId,
                "DES-004: catálogo atual não altera a causa aplicada ao vínculo");
        assertThat(vinculoDepois.path("motivoDesligamentoDescricao").asText()).isEqualTo(descricaoOriginal);
        assertThat(vinculoDepois.path("desligamentoRegistradoEm")).isEqualTo(vinculoAntes.path("desligamentoRegistradoEm"));
        assertThat(consultar(c.localA, V + "/" + vinculoId + "/eventos",
                "DES-004: autoria, instante, categoria, descrição e hashes do evento permanecem idênticos"))
                .isEqualTo(eventosAntes);
        assertThat(desligamentoPersistido(vinculoId, evento.path("id").asLong()))
                .as("Nova transação: metadados e fotografias do vínculo e do evento são preservados integralmente")
                .isEqualTo(fotoAntes);
    }

    private JsonNode cadastrar(Cenario c, NovoContrato n, LocalDate inicio, String matricula, long baseCpf) throws Exception {
        return enviar(c.localA, V, mapa("contratoId", n.id(), "composicaoId", n.composicao1(),
                "dataInicio", inicio, "matricula", matricula,
                "pessoa", mapa("nome", "Pessoa das lacunas específicas " + n.id(),
                        "cpf", cpf(Math.toIntExact(baseCpf + n.id())))), 201,
                "Criação canônica fornece identidade nominal e origem temporal documentadas");
    }

    private Map<String, Object> acao(String identificacao) {
        return mapa("justificativa", "Ato documentado da prova " + identificacao,
                "evidencia", "SEI-LACUNAS-" + identificacao, "autorizacao", "ATO-LACUNAS-" + identificacao);
    }

    private Map<String, Object> frequencia(long vinculoId, LocalDate data) {
        return mapa("vinculoContratualId", vinculoId, "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Conferência documental do primeiro dia válido do vínculo",
                "chaveIdempotencia", "lacuna-vin005-" + UUID.randomUUID());
    }

    private String posicaoPessoa(long pessoaId, LocalDate data, Instant corte) {
        return V + "/pessoas/" + pessoaId + "/posicao?data=" + data + "&conhecidoAte=" + corte;
    }

    private JsonNode posicaoDoVinculo(JsonNode pessoa, long id) {
        for (JsonNode v : pessoa.path("vinculos")) if (v.path("vinculoId").asLong() == id) return v;
        throw new AssertionError("Vínculo esperado ausente na posição: " + id);
    }

    private JsonNode eventoDoTipo(JsonNode eventos, String tipo) {
        var encontrados = new java.util.ArrayList<JsonNode>();
        eventos.forEach(e -> { if (tipo.equals(e.path("tipo").asText())) encontrados.add(e); });
        assertThat(encontrados).as("Exatamente um evento do ato alvo: %s", tipo).hasSize(1);
        return encontrados.get(0);
    }

    private Projecao projecaoPersistida(long pessoaId) {
        return tx(() -> {
            Empregado e = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:p", Empregado.class)
                    .setParameter("p", pessoaId).getSingleResult();
            return new Projecao(e.getId(), e.getStatus().getDescricao(),
                    e.getContratos().stream().map(c -> c.getId()).sorted().toList());
        });
    }

    private List<Object[]> frequencias(long vinculoId) {
        return tx(() -> em.createQuery("select f.id, f.pessoa.id, f.vinculoContratual.id, f.dataReferencia "
                        + "from Frequencia f where f.vinculoContratual.id=:v order by f.id", Object[].class)
                .setParameter("v", vinculoId).getResultList());
    }

    private List<String> historiaPersistida(long vinculoId) {
        return tx(() -> {
            var r = new java.util.ArrayList<String>();
            VinculoContratual v = em.find(VinculoContratual.class, vinculoId);
            r.add("V|" + v.getId() + "|" + v.getPessoa().getId() + "|" + v.getDataInicio() + "|" + v.getDataFim()
                    + "|" + v.getMatricula() + "|" + v.getEstadoNegocial() + "|" + v.getVersion());
            em.createQuery("select a.id, a.dataInicio, a.dataFim, a.estadoNegocial from AlocacaoVinculo a "
                            + "where a.vinculo.id=:v order by a.id", Object[].class)
                    .setParameter("v", vinculoId).getResultList().forEach(a -> r.add("A|" + java.util.Arrays.toString(a)));
            em.createQuery("select e.id, e.registradoEm, e.hashEvento from EventoVinculoContratual e "
                            + "where e.vinculo.id=:v order by e.id", Object[].class)
                    .setParameter("v", vinculoId).getResultList().forEach(e -> r.add("EV|" + java.util.Arrays.toString(e)));
            em.createQuery("select e.id, e.registradoEm, e.hashEvento from EventoAlocacaoVinculo e "
                            + "where e.vinculo.id=:v order by e.id", Object[].class)
                    .setParameter("v", vinculoId).getResultList().forEach(e -> r.add("EA|" + java.util.Arrays.toString(e)));
            return r;
        });
    }

    private DesligamentoPersistido desligamentoPersistido(long vinculoId, long eventoId) {
        return tx(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, vinculoId);
            EventoVinculoContratual e = em.find(EventoVinculoContratual.class, eventoId);
            assertThat(e.getVinculo().getId()).isEqualTo(vinculoId);
            assertThat(e.getMotivoDesligamentoIdFotografia()).isEqualTo(v.getMotivoDesligamento().getId());
            return new DesligamentoPersistido(v.getDataFim(), v.getMotivoDesligamento().getId(),
                    v.getDesligamentoRegistradoPorId(), v.getDesligamentoRegistradoPorUsername(), v.getDesligamentoRegistradoEm(),
                    v.getMotivoDesligamentoDescricaoFotografia(), v.getMotivoDesligamentoCategoriaFotografia().name(),
                    e.getResponsavelId(), e.getResponsavelUsername(), e.getRegistradoEm(),
                    e.getMotivoDesligamentoDescricaoFotografia(), e.getMotivoDesligamentoCategoriaFotografia().name(),
                    e.getHashAnterior(), e.getHashEvento());
        });
    }

    private <T> T tx(Callable<T> acao) {
        return emTransacao(() -> {
            try { return acao.call(); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private record Projecao(long empregadoId, String status, List<Long> contratos) {}
    private record DesligamentoPersistido(LocalDate fim, Long motivoId, Long responsavelId,
            String responsavelUsername, Instant registradoEm, String descricao, String categoria,
            Long eventoResponsavelId, String eventoResponsavelUsername, Instant eventoRegistradoEm,
            String eventoDescricao, String eventoCategoria, String eventoHashAnterior, String eventoHash) {}
}
