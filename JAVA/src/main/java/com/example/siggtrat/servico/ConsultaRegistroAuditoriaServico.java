package com.example.siggtrat.servico;

import com.example.siggtrat.dto.RegistroAuditoriaDTO;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.repositorio.RegistroAuditoriaRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ConsultaRegistroAuditoriaServico {
    private final RegistroAuditoriaRepositorio repositorio;
    private final ObjectMapper mapper;
    private static final Set<String> ROTULOS_NAO_PESSOAIS = Set.of(
            "Contrato", "ComposicaoContratual", "AditamentoContratual", "RemanejamentoContratual",
            "Cargo", "Jornada", "Turno", "Lotacao", "GestaoLocal", "Superintendencia",
            "TipoContratacao", "Banco", "Agencia", "TipoConta", "ConfiguracaoSistema");
    private static final Set<String> CAMPOS_TECNICOS = Set.of(
            "id", "ids", "antes", "depois", "quantidade", "total", "versao", "version",
            "ativo", "confirmado", "sucesso", "excedente");

    public ConsultaRegistroAuditoriaServico(RegistroAuditoriaRepositorio repositorio, ObjectMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> pesquisar(String acao, String nomeUsuario, String tipoEntidade, Long idEntidade,
                                    String idRequisicao, Instant dataInicio, Instant dataFim, String tipoEntidadeIncluida,
                                    List<Long> idsEntidadesIncluidas, Pageable pageable) {
        return repositorio.findAll(construirEspecificacao(acao, nomeUsuario, tipoEntidade, idEntidade, idRequisicao,
                dataInicio, dataFim, tipoEntidadeIncluida, idsEntidadesIncluidas), pageable).map(this::paraDTO);
    }

    @Transactional(readOnly = true)
    public RegistroAuditoriaDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    private Specification<RegistroAuditoria> construirEspecificacao(String acao, String nomeUsuario, String tipoEntidade,
            Long idEntidade, String idRequisicao, Instant dataInicio, Instant dataFim, String tipoEntidadeIncluida,
            List<Long> idsEntidadesIncluidas) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (naoVazio(acao)) predicates.add(cb.like(cb.lower(root.get("action")), "%" + acao.toLowerCase().trim() + "%"));
            if (naoVazio(nomeUsuario)) predicates.add(cb.like(cb.lower(root.get("username")), "%" + nomeUsuario.toLowerCase().trim() + "%"));
            if (naoVazio(idRequisicao)) predicates.add(cb.equal(root.get("requestId"), idRequisicao.trim()));
            if (dataInicio != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dataInicio));
            if (dataFim != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dataFim));

            Predicate main = null;
            if (naoVazio(tipoEntidade)) {
                Predicate type = cb.equal(root.get("entityType"), tipoEntidade.trim());
                main = idEntidade == null ? type : cb.and(type, cb.equal(root.get("entityId"), idEntidade));
            }
            Predicate included = null;
            if (naoVazio(tipoEntidadeIncluida) && idsEntidadesIncluidas != null && !idsEntidadesIncluidas.isEmpty()) {
                included = cb.and(cb.equal(root.get("entityType"), tipoEntidadeIncluida.trim()), root.get("entityId").in(idsEntidadesIncluidas));
            }
            if (main != null && included != null) predicates.add(cb.or(main, included));
            else if (main != null) predicates.add(main);
            else if (included != null) predicates.add(included);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean naoVazio(String value) { return value != null && !value.isBlank(); }

    private RegistroAuditoriaDTO paraDTO(RegistroAuditoria a) {
        // Esta rota é de administração técnica, sem permissão pessoal implícita.
        // O original imutável permanece intacto; a resposta não entrega corpos,
        // consultas ou narrativas livres de outra fronteira de autorização.
        String rotulo = a.getEntityType() != null && ROTULOS_NAO_PESSOAIS.contains(a.getEntityType())
                ? a.getEntityLabel() : null;
        return new RegistroAuditoriaDTO(a.getId(), a.getCreatedAt(), a.getRequestId(), a.getUserId(), a.getUsername(),
                a.getAction(), a.getEntityType(), a.getEntityId(), rotulo, a.getIp(), a.getUserAgent(),
                a.getHttpMethod(), a.getPath(), null, null, a.getPayloadTruncated(), detalhesTecnicos(a.getDetailsJson()));
    }

    private String detalhesTecnicos(String original) {
        if (original == null || original.isBlank()) return original;
        try {
            JsonNode raiz = mapper.readTree(original);
            return raiz != null && raiz.isObject() ? mapper.writeValueAsString(filtrarDetalhes(raiz)) : null;
        } catch (Exception ignorada) {
            // Registros antigos podem conter texto livre ou JSON truncado.
            return null;
        }
    }

    private JsonNode filtrarDetalhes(JsonNode valor) {
        if (valor.isObject()) {
            ObjectNode seguro = mapper.createObjectNode();
            valor.fields().forEachRemaining(campo -> {
                String nome = campo.getKey();
                if (CAMPOS_TECNICOS.contains(nome) || nome.matches("[a-zA-Z]+Ids?")) {
                    JsonNode filho = filtrarDetalhes(campo.getValue());
                    if (filho != null) seguro.set(nome, filho);
                }
            });
            return seguro;
        }
        if (valor.isArray()) {
            ArrayNode seguro = mapper.createArrayNode();
            for (JsonNode filho : valor) {
                JsonNode filtrado = filtrarDetalhes(filho);
                if (filtrado != null) seguro.add(filtrado);
            }
            return seguro;
        }
        return valor.isNumber() || valor.isBoolean() || valor.isNull() ? valor : null;
    }
}
