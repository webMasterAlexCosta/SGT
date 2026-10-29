package com.example.siggtrat.integration;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração MySQL: contrato do schema Hibernate")
class DatabaseSchemaContractIT extends MySqlIntegrationTestSupport {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("todas as entidades conseguem executar SELECT contra o schema criado pelo Hibernate")
    void todasEntidadesSaoConsultaveis() {
        List<String> falhas = new ArrayList<>();

        for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
            try {
                entityManager.createQuery("select e from " + entity.getName() + " e")
                        .setMaxResults(1)
                        .getResultList();
            } catch (RuntimeException ex) {
                falhas.add(entity.getJavaType().getSimpleName() + ": " + mensagemRaiz(ex));
            }
        }

        assertTrue(falhas.isEmpty(), () -> "Mapeamentos incompatíveis com o schema:\n" + String.join("\n", falhas));
    }

    @Test
    @DisplayName("tabelas, índices, uniques, FKs e NOT NULL declarados existem fisicamente")
    void metadadosFisicosRespeitamAsAnotacoesJpa() throws Exception {
        List<String> falhas = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            String catalog = connection.getCatalog();

            for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
                Class<?> type = entity.getJavaType();
                Table table = type.getAnnotation(Table.class);
                String tableName = table == null || table.name().isBlank()
                        ? camelParaSnake(type.getSimpleName())
                        : table.name();

                if (!tableExists(md, catalog, tableName)) {
                    falhas.add(type.getSimpleName() + ": tabela ausente " + tableName);
                    continue;
                }

                Set<String> indexes = indexNames(md, catalog, tableName);
                if (table != null) {
                    for (Index index : table.indexes()) {
                        if (!index.name().isBlank() && !containsIgnoreCase(indexes, index.name())) {
                            falhas.add(tableName + ": índice ausente " + index.name());
                        }
                    }
                    for (UniqueConstraint unique : table.uniqueConstraints()) {
                        if (!unique.name().isBlank() && !containsIgnoreCase(indexes, unique.name())) {
                            falhas.add(tableName + ": unique ausente " + unique.name());
                        }
                    }
                }

                Map<String, Boolean> nullableByColumn = nullableByColumn(md, catalog, tableName);
                Set<String> importedFkColumns = importedForeignKeyColumns(md, catalog, tableName);

                for (Field field : todosCampos(type)) {
                    if (field.isAnnotationPresent(Transient.class)) continue;

                    Column column = field.getAnnotation(Column.class);
                    if (column != null && !column.nullable()) {
                        String columnName = column.name().isBlank() ? camelParaSnake(field.getName()) : column.name();
                        Boolean nullable = getIgnoreCase(nullableByColumn, columnName);
                        if (nullable == null) {
                            falhas.add(tableName + ": coluna NOT NULL mapeada não existe: " + columnName);
                        } else if (nullable) {
                            falhas.add(tableName + ": coluna deveria ser NOT NULL: " + columnName);
                        }
                    }

                    JoinColumn join = field.getAnnotation(JoinColumn.class);
                    if (join != null && !join.name().isBlank()) {
                        if (!join.nullable()) {
                            Boolean nullable = getIgnoreCase(nullableByColumn, join.name());
                            if (nullable == null) {
                                falhas.add(tableName + ": FK NOT NULL mapeada não existe: " + join.name());
                            } else if (nullable) {
                                falhas.add(tableName + ": FK deveria ser NOT NULL: " + join.name());
                            }
                        }
                        if (!containsIgnoreCase(importedFkColumns, join.name())) {
                            falhas.add(tableName + ": constraint FK ausente para " + join.name());
                        }
                    }

                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                    if (joinTable != null && !joinTable.name().isBlank()) {
                        validarJoinTable(md, catalog, joinTable, falhas);
                    }
                }
            }
        }

        assertTrue(falhas.isEmpty(), () -> "Divergências entre JPA e MySQL:\n" + String.join("\n", falhas));
    }

    private static void validarJoinTable(DatabaseMetaData md, String catalog, JoinTable joinTable,
                                         List<String> falhas) throws Exception {
        String name = joinTable.name();
        if (!tableExists(md, catalog, name)) {
            falhas.add("join table ausente: " + name);
            return;
        }
        Set<String> indexes = indexNames(md, catalog, name);
        for (Index index : joinTable.indexes()) {
            if (!index.name().isBlank() && !containsIgnoreCase(indexes, index.name())) {
                falhas.add(name + ": índice ausente " + index.name());
            }
        }
        for (UniqueConstraint unique : joinTable.uniqueConstraints()) {
            if (!unique.name().isBlank() && !containsIgnoreCase(indexes, unique.name())) {
                falhas.add(name + ": unique ausente " + unique.name());
            }
        }
        Set<String> fks = importedForeignKeyColumns(md, catalog, name);
        for (JoinColumn column : joinTable.joinColumns()) {
            if (!column.name().isBlank() && !containsIgnoreCase(fks, column.name())) {
                falhas.add(name + ": FK ausente para " + column.name());
            }
        }
        for (JoinColumn column : joinTable.inverseJoinColumns()) {
            if (!column.name().isBlank() && !containsIgnoreCase(fks, column.name())) {
                falhas.add(name + ": FK inversa ausente para " + column.name());
            }
        }
    }

    private static boolean tableExists(DatabaseMetaData md, String catalog, String table) throws Exception {
        try (ResultSet rs = md.getTables(catalog, null, table, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = md.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) return true;
            }
        }
        return false;
    }

    private static Set<String> indexNames(DatabaseMetaData md, String catalog, String table) throws Exception {
        Set<String> names = new HashSet<>();
        try (ResultSet rs = md.getIndexInfo(catalog, null, table, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name != null) names.add(name);
            }
        }
        return names;
    }

    private static Map<String, Boolean> nullableByColumn(DatabaseMetaData md, String catalog, String table)
            throws Exception {
        Map<String, Boolean> result = new HashMap<>();
        try (ResultSet rs = md.getColumns(catalog, null, table, "%")) {
            while (rs.next()) {
                result.put(rs.getString("COLUMN_NAME"),
                        rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls);
            }
        }
        return result;
    }

    private static Set<String> importedForeignKeyColumns(DatabaseMetaData md, String catalog, String table)
            throws Exception {
        Set<String> result = new HashSet<>();
        try (ResultSet rs = md.getImportedKeys(catalog, null, table)) {
            while (rs.next()) result.add(rs.getString("FKCOLUMN_NAME"));
        }
        return result;
    }

    private static List<Field> todosCampos(Class<?> type) {
        List<Field> result = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            result.addAll(Arrays.asList(current.getDeclaredFields()));
        }
        return result;
    }

    private static boolean containsIgnoreCase(Collection<String> values, String expected) {
        return values.stream().anyMatch(v -> v.equalsIgnoreCase(expected));
    }

    private static Boolean getIgnoreCase(Map<String, Boolean> values, String expected) {
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(expected)) return entry.getValue();
        }
        return null;
    }

    private static String camelParaSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String mensagemRaiz(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) current = current.getCause();
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }
}
