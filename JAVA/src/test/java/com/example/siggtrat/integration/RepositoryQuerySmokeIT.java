package com.example.siggtrat.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.BaseStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executa todos os métodos de repositorio definidos pela aplicação contra um
 * PostgreSQL isolado com schema real e IDs inexistentes. O objetivo é detectar JPQL inválida,
 * derived-query quebrada, coluna/tabela divergente e SQL nativo incompatível.
 */
@DisplayName("Integração PostgreSQL: smoke de todas as queries de repositorio")
class RepositoryQuerySmokeIT extends PostgresIntegrationTestSupport {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("todos os métodos declarados em Spring Data repositories executam sem erro SQL")
    void executarMetodosSpringDataDeclaradosPelaAplicacao() throws Exception {
        List<String> falhas = new ArrayList<>();
        List<Class<?>> repositories = descobrirSpringDataRepositories();

        for (Class<?> repositoryType : repositories) {
            Object bean = context.getBean(repositoryType);
            for (Method method : repositoryType.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic() || method.isBridge()) continue;
                executarIsolado(repositoryType.getSimpleName(), bean, method, falhas);
            }
        }

        assertTrue(!repositories.isEmpty(), "Nenhum Spring Data repositorio foi descoberto no classpath de teste.");
        assertTrue(falhas.isEmpty(), () -> "Queries de repositorio com falha:\n" + String.join("\n", falhas));
    }

    @Test
    @DisplayName("repositories JDBC/Criteria concretos executam todos os métodos públicos")
    void executarRepositoriesConcretos() throws Exception {
        List<String> falhas = new ArrayList<>();
        List<Class<?>> concreteRepositories = descobrirRepositoriesConcretos();

        for (Class<?> type : concreteRepositories) {
            Object bean = context.getBean(type);
            for (Method method : type.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)
                        || method.isSynthetic() || method.isBridge()) continue;
                if (method.getName().equals(type.getSimpleName())) continue;
                // Injeção de dependências não é uma operação de consulta.
                if (method.isAnnotationPresent(Autowired.class)) continue;
                executarIsolado(type.getSimpleName(), bean, method, falhas);
            }
        }

        assertTrue(!concreteRepositories.isEmpty(), "Nenhum repositorio concreto foi descoberto.");
        assertTrue(falhas.isEmpty(), () -> "Chamadas JDBC/Criteria com falha:\n" + String.join("\n", falhas));
    }

    private void executarIsolado(String owner, Object bean, Method method, List<String> falhas) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            tx.executeWithoutResult(status -> {
                try {
                    Object result = method.invoke(bean, DatabaseTestArguments.forParameters(method.getParameters()));
                    fecharSeNecessario(result);
                    status.setRollbackOnly();
                } catch (InvocationTargetException ex) {
                    if (ex.getTargetException() instanceof org.springframework.dao.EmptyResultDataAccessException) {
                        // queryForObject executou SQL válido para o ID inexistente.
                        status.setRollbackOnly();
                        return;
                    }
                    throw rethrow(ex.getTargetException());
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        } catch (RuntimeException ex) {
            falhas.add(owner + "." + assinatura(method) + " -> " + mensagemRaiz(ex));
        }
    }

    private List<Class<?>> descobrirSpringDataRepositories() throws Exception {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> type : descobrirClassesRepositorio()) {
            if (type.isInterface() && Repository.class.isAssignableFrom(type)) result.add(type);
        }
        result.sort(Comparator.comparing(Class::getName));
        return result;
    }

    private List<Class<?>> descobrirRepositoriesConcretos() throws Exception {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> type : descobrirClassesRepositorio()) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) continue;
            if (type.getAnnotation(org.springframework.stereotype.Repository.class) != null
                    || type.getAnnotation(Component.class) != null) {
                result.add(type);
            }
        }
        result.sort(Comparator.comparing(Class::getName));
        return result;
    }

    private List<Class<?>> descobrirClassesRepositorio() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:com/example/siggtrat/repositorio/*Repositorio.class");
        List<Class<?>> result = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !filename.endsWith("Repositorio.class") || filename.contains("$")) continue;
            String simpleName = filename.substring(0, filename.length() - ".class".length());
            result.add(Class.forName("com.example.siggtrat.repositorio." + simpleName));
        }
        return result;
    }

    private static void fecharSeNecessario(Object result) {
        if (result instanceof BaseStream<?, ?> stream) stream.close();
        else if (result instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                throw new IllegalStateException("Falha ao fechar resultado do repositorio", ex);
            }
        }
    }

    private static RuntimeException rethrow(Throwable throwable) {
        if (throwable instanceof RuntimeException runtime) return runtime;
        return new IllegalStateException(throwable);
    }

    private static String assinatura(Method method) {
        return method.getName() + "(" + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .reduce((a, b) -> a + "," + b)
                .orElse("") + ")";
    }

    private static String mensagemRaiz(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }
}
