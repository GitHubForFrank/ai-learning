package org.springframework.ai.mcp.sample.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import java.util.Map;

/**
 * With stdio transport, the MCP handler is automatically started by the client. But you
 * have to build the handler jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */
public class ClientStdio {

    public static void main(String[] args) {

        var stdioParams = ServerParameters.builder("java")
                                          .args("-jar", "D:\\workspaces\\My-MCP-Server\\mcp-handler-demo-stdio-0.0.1-SNAPSHOT.jar")
                                          .build();

        ObjectMapper objectMapper = new ObjectMapper();
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        var transport = new StdioClientTransport(stdioParams, jsonMapper);
        var client = McpClient.sync(transport)
                              .build();

        client.initialize();

        // List and demonstrate tools
        ListToolsResult toolsList = client.listTools();
        System.out.println("Available Tools = " + toolsList);

        CallToolResult weatherForcastResult = client.callTool(
            new CallToolRequest("getWeatherForecastByLocation", Map.of("latitude", "47.6062", "longitude", "-122.3321")));
        System.out.println("Weather Forcast: " + weatherForcastResult);

        CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", Map.of("state", "NY")));
        System.out.println("Alert Response = " + alertResult);

        client.closeGracefully();
    }

}