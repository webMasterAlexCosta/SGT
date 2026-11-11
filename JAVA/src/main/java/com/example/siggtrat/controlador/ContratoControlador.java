package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.dto.CancelamentoTransferenciaGestaoRequisicao;
import com.example.siggtrat.dto.ContratoDTO;
import com.example.siggtrat.dto.ContratoOpcaoDTO;
import com.example.siggtrat.dto.EventoCicloVidaContratoDTO;
import com.example.siggtrat.dto.PublicacaoContratoDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.dto.PreviaCicloVidaContratoDTO;
import com.example.siggtrat.dto.RelacaoPrecedenciaEventoContratoDTO;
import com.example.siggtrat.dto.TransferenciaGestaoContratoDTO;
import com.example.siggtrat.dto.TransferenciaGestaoContratoRequisicao;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.dto.VersaoContratoLeituraDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoLeituraDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.ContratoServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/contratos")
public class ContratoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ContratoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final ContratoServico servico;

    public ContratoControlador(ContratoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ContratoDTO> listarTodos() {
        return servico.listarTodos();
    }

    @GetMapping("/opcoes")
    public List<ContratoOpcaoDTO> obterOpcoes() {
        return servico.obterOpcoes();
    }

    @GetMapping("/{id}")
    public ContratoDTO buscarPorId(@PathVariable Long id) {
        return servico.buscarPorId(id);
    }

    @GetMapping("/{id}/eventos")
    public List<EventoCicloVidaContratoDTO> listarEventos(@PathVariable Long id) {
        return servico.listarEventos(id);
    }

    @GetMapping("/{id}/eventos/precedencias")
    public List<RelacaoPrecedenciaEventoContratoDTO> listarPrecedencias(@PathVariable Long id) {
        return servico.listarRelacoesPrecedencia(id);
    }

    @GetMapping("/{id}/versoes")
    public List<VersaoContratoLeituraDTO> listarVersoes(@PathVariable Long id,@RequestParam(required=false) Instant conhecidoAte) {
        return servico.listarVersoesNegociaisLeitura(id,conhecidoAte);
    }

    @GetMapping("/{id}/transferencias-gestao")
    public List<TransferenciaGestaoContratoDTO> listarTransferenciasGestao(@PathVariable Long id) {
        return servico.listarTransferenciasGestao(id);
    }

    /**
     * Consulta bitemporal:
     * - data: qual verdade efetiva se deseja reconstruir;
     * - conhecidoAte: opcional, limita a visão ao que já havia sido registrado
     *   naquele instante. Sem conhecidoAte, retorna a verdade efetiva atual.
     */
    @GetMapping("/{id}/posicao-temporal")
    public PosicaoTemporalContratoLeituraDTO consultarPosicaoTemporal(
            @PathVariable Long id,
            @RequestParam LocalDate data,
            @RequestParam(required = false) Instant conhecidoAte
    ) {
        return servico.consultarPosicaoTemporalLeitura(id, data, conhecidoAte);
    }

    /** Cria um Contrato em estado RASCUNHO. */
    @PostMapping
    @ExigirSenha
    public ResponseEntity<ContratoDTO> criar(@RequestBody ContratoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PostMapping("/{id}/publicar")
    @ExigirSenha
    public ContratoDTO publicar(@PathVariable Long id, @RequestBody PublicacaoContratoDTO dto) {
        return servico.publicar(id, dto);
    }

    @PostMapping("/{id}/previas/publicar")
    public PreviaCicloVidaContratoDTO previaPublicacao(@PathVariable Long id, @RequestBody PublicacaoContratoDTO dto) {
        return servico.previaPublicacao(id, dto);
    }

    /**
     * Edição livre somente para RASCUNHO.
     */
    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<ContratoDTO> atualizar(@PathVariable Long id, @RequestBody ContratoDTO dto) {
        return ResponseEntity.ok(servico.atualizar(id, dto));
    }

    @PostMapping("/{id}/inativar")
    @ExigirSenha
    public ContratoDTO inativar(
            @PathVariable Long id,
            @RequestBody AcaoCicloVidaContratoDTO acao
    ) {
        return servico.inativarAdministrativamente(id, acao);
    }

    @PostMapping("/{id}/previas/{operacao}")
    public PreviaCicloVidaContratoDTO previaCicloVida(@PathVariable Long id, @PathVariable String operacao,
                                                    @RequestBody AcaoCicloVidaContratoDTO acao) {
        return servico.previaCicloVida(id, operacao, acao);
    }

    @PostMapping("/{id}/reativar")
    @ExigirSenha
    public ContratoDTO reativar(
            @PathVariable Long id,
            @RequestBody AcaoCicloVidaContratoDTO acao
    ) {
        return servico.reativarAdministrativamente(id, acao);
    }

    @PostMapping("/{id}/encerrar-antecipadamente")
    @ExigirSenha
    public ContratoDTO encerrarAntecipadamente(
            @PathVariable Long id,
            @RequestBody AcaoCicloVidaContratoDTO acao
    ) {
        return servico.encerrarAntecipadamente(id, acao);
    }

    @PostMapping("/{id}/cancelar-futuro")
    @ExigirSenha
    public ContratoDTO cancelarFuturo(
            @PathVariable Long id,
            @RequestBody AcaoCicloVidaContratoDTO acao
    ) {
        return servico.cancelarContratoFuturo(id, acao);
    }

    @PostMapping("/{id}/anular")
    @ExigirSenha
    public ContratoDTO anular(
            @PathVariable Long id,
            @RequestBody AcaoCicloVidaContratoDTO acao
    ) {
        return servico.anular(id, acao);
    }

    @PostMapping("/{id}/corrigir-anulacao")
    @ExigirSenha
    public ContratoDTO corrigirAnulacao(
            @PathVariable Long id,
            @RequestBody AcaoCicloVidaContratoDTO acao
    ) {
        return servico.corrigirAnulacao(id, acao);
    }

    @PostMapping("/{id}/transferir-gestao")
    @ExigirSenha
    public ContratoDTO transferirGestao(@PathVariable Long id,
                                        @RequestBody TransferenciaGestaoContratoRequisicao requisicao) {
        return servico.transferirGestao(id, requisicao);
    }

    @PostMapping("/{id}/transferencias-gestao/{transferenciaId}/cancelar")
    @ExigirSenha
    public ContratoDTO cancelarTransferenciaGestao(@PathVariable Long id, @PathVariable Long transferenciaId,
            @RequestBody CancelamentoTransferenciaGestaoRequisicao requisicao) {
        return servico.cancelarTransferenciaGestao(id, transferenciaId, requisicao);
    }

    @PostMapping("/{id}/transferencias-gestao/previa")
    public PreviaCicloVidaContratoDTO previaTransferenciaGestao(@PathVariable Long id,
            @RequestBody TransferenciaGestaoContratoRequisicao requisicao) {
        return servico.previaTransferenciaGestao(id, requisicao);
    }

    /**
     * Descarte físico apenas de rascunho nunca publicado.
     */
    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
