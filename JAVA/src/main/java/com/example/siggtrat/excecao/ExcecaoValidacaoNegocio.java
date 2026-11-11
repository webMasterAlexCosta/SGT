package com.example.siggtrat.excecao;

/** Mensagem de validação escrita pela aplicação, deliberadamente pública. */
public class ExcecaoValidacaoNegocio extends IllegalArgumentException {
    public ExcecaoValidacaoNegocio(String mensagem) { super(mensagem); }
    public ExcecaoValidacaoNegocio(String mensagem, Throwable causa) { super(mensagem, causa); }
}
