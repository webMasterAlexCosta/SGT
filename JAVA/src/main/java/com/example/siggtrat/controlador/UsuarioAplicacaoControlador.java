package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.UsuarioAplicacaoServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioAplicacaoControlador {
    private final UsuarioAplicacaoServico appUserServico;
    public UsuarioAplicacaoControlador(UsuarioAplicacaoServico appUserServico) { this.appUserServico = appUserServico; }

    @GetMapping
    public ResponseEntity<List<ResumoUsuarioAplicacaoDTO>> listarTodos() { return ResponseEntity.ok(appUserServico.listarTodos()); }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<ResumoUsuarioAplicacaoDTO> criar(@Valid @RequestBody CriacaoUsuarioAplicacaoDTO payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserServico.criar(payload));
    }

    @PostMapping("/{id}/conceder-acesso-retroativo")
    @ExigirSenha
    public ResponseEntity<MensagemApiDTO> concederAcessoRetroativo(@PathVariable Long id) {
        return ResponseEntity.ok(MensagemApiDTO.sucesso(appUserServico.concederAcessoRetroativo(id)));
    }

    @PostMapping("/{id}/revogar-acesso-retroativo")
    @ExigirSenha
    public ResponseEntity<MensagemApiDTO> revogarAcessoRetroativo(@PathVariable Long id) {
        appUserServico.revogarAcessoRetroativo(id);
        return ResponseEntity.ok(MensagemApiDTO.sucesso("Acesso retroativo bloqueado com sucesso."));
    }

    @PostMapping("/{id}/redefinir-senha")
    @ExigirSenha
    public ResponseEntity<ResultadoRedefinicaoSenhaDTO> redefinirSenha(@PathVariable Long id,
                                           @Valid @RequestBody RedefinicaoSenhaUsuarioAplicacaoDTO payload) {
        appUserServico.redefinirSenha(id, payload);
        return ResponseEntity.ok(new ResultadoRedefinicaoSenhaDTO(
                "Senha redefinida com sucesso. As sessões anteriores foram encerradas.",
                true
        ));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<MensagemApiDTO> atualizar(@PathVariable Long id, @Valid @RequestBody AtualizacaoUsuarioAplicacaoDTO payload) {
        appUserServico.atualizar(id, payload);
        return ResponseEntity.ok(MensagemApiDTO.sucesso("Usuário atualizado com sucesso."));
    }

    @GetMapping("/meu-perfil")
    public ResponseEntity<MeuPerfilDTO> obterMeuPerfil() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(appUserServico.obterMeuPerfil(auth.getName()));
    }

    @PostMapping("/{id}/ativar")
    @ExigirSenha
    public ResponseEntity<AtivacaoUsuarioAplicacaoDTO> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(appUserServico.ativarUsuario(id));
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<MensagemApiDTO> excluir(@PathVariable Long id) {
        appUserServico.excluirUsuario(id);
        return ResponseEntity.ok(MensagemApiDTO.sucesso("Usuário desativado com sucesso."));
    }
}
