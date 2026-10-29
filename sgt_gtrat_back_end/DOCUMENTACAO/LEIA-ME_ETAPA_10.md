# SGT-GTRAT — Leia-me da Etapa 10

**Data-base:** 09/09/2026  
**Escopo:** perfis, permissões excepcionais e minimização de dados  
**Plataforma:** Java 17, Spring Boot 3.5.16 e MySQL 8.4

## Resultado

A Etapa 10 está concluída. Os requisitos `PER-001` a `PER-012` foram fechados com:

- um único perfil canônico por conta;
- seis perfis explícitos: Administrador, Gestor Nacional, Gestor Estadual, Gestor Local, Presidente e Operacional;
- escrita negocial concentrada no Gestor Local de escopo compatível;
- administração técnica separada da responsabilidade negocial;
- concessões excepcionais temporárias, específicas, revogáveis e auditadas;
- minimização de CPF, matrícula, endereço, telefone e dados bancários segundo perfil e finalidade;
- correção versionada de número/ano de contrato;
- autorização específica para redução excepcional e transferência de Gestão Local;
- preservação do escopo histórico de frequência.

## Início rápido

1. Crie um banco MySQL 8.4 vazio, por exemplo `sgt_gtrat_etapa10`.
2. Copie `.env.example` para `.env`.
3. Troque obrigatoriamente `DB_PASS`, `JWT_SECRET`, `AUDIT_SECRET` e senhas iniciais.
4. Ajuste `DB_URL` para a base nova.
5. Execute `./mvnw spring-boot:run` ou gere o JAR com `./mvnw clean package`.

O `JPA_DDL_AUTO=update` cria o schema canônico em uma base vazia e preserva os dados nas reinicializações. A entrega não tenta converter tabelas ou autoridades de versões antigas.

## Comandos de validação

```bash
./mvnw clean test
./mvnw -Pintegration verify
./mvnw -Pdb-load verify
```

As duas últimas suítes exigem Docker/Testcontainers ou as variáveis `INTEGRATION_MYSQL_URL`, `INTEGRATION_MYSQL_USER` e `INTEGRATION_MYSQL_PASSWORD` apontando para um MySQL descartável.

## Atenção para o futuro React

O contrato de usuário mudou de `role`/`roles` para:

```json
{
  "perfil": "GESTOR_LOCAL",
  "autoridade": "PERFIL_GESTOR_LOCAL"
}
```

O frontend deverá usar exclusivamente `perfil` e `autoridade`. A documentação completa está em `SGT_GTRAT_API_PERFIS_PERMISSOES_ETAPA_10.md`.
