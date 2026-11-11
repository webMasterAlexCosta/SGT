package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;

/** Controles por HTTP com confirmação da persistência em outra transação. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasComposicaoConsentimentoHttpIT extends ContratosHttpBase {
    private static final String ROTA = "/api/vinculos-contratuais";

    @Test
    void composicaoInexistenteRecusaVinculoSemGravarPessoaReservaOuHistorico() throws Exception {
        Preparado f = preparar();
        String cpf = cpf(Math.toIntExact(743000000L + f.contrato().id()));
        Map<String, Object> req = cadastro(f, cpf, RelogioSistema.hoje());
        req.put("composicaoId", Long.MAX_VALUE);
        List<Long> antes = contagens(f, cpf);
        enviar(f.c().localA, ROTA, req, 400,
                "CMP001 composição inexistente não pode gerar Pessoa, Vínculo, Alocação ou evento parcial");
        assertThat(contagens(f, cpf)).as("Persistência permanece idêntica após rejeição em nova transação")
                .isEqualTo(antes);
        assertThat(antes).containsOnly(0L);
    }

    @Test
    void consentimentoDeExcedenteSemJustificativaRecusaSemGravar() throws Exception {
        Preparado f = preparar();
        ocupar(f);
        String cpf = cpf(Math.toIntExact(744000000L + f.contrato().id()));
        Map<String, Object> req = cadastro(f, cpf, RelogioSistema.hoje());
        req.put("confirmarInclusaoExcedente", true);
        req.put("evidenciaExcedente", "SEI-CONSENTIMENTO-SEM-JUSTIFICATIVA");
        req.put("autorizacaoExcedente", "ATO-CONSENTIMENTO-SEM-JUSTIFICATIVA");
        List<Long> antes = contagens(f, cpf);
        enviar(f.c().localA, ROTA, req, 409,
                "CMP009 consentimento e documentos não dispensam justificativa do excedente");
        assertThat(contagens(f, cpf)).isEqualTo(antes);
        req.put("justificativaExcedente", "   ");
        enviar(f.c().localA, ROTA, req, 409,
                "CMP009 justificativa em branco também não autoriza excedente");
        assertThat(contagens(f, cpf)).isEqualTo(antes);
    }

    @Test
    void excedenteAutorizadoPersisteAutoriaDocumentosDataECapacidadeNoEvento() throws Exception {
        Preparado f = preparar();
        ocupar(f);
        LocalDate dataEfetiva = RelogioSistema.hoje();
        String cpf = cpf(Math.toIntExact(745000000L + f.contrato().id()));
        Map<String, Object> req = cadastro(f, cpf, dataEfetiva);
        req.put("confirmarInclusaoExcedente", true);
        req.put("justificativaExcedente", "Substituição extraordinária formalmente aprovada");
        req.put("evidenciaExcedente", "SEI-PROVA-EVENTO-EXCEDENTE");
        req.put("autorizacaoExcedente", "ATO-PROVA-EVENTO-EXCEDENTE");
        FatoHashExcedente esperadoDasEntradas = new FatoHashExcedente(dataEfetiva, null,
                (String) req.get("matricula"), cpf, f.contrato().id(), f.c().localA.getId(),
                f.contrato().composicao1(), f.c().lotacaoA.getId(), f.c().jornadaA.getId(), f.c().cargoA.getId(),
                1, 2L, (String) req.get("justificativaExcedente"), (String) req.get("evidenciaExcedente"),
                (String) req.get("autorizacaoExcedente"));
        String hashEsperado = hashDoFatoTipadoEsperado(esperadoDasEntradas);
        Instant antes = Instant.now().minusSeconds(1);
        JsonNode criado = enviar(f.c().localA, ROTA, req, 201,
                "CMP009 Gestor Local pode confirmar excedente com documentação completa");
        Instant depois = Instant.now().plusSeconds(1);
        long vinculoId = criado.path("id").asLong();
        JsonNode eventos = consultar(f.c().localA, ROTA + "/" + vinculoId + "/eventos",
                "CMP009 leitura nova mostra a autorização fotografada no primeiro evento");
        assertThat(eventos).hasSize(1);
        JsonNode evento = eventos.get(0);
        assertThat(evento.path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(evento.path("responsavelId").asLong()).isEqualTo(f.c().localA.getId());
        assertThat(evento.path("responsavelUsername").asText()).isEqualTo(f.c().localA.getUsername());
        assertThat(evento.path("dataEfetiva").asText()).isEqualTo(dataEfetiva.toString());
        assertThat(Instant.parse(evento.path("registradoEm").asText())).isBetween(antes, depois);
        assertThat(evento.path("justificativa").asText()).contains((String) req.get("justificativaExcedente"));
        assertThat(evento.path("evidencia").asText()).isEqualTo(req.get("evidenciaExcedente"));
        assertThat(evento.path("autorizacao").asText()).isEqualTo(req.get("autorizacaoExcedente"));
        assertThat(evento.path("inclusaoExcedente").asBoolean()).isTrue();
        assertThat(evento.path("composicaoExcedenteId").asLong()).isEqualTo(f.contrato().composicao1());
        assertThat(evento.path("lotacaoExcedenteId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(evento.path("jornadaExcedenteId").asLong()).isEqualTo(f.c().jornadaA.getId());
        assertThat(evento.path("cargoExcedenteId").asLong()).isEqualTo(f.c().cargoA.getId());
        assertThat(evento.path("efetivoPrevistoExcedente").asInt()).isEqualTo(1);
        assertThat(evento.path("ocupacaoAposInclusaoExcedente").asLong()).isEqualTo(2);
        assertThat(evento.path("hashEvento").asText()).hasSize(64).isEqualTo(hashEsperado);
        emTransacao(() -> {
            EventoVinculoContratual persistido = em.find(EventoVinculoContratual.class, evento.path("id").asLong());
            assertThat(persistido.getVinculo().getId()).isEqualTo(vinculoId);
            assertThat(persistido.getResponsavelId()).isEqualTo(f.c().localA.getId());
            assertThat(persistido.getResponsavelUsername()).isEqualTo(f.c().localA.getUsername());
            assertThat(persistido.getDataEfetiva()).isEqualTo(dataEfetiva);
            assertThat(persistido.getRegistradoEm()).isEqualTo(Instant.parse(evento.path("registradoEm").asText()));
            assertThat(persistido.getJustificativa()).isEqualTo(evento.path("justificativa").asText());
            assertThat(persistido.getEvidencia()).isEqualTo(req.get("evidenciaExcedente"));
            assertThat(persistido.getAutorizacao()).isEqualTo(req.get("autorizacaoExcedente"));
            assertThat(persistido.isInclusaoExcedente()).isTrue();
            assertThat(persistido.getEfetivoPrevistoExcedente()).isEqualTo(1);
            assertThat(persistido.getOcupacaoAposInclusaoExcedente()).isEqualTo(2L);
            assertThat(persistido.getComposicaoExcedenteId()).isEqualTo(f.contrato().composicao1());
            assertThat(persistido.getLotacaoExcedenteId()).isEqualTo(f.c().lotacaoA.getId());
            assertThat(persistido.getJornadaExcedenteId()).isEqualTo(f.c().jornadaA.getId());
            assertThat(persistido.getCargoExcedenteId()).isEqualTo(f.c().cargoA.getId());
            assertThat(persistido.getPessoaCpfFotografia()).isEqualTo(cpf);
            assertThat(persistido.getInicioNovo()).isEqualTo(dataEfetiva);
            assertThat(persistido.getFimNovo()).isNull();
            assertThat(persistido.getMatriculaNova()).isEqualTo(req.get("matricula"));
            assertThat(persistido.getHashAnterior()).isNull();
            assertThat(persistido.getHashEvento()).isEqualTo(hashEsperado).isEqualTo(evento.path("hashEvento").asText());
            return null;
        });
    }

    @Test
    void oraculoDasEntradasCorrespondeAosVetoresTipadosIndependentes() throws Exception {
        FatoHashExcedente fixo = new FatoHashExcedente(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                "MAT-001", "52998224725", 2L, 42L, null, 10L, 11L, 12L, 1, 2L,
                "Excedente autorizado", "SEI-TESTE", "ATO-TESTE");
        // Literais gerados offline por Python/struct/hashlib a partir do contrato tipado documentado.
        assertThat(digestEsperado(materialBaseDeCriacao(fixo)))
                .isEqualTo("31832814eab55d99e98befeffc8081c454a4efe24b0138bb707afbed099c0158");
        assertThat(hashDoFatoTipadoEsperado(fixo))
                .isEqualTo("531972581e0b28a4a0209a07b5daa48afa62f44e53a9acb25090caca12266a81");
    }

    private record FatoHashExcedente(LocalDate inicio, LocalDate fim, String matricula, String cpf,
                                    long contratoId, long autorId, Long composicaoId, long lotacaoId,
                                    long jornadaId, long cargoId, int previsto, long ocupacao,
                                    String justificativa, String evidencia, String autorizacao) { }

    /** Oráculo separado: fatos vêm das entradas; ByteBuffer/JDK, sem serializador ou hash do produto. */
    private byte[] materialBaseDeCriacao(FatoHashExcedente f) {
        return materialEsperado("sgt.evento-vinculo.base", List.of(
                new CampoEsperado(1, null), new CampoEsperado(5, "CRIACAO"),
                new CampoEsperado(4, f.inicio().toString()), new CampoEsperado(4, null),
                new CampoEsperado(4, f.inicio().toString()), new CampoEsperado(4, null),
                new CampoEsperado(4, f.fim() == null ? null : f.fim().toString()),
                new CampoEsperado(1, null), new CampoEsperado(1, f.matricula()), new CampoEsperado(1, f.cpf()),
                new CampoEsperado(2, Long.toString(f.contratoId())), new CampoEsperado(2, Long.toString(f.autorId())),
                new CampoEsperado(1, "Cadastro integrado de Pessoa, Vínculo e Alocação inicial"),
                new CampoEsperado(2, null), new CampoEsperado(1, null), new CampoEsperado(5, null)));
    }

    private String hashDoFatoTipadoEsperado(FatoHashExcedente f) throws Exception {
        return digestEsperado(materialEsperado("sgt.evento-vinculo.excedente", List.of(
                new CampoEsperado(1, digestEsperado(materialBaseDeCriacao(f))),
                new CampoEsperado(2, f.composicaoId() == null ? null : Long.toString(f.composicaoId())),
                new CampoEsperado(2, Long.toString(f.lotacaoId())), new CampoEsperado(2, Long.toString(f.jornadaId())),
                new CampoEsperado(2, Long.toString(f.cargoId())), new CampoEsperado(3, Integer.toString(f.previsto())),
                new CampoEsperado(2, Long.toString(f.ocupacao())), new CampoEsperado(1, f.justificativa()),
                new CampoEsperado(1, f.evidencia()), new CampoEsperado(1, f.autorizacao()))));
    }

    private record CampoEsperado(int tipo, String valor) { }

    private byte[] materialEsperado(String dominio, List<CampoEsperado> campos) {
        List<CampoEsperado> todos = new ArrayList<>();
        todos.add(new CampoEsperado(1, dominio)); todos.addAll(campos);
        int tamanho = 4 + 2 + 4;
        for (CampoEsperado f : todos) tamanho += 6 + (f.valor() == null ? 0 : f.valor().getBytes(StandardCharsets.UTF_8).length);
        ByteBuffer bytes = ByteBuffer.allocate(tamanho).order(ByteOrder.BIG_ENDIAN);
        bytes.putInt(0x53475448).putShort((short) 1);
        escreverEsperado(bytes, todos.get(0));
        bytes.putInt(campos.size());
        for (CampoEsperado f : campos) escreverEsperado(bytes, f);
        return bytes.array();
    }

    private void escreverEsperado(ByteBuffer destino, CampoEsperado f) {
        byte[] valor = f.valor() == null ? new byte[0] : f.valor().getBytes(StandardCharsets.UTF_8);
        destino.put((byte) f.tipo()).put((byte) (f.valor() == null ? 0 : 1)).putInt(valor.length).put(valor);
    }

    private String digestEsperado(byte[] material) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material));
    }

    private Preparado preparar() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                return new Preparado(c, publicado(c, 1, null, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private void ocupar(Preparado f) throws Exception {
        Map<String, Object> req = cadastro(f, cpf(Math.toIntExact(746000000L + f.contrato().id())),
                RelogioSistema.hoje().minusDays(1));
        req.put("matricula", "CMP-OCUPANTE-" + f.contrato().id());
        enviar(f.c().localA, ROTA, req, 201, "Fixture ocupa a única vaga prevista na composição publicada");
    }

    private Map<String, Object> cadastro(Preparado f, String cpf, LocalDate inicio) {
        return mapa("pessoa", mapa("nome", "Pessoa prova CMP " + cpf, "cpf", cpf),
                "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                "dataInicio", inicio, "matricula", "CMP-PROPOSTA-" + f.contrato().id());
    }

    private List<Long> contagens(Preparado f, String cpf) {
        return emTransacao(() -> List.of(
                em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class).setParameter("cpf", cpf).getSingleResult(),
                em.createQuery("select count(e) from Empregado e where e.cpf=:cpf", Long.class).setParameter("cpf", cpf).getSingleResult(),
                em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(r) from ReservaMatriculaContrato r where r.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult()));
    }

    private record Preparado(Cenario c, NovoContrato contrato) {}
}
