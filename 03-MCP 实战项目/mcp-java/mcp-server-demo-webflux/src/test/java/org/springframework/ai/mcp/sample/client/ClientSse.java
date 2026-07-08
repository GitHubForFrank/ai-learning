package org.springframework.ai.mcp.sample.client;

import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;


/**
 * @author Frank Kang
 * @since 2023-09-01 09:05
 */
public class ClientSse {
    
    public static void main(String[] args) {
        var transport = HttpClientSseClientTransport.builder("http://localhost:8080")
                                                    .build();
        new SampleClient(transport).run();
    }
    
}
