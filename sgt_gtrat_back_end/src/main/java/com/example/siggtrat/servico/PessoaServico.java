package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.pessoa.TipoCorrecaoPessoa;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.time.LocalDate;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;

@Service
@Transactional(readOnly = true)
public class PessoaServico {
    private final PessoaRepositorio pessoaRepositorio;
    private final CorrecaoPessoaRepositorio correcaoRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final PermissaoExcepcionalServico permissaoExcepcionalServico;

    public PessoaServico(PessoaRepositorio pessoaRepositorio,
                         CorrecaoPessoaRepositorio correcaoRepositorio,
                         VinculoContratualRepositorio vinculoRepositorio,
                         AcessoOrganizacionalServico acesso,
                         MinimizadorDadosPessoaisServico minimizador,
                         PermissaoExcepcionalServico permissaoExcepcionalServico) {
        this.pessoaRepositorio = pessoaRepositorio;
        this.correcaoRepositorio = correcaoRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.acesso = acesso;
        this.minimizador = minimizador;
        this.permissaoExcepcionalServico = permissaoExcepcionalServico;
    }

    public Page<PessoaResumoDTO> listar(String busca, Long contratoId, LocalDate ativoEm, Pageable pageable) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        if (!acesso.podeConsultarEmpregados(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso a pessoas.");
        EscopoConsulta escopo = escopoConsulta(user);
        String termo = busca == null || busca.isBlank() ? null : busca.trim().toLowerCase(Locale.ROOT);
        String cpf = termo == null ? null : termo.replaceAll("\\D", "");
        if (cpf != null && cpf.isBlank()) cpf = null;
        Pageable limitado = UtilitarioPaginacao.limitarComOrdenacaoFixa(pageable,
                Sort.by(Sort.Direction.ASC, "nome").and(Sort.by("id")));
        return pessoaRepositorio.pesquisarNoEscopo(escopo.global(), escopo.gestaoLocalId(),
                        escopo.superintendenciaId(), contratoId, ativoEm, termo, cpf, limitado)
                .map(p -> paraResumo(p, user));
    }

    public PessoaDTO buscar(Long id) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Pessoa pessoa = obter(id);
        if (!podeConsultar(user, pessoa)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada no seu escopo.");
        return paraDTO(pessoa, user, false);
    }

    /** Chamado pelo cadastro integrado Pessoa + Vínculo + Alocação. */
    @Transactional
    public Pessoa criarParaVinculo(CadastroPessoaRequisicao req, UsuarioAplicacao user) {
        exigirGestao(user);
        validarCadastro(req);
        String cpf = normalizarCpf(req.cpf());
        pessoaRepositorio.findByCpf(cpf).ifPresent(p -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CPF já pertence a outra pessoa. Duplicidade exige tratamento excepcional; nenhuma fusão foi realizada.");
        });
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(normalizarNome(req.nome()));
        pessoa.setCpf(cpf);
        aplicarOpcionaisCadastro(req, pessoa);
        return pessoaRepositorio.save(pessoa);
    }

    @Transactional
    public PessoaDTO atualizarOpcionais(Long id, AtualizacaoPessoaOpcionalRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        exigirGestao(user);
        Pessoa pessoa = obter(id);
        exigirGestaoDoVinculo(user, pessoa);
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados opcionais não informados.");

        pessoa.setRg(resolverOpcional(pessoa.getRg(), req.rg(), req.limparRg()));
        pessoa.setCtpsSerie(resolverOpcional(pessoa.getCtpsSerie(), req.ctpsSerie(), req.limparCtpsSerie()));
        pessoa.setNitPisPasep(resolverOpcional(pessoa.getNitPisPasep(), req.nitPisPasep(), req.limparNitPisPasep()));
        if (Boolean.TRUE.equals(req.limparDataNascimento())) pessoa.setDataNascimento(null);
        else if (req.dataNascimento() != null) pessoa.setDataNascimento(req.dataNascimento());
        pessoa.setEmail(resolverOpcional(pessoa.getEmail(), req.email(), req.limparEmail()));
        if (Boolean.TRUE.equals(req.limparEndereco())) pessoa.setEndereco(null);
        else if (req.endereco() != null) pessoa.setEndereco(mapearEndereco(req.endereco(), pessoa.getEndereco()));
        return paraDTO(pessoaRepositorio.save(pessoa), user, false);
    }

    @Transactional
    public PessoaDTO corrigirIdentidade(Long id, CorrecaoIdentidadePessoaRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        if (!acesso.ehGestaoLocal(user) || user.getGestaoLocal() == null || !Boolean.TRUE.equals(user.getGestaoLocal().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Correção excepcional de identidade é responsabilidade do Gestor Local ativo.");
        }
        Pessoa pessoa = obter(id);
        exigirGestaoDoVinculo(user, pessoa);
        validarCorrecao(req);

        String anterior;
        String novo;
        if (req.tipo() == TipoCorrecaoPessoa.CPF) {
            anterior = pessoa.getCpf();
            novo = normalizarCpf(req.novoValor());
            pessoaRepositorio.findByCpf(novo).filter(p -> !Objects.equals(p.getId(), id)).ifPresent(p -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O novo CPF já pertence a outra pessoa. A correção automática e a fusão silenciosa foram bloqueadas.");
            });
            if (Objects.equals(anterior, novo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O novo CPF é igual ao atual.");
            pessoa.setCpf(novo);
        } else if (req.tipo() == TipoCorrecaoPessoa.NOME) {
            anterior = pessoa.getNome();
            novo = normalizarNome(req.novoValor());
            if (Objects.equals(anterior, novo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O novo nome é igual ao atual.");
            pessoa.setNome(novo);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de correção não suportado.");
        }

        CorrecaoPessoa correcao = new CorrecaoPessoa();
        correcao.setPessoa(pessoa);
        correcao.setTipoCorrecao(req.tipo());
        correcao.setValorAnterior(anterior);
        correcao.setValorNovo(novo);
        correcao.setMotivo(req.motivo());
        correcao.setEvidencia(req.evidencia());
        correcao.setAutorizacao(req.autorizacao());
        correcao.setImpactosEncontrados(req.impactosEncontrados());
        correcao.setResponsavelId(user.getId());
        correcao.setResponsavelUsername(user.getUsername());
        pessoaRepositorio.save(pessoa);
        correcaoRepositorio.save(correcao);
        return paraDTO(pessoa, user, false);
    }

    public List<CorrecaoPessoaDTO> listarCorrecoes(Long pessoaId) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Pessoa pessoa = obter(pessoaId);
        if (!podeConsultar(user, pessoa)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada no seu escopo.");
        return correcaoRepositorio.findByPessoaIdOrderByRegistradoEmAscIdAsc(pessoaId).stream()
                .map(c -> paraDTO(c, user)).toList();
    }

    /** Acesso excepcional ao cadastro integral, sempre escopado e registrado. */
    public PessoaDTO buscarDadosRestritos(Long id, String finalidade) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Pessoa pessoa = obter(id);
        List<VinculoContratual> vinculos = vinculoRepositorio.findByPessoaIdOrderByDataInicioAscIdAsc(id);
        Contrato contrato = vinculos.stream().map(VinculoContratual::getContrato).filter(Objects::nonNull)
                .filter(c -> acesso.podeAcessarContrato(user, c)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pessoa não encontrada no seu escopo."));
        if (acesso.ehGestaoLocal(user)) return paraDTO(pessoa, user, true);
        if (acesso.ehOperacional(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário operacional não acessa o cadastro pessoal integral.");
        }
        TipoPermissaoExcepcional tipo = acesso.ehAdministrador(user)
                ? TipoPermissaoExcepcional.INTERVENCAO_NEGOCIAL_TECNICA
                : TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS;
        permissaoExcepcionalServico.exigirERegistrarUso(user, tipo, contrato,
                contrato.getGestaoLocal(), finalidade, "Pessoa", pessoa.getId());
        return paraDTO(pessoa, user, true);
    }

    public Pessoa obterEntidade(Long id) { return obter(id); }

    public Pessoa obterEntidadeParaAtualizacao(Long id) {
        return pessoaRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada."));
    }

    private void validarCadastro(CadastroPessoaRequisicao req) {
        if (req == null || req.nome() == null || req.nome().isBlank() || req.cpf() == null || req.cpf().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e CPF são obrigatórios.");
        }
        normalizarCpf(req.cpf());
    }
    private void validarCorrecao(CorrecaoIdentidadePessoaRequisicao req) {
        if (req == null || req.tipo() == null || vazio(req.novoValor()) || vazio(req.motivo())
                || vazio(req.evidencia()) || vazio(req.autorizacao())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo, novo valor, motivo, evidência e autorização são obrigatórios na correção excepcional.");
        }
    }

    public static String normalizarCpf(String valor) {
        String cpf = valor == null ? "" : valor.replaceAll("\\D", "");
        if (!cpfValido(cpf)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido.");
        return cpf;
    }

    public static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int digito = 9; digito < 11; digito++) {
            int soma = 0;
            for (int i = 0; i < digito; i++) soma += (cpf.charAt(i) - '0') * (digito + 1 - i);
            int esperado = (soma * 10) % 11;
            if (esperado == 10) esperado = 0;
            if (esperado != cpf.charAt(digito) - '0') return false;
        }
        return true;
    }

    private Pessoa obter(Long id) {
        return pessoaRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada."));
    }
    private void exigirGestao(UsuarioAplicacao user) { acesso.exigirGestaoEmpregado(user); }
    private boolean podeConsultar(UsuarioAplicacao user, Pessoa pessoa) {
        if (acesso.ehGlobal(user)) return true;
        return vinculoRepositorio.findByPessoaIdOrderByDataInicioAscIdAsc(pessoa.getId()).stream()
                .anyMatch(v -> acesso.podeAcessarContrato(user, v.getContrato()));
    }
    private void exigirGestaoDoVinculo(UsuarioAplicacao user, Pessoa pessoa) {
        boolean local = vinculoRepositorio.findByPessoaIdOrderByDataInicioAscIdAsc(pessoa.getId()).stream()
                .anyMatch(v -> v.getContrato() != null && v.getContrato().getGestaoLocal() != null
                        && user.getGestaoLocal() != null
                        && Objects.equals(v.getContrato().getGestaoLocal().getId(), user.getGestaoLocal().getId()));
        if (!local) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada na sua Gestão Local.");
    }

    private void aplicarOpcionaisCadastro(CadastroPessoaRequisicao req, Pessoa p) {
        p.setRg(req.rg()); p.setCtpsSerie(req.ctpsSerie()); p.setNitPisPasep(req.nitPisPasep());
        p.setDataNascimento(req.dataNascimento()); p.setEmail(req.email());
        if (req.endereco() != null) p.setEndereco(mapearEndereco(req.endereco(), null));
    }
    private Endereco mapearEndereco(EnderecoDTO dto, Endereco atual) {
        if (dto.logradouro() == null || dto.logradouro().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logradouro do endereço é obrigatório.");
        Endereco e = atual == null ? new Endereco() : atual;
        e.setLogradouro(dto.logradouro()); e.setNumero(dto.numero()); e.setBairro(dto.bairro());
        e.setCidade(dto.cidade()); e.setUf(dto.uf()); e.setCep(dto.cep());
        return e;
    }
    private String resolverOpcional(String atual, String novo, Boolean limpar) {
        if (Boolean.TRUE.equals(limpar)) return null;
        return novo == null ? atual : novo;
    }
    private String normalizarNome(String valor) {
        if (valor == null || valor.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório.");
        return valor.trim().replaceAll("\\s+", " ");
    }
    private boolean vazio(String v) { return v == null || v.isBlank(); }

    private EscopoConsulta escopoConsulta(UsuarioAplicacao user) {
        if (acesso.ehGlobal(user)) return new EscopoConsulta(true, null, null);
        if (acesso.ehGestorEstadual(user)) {
            SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(user);
            return new EscopoConsulta(false, null, se == null ? null : se.getId());
        }
        GestaoLocal gestao = acesso.gestaoLocalEfetiva(user);
        return new EscopoConsulta(false, gestao == null ? null : gestao.getId(), null);
    }

    private PessoaResumoDTO paraResumo(Pessoa p, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return new PessoaResumoDTO(p.getId(), minimizador.nome(p.getNome(), nivel),
                minimizador.cpf(p.getCpf(), nivel), p.getCriadoEm(), p.getAtualizadoEm(), p.getVersion());
    }

    private record EscopoConsulta(boolean global, Long gestaoLocalId, Long superintendenciaId) {}

    private PessoaDTO paraDTO(Pessoa p, UsuarioAplicacao usuario, boolean acessoExcepcional) {
        NivelExposicaoDadosPessoais nivel = acessoExcepcional
                ? NivelExposicaoDadosPessoais.COMPLETO : minimizador.nivelPadrao(usuario);
        Endereco e = p.getEndereco();
        EnderecoDTO end = e == null || !minimizador.completo(nivel) ? null
                : new EnderecoDTO(e.getId(), e.getLogradouro(), e.getNumero(), e.getBairro(), e.getCidade(), e.getUf(), e.getCep(), e.getVersion());
        return new PessoaDTO(p.getId(), minimizador.nome(p.getNome(), nivel), minimizador.cpf(p.getCpf(), nivel),
                minimizador.restrito(p.getRg(), nivel), minimizador.restrito(p.getCtpsSerie(), nivel),
                minimizador.restrito(p.getNitPisPasep(), nivel), minimizador.restrito(p.getDataNascimento(), nivel),
                minimizador.restrito(p.getEmail(), nivel), end, p.getCriadoEm(), p.getAtualizadoEm(), p.getVersion());
    }
    private CorrecaoPessoaDTO paraDTO(CorrecaoPessoa c, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        String anterior = c.getTipoCorrecao() == TipoCorrecaoPessoa.CPF
                ? minimizador.cpf(c.getValorAnterior(), nivel) : minimizador.nome(c.getValorAnterior(), nivel);
        String novo = c.getTipoCorrecao() == TipoCorrecaoPessoa.CPF
                ? minimizador.cpf(c.getValorNovo(), nivel) : minimizador.nome(c.getValorNovo(), nivel);
        return new CorrecaoPessoaDTO(c.getId(), c.getPessoa().getId(), c.getTipoCorrecao(), anterior, novo,
                c.getMotivo(), minimizador.restrito(c.getEvidencia(), nivel),
                minimizador.restrito(c.getAutorizacao(), nivel), c.getImpactosEncontrados(), c.getResponsavelId(),
                c.getResponsavelUsername(), c.getRegistradoEm());
    }
}
