package com.example.mcp.handler.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.mcp.handler.BaseHandler;
import com.example.mcp.util.LogUtil;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

/**
 * MCP 结构化数据处理工具实现，提供 JSON/XML/YAML/CSV/Properties 等格式的解析、校验、查询和互转功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class JsonDataHandler extends BaseHandler {

    // --- 1. json_format ---

    /**
     * 对 JSON 字符串进行格式化美化，支持自定义缩进空格数。
     *
     * @param jsonContent 要格式化的 JSON 字符串
     * @param indent      缩进空格数，默认 2
     * @return 格式化后的 JSON 字符串
     */
    @Tool(name = "json_format", description = "对 JSON 字符串进行格式化美化，支持自定义缩进空格数。")
    public String jsonFormat(@ToolParam(description = "要格式化的 JSON 字符串") String jsonContent,
        @ToolParam(description = "缩进空格数，默认 2", required = false) Integer indent) {
        return execute("jsonFormat", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "错误: JSON 内容不能为空";
            }
            int space = (indent != null && indent >= 0) ? indent : 2;
            Object parsed = JSON.parse(jsonContent.trim());
            String result = JSON.toJSONString(parsed, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat,
                                              com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue);
            if (space != 2) {
                result = result.replace("  ", " ".repeat(Math.max(1, space)));
            }
            LogUtil.info("jsonFormat 完成: 长度={}", jsonContent.length());
            return result;
        });
    }

    // --- 2. json_validate ---

    /**
     * 校验 JSON 字符串格式是否合法。
     *
     * @param jsonContent 要校验的 JSON 字符串
     * @return 校验结果消息
     */
    @Tool(name = "json_validate", description = "校验 JSON 字符串格式是否合法。")
    public String jsonValidate(@ToolParam(description = "要校验的 JSON 字符串") String jsonContent) {
        return execute("jsonValidate", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "错误: JSON 内容不能为空";
            }
            Object parsed = JSON.parse(jsonContent.trim());
            String type = (parsed instanceof JSONArray) ? "数组" : "对象";
            LogUtil.info("jsonValidate 完成: 合法, 类型={}", type);
            return "JSON 格式合法，顶层类型为: " + type;
        });
    }

    // --- 3. json_query ---

    /**
     * 使用简化的 JSONPath 表达式查询 JSON 数据。
     * 支持点号路径和数组索引，如 "data.items[0].name"。
     *
     * @param jsonContent JSON 字符串
     * @param jsonPath    JSONPath 查询表达式（如 "data.items[0].name"）
     * @return JSON 格式的查询结果
     */
    @Tool(name = "json_query", description = "使用 JSONPath 表达式查询 JSON 数据，支持点号路径和数组索引。")
    public String jsonQuery(@ToolParam(description = "JSON 字符串") String jsonContent,
        @ToolParam(description = "JSONPath 查询表达式，如 data.items[0].name") String jsonPath) {
        return execute("jsonQuery", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "{\"error\": \"JSON 内容不能为空\"}";
            }
            if (jsonPath == null || jsonPath.isBlank()) {
                return "{\"error\": \"JSONPath 不能为空\"}";
            }
            Object root = JSON.parse(jsonContent.trim());
            Object result = resolveJsonPath(root, jsonPath.trim());
            if (result == null) {
                Map<String, Object> nullResult = new LinkedHashMap<>();
                nullResult.put("result", null);
                nullResult.put("path", jsonPath);
                return JSON.toJSONString(nullResult);
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("path", jsonPath);
            map.put("result", (result instanceof String) ? result : JSON.toJSONString(result));
            LogUtil.info("jsonQuery 完成: 路径={}", jsonPath);
            return JSON.toJSONString(map);
        });
    }

    @SuppressWarnings("unchecked")
    private Object resolveJsonPath(Object current, String path) {
        if (current == null || path.isEmpty()) {
            return current;
        }
        // handle array index: items[0]
        String segment = path;
        String remaining = "";
        int dotIdx = path.indexOf('.');
        int bracketIdx = path.indexOf('[');
        if (bracketIdx >= 0 && (dotIdx < 0 || bracketIdx < dotIdx)) {
            segment = path.substring(0, bracketIdx);
            int endBracket = path.indexOf(']', bracketIdx);
            if (endBracket < 0) {
                throw new IllegalArgumentException("无效的数组索引表达式: " + path);
            }
            segment = path.substring(0, bracketIdx);
            String indexStr = path.substring(bracketIdx + 1, endBracket);
            remaining = path.substring(endBracket + 1);
            if (remaining.startsWith(".")) {
                remaining = remaining.substring(1);
            }

            if (!segment.isEmpty()) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(segment);
                } else {
                    return null;
                }
            }
            if (current instanceof List) {
                int idx = Integer.parseInt(indexStr);
                List<Object> list = (List<Object>) current;
                if (idx < 0 || idx >= list.size()) {
                    return null;
                }
                current = list.get(idx);
            } else if (current instanceof JSONArray) {
                int idx = Integer.parseInt(indexStr);
                JSONArray arr = (JSONArray) current;
                if (idx < 0 || idx >= arr.size()) {
                    return null;
                }
                current = arr.get(idx);
            }
            return resolveJsonPath(current, remaining);
        } else if (dotIdx >= 0) {
            segment = path.substring(0, dotIdx);
            remaining = path.substring(dotIdx + 1);
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(segment);
            } else if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(segment);
            }
            return resolveJsonPath(current, remaining);
        } else {
            if (current instanceof Map) {
                return ((Map<String, Object>) current).get(path);
            } else if (current instanceof JSONObject) {
                return ((JSONObject) current).get(path);
            }
            return current;
        }
    }

    // --- 4. json_to_csv ---

    /**
     * 将 JSON 数组转换为 CSV 格式。
     *
     * @param jsonContent    JSON 数组字符串（如 [{"name":"张三","age":25}]）
     * @param outputFilePath 输出 CSV 文件路径（可选，不指定则返回 CSV 文本内容）
     * @param delimiter      分隔符，默认为逗号
     * @return CSV 内容或保存成功消息
     */
    @Tool(name = "json_to_csv", description = "将 JSON 数组转换为 CSV 格式，可指定输出文件路径。")
    public String jsonToCsv(@ToolParam(description = "JSON 数组字符串") String jsonContent,
        @ToolParam(description = "输出 CSV 文件路径（可选，不指定则返回 CSV 文本）", required = false) String outputFilePath,
        @ToolParam(description = "分隔符，默认为逗号", required = false) String delimiter) {
        return execute("jsonToCsv", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "错误: JSON 内容不能为空";
            }
            String delim = (delimiter != null && !delimiter.isBlank()) ? delimiter : ",";
            Object parsed = JSON.parse(jsonContent.trim());
            List<Map<String, Object>> rows = new ArrayList<>();

            if (parsed instanceof JSONArray arr) {
                for (int i = 0; i < arr.size(); i++) {
                    Object item = arr.get(i);
                    if (item instanceof JSONObject jo) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String key : jo.keySet()) {
                            row.put(key, jo.get(key));
                        }
                        rows.add(row);
                    }
                }
            } else if (parsed instanceof JSONObject jo) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String key : jo.keySet()) {
                    row.put(key, jo.get(key));
                }
                rows.add(row);
            } else {
                return "错误: JSON 数据必须是对象或对象数组";
            }

            if (rows.isEmpty()) {
                return "错误: JSON 数组为空";
            }

            // get all headers
            List<String> headers = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                for (String key : row.keySet()) {
                    if (!headers.contains(key)) {
                        headers.add(key);
                    }
                }
            }

            StringBuilder csv = new StringBuilder();
            csv.append(String.join(delim, headers))
               .append("\n");
            for (Map<String, Object> row : rows) {
                List<String> values = new ArrayList<>();
                for (String h : headers) {
                    Object v = row.get(h);
                    String val = v == null ? "" : String.valueOf(v);
                    if (val.contains(delim) || val.contains("\"") || val.contains("\n")) {
                        val = "\"" + val.replace("\"", "\"\"") + "\"";
                    }
                    values.add(val);
                }
                csv.append(String.join(delim, values))
                   .append("\n");
            }

            if (outputFilePath != null && !outputFilePath.isBlank()) {
                Files.writeString(Path.of(outputFilePath), csv.toString());
                LogUtil.info("jsonToCsv 完成: 已保存到 {}", outputFilePath);
                return "CSV 文件已保存到: " + outputFilePath + "，共 " + (csv.toString()
                                                                            .split("\\n").length - 1) + " 行数据";
            }

            LogUtil.info("jsonToCsv 完成: {}行", rows.size());
            return csv.toString();
        });
    }

    // --- 5. json_to_yaml ---

    /**
     * 将 JSON 字符串转换为 YAML 格式。
     *
     * @param jsonContent JSON 字符串
     * @return YAML 格式的文本
     */
    @Tool(name = "json_to_yaml", description = "将 JSON 字符串转换为 YAML 格式。")
    public String jsonToYaml(@ToolParam(description = "JSON 字符串") String jsonContent) {
        return execute("jsonToYaml", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "错误: JSON 内容不能为空";
            }
            Object obj = JSON.parse(jsonContent.trim());
            // Convert to Map/List first if it's a JSONObject/JSONArray
            if (obj instanceof JSONObject || obj instanceof JSONArray) {
                obj = JSON.parse(jsonContent.trim());
            }
            Yaml yaml = new Yaml();
            String result = yaml.dumpAsMap(obj instanceof Map ? obj : Map.of("root", obj));
            // Remove !! wrapper if present
            if (result.startsWith("!!")) {
                result = result.substring(result.indexOf('\n') + 1);
            }
            LogUtil.info("jsonToYaml 完成");
            return result;
        });
    }

    // --- 6. yaml_to_json ---

    /**
     * 将 YAML 字符串转换为 JSON 格式。
     *
     * @param yamlContent YAML 字符串
     * @return JSON 字符串
     */
    @Tool(name = "yaml_to_json", description = "将 YAML 字符串转换为 JSON 格式。")
    public String yamlToJson(@ToolParam(description = "YAML 字符串") String yamlContent) {
        return execute("yamlToJson", () -> {
            if (yamlContent == null || yamlContent.isBlank()) {
                return "错误: YAML 内容不能为空";
            }
            Yaml yaml = new Yaml();
            Object obj = yaml.load(yamlContent.trim());
            String result = JSON.toJSONString(obj);
            LogUtil.info("yamlToJson 完成");
            return result;
        });
    }

    // --- 7. json_to_xml ---

    /**
     * 将 JSON 字符串转换为 XML 格式。
     *
     * @param jsonContent JSON 字符串
     * @param rootName    根元素名称，默认 "root"
     * @return XML 格式的文本
     */
    @Tool(name = "json_to_xml", description = "将 JSON 字符串转换为 XML 格式。")
    public String jsonToXml(@ToolParam(description = "JSON 字符串") String jsonContent,
        @ToolParam(description = "根元素名称，默认 root", required = false) String rootName) {
        return execute("jsonToXml", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "错误: JSON 内容不能为空";
            }
            String root = (rootName != null && !rootName.isBlank()) ? rootName : "root";
            Object obj = JSON.parse(jsonContent.trim());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element rootElement = doc.createElement(root);
            doc.appendChild(rootElement);

            if (obj instanceof JSONObject jo) {
                for (String key : jo.keySet()) {
                    appendXmlElement(doc, rootElement, key, jo.get(key));
                }
            } else if (obj instanceof JSONArray ja) {
                for (int i = 0; i < ja.size(); i++) {
                    Element item = doc.createElement("item");
                    appendXmlElement(doc, item, "value", ja.get(i));
                    rootElement.appendChild(item);
                }
            }

            Transformer transformer = TransformerFactory.newInstance()
                                                        .newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            LogUtil.info("jsonToXml 完成");
            return writer.toString();
        });
    }

    @SuppressWarnings("unchecked")
    private void appendXmlElement(Document doc, Element parent, String name, Object value) {
        if (value instanceof JSONObject jo) {
            Element child = doc.createElement(name);
            for (String key : jo.keySet()) {
                appendXmlElement(doc, child, key, jo.get(key));
            }
            parent.appendChild(child);
        } else if (value instanceof JSONArray ja) {
            for (int i = 0; i < ja.size(); i++) {
                Element item = doc.createElement(name);
                Object itemVal = ja.get(i);
                if (itemVal instanceof JSONObject) {
                    for (String key : ((JSONObject) itemVal).keySet()) {
                        appendXmlElement(doc, item, key, ((JSONObject) itemVal).get(key));
                    }
                } else {
                    item.setTextContent(String.valueOf(itemVal));
                }
                parent.appendChild(item);
            }
        } else if (value instanceof Map) {
            Element child = doc.createElement(name);
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                appendXmlElement(doc, child, entry.getKey(), entry.getValue());
            }
            parent.appendChild(child);
        } else {
            Element child = doc.createElement(name);
            child.setTextContent(value == null ? "" : String.valueOf(value));
            parent.appendChild(child);
        }
    }

    // --- 8. xml_to_json ---

    /**
     * 将 XML 字符串转换为 JSON 格式。
     *
     * @param xmlContent XML 字符串
     * @return JSON 字符串
     */
    @Tool(name = "xml_to_json", description = "将 XML 字符串转换为 JSON 格式。")
    public String xmlToJson(@ToolParam(description = "XML 字符串") String xmlContent) {
        return execute("xmlToJson", () -> {
            if (xmlContent == null || xmlContent.isBlank()) {
                return "错误: XML 内容不能为空";
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlContent.trim()
                                                                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            JSONObject result = xmlElementToJson(doc.getDocumentElement());
            LogUtil.info("xmlToJson 完成");
            return JSON.toJSONString(result);
        });
    }

    private JSONObject xmlElementToJson(Element element) {
        JSONObject jo = new JSONObject();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String tagName = childElement.getTagName();
                if (childElement.getChildNodes()
                                .getLength() == 1 && childElement.getFirstChild()
                                                                 .getNodeType() == Node.TEXT_NODE) {
                    jo.put(tagName, childElement.getTextContent());
                } else {
                    jo.put(tagName, xmlElementToJson(childElement));
                }
            }
        }
        return jo;
    }

    // --- 9. xml_validate ---

    /**
     * 校验 XML 字符串格式是否合法。
     *
     * @param xmlContent 要校验的 XML 字符串
     * @return 校验结果消息
     */
    @Tool(name = "xml_validate", description = "校验 XML 字符串格式是否合法。")
    public String xmlValidate(@ToolParam(description = "要校验的 XML 字符串") String xmlContent) {
        return execute("xmlValidate", () -> {
            if (xmlContent == null || xmlContent.isBlank()) {
                return "错误: XML 内容不能为空";
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new java.io.ByteArrayInputStream(xmlContent.trim()
                                                                     .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            LogUtil.info("xmlValidate 完成: 合法");
            return "XML 格式合法";
        });
    }

    // --- 10. json_to_properties ---

    /**
     * 将 JSON 对象转换为 Java Properties 格式（扁平化键值对）。
     *
     * @param jsonContent JSON 对象字符串
     * @return Properties 格式的文本
     */
    @Tool(name = "json_to_properties", description = "将 JSON 对象转换为 Java Properties 格式（扁平化键值对）。")
    public String jsonToProperties(@ToolParam(description = "JSON 对象字符串") String jsonContent) {
        return execute("jsonToProperties", () -> {
            if (jsonContent == null || jsonContent.isBlank()) {
                return "错误: JSON 内容不能为空";
            }
            JSONObject jo = JSON.parseObject(jsonContent.trim());
            StringBuilder sb = new StringBuilder();
            flattenForProperties(jo, "", sb);
            LogUtil.info("jsonToProperties 完成");
            return sb.toString();
        });
    }

    private void flattenForProperties(JSONObject jo, String prefix, StringBuilder sb) {
        for (String key : jo.keySet()) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = jo.get(key);
            if (value instanceof JSONObject) {
                flattenForProperties((JSONObject) value, fullKey, sb);
            } else {
                sb.append(fullKey)
                  .append("=")
                  .append(value == null ? "" : String.valueOf(value))
                  .append("\n");
            }
        }
    }

    // --- 11. csv_to_json ---

    /**
     * 将 CSV 文本转换为 JSON 数组格式。
     *
     * @param csvContent CSV 文本内容
     * @param delimiter  分隔符，默认为逗号
     * @param hasHeader  是否有表头，默认 true
     * @return JSON 数组字符串
     */
    @Tool(name = "csv_to_json", description = "将 CSV 文本转换为 JSON 数组格式。")
    public String csvToJson(@ToolParam(description = "CSV 文本内容") String csvContent,
        @ToolParam(description = "分隔符，默认为逗号", required = false) String delimiter,
        @ToolParam(description = "是否有表头行，默认 true", required = false) Boolean hasHeader) {
        return execute("csvToJson", () -> {
            if (csvContent == null || csvContent.isBlank()) {
                return "错误: CSV 内容不能为空";
            }
            String delim = (delimiter != null && !delimiter.isBlank()) ? delimiter : ",";
            boolean header = hasHeader == null || hasHeader;

            String[] lines = csvContent.trim()
                                       .split("\\r?\\n");
            if (lines.length == 0) {
                return "[]";
            }

            JSONArray result = new JSONArray();
            String[] headers;

            if (header && lines.length >= 1) {
                headers = parseCsvLine(lines[0], delim);
                for (int i = 1; i < lines.length; i++) {
                    String[] values = parseCsvLine(lines[i], delim);
                    JSONObject row = new JSONObject();
                    for (int j = 0; j < headers.length && j < values.length; j++) {
                        row.put(headers[j].trim(), values[j].trim());
                    }
                    result.add(row);
                }
            } else {
                for (String line : lines) {
                    String[] values = parseCsvLine(line, delim);
                    JSONArray row = new JSONArray();
                    for (String v : values) {
                        row.add(v.trim());
                    }
                    result.add(row);
                }
            }

            LogUtil.info("csvToJson 完成: {}行", result.size());
            return JSON.toJSONString(result);
        });
    }

    private String[] parseCsvLine(String line, String delimiter) {
        // Simple CSV parsing handling quoted fields
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes && line.substring(i)
                                        .startsWith(delimiter)) {
                fields.add(field.toString());
                field.setLength(0);
                i += delimiter.length() - 1;
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    // --- 12. xml_format ---

    /**
     * 对 XML 字符串进行格式化美化。
     *
     * @param xmlContent 要格式化的 XML 字符串
     * @return 格式化后的 XML 字符串
     */
    @Tool(name = "xml_format", description = "对 XML 字符串进行格式化美化。")
    public String xmlFormat(@ToolParam(description = "要格式化的 XML 字符串") String xmlContent) {
        return execute("xmlFormat", () -> {
            if (xmlContent == null || xmlContent.isBlank()) {
                return "错误: XML 内容不能为空";
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlContent.trim()
                                                                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            Transformer transformer = TransformerFactory.newInstance()
                                                        .newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            LogUtil.info("xmlFormat 完成");
            return writer.toString();
        });
    }
}
