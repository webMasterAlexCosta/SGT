package com.example.siggtrat.infra;

import com.example.siggtrat.configuracao.TratadorGlobalExcecoes;
import com.example.siggtrat.dto.FornecedorEntradaDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato público: código estável sem perder status, campos ou sanitização. */
class TratadorGlobalCodigoErroTest {
    private final TratadorGlobalExcecoes tratador = new TratadorGlobalExcecoes();

    @ParameterizedTest @ValueSource(ints = {400, 403, 404, 409, 428})
    void statusExplicitoPreservaMensagemPublicaEAcrescentaCodigo(int status) {
        String mensagem = "Operação recusada por uma regra pública.";
        var resposta = tratador.tratarStatusResposta(new ResponseStatusException(HttpStatus.valueOf(status), mensagem));
        var corpo = resposta.getBody();
        assertNotNull(corpo);
        assertAll(
                () -> assertEquals(status, resposta.getStatusCode().value()),
                () -> assertEquals(status, corpo.get("status")),
                () -> assertEquals("HTTP_" + status, corpo.get("code")),
                () -> assertEquals(mensagem, corpo.get("message")),
                () -> assertNotNull(corpo.get("timestamp")),
                () -> assertNotNull(corpo.get("error")));
    }

    @Test void erroInternoNaoExpoeMotivoTecnicoAoAcrescentarCodigo() {
        String privado = "SENTINELA_FICTICIA_NAO_PUBLICA";
        var resposta = tratador.tratarStatusResposta(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, privado));
        var corpo = resposta.getBody();
        assertNotNull(corpo);
        assertAll(
                () -> assertEquals(500, resposta.getStatusCode().value()),
                () -> assertEquals("HTTP_500", corpo.get("code")),
                () -> assertFalse(corpo.toString().contains(privado)),
                () -> assertEquals("Não foi possível concluir esta operação. Informe o identificador da requisição ao suporte.", corpo.get("message")));
    }

    @Test void motivoAusenteMantemOrientacaoSegura() {
        var corpo = tratador.tratarStatusResposta(new ResponseStatusException(HttpStatus.BAD_REQUEST)).getBody();
        assertNotNull(corpo);
        assertAll(
                () -> assertEquals("HTTP_400", corpo.get("code")),
                () -> assertEquals("A operação foi recusada. Verifique os dados e sua permissão.", corpo.get("message")));
    }

    @Test void validacaoDeCorpoPreservaCamposEMensagem() throws Exception {
        var erros = new BeanPropertyBindingResult(new Object(), "entrada");
        erros.addError(new FieldError("entrada", "email", "Informe um e-mail válido."));
        var parametro = new MethodParameter(getClass().getDeclaredMethod("receber", FornecedorEntradaDTO.class), 0);
        var resposta = tratador.tratarValidacao(new MethodArgumentNotValidException(parametro, erros));
        var corpo = resposta.getBody();
        assertNotNull(corpo);
        assertAll(
                () -> assertEquals(400, resposta.getStatusCode().value()),
                () -> assertEquals("VALIDATION_ERROR", corpo.get("code")),
                () -> assertEquals("Informe um e-mail válido.", corpo.get("message")),
                () -> assertEquals(List.of(java.util.Map.of("field", "email", "message", "Informe um e-mail válido.")), corpo.get("fields")));
    }

    @Test void violacaoDeRestricaoPreservaCampoSemExporValor() {
        try (var fabrica = Validation.buildDefaultValidatorFactory()) {
            var violacoes = fabrica.getValidator().validateValue(FornecedorEntradaDTO.class, "email", "sem-arroba");
            assertEquals(1, violacoes.size());
            var resposta = tratador.tratarViolacaoRestricao(new ConstraintViolationException(violacoes));
            var corpo = resposta.getBody();
            assertNotNull(corpo);
            assertAll(
                    () -> assertEquals(400, resposta.getStatusCode().value()),
                    () -> assertEquals("VALIDATION_ERROR", corpo.get("code")),
                    () -> assertEquals("Erro de validação", corpo.get("message")),
                    () -> assertTrue(corpo.get("fields").toString().contains("email")),
                    () -> assertFalse(corpo.toString().contains("sem-arroba")));
        }
    }

    private void receber(FornecedorEntradaDTO entrada) { }
}
