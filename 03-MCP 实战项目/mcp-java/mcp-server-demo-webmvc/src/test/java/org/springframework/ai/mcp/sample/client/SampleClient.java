package org.springframework.ai.mcp.sample.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import java.util.Map;

/**
 * @author Frank Kang
 * @since 2023-09-01 09:05
 */
public record SampleClient(McpClientTransport transport) {
    
    public void run() {
        var client = McpClient.sync(this.transport)
                              .build();
        client.initialize();
        client.ping();
        
        // List and demonstrate tools
        ListToolsResult toolsList = client.listTools();
        System.out.println("Available Tools = " + toolsList);
        toolsList.tools()
                 .forEach(tool -> {
                     System.out.println("Tool: " + tool.name() + ", description: " + tool.description() + ", schema: " + tool.inputSchema());
                 });
        
        CallToolResult weatherForecastResult = client.callTool(
            new CallToolRequest("getWeatherForecastByLocation", Map.of("latitude", "47.6062", "longitude", "-122.3321")));
        System.out.println("Weather Forecast: " + weatherForecastResult);
        
        CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", Map.of("state", "NY")));
        System.out.println("Alert Response = " + alertResult);
        
        client.closeGracefully();
        
    }
    
}
