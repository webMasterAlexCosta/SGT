package com.example.siggtrat.controlador;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.servico.ProvaNomePessoaHistoricoServico;
import com.example.siggtrat.seguranca.ExigirSenha;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
@RestController
@RequestMapping("/api/pessoas/{pessoaId}")
public class ProvaNomePessoaHistoricoControlador {
 private final ProvaNomePessoaHistoricoServico servico;
 public ProvaNomePessoaHistoricoControlador(ProvaNomePessoaHistoricoServico servico){this.servico=servico;}
 @PostMapping("/provas-nome/previa")
 public PreviaProvaHistoricaDTO previa(@PathVariable Long pessoaId,@RequestBody RegistroProvaNomeHistoricoRequisicao r){return servico.previa(pessoaId,r);}
 @ExigirSenha @PostMapping("/provas-nome") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> registrar(@PathVariable Long pessoaId,@RequestBody RegistroProvaNomeHistoricoRequisicao r){return servico.registrar(pessoaId,r);}
 @GetMapping("/provas-nome")
 public List<Map<String,Object>> listar(@PathVariable Long pessoaId,@RequestParam(required=false)Long vinculoId){return servico.listar(pessoaId,vinculoId);}
 @GetMapping("/nome-historico")
 public IdentidadeHistoricaResolvidaDTO consultar(@PathVariable Long pessoaId,@RequestParam Long vinculoId,@RequestParam LocalDate data,@RequestParam(required=false)Instant conhecidoAte){return servico.consultar(pessoaId,vinculoId,data,conhecidoAte);}
}
