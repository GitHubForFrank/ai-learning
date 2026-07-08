package org.springframework.ai.mcp.sample.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import java.io.File;

/**
 * With stdio transport, the MCP handler is automatically started by the client.
 * But you
 * have to build the handler jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 *
 * @author Frank Kang
 * @since 2023-09-01 09:05
 */
public class ClientStdio {

    public static void main(String[] args) {

        System.out.println(new File(".").getAbsolutePath());

        var stdioParams = ServerParameters.builder("java")
                                          .args("-Dspring.ai.mcp.handler.stdio=true", "-Dspring.main.web-application-type=none",
                                                "-Dlogging.pattern.console=", "-jar",
                                                "model-context-protocol/weather/starter-webmvc-handler/target/mcp-weather-starter-webmvc-handler-0.0.1-SNAPSHOT.jar")
                                          .build();
        ObjectMapper objectMapper = new ObjectMapper();
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        var transport = new StdioClientTransport(stdioParams, jsonMapper);

        new SampleClient(transport).run();
    }

}
