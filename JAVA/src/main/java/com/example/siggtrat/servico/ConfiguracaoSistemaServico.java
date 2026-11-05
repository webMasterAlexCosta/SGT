package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesSistema;
import com.example.siggtrat.dto.MensagemApiDTO;
import com.example.siggtrat.dto.ConfiguracaoSistemaDTO;
import com.example.siggtrat.dto.ConfiguracaoPublicaSistemaDTO;
import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfiguracaoSistemaServico {
    private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Set<String> CHAVES_EDITAVEIS = Set.of(
            "HORAS_DIA_TRABALHO", "HORA_INICIO_NOTURNO", "MINUTOS_HORA_NOTURNA"
    );

    private final boolean emManutencao;
    private final String previsaoRetorno;
    private final ConfiguracaoSistemaRepositorio repositorio;
    private final AcessoOrganizacionalServico acesso;

    public ConfiguracaoSistemaServico(ConfiguracaoSistemaRepositorio repositorio, PropriedadesSistema properties,
                                     AcessoOrganizacionalServico acesso) {
        this.repositorio = repositorio;
        this.acesso = acesso;
        this.emManutencao = properties.manutencao();
        this.previsaoRetorno = properties.previsaoRetorno();
    }

    @Transactional(readOnly = true)
    public ConfiguracaoPublicaSistemaDTO configuracaoPublica(String clientIp) {
        Map<String, String> configs = repositorio.findAll().stream().collect(Collectors.toMap(
                ConfiguracaoSistema::getChave, ConfiguracaoSistema::obterValor, (a, b) -> b));
        return new ConfiguracaoPublicaSistemaDTO(
                emManutencao,
                previsaoRetorno == null ? "" : previsaoRetorno,
                clientIp,
                interpretarInteiro(configs.get("HORAS_DIA_TRABALHO"), 8),
                interpretarHorario(configs.get("HORA_INICIO_NOTURNO"), "22:00"),
                interpretarDecimal(configs.get("MINUTOS_HORA_NOTURNA"), 52.5)
        );
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoSistemaDTO> listar() {
        return repositorio.findAll().stream().map(this::paraDTO).toList();
    }

    @Transactional
    public MensagemApiDTO salvar(List<ConfiguracaoSistemaDTO> configuracoes) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        if (configuracoes == null || configuracoes.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma configuração.");
        }
        List<ConfiguracaoSistema> entities = configuracoes.stream().map(dto -> {
            validar(dto);
            return new ConfiguracaoSistema(dto.chave(), dto.valor(), dto.descricao());
        }).toList();
        // A ordem das chaves é estável para escritores que recebem lotes diferentes.
        // Nenhum valor é alterado antes da espera e da autorização corrente.
        entities.stream().map(ConfiguracaoSistema::getChave).distinct().sorted()
                .forEach(repositorio::buscarPorChaveParaAtualizacao);
        acesso.revalidarAposEspera(usuario);
        if (!acesso.ehAdministrador(usuario)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Somente o Administrador pode alterar configurações.");
        repositorio.saveAll(entities);
        return MensagemApiDTO.sucesso("Configurações atualizadas com sucesso!");
    }

    private void validar(ConfiguracaoSistemaDTO config) {
        if (config == null || config.chave() == null || !CHAVES_EDITAVEIS.contains(config.chave())) {
            throw new IllegalArgumentException("Chave de configuração não permitida.");
        }
        String valor = config.valor();
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O valor da configuração " + config.chave() + " é obrigatório.");
        }
        switch (config.chave()) {
            case "HORAS_DIA_TRABALHO" -> {
                int horas = Integer.parseInt(valor);
                if (horas < 1 || horas > 24) throw new IllegalArgumentException("HORAS_DIA_TRABALHO deve estar entre 1 e 24.");
            }
            case "HORA_INICIO_NOTURNO" -> LocalTime.parse(valor, HORA_FORMAT);
            case "MINUTOS_HORA_NOTURNA" -> {
                double minutos = Double.parseDouble(valor);
                if (!Double.isFinite(minutos) || minutos <= 0 || minutos > 60) {
                    throw new IllegalArgumentException("MINUTOS_HORA_NOTURNA deve ser maior que zero e no máximo 60.");
                }
            }
            default -> throw new IllegalArgumentException("Chave de configuração não permitida.");
        }
    }

    private ConfiguracaoSistemaDTO paraDTO(ConfiguracaoSistema c) {
        return new ConfiguracaoSistemaDTO(c.getChave(), c.obterValor(), c.getDescricao());
    }

    private int interpretarInteiro(String value, int fallback) {
        try { int parsed = value == null ? fallback : Integer.parseInt(value); return parsed >= 1 && parsed <= 24 ? parsed : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }
    private double interpretarDecimal(String value, double fallback) {
        try { double parsed = value == null ? fallback : Double.parseDouble(value); return Double.isFinite(parsed) && parsed > 0 && parsed <= 60 ? parsed : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }
    private String interpretarHorario(String value, String fallback) {
        try { return value == null ? fallback : LocalTime.parse(value, HORA_FORMAT).format(HORA_FORMAT); }
        catch (java.time.format.DateTimeParseException e) { return fallback; }
    }
}
