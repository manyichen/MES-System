package com.example.messystem.demo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonCodec {
    private JsonCodec() {
    }

    static String ok(String message, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("message", message);
        payload.put("data", data);
        return stringify(payload);
    }

    static String fail(String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("message", message);
        payload.put("data", null);
        return stringify(payload);
    }

    static <T> T parseObject(String json, Class<T> type) {
        Object value = new Parser(json).parseValue();
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("request body must be a JSON object");
        }
        return mapToObject(map, type);
    }

    private static <T> T mapToObject(Map<?, ?> map, Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getFields()) {
                if (map.containsKey(field.getName())) {
                    field.set(instance, convert(map.get(field.getName()), field.getType(), field.getGenericType()));
                }
            }
            return instance;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("cannot parse request body", ex);
        }
    }

    private static Object convert(Object value, Class<?> targetType, Type genericType) {
        if (value == null) {
            return null;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        }
        if (targetType == BigDecimal.class) {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(String.valueOf(value));
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(String.valueOf(value));
        }
        if (List.class.isAssignableFrom(targetType) && value instanceof List<?> source) {
            List<Object> result = new ArrayList<>();
            Class<?> itemType = Object.class;
            if (genericType instanceof ParameterizedType parameterizedType
                    && parameterizedType.getActualTypeArguments()[0] instanceof Class<?> clazz) {
                itemType = clazz;
            }
            for (Object item : source) {
                if (item instanceof Map<?, ?> itemMap && itemType != Object.class) {
                    result.add(mapToObject(itemMap, itemType));
                } else {
                    result.add(item);
                }
            }
            return result;
        }
        if (value instanceof Map<?, ?> nested) {
            return mapToObject(nested, targetType);
        }
        return value;
    }

    private static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        writeJson(out, value);
        return out.toString();
    }

    private static void writeJson(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (value instanceof String || value instanceof Character || value instanceof LocalDate || value instanceof LocalDateTime) {
            writeString(out, String.valueOf(value));
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                writeString(out, String.valueOf(entry.getKey()));
                out.append(':');
                writeJson(out, entry.getValue());
                first = false;
            }
            out.append('}');
            return;
        }
        if (value instanceof Collection<?> collection) {
            out.append('[');
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    out.append(',');
                }
                writeJson(out, item);
                first = false;
            }
            out.append(']');
            return;
        }
        out.append('{');
        boolean first = true;
        for (Field field : value.getClass().getFields()) {
            try {
                if (!first) {
                    out.append(',');
                }
                writeString(out, field.getName());
                out.append(':');
                writeJson(out, field.get(value));
                first = false;
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
        out.append('}');
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 32) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }

    static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class Parser {
        private final String text;
        private int pos;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= text.length()) {
                return null;
            }
            char ch = text.charAt(pos);
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == '"') {
                return parseString();
            }
            if (ch == 't' || ch == 'f') {
                return parseBoolean();
            }
            if (ch == 'n') {
                pos += 4;
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            while (pos < text.length() && text.charAt(pos) != '}') {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < text.length() && text.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                }
            }
            expect('}');
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWhitespace();
            while (pos < text.length() && text.charAt(pos) != ']') {
                list.add(parseValue());
                skipWhitespace();
                if (pos < text.length() && text.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                }
            }
            expect(']');
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (pos < text.length()) {
                char ch = text.charAt(pos++);
                if (ch == '"') {
                    return out.toString();
                }
                if (ch == '\\') {
                    char escaped = text.charAt(pos++);
                    switch (escaped) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            String hex = text.substring(pos, pos + 4);
                            out.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> out.append(escaped);
                    }
                } else {
                    out.append(ch);
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        private Boolean parseBoolean() {
            if (text.startsWith("true", pos)) {
                pos += 4;
                return true;
            }
            pos += 5;
            return false;
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < text.length()) {
                char ch = text.charAt(pos);
                if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String raw = text.substring(start, pos);
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                return new BigDecimal(raw);
            }
            return Long.parseLong(raw);
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private void expect(char expected) {
            if (pos >= text.length() || text.charAt(pos) != expected) {
                throw new IllegalArgumentException("expected '" + expected + "'");
            }
            pos++;
        }
    }
}
