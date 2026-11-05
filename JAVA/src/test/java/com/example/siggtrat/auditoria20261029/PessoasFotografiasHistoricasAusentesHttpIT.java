package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Provas persistidas de ausência documental: o mestre atual não é uma fotografia histórica. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasFotografiasHistoricasAusentesHttpIT extends ContratosHttpBase {
    enum Fotografia { NULA, VAZIA, CONHECIDA }

    @ParameterizedTest
    @EnumSource(Fotografia.class)
    void historicoContratualNaoCompletaNomeAusenteAposCorrecaoDaPessoa(Fotografia foto) throws Exception {
        HistoricoPreparado f = emTransacao(() -> {
            Cenario c = criarCenario();
            HistoricoVinculoContrato h = new HistoricoVinculoContrato();
            h.setEmpregado(c.empregadoA); h.setContrato(c.contratoA);
            h.setLotacao(c.lotacaoA); h.setJornada(c.jornadaA); h.setCargo(c.cargoA);
            h.setDataInicio(INICIO.atStartOfDay(ZoneId.of("America/Sao_Paulo")));
            h.setObservacao("Período legado preservado"); em.persist(h); em.flush();
            String nomeOriginal = h.getEmpregadoSnapshotNome();
            if (foto != Fotografia.CONHECIDA) {
                assertThat(em.createQuery("update HistoricoVinculoContrato set empregadoSnapshotNome=:nome where id=:id")
                        .setParameter("nome", ausente(foto)).setParameter("id", h.getId()).executeUpdate()).isEqualTo(1);
            }
            return new HistoricoPreparado(c, h.getId(), nomeOriginal);
        });
        String novoNome = "Nome atual posterior ao período " + f.c().pessoaA.getId();
        enviar(f.c().localA, "/api/pessoas/" + f.c().pessoaA.getId() + "/correcoes-identidade",
                mapa("tipo", "NOME", "novoValor", novoNome, "motivo", "Correção nominal comprovada",
                        "impactosEncontrados", "Cadastro atual e projeção operacional",
                        "evidencia", "SEI-NOME-LEGADO", "autorizacao", "ATO-NOME-LEGADO"), 200,
                "COL010 correção atual não atribui um nome retroativo ao histórico sem fotografia");
        JsonNode historico = consultar(f.c().localA, "/api/empregados/" + f.c().empregadoA.getId() + "/historico-contratos",
                "HIS004 COL012 nome histórico somente quando comprovado");
        assertThat(historico).hasSize(1); JsonNode item = historico.get(0);
        assertThat(item.path("id").asLong()).isEqualTo(f.id());
        conferirTexto(item, "empregadoNome", foto, f.nome());
        assertThat(item.path("numeroContrato").asText()).isEqualTo(f.c().contratoA.getNumeroContrato());
        emTransacao(() -> {
            HistoricoVinculoContrato h = em.find(HistoricoVinculoContrato.class, f.id());
            assertThat(h.getEmpregadoSnapshotNome()).isEqualTo(foto == Fotografia.CONHECIDA ? f.nome() : ausente(foto));
            assertThat(h.getEmpregado().getId()).isEqualTo(f.c().empregadoA.getId());
            assertThat(h.getEmpregado().getPessoaCanonica().getId()).isEqualTo(f.c().pessoaA.getId());
            assertThat(h.getEmpregado().getNome()).isEqualTo(novoNome);
            assertThat(h.getContrato().getId()).isEqualTo(f.c().contratoA.getId());
            assertThat(h.getDataInicio().toLocalDate()).isEqualTo(INICIO);
            return null;
        });
    }

    @ParameterizedTest
    @EnumSource(Fotografia.class)
    void listaDeAlocacoesNaoCompletaRotulosAusentesPeloCadastroAtual(Fotografia foto) throws Exception {
        AlocacaoPreparada f = prepararAlocacao(foto);
        JsonNode itens = consultar(f.c().localA, rota(f.c()) + "/alocacoes", "ALO001 HIS004 consulta preserva fotografia ausente");
        assertThat(itens).hasSize(1); JsonNode item = itens.get(0);
        assertThat(item.path("id").asLong()).isEqualTo(f.c().alocacaoA.getId());
        conferirContexto(item, f, foto); conferirFonteAlocacao(f, foto, INICIO);
    }

    @ParameterizedTest
    @EnumSource(Fotografia.class)
    void motorSemEventosNaoInfereRotulosHistoricosDoMestre(Fotografia foto) throws Exception {
        AlocacaoPreparada f = prepararAlocacao(foto);
        JsonNode item = consultar(f.c().localA, rota(f.c()) + "/alocacoes/posicao?data=" + DATA,
                "TMP004 HIS005 faixa legada sem evento usa somente os campos fotografados");
        assertThat(item.path("alocacaoId").asLong()).isEqualTo(f.c().alocacaoA.getId());
        assertThat(item.path("eventoConhecidoMaisRecenteId").isNull()).isTrue();
        assertThat(item.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        conferirContexto(item, f, foto); conferirFonteAlocacao(f, foto, INICIO);
    }

    @ParameterizedTest
    @EnumSource(Fotografia.class)
    void retificacaoDoLimiteNaoFotografaRotulosAtuaisSobreAusenciaLegada(Fotografia foto) throws Exception {
        AlocacaoPreparada f = prepararAlocacao(foto);
        // A fonte nominal é independente das fotografias de catálogo ausentes.
        // Não cria evento de Alocação nem completa qualquer rótulo da faixa.
        declararFonteNominalSintetica(f.c(), "FOTO-LIMITE-NOMINAL-" + foto);
        Map<String, Object> req = mapa("novoInicio", INICIO.plusDays(1), "justificativa", "Correção documental do início",
                "evidencia", "SEI-LIMITE-FOTO", "autorizacao", "ATO-LIMITE-FOTO");
        JsonNode previa = enviar(f.c().localA, rota(f.c()) + "/previas/retificacao-inicio", req, 200,
                "VIN014 prévia mantém IDs da faixa legada");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        enviar(f.c().localA, rota(f.c()) + "/retificar-inicio", req, 200,
                "ALO005 ajuste do limite não constitui um novo ato de escolha dos catálogos");
        JsonNode eventos = consultar(f.c().localA, rota(f.c()) + "/alocacoes/eventos",
                "HIS004 evento novo conserva a ausência ou o valor documental anterior");
        assertThat(eventos).hasSize(1); JsonNode evento = eventos.get(0);
        assertThat(evento.path("tipoEvento").asText()).isEqualTo("AJUSTE_LIMITE_VINCULO");
        assertThat(evento.path("alocacaoAnteriorId").asLong()).isEqualTo(f.c().alocacaoA.getId());
        assertThat(evento.path("alocacaoNovaId").asLong()).isEqualTo(f.c().alocacaoA.getId());
        for (String lado : List.of("Anterior", "Nova")) {
            conferirTexto(evento, "lotacao" + lado + "Nome", foto, f.lotacao());
            conferirTexto(evento, "jornada" + lado + "Descricao", foto, f.jornada());
        }
        conferirTexto(evento, "cargoAnteriorNome", foto, f.cargo());
        conferirTexto(evento, "cargoNovoNome", foto, f.cargo());
        assertThat(evento.path("inicioAnterior").asText()).isEqualTo(INICIO.toString());
        assertThat(evento.path("inicioNovo").asText()).isEqualTo(INICIO.plusDays(1).toString());
        emTransacao(() -> {
            EventoAlocacaoVinculo salvo = em.find(EventoAlocacaoVinculo.class, evento.path("id").asLong());
            String esperado = foto == Fotografia.CONHECIDA ? f.lotacao() : null;
            assertThat(salvo.getLotacaoAnteriorNome()).isEqualTo(esperado);
            assertThat(salvo.getLotacaoNovaNome()).isEqualTo(esperado);
            assertThat(salvo.getHashEvento()).isNotBlank();
            assertThat(salvo.getResponsavelId()).isEqualTo(f.c().localA.getId());
            return null;
        });
        conferirFonteAlocacao(f, foto, INICIO.plusDays(1));
    }

    @ParameterizedTest
    @EnumSource(Fotografia.class)
    void detalheDoVinculoNaoCompletaDescricaoAusenteDoMotivo(Fotografia foto) throws Exception {
        MotivoPreparado f = prepararMotivo(foto);
        JsonNode item = consultar(f.c().localA, rota(f.c()), "DES004 motivo histórico não recebe descrição renomeada do mestre");
        assertThat(item.path("motivoDesligamentoId").asLong()).isEqualTo(f.motivoId());
        conferirTexto(item, "motivoDesligamentoDescricao", foto, f.descricao());
        assertThat(item.path("dataFim").asText()).isEqualTo(DATA.plusDays(3).toString());
        conferirFonteMotivo(f, foto);
    }

    @ParameterizedTest
    @EnumSource(Fotografia.class)
    void alteracaoDaMatriculaNaoInventaDescricaoOuCategoriaDoDesligamento(Fotografia foto) throws Exception {
        MotivoPreparado f = prepararMotivo(foto);
        // Fonte sintética positiva da identidade e dos limites conhecidos.
        // Não declara descrição/categoria da causa, que continua ausente quando NULA/VAZIA.
        declararFonteNominalSintetica(f.c(), "FOTO-MATRICULA-NOMINAL-" + foto);
        enviar(f.c().localA, rota(f.c()) + "/alterar-matricula",
                mapa("dataEfetiva", DATA, "novaMatricula", "FOTO-MATRICULA-" + f.c().vinculoA.getId(),
                        "justificativa", "Ajuste documental apenas da matrícula", "evidencia", "SEI-MATRICULA-LEGADA",
                        "autorizacao", "ATO-MATRICULA-LEGADA"), 200,
                "MAT004 o novo fato de matrícula não redefine a causa histórica do término");
        JsonNode eventos = consultar(f.c().localA, rota(f.c()) + "/eventos", "DES004 leitura do novo evento auditável");
        assertThat(eventos).hasSize(2);
        JsonNode origem = eventos.get(0), evento = eventos.get(1);
        assertThat(origem.path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(origem.path("pessoaNomeFotografia").asText()).isEqualTo(f.c().pessoaA.getNome());
        assertThat(origem.path("fimNovo").asText()).isEqualTo(DATA.plusDays(3).toString());
        assertThat(origem.path("motivoDesligamentoDescricaoFotografia").isNull()).isTrue();
        assertThat(origem.path("motivoDesligamentoCategoriaFotografia").isNull()).isTrue();
        assertThat(evento.path("tipo").asText()).isEqualTo("ALTERACAO_MATRICULA");
        assertThat(evento.path("eventoPrecedenteId").asLong()).isEqualTo(origem.path("id").asLong());
        assertThat(evento.path("motivoDesligamentoIdFotografia").asLong()).isEqualTo(f.motivoId());
        conferirTexto(evento, "motivoDesligamentoDescricaoFotografia", foto, f.descricao());
        conferirTexto(evento, "motivoDesligamentoCategoriaFotografia", foto, "DESLIGAMENTO_INDIVIDUAL");
        emTransacao(() -> {
            EventoVinculoContratual salvo = em.find(EventoVinculoContratual.class, evento.path("id").asLong());
            assertThat(salvo.getMotivoDesligamentoDescricaoFotografia()).isEqualTo(foto == Fotografia.CONHECIDA ? f.descricao() : null);
            assertThat(salvo.getMotivoDesligamentoCategoriaFotografia()).isEqualTo(
                    foto == Fotografia.CONHECIDA ? CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL : null);
            assertThat(salvo.getVinculo().getId()).isEqualTo(f.c().vinculoA.getId());
            assertThat(salvo.getFimNovo()).isEqualTo(DATA.plusDays(3));
            assertThat(salvo.getHashEvento()).isNotBlank();
            assertThat(salvo.getResponsavelId()).isEqualTo(f.c().localA.getId());
            return null;
        });
        conferirFonteMotivo(f, foto);
    }

    private AlocacaoPreparada prepararAlocacao(Fotografia foto) {
        return emTransacao(() -> {
            Cenario c = criarCenario();
            AlocacaoPreparada f = new AlocacaoPreparada(c, c.alocacaoA.getLotacaoNomeFotografia(),
                    c.alocacaoA.getJornadaDescricaoFotografia(), c.alocacaoA.getCargoNomeFotografia(),
                    c.alocacaoA.getLotacaoMunicipioFotografia(), c.alocacaoA.getJornadaSemanalFotografia());
            assertThat(em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult()).isZero();
            if (foto != Fotografia.CONHECIDA) {
                assertThat(em.createQuery("update AlocacaoVinculo set lotacaoNomeFotografia=:foto, "
                                + "jornadaDescricaoFotografia=:foto, cargoNomeFotografia=:foto where id=:id")
                        .setParameter("foto", ausente(foto)).setParameter("id", c.alocacaoA.getId()).executeUpdate()).isEqualTo(1);
            }
            c.lotacaoA.setNome("Lotação renomeada depois da faixa " + c.lotacaoA.getId());
            c.jornadaA.setDescricao("Turno renomeado depois da faixa " + c.jornadaA.getId());
            c.cargoA.setNome("Cargo renomeado depois da faixa " + c.cargoA.getId());
            return f;
        });
    }

    private MotivoPreparado prepararMotivo(Fotografia foto) {
        return emTransacao(() -> {
            Cenario c = criarCenario(); MotivoDesligamento motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(c.gestaoA); motivo.setDescricao("Causa original " + c.vinculoA.getId());
            motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(motivo);
            String descricao = motivo.getDescricao();
            // Estado legado com causa relacionada e sem evento/documento nominal disponível.
            // Nenhum evento assinado preexistente é editado para produzir esta fixture.
            c.vinculoA.setDataFim(DATA.plusDays(3)); c.alocacaoA.setDataFim(DATA.plusDays(3));
            c.vinculoA.setMotivoDesligamento(motivo);
            c.vinculoA.setMotivoDesligamentoDescricaoFotografia(foto == Fotografia.CONHECIDA ? descricao : ausente(foto));
            c.vinculoA.setMotivoDesligamentoCategoriaFotografia(foto == Fotografia.CONHECIDA
                    ? CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL : null);
            motivo.setDescricao("Descrição posterior do catálogo " + motivo.getId()); motivo.setCategoria(CategoriaTerminoVinculo.OUTRO);
            em.flush();
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult()).isZero();
            return new MotivoPreparado(c, motivo.getId(), descricao);
        });
    }

    private void conferirContexto(JsonNode item, AlocacaoPreparada f, Fotografia foto) {
        assertThat(item.path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(item.path("jornadaId").asLong()).isEqualTo(f.c().jornadaA.getId());
        assertThat(item.path("cargoId").asLong()).isEqualTo(f.c().cargoA.getId());
        assertThat(item.path("composicaoBaseId").asLong()).isEqualTo(f.c().composicaoA.getId());
        conferirTexto(item, "lotacaoNome", foto, f.lotacao());
        conferirTexto(item, "jornadaDescricao", foto, f.jornada());
        conferirTexto(item, "cargoNome", foto, f.cargo());
    }

    private void conferirFonteAlocacao(AlocacaoPreparada f, Fotografia foto, java.time.LocalDate inicio) {
        emTransacao(() -> {
            AlocacaoVinculo a = em.find(AlocacaoVinculo.class, f.c().alocacaoA.getId());
            assertThat(a.getLotacaoNomeFotografia()).isEqualTo(foto == Fotografia.CONHECIDA ? f.lotacao() : ausente(foto));
            assertThat(a.getJornadaDescricaoFotografia()).isEqualTo(foto == Fotografia.CONHECIDA ? f.jornada() : ausente(foto));
            assertThat(a.getCargoNomeFotografia()).isEqualTo(foto == Fotografia.CONHECIDA ? f.cargo() : ausente(foto));
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(f.grade());
            assertThat(a.getLotacaoMunicipioFotografia()).isEqualTo(f.municipio());
            assertThat(a.getVinculo().getId()).isEqualTo(f.c().vinculoA.getId());
            assertThat(a.getDataInicio()).isEqualTo(inicio);
            return null;
        });
    }

    private void conferirFonteMotivo(MotivoPreparado f, Fotografia foto) {
        emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, f.c().vinculoA.getId());
            assertThat(v.getMotivoDesligamento().getId()).isEqualTo(f.motivoId());
            assertThat(v.getMotivoDesligamentoDescricaoFotografia()).isEqualTo(foto == Fotografia.CONHECIDA ? f.descricao() : ausente(foto));
            assertThat(v.getMotivoDesligamentoCategoriaFotografia()).isEqualTo(
                    foto == Fotografia.CONHECIDA ? CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL : null);
            assertThat(v.getDataFim()).isEqualTo(DATA.plusDays(3));
            return null;
        });
    }

    private void conferirTexto(JsonNode item, String campo, Fotografia foto, String comprovado) {
        assertThat(item.has(campo)).as("campo histórico %s deve ser explícito", campo).isTrue();
        if (foto == Fotografia.CONHECIDA) assertThat(item.path(campo).asText()).isEqualTo(comprovado);
        else assertThat(item.path(campo).isNull()).as("%s ausente não pode ser preenchido pelo mestre", campo).isTrue();
    }

    private String ausente(Fotografia foto) { return foto == Fotografia.VAZIA ? "" : null; }
    private String rota(Cenario c) { return "/api/vinculos-contratuais/" + c.vinculoA.getId(); }
    private record HistoricoPreparado(Cenario c, long id, String nome) {}
    private record AlocacaoPreparada(Cenario c, String lotacao, String jornada, String cargo, String municipio, String grade) {}
    private record MotivoPreparado(Cenario c, long motivoId, String descricao) {}
}
