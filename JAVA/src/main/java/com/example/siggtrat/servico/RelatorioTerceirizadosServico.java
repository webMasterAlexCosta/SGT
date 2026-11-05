package com.example.siggtrat.servico;

import com.example.siggtrat.dto.RelatorioTerceirizadosDTO;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.RelatorioTerceirizadosRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Service
public class RelatorioTerceirizadosServico {

    static final int TAMANHO_PADRAO = 100;
    static final int TAMANHO_MAXIMO = 5000;
    public static final int TAMANHO_LOTE_EXPORTACAO = 5000;

    private final RelatorioTerceirizadosRepositorio repositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final MinimizadorDadosPessoaisServico minimizador;

    public RelatorioTerceirizadosServico(
            RelatorioTerceirizadosRepositorio repositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            MinimizadorDadosPessoaisServico minimizador
    ) {
        this.repositorio = repositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.minimizador = minimizador;
    }

    @Transactional(readOnly = true)
    public RelatorioTerceirizadosDTO consultar(
            String busca,
            String status,
            Long contratoId,
            Boolean semContrato,
            Long lotacaoId,
            Integer page,
            Integer size
    ) {
        ContextoConsulta contexto = prepararContexto(busca, status, contratoId, semContrato, lotacaoId);
        int pagina = Math.max(page == null ? 0 : page, 0);
        int tamanho = Math.min(Math.max(size == null ? TAMANHO_PADRAO : size, 10), TAMANHO_MAXIMO);

        RelatorioTerceirizadosDTO resultado = repositorio.consultar(
                contexto.referencia(),
                contexto.escopo(),
                contexto.superintendenciaId(),
                contexto.gestaoLocalId(),
                contexto.busca(),
                contexto.status(),
                contexto.contratoId(),
                contexto.semContrato(),
                contexto.lotacaoId(),
                pagina,
                tamanho
        );
        return minimizar(resultado, contexto.usuario());
    }

    /**
     * Processa a base completa em lotes estáveis para exportação XLSX/PDF.
     * O cursor pelo ID evita OFFSET crescente e mantém apenas um lote em memória.
     */
    @Transactional(readOnly = true)
    public void processarEmpregadosEmLotes(
            String busca,
            String status,
            Long contratoId,
            Boolean semContrato,
            Long lotacaoId,
            int tamanhoLote,
            Consumer<List<RelatorioTerceirizadosDTO.EmpregadoResumo>> consumidor
    ) {
        ContextoConsulta contexto = prepararContexto(busca, status, contratoId, semContrato, lotacaoId);
        int limite = Math.min(Math.max(tamanhoLote, 100), TAMANHO_MAXIMO);
        long ultimoId = 0L;

        while (true) {
            List<RelatorioTerceirizadosDTO.EmpregadoResumo> lote = repositorio.buscarEmpregadosParaExportacao(
                    contexto.referencia(), contexto.superintendenciaId(), contexto.gestaoLocalId(), contexto.busca(), contexto.status(),
                    contexto.contratoId(), contexto.semContrato(), contexto.lotacaoId(), ultimoId, limite);
            if (lote.isEmpty()) return;

            consumidor.accept(lote.stream().map(item -> minimizar(item, contexto.usuario())).toList());
            ultimoId = lote.get(lote.size() - 1).id();
            if (lote.size() < limite) return;
        }
    }

    private ContextoConsulta prepararContexto(
            String busca,
            String status,
            Long contratoId,
            Boolean semContrato,
            Long lotacaoId
    ) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.podeAcessarRelatoriosGestao(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não possui acesso à visão consolidada.");
        }

        Long superintendenciaId = null;
        Long gestaoLocalId = null;
        String escopo;
        if (acessoOrganizacionalServico.ehAdministrador(usuario)) {
            escopo = "Brasil — auditoria técnica";
        } else if (acessoOrganizacionalServico.ehGestorNacional(usuario)) {
            escopo = "Brasil — Gestão Nacional";
        } else if (acessoOrganizacionalServico.ehPresidente(usuario)) {
            escopo = "Brasil — Presidência";
        } else if (acessoOrganizacionalServico.ehGestorEstadual(usuario)) {
            SuperintendenciaEstadual se = acessoOrganizacionalServico.superintendenciaEfetiva(usuario);
            if (se == null || se.getId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Gestor estadual sem Gestão Estadual/UF vinculada.");
            }
            superintendenciaId = se.getId();
            escopo = se.getNome();
        } else {
            GestaoLocal gestaoLocal = acessoOrganizacionalServico.gestaoLocalEfetiva(usuario);
            if (gestaoLocal == null || gestaoLocal.getId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gestão Local sem unidade de gestão vinculada.");
            }
            gestaoLocalId = gestaoLocal.getId();
            escopo = gestaoLocal.getNome();
        }

        String statusNormalizado = status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status)
                ? null : status.trim().toUpperCase(Locale.ROOT);
        String buscaNormalizada = busca == null ? null : busca.trim();
        if (buscaNormalizada != null && buscaNormalizada.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pesquisa deve ter no máximo 120 caracteres.");
        }

        return new ContextoConsulta(
                usuario, RelogioSistema.hoje(), escopo, superintendenciaId, gestaoLocalId, buscaNormalizada, statusNormalizado,
                contratoId, Boolean.TRUE.equals(semContrato), lotacaoId);
    }

    private RelatorioTerceirizadosDTO minimizar(RelatorioTerceirizadosDTO dto, UsuarioAplicacao usuario) {
        if (dto == null) return null;
        RelatorioTerceirizadosDTO.PaginaEmpregados pagina = dto.empregados();
        List<RelatorioTerceirizadosDTO.EmpregadoResumo> conteudo = pagina.content().stream()
                .map(item -> minimizar(item, usuario)).toList();
        return new RelatorioTerceirizadosDTO(dto.referencia(), dto.geradoEm(), dto.escopo(), dto.indicadores(),
                dto.distribuicaoStatus(), dto.distribuicaoContratos(), dto.statusDisponiveis(), dto.lotacoes(),
                dto.contratos(), new RelatorioTerceirizadosDTO.PaginaEmpregados(conteudo, pagina.totalElements(),
                        pagina.totalPages(), pagina.number(), pagina.size(), pagina.first(), pagina.last()));
    }

    private RelatorioTerceirizadosDTO.EmpregadoResumo minimizar(
            RelatorioTerceirizadosDTO.EmpregadoResumo item, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return new RelatorioTerceirizadosDTO.EmpregadoResumo(item.id(), minimizador.nome(item.nome(), nivel),
                minimizador.matricula(item.matricula(), nivel), minimizador.cpf(item.cpf(), nivel),
                item.statusDescricao(), item.lotacaoId(), item.lotacaoNome(), item.cargoNome(),
                item.jornadaDescricao(), item.dataAdmissao(), item.contratos());
    }

    private record ContextoConsulta(
            UsuarioAplicacao usuario,
            LocalDate referencia,
            String escopo,
            Long superintendenciaId,
            Long gestaoLocalId,
            String busca,
            String status,
            Long contratoId,
            boolean semContrato,
            Long lotacaoId
    ) {}
}
