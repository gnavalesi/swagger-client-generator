package com.navent.swagger.client.generator;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Streams;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PathsGenerator {

    private static final String PATH_PARAMETER_REGEX = "\\{[^\\}]+}";
    private final String basePackage;
    private final String serviceName;
    private final String outputPath;

    public PathsGenerator(String basePackage, String serviceName, String outputPath) {
        this.basePackage = basePackage;
        this.serviceName = serviceName;
        this.outputPath = outputPath;
    }

    public static void generate(String basePackage, String serviceName, String outputPath, JSONObject definition) {
        new PathsGenerator(basePackage, serviceName, outputPath).generatePaths(definition.getJSONObject("paths")).forEach(m -> {
            try {
                JavaFile.builder(basePackage + ".client.controller", m)
                        .build()
                        .writeTo(new File(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String getControllerName(Pair<Pair<String, String>, JSONObject> pair) {
        JSONArray tags = pair.getRight().getJSONArray("tags");
        return IntStream.range(0, tags.length())
                .mapToObj(tags::getString)
                .filter(t -> t.endsWith("controller"))
                .map(t -> t.substring(0, t.lastIndexOf("-")))
                .map(t -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, t.replaceAll("-", "_")))
                .findFirst()
                .get();
    }

    private static <T> List<T> createList(T t) {
        List<T> list = new ArrayList<>();
        list.add(t);
        return list;
    }

    private static <T> List<T> mergeLists(List<T> a, List<T> b) {
        List<T> list = new ArrayList<>();
        list.addAll(a);
        list.addAll(b);
        return list;
    }

    private static List<String> parsePath(String path) {
        StringBuilder builder = new StringBuilder("root");
        if (!path.startsWith("/")) builder.append("/");
        if (path.endsWith("/")) {
            builder.append(path.substring(0, path.length() - 1));
        } else {
            builder.append(path);
        }

        return Arrays.asList(builder.toString().split("/"));
    }

    private static TypeSpec generateFromPathsNode(PathsNode pn) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(pn.partialPath);

        Iterable<TypeSpec> innerClasses = pn.nodes.keySet().stream()
                .map(k -> pn.nodes.get(k))
                .filter(n -> !(n instanceof ParameterizedPathsNode))
                .map(PathsGenerator::generateFromPathsNode)
                .collect(Collectors.toList());

        builder.addTypes(innerClasses);

        builder.addMethods(Streams.stream(innerClasses)
                .map(k -> MethodSpec.methodBuilder(k.name)
                        .returns(ClassName.get("", k.name))
                        .addStatement("return new $T()", ClassName.get("", k.name))
                        .build())
                .collect(Collectors.toList()));

        return builder.build();
    }

    private static List<String> splitPath(String path) {
        return Stream.of(path.split("/"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    Iterable<TypeSpec> generatePaths(JSONObject paths) {
//        Map<String, List<String>> pathsListMap = paths.keySet().stream()
//                .collect(Collectors.toMap(Function.identity(), PathsGenerator::splitPath));
//
//        Map<String, Map<String, List<String>>> res = pathsListMap.entrySet().stream()
//                .reduce(new HashMap<>(),
//                        (Map<String, Map<String, List<String>>> acc, Map.Entry<String, List<String>> e) -> {
//                            Map<String, List<String>> item = acc.computeIfAbsent(e.getValue().get(0), k -> new HashMap<>());
//                            item.put(e.getKey(), e.getValue().subList(1, e.getValue().size()));
//
//                            return acc;
//                        }, (a, b) -> a);

        // paths.keySet().stre
        return paths.keySet().stream()
                .map(path -> Pair.of(path, paths.getJSONObject(path)))
                .flatMap(pair -> pair.getRight().keySet().stream()
                        .map(method -> Pair.of(Pair.of(pair.getLeft(), method), pair.getRight().getJSONObject(method))))
                .collect(Collectors.toMap(PathsGenerator::getControllerName, PathsGenerator::createList, PathsGenerator::mergeLists))
                .entrySet().stream()
                .map(this::createController)
                .collect(Collectors.toList());
    }

    private TypeSpec createController(Map.Entry<String, List<Pair<Pair<String, String>, JSONObject>>> definition) {
        return TypeSpec.classBuilder(definition.getKey() + "Controller")
                .addAnnotation(Component.class)
                .addMethods(definition.getValue().stream()
                        .flatMap(this::createMethods)
                        .collect(Collectors.toList()))
                .build();
    }

    private Stream<MethodSpec> createMethods(Pair<Pair<String, String>, JSONObject> params) {
        List<MethodSpec> methods = new ArrayList<>();

        String path = params.getLeft().getLeft();
        String httpMethod = params.getLeft().getRight();
        JSONObject definition = params.getRight();

        String methodName = definition.getString("operationId").replace("Using" + httpMethod.toUpperCase(), "");

        methods.add(MethodSpec.methodBuilder(methodName)
                .returns(
                        definition.getJSONObject("responses").keySet().stream()
                                .map(k -> definition.getJSONObject("responses").getJSONObject(k))
                                .filter(d -> d.has("schema"))
                                .map(d -> d.getJSONObject("schema"))
                                .map(d -> {
                                    if (d.has("$ref")) {
                                        return ClassName.get(basePackage + ".model", d.getString("$ref").replace("#/definitions/", ""));
                                    } else {
                                        return ClassName.get(Void.class);
                                    }
                                })
                                .findFirst().orElse(ClassName.get(Void.class)))
                .build());

        return methods.stream();
    }

    private static class PathsNode {
        String partialPath;
        Map<String, PathsNode> nodes = new HashMap<>();
        Map<String, JSONObject> methods = new HashMap<>();

        static PathsNode fromDefinition(String path, JSONObject definition) {
            return fromDefinition(splitPath(path), definition);
        }

        static PathsNode merge(PathsNode a, PathsNode b) {
            PathsNode result = new PathsNode();
            mergeInto(a, b, result);
            return result;
        }

        private static PathsNode fromDefinition(List<String> path, JSONObject definition) {
            PathsNode result;

            if (path.get(0).matches("\\{[^\\}]+}")) {
                result = new PathsNode();
                result.partialPath = path.get(0).replaceAll("(\\{|\\})", "");
                if (path.size() > 1) {
                    result.nodes.put(path.get(1), PathsNode.fromDefinition(path.subList(1, path.size()), definition));
                }
            } else if (path.size() > 1 && path.get(1).matches("\\{[^\\}]+}")) {
                result = generateParameterized(path, definition);
                result.nodes.put(path.get(1).replaceAll("(\\{|\\})", ""), PathsNode.fromDefinition(path.subList(1, path.size()), definition));
            } else {
                result = new PathsNode();
                result.partialPath = path.get(0);
                if (path.size() > 1) {
                    result.nodes.put(path.get(1), PathsNode.fromDefinition(path.subList(1, path.size()), definition));
                }

            }

            if (path.size() == 1) {
                result.methods = definition.keySet().stream()
                        .collect(Collectors.toMap(a -> a, definition::getJSONObject));
            }

            return result;
        }

        private static ParameterizedPathsNode<?> generateParameterized(List<String> path, JSONObject definition) {
            String parameterName = path.get(1).replaceAll("(\\{|\\})", "");

            String firstMethod = definition.keys().next();
            JSONObject methodDefinition = definition.getJSONObject(firstMethod);
            JSONArray parameters = methodDefinition.getJSONArray("parameters");

            Optional<JSONObject> def = IntStream.range(0, parameters.length())
                    .mapToObj(i -> parameters.getJSONObject(i))
                    .filter((JSONObject j) -> j.has("name") && j.getString("name").equals(parameterName))
                    .findFirst();

            if (def.isPresent()) {
                JSONObject obj = def.get();
                String type = obj.getString("type");
                if ("integer".equals(type)) {
                    String format = obj.getString("format");
                    if ("int32".equals(format)) {
                        ParameterizedPathsNode<Integer> res = new ParameterizedPathsNode<>();
                        res.theClass = Integer.class;
                        res.partialPath = parameterName;
                        return res;
                    } else if ("int64".equals(format)) {
                        ParameterizedPathsNode<Long> res = new ParameterizedPathsNode<>();
                        res.theClass = Long.class;
                        res.partialPath = parameterName;
                        return res;
                    } else {
                        throw new RuntimeException("nop");
                    }
                }
            }

            throw new RuntimeException("nop2");
        }


        protected static void mergeInto(PathsNode a, PathsNode b, PathsNode target) {
            target.partialPath = a.partialPath;

            target.nodes.putAll(a.nodes);
            b.nodes.forEach((k, v) -> {
                PathsNode n = v;

                if (target.nodes.containsKey(k)) {
                    n = PathsNode.merge(v, target.nodes.get(k));
                }

                target.nodes.put(k, n);
            });

            target.methods.putAll(a.methods);
            target.methods.putAll(b.methods);
        }


    }

    private static class ParameterizedPathsNode<T> extends PathsNode {
        Class<T> theClass;

        static <T> ParameterizedPathsNode merge(ParameterizedPathsNode<T> a, ParameterizedPathsNode<T> b) {
            ParameterizedPathsNode<T> result = new ParameterizedPathsNode<>();
            result.theClass = a.theClass;
            mergeInto(a, b, result);
            return result;
        }
    }
}
