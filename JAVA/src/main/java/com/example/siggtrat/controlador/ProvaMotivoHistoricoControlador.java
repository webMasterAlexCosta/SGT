package com.example.siggtrat.controlador;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.servico.MotivoDesligamentoHistoricoServico;
import com.example.siggtrat.seguranca.ExigirSenha;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
@RestController
@RequestMapping("/api/motivos-desligamento/{id}")
public class ProvaMotivoHistoricoControlador {
 private final MotivoDesligamentoHistoricoServico servico;
 public ProvaMotivoHistoricoControlador(MotivoDesligamentoHistoricoServico servico){this.servico=servico;}
 @PostMapping("/provas-historicas/previa")
 public PreviaProvaHistoricaDTO previa(@PathVariable Long id,@RequestBody RegistroProvaMotivoHistoricoRequisicao r){return servico.previa(id,r);}
 @ExigirSenha @PostMapping("/provas-historicas") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> registrar(@PathVariable Long id,@RequestBody RegistroProvaMotivoHistoricoRequisicao r){return servico.registrar(id,r);}
 @GetMapping("/provas-historicas")
 public List<Map<String,Object>> listar(@PathVariable Long id,@RequestParam Long contratoId){return servico.listar(id,contratoId);}
 @GetMapping("/contexto-historico")
 public ContextoMotivoHistoricoDTO consultar(@PathVariable Long id,@RequestParam Long contratoId,@RequestParam LocalDate data,@RequestParam(required=false)Instant conhecidoAte){return servico.consultar(id,contratoId,data,conhecidoAte);}
}
