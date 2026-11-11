package com.example.siggtrat.infra;
/** O contexto mínimo usa a mesma guarda obrigatória do JAR antes do Hibernate. */
public final class GuardaPostgres186 implements org.springframework.beans.factory.config.BeanPostProcessor {
 public static void exigirVersao(String produto, String metadata, String servidor, int numero) {
  com.example.siggtrat.configuracao.VerificadorAmbientePostgres186.exigirVersao(produto, metadata, servidor, numero);
 }
 @Override public Object postProcessAfterInitialization(Object bean, String nome) {
  return new com.example.siggtrat.configuracao.VerificadorAmbientePostgres186().postProcessAfterInitialization(bean, nome);
 }
}
