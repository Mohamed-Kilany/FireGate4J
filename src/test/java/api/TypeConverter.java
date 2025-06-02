package api;

public class TypeConverter {
    public Object convert(String value, String type) {
        if (value == null) return null;

        return switch (type) {
            case "string" -> value;
            case "character" -> {
                if (value.length() != 1)
                    throw new IllegalArgumentException("character type requires single character");
                yield value.charAt(0);
            }
            case "integer" -> Integer.valueOf(value);
            case "long" -> Long.valueOf(value);
            case "float" -> Float.valueOf(value);
            case "double" -> Double.valueOf(value);
            case "boolean" -> Boolean.valueOf(value);
            default -> throw new IllegalArgumentException("unsupported type: " + type);
        };
    }
}
