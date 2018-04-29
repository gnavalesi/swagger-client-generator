package com.navent.swagger.client.generator;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.Data;
import org.json.JSONObject;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.lang.model.element.Modifier;
import java.io.File;

class ContextGenerator {

    public static void generate(String basePackage, String serviceName, String outputPath, JSONObject definition) throws Exception {
        generateRestConfig(basePackage, serviceName, outputPath, definition);
        generateConfig(basePackage, serviceName, outputPath);
    }

    private static void generateRestConfig(String basePackage, String serviceName, String outputPath, JSONObject definition) throws Exception {
        JavaFile.builder(basePackage + ".client.config",
                TypeSpec.classBuilder(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, serviceName) + "ClientRestConfig")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Data.class)
                        .addAnnotation(AnnotationSpec.builder(PropertySource.class)
                                .addMember("prefix", "$S", serviceName)
                                .build())
                        .addField(FieldSpec.builder(String.class, "host", Modifier.PRIVATE)
                                .initializer("$S", definition.get("host"))
                                .build())
                        .addField(FieldSpec.builder(String.class, "basePath", Modifier.PRIVATE)
                                .initializer("$S", definition.get("basePath"))
                                .build())
                        .build())
                .build()
                .writeTo(new File(outputPath));
    }

    private static void generateConfig(String basePackage, String serviceName, String outputPath) throws Exception {
        JavaFile.builder(basePackage + ".client.config",
                TypeSpec.classBuilder(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, serviceName) + "ClientConfig")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Configuration.class)
                        .addAnnotation(AnnotationSpec.builder(ComponentScan.class)
                                .addMember("value", "$S", basePackage)
                                .build())
                        .build())
                .build()
                .writeTo(new File(outputPath));
    }
}
