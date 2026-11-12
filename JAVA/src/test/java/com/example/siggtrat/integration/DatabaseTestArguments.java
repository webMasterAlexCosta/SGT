package com.example.siggtrat.integration;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.Id;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

final class DatabaseTestArguments {

    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    private DatabaseTestArguments() {
    }

    static Object[] forParameters(Parameter[] parameters) {
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            values[i] = valueFor(parameters[i]);
        }
        return values;
    }

    static Object valueFor(Parameter parameter) {
        return valueFor(parameter.getType(), parameter.getParameterizedType(), parameter.getName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object valueFor(Class<?> raw, Type generic, String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);

        if (raw == String.class) {
            if (n.equals("metrica")) return "registros";
            if (n.contains("status")) return "ATIVO";
            if (n.contains("uf")) return "ZZ";
            if (n.contains("busca") || n.contains("filtro") || n.contains("prefix")) return "__SEM_RESULTADO__";
            return "__TESTE_BANCO__";
        }
        if (raw == Long.class || raw == long.class) return -9_999_991L;
        if (raw == Integer.class || raw == int.class) {
            if (n.contains("pagina") || n.equals("page")) return 0;
            if (n.contains("tamanho") || n.contains("size") || n.contains("limit") || n.contains("batch")) return 10;
            return 1;
        }
        if (raw == Short.class || raw == short.class) return (short) 1;
        if (raw == Byte.class || raw == byte.class) return (byte) 1;
        if (raw == Double.class || raw == double.class) return 1.0d;
        if (raw == Float.class || raw == float.class) return 1.0f;
        if (raw == Boolean.class || raw == boolean.class) return false;
        if (raw == Character.class || raw == char.class) return 'X';
        if (raw == BigDecimal.class) return BigDecimal.ONE;
        if (raw == BigInteger.class) return BigInteger.ONE;
        if (raw == UUID.class) return new UUID(0L, 1L);

        if (raw == LocalDate.class) {
            if (n.contains("inicio")) return DATA.minusDays(7);
            if (n.contains("fim")) return DATA.plusDays(7);
            return DATA;
        }
        if (raw == LocalDateTime.class) return DATA.atTime(12, 0);
        if (raw == LocalTime.class) return LocalTime.NOON;
        if (raw == Instant.class) return DATA.atStartOfDay(ZoneOffset.UTC).toInstant();
        if (raw == ZonedDateTime.class) return DATA.atStartOfDay(ZoneId.of("America/Sao_Paulo"));
        if (raw == OffsetDateTime.class) return DATA.atStartOfDay().atOffset(ZoneOffset.ofHours(-3));
        if (raw == YearMonth.class) return YearMonth.from(DATA);
        if (raw == java.sql.Date.class) return java.sql.Date.valueOf(DATA);
        if (raw == java.sql.Timestamp.class) return java.sql.Timestamp.valueOf(DATA.atTime(12, 0));

        if (Pageable.class.isAssignableFrom(raw)) return PageRequest.of(0, 10, Sort.by("id").ascending());
        if (Sort.class.isAssignableFrom(raw)) return Sort.unsorted();
        if (Specification.class.isAssignableFrom(raw)) {
            return (Specification<Object>) (root, query, cb) -> cb.conjunction();
        }

        if (raw.isEnum()) {
            Object[] constants = raw.getEnumConstants();
            if (constants.length == 0) throw unsupported(raw, name);
            return constants[0];
        }

        if (Optional.class.isAssignableFrom(raw)) return Optional.empty();

        if (Collection.class.isAssignableFrom(raw) || Iterable.class.isAssignableFrom(raw)) {
            Object item = collectionItem(generic);
            if (Set.class.isAssignableFrom(raw)) return Set.of(item);
            return List.of(item);
        }

        if (Map.class.isAssignableFrom(raw)) return Map.of();

        if (raw == Class.class) return Object.class;

        if (raw.getPackageName().startsWith("com.example.siggtrat.entidade")) {
            return entidadeComIdFicticio(raw);
        }

        if (raw.isRecord()) return instanciarRecord(raw);

        throw unsupported(raw, name);
    }

    private static Object collectionItem(Type generic) {
        if (generic instanceof ParameterizedType pt && pt.getActualTypeArguments().length > 0) {
            Type itemType = pt.getActualTypeArguments()[0];
            if (itemType instanceof Class<?> itemClass) {
                return valueForSynthetic(itemClass);
            }
            if (itemType instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> raw) {
                return valueForSynthetic(raw);
            }
        }
        return -9_999_991L;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object valueForSynthetic(Class<?> type) {
        if (type == String.class) return "__TESTE_BANCO__";
        if (type == Long.class || type == long.class) return -9_999_991L;
        if (type == Integer.class || type == int.class) return -9_999_991;
        if (type == LocalDate.class) return DATA;
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants.length > 0) return constants[0];
        }
        if (type.getPackageName().startsWith("com.example.siggtrat.entidade")) {
            return entidadeComIdFicticio(type);
        }
        if (type.isRecord()) return instanciarRecord(type);
        return new Object();
    }

    private static Object entidadeComIdFicticio(Class<?> type) {
        try {
            Constructor<?> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object instance = ctor.newInstance();
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.getAnnotation(Id.class) == null) continue;
                    field.setAccessible(true);
                    if (field.getType() == Long.class || field.getType() == long.class) field.set(instance, -9_999_991L);
                    else if (field.getType() == Integer.class || field.getType() == int.class) field.set(instance, -9_999_991);
                    else throw unsupported(field.getType(), field.getName());
                    return instance;
                }
            }
            throw new IllegalArgumentException("Entidade sem @Id para smoke test: " + type.getName());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Não foi possível instanciar entidade para smoke test: " + type.getName(), ex);
        }
    }

    private static Object instanciarRecord(Class<?> type) {
        try {
            var components = type.getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] values = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
                values[i] = valueFor(parameterTypes[i], components[i].getGenericType(), components[i].getName());
            }
            Constructor<?> ctor = type.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(values);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Não foi possível instanciar record para smoke test: " + type.getName(), ex);
        }
    }

    private static IllegalArgumentException unsupported(Class<?> raw, String name) {
        return new IllegalArgumentException(
                "Tipo de parâmetro ainda não suportado pelo gerador de smoke test: "
                        + raw.getName() + " (" + name + ")");
    }
}
