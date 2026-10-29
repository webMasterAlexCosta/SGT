package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Fluxo excepcional, explícito e auditável de transferência de Gestão Local. */
@Service
@Transactional(readOnly = true)
public class TransferenciaGestaoContratoServico {
    private final TransferenciaGestaoContratoRepositorio transferenciaRepositorio;
    private final GestaoLocalRepositorio gestaoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final MotorTemporalContratoServico motorTemporal;
    private final AlocacaoVinculoServico alocacaoServico;
    private final PermissaoExcepcionalServico permissaoExcepcionalServico;
    private DocumentoComprobatorioServico documentoComprobatorioServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;

    public TransferenciaGestaoContratoServico(TransferenciaGestaoContratoRepositorio transferenciaRepositorio,
                                              GestaoLocalRepositorio gestaoRepositorio,
                                              LotacaoRepositorio lotacaoRepositorio,
                                              JornadaRepositorio jornadaRepositorio,
                                              CargoRepositorio cargoRepositorio,
                                              ComposicaoContratualRepositorio composicaoRepositorio,
                                              AlocacaoVinculoRepositorio alocacaoRepositorio,
                                              ComposicaoContratualTemporalServico composicaoTemporal,
                                              MotorTemporalContratoServico motorTemporal,
                                              AlocacaoVinculoServico alocacaoServico,
                                              PermissaoExcepcionalServico permissaoExcepcionalServico) {
        this.transferenciaRepositorio = transferenciaRepositorio;
        this.gestaoRepositorio = gestaoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.cargoRepositorio = cargoRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.composicaoTemporal = composicaoTemporal;
        this.motorTemporal = motorTemporal;
        this.alocacaoServico = alocacaoServico;
        this.permissaoExcepcionalServico = permissaoExcepcionalServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDocumentos(DocumentoComprobatorioServico documentoComprobatorioServico) {
        this.documentoComprobatorioServico = documentoComprobatorioServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    @Transactional
    public TransferenciaGestaoContrato transferir(Contrato contrato, TransferenciaGestaoContratoRequisicao req,
                                                   UsuarioAplicacao usuario) {
        validarRequisicao(contrato, req);
        GestaoLocal origem = contrato.getGestaoLocal();
        GestaoLocal destino = gestaoRepositorio.findById(req.gestaoLocalDestinoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Local de destino não encontrada."));
        validarDestino(destino);
        if (Objects.equals(origem.getId(), destino.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Gestão Local de destino deve ser diferente da origem.");
        }
        PermissaoExcepcional permissao = permissaoExcepcionalServico.exigirERegistrarUso(usuario,
                TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL,
                contrato, origem, req.motivo(), "Contrato", contrato.getId());
        String autorizacaoConcedida = permissao.getAutorizacaoAdministrativa();
        if (!autorizacaoConcedida.equals(normalizar(req.autorizacaoAdministrativa()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A autorização informada não corresponde à concessão administrativa vigente.");
        }

        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> contexto = composicaoTemporal
                .projetarEntidades(contrato, req.dataEfetiva(), Instant.now());
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> operacionais = contexto.stream()
                .filter(p -> p.efetivoPrevisto() > 0).toList();
        if (operacionais.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A transferência é bloqueada enquanto não existir composição operacional válida para o período posterior.");
        }
        List<MapeamentoTransferenciaGestao> mapeamentos = prepararMapeamentos(contrato, origem, destino, req, contexto);
        validarCoberturaDoContexto(operacionais, mapeamentos);
        validarCoberturaDasAlocacoesPosteriores(contrato, req.dataEfetiva(), mapeamentos);

        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(req.dataEfetiva(), req.dataAto(), req.motivo(),
                req.referenciaSei(), req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental());
        EventoCicloVidaContrato evento = motorTemporal.registrarEvento(contrato,
                TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL, acao, usuario,
                resumoGestao(origem), resumoGestao(destino));

        TransferenciaGestaoContrato transferencia = new TransferenciaGestaoContrato();
        transferencia.setContrato(contrato); transferencia.setEvento(evento); transferencia.setGestaoOrigem(origem);
        transferencia.setGestaoDestino(destino); transferencia.setDataEfetiva(req.dataEfetiva()); transferencia.setMotivo(req.motivo());
        transferencia.setReferenciaSei(req.referenciaSei()); transferencia.setAutorizacaoAdministrativa(autorizacaoConcedida);
        transferencia.setResponsavelId(usuario == null ? null : usuario.getId());
        transferencia.setResponsavelUsername(usuario == null ? null : usuario.getUsername());
        transferencia.setMapeamentos(mapeamentos);
        transferencia = transferenciaRepositorio.save(transferencia);

        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(contrato, TipoFatoDocumental.TRANSFERENCIA_GESTAO,
                    TipoFatoDocumental.TRANSFERENCIA_GESTAO.name(), transferencia.getId(), req.referenciaSei(),
                    req.dataEfetiva(), usuario);
        }

        // As alocações são movimentadas na mesma transação, sem novo vínculo ou desligamento.
        alocacaoServico.aplicarTransferenciaGestao(transferencia, usuario);
        motorTemporal.registrarFotografiaProjetada(contrato, evento, req.dataEfetiva(),
                fotografiaComGestaoDestino(contrato, destino, req.dataEfetiva()));
        if (!req.dataEfetiva().isAfter(RelogioSistema.hoje())) contrato.setGestaoLocal(destino);
        return transferencia;
    }

    private VersaoContratoNegocialDTO fotografiaComGestaoDestino(Contrato contrato,
                                                                  GestaoLocal destino,
                                                                  LocalDate dataEfetiva) {
        Optional<VersaoContratoNegocial> base = motorTemporal.versaoAplicavel(
                contrato, dataEfetiva, Instant.now());
        return base.map(v -> new VersaoContratoNegocialDTO(
                        null, null, null, dataEfetiva, null,
                        v.getNumeroContrato(), v.getAnoContrato(), v.getDescricao(),
                        v.getTipoContratacaoId(), v.getTipoContratacaoDescricao(), v.getFornecedorId(),
                        v.getCnpjEmpresa(), v.getRazaoSocialEmpresa(), v.getNomeFantasiaEmpresa(),
                        v.getDataInicio(), v.getDataFim(), v.getEfetivoPrevisto(), destino.getId(), destino.getNome()))
                .orElseGet(() -> new VersaoContratoNegocialDTO(
                        null, null, null, dataEfetiva, null,
                        contrato.getNumeroContrato(), contrato.getAnoContrato(), contrato.getDescricao(),
                        contrato.getTipoContratacao() == null ? null : contrato.getTipoContratacao().getId(),
                        contrato.getTipo(), contrato.getFornecedor() == null ? null : contrato.getFornecedor().getId(),
                        contrato.getCnpjEmpresa(), contrato.getRazaoSocialEmpresa(), contrato.getNomeFantasiaEmpresa(),
                        contrato.getDataInicio(), contrato.getDataFim(), contrato.getEfetivoPrevisto(),
                        destino.getId(), destino.getNome()));
    }

    public List<TransferenciaGestaoContratoDTO> listar(Long contratoId) {
        return transferenciaRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId).stream()
                .map(this::paraDTO).toList();
    }

    public TransferenciaGestaoContratoDTO paraDTO(TransferenciaGestaoContrato t) {
        return new TransferenciaGestaoContratoDTO(t.getId(), t.getContrato().getId(),
                t.getEvento() == null ? null : t.getEvento().getId(), t.getGestaoOrigem().getId(), t.getGestaoOrigem().getNome(),
                t.getGestaoDestino().getId(), t.getGestaoDestino().getNome(), t.getDataEfetiva(), t.getMotivo(),
                t.getReferenciaSei(), t.getAutorizacaoAdministrativa(), t.getRegistradoEm(), t.getResponsavelId(),
                t.getResponsavelUsername(), t.getMapeamentos().stream().map(this::paraDTO).toList());
    }

    private List<MapeamentoTransferenciaGestao> prepararMapeamentos(Contrato contrato, GestaoLocal origem, GestaoLocal destino,
                                                                     TransferenciaGestaoContratoRequisicao req,
                                                                     List<ComposicaoContratualTemporalServico.ComposicaoProjetada> contexto) {
        if (req.mapeamentos() == null || req.mapeamentos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A transferência exige mapeamento explícito para os catálogos da Gestão de destino.");
        }
        Map<String, ComposicaoContratualTemporalServico.ComposicaoProjetada> porOrigem = contexto.stream()
                .collect(Collectors.toMap(ComposicaoContratualTemporalServico.ComposicaoProjetada::chave,
                        p -> p, (a, b) -> a, LinkedHashMap::new));
        Set<String> usados = new HashSet<>();
        List<MapeamentoTransferenciaGestao> resultado = new ArrayList<>();
        for (MapeamentoTransferenciaGestaoRequisicao item : req.mapeamentos()) {
            if (item == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mapeamento de transferência inválido.");
            ComposicaoContratual origemComposicao = resolverComposicaoOrigem(contrato, item.composicaoOrigemId());
            Lotacao lotacaoOrigem = origemComposicao != null ? origemComposicao.getLotacao() : obterLotacao(item.lotacaoOrigemId(), "origem");
            Jornada jornadaOrigem = origemComposicao != null ? origemComposicao.getJornada() : obterJornada(item.jornadaOrigemId(), "origem");
            Cargo cargoOrigem = origemComposicao != null ? origemComposicao.getCargo() : obterCargo(item.cargoOrigemId(), "origem");
            String chave = chave(lotacaoOrigem, jornadaOrigem, cargoOrigem);
            if (!porOrigem.containsKey(chave)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O recorte de origem não pertence à composição temporal válida do contrato.");
            }
            if (!usados.add(chave)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A mesma composição de origem foi mapeada mais de uma vez.");
            validarGestaoDoRecorte(lotacaoOrigem, jornadaOrigem, cargoOrigem, origem, "origem");

            Lotacao lotacaoDestino = obterLotacao(item.lotacaoDestinoId(), "destino");
            Jornada jornadaDestino = obterJornada(item.jornadaDestinoId(), "destino");
            Cargo cargoDestino = obterCargo(item.cargoDestinoId(), "destino");
            validarGestaoDoRecorte(lotacaoDestino, jornadaDestino, cargoDestino, destino, "destino");
            boolean turnoDaLotacao = lotacaoDestino.getTurnos().stream().anyMatch(j -> Objects.equals(j.getId(), jornadaDestino.getId()));
            if (!turnoDaLotacao) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O Turno de destino não pertence à Lotação de destino.");
            if (!catalogoAtivoEm(TipoCatalogoTemporal.LOTACAO, lotacaoDestino.getId(), req.dataEfetiva(), lotacaoDestino.getAtivo())
                    || !catalogoAtivoEm(TipoCatalogoTemporal.JORNADA, jornadaDestino.getId(), req.dataEfetiva(), jornadaDestino.getAtivo())
                    || !catalogoAtivoEm(TipoCatalogoTemporal.CARGO, cargoDestino.getId(), req.dataEfetiva(), cargoDestino.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Catálogo inativo não pode ser mapeado para transferência atual ou futura.");
            }
            MapeamentoTransferenciaGestao mapa = new MapeamentoTransferenciaGestao();
            mapa.setComposicaoOrigem(origemComposicao); mapa.setLotacaoOrigem(lotacaoOrigem); mapa.setJornadaOrigem(jornadaOrigem);
            mapa.setCargoOrigem(cargoOrigem); mapa.setLotacaoDestino(lotacaoDestino); mapa.setJornadaDestino(jornadaDestino);
            mapa.setCargoDestino(cargoDestino); resultado.add(mapa);
        }
        return resultado;
    }

    private boolean catalogoAtivoEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean atual) {
        return disponibilidadeCatalogo != null
                ? disponibilidadeCatalogo.ativoEm(tipo, id, data, atual)
                : Boolean.TRUE.equals(atual);
    }

    private void validarCoberturaDoContexto(List<ComposicaoContratualTemporalServico.ComposicaoProjetada> operacionais,
                                             List<MapeamentoTransferenciaGestao> mapeamentos) {
        Set<String> fontes = mapeamentos.stream().map(m -> chave(m.getLotacaoOrigem(), m.getJornadaOrigem(), m.getCargoOrigem()))
                .collect(Collectors.toSet());
        List<String> faltantes = operacionais.stream().map(ComposicaoContratualTemporalServico.ComposicaoProjetada::chave)
                .filter(chave -> !fontes.contains(chave)).toList();
        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A transferência não possui mapeamento para todas as composições operacionais: " + faltantes);
        }
    }

    private void validarCoberturaDasAlocacoesPosteriores(Contrato contrato, LocalDate data,
                                                          List<MapeamentoTransferenciaGestao> mapeamentos) {
        Set<String> fontes = mapeamentos.stream().map(m -> chave(m.getLotacaoOrigem(), m.getJornadaOrigem(), m.getCargoOrigem()))
                .collect(Collectors.toSet());
        List<String> faltantes = alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(contrato.getId()).stream()
                .filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA)
                .filter(a -> a.getDataFim() == null || !a.getDataFim().isBefore(data))
                .map(a -> chave(a.getLotacao(), a.getJornada(), a.getCargo())).filter(chave -> !fontes.contains(chave))
                .distinct().toList();
        if (!faltantes.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Existem Alocações posteriores sem mapeamento de destino: " + faltantes);
    }

    private void validarRequisicao(Contrato contrato, TransferenciaGestaoContratoRequisicao req) {
        if (contrato == null || contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transferência exige contrato publicado válido.");
        }
        if (req == null || req.dataEfetiva() == null || req.gestaoLocalDestinoId() == null || vazio(req.motivo())
                || vazio(req.referenciaSei()) || vazio(req.autorizacaoAdministrativa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data efetiva, Gestão destino, motivo, referência SEI e autorização administrativa são obrigatórios.");
        }
        if (contrato.getDataInicio() != null && req.dataEfetiva().isBefore(contrato.getDataInicio())
                || contrato.getDataFim() != null && req.dataEfetiva().isAfter(contrato.getDataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data da transferência deve estar contida na vigência do contrato.");
        }
        if ((req.eventoPrecedenteId() == null) != (req.relacaoPrecedencia() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eventoPrecedenteId e relacaoPrecedencia devem ser informados juntos.");
        }
    }

    private void validarDestino(GestaoLocal destino) {
        if (destino == null || Boolean.FALSE.equals(destino.getAtivo()) || destino.getSuperintendencia() == null
                || Boolean.FALSE.equals(destino.getSuperintendencia().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A Gestão Local de destino e sua Superintendência precisam estar ativas.");
        }
    }

    private ComposicaoContratual resolverComposicaoOrigem(Contrato contrato, Long id) {
        if (id == null) return null;
        ComposicaoContratual composicao = composicaoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição de origem não encontrada."));
        if (!Objects.equals(composicao.getContrato().getId(), contrato.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição de origem pertence a outro contrato.");
        }
        return composicao;
    }

    private Lotacao obterLotacao(Long id, String lado) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lotação de " + lado + " é obrigatória.");
        return lotacaoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Lotação de " + lado + " não encontrada."));
    }
    private Jornada obterJornada(Long id, String lado) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno de " + lado + " é obrigatório.");
        return jornadaRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Turno de " + lado + " não encontrado."));
    }
    private Cargo obterCargo(Long id, String lado) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo de " + lado + " é obrigatório.");
        return cargoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cargo de " + lado + " não encontrado."));
    }

    private void validarGestaoDoRecorte(Lotacao lotacao, Jornada jornada, Cargo cargo, GestaoLocal gestao, String lado) {
        if (!Objects.equals(lotacao.getGestaoLocal().getId(), gestao.getId())
                || !Objects.equals(jornada.getGestaoLocal().getId(), gestao.getId())
                || !Objects.equals(cargo.getGestaoLocal().getId(), gestao.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo de " + lado + " devem pertencer à Gestão Local correspondente.");
        }
    }

    private MapeamentoTransferenciaGestaoDTO paraDTO(MapeamentoTransferenciaGestao m) {
        return new MapeamentoTransferenciaGestaoDTO(m.getId(), m.getComposicaoOrigem() == null ? null : m.getComposicaoOrigem().getId(),
                m.getLotacaoOrigem().getId(), m.getLotacaoOrigem().getNome(), m.getJornadaOrigem().getId(),
                m.getJornadaOrigem().getDescricao(), m.getCargoOrigem().getId(), m.getCargoOrigem().getNome(),
                m.getLotacaoDestino().getId(), m.getLotacaoDestino().getNome(), m.getJornadaDestino().getId(),
                m.getJornadaDestino().getDescricao(), m.getCargoDestino().getId(), m.getCargoDestino().getNome());
    }

    private String chave(Lotacao l, Jornada j, Cargo c) { return l.getId() + ":" + j.getId() + ":" + c.getId(); }
    private String resumoGestao(GestaoLocal g) { return g.getId() + ":" + g.getNome(); }
    private boolean vazio(String texto) { return texto == null || texto.isBlank(); }
    private String normalizar(String texto) { return texto.trim().replaceAll("\\s+", " "); }
}
