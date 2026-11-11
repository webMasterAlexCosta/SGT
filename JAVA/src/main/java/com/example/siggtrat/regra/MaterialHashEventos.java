package com.example.siggtrat.regra;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Formato único para novos eventos de Vínculo/Alocação; não lê o formato anterior. */
public final class MaterialHashEventos {
    public static final String CHAVE_MARCADOR = "FORMATO_HASH_EVENTOS_VINCULO_ALOCACAO";
    public static final String FORMATO = "SGT-EVENTOS-UTF8-TIPADO-V1";
    private static final int MAGIC = 0x53475448;
    private static final int VERSAO = 1;

    private MaterialHashEventos() { }

    public enum Tipo {
        TEXTO(1), INT64(2), INT32(3), DATA(4), ENUM(5), GRUPO(6);
        private final int codigo;
        Tipo(int codigo) { this.codigo = codigo; }
    }

    public record Campo(Tipo tipo, Object valor) {
        public Campo {
            Objects.requireNonNull(tipo, "tipo");
            boolean compativel = valor == null || switch (tipo) {
                case TEXTO -> valor instanceof String;
                case INT64 -> valor instanceof Long;
                case INT32 -> valor instanceof Integer;
                case DATA -> valor instanceof LocalDate;
                case ENUM -> valor instanceof Enum<?>;
                case GRUPO -> valor instanceof List<?> lista && lista.stream().allMatch(Campo.class::isInstance);
            };
            if (!compativel) throw new IllegalArgumentException("SGT_HASH_EVENTOS_TIPO_INVALIDO");
            if (tipo == Tipo.GRUPO && valor != null) valor = List.copyOf((List<?>) valor);
        }
    }

    public static Campo texto(String valor) { return new Campo(Tipo.TEXTO, valor); }
    public static Campo longo(Long valor) { return new Campo(Tipo.INT64, valor); }
    public static Campo inteiro(Integer valor) { return new Campo(Tipo.INT32, valor); }
    public static Campo data(LocalDate valor) { return new Campo(Tipo.DATA, valor); }
    public static Campo enumeracao(Enum<?> valor) { return new Campo(Tipo.ENUM, valor); }
    public static Campo grupo(Campo... valores) {
        return new Campo(Tipo.GRUPO, valores == null ? null : List.of(valores));
    }

    public static byte[] material(String dominio, Campo... campos) {
        Objects.requireNonNull(dominio, "dominio");
        Objects.requireNonNull(campos, "campos");
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeInt(MAGIC);
            out.writeShort(VERSAO);
            escrever(out, texto(dominio));
            out.writeInt(campos.length);
            for (Campo campo : campos) escrever(out, Objects.requireNonNull(campo, "campo"));
            out.flush();
            return bytes.toByteArray();
        } catch (java.io.IOException impossivel) {
            throw new IllegalStateException("SGT_HASH_EVENTOS_MATERIAL_INDISPONIVEL", impossivel);
        }
    }

    private static void escrever(DataOutputStream out, Campo campo) throws java.io.IOException {
        out.writeByte(campo.tipo().codigo);
        out.writeBoolean(campo.valor() != null);
        if (campo.valor() == null) { out.writeInt(0); return; }
        byte[] valor;
        if (campo.tipo() == Tipo.GRUPO) {
            var bytes = new ByteArrayOutputStream();
            var grupo = new DataOutputStream(bytes);
            List<?> partes = (List<?>) campo.valor();
            grupo.writeInt(partes.size());
            for (Object parte : partes) escrever(grupo, (Campo) parte);
            grupo.flush();
            valor = bytes.toByteArray();
        } else {
            String canonico = campo.tipo() == Tipo.ENUM ? ((Enum<?>) campo.valor()).name() : campo.valor().toString();
            valor = utf8Estrito(canonico);
        }
        out.writeInt(valor.length);
        out.write(valor);
    }

    private static byte[] utf8Estrito(String texto) {
        try {
            var buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(texto));
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        } catch (CharacterCodingException invalido) {
            throw new IllegalArgumentException("SGT_HASH_EVENTOS_UTF8_INVALIDO");
        }
    }

    public static String sha256(String dominio, Campo... campos) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material(dominio, campos)));
        } catch (java.security.NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("SHA-256 indisponível", impossivel);
        }
    }
}
