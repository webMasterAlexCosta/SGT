package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesEmpregado;
import com.example.siggtrat.dto.JornadaDTO;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.JornadaDiaSemanaDTO;
import com.example.siggtrat.dto.RegraAdicionalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.UtilitarioHorario;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class JornadaServico {

    private final JornadaRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final EscopoAcessoUsuarioServico scopeServico;
    private final ProgramacaoFuturaCatalogoServico programacaoFutura;
    private final RegistroAuditoriaServico auditoria;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeTemporal;
    private final long maxJornadaMinutes;

    public JornadaServico(JornadaRepositorio repositorio,
                             EmpregadoRepositorio empregadoRepositorio,
                             LotacaoRepositorio lotacaoRepositorio,
                             GestaoLocalRepositorio gestaoLocalRepositorio,
                             UsuarioAplicacaoRepositorio appUserRepositorio,
                             AcessoOrganizacionalServico acessoOrganizacionalServico,
                             EscopoAcessoUsuarioServico scopeServico,
                             PropriedadesEmpregado empregadoProperties,
                             ProgramacaoFuturaCatalogoServico programacaoFutura,
                             RegistroAuditoriaServico auditoria) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.appUserRepositorio = appUserRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.scopeServico = scopeServico;
        this.maxJornadaMinutes = empregadoProperties.maxJornadaMinutes();
        this.programacaoFutura = programacaoFutura;
        this.auditoria = auditoria;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeTemporal(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeTemporal = servico;
    }

    private void validar(JornadaDTO dto, GestaoLocal gestaoLocal, Long idAtual) {
        if (dto == null) throw new IllegalArgumentException("Turno inválido.");
        if (dto.descricao() == null || dto.descricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do turno é obrigatória.");
        }
        validarGestaoLocalAtiva(gestaoLocal);
        String descricao = dto.descricao().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        repositorio.findByDescricaoIgnoreCaseAndGestaoLocalId(descricao, gestaoLocal.getId()).ifPresent(existente -> {
            if (!Objects.equals(existente.getId(), idAtual)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um turno com esta descrição na Gestão Local.");
            }
        });

        validarSeteDiasEIntervalos(dto.dias());

        if (dto.regrasAdicionais() != null) {
            for (RegraAdicionalDTO regra : dto.regrasAdicionais()) {
                if (regra.tipo() == null || regra.tipo().isBlank()) {
                    throw new IllegalArgumentException("Tipo da regra adicional é obrigatório.");
                }
                UtilitarioHorario.validarPar(regra.inicio(), regra.fim(), "início da regra", "fim da regra");
            }
        }
    }

    private void validarSeteDiasEIntervalos(List<JornadaDiaSemanaDTO> dias) {
        if (dias == null || dias.size() != java.time.DayOfWeek.values().length) {
            throw new IllegalArgumentException("O turno deve conter exatamente os sete dias da semana.");
        }

        EnumSet<java.time.DayOfWeek> encontrados = EnumSet.noneOf(java.time.DayOfWeek.class);
        for (JornadaDiaSemanaDTO dia : dias) {
            if (dia == null || dia.diaSemana() == null) {
                throw new IllegalArgumentException("Todos os dias do turno devem informar o dia da semana.");
            }
            if (!encontrados.add(dia.diaSemana())) {
                throw new IllegalArgumentException("O dia " + dia.diaSemana() + " foi informado mais de uma vez.");
            }
            if (dia.diaTrabalho() == null) {
                throw new IllegalArgumentException("Informe se " + dia.diaSemana() + " é dia de trabalho ou descanso.");
            }

            boolean possuiHorario = possuiValor(dia.entrada1()) || possuiValor(dia.saida1())
                    || possuiValor(dia.entrada2()) || possuiValor(dia.saida2());
            if (!dia.diaTrabalho()) {
                if (possuiHorario) {
                    throw new IllegalArgumentException("Dia de descanso não pode possuir horários: " + dia.diaSemana() + ".");
                }
                continue;
            }

            if (!possuiValor(dia.entrada1()) && !possuiValor(dia.saida1())) {
                throw new IllegalArgumentException("Dia de trabalho deve possuir ao menos o primeiro intervalo: " + dia.diaSemana() + ".");
            }

            Intervalo primeiro = validarIntervalo(dia.diaSemana(), 1, dia.entrada1(), dia.saida1(), true);
            Intervalo segundo = validarIntervalo(dia.diaSemana(), 2, dia.entrada2(), dia.saida2(), false);

            if (segundo != null) {
                segundo = posicionarDepoisDoPrimeiro(segundo, primeiro);
                if (segundo.fimAbsoluto() - primeiro.inicioAbsoluto() > 24 * 60) {
                    throw new IllegalArgumentException("A jornada de " + dia.diaSemana()
                            + " deve terminar em até 24 horas após a primeira entrada.");
                }
            }

            long total = primeiro.minutos() + (segundo == null ? 0 : segundo.minutos());
            if (total > maxJornadaMinutes) {
                throw new IllegalArgumentException("A jornada excede o limite de " + maxJornadaMinutes
                        + " minutos em " + dia.diaSemana() + ".");
            }
        }

        if (encontrados.size() != java.time.DayOfWeek.values().length) {
            throw new IllegalArgumentException("O turno deve conter os sete dias da semana, sem repetição.");
        }
    }

    private Intervalo validarIntervalo(java.time.DayOfWeek dia, int numero, String entrada, String saida, boolean obrigatorio) {
        boolean temEntrada = possuiValor(entrada);
        boolean temSaida = possuiValor(saida);
        if (!temEntrada && !temSaida && !obrigatorio) return null;
        if (temEntrada != temSaida) {
            throw new IllegalArgumentException("Entrada e saída do intervalo " + numero + " devem ser informadas juntas em " + dia + ".");
        }

        String entradaNormalizada = UtilitarioHorario.normalizarHorario(entrada, "entrada " + numero + " de " + dia);
        String saidaNormalizada = UtilitarioHorario.normalizarHorario(saida, "saída " + numero + " de " + dia);
        int inicio = UtilitarioHorario.horarioParaMinutos(entradaNormalizada, "entrada " + numero + " de " + dia);
        int fim = UtilitarioHorario.horarioParaMinutos(saidaNormalizada, "saída " + numero + " de " + dia);
        if (fim == inicio) {
            throw new IllegalArgumentException("O intervalo " + numero + " deve possuir duração maior que zero em " + dia + ".");
        }
        int fimAbsoluto = fim > inicio ? fim : fim + (24 * 60);
        return new Intervalo(inicio, fimAbsoluto, fimAbsoluto - inicio);
    }

    private boolean possuiValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private Intervalo posicionarDepoisDoPrimeiro(Intervalo segundo, Intervalo primeiro) {
        int inicio = segundo.inicioAbsoluto();
        int fim = segundo.fimAbsoluto();
        while (inicio < primeiro.fimAbsoluto()) {
            inicio += 24 * 60;
            fim += 24 * 60;
        }
        return new Intervalo(inicio, fim, segundo.minutos());
    }

    private record Intervalo(int inicioAbsoluto, int fimAbsoluto, long minutos) {}
    @Transactional(readOnly = true)
    public List<JornadaDTO> listar(Long lotacaoId) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        List<Jornada> jornadas;

        if (lotacaoId != null) {
            Lotacao lotacao = lotacaoRepositorio.findById(lotacaoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada."));
            if (!acessoOrganizacionalServico.podeAcessarLotacao(usuario, lotacao)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada no seu escopo.");
            }
            jornadas = repositorio.findByLotacaoOrLotacoes(lotacaoId);
            if (acessoOrganizacionalServico.ehOperacional(usuario)) {
                Long gestaoLocalId = lotacao.getGestaoLocal() != null ? lotacao.getGestaoLocal().getId() : null;
                jornadas = jornadas.stream()
                        .filter(jornada -> scopeServico.podeAcessarJornadaLotacao(
                                usuario, gestaoLocalId, lotacaoId, jornada.getId()))
                        .toList();
            }
        } else if (acessoOrganizacionalServico.ehGlobal(usuario)) {
            jornadas = repositorio.findAll().stream()
                    .sorted(Comparator.comparing(Jornada::getDescricao, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } else if (acessoOrganizacionalServico.ehGestorEstadual(usuario)
                && acessoOrganizacionalServico.superintendenciaEfetiva(usuario) != null) {
            jornadas = repositorio.findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(
                    acessoOrganizacionalServico.superintendenciaEfetiva(usuario).getId());
        } else if (acessoOrganizacionalServico.ehGestaoLocal(usuario) && usuario.getGestaoLocal() != null) {
            jornadas = repositorio.findByGestaoLocalIdOrderByDescricaoAsc(usuario.getGestaoLocal().getId());
        } else if (acessoOrganizacionalServico.ehOperacional(usuario)) {
            Set<Long> ids = scopeServico.obterIdsJornadas(usuario);
            jornadas = ids.isEmpty() ? List.of() : repositorio.findAllById(ids).stream()
                    .filter(jornada -> acessoOrganizacionalServico.podeAcessarJornada(usuario, jornada))
                    .sorted(Comparator.comparing(Jornada::getDescricao, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } else {
            jornadas = List.of();
        }
        return jornadas.stream().filter(item -> podeVisualizar(usuario, item)).map(this::paraDTO).toList();
    }
    @Transactional(readOnly = true)
    public JornadaDTO obter(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        return repositorio.findById(id)
                .filter(item -> podeVisualizar(usuario, item))
                .map(this::paraDTO)
                .orElse(null);
    }
    @Transactional
    public JornadaDTO criar(JornadaDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestao(usuario);
        GestaoLocal gestaoLocal = resolverGestaoLocal(usuario, dto != null ? dto.gestaoLocalId() : null);
        validar(dto, gestaoLocal, null);
        Jornada jornada = new Jornada();
        jornada.setGestaoLocal(gestaoLocal);
        mapearParaEntidade(dto, jornada);
        jornada.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        Jornada salva = repositorio.save(jornada);
        registrarDisponibilidade(salva, null, salva.getAtivo(), "Criação do Turno", usuario);
        // Incluir dias e regras em cascata na sincronização anterior à guarda.
        repositorio.flush();
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        auditar("JORNADA_CRIADA", salva, null, salva.getAtivo());
        return paraDTO(salva);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public JornadaDTO atualizar(Long id, JornadaDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestao(usuario);
        Jornada existente = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado."));
        if (disponibilidadeTemporal != null)
            disponibilidadeTemporal.bloquearParaAlteracao(TipoCatalogoTemporal.JORNADA, id);
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        if (!podeAlterar(usuario, existente)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado no seu escopo.");
        }
        Long gestaoLocalId = dto != null && dto.gestaoLocalId() != null
                ? dto.gestaoLocalId()
                : existente.getGestaoLocal() != null ? existente.getGestaoLocal().getId() : null;
        GestaoLocal gestaoLocal = resolverGestaoLocal(usuario, gestaoLocalId);
        boolean mudandoGestaoLocal = existente.getGestaoLocal() != null
                && gestaoLocal != null
                && !Objects.equals(existente.getGestaoLocal().getId(), gestaoLocal.getId());
        if (mudandoGestaoLocal && (empregadoRepositorio.existsByJornada_Id(id)
                || !lotacaoRepositorio.findByTurnos_Id(id).isEmpty()
                || appUserRepositorio.existsByJornadaId(id)
                || appUserRepositorio.existsByVinculosGestao_Jornada_Id(id))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível transferir o turno para outra Gestão Local enquanto existirem "
                            + "empregados, lotações ou usuários vinculados."
            );
        }

        validar(dto, gestaoLocal, id);
        Boolean ativoAnterior = existente.getAtivo();
        if (Boolean.TRUE.equals(ativoAnterior) && Boolean.FALSE.equals(dto.ativo())) {
            programacaoFutura.exigirSemProgramacaoFutura(
                    ProgramacaoFuturaCatalogoServico.TipoCatalogo.JORNADA, existente.getId());
        }
        existente.setGestaoLocal(gestaoLocal);
        mapearParaEntidade(dto, existente);
        if (dto.ativo() != null) existente.setAtivo(dto.ativo());
        Jornada salva = repositorio.save(existente);
        if (!Objects.equals(ativoAnterior, salva.getAtivo()))
            registrarDisponibilidade(salva, ativoAnterior, salva.getAtivo(), "Alteração administrativa do Turno", usuario);
        auditar(acaoAtualizacao(ativoAnterior, salva.getAtivo()), salva, ativoAnterior, salva.getAtivo());
        // O cliente usará esta versão na prévia do próximo ato documentado.
        // Sincronizar o @Version antes de montar o DTO, ainda nesta transação.
        repositorio.flush();
        return paraDTO(salva);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void excluir(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestao(usuario);
        Jornada jornada = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado."));
        if (disponibilidadeTemporal != null)
            disponibilidadeTemporal.bloquearParaAlteracao(TipoCatalogoTemporal.JORNADA, id);
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        if (!podeAlterar(usuario, jornada)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado no seu escopo.");
        }
        // O cadastro não é apagado: torna-se indisponível para novos fatos e
        // permanece consultável no histórico.
        Boolean ativoAnterior = jornada.getAtivo();
        if (Boolean.TRUE.equals(ativoAnterior)) {
            programacaoFutura.exigirSemProgramacaoFutura(
                    ProgramacaoFuturaCatalogoServico.TipoCatalogo.JORNADA, jornada.getId());
        }
        jornada.setAtivo(false);
        repositorio.save(jornada);
        if (Boolean.TRUE.equals(ativoAnterior))
            registrarDisponibilidade(jornada, ativoAnterior, false, "Inativação administrativa do Turno", usuario);
        auditar("JORNADA_INATIVADA", jornada, ativoAnterior, false);
    }

    private void exigirGestao(UsuarioAplicacao usuario) {
        if (!acessoOrganizacionalServico.ehGestaoLocal(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente a Gestão Local pode gerir os turnos da sua operação.");
        }
    }

    private GestaoLocal resolverGestaoLocal(UsuarioAplicacao usuario, Long gestaoLocalId) {
        if (acessoOrganizacionalServico.ehGestaoLocal(usuario)) {
            if (usuario.getGestaoLocal() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário Gestão Local sem vínculo organizacional.");
            }
            if (gestaoLocalId != null && !Objects.equals(gestaoLocalId, usuario.getGestaoLocal().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O turno deve pertencer à sua Gestão Local.");
            }
            return usuario.getGestaoLocal();
        }
        if (gestaoLocalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione a Gestão Local responsável pelo turno.");
        }
        return gestaoLocalRepositorio.findById(gestaoLocalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Local inválida."));
    }

    private void validarGestaoLocalAtiva(GestaoLocal gestaoLocal) {
        if (gestaoLocal == null || Boolean.FALSE.equals(gestaoLocal.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Gestão Local selecionada está inativa ou é inválida.");
        }
        if (gestaoLocal.getSuperintendencia() == null || Boolean.FALSE.equals(gestaoLocal.getSuperintendencia().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O gestor estadual da Gestão Local está inativo ou não foi definido.");
        }
    }

    private boolean podeVisualizar(UsuarioAplicacao usuario, Jornada jornada) {
        if (acessoOrganizacionalServico.podeAcessarJornada(usuario, jornada)) return true;
        if (acessoOrganizacionalServico.ehOperacional(usuario)) return false;
        return jornada.getLotacoes() != null
                && jornada.getLotacoes().stream().anyMatch(lotacao -> acessoOrganizacionalServico.podeAcessarLotacao(usuario, lotacao));
    }

    private boolean podeAlterar(UsuarioAplicacao usuario, Jornada jornada) {
        return acessoOrganizacionalServico.ehGestaoLocal(usuario)
                && jornada.getGestaoLocal() != null
                && acessoOrganizacionalServico.podeAcessarGestaoLocal(usuario, jornada.getGestaoLocal());
    }

    private JornadaDTO paraDTO(Jornada entity) {
        List<JornadaDiaSemanaDTO> dias = entity.getDias() == null ? List.of() : entity.getDias().stream()
                .map(item -> new JornadaDiaSemanaDTO(item.getId(), item.getDiaSemana(), item.getDiaTrabalho(),
                        item.getEntrada1(), item.getSaida1(), item.getEntrada2(), item.getSaida2()))
                .collect(Collectors.toList());
        List<RegraAdicionalDTO> regras = entity.getRegrasAdicionais() == null ? List.of()
                : entity.getRegrasAdicionais().stream()
                .map(item -> new RegraAdicionalDTO(item.getId(), item.getTipo(), item.getInicio(), item.getFim(),
                        item.getPercentual(), item.getAtivo(), item.getOrdem()))
                .collect(Collectors.toList());
        GestaoLocal gestaoLocal = entity.getGestaoLocal();
        SuperintendenciaEstadual se = gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null;
        return new JornadaDTO(entity.getId(), entity.getDescricao(), dias, regras,
                gestaoLocal != null ? gestaoLocal.getId() : null, gestaoLocal != null ? gestaoLocal.getNome() : null,
                se != null ? se.getId() : null, se != null ? se.getNome() : null,
                se != null ? se.getUf() : null, entity.getAtivo(), entity.getVersion());
    }

    private void mapearParaEntidade(JornadaDTO dto, Jornada entity) {
        entity.setDescricao(dto.descricao().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT));
        Map<java.time.DayOfWeek, JornadaDiaSemana> existentes = new EnumMap<>(java.time.DayOfWeek.class);
        for (JornadaDiaSemana dia : entity.getDias()) existentes.put(dia.getDiaSemana(), dia);
        Set<java.time.DayOfWeek> recebidos = new HashSet<>();
        if (dto.dias() != null) {
            for (JornadaDiaSemanaDTO item : dto.dias()) {
                recebidos.add(item.diaSemana());
                JornadaDiaSemana dia = existentes.get(item.diaSemana());
                if (dia == null) {
                    dia = new JornadaDiaSemana();
                    dia.setJornada(entity);
                    entity.getDias().add(dia);
                }
                dia.setDiaSemana(item.diaSemana());
                dia.setDiaTrabalho(item.diaTrabalho());
                boolean trabalho = Boolean.TRUE.equals(item.diaTrabalho());
                dia.setEntrada1(trabalho ? UtilitarioHorario.normalizarHorario(item.entrada1(), "entrada 1") : null);
                dia.setSaida1(trabalho ? UtilitarioHorario.normalizarHorario(item.saida1(), "saída 1") : null);
                dia.setEntrada2(trabalho ? UtilitarioHorario.normalizarHorario(item.entrada2(), "entrada 2") : null);
                dia.setSaida2(trabalho ? UtilitarioHorario.normalizarHorario(item.saida2(), "saída 2") : null);
                dia.setJornada(entity);
            }
        }
        entity.getDias().removeIf(dia -> !recebidos.contains(dia.getDiaSemana()));
        entity.getRegrasAdicionais().clear();
        if (dto.regrasAdicionais() != null) {
            for (RegraAdicionalDTO item : dto.regrasAdicionais()) {
                RegraAdicional regra = new RegraAdicional();
                regra.setTipo(item.tipo());
                regra.setInicio(item.inicio());
                regra.setFim(item.fim());
                regra.setPercentual(item.percentual());
                regra.setAtivo(item.ativo() == null || item.ativo());
                regra.setOrdem(item.ordem());
                regra.setJornada(entity);
                entity.getRegrasAdicionais().add(regra);
            }
        }
    }

    private String acaoAtualizacao(Boolean antes, Boolean depois) {
        if (Boolean.TRUE.equals(antes) && Boolean.FALSE.equals(depois)) return "JORNADA_INATIVADA";
        if (Boolean.FALSE.equals(antes) && Boolean.TRUE.equals(depois)) return "JORNADA_REATIVADA";
        return "JORNADA_ATUALIZADA";
    }

    private void auditar(String acao, Jornada jornada, Boolean ativoAnterior, Boolean ativoNovo) {
        auditoria.registrarAcao(acao, "Jornada", jornada.getId(), jornada.getDescricao(),
                RegistroAuditoriaServico.detalhes("gestaoLocalId",
                        jornada.getGestaoLocal() != null ? jornada.getGestaoLocal().getId() : null,
                        "ativoAnterior", ativoAnterior, "ativoNovo", ativoNovo,
                        "version", jornada.getVersion()));
    }

    private void registrarDisponibilidade(Jornada jornada, Boolean ativoAnterior, Boolean ativo, String motivo, UsuarioAplicacao usuario) {
        if (disponibilidadeTemporal != null && jornada.getId() != null)
            disponibilidadeTemporal.registrarTransicao(TipoCatalogoTemporal.JORNADA, jornada.getId(),
                    ativoAnterior, Boolean.TRUE.equals(ativo), motivo, usuario);
    }
}
