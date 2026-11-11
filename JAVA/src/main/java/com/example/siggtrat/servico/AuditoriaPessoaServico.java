package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.Endereco;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Auditoria da escrita canônica: captura valores, sem manter um segundo cadastro editável. */
@Service
public class AuditoriaPessoaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AuditoriaPessoaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final String MOTIVO = "Atualização de dados opcionais da Pessoa";
    private static final Map<String, String> ROTULOS = Map.of(
            "rg", "RG", "ctpsSerie", "CTPS", "nitPisPasep", "NIT/PIS/PASEP",
            "dataNascimento", "Data de nascimento", "email", "E-mail", "endereco", "Endereço");
    private final EmpregadoRepositorio empregados;
    private final ContextoEmpregadoCanonicoServico contextos;
    private final HistoricoIntegridadeServico historicos;
    private final RegistroAuditoriaServico auditoria;

    public AuditoriaPessoaServico(EmpregadoRepositorio empregados, ContextoEmpregadoCanonicoServico contextos,
                                 HistoricoIntegridadeServico historicos, RegistroAuditoriaServico auditoria) {
        this.empregados = empregados;
        this.contextos = contextos;
        this.historicos = historicos;
        this.auditoria = auditoria;
    }

    /** Cópia de valores: o endereço gerenciado poderá ser modificado na mesma transação. */
    public Map<String, Object> capturar(Pessoa pessoa) {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("rg", pessoa.getRg());
        estado.put("ctpsSerie", pessoa.getCtpsSerie());
        estado.put("nitPisPasep", pessoa.getNitPisPasep());
        estado.put("dataNascimento", pessoa.getDataNascimento());
        estado.put("email", pessoa.getEmail());
        Endereco endereco = pessoa.getEndereco();
        if (endereco == null) estado.put("endereco", null);
        else {
            Map<String, Object> valores = new LinkedHashMap<>();
            valores.put("logradouro", endereco.getLogradouro());
            valores.put("numero", endereco.getNumero());
            valores.put("bairro", endereco.getBairro());
            valores.put("cidade", endereco.getCidade());
            valores.put("uf", endereco.getUf());
            valores.put("cep", endereco.getCep());
            estado.put("endereco", valores);
        }
        return estado;
    }

    /** A prova assinada participa do commit da Pessoa; a auditoria técnica só é publicada após ele. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Pessoa pessoa, Map<String, Object> antes, UsuarioAplicacao usuario) {
        Map<String, Object> depois = capturar(pessoa);
        LocalDate dataEfetiva = RelogioSistema.hoje();
        if (!Objects.equals(antes, depois)) {
            Empregado empregado = empregados.findByPessoaCanonicaId(pessoa.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A Pessoa não possui associação operacional comprovada para registrar a alteração."));
            var contexto = contextos.resolver(empregado);
            GestaoLocal gestao = contexto == null ? null : contexto.gestao();
            if (gestao == null || gestao.getId() == null || gestao.getSuperintendencia() == null
                    || gestao.getSuperintendencia().getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O contexto canônico da Pessoa não está disponível para registrar o histórico.");
            }
            LocalDateTime registradoEm = LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
            for (var campo : depois.entrySet()) {
                Object anterior = antes.get(campo.getKey());
                if (Objects.equals(anterior, campo.getValue())) continue;
                HistoricoEmpregado historico = new HistoricoEmpregado();
                historico.setEmpregado(empregado);
                historico.setUsuario(usuario);
                historico.setDataEvento(registradoEm);
                historico.setDataEfetiva(dataEfetiva);
                historico.setGestaoLocalSnapshotId(gestao.getId());
                historico.setSuperintendenciaSnapshotId(gestao.getSuperintendencia().getId());
                historico.setDescricao(MOTIVO + ": " + ROTULOS.get(campo.getKey())
                        + " alterado de [" + valor(anterior) + "] para [" + valor(campo.getValue()) + "].");
                historicos.salvarAssinado(historico);
            }
        }
        auditoria.registrarAcaoComoUsuario("PESSOA_ATUALIZACAO_OPCIONAL", "Pessoa", pessoa.getId(),
                "Pessoa " + pessoa.getId(), RegistroAuditoriaServico.detalhes(
                        "pessoaId", pessoa.getId(), "antes", antes, "depois", depois,
                        "dataEfetiva", dataEfetiva, "motivo", MOTIVO), usuario);
    }

    private String valor(Object valor) { return valor == null ? "não informado" : String.valueOf(valor); }
}
