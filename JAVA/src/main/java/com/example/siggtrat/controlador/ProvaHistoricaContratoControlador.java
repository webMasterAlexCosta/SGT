package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.ProvaHistoricaContratoServico;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@InvalidaCacheNegocio
@RequestMapping("/api/contratos/{contratoId}/provas-historicas")
public class ProvaHistoricaContratoControlador {
    private final ProvaHistoricaContratoServico servico;
    public ProvaHistoricaContratoControlador(ProvaHistoricaContratoServico servico){this.servico=servico;}
    @PostMapping("/previa")
    public PreviaProvaHistoricaContratoDTO previa(@PathVariable Long contratoId,@RequestBody ProvaHistoricaContratoRequisicao r){return servico.previa(contratoId,r);}
    @PostMapping @ExigirSenha @ResponseStatus(HttpStatus.CREATED)
    public ProvaHistoricaContratoDTO incorporar(@PathVariable Long contratoId,@RequestBody ProvaHistoricaContratoRequisicao r){return servico.incorporar(contratoId,r);}
    @GetMapping
    public List<ProvaHistoricaContratoDTO> listar(@PathVariable Long contratoId,@RequestParam(required=false) Instant conhecidoAte){return servico.listar(contratoId,conhecidoAte);}
}
