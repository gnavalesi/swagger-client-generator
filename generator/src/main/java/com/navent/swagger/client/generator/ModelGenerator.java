package com.navent.swagger.client.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.squareup.javapoet.*;
import org.json.JSONObject;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelGenerator {


    public static void generate(String basePackage, String serviceName, String outputPath, JSONObject definition) {
        ModelGenerator.generateModels(definition.getJSONObject("definitions")).forEach(m -> {
            try {
                JavaFile.builder(basePackage + ".client.model", m)
                        .build()
                        .writeTo(new File(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    static Iterable<TypeSpec> generateModels(JSONObject definitions) {
        return definitions.keySet().stream()
                .map(k -> generateModel(k, definitions.getJSONObject(k)))
                .collect(Collectors.toList());
    }

    public static TypeSpec generateModel(String name, JSONObject definition) {
        String type = definition.getString("type");

        TypeSpec.Builder builder;
        if ("object".equals(type) || null == type) {
            builder = generateObject(name, definition);
        } else {
            throw new UnknownModelTypeException(type);
        }

        return builder.addModifiers(Modifier.PUBLIC)
                .addAnnotation(lombok.Data.class)
                .build();
    }

    private static TypeSpec.Builder generateObject(String name, JSONObject definition) {
        return TypeSpec.classBuilder(name)
                .addFields(FieldGenerator.generateFields(definition.getJSONObject("properties")));
    }

    static class UnknownModelTypeException extends RuntimeException {
        UnknownModelTypeException(String message) {
            super(message);
        }
    }

    private static class FieldGenerator {
        static Iterable<FieldSpec> generateFields(JSONObject properties) {
            return properties.keySet().stream()
                    .map(k -> generateField(k, properties.getJSONObject(k)))
                    .collect(Collectors.toList());
        }

        static FieldSpec generateField(String name, JSONObject definition) {
            String type = definition.getString("type");

            FieldSpec.Builder builder;
            if ("integer".equals(type)) {
                builder = generateIntegerField(name, definition);
            } else if ("string".equals(type)) {
                builder = generateStringField(name, definition);
            } else if ("boolean".equals(type)) {
                builder = generateBooleanField(name, definition);
            } else if ("object".equals(type)) {
                builder = generateObjectField(name, definition);
            } else {
                throw new UnknownFieldTypeException(type);
            }

            if (definition.has("description")) {
                String description = definition.getString("description");

                builder.addJavadoc(description);
            }


            return builder.build();
        }

        private static FieldSpec.Builder generateIntegerField(String name, JSONObject definition) {
            String format = definition.getString("format");

            if ("int32".equals(format)) {
                return FieldSpec.builder(Integer.class, name, Modifier.PRIVATE);
            } else if ("int64".equals(format)) {
                return FieldSpec.builder(Long.class, name, Modifier.PRIVATE);
            } else {
                throw new UnknownIntegerFieldFormatException(format);
            }
        }

        private static FieldSpec.Builder generateBooleanField(String name, JSONObject definition) {
            return FieldSpec.builder(Boolean.class, name, Modifier.PRIVATE);
        }

        private static FieldSpec.Builder generateStringField(String name, JSONObject definition) {
            if (!definition.has("format")) {
                return FieldSpec.builder(String.class, name, Modifier.PRIVATE);
            } else {
                String format = definition.getString("format");

                if ("date-time".equals(format)) {
                    return FieldSpec.builder(Date.class, name, Modifier.PRIVATE)
                            .addAnnotation(AnnotationSpec.builder(JsonFormat.class)
                                    .addMember("shape", CodeBlock.builder().add("JsonFormat.Shape.STRING").build())
                                    .addMember("pattern", "$S", "dd-MM-yyyy hh:mm:ss")
                                    .build());
                } else {
                    throw new UnknownStringFieldFormatException(format);
                }
            }
        }

        private static FieldSpec.Builder generateObjectField(String name, JSONObject definition) {
            return FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Object.class), name, Modifier.PRIVATE);
        }

        static class UnknownFieldTypeException extends RuntimeException {
            UnknownFieldTypeException(String message) {
                super(message);
            }
        }

        static class UnknownIntegerFieldFormatException extends RuntimeException {
            UnknownIntegerFieldFormatException(String message) {
                super(message);
            }
        }

        static class UnknownStringFieldFormatException extends RuntimeException {
            UnknownStringFieldFormatException(String message) {
                super(message);
            }
        }
    }
}
