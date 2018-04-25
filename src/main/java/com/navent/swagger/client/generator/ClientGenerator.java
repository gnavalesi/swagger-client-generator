package com.navent.swagger.client.generator;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ClientGenerator {

    // TODO: Argumentos

    private static final String basePackage = "com.navent.jobs.applicant";
    private static final String serviceName = "ApplicantService";
    private static final String outputPath = "/home/guido/navent/swagger-client-generator/out";

    public static void main(String[] args) throws Exception {
        String definitionPath = args[0];

        URL url = Resources.getResource(definitionPath);
        String text = Resources.toString(url, Charsets.UTF_8);

        JSONObject definition = new JSONObject(text);

        ContextGenerator.generate(basePackage, serviceName, outputPath, definition);
        ModelGenerator.generate(basePackage, serviceName, outputPath, definition);
        PathsGenerator.generate(basePackage, serviceName, outputPath, definition);


//        TypeSpec rootPath = TypeSpec.classBuilder("Root")
//                .addTypes(PathsGenerator.generatePaths(definition.getJSONObject("paths")))
//                .build();
//
//        JavaFile.builder(basePackage + ".client.controllers", rootPath)
//                .build()
//                .writeTo(new File("/home/guido/navent/swagger-client-generator/out"));
    }


}
