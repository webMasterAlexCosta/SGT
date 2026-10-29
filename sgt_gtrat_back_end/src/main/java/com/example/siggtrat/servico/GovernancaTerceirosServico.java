package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@org.springframework.stereotype.Service
@Transactional
public class GovernancaTerceirosServico {

    private final FornecedorRepositorio fornecedorRepositorio;
    private final TabelaTarifariaRepositorio tabelaTarifariaRepositorio;
    private final ProcessoTerceiroRepositorio processoRepositorio;
    private final FolhaHorasRepositorio folhaHorasRepositorio;
    private final FaturaRepositorio faturaRepositorio;
    private final AvaliacaoRiscoFornecedorRepositorio riscoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final RelatorioFrequenciaConsolidadoServico relatorioFrequenciaServico;

    public GovernancaTerceirosServico(FornecedorRepositorio fornecedorRepositorio,
                                         TabelaTarifariaRepositorio tabelaTarifariaRepositorio,
                                         ProcessoTerceiroRepositorio processoRepositorio,
                                         FolhaHorasRepositorio folhaHorasRepositorio,
                                         FaturaRepositorio faturaRepositorio,
                                         AvaliacaoRiscoFornecedorRepositorio riscoRepositorio,
                                         ContratoRepositorio contratoRepositorio,
                                         CargoRepositorio cargoRepositorio,
                                         EmpregadoRepositorio empregadoRepositorio,
                                         AcessoOrganizacionalServico acessoOrganizacionalServico,
                                         RelatorioFrequenciaConsolidadoServico relatorioFrequenciaServico) {
        this.fornecedorRepositorio = fornecedorRepositorio;
        this.tabelaTarifariaRepositorio = tabelaTarifariaRepositorio;
        this.processoRepositorio = processoRepositorio;
        this.folhaHorasRepositorio = folhaHorasRepositorio;
        this.faturaRepositorio = faturaRepositorio;
        this.riscoRepositorio = riscoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.cargoRepositorio = cargoRepositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.relatorioFrequenciaServico = relatorioFrequenciaServico;
    }
    @Transactional(readOnly = true)
    public GovernancaResumoDTO resumo() {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.ehGlobal(usuario)) {
            Long gestaoLocalId = Optional.ofNullable(acessoOrganizacionalServico.gestaoLocalEfetiva(usuario))
                    .map(GestaoLocal::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem Gestão Local."));
            return new GovernancaResumoDTO(
                    contratoRepositorio.countFornecedoresByGestaoLocalId(gestaoLocalId),
                    processoRepositorio.countByContratoGestaoLocalIdAndStatusNotIn(gestaoLocalId, Set.of("CONCLUIDO", "CANCELADO")),
                    folhaHorasRepositorio.countByContratoGestaoLocalIdAndStatusIn(gestaoLocalId, Set.of("RASCUNHO", "SUBMETIDO", "REJEITADO")),
                    faturaRepositorio.countByContratoGestaoLocalIdAndStatusNotIn(gestaoLocalId, Set.of("PAGA", "CANCELADA")),
                    riscoRepositorio.countVencidasByGestaoLocalId(gestaoLocalId, LocalDate.now()));
        }
        return new GovernancaResumoDTO(
                fornecedorRepositorio.count(),
                processoRepositorio.countByStatusNotIn(Set.of("CONCLUIDO", "CANCELADO")),
                folhaHorasRepositorio.countByStatusIn(Set.of("RASCUNHO", "SUBMETIDO", "REJEITADO")),
                faturaRepositorio.countByStatusNotIn(Set.of("PAGA", "CANCELADA")),
                riscoRepositorio.countByProximaAvaliacaoBefore(LocalDate.now()));
    }
    @Transactional(readOnly = true)
    public List<FornecedorGovernancaDTO> fornecedores() {
        return fornecedorRepositorio.findAllByOrderByRazaoSocialAsc().stream().map(this::fornecedorDTO).toList();
    }
    public FornecedorGovernancaDTO criarFornecedor(FornecedorGovernancaEntradaDTO dto) {
        Map<String, Object> dados = dados(dto);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setRazaoSocial(textoObrigatorio(dados, "razaoSocial"));
        fornecedor.setCnpj(textoObrigatorio(dados, "cnpj"));
        fornecedor.setEmail(texto(dados.get("email")));
        return fornecedorDTO(fornecedorRepositorio.save(fornecedor));
    }
    public FornecedorGovernancaDTO atualizarFornecedor(Long id, FornecedorGovernancaEntradaDTO dto) {
        Map<String, Object> dados = dados(dto);
        Fornecedor fornecedor = fornecedorRepositorio.findById(id).orElseThrow(() -> naoEncontrado("Fornecedor"));
        fornecedor.setRazaoSocial(textoObrigatorio(dados, "razaoSocial"));
        fornecedor.setCnpj(textoObrigatorio(dados, "cnpj"));
        fornecedor.setEmail(texto(dados.get("email")));
        if (dto != null && dto.ativo() != null) fornecedor.setAtivo(dto.ativo());
        return fornecedorDTO(fornecedorRepositorio.save(fornecedor));
    }
    public ContratoFornecedorGovernancaDTO vincularFornecedor(Long contratoId, Long fornecedorId) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(usuario);
        Contrato contrato = contratoVisivel(contratoId);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contrato publicado não pode ter fornecedor alterado diretamente. Utilize Aditamento contratual."
            );
        }
        contrato.setFornecedor(fornecedorRepositorio.findById(fornecedorId)
                .orElseThrow(() -> naoEncontrado("Fornecedor")));
        contratoRepositorio.save(contrato);
        return new ContratoFornecedorGovernancaDTO(contratoId, fornecedorDTO(contrato.getFornecedor()));
    }
    @Transactional(readOnly = true)
    public List<TabelaTarifariaGovernancaDTO> tabelasTarifarias(Long contratoId) {
        contratoVisivel(contratoId);
        return tabelaTarifariaRepositorio.findByContratoIdOrderByVigenciaInicioDesc(contratoId).stream().map(this::tabelaTarifariaDTO).toList();
    }
    public TabelaTarifariaGovernancaDTO criarTabelaTarifaria(TabelaTarifariaGovernancaEntradaDTO dto) {
        Map<String, Object> dados = dados(dto);
        Contrato contrato = contratoVisivel(longObrigatorio(dados, "contratoId"));
        TabelaTarifaria rate = new TabelaTarifaria();
        rate.setContrato(contrato);
        Long cargoId = longOpcional(dados.get("cargoId"));
        if (cargoId != null) rate.setCargo(cargoRepositorio.findById(cargoId).orElseThrow(() -> naoEncontrado("Cargo")));
        rate.setVigenciaInicio(dataObrigatoria(dados, "vigenciaInicio"));
        rate.setVigenciaFim(dataOpcional(dados.get("vigenciaFim")));
        rate.setValorHora(decimalObrigatorio(dados, "valorHora"));
        if (dados.get("adicionalNoturnoPercent") != null)
            rate.setAdicionalNoturnoPercent(decimalObrigatorio(dados, "adicionalNoturnoPercent"));
        validarSobreposicaoTabelaTarifaria(rate);
        return tabelaTarifariaDTO(tabelaTarifariaRepositorio.save(rate));
    }
    @Transactional(readOnly = true)
    public List<ProcessoTerceiroGovernancaDTO> processos() {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        return processoRepositorio.findTop200ByOrderByCriadoEmDesc().stream()
                .filter(item -> acessoOrganizacionalServico.podeAcessarContrato(usuario, item.getContrato()))
                .map(this::processoDTO).toList();
    }
    public ProcessoTerceiroGovernancaDTO criarProcesso(ProcessoTerceiroEntradaDTO dto) {
        Map<String, Object> dados = dados(dto);
        Empregado empregado = empregadoRepositorio.findById(longObrigatorio(dados, "empregadoId"))
                .orElseThrow(() -> naoEncontrado("Empregado"));
        acessoOrganizacionalServico.exigirConsultaEmpregado(acessoOrganizacionalServico.usuarioAtual(), empregado);
        Contrato contrato = contratoVisivel(longObrigatorio(dados, "contratoId"));
        ProcessoTerceiro processo = new ProcessoTerceiro();
        processo.setEmpregado(empregado); processo.setContrato(contrato);
        processo.setTipo(textoObrigatorio(dados, "tipo").toUpperCase(Locale.ROOT));
        processo.setPrazo(dataObrigatoria(dados, "prazo"));
        processo.setChecklistJson(texto(dados.get("checklistJson")));
        processo.setObservacao(texto(dados.get("observacao")));
        return processoDTO(processoRepositorio.save(processo));
    }
    public ProcessoTerceiroGovernancaDTO statusProcesso(Long id, StatusGovernancaDTO dto) {
        Map<String, Object> dados = dados(dto);
        ProcessoTerceiro processo = processoRepositorio.findById(id).orElseThrow(() -> naoEncontrado("Processo"));
        acessoOrganizacionalServico.exigirAcessoContrato(acessoOrganizacionalServico.usuarioAtual(), processo.getContrato());
        String novoStatus = textoObrigatorio(dados, "status").toUpperCase(Locale.ROOT);
        validarTransicaoProcesso(processo.getStatus(), novoStatus);
        processo.setStatus(novoStatus);
        if ("CONCLUIDO".equals(processo.getStatus())) processo.setConcluidoEm(LocalDateTime.now());
        return processoDTO(processoRepositorio.save(processo));
    }
    @Transactional(readOnly = true)
    public List<FolhaHorasGovernancaDTO> folhasHoras() {
        return folhaHorasRepositorio.findTop200ByOrderByPeriodoInicioDesc().stream()
                .filter(item -> acessoOrganizacionalServico.podeAcessarContrato(acessoOrganizacionalServico.usuarioAtual(), item.getContrato()))
                .map(this::folhaHorasDTO).toList();
    }
    public FolhaHorasGovernancaDTO gerarFolhaHoras(FolhaHorasGeracaoDTO dto) {
        Map<String, Object> dados = dados(dto);
        Contrato contrato = contratoVisivel(longObrigatorio(dados, "contratoId"));
        LocalDate inicio = dataObrigatoria(dados, "inicio");
        LocalDate fim = dataObrigatoria(dados, "fim");
        RelatorioFrequenciaConsolidadoDTO relatorio = relatorioFrequenciaServico.gerar(inicio, fim, List.of(contrato.getId()), null, null);
        List<TabelaTarifaria> rates = tabelaTarifariaRepositorio.findByContratoIdOrderByVigenciaInicioDesc(contrato.getId()).stream()
                .filter(rate -> Boolean.TRUE.equals(rate.getAtivo()))
                .filter(rate -> !fim.isBefore(rate.getVigenciaInicio()))
                .filter(rate -> rate.getVigenciaFim() == null || !inicio.isAfter(rate.getVigenciaFim()))
                .toList();
        if (rates.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cadastre um tabela tarifaria vigente antes de gerar a folha de horas.");

        BigDecimal valor = BigDecimal.ZERO;
        for (RelatorioFrequenciaConsolidadoDTO.Linha linha : relatorio.registros()) {
            for (RelatorioFrequenciaConsolidadoDTO.Dia dia : linha.diario().values()) {
                if (dia.minutosPagaveis() <= 0) continue;
                TabelaTarifaria rate = selecionarTabelaTarifaria(rates, linha.cargoId(), linha.cargo(), dia.data());
                BigDecimal hora = rate.getValorHora();
                BigDecimal base = BigDecimal.valueOf(dia.minutosPagaveis())
                        .divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP).multiply(hora);
                BigDecimal noturno = BigDecimal.valueOf(dia.minutosNoturnosReduzidos())
                        .divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP).multiply(hora)
                        .multiply(rate.getAdicionalNoturnoPercent())
                        .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
                valor = valor.add(base).add(noturno);
            }
        }

        FolhaHoras folhaHoras = folhaHorasRepositorio.findByContratoIdAndPeriodoInicioAndPeriodoFim(contrato.getId(), inicio, fim)
                .orElseGet(FolhaHoras::new);
        if (Set.of("APROVADO", "FATURADO").contains(folhaHoras.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Folha de horas aprovada/faturada é imutável; gere uma competência de ajuste.");
        folhaHoras.setContrato(contrato); folhaHoras.setPeriodoInicio(inicio); folhaHoras.setPeriodoFim(fim);
        folhaHoras.setStatus("RASCUNHO"); folhaHoras.setCalculoVersao(relatorio.calculoVersao());
        folhaHoras.setMinutosFisicos(relatorio.totais().minutosFisicos());
        folhaHoras.setMinutosNoturnos(relatorio.totais().minutosNoturnosReduzidos());
        folhaHoras.setMinutosAdicionais(relatorio.totais().minutosAdicionais());
        folhaHoras.setMinutosPagaveis(relatorio.totais().minutosPagaveis());
        folhaHoras.setValorApurado(valor.setScale(2, RoundingMode.HALF_UP));
        return folhaHorasDTO(folhaHorasRepositorio.save(folhaHoras));
    }
    public FolhaHorasGovernancaDTO submeter(Long id) {
        FolhaHoras item = folhaHorasVisivel(id);
        if (!Set.of("RASCUNHO", "REJEITADO").contains(item.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente rascunho ou rejeitado pode ser submetido.");
        item.setStatus("SUBMETIDO"); item.setSubmetidoPor(acessoOrganizacionalServico.usuarioAtual().getUsername());
        item.setSubmetidoEm(LocalDateTime.now()); item.setMotivoRejeicao(null);
        return folhaHorasDTO(folhaHorasRepositorio.save(item));
    }
    public FolhaHorasGovernancaDTO aprovar(Long id) {
        FolhaHoras item = folhaHorasVisivel(id);
        String aprovador = acessoOrganizacionalServico.usuarioAtual().getUsername();
        if (!"SUBMETIDO".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "FolhaHoras não está submetido.");
        if (Objects.equals(aprovador, item.getSubmetidoPor()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Segregação de função: quem submeteu não pode aprovar.");
        item.setStatus("APROVADO"); item.setAprovadoPor(aprovador); item.setAprovadoEm(LocalDateTime.now());
        return folhaHorasDTO(folhaHorasRepositorio.save(item));
    }
    public FolhaHorasGovernancaDTO rejeitar(Long id, MotivoGovernancaDTO dto) {
        Map<String, Object> dados = dados(dto);
        FolhaHoras item = folhaHorasVisivel(id);
        if (!"SUBMETIDO".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "FolhaHoras não está submetido.");
        item.setStatus("REJEITADO"); item.setMotivoRejeicao(textoObrigatorio(dados, "motivo"));
        return folhaHorasDTO(folhaHorasRepositorio.save(item));
    }
    @Transactional(readOnly = true)
    public List<FaturaGovernancaDTO> faturas() {
        return faturaRepositorio.findTop200ByOrderByCriadoEmDesc().stream()
                .filter(item -> acessoOrganizacionalServico.podeAcessarContrato(acessoOrganizacionalServico.usuarioAtual(), item.getContrato()))
                .map(this::faturaDTO).toList();
    }
    public FaturaGovernancaDTO gerarFatura(Long folhaHorasId, FaturaGeracaoDTO dto) {
        Map<String, Object> dados = dados(dto);
        FolhaHoras folhaHoras = folhaHorasVisivel(folhaHorasId);
        if (!"APROVADO".equals(folhaHoras.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente folha de horas aprovada pode gerar faturamento.");
        if (faturaRepositorio.findByFolhaHorasId(folhaHorasId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta folha de horas já possui fatura.");
        BigDecimal glosas = dados != null && dados.get("glosas") != null ? decimalObrigatorio(dados, "glosas") : BigDecimal.ZERO;
        if (glosas.signum() < 0 || glosas.compareTo(folhaHoras.getValorApurado()) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Glosa inválida.");
        Fatura fatura = new Fatura(); fatura.setContrato(folhaHoras.getContrato()); fatura.setFolhaHoras(folhaHoras);
        fatura.setValorBruto(folhaHoras.getValorApurado()); fatura.setGlosas(glosas);
        fatura.setValorLiquido(folhaHoras.getValorApurado().subtract(glosas));
        folhaHoras.setStatus("FATURADO"); folhaHorasRepositorio.save(folhaHoras);
        return faturaDTO(faturaRepositorio.save(fatura));
    }
    public FaturaGovernancaDTO statusFatura(Long id, FaturaStatusDTO dto) {
        Map<String, Object> dados = dados(dto);
        Fatura fatura = faturaRepositorio.findById(id).orElseThrow(() -> naoEncontrado("Fatura"));
        acessoOrganizacionalServico.exigirAcessoContrato(acessoOrganizacionalServico.usuarioAtual(), fatura.getContrato());
        String status = textoObrigatorio(dados, "status").toUpperCase(Locale.ROOT);
        if (!Set.of("RASCUNHO", "EMITIDA", "PAGA", "CANCELADA").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de fatura inválido.");
        validarTransicaoFatura(fatura.getStatus(), status);
        String documento = texto(dados.get("numeroDocumento"));
        if (documento != null && !documento.isBlank()) fatura.setNumeroDocumento(documento);
        if (Set.of("EMITIDA", "PAGA").contains(status)
                && (fatura.getNumeroDocumento() == null || fatura.getNumeroDocumento().isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o número do documento fiscal.");
        fatura.setStatus(status);
        if ("EMITIDA".equals(status)) fatura.setEmitidoEm(LocalDateTime.now());
        if ("PAGA".equals(status)) fatura.setPagoEm(LocalDateTime.now());
        return faturaDTO(faturaRepositorio.save(fatura));
    }
    @Transactional(readOnly = true)
    public List<RiscoFornecedorGovernancaDTO> riscos() {
        return riscoRepositorio.findTop200ByOrderByAvaliadoEmDesc().stream().map(this::riscoDTO).toList();
    }
    public RiscoFornecedorGovernancaDTO avaliarRisco(RiscoFornecedorEntradaDTO dto) {
        Map<String, Object> dados = dados(dto);
        Fornecedor fornecedor = fornecedorRepositorio.findById(longObrigatorio(dados, "fornecedorId"))
                .orElseThrow(() -> naoEncontrado("Fornecedor"));
        int trabalhista = score(dados, "scoreTrabalhista"), financeiro = score(dados, "scoreFinanceiro");
        int compliance = score(dados, "scoreCompliance"), seguranca = score(dados, "scoreSeguranca");
        int total = (int) Math.round(trabalhista * .35 + financeiro * .20 + compliance * .30 + seguranca * .15);
        AvaliacaoRiscoFornecedor avaliacao = new AvaliacaoRiscoFornecedor(); avaliacao.setFornecedor(fornecedor);
        avaliacao.setAvaliadoEm(LocalDate.now()); avaliacao.setProximaAvaliacao(dataObrigatoria(dados, "proximaAvaliacao"));
        avaliacao.setScoreTrabalhista(trabalhista); avaliacao.setScoreFinanceiro(financeiro);
        avaliacao.setScoreCompliance(compliance); avaliacao.setScoreSeguranca(seguranca); avaliacao.setScoreTotal(total);
        avaliacao.setEvidencias(texto(dados.get("evidencias"))); avaliacao.setAvaliadoPor(acessoOrganizacionalServico.usuarioAtual().getUsername());
        fornecedor.setRiscoScore(total); fornecedor.setRiscoNivel(total >= 75 ? "CRITICO" : total >= 50 ? "ALTO" : total >= 25 ? "MEDIO" : "BAIXO");
        fornecedor.setProximaAvaliacao(avaliacao.getProximaAvaliacao()); fornecedor.setMonitoramentoStatus("EM_DIA"); fornecedorRepositorio.save(fornecedor);
        return riscoDTO(riscoRepositorio.save(avaliacao));
    }

    private Contrato contratoVisivel(Long id) {
        Contrato contrato = contratoRepositorio.findById(id).orElseThrow(() -> naoEncontrado("Contrato"));
        acessoOrganizacionalServico.exigirAcessoContrato(acessoOrganizacionalServico.usuarioAtual(), contrato); return contrato;
    }
    private FolhaHoras folhaHorasVisivel(Long id) {
        FolhaHoras item = folhaHorasRepositorio.findById(id).orElseThrow(() -> naoEncontrado("FolhaHoras"));
        acessoOrganizacionalServico.exigirAcessoContrato(acessoOrganizacionalServico.usuarioAtual(), item.getContrato()); return item;
    }
    private void validarSobreposicaoTabelaTarifaria(TabelaTarifaria novo) {
        Long novoCargo = novo.getCargo() != null ? novo.getCargo().getId() : null;
        boolean sobreposto = tabelaTarifariaRepositorio.findByContratoIdOrderByVigenciaInicioDesc(novo.getContrato().getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> Objects.equals(novoCargo, item.getCargo() != null ? item.getCargo().getId() : null))
                .anyMatch(item -> !fimVigencia(novo).isBefore(item.getVigenciaInicio())
                        && !fimVigencia(item).isBefore(novo.getVigenciaInicio()));
        if (sobreposto) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Já existe tabela tarifaria ativo para este contrato/cargo em período sobreposto.");
    }
    private LocalDate fimVigencia(TabelaTarifaria item) {
        return item.getVigenciaFim() != null ? item.getVigenciaFim() : LocalDate.of(9999, 12, 31);
    }
    private TabelaTarifaria selecionarTabelaTarifaria(List<TabelaTarifaria> rates, Long cargoId, String cargo, LocalDate data) {
        List<TabelaTarifaria> vigentes = rates.stream()
                .filter(item -> !data.isBefore(item.getVigenciaInicio()))
                .filter(item -> item.getVigenciaFim() == null || !data.isAfter(item.getVigenciaFim()))
                .toList();
        return vigentes.stream().filter(item -> item.getCargo() != null
                        && Objects.equals(item.getCargo().getId(), cargoId)).findFirst()
                .orElseGet(() -> vigentes.stream().filter(item -> item.getCargo() == null).findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "Tabela tarifaria ausente para " + cargo + " em " + data + ".")));
    }
    private void validarTransicaoProcesso(String atual, String novo) {
        if (Objects.equals(atual, novo)) return;
        Map<String, Set<String>> transicoes = Map.of(
                "PENDENTE", Set.of("EM_ANDAMENTO", "BLOQUEADO", "CANCELADO"),
                "EM_ANDAMENTO", Set.of("CONCLUIDO", "BLOQUEADO", "CANCELADO"),
                "BLOQUEADO", Set.of("EM_ANDAMENTO", "CANCELADO"),
                "CONCLUIDO", Set.of(), "CANCELADO", Set.of());
        if (!transicoes.getOrDefault(atual, Set.of()).contains(novo))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transição de processo inválida: " + atual + " → " + novo + ".");
    }
    private void validarTransicaoFatura(String atual, String novo) {
        if (Objects.equals(atual, novo)) return;
        Map<String, Set<String>> transicoes = Map.of(
                "RASCUNHO", Set.of("EMITIDA", "CANCELADA"),
                "EMITIDA", Set.of("PAGA", "CANCELADA"),
                "PAGA", Set.of(), "CANCELADA", Set.of());
        if (!transicoes.getOrDefault(atual, Set.of()).contains(novo))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transição de fatura inválida: " + atual + " → " + novo + ".");
    }
    private ResponseStatusException naoEncontrado(String nome) { return new ResponseStatusException(HttpStatus.NOT_FOUND, nome + " não encontrado."); }
    private String texto(Object valor) { return valor == null ? null : String.valueOf(valor).trim(); }
    private String textoObrigatorio(Map<String, Object> dados, String campo) {
        String valor = texto(dados.get(campo)); if (valor == null || valor.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " é obrigatório."); return valor;
    }
    private Long longObrigatorio(Map<String, Object> dados, String campo) {
        Long valor = longOpcional(dados.get(campo)); if (valor == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " é obrigatório."); return valor;
    }
    private Long longOpcional(Object valor) { try { return valor == null || String.valueOf(valor).isBlank() ? null : Long.valueOf(String.valueOf(valor)); } catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido."); } }
    private LocalDate dataObrigatoria(Map<String, Object> dados, String campo) { LocalDate data = dataOpcional(dados.get(campo)); if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " é obrigatório."); return data; }
    private LocalDate dataOpcional(Object valor) { try { return valor == null || String.valueOf(valor).isBlank() ? null : LocalDate.parse(String.valueOf(valor)); } catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inválida."); } }
    private BigDecimal decimalObrigatorio(Map<String, Object> dados, String campo) { try { return new BigDecimal(textoObrigatorio(dados, campo).replace(",", ".")); } catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " inválido."); } }
    private int score(Map<String, Object> dados, String campo) { int valor = decimalObrigatorio(dados, campo).intValue(); if (valor < 0 || valor > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve estar entre 0 e 100."); return valor; }

    private FornecedorGovernancaDTO fornecedorDTO(Fornecedor item) {
        return new FornecedorGovernancaDTO(item.getId(), item.getRazaoSocial(), item.getCnpj(), item.getEmail(), item.getAtivo(), item.getRiscoScore(), item.getRiscoNivel(), item.getProximaAvaliacao(), item.getMonitoramentoStatus());
    }
    private TabelaTarifariaGovernancaDTO tabelaTarifariaDTO(TabelaTarifaria item) {
        return new TabelaTarifariaGovernancaDTO(item.getId(), item.getContrato().getId(), item.getContrato().getNumeroContrato(), item.getCargo()!=null?item.getCargo().getId():null, item.getCargo()!=null?item.getCargo().getNome():"Todos", item.getVigenciaInicio(), item.getVigenciaFim(), item.getValorHora(), item.getAdicionalNoturnoPercent(), item.getAtivo());
    }
    private ProcessoTerceiroGovernancaDTO processoDTO(ProcessoTerceiro item) {
        return new ProcessoTerceiroGovernancaDTO(item.getId(), item.getEmpregado().getId(), item.getEmpregado().getNome(), item.getContrato().getId(), item.getContrato().getNumeroContrato(), item.getTipo(), item.getStatus(), item.getPrazo(), item.getChecklistJson(), item.getObservacao());
    }
    private FolhaHorasGovernancaDTO folhaHorasDTO(FolhaHoras item) {
        return new FolhaHorasGovernancaDTO(item.getId(), item.getContrato().getId(), item.getContrato().getNumeroContrato(), item.getPeriodoInicio(), item.getPeriodoFim(), item.getStatus(), item.getMinutosFisicos(), item.getMinutosNoturnos(), item.getMinutosAdicionais(), item.getMinutosPagaveis(), item.getValorApurado(), item.getCalculoVersao(), item.getSubmetidoPor(), item.getAprovadoPor(), item.getMotivoRejeicao());
    }
    private FaturaGovernancaDTO faturaDTO(Fatura item) {
        return new FaturaGovernancaDTO(item.getId(), item.getFolhaHoras().getId(), item.getContrato().getNumeroContrato(), item.getStatus(), item.getValorBruto(), item.getGlosas(), item.getValorLiquido(), item.getNumeroDocumento());
    }
    private RiscoFornecedorGovernancaDTO riscoDTO(AvaliacaoRiscoFornecedor item) {
        return new RiscoFornecedorGovernancaDTO(item.getId(), item.getFornecedor().getId(), item.getFornecedor().getRazaoSocial(), item.getAvaliadoEm(), item.getProximaAvaliacao(), item.getScoreTotal(), item.getFornecedor().getRiscoNivel(), item.getEvidencias(), item.getAvaliadoPor());
    }

    private Map<String,Object> dados(FornecedorGovernancaEntradaDTO d) { return mapa("razaoSocial", d != null ? d.razaoSocial() : null, "cnpj", d != null ? d.cnpj() : null, "email", d != null ? d.email() : null, "ativo", d != null ? d.ativo() : null); }
    private Map<String,Object> dados(TabelaTarifariaGovernancaEntradaDTO d) { return mapa("contratoId", d != null ? d.contratoId() : null, "cargoId", d != null ? d.cargoId() : null, "vigenciaInicio", d != null ? d.vigenciaInicio() : null, "vigenciaFim", d != null ? d.vigenciaFim() : null, "valorHora", d != null ? d.valorHora() : null, "adicionalNoturnoPercent", d != null ? d.adicionalNoturnoPercent() : null); }
    private Map<String,Object> dados(ProcessoTerceiroEntradaDTO d) { return mapa("empregadoId", d != null ? d.empregadoId() : null, "contratoId", d != null ? d.contratoId() : null, "tipo", d != null ? d.tipo() : null, "prazo", d != null ? d.prazo() : null, "checklistJson", d != null ? d.checklistJson() : null, "observacao", d != null ? d.observacao() : null); }
    private Map<String,Object> dados(StatusGovernancaDTO d) { return mapa("status", d != null ? d.status() : null); }
    private Map<String,Object> dados(FolhaHorasGeracaoDTO d) { return mapa("contratoId", d != null ? d.contratoId() : null, "inicio", d != null ? d.inicio() : null, "fim", d != null ? d.fim() : null); }
    private Map<String,Object> dados(MotivoGovernancaDTO d) { return mapa("motivo", d != null ? d.motivo() : null); }
    private Map<String,Object> dados(FaturaGeracaoDTO d) { return mapa("glosas", d != null ? d.glosas() : null); }
    private Map<String,Object> dados(FaturaStatusDTO d) { return mapa("status", d != null ? d.status() : null, "numeroDocumento", d != null ? d.numeroDocumento() : null); }
    private Map<String,Object> dados(RiscoFornecedorEntradaDTO d) { return mapa("fornecedorId", d != null ? d.fornecedorId() : null, "scoreTrabalhista", d != null ? d.scoreTrabalhista() : null, "scoreFinanceiro", d != null ? d.scoreFinanceiro() : null, "scoreCompliance", d != null ? d.scoreCompliance() : null, "scoreSeguranca", d != null ? d.scoreSeguranca() : null, "proximaAvaliacao", d != null ? d.proximaAvaliacao() : null, "evidencias", d != null ? d.evidencias() : null); }
    private Map<String,Object> mapa(Object... pares) { Map<String,Object> m = new LinkedHashMap<>(); for (int i=0;i+1<pares.length;i+=2) m.put(String.valueOf(pares[i]), pares[i+1]); return m; }

}
