package com.skyflow;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.ArrayList;
import java.util.Comparator;

public class GenMarkdown {
    public static void generateMarkdown(List<Map<String, Object>> packages, String outputPath) {
        StringBuilder overviewSb = new StringBuilder();
        StringBuilder classSb = new StringBuilder();
        StringBuilder interfaceSb = new StringBuilder();
        StringBuilder enumSb = new StringBuilder();
        for (Map<String, Object> pkg : packages) {
            List<Map<String, Object>> classes = (List<Map<String, Object>>) pkg.get("classes");
            for (Map<String, Object> cls : classes) {
                StringBuilder sb = new StringBuilder();

                sb.append("{% env enable=\"javaSdkRef\" %}\n\n");
                appendClass(sb, cls);
                sb.append("{% /env %}");
                String className = (String) cls.get("name");
                String classType = (String) cls.get("classType");
                String type = classType.equals("CLASS") ? "classes" : classType.equals("ENUM") ? "enums" : "interfaces";

                if(type.equals("classes")) {
                    classSb.append("- [" + className + "](/sdks/skyflow-java/" + type + "/" + className + ")\n");
                }
                else if(type.equals("enums")) {
                    enumSb.append("- [" + className + "](/sdks/skyflow-java/" + type + "/" + className + ")\n");
                }
                else {
                    interfaceSb.append("- [" + className + "](/sdks/skyflow-java/" + type + "/" + className + ")\n");
                }

                String filePath = outputPath + type + "/" + className + ".md";
                File file = new File(filePath);
                File parentDir = file.getParentFile();

                if (!parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        System.out.println("Failed to create the directory: " + parentDir.getAbsolutePath());
                        return; // or handle the error in an appropriate way
                    }
                }
                try {
                    FileWriter writer = new FileWriter(filePath);
                    writer.write(sb.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
            }
        }
        overviewSb.append("{% env enable=\"javaSdkRef\" %}\n\n");
        overviewSb.append("# Java\n\n");
        overviewSb.append("Some documentation for overview page\n\n");
        overviewSb.append("## Classes\n\n");
        overviewSb.append(sortValues(classSb)).append("\n");
        overviewSb.append("## Enums\n\n");
        overviewSb.append(sortValues(enumSb)).append("\n");
        overviewSb.append("## Interfaces\n\n");
        overviewSb.append(sortValues(interfaceSb)).append("\n");
        overviewSb.append("{% /env %}");
        try {
            FileWriter overviewWriter = new FileWriter("docs/markdown/Overview.md");
            overviewWriter.write(overviewSb.toString());
            overviewWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String sortValues(StringBuilder sb) {
        // Split the string builder by newlines
        String[] lines = sb.toString().split("\n");

        // Extract the link names
        List<String> linkNames = new ArrayList<>();
        for (String line : lines) {
            int startIndex = line.indexOf("[") + 1;
            int endIndex = line.indexOf("]");
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String linkName = line.substring(startIndex, endIndex);
                linkNames.add(linkName);
            }
        }

        // Sort the link names
        linkNames.sort(Comparator.naturalOrder());

        // Reconstruct the sorted links
        StringBuilder sortedStringBuilder = new StringBuilder();
        for (String linkName : linkNames) {
            for (String line : lines) {
                int startIndex = line.indexOf("[") + 1;
                int endIndex = line.indexOf("]");
                if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                    String name = line.substring(startIndex, endIndex);
                    if (linkName.equals(name)) {
                        sortedStringBuilder.append(line).append("\n");
                        break;
                    }
                }
            }
        }
        return sortedStringBuilder.toString();
    }

    public static void appendClass(StringBuilder sb, Map<String, Object> cls) {
        String className = (String) cls.get("name");
        String summary = (String) cls.get("summary");

        // sb.append("## Class: ").append(className).append("\n\n");
        sb.append("# ").append(className).append("\n\n");
        sb.append(summary).append("\n\n");

        appendConstructors(sb, cls);
        appendMethods(sb, cls);
        appendEnumValues(sb, cls);
    }

    public static void appendConstructors(StringBuilder sb, Map<String, Object> cls) {
        String className = (String) cls.get("name");
        List<Map<String, Object>> constructors = (List<Map<String, Object>>) cls.get("constructors");
        if (!constructors.isEmpty()) {
            sb.append("## Constructors").append("\n\n");

            for (Map<String, Object> constructor : constructors) {
                String signature = (String) constructor.get("signature");
                sb.append("```java\n").append(signature.replace("<init>", className)).append("\n```\n\n");
                List<Map<String, Object>> parameters = (List<Map<String, Object>>) constructor.get("parameters");
                if (!parameters.isEmpty()) {
                    sb.append("#### Parameters").append("\n\n");
                    sb.append("| Name | Type | Description |\n| --- | --- | --- |\n");
                    for (Map<String, Object> parameter : parameters) {
                        appendParameter(sb, parameter);
                    }

                    sb.append("\n");
                }
            }
        }
    }

    public static void appendMethods(StringBuilder sb, Map<String, Object> cls) {
        List<Map<String, Object>> methods = (List<Map<String, Object>>) cls.get("methods");
        if (!methods.isEmpty()) {
            sb.append("## Methods").append("\n\n");

            for (Map<String, Object> method : methods) {
                appendMethod(sb, method);
            }

            // sb.append("\n");
        }
    }

    public static void appendEnumValues(StringBuilder sb, Map<String, Object> cls) {
        List<String> enumValues = (List<String>) cls.get("enumValues");
        if (!enumValues.isEmpty()) {
            sb.append("## Enum Values").append("\n\n");
            sb.append("| Value |\n| --- |\n");
            for (String enumValue : enumValues) {
                sb.append("| ").append(enumValue).append(" |\n");
            }

            sb.append("\n");
        }
    }

    public static void appendMethod(StringBuilder sb, Map<String, Object> method) {
        String methodName = (String) method.get("name");
        String signature = (String) method.get("signature");
        String description = (String) method.get("description");

        sb.append("### ").append(methodName).append("\n\n");
        sb.append(description).append("\n\n");
        sb.append("```java\n").append(signature).append("\n```\n\n");

        List<Map<String, Object>> parameters = (List<Map<String, Object>>) method.get("parameters");
        if (!parameters.isEmpty()) {
            sb.append("#### Parameters").append("\n\n");
            sb.append("| Name | Type | Description |\n| --- | --- | --- |\n");
            for (Map<String, Object> parameter : parameters) {
                appendParameter(sb, parameter);
            }

            sb.append("\n");
        }

        List<Map<String, Object>> exceptions = (List<Map<String, Object>>) method.get("exceptions");
        if (!exceptions.isEmpty()) {
            sb.append("#### Exception").append("\n\n");

            for (Map<String, Object> exception : exceptions) {
                appendException(sb, exception);
            }

            sb.append("\n");
        }

        Map<String, Object> returns = (Map<String, Object>) method.get("returns");
        if (returns != null) {
            appendReturn(sb, returns);
        }

        sb.append("\n");
    }

    public static void appendParameter(StringBuilder sb, Map<String, Object> parameter) {
        String paramName = (String) parameter.get("name");
        String paramType = (String) parameter.get("type");
        String description = (String) parameter.get("description");

        sb.append("| ").append(paramName).append(" | ").append(paramType).append(" | ").append(description).append(" |\n");
    }

    public static void appendException(StringBuilder sb, Map<String, Object> exception) {
        String exceptionType = (String) exception.get("type");
        String description = (String) exception.get("description");

        sb.append("**").append(exceptionType).append("**: ").append(description).append("\n");
    }

    public static void appendReturn(StringBuilder sb, Map<String, Object> returns) {
        String returnType = (String) returns.get("type");
        String description = (String) returns.get("description");

        if(!returnType.equals("void"))
        {
            sb.append("#### Returns").append("\n");
            sb.append(returnType).append("\n\n");
            if(description != "")
            {
                sb.append(description).append("\n");
            }
        }
    }

    public static void main(String[] args) {
        String filePath = "target/site/apidocs/generated-files/output.json"; // Replace with the actual path to the JSON file
        String outputPath = "docs/markdown/"; // Replace with the desired output directory

        try (Reader reader = new FileReader(filePath)) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonArray = (JSONObject) jsonParser.parse(reader);
            List<Map<String, Object>> packages = (List<Map<String, Object>>) jsonArray.get("packages");

            generateMarkdown(packages, outputPath);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}

