package com.navent.swagger.client.generator;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ClientGenerator {

    // TODO: Argumentos

    private static final String basePackage = "com.navent.jobs.applicantservice";
    private static final String serviceName = "applicant-service";

    private static final String group = "com.navent.jobs";
    private static final String version = "1.0.0-SNAPSHOT";

    private static final String outputPath = "/home/guido/navent/swagger-client-generator/";

    public static void main(String[] args) throws Exception {
        URL url = Resources.getResource(args[0]);
        String text = Resources.toString(url, Charsets.UTF_8);

        JSONObject definition = new JSONObject(text);

        ContextGenerator.generate(basePackage, serviceName, outputPath + serviceName + "-client/src/main/java", definition);
        ModelGenerator.generate(basePackage, serviceName, outputPath + serviceName + "-client/src/main/java", definition);
        PathsGenerator.generate(basePackage, serviceName, outputPath + serviceName + "-client/src/main/java", definition);

        URL gradleBuildTemplateUrl = Resources.getResource("build.gradle.j2");
        String gradleBuildTemplateText = Resources.toString(gradleBuildTemplateUrl, Charsets.UTF_8);

        Map<String, String> jinjaParameters =new HashMap<>();
        jinjaParameters.put("group", group);
        jinjaParameters.put("version", version);

        Jinjava jinjava = new Jinjava();
        String gradleBuildText = jinjava.render(gradleBuildTemplateText, jinjaParameters);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + serviceName + "-client/build.gradle"));
        writer.write(gradleBuildText);
        writer.close();
    }


}
