package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.TipoContratacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * CAT-006/CAT-007 e AUD-004: um cliente HTTP precisa obter a versão real sem
 * adivinhação para preparar e confirmar uma transição documentada.
 *
 * Proposta independente. A fixture de organização é compartilhada; cada item
 * testado nasce e é editado por HTTP. Leituras JPA são apenas oráculos, em novas
 * transações, e nunca fornecem a versão enviada ao comando de sucesso.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogosVersaoCanonicaAuditoriaIndependenteHttpIT extends ContratosHttpBase {

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(TipoCatalogoTemporal.class)
    void edicaoExpoeVersaoConfirmadaEPermiteTransicaoSemAdivinhacao(TipoCatalogoTemporal tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        String recurso = recurso(tipo);
        String nome = "Versao canonica " + UUID.randomUUID();
        Map<String, Object> cadastro = cadastro(tipo, c, nome);
        JsonNode criado = enviar(c.localA, "/api/" + recurso, cadastro,
                tipo == TipoCatalogoTemporal.CARGO ? 200 : 201,
                "CAT criação HTTP de item novo, sem referências contratuais: " + tipo);
        assertThat(criado.hasNonNull("id")).isTrue();
        long id = criado.get("id").longValue();
        assertThat(id).isPositive();
        Estado antes = estadoPersistido(tipo, id);
        assertThat(antes.ativo()).isTrue();

        // Alterar um escalar do próprio agregado garante uma alteração real da
        // linha versionada. Não se presume que a primeira versão seja zero.
        cadastro.put(campoNome(tipo), nome + " editado");
        JsonNode respostaPut = dados(executar(c.localA, put("/api/" + recurso + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(json(cadastro)),
                200, "CAT edição HTTP confirmada antes de consumir version: " + tipo));
        Estado aposEdicao = estadoPersistido(tipo, id);
        assertThat(aposEdicao.versao()).as("A edição deve consumir uma versão real: %s", tipo)
                .isGreaterThan(antes.versao());
        assertThat(aposEdicao.ativo()).isTrue();

        JsonNode consultado = consultar(c.localA, "/api/" + recurso + "/" + id,
                "CAT consulta canônica após commit deve expor version: " + tipo);
        long versaoHttp = numeroExplicito(consultado, "version", "GET canônico " + tipo);
        assertThat(versaoHttp).as("GET e persistência confirmada devem concordar: %s", tipo)
                .isEqualTo(aposEdicao.versao());
        assertThat(versaoHttp).isGreaterThan(antes.versao());

        String rota = "/api/catalogos-temporais/" + tipo + "/" + id + "/transicoes";
        Map<String, Object> ato = mapa("dataEfetiva", RelogioSistema.hoje(), "ativo", false,
                "motivo", "Inativação documentada depois de editar o cadastro",
                "evidencia", "SEI-VERSAO-" + tipo + "-" + id,
                "autorizacao", "ATO-VERSAO-" + tipo + "-" + id,
                "versaoEsperada", versaoHttp, "chaveIdempotencia", UUID.randomUUID().toString());
        long eventosAntes = contarEventos(tipo, id);
        JsonNode previa = enviar(c.localA, rota + "/previa", ato, 200,
                "AUD004 versão obtida exclusivamente do GET permite preparar a transição: " + tipo);
        assertThat(numeroExplicito(previa, "versaoAtual", "Prévia " + tipo)).isEqualTo(versaoHttp);
        assertThat(previa.hasNonNull("tokenPrevia")).isTrue();
        assertThat(previa.get("tokenPrevia").isTextual()).isTrue();
        assertThat(previa.get("tokenPrevia").textValue()).isNotBlank();
        ato.put("tokenPrevia", previa.get("tokenPrevia").textValue());
        ato.put("confirmarImpacto", true);

        // A versão anterior foi observada em uma transação concluída, e ficou
        // obsoleta pela edição acima; não se inventa um número arbitrário.
        Map<String, Object> obsoleto = new LinkedHashMap<>(ato);
        obsoleto.put("versaoEsperada", antes.versao());
        obsoleto.put("chaveIdempotencia", UUID.randomUUID().toString());
        enviar(c.localA, rota + "/previa", obsoleto, 409,
                "AUD004 prévia rejeita a versão anterior à edição: " + tipo);
        enviar(c.localA, rota, obsoleto, 409,
                "AUD004 confirmação rejeita a versão anterior à edição: " + tipo);
        assertThat(contarEventos(tipo, id)).as("Conflito não registra transição: %s", tipo).isEqualTo(eventosAntes);
        assertThat(estadoPersistido(tipo, id)).as("Conflito não altera o agregado: %s", tipo).isEqualTo(aposEdicao);

        JsonNode registrado = enviar(c.localA, rota, ato, 200,
                "CAT006 transição efetiva usando versão e token recebidos por HTTP: " + tipo);
        assertThat(numeroExplicito(registrado, "catalogoId", "Recibo " + tipo)).isEqualTo(id);
        assertThat(numeroExplicito(registrado, "eventoId", "Recibo " + tipo)).isPositive();
        assertThat(registrado.path("ativo").asBoolean()).isFalse();
        assertThat(registrado.path("ativoAtualAposRegistro").asBoolean()).isFalse();
        Estado aposTransicao = estadoPersistido(tipo, id);
        assertThat(aposTransicao.ativo()).isFalse();
        assertThat(aposTransicao.versao()).isGreaterThan(versaoHttp);
        assertThat(numeroExplicito(registrado, "versaoAtual", "Recibo " + tipo))
                .isEqualTo(aposTransicao.versao());
        assertThat(contarEventos(tipo, id)).isEqualTo(eventosAntes + 1);

        JsonNode finalHttp = consultar(c.localA, "/api/" + recurso + "/" + id,
                "CAT consulta canônica confirma estado e versão após transição: " + tipo);
        assertThat(finalHttp.path("ativo").asBoolean()).isFalse();
        assertThat(numeroExplicito(finalHttp, "version", "GET após transição " + tipo))
                .isEqualTo(aposTransicao.versao());
        JsonNode disponibilidade = consultar(c.localA,
                "/api/catalogos-temporais/" + tipo + "/" + id + "/disponibilidade?data=" + RelogioSistema.hoje(),
                "CAT007 disponibilidade efetiva possui a fonte recém-confirmada: " + tipo);
        assertThat(disponibilidade.path("disponibilidadeComprovada").asBoolean()).isTrue();
        assertThat(disponibilidade.hasNonNull("ativo")).isTrue();
        assertThat(disponibilidade.get("ativo").booleanValue()).isFalse();

        // Mantém os controles funcionais acima executáveis nos tipos cujo GET já
        // possui version, mesmo se o DTO do PUT foi montado antes do flush.
        // O oráculo é o GET/JPA imediatamente após o PUT, antes da transição.
        long versaoRespostaPut = numeroExplicito(respostaPut, "version", "Resposta PUT " + tipo);
        assertThat(versaoRespostaPut).as("PUT deve devolver a versão confirmada, não a anterior: %s", tipo)
                .isEqualTo(versaoHttp);
    }

    private long numeroExplicito(JsonNode dto, String campo, String origem) {
        assertThat(dto.hasNonNull(campo)).as("%s precisa expor %s explicitamente; ausência não equivale a zero", origem, campo)
                .isTrue();
        JsonNode valor = dto.get(campo);
        assertThat(valor.isIntegralNumber()).as("%s.%s precisa ser número inteiro", origem, campo).isTrue();
        assertThat(valor.canConvertToLong()).as("%s.%s precisa caber em long", origem, campo).isTrue();
        assertThat(valor.longValue()).as("%s.%s não pode ser negativo", origem, campo).isGreaterThanOrEqualTo(0L);
        return valor.longValue();
    }

    private Map<String, Object> cadastro(TipoCatalogoTemporal tipo, Cenario c, String nome) {
        Map<String, Object> body = mapa(campoNome(tipo), nome, "ativo", true, "gestaoLocalId", c.gestaoA.getId());
        if (tipo == TipoCatalogoTemporal.TIPO_CONTRATACAO) body.put("matriculaVinculoObrigatoria", false);
        if (tipo == TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO) body.put("categoria", "DESLIGAMENTO_INDIVIDUAL");
        if (tipo == TipoCatalogoTemporal.LOTACAO) {
            body.put("municipio", "CURITIBA");
            body.put("turnos", List.of(mapa("id", c.jornadaA.getId())));
        }
        if (tipo == TipoCatalogoTemporal.JORNADA) {
            List<Map<String, Object>> dias = new ArrayList<>();
            for (DayOfWeek dia : DayOfWeek.values()) {
                Map<String, Object> item = mapa("diaSemana", dia.name(), "diaTrabalho", dia.getValue() <= 5);
                if (dia.getValue() <= 5) {
                    item.put("entrada1", "08:00");
                    item.put("saida1", "16:00");
                }
                dias.add(item);
            }
            body.put("dias", dias);
        }
        return body;
    }

    private String campoNome(TipoCatalogoTemporal tipo) {
        return tipo == TipoCatalogoTemporal.CARGO || tipo == TipoCatalogoTemporal.LOTACAO ? "nome" : "descricao";
    }

    private String recurso(TipoCatalogoTemporal tipo) {
        return switch (tipo) {
            case CARGO -> "cargos";
            case JORNADA -> "jornadas";
            case LOTACAO -> "lotacoes";
            case TIPO_CONTRATACAO -> "tipos-contratacao";
            case MOTIVO_DESLIGAMENTO -> "motivos-desligamento";
        };
    }

    private record Estado(long versao, Boolean ativo) {}

    private Estado estadoPersistido(TipoCatalogoTemporal tipo, long id) {
        return emTransacao(() -> {
            em.clear();
            return switch (tipo) {
                case CARGO -> { Cargo e = em.find(Cargo.class, id); yield new Estado(e.getVersion(), e.getAtivo()); }
                case JORNADA -> { Jornada e = em.find(Jornada.class, id); yield new Estado(e.getVersion(), e.getAtivo()); }
                case LOTACAO -> { Lotacao e = em.find(Lotacao.class, id); yield new Estado(e.getVersion(), e.getAtivo()); }
                case TIPO_CONTRATACAO -> { TipoContratacao e = em.find(TipoContratacao.class, id); yield new Estado(e.getVersion(), e.getAtivo()); }
                case MOTIVO_DESLIGAMENTO -> { MotivoDesligamento e = em.find(MotivoDesligamento.class, id); yield new Estado(e.getVersion(), e.getAtivo()); }
            };
        });
    }

    private long contarEventos(TipoCatalogoTemporal tipo, long id) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e "
                        + "where e.tipoCatalogo=:tipo and e.catalogoId=:id", Long.class)
                .setParameter("tipo", tipo).setParameter("id", id).getSingleResult());
    }
}
